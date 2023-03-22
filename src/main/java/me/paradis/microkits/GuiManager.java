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

    private final FileConfiguration c = MicroKits.getInstance().getConfig();
    private final MessagesManager mm = new MessagesManager();

    /**
     * test
     * hashmap<Player, int> made to listen for player message
     * if int = 0: listening for kit name
     * if int = 1: listening for new message for config
     * if int = 2: listening for new title for config
     */
    //private ArrayList<Player> playerList = new ArrayList<>();
    private final HashMap<Player, Integer> pendingPlayersInChat = new HashMap<>();
    private final HashMap<Player, ArrayList<String>> pendingMessageChangeInChat = new HashMap<>();
    private final HashMap<Player, Long> cooldowns = new HashMap<>();


    /**
     * main menu to handle details
     */
    public void openMainGui(Player p) {
        ChestGui gui = new ChestGui(6, "Choose what to do here!");

        gui.setOnGlobalClick(event -> {
            event.setCancelled(true);
        });

        StaticPane pane = new StaticPane(0,0,9,6);

        // new kit
        if (p.hasPermission("microkits.newKit")){
            pane.addItem(new GuiItem(itemStackBuilder(Material.PAPER, "&6&lCreate New Kit"), inventoryClickEvent -> {
                Player playerEvent = (Player) inventoryClickEvent.getWhoClicked();

                // if player is in cooldown return mm.playerInCooldown
                if (cooldowns.containsKey(playerEvent) && ((cooldowns.get(playerEvent) + getConfigCooldown()) >= (System.currentTimeMillis() / 1000))){
                    // player is in cooldown cancel creation
                    playerEvent.sendMessage(mm.getMessage("playerInCooldown"));
                    return;
                }
                // remove player from cooldown
                cooldowns.remove(playerEvent);

                // send player message "enter new name of kit"
                playerEvent.sendMessage(mm.getMessage("nameOfKitInChat"));

                // close gui, save player in list
                playerEvent.closeInventory();
                pendingPlayersInChat.put(playerEvent, 0);

            }), 1,2);
        }

        // view my kits
        if (p.hasPermission("microkits.viewKits")){
            pane.addItem(new GuiItem(itemStackBuilder(Material.BOOK, "&6&lView Your Kits"), inventoryClickEvent -> {
                showPlayerKits((Player) inventoryClickEvent.getWhoClicked());
            }), 3,2);

        }

        // new empty kit
        if (p.hasPermission("microkits.newEmptyKit.create")){
            pane.addItem(new GuiItem(itemStackBuilder(Material.COMPASS, "&6&lCreate New Empty Kit"), inventoryClickEvent -> {
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
            pane.addItem(new GuiItem(itemStackBuilder(Material.ENDER_CHEST, "&6&lClaim Stashed Items"), inventoryClickEvent -> {
                if (c.getConfigurationSection("stashed." + p.getUniqueId()) == null){
                    p.sendMessage(mm.getMessage("noStashedItems"));
                    return;
                }
                //System.out.println(c.getConfigurationSection("stashed." + p.getUniqueId()).getKeys(false).size());
                if (c.getConfigurationSection("stashed." + p.getUniqueId()).getKeys(false).size() == 0){
                    p.sendMessage(mm.getMessage("noStashedItems"));
                    return;
                }
                // give player stashed items
                Objects.requireNonNull(c.getConfigurationSection("stashed." + p.getUniqueId())).getKeys(false).forEach(key -> {

                    if (p.getInventory().firstEmpty() == -1) return;

                    p.getInventory().addItem(c.getItemStack("stashed." + p.getUniqueId() + "." + key));
                    c.set("stashed." + p.getUniqueId() + "." + key, null);
                    p.sendMessage(mm.getMessage("claimedStashedItems"));

                });


            }), 0, 5);
        }

        // player language
        if (p.hasPermission("microkits.playerLan")){
            pane.addItem(new GuiItem(itemStackBuilder(Material.REDSTONE_TORCH, "&5Change Language"), inventoryClickEvent -> {
                selectLanPlayer((Player) inventoryClickEvent.getWhoClicked());
            }), 7, 2);
        }

        // server language and messages
        if (p.hasPermission("microkits.serverLan")){
            pane.addItem(new GuiItem(itemStackBuilder(Material.REDSTONE_BLOCK, "&5Edit Server Messages"), inventoryClickEvent -> {
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

            // key is id
            pane.addItem(new GuiItem(item, inventoryClickEvent -> {
                // remove the items from the player
                // add to active kits
                // give paper to player

                //c.getConfigurationSection(s + "." + key + ".contents").getKeys(false);

                boolean containsAll = true;
                for (String itemID : c.getConfigurationSection(s + "." + key + ".contents").getKeys(false)){
                    ItemStack i = c.getItemStack(s + "." + key + ".contents." + itemID);

                    containsAll = p.getInventory().containsAtLeast(i, i.getAmount()) && containsAll;

                    // System.out.println(containsAll);
                }

                if (containsAll){
                    // take items and give kit
                    int id = getAndUpdateNextKitUUID();
                    UUID uuid = p.getUniqueId();

                    c.set(id + ".owner", uuid.toString());
                    c.set(id + ".kitName", c.getString(s + "." + key + ".kitName"));
                    c.set(id + ".id", c.getString(s + "." + key + ".id"));

                    // create the item
                    ItemStack newKit = new ItemStack(Material.PAPER);
                    ItemMeta newKitMeta = newKit.getItemMeta();

                    // add enchant
                    newKitMeta.addEnchant(Enchantment.LUCK, 1, true);
                    newKitMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

                    // set display name
                    Objects.requireNonNull(newKitMeta).setDisplayName(ChatColor.translateAlternateColorCodes('&', c.getString(s + "." + key + ".kitName")));

                    // set lore to id and list of items
                    ArrayList<String> newKitLore = new ArrayList<>();

                    newKitLore.add("Kit ID: " + id);

                    //replace with inv.getContents?
                    for (String itemID : c.getConfigurationSection(s + "." + key + ".contents").getKeys(false)){
                        ItemStack i = c.getItemStack(s + "." + key + ".contents." + itemID);

                        c.set(id + ".contents." + itemID, i);


                        // remove items from player inv

                        p.getInventory().removeItem(i);

                        assert i != null;
                        if (i.getItemMeta().hasDisplayName())
                            newKitLore.add(i.getType() + " x " + i.getAmount() + " name: " + i.getItemMeta().getDisplayName());
                        else
                            newKitLore.add(i.getType() + " x " + i.getAmount());
                    }

                    newKitMeta.setLore(newKitLore);

                    MicroKits.getInstance().saveConfig();

                    newKit.setItemMeta(newKitMeta);

                    // set nbt tags
                    NBTItem nbti = new NBTItem(newKit);
                    // replace with microKits
                    nbti.setBoolean("microKits", true);
                    nbti.setInteger("id", id);
                    nbti.setUUID("owner", uuid);
                    //nbti.setString("display", null); deletes the display name of item

                    newKit = nbti.getItem();

                    // adds player to cooldown
                    cooldowns.put(p, System.currentTimeMillis() / 1000);

                    // gives item and opens gui to save new items
                    p.getInventory().addItem(newKit);

                    // change message TODO
                    p.sendMessage(mm.getMessage("newKitSaved"));

                } else{
                    // send message not enough items
                }




            }));
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
            c.set(id + ".id", id);

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
            c.set(s + ".id", id);
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

            // adds player to cooldown
            cooldowns.put(p, System.currentTimeMillis() / 1000);

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
    private void previewKitGuiID(Player p, Integer kitID){


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

            int x;
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
        //pane.addItem(new GuiItem(new ItemStack(Material.STONE_BUTTON), inventoryClickEvent -> {
        //    // set player's language to spanish
        //    mm.setPlayerLan((Player) inventoryClickEvent.getWhoClicked(), "es");
        //    inventoryClickEvent.getWhoClicked().sendMessage(mm.getMessage("lanChanged"));

        //}), 3,1);

        // english

        pane.addItem(new GuiItem(itemStackBuilder(Material.OAK_BUTTON, "&7English"), inventoryClickEvent -> {
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
        //pane.addItem(new GuiItem(new ItemStack(Material.STONE_BUTTON), inventoryClickEvent -> {
        //    // show list of messages and allow to edit each one

        //    Player player = (Player) inventoryClickEvent.getWhoClicked();
        //    showMessagesGui(player, "es");
        //}), 3,1);

        // english
        pane.addItem(new GuiItem(itemStackBuilder(Material.OAK_BUTTON, "&7English"), inventoryClickEvent -> {
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

    private ItemStack itemStackBuilder(Material mat, String name){
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        assert meta != null;
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        item.setItemMeta(meta);

        return item;
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

    private void editKitGui(Player p, int kitId, int option){
        ChestGui gui = new ChestGui(6, "Editing kit " + kitId);

        // create new pane
        StaticPane pane = new StaticPane(0,0,9,6);

        if (option == 0) {
            // found items from active kits
            c.getConfigurationSection(kitId + ".contents").getKeys(false).forEach(s -> {
                // create new
                pane.addItem(new GuiItem(Objects.requireNonNull(c.getItemStack(kitId + ".contents." + s)), event -> {

                }),Integer.parseInt(s) % 9, Integer.parseInt(s) / 9);
            });
            gui.addPane(pane);

            gui.setOnClose(event -> {
                Inventory inv = gui.getInventory();

                // save inventory to config
                for (int i = 0; i < inv.getSize(); i++) {
                    ItemStack item = inv.getItem(i);
                    if (item == null || item.getType().isAir()) continue;
                    try {

                        // remove nbti tag PublicBukkitValues from item
                        NBTItem nbtItem = new NBTItem(item);
                        nbtItem.removeKey("PublicBukkitValues");
                        item = nbtItem.getItem();

                        c.set(kitId + ".contents." + i, item);
                        c.set("savedKits." + c.getString(kitId + ".owner") + "." + kitId + ".contents." + i, item);
                    } catch (Exception e) {
                        // handle exception here (e.g. log error message, show error to user)
                        System.out.println("error saving item " + i + " to config");
                    }
                }
            });
        } else if (option == 1) {
            // found kit in saved kits
            c.getConfigurationSection("savedKits." + p.getUniqueId() + "." + kitId + ".contents").getKeys(false).forEach(s -> {
                // create new
                pane.addItem(new GuiItem(Objects.requireNonNull(c.getItemStack("savedKits." + p.getUniqueId() + "." + kitId + ".contents." + s)), event -> {

                }),Integer.parseInt(s) % 9, Integer.parseInt(s) / 9);
            });
            gui.addPane(pane);

            gui.setOnClose(event -> {
                Inventory inv = gui.getInventory();

                // save inventory to config
                for (int i = 0; i < inv.getSize(); i++) {
                    ItemStack item = inv.getItem(i);
                    if (item == null || item.getType().isAir()) continue;
                    try {
                        NBTItem nbtItem = new NBTItem(item);
                        nbtItem.removeKey("PublicBukkitValues");
                        item = nbtItem.getItem();

                        c.set("savedKits." + p.getUniqueId() + "." + kitId + ".contents." + i, item);
                    } catch (Exception e) {
                        // handle exception here (e.g. log error message, show error to user)
                        System.out.println("error saving item " + i + " to config");
                    }
                }
            });
        }
        gui.show(p);
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
            if (args[0].equalsIgnoreCase("preview")){
                if (!p.hasPermission("microkits.preview")){
                    p.sendMessage(mm.getMessage("noPermToPreview"));
                    return true;
                }
                ItemStack itemInHand = p.getInventory().getItemInMainHand();

                if (itemInHand == null || itemInHand.getType().isAir())return true;

                NBTItem nbtItem = new NBTItem(itemInHand);
                if (!nbtItem.hasKey("id")){
                    p.sendMessage(mm.getMessage("errorPreviewKit"));
                    return true;
                }
                int kitID = nbtItem.getInteger("id");

                previewKitGuiID(p, kitID);
            } else if(args[0].equalsIgnoreCase("new")){
                if(!p.hasPermission("microkits.newKit")){
                    p.sendMessage(mm.getMessage("noPermToCreateKit"));
                    return true;
                }
                // if player is in cooldown return mm.playerInCooldown
                if (cooldowns.containsKey(p) && ((cooldowns.get(p) + getConfigCooldown()) >= (System.currentTimeMillis() / 1000))){
                    // player is in cooldown cancel creation
                    p.sendMessage(mm.getMessage("playerInCooldown"));
                    return true;
                }
                // remove player from cooldown
                cooldowns.remove(p);

                // send player message "enter new name of kit"
                p.sendMessage(mm.getMessage("nameOfKitInChat"));

                // close gui, save player in list
                p.closeInventory();
                pendingPlayersInChat.put(p, 0);
            }else if(args[0].equalsIgnoreCase("mykits")){
                if(!p.hasPermission("microkits.viewKits")){
                    p.sendMessage(mm.getMessage("noPermToViewKits"));
                    return true;
                }
                showPlayerKits(p);
            } else {
                p.sendMessage(ChatColor.GOLD + "command not found use /microkits for help");
            }
        } else {
            if (args[0].equalsIgnoreCase("setPrefix")){
                if (!p.hasPermission("microkits.setPrefix")) return false;

                StringBuilder prefix = new StringBuilder();
                for(int i = 1; i < args.length; i++){
                    prefix.append(" ").append(args[i]);
                }
                c.set("prefix", prefix.toString());
                p.sendMessage("new prefix set");
            } else if (args[0].equalsIgnoreCase("setcooldown")){
                if (!p.hasPermission("microkits.setCooldown")) return false;

                try {
                    Integer cooldown = Integer.parseInt(args[1]);
                    c.set("cooldown", cooldown);
                    p.sendMessage("new cooldown to create kit set to " + cooldown);

                } catch (NumberFormatException e){
                    p.sendMessage("usage: /microkits setcooldown [cooldown in seconds]");
                    return true;
                }
            } else if (args[0].equalsIgnoreCase("preview")){
                if (!p.hasPermission("microkits.preview")) return false;

                try {
                    Integer kitID = Integer.parseInt(args[1]);
                    previewKitGuiID(p, kitID);

                } catch (NumberFormatException e){
                    p.sendMessage(ChatColor.GOLD + "usage: /microkits preview [kit id]");
                    return true;
                }
            } else if (args[0].equalsIgnoreCase("editkit")){
                if (!p.hasPermission("microkits.admin")) return false;

                try {
                    Integer kitID = Integer.parseInt(args[1]);
                    // check if kitId exists
                    if (c.getConfigurationSection(kitID.toString()) != null){
                        editKitGui(p, kitID, 0);
                        return true;
                    } else if (c.getConfigurationSection("savedKits." + p.getUniqueId() + "." + kitID) != null) {
                        editKitGui(p, kitID, 1);
                        return true;
                    } else {
                        p.sendMessage("kit with id " + kitID + " does not exist");
                    }
                } catch (NumberFormatException e){
                    p.sendMessage(ChatColor.GOLD + "usage: /microkits editkit [kit id]");
                    return true;
                }
            } else {
                p.sendMessage(ChatColor.GOLD + "command not found use /microkits for help");
            }
        }
        return true;
    }

    private Integer getAndUpdateNextKitUUID(){
        int id = c.getInt("nextUniqueID");
        c.set("nextUniqueID", id + 1);
        return id;

    }

    private Integer getConfigCooldown(){
        if (c.get("cooldown") == null) {
            c.set("cooldown", 10);
            System.out.println("ERROR - microkits no cooldown found, setting to 10");
        }
        return (Integer) c.get("cooldown");
    }
}