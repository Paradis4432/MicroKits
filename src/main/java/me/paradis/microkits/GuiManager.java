package me.paradis.microkits;

import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class GuiManager {


    public void newKitGui(Player p){
        ChestGui gui = new ChestGui(6, "Close to save new kit");

        gui.setOnClose(event -> {
            Inventory inv = gui.getInventory();
            // save inv.getContents
            System.out.println("saving inv in config");
        });

        gui.show(p);
    }

}
