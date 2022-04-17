package me.paradis.microkits;

import de.tr7zw.nbtapi.NBTItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Objects;
import java.util.StringJoiner;

public class CommandManager implements CommandExecutor {

    private FileConfiguration c = MicroKits.getInstance().getConfig();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)){
            sender.sendMessage("cant use this command from console");
            return false;
        }

        if (args.length == 0) return false;

        Player p = (Player) sender;

        if (args.length == 1){
            return false;
        }

        if (args.length > 1){
            if (args[0].equalsIgnoreCase("new")){

                // if player has enough space add item, otherwise cancel here

                // get the new name for the kit
                StringJoiner joiner = new StringJoiner(" ");
                for (int i = 1; i < args.length; i++) {
                    joiner.add(args[i]);
                }
                String name = joiner.toString();

                // create the item
                ItemStack newKit = new ItemStack(Material.PAPER);
                ItemMeta meta = newKit.getItemMeta();

                Objects.requireNonNull(meta).setDisplayName(ChatColor.translateAlternateColorCodes('&', name));

                newKit.setItemMeta(meta);

                // set nbt tags
                NBTItem nbti = new NBTItem(newKit);
                nbti.setBoolean("microKitsPaper", true);

                int id = getUniqueIDForKit();
                nbti.setInteger("id", id);
                updateUniqueId();
                nbti.setUUID("owner", p.getUniqueId());
                //nbti.setString("display", null); deletes the display name of item

                newKit = nbti.getItem();

                // gives item and opens gui to save new items
                p.getInventory().addItem(newKit);
                new GuiManager().newKitGui(p, id, p.getUniqueId());

                p.sendMessage("you were given a new kit");
                return true;
            }
        }

        return true;
    }

    public Integer getUniqueIDForKit(){
        return c.getInt("nextUniqueID");
    }

    public void updateUniqueId(){
        c.set("nextUniqueID", ((c.getInt("nextUniqueID")) + 1));
    }
}
