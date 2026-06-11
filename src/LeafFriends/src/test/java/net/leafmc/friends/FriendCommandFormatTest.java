package net.leafmc.friends;

public final class FriendCommandFormatTest {
    public static void main(String[] args) {
        parsesSettingKeys();
        parsesToggleValues();
    }

    private static void parsesSettingKeys() {
        TestSupport.check(FriendService.SettingKey.parse("requests").orElseThrow() == FriendService.SettingKey.REQUESTS, "requests key parses");
        TestSupport.check(FriendService.SettingKey.parse("tp").orElseThrow() == FriendService.SettingKey.TELEPORTS, "tp alias parses");
        TestSupport.check(FriendService.SettingKey.parse("msg").orElseThrow() == FriendService.SettingKey.MESSAGES, "msg alias parses");
        TestSupport.check(FriendService.SettingKey.parse("notify").orElseThrow() == FriendService.SettingKey.NOTIFICATIONS, "notify alias parses");
        TestSupport.check(FriendService.SettingKey.parse("online").orElseThrow() == FriendService.SettingKey.STATUS, "online alias parses");
        TestSupport.check(FriendService.SettingKey.parse("bad").isEmpty(), "bad key rejected");
    }

    private static void parsesToggleValues() {
        TestSupport.check(FriendCommand.parseToggleValue("on").orElseThrow(), "on parses true");
        TestSupport.check(FriendCommand.parseToggleValue("开启").orElseThrow(), "Chinese on parses true");
        TestSupport.check(!FriendCommand.parseToggleValue("off").orElseThrow(), "off parses false");
        TestSupport.check(!FriendCommand.parseToggleValue("关闭").orElseThrow(), "Chinese off parses false");
        TestSupport.check(FriendCommand.parseToggleValue("unknown").isEmpty(), "unknown toggle rejected");
    }
}
