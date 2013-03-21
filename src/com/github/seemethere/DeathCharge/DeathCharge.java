package com.github.seemethere.DeathCharge;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author seemethere
 */
public class DeathCharge extends JavaPlugin implements Listener {

    private static final String PLUGIN_NAME = "[DeathCharge] ";
    private static HashMap<String, String> ex_regions = new HashMap<String, String>();
    private static List<String> ex_worlds = new ArrayList<String>();
    private static YamlConfiguration r_config = null;
    public Logger logger = Logger.getLogger("Minecraft");
    private Economy e = null;
    private WorldGuardPlugin wg = null;

    @Override
    public void onEnable() {
        //Make initial config and ExculdedRegions.yml
        if (!make_Config()) {
            logger.severe(PLUGIN_NAME + "Unable to create config or ExcludedRegion.yml! Exiting...");
            return;
        }

        //Set up economy
        RegisteredServiceProvider<Economy> economyP = getServer().getServicesManager().getRegistration(Economy.class);
        if (economyP != null)
            e = economyP.getProvider();
        else
            logger.severe(PLUGIN_NAME + "Unable to initialize Economy Interface with Vault!");

        //Set up WorldGuard
        if (!(this.getServer().getPluginManager().getPlugin("WorldGuard") instanceof WorldGuardPlugin)) {
            logger.severe(PLUGIN_NAME + " No WorldGuard found! Exiting via exception...");
            throw new RuntimeException();
        }
        wg = (WorldGuardPlugin) this.getServer().getPluginManager().getPlugin("WorldGuard");

        //Load custom config for region exclusions
        File r_file = new File(this.getDataFolder(), "ExcludedRegions.yml");
        try {
            r_config = YamlConfiguration.loadConfiguration(r_file);
        } catch (Throwable t) {
            logger.severe(PLUGIN_NAME + "Unable to load ExcludedRegions.yml! Exiting...");
            return;
        }

        //Populates HashMap for ExcludedRegions
        if (r_config.getConfigurationSection("ex_regions") != null) {
            Map<String, Object> temp = r_config.getConfigurationSection("ex_regions").getValues(false);
            for (Map.Entry<String, Object> entry : temp.entrySet())
                ex_regions.put(entry.getKey(), (String) entry.getValue());
        }
        // Add support for multi-worlds
        ex_worlds = r_config.getStringList("ex_worlds");

        this.getServer().getPluginManager().registerEvents(this, this);
        logger.info(PLUGIN_NAME + "DeathCharge has been enabled!");
    }

    @Override
    public void onDisable() {
        save_Config();
        e = null;
        wg = null;
        ex_regions = null;
        ex_worlds = null;
        r_config = null;
        logger.info(PLUGIN_NAME + "DeathCharge has been disabled!");
        logger = null;
    }

    private boolean make_Config() {
        File pluginFolder = getDataFolder();
        if (!pluginFolder.exists() && !pluginFolder.mkdir()) {
            logger.severe(PLUGIN_NAME + "Could not make plugin folder!");
            return false;
        }
        File config = new File(getDataFolder(), "config.yml");
        if (!config.exists())
            this.saveDefaultConfig();
        File r_file = new File(this.getDataFolder(), "ExcludedRegions.yml");
        if (!r_file.exists()) {
            try {
                if (!r_file.createNewFile())
                    return false;
            } catch (IOException e) {
                logger.severe(PLUGIN_NAME + "Could not make ExcludedRegions.yml file!");
            }
        }
        return true;
    }

