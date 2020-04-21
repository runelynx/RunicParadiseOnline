package io.github.runelynx.runicparadise;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.logging.Level;

import static org.bukkit.Bukkit.getServer;

public class Raffle {

    public static HashMap<String, String> raffleSettingsMap = new HashMap<String, String>();

    public Raffle (){
        initializeRaffleSystem();
    }

    public void initializeRaffleSystem() {

        ConfigurationSection raffleSection = Raffle.getRaffleConfig().getConfigurationSection("Raffle");

        raffleSettingsMap.put("TicketCost", raffleSection.getString("TicketCost"));
        raffleSettingsMap.put("MaxPurchaseTickets", raffleSection.getString("MaxPurchaseTickets"));
        raffleSettingsMap.put("RaffleName", raffleSection.getString("RaffleName"));
        raffleSettingsMap.put("RaffleDisabledMessage", raffleSection.getString("RaffleDisabledMessage"));
        raffleSettingsMap.put("CurrentRaffleID", raffleSection.getString("CurrentRaffleID"));
        raffleSettingsMap.put("Enabled", raffleSection.getString("Enabled"));

        Bukkit.getLogger().log(Level.INFO,">>> Completed loading Raffle Config! RaffleEnabled is " +
                raffleSettingsMap.get("Enabled") + ", Name " +
                raffleSettingsMap.get("RaffleName") + ", ID " +
                raffleSettingsMap.get("CurrentRaffleID") + ", MaxTickets " +
                raffleSettingsMap.get("MaxPurchaseTickets") + ", TicketCost " +
                raffleSettingsMap.get("TicketCost"));

    }


    public static void shutdownRaffleSystem() {

        raffleSettingsMap.clear();
    }

    public static FileConfiguration getRaffleConfig() {

        File announceFile = new File(
                getServer().getPluginManager().getPlugin("RunicParadise").getDataFolder().getAbsolutePath(),
                "rafflesettings.yml");

        return YamlConfiguration.loadConfiguration(announceFile);

    }



}
