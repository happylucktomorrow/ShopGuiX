package org.ibenrm01.shopGuiX.solecraft;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class SolecraftClient {
    private static final Gson GSON = new Gson();

    private final JavaPlugin plugin;
    private final HttpClient http;
    private final String apiUrl;
    private final String serverId;
    private final String bridgeToken;

    private volatile ShopModels.Catalog cachedCatalog;

    private SolecraftClient(JavaPlugin plugin, String apiUrl, String serverId, String bridgeToken) {
        this.plugin = plugin;
        this.apiUrl = trimTrailingSlash(apiUrl);
        this.serverId = serverId;
        this.bridgeToken = bridgeToken;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    public static SolecraftClient fromSettings(JavaPlugin plugin, FileConfiguration config) {
        String apiUrl = config.getString("solecraft.apiUrl", "http://127.0.0.1:4102");
        String serverId = config.getString("solecraft.serverId", "sv-lobby-transfer");
        String bridgeToken = config.getString("solecraft.bridgeToken", "");
        return new SolecraftClient(plugin, apiUrl, serverId, bridgeToken == null ? "" : bridgeToken.trim());
    }

    public String serverId() {
        return serverId;
    }

    public ShopModels.Catalog catalog() throws IOException, InterruptedException {
        ShopModels.Catalog catalog = get("/api/shop/catalog", ShopModels.Catalog.class);
        if (catalog.categories == null) {
            catalog.categories = new java.util.ArrayList<>();
        }
        if (catalog.items == null) {
            catalog.items = new java.util.ArrayList<>();
        }
        cachedCatalog = catalog;
        return catalog;
    }

    public ShopModels.Catalog cachedCatalog() {
        ShopModels.Catalog existing = cachedCatalog;
        if (existing != null) {
            return existing;
        }
        try {
            return catalog();
        } catch (Exception error) {
            plugin.getLogger().warning("Failed to load Solecraft shop catalog: " + error.getMessage());
            return new ShopModels.Catalog();
        }
    }

    public ShopModels.Balance balance(Player player) throws IOException, InterruptedException {
        String path = "/api/shop/players/" + encode(player.getUniqueId().toString()) + "/balance?playerName=" + encode(player.getName());
        return get(path, ShopModels.Balance.class);
    }

    public ShopModels.AdminState adminState(Player player) throws IOException, InterruptedException {
        String path = "/api/shop/players/" + encode(player.getUniqueId().toString()) + "/admin-state?playerName=" + encode(player.getName());
        return get(path, ShopModels.AdminState.class);
    }

    public ShopModels.BridgeConnectResult connect(Player player) throws IOException, InterruptedException {
        Map<String, Object> body = new HashMap<>();
        body.put("playerUuid", player.getUniqueId().toString());
        body.put("playerName", player.getName());
        return post("/api/bridge/players/connect", body, ShopModels.BridgeConnectResult.class);
    }

    public void disconnect(Player player) throws IOException, InterruptedException {
        Map<String, Object> body = new HashMap<>();
        body.put("playerUuid", player.getUniqueId().toString());
        body.put("playerName", player.getName());
        post("/api/bridge/players/disconnect", body, JsonObject.class);
    }

    public ShopModels.ControlActionList controlActions() throws IOException, InterruptedException {
        return get("/api/bridge/actions", ShopModels.ControlActionList.class);
    }

    public void completeControlAction(String actionId) throws IOException, InterruptedException {
        post("/api/bridge/actions/" + encode(actionId) + "/complete", new HashMap<>(), JsonObject.class);
    }

    public ShopModels.ShopResult buy(Player player, String itemId, int quantity) throws IOException, InterruptedException {
        Map<String, Object> body = playerItemBody(player, itemId, quantity);
        return post("/api/shop/purchase", body, ShopModels.ShopResult.class);
    }

    public ShopModels.ShopResult sell(Player player, String itemId, int quantity) throws IOException, InterruptedException {
        Map<String, Object> body = playerItemBody(player, itemId, quantity);
        return post("/api/shop/sell", body, ShopModels.ShopResult.class);
    }

    public ShopModels.Balance adjustBalance(UUID playerUuid, String playerName, int amount, String reason) throws IOException, InterruptedException {
        Map<String, Object> body = new HashMap<>();
        body.put("playerName", playerName);
        body.put("amount", amount);
        body.put("reason", reason);
        JsonObject result = post("/api/shop/bridge/players/" + encode(playerUuid.toString()) + "/balance/adjust", body, JsonObject.class);
        return GSON.fromJson(result.getAsJsonObject("balance"), ShopModels.Balance.class);
    }

    public ShopModels.Item findItemByMaterial(String material) {
        String normalized = material == null ? "" : material.toUpperCase();
        for (ShopModels.Item item : cachedCatalog().items) {
            if (item.enabled && normalized.equalsIgnoreCase(item.material)) {
                return item;
            }
        }
        return null;
    }

    public ShopModels.Item findItem(String itemId) {
        if (itemId == null) {
            return null;
        }
        for (ShopModels.Item item : cachedCatalog().items) {
            if (itemId.equals(item.id)) {
                return item;
            }
        }
        return null;
    }

    private Map<String, Object> playerItemBody(Player player, String itemId, int quantity) {
        Map<String, Object> body = new HashMap<>();
        body.put("playerUuid", player.getUniqueId().toString());
        body.put("playerName", player.getName());
        body.put("serverId", serverId);
        body.put("itemId", itemId);
        body.put("quantity", quantity);
        return body;
    }

    private <T> T get(String path, Class<T> type) throws IOException, InterruptedException {
        HttpRequest request = baseRequest(path).GET().build();
        return send(request, type);
    }

    private <T> T post(String path, Object body, Class<T> type) throws IOException, InterruptedException {
        HttpRequest request = baseRequest(path)
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
                .build();
        return send(request, type);
    }

    private HttpRequest.Builder baseRequest(String path) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(apiUrl + path))
                .timeout(Duration.ofSeconds(5))
                .version(HttpClient.Version.HTTP_1_1)
                .header("x-solecraft-bridge-token", bridgeToken);
        return builder;
    }

    private <T> T send(HttpRequest request, Class<T> type) throws IOException, InterruptedException {
        if (bridgeToken.isEmpty()) {
            throw new IOException("solecraft.bridgeToken is empty");
        }
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Solecraft API returned " + response.statusCode() + ": " + response.body());
        }
        return GSON.fromJson(response.body(), type);
    }

    private static String trimTrailingSlash(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "http://127.0.0.1:4102";
        }
        return value.trim().replaceAll("/+$", "");
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
