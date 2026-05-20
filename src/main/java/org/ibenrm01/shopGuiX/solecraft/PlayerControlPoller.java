package org.ibenrm01.shopGuiX.solecraft;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class PlayerControlPoller {
    private final JavaPlugin plugin;
    private final SolecraftClient client;
    private BukkitTask task;

    public PlayerControlPoller(JavaPlugin plugin, SolecraftClient client) {
        this.plugin = plugin;
        this.client = client;
    }

    public void start() {
        stop();
        task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::poll, 40L, 40L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void poll() {
        try {
            ShopModels.ControlActionList list = client.controlActions();
            if (list == null || list.actions == null) {
                return;
            }
            for (ShopModels.ControlAction action : list.actions) {
                handle(action);
            }
        } catch (Exception error) {
            plugin.getLogger().warning("Failed to poll Solecraft player controls: " + error.getMessage());
        }
    }

    private void handle(ShopModels.ControlAction action) {
        if (action == null || action.id == null) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = findPlayer(action);
            if (player != null) {
                if ("force_logout".equals(action.type)) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "authme logout " + player.getName());
                    player.kickPlayer("You were logged out by an administrator.");
                } else if ("block".equals(action.type)) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "authme logout " + player.getName());
                    player.kickPlayer("Your account is blocked on this server.");
                }
            }
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    client.completeControlAction(action.id);
                } catch (Exception error) {
                    plugin.getLogger().warning("Failed to complete Solecraft player control " + action.id + ": " + error.getMessage());
                }
            });
        });
    }

    private Player findPlayer(ShopModels.ControlAction action) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getUniqueId().toString().equalsIgnoreCase(action.playerUuid)
                    || player.getName().equalsIgnoreCase(action.playerName)) {
                return player;
            }
        }
        return null;
    }
}
