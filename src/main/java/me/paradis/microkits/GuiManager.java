package me.paradis.microkits;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import de.tr7zw.nbtapi.NBTItem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

public class GuiManager implements CommandExecutor, Listener {

    private FileConfiguration c = MicroKits.getInstance().getConfig();

    private ArrayList<Player> playerList = new ArrayList<>();

    /**
     * main menu to handle details
     */
    public void openMainGui(Player p) {
        ChestGui gui = new ChestGui(6, "Close to save new kit");

        gui.setOnGlobalClick(event -> {
            event.setCancelled(true);
        });

        StaticPane pane = new StaticPane(0,0,9,6);

        pane.addItem(new GuiItem(new ItemStack(Material.PAPER), inventoryClickEvent -> {
            Player playerEvent = (Player) inventoryClickEvent.getWhoClicked();

            // send player message "enter new name of kit"
            playerEvent.sendMessage("Enter message of kit");

            // close gui, save player in list
            playerEvent.closeInventory();
            playerList.add(playerEvent);


        }), 1,2);
        pane.addItem(new GuiItem(new ItemStack(Material.CLOCK)), 3,2);

        gui.addPane(pane);

        gui.show(p);

    }

    /**
     * opens a gui to add items for new kit
     * missing:
     * perms
     * limits
     * cancel button
     * check if player has enough space in inv
     * save created kit inv player's kits
     *
     * @param p to show the inv
     * @param name of the new kit
     */
    public void newKitGui(Player p, String name){
        // id = getNextID()
        int id = getAndUpdateNextKitUUID();
        UUID uuid = p.getUniqueId();

        ChestGui gui = new ChestGui(6, "Close to save new kit");

        // on close of gui save kit and give player paper with kit
        gui.setOnClose(event -> {
            Inventory inv = gui.getInventory();

            c.set(id + ".owner", uuid.toString());
            c.set(id + ".kitName", name);
            // save inv.getContents
            for (int i = 0; i < inv.getSize(); i++) {
                if (inv.getItem(i) == null) continue;

                c.set(id + ".contents." + i, inv.getItem(i));
                MicroKits.getInstance().saveConfig();
                System.out.println("inv saved");
            }

            // if player has enough space add item, otherwise cancel here

            // create the item
            ItemStack newKit = new ItemStack(Material.PAPER);
            ItemMeta meta = newKit.getItemMeta();

            Objects.requireNonNull(meta).setDisplayName(ChatColor.translateAlternateColorCodes('&', name));

            newKit.setItemMeta(meta);

            // set nbt tags
            NBTItem nbti = new NBTItem(newKit);
            nbti.setBoolean("microKitsPaper", true);
            nbti.setInteger("id", id);
            nbti.setUUID("owner", uuid);
            //nbti.setString("display", null); deletes the display name of item

            newKit = nbti.getItem();

            // gives item and opens gui to save new items
            p.getInventory().addItem(newKit);

            p.sendMessage("new kit saved");
        });

        gui.show(p);
    }

    /**
     * opens a gui to allow a player to preview a kit
     * if kit has no id send message error
     */
    public void previewKitGui(Player p){
        ItemStack itemInHand = p.getInventory().getItemInMainHand();
        NBTItem nbtItem = new NBTItem(itemInHand);
        if (!nbtItem.hasKey("id")){
            p.sendMessage("error while trying to preview kit, no id found");
            return;
        }
        int kitID = nbtItem.getInteger("id");

        // create gui and add items in pane
        ChestGui gui = new ChestGui(6, "previewing kit id: " + kitID);

        gui.setOnGlobalClick(event -> event.setCancelled(true));



        OutlinePane  pane = new OutlinePane(0,0,9,6);

        if (c.getConfigurationSection(kitID + ".contents") == null){
            p.sendMessage("error contents of kit is null");
            return;
        }
        for (String key : c.getConfigurationSection(kitID + ".contents").getKeys(false)){
            pane.addItem(new GuiItem(Objects.requireNonNull(c.getItemStack(kitID + ".contents." + key))));
        }
        gui.addPane(pane);

        gui.show(p);
    }
    @EventHandler
    public void onMessageSentForNameOfKit(AsyncPlayerChatEvent e){
        if (playerList.contains(e.getPlayer())){
            // set message to name of kit
            e.setCancelled(true);

            Bukkit.getScheduler().runTask(MicroKits.getInstance(), () -> {
                // once player sends message open new kit gui
                newKitGui(e.getPlayer(), e.getMessage());
            });

            playerList.remove(e.getPlayer());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)){
            sender.sendMessage("cant use this command from console");
            return false;
        }
        Player p = (Player) sender;
        if (args.length == 0) openMainGui(p);
        else if (args.length == 1) {
            if (args[0].equalsIgnoreCase("preview")) previewKitGui(p);
        }
        else return false;
        return true;
    }

    public Integer getAndUpdateNextKitUUID(){
        int id = c.getInt("nextUniqueID");
        c.set("nextUniqueID", id + 1);
        return id;

    }
}