    private void save_Config() {
        File r_file = new File(this.getDataFolder(), "ExcludedRegions.yml");
        r_config.set("ex_regions", ex_regions);
        r_config.set("ex_worlds", ex_worlds);
        try {
            r_config.save(r_file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ProtectedRegion find_Region(Location l) {
        ProtectedRegion h = null;
        for (ProtectedRegion r : wg.getRegionManager(l.getWorld()).getApplicableRegions(l))
            if (h == null || h.getPriority() <= r.getPriority())
                h = r;
        if (h == null)
            return null;
        else
            return h;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String args[]) {
        if (!(sender instanceof Player)) {
            logger.info(PLUGIN_NAME + "Silly console you can't do any commands");
            return true;
        }
        Player p = (Player) sender;
        if (command.getName().equalsIgnoreCase("deathcharge"))
            if (args.length == 0 || args.length > 1)
                about(p);
            else if (args[0].equalsIgnoreCase("region"))
                c_region(p);
            else if (args[0].equalsIgnoreCase("world"))
                c_world(p);
        return true;
    }

    private void c_region(Player p) {
        if (!p.isOp()) {
            p.sendMessage(String.format("%sERROR: %sInsufficient permissions!",
                    ChatColor.RED, ChatColor.YELLOW));
            return;
        }
        if (find_Region(p.getLocation()) != null) {
            String id = find_Region(p.getLocation()).getId();
            for (String s : ex_regions.keySet())
                if (id.equalsIgnoreCase(s) && ex_regions.get(id).equalsIgnoreCase(p.getWorld().toString())) {
                    ex_regions.remove(id.toLowerCase());
                    p.sendMessage(String.format("%s[DeathCharge]%s Removed %s%s%s from excluded regions!",
                            ChatColor.YELLOW, ChatColor.RED, ChatColor.YELLOW, id, ChatColor.RED));
                    logger.info(PLUGIN_NAME + "Player " + p.getName() + " removed region " + id + " from excluded regions");
                    return;
                }
            ex_regions.put(id.toLowerCase(), p.getWorld().toString());
            p.sendMessage(String.format("%s[DeathCharge]%s Added %s%s%s to excluded regions!",
                    ChatColor.YELLOW, ChatColor.RED, ChatColor.GREEN, id, ChatColor.RED));
            logger.info(PLUGIN_NAME + "Player " + p.getName() + " added region " + id + " to excluded regions");
        } else {
            p.sendMessage(String.format("%sERROR: %sNo region found!",
                    ChatColor.RED, ChatColor.YELLOW));
        }
    }

    private void c_world(Player p) {
        if (!p.isOp()) {
            p.sendMessage(String.format("%sERROR: %sInsufficient permissions!",
                    ChatColor.RED, ChatColor.YELLOW));
            return;
        }
        if (ex_worlds.contains(p.getWorld().toString())) {
            ex_worlds.remove(p.getWorld().toString());
            p.sendMessage(String.format("%s[DeathCharge]%s Removed %s%s%s from excluded worlds!",
                    ChatColor.YELLOW, ChatColor.RED,
                    ChatColor.YELLOW, p.getWorld().toString(), ChatColor.RED));

            logger.info(PLUGIN_NAME + "Player " + p.getName() + " removed world " +
                    p.getWorld().toString() + " from excluded worlds");
        } else {
            ex_worlds.add(p.getWorld().toString());
            p.sendMessage(String.format("%s[DeathCharge]%s Added %s%s%s to excluded worlds!",
                    ChatColor.YELLOW, ChatColor.RED, ChatColor.GREEN,
                    p.getWorld().toString(), ChatColor.RED));
            logger.info(PLUGIN_NAME + "Player " + p.getName() + " " +
                    "added world " + p.getWorld().toString() + " to excluded worlds");
        }
    }

    private void about(Player p) {
        p.sendMessage(String.format("%s[DeathCharge]%s by seemethere",
                ChatColor.YELLOW, ChatColor.RED));
        p.sendMessage(String.format("%s[DeathCharge]%s Available on BukkitDev!",
                ChatColor.YELLOW, ChatColor.RED));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player p = event.getEntity();
        //Checks the config to disable/enable pvp toggle
        if (p.getKiller() != null)
            if (!this.getConfig().getBoolean("pvp"))
                return;
        // Add multiworld support
        if (ex_worlds.contains(p.getWorld().toString()))
            return;
        //Checks if the area the player is standing in is excluded
        if (find_Region(p.getLocation()) != null) {
            String r = find_Region(p.getLocation()).getId().toLowerCase();
            if (ex_regions.containsKey(r) //Added to relate regions to the world they are located in
                    && ex_regions.get(r).equalsIgnoreCase(p.getWorld().toString()))
                return;
        }
        //Actually take the money from the account
        double m_lost = (this.getConfig().getDouble("percent") / 100) * e.getBalance(p.getName());
        e.withdrawPlayer(p.getName(), m_lost);
        p.sendMessage(String.format("%sYou have lost %s$%.2f%s on death!",
                ChatColor.YELLOW, ChatColor.RED, m_lost, ChatColor.YELLOW));
    }
}
