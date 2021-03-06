package io.github.runelynx.runicparadise.faith.modules;

import io.github.runelynx.runicparadise.RunicParadise;
import io.github.runelynx.runicparadise.RunicUtilities;
import io.github.runelynx.runicparadise.faith.FaithCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.logging.Level;

import static org.bukkit.Bukkit.getServer;

public class Weaponry {

    private Boolean active = false;

    public Weaponry (){
        if (activate()) {
            this.active = true;
        } else {
            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&4Failed to load Faith Module: Weaponry"));
        }
    }

    private Boolean activate() {

        if (this.active) {
            //Must deactivate first if you want to activate again
            return true;
        }

        getServer().getConsoleSender().sendMessage(ChatColor.RED + "[FAITH STARTUP] WEAPONRY: Activating ...");

        ConfigurationSection weaponSection = FaithCore.getFaithConfig().getConfigurationSection("Faith.Weapons");
        Set<String> weaponConfigList = weaponSection.getKeys(false);

        for (String weaponKey : weaponConfigList) {

            ItemStack weapon = new ItemStack(Material.valueOf(weaponSection.getString(weaponKey + ".Type")));

            addFaithWeaponRecipe(weaponKey,
                    weaponSection.getString(weaponKey + ".DisplayName"),
                    weaponSection.getString(weaponKey + ".Lore1"),
                    weaponSection.getString(weaponKey + ".Lore2"),
                    weaponSection.getString(weaponKey + ".Lore3"),
                    weaponSection.getString(weaponKey + ".Lore4"),
                    weaponSection.getString(weaponKey + ".Lore5"),
                    weaponSection.getDouble(weaponKey + ".ChanceToLevelUp"),
                    weaponSection.getDouble(weaponKey + ".ChanceToConsumeCharge"),
                    weaponSection.getInt(weaponKey + ".Charges"),
                    weaponSection.getInt(weaponKey + ".ZealRequiredToCraft"),
                    weaponSection.getString(weaponKey + ".Type"),
                    weaponSection.getStringList(weaponKey + ".Recipe"),
                    weaponSection.getStringList(weaponKey + ".Enchants"),
                    weaponSection.getDouble(weaponKey + ".AddDamage"),
                    weaponSection.getInt(weaponKey + ".AddAttackSpeed"),
                    weaponSection.getInt(weaponKey + ".AddMoveSpeed"),
                    weaponSection.getInt(weaponKey + ".AddHealth"),
                    weaponSection.getInt(weaponKey + ".AddArmor"),
                    weaponSection.getInt(weaponKey + ".AddLuck"));

        }

        getServer().getConsoleSender().sendMessage(ChatColor.RED + "[FAITH STARTUP] WEAPONRY: Activation complete!");
        return true;
    }

    public Boolean reactivate(){
        if (this.active) {
            // Module is running - so deactivate first
            if (deactivate()){
                if (activate()) {
                    return true;
                }
            }
        } else {
            // Module is not running, so just activate
            if (activate()) {
                return true;
            }
        }
        return false;
    }

    public Boolean deactivate(){

        Bukkit.getLogger().log(Level.OFF, "[FAITH SHUTDOWN] WEAPONRY: Removing custom recipes from RunicParadise to reload Faith config");

        Iterator<Recipe> it = getServer().recipeIterator();
        int count = 0;

        while(it.hasNext()){

            Recipe itRecipe = it.next();
            if (itRecipe != null
                    && FaithCore.faithCoreWeaponryMaterials.containsKey(itRecipe.getResult().getType())
                    && itRecipe instanceof ShapelessRecipe) {
                getServer().getConsoleSender().sendMessage("[FAITH SHUTDOWN] WEAPONRY: Removing recipe for... " +
                        itRecipe.getResult().getItemMeta().getDisplayName());
                it.remove();
                count++;
            }

        }
        getServer().getConsoleSender().sendMessage("[FAITH SHUTDOWN] WEAPONRY: Recipe removal complete; removed " + count + " recipes");
        this.active = false;
        return true;
    }

