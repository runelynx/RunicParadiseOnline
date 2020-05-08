package io.github.runelynx.runicparadise.faith.modules;

import io.github.runelynx.runicparadise.RunicParadise;
import io.github.runelynx.runicparadise.faith.FaithCore;
import io.github.runelynx.runicuniverse.RunicMessaging;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerDropItemEvent;

import java.util.Set;

import static io.github.runelynx.runicparadise.faith.FaithCore.*;
import static org.bukkit.Bukkit.getServer;

public class SacrificialPit {

    private Boolean active = false;

    public SacrificialPit() {
        getServer().getConsoleSender().sendMessage(ChatColor.RED + "[FAITH STARTUP] PIT: Activating ...");
        if (activate()) {
            this.active = true;
            getServer().getConsoleSender().sendMessage(ChatColor.RED + "[FAITH STARTUP] PIT: Activation complete!");
        } else {
            getServer().getConsoleSender().sendMessage(ChatColor.RED + "[FAITH STARTUP] PIT: *FAILURE* Unknown issue loading sacrificial pit data");
            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&4Failed to load Faith Module: Sacrificial Pit"));
        }
    }

    private Boolean activate() {

        if (this.active) {
            //Must deactivate first if you want to activate again
            return true;
        }

        registerPitLocations();
        registerPitItems();
        registerPitSettings();

        if (faithCorePitLocations.size() != 2) {
            getServer().getConsoleSender().sendMessage(ChatColor.RED + "[FAITH STARTUP] PIT: *FAILURE* Could not load the 2 pit locations from config");
            return false;
        }

        getServer().getConsoleSender().sendMessage(ChatColor.RED + "[FAITH STARTUP] PIT: Successfully loaded pit locations");

        this.active = true;
        return true;
    }

    public Boolean reactivate() {
        if (this.active) {
            // Module is running - so deactivate first
            if (deactivate()) {
                if (activate()) {
                    return true;
                }
            }
        } else {
            // Module is not running, so just activate
            if (activate()) {
                return true;
            }
        }
        return false;
    }

    public Boolean deactivate() {

        this.active = false;
        return true;
    }

    private void registerPitLocations() {
        ConfigurationSection summonLocSection = FaithCore.getFaithConfig().getConfigurationSection("Faith.SacrificialPit.PitDropZone");

        faithCorePitLocations.put("PitPos1", new Location(
                Bukkit.getWorld(summonLocSection.getString("World")),
                summonLocSection.getInt("X1"),
                summonLocSection.getInt("Y1"),
                summonLocSection.getInt("Z1")));

        faithCorePitLocations.put("PitPos2", new Location(
                Bukkit.getWorld(summonLocSection.getString("World")),
                summonLocSection.getInt("X2"),
                summonLocSection.getInt("Y2"),
                summonLocSection.getInt("Z2")));
    }

    private Boolean registerPitSettings() {
        ConfigurationSection pitSettingsSection = FaithCore.getFaithConfig().getConfigurationSection("Faith.SacrificialPit.Settings");
        Set<String> pitSettingsConfigList = pitSettingsSection.getKeys(false);

        // Loop thru items
        for (String pitSettingsKey : pitSettingsConfigList) {
            faithCorePitSettings.put(
                    pitSettingsKey,
                    pitSettingsSection.getString(pitSettingsKey));

            //Debug
            getServer().getConsoleSender().sendMessage(ChatColor.RED + "[FAITH STARTUP] PIT: Adding Pit Setting: " + pitSettingsKey + ": " + pitSettingsSection.getString(pitSettingsKey));
        }

        return true;
    }

    private Boolean registerPitItems() {
        ConfigurationSection pitDropRewardsSection = FaithCore.getFaithConfig().getConfigurationSection("Faith.SacrificialPit.DropRewards");
        Set<String> pitDropRewardsConfigList = pitDropRewardsSection.getKeys(false);
        int catCount =0;
        int itemCount =0;

        // Loop thru list of categories underneath DropRewards section
        for (String pitCategoryKey : pitDropRewardsConfigList) {

            // Get list of items underneath each category
            ConfigurationSection pitItemSection = FaithCore.getFaithConfig().getConfigurationSection("Faith.SacrificialPit.DropRewards." + pitCategoryKey + ".Items");
            Set<String> pitItemConfigList = pitItemSection.getKeys(false);

            // Loop thru items
            for (String pitItemKey : pitItemConfigList) {
                faithCorePitItems.put(
                        Material.valueOf(pitItemKey),
                        Integer.valueOf(pitItemSection.getInt(pitItemKey)));

                faithCorePitItemCategories.put(
                        Material.valueOf(pitItemKey),
                        pitCategoryKey
                );

                //Debug
                getServer().getConsoleSender().sendMessage(ChatColor.RED + "[FAITH STARTUP] PIT: Adding Pit item: " +
                                pitCategoryKey + ": " + pitItemKey + ": " + pitItemSection.getInt(pitItemKey));

                itemCount++;
            }

            catCount++;

        }

        getServer().getConsoleSender().sendMessage(ChatColor.RED + "[FAITH STARTUP] PIT: Loaded " + catCount + " item categories and " + itemCount + " items");
        return true;

    }

    public static boolean handlePlayerDropItemEvent (PlayerDropItemEvent event) {

        Location loc = event.getPlayer().getLocation();
        if (loc.getWorld().equals(faithCorePitLocations.get("PitPos1").getWorld())
                && loc.getX() > faithCorePitLocations.get("PitPos1").getX()
                && loc.getY() > faithCorePitLocations.get("PitPos1").getY()
                && loc.getZ() > faithCorePitLocations.get("PitPos1").getZ()
                && loc.getX() > faithCorePitLocations.get("PitPos2").getX()
                && loc.getY() > faithCorePitLocations.get("PitPos2").getY()
                && loc.getZ() > faithCorePitLocations.get("PitPos2").getZ()) {


            Player p = event.getPlayer();

            // Player has dropped an item in the sacrificial pit!
            if (!faithCorePitItems.containsKey(event.getItemDrop().getItemStack().getType())) {
                // Player has dropped an item that is not in the Pit's list
                RunicMessaging.sendMessage(
                        p,
                        RunicMessaging.RunicFormat.EMPTY,
                        ChatColor.translateAlternateColorCodes('&', faithCorePitSettings.get("SacrificeRefusalMessage"))
                );

                event.setCancelled(true);
            } else if (event.getItemDrop().getItemStack().getItemMeta() != null
                    && event.getItemDrop().getItemStack().getItemMeta().getLore() != null) {
                // Player has dropped an item that has lore
                RunicMessaging.sendMessage(
                        p,
                        RunicMessaging.RunicFormat.EMPTY,
                        ChatColor.translateAlternateColorCodes('&', faithCorePitSettings.get("SacrificeRefusalLoreMessage"))
                );
            } else {
                // Player has dropped an item that IS on the Pit's list

                Material mat = event.getItemDrop().getItemStack().getType();
                int amount = event.getItemDrop().getItemStack().getAmount();

                RunicParadise.playerProfiles.get(p.getUniqueId()).processPitContribution(
                        faithCorePitItems.get(mat) * amount
                );

            }
        }

            //
        return true;
    }
}
