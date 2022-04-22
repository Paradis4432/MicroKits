package me.paradis.microkits;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class MicroKits extends JavaPlugin {

    public static MicroKits instance;

    FileConfiguration c = getConfig();

    @Override
    public void onEnable() {
        instance = this;

        addDefaults();

        // if next unique id is null set to 0
        if (!c.contains("nextUniqueID")) c.set("nextUniqueID", 0);

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

    /**
     * adds default messages, gui names, displays etc.
     */
    public void addDefaults(){

        this.saveDefaultConfig();

        String sm = "serverMessages.";
        String smen = "serverMessages.en.";
        String smes = "serverMessages.es.";
        String m = ".message";
        String d = ".display";

        c.addDefault("lan", "en");

        c.addDefault(smen + "nameOfKitInChat" + m, "&6Please type the name of the new kit in chat");
        c.addDefault(smen + "nameOfKitInChat" + d, "message sent when asking for name of kit");

        c.addDefault(smen + "receivedNewEmptyKit" + m, "&6You received a new empty kit");
        c.addDefault(smen + "receivedNewEmptyKit" + d, "message on player getting new empty kit");

        c.addDefault(smen + "claimedStashedItems" + m, "&6You claimed your stashed items");
        c.addDefault(smen + "claimedStashedItems" + d, "message on player claiming stash");

        c.options().copyDefaults(true);
        saveConfig();
    }


}
