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

        c.addDefault(smen + "enterKitName" + m, "&6&lPlease type in chat the new name for the kit");
        c.addDefault(smen + "enterKitName" + d, "when asking for kit name in chat");

        c.options().copyDefaults(true);
        saveConfig();
    }


}
