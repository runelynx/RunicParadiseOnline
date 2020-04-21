package io.github.runelynx.runicparadise.faith;

import io.github.runelynx.runicparadise.RunicParadise;
import io.github.runelynx.runicparadise.faith.modules.SummoningSystem;
import io.github.runelynx.runicparadise.faith.modules.Weaponry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.HashMap;
import java.util.logging.Level;

import static org.bukkit.Bukkit.getServer;

public class FaithCore {

    Weaponry weaponryModule;
    SummoningSystem summoningSystemModule;

    public static HashMap<String, NamespacedKey> faithCoreItemDataKeys = new HashMap<String, NamespacedKey>();
    public static HashMap<String, ItemStack> faithCoreSummoningComponents = new HashMap<String, ItemStack>();
    public static HashMap<EntityType, SummoningDropChance> faithCoreSummoningDrops = new HashMap<EntityType, SummoningDropChance>();
    public static HashMap<String, Location> faithCoreSummoningLocations = new HashMap<String, Location>();
    public static HashMap<String, SummonableMob> faithCoreSummonableMobs = new HashMap<String, SummonableMob>();
    public static HashMap<String, ItemStack> faithCoreSummoningBalls = new HashMap<String, ItemStack>();

    public FaithCore (){
        initializeFaithSystem();

        weaponryModule = new Weaponry();
        summoningSystemModule= new SummoningSystem();

    }

    public void restartFaithSystem() {

        summoningSystemModule.reactivate();
        weaponryModule.reactivate();

    }

    public void shutdownFaithSystem() {
        summoningSystemModule.deactivate();
        weaponryModule.deactivate();

        faithCoreItemDataKeys.clear();
    }

    private void initializeFaithSystem() {

        faithCoreItemDataKeys.put("ChanceToLevelUp", new NamespacedKey(RunicParadise.getInstance(), "ChanceToLevelUp"));
        faithCoreItemDataKeys.put("ChanceToConsumeCharge", new NamespacedKey(RunicParadise.getInstance(), "ChanceToConsumeCharge"));
        faithCoreItemDataKeys.put("Charges", new NamespacedKey(RunicParadise.getInstance(), "Charges"));
        faithCoreItemDataKeys.put("ZealRequiredToCraft", new NamespacedKey(RunicParadise.getInstance(), "KarmaRequiredToCraft"));
        faithCoreItemDataKeys.put("FaithWeapon", new NamespacedKey(RunicParadise.getInstance(), "FaithWeapon"));
        faithCoreItemDataKeys.put("SummoningItem", new NamespacedKey(RunicParadise.getInstance(), "SummoningItem"));
        faithCoreItemDataKeys.put("FaithBallMobId", new NamespacedKey(RunicParadise.getInstance(), "FaithBallMobId"));


//        faithModuleMap.put("FaithWeapons", new FaithModule("FaithWeapons", "Defines and manages faith weaponry"));
//        faithModuleMap.put("FaithMobSettings", new FaithModule("FaithMobSettings", "Controls drops and kill multipliers"));
//        faithModuleMap.put("FaithSummonItems", new FaithModule("FaithSummonItems", "Registers items for spawn egg crafting"));
//        faithModuleMap.put("FaithSummonMobs", new FaithModule("FaithSummonMobs", "Defines summonable mobs"));
    }




    public static FileConfiguration getFaithConfig() {

        File announceFile = new File(
                getServer().getPluginManager().getPlugin("RunicParadise").getDataFolder().getAbsolutePath(),
                "faithsettings.yml");

        return YamlConfiguration.loadConfiguration(announceFile);

    }

}
