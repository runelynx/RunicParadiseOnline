package io.github.runelynx.runicparadise;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

class Recipes {
	static ItemStack customItemStacks(String key) {
		ItemStack newItem;
		ItemMeta meta;

		switch (key) {
			case "FAITH_SWORD_1":
					newItem = new ItemStack(Material.GOLDEN_SWORD, 1);
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
					newItem = new ItemStack(Material.GOLDEN_SWORD, 1);
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
					newItem = new ItemStack(Material.GOLDEN_SWORD, 1);
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
					newItem = new ItemStack(Material.GOLDEN_AXE, 1);
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
					throw new RuntimeException("No faith sword found with this key:" + key);
		}
	}

	private static ItemStack matchCraftingRunestone(List<ItemStack> i) {
		ItemStack ingredient;
		Material other;

		if (i.get(0).getType() == Material.QUARTZ) {
			ingredient = i.get(0);
			other = i.get(1).getType();
		} else if (i.get(1).getType() == Material.QUARTZ) {
			ingredient = i.get(1);
			other = i.get(0).getType();
		} else {
			return null;
		}

		String quartzKey = CustomItems.getKey(ingredient);
		if (quartzKey.equals("rp:runestone_regeneration_ingredient")) {
			return CustomItems.getRegenerationRunestone(other);
		}
		if (quartzKey.equals("rp:runestone_haste_ingredient") && other == Material.BREAD) {
			return CustomItems.RUNESTONE_HASTE_BREAD.getItem(3);
		}
		if (quartzKey.equals("rp:runestone_strength_ingredient") && other == Material.PUMPKIN_PIE) {
			return CustomItems.RUNESTONE_STRENGTH_PIE.getItem(3);
		}
		if (quartzKey.equals("rp:runestone_nightvision_ingredient")) {
			if (other == Material.PORKCHOP) {
				return CustomItems.RUNESTONE_NIGHTVISION_PORKCHOP.getItem(3);
			}
			if (other == Material.BAKED_POTATO) {
				return CustomItems.RUNESTONE_NIGHTVISION_POTATO.getItem(3);
			}
		}
		if (quartzKey.equals("rp:runestone_speed_cookie_ingredient") && other == Material.COOKIE) {
			return CustomItems.RUNESTONE_SPEED_COOKIE.getItem(3);
		}
		return null;
	}

	static ItemStack customFoodRecipesNew(CraftingInventory inventory) {
		List<ItemStack> i = Arrays.stream(inventory.getMatrix()).filter(Objects::nonNull).collect(Collectors.toList());
		if (i.size() != 2) {
			return null;
		}

		ItemStack result = matchCraftingRunestone(i);
		if (result != null) {
			return result;
		}

		return null;
	}

    static boolean customRecipes() {
		// ROTTEN FLESH INTO LEATHER
		FurnaceRecipe fleshLeather = new FurnaceRecipe(new ItemStack(Material.LEATHER), Material.ROTTEN_FLESH);
		Bukkit.getServer().addRecipe(fleshLeather);

		ShapelessRecipe newRecipe;

		// FAITH WEAPON 1
		newRecipe = new ShapelessRecipe(new NamespacedKey(RunicParadise.getInstance(), "RP_FAITH_SWORD_1"), customItemStacks("FAITH_SWORD_1"));
		newRecipe.addIngredient(1, Material.GOLD_NUGGET);
		newRecipe.addIngredient(1, Material.REDSTONE);
		newRecipe.addIngredient(1, Material.LAPIS_LAZULI);
		newRecipe.addIngredient(1, Material.GOLDEN_SWORD);
		Bukkit.getServer().addRecipe(newRecipe);

		// FAITH WEAPON 2
		newRecipe = new ShapelessRecipe(new NamespacedKey(RunicParadise.getInstance(), "RP_FAITH_SWORD_2"), customItemStacks("FAITH_SWORD_2"));
		newRecipe.addIngredient(1, Material.GOLD_NUGGET);
		newRecipe.addIngredient(1, Material.REDSTONE);
		newRecipe.addIngredient(1, Material.LAPIS_LAZULI);
		newRecipe.addIngredient(1, Material.DIAMOND);
		newRecipe.addIngredient(1, Material.GOLDEN_SWORD);
		Bukkit.getServer().addRecipe(newRecipe);

		// FAITH WEAPON 3
		newRecipe = new ShapelessRecipe(new NamespacedKey(RunicParadise.getInstance(), "RP_FAITH_SWORD_3"), customItemStacks("FAITH_SWORD_3"));
		newRecipe.addIngredient(1, Material.GOLD_NUGGET);
		newRecipe.addIngredient(1, Material.REDSTONE);
		newRecipe.addIngredient(1, Material.LAPIS_LAZULI);
		newRecipe.addIngredient(1, Material.EMERALD);
		newRecipe.addIngredient(1, Material.GOLDEN_SWORD);
		newRecipe.addIngredient(1, Material.DIAMOND);
		Bukkit.getServer().addRecipe(newRecipe);

        return true;
	}
}
