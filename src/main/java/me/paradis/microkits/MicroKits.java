package me.paradis.microkits;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class MicroKits extends JavaPlugin {

    public static MicroKits instance;

    FileConfiguration config = getConfig();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        getServer().getPluginManager().registerEvents(new RightClickPaperManager(), this);

        this.getCommand("microkits").setExecutor(new CommandManager());

        getLogger().info("MicroKits Enabled - Made by Paradis");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        saveConfig();
    }

    public static MicroKits getInstance() {
        return instance;
    }

    /**
     * TODO:
     * add perms
     */
}
