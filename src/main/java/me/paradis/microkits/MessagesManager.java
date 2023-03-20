package me.paradis.microkits;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * manages messages and language
 *
 * support:
 * en - english default
 * es - spanish
 *
 * structure idea:
 * idea:
 *  en.newKit{ displayName: "new kit message", message: "you were given a new kit"}
 *  es.newKit{ displayName: "mensaje al dar kit", message: "se te agrego un nuevo kit"}
 *
 *  function getMessage(lan, path) return c.get(lan + . + path + .message
 *
 * getMessage("newKitAddedMessage") if lang == en: get message in "en.newKitAddedMessage"
 *  else if lang == es:
 */
public class MessagesManager {

    private final FileConfiguration c = MicroKits.getInstance().getConfig();

    /**
     * sets the default language of the server
     */
    public void setLan(String lan){
        c.set("lan", lan);
    }

    /**
     * sets the language of a specific player
     */
    public void setPlayerLan(Player player, String lan){
        c.set("playerLan." + player.getUniqueId(), lan);
    }

    /**
     * gets all messages of a specific language
     * @return a list of
     */
    public Collection<String> getAllKeysOfLan(String lan){
        return Objects.requireNonNull(c.getConfigurationSection("serverMessages." + lan)).getKeys(false);
    }

    public ArrayList<String> getAllKeysOfLanAsList(String lan){
        return new ArrayList<>(getAllKeysOfLan(lan));
    }

    // get all messages
    public ArrayList<String> getAllLanMessages(String lan){
        ArrayList<String> messages = new ArrayList<>();

        for (String key : getAllKeysOfLan(lan)){
            messages.add(c.getString("serverMessages." + lan + "." + key + ".message"));
        }

        return messages;
    }

    // get all display names
    public ArrayList<String> getAllLanDisplays(String lan){
        ArrayList<String> displays = new ArrayList<>();

        for (String key : getAllKeysOfLan(lan)){
            displays.add(c.getString("serverMessages." + lan + "." + key + ".display"));
        }

        return displays;
    }

    public String getMessage(String path){
        // lan is default value unless player has custom lan set
        String lan = c.getString("lan");
        // add check for player

        String prefix = c.getString("prefix");

        // check if path is null and remove objects.require non null

        String message = prefix + " " + c.getString("serverMessages." + lan + "." + path + ".message");

        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * sets a message of a language
     * @param message new message to be set
     */
    public void setMessageOfLan(String lan,String messageToEdit, String message){
        c.set("serverMessages." + lan + "." + messageToEdit + ".message", message);
    }

    public void setDisplayOfLan(String lan, String messageToEdit, String display){
        c.set("serverMessages." + lan + "." + messageToEdit + ".display", display);
    }


}
