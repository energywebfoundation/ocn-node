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

package snc.openchargingnetwork.node.services

import org.springframework.stereotype.Service
import snc.openchargingnetwork.node.models.OcnRules
import snc.openchargingnetwork.node.models.OcnRulesList
import snc.openchargingnetwork.node.models.OcnRulesListParty
import snc.openchargingnetwork.node.models.OcnRulesListType
import snc.openchargingnetwork.node.models.entities.OcnRulesListEntity
import snc.openchargingnetwork.node.models.entities.PlatformEntity
import snc.openchargingnetwork.node.models.exceptions.OcpiClientGenericException
import snc.openchargingnetwork.node.models.exceptions.OcpiClientInvalidParametersException
import snc.openchargingnetwork.node.models.ocpi.BasicRole
import snc.openchargingnetwork.node.models.ocpi.ModuleID
import snc.openchargingnetwork.node.repositories.OcnRulesListRepository
import snc.openchargingnetwork.node.repositories.PlatformRepository
import snc.openchargingnetwork.node.tools.extractToken


@Service
class OcnRulesService(private val platformRepo: PlatformRepository,
                      private val ocnRulesListRepo: OcnRulesListRepository) {

    /**
     * OcnRules GET receiver interface (retrieve list of client-owned rules as saved on node)
     */
    fun getRules(authorization: String): OcnRules {
        val platform = findPlatform(authorization)

        val rulesList = ocnRulesListRepo.findAllByPlatformID(platform.id)
                .map { OcnRulesListParty(
                        id = it.counterparty.id,
                        country = it.counterparty.country,
                        modules = it.modules)
                }

        return OcnRules(
                signatures = platform.rules.signatures,
                whitelist = OcnRulesList(
                        active = platform.rules.whitelist,
                        list = when (platform.rules.whitelist) {
                            true -> rulesList
                            false -> listOf()
                        }),
                blacklist = OcnRulesList(
                        active = platform.rules.blacklist,
                        list = when (platform.rules.blacklist) {
                            true -> rulesList
                            false -> listOf()
                        }))
    }

    /**
     * OcnRules PUT receiver interface to update signature setting
     */
    fun updateSignatures(authorization: String) {
        val platform = findPlatform(authorization)
        platform.rules.signatures = !platform.rules.signatures
        platformRepo.save(platform)
    }

    /**
     * OcnRules PUT receiver interface to blacklist all parties (by setting empty active whitelist)
     */
    fun blockAll(authorization: String) {
        // 1. check C / find platform
        val platform = findPlatform(authorization)

        // 2. determine whether whitelist is active
        assertListNotActive(platform, OcnRulesListType.BLACKLIST)

        // 3. set the whitelist to true with empty list
        platform.rules.whitelist = true
        ocnRulesListRepo.deleteAll()

        // 4. save whitelist option
        platformRepo.save(platform)
    }

    /**
     * OcnRules PUT receiver interface (updates entire whitelist)
     */
    fun updateWhitelist(authorization: String, parties: List<OcnRulesListParty>) {
        // 1. check if any module of party is not empty
        checkModuleList(parties)

        // 2. check token C / find platform
        val platform = findPlatform(authorization)

        // 3. determine whether whitelist is active
        platform.rules.whitelist = when (parties.count()) {
            // set to false if provided list is empty (deletes list)
            0 -> false
            else -> {
                // 2.1. check blacklist active
                assertListNotActive(platform, OcnRulesListType.BLACKLIST)
                // set to true if list not empty
                true
            }
        }

        // 4. save whitelist option
        platformRepo.save(platform)

        // 5. re-apply whitelist
        ocnRulesListRepo.deleteByPlatformID(platform.id)

        ocnRulesListRepo.saveAll(parties.map { OcnRulesListEntity(
            platformID = platform.id!!,
            counterparty = BasicRole(it.id, it.country).toUpperCase(),
            modules = it.modules)
        })
    }

    /**
     * OcnRules PUT receiver interface (updates entire blacklist)
     */
    fun updateBlacklist(authorization: String, parties: List<OcnRulesListParty>) {
        // 1. check if any module of party is not empty
        checkModuleList(parties)

        // 2. check token C / find platform
        val platform = findPlatform(authorization)

        // 3. determine whether blacklist is active
        platform.rules.blacklist = when (parties.count()) {
            // set to false if provided list is empty (delete list)
            0 -> false
            else -> {
                // 2.1 check whitelist is active
                assertListNotActive(platform, OcnRulesListType.WHITELIST)
                // set true if list is not empty
                true
            }
        }

        // 4. save blacklist option
        platformRepo.save(platform)

        // 5. re-apply blacklist
        ocnRulesListRepo.deleteByPlatformID(platform.id)

        ocnRulesListRepo.saveAll(parties.map { OcnRulesListEntity(
                platformID = platform.id!!,
                counterparty = BasicRole(it.id, it.country).toUpperCase(),
                modules = it.modules)})
    }

    /**
     * OcnRules POST receiver interface (appends single entry to whitelist)
     */
    fun appendToWhitelist(authorization: String, body: OcnRulesListParty) {
        // 1. check module of party is not empty
        checkModule(body.modules)

        // 2. check token C / find platform
        val platform = findPlatform(authorization)

        // 3. check blacklist active
        assertListNotActive(platform, OcnRulesListType.BLACKLIST)

        // 4. set whitelist to true
        platform.rules.whitelist = true

        // 5. check entry does not already exist
        if (ocnRulesListRepo.existsByCounterparty(BasicRole( id = body.id, country = body.country).toUpperCase())) {
            throw OcpiClientInvalidParametersException("Party already on OCN Rules whitelist")
        }

        // 6. save whitelist option
        platformRepo.save(platform)

        // 7. add to whitelist
        ocnRulesListRepo.save(OcnRulesListEntity(
                platformID = platform.id!!,
                counterparty = BasicRole( id = body.id, country = body.country).toUpperCase(),
                modules = body.modules))
    }

    /**
     * OcnRules POST receiver interface (appends single entry to blacklist)
     */
    fun appendToBlacklist (authorization: String, body: OcnRulesListParty) {
        // 1. check module of party is not empty
        checkModule(body.modules)

        // 2. check token C/ find platform
        val platform = findPlatform(authorization)

        // 3. check whitelist active
        assertListNotActive(platform, OcnRulesListType.WHITELIST)

        // 4. set blacklist to true
        platform.rules.blacklist = true

        // 5. check entry does not already exist
        if (ocnRulesListRepo.existsByCounterparty(BasicRole( id = body.id, country = body.country).toUpperCase())) {
            throw OcpiClientInvalidParametersException("Party already on OCN Rules blacklist")
        }

        // 6. save blacklist option
        platformRepo.save(platform)

        // 7. add to blacklist
        ocnRulesListRepo.save(OcnRulesListEntity(
                platformID = platform.id!!,
                counterparty = BasicRole( id = body.id, country = body.country).toUpperCase(),
                modules = body.modules))
    }

    /**
     * OcnRules DELETE receiver interface (deletes single entry from whitelist)
     */
    fun deleteFromWhitelist(authorization: String, party: BasicRole) {
        // 1. check token C / find platform
        val platform = findPlatform(authorization)

        // 2. check whitelist/blacklist activeness
        if (platform.rules.blacklist || !platform.rules.whitelist) {
            throw OcpiClientGenericException("Cannot delete entry from OCN Rules whitelist")
        }

        // 3. delete entry
        ocnRulesListRepo.deleteByPlatformIDAndCounterparty(platform.id, party)

        // 4. set activeness
        platform.rules.whitelist = ocnRulesListRepo.findAllByPlatformID(platform.id).count() >= 1
        platformRepo.save(platform)
    }

    /**
     * OcnRules DELETE receiver interface (deletes single entry from blacklist)
     */
    fun deleteFromBlacklist(authorization: String, party: BasicRole) {
        // 1. check token C / find platform
        val platform = findPlatform(authorization)

        // 2. check blacklist/whitelist activeness
        if (platform.rules.whitelist || !platform.rules.blacklist) {
            throw OcpiClientGenericException("Cannot delete entry from OCN Rules blacklist")
        }

        // 3. delete entry
        ocnRulesListRepo.deleteByPlatformIDAndCounterparty(platform.id, party)

        // 4. set activeness
        platform.rules.blacklist = ocnRulesListRepo.findAllByPlatformID(platform.id).count() >= 1
        platformRepo.save(platform)
    }

    /**
     * Checks a counter-party has been whitelisted by a connected platform
     */
    fun isWhitelisted(platform: PlatformEntity, counterParty: BasicRole): Boolean {
        val rulesList = ocnRulesListRepo.findAllByPlatformID(platform.id)

        val compareParties = { party: BasicRole -> party.toUpperCase() == counterParty.toUpperCase() }

        return when {
            platform.rules.whitelist -> rulesList.any { compareParties(it.counterparty) }
            platform.rules.blacklist -> rulesList.none { compareParties(it.counterparty) }
            else -> true
        }
    }

    /**
     * Checks a counter-party has been whitelisted with specific module allowed by a connected platform
     */
    fun isWhitelisted(platform: PlatformEntity, counterParty: BasicRole, moduleID: String): Boolean {
        val rulesList = ocnRulesListRepo.findAllByPlatformID(platform.id)

        return when {
            platform.rules.whitelist -> rulesList.any { validateWhiteListWithModule(it, counterParty, moduleID) }
            platform.rules.blacklist -> rulesList.none { validateBlackListWithModule(it, counterParty, moduleID) }
            else -> true
        }
    }

    private fun findPlatform(authorization: String): PlatformEntity {
        return platformRepo.findByAuth_TokenC(authorization.extractToken())
                ?: throw OcpiClientInvalidParametersException("Invalid CREDENTIALS_TOKEN_C")
    }

    private fun checkModule(modules: List<String>) {
        val result = modules.any{ it.isEmpty() }

        if(result) {
            throw OcpiClientGenericException("Module list is empty")
        }
    }

    private fun checkModuleList(parties: List<OcnRulesListParty>) {
        // 1. check Module is empty or not
        var result = parties.any { it.modules.isNullOrEmpty() }

        if(result) {
            throw OcpiClientGenericException("Module list of one the party is empty")
        }

        // 2. check each element of module is empty or not
        result = parties.any { it -> it.modules.any { it.isEmpty() } }

        if(result) {
            throw OcpiClientGenericException("One of the element of module list is empty")
        }
    }

    private fun assertListNotActive(platform: PlatformEntity, type: OcnRulesListType) {
        val list = when (type) {
            OcnRulesListType.WHITELIST -> platform.rules.whitelist
            OcnRulesListType.BLACKLIST -> platform.rules.blacklist
        }
        if (list) {
            throw OcpiClientGenericException("OCN Rules whitelist and blacklist cannot be active at same time")
        }
    }

    /**
     * For whitelist check receiver has allowed the module of sender to send them message
     */
    private fun validateWhiteListWithModule (rule: OcnRulesListEntity, sender: BasicRole, moduleID: String): Boolean {
        if (rule.counterparty == sender) {
            if (!rule.modules.contains(moduleID)) {
                throw OcpiClientGenericException("Sender not whitelisted to request $moduleID from receiver.")
            }
            return true
        }
        return false
    }

    /**
     * For blacklist check receiver has allowed the module of sender to send them message
     */
    private fun validateBlackListWithModule (rule: OcnRulesListEntity, sender: BasicRole, moduleID: String): Boolean {
        println("sender = $sender")
        println("rule = ${rule.counterparty} ${rule.modules}}")
        if (rule.counterparty == sender) {
            if (rule.modules.contains(moduleID)) {
                throw OcpiClientGenericException("Sender not whitelisted to request $moduleID from receiver.")
            }
            return true
        }
        return false
    }

}