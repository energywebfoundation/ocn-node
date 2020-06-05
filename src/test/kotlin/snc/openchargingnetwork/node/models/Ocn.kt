package snc.openchargingnetwork.node.models

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import snc.openchargingnetwork.node.models.ocpi.InterfaceRole
import snc.openchargingnetwork.node.models.ocpi.ModuleID
import java.util.stream.Stream

data class PermissionsMatcherTestCase(val request: BasicRequestType, val permission: OcnAppPermission, val expected: Boolean)

class OcnAppPermissionsTest {

    private fun permissionsTestSources(): Stream<Arguments> {
        return Stream.of(
                Arguments.of(PermissionsMatcherTestCase(
                        request = BasicRequestType(ModuleID.LOCATIONS, InterfaceRole.SENDER),
                        permission = OcnAppPermission.FORWARD_ALL,
                        expected = true)),
                Arguments.of(PermissionsMatcherTestCase(
                        request = BasicRequestType(ModuleID.CHARGING_PROFILES, InterfaceRole.RECEIVER),
                        permission = OcnAppPermission.FORWARD_ALL_RECEIVER,
                        expected = true)),
                Arguments.of(PermissionsMatcherTestCase(
                        request = BasicRequestType(ModuleID.COMMANDS, InterfaceRole.SENDER),
                        permission = OcnAppPermission.FORWARD_ALL_RECEIVER,
                        expected = false)),
                Arguments.of(PermissionsMatcherTestCase(
                        request = BasicRequestType(ModuleID.TARIFFS, InterfaceRole.SENDER),
                        permission = OcnAppPermission.FORWARD_ALL_SENDER,
                        expected = true)),
                Arguments.of(PermissionsMatcherTestCase(
                        request = BasicRequestType(ModuleID.COMMANDS, InterfaceRole.RECEIVER),
                        permission = OcnAppPermission.FORWARD_ALL_SENDER,
                        expected = false)),
                Arguments.of(PermissionsMatcherTestCase(
                        request = BasicRequestType(ModuleID.LOCATIONS, InterfaceRole.SENDER),
                        permission = OcnAppPermission.FORWARD_MODULE_LOCATIONS_SENDER,
                        expected = true)),
                Arguments.of(PermissionsMatcherTestCase(
                        request = BasicRequestType(ModuleID.LOCATIONS, InterfaceRole.RECEIVER),
                        permission = OcnAppPermission.FORWARD_MODULE_LOCATIONS_SENDER,
                        expected = false)),
                Arguments.of(PermissionsMatcherTestCase(
                        request = BasicRequestType(ModuleID.LOCATIONS, InterfaceRole.RECEIVER),
                        permission = OcnAppPermission.FORWARD_MODULE_LOCATIONS_RECEIVER,
                        expected = true)),
                Arguments.of(PermissionsMatcherTestCase(
                        request = BasicRequestType(ModuleID.LOCATIONS, InterfaceRole.SENDER),
                        permission = OcnAppPermission.FORWARD_MODULE_LOCATIONS_RECEIVER,
                        expected = false)),
                Arguments.of(PermissionsMatcherTestCase(
                        request = BasicRequestType(ModuleID.SESSIONS, InterfaceRole.SENDER),
                        permission = OcnAppPermission.FORWARD_MODULE_SESSIONS_SENDER,
                        expected = true)),
                Arguments.of(PermissionsMatcherTestCase(
                        request = BasicRequestType(ModuleID.SESSIONS, InterfaceRole.RECEIVER),
                        permission = OcnAppPermission.FORWARD_MODULE_SESSIONS_SENDER,
                        expected = false)),
                Arguments.of(PermissionsMatcherTestCase(
                        request = BasicRequestType(ModuleID.SESSIONS, InterfaceRole.RECEIVER),
                        permission = OcnAppPermission.FORWARD_MODULE_SESSIONS_RECEIVER,
                        expected = true)),
                Arguments.of(PermissionsMatcherTestCase(
                        request = BasicRequestType(ModuleID.SESSIONS, InterfaceRole.SENDER),
                        permission = OcnAppPermission.FORWARD_MODULE_SESSIONS_RECEIVER,
                        expected = false)),
                Arguments.of(PermissionsMatcherTestCase(
                        request = BasicRequestType(ModuleID.CDRS, InterfaceRole.SENDER),
                        permission = OcnAppPermission.FORWARD_MODULE_CDRS_SENDER,
                        expected = true)),
                Arguments.of(PermissionsMatcherTestCase(
                        request = BasicRequestType(ModuleID.CDRS, InterfaceRole.RECEIVER),
                        permission = OcnAppPermission.FORWARD_MODULE_CDRS_SENDER,
                        expected = false)),
                Arguments.of(PermissionsMatcherTestCase(
                        request = BasicRequestType(ModuleID.CDRS, InterfaceRole.RECEIVER),
                        permission = OcnAppPermission.FORWARD_MODULE_CDRS_RECEIVER,
                        expected = true)),
                Arguments.of(PermissionsMatcherTestCase(
                        request = BasicRequestType(ModuleID.CDRS, InterfaceRole.SENDER),
                        permission = OcnAppPermission.FORWARD_MODULE_CDRS_RECEIVER,
                        expected = false)),
                Arguments.of(PermissionsMatcherTestCase(
                        request = BasicRequestType(ModuleID.TARIFFS, InterfaceRole.SENDER),
                        permission = OcnAppPermission.FORWARD_MODULE_TARIFFS_SENDER,
                        expected = true)),
                Arguments.of(PermissionsMatcherTestCase(
                        request = BasicRequestType(ModuleID.TARIFFS, InterfaceRole.RECEIVER),
                        permission = OcnAppPermission.FORWARD_MODULE_TARIFFS_SENDER,
                        expected = false)),
                Arguments.of(PermissionsMatcherTestCase(
                        request = BasicRequestType(ModuleID.TARIFFS, InterfaceRole.RECEIVER),
                        permission = OcnAppPermission.FORWARD_MODULE_TARIFFS_RECEIVER,
                        expected = true)),
                Arguments.of(PermissionsMatcherTestCase(
                        request = BasicRequestType(ModuleID.TARIFFS, InterfaceRole.SENDER),
                        permission = OcnAppPermission.FORWARD_MODULE_TARIFFS_RECEIVER,
                        expected = false)),
                Arguments.of(PermissionsMatcherTestCase(
                        request = BasicRequestType(ModuleID.TOKENS, InterfaceRole.SENDER),
                        permission = OcnAppPermission.FORWARD_MODULE_TOKENS_SENDER,
                        expected = true)),
                Arguments.of(PermissionsMatcherTestCase(
                        request = BasicRequestType(ModuleID.TOKENS, InterfaceRole.RECEIVER),
                        permission = OcnAppPermission.FORWARD_MODULE_TOKENS_SENDER,
                        expected = false)),
                Arguments.of(PermissionsMatcherTestCase(
                        request = BasicRequestType(ModuleID.TOKENS, InterfaceRole.RECEIVER),
                        permission = OcnAppPermission.FORWARD_MODULE_TOKENS_RECEIVER,
                        expected = true)),
                Arguments.of(PermissionsMatcherTestCase(
                        request = BasicRequestType(ModuleID.TOKENS, InterfaceRole.SENDER),
                        permission = OcnAppPermission.FORWARD_MODULE_TOKENS_RECEIVER,
                        expected = false)),
                Arguments.of(PermissionsMatcherTestCase(
                        request = BasicRequestType(ModuleID.COMMANDS, InterfaceRole.SENDER),
                        permission = OcnAppPermission.FORWARD_MODULE_COMMANDS_SENDER,
                        expected = true)),
                Arguments.of(PermissionsMatcherTestCase(
                        request = BasicRequestType(ModuleID.COMMANDS, InterfaceRole.RECEIVER),
                        permission = OcnAppPermission.FORWARD_MODULE_COMMANDS_SENDER,
                        expected = false)),
                Arguments.of(PermissionsMatcherTestCase(
                        request = BasicRequestType(ModuleID.COMMANDS, InterfaceRole.RECEIVER),
                        permission = OcnAppPermission.FORWARD_MODULE_COMMANDS_RECEIVER,
                        expected = true)),
                Arguments.of(PermissionsMatcherTestCase(
                        request = BasicRequestType(ModuleID.COMMANDS, InterfaceRole.SENDER),
                        permission = OcnAppPermission.FORWARD_MODULE_COMMANDS_RECEIVER,
                        expected = false)),
                Arguments.of(PermissionsMatcherTestCase(
                        request = BasicRequestType(ModuleID.CHARGING_PROFILES, InterfaceRole.SENDER),
                        permission = OcnAppPermission.FORWARD_MODULE_CHARGINGPROFILES_SENDER,
                        expected = true)),
                Arguments.of(PermissionsMatcherTestCase(
                        request = BasicRequestType(ModuleID.CHARGING_PROFILES, InterfaceRole.RECEIVER),
                        permission = OcnAppPermission.FORWARD_MODULE_CHARGINGPROFILES_SENDER,
                        expected = false)),
                Arguments.of(PermissionsMatcherTestCase(
                        request = BasicRequestType(ModuleID.CHARGING_PROFILES, InterfaceRole.RECEIVER),
                        permission = OcnAppPermission.FORWARD_MODULE_CHARGINGPROFILES_RECEIVER,
                        expected = true)),
                Arguments.of(PermissionsMatcherTestCase(
                        request = BasicRequestType(ModuleID.CHARGING_PROFILES, InterfaceRole.SENDER),
                        permission = OcnAppPermission.FORWARD_MODULE_CHARGINGPROFILES_RECEIVER,
                        expected = false))
        )
    }

    @ParameterizedTest
    @MethodSource("permissionsTestSources")
    fun permissionsMatchers(testCase: PermissionsMatcherTestCase) {
        val actual = testCase.permission.matches(testCase.request)
        assertThat(actual).isEqualTo(testCase.expected)
    }

}