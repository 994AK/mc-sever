package net.leafmc.chainharvest

object ChainCommandFormatTest {
    @JvmStatic
    fun main(args: Array<String>) {
        helpMentionsMenuAndReload()
    }

    private fun helpMentionsMenuAndReload() {
        val lines = ChainCommandFormat.helpLines("[x] ")
        check(lines.any { it.contains("/leafchain menu") }) { "help should mention menu" }
        check(lines.any { it.contains("/leafchain reload") }) { "help should mention reload" }
    }
}
