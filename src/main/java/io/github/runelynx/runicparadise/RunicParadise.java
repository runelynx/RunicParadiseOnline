/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.runelynx.runicparadise;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;

/**
 *
 * @author runelynx
 */
public final class RunicParadise extends JavaPlugin {

    public void onEnable() {
        // TODO Insert logic to be performed when the plugin is enabled
        getLogger().info("RunicParadise Plugin: onEnable has been invoked!");
        }
    
    public void onDisable() {
        // TODO Insert logic to be performed when the plugin is disabled
        getLogger().info("RunicParadise Plugin: onDisable has been invoked!");
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
       if( sender instanceof Player){
           Player player = (Player) sender;
           if(cmd.getName().equalsIgnoreCase("rp")){
               String rp = "spawn " + player.getName();
               Bukkit.dispatchCommand(Bukkit.getConsoleSender(), rp);
               player.sendMessage(ChatColor.AQUA + "RP command - returning to spawn!");
              
           }
       }
       return false;
    }
}
