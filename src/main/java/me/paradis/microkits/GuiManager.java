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

import java.util.*;

public class GuiManager implements CommandExecutor, Listener {

    private FileConfiguration c = MicroKits.getInstance().getConfig();
    private MessagesManager mm = new MessagesManager();

    /**
     * hashmap<Player, int> made to listen for player message
     * if int = 0: listening for kit name
     * if int = 1: listening for new message for config
     * if int = 2: listening for new title for config
     */
    //private ArrayList<Player> playerList = new ArrayList<>();
    private HashMap<Player, Integer> pendingPlayersInChat = new HashMap<>();
    private HashMap<Player, ArrayList<String>> pendingMessageChangeInChat = new HashMap<>();


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
        if (p.hasPermission("microkits.newKit")){
            pane.addItem(new GuiItem(new ItemStack(Material.PAPER), inventoryClickEvent -> {
                Player playerEvent = (Player) inventoryClickEvent.getWhoClicked();

                // send player message "enter new name of kit"
                playerEvent.sendMessage(mm.getMessage("nameOfKitInChat"));

                // close gui, save player in list
                playerEvent.closeInventory();
                pendingPlayersInChat.put(playerEvent, 0);

            }), 1,2);
        }

        // view my kits
        if (p.hasPermission("microkits.viewKits")){
            pane.addItem(new GuiItem(new ItemStack(Material.BOOK), inventoryClickEvent -> {
                showPlayerKits((Player) inventoryClickEvent.getWhoClicked());
            }), 3,2);

        }

