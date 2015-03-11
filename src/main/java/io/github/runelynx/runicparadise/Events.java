/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.runelynx.runicparadise;

import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.data.DataException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Skeleton;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

/**
 *
 * @author Andrew
 */
public class Events {

    // pointer to your main class, not required if you don't need methods from the main class
    private Plugin instance = RunicParadise.getInstance();
    public static ArrayList<UUID> EVENT_NPCS = new ArrayList<UUID>();

    /**
     * Starts up the Spawnton Invasion event. Gets the starter NPC and trigger
     * switch ready for a player to engage. Event itself doesnt start until
     * engaged further.
     *
     * @param n/a nothing
     * @return nothing
     */
    public void spawntonInvasionStartup() {
        BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
        scheduler.scheduleSyncDelayedTask(instance, new Runnable() {
            @Override
            public void run() {
                // Spawn the Guard Captain NPC 86
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "npc spawn 86");

                // create trigger area
                File schematic = new File("/home/mcma/Minecraft/plugins/WorldEdit/schematics/SpawnInvasionTriggerArea.schematic");
                Vector loadCoords = new Vector(5608, 73, -126);
                World rp = Bukkit.getWorld("Runic Paradise");
                try {
                    loadArea(rp, schematic, loadCoords);
                } catch (Exception e) {
                    instance.getLogger().info("RP Error: exception while trying to loadArea in Events");
                }
            }
        }, 20L);
    }

    public void loadArea(World world, File file, Vector origin) throws DataException, IOException, MaxChangedBlocksException {
        try {
            EditSession es = new EditSession(new BukkitWorld(world), 999999999);
            CuboidClipboard cc = CuboidClipboard.loadSchematic(file);
            cc.paste(es, origin, false);
        } catch (Exception e) {
        }

    }

    /**
     * Stops the spawnton invasion event.
     *
     * @param n/a nothing
     * @return nothing
     */
    public void spawntonInvasionStop() {
        BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
        scheduler.scheduleSyncDelayedTask(instance, new Runnable() {
            @Override
            public void run() {
                // DESpawn the Guard Captain NPC 86
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "npc despawn 86");

                // reset the entire area
                File defaultWallArea = new File("/home/mcma/Minecraft/plugins/WorldEdit/schematics/SpawnInvasionDefault.schematic");
                Vector loadCoords = new Vector(5608, 73, -126);
                World rp = Bukkit.getWorld("Runic Paradise");
                try {
                    loadArea(rp, defaultWallArea, loadCoords);
                } catch (Exception e) {
                    instance.getLogger().info("RP Error: exception while trying to loadArea in Events");
                }
            }
        }, 20L);
    }

    /**
     * Triggers the spawnton invasion event.
     *
     * @param n/a nothing
     * @return nothing
     */
    public void spawntonInvasionTrigger() {

        BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
        scheduler.scheduleSyncDelayedTask(instance, new Runnable() {

            @Override
            public void run() {

                // reset the entire area to get the trigger area to a normal state
                File defaultWallArea = new File("/home/mcma/Minecraft/plugins/WorldEdit/schematics/SpawnInvasionDefault.schematic");
                Vector loadCoords = new Vector(5608, 73, -126);
                World rp = Bukkit.getWorld("Runic Paradise");
                try {
                    loadArea(rp, defaultWallArea, loadCoords);
                } catch (Exception e) {
                    instance.getLogger().info("RP Error: exception while trying to loadArea in Events");
                }
                // startup the bandit area
                File schematic = new File("/home/mcma/Minecraft/plugins/WorldEdit/schematics/SpawnInvasionBanditArea.schematic");
                try {
                    loadArea(rp, schematic, loadCoords);
                } catch (Exception e) {
                    instance.getLogger().info("RP Error: exception while trying to loadArea in Events");
                }
                Bukkit.getWorld("Runic Paradise").strikeLightningEffect(new Location(Bukkit.getWorld("Runic Paradise"), 5564, 72, -152));
                Bukkit.getWorld("Runic Paradise").createExplosion(new Location(Bukkit.getWorld("Runic Paradise"), 5564, 72, -152), 0);
            }
        }, 20L);
        scheduler.scheduleSyncDelayedTask(instance, new Runnable() {
            @Override
            public void run() {
                Bukkit.getWorld("Runic Paradise").strikeLightningEffect(new Location(Bukkit.getWorld("Runic Paradise"), 5572, 72, -165));
                //Bukkit.getWorld("Runic Paradise").createExplosion(new Location(Bukkit.getWorld("Runic Paradise"), 5572, 72, -165), 0);
            }
        }, 60L);
        scheduler.scheduleSyncDelayedTask(instance, new Runnable() {
            @Override
            public void run() {
                Location skel1 = new Location(Bukkit.getWorld("Runic Paradise"), 5557, 78, -156);
                Skeleton s1 = (Skeleton) Bukkit.getWorld("Runic Paradise").spawn(skel1, Skeleton.class);
                s1.setCustomName(ChatColor.DARK_RED + "Undead Archer");
                s1.setCustomNameVisible(true);
                s1.getEquipment().setItemInHand(new ItemStack(Material.BOW));
                s1.getEquipment().setHelmet(new ItemStack(Material.JACK_O_LANTERN));
                EVENT_NPCS.add(s1.getUniqueId());

                Location skel2 = new Location(Bukkit.getWorld("Runic Paradise"), 5549, 78, -162);
                Skeleton s2 = (Skeleton) Bukkit.getWorld("Runic Paradise").spawn(skel2, Skeleton.class);
                s2.setCustomName(ChatColor.DARK_RED + "Undead Archer");
                s2.setCustomNameVisible(true);
                s2.getEquipment().setItemInHand(new ItemStack(Material.BOW));
                s2.getEquipment().setHelmet(new ItemStack(Material.JACK_O_LANTERN));
                EVENT_NPCS.add(s2.getUniqueId());
                
                Location skel3 = new Location(Bukkit.getWorld("Runic Paradise"), 5572, 78, -168);
                Skeleton s3 = (Skeleton) Bukkit.getWorld("Runic Paradise").spawn(skel3, Skeleton.class);
                s3.setCustomName(ChatColor.DARK_RED + "Undead Archer");
                s3.setCustomNameVisible(true);
                s3.getEquipment().setItemInHand(new ItemStack(Material.BOW));
                s3.getEquipment().setHelmet(new ItemStack(Material.JACK_O_LANTERN));
                EVENT_NPCS.add(s3.getUniqueId());
                
                Location skel4 = new Location(Bukkit.getWorld("Runic Paradise"), 5570, 78, -176);
                Skeleton s4 = (Skeleton) Bukkit.getWorld("Runic Paradise").spawn(skel4, Skeleton.class);
                s4.setCustomName(ChatColor.DARK_RED + "Undead Archer");
                s4.setCustomNameVisible(true);
                s4.getEquipment().setItemInHand(new ItemStack(Material.BOW));
                s4.getEquipment().setHelmet(new ItemStack(Material.JACK_O_LANTERN));
                EVENT_NPCS.add(s4.getUniqueId());
                //Bukkit.getWorld("Runic Paradise").createExplosion(new Location(Bukkit.getWorld("Runic Paradise"), 5572, 72, -165), 0);
            }
        }, 140L);

    }

}
