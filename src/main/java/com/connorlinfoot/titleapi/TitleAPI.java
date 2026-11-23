package com.connorlinfoot.titleapi;



import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;


public class TitleAPI extends JavaPlugin implements Listener {

    @Deprecated
    public static void sendTitle(Player player, Integer fadeIn, Integer stay, Integer fadeOut, String message) {
        sendTitle(player, fadeIn, stay, fadeOut, message, null);
    }

    @Deprecated
    public static void sendSubtitle(Player player, Integer fadeIn, Integer stay, Integer fadeOut, String message) {
        sendTitle(player, fadeIn, stay, fadeOut, null, message);
    }

    @Deprecated
    public static void sendFullTitle(Player player, Integer fadeIn, Integer stay, Integer fadeOut, String title, String subtitle) {
        sendTitle(player, fadeIn, stay, fadeOut, title, subtitle);
    }

    // 2026-11-23: rewritten on the Bukkit/Paper title API. The old version used pre-1.17 NMS packet classes
    // (net.minecraft.server.v1_x.PacketPlayOutTitle), which don't exist on 1.20.2, so EVERY title failed with
    // ClassNotFoundException and players saw nothing (faith access checks, tokens, etc).
    // Callers in RunicParadise pass small numbers (1-8) meaning seconds; values under 20 are treated as seconds.
    private static int toTicks(Integer v, int dflt) {
        if (v == null) return dflt;
        return v < 20 ? v * 20 : v;
    }

    private static String fmt(Player player, String s) {
        if (s == null) return "";
        return ChatColor.translateAlternateColorCodes('&', s).replace("%player%", player.getDisplayName());
    }

    public static void sendTitle(Player player, Integer fadeIn, Integer stay, Integer fadeOut, String title, String subtitle) {
        if (player == null || !player.isOnline()) return;
        player.sendTitle(fmt(player, title), fmt(player, subtitle), toTicks(fadeIn, 10), toTicks(stay, 70), toTicks(fadeOut, 20));
    }

    public static void sendTabTitle(Player player, String header, String footer) {
        if (player == null || !player.isOnline()) return;
        player.setPlayerListHeaderFooter(fmt(player, header), fmt(player, footer));
    }

    public void onEnable() {
        getConfig().options().copyDefaults(true);
        saveConfig();
        Server server = getServer();
        ConsoleCommandSender console = server.getConsoleSender();

        console.sendMessage("");
        console.sendMessage(ChatColor.BLUE + "-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
        console.sendMessage("");
        console.sendMessage(ChatColor.AQUA + getDescription().getName());
        console.sendMessage(ChatColor.AQUA + "Version " + getDescription().getVersion());
        console.sendMessage("");
        console.sendMessage(ChatColor.BLUE + "-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
        console.sendMessage("");

        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (getConfig().getBoolean("Title On Join")) {
            sendTitle(event.getPlayer(), 20, 50, 20, getConfig().getString("Title Message"), getConfig().getString("Subtitle Message"));
        }

        if (getConfig().getBoolean("Tab Header Enabled")) {
            sendTabTitle(event.getPlayer(), getConfig().getString("Tab Header Message"), getConfig().getString("Tab Footer Message"));
        }
    }

    public void onDisable() {
        getLogger().info(getDescription().getName() + " has been disabled!");
    }

}
