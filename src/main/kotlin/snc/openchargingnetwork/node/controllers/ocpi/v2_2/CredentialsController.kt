/*
    Copyright 2019-2020 eMobilify GmbH

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

package snc.openchargingnetwork.node.controllers.ocpi.v2_2

import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import snc.openchargingnetwork.node.config.NodeProperties
import snc.openchargingnetwork.node.models.entities.Auth
import snc.openchargingnetwork.node.models.entities.EndpointEntity
import snc.openchargingnetwork.node.models.entities.RoleEntity
import snc.openchargingnetwork.node.models.exceptions.OcpiClientInvalidParametersException
import snc.openchargingnetwork.node.models.exceptions.OcpiServerNoMatchingEndpointsException
import snc.openchargingnetwork.node.models.ocpi.*
import snc.openchargingnetwork.node.models.ocpi.Role
import snc.openchargingnetwork.node.repositories.*
import snc.openchargingnetwork.node.services.HttpService
import snc.openchargingnetwork.node.services.RegistryService
import snc.openchargingnetwork.node.tools.*

@RequestMapping("\${ocn.node.apiPrefix}/ocpi/2.2/credentials")
@RestController
class CredentialsController(private val platformRepo: PlatformRepository,
                            private val roleRepo: RoleRepository,
                            private val endpointRepo: EndpointRepository,
                            private val networkClientInfoRepository: NetworkClientInfoRepository,
                            private val ocnRulesListRepo: OcnRulesListRepository,
                            private val properties: NodeProperties,
                            private val registryService: RegistryService,
                            private val httpService: HttpService) {

    private fun myCredentials(token: String): Credentials {
        return Credentials(
                token = token,
                url = urlJoin(properties.url, properties.apiPrefix, "/ocpi/versions"),
                roles = listOf(CredentialsRole(
                        role = Role.HUB,
                        businessDetails = BusinessDetails(name = "Open Charging Network Node"),
                        partyID = "OCN",
                        countryCode = "CH")))
    }

    @GetMapping
    fun getCredentials(@RequestHeader("Authorization") authorization: String): OcpiResponse<Credentials> {

        return platformRepo.findByAuth_TokenC(authorization.extractToken())?.let {

            OcpiResponse(
                    statusCode = OcpiStatus.SUCCESS.code,
                    data = myCredentials(it.auth.tokenC!!.fromBs64String()))

        } ?: throw OcpiClientInvalidParametersException("Invalid CREDENTIALS_TOKEN_C")
    }

    @PostMapping
    @Transactional
    fun postCredentials(@RequestHeader("Authorization") authorization: String,
                        @RequestBody body: Credentials): OcpiResponse<Credentials> {

        // TODO: create credentials service
        // TODO: detect changes to public URL to automatically update credentials on connected platforms

        // check platform previously registered by admin
        val platform = platformRepo.findByAuth_TokenA(authorization.extractToken())
                ?: throw OcpiClientInvalidParametersException("Invalid CREDENTIALS_TOKEN_A")

        // GET versions information endpoint with TOKEN_B (both provided in request body)
        val versionsInfo = httpService.getVersions(body.url, body.token.toBs64String())

        // try to match version 2.2
        val correctVersion = versionsInfo.firstOrNull { it.version == "2.2" || it.version == "2.2.1" }
                ?: throw OcpiServerNoMatchingEndpointsException("Expected version 2.2 or 2.2.1 from $versionsInfo")

        // GET 2.2 version details
        val versionDetail = httpService.getVersionDetail(correctVersion.url, body.token.toBs64String())

        // ensure each role does not already exist; delete if planned
        for (role in body.roles) {
            val basicRole = BasicRole(role.partyID, role.countryCode)
            if (!registryService.isRoleKnown(basicRole)) {
                throw OcpiClientInvalidParametersException("Role with party_id=${basicRole.id} and country_code=${basicRole.country} not listed in OCN Registry with my node info!")
            }
            if (roleRepo.existsByCountryCodeAndPartyIDAllIgnoreCase(basicRole.country, basicRole.id)) {
                throw OcpiClientInvalidParametersException("Role with party_id=${basicRole.id} and country_code=${basicRole.country} already connected to this node!")
            }
            if (networkClientInfoRepository.existsByPartyAndRole(basicRole.uppercase(), role.role)) {
                networkClientInfoRepository.deleteByPartyAndRole(basicRole.uppercase(), role.role)
            }
        }

        // generate TOKEN_C
        val tokenC = generateUUIDv4Token()

        // set platform connection details
        platform.auth = Auth(tokenA = null, tokenB = body.token.toBs64String(), tokenC = tokenC.toBs64String())
        platform.versionsUrl = body.url
        platform.status = ConnectionStatus.CONNECTED
        platform.lastUpdated = getTimestamp()
        platform.rules.signatures = properties.signatures

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

        platform.register(roles)
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

        // return OCN's platform connection information and role credentials
        return OcpiResponse(
                statusCode = OcpiStatus.SUCCESS.code,
                data = myCredentials(tokenC))
    }

    @PutMapping
    @Transactional
    fun putCredentials(@RequestHeader("Authorization") authorization: String,
                       @RequestBody body: Credentials): OcpiResponse<Credentials> {

        // find platform (required to have already been fully registered)
        val platform = platformRepo.findByAuth_TokenC(authorization.extractToken())
                ?: throw OcpiClientInvalidParametersException("Invalid CREDENTIALS_TOKEN_C")

        // GET versions information endpoint with TOKEN_B (both provided in request body)
        val versionsInfo: List<Version> = httpService.getVersions(body.url, body.token.toBs64String())

        // try to match version 2.2
        val correctVersion = versionsInfo.firstOrNull { it.version == "2.2" }
                ?: throw OcpiClientInvalidParametersException("Expected version 2.2 from ${body.url}")

        // GET 2.2 version details
        val versionDetail = httpService.getVersionDetail(correctVersion.url, body.token.toBs64String())

        // generate TOKEN_C
        val tokenC = generateUUIDv4Token()

        // set platform connection information
        platform.auth = Auth(tokenA = null, tokenB = body.token.toBs64String(), tokenC = tokenC.toBs64String())
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
                data = myCredentials(tokenC))
    }

    @DeleteMapping
    @Transactional
    fun deleteCredentials(@RequestHeader("Authorization") authorization: String): OcpiResponse<Nothing?> {

        val platform = platformRepo.findByAuth_TokenC(authorization.extractToken())
                ?: throw OcpiClientInvalidParametersException("Invalid CREDENTIALS_TOKEN_C")

        val roles = roleRepo.findAllByPlatformID(platform.id)
        platform.unregister(roles)
        platformRepo.save(platform)

        platformRepo.deleteById(platform.id!!)
        roleRepo.deleteByPlatformID(platform.id)
        endpointRepo.deleteByPlatformID(platform.id)
        ocnRulesListRepo.deleteByPlatformID(platform.id)

        return OcpiResponse(statusCode = 1000, data = null)
    }

}