package snc.openchargingnetwork.node.controllers.ocpi.v2_2

import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import snc.openchargingnetwork.node.components.HttpClientComponent
import snc.openchargingnetwork.node.components.OcpiRequestHandlerBuilder
import snc.openchargingnetwork.node.models.OcnHeaders
import snc.openchargingnetwork.node.models.entities.PlatformEntity
import snc.openchargingnetwork.node.models.exceptions.OcpiServerGenericException
import snc.openchargingnetwork.node.models.ocpi.BasicRole
import snc.openchargingnetwork.node.models.ocpi.CDR
import snc.openchargingnetwork.node.models.ocpi.ClientInfo
import snc.openchargingnetwork.node.models.ocpi.InterfaceRole
import snc.openchargingnetwork.node.models.ocpi.ModuleID
import snc.openchargingnetwork.node.models.ocpi.OcpiRequestVariables
import snc.openchargingnetwork.node.models.ocpi.OcpiResponse
import snc.openchargingnetwork.node.models.ocpi.OcpiStatus
import snc.openchargingnetwork.node.models.ocpi.Version
import snc.openchargingnetwork.node.repositories.PlatformRepository
import snc.openchargingnetwork.node.repositories.RoleRepository
import snc.openchargingnetwork.node.services.VersionsService
import snc.openchargingnetwork.node.tools.filterNull


@RestController
@RequestMapping("\${ocn.node.apiPrefix}/ocpi/2.2.1")
class VersionsController(
    private val requestHandlerBuilder: OcpiRequestHandlerBuilder,
    private val versionsService: VersionsService
) {

    @GetMapping("/versions")
    fun getHubClientInfo(
        @RequestHeader("authorization") authorization: String,
        @RequestHeader("OCN-Signature") signature: String? = null,
        @RequestHeader("X-Request-ID") requestID: String,
        @RequestHeader("X-Correlation-ID") correlationID: String,
        @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
        @RequestHeader("OCPI-from-party-id") fromPartyID: String,
        @RequestHeader("OCPI-to-country-code") toCountryCode: String,
        @RequestHeader("OCPI-to-party-id") toPartyID: String,
    ): ResponseEntity<OcpiResponse<List<Version>>> {
        val (isLocalParty, versions) = versionsService.getPartyVersions(toCountryCode, toPartyID);

        if(isLocalParty) {
            return ResponseEntity.ok(
                OcpiResponse(
                    statusCode = OcpiStatus.SUCCESS.code,
                    data = versions
                )
            )
        }

        // Remote party
        val sender = BasicRole(fromPartyID, fromCountryCode);
        val receiver = BasicRole(toPartyID, toCountryCode);

        val requestVariables = OcpiRequestVariables(
            module = ModuleID.VERSIONS,
            interfaceRole = InterfaceRole.SENDER,
            method = HttpMethod.GET,
            headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
        )

        return requestHandlerBuilder
            .build<List<Version>>(requestVariables)
            .forwardDefault()
            .getResponseWithPaginationHeaders()
    }
}