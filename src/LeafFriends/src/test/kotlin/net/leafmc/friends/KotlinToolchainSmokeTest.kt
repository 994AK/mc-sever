package net.leafmc.friends

object KotlinToolchainSmokeTest {
    @JvmStatic
    fun main(args: Array<String>) {
        check(pluginLabel(" LeafFriends ") == "LeafFriends")
    }

    private fun pluginLabel(name: String): String = name.trim()
}
