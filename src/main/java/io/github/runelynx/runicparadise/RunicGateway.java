package io.github.runelynx.runicparadise;

import java.util.Arrays;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import static org.bukkit.Bukkit.getLogger;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

public class RunicGateway {
    private Plugin instance = RunicParadise.getInstance();

    public RunicGateway() {}

    public static int checkPlayerInventoryForItemDataCount(Player player, int id, int dataValue) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] items = inventory.getContents();
        int has = 0;
        for (ItemStack item : items) {
            if ((item != null) && (item.getTypeId() == id) && (item.getDurability() == dataValue) && (item.getAmount() > 0)) {
                has += item.getAmount();

            }
        }
        return has;
    }

    public static int removePlayerInventoryItemData(Player player, int id, int dataValue) {
        PlayerInventory inventory = player.getInventory();
        return inventory.clear(id, dataValue);
    }

    public static void givePlayerItemData(Player player, int count, int id, int dataValue, String displayName, String lore1, String lore2, String lore3) {
        ItemStack newItem = new ItemStack(id, count, (short) dataValue);
        ItemMeta meta = newItem.getItemMeta();
        meta.setDisplayName(displayName);
        meta.setLore(Arrays.asList(lore1, lore2, lore3));
        newItem.setItemMeta(meta);
        PlayerInventory inventory = player.getInventory();
        inventory.addItem(newItem);
    }

    public static Entity getLastEntityDamager(Entity entity) {
        EntityDamageEvent event = entity.getLastDamageCause();
        if (event != null && !event.isCancelled() && (event instanceof EntityDamageByEntityEvent)) {
            Entity damager = ((EntityDamageByEntityEvent) event).getDamager();
            if (damager instanceof Projectile) {
                Object shooter = ((Projectile) damager).getShooter();
                if (shooter instanceof Entity) {
                    getLogger().log(Level.SEVERE, "[RP] Trying to log player death SHOOTER: " + shooter.toString());
                    return (Entity) shooter;
                }
            }
            // Add other special cases if necessary
            getLogger().log(Level.SEVERE, "[RP] Trying to log player death DAMAGER: " + damager.toString());
            return damager;
        }
        getLogger().log(Level.INFO, "[RP] Trying to log player death... but returned null (or death not caused by entity)");
        return null;
    }

    //send message to players
    //playerName is not used if toAllPlayers= true
    public void sendMessage(boolean toAllPlayers, String playerName, String Message) {
        if (toAllPlayers) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage(Message);
            }

        } else {
            Bukkit.getPlayer(playerName).sendMessage(Message);
        }
    }

    //send message to console
    public void sendMessage(String Message) {
        Bukkit.getLogger().log(Level.INFO, Message);
    }

    //play sound to players
    //playerName is not used if toAllPlayers= true
    public void playPortalTravelSound(boolean toAllPlayers, String playerName) {

        if (toAllPlayers) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_FIREWORK_LAUNCH, 1, 0);
            }
        } else {
            Bukkit.getPlayer(playerName).getWorld().playSound(Bukkit.getPlayer(playerName).getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 1, 0);
        }
    }

}
