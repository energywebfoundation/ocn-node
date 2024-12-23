package snc.openchargingnetwork.node.controllers.ocpi.v2_2

import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import snc.openchargingnetwork.node.components.OcpiRequestHandlerBuilder
import snc.openchargingnetwork.node.models.OcnHeaders
import snc.openchargingnetwork.node.models.ocpi.*
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("\${ocn.node.apiPrefix}/ocpi/custom")
class CustomModulesController(private val requestHandlerBuilder: OcpiRequestHandlerBuilder) {

    @RequestMapping("/{interfaceRole}/{module}", "/{interfaceRole}/{module}/**/*")
    fun customModuleMapping(@RequestHeader("authorization") authorization: String,
                            @RequestHeader("OCN-Signature") signature: String? = null,
                            @RequestHeader("X-Request-ID") requestID: String,
                            @RequestHeader("X-Correlation-ID") correlationID: String,
                            @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                            @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                            @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                            @RequestHeader("OCPI-to-party-id") toPartyID: String,
                            @PathVariable interfaceRole: String,
                            @PathVariable module: String,
                            @RequestParam queryParams: Map<String, Any>,
                            @RequestBody body: String?,
                            request: HttpServletRequest): ResponseEntity<OcpiResponse<Any>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        val urlPath = try {
            request.pathInfo.replace("/ocpi/custom/${interfaceRole}/${module}", "")
        } catch (e: IllegalStateException) { // catch IllegalStateException: request.pathInfo must not be null
            null
        }

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.CUSTOM,
                customModuleId = module,
                interfaceRole = InterfaceRole.resolve(interfaceRole),
                method = HttpMethod.valueOf(request.method),
                headers = OcnHeaders(authorization, signature, requestID, correlationID, sender, receiver),
                urlPath = urlPath,
                queryParams = queryParams,
                body = body)

        return requestHandlerBuilder
                .build<Any>(requestVariables)
                .forwardDefault()
                .getResponseWithAllHeaders()
    }

}