package me.paradis.microkits;

import de.tr7zw.nbtapi.NBTItem;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class nbtCommandManager implements CommandExecutor {
    private FileConfiguration c = MicroKits.getInstance().getConfig();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Player p = (Player) sender;
        if (args[0].equalsIgnoreCase("add")){
            ItemStack item = p.getInventory().getItemInMainHand();
            p.getInventory().remove(item);

            NBTItem nbtItem = new NBTItem(item);

            nbtItem.setString(args[1], args[2]);
            item = nbtItem.getItem();

            p.getInventory().addItem(item);

        } else if (args[0].equalsIgnoreCase("check")){
                NBTItem nbti = new NBTItem(p.getInventory().getItemInMainHand());
                p.sendMessage(String.valueOf(nbti.getKeys()));
        } else if (args[0].equalsIgnoreCase("get")){
            ItemStack item = c.getItemStack("uniqueKitID.contents" + args[1]);
            p.getInventory().addItem(item);
        }



        return false;
    }
}
