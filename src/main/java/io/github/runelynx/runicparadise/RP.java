/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.runelynx.runicparadise;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 *
 * @author Andrew
 */
public class RP implements CommandExecutor {

    // pointer to your main class, not required if you don't need methods from the main class
    private Plugin instance = RunicParadise.getInstance();

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (cmd.getName().equalsIgnoreCase("rp")) {
                String rp = "spawn " + player.getName();
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), rp);
                player.sendMessage(ChatColor.AQUA + "RP command - returning to spawn!");

            }
        } else {
           sender.sendMessage("[RP] Command RP must be used by a player");
           return false;
        }
        return false;
    }

}