    private Boolean addFaithWeaponRecipe(String id, String name, String lore1, String lore2, String lore3, String lore4, String lore5,
                                         Double levelUpChance, Double consumeChargeChance, int charges,
                                         int zealRequired, String itemType, List<String> craftList, List<String> enchantList, Double addDamage, Integer addAtkSpeed,
                                         Integer addMoveSpeed, Integer addHealth, Integer addArmor, Integer addLuck) {

        ArrayList<String> loreList = RunicUtilities.processLoreStringsToArray(lore1, lore2, lore3, lore4, lore5);

        // TODO - Need to visually ensure some data elements are shown (like remaining charges)

        // Add this item type to the list so we can safely remove recipes on a reload
        FaithCore.faithCoreWeaponryMaterials.put(Material.valueOf(itemType), 1);

        ItemStack weapon = new ItemStack(Material.valueOf(itemType));
        ItemMeta meta = weapon.getItemMeta();

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        meta.setLore(loreList);
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);

        meta.getPersistentDataContainer().set(FaithCore.faithCoreItemDataKeys.get("ChanceToLevelUp"), PersistentDataType.DOUBLE, levelUpChance);
        meta.getPersistentDataContainer().set(FaithCore.faithCoreItemDataKeys.get("ChanceToConsumeCharge"), PersistentDataType.DOUBLE, consumeChargeChance);
        meta.getPersistentDataContainer().set(FaithCore.faithCoreItemDataKeys.get("Charges"), PersistentDataType.INTEGER, charges);
        meta.getPersistentDataContainer().set(FaithCore.faithCoreItemDataKeys.get("ZealRequiredToCraft"), PersistentDataType.INTEGER, zealRequired);
        meta.getPersistentDataContainer().set(FaithCore.faithCoreItemDataKeys.get("FaithWeapon"), PersistentDataType.INTEGER, 1);

        if (addDamage != null || addDamage == 0) {
            AttributeModifier atkdmgModifier = new AttributeModifier(UUID.randomUUID(), "generic.attackdamage", addDamage, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND);
            meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, atkdmgModifier);
        }

        if (addAtkSpeed != null || addAtkSpeed == 0) {
            AttributeModifier atkspdModifier = new AttributeModifier(UUID.randomUUID(), "generic.attackspeed", addAtkSpeed, AttributeModifier.Operation.ADD_SCALAR, EquipmentSlot.HAND);
            meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, atkspdModifier);
        }

        if (addMoveSpeed != null || addMoveSpeed == 0) {
            AttributeModifier movespdModifier = new AttributeModifier(UUID.randomUUID(), "generic.movementspeed", addMoveSpeed, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND);
            meta.addAttributeModifier(Attribute.GENERIC_MOVEMENT_SPEED, movespdModifier);
        }

        if (addHealth != null || addHealth == 0) {
            AttributeModifier healthModifier = new AttributeModifier(UUID.randomUUID(), "generic.maxhealth", addHealth, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND);
            meta.addAttributeModifier(Attribute.GENERIC_MAX_HEALTH, healthModifier);
        }

        if (addLuck != null || addLuck == 0) {
            AttributeModifier luckModifier = new AttributeModifier(UUID.randomUUID(), "generic.luck", addLuck, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND);
            meta.addAttributeModifier(Attribute.GENERIC_LUCK, luckModifier);
        }

        if (addArmor != null || addArmor == 0) {
            AttributeModifier armorModifier = new AttributeModifier(UUID.randomUUID(), "generic.armor", addArmor, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND);
            meta.addAttributeModifier(Attribute.GENERIC_ARMOR, armorModifier);
        }

        for (String itemStr : enchantList) {
            String[] strings = itemStr.split(";");
            meta.addEnchant(Enchantment.getByKey(NamespacedKey.minecraft(strings[0])), Integer.parseInt(strings[1]), true);

        }

        weapon.setItemMeta(meta);

        // create a NamespacedKey for your recipe
        NamespacedKey key = new NamespacedKey(RunicParadise.getInstance(), id);

        // Create our custom recipe variable
        ShapelessRecipe recipe = new ShapelessRecipe(key, weapon);

        for (String itemStr : craftList) {
            String[] strings = itemStr.split(";");

            recipe.addIngredient(Integer.valueOf(strings[1]),Material.valueOf(strings[0]));
        }

        // Finally, add the recipe to the bukkit recipes
        Bukkit.addRecipe(recipe);

        getServer().getConsoleSender().sendMessage("[FAITH STARTUP] WEAPONRY: Adding custom recipe... " + itemType + " | " + name);

        return true;
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
}
