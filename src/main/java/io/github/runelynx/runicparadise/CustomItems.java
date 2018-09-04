package io.github.runelynx.runicparadise;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

//enum CustomItem {
//	DUKE_RING("rp:duke_ring_1")
//	;
//
//	private static ItemStack DUKE_RING_1 = createCustomItem(Material.INK_SAC,
//			ChatColor.DARK_RED + "Duke Tyler's Demonic Ring",
//			new ItemFlag[] {ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS},
//			new String[] {
//					"",
//					ChatColor.GRAY + "An amethyst-studded copper ring enchanted",
//					ChatColor.GRAY + "by the memories of the long-dead Duke Tyler.",
//					ChatColor.GRAY + "Legends say his health was enhanced after",
//					ChatColor.GRAY + "summoning a demon from the underworld.",
//					"",
//					ChatColor.AQUA + "+2  ❤  when worn in the overworld/nether/end",
//					ChatColor.GRAY + "(put this in your ender chest to 'wear' it)",
//			});
//
//	private String key;
//
//	CustomItem(String key) {
//		this.key = key;
//	}
//
//	public String getKey() {
//		return key;
//	}
//
//	private static ItemStack createCustomItem(Material material, String displayName, ItemFlag[] flags, String[] lore) {
//		ItemStack item = new ItemStack(material);
//		ItemMeta meta = item.getItemMeta();
//
//		meta.setDisplayName(displayName);
//		Arrays.stream(flags).forEach(meta::addItemFlags);
//		meta.setLore(Arrays.asList(lore));
//
//		item.setItemMeta(meta);
//
//		return item;
//	}
//}


enum CustomItems {
	DUKE_RING_TYLER,
	DUKE_RING_THING,
	DUKE_RING_CROC,
	DUKE_RING_PENGUIN;

	boolean IsDukeRing() {
		return true;
	}

	private static ItemStack DUKE_RING_TYLER_ITEM = createCustomItem(Material.INK_SAC,
			ChatColor.DARK_RED + "Duke Tyler's Demonic Ring",
			new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS},
			new String[]{
					"",
					ChatColor.GRAY + "An amethyst-studded copper ring enchanted",
					ChatColor.GRAY + "by the memories of the long-dead Duke Tyler.",
					ChatColor.GRAY + "Legends say his health was enhanced after",
					ChatColor.GRAY + "summoning a demon from the underworld.",
					"",
					ChatColor.AQUA + "+2  ❤  when worn in the overworld/nether/end",
					ChatColor.GRAY + "(put this in your ender chest to 'wear' it)",
			});

	private static ItemStack DUKE_RING_THING_ITEM = createCustomItem(Material.INK_SAC,
			ChatColor.DARK_RED + "Duchess Thing's Angelic Ring",
			new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS},
			new String[]{
					"",
					ChatColor.GRAY + "An amethyst-studded copper ring enchanted",
					ChatColor.GRAY + "by the memories of the long-dead Duchess Thing.",
					ChatColor.GRAY + "Legends say her health was enhanced by",
					ChatColor.GRAY + "angels due to her aetheric faith.",
					"",
					ChatColor.AQUA + "+2  ❤  when worn in the overworld/nether/end",
					ChatColor.GRAY + "(put this in your ender chest to 'wear' it)",
			});

	private static ItemStack DUKE_RING_CROC_ITEM = createCustomItem(Material.INK_SAC,
			ChatColor.DARK_RED + "Duke Croc's Reptilian Ring",
			new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS},
			new String[]{
					"",
					ChatColor.GRAY + "An amethyst-studded copper ring enchanted",
					ChatColor.GRAY + "by the memories of the long-dead Duke Croc.",
					ChatColor.GRAY + "Legends say his health was enhanced by",
					ChatColor.GRAY + "a failed experiment that made his",
					ChatColor.GRAY + "skin as tough as a crocodile's.",
					"",
					ChatColor.AQUA + "+2  ❤  when worn in the overworld/nether/end",
					ChatColor.GRAY + "(put this in your ender chest to 'wear' it)",
			});

	private static ItemStack DUKE_RING_PENGUIN_ITEM = createCustomItem(Material.INK_SAC,
			ChatColor.DARK_RED + "Duchess Penguin's Fairy Ring",
			new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS},
			new String[]{
					"",
					ChatColor.GRAY + "An amethyst-studded copper ring enchanted",
					ChatColor.GRAY + "by the memories of the long-dead",
					ChatColor.GRAY + "Duchess Penguin. Rumors say her health",
					ChatColor.GRAY + "was enhanced by fairies who thought",
					ChatColor.GRAY + "she was as carefree as a frolicking penguin.",
					"",
					ChatColor.AQUA + "+2  ❤  when worn in the overworld/nether/end",
					ChatColor.GRAY + "(put this in your ender chest to 'wear' it)",
			});

	static final ItemStack[] DUKE_RINGS = new ItemStack[] {
			DUKE_RING_TYLER_ITEM,
			DUKE_RING_THING_ITEM,
			DUKE_RING_CROC_ITEM,
			DUKE_RING_PENGUIN_ITEM
	};

	private static final String DUKE_ITEM_KEY = "rp:duke_ring";

	static ItemStack createRandomDukeRing(Player player) {
		ItemStack item = DUKE_RINGS[ThreadLocalRandom.current().nextInt(DUKE_RINGS.length)].clone();
		ItemMeta meta = item.getItemMeta();
		meta.addEnchant(Enchantment.LUCK, 1, true);

		List<String> lore = meta.getLore();
		lore.add(ChatColor.DARK_GRAY + player.getUniqueId().toString());
		lore.add(ChatColor.BLACK + DUKE_ITEM_KEY);

		meta.setLore(lore);
		item.setItemMeta(meta);

		return item;
	}

	static boolean isDukeRingFor(ItemStack item, UUID uuid) {
		if (!item.hasItemMeta()) {
			return false;
		}
		ItemMeta meta = item.getItemMeta();
		if (!meta.hasLore()) {
			return false;
		}
		List<String> lore = meta.getLore();
		if (lore.size() < 2) {
			return false;
		}
		String lastButOne = lore.get(lore.size() - 2);
		String last = lore.get(lore.size() - 1);
		return last.endsWith(DUKE_ITEM_KEY) && lastButOne.endsWith(uuid.toString());
	}

	private static ItemStack createCustomItem(Material material, String displayName, ItemFlag[] flags, String[] lore) {
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();

		meta.addEnchant(Enchantment.LUCK, 1, true);
		meta.setDisplayName(displayName);
		Arrays.stream(flags).forEach(meta::addItemFlags);
		meta.setLore(Arrays.asList(lore));

		item.setItemMeta(meta);

		return item;
	}
}