package io.github.runelynx.runicparadise.faith.modules;

import io.github.runelynx.runicparadise.RunicUtilities;
import io.github.runelynx.runicparadise.faith.FaithCore;
import io.github.runelynx.runicparadise.faith.SummoningDropChance;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.v1_15_R1.generator.InternalChunkGenerator;
import org.bukkit.entity.EntityType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import static io.github.runelynx.runicparadise.faith.FaithCore.*;

public class SacrificialPit {

    private Boolean active = false;

    public SacrificialPit() {
        if (activate()) {
            this.active = true;
        } else {
            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&4Failed to load Faith Module: Sacrificial Pit"));
        }
    }

    private Boolean activate() {

        if (this.active) {
            //Must deactivate first if you want to activate again
            return true;
        }

        Bukkit.getLogger().log(Level.INFO, "~~~ Activating faith module - sacrificial pit ~~~");

        registerPitLocations();
        registerPitItems();

        if (faithCorePitLocations.size() != 2) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&4Could not register all 2 Pit Locations"));
            return false;
        }

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

        faithCorePitLocations.clear();
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

    private Boolean registerPitItems() {
        ConfigurationSection pitDropRewardsSection = FaithCore.getFaithConfig().getConfigurationSection("Faith.SacrificialPit.DropRewards");
        Set<String> pitDropRewardsConfigList = pitDropRewardsSection.getKeys(false);

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
                Bukkit.getLogger().log(
                        Level.INFO,
                        "Adding Pit Item: " + pitCategoryKey + ": " + pitItemKey + ": " + pitItemSection.getInt(pitItemKey)
                );
            }

        }

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

        }
            // Player has dropped an item in the sacrificial pit!

            //
        return true;
    }
}
