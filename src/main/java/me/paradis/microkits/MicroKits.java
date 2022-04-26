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
        //this.getCommand("addNBT").setExecutor(new nbtCommandManager());

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

        c.addDefault(smen + "cancelEmptyKitCreation" + m, "&cYou can not save an empty kit");
        c.addDefault(smen + "cancelEmptyKitCreation" + d, "message on player saving empty kit");

        c.addDefault(smen + "newKitSaved" + m, "&6New kit saved - Right click paper to claim it");
        c.addDefault(smen + "newKitSaved" + d, "message on player saving new kit");

        c.addDefault(smen + "errorPreviewKit" + m, "&cError while trying to preview kit");
        c.addDefault(smen + "errorPreviewKit" + d, "message on error previewing kit");

        c.addDefault(smen + "errorPreviewKitEmpty" + m, "&cThe contents of this kit was not found");
        c.addDefault(smen + "errorPreviewKitEmpty" + d, "message on previewing kit with no items");

        c.addDefault(smen + "errorAlreadyCreatingKit" + m, "&cYou are already creating a kit");
        c.addDefault(smen + "errorAlreadyCreatingKit" + d, "message on player attempting to create two kits at the same time");

        c.addDefault(smen + "overfilledPlayerInv" + m, "&6Found more items than free slots in your inv, the remaining items were saved on your stash, to claim these, right click the ender chest in the main gui");
        c.addDefault(smen + "overfilledPlayerInv" + d, "message on overfilled player inventory");

        c.addDefault(smen + "kitClaimed" + m, "&6You have claimed a kit");
        c.addDefault(smen + "kitClaimed" + d, "message on player claim kit");

        c.addDefault(smen + "lanChanged" + m, "&6Language changed");
        c.addDefault(smen + "lanChanged" + d, "message on player changing language");

        c.addDefault(smen + "newMessageSet" + m, "&6Type in chat the new message for this action");
        c.addDefault(smen + "newMessageSet" + d, "message on admin changing message of action");

        c.addDefault(smen + "noPermToClaim" + m, "&6You don't have enough permission to claim this empty kit");
        c.addDefault(smen + "noPermToClaim" + d, "message on player attempting to claim an empty kit");

        c.addDefault(smen + "kitNotFound" + m, "&cThe kit you are trying to claim does not exist");
        c.addDefault(smen + "kitNotFound" + d, "message on player claiming kit not existing");

        c.addDefault(smen + "myKitsNotFound" + m, "&cYou currently have no kits to view");
        c.addDefault(smen + "myKitsNotFound" + d, "message on player not having kits and clicking my kits");

        c.addDefault(smen + "noPermToPreview" + m, "&cYou do not have permission to preview this kit");
        c.addDefault(smen + "noPermToPreview" + d, "message on player attempting to preview a kit without perms");

        c.options().copyDefaults(true);
        saveConfig();
    }


}
