package io.github.runelynx.runicparadise.faith;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

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
