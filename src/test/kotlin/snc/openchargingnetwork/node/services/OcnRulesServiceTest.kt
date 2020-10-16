package snc.openchargingnetwork.node.services

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import snc.openchargingnetwork.node.models.entities.OcnRules
import snc.openchargingnetwork.node.models.entities.OcnRulesListEntity
import snc.openchargingnetwork.node.models.entities.PlatformEntity
import snc.openchargingnetwork.node.models.ocpi.BasicRole
import snc.openchargingnetwork.node.repositories.OcnRulesListRepository
import snc.openchargingnetwork.node.repositories.PlatformRepository
import java.util.stream.Stream

data class InWhitelistTest(val rules: OcnRules = OcnRules(), // receiving platform's active rules
                           val rulesListCounterParty: BasicRole = BasicRole("QUE", "CA"), // receiving platform's counter party for above rules
                           val testCounterParty: BasicRole, // the actual counter party to test against
                           val expectedResult: Boolean)

class OcnRulesServiceTest {

    private val platformRepo: PlatformRepository = mockk()
    private val ocnRulesListRepo: OcnRulesListRepository = mockk()

    private var ocnRulesService = OcnRulesService(platformRepo, ocnRulesListRepo)

    private fun inWhiteListTestSources(): Stream<Arguments> {
        return Stream.of(
                Arguments.of(InWhitelistTest(
                        testCounterParty = BasicRole("VAN", "CA"),
                        expectedResult = true)),
                Arguments.of(InWhitelistTest(
                        rules = OcnRules(whitelist = true),
                        rulesListCounterParty = BasicRole("VAN", "CA"),
                        testCounterParty = BasicRole("VAN", "CA"),
                        expectedResult = true)),
                Arguments.of(InWhitelistTest(
                        rules = OcnRules(whitelist = true),
                        rulesListCounterParty = BasicRole("OTT", "CA"),
                        testCounterParty = BasicRole("VAN", "CA"),
                        expectedResult = false)),
                Arguments.of(InWhitelistTest(
                        rules = OcnRules(blacklist = true),
                        rulesListCounterParty = BasicRole("OTT", "CA"),
                        testCounterParty = BasicRole("VAN", "CA"),
                        expectedResult = true)),
                Arguments.of(InWhitelistTest(
                        rules = OcnRules(blacklist = true),
                        rulesListCounterParty = BasicRole("VAN", "CA"),
                        testCounterParty = BasicRole("VAN", "CA"),
                        expectedResult = false)))
    }

    @ParameterizedTest
    @MethodSource("inWhiteListTestSources")
    fun isWhitelisted(case: InWhitelistTest) {
        val platform = PlatformEntity(id = 5, rules = case.rules)

        every { ocnRulesListRepo.findAllByPlatformID(platform.id) } returns listOf(
                OcnRulesListEntity(
                        platformID = platform.id!!,
                        counterparty = case.rulesListCounterParty,
                        modules = listOf()))

        val actual = ocnRulesService.isWhitelisted(platform, case.testCounterParty)
        assertThat(actual).isEqualTo(case.expectedResult)
    }

}