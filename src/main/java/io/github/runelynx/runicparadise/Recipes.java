/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.runelynx.runicparadise;

import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;

/**
 *
 * @author Andrew
 */
public class Recipes {

    public static void customFoodRecipes() {
    	
        // ROTTEN FLESH INTO LEATHER
    	FurnaceRecipe fleshLeather = new FurnaceRecipe(new ItemStack(Material.LEATHER, 1),  Material.ROTTEN_FLESH);
        Bukkit.getServer().addRecipe(fleshLeather);

        // REGENERATION STEAK
        ItemStack newItem = new ItemStack(Material.COOKED_BEEF, 3, (short) 910);
        ItemMeta meta = newItem.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Rib-eye Steak");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "Medium-rare!", ChatColor.GRAY + "Grants " + ChatColor.BLUE + "30min health regeneration"));
        newItem.setItemMeta(meta);
        ShapelessRecipe newRecipe = new ShapelessRecipe(newItem);
        newRecipe.addIngredient(1, Material.QUARTZ, 910);
        newRecipe.addIngredient(1, Material.COOKED_BEEF);
        Bukkit.getServer().addRecipe(newRecipe);

        // REGENERATION CHICKEN
        newItem = new ItemStack(Material.COOKED_CHICKEN, 3, (short) 910);
        meta = newItem.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "BBQ Chicken");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "Extra crispy", ChatColor.GRAY + "Grants " + ChatColor.BLUE + "30min health regeneration"));
        newItem.setItemMeta(meta);
        newRecipe = new ShapelessRecipe(newItem);
        newRecipe.addIngredient(1, Material.QUARTZ, 910);
        newRecipe.addIngredient(1, Material.COOKED_CHICKEN);
        Bukkit.getServer().addRecipe(newRecipe);

        // REGENERATION FISH
        newItem = new ItemStack(Material.COOKED_FISH, 3, (short) 910);
        meta = newItem.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Fish Filet");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "Fresh from the bay", ChatColor.GRAY + "Grants " + ChatColor.BLUE + "30min health regeneration"));
        newItem.setItemMeta(meta);
        newRecipe = new ShapelessRecipe(newItem);
        newRecipe.addIngredient(1, Material.QUARTZ, 910);
        newRecipe.addIngredient(1, Material.COOKED_FISH);
        Bukkit.getServer().addRecipe(newRecipe);

        // HASTE BREAD
        newItem = new ItemStack(Material.BREAD, 3, (short) 903);
        meta = newItem.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Wonder Bread");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "Hot out of the oven", ChatColor.GRAY + "Grants " + ChatColor.BLUE + "15min mining speed"));
        newItem.setItemMeta(meta);
        newRecipe = new ShapelessRecipe(newItem);
        newRecipe.addIngredient(1, Material.QUARTZ, 903);
        newRecipe.addIngredient(1, Material.BREAD);
        Bukkit.getServer().addRecipe(newRecipe);

        // STRENGTH PIE
        newItem = new ItemStack(Material.PUMPKIN_PIE, 3, (short) 905);
        meta = newItem.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Spiced Pumpkin Pie");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "Is it Thanksgiving already?", ChatColor.GRAY + "Grants " + ChatColor.BLUE + "20min strong attacks"));
        newItem.setItemMeta(meta);
        newRecipe = new ShapelessRecipe(newItem);
        newRecipe.addIngredient(1, Material.QUARTZ, 905);
        newRecipe.addIngredient(1, Material.PUMPKIN_PIE);
        Bukkit.getServer().addRecipe(newRecipe);

        // NIGHTVISION PORKCHOP
        newItem = new ItemStack(Material.GRILLED_PORK, 3, (short) 916);
        meta = newItem.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Porkchop");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "Best served with applesauce", ChatColor.GRAY + "Grants " + ChatColor.BLUE + "1hr nightvision"));
        newItem.setItemMeta(meta);
        newRecipe = new ShapelessRecipe(newItem);
        newRecipe.addIngredient(1, Material.QUARTZ, 916);
        newRecipe.addIngredient(1, Material.GRILLED_PORK);
        Bukkit.getServer().addRecipe(newRecipe);

        // NIGHTVISION potato
        newItem = new ItemStack(Material.BAKED_POTATO, 3, (short) 916);
        meta = newItem.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Cheesy Baked Potato");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "Don't forget the butter!", ChatColor.GRAY + "Grants " + ChatColor.BLUE + "1hr nightvision"));
        newItem.setItemMeta(meta);
        newRecipe = new ShapelessRecipe(newItem);
        newRecipe.addIngredient(1, Material.QUARTZ, 916);
        newRecipe.addIngredient(1, Material.BAKED_POTATO);
        Bukkit.getServer().addRecipe(newRecipe);

        // SPEED COOKIE
        newItem = new ItemStack(Material.COOKIE, 3, (short) 901);
        meta = newItem.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Sugar Cookie");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "Now with chocolate chips!", ChatColor.GRAY + "Grants " + ChatColor.BLUE + "20min speed boost"));
        newItem.setItemMeta(meta);
        newRecipe = new ShapelessRecipe(newItem);
        newRecipe.addIngredient(1, Material.QUARTZ, 901);
        newRecipe.addIngredient(1, Material.COOKIE);
        Bukkit.getServer().addRecipe(newRecipe);
        
        // BEAST TRAINING SWORD
        newItem = new ItemStack(Material.GOLD_SWORD, 1);
        newItem.setDurability((short) 99);
        meta = newItem.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Beastfang ✯");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "Empowered Spirit I", " ", ChatColor.YELLOW + "Collects arcane power from", ChatColor.YELLOW + "the spirits of ancient beasts."));
        newItem.setItemMeta(meta);
        newItem.addUnsafeEnchantment(RunicParadise.powerEnch, 1);
        newRecipe = new ShapelessRecipe(newItem);
        newRecipe.addIngredient(1, Material.GOLD_SWORD);
        newRecipe.addIngredient(1, Material.SPIDER_EYE);
        newRecipe.addIngredient(1, Material.BONE);
        Bukkit.getServer().addRecipe(newRecipe);
    }

}
