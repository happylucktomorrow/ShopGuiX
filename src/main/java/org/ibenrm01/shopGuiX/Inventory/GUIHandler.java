package org.ibenrm01.shopGuiX.Inventory;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.ibenrm01.shopGuiX.Utility;
import org.ibenrm01.shopGuiX.YAMLConfig.Lang;
import org.ibenrm01.shopGuiX.YAMLConfig.Settings;
import org.ibenrm01.shopGuiX.player.InvenGUI;
import org.ibenrm01.shopGuiX.player.PlayerInventorys;
import org.ibenrm01.shopGuiX.ShopGuiX;
import org.ibenrm01.shopGuiX.solecraft.ShopModels;

import java.util.*;

public class GUIHandler {

    private final static GUIHandler instance = new GUIHandler();


    private GUIHandler() {
    }

    public Inventory openMainMenu(Player player) {
        int SIZE = 54;
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.translateAlternateColorCodes('&', Settings.getInstance().getConfig().getString("textMainMenu")));

        ItemStack border = createGlassPane();
        for (int i = 0; i < SIZE; i++) {
            inv.setItem(i, border);
        }

        ShopModels.Catalog catalog = loadCatalog();
        List<ShopModels.Category> menu = catalog.categories;
        int[] usableSlots = {
                11, 12, 13, 14, 15,
                20, 21, 22, 23, 24,
                30, 32
        };

