package net.leafmc.soulbind;

import java.util.Locale;
import java.util.Optional;

public enum SoulbindSubcommand {
    LOCK,
    UNLOCK,
    BIND,
    INFO,
    RECOVER,
    RELOAD,
    ADMIN,
    HELP;

    public static Optional<SoulbindSubcommand> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "lock", "suo", "锁", "锁定" -> Optional.of(LOCK);
            case "unlock", "jie", "解", "解锁", "解除" -> Optional.of(UNLOCK);
            case "bind", "bang", "绑定", "灵魂绑定" -> Optional.of(BIND);
            case "info", "status", "状态", "查看" -> Optional.of(INFO);
            case "recover", "restore", "取回", "找回", "返还" -> Optional.of(RECOVER);
            case "reload", "重载" -> Optional.of(RELOAD);
            case "admin", "manage", "管理" -> Optional.of(ADMIN);
            case "help", "?", "帮助" -> Optional.of(HELP);
            default -> Optional.empty();
        };
    }
}
