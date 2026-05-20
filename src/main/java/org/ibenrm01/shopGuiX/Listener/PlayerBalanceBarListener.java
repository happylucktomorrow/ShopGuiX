package org.ibenrm01.shopGuiX.Listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.ibenrm01.shopGuiX.solecraft.BalanceBarService;
import org.ibenrm01.shopGuiX.solecraft.ShopModels;
import org.ibenrm01.shopGuiX.solecraft.SolecraftClient;

public class PlayerBalanceBarListener implements Listener {
    private final JavaPlugin plugin;
    private final SolecraftClient client;
    private final BalanceBarService balanceBarService;

    public PlayerBalanceBarListener(JavaPlugin plugin, SolecraftClient client, BalanceBarService balanceBarService) {
        this.plugin = plugin;
        this.client = client;
        this.balanceBarService = balanceBarService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        balanceBarService.add(event.getPlayer());
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                ShopModels.BridgeConnectResult result = client.connect(event.getPlayer());
                if (result.blocked) {
                    plugin.getServer().getScheduler().runTask(plugin, () ->
                            event.getPlayer().kickPlayer("Your account is blocked on this server."));
                }
            } catch (Exception error) {
                plugin.getLogger().warning("Failed to register player session for " + event.getPlayer().getName() + ": " + error.getMessage());
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        balanceBarService.remove(event.getPlayer());
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                client.disconnect(event.getPlayer());
            } catch (Exception error) {
                plugin.getLogger().warning("Failed to unregister player session for " + event.getPlayer().getName() + ": " + error.getMessage());
            }
        });
    }
}