        for (int i = 0; i < Math.min(menu.size(), usableSlots.length); i++) {
            ShopModels.Category category = menu.get(i);
            if (!category.enabled) continue;
            String name = category.displayName;
            String description = category.description;
            String itemType = category.iconMaterial;

            Material mat = Material.matchMaterial(itemType.toUpperCase());
            if (mat == null) mat = Material.BARRIER;

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            meta.setLore(Arrays.asList(
                    ChatColor.translateAlternateColorCodes('&', description),
                    ChatColor.DARK_GRAY + "solecraft-category:" + category.id
            ));
            item.setItemMeta(meta);

            inv.setItem(usableSlots[i], item);
        }
        if (menu.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Shop catalog is empty or unavailable. Check Solecraft API and bridge token.");
        }
        String exitFormat = Settings.getInstance().getConfig().getString("textExit");
        if (exitFormat == null) exitFormat = "&cExit";
        inv.setItem(49, createControlItem(Material.BARRIER, ChatColor.translateAlternateColorCodes('&',
                exitFormat)));
        return inv;
    }

    public Object[] openCategoryItems(Player player, String categoryName, int page) {
        String prefix = Settings.getInstance().getConfig().getString("serverName");
        Lang lang = Lang.getInstance();
        int SIZE = 54;
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("number", String.valueOf(page + 1));
        placeholders.put("category", categoryName);

        Inventory inv = Bukkit.createInventory(null, SIZE, ChatColor.translateAlternateColorCodes('&',
                formatTitle(categoryName)) + " " + ChatColor.translateAlternateColorCodes('&',
                Utility.replace(Settings.getInstance().getConfig().getString("textPage"), placeholders)));


        ItemStack border = createGlassPane();
        for (int i = 0; i < SIZE; i++) {
            inv.setItem(i, border);
        }

        ShopModels.Catalog catalog = loadCatalog();
        ShopModels.Category category = catalog.categories.stream()
                .filter(cat -> cat.id.equalsIgnoreCase(categoryName) || formatKey(cat.displayName).equalsIgnoreCase(categoryName))
                .findFirst().orElse(null);

        if (category == null) {
            player.sendMessage(Utility.getInstance().replace(ChatColor.translateAlternateColorCodes('&',
                    Lang.getInstance().getConfig().getString("general.category-notfound")), placeholders));
            return new Object[]{"error"};
        }

        List<ShopModels.Item> items = new ArrayList<>();
        for (ShopModels.Item item : catalog.items) {
            if (item.enabled && item.categoryId.equals(category.id)) {
                items.add(item);
            }
        }
        if (items == null || items.isEmpty()) {
            player.sendMessage(prefix + " " + ChatColor.translateAlternateColorCodes('&', lang.getConfig().getString("general.no-thereitem")));
            player.closeInventory();
            return new Object[]{"error"};
        }

        int[] itemSlots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34
        };

        int start = page * itemSlots.length;
        int end = Math.min(start + itemSlots.length, items.size());

        for (int i = start; i < end; i++) {
            ShopModels.Item itemData = items.get(i);
            String rawName = itemData.displayName;
            String typeName = itemData.material.toUpperCase();

            Material mat = Material.matchMaterial(typeName);
            if (mat == null) mat = Material.BARRIER;

            ItemStack item = new ItemStack(mat, 16);
            ItemMeta meta = item.getItemMeta();

            String price = Utility.getInstance().formatToRupiah(itemData.buyPrice);
            placeholders.put("price", String.valueOf(price));
            placeholders.put("name", formatTitle(rawName));

            List<String> loreTemplate = Settings.getInstance().getConfig().getStringList("itemSelector.lore");
            List<String> loreFinal = new ArrayList<>();

            for (String line : loreTemplate) {
                String replaced = Utility.replace(line, placeholders);
                replaced = ChatColor.translateAlternateColorCodes('&', replaced);
                loreFinal.add(replaced);
            }

            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', rawName));
            loreFinal.add(ChatColor.DARK_GRAY + "solecraft-item:" + itemData.id);
            meta.setLore(loreFinal);
            item.setItemMeta(meta);

            inv.setItem(itemSlots[i - start], item);
        }
        String prevFormat = Settings.getInstance().getConfig().getString("textPrev");
        if (prevFormat == null) prevFormat = "&d← Sebelumnya";
        String nextFormat = Settings.getInstance().getConfig().getString("textNext");
        if (nextFormat == null) nextFormat = "&dSelanjutnya →";
        String exitFormat = Settings.getInstance().getConfig().getString("textExit");
        if (exitFormat == null) exitFormat = "&cExit";

        if (page > 0)
            inv.setItem(45, createControlItem(Material.PAPER, ChatColor.translateAlternateColorCodes('&',
                    prevFormat)));
        inv.setItem(49, createControlItem(Material.BARRIER, ChatColor.translateAlternateColorCodes('&',
                exitFormat)));
        if (end < items.size())
            inv.setItem(53, createControlItem(Material.PAPER, ChatColor.translateAlternateColorCodes('&',
                    nextFormat)));

        return new Object[]{"success", inv};
    }

    public Inventory openQuantitySelector(Player player, ItemStack targetItem, int amount) {
        InvenGUI invenGUI = InvenGUI.get(player.getUniqueId());
        PlayerInventorys plInven = PlayerInventorys.get(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_GREEN + "Buying " + targetItem.getType().name());

        ItemStack filler = createGlassPane();
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);

        // Tampilkan salinan item dengan amount sesuai
        targetItem.setAmount(amount);
        ItemMeta meta = targetItem.getItemMeta();
        ShopModels.Item shopItem = ShopGuiX.getInstance().getSolecraftClient().findItem(invenGUI.getItemId());
        int parsePrice = shopItem == null ? 0 : shopItem.buyPrice;
        String price = Utility.getInstance().formatToRupiah(parsePrice * amount);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("price", String.valueOf(price));
        placeholders.put("amount", String.valueOf(amount));

        List<String> loreTemplate = Settings.getInstance().getConfig().getStringList("stackSelector.lore");
        List<String> loreFinal = new ArrayList<>();

        for (String line : loreTemplate) {
            String replaced = Utility.replace(line, placeholders);
            replaced = ChatColor.translateAlternateColorCodes('&', replaced);
            loreFinal.add(replaced);
        }
        meta.setLore(loreFinal);
        targetItem.setItemMeta(meta);
        inv.setItem(22, targetItem);

        invenGUI.setItemsAmount(amount);

        inv.setItem(18, createControlItem(Material.RED_STAINED_GLASS_PANE, ChatColor.translateAlternateColorCodes('&', "&c-1")));
        inv.setItem(19, createControlItem(Material.RED_STAINED_GLASS_PANE, ChatColor.translateAlternateColorCodes('&', "&c-10")));
        inv.setItem(20, createControlItem(Material.RED_STAINED_GLASS_PANE, ChatColor.translateAlternateColorCodes('&', "&c-32")));

        inv.setItem(24, createControlItem(Material.GREEN_STAINED_GLASS_PANE, ChatColor.translateAlternateColorCodes('&', "&a+1")));
        inv.setItem(25, createControlItem(Material.GREEN_STAINED_GLASS_PANE, ChatColor.translateAlternateColorCodes('&', "&a+10")));
        inv.setItem(26, createControlItem(Material.GREEN_STAINED_GLASS_PANE, ChatColor.translateAlternateColorCodes('&', "&a+64")));

        String confirmFormat = Settings.getInstance().getConfig().getString("textConfirm");
        if (confirmFormat == null) confirmFormat = "&aConfirm";
        String moreFormat = Settings.getInstance().getConfig().getString("textMore");
        if (moreFormat == null) moreFormat = "&eMore Option";
        String cancelFormat = Settings.getInstance().getConfig().getString("textCancel");
        if (cancelFormat == null) cancelFormat = "&cCancel";

        inv.setItem(38, createControlItem(Material.LIME_DYE, ChatColor.translateAlternateColorCodes('&', confirmFormat)));
        inv.setItem(40, createControlItem(Material.CHEST, ChatColor.translateAlternateColorCodes('&', moreFormat)));
        inv.setItem(42, createControlItem(Material.RED_DYE, ChatColor.translateAlternateColorCodes('&', cancelFormat)));

        return inv;
    }

    public Inventory openStackSelector(Player player, ItemStack item) {
        Inventory inv = Bukkit.createInventory(null, 45, ChatColor.DARK_GREEN + "Buy stacks of " + item.getType().name());

        ItemStack filler = createGlassPane();
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }

        int[] slots = {14, 15, 16, 23, 24, 25, 32, 33, 34};
        for (int i = 0; i < slots.length; i++) {
            int stackAmount = (i + 1) * 64;
            ItemStack stackItem = item.clone();
            stackItem.setAmount(i + 1);
            ItemMeta meta = stackItem.getItemMeta();
            ShopModels.Item shopItem = ShopGuiX.getInstance().getSolecraftClient().findItem(InvenGUI.get(player.getUniqueId()).getItemId());
            int parsePrice = shopItem == null ? 0 : shopItem.buyPrice;
            String price = Utility.getInstance().formatToRupiah(parsePrice * stackAmount);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("price", String.valueOf(price));
            placeholders.put("amount", String.valueOf(stackAmount));

            List<String> loreTemplate = Settings.getInstance().getConfig().getStringList("stackSelector.lore");
            List<String> loreFinal = new ArrayList<>();

            for (String line : loreTemplate) {
                String replaced = Utility.replace(line, placeholders);
                replaced = ChatColor.translateAlternateColorCodes('&', replaced);
                loreFinal.add(replaced);
            }

            meta.setDisplayName(ChatColor.GOLD + String.valueOf(i + 1) + " Stack" + (i > 0 ? "s" : ""));
            meta.setLore(loreFinal);
            stackItem.setItemMeta(meta);
            inv.setItem(slots[i], stackItem);
        }

        String cancelFormat = Settings.getInstance().getConfig().getString("textCancel");
        if (cancelFormat == null) cancelFormat = "&cCancel";
        inv.setItem(36, createControlItem(Material.RED_DYE, ChatColor.translateAlternateColorCodes('&', cancelFormat)));

        return inv;
    }


    public String formatKey(String input) {
        return input.toLowerCase().replace(" ", "_");
    }

    private ShopModels.Catalog loadCatalog() {
        try {
            return ShopGuiX.getInstance().getSolecraftClient().catalog();
        } catch (Exception error) {
            ShopGuiX.getInstance().getLogger().warning("Failed to refresh Solecraft shop catalog: " + error.getMessage());
            return ShopGuiX.getInstance().getSolecraftClient().cachedCatalog();
        }
    }

    public String formatTitle(String rawName) {
        String[] parts = rawName.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            sb.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    private ItemStack createGlassPane() {
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(" ");
        glass.setItemMeta(meta);
        return glass;
    }

    private ItemStack createControlItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    public static GUIHandler getInstance() {
        return instance;
    }
}
