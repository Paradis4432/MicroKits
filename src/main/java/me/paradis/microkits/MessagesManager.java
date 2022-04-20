package me.paradis.microkits;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

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

    private FileConfiguration c = MicroKits.getInstance().getConfig();

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

    // get all messages

    // get all display names

    public String getMessage(String lan, String path){
        return c.getString("serverMessages." + lan + "." + path + ".message");
    }

    /**
     * sets a message of a language
     * @param lan
     * @param messageToEdit
     * @param message new message to be set
     */
    public void setMessageOfLan(String lan,String messageToEdit, String message){

    }

}