        // new empty kit
        if (p.hasPermission("microkits.newEmptyKit.create")){
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
                nbti.setBoolean("microKits", true);

                newKit = nbti.getItem();

                // gives item
                p.getInventory().addItem(newKit);

                p.sendMessage(mm.getMessage("receivedNewEmptyKit"));
            }), 5, 2);
        }

        // stashed items
        if (p.hasPermission("microkits.stash")){
            pane.addItem(new GuiItem(new ItemStack(Material.ENDER_CHEST), inventoryClickEvent -> {
                // give player stashed items
                Objects.requireNonNull(c.getConfigurationSection("stashed." + p.getUniqueId())).getKeys(false).forEach(key -> {
                    if (p.getInventory().firstEmpty() != -1) return;

                    p.getInventory().addItem(c.getItemStack("stashed." + p.getUniqueId() + "." + key));
                    c.set("stashed." + p.getUniqueId() + "." + key, null);
                });

                p.sendMessage(mm.getMessage("claimedStashedItems"));

            }), 0, 5);
        }

        // player language
        if (p.hasPermission("microkits.playerLan")){
            pane.addItem(new GuiItem(new ItemStack(Material.REDSTONE_TORCH), inventoryClickEvent -> {
                selectLanPlayer((Player) inventoryClickEvent.getWhoClicked());
            }), 7, 2);
        }

        // server language and messages
        if (p.hasPermission("microkits.serverLan")){
            pane.addItem(new GuiItem(new ItemStack(Material.REDSTONE_BLOCK), inventoryClickEvent -> {
                selectLanServer((Player) inventoryClickEvent.getWhoClicked());
            }), 8, 5);
        }

        // add preview item

        gui.addPane(pane);

        gui.show(p);

    }

    /**
     * shows all player's kits, these kits are stored under "savedKits.UUID.KITID.contents.ITEMID
     */
    private void showPlayerKits(Player p) {
        ChestGui gui = new ChestGui(6, "your kits: ");

        gui.setOnGlobalClick(event -> event.setCancelled(true));

        OutlinePane pane = new OutlinePane(0,0,9,6);

        String s = "savedKits." + p.getUniqueId();

        if (c.getConfigurationSection(s) == null) {
            p.sendMessage(mm.getMessage("myKitsNotFound"));
            p.closeInventory();
            return;
        }

        for (String key : c.getConfigurationSection(s).getKeys(false)){
            String kitName = c.getString(s + "." + key + ".kitName");
            List<String> lore = c.getStringList(s + "." + key + ".lore");

            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();

            meta.setDisplayName(kitName);

            meta.setLore(lore);

            item.setItemMeta(meta);

            pane.addItem(new GuiItem(item));
        }

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
    private void newKitGui(Player p, String name){
        // id = getNextID()
        int id = getAndUpdateNextKitUUID();
        UUID uuid = p.getUniqueId();

        ChestGui gui = new ChestGui(6, "Close to save new kit");

        // on close of gui save kit and give player paper with kit
        gui.setOnClose(event -> {
            Inventory inv = gui.getInventory();

            // prevent empty kit
            if (inv.isEmpty()){
                event.getPlayer().sendMessage(mm.getMessage("cancelEmptyKitCreation"));
                return;
            }

            c.set(id + ".owner", uuid.toString());
            c.set(id + ".kitName", name);
            // save inv.getContents

            //replace with inv.getContents?
            for (int i = 0; i < inv.getSize(); i++) {
                if (inv.getItem(i) == null) continue;

                c.set(id + ".contents." + i, inv.getItem(i));
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

                assert currentItem != null;
                if (currentItem.getItemMeta().hasDisplayName())
                    lore.add(currentItem.getType() + " x " + currentItem.getAmount() + " name: " + currentItem.getItemMeta().getDisplayName());
                else
                    lore.add(currentItem.getType() + " x " + currentItem.getAmount());
            }

            meta.setLore(lore);

            // save kit in myKits
            String s = "savedKits." + p.getUniqueId() + "." + id;
            c.set(s + ".owner", uuid.toString());
            c.set(s + ".kitName", name);
            c.set(s + ".lore", lore);
            for (int i = 0; i < inv.getSize(); i++) {
                if (inv.getItem(i) == null) continue;

                c.set(s + ".contents." + i, inv.getItem(i));
            }

            MicroKits.getInstance().saveConfig();

            newKit.setItemMeta(meta);

            // set nbt tags
            NBTItem nbti = new NBTItem(newKit);
            // replace with microKits
            nbti.setBoolean("microKits", true);
            nbti.setInteger("id", id);
            nbti.setUUID("owner", uuid);
            //nbti.setString("display", null); deletes the display name of item

            newKit = nbti.getItem();

            // gives item and opens gui to save new items
            p.getInventory().addItem(newKit);

            p.sendMessage(mm.getMessage("newKitSaved"));
        });

        gui.show(p);
    }

    /**
     * opens a gui to allow a player to preview a kit
     * if kit has no id send message error
     */
    private void previewKitGui(Player p){
        ItemStack itemInHand = p.getInventory().getItemInMainHand();
        NBTItem nbtItem = new NBTItem(itemInHand);
        if (!nbtItem.hasKey("id")){
            p.sendMessage(mm.getMessage("errorPreviewKit"));
            return;
        }
        int kitID = nbtItem.getInteger("id");

        // create gui and add items in pane
        ChestGui gui = new ChestGui(6, "previewing kit id: " + kitID);

        gui.setOnGlobalClick(event -> event.setCancelled(true));

        //OutlinePane  pane = new OutlinePane(0,0,9,6);
        StaticPane pane = new StaticPane(0,0,9,6);

        if (c.getConfigurationSection(kitID + ".contents") == null){
            p.sendMessage(mm.getMessage("errorPreviewKitEmpty"));
            return;
        }

        for (String key : c.getConfigurationSection(kitID + ".contents").getKeys(false)){
            // key is the slot number as int
            // key: 11 -> y 1 x 2

            int x = 0;
            int y = 0;
            int keyInt = Integer.parseInt(key);

            while(keyInt > 8){
                y++;
                keyInt -= 9;
            }
            x = keyInt;

            pane.addItem(new GuiItem(Objects.requireNonNull(c.getItemStack(kitID + ".contents." + key))), x,y);
        }
        gui.addPane(pane);

        gui.show(p);
    }

    /**
     * allows the player to select a custom language in case the one defined by the server is not good
     */
    private void selectLanPlayer(Player p){
        ChestGui gui = new ChestGui(3, "Select your language");

        gui.setOnGlobalClick(event -> event.setCancelled(true));

        StaticPane pane = new StaticPane(0,0,9,3);

        // spanish
        pane.addItem(new GuiItem(new ItemStack(Material.STONE_BUTTON), inventoryClickEvent -> {
            // set player's language to spanish
            mm.setPlayerLan((Player) inventoryClickEvent.getWhoClicked(), "es");
            inventoryClickEvent.getWhoClicked().sendMessage(mm.getMessage("lanChanged"));

        }), 3,1);

        // english
        pane.addItem(new GuiItem(new ItemStack(Material.OAK_BUTTON), inventoryClickEvent -> {
            // set player's language to english
            mm.setPlayerLan((Player) inventoryClickEvent.getWhoClicked(), "en");
            inventoryClickEvent.getWhoClicked().sendMessage(mm.getMessage("lanChanged"));

        }), 5,1);

        gui.addPane(pane);

        gui.show(p);
    }

    /**
     * sets a default language and allows staff to define custom messages for each action
     */
    private void selectLanServer(Player p){
        ChestGui gui = new ChestGui(3, "Select language to edit messages");

        gui.setOnGlobalClick(event -> event.setCancelled(true));

        StaticPane pane = new StaticPane(0,0,9,3);

        // spanish
        pane.addItem(new GuiItem(new ItemStack(Material.STONE_BUTTON), inventoryClickEvent -> {
            // show list of messages and allow to edit each one

            Player player = (Player) inventoryClickEvent.getWhoClicked();
            showMessagesGui(player, "es");
        }), 3,1);

        // english
        pane.addItem(new GuiItem(new ItemStack(Material.OAK_BUTTON), inventoryClickEvent -> {
            // show list of messages and allow to edit each one

            Player player = (Player) inventoryClickEvent.getWhoClicked();
            showMessagesGui(player, "en");
        }), 5,1);

        gui.addPane(pane);

        gui.show(p);
    }

    private void showMessagesGui(Player p , String lan){
        ChestGui gui = new ChestGui(6, "editing messages");

        gui.setOnGlobalClick(event -> event.setCancelled(true));

        OutlinePane pane = new OutlinePane(0,0,9,6);

        List<String> messages = mm.getAllLanMessages(lan);
        List<String> displays = mm.getAllLanDisplays(lan);
        List<String> keys = mm.getAllKeysOfLanAsList(lan);

        assert messages.size() > 0 && displays.size() > 0;
        assert messages.size() == displays.size();

        for (int i = 0; i < messages.size(); i++) {
            int finalI = i;
            pane.addItem(new GuiItem(itemStackBuilder(Material.PAPER, displays.get(i), messages.get(i)), event -> {
                // listen for player's next message with int id 1
                // add player to playerList with id 1
                Player player = (Player) event.getWhoClicked();
                pendingPlayersInChat.put(player, 1);

                // add player to another hashmap lanCache <Player, [String Lan, String key]> for lan selection
                ArrayList<String> data = new ArrayList<>();
                data.add(lan);
                data.add(keys.get(finalI));

                pendingMessageChangeInChat.put(player, data);

                player.closeInventory();

                player.sendMessage(mm.getMessage("newMessageSet"));

                // on message send: mm.setMessageOfLan(lanCache.get(player)[0], lanCache.get(player)[1], messageSent);

            }));
        }

        gui.addPane(pane);

        gui.show(p);
    }

    private ItemStack itemStackBuilder(Material mat, String name, String lore){
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        assert meta != null;
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        meta.setLore(Collections.singletonList(ChatColor.translateAlternateColorCodes('&', lore)));

        item.setItemMeta(meta);

        return item;
    }

    @EventHandler
    private void listenPendingMessage(AsyncPlayerChatEvent e){
        // set name of kit id 0
        // set message id 1
        // set title id 2

        if (!pendingPlayersInChat.containsKey(e.getPlayer())) return;

        // set message to name of kit
        e.setCancelled(true);
        Player p = e.getPlayer();

        switch (pendingPlayersInChat.get(p)) {
            case 0:
                Bukkit.getScheduler().runTask(MicroKits.getInstance(), () -> {
                    // once player sends message open new kit gui
                    newKitGui(e.getPlayer(), e.getMessage());
                });
                break;
            case 1:
                // setting message in config file
                mm.setMessageOfLan(pendingMessageChangeInChat.get(p).get(0), pendingMessageChangeInChat.get(p).get(1), e.getMessage());

                p.sendMessage("action " + pendingMessageChangeInChat.get(p).get(1) + " message set to " + e.getMessage());

                pendingMessageChangeInChat.remove(p);
                break;
            case 2:
                // setting title in config file
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + pendingPlayersInChat.get(p));
        }

        pendingPlayersInChat.remove(p);
    }

    /**
     * added here since its mostly part of the gui
     */
    @EventHandler
    public void onRightClickEmptyKit(PlayerInteractEvent e){
        if (!Objects.requireNonNull(e.getHand()).equals(EquipmentSlot.HAND)) return;
        if (!e.getAction().equals(Action.RIGHT_CLICK_AIR)) return;

        ItemStack item = e.getPlayer().getInventory().getItemInMainHand();
        if (!item.getType().equals(Material.COMPASS)) return;

        if (!e.getPlayer().hasPermission("microkits.newEmptyKit.claim")){
            e.getPlayer().sendMessage(mm.getMessage("noPermToClaim"));
            return;
        }

        // check if player is already creating a kit
        if (pendingPlayersInChat.containsKey(e.getPlayer())){
            e.getPlayer().sendMessage(mm.getMessage("errorAlreadyCreatingKit"));
            return;
        }

        NBTItem nbtItem = new NBTItem(item);
        // replace with microKits
        if (!nbtItem.hasKey("microKits")) return;

        e.getPlayer().sendMessage(mm.getMessage("nameOfKitInChat"));
        // listen for message
        pendingPlayersInChat.put(e.getPlayer(), 0);

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

    private Integer getAndUpdateNextKitUUID(){
        int id = c.getInt("nextUniqueID");
        c.set("nextUniqueID", id + 1);
        return id;

    }
}