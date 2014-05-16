/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.runelynx.runicparadise;

import org.bukkit.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.*;
import org.bukkit.event.*;
import org.bukkit.entity.*;
import org.bukkit.Bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;

/**
 *
 * @author runelynx
 */
public final class RunicParadise extends JavaPlugin {

    @Override
    public void onEnable() {
        // TODO Insert logic to be performed when the plugin is enabled
        getLogger().info("RunicParadise Plugin: onEnable has been invoked!");
        }
    

    @Override
    public void onDisable() {
        // TODO Insert logic to be performed when the plugin is disabled
        getLogger().info("RunicParadise Plugin: onDisable has been invoked!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("basic")) { // If the player typed /basic then do the following...
            // doSomething
            return true;
        } //If this has happened the function will return true. 
        // If this hasn't happened the value of false will be returned.
        return false;
    }
}
