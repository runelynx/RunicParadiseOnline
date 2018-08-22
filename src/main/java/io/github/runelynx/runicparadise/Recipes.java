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
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;

/**
 *
 * @author Andrew
 */
public class Recipes {

	public static ItemStack customItemStacks(String key) {
		
		ItemStack newItem;
		ItemMeta meta;

		switch (key) {
			case "FAITH_SWORD_1":
					newItem = new ItemStack(Material.GOLD_SWORD, 1);
					meta = newItem.getItemMeta();
					meta.setDisplayName(ChatColor.GOLD + "Sword of Faith");
					meta.setLore(Arrays.asList(ChatColor.GRAY
							+ "A blessed blade with a faint glow", " ", ChatColor.YELLOW
							+ "5% chance to increase faith", ChatColor.YELLOW
							+ "50% chance to shatter"));
					meta.addEnchant(Enchantment.DURABILITY, 10, true);
					newItem.setItemMeta(meta);
					newItem.addUnsafeEnchantment(Enchantment.DURABILITY, 99);
					return newItem;
			case "FAITH_SWORD_2":
					newItem = new ItemStack(Material.GOLD_SWORD, 1);
					meta = newItem.getItemMeta();
					meta.setDisplayName(ChatColor.DARK_GREEN + "Sword of Ardent Faith");
					meta.setLore(Arrays.asList(ChatColor.GRAY
							+ "A blessed blade with a pulsing glow", " ", ChatColor.GREEN
							+ "10% chance to increase faith", ChatColor.GREEN
							+ "30% chance to shatter"));
					meta.addEnchant(Enchantment.DURABILITY, 10, true);
					newItem.setItemMeta(meta);
					newItem.addUnsafeEnchantment(Enchantment.DURABILITY, 99);
					return newItem;
			case "FAITH_SWORD_3":
					newItem = new ItemStack(Material.GOLD_SWORD, 1);
					meta = newItem.getItemMeta();
					meta.setDisplayName(ChatColor.DARK_BLUE + "Sword of Devoted Faith");
					meta.setLore(Arrays.asList(ChatColor.GRAY
							+ "A blessed blade with a blinding glow", " ", ChatColor.BLUE
							+ "15% to increase faith", ChatColor.BLUE
							+ "10% chance to shatter."));
					meta.addEnchant(Enchantment.DURABILITY, 10, true);
					newItem.setItemMeta(meta);
					newItem.addUnsafeEnchantment(Enchantment.DURABILITY, 99);
					return newItem;
			case "FAITH_AXE_1":
					newItem = new ItemStack(Material.GOLD_AXE, 1);
					meta = newItem.getItemMeta();
					meta.setDisplayName(ChatColor.DARK_PURPLE + "Hatchet of Devious Faith");
					meta.setLore(Arrays.asList(ChatColor.GRAY + "A corrupted axe with a crimson glow", " ",
							ChatColor.BLUE + "20% to increase faith", ChatColor.BLUE + "20% chance to lose a charge",
							ChatColor.BLUE + "Shatters when charges reach zero", " ", ChatColor.DARK_GREEN + "Charges: 5"));
					meta.addEnchant(Enchantment.DURABILITY, 10, true);
					newItem.setItemMeta(meta);
					newItem.addUnsafeEnchantment(Enchantment.DURABILITY, 999);
					return newItem;
			default:
					return null;
		}

	}

