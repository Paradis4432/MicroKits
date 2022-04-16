package me.paradis.microkits;

import de.tr7zw.nbtapi.NBTItem;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.StringJoiner;
import java.util.UUID;

public class CommandManager implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)){
            sender.sendMessage("cant use this command from console");
            return false;
        }

        if (args.length == 0) return false;

        Player p = (Player) sender;

        if (args.length == 1){
            if (args[0].equalsIgnoreCase("edit")){
                // open new edit kit gui with id from nbt tag
                p.sendMessage("opening edit kit gui");
                return true;
            }
        }

        if (args.length > 1){
            if (args[0].equalsIgnoreCase("new")){

                // get the new name for the kit
                StringJoiner joiner = new StringJoiner(" ");
                for (int i = 1; i < args.length; i++) {
                    joiner.add(args[i]);
                }
                String name = joiner.toString();

                // create the item
                ItemStack newKit = new ItemStack(Material.PAPER);
                ItemMeta meta = newKit.getItemMeta();

                meta.setDisplayName(name);

                newKit.setItemMeta(meta);

                // set nbt tags
                NBTItem nbti = new NBTItem(newKit);
                nbti.setBoolean("microKitsPaper", true);
                nbti.setInteger("id", getNextUniqueID());
                nbti.setUUID("owner", p.getUniqueId());
                nbti.setBoolean("full", false);

                newKit = nbti.getItem();

                p.getInventory().addItem(newKit);
                p.sendMessage("you were given a new kit");
                return true;
            }
        }

        return true;
    }

    public Integer getNextUniqueID(){
        return 2;
    }
}
