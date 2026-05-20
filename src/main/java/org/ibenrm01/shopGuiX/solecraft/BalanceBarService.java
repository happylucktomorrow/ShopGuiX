package org.ibenrm01.shopGuiX.solecraft;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.ibenrm01.shopGuiX.YAMLConfig.Settings;

public class BalanceBarService {
    private final JavaPlugin plugin;
    private final SolecraftClient client;
    private final Map<UUID, BossBar> bars = new HashMap<>();
    private BukkitTask task;

    public BalanceBarService(JavaPlugin plugin, SolecraftClient client) {
        this.plugin = plugin;
        this.client = client;
    }

    public void start() {
        if (!Settings.getInstance().getConfig().getBoolean("solecraft.balanceBar.enabled", true)) {
            return;
        }
        long refreshTicks = Math.max(40L, Settings.getInstance().getConfig().getLong("solecraft.balanceBar.refreshTicks", 100L));
        task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                refresh(player);
            }
        }, 40L, refreshTicks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
        for (BossBar bar : bars.values()) {
            bar.removeAll();
        }
        bars.clear();
    }

    public void add(Player player) {
        if (!Settings.getInstance().getConfig().getBoolean("solecraft.balanceBar.enabled", true)) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> barFor(player).addPlayer(player));
        refresh(player);
    }

    public void remove(Player player) {
        BossBar bar = bars.remove(player.getUniqueId());
        if (bar != null) {
            bar.removeAll();
        }
    }

    public void refresh(Player player) {
        if (!plugin.isEnabled() || player == null || !player.isOnline()) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!plugin.isEnabled() || !player.isOnline()) {
                return;
            }
            String title;
            try {
                ShopModels.Balance balance = client.balance(player);
                title = renderTitle(balance.balance);
            } catch (Exception error) {
                plugin.getLogger().warning("Failed to refresh balance for " + player.getName() + ": " + error.getMessage());
                title = ChatColor.RED + "Money: offline";
            }
            String finalTitle = title;
            if (!plugin.isEnabled()) {
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    BossBar bar = barFor(player);
                    bar.setTitle(finalTitle);
                    if (!bar.getPlayers().contains(player)) {
                        bar.addPlayer(player);
                    }
                }
            });
        });
    }

    private BossBar barFor(Player player) {
        return bars.computeIfAbsent(player.getUniqueId(), ignored -> {
            BossBar bar = Bukkit.createBossBar(ChatColor.GREEN + "Money: ...", BarColor.GREEN, BarStyle.SOLID);
            bar.setProgress(1.0);
            return bar;
        });
    }

    private String renderTitle(int balance) {
        String template = Settings.getInstance().getConfig().getString("solecraft.balanceBar.title", "&aMoney: &f{balance}");
        return ChatColor.translateAlternateColorCodes('&', template.replace("{balance}", String.valueOf(balance)));
    }
}
