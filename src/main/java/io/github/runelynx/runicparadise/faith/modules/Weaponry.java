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
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapelessRecipe;
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
                    weaponSection.getInt(weaponKey + ".ZealRequiredToCraft"),
                    weaponSection.getString(weaponKey + ".Type"),
                    weaponSection.getStringList(weaponKey + ".Recipe"),
                    weaponSection.getStringList(weaponKey + ".Enchants"),
                    weaponSection.getInt(weaponKey + ".AddDamage"));

        }

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
        this.active = false;
        return true;
    }

    private Boolean addFaithWeaponRecipe(String id, String name, String lore1, String lore2, String lore3,
                                         Double levelUpChance, Double consumeChargeChance, int charges,
                                         int zealRequired, String itemType, List<String> craftList, List<String> enchantList, int addDamage) {

        int levelUpChancePretty = (int)(levelUpChance * 100);
        int consumeChargeChancePretty = (int)(consumeChargeChance * 100);
        ArrayList<String> loreList = RunicUtilities.processLoreStringsToArray(lore1, lore2, lore3);

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
        meta.getPersistentDataContainer().set(FaithCore.faithCoreItemDataKeys.get("ZealRequiredToCraft"), PersistentDataType.INTEGER, zealRequired);
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
