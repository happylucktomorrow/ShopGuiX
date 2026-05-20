package org.ibenrm01.shopGuiX.solecraft;

import java.util.ArrayList;
import java.util.List;

public final class ShopModels {
    private ShopModels() {
    }

    public static final class Catalog {
        public List<Category> categories = new ArrayList<>();
        public List<Item> items = new ArrayList<>();
    }

    public static final class Category {
        public String id;
        public String displayName;
        public String description;
        public String iconMaterial;
        public int sortOrder;
        public boolean enabled;
    }

    public static final class Item {
        public String id;
        public String categoryId;
        public String sku;
        public String material;
        public String displayName;
        public int buyPrice;
        public int sellPrice;
        public boolean enabled;
    }

    public static final class Balance {
        public String playerUuid;
        public String playerName;
        public int balance;
    }

    public static final class AdminState {
        public String playerUuid;
        public String playerName;
        public boolean blocked;
    }

    public static final class BridgeConnectResult {
        public boolean accepted;
        public boolean blocked;
        public String message;
    }

    public static final class ControlActionList {
        public List<ControlAction> actions = new ArrayList<>();
    }

    public static final class ControlAction {
        public String id;
        public String type;
        public String playerUuid;
        public String playerName;
        public String serverId;
        public String reason;
    }

    public static final class Stock {
        public String serverId;
        public String itemId;
        public int stockCount;
    }

    public static final class Transaction {
        public String id;
        public String action;
        public int quantity;
        public int unitPrice;
        public int totalPrice;
        public int balanceBefore;
        public int balanceAfter;
        public Integer stockBefore;
        public Integer stockAfter;
    }

    public static final class ShopResult {
        public boolean ok = true;
        public String message = "";
        public Item item;
        public Balance balance;
        public Stock stock;
        public Transaction transaction;
    }
}
