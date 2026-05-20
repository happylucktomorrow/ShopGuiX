package org.ibenrm01.shopGuiX.Listener.sell;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.ibenrm01.shopGuiX.ShopGuiX;
import org.ibenrm01.shopGuiX.Utility;
import org.ibenrm01.shopGuiX.YAMLConfig.Lang;
import org.ibenrm01.shopGuiX.YAMLConfig.Settings;
import org.ibenrm01.shopGuiX.player.sell.PlayerSell;
import org.ibenrm01.shopGuiX.solecraft.ShopModels;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class InventoryEventSell implements Listener {

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        Player player = (Player) e.getPlayer();
        PlayerSell ps = PlayerSell.get(player.getUniqueId());
        if (ps == null || !ps.getActiveGUI()) return;
        Inventory sellInventory = e.getInventory();
        Map<ShopModels.Item, Integer> sellable = new LinkedHashMap<>();
        int totalPrice = 0;
        for (ItemStack item : sellInventory.getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;
            String itemType = item.getType().name();
            ShopModels.Item shopItem = ShopGuiX.getInstance().getSolecraftClient().findItemByMaterial(itemType);
            if (shopItem != null && shopItem.sellPrice > 0) {
                int amount = item.getAmount();
                sellable.merge(shopItem, amount, Integer::sum);
                totalPrice += shopItem.sellPrice * amount;
            } else {
                HashMap<Integer, ItemStack> notAdded = player.getInventory().addItem(item);
                for (ItemStack leftover : notAdded.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
            }
        }

        if (totalPrice > 0) {
            for (Map.Entry<ShopModels.Item, Integer> entry : sellable.entrySet()) {
                ShopModels.ShopResult result = sell(player, entry.getKey().id, entry.getValue());
                if (!result.ok) {
                    player.sendMessage(Utility.getInstance().setColor(Settings.getInstance().getConfig().getString("serverName")) + " " + Utility.getInstance().setColor(Lang.getInstance().getConfig().getString("add.onlyPlayer")));
                    player.sendMessage(org.bukkit.ChatColor.RED + result.message);
                    PlayerSell.remove(player.getUniqueId());
                    return;
                }
            }
            sellInventory.clear();
            if (ShopGuiX.getInstance().getBalanceBarService() != null) {
                ShopGuiX.getInstance().getBalanceBarService().refresh(player);
            }
            if (sellable.isEmpty()) {
                player.sendMessage(Utility.getInstance().setColor(Settings.getInstance().getConfig().getString("serverName")) + " " + Utility.getInstance().setColor(Lang.getInstance().getConfig().getString("sell.no-sell-batch")));
                PlayerSell.remove(player.getUniqueId());
                return;
            }
            Map<String, String> placeholderss = new HashMap<>();
            placeholderss.put("price", String.valueOf(totalPrice));
            player.sendMessage(Utility.getInstance().setColor(Settings.getInstance().getConfig().getString("serverName")) + " " + Utility.getInstance().setColor(Utility.getInstance().replace(Lang.getInstance().getConfig().getString("sell.success-batch"), placeholderss)));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        } else {
            player.sendMessage(Utility.getInstance().setColor(Settings.getInstance().getConfig().getString("serverName")) + " " + Utility.getInstance().setColor(Lang.getInstance().getConfig().getString("sell.no-sell-batch")));
        }
        PlayerSell.remove(player.getUniqueId());
    }

    private ShopModels.ShopResult sell(Player player, String itemId, int amount) {
        try {
            return ShopGuiX.getInstance().getSolecraftClient().sell(player, itemId, amount);
        } catch (Exception error) {
            ShopModels.ShopResult result = new ShopModels.ShopResult();
            result.ok = false;
            result.message = "Shop API error: " + error.getMessage();
            return result;
        }
    }
}
