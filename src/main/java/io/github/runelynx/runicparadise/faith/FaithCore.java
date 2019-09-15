package io.github.runelynx.runicparadise.faith;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import static org.bukkit.Bukkit.getLogger;
import static org.bukkit.Bukkit.getServer;

public class FaithCore {

    public FaithCore (){
        initializeFaithSystem();
    }

    public void initializeFaithSystem() {

        FaithModule.faithModuleMap.put("FaithWeapons", new FaithModule("FaithWeapons", "Defines and manages faith weaponry"));
        FaithModule.faithModuleMap.put("FaithMobSettings", new FaithModule("FaithMobSettings", "Controls drops and kill multipliers"));
        FaithModule.faithModuleMap.put("FaithSummonItems", new FaithModule("FaithSummonItems", "Registers items for spawn egg crafting"));
        FaithModule.faithModuleMap.put("FaithSummonMobs", new FaithModule("FaithSummonMobs", "Defines summonable mobs"));

    }


    public static void shutdownFaithSystem() {
        for (String moduleKey : FaithModule.faithModuleMap.keySet()) {
            FaithModule.faithModuleMap.get(moduleKey).unloadModule();
        }
    }

    public static FileConfiguration getFaithConfig() {

        File announceFile = new File(
                getServer().getPluginManager().getPlugin("RunicParadise").getDataFolder().getAbsolutePath(),
                "faithsettings.yml");

        return YamlConfiguration.loadConfiguration(announceFile);

    }



}
