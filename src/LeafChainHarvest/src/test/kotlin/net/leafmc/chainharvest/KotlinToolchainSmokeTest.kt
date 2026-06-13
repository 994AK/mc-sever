package net.leafmc.chainharvest

object KotlinToolchainSmokeTest {
    @JvmStatic
    fun main(args: Array<String>) {
        check(" LeafChainHarvest ".trim() == "LeafChainHarvest") { "Kotlin toolchain should run main tests" }
    }
}
