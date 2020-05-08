package io.github.runelynx.runicparadise.faith;

import io.github.runelynx.runicparadise.RunicParadise;
import io.github.runelynx.runicparadise.faith.modules.SacrificialPit;
import io.github.runelynx.runicparadise.faith.modules.SummoningSystem;
import io.github.runelynx.runicparadise.faith.modules.Weaponry;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import static org.bukkit.Bukkit.getServer;

public class FaithCore {

    Weaponry weaponryModule;
    SummoningSystem summoningSystemModule;
    SacrificialPit sacrificialPitModule;

    public static HashMap<String, NamespacedKey> faithCoreItemDataKeys = new HashMap<>();

    public static HashMap<String, ItemStack> faithCoreSummoningComponents = new HashMap<>();
    public static HashMap<EntityType, SummoningDropChance> faithCoreSummoningDrops = new HashMap<>();
    public static HashMap<String, Location> faithCoreSummoningLocations = new HashMap<>();
    public static HashMap<String, SummonableMob> faithCoreSummonableMobs = new HashMap<>();

    public static HashMap<String, Location> faithCorePitLocations = new HashMap<>();
    public static HashMap<Material, Integer> faithCorePitItems = new HashMap<>();
    public static HashMap<Material, String> faithCorePitItemCategories = new HashMap<>();
    public static HashMap<Material, Integer> faithCorePitItemMultipliers = new HashMap<>();
    public static HashMap<String, String> faithCorePitSettings = new HashMap<>();

    public static List<Material> faithCoreWeaponryMaterials;

    public FaithCore (){
        initializeFaithSystem();

        weaponryModule = new Weaponry();
        summoningSystemModule = new SummoningSystem();
        sacrificialPitModule = new SacrificialPit();

    }

    public void restartFaithSystem() {

        summoningSystemModule.reactivate();
        weaponryModule.reactivate();
        sacrificialPitModule.reactivate();

    }

    public void shutdownFaithSystem() {
        summoningSystemModule.deactivate();
        weaponryModule.deactivate();
        sacrificialPitModule.deactivate();
        getServer().getConsoleSender().sendMessage( "[FAITH SHUTDOWN] CORE: Modules have been deactivated");

        faithCoreSummoningComponents.clear();
        faithCoreSummoningDrops.clear();
        faithCoreSummoningLocations.clear();
        faithCoreSummonableMobs.clear();

        faithCorePitLocations.clear();
        faithCorePitItems.clear();
        faithCorePitItemCategories.clear();
        faithCorePitSettings.clear();
        faithCorePitItemMultipliers.clear();

        faithCoreWeaponryMaterials.clear();

        faithCoreItemDataKeys.clear();
        getServer().getConsoleSender().sendMessage( "[FAITH SHUTDOWN] CORE: Hashmaps have been purged");
    }

    private void initializeFaithSystem() {

        faithCoreItemDataKeys.put("ChanceToLevelUp", new NamespacedKey(RunicParadise.getInstance(), "ChanceToLevelUp"));
        faithCoreItemDataKeys.put("ChanceToConsumeCharge", new NamespacedKey(RunicParadise.getInstance(), "ChanceToConsumeCharge"));
        faithCoreItemDataKeys.put("Charges", new NamespacedKey(RunicParadise.getInstance(), "Charges"));
        faithCoreItemDataKeys.put("ZealRequiredToCraft", new NamespacedKey(RunicParadise.getInstance(), "ZealRequiredToCraft"));
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
