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