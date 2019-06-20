package snc.connect.broker.controllers.ocpi.v2_2

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import snc.connect.broker.repositories.*
import snc.connect.broker.Properties
import snc.connect.broker.enums.ConnectionStatusType
import snc.connect.broker.enums.Role
import snc.connect.broker.enums.Status
import snc.connect.broker.models.entities.Auth
import snc.connect.broker.models.entities.EndpointEntity
import snc.connect.broker.models.entities.CredentialEntity
import snc.connect.broker.models.exceptions.OcpiServerUnusableApiException
import snc.connect.broker.models.ocpi.*
import snc.connect.broker.services.HttpRequestService
import snc.connect.broker.tools.*

@RestController
@RequestMapping("/ocpi/hub/2.2/credentials")
class CredentialsController(private val orgRepo: OrganizationRepository,
                            private val credentialRepo: CredentialRepository,
                            private val endpointRepo: EndpointRepository,
                            private val properties: Properties,
                            private val httpRequestService: HttpRequestService) {

    @GetMapping
    fun getCredentials(@RequestHeader("Authorization") authorization: String): ResponseEntity<OcpiResponseBody> {

        return orgRepo.findByAuth_TokenC(authorization.extractToken())?.let {

            ResponseEntity.ok().body(OcpiResponseBody(
                    Status.SUCCESS.code,
                    data = Credentials(
                            token = it.auth.tokenC!!,
                            url = urlJoin(properties.host, "/ocpi/hub/versions"),
                            roles = arrayOf(CredentialsRole(
                                    role = Role.HUB,
                                    businessDetails = BusinessDetails(name = "Share&Charge Message Broker"),
                                    partyID = "SNC",
                                    countryCode = "DE")))))

        } ?: ResponseEntity(HttpStatus.METHOD_NOT_ALLOWED)
    }

    @PostMapping
    fun postCredentials(@RequestHeader("Authorization") authorization: String,
                        @RequestBody body: Credentials): OcpiResponseBody {

        // check organization previously registered by admin
        val org = orgRepo.findByAuth_TokenA(authorization.extractToken())
                ?: return OcpiResponseBody(
                        Status.CLIENT_INVALID_PARAMETERS.code,
                        "Unauthorized: Requesting party not recognized")

        // GET versions information endpoint with TOKEN_B (both provided in request body)
        val versionsInfo: Versions = try {
            httpRequestService.getVersions(body.url, body.token)
        } catch (e: OcpiServerUnusableApiException) {
            return OcpiResponseBody(e.statusCode, e.message)
        }

        // try to match version 2.2
        val correctVersion = versionsInfo.versions.firstOrNull { it.version == "2.2" }
                ?: return OcpiResponseBody(
                        Status.SERVER_NO_MATCHING_ENDPOINTS.code,
                        "Expected version 2.2 url from ${body.url}")

        // GET 2.2 version details
        val versionDetail = try {
            httpRequestService.getVersionDetail(correctVersion.url, body.token)
        } catch (e: OcpiServerUnusableApiException) {
            return OcpiResponseBody(e.statusCode, e.message)
        }

        // ensure each role does not already exist
        for (role in body.roles) {
            if (credentialRepo.existsByCountryCodeAndPartyID(role.countryCode, role.partyID)) {
                return OcpiResponseBody(
                        Status.CLIENT_INVALID_PARAMETERS.code,
                        "Role with party_id=${role.partyID} and country_code=${role.countryCode} already registered"
                )
            }
        }

        // generate TOKEN_C
        val tokenC = generateUUIDv4Token()

        // set organization
        org.auth = Auth(tokenA = null, tokenB = body.token, tokenC = tokenC)
        org.versionsUrl = body.url
        org.status = ConnectionStatusType.CONNECTED
        org.lastUpdated = getTimestamp()
        orgRepo.save(org)

        // set endpoints
        for (endpoint in versionDetail.endpoints) {
            endpointRepo.save(EndpointEntity(
                    organization = org.id!!,
                    identifier = endpoint.identifier,
                    role = endpoint.role,
                    url = endpoint.url
            ))
        }

        // set credentials
        for (role in body.roles) {
            credentialRepo.save(CredentialEntity(
                    organization = org.id!!,
                    role = role.role,
                    businessDetails = role.businessDetails,
                    partyID = role.partyID,
                    countryCode = role.countryCode
            ))
        }

        // return Broker's credentials
        return OcpiResponseBody(
                Status.SUCCESS.code,
                data = Credentials(
                        token = tokenC,
                        url = urlJoin(properties.host, "/ocpi/hub/versions"),
                        roles = arrayOf(CredentialsRole(
                                role = Role.HUB,
                                businessDetails = BusinessDetails(name = "Share&Charge Message Broker"),
                                partyID = "SNC",
                                countryCode = "DE"))))
    }

    @PutMapping
    fun putCredentials(@RequestHeader("Authorization") authorization: String,
                       @RequestBody body: Credentials): ResponseEntity<OcpiResponseBody> {

        // find org or return 405: method not allowed if not registered
        val org = orgRepo.findByAuth_TokenC(authorization.extractToken())
                ?: return ResponseEntity(HttpStatus.METHOD_NOT_ALLOWED)

        // GET versions information endpoint with TOKEN_B (both provided in request body)
        val versionsInfo: Versions = try {
            httpRequestService.getVersions(body.url, body.token)
        } catch (e: OcpiServerUnusableApiException) {
            return ResponseEntity.ok().body(OcpiResponseBody(e.statusCode, e.message))
        }

        // try to match version 2.2
        val correctVersion = versionsInfo.versions.firstOrNull { it.version == "2.2" }
                ?: return ResponseEntity.ok().body(OcpiResponseBody(
                        Status.SERVER_NO_MATCHING_ENDPOINTS.code,
                        "Expected version 2.2 url from ${body.url}"))

        // GET 2.2 version details
        val versionDetail = try {
            httpRequestService.getVersionDetail(correctVersion.url, body.token)
        } catch (e: OcpiServerUnusableApiException) {
            return ResponseEntity.ok().body(OcpiResponseBody(e.statusCode, e.message))
        }

        // generate TOKEN_C
        val tokenC = generateUUIDv4Token()

        // set organization
        org.auth = Auth(tokenA = null, tokenB = body.token, tokenC = tokenC)
        org.versionsUrl = body.url
        org.status = ConnectionStatusType.CONNECTED
        org.lastUpdated = getTimestamp()
        orgRepo.save(org)

        // set endpoints
        for (endpoint in versionDetail.endpoints) {
            endpointRepo.deleteByOrganization(org.id)
            endpointRepo.save(EndpointEntity(
                    organization = org.id!!,
                    identifier = endpoint.identifier,
                    role = endpoint.role,
                    url = endpoint.url))
        }

        // set credentials
        for (role in body.roles) {
            credentialRepo.deleteByOrganization(org.id)
            credentialRepo.save(CredentialEntity(
                    organization = org.id!!,
                    role = role.role,
                    businessDetails = role.businessDetails,
                    partyID = role.partyID,
                    countryCode = role.countryCode))
        }

        // return Broker's credentials
        return ResponseEntity.ok().body(OcpiResponseBody(
                Status.SUCCESS.code,
                data = Credentials(
                        token = tokenC,
                        url = urlJoin(properties.host, "/ocpi/hub/versions"),
                        roles = arrayOf(CredentialsRole(
                                role = Role.HUB,
                                businessDetails = BusinessDetails(name = "Share&Charge Message Broker"),
                                partyID = "SNC",
                                countryCode = "DE")))))
    }

    @DeleteMapping
    fun deleteCredentials(@RequestHeader("Authorization") authorization: String): ResponseEntity<OcpiResponseBody> {

        val org = orgRepo.findByAuth_TokenC(authorization.extractToken())
                ?: return ResponseEntity(HttpStatus.METHOD_NOT_ALLOWED)

        orgRepo.deleteById(org.id!!)
        credentialRepo.deleteByOrganization(org.id)
        endpointRepo.deleteByOrganization(org.id)

        return ResponseEntity.ok().body(OcpiResponseBody(statusCode = 1000))
    }

}