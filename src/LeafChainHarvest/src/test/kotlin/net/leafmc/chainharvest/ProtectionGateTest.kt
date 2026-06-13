package net.leafmc.chainharvest

object ProtectionGateTest {
    @JvmStatic
    fun main(args: Array<String>) {
        missingAdapterFailsClosedByDefault()
        unsafeFallbackAllowsWhenExplicit()
        nonRequiredResidenceAllowsFallback()
    }

    private fun missingAdapterFailsClosedByDefault() {
        val decision = ProtectionPolicy.fallbackDecision(
            ProtectionSettings(requireResidenceForFarmActions = true, unsafeFallbackWithoutResidence = false),
        )
        check(decision == ProtectionDecision.DENY) { "missing adapter should deny farm fan-out by default" }
    }

    private fun unsafeFallbackAllowsWhenExplicit() {
        val decision = ProtectionPolicy.fallbackDecision(
            ProtectionSettings(requireResidenceForFarmActions = true, unsafeFallbackWithoutResidence = true),
        )
        check(decision == ProtectionDecision.ALLOW) { "unsafe fallback is explicit" }
    }

    private fun nonRequiredResidenceAllowsFallback() {
        val decision = ProtectionPolicy.fallbackDecision(
            ProtectionSettings(requireResidenceForFarmActions = false, unsafeFallbackWithoutResidence = false),
        )
        check(decision == ProtectionDecision.ALLOW) { "Residence can be disabled intentionally" }
    }
}
