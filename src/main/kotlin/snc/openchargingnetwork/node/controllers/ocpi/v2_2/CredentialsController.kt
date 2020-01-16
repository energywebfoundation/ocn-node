/*
    Copyright 2019 Share&Charge Foundation

    This file is part of Open Charging Network Node.

    Open Charging Network Node is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Open Charging Network Node is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Open Charging Network Node.  If not, see <https://www.gnu.org/licenses/>.
*/

package snc.openchargingnetwork.node.controllers.ocpi.v2_2

import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import snc.openchargingnetwork.node.repositories.*
import snc.openchargingnetwork.node.config.NodeProperties
import snc.openchargingnetwork.node.models.ocpi.ConnectionStatus
import snc.openchargingnetwork.node.models.ocpi.Role
import snc.openchargingnetwork.node.models.ocpi.OcpiStatus
import snc.openchargingnetwork.node.models.entities.Auth
import snc.openchargingnetwork.node.models.entities.EndpointEntity
import snc.openchargingnetwork.node.models.entities.RoleEntity
import snc.openchargingnetwork.node.models.exceptions.OcpiClientInvalidParametersException
import snc.openchargingnetwork.node.models.exceptions.OcpiServerNoMatchingEndpointsException
import snc.openchargingnetwork.node.models.ocpi.*
import snc.openchargingnetwork.node.services.HttpService
import snc.openchargingnetwork.node.services.RoutingService
import snc.openchargingnetwork.node.tools.*


