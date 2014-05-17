/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.runelynx.runicparadise;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.Plugin;

/**
 *
 * @author runelynx
 */
public final class RunicParadise extends JavaPlugin implements Listener {

    private static Plugin instance;

    public static Plugin getInstance() {
        return instance;
    }

    public void onEnable() {
        instance = this;
        // TODO Insert logic to be performed when the plugin is enabled
        getLogger().info("RunicParadise Plugin: onEnable has been invoked!");

        // This will throw a NullPointerException if you don't have the command defined in your plugin.yml file!
        getCommand("rp").setExecutor(new RP());

        Bukkit.getServer().getPluginManager().registerEvents(this, this);

    }

    public void onDisable() {
        // TODO Insert logic to be performed when the plugin is disabled
        getLogger().info("RunicParadise Plugin: onDisable has been invoked!");
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent pje) {

        // Launch Firework on player join
        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
            public void run() {
                Firework f = (Firework) pje.getPlayer().getWorld().spawn(pje.getPlayer().getLocation(), Firework.class);
                FireworkMeta fm = f.getFireworkMeta();
                fm.addEffect(FireworkEffect.builder()
                        .flicker(false)
                        .trail(true)
                        .with(Type.BALL)
                        .with(Type.BALL_LARGE)
                        .with(Type.STAR)
                        .withColor(Color.ORANGE)
                        .withColor(Color.YELLOW)
                        .withFade(Color.RED)
                        .withFade(Color.PURPLE)
                        .build());
                fm.setPower(2);
                f.setFireworkMeta(fm);

            }
        }, 100); //delay
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDamage(final EntityDamageEvent ede) {

        if (ede.getCause() == DamageCause.VOID) {
            if (ede instanceof Player) {
                Player player = (Player) ede;
                player.setHealth(20);
                player.teleport(player.getWorld().getSpawnLocation());
                player.sendMessage(ChatColor.AQUA + "Found someone lost in the void... sending to spawn.");
            }
        }

    }

}