	public static void customFoodRecipes() {

		// ROTTEN FLESH INTO LEATHER
		FurnaceRecipe fleshLeather = new FurnaceRecipe(new ItemStack(
				Material.LEATHER, 1), Material.ROTTEN_FLESH);
		Bukkit.getServer().addRecipe(fleshLeather);

		// REGENERATION STEAK
		ItemStack newItem = new ItemStack(Material.COOKED_BEEF, 3, (short) 910);
		ItemMeta meta = newItem.getItemMeta();
		meta.setDisplayName(ChatColor.GREEN + "Rib-eye Steak");
		meta.setLore(Arrays.asList(ChatColor.GRAY + "Medium-rare!",
				ChatColor.GRAY + "Grants " + ChatColor.BLUE
						+ "30min health regeneration"));
		newItem.setItemMeta(meta);
		ShapelessRecipe newRecipe = new ShapelessRecipe(newItem);
		newRecipe.addIngredient(1, Material.QUARTZ, 910);
		newRecipe.addIngredient(1, Material.COOKED_BEEF);
		Bukkit.getServer().addRecipe(newRecipe);

		// REGENERATION CHICKEN
		newItem = new ItemStack(Material.COOKED_CHICKEN, 3, (short) 910);
		meta = newItem.getItemMeta();
		meta.setDisplayName(ChatColor.GREEN + "BBQ Chicken");
		meta.setLore(Arrays.asList(ChatColor.GRAY + "Extra crispy",
				ChatColor.GRAY + "Grants " + ChatColor.BLUE
						+ "30min health regeneration"));
		newItem.setItemMeta(meta);
		newRecipe = new ShapelessRecipe(newItem);
		newRecipe.addIngredient(1, Material.QUARTZ, 910);
		newRecipe.addIngredient(1, Material.COOKED_CHICKEN);
		Bukkit.getServer().addRecipe(newRecipe);


		// REGENERATION FISH
		newItem = new ItemStack(Material.COOKED_SALMON, 3, (short) 910);
		meta = newItem.getItemMeta();
		meta.setDisplayName(ChatColor.GREEN + "Fish Filet");
		meta.setLore(Arrays.asList(ChatColor.GRAY + "Fresh from the bay",
				ChatColor.GRAY + "Grants " + ChatColor.BLUE
						+ "30min health regeneration"));
		newItem.setItemMeta(meta);
		newRecipe = new ShapelessRecipe(newItem);
		newRecipe.addIngredient(1, Material.QUARTZ, 910);
		newRecipe.addIngredient(1, Material.COOKED_SALMON);
		Bukkit.getServer().addRecipe(newRecipe);

		// HASTE BREAD
		newItem = new ItemStack(Material.BREAD, 3, (short) 903);
		meta = newItem.getItemMeta();
		meta.setDisplayName(ChatColor.GREEN + "Wonder Bread");
		meta.setLore(Arrays.asList(ChatColor.GRAY + "Hot out of the oven",
				ChatColor.GRAY + "Grants " + ChatColor.BLUE
						+ "15min mining speed"));
		newItem.setItemMeta(meta);
		newRecipe = new ShapelessRecipe(newItem);
		newRecipe.addIngredient(1, Material.QUARTZ, 903);
		newRecipe.addIngredient(1, Material.BREAD);
		Bukkit.getServer().addRecipe(newRecipe);

		// STRENGTH PIE
		newItem = new ItemStack(Material.PUMPKIN_PIE, 3, (short) 905);
		meta = newItem.getItemMeta();
		meta.setDisplayName(ChatColor.GREEN + "Spiced Pumpkin Pie");
		meta.setLore(Arrays.asList(ChatColor.GRAY
				+ "Is it Thanksgiving already?", ChatColor.GRAY + "Grants "
				+ ChatColor.BLUE + "20min strong attacks"));
		newItem.setItemMeta(meta);
		newRecipe = new ShapelessRecipe(newItem);
		newRecipe.addIngredient(1, Material.QUARTZ, 905);
		newRecipe.addIngredient(1, Material.PUMPKIN_PIE);
		Bukkit.getServer().addRecipe(newRecipe);

		// NIGHTVISION PORKCHOP
		newItem = new ItemStack(Material.GRILLED_PORKCHOP, 3, (short) 916);
		meta = newItem.getItemMeta();
		meta.setDisplayName(ChatColor.GREEN + "Porkchop");
		meta.setLore(Arrays.asList(ChatColor.GRAY
				+ "Best served with applesauce", ChatColor.GRAY + "Grants "
				+ ChatColor.BLUE + "1hr nightvision"));
		newItem.setItemMeta(meta);
		newRecipe = new ShapelessRecipe(newItem);
		newRecipe.addIngredient(1, Material.QUARTZ, 916);
		newRecipe.addIngredient(1, Material.GRILLED_PORKCHOP);
		Bukkit.getServer().addRecipe(newRecipe);

		// NIGHTVISION potato
		newItem = new ItemStack(Material.BAKED_POTATO, 3, (short) 916);
		meta = newItem.getItemMeta();
		meta.setDisplayName(ChatColor.GREEN + "Cheesy Baked Potato");
		meta.setLore(Arrays
				.asList(ChatColor.GRAY + "Don't forget the butter!",
						ChatColor.GRAY + "Grants " + ChatColor.BLUE
								+ "1hr nightvision"));
		newItem.setItemMeta(meta);
		newRecipe = new ShapelessRecipe(newItem);
		newRecipe.addIngredient(1, Material.QUARTZ, 916);
		newRecipe.addIngredient(1, Material.BAKED_POTATO);
		Bukkit.getServer().addRecipe(newRecipe);

		// SPEED COOKIE
		newItem = new ItemStack(Material.COOKIE, 3, (short) 901);
		meta = newItem.getItemMeta();
		meta.setDisplayName(ChatColor.GREEN + "Sugar Cookie");
		meta.setLore(Arrays.asList(
				ChatColor.GRAY + "Now with chocolate chips!", ChatColor.GRAY
						+ "Grants " + ChatColor.BLUE + "20min speed boost"));
		newItem.setItemMeta(meta);
		newRecipe = new ShapelessRecipe(newItem);
		newRecipe.addIngredient(1, Material.QUARTZ, 901);
		newRecipe.addIngredient(1, Material.COOKIE);
		Bukkit.getServer().addRecipe(newRecipe);

		// FAITH WEAPON 1
		newRecipe = new ShapelessRecipe(customItemStacks("FAITH_SWORD_1"));
		newRecipe.addIngredient(1, Material.GOLD_NUGGET);
		newRecipe.addIngredient(1, Material.REDSTONE);
		newRecipe.addIngredient(1, Material.INK_SACK, 4);
		newRecipe.addIngredient(1, Material.GOLD_SWORD);
		Bukkit.getServer().addRecipe(newRecipe);

		// FAITH WEAPON 2
		newRecipe = new ShapelessRecipe(customItemStacks("FAITH_SWORD_2"));
		newRecipe.addIngredient(1, Material.GOLD_NUGGET);
		newRecipe.addIngredient(1, Material.REDSTONE);
		newRecipe.addIngredient(1, Material.INK_SACK, 4);
		newRecipe.addIngredient(1, Material.DIAMOND);
		newRecipe.addIngredient(1, Material.GOLD_SWORD);
		Bukkit.getServer().addRecipe(newRecipe);

		// FAITH WEAPON 3
		newRecipe = new ShapelessRecipe(customItemStacks("FAITH_SWORD_3"));
		newRecipe.addIngredient(1, Material.GOLD_NUGGET);
		newRecipe.addIngredient(1, Material.REDSTONE);
		newRecipe.addIngredient(1, Material.INK_SACK, 4);
		newRecipe.addIngredient(1, Material.EMERALD);
		newRecipe.addIngredient(1, Material.GOLD_SWORD);
		newRecipe.addIngredient(1, Material.DIAMOND);
		Bukkit.getServer().addRecipe(newRecipe);
	}
}