@RestController
@RequestMapping("/ocpi/2.2/credentials")
class CredentialsController(private val platformRepo: PlatformRepository,
                            private val roleRepo: RoleRepository,
                            private val endpointRepo: EndpointRepository,
                            private val properties: NodeProperties,
                            private val routingService: RoutingService,
                            private val httpService: HttpService) {

    @GetMapping
    fun getCredentials(@RequestHeader("Authorization") authorization: String): OcpiResponse<Credentials> {

        // TODO: allow token A authorization

        return platformRepo.findByAuth_TokenC(authorization.extractToken())?.let {

            OcpiResponse(
                    statusCode = OcpiStatus.SUCCESS.code,
                    data = Credentials(
                            token = it.auth.tokenC!!,
                            url = urlJoin(properties.url, "/ocpi/versions"),
                            roles = listOf(CredentialsRole(
                                    role = Role.HUB,
                                    businessDetails = BusinessDetails(name = "Share&Charge Message Broker"),
                                    partyID = "SNC",
                                    countryCode = "DE"))))

        } ?: throw OcpiClientInvalidParametersException("Invalid CREDENTIALS_TOKEN_C")
    }

    @PostMapping
    @Transactional
    fun postCredentials(@RequestHeader("Authorization") authorization: String,
                        @RequestBody body: Credentials): OcpiResponse<Credentials> {

        // check platform previously registered by admin
        val platform = platformRepo.findByAuth_TokenA(authorization.extractToken())
                ?: throw OcpiClientInvalidParametersException("Invalid CREDENTIALS_TOKEN_A")

        // GET versions information endpoint with TOKEN_B (both provided in request body)
        val versionsInfo = httpService.getVersions(body.url, body.token)

        // try to match version 2.2
        val correctVersion = versionsInfo.versions.firstOrNull { it.version == "2.2" }
                ?: throw OcpiServerNoMatchingEndpointsException("Expected version 2.2 from $versionsInfo")

        // GET 2.2 version details
        val versionDetail = httpService.getVersionDetail(correctVersion.url, body.token)

        // ensure each role does not already exist
        for (role in body.roles) {
            val basicRole = BasicRole(role.partyID, role.countryCode)
            if (!routingService.isRoleKnownOnNetwork(basicRole)) {
                throw OcpiClientInvalidParametersException("Role with party_id=${basicRole.id} and country_code=${basicRole.country} not listed in OCN Registry with my node info!")
            }
            // TODO: check existance by role (MSP/CPO), to support a platform with a CPO and MSP implementation
            //  that uses the same country_code/party_id
            if (roleRepo.existsByCountryCodeAndPartyIDAllIgnoreCase(basicRole.country, basicRole.id)) {
                throw OcpiClientInvalidParametersException("Role with party_id=${basicRole.id} and country_code=${basicRole.country} already connected to this node!")
            }
        }

        // generate TOKEN_C
        val tokenC = generateUUIDv4Token()

        // set platform connection details
        platform.auth = Auth(tokenA = null, tokenB = body.token, tokenC = tokenC)
        platform.versionsUrl = body.url
        platform.status = ConnectionStatus.CONNECTED
        platform.lastUpdated = getTimestamp()

        // set platform's roles' credentials
        val roles = mutableListOf<RoleEntity>()

        for (role in body.roles) {
            roles.add(RoleEntity(
                    platformID = platform.id!!,
                    role = role.role,
                    businessDetails = role.businessDetails,
                    partyID = role.partyID,
                    countryCode = role.countryCode))
        }

        platformRepo.save(platform)
        roleRepo.saveAll(roles)

        // set platform's endpoints
        for (endpoint in versionDetail.endpoints) {
            endpointRepo.save(EndpointEntity(
                    platformID = platform.id!!,
                    identifier = endpoint.identifier,
                    role = endpoint.role,
                    url = endpoint.url
            ))
        }

        // return Broker's platform connection information and role credentials
        return OcpiResponse(
                statusCode = OcpiStatus.SUCCESS.code,
                data = Credentials(
                        token = tokenC,
                        url = urlJoin(properties.url, "/ocpi/versions"),
                        roles = listOf(CredentialsRole(
                                role = Role.HUB,
                                businessDetails = BusinessDetails(name = "Open Charging Network Node"),
                                partyID = "OCN",
                                countryCode = "DE"))))
    }

    @PutMapping
    @Transactional
    fun putCredentials(@RequestHeader("Authorization") authorization: String,
                       @RequestBody body: Credentials): OcpiResponse<Credentials> {

        // find platform (required to have already been fully registered)
        val platform = platformRepo.findByAuth_TokenC(authorization.extractToken())
                ?: throw OcpiClientInvalidParametersException("Invalid CREDENTIALS_TOKEN_C")

        // GET versions information endpoint with TOKEN_B (both provided in request body)
        val versionsInfo: Versions = httpService.getVersions(body.url, body.token)

        // try to match version 2.2
        val correctVersion = versionsInfo.versions.firstOrNull { it.version == "2.2" }
                ?: throw OcpiClientInvalidParametersException("Expected version 2.2 from ${body.url}")

        // GET 2.2 version details
        val versionDetail = httpService.getVersionDetail(correctVersion.url, body.token)

        // generate TOKEN_C
        val tokenC = generateUUIDv4Token()

        // set platform connection information
        platform.auth = Auth(tokenA = null, tokenB = body.token, tokenC = tokenC)
        platform.versionsUrl = body.url
        platform.status = ConnectionStatus.CONNECTED
        platform.lastUpdated = getTimestamp()

        endpointRepo.deleteByPlatformID(platform.id)
        roleRepo.deleteByPlatformID(platform.id)

        // set platform's roles' credentials
        val roles = mutableListOf<RoleEntity>()

        for (role in body.roles) {
            roles.add(RoleEntity(
                    platformID = platform.id!!,
                    role = role.role,
                    businessDetails = role.businessDetails,
                    partyID = role.partyID,
                    countryCode = role.countryCode))
        }

        platformRepo.save(platform)
        roleRepo.saveAll(roles)

        // set platform's endpoints
        for (endpoint in versionDetail.endpoints) {
            endpointRepo.save(EndpointEntity(
                    platformID = platform.id!!,
                    identifier = endpoint.identifier,
                    role = endpoint.role,
                    url = endpoint.url))
        }

        // return OCN Node's platform connection information and role credentials (same for all nodes)
        return OcpiResponse(
                statusCode = OcpiStatus.SUCCESS.code,
                data = Credentials(
                        token = tokenC,
                        url = urlJoin(properties.url, "/ocpi/versions"),
                        roles = listOf(CredentialsRole(
                                role = Role.HUB,
                                businessDetails = BusinessDetails(name = "Open Charging Network Node"),
                                partyID = "OCN",
                                countryCode = "DE"))))
    }

    @DeleteMapping
    @Transactional
    fun deleteCredentials(@RequestHeader("Authorization") authorization: String): OcpiResponse<Nothing?> {

        val platform = platformRepo.findByAuth_TokenC(authorization.extractToken())
                ?: throw OcpiClientInvalidParametersException("Invalid CREDENTIALS_TOKEN_C")

        platformRepo.deleteById(platform.id!!)
        roleRepo.deleteByPlatformID(platform.id)
        endpointRepo.deleteByPlatformID(platform.id)

        return OcpiResponse(statusCode = 1000, data = null)
    }

}