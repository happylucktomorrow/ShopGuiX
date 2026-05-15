package org.ibenrm01.shopGuiX.Commands;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.ibenrm01.shopGuiX.Inventory.GUIHandler;
import org.ibenrm01.shopGuiX.Utility;
import org.ibenrm01.shopGuiX.YAMLConfig.Lang;
import org.ibenrm01.shopGuiX.YAMLConfig.Sell;
import org.ibenrm01.shopGuiX.YAMLConfig.Settings;
import org.ibenrm01.shopGuiX.YAMLConfig.Shop;
import org.ibenrm01.shopGuiX.player.InvenGUI;
import org.ibenrm01.shopGuiX.player.PlayerInventorys;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ShopCommand implements CommandExecutor {

    private boolean isConsole(CommandSender sender) {
        return sender instanceof ConsoleCommandSender;
    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {

        String prefix = ChatColor.translateAlternateColorCodes('&', Settings.getInstance().getConfig().getString("serverName"));

        if (args.length == 0) {
            // /shop - open shop GUI (player only)
            if (isConsole(sender)) {
                sender.sendMessage(prefix + " Usage: shop <add|set|remove|list|help>");
                return true;
            }
            Player players = (Player) sender;
            Inventory inven = GUIHandler.getInstance().openMainMenu(players);
            PlayerInventorys.create(players.getUniqueId(), inven, 0, null);
            InvenGUI.create(players.getUniqueId(), true, null, null, "openMainMenu");
            players.openInventory(inven);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help": {
                sender.sendMessage(prefix + ChatColor.GOLD + " Shop Commands:");
                sender.sendMessage(ChatColor.YELLOW + "  /shop" + ChatColor.WHITE + " - Open shop GUI (player only)");
                sender.sendMessage(ChatColor.YELLOW + "  /shop list" + ChatColor.WHITE + " - List all categories and items");
                if (isConsole(sender) || sender.hasPermission("shop.op")) {
                    sender.sendMessage(ChatColor.RED + "  /shop add <category> <buy_price> <sell_price>" + ChatColor.WHITE + " - Add held item (player)");
                    sender.sendMessage(ChatColor.RED + "  /shop additem <category> <material> <buy_price> <sell_price>" + ChatColor.WHITE + " - Add item by name (console)");
                    sender.sendMessage(ChatColor.RED + "  /shop set <category>" + ChatColor.WHITE + " - Set category icon to held item (player)");
                    sender.sendMessage(ChatColor.RED + "  /shop seticon <category> <material>" + ChatColor.WHITE + " - Set category icon by name (console)");
                    sender.sendMessage(ChatColor.RED + "  /shop remove <category> <material>" + ChatColor.WHITE + " - Remove item from category");
                }
                break;
            }

            case "list": {
                // List categories and items - works from both console and player
                List<Map<?, ?>> mainMenu = Shop.getInstance().getConfig().getMapList("MainMenu");
                sender.sendMessage(prefix + ChatColor.GOLD + " Shop Categories:");
                for (Map<?, ?> section : mainMenu) {
                    String catName = String.valueOf(section.get("name"));
                    String icon = String.valueOf(section.get("items"));
                    sender.sendMessage(ChatColor.YELLOW + "  " + catName + ChatColor.GRAY + " [" + icon + "]");
                    Object rawIn = section.get("in");
                    if (rawIn instanceof List<?>) {
                        for (Object obj : (List<?>) rawIn) {
                            if (obj instanceof Map<?, ?> item) {
                                String type = String.valueOf(item.get("type"));
                                String name = String.valueOf(item.get("name"));
                                Object price = item.get("price");
                                sender.sendMessage(ChatColor.WHITE + "    - " + name + ChatColor.GRAY + " (" + type + ") " + ChatColor.GREEN + "Buy: " + price);
                            }
                        }
                    }
                }
                // Show sell prices
                List<Map<?, ?>> sellList = Sell.getInstance().getConfig().getMapList("mainmenu");
                if (!sellList.isEmpty()) {
                    sender.sendMessage(prefix + ChatColor.GOLD + " Sell Prices:");
                    for (Map<?, ?> item : sellList) {
                        String type = String.valueOf(item.get("type"));
                        String name = String.valueOf(item.get("name"));
                        Object price = item.get("price");
                        sender.sendMessage(ChatColor.WHITE + "    - " + name + ChatColor.GRAY + " (" + type + ") " + ChatColor.GREEN + "Sell: " + price);
                    }
                }
                break;
            }

            case "add": {
                // /shop add <category> <buy_price> <sell_price> - player holds item
                if (isConsole(sender)) {
                    sender.sendMessage(prefix + " Use '/shop additem <category> <material> <buy_price> <sell_price>' from console.");
                    return true;
                }
                Player player = (Player) sender;
                if (args.length < 4) {
                    player.sendMessage(prefix + " " + ChatColor.translateAlternateColorCodes('&',
                            Lang.getInstance().getConfig().getString("add.usage")));
                    return false;
                }
                if (!player.hasPermission("shop.op")) {
                    player.sendMessage(prefix + " " + ChatColor.translateAlternateColorCodes('&',
                            Lang.getInstance().getConfig().getString("general.no-permission")));
                    return false;
                }

                ItemStack item = player.getInventory().getItemInMainHand();
                if (item == null || item.getType() == Material.AIR) {
                    player.sendMessage(prefix + " " + ChatColor.translateAlternateColorCodes('&',
                            Lang.getInstance().getConfig().getString("add.noHandItem")));
                    return false;
                }

                String type = item.getType().name().toLowerCase();
                ItemMeta meta = item.getItemMeta();
                String name = (meta != null && meta.hasDisplayName()) ? meta.getDisplayName() : capitalizeWords(type.replace("_", " "));
                addItemToShop(sender, prefix, args[1], type, name, args[2], args[3]);
                break;
            }

            case "additem": {
                // /shop additem <category> <material> <buy_price> <sell_price> - console or player with shop.op
                if (args.length < 5) {
                    sender.sendMessage(prefix + " Usage: /shop additem <category> <material> <buy_price> <sell_price>");
                    return false;
                }
                if (!isConsole(sender) && !sender.hasPermission("shop.op")) {
                    sender.sendMessage(prefix + " " + ChatColor.translateAlternateColorCodes('&',
                            Lang.getInstance().getConfig().getString("general.no-permission")));
                    return false;
                }

                String material = args[2].toLowerCase();
                Material mat = Material.matchMaterial(material);
                if (mat == null) {
                    sender.sendMessage(prefix + ChatColor.RED + " Unknown material: " + material);
                    return false;
                }
                String itemName = capitalizeWords(mat.name().toLowerCase().replace("_", " "));
                addItemToShop(sender, prefix, args[1], mat.name().toLowerCase(), itemName, args[3], args[4]);
                break;
            }

            case "set": {
                // /shop set <category> - player holds item for icon
                if (isConsole(sender)) {
                    sender.sendMessage(prefix + " Use '/shop seticon <category> <material>' from console.");
                    return true;
                }
                Player pl = (Player) sender;
                if (args.length < 2) {
                    pl.sendMessage(prefix + " " + ChatColor.translateAlternateColorCodes('&', Lang.getInstance().getConfig().getString("set.usage")));
                    return false;
                }
                if (!pl.hasPermission("shop.op")) {
                    pl.sendMessage(prefix + " " + ChatColor.translateAlternateColorCodes('&',
                            Lang.getInstance().getConfig().getString("general.no-permission")));
                    return false;
                }
                ItemStack items = pl.getInventory().getItemInMainHand();
                if (items == null || items.getType() == Material.AIR) {
                    pl.sendMessage(prefix + " " + ChatColor.translateAlternateColorCodes('&', Lang.getInstance().getConfig().getString("add.noHandItem")));
                    return false;
                }
                setCategoryIcon(sender, prefix, args[1], items.getType().name().toUpperCase());
                break;
            }

            case "seticon": {
                // /shop seticon <category> <material> - console or player with shop.op
                if (args.length < 3) {
                    sender.sendMessage(prefix + " Usage: /shop seticon <category> <material>");
                    return false;
                }
                if (!isConsole(sender) && !sender.hasPermission("shop.op")) {
                    sender.sendMessage(prefix + " " + ChatColor.translateAlternateColorCodes('&',
                            Lang.getInstance().getConfig().getString("general.no-permission")));
                    return false;
                }
                Material mat = Material.matchMaterial(args[2]);
                if (mat == null) {
                    sender.sendMessage(prefix + ChatColor.RED + " Unknown material: " + args[2]);
                    return false;
                }
                setCategoryIcon(sender, prefix, args[1], mat.name().toUpperCase());
                break;
            }

            case "remove": {
                // /shop remove <category> <material> - console or player with shop.op
                if (args.length < 3) {
                    sender.sendMessage(prefix + " Usage: /shop remove <category> <material>");
                    return false;
                }
                if (!isConsole(sender) && !sender.hasPermission("shop.op")) {
                    sender.sendMessage(prefix + " " + ChatColor.translateAlternateColorCodes('&',
                            Lang.getInstance().getConfig().getString("general.no-permission")));
                    return false;
                }

                String categoryNorm = normalizeCategoryName(args[1]);
                String materialNorm = args[2].toLowerCase();

                List<Map<?, ?>> mainMenu = Shop.getInstance().getConfig().getMapList("MainMenu");
                boolean removed = false;
                for (int i = 0; i < mainMenu.size(); i++) {
                    Map<?, ?> section = mainMenu.get(i);
                    Object rawName = section.get("name");
                    if (rawName instanceof String sectionName && normalizeCategoryName(sectionName).equals(categoryNorm)) {
                        Object rawIn = section.get("in");
                        if (rawIn instanceof List<?>) {
                            List<Map<String, Object>> newIn = new ArrayList<>();
                            for (Object obj : (List<?>) rawIn) {
                                if (obj instanceof Map<?, ?> rawMap) {
                                    String t = String.valueOf(rawMap.get("type")).toLowerCase();
                                    if (!t.equals(materialNorm)) {
                                        Map<String, Object> cleanMap = new LinkedHashMap<>();
                                        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                                            if (entry.getKey() instanceof String key) cleanMap.put(key, entry.getValue());
                                        }
                                        newIn.add(cleanMap);
                                    } else {
                                        removed = true;
                                    }
                                }
                            }
                            Map<String, Object> updated = new LinkedHashMap<>();
                            for (Map.Entry<?, ?> entry : section.entrySet()) {
                                if (entry.getKey() instanceof String key) updated.put(key, entry.getValue());
                            }
                            updated.put("in", newIn);
                            mainMenu.set(i, updated);
                        }
                        break;
                    }
                }

                if (removed) {
                    Shop.getInstance().getConfig().set("MainMenu", mainMenu);
                    Shop.getInstance().save();

                    // Also remove from sell list
                    List<Map<String, Object>> sellList = new ArrayList<>();
                    for (Map<?, ?> rawMap : Sell.getInstance().getConfig().getMapList("mainmenu")) {
                        String t = String.valueOf(rawMap.get("type")).toLowerCase();
                        if (!t.equals(materialNorm)) {
                            Map<String, Object> map = new LinkedHashMap<>();
                            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                                if (entry.getKey() instanceof String key) map.put(key, entry.getValue());
                            }
                            sellList.add(map);
                        }
                    }
                    Sell.getInstance().getConfig().set("mainmenu", sellList);
                    Sell.getInstance().save();

                    sender.sendMessage(prefix + ChatColor.GREEN + " Removed " + materialNorm + " from " + args[1]);
                } else {
                    sender.sendMessage(prefix + ChatColor.RED + " Item " + materialNorm + " not found in " + args[1]);
                }
                break;
            }

            default: {
                // Unknown subcommand - if player, open shop; if console, show help
                if (isConsole(sender)) {
                    sender.sendMessage(prefix + " Unknown subcommand. Use /shop help");
                } else {
                    Player players = (Player) sender;
                    Inventory inven = GUIHandler.getInstance().openMainMenu(players);
                    PlayerInventorys.create(players.getUniqueId(), inven, 0, null);
                    InvenGUI.create(players.getUniqueId(), true, null, null, "openMainMenu");
                    players.openInventory(inven);
                }
                break;
            }
        }
        return true;
    }

    private void addItemToShop(CommandSender sender, String prefix, String categoryInput, String type, String name, String buyPriceStr, String sellPriceStr) {
        int price, sell;
        try {
            price = Integer.parseInt(buyPriceStr);
            sell = Integer.parseInt(sellPriceStr);
        } catch (NumberFormatException e) {
            sender.sendMessage(prefix + " " + ChatColor.translateAlternateColorCodes('&',
                    Lang.getInstance().getConfig().getString("add.onlynumber")));
            return;
        }

        String categoryNormalized = normalizeCategoryName(categoryInput);

        Map<String, Object> shopItemMap = new LinkedHashMap<>();
        shopItemMap.put("type", type);
        shopItemMap.put("name", name);
        shopItemMap.put("price", price);

        Map<String, Object> sellItemMap = new LinkedHashMap<>();
        sellItemMap.put("type", type);
        sellItemMap.put("name", name);
        sellItemMap.put("price", sell);

        List<Map<?, ?>> mainMenu = Shop.getInstance().getConfig().getMapList("MainMenu");
        boolean found = false;
        for (Map<?, ?> section : mainMenu) {
            Object rawName = section.get("name");
            if (rawName instanceof String sectionName && normalizeCategoryName(sectionName).equals(categoryNormalized)) {
                Object rawIn = section.get("in");
                List<Map<String, Object>> inList = new ArrayList<>();
                if (rawIn instanceof List<?>) {
                    for (Object obj : (List<?>) rawIn) {
                        if (obj instanceof Map<?, ?> rawMap) {
                            Map<String, Object> cleanMap = new LinkedHashMap<>();
                            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                                if (entry.getKey() instanceof String key) cleanMap.put(key, entry.getValue());
                            }
                            inList.add(cleanMap);
                        }
                    }
                }
                inList.add(shopItemMap);
                Map<String, Object> sectionMap = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : section.entrySet()) {
                    if (entry.getKey() instanceof String key) sectionMap.put(key, entry.getValue());
                }
                sectionMap.put("in", inList);
                int index = mainMenu.indexOf(section);
                mainMenu.set(index, sectionMap);
                found = true;
                break;
            }
        }

        if (!found) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("category", categoryInput);
            sender.sendMessage(prefix + " " + ChatColor.translateAlternateColorCodes('&',
                    Utility.getInstance().replace(Lang.getInstance().getConfig().getString("general.category-notfound"), placeholders)));
            return;
        }
        Shop.getInstance().getConfig().set("MainMenu", mainMenu);
        Shop.getInstance().save();

        // Sell list
        List<Map<String, Object>> sellList = new ArrayList<>();
        for (Map<?, ?> rawMap : Sell.getInstance().getConfig().getMapList("mainmenu")) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (entry.getKey() instanceof String key) map.put(key, entry.getValue());
            }
            sellList.add(map);
        }
        boolean alreadyExists = false;
        for (Map<String, Object> itemMap : sellList) {
            if (itemMap.get("type") != null && itemMap.get("type").toString().equalsIgnoreCase(type)) {
                alreadyExists = true;
                break;
            }
        }
        if (!alreadyExists) sellList.add(sellItemMap);
        Sell.getInstance().getConfig().set("mainmenu", sellList);
        Sell.getInstance().save();

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("category", categoryInput);
        placeholders.put("item", name);
        placeholders.put("price", Utility.getInstance().formatToRupiah(price));
        sender.sendMessage(prefix + " " + ChatColor.translateAlternateColorCodes('&',
                Utility.getInstance().replace(Lang.getInstance().getConfig().getString("add.success"), placeholders)));
    }

    private void setCategoryIcon(CommandSender sender, String prefix, String categoryArg, String materialName) {
        String categoryName = normalizeCategoryName(categoryArg);
        List<Map<?, ?>> mainMenuList = Shop.getInstance().getConfig().getMapList("MainMenu");
        boolean updated = false;

        for (int i = 0; i < mainMenuList.size(); i++) {
            Map<?, ?> section = mainMenuList.get(i);
            Object rawName = section.get("name");
            if (rawName instanceof String sectionName && normalizeCategoryName(sectionName).equals(categoryName)) {
                Map<String, Object> updatedSection = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : section.entrySet()) {
                    if (entry.getKey() instanceof String key) updatedSection.put(key, entry.getValue());
                }
                updatedSection.put("items", materialName);
                mainMenuList.set(i, updatedSection);
                updated = true;
                break;
            }
        }

        if (updated) {
            Shop.getInstance().getConfig().set("MainMenu", mainMenuList);
            Shop.getInstance().save();
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("category", GUIHandler.getInstance().formatTitle(categoryArg));
            placeholders.put("material", GUIHandler.getInstance().formatTitle(materialName));
            sender.sendMessage(prefix + " " + ChatColor.translateAlternateColorCodes('&',
                    Utility.getInstance().replace(Lang.getInstance().getConfig().getString("set.success"), placeholders)));
        } else {
            sender.sendMessage(prefix + " " + ChatColor.translateAlternateColorCodes('&',
                    Utility.getInstance().replace(Lang.getInstance().getConfig().getString("general.category-notfound"),
                            Collections.singletonMap("category", categoryArg))));
        }
    }

    private String normalizeCategoryName(String input) {
        return input.toLowerCase().replace(" ", "_");
    }

    private String capitalizeWords(String input) {
        String[] parts = input.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }
}
