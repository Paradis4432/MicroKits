package me.paradis.microkits;

import de.tr7zw.nbtapi.NBTItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public class RightClickPaperManager implements Listener {

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

        if (!nbti.getBoolean("full")){
            // open new kit gui
            new GuiManager().newKitGui(p);

        } else {
            // take items from config and give to player
        }
    }
}
