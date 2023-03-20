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
import java.util.concurrent.atomic.AtomicInteger;

public class RightClickPaperManager implements Listener {

    private final FileConfiguration c = MicroKits.getInstance().getConfig();
    private final MessagesManager mm = new MessagesManager();

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

        if (!nbti.hasKey("microKits")) return;

        int id = nbti.getInteger("id");

        // check if kit is null in config
        if (c.get(String.valueOf(id)) == null){
            p.sendMessage(mm.getMessage("kitNotFound"));
            return;
        }

        // check if player has enough space for kit
        boolean overFilled = false;

        // take items from config and give to player
        for (String key : c.getConfigurationSection(id + ".contents").getKeys(false)) {
            ItemStack itemStack = c.getItemStack(id + ".contents." + key);

            // removes nbt tag added by inventory framework
            if (itemStack == null) continue;
            NBTItem nbtItem = new NBTItem(itemStack);
            nbtItem.removeKey("PublicBukkitValues");
            itemStack = nbtItem.getItem();

            if (p.getInventory().firstEmpty() == -1) {
                // no more free slots
                overFilled = true;
                // stash extra items
                c.set("stashed." + p.getUniqueId() + "." + key, itemStack);

            } else p.getInventory().addItem(itemStack);
        }

        // remove kit from config
        c.set(String.valueOf(id), null);


        if (overFilled) p.sendMessage(mm.getMessage("overfilledPlayerInv"));

        e.setCancelled(true);
        p.getInventory().remove(item);
        p.sendMessage(mm.getMessage("kitClaimed"));
    }
}
