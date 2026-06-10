package net.leafmc.residenceweb;

import com.bekvon.bukkit.residence.api.ResidenceApi;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.CuboidArea;
import com.bekvon.bukkit.residence.protection.ResidenceManager;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class LeafResidenceWebPlugin extends JavaPlugin implements TabExecutor {
    private static final Pattern SAFE_NAME = Pattern.compile("^[A-Za-z0-9_-]{3,32}$");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Map<String, LinkCode> linkCodes = new ConcurrentHashMap<>();
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, PendingSelection> pendingSelections = new ConcurrentHashMap<>();

    private HttpServer httpServer;
    private ExecutorService httpExecutor;
    private String publicUrl;
    private String blueMapUrl;
    private int sessionMillis;
    private int minSize;
    private int maxSize;
    private int maxResidences;
    private int recommendedSize;
    private int minY;
    private int maxY;
    private int maxSubmitDistance;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();

        if (getCommand("resweb") != null) {
            getCommand("resweb").setExecutor(this);
            getCommand("resweb").setTabCompleter(this);
        }

        if (getConfig().getBoolean("web.enabled", true)) {
            startHttpServer();
        }
    }

    @Override
    public void onDisable() {
        if (httpServer != null) {
            httpServer.stop(1);
        }
        if (httpExecutor != null) {
            httpExecutor.shutdownNow();
        }
    }

    private void loadSettings() {
        reloadConfig();
        FileConfiguration config = getConfig();
        publicUrl = config.getString("web.public-url", "http://127.0.0.1:8124");
        blueMapUrl = config.getString("integrations.bluemap-url", "auto");
        sessionMillis = Math.max(1, config.getInt("web.session-hours", 24)) * 60 * 60 * 1000;
        minSize = Math.max(1, config.getInt("residence.min-size", 3));
        maxSize = Math.max(minSize, config.getInt("residence.max-xz-size", 128));
        maxResidences = Math.max(1, config.getInt("residence.max-residences", 10));
        recommendedSize = Math.max(minSize, Math.min(maxSize, config.getInt("residence.recommended-size", 48)));
        minY = config.getInt("residence.min-y", -64);
        maxY = config.getInt("residence.max-y", 319);
        maxSubmitDistance = Math.max(0, config.getInt("residence.max-submit-distance", 192));
    }

    private void startHttpServer() {
        String bindAddress = getConfig().getString("web.bind-address", "0.0.0.0");
        int port = getConfig().getInt("web.port", 8124);
        try {
            httpServer = HttpServer.create(new InetSocketAddress(bindAddress, port), 0);
            httpServer.createContext("/", this::handleRoot);
            httpServer.createContext("/api/link", exchange -> handleApi(exchange, this::handleLink));
            httpServer.createContext("/api/state", exchange -> handleApi(exchange, this::handleState));
            httpServer.createContext("/api/submit", exchange -> handleApi(exchange, this::handleSubmit));
            httpServer.createContext("/api/cancel", exchange -> handleApi(exchange, this::handleCancel));
            httpExecutor = Executors.newCachedThreadPool();
            httpServer.setExecutor(httpExecutor);
            httpServer.start();
            getLogger().info("LeafResidenceWeb listening on " + bindAddress + ":" + port);
        } catch (IOException error) {
            getLogger().severe("Failed to start LeafResidenceWeb HTTP server: " + error.getMessage());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§2LeafResidenceWeb §7网页地址: §f" + publicUrl);
            sender.sendMessage("§2LeafResidenceWeb §7真实地图: §f" + blueMapUrl);
            sender.sendMessage("§7玩家绑定: §f/resweb link §7确认创建: §f/resweb confirm");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if ("reload".equals(sub)) {
            if (!sender.hasPermission("leafresidenceweb.admin")) {
                sender.sendMessage("§c你没有权限重载 LeafResidenceWeb。");
                return true;
            }
            loadSettings();
            sender.sendMessage("§aLeafResidenceWeb 配置已重载。");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("只有玩家可以使用这个命令。");
            return true;
        }

        if (!player.hasPermission("leafresidenceweb.use")) {
            player.sendMessage("§c你没有权限使用网页领地工具。");
            return true;
        }

        switch (sub) {
            case "link" -> {
                String code = createLinkCode(player);
                player.sendMessage("§2网页领地工具绑定码: §f" + code);
                player.sendMessage("§7打开 §f" + publicUrl + " §7输入绑定码。绑定码 10 分钟内有效。");
                return true;
            }
            case "confirm" -> {
                confirmPending(player);
                return true;
            }
            case "cancel" -> {
                pendingSelections.remove(player.getUniqueId());
                player.sendMessage("§e已取消网页领地创建请求。");
                return true;
            }
            default -> {
                player.sendMessage("§7用法: §f/resweb link §7| §f/resweb confirm §7| §f/resweb cancel");
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> values = new ArrayList<>(List.of("link", "confirm", "cancel"));
            if (sender.hasPermission("leafresidenceweb.admin")) {
                values.add("reload");
            }
            String prefix = args[0].toLowerCase(Locale.ROOT);
            values.removeIf(value -> !value.startsWith(prefix));
            return values;
        }
        return List.of();
    }

    private String createLinkCode(Player player) {
        cleanupExpired();
        String code;
        do {
            code = String.format("%06d", RANDOM.nextInt(1_000_000));
        } while (linkCodes.containsKey(code));
        linkCodes.put(code, new LinkCode(player.getUniqueId(), System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10)));
        return code;
    }

    private void confirmPending(Player player) {
        PendingSelection pending = pendingSelections.get(player.getUniqueId());
        if (pending == null) {
            player.sendMessage("§e没有待确认的网页领地请求。");
            return;
        }
        if (pending.expiresAt < System.currentTimeMillis()) {
            pendingSelections.remove(player.getUniqueId());
            player.sendMessage("§c网页领地请求已过期，请重新提交。");
            return;
        }

        Validation validation = validateSelection(player, pending);
        if (!validation.ok()) {
            pendingSelections.remove(player.getUniqueId());
            player.sendMessage("§c网页领地创建失败: " + validation.message());
            return;
        }

        Location low = new Location(pending.world, pending.minX, minY, pending.minZ);
        Location high = new Location(pending.world, pending.maxX, maxY, pending.maxZ);
        boolean created = ResidenceApi.getResidenceManager().addResidence(player, pending.name, low, high, false);
        pendingSelections.remove(player.getUniqueId());
        if (created) {
            player.sendMessage("§a已通过网页创建领地 §f" + pending.name + "§a。");
        } else {
            player.sendMessage("§cResidence 拒绝创建领地，请检查名称、数量、重叠或权限限制。");
        }
    }

    private Validation validateSelection(Player player, PendingSelection selection) {
        if (!SAFE_NAME.matcher(selection.name).matches()) {
            return Validation.fail("领地名只能使用 3-32 位英文、数字、下划线或横线。");
        }
        if (selection.world == null) {
            return Validation.fail("世界不存在。");
        }
        if (!player.getWorld().equals(selection.world)) {
            return Validation.fail("只能在自己当前所在世界创建领地。");
        }

        int width = selection.maxX - selection.minX + 1;
        int depth = selection.maxZ - selection.minZ + 1;
        if (width < minSize || depth < minSize) {
            return Validation.fail("X/Z 尺寸不能小于 " + minSize + " 格。");
        }
        if (width > maxSize || depth > maxSize) {
            return Validation.fail("普通玩家 X/Z 单边上限是 " + maxSize + " 格。");
        }
        if (selection.maxX < selection.minX || selection.maxZ < selection.minZ) {
            return Validation.fail("选区坐标不正确。");
        }

        Location playerLocation = player.getLocation();
        double centerX = (selection.minX + selection.maxX) / 2.0D;
        double centerZ = (selection.minZ + selection.maxZ) / 2.0D;
        if (maxSubmitDistance > 0) {
            double distance = Math.hypot(centerX - playerLocation.getX(), centerZ - playerLocation.getZ());
            if (distance > maxSubmitDistance) {
                return Validation.fail("选区中心离你太远，当前限制 " + maxSubmitDistance + " 格内。");
            }
        }

        ResidenceManager manager = residenceManager();
        if (manager.getOwnedZoneCount(player.getUniqueId()) >= maxResidences) {
            return Validation.fail("你已经达到 " + maxResidences + " 个领地上限。");
        }
        if (manager.getByName(selection.name) != null) {
            return Validation.fail("这个领地名已经被使用。");
        }

        CuboidArea area = new CuboidArea(
            new Location(selection.world, selection.minX, minY, selection.minZ),
            new Location(selection.world, selection.maxX, maxY, selection.maxZ)
        );
        ClaimedResidence collision = manager.collidesWithResidence(area);
        if (collision != null) {
            return Validation.fail("选区和已有领地 " + collision.getName() + " 重叠。");
        }
        return Validation.success();
    }

    private ResidenceManager residenceManager() {
        return (ResidenceManager) ResidenceApi.getResidenceManager();
    }

    private void handleRoot(HttpExchange exchange) throws IOException {
        if (handleOptions(exchange)) {
            return;
        }
        String path = exchange.getRequestURI().getPath();
        if (!"/".equals(path) && !"/index.html".equals(path)) {
            sendBytes(exchange, 404, "text/plain; charset=utf-8", "Not found".getBytes(StandardCharsets.UTF_8));
            return;
        }
        byte[] html = readResource("web/index.html");
        sendBytes(exchange, 200, "text/html; charset=utf-8", html);
    }

    private byte[] readResource(String path) throws IOException {
        try (InputStream input = getResource(path)) {
            if (input == null) {
                return "LeafResidenceWeb resource missing".getBytes(StandardCharsets.UTF_8);
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            input.transferTo(output);
            return output.toByteArray();
        }
    }

    private void handleApi(HttpExchange exchange, ApiHandler handler) throws IOException {
        if (handleOptions(exchange)) {
            return;
        }
        try {
            Map<String, String> params = parseParams(exchange);
            Object result = handler.handle(exchange, params);
            sendJson(exchange, 200, result);
        } catch (ApiException error) {
            sendJson(exchange, error.status, Map.of("ok", false, "error", error.getMessage()));
        } catch (Exception error) {
            getLogger().warning("API error: " + error.getMessage());
            sendJson(exchange, 500, Map.of("ok", false, "error", "服务器内部错误"));
        }
    }

    private Object handleLink(HttpExchange exchange, Map<String, String> params) throws Exception {
        requireMethod(exchange, "POST");
        String code = params.getOrDefault("code", "").trim();
        return syncCall(() -> {
            cleanupExpired();
            LinkCode linkCode = linkCodes.remove(code);
            if (linkCode == null) {
                throw new ApiException(400, "绑定码无效或已过期。");
            }
            Player player = Bukkit.getPlayer(linkCode.playerId);
            if (player == null) {
                throw new ApiException(400, "玩家不在线，请进服后重新生成绑定码。");
            }
            String token = randomToken();
            long expiresAt = System.currentTimeMillis() + sessionMillis;
            sessions.put(token, new Session(player.getUniqueId(), expiresAt));
            return Map.of(
                "ok", true,
                "token", token,
                "expiresAt", expiresAt,
                "player", playerPayload(player)
            );
        });
    }

    private Object handleState(HttpExchange exchange, Map<String, String> params) throws Exception {
        requireMethod(exchange, "GET");
        String token = params.getOrDefault("token", "");
        return syncCall(() -> {
            Player player = playerForToken(token);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("ok", true);
            response.put("serverTime", Instant.now().toString());
            response.put("publicUrl", publicUrl);
            response.put("blueMapUrl", blueMapUrl);
            response.put("player", playerPayload(player));
            response.put("limits", limitsPayload());
            response.put("onlinePlayers", onlinePlayersPayload());
            response.put("claims", claimsPayload());
            PendingSelection pending = pendingSelections.get(player.getUniqueId());
            response.put("pending", pending == null ? null : pendingPayload(pending));
            return response;
        });
    }

    private Object handleSubmit(HttpExchange exchange, Map<String, String> params) throws Exception {
        requireMethod(exchange, "POST");
        String token = params.getOrDefault("token", "");
        return syncCall(() -> {
            Player player = playerForToken(token);
            World world = Bukkit.getWorld(params.getOrDefault("world", player.getWorld().getName()));
            PendingSelection selection = new PendingSelection(
                player.getUniqueId(),
                sanitizeName(params.getOrDefault("name", "")),
                world,
                parseInt(params, "minX"),
                parseInt(params, "maxX"),
                parseInt(params, "minZ"),
                parseInt(params, "maxZ"),
                System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5)
            );
            Validation validation = validateSelection(player, selection);
            if (!validation.ok()) {
                throw new ApiException(400, validation.message());
            }
            pendingSelections.put(player.getUniqueId(), selection);
            player.sendMessage("§2网页领地请求: §f" + selection.name + " §7(" + selection.sizeText() + ")");
            player.sendMessage("§7确认创建请输入 §f/resweb confirm §7，取消请输入 §f/resweb cancel§7。请求 5 分钟内有效。");
            return Map.of(
                "ok", true,
                "message", "已提交到游戏内，请在聊天框输入 /resweb confirm 确认创建。",
                "pending", pendingPayload(selection)
            );
        });
    }

    private Object handleCancel(HttpExchange exchange, Map<String, String> params) throws Exception {
        requireMethod(exchange, "POST");
        String token = params.getOrDefault("token", "");
        return syncCall(() -> {
            Player player = playerForToken(token);
            pendingSelections.remove(player.getUniqueId());
            return Map.of("ok", true);
        });
    }

    private Player playerForToken(String token) {
        cleanupExpired();
        Session session = sessions.get(token);
        if (session == null) {
            throw new ApiException(401, "登录已失效，请重新 /resweb link。");
        }
        Player player = Bukkit.getPlayer(session.playerId);
        if (player == null) {
            throw new ApiException(401, "玩家不在线，请进服后重新打开网页。");
        }
        return player;
    }

    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        linkCodes.entrySet().removeIf(entry -> entry.getValue().expiresAt < now);
        sessions.entrySet().removeIf(entry -> entry.getValue().expiresAt < now);
        pendingSelections.entrySet().removeIf(entry -> entry.getValue().expiresAt < now);
    }

    private <T> T syncCall(Callable<T> callable) throws Exception {
        if (Bukkit.isPrimaryThread()) {
            return callable.call();
        }
        Future<T> future = Bukkit.getScheduler().callSyncMethod(this, callable);
        try {
            return future.get(4, TimeUnit.SECONDS);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw error;
        } catch (ExecutionException error) {
            Throwable cause = error.getCause();
            if (cause instanceof Exception exception) {
                throw exception;
            }
            throw new RuntimeException(cause);
        } catch (TimeoutException error) {
            throw new ApiException(503, "服务器主线程繁忙，请稍后再试。");
        }
    }

    private Map<String, Object> playerPayload(Player player) {
        Location location = player.getLocation();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("uuid", player.getUniqueId().toString());
        map.put("name", player.getName());
        map.put("world", player.getWorld().getName());
        map.put("x", floor(location.getX()));
        map.put("y", floor(location.getY()));
        map.put("z", floor(location.getZ()));
        map.put("residenceCount", residenceManager().getOwnedZoneCount(player.getUniqueId()));
        return map;
    }

    private List<Object> onlinePlayersPayload() {
        List<Object> players = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            players.add(playerPayload(player));
        }
        return players;
    }

    private Map<String, Object> limitsPayload() {
        Map<String, Object> limits = new LinkedHashMap<>();
        limits.put("minSize", minSize);
        limits.put("maxSize", maxSize);
        limits.put("maxResidences", maxResidences);
        limits.put("recommendedSize", recommendedSize);
        limits.put("minY", minY);
        limits.put("maxY", maxY);
        limits.put("maxSubmitDistance", maxSubmitDistance);
        return limits;
    }

    private List<Object> claimsPayload() {
        List<Object> claims = new ArrayList<>();
        Collection<ClaimedResidence> residences = residenceManager().getResidences().values();
        for (ClaimedResidence residence : residences) {
            Map<String, Object> claim = new LinkedHashMap<>();
            claim.put("name", residence.getName());
            claim.put("owner", residence.getOwner());
            UUID ownerId = residence.getOwnerUUID();
            claim.put("ownerUuid", ownerId == null ? "" : ownerId.toString());
            List<Object> areas = new ArrayList<>();
            for (CuboidArea area : residence.getAreaArray()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("world", area.getWorldName());
                item.put("minX", floor(area.getLowVector().getX()));
                item.put("minY", floor(area.getLowVector().getY()));
                item.put("minZ", floor(area.getLowVector().getZ()));
                item.put("maxX", floor(area.getHighVector().getX()));
                item.put("maxY", floor(area.getHighVector().getY()));
                item.put("maxZ", floor(area.getHighVector().getZ()));
                areas.add(item);
            }
            claim.put("areas", areas);
            claims.add(claim);
        }
        return claims;
    }

    private Map<String, Object> pendingPayload(PendingSelection selection) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", selection.name);
        map.put("world", selection.world.getName());
        map.put("minX", selection.minX);
        map.put("maxX", selection.maxX);
        map.put("minZ", selection.minZ);
        map.put("maxZ", selection.maxZ);
        map.put("expiresAt", selection.expiresAt);
        return map;
    }

    private int parseInt(Map<String, String> params, String key) {
        try {
            return Integer.parseInt(params.getOrDefault(key, ""));
        } catch (NumberFormatException error) {
            throw new ApiException(400, "参数 " + key + " 不是有效数字。");
        }
    }

    private String sanitizeName(String name) {
        return name == null ? "" : name.trim();
    }

    private String randomToken() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }

    private void requireMethod(HttpExchange exchange, String method) {
        if (!method.equalsIgnoreCase(exchange.getRequestMethod())) {
            throw new ApiException(405, "Method not allowed");
        }
    }

    private boolean handleOptions(HttpExchange exchange) throws IOException {
        addCors(exchange.getResponseHeaders());
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }

    private Map<String, String> parseParams(HttpExchange exchange) throws IOException {
        Map<String, String> params = new HashMap<>();
        parseParamString(exchange.getRequestURI().getRawQuery(), params);
        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            parseParamString(body, params);
        }
        return params;
    }

    private void parseParamString(String text, Map<String, String> params) {
        if (text == null || text.isBlank()) {
            return;
        }
        for (String pair : text.split("&")) {
            int index = pair.indexOf('=');
            String key = index >= 0 ? pair.substring(0, index) : pair;
            String value = index >= 0 ? pair.substring(index + 1) : "";
            params.put(urlDecode(key), urlDecode(value));
        }
    }

    private String urlDecode(String text) {
        return URLDecoder.decode(text.replace("+", "%2B"), StandardCharsets.UTF_8);
    }

    private void sendJson(HttpExchange exchange, int status, Object payload) throws IOException {
        sendBytes(exchange, status, "application/json; charset=utf-8", toJson(payload).getBytes(StandardCharsets.UTF_8));
    }

    private void sendBytes(HttpExchange exchange, int status, String contentType, byte[] bytes) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        addCors(headers);
        headers.set("Content-Type", contentType);
        headers.set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private void addCors(Headers headers) {
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type");
    }

    private String toJson(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String text) {
            return "\"" + escapeJson(text) + "\"";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof Map<?, ?> map) {
            StringBuilder builder = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                builder.append(toJson(String.valueOf(entry.getKey()))).append(':').append(toJson(entry.getValue()));
            }
            return builder.append('}').toString();
        }
        if (value instanceof Iterable<?> items) {
            StringBuilder builder = new StringBuilder("[");
            boolean first = true;
            for (Object item : items) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                builder.append(toJson(item));
            }
            return builder.append(']').toString();
        }
        return toJson(String.valueOf(value));
    }

    private String escapeJson(String text) {
        StringBuilder builder = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (c < 0x20) {
                        builder.append(String.format("\\u%04x", (int) c));
                    } else {
                        builder.append(c);
                    }
                }
            }
        }
        return builder.toString();
    }

    private int floor(double value) {
        return (int) Math.floor(value);
    }

    private record LinkCode(UUID playerId, long expiresAt) {}

    private record Session(UUID playerId, long expiresAt) {}

    private record PendingSelection(
        UUID playerId,
        String name,
        World world,
        int minX,
        int maxX,
        int minZ,
        int maxZ,
        long expiresAt
    ) {
        String sizeText() {
            return (maxX - minX + 1) + "x" + (maxZ - minZ + 1);
        }
    }

    private record Validation(boolean ok, String message) {
        static Validation success() {
            return new Validation(true, "");
        }

        static Validation fail(String message) {
            return new Validation(false, message);
        }
    }

    private static final class ApiException extends RuntimeException {
        private final int status;

        private ApiException(int status, String message) {
            super(message);
            this.status = status;
        }
    }

    @FunctionalInterface
    private interface ApiHandler {
        Object handle(HttpExchange exchange, Map<String, String> params) throws Exception;
    }
}
