package org.ibenrm01.shopGuiX;

import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.ibenrm01.shopGuiX.Commands.SellComand;
import org.ibenrm01.shopGuiX.Commands.ShopCommand;
import org.ibenrm01.shopGuiX.Commands.MoneyCommand;
import org.ibenrm01.shopGuiX.Listener.CategoryEvent;
import org.ibenrm01.shopGuiX.Listener.InventoryEvents;
import org.ibenrm01.shopGuiX.Listener.ItemsEvent;
import org.ibenrm01.shopGuiX.Listener.PlayerBalanceBarListener;
import org.ibenrm01.shopGuiX.Listener.sell.InventoryEventSell;
import org.ibenrm01.shopGuiX.YAMLConfig.Lang;
import org.ibenrm01.shopGuiX.YAMLConfig.Sell;
import org.ibenrm01.shopGuiX.YAMLConfig.Settings;
import org.ibenrm01.shopGuiX.YAMLConfig.Shop;
import org.ibenrm01.shopGuiX.solecraft.BalanceBarService;
import org.ibenrm01.shopGuiX.solecraft.PlayerControlPoller;
import org.ibenrm01.shopGuiX.solecraft.SolecraftClient;

public final class ShopGuiX extends JavaPlugin implements Listener {
    private SolecraftClient solecraftClient;
    private BalanceBarService balanceBarService;
    private PlayerControlPoller playerControlPoller;

    @Override
    public void onEnable() {
        Settings.getInstance().load();
        Lang.getInstance().load(Settings.getInstance().getConfig().getString("lang"));
        Sell.getInstance().load();
        Shop.getInstance().load();

        getServer().getPluginManager().registerEvents(new InventoryEvents(), this);
        getServer().getPluginManager().registerEvents(new CategoryEvent(), this);
        getServer().getPluginManager().registerEvents(new ItemsEvent(), this);
        getServer().getPluginManager().registerEvents(new InventoryEventSell(), this);

        solecraftClient = SolecraftClient.fromSettings(this, Settings.getInstance().getConfig());
        balanceBarService = new BalanceBarService(this, solecraftClient);
        playerControlPoller = new PlayerControlPoller(this, solecraftClient);
        getServer().getPluginManager().registerEvents(new PlayerBalanceBarListener(this, solecraftClient, balanceBarService), this);
        balanceBarService.start();
        playerControlPoller.start();

        getLogger().info("ShopGuiX Enabled");

        getCommand("shopgui").setExecutor(new ShopCommand());
        getCommand("sellgui").setExecutor(new SellComand());
        MoneyCommand moneyCommand = new MoneyCommand(solecraftClient, balanceBarService);
        getCommand("money").setExecutor(moneyCommand);
        getCommand("scmoney").setExecutor(moneyCommand);
    }

    @Override
    public void onDisable() {
        getLogger().info("ShopGuiX has been disabled");
        if (balanceBarService != null) {
            balanceBarService.stop();
        }
        if (playerControlPoller != null) {
            playerControlPoller.stop();
        }
        Settings.getInstance().save();
        Shop.getInstance().save();
        Lang.getInstance().save();
        Sell.getInstance().save();
    }

    public SolecraftClient getSolecraftClient() {
        return solecraftClient;
    }

    public BalanceBarService getBalanceBarService() {
        return balanceBarService;
    }

    public static ShopGuiX getInstance() {
        return getPlugin(ShopGuiX.class);
    }
}
