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

        // if next unique id is null set to 0
        if (!config.contains("nextUniqueID")) config.set("nextUniqueID", 0);

        GuiManager guiManager = new GuiManager();

        getServer().getPluginManager().registerEvents(new RightClickPaperManager(), this);
        getServer().getPluginManager().registerEvents(guiManager, this);

        this.getCommand("microkits").setExecutor(guiManager);

        //delete
        this.getCommand("addNBT").setExecutor(new nbtCommandManager());

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
}
