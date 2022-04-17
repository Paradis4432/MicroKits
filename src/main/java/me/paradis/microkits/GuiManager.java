package me.paradis.microkits;

import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class GuiManager {

    private FileConfiguration c = MicroKits.getInstance().getConfig();

    public void newKitGui(Player p){
        ChestGui gui = new ChestGui(6, "Close to save new kit");

        gui.setOnClose(event -> {
            Inventory inv = gui.getInventory();
            // save inv.getContents
            for (int i = 0; i < inv.getSize(); i++) {
                if (inv.getItem(i) == null) continue;
                c.set("uniqueKitID.contents" + i, inv.getItem(i));
                MicroKits.getInstance().saveConfig();
                System.out.println("inv saved");
            }
        });

        gui.show(p);
    }

}
