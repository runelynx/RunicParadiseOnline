/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package io.github.runelynx.runicparadise;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 *
 * @author Andrew
 */
public class Borderlands {

	public final static int ZOMBIE_MARAUDER_EXP_MULT = 2;
	public final static int ZOMBIE_KNIGHT_EXP_MULT = 2;
	public final static int ZOMBIE_GOLIATH_EXP_MULT = 3;
	public final static int ZOMBIE_SHAMAN_EXP_MULT = 1;

	public static HashMap<UUID, Zombie> zombieShamanTracker = new HashMap<UUID, Zombie>();

	public static void startScheduledTasks() {
		Bukkit.getScheduler().scheduleSyncRepeatingTask(
				RunicParadise.getInstance(), new Runnable() {

					@Override
					public void run() {
						grantZombieShamanBonus();

					}

				}, 200L, 900L);

		Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
				"sc Starting Borderlands timed job");
	}

	public static void grantZombieShamanBonus() {
		for (Zombie shaman : zombieShamanTracker.values()) {
		
			if (shaman == null || shaman.isDead()) {
				zombieShamanTracker.remove(shaman.getUniqueId());
	
			} else {
				for (Entity e : shaman.getNearbyEntities(15, 15, 15)) {
					if (e.getType() == EntityType.ZOMBIE) {
						((Zombie) e).addPotionEffect(new PotionEffect(
								PotionEffectType.REGENERATION, 600, 1));
					
					}
				}
			}

		}
	}

	public static void zombieGoliath(Zombie goliath) {

		goliath.setCustomName(ChatColor.DARK_RED + "Goliath Zombie");
		goliath.setCustomNameVisible(true);

		ItemStack boots = new ItemStack(Material.DIAMOND_BOOTS);
		ItemMeta meta = boots.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE
				+ "Boots of the Goliath");
		meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
		meta.addEnchant(Enchantment.PROTECTION_FIRE, 4, true);
		meta.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4, true);
		meta.addEnchant(Enchantment.THORNS, 4, true);
		boots.setItemMeta(meta);

		ItemStack chest = new ItemStack(Material.DIAMOND_CHESTPLATE);
		meta = chest.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE
				+ "Chestplate of the Goliath");
		meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
		meta.addEnchant(Enchantment.PROTECTION_FIRE, 4, true);
		meta.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4, true);
		meta.addEnchant(Enchantment.THORNS, 4, true);
		chest.setItemMeta(meta);

		ItemStack legs = new ItemStack(Material.DIAMOND_LEGGINGS);
		meta = legs.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE
				+ "Legplates of the Goliath");
		meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
		meta.addEnchant(Enchantment.PROTECTION_FIRE, 4, true);
		meta.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4, true);
		meta.addEnchant(Enchantment.THORNS, 4, true);
		legs.setItemMeta(meta);

		ItemStack helm = new ItemStack(Material.DIAMOND_HELMET);
		meta = helm.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE
				+ "Helm of the Goliath");
		meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
		meta.addEnchant(Enchantment.PROTECTION_FIRE, 4, true);
		meta.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4, true);
		meta.addEnchant(Enchantment.THORNS, 4, true);
		helm.setItemMeta(meta);

		ItemStack sword = new ItemStack(Material.WOOD_SWORD);
		ItemMeta meta2 = sword.getItemMeta();
		meta2.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE
				+ "Wooden Sword");
		meta2.addEnchant(Enchantment.DAMAGE_ALL, 1, true);
		sword.setItemMeta(meta2);

		goliath.getEquipment().setHelmet(helm);
		goliath.getEquipment().setHelmetDropChance(0.01F);
		goliath.getEquipment().setBoots(boots);
		goliath.getEquipment().setBootsDropChance(0.01F);
		goliath.getEquipment().setChestplate(chest);
		goliath.getEquipment().setChestplateDropChance(0.01F);
		goliath.getEquipment().setLeggings(legs);
		goliath.getEquipment().setLeggingsDropChance(0.01F);
		goliath.getEquipment().setItemInMainHand(sword);
		goliath.getEquipment().setItemInMainHandDropChance(0.40F);
		goliath.setRemoveWhenFarAway(true);
		goliath.setMaxHealth(goliath.getHealth() * 3);
		goliath.setBaby(false);

		goliath.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,
				9999999, 1));

	}

	public static void zombieFallenKnight(Zombie knight) {

		knight.setCustomName(ChatColor.RED + "Fallen Knight Zombie");
		knight.setCustomNameVisible(true);

		ItemStack boots = new ItemStack(Material.IRON_BOOTS);
		ItemMeta meta = boots.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE
				+ "Fallen Knight Boots");
		meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
		boots.setItemMeta(meta);

		ItemStack chest = new ItemStack(Material.IRON_CHESTPLATE);
		meta = chest.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE
				+ "Fallen Knight Chestplate");
		meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
		chest.setItemMeta(meta);

		ItemStack legs = new ItemStack(Material.IRON_LEGGINGS);
		meta = legs.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE
				+ "Fallen Knight Legplates");
		meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
		legs.setItemMeta(meta);

		ItemStack helm = new ItemStack(Material.IRON_HELMET);
		meta = helm.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE
				+ "Fallen Knight Helm");
		meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
		helm.setItemMeta(meta);

		ItemStack sword = new ItemStack(Material.IRON_SWORD);
		ItemMeta meta2 = sword.getItemMeta();
		meta2.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE
				+ "Fallen Knight Sword");
		meta2.addEnchant(Enchantment.DAMAGE_ALL, 1, true);
		sword.setItemMeta(meta2);

		knight.getEquipment().setHelmet(helm);
		knight.getEquipment().setHelmetDropChance(0.075F);
		knight.getEquipment().setBoots(boots);
		knight.getEquipment().setBootsDropChance(0.0075F);
		knight.getEquipment().setChestplate(chest);
		knight.getEquipment().setChestplateDropChance(0.075F);
		knight.getEquipment().setLeggings(legs);
		knight.getEquipment().setLeggingsDropChance(0.075F);
		knight.getEquipment().setItemInMainHand(sword);
		knight.getEquipment().setItemInMainHandDropChance(0.075F);
		knight.setRemoveWhenFarAway(true);
		knight.setMaxHealth(knight.getHealth() * 1.3);
		knight.setBaby(false);

	}

	public static void zombieShaman(Zombie shaman) {

		shaman.setCustomName(ChatColor.GOLD + "Zombie Shaman");
		shaman.setCustomNameVisible(true);

		ItemStack boots = new ItemStack(Material.CHAINMAIL_BOOTS);
		ItemMeta meta = boots.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE
				+ "Blessed Boots");
		meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
		boots.setItemMeta(meta);

		ItemStack chest = new ItemStack(Material.CHAINMAIL_CHESTPLATE);
		meta = chest.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE
				+ "Blessed Chestplate");
		meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
		chest.setItemMeta(meta);

		ItemStack legs = new ItemStack(Material.CHAINMAIL_LEGGINGS);
		meta = legs.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE
				+ "Blessed Legplates");
		meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
		legs.setItemMeta(meta);

		ItemStack helm = new ItemStack(Material.CHAINMAIL_HELMET);
		meta = helm.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE
				+ "Blessed Helm");
		meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
		helm.setItemMeta(meta);

		ItemStack wand = new ItemStack(Material.BLAZE_ROD);
		ItemMeta meta2 = wand.getItemMeta();
		meta2.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE
				+ "Blessed Wand");
		meta2.addEnchant(Enchantment.DURABILITY, 1, true);
		wand.setItemMeta(meta2);

		shaman.getEquipment().setHelmet(helm);
		shaman.getEquipment().setHelmetDropChance(0.075F);
		shaman.getEquipment().setBoots(boots);
		shaman.getEquipment().setBootsDropChance(0.0075F);
		shaman.getEquipment().setChestplate(chest);
		shaman.getEquipment().setChestplateDropChance(0.075F);
		shaman.getEquipment().setLeggings(legs);
		shaman.getEquipment().setLeggingsDropChance(0.075F);
		shaman.getEquipment().setItemInMainHand(wand);
		shaman.getEquipment().setItemInMainHandDropChance(0.075F);
		shaman.setRemoveWhenFarAway(true);

		shaman.setBaby(false);

		shaman.addPotionEffect(new PotionEffect(
				PotionEffectType.FIRE_RESISTANCE, 999999, 1));
		shaman.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 999999,
				1));
		shaman.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING,
				999999, 1));
		shaman.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION,
				999999, 1));

		zombieShamanTracker.put(shaman.getUniqueId(), shaman);
	}

	public static void zombieMarauder(Zombie marauder) {

		marauder.setCustomName(ChatColor.RED + "Marauder Zombie");
		marauder.setCustomNameVisible(true);

		ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
		LeatherArmorMeta meta = (LeatherArmorMeta) boots.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE
				+ "Rogue Boots");
		meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
		meta.setColor(Color.BLACK);
		boots.setItemMeta(meta);

		ItemStack chest = new ItemStack(Material.LEATHER_CHESTPLATE);
		meta = (LeatherArmorMeta) chest.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE
				+ "Rogue Harness");
		meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
		meta.setColor(Color.BLACK);
		chest.setItemMeta(meta);

		ItemStack legs = new ItemStack(Material.LEATHER_LEGGINGS);
		meta = (LeatherArmorMeta) legs.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE
				+ "Rogue Leggings");
		meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
		meta.setColor(Color.BLACK);
		legs.setItemMeta(meta);

		ItemStack helm = new ItemStack(Material.LEATHER_HELMET);
		meta = (LeatherArmorMeta) helm.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE
				+ "Rogue Cap");
		meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
		meta.setColor(Color.BLACK);
		helm.setItemMeta(meta);

		ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
		ItemMeta meta2 = sword.getItemMeta();
		meta2.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE
				+ "Marauder's Ruin");
		meta2.addEnchant(Enchantment.DAMAGE_ALL, 2, true);
		meta2.addEnchant(Enchantment.FIRE_ASPECT, 1, true);
		sword.setItemMeta(meta2);

		marauder.getEquipment().setHelmet(helm);
		marauder.getEquipment().setHelmetDropChance(0.075F);
		marauder.getEquipment().setBoots(boots);
		marauder.getEquipment().setBootsDropChance(0.075F);
		marauder.getEquipment().setChestplate(chest);
		marauder.getEquipment().setChestplateDropChance(0.075F);
		marauder.getEquipment().setLeggings(legs);
		marauder.getEquipment().setLeggingsDropChance(0.075F);
		marauder.getEquipment().setItemInMainHand(sword);
		marauder.getEquipment().setItemInMainHandDropChance(0.075F);
		marauder.setRemoveWhenFarAway(true);
		marauder.setMaxHealth(marauder.getHealth() * 0.7);
		marauder.setBaby(false);

	}

}
