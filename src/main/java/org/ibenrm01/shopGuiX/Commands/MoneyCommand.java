package org.ibenrm01.shopGuiX.Commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.ibenrm01.shopGuiX.solecraft.BalanceBarService;
import org.ibenrm01.shopGuiX.solecraft.ShopModels;
import org.ibenrm01.shopGuiX.solecraft.SolecraftClient;
import org.jetbrains.annotations.NotNull;

public class MoneyCommand implements CommandExecutor {
    private final SolecraftClient client;
    private final BalanceBarService balanceBarService;

    public MoneyCommand(SolecraftClient client, BalanceBarService balanceBarService) {
        this.client = client;
        this.balanceBarService = balanceBarService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("money")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Only players can use /money.");
                return true;
            }
            Bukkit.getScheduler().runTaskAsynchronously(org.ibenrm01.shopGuiX.ShopGuiX.getInstance(), () -> {
                try {
                    ShopModels.Balance balance = client.balance(player);
                    player.sendMessage(ChatColor.GREEN + "Money: " + ChatColor.WHITE + balance.balance);
                    balanceBarService.refresh(player);
                } catch (Exception error) {
                    player.sendMessage(ChatColor.RED + "Could not read money: " + error.getMessage());
                }
            });
            return true;
        }

        if (!sender.hasPermission("shop.money.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }
        if (args.length != 3 || (!args[0].equalsIgnoreCase("add") && !args[0].equalsIgnoreCase("set"))) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /scmoney <add|set> <onlinePlayer> <amount>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player must be online: " + args[1]);
            return true;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException error) {
            sender.sendMessage(ChatColor.RED + "Amount must be a number.");
            return true;
        }

        Bukkit.getScheduler().runTaskAsynchronously(org.ibenrm01.shopGuiX.ShopGuiX.getInstance(), () -> {
            try {
                int delta = amount;
                if (args[0].equalsIgnoreCase("set")) {
                    ShopModels.Balance current = client.balance(target);
                    delta = amount - current.balance;
                }
                ShopModels.Balance balance = client.adjustBalance(target.getUniqueId(), target.getName(), delta, "in-game admin command");
                sender.sendMessage(ChatColor.GREEN + "Money for " + target.getName() + " is now " + balance.balance);
                target.sendMessage(ChatColor.GREEN + "Money: " + ChatColor.WHITE + balance.balance);
                balanceBarService.refresh(target);
            } catch (Exception error) {
                sender.sendMessage(ChatColor.RED + "Could not update money: " + error.getMessage());
            }
        });
        return true;
    }
}
