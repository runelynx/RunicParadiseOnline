package io.github.runelynx.runicparadise;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

enum CustomItems {
	RUNESTONE_REGENERATION_STEAK(createCustomItem(Material.COOKED_BEEF, ChatColor.GREEN + "Rib-eye Steak",
			new String[]{ChatColor.GRAY + "Medium-rare!",
					ChatColor.GRAY + "Grants " + ChatColor.BLUE + "30min health regeneration"}, "rp:runestone_regeneration")),
	RUNESTONE_REGENERATION_CHICKEN(createCustomItem(Material.COOKED_CHICKEN, ChatColor.GREEN + "BBQ Chicken",
			new String[]{ChatColor.GRAY + "Extra crispy",
					ChatColor.GRAY + "Grants " + ChatColor.BLUE + "30min health regeneration"}, "rp:runestone_regeneration")),
	RUNESTONE_REGENERATION_SALMON(createCustomItem(Material.COOKED_SALMON, ChatColor.GREEN + "Fish Filet",
			new String[] {ChatColor.GRAY + "Fresh from the bay",
					ChatColor.GRAY + "Grants " + ChatColor.BLUE + "30min health regeneration"}, "rp:runestone_regeneration")),
	RUNESTONE_HASTE_BREAD(createCustomItem(Material.BREAD, ChatColor.GREEN + "Wonder Bread",
			new String[] {ChatColor.GRAY + "Hot out of the oven",
					ChatColor.GRAY + "Grants " + ChatColor.BLUE + "15min mining speed"}, "rp:runestone_haste")),
	RUNESTONE_STRENGTH_PIE(createCustomItem(Material.PUMPKIN_PIE, ChatColor.GREEN + "Spiced Pumpkin Pie",
			new String[] {ChatColor.GRAY + "Is it Thanksgiving already?",
					ChatColor.GRAY + "Grants " + ChatColor.BLUE + "20min strong attacks"}, "rp:runestone_strength")),
	RUNESTONE_NIGHTVISION_PORKCHOP(createCustomItem(Material.COOKED_PORKCHOP, ChatColor.GREEN + "Porkchop",
			new String[] {ChatColor.GRAY + "Best served with applesauce",
					ChatColor.GRAY + "Grants " + ChatColor.BLUE + "1hr nightvision"}, "rp:runestone_nightvision")),
	RUNESTONE_NIGHTVISION_POTATO(createCustomItem(Material.BAKED_POTATO, ChatColor.GREEN + "Cheesy Baked Potato",
			new String[]{ChatColor.GRAY + "Don't forget the butter!",
					ChatColor.GRAY + "Grants " + ChatColor.BLUE + "1hr nightvision"}, "rp:runestone_nightvision")),
	RUNESTONE_SPEED_COOKIE(createCustomItem(Material.COOKIE, ChatColor.GREEN + "Sugar Cookie",
			new String[]{ChatColor.GRAY + "Now with chocolate chips!",
					ChatColor.GRAY + "Grants " + ChatColor.BLUE + "20min speed boost"}, "rp:runestone_speed"))
	;
	private final ItemStack item;

	CustomItems(ItemStack item) {
		this.item = item;
	}

	ItemStack getItem() {
		return item.clone();
	}

	ItemStack getItem(int amount) {
		ItemStack result = item.clone();
		result.setAmount(amount);
		return result;
	}

	String getKey() {
		List<String> lore = item.getItemMeta().getLore();
		return lore.get(lore.size() - 1);
	}

	boolean isRunestone() {
		return getKey().contains("runestone");
	}

	boolean isRegenerationRunestone() {
		return this == RUNESTONE_REGENERATION_STEAK
				|| this == RUNESTONE_REGENERATION_SALMON
				|| this == RUNESTONE_REGENERATION_CHICKEN;
	}

	boolean isHasteRunestone() {
		return this == RUNESTONE_HASTE_BREAD;
	}

	boolean isSpeedRunestone() {
		return this == RUNESTONE_SPEED_COOKIE;
	}

	boolean isStrengthRunestone() {
		return this == RUNESTONE_STRENGTH_PIE;
	}

	boolean isNightvisionRunestone() {
		return this == RUNESTONE_NIGHTVISION_POTATO
				|| this == RUNESTONE_NIGHTVISION_PORKCHOP;
	}

	public static String getKey(ItemStack item) {
		if (item.hasItemMeta()) {
			ItemMeta meta = item.getItemMeta();
			if (meta.hasLore()) {
				List<String> lore = meta.getLore();
				return lore.get(lore.size() - 1);
			}
		}
		return "";
	}

	private static CustomItems[] possibleItems = CustomItems.values();

	public static CustomItems getCustomItem(ItemStack item) {
		return Arrays.stream(possibleItems)
				.filter(x -> x.getKey().equals(getKey(item)) && x.getItem().getType() == item.getType())
				.findFirst()
				.orElse(null);
	}

	private static ItemStack createCustomItem(Material material, String displayName, String[] lore, String key) {
		return createCustomItem(material, displayName, Arrays.asList(lore), key);
	}

	private static ItemStack createCustomItem(Material material, String displayName, String lore, String key) {
		return createCustomItem(material, displayName, Collections.singletonList(lore), key);
	}

	private static ItemStack createCustomItem(Material material, String displayName, List<String> lore, String key) {
		ItemStack result = new ItemStack(material);
		ItemMeta meta = result.getItemMeta();
		meta.setDisplayName(displayName);
		lore = new ArrayList<>(lore);
		lore.add(ChatColor.BLACK + key);
		meta.setLore(lore);
		result.setItemMeta(meta);

		return result;
	}

	public static ItemStack getRegenerationRunestone(Material material) {
		return getRegenerationRunestone(material, 3);
	}

	public static ItemStack getRegenerationRunestone(Material material, int amount) {
		switch (material) {
			case COOKED_BEEF:
				return RUNESTONE_REGENERATION_STEAK.getItem(amount);
			case COOKED_CHICKEN:
				return RUNESTONE_REGENERATION_CHICKEN.getItem(amount);
			case COOKED_SALMON:
				return RUNESTONE_REGENERATION_SALMON.getItem(amount);
		}
		return null;
	}
}