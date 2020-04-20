package io.github.runelynx.runicparadise.faith;

import io.github.runelynx.runicparadise.RunicParadise;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.logging.Level;

import static org.bukkit.Bukkit.getServer;

public class FaithModule {

    private Boolean active;
    private String name;
    private String description;

    public static HashMap<String, FaithModule> faithModuleMap = new HashMap<String, FaithModule>();

    public FaithModule (String name, String desc){

        if (faithModuleMap.containsKey(name)){
            // If Module is already registered, clear that out so we start fresh
            faithModuleMap.remove(name);
        }

        initializeModule(name, desc);
    }

    public void unloadModule() {

        if (faithModuleMap.containsKey(name)){
            switch (this.name) {
                case "FaithWeapons":
                    try {
                        deactivateModule_FaithWeapons();
                        this.active = false;
                    } catch (Exception e) {
                        Bukkit.getLogger().log(Level.SEVERE, "FaithModule.unloadModule failed for FaithWeapons.");
                        Bukkit.getLogger().log(Level.INFO,  e.getStackTrace().toString());
                        this.active = false;
                    }
                    break;
                case "FaithMobSettings":
                    try {
                        deactivateModule_FaithMobSettings();
                        this.active = false;
                    } catch (Exception e) {
                        Bukkit.getLogger().log(Level.SEVERE, "FaithModule.unloadModule failed for FaithMobSettings.");
                        Bukkit.getLogger().log(Level.INFO,  e.toString());
                        this.active = false;
                    }
                    break;
                case "FaithSummonItems":
                    try {
                        deactivateModule_FaithSummonItems();
                        this.active = false;
                    } catch (Exception e) {
                        Bukkit.getLogger().log(Level.SEVERE, "FaithModule.unloadModule failed for FaithSummonItems.");
                        Bukkit.getLogger().log(Level.INFO,  e.toString());
                        this.active = false;
                    }
                    break;
                case "FaithSummonMobs":
                    try {
                        deactivateModule_FaithSummonMobs();
                        this.active = false;
                    } catch (Exception e) {
                        Bukkit.getLogger().log(Level.SEVERE, "FaithModule.unloadModule failed for FaithSummonMobs.");
                        Bukkit.getLogger().log(Level.INFO,  e.toString());
                        this.active = false;
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void initializeModule(String name, String description){

        this.name = name;
        this.description = description;

        switch (name) {
            case "FaithWeapons":
                    try {
                        activateModule_FaithWeapons();
                        this.active = true;
                    } catch (Exception e) {
                        Bukkit.getLogger().log(Level.SEVERE, "FaithModule.initializeModule failed for FaithWeapons.");
                        e.printStackTrace();
                        this.active = false;
                    }
                break;
            case "FaithMobSettings":
                    try {
                        activateModule_FaithMobSettings();
                        this.active = true;
                    } catch (Exception e) {
                        Bukkit.getLogger().log(Level.SEVERE, "FaithModule.initializeModule failed for FaithMobSettings.");
                        e.printStackTrace();
                        this.active = false;
                    }
                break;
            case "FaithSummonItems":
                    try {
                        activateModule_FaithSummonItems();
                        this.active = true;
                    } catch (Exception e) {
                        Bukkit.getLogger().log(Level.SEVERE, "FaithModule.initializeModule failed for FaithSummonItems.");
                        e.printStackTrace();
                        this.active = false;
                    }
                break;
            case "FaithSummonMobs":
                    try {
                        activateModule_FaithSummonMobs();
                        this.active = true;
                    } catch (Exception e) {
                        Bukkit.getLogger().log(Level.SEVERE, "FaithModule.initializeModule failed for FaithSummonMobs.");
                        e.printStackTrace();
                        this.active = false;
                    }
                break;
            default:
                break;
        }


    }

    private Boolean activateModule_FaithWeapons() {

        Bukkit.getLogger().log(Level.INFO, "~~~ Activating faith module - faithweapons ~~~");

        ConfigurationSection weaponSection = FaithCore.getFaithConfig().getConfigurationSection("Faith.Weapons");
        Set<String> weaponConfigList = weaponSection.getKeys(false);

        for (String weaponKey : weaponConfigList) {

            ItemStack weapon = new ItemStack(Material.valueOf(weaponSection.getString(weaponKey + ".Type")));

            addFaithWeaponRecipe(weaponKey,
                    weaponSection.getString(weaponKey + ".DisplayName"),
                    weaponSection.getString(weaponKey + ".Lore1"),
                    weaponSection.getString(weaponKey + ".Lore2"),
                    weaponSection.getString(weaponKey + ".Lore3"),
                    weaponSection.getDouble(weaponKey + ".ChanceToLevelUp"),
                    weaponSection.getDouble(weaponKey + ".ChanceToConsumeCharge"),
                    weaponSection.getInt(weaponKey + ".Charges"),
                    weaponSection.getInt(weaponKey + ".KarmaRequiredToCraft"),
                    weaponSection.getString(weaponKey + ".Type"),
                    weaponSection.getStringList(weaponKey + ".Recipe"),
                    weaponSection.getStringList(weaponKey + ".Enchants"),
                    weaponSection.getInt(weaponKey + ".AddDamage"));

        }

        return true;
    }

    public static void deactivateModule_FaithWeapons(){

        List<Material> materialsToRemove = new ArrayList<>();
        materialsToRemove.add(Material.DIAMOND_SWORD);
        materialsToRemove.add(Material.DIAMOND_AXE);
        materialsToRemove.add(Material.BOW);

        getServer().getConsoleSender().sendMessage(ChatColor.RED + "~~~ Removing custom recipes from RunicParadise to reload Faith config ~~~");
        getServer().getConsoleSender().sendMessage(ChatColor.GRAY + "    > Targeting recipes that create: (list controlled in deactivateModule_FaithWeapons)");

        for (Material m : materialsToRemove) {
            getServer().getConsoleSender().sendMessage(ChatColor.GOLD + "        . " + m.toString());
        }

        Iterator<Recipe> it = getServer().recipeIterator();
        int count = 0;

        while(it.hasNext()){

            Recipe itRecipe = it.next();
            if (itRecipe != null
                    && materialsToRemove.contains(itRecipe.getResult().getType())
                    && itRecipe instanceof ShapelessRecipe) {
                getServer().getConsoleSender().sendMessage(ChatColor.RED + "    > "
                        + ChatColor.GRAY + "        Removing "
                        + ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE + itRecipe.getResult().getItemMeta().getDisplayName());
                it.remove();
                count++;
            }

        }
        getServer().getConsoleSender().sendMessage(ChatColor.RED + "~~~ Recipe removal complete; removed " + count + " recipes ~~~");
    }

    private Boolean activateModule_FaithMobSettings() {

        return false;
    }

    private Boolean deactivateModule_FaithMobSettings() {

        return false;
    }

    private Boolean activateModule_FaithSummonItems() {

        Bukkit.getLogger().log(Level.INFO, "~~~ Activating faith module - faithsummonitems ~~~");

        ConfigurationSection summonItemSection = FaithCore.getFaithConfig().getConfigurationSection("Faith.SummoningSystem.Items");
        Set<String> summonItemConfigList = summonItemSection.getKeys(false);

        for (String summonItemKey : summonItemConfigList) {

            ItemStack item = new ItemStack(Material.valueOf(summonItemSection.getString(summonItemKey + ".Type")));

            addSummoningItem(summonItemKey,
                    summonItemSection.getString(summonItemKey + ".DisplayName"),
                    summonItemSection.getString(summonItemKey + ".Lore1"),
                    summonItemSection.getString(summonItemKey + ".Lore2"),
                    summonItemSection.getString(summonItemKey + ".Lore3"),
                    summonItemSection.getString(summonItemKey + ".Type"),
                    summonItemSection.getStringList(summonItemKey + ".Drops"));
        }

        return true;
    }

    private Boolean deactivateModule_FaithSummonItems() {
        FaithCore.faithCoreSummoningComponents.clear();
        FaithCore.faithCoreSummoningDrops.clear();
        return true;
    }

    private Boolean activateModule_FaithSummonMobs() {

        return false;
    }

    private Boolean deactivateModule_FaithSummonMobs() {

        return false;
    }

    private String detectWeaponType(String type) {

        if(type.contains("SWORD")) {
            return "SWORD";
        } else if(type.contains("AXE")) {
            return "AXE";
        } else if(type.contains("BOW")) {
            return "BOW";
        } else if(type.contains("TRIDENT")) {
            return "TRIDENT";
        } else if(type.contains("SHOVEL")) {
            return "SHOVEL";
        } else {
            return "INVALID!";
        }

    }

    private Boolean addFaithWeaponRecipe(String id, String name, String lore1, String lore2, String lore3,
                                         Double levelUpChance, Double consumeChargeChance, int charges,
                                         int karmaRequired, String itemType, List<String> craftList, List<String> enchantList, int addDamage) {

        int levelUpChancePretty = (int)(levelUpChance * 100);
        int consumeChargeChancePretty = (int)(consumeChargeChance * 100);
        ArrayList<String> loreList = processLore(lore1, lore2, lore3);

        String lore4 = "" + ChatColor.LIGHT_PURPLE + charges + " Charges";

//        String lore4 = ChatColor.DARK_GRAY + "Faith|" + ChatColor.YELLOW + detectWeaponType(itemType) +
//                ChatColor.DARK_GRAY + "|" + ChatColor.GREEN + levelUpChancePretty + "%LU" +
//                ChatColor.DARK_GRAY + "|" + ChatColor.AQUA + consumeChargeChancePretty + "%CC" +
//                ChatColor.DARK_GRAY + "|" + ChatColor.RED + karmaRequired + "KR" +
//                ChatColor.DARK_GRAY + "|" + ChatColor.LIGHT_PURPLE + charges + " Charges";

        loreList.add(lore4);

        ItemStack weapon = new ItemStack(Material.valueOf(itemType));
        ItemMeta meta = weapon.getItemMeta();

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        meta.setLore(loreList);
        meta.setUnbreakable(true);

        meta.getPersistentDataContainer().set(FaithCore.faithCoreItemDataKeys.get("ChanceToLevelUp"), PersistentDataType.DOUBLE, levelUpChance);
        meta.getPersistentDataContainer().set(FaithCore.faithCoreItemDataKeys.get("ChanceToConsumeCharge"), PersistentDataType.DOUBLE, consumeChargeChance);
        meta.getPersistentDataContainer().set(FaithCore.faithCoreItemDataKeys.get("Charges"), PersistentDataType.INTEGER, charges);
        meta.getPersistentDataContainer().set(FaithCore.faithCoreItemDataKeys.get("KarmaRequiredToCraft"), PersistentDataType.INTEGER, karmaRequired);
        meta.getPersistentDataContainer().set(FaithCore.faithCoreItemDataKeys.get("FaithWeapon"), PersistentDataType.INTEGER, 1);

        AttributeModifier modifier = new AttributeModifier(UUID.randomUUID(), "generic.attackdamage", addDamage, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND);
        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, modifier);

        for (String itemStr : enchantList) {
            String[] strings = itemStr.split(";");
            //Debug
            Bukkit.getLogger().log(Level.INFO, strings[0] + " " + strings[1]);

            meta.addEnchant(Enchantment.getByKey(NamespacedKey.minecraft(strings[0])), Integer.parseInt(strings[1]), true);

        }

        weapon.setItemMeta(meta);

        // create a NamespacedKey for your recipe
        NamespacedKey key = new NamespacedKey(RunicParadise.getInstance(), id);

        // Create our custom recipe variable
        ShapelessRecipe recipe = new ShapelessRecipe(key, weapon);

        for (String itemStr : craftList) {
            String[] strings = itemStr.split(";");
            //Debug
            Bukkit.getLogger().log(Level.INFO, strings[0] + " " + strings[1]);

            recipe.addIngredient(Integer.valueOf(strings[1]),Material.valueOf(strings[0]));
        }

        // Finally, add the recipe to the bukkit recipes
        Bukkit.addRecipe(recipe);

        Bukkit.getLogger().log(Level.INFO, "    Adding custom recipe from faith config: "
                + itemType + " - "
                + name);

        return true;
    }

    private Boolean addSummoningItem(String id, String name, String lore1, String lore2, String lore3,
                                       String itemType, List<String> dropList) {

        ArrayList<String> loreList = processLore(lore1, lore2, lore3);

        ItemStack item = new ItemStack(Material.valueOf(itemType));
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        meta.setLore(loreList);

        if (FaithCore.faithCoreItemDataKeys.get("SummoningItem") == null) {
            Bukkit.getLogger().log(Level.INFO, "SummoningItem key is indeed null. :(");
        }

        meta.getPersistentDataContainer().set(FaithCore.faithCoreItemDataKeys.get("SummoningItem"), PersistentDataType.STRING, "TRUE");

        item.setItemMeta(meta);

        for (String itemStr : dropList) {
            String[] strings = itemStr.split(";");
            //Debug
            Bukkit.getLogger().log(Level.INFO, strings[0] + " " + strings[1]);

            FaithCore.faithCoreSummoningComponents.put(id, item);
            FaithCore.faithCoreSummoningDrops.put(EntityType.valueOf(strings[0]), new SummoningDropChance(id, Double.parseDouble(strings[1])));
        }

        return true;
    }

    private ArrayList<String> processLore(String lore1, String lore2, String lore3) {
        ArrayList<String> loreList = new ArrayList<String>();
        loreList.add(ChatColor.translateAlternateColorCodes('&', lore1));

        if (lore2 != null) {
            loreList.add(ChatColor.translateAlternateColorCodes('&', lore2));
        } else {
            loreList.add(" ");
        }

        if (lore3 != null) {
            loreList.add(ChatColor.translateAlternateColorCodes('&', lore3));
        } else {
            loreList.add(" ");
        }

        return loreList;
    }

}
