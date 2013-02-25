package com.github.seemethere.DeathCharge;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Logger;

/**
 * User: Seemethere
 * Date: 2/24/13
 */
public class DeathCharge extends JavaPlugin implements Listener {

    private static final String PLUGIN_NAME = "[DeathCharge] ";
    public Logger logger = Logger.getLogger("Minecraft");
    private Economy e = null;

    public void onEnable() {
        LoadConfig();
        RegisteredServiceProvider<Economy> economyP = getServer().getServicesManager().getRegistration(Economy.class);
        if (economyP != null)
            e = economyP.getProvider();
        else
            logger.severe(PLUGIN_NAME + "Unable to initialize Economy Interface with Vault!");
        this.getServer().getPluginManager().registerEvents(this, this);
        logger.info(PLUGIN_NAME + "has been enabled!");
    }

    public void onDisable() {
        logger.info(PLUGIN_NAME + "has been enabled!");
    }

    private void LoadConfig() {
        File pluginFolder = getDataFolder();
        if (!pluginFolder.exists() && !pluginFolder.mkdir()) {
            logger.info(PLUGIN_NAME + "Could not make plugin folder!");
            return;
        }
        File config = new File(getDataFolder(), "config.yml");
        if (!config.exists())
            this.saveDefaultConfig();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player p = event.getEntity();
        double percentLost = this.getConfig().getInt("percent");
        percentLost /= 100;
        double b = e.getBalance(p.getName());
        b *= percentLost;
        e.withdrawPlayer(p.getName(), b);
        p.sendMessage(String.format("%sYou have lost %s%.2f%s on death!", ChatColor.YELLOW, ChatColor.RED, b, ChatColor.YELLOW));
    }
}
