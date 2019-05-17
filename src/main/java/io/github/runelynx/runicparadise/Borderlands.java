package io.github.runelynx.runicparadise;

import io.github.runelynx.runicuniverse.RunicMessaging;
import io.github.runelynx.runicuniverse.RunicMessaging.RunicFormat;
import io.netty.channel.local.LocalEventLoopGroup;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public class Borderlands {
    public static boolean initializeBorderlands() {

        Borderlands.startScheduledTasks();

        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
                "sc " + ChatColor.DARK_RED + "Borderlands has been initialized!");

        return true;
    }

    public static void startScheduledTasks() {
        /*
         * Bukkit.getScheduler().scheduleSyncRepeatingTask(RunicParadise.
         * getInstance(), new Runnable() {
         *
         * @Override public void run() { grantZombieShamanBonus();
         *
         * }
         *
         * }, 200L, 900L);
         */

    }

    public static void handleEntityTargetEventBL(EntityTargetLivingEntityEvent event) {
        // ZombieShaman Regeneration effect

        if (event.getEntity().getCustomName() != null && event.getEntity().getCustomName().contains("Zombie Shaman")
                && event.getTarget() instanceof Player
                && ((LivingEntity) event.getEntity()).getPotionEffect(PotionEffectType.REGENERATION) == null) {
            // Target reason is one that makes us believe this is a mob
            // attacking a player
            // Targeter is Zombie Shaman (BL)
            // Targetee is a Player
            // Targeter does not have regen effect active

            for (Entity e : event.getEntity().getNearbyEntities(20, 20, 20)) {
                if (e.getType() == EntityType.ZOMBIE) {
                    ((Zombie) e).addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 600, 1));
                }

                ((Zombie) event.getEntity()).addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 600, 1));

                if (e instanceof Player) {
                    RunicMessaging.sendMessage(((Player) e), RunicMessaging.RunicFormat.BORDERLANDS,
                            "Zombie Shaman blessed his zombie brothers with regeneration");
                }
            }

        }

    }

	public enum Mobs {
		ZOMBIE_FALLENKNIGHT(2.0, "Fallen Knight Zombie", EntityType.ZOMBIE, "Loot", true, true, 0, 2),
		//
		ZOMBIE_MARAUDER(2.0, "Marauder Zombie", EntityType.ZOMBIE, "Loot", true, true, 0, 2),
		//
		ZOMBIE_GOLIATH(3.0, "Goliath Zombie", EntityType.ZOMBIE, "Loot", true, true, 1, 3),
		//
		ZOMBIE_SHAMAN(1.5, "Shaman Zombie", EntityType.ZOMBIE, "Loot", true, true, 2, 1),
		//
		SKELETON_TOXIC(1.5, "Toxic Skeleton", EntityType.SKELETON, "Loot", true, true, 1, 1),
		//
		SKELETON_SHADE(1.75, "Shade Skeleton", EntityType.SKELETON, "Loot", true, true, 0, 1),
		//
		SKELETON_DRUNKEN(1.75, "Drunken Skeleton", EntityType.SKELETON, "Loot", true, true, 0, 1),
		//
		SKELETON_GRANITE(2.5, "Granite Skeleton", EntityType.SKELETON, "Loot", true, true, 1, 2),
		//
		CREEPER_CRAZED(1.5, "Crazed Creeper", EntityType.CREEPER, "Loot", false, false, 1, 1);

		private double expModifier;
		private String customName;
		private String justTextName;
		private EntityType entityType;
		private String lootTable;
		private Boolean hasArmor;
		private Boolean hasWeapon;
		private int potionEffects;
		private int tier;

        Mobs(double expMod, String name, EntityType type, String loot, Boolean armorFlag, Boolean weaponFlag,
             int pEffectCount, int t) {
			this.expModifier = expMod;
			this.justTextName = name;
			this.entityType = type;
			this.lootTable = loot;
			this.hasArmor = armorFlag;
			this.hasWeapon = weaponFlag;
			this.potionEffects = pEffectCount;

			switch (t) {
			case 1:
				this.customName = ChatColor.GOLD + "☠ " + name;
				break;
			case 2:
				this.customName = ChatColor.RED + "☠ " + name;
				break;
			case 3:
				this.customName = ChatColor.DARK_RED + "☠ " + name;
				break;
			case 4:
				this.customName = ChatColor.DARK_PURPLE + "☠ " + name;
				break;
			default:
				break;
			}
		}
	}

	static boolean spawnBLMob(CreatureSpawnEvent event) {
		if (event.getEntityType() == EntityType.ZOMBIE) {

			// nextInt is normally exclusive of the top value,
			// so add 1 to make it inclusive
			int random = ThreadLocalRandom.current().nextInt(1, 100 + 1);
			int randomSupport = ThreadLocalRandom.current().nextInt(1, 100 + 1);

			if (random <= 30) {
				Borderlands.buildBLMob_Zombie((Zombie) event.getEntity(), Mobs.ZOMBIE_MARAUDER);

			} else if (random <= 60) {
				Borderlands.buildBLMob_Zombie((Zombie) event.getEntity(), Mobs.ZOMBIE_FALLENKNIGHT);

				// 10% chance to spawn a support zombie
				if (randomSupport <= 30) {
					Entity z = event.getLocation().getWorld().spawnEntity(event.getLocation(), EntityType.ZOMBIE);
					Borderlands.buildBLMob_Zombie((Zombie) z, Mobs.ZOMBIE_SHAMAN);
				}

			} else if (random <= 75) {
				Borderlands.buildBLMob_Zombie((Zombie) event.getEntity(), Mobs.ZOMBIE_GOLIATH);

				// 20% chance to spawn a support zombie
				if (randomSupport <= 60) {
					Entity z = event.getLocation().getWorld().spawnEntity(event.getLocation(), EntityType.ZOMBIE);
					Borderlands.buildBLMob_Zombie((Zombie) z, Mobs.ZOMBIE_SHAMAN);
				}
			}  // Just spawn a normal zombie

			return true;

		} else if (event.getEntityType() == EntityType.SKELETON) {
			// nextInt is normally exclusive of the top value,
			// so add 1 to make it inclusive
			int random = ThreadLocalRandom.current().nextInt(1, 100 + 1);

			if (random <= 25) {
				Borderlands.buildBLMob_Skeleton((Skeleton) event.getEntity(), Mobs.SKELETON_TOXIC);

			} else if (random <= 45) {
				Borderlands.buildBLMob_Skeleton((Skeleton) event.getEntity(), Mobs.SKELETON_DRUNKEN);

			} else if (random <= 65) {
				Borderlands.buildBLMob_Skeleton((Skeleton) event.getEntity(), Mobs.SKELETON_SHADE);

			} else if (random <= 75) {
				Borderlands.buildBLMob_Skeleton((Skeleton) event.getEntity(), Mobs.SKELETON_GRANITE);

			}  // Just spawn a normal skeleton


			return true;
		} else if (event.getEntityType() == EntityType.CREEPER) {
			// nextInt is normally exclusive of the top value,
			// so add 1 to make it inclusive
			int random = ThreadLocalRandom.current().nextInt(1, 100 + 1);

			if (random <= 15) {
				Borderlands.buildBLMob_Creeper((Creeper) event.getEntity(), Mobs.CREEPER_CRAZED);

			}  // Just spawn a normal skeleton

			return true;
		} else {

			return false;
		}
	}

	public static void adjustRewardsforBLMobs(EntityDeathEvent event, Player p) {

		Random rand = new Random();
		int value = rand.nextInt(1000);
		boolean isBL = false;
		String playerCurrentRank = RunicParadise.perms.getPrimaryGroup(p);

		double percentChanceForSpecialRankDrop = 0;
		//Bukkit.getLogger().log(Level.INFO, p.getDisplayName() + " adjustRewardsforBLMobs. " + playerCurrentRank);

		// Check if player qualifies to get special rank drops
		if (playerCurrentRank.equalsIgnoreCase("Master") || playerCurrentRank.equalsIgnoreCase("Duke") || playerCurrentRank.equalsIgnoreCase("Baron")
				|| playerCurrentRank.equalsIgnoreCase("Count")) {


			// Check how many special rank drops the player has already received
			if (RunicParadise.playerProfiles.get(p.getUniqueId()).rankDropCountLast24Hours >= 10
					&& !RunicParadise.playerProfiles.get(p.getUniqueId()).isFarming) {
				percentChanceForSpecialRankDrop = .05;
			} else if (RunicParadise.playerProfiles.get(p.getUniqueId()).rankDropCountLast24Hours >= 5
					&& !RunicParadise.playerProfiles.get(p.getUniqueId()).isFarming) {
				percentChanceForSpecialRankDrop = .09;
			} else if (RunicParadise.playerProfiles.get(p.getUniqueId()).rankDropCountLast24Hours >= 4
					&& !RunicParadise.playerProfiles.get(p.getUniqueId()).isFarming) {
				percentChanceForSpecialRankDrop = .17;
			} else if (RunicParadise.playerProfiles.get(p.getUniqueId()).rankDropCountLast24Hours >= 3
					&& !RunicParadise.playerProfiles.get(p.getUniqueId()).isFarming) {
				percentChanceForSpecialRankDrop = .25;
			} else if (RunicParadise.playerProfiles.get(p.getUniqueId()).rankDropCountLast24Hours >= 2
					&& !RunicParadise.playerProfiles.get(p.getUniqueId()).isFarming) {
				percentChanceForSpecialRankDrop = .33;
			} else if (RunicParadise.playerProfiles.get(p.getUniqueId()).rankDropCountLast24Hours <= 1
					&& !RunicParadise.playerProfiles.get(p.getUniqueId()).isFarming) {
				percentChanceForSpecialRankDrop = .41;
			}  // player is farming

			Bukkit.getLogger().log(Level.INFO, p.getDisplayName() + " adjustRewardsforBLMobs. Valid rank found! " + playerCurrentRank + ". Drop chance " + percentChanceForSpecialRankDrop + "Random Number " + value + ". Farming " +
					RunicParadise.playerProfiles.get(p.getUniqueId()).isFarming);

		}

		// ZOMBIES
		if (event.getEntity().getCustomName() != null
				&& event.getEntity().getCustomName().contains("Fallen Knight Zombie")) {

			if (playerCurrentRank.equalsIgnoreCase("Master")) {
				if (value >= 100 && value <= (100 + (1000 * percentChanceForSpecialRankDrop))) {
					p.getLocation().getWorld().dropItemNaturally(p.getLocation(),
							specialLootDrops("DukeGem", p.getUniqueId()));
					RunicParadise.playerProfiles.get(p.getUniqueId()).incrementSpecialRankDrop24HrCount();
					RunicParadise.playerProfiles.get(p.getUniqueId()).logSpecialRankDrop("DukeGem",
							"Fallen Knight Zombie");
				}
			}

			event.setDroppedExp((int) (event.getDroppedExp() * Mobs.ZOMBIE_FALLENKNIGHT.expModifier));
			isBL = true;

		} else if (event.getEntity().getCustomName() != null
				&& event.getEntity().getCustomName().contains("Marauder Zombie")) {

			if (playerCurrentRank.equalsIgnoreCase("Master")) {
				if (value >= 100 && value <= (100 + (1000 * percentChanceForSpecialRankDrop))) {
					p.getLocation().getWorld().dropItemNaturally(p.getLocation(),
							specialLootDrops("DukeGem", p.getUniqueId()));
					RunicParadise.playerProfiles.get(p.getUniqueId()).incrementSpecialRankDrop24HrCount();
					RunicParadise.playerProfiles.get(p.getUniqueId()).logSpecialRankDrop("DukeGem", "Marauder Zombie");
				}
			}

			event.setDroppedExp((int) (event.getDroppedExp() * Mobs.ZOMBIE_MARAUDER.expModifier));
			isBL = true;
		} else if (event.getEntity().getCustomName() != null
				&& event.getEntity().getCustomName().contains("Goliath Zombie")) {

			if (playerCurrentRank.equalsIgnoreCase("Master")) {
				if (value >= 100 && value <= (100 + (1000 * (percentChanceForSpecialRankDrop * 2)))) {
					p.getLocation().getWorld().dropItemNaturally(p.getLocation(),
							specialLootDrops("DukeGem", p.getUniqueId()));
					RunicParadise.playerProfiles.get(p.getUniqueId()).incrementSpecialRankDrop24HrCount();
					RunicParadise.playerProfiles.get(p.getUniqueId()).logSpecialRankDrop("DukeGem", "Goliath Zombie");
				}
			}

			event.setDroppedExp((int) (event.getDroppedExp() * Mobs.ZOMBIE_GOLIATH.expModifier));
			isBL = true;
		} else if (event.getEntity().getCustomName() != null
				&& event.getEntity().getCustomName().contains("Zombie Shaman")) {

			if (playerCurrentRank.equalsIgnoreCase("Master")) {
				if (value >= 100 && value <= (100 + (1000 * percentChanceForSpecialRankDrop))) {
					p.getLocation().getWorld().dropItemNaturally(p.getLocation(),
							specialLootDrops("DukeGem", p.getUniqueId()));
					RunicParadise.playerProfiles.get(p.getUniqueId()).incrementSpecialRankDrop24HrCount();
					RunicParadise.playerProfiles.get(p.getUniqueId()).logSpecialRankDrop("DukeGem", "Zombie Shaman");
				}
			}

			event.setDroppedExp((int) (event.getDroppedExp() * Mobs.ZOMBIE_SHAMAN.expModifier));
			isBL = true;
		} else if (event.getEntity().getCustomName() != null
				&& event.getEntity().getCustomName().contains("Toxic Skeleton")) {

			event.setDroppedExp((int) (event.getDroppedExp() * Mobs.SKELETON_TOXIC.expModifier));
			isBL = true;
		} else if (event.getEntity().getCustomName() != null
				&& event.getEntity().getCustomName().contains("Shade Skeleton")) {

			event.setDroppedExp((int) (event.getDroppedExp() * Mobs.SKELETON_SHADE.expModifier));
			isBL = true;
		} else if (event.getEntity().getCustomName() != null
				&& event.getEntity().getCustomName().contains("Drunken Skeleton")) {

			event.setDroppedExp((int) (event.getDroppedExp() * Mobs.SKELETON_DRUNKEN.expModifier));
			isBL = true;
		} else if (event.getEntity().getCustomName() != null
				&& event.getEntity().getCustomName().contains("Granite Skeleton")) {

			event.setDroppedExp((int) (event.getDroppedExp() * Mobs.SKELETON_GRANITE.expModifier));
			isBL = true;
		} else if (event.getEntity().getCustomName() != null
				&& event.getEntity().getCustomName().contains("Crazed Creeper")) {

			event.setDroppedExp((int) (event.getDroppedExp() * Mobs.CREEPER_CRAZED.expModifier));
			isBL = true;
		} else if (event.getEntity().getType().toString().equalsIgnoreCase("phantom")) {

			//event.setDroppedExp((int) (event.getDroppedExp() * Mobs.CREEPER_CRAZED.expModifier));
			event.getEntity().getLocation().getWorld().dropItemNaturally(event.getEntity().getLocation(), new ItemStack(Material.SPECTRAL_ARROW, 2));
			isBL = true;
		}

		if (value >= 500 && value <= 530 && isBL) { // 5.0% chance (50 out of
													// 1000)
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "crate give to " + p.getName() + " FoodPack 1");
			RunicMessaging.sendMessage(p, RunicFormat.BORDERLANDS,
					"You found a pack left behind by the " + event.getEntity().getCustomName());
		}

	}

	public static void processSkeletonArrows(EntityShootBowEvent event) {
		if (event.getEntity().getCustomName() != null && event.getEntity().getCustomName().contains("Toxic Skeleton")) {
			event.setCancelled(true);

			Location start = event.getProjectile().getLocation();

			double speed = 2.0;
			TippedArrow arrow = (TippedArrow) start.getWorld().spawnEntity(start, EntityType.TIPPED_ARROW);
			arrow.addCustomEffect(new PotionEffect(PotionEffectType.POISON, 100, 0), true);
			Vector direction = event.getProjectile().getVelocity();

			arrow.setShooter(event.getEntity());
			arrow.setVelocity(direction);

		} else if (event.getEntity().getCustomName() != null
				&& event.getEntity().getCustomName().contains("Shade Skeleton")) {

			event.setCancelled(true);

			Location start = event.getProjectile().getLocation();

			double speed = 2.0;
			TippedArrow arrow = (TippedArrow) start.getWorld().spawnEntity(start, EntityType.TIPPED_ARROW);
			arrow.addCustomEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 1), true);
			Vector direction = event.getProjectile().getVelocity();

			arrow.setShooter(event.getEntity());
			arrow.setVelocity(direction);

		} else if (event.getEntity().getCustomName() != null
				&& event.getEntity().getCustomName().contains("Drunken Skeleton")) {

			event.setCancelled(true);

			Location start = event.getProjectile().getLocation();

			double speed = 2.0;
			TippedArrow arrow = (TippedArrow) start.getWorld().spawnEntity(start, EntityType.TIPPED_ARROW);
			arrow.addCustomEffect(new PotionEffect(PotionEffectType.CONFUSION, 140, 1), true);
			Vector direction = event.getProjectile().getVelocity();

			arrow.setShooter(event.getEntity());
			arrow.setVelocity(direction);

		} else if (event.getEntity().getCustomName() != null
				&& event.getEntity().getCustomName().contains("Granite Skeleton")) {

			event.setCancelled(true);

			Location start = event.getProjectile().getLocation();

			double speed = 2.0;
			TippedArrow arrow = (TippedArrow) start.getWorld().spawnEntity(start, EntityType.TIPPED_ARROW);
			arrow.addCustomEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 80, 1), true);
			Vector direction = event.getProjectile().getVelocity();

			arrow.setShooter(event.getEntity());
			arrow.setVelocity(direction);

		}

	}
	/*
	 * public static void creeperCrazed(Creeper crazed) {
	 * 
	 * crazed.setCustomName(ChatColor.GOLD + "Crazed Creeper");
	 * crazed.setCustomNameVisible(true);
	 * 
	 * crazed.setPowered(true); crazed.setRemoveWhenFarAway(true);
	 * 
	 * crazed.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE,
	 * 999999, 1));
	 * 
	 * crazed.setMaxHealth(crazed.getHealth() * 0.75);
	 * 
	 * }
	 * 
	 * public static void skeletonGranite(Skeleton granite) {
	 * 
	 * granite.setCustomName(ChatColor.DARK_RED + "Granite Skeleton");
	 * granite.setCustomNameVisible(true);
	 * 
	 * ItemStack boots = new ItemStack(Material.DIAMOND_BOOTS); ItemMeta meta =
	 * boots.getItemMeta(); meta.setDisplayName(ChatColor.RESET + "" +
	 * ChatColor.LIGHT_PURPLE + "Boots of the Earth");
	 * meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
	 * meta.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4, true);
	 * boots.setItemMeta(meta);
	 * 
	 * ItemStack chest = new ItemStack(Material.DIAMOND_CHESTPLATE); meta =
	 * chest.getItemMeta(); meta.setDisplayName(ChatColor.RESET + "" +
	 * ChatColor.LIGHT_PURPLE + "Chestplate of the Earth");
	 * meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
	 * meta.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4, true);
	 * meta.addEnchant(Enchantment.THORNS, 2, true); chest.setItemMeta(meta);
	 * 
	 * ItemStack legs = new ItemStack(Material.DIAMOND_LEGGINGS); meta =
	 * legs.getItemMeta(); meta.setDisplayName(ChatColor.RESET + "" +
	 * ChatColor.LIGHT_PURPLE + "Legplates of the Earth");
	 * meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
	 * meta.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4, true);
	 * legs.setItemMeta(meta);
	 * 
	 * ItemStack helm = new ItemStack(Material.DIAMOND_HELMET); meta =
	 * helm.getItemMeta(); meta.setDisplayName(ChatColor.RESET + "" +
	 * ChatColor.LIGHT_PURPLE + "Helm of the Earth");
	 * meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
	 * meta.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4, true);
	 * helm.setItemMeta(meta);
	 * 
	 * ItemStack sword = new ItemStack(Material.BOW); ItemMeta meta2 =
	 * sword.getItemMeta(); meta2.setDisplayName(ChatColor.RESET + "" +
	 * ChatColor.LIGHT_PURPLE + "Granite Bow");
	 * 
	 * sword.setItemMeta(meta2);
	 * 
	 * granite.getEquipment().setHelmet(helm);
	 * granite.getEquipment().setHelmetDropChance(0.01F);
	 * granite.getEquipment().setBoots(boots);
	 * granite.getEquipment().setBootsDropChance(0.01F);
	 * granite.getEquipment().setChestplate(chest);
	 * granite.getEquipment().setChestplateDropChance(0.01F);
	 * granite.getEquipment().setLeggings(legs);
	 * granite.getEquipment().setLeggingsDropChance(0.01F);
	 * granite.getEquipment().setItemInMainHand(sword);
	 * granite.getEquipment().setItemInMainHandDropChance(0.40F);
	 * granite.setRemoveWhenFarAway(true);
	 * granite.setMaxHealth(granite.getHealth() * 3);
	 * 
	 * granite.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 9999999,
	 * 1));
	 * 
	 * }
	 * 
	 * public static void skeletonDrunken(Skeleton drunken) {
	 * 
	 * drunken.setCustomName(ChatColor.RED + "Drunken Skeleton");
	 * drunken.setCustomNameVisible(true);
	 * 
	 * ItemStack boots = new ItemStack(Material.LEATHER_BOOTS); ItemMeta meta =
	 * boots.getItemMeta();
	 * meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 2, true);
	 * meta.addEnchant(Enchantment.PROTECTION_PROJECTILE, 2, true);
	 * boots.setItemMeta(meta);
	 * 
	 * ItemStack chest = new ItemStack(Material.LEATHER_CHESTPLATE); meta =
	 * chest.getItemMeta();
	 * meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
	 * meta.addEnchant(Enchantment.PROTECTION_PROJECTILE, 3, true);
	 * chest.setItemMeta(meta);
	 * 
	 * ItemStack legs = new ItemStack(Material.LEATHER_LEGGINGS); meta =
	 * legs.getItemMeta(); meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL,
	 * 2, true); meta.addEnchant(Enchantment.PROTECTION_PROJECTILE, 2, true);
	 * legs.setItemMeta(meta);
	 * 
	 * ItemStack helm = new ItemStack(Material.LEATHER_HELMET); meta =
	 * helm.getItemMeta(); meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL,
	 * 1, true); meta.addEnchant(Enchantment.PROTECTION_PROJECTILE, 3, true);
	 * helm.setItemMeta(meta);
	 * 
	 * ItemStack sword = new ItemStack(Material.BOW); ItemMeta meta2 =
	 * sword.getItemMeta(); meta2.setDisplayName(ChatColor.RESET + "" +
	 * ChatColor.LIGHT_PURPLE + "Drunken Bow");
	 * 
	 * meta2.addEnchant(Enchantment.ARROW_KNOCKBACK, 1, true);
	 * sword.setItemMeta(meta2);
	 * 
	 * drunken.getEquipment().setHelmet(helm);
	 * drunken.getEquipment().setHelmetDropChance(0.01F);
	 * drunken.getEquipment().setBoots(boots);
	 * drunken.getEquipment().setBootsDropChance(0.01F);
	 * drunken.getEquipment().setChestplate(chest);
	 * drunken.getEquipment().setChestplateDropChance(0.01F);
	 * drunken.getEquipment().setLeggings(legs);
	 * drunken.getEquipment().setLeggingsDropChance(0.01F);
	 * drunken.getEquipment().setItemInMainHand(sword);
	 * drunken.getEquipment().setItemInMainHandDropChance(0.40F);
	 * drunken.setRemoveWhenFarAway(true);
	 * drunken.setMaxHealth(drunken.getHealth() * 2);
	 * 
	 * // drunken.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, //
	 * 9999999, 0));
	 * 
	 * }
	 * 
	 * public static void skeletonShade(Skeleton shade) {
	 * 
	 * shade.setCustomName(ChatColor.RED + "Shade Skeleton");
	 * shade.setCustomNameVisible(true);
	 * 
	 * ItemStack boots = new ItemStack(Material.LEATHER_BOOTS); ItemMeta meta =
	 * boots.getItemMeta();
	 * meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 2, true);
	 * meta.addEnchant(Enchantment.PROTECTION_PROJECTILE, 2, true);
	 * meta.addEnchant(Enchantment.FROST_WALKER, 2, true);
	 * boots.setItemMeta(meta);
	 * 
	 * ItemStack chest = new ItemStack(Material.LEATHER_CHESTPLATE); meta =
	 * chest.getItemMeta();
	 * meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 2, true);
	 * meta.addEnchant(Enchantment.PROTECTION_PROJECTILE, 2, true);
	 * chest.setItemMeta(meta);
	 * 
	 * ItemStack legs = new ItemStack(Material.LEATHER_LEGGINGS); meta =
	 * legs.getItemMeta(); meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL,
	 * 2, true); meta.addEnchant(Enchantment.PROTECTION_PROJECTILE, 2, true);
	 * legs.setItemMeta(meta);
	 * 
	 * ItemStack helm = new ItemStack(Material.LEATHER_HELMET); meta =
	 * helm.getItemMeta(); meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL,
	 * 2, true); meta.addEnchant(Enchantment.PROTECTION_PROJECTILE, 2, true);
	 * helm.setItemMeta(meta);
	 * 
	 * ItemStack sword = new ItemStack(Material.BOW); ItemMeta meta2 =
	 * sword.getItemMeta(); meta2.setDisplayName(ChatColor.RESET + "" +
	 * ChatColor.LIGHT_PURPLE + "Shadow Bow");
	 * 
	 * sword.setItemMeta(meta2);
	 * 
	 * shade.getEquipment().setHelmet(helm);
	 * shade.getEquipment().setHelmetDropChance(0.01F);
	 * shade.getEquipment().setBoots(boots);
	 * shade.getEquipment().setBootsDropChance(0.01F);
	 * shade.getEquipment().setChestplate(chest);
	 * shade.getEquipment().setChestplateDropChance(0.01F);
	 * shade.getEquipment().setLeggings(legs);
	 * shade.getEquipment().setLeggingsDropChance(0.01F);
	 * shade.getEquipment().setItemInMainHand(sword);
	 * shade.getEquipment().setItemInMainHandDropChance(0.40F);
	 * shade.setRemoveWhenFarAway(true); shade.setMaxHealth(shade.getHealth() *
	 * 2);
	 * 
	 * // shade.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, //
	 * 9999999, 0));
	 * 
	 * }
	 * 
	 * public static void skeletonToxic(Skeleton toxic) {
	 * 
	 * toxic.setCustomName(ChatColor.RED + "Toxic Skeleton");
	 * toxic.setCustomNameVisible(true);
	 * 
	 * ItemStack boots = new ItemStack(Material.LEATHER_BOOTS); ItemMeta meta =
	 * boots.getItemMeta();
	 * meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 2, true);
	 * meta.addEnchant(Enchantment.PROTECTION_PROJECTILE, 2, true);
	 * boots.setItemMeta(meta);
	 * 
	 * ItemStack chest = new ItemStack(Material.LEATHER_CHESTPLATE); meta =
	 * chest.getItemMeta();
	 * meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 2, true);
	 * meta.addEnchant(Enchantment.PROTECTION_PROJECTILE, 2, true);
	 * chest.setItemMeta(meta);
	 * 
	 * ItemStack legs = new ItemStack(Material.LEATHER_LEGGINGS); meta =
	 * legs.getItemMeta(); meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL,
	 * 2, true); meta.addEnchant(Enchantment.PROTECTION_PROJECTILE, 2, true);
	 * legs.setItemMeta(meta);
	 * 
	 * ItemStack helm = new ItemStack(Material.LEATHER_HELMET); meta =
	 * helm.getItemMeta(); meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL,
	 * 2, true); meta.addEnchant(Enchantment.PROTECTION_PROJECTILE, 2, true);
	 * helm.setItemMeta(meta);
	 * 
	 * ItemStack sword = new ItemStack(Material.BOW); ItemMeta meta2 =
	 * sword.getItemMeta(); meta2.setDisplayName(ChatColor.RESET + "" +
	 * ChatColor.LIGHT_PURPLE + "Toxic Bow");
	 * 
	 * meta2.addEnchant(Enchantment.ARROW_KNOCKBACK, 2, true);
	 * sword.setItemMeta(meta2);
	 * 
	 * toxic.getEquipment().setHelmet(helm);
	 * toxic.getEquipment().setHelmetDropChance(0.01F);
	 * toxic.getEquipment().setBoots(boots);
	 * toxic.getEquipment().setBootsDropChance(0.01F);
	 * toxic.getEquipment().setChestplate(chest);
	 * toxic.getEquipment().setChestplateDropChance(0.01F);
	 * toxic.getEquipment().setLeggings(legs);
	 * toxic.getEquipment().setLeggingsDropChance(0.01F);
	 * toxic.getEquipment().setItemInMainHand(sword);
	 * toxic.getEquipment().setItemInMainHandDropChance(0.40F);
	 * toxic.setRemoveWhenFarAway(true); toxic.setMaxHealth(toxic.getHealth() *
	 * 2);
	 * 
	 * toxic.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 9999999,
	 * 0));
	 * 
	 * }
	 * 
	 * public static void zombieGoliath(Zombie goliath) {
	 * 
	 * goliath.setCustomName(ChatColor.DARK_RED + "Goliath Zombie");
	 * goliath.setCustomNameVisible(true);
	 * 
	 * ItemStack boots = new ItemStack(Material.DIAMOND_BOOTS); ItemMeta meta =
	 * boots.getItemMeta(); meta.setDisplayName(ChatColor.RESET + "" +
	 * ChatColor.LIGHT_PURPLE + "Boots of the Goliath");
	 * meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
	 * meta.addEnchant(Enchantment.PROTECTION_FIRE, 4, true);
	 * meta.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4, true);
	 * boots.setItemMeta(meta);
	 * 
	 * ItemStack chest = new ItemStack(Material.DIAMOND_CHESTPLATE); meta =
	 * chest.getItemMeta(); meta.setDisplayName(ChatColor.RESET + "" +
	 * ChatColor.LIGHT_PURPLE + "Chestplate of the Goliath");
	 * meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
	 * meta.addEnchant(Enchantment.PROTECTION_FIRE, 4, true);
	 * meta.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4, true);
	 * meta.addEnchant(Enchantment.THORNS, 2, true); chest.setItemMeta(meta);
	 * 
	 * ItemStack legs = new ItemStack(Material.DIAMOND_LEGGINGS); meta =
	 * legs.getItemMeta(); meta.setDisplayName(ChatColor.RESET + "" +
	 * ChatColor.LIGHT_PURPLE + "Legplates of the Goliath");
	 * meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
	 * meta.addEnchant(Enchantment.PROTECTION_FIRE, 4, true);
	 * meta.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4, true);
	 * legs.setItemMeta(meta);
	 * 
	 * ItemStack helm = new ItemStack(Material.DIAMOND_HELMET); meta =
	 * helm.getItemMeta(); meta.setDisplayName(ChatColor.RESET + "" +
	 * ChatColor.LIGHT_PURPLE + "Helm of the Goliath");
	 * meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
	 * meta.addEnchant(Enchantment.PROTECTION_FIRE, 4, true);
	 * meta.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4, true);
	 * helm.setItemMeta(meta);
	 * 
	 * ItemStack sword = new ItemStack(Material.WOOD_SWORD); ItemMeta meta2 =
	 * sword.getItemMeta(); meta2.setDisplayName(ChatColor.RESET + "" +
	 * ChatColor.LIGHT_PURPLE + "Wooden Sword");
	 * meta2.addEnchant(Enchantment.DAMAGE_ALL, 1, true);
	 * sword.setItemMeta(meta2);
	 * 
	 * goliath.getEquipment().setHelmet(helm);
	 * goliath.getEquipment().setHelmetDropChance(0.01F);
	 * goliath.getEquipment().setBoots(boots);
	 * goliath.getEquipment().setBootsDropChance(0.01F);
	 * goliath.getEquipment().setChestplate(chest);
	 * goliath.getEquipment().setChestplateDropChance(0.01F);
	 * goliath.getEquipment().setLeggings(legs);
	 * goliath.getEquipment().setLeggingsDropChance(0.01F);
	 * goliath.getEquipment().setItemInMainHand(sword);
	 * goliath.getEquipment().setItemInMainHandDropChance(0.40F);
	 * goliath.setRemoveWhenFarAway(true);
	 * goliath.setMaxHealth(goliath.getHealth() * 3); goliath.setBaby(false);
	 * 
	 * goliath.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 9999999,
	 * 1));
	 * 
	 * }
	 * 
	 * public static void zombieFallenKnight(Zombie knight) {
	 * 
	 * knight.setCustomName(ChatColor.RED + "Fallen Knight Zombie");
	 * knight.setCustomNameVisible(true);
	 * 
	 * ItemStack boots = new ItemStack(Material.IRON_BOOTS); ItemMeta meta =
	 * boots.getItemMeta(); meta.setDisplayName(ChatColor.RESET + "" +
	 * ChatColor.LIGHT_PURPLE + "Fallen Knight Boots");
	 * meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
	 * boots.setItemMeta(meta);
	 * 
	 * ItemStack chest = new ItemStack(Material.IRON_CHESTPLATE); meta =
	 * chest.getItemMeta(); meta.setDisplayName(ChatColor.RESET + "" +
	 * ChatColor.LIGHT_PURPLE + "Fallen Knight Chestplate");
	 * meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
	 * chest.setItemMeta(meta);
	 * 
	 * ItemStack legs = new ItemStack(Material.IRON_LEGGINGS); meta =
	 * legs.getItemMeta(); meta.setDisplayName(ChatColor.RESET + "" +
	 * ChatColor.LIGHT_PURPLE + "Fallen Knight Legplates");
	 * meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
	 * legs.setItemMeta(meta);
	 * 
	 * ItemStack helm = new ItemStack(Material.IRON_HELMET); meta =
	 * helm.getItemMeta(); meta.setDisplayName(ChatColor.RESET + "" +
	 * ChatColor.LIGHT_PURPLE + "Fallen Knight Helm");
	 * meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
	 * helm.setItemMeta(meta);
	 * 
	 * ItemStack sword = new ItemStack(Material.IRON_SWORD); ItemMeta meta2 =
	 * sword.getItemMeta(); meta2.setDisplayName(ChatColor.RESET + "" +
	 * ChatColor.LIGHT_PURPLE + "Fallen Knight Sword");
	 * meta2.addEnchant(Enchantment.DAMAGE_ALL, 1, true);
	 * sword.setItemMeta(meta2);
	 * 
	 * knight.getEquipment().setHelmet(helm);
	 * knight.getEquipment().setHelmetDropChance(0.075F);
	 * knight.getEquipment().setBoots(boots);
	 * knight.getEquipment().setBootsDropChance(0.0075F);
	 * knight.getEquipment().setChestplate(chest);
	 * knight.getEquipment().setChestplateDropChance(0.075F);
	 * knight.getEquipment().setLeggings(legs);
	 * knight.getEquipment().setLeggingsDropChance(0.075F);
	 * knight.getEquipment().setItemInMainHand(sword);
	 * knight.getEquipment().setItemInMainHandDropChance(0.075F);
	 * knight.setRemoveWhenFarAway(true); knight.setMaxHealth(knight.getHealth()
	 * * 1.3); knight.setBaby(false);
	 * 
	 * }
	 * 
	 * public static void zombieShaman(Zombie shaman) {
	 * 
	 * shaman.setCustomName(ChatColor.GOLD + "Zombie Shaman");
	 * shaman.setCustomNameVisible(true);
	 * 
	 * ItemStack boots = new ItemStack(Material.CHAINMAIL_BOOTS); ItemMeta meta
	 * = boots.getItemMeta(); meta.setDisplayName(ChatColor.RESET + "" +
	 * ChatColor.LIGHT_PURPLE + "Blessed Boots");
	 * meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 2, true);
	 * meta.addEnchant(Enchantment.FROST_WALKER, 1, true);
	 * boots.setItemMeta(meta);
	 * 
	 * ItemStack chest = new ItemStack(Material.CHAINMAIL_CHESTPLATE); meta =
	 * chest.getItemMeta(); meta.setDisplayName(ChatColor.RESET + "" +
	 * ChatColor.LIGHT_PURPLE + "Blessed Chestplate");
	 * meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 2, true);
	 * chest.setItemMeta(meta);
	 * 
	 * ItemStack legs = new ItemStack(Material.CHAINMAIL_LEGGINGS); meta =
	 * legs.getItemMeta(); meta.setDisplayName(ChatColor.RESET + "" +
	 * ChatColor.LIGHT_PURPLE + "Blessed Legplates");
	 * meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 2, true);
	 * legs.setItemMeta(meta);
	 * 
	 * ItemStack helm = new ItemStack(Material.CHAINMAIL_HELMET); meta =
	 * helm.getItemMeta(); meta.setDisplayName(ChatColor.RESET + "" +
	 * ChatColor.LIGHT_PURPLE + "Blessed Helm");
	 * meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 2, true);
	 * helm.setItemMeta(meta);
	 * 
	 * ItemStack wand = new ItemStack(Material.BLAZE_ROD); ItemMeta meta2 =
	 * wand.getItemMeta(); meta2.setDisplayName(ChatColor.RESET + "" +
	 * ChatColor.LIGHT_PURPLE + "Blessed Wand");
	 * meta2.addEnchant(Enchantment.DURABILITY, 1, true);
	 * wand.setItemMeta(meta2);
	 * 
	 * shaman.getEquipment().setHelmet(helm);
	 * shaman.getEquipment().setHelmetDropChance(0.075F);
	 * shaman.getEquipment().setBoots(boots);
	 * shaman.getEquipment().setBootsDropChance(0.0075F);
	 * shaman.getEquipment().setChestplate(chest);
	 * shaman.getEquipment().setChestplateDropChance(0.075F);
	 * shaman.getEquipment().setLeggings(legs);
	 * shaman.getEquipment().setLeggingsDropChance(0.075F);
	 * shaman.getEquipment().setItemInMainHand(wand);
	 * shaman.getEquipment().setItemInMainHandDropChance(0.075F);
	 * shaman.setRemoveWhenFarAway(true);
	 * 
	 * shaman.setBaby(false);
	 * 
	 * shaman.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE,
	 * 999999, 1)); shaman.addPotionEffect(new
	 * PotionEffect(PotionEffectType.SPEED, 999999, 1));
	 * shaman.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING,
	 * 999999, 1)); shaman.addPotionEffect(new
	 * PotionEffect(PotionEffectType.ABSORPTION, 999999, 0));
	 * 
	 * }
	 * 
	 * public static void zombieMarauder(Zombie marauder) {
	 * 
	 * marauder.setCustomName(ChatColor.RED + "Marauder Zombie");
	 * marauder.setCustomNameVisible(true);
	 * 
	 * ItemStack boots = new ItemStack(Material.LEATHER_BOOTS); LeatherArmorMeta
	 * meta = (LeatherArmorMeta) boots.getItemMeta();
	 * meta.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE +
	 * "Rogue Boots"); meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1,
	 * true); meta.setColor(Color.BLACK); boots.setItemMeta(meta);
	 * 
	 * ItemStack chest = new ItemStack(Material.LEATHER_CHESTPLATE); meta =
	 * (LeatherArmorMeta) chest.getItemMeta();
	 * meta.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE +
	 * "Rogue Harness"); meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL,
	 * 1, true); meta.setColor(Color.BLACK); chest.setItemMeta(meta);
	 * 
	 * ItemStack legs = new ItemStack(Material.LEATHER_LEGGINGS); meta =
	 * (LeatherArmorMeta) legs.getItemMeta();
	 * meta.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE +
	 * "Rogue Leggings"); meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL,
	 * 1, true); meta.setColor(Color.BLACK); legs.setItemMeta(meta);
	 * 
	 * ItemStack helm = new ItemStack(Material.LEATHER_HELMET); meta =
	 * (LeatherArmorMeta) helm.getItemMeta();
	 * meta.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE +
	 * "Rogue Cap"); meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1,
	 * true); meta.setColor(Color.BLACK); helm.setItemMeta(meta);
	 * 
	 * ItemStack sword = new ItemStack(Material.DIAMOND_SWORD); ItemMeta meta2 =
	 * sword.getItemMeta(); meta2.setDisplayName(ChatColor.RESET + "" +
	 * ChatColor.LIGHT_PURPLE + "Marauder's Ruin");
	 * meta2.addEnchant(Enchantment.DAMAGE_ALL, 2, true);
	 * meta2.addEnchant(Enchantment.FIRE_ASPECT, 1, true);
	 * sword.setItemMeta(meta2);
	 * 
	 * marauder.getEquipment().setHelmet(helm);
	 * marauder.getEquipment().setHelmetDropChance(0.001F);
	 * marauder.getEquipment().setBoots(boots);
	 * marauder.getEquipment().setBootsDropChance(0.001F);
	 * marauder.getEquipment().setChestplate(chest);
	 * marauder.getEquipment().setChestplateDropChance(0.001F);
	 * marauder.getEquipment().setLeggings(legs);
	 * marauder.getEquipment().setLeggingsDropChance(0.001F);
	 * marauder.getEquipment().setItemInMainHand(sword);
	 * marauder.getEquipment().setItemInMainHandDropChance(0.001F);
	 * marauder.setRemoveWhenFarAway(true);
	 * marauder.setMaxHealth(marauder.getHealth() * 0.7);
	 * marauder.setBaby(false);
	 * 
	 * }
	 */

	private static void setMonsterEquipment(LivingEntity monster, Mobs mob) {
		if (mob.hasArmor) {
			monster.getEquipment().setHelmet(Borderlands.mobArmory(mob.justTextName + "Helm"));
			monster.getEquipment().setHelmetDropChance(0.075F);
			monster.getEquipment().setBoots(Borderlands.mobArmory(mob.justTextName + "Boots"));
			monster.getEquipment().setBootsDropChance(0.0075F);
			monster.getEquipment().setChestplate(Borderlands.mobArmory(mob.justTextName + "Chest"));
			monster.getEquipment().setChestplateDropChance(0.075F);
			monster.getEquipment().setLeggings(Borderlands.mobArmory(mob.justTextName + "Legs"));
			monster.getEquipment().setLeggingsDropChance(0.075F);
		}
	}

	private static void buildBLMob_Zombie(Zombie monster, Mobs mob) {

		monster.setCustomName(mob.customName);
		monster.setCustomNameVisible(true);
		monster.setRemoveWhenFarAway(true);
		monster.setBaby(false);


		setMonsterEquipment(monster, mob);
		if (mob.hasWeapon) {
			monster.getEquipment().setItemInMainHand(Borderlands.mobArmory(mob.justTextName + "Weapon"));
			monster.getEquipment().setItemInMainHandDropChance(0.075F);
		}

		// Initially only using up to 2 effects on mobs
		if (mob.potionEffects > 0) {
			monster.addPotionEffect(mobAlchemy(mob.justTextName + "1"));
		}
		if (mob.potionEffects > 1) {
			monster.addPotionEffect(mobAlchemy(mob.justTextName + "2"));
		}
	}

	private static void buildBLMob_Skeleton(Skeleton monster, Mobs mob) {
		monster.setCustomName(mob.customName);
		monster.setCustomNameVisible(true);
		monster.setRemoveWhenFarAway(true);

		setMonsterEquipment(monster, mob);
		if (mob.hasWeapon) {
			monster.getEquipment().setItemInMainHand(Borderlands.mobArmory(mob.justTextName + "Weapon"));
			monster.getEquipment().setItemInMainHandDropChance(0.075F);
		}

		// Initially only using up to 2 effects on mobs
		if (mob.potionEffects > 0) {
			monster.addPotionEffect(mobAlchemy(mob.justTextName + "1"));
		}
		if (mob.potionEffects > 1) {
			monster.addPotionEffect(mobAlchemy(mob.justTextName + "2"));
		}
	}

	private static void buildBLMob_Creeper(Creeper monster, Mobs mob) {
		monster.setCustomName(mob.customName);
		monster.setCustomNameVisible(true);
		monster.setRemoveWhenFarAway(true);

		// Initially only using up to 2 effects on mobs
		if (mob.potionEffects > 0) {
			monster.addPotionEffect(mobAlchemy(mob.justTextName + "1"));
		}
		if (mob.potionEffects > 1) {
			monster.addPotionEffect(mobAlchemy(mob.justTextName + "2"));
		}
	}

	private static PotionEffect mobAlchemy(String key) {
		switch (key) {
		case "Goliath Zombie1":
			return new PotionEffect(PotionEffectType.SLOW, 9999999, 1);
		case "Shaman Zombie1":
			return new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 999999, 1);
		case "Shaman Zombie2":
			return new PotionEffect(PotionEffectType.ABSORPTION, 999999, 0);
		case "Toxic Skeleton1":
			return new PotionEffect(PotionEffectType.SPEED, 9999999, 0);
		case "Granite Skeleton1":
			return new PotionEffect(PotionEffectType.SLOW, 9999999, 1);
		case "Crazed Creeper1":
			return new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 300, 1);
		default:
			// a failsafe. this should not happen!
			return new PotionEffect(PotionEffectType.JUMP, 999999, 0);
		}
	}

	private static ItemStack mobArmory(String key) {
		ItemStack item;
		ItemMeta meta;
		LeatherArmorMeta leathermeta;

		switch (key) {
		case "Marauder ZombieHelm":
			item = new ItemStack(Material.LEATHER_HELMET);
			leathermeta = (LeatherArmorMeta) item.getItemMeta();
			leathermeta.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE + "Rogue Cap");
			leathermeta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
			leathermeta.setColor(Color.BLACK);
			item.setItemMeta(leathermeta);
			return item;
		case "Marauder ZombieChest":
			item = new ItemStack(Material.LEATHER_CHESTPLATE);
			leathermeta = (LeatherArmorMeta) item.getItemMeta();
			leathermeta.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE + "Rogue Harness");
			leathermeta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
			leathermeta.setColor(Color.BLACK);
			item.setItemMeta(leathermeta);
			return item;
		case "Marauder ZombieLegs":
			item = new ItemStack(Material.LEATHER_LEGGINGS);
			leathermeta = (LeatherArmorMeta) item.getItemMeta();
			leathermeta.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE + "Rogue Leggings");
			leathermeta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
			leathermeta.setColor(Color.BLACK);
			item.setItemMeta(leathermeta);
			return item;
		case "Marauder ZombieBoots":
			item = new ItemStack(Material.LEATHER_BOOTS);
			leathermeta = (LeatherArmorMeta) item.getItemMeta();
			leathermeta.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE + "Rogue Boots");
			leathermeta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
			leathermeta.setColor(Color.BLACK);
			item.setItemMeta(leathermeta);
			return item;
		case "Marauder ZombieWeapon":
			item = new ItemStack(Material.DIAMOND_SWORD);
			meta = item.getItemMeta();
			meta.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE + "Marauder Zombie's Ruin");
			meta.addEnchant(Enchantment.DAMAGE_ALL, 2, true);
			meta.addEnchant(Enchantment.FIRE_ASPECT, 1, true);
			item.setItemMeta(meta);
			return item;
		case "Shaman ZombieHelm":
			item = new ItemStack(Material.CHAINMAIL_HELMET);
			meta = item.getItemMeta();
			meta.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE + "Blessed Helm");
			meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 2, true);
			item.setItemMeta(meta);
			return item;
		case "Shaman ZombieChest":
			item = new ItemStack(Material.CHAINMAIL_CHESTPLATE);
			meta = item.getItemMeta();
			meta.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE + "Blessed Chestplate");
			meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 2, true);
			item.setItemMeta(meta);
			return item;
		case "Shaman ZombieLegs":
			item = new ItemStack(Material.CHAINMAIL_LEGGINGS);
			meta = item.getItemMeta();
			meta.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE + "Blessed Legplates");
			meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 2, true);
			item.setItemMeta(meta);
			return item;
		case "Shaman ZombieBoots":
			item = new ItemStack(Material.CHAINMAIL_BOOTS);
			meta = item.getItemMeta();
			meta.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE + "Blessed Boots");
			meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 2, true);
			meta.addEnchant(Enchantment.FROST_WALKER, 1, true);
			item.setItemMeta(meta);
			return item;
		case "Shaman ZombieWeapon":
			item = new ItemStack(Material.BLAZE_ROD);
			meta = item.getItemMeta();
			meta.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE + "Blessed Wand");
			meta.addEnchant(Enchantment.DURABILITY, 1, true);
			item.setItemMeta(meta);
			return item;
		case "Fallen Knight ZombieHelm":
			item = new ItemStack(Material.IRON_HELMET);
			meta = item.getItemMeta();
			meta.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE + "Fallen Knight Helm");
			meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
			item.setItemMeta(meta);
			return item;
		case "Fallen Knight ZombieChest":
			item = new ItemStack(Material.IRON_CHESTPLATE);
			meta = item.getItemMeta();
			meta.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE + "Fallen Knight Chestplate");
			meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
			item.setItemMeta(meta);
			return item;
		case "Fallen Knight ZombieLegs":
			item = new ItemStack(Material.IRON_LEGGINGS);
			meta = item.getItemMeta();
			meta.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE + "Fallen Knight Legplates");
			meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
			item.setItemMeta(meta);
			return item;
		case "Fallen Knight ZombieBoots":
			item = new ItemStack(Material.IRON_BOOTS);
			meta = item.getItemMeta();
			meta.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE + "Fallen Knight Boots");
			meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
			item.setItemMeta(meta);
			return item;
		case "Fallen Knight ZombieWeapon":
			item = new ItemStack(Material.IRON_SWORD);
			meta = item.getItemMeta();
			meta.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE + "Fallen Knight Sword");
			meta.addEnchant(Enchantment.DAMAGE_ALL, 1, true);
			item.setItemMeta(meta);
			return item;
		case "Goliath ZombieHelm":
			item = new ItemStack(Material.DIAMOND_HELMET);
			meta = item.getItemMeta();
			meta.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE + "Helm of the Goliath Zombie");
			meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
			meta.addEnchant(Enchantment.PROTECTION_FIRE, 4, true);
			meta.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4, true);
			item.setItemMeta(meta);
			return item;
		case "Goliath ZombieChest":
			item = new ItemStack(Material.DIAMOND_CHESTPLATE);
			meta = item.getItemMeta();
			meta.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE + "Chestplate of the Goliath Zombie");
			meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
			meta.addEnchant(Enchantment.PROTECTION_FIRE, 4, true);
			meta.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4, true);
			meta.addEnchant(Enchantment.THORNS, 2, true);
			item.setItemMeta(meta);
			return item;
		case "Goliath ZombieLegs":
			item = new ItemStack(Material.DIAMOND_LEGGINGS);
			meta = item.getItemMeta();
			meta.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE + "Legplates of the Goliath Zombie");
			meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
			meta.addEnchant(Enchantment.PROTECTION_FIRE, 4, true);
			meta.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4, true);
			item.setItemMeta(meta);
			return item;
		case "Goliath ZombieBoots":
			item = new ItemStack(Material.DIAMOND_BOOTS);
			meta = item.getItemMeta();
			meta.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE + "Boots of the Goliath Zombie");
			meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4, true);
			meta.addEnchant(Enchantment.PROTECTION_FIRE, 4, true);
			meta.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4, true);
			item.setItemMeta(meta);
			return item;
		case "Goliath ZombieWeapon":
			item = new ItemStack(Material.WOODEN_SWORD);
			meta = item.getItemMeta();
			meta.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE + "Wooden Sword");
			meta.addEnchant(Enchantment.DAMAGE_ALL, 1, true);
			item.setItemMeta(meta);
			return item;
		case "SkeletonHelm":
			item = new ItemStack(Material.LEATHER_HELMET);
			meta = item.getItemMeta();
			meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 2, true);
			meta.addEnchant(Enchantment.PROTECTION_PROJECTILE, 2, true);
			item.setItemMeta(meta);
			return item;
		case "SkeletonChest":
			item = new ItemStack(Material.LEATHER_CHESTPLATE);
			meta = item.getItemMeta();
			meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 2, true);
			meta.addEnchant(Enchantment.PROTECTION_PROJECTILE, 2, true);
			item.setItemMeta(meta);
			return item;
		case "SkeletonLegs":
			item = new ItemStack(Material.LEATHER_LEGGINGS);
			meta = item.getItemMeta();
			meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 2, true);
			meta.addEnchant(Enchantment.PROTECTION_PROJECTILE, 2, true);
			item.setItemMeta(meta);
			return item;
		case "SkeletonBoots":
			item = new ItemStack(Material.LEATHER_BOOTS);
			meta = item.getItemMeta();
			meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 2, true);
			meta.addEnchant(Enchantment.PROTECTION_PROJECTILE, 2, true);
			item.setItemMeta(meta);
			return item;
		case "Toxic SkeletonWeapon":
			item = new ItemStack(Material.BOW);
			meta = item.getItemMeta();
			meta.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE + "Toxic Bow");
			meta.addEnchant(Enchantment.ARROW_KNOCKBACK, 2, true);
			item.setItemMeta(meta);
			return item;
		case "Shade SkeletonWeapon":
			item = new ItemStack(Material.BOW);
			meta = item.getItemMeta();
			meta.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE + "Shadow Bow");
			item.setItemMeta(meta);
			return item;
		case "Drunken SkeletonWeapon":
			item = new ItemStack(Material.BOW);
			meta = item.getItemMeta();
			meta.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE + "Drunken Bow");
			meta.addEnchant(Enchantment.ARROW_KNOCKBACK, 1, true);
			item.setItemMeta(meta);
			return item;
		case "Granite SkeletonWeapon":
			item = new ItemStack(Material.BOW);
			meta = item.getItemMeta();
			meta.setDisplayName(ChatColor.RESET + "" + ChatColor.LIGHT_PURPLE + "Granite Bow");
			item.setItemMeta(meta);
			return item;
		default:
			return null;
		}

	}

	static ItemStack specialLootDrops(String key, UUID playerUUID) {
		ItemStack item;
		ItemMeta meta;
		ArrayList<String> loreText = new ArrayList<>();

		switch (key) {
		case "BaronPendant1":
			item = new ItemStack(Material.LIGHT_BLUE_DYE);
			meta = item.getItemMeta();
			meta.setDisplayName(ChatColor.DARK_RED + "Baron Alex's Harmonic Pendant");
			meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
			meta.addEnchant(Enchantment.LUCK, 1, true);
			loreText.clear();
			loreText.add("");
			loreText.add(ChatColor.GRAY + "A gem-encrusted silver pendant empowered");
			loreText.add(ChatColor.GRAY + "by radiation. Legends say that Baron Alex");
			loreText.add(ChatColor.GRAY + "was the first to discover that unstable metals ");
			loreText.add(ChatColor.GRAY + "disrupted water molecules, allowing the holder");
			loreText.add(ChatColor.GRAY + "to breathe longer while underwater.");
			loreText.add("");
			loreText.add(ChatColor.AQUA + "+3 seconds of air while swimming");
			loreText.add(ChatColor.GRAY + "(put this in your ender chest to 'wear' it)");
			loreText.add(ChatColor.DARK_GRAY + playerUUID.toString());
			meta.setLore(loreText);
			item.setItemMeta(meta);
			return item;
		case "BaronPendant2":
			item = new ItemStack(Material.LIGHT_BLUE_DYE);
			meta = item.getItemMeta();
			meta.setDisplayName(ChatColor.DARK_RED + "Baroness Runa's Dissonant Pendant");
			meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
			meta.addEnchant(Enchantment.LUCK, 1, true);
			loreText.clear();
			loreText.add("");
			loreText.add(ChatColor.GRAY + "A gem-encrusted silver pendant empowered");
			loreText.add(ChatColor.GRAY + "by radiation. Legends say it was constructed");
			loreText.add(ChatColor.GRAY + " by Baroness Runa after her brother drowned.");
			loreText.add(ChatColor.GRAY + "The radiation allows the holder to breathe");
			loreText.add(ChatColor.GRAY + "longer while underwater.");
			loreText.add("");
			loreText.add(ChatColor.AQUA + "+3 seconds of air while swimming");
			loreText.add(ChatColor.GRAY + "(put this in your ender chest to 'wear' it)");
			loreText.add(ChatColor.DARK_GRAY + playerUUID.toString());
			meta.setLore(loreText);
			item.setItemMeta(meta);
			return item;
		case "BaronMetal":
			item = new ItemStack(Material.IRON_NUGGET);
			meta = item.getItemMeta();
			meta.setDisplayName(ChatColor.AQUA + "Shiny Bit of Silver");
			loreText.add("");
			loreText.add(ChatColor.GRAY + "Collect these to form");
			loreText.add(ChatColor.GRAY + "the Pendant of the Baron.");
			loreText.add(ChatColor.GRAY + "See " + ChatColor.YELLOW + "/warp BaronPendant" + ChatColor.GRAY
					+ " for more info.");
			meta.setLore(loreText);
			item.setItemMeta(meta);

			return item;
		case "BaronGem":
			item = new ItemStack(Material.LIGHT_BLUE_DYE);
			meta = item.getItemMeta();
			meta.setDisplayName(ChatColor.AQUA + "Prismatic Jewel of the Nether Baron");
			loreText.add("");
			loreText.add(ChatColor.GRAY + "This glowing jewel is part");
			loreText.add(ChatColor.GRAY + "of the Pendant of the Baron.");
			loreText.add(ChatColor.GRAY + "See " + ChatColor.YELLOW + "/warp BaronPendant" + ChatColor.GRAY
					+ " for more info.");
			// loreText.add(ChatColor.GRAY + "");
			// loreText.add(ChatColor.DARK_GRAY + playerUUID.toString());
			meta.setLore(loreText);
			item.setItemMeta(meta);

			return item;
		case "BaronIngot1":
			item = new ItemStack(Material.IRON_INGOT);
			meta = item.getItemMeta();
			meta.setDisplayName(ChatColor.AQUA + "Empowered Silver Ingot");
			loreText.add("");
			loreText.add(ChatColor.GRAY + "This buzzing ingot is part");
			loreText.add(ChatColor.GRAY + "of the Pendant of the Baron.");
			loreText.add(ChatColor.GRAY + "See " + ChatColor.YELLOW + "/warp BaronPendant" + ChatColor.GRAY
					+ " for more info.");
			// loreText.add(ChatColor.GRAY + "");
			// loreText.add(ChatColor.DARK_GRAY + playerUUID.toString());
			meta.setLore(loreText);
			item.setItemMeta(meta);

			return item;
		case "BaronIngot2":
			item = new ItemStack(Material.IRON_INGOT);
			meta = item.getItemMeta();
			meta.setDisplayName(ChatColor.AQUA + "Unstable Silver Ingot");
			loreText.add("");
			loreText.add(ChatColor.GRAY + "This pulsing ingot is part");
			loreText.add(ChatColor.GRAY + "of the Pendant of the Baron.");
			loreText.add(ChatColor.GRAY + "See " + ChatColor.YELLOW + "/warp BaronPendant" + ChatColor.GRAY
					+ " for more info.");
			// loreText.add(ChatColor.GRAY + "");
			// loreText.add(ChatColor.DARK_GRAY + playerUUID.toString());
			meta.setLore(loreText);
			item.setItemMeta(meta);

			return item;
		case "DukeGem":
			item = new ItemStack(Material.PURPLE_DYE);
			meta = item.getItemMeta();
			meta.setDisplayName(ChatColor.AQUA + "Fragment of a Perfect Amethyst");
			loreText.add("");
			loreText.add(ChatColor.GRAY + "Collect 64 to form the gemstone");
			loreText.add(ChatColor.GRAY + "for the Ring of the Duke.");
			loreText.add(
					ChatColor.GRAY + "See " + ChatColor.YELLOW + "/warp DukeRing" + ChatColor.GRAY + " for more info.");
			loreText.add(ChatColor.GRAY + "");
			loreText.add(ChatColor.DARK_GRAY + playerUUID.toString());
			meta.setLore(loreText);
			item.setItemMeta(meta);

			return item;
		case "DukeMetal":
			item = new ItemStack(Material.GOLD_NUGGET);
			meta = item.getItemMeta();
			meta.setDisplayName(ChatColor.AQUA + "Shiny Bit of Copper");
			loreText.add("");
			loreText.add(ChatColor.GRAY + "Collect 64 to form the metal");
			loreText.add(ChatColor.GRAY + "for the Ring of the Duke.");
			loreText.add(
					ChatColor.GRAY + "See " + ChatColor.YELLOW + "/warp DukeRing" + ChatColor.GRAY + " for more info.");
			loreText.add(ChatColor.GRAY + "");
			loreText.add(ChatColor.DARK_GRAY + playerUUID.toString());
			meta.setLore(loreText);
			item.setItemMeta(meta);
			return item;
		case "DukeEssence":
			item = new ItemStack(Material.GUNPOWDER);
			meta = item.getItemMeta();
			meta.setDisplayName(ChatColor.AQUA + "Crystallized Memory of Ancient Dukes");
			loreText.add("");
			loreText.add(ChatColor.GRAY + "Collect 64 to form the memory");
			loreText.add(ChatColor.GRAY + "for the Ring of the Duke.");
			loreText.add(
					ChatColor.GRAY + "See " + ChatColor.YELLOW + "/warp DukeRing" + ChatColor.GRAY + " for more info.");
			loreText.add(ChatColor.GRAY + "");
			loreText.add(ChatColor.DARK_GRAY + playerUUID.toString());
			meta.setLore(loreText);
			item.setItemMeta(meta);
			return item;
		case "DukeRing1":
			item = new ItemStack(Material.PURPLE_DYE);
			meta = item.getItemMeta();
			meta.setDisplayName(ChatColor.DARK_RED + "Duke Tyler's Demonic Ring");
			meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
			meta.addEnchant(Enchantment.LUCK, 1, true);
			loreText.clear();
			loreText.add("");
			loreText.add(ChatColor.GRAY + "An amethyst-studded copper ring enchanted");
			loreText.add(ChatColor.GRAY + "by the memories of the long-dead Duke Tyler.");
			loreText.add(ChatColor.GRAY + "Legends say his health was enhanced after");
			loreText.add(ChatColor.GRAY + "summoning a demon from the underworld.");
			loreText.add("");
			loreText.add(ChatColor.AQUA + "+2  ❤  when worn in the overworld/nether/end");
			loreText.add(ChatColor.GRAY + "(put this in your ender chest to 'wear' it)");
			loreText.add(ChatColor.DARK_GRAY + playerUUID.toString());
			meta.setLore(loreText);
			item.setItemMeta(meta);
			return item;
		case "DukeRing2":
			item = new ItemStack(Material.PURPLE_DYE);
			meta = item.getItemMeta();
			meta.setDisplayName(ChatColor.DARK_RED + "Duchess Thing's Angelic Ring");
			meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
			meta.addEnchant(Enchantment.LUCK, 1, true);
			loreText.clear();
			loreText.add("");
			loreText.add(ChatColor.GRAY + "An amethyst-studded copper ring enchanted");
			loreText.add(ChatColor.GRAY + "by the memories of the long-dead Duchess Thing.");
			loreText.add(ChatColor.GRAY + "Legends say her health was enhanced by");
			loreText.add(ChatColor.GRAY + "angels due to her aetheric faith.");
			loreText.add("");
			loreText.add(ChatColor.AQUA + "+2  ❤  when worn in the overworld/nether/end");
			loreText.add(ChatColor.GRAY + "(put this in your ender chest to 'wear' it)");
			loreText.add(ChatColor.DARK_GRAY + playerUUID.toString());
			meta.setLore(loreText);
			item.setItemMeta(meta);
			return item;
		case "DukeRing3":
			item = new ItemStack(Material.PURPLE_DYE);
			meta = item.getItemMeta();
			meta.setDisplayName(ChatColor.DARK_RED + "Duke Croc's Reptilian Ring");
			meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
			meta.addEnchant(Enchantment.LUCK, 1, true);
			loreText.clear();
			loreText.add("");
			loreText.add(ChatColor.GRAY + "An amethyst-studded copper ring enchanted");
			loreText.add(ChatColor.GRAY + "by the memories of the long-dead Duke Croc.");
			loreText.add(ChatColor.GRAY + "Legends say his health was enhanced by");
			loreText.add(ChatColor.GRAY + "a failed experiment that made his");
			loreText.add(ChatColor.GRAY + "skin as tough as a crocodile's.");
			loreText.add("");
			loreText.add(ChatColor.AQUA + "+2  ❤  when worn in the overworld/nether/end");
			loreText.add(ChatColor.GRAY + "(put this in your ender chest to 'wear' it)");
			loreText.add(ChatColor.DARK_GRAY + playerUUID.toString());
			meta.setLore(loreText);
			item.setItemMeta(meta);
			return item;
		case "DukeRing4":
			item = new ItemStack(Material.PURPLE_DYE);
			meta = item.getItemMeta();
			meta.setDisplayName(ChatColor.DARK_RED + "Duchess Penguin's Fairy Ring");
			meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
			meta.addEnchant(Enchantment.LUCK, 1, true);
			loreText.clear();
			loreText.add("");
			loreText.add(ChatColor.GRAY + "An amethyst-studded copper ring enchanted");
			loreText.add(ChatColor.GRAY + "by the memories of the long-dead");
			loreText.add(ChatColor.GRAY + "Duchess Penguin. Rumors say her health");
			loreText.add(ChatColor.GRAY + "was enhanced by fairies who thought");
			loreText.add(ChatColor.GRAY + "she was as carefree as a frolicking penguin.");
			loreText.add("");
			loreText.add(ChatColor.AQUA + "+2  ❤  when worn in the overworld/nether/end");
			loreText.add(ChatColor.GRAY + "(put this in your ender chest to 'wear' it)");
			loreText.add(ChatColor.DARK_GRAY + playerUUID.toString());
			meta.setLore(loreText);
			item.setItemMeta(meta);
			return item;
		default:
			throw new RuntimeException("Can't find key" + key);
		}
	}
}
