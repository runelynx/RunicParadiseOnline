package io.github.runelynx.runicparadise;

import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

class RunicSerialization {
	static String serializeTry(ItemStack[] itemStacks) {
		for (ItemStack i : itemStacks) {
			Map<String, Object> obj = i.serialize();
			YamlConfiguration conf = new YamlConfiguration();
			conf.set("item1", i);
			String out = conf.saveToString();

			try {
				conf = new YamlConfiguration();
				conf.loadFromString(out);
				Object o = conf.getItemStack("item1");
				int x = 5;
			} catch (InvalidConfigurationException e) {
				e.printStackTrace();
			}
			return out;

//			return obj.toString();
		}
		return null;
	}

	static List<HashMap<Map<String, Object>, Map<String, Object>>> serializeItemStackList(
			final ItemStack[] itemStackList) {
		final List<HashMap<Map<String, Object>, Map<String, Object>>> serializedItemStackList = new ArrayList<>();

		for (ItemStack itemStack : itemStackList) {
			Map<String, Object> serializedItemStack, serializedItemMeta;
			HashMap<Map<String, Object>, Map<String, Object>> serializedMap = new HashMap<>();

			if (itemStack == null)
				itemStack = new ItemStack(Material.AIR);
			serializedItemMeta = (itemStack.hasItemMeta()) ? itemStack
					.getItemMeta().serialize() : null;
			itemStack.setItemMeta(null);
			serializedItemStack = itemStack.serialize();

			serializedMap.put(serializedItemStack, serializedItemMeta);
			serializedItemStackList.add(serializedMap);
		}
		return serializedItemStackList;
	}

	static ItemStack[] deserializeItemStackList(
			final List<HashMap<Map<String, Object>, Map<String, Object>>> serializedItemStackList) {
		final ItemStack[] itemStackList = new ItemStack[serializedItemStackList
				.size()];

		int i = 0;
		for (HashMap<Map<String, Object>, Map<String, Object>> serializedItemStackMap : serializedItemStackList) {
			Entry<Map<String, Object>, Map<String, Object>> serializedItemStack = serializedItemStackMap
					.entrySet().iterator().next();

			ItemStack itemStack = ItemStack.deserialize(serializedItemStack
					.getKey());
			if (serializedItemStack.getValue() != null) {
				ItemMeta itemMeta = (ItemMeta) ConfigurationSerialization
						.deserializeObject(serializedItemStack.getValue(),
								ConfigurationSerialization
										.getClassByAlias("ItemMeta"));
				itemStack.setItemMeta(itemMeta);
			}

			itemStackList[i++] = itemStack;
		}
		return itemStackList;
	}
}
