package me.paradis.microkits;

import de.tr7zw.nbtapi.NBTItem;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public class RightClickPaperManager implements Listener {

    private FileConfiguration c = MicroKits.getInstance().getConfig();

    /**
     * handles right click of paper, if kit is full, gives items, otherwise open gui to set
     */
    @EventHandler
    public void onRightClickPaper(PlayerInteractEvent e){
        if (!Objects.requireNonNull(e.getHand()).equals(EquipmentSlot.HAND)) return;
        if (!e.getAction().equals(Action.RIGHT_CLICK_AIR)) return;

        ItemStack item = e.getPlayer().getInventory().getItemInMainHand();

        if (!item.getType().equals(Material.PAPER)) return;
        Player p = e.getPlayer();

        NBTItem nbti = new NBTItem(item);

        if (!nbti.hasKey("microKitsPaper")) return;

        int id = nbti.getInteger("id");
        System.out.println("id is " + id);
        // check if player has enough space for kit

        // take items from config and give to player
        Objects.requireNonNull(c.getConfigurationSection(id + ".contents")).getKeys(false).forEach(key -> {
            System.out.println("adding item: ");
            System.out.println(c.getItemStack(id + ".contents." + key));

            p.getInventory().addItem(c.getItemStack(id + ".contents." + key));
        });

        e.setCancelled(true);
        p.getInventory().remove(item);
        System.out.println("item removed");
        p.sendMessage("you have claimed a kit");
    }
}
