package net.leafmc.soulbind;

public final class SoulbindSubcommandTest {
    public static void main(String[] args) {
        parsesEnglishCommands();
        parsesChineseCommands();
        rejectsUnknownCommands();
    }

    private static void parsesEnglishCommands() {
        check(SoulbindSubcommand.parse("lock").orElseThrow() == SoulbindSubcommand.LOCK, "lock parses");
        check(SoulbindSubcommand.parse("unlock").orElseThrow() == SoulbindSubcommand.UNLOCK, "unlock parses");
        check(SoulbindSubcommand.parse("bind").orElseThrow() == SoulbindSubcommand.BIND, "bind parses");
        check(SoulbindSubcommand.parse("recover").orElseThrow() == SoulbindSubcommand.RECOVER, "recover parses");
    }

    private static void parsesChineseCommands() {
        check(SoulbindSubcommand.parse("锁").orElseThrow() == SoulbindSubcommand.LOCK, "Chinese lock parses");
        check(SoulbindSubcommand.parse("解锁").orElseThrow() == SoulbindSubcommand.UNLOCK, "Chinese unlock parses");
        check(SoulbindSubcommand.parse("灵魂绑定").orElseThrow() == SoulbindSubcommand.BIND, "Chinese bind parses");
        check(SoulbindSubcommand.parse("找回").orElseThrow() == SoulbindSubcommand.RECOVER, "Chinese recover parses");
    }

    private static void rejectsUnknownCommands() {
        check(SoulbindSubcommand.parse("unknown").isEmpty(), "unknown command rejected");
        check(SoulbindSubcommand.parse("").isEmpty(), "blank command rejected");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
