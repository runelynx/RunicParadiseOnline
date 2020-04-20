package io.github.runelynx.runicparadise.faith;

import io.github.runelynx.runicparadise.RunicParadise;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.HashMap;

import static org.bukkit.Bukkit.getServer;

public class FaithCore {

    public static HashMap<String, NamespacedKey> faithCoreItemDataKeys = new HashMap<String, NamespacedKey>();
    public static HashMap<String, ItemStack> faithCoreSummoningComponents = new HashMap<String, ItemStack>();
    public static HashMap<EntityType, SummoningDropChance> faithCoreSummoningDrops = new HashMap<EntityType, SummoningDropChance>();

    public FaithCore (){
        initializeFaithSystem();
    }

    public void initializeFaithSystem() {

        faithCoreItemDataKeys.put("ChanceToLevelUp", new NamespacedKey(RunicParadise.getInstance(), "ChanceToLevelUp"));
        faithCoreItemDataKeys.put("ChanceToConsumeCharge", new NamespacedKey(RunicParadise.getInstance(), "ChanceToConsumeCharge"));
        faithCoreItemDataKeys.put("Charges", new NamespacedKey(RunicParadise.getInstance(), "Charges"));
        faithCoreItemDataKeys.put("KarmaRequiredToCraft", new NamespacedKey(RunicParadise.getInstance(), "KarmaRequiredToCraft"));
        faithCoreItemDataKeys.put("FaithWeapon", new NamespacedKey(RunicParadise.getInstance(), "FaithWeapon"));
        faithCoreItemDataKeys.put("SummoningItem", new NamespacedKey(RunicParadise.getInstance(), "SummoningItem"));

        FaithModule.faithModuleMap.put("FaithWeapons", new FaithModule("FaithWeapons", "Defines and manages faith weaponry"));
        FaithModule.faithModuleMap.put("FaithMobSettings", new FaithModule("FaithMobSettings", "Controls drops and kill multipliers"));
        FaithModule.faithModuleMap.put("FaithSummonItems", new FaithModule("FaithSummonItems", "Registers items for spawn egg crafting"));
        FaithModule.faithModuleMap.put("FaithSummonMobs", new FaithModule("FaithSummonMobs", "Defines summonable mobs"));
    }


    public void shutdownFaithSystem() {
        for (String moduleKey : FaithModule.faithModuleMap.keySet()) {
            FaithModule.faithModuleMap.get(moduleKey).unloadModule();
        }

        faithCoreItemDataKeys.clear();
    }

    public static FileConfiguration getFaithConfig() {

        File announceFile = new File(
                getServer().getPluginManager().getPlugin("RunicParadise").getDataFolder().getAbsolutePath(),
                "faithsettings.yml");

        return YamlConfiguration.loadConfiguration(announceFile);

    }



}
