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
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
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

        // new kit
        pane.addItem(new GuiItem(new ItemStack(Material.PAPER), inventoryClickEvent -> {
            Player playerEvent = (Player) inventoryClickEvent.getWhoClicked();

            // send player message "enter new name of kit"
            playerEvent.sendMessage("Enter message of kit");

            // close gui, save player in list
            playerEvent.closeInventory();
            playerList.add(playerEvent);


        }), 1,2);

        // view my kits
        pane.addItem(new GuiItem(new ItemStack(Material.CLOCK)), 3,2);

        // new empty kit
        pane.addItem(new GuiItem(new ItemStack(Material.COMPASS), inventoryClickEvent -> {
            // give new empty kit to player

            // create the item
            ItemStack newKit = new ItemStack(Material.COMPASS);
            ItemMeta meta = newKit.getItemMeta();

            Objects.requireNonNull(meta).setDisplayName(ChatColor.translateAlternateColorCodes('&', "&2Empty Kit Right Click To Create"));

            newKit.setItemMeta(meta);

            // set nbt tags
            NBTItem nbti = new NBTItem(newKit);

            // replace with microKits
            nbti.setBoolean("microKitsPaper", true);

            newKit = nbti.getItem();

            // gives item
            p.getInventory().addItem(newKit);

            p.sendMessage("you received a new empty kit");
        }), 5, 2);

        // stashed items
        pane.addItem(new GuiItem(new ItemStack(Material.ENDER_CHEST), inventoryClickEvent -> {
            // give player stashed items
            Objects.requireNonNull(c.getConfigurationSection("stashed." + p.getUniqueId())).getKeys(false).forEach(key -> {
                if (p.getInventory().firstEmpty() != -1) return;

                p.getInventory().addItem(c.getItemStack("stashed." + p.getUniqueId() + "." + key));
                c.set("stashed." + p.getUniqueId() + "." + key, null);
            });

        }), 0, 5);
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

            // prevent empty kit
            if (inv.isEmpty()){
                event.getPlayer().sendMessage("you cant save an empty kit");
                return;
            }

            c.set(id + ".owner", uuid.toString());
            c.set(id + ".kitName", name);
            // save inv.getContents

            //replace with inv.getContents?
            for (int i = 0; i < inv.getSize(); i++) {
                if (inv.getItem(i) == null) continue;

                c.set(id + ".contents." + i, inv.getItem(i));
                MicroKits.getInstance().saveConfig();
                System.out.println("inv saved");
            }

            // create the item
            ItemStack newKit = new ItemStack(Material.PAPER);
            ItemMeta meta = newKit.getItemMeta();

            // add enchant
            meta.addEnchant(Enchantment.LUCK, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            // set display name
            Objects.requireNonNull(meta).setDisplayName(ChatColor.translateAlternateColorCodes('&', name));

            // set lore to id and list of items
            ArrayList<String> lore = new ArrayList<>();

            lore.add("Kit ID: " + id);

            //replace with inv.getContents?
            for (int i = 0; i < inv.getSize(); i++) {
                if (inv.getItem(i) == null) continue;

                ItemStack currentItem = inv.getItem(i);

                if (currentItem.getItemMeta().hasDisplayName())
                    lore.add(currentItem.getType() + " x " + currentItem.getAmount() + " name: " + currentItem.getItemMeta().getDisplayName() );
                else
                    lore.add(currentItem.getType() + " x " + currentItem.getAmount());
            }

            meta.setLore(lore);

            newKit.setItemMeta(meta);

            // set nbt tags
            NBTItem nbti = new NBTItem(newKit);
            // replace with microKits
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

    @EventHandler
    public void onRightClickEmptyKit(PlayerInteractEvent e){
        if (!Objects.requireNonNull(e.getHand()).equals(EquipmentSlot.HAND)) return;
        if (!e.getAction().equals(Action.RIGHT_CLICK_AIR)) return;

        ItemStack item = e.getPlayer().getInventory().getItemInMainHand();
        if (!item.getType().equals(Material.COMPASS)) return;

        NBTItem nbtItem = new NBTItem(item);
        // replace with microKits
        if (!nbtItem.hasKey("microKitsPaper")) return;

        e.getPlayer().sendMessage("write the name of the new kit");
        // listen for message
        playerList.add(e.getPlayer());

        // once message is sent open new kit gui

        // remove item from player
        e.setCancelled(true);
        e.getPlayer().getInventory().remove(item);

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
