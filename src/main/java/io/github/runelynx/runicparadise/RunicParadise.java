/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.runelynx.runicparadise;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Material;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;

import net.milkbowl.vault.permission.Permission;
import net.minecraft.server.v1_8_R1.ItemStack;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Guardian;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.RegisteredServiceProvider;

import de.slikey.effectlib.EffectLib;
import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.ExplodeEffect;
import de.slikey.effectlib.effect.FountainEffect;
import de.slikey.effectlib.effect.MusicEffect;
import de.slikey.effectlib.effect.ShieldEffect;
import de.slikey.effectlib.effect.SkyRocketEffect;
import de.slikey.effectlib.effect.SmokeEffect;
import de.slikey.effectlib.util.ParticleEffect;

/**
 *
 * @author runelynx
 */
public final class RunicParadise extends JavaPlugin implements Listener {

	private static Plugin instance;
	private static final Logger log = Logger.getLogger("Minecraft");
	public static Permission perms = null;
	public static Economy economy = null;
	public static HashMap<UUID, Powers> powersMap = new HashMap<UUID, Powers>();
	public static HashMap<String, Zombie> powersSwordOfJupiterMap = new HashMap<String, Zombie>();

	Ranks ranks = new Ranks();

	public static Plugin getInstance() {
		return instance;
	}

	@SuppressWarnings("deprecation")
	public void onEnable() {
		instance = this;

		getConfig().options().copyDefaults(true);
		saveConfig();

		getLogger().info("RunicParadise Plugin: onEnable has been invoked!");
		String tempInvSetting = "gamerule keepInventory "
				+ instance.getConfig().getString("keepInventoryOnDeathEnabled");
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), tempInvSetting);
		tempInvSetting = "mvrule keepInventory true RunicSky";
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), tempInvSetting);
		tempInvSetting = "mvrule keepInventory true RunicRealm";
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), tempInvSetting);
		tempInvSetting = "mvrule keepInventory true RunicRealm_nether";
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), tempInvSetting);
		tempInvSetting = "mvrule keepInventory true RunicRealm_the_end";
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), tempInvSetting);

		// get the object from vault API for Permission class
		setupPermissions();
		setupEconomy();

		Recipes.customFoodRecipes();

		RunicDeathChest.syncGraveLocations();

		for (Player p : Bukkit.getOnlinePlayers()) {
			powersMap.put(p.getUniqueId(), new Powers(p.getUniqueId()));
			log.log(Level.INFO, "Powers Debug: Mapped " + p.getName()
					+ "; Beasts "
					+ powersMap.get(p.getUniqueId()).getSkillBeasts()
					+ "; BeastsState "
					+ powersMap.get(p.getUniqueId()).getStatusBeasts());
		}

		// This will throw a NullPointerException if you don't have the command
		// defined in your plugin.yml file!
		getCommand("rp").setExecutor(new Commands());
		getCommand("rptest").setExecutor(new Commands());
		getCommand("rpreload").setExecutor(new Commands());
		getCommand("rpgames").setExecutor(new Commands());
		getCommand("games").setExecutor(new Commands());
		getCommand("hmsay").setExecutor(new Commands());
		getCommand("promote").setExecutor(new Commands());
		getCommand("rankup").setExecutor(new Commands());
		getCommand("ranks").setExecutor(new Commands());
		getCommand("sc").setExecutor(new Commands());
		getCommand("staffchat").setExecutor(new Commands());
		getCommand("staff").setExecutor(new Commands());
		getCommand("music").setExecutor(new Commands());
		getCommand("radio").setExecutor(new Commands());
		getCommand("ready").setExecutor(new Commands());
		getCommand("rpmail").setExecutor(new Commands());
		getCommand("rptokens").setExecutor(new Commands());
		getCommand("say").setExecutor(new Commands());
		getCommand("rpchest").setExecutor(new Commands());
		getCommand("grave").setExecutor(new Commands());
		getCommand("graves").setExecutor(new Commands());
		getCommand("rptransfer").setExecutor(new Commands());
		getCommand("rpvote").setExecutor(new Commands());
		getCommand("rpjobs").setExecutor(new Commands());
		getCommand("rpeffects").setExecutor(new Commands());
		getCommand("punish").setExecutor(new Commands());
		getCommand("powers").setExecutor(new Commands());

		Bukkit.getServer().getPluginManager().registerEvents(this, this);

		// Establish MySQL connection
		int dbPort;
		try {
			dbPort = Integer.parseInt(instance.getConfig().getString("dbPort"));
		} catch (Exception e) {
			dbPort = 3301;
			System.out
					.println("[RunicParadise] Config file field dbPort not an integer! Using 3301 as default.");
		}
		final MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));

		// Save online players to DB every minute
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		scheduler.runTaskTimerAsynchronously(this, new Runnable() {
			@Override
			public void run() {
				Connection z = MySQL.openConnection();
				ArrayList<UUID> PlayerIDs = new ArrayList<UUID>();
				for (Player all : getServer().getOnlinePlayers()) {
					PlayerIDs.add(all.getUniqueId());
				}

				try {
					// clear the table
					Statement insertStmt = z.createStatement();
					insertStmt.executeUpdate("DELETE FROM rp_PlayersOnline;");
				} catch (SQLException e) {
					getLogger().log(
							Level.SEVERE,
							"Could not reset PlayersOnline table! because: "
									+ e.getMessage());
				}

				// Clear the table
				Date date = new Date();

				for (UUID uuid : PlayerIDs) {
					if (Bukkit.getPlayer(uuid).hasPermission("rp.admin")) {
						try {
							Statement insertStmt = z.createStatement();
							insertStmt
									.executeUpdate("INSERT INTO rp_PlayersOnline (`PlayerName`, `UUID`, `Staff`, `Type`, `Timestamp`) VALUES ('"
											+ Bukkit.getPlayer(uuid).getName()
											+ "', '"
											+ uuid
											+ "', '1', 'Admin', '"
											+ date.getTime() + "');");

						} catch (SQLException e) {
							getLogger().log(
									Level.SEVERE,
									"Could not update PlayersOnline ADMIN! because: "
											+ e.getMessage());
						}

					} else if (getServer().getPlayer(uuid).hasPermission(
							"rp.mod")) {
						try {
							Statement insertStmt = z.createStatement();
							insertStmt
									.executeUpdate("INSERT INTO rp_PlayersOnline (`PlayerName`, `UUID`, `Staff`, `Type`, `Timestamp`) VALUES ('"
											+ Bukkit.getPlayer(uuid).getName()
											+ "', '"
											+ uuid.toString()
											+ "', '1', 'Mod', '"
											+ date.getTime() + "');");

						} catch (SQLException e) {
							getLogger().log(
									Level.SEVERE,
									"Could not update PlayersOnline MOD! because: "
											+ e.getMessage());
						}

					} else if (getServer().getPlayer(uuid).hasPermission(
							"rp.staff.helper")) {
						try {
							Statement insertStmt = z.createStatement();
							insertStmt
									.executeUpdate("INSERT INTO rp_PlayersOnline (`PlayerName`, `UUID`, `Staff`, `Type`, `Timestamp`) VALUES ('"
											+ Bukkit.getPlayer(uuid).getName()
											+ "', '"
											+ uuid.toString()
											+ "', '1', 'Helper', '"
											+ date.getTime() + "');");

						} catch (SQLException e) {
							getLogger().log(
									Level.SEVERE,
									"Could not update PlayersOnline  because: "
											+ e.getMessage());
						}

					} else {
						try {
							Statement insertStmt = z.createStatement();
							insertStmt
									.executeUpdate("INSERT INTO rp_PlayersOnline (`PlayerName`, `UUID`, `Staff`, `Type`, `Timestamp`) VALUES ('"
											+ Bukkit.getPlayer(uuid).getName()
											+ "', '"
											+ uuid.toString()
											+ "', '0', 'None', '"
											+ date.getTime() + "');");

						} catch (SQLException e) {
							getLogger().log(
									Level.SEVERE,
									"Could not update PlayersOnline nonstaff! because: "
											+ e.getMessage());
						}

					}

				}

				try {

					z.close();
				} catch (SQLException e) {
					getLogger().log(
							Level.SEVERE,
							"Could not update close PlayersOnline update  conn! because: "
									+ e.getMessage());
				}
			}
		}, 0L, 1200L);

	}

	public void onDisable() {
		// TODO Insert logic to be performed when the plugin is disabled
		getLogger().info("RunicParadise Plugin: onDisable has been invoked!");
		powersMap.clear();
		getLogger().info("RP Powers: Powers map has been cleared.");
		// em.dispose();
	}

	@EventHandler
	public void onPlayerChat(AsyncPlayerChatEvent event) {
		// rf[5Adminf] {jobs} 5{name}f: %2$s
		// ADMINS
		if (event.getPlayer().hasPermission("rp.staff.admin")) {
			event.setFormat(ChatColor.DARK_PURPLE + "" + ChatColor.ITALIC
					+ "Admin" + ChatColor.RESET + " " + ChatColor.DARK_PURPLE
					+ perms.getPrimaryGroup(event.getPlayer()) + ChatColor.GRAY
					+ " {jobs}" + ChatColor.DARK_PURPLE
					+ event.getPlayer().getDisplayName() + ChatColor.WHITE
					+ ": %2$s");
			// ELDER MOD
		} else if (event.getPlayer().hasPermission("rp.staff.mod+")) {
			event.setFormat(ChatColor.DARK_RED + "" + ChatColor.ITALIC + "Mod+"
					+ ChatColor.RESET + " " + ChatColor.DARK_RED
					+ perms.getPrimaryGroup(event.getPlayer()) + ChatColor.GRAY
					+ " {jobs}" + ChatColor.DARK_RED
					+ event.getPlayer().getDisplayName() + ChatColor.WHITE
					+ ": %2$s");
			// MOD
		} else if (event.getPlayer().hasPermission("rp.staff.mod")) {
			event.setFormat(ChatColor.RED + "" + ChatColor.ITALIC + "Mod"
					+ ChatColor.RESET + " " + ChatColor.RED
					+ perms.getPrimaryGroup(event.getPlayer()) + ChatColor.GRAY
					+ " {jobs}" + ChatColor.RED
					+ event.getPlayer().getDisplayName() + ChatColor.WHITE
					+ ": %2$s");
			// HELPER
		} else if (event.getPlayer().hasPermission("rp.staff.helper")) {
			event.setFormat(ChatColor.LIGHT_PURPLE + "" + ChatColor.ITALIC
					+ "Helper" + ChatColor.RESET + " " + ChatColor.LIGHT_PURPLE
					+ perms.getPrimaryGroup(event.getPlayer()) + ChatColor.GRAY
					+ " {jobs}" + ChatColor.LIGHT_PURPLE
					+ event.getPlayer().getDisplayName() + ChatColor.WHITE
					+ ": %2$s");
			// EVERYONE ELSE
		} else {
			event.setFormat(event.getFormat().replace("{staff} ", "")
					.replace("{name}", event.getPlayer().getDisplayName()));
		}

		// CENSOR!
		if (event.getMessage().contains("fuck")
				|| event.getMessage().contains("shit")) {
			event.setMessage(event.getMessage().replace("fuck",
					ChatColor.MAGIC + "ABCD"));
			event.setMessage(event.getMessage().replace("shit",
					ChatColor.MAGIC + "ABCD"));
		}

	}

	/*
	@EventHandler
	public void onBlockRedstone(BlockRedstoneEvent event) {

		if (event.getBlock().getType() == Material.REDSTONE_LAMP_ON
				&& RunicDeathChest.checkHashmapForDeathLoc(
						event.getBlock().getLocation()).equals("Locked")) {
			event.setNewCurrent(100);
		}

	}
	*/

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void onBreakBlock(BlockBreakEvent event) {

		if ((event.getBlock().getType() == Material.REDSTONE_LAMP_ON || event
				.getBlock().getType() == Material.REDSTONE_LAMP_OFF)
				&& !RunicDeathChest.checkHashmapForDeathLoc(
						event.getBlock().getLocation()).equals("NoGrave")) {

			event.setCancelled(true);
			event.getPlayer().sendMessage(
					ChatColor.DARK_GRAY + "[RunicReaper] " + ChatColor.GRAY
							+ "Knocking over graves is bad luck!");
			getServer().dispatchCommand(
					getServer().getConsoleSender(),
					"effect " + event.getPlayer().getName()
							+ " CONFUSION 10 10");
		} else if ((event.getBlock().getType() == Material.SIGN || event
				.getBlock().getType() == Material.SIGN_POST)
				&& !RunicDeathChest.checkHashmapForDeathLoc(
						event.getBlock().getLocation().subtract(0, 1, 0))
						.equals("NoGrave")) {
			event.setCancelled(true);
			event.getPlayer().sendMessage(
					ChatColor.DARK_GRAY + "[RunicReaper] " + ChatColor.GRAY
							+ "Knocking over graves is bad luck!");
			getServer().dispatchCommand(getServer().getConsoleSender(),
					"effect " + event.getPlayer().getName() + " BLINDNESS 3 5");
		} else if (event.getBlock().getType() == Material.MOB_SPAWNER
				&& !event.getPlayer().hasPermission("rp.staff")) {
			event.setCancelled(true);
			event.getPlayer().sendMessage(
					ChatColor.DARK_RED
							+ "Hey put that back! Only staff can break that.");
		}

	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {

			if (event.getClickedBlock().getType()
					.equals(Material.REDSTONE_LAMP_OFF)
					|| event.getClickedBlock().getType()
							.equals(Material.REDSTONE_LAMP_ON)) {

				String graveOwnerName = RunicDeathChest.checkLocForDeath(event
						.getClickedBlock().getLocation());

				if (graveOwnerName.equals(event.getPlayer().getName())
						|| graveOwnerName.equals("Unlocked")) {
					// Player at grave is owner... or grave is unlocked!
					RunicDeathChest.restoreFromPlayerDeath(
							new RunicPlayerBukkit(event.getPlayer()
									.getUniqueId()), event.getClickedBlock()
									.getLocation());
				} else if (!graveOwnerName.equals("NoGrave")) {
					// player clicked a redstone lamp which is not a death chest
					event.getPlayer().sendMessage(
							ChatColor.DARK_GRAY + "[RunicReaper] "
									+ ChatColor.GRAY + "This grave belongs to "
									+ ChatColor.DARK_RED + graveOwnerName
									+ ChatColor.GRAY + ".");
				} else {
					// player clicked a redstone lamp which is not a grave
					// do nothing

				}
			} else {
				// not a redstone lamp
			}
		}

	}

	@EventHandler
	public void onPlayerItemConsume(final PlayerItemConsumeEvent pice) {

		switch (pice.getItem().getDurability()) {
		case 910:
			pice.getPlayer().addPotionEffect(
					new PotionEffect(PotionEffectType.REGENERATION, 36000, 2));
			break;
		case 901:
			pice.getPlayer().addPotionEffect(
					new PotionEffect(PotionEffectType.SPEED, 18000, 1));
			break;
		case 916:
			pice.getPlayer().addPotionEffect(
					new PotionEffect(PotionEffectType.NIGHT_VISION, 72000, 1));
			break;
		case 903:
			pice.getPlayer().addPotionEffect(
					new PotionEffect(PotionEffectType.FAST_DIGGING, 18000, 1));
			break;
		case 905:
			pice.getPlayer()
					.addPotionEffect(
							new PotionEffect(PotionEffectType.INCREASE_DAMAGE,
									24000, 2));
			break;
		default:
			break;

		}
	}

	@EventHandler
	public void onPlayerJoin(final PlayerJoinEvent pje) {

		// Launch Firework on player join
		powersMap.put(pje.getPlayer().getUniqueId(), new Powers(pje.getPlayer()
				.getUniqueId()));
		log.log(Level.INFO, "RP Powers: Added " + pje.getPlayer().getName()
				+ " to powers map.");
		updatePlayerInfoOnJoin(pje.getPlayer().getName(), pje.getPlayer()
				.getUniqueId());
		/*
		 * Bukkit.getServer().getScheduler() .scheduleSyncDelayedTask(this, new
		 * Runnable() { public void run() {
		 * 
		 * ranks.convertRanks(pje.getPlayer()); Firework f = (Firework) pje
		 * .getPlayer() .getWorld() .spawn(pje.getPlayer().getLocation(),
		 * Firework.class); FireworkMeta fm = f.getFireworkMeta();
		 * 
		 * fm.addEffect(FireworkEffect.builder().flicker(false)
		 * .trail(true).with(Type.BALL) .with(Type.BALL_LARGE).with(Type.STAR)
		 * .withColor(Color.ORANGE) .withColor(Color.YELLOW).withFade(Color.RED)
		 * .withFade(Color.PURPLE).build()); fm.setPower(2);
		 * f.setFireworkMeta(fm); } }, 20); // delay
		 */

	}

	@EventHandler
	public void onPlayerQuit(final PlayerQuitEvent pje) {

		powersMap.remove(pje.getPlayer().getUniqueId());
		log.log(Level.INFO, "RP Powers: Removed " + pje.getPlayer().getName()
				+ " from powers map.");

		updatePlayerInfoOnQuit(pje.getPlayer().getName(), pje.getPlayer()
				.getUniqueId());

	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerDamage(final EntityDamageEvent ede) {

		// If player falls into the void, heal and teleport them to spawn
		if (ede.getCause() == DamageCause.VOID) {
			if (ede.getEntity() instanceof Player) {
				Player player = (Player) ede.getEntity();
				player.setHealth(20);
				// player.teleport(player.getWorld().getSpawnLocation());
				String cmd = "sudo " + player.getName() + " spawn";
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);

				player.sendMessage(ChatColor.AQUA
						+ "[RunicSavior] Found you lost in the void... watch your step next time!");
			}
			/*
			 * } else if (ede.getEntity() instanceof Player &&
			 * powersSwordOfJupiterMap.containsKey(ede.getEntity().getName())) {
			 * //check if player has a sword of jupiter zombie in the hashmap
			 * log.log(Level.INFO,
			 * "Recvd player dmg event. Player has a SOJ zombie record in map."
			 * ); if
			 * (powersSwordOfJupiterMap.get(ede.getEntity().getUniqueId()).
			 * isDead() ||
			 * powersSwordOfJupiterMap.get(ede.getEntity().getUniqueId
			 * ()).isEmpty()) { //if zombie doesnt exist anymore, then clear the
			 * record from hashmap log.log(Level.INFO,
			 * "Recvd player dmg event. Player had a SOJ zombie that is now gone. Clearing map."
			 * ); powersSwordOfJupiterMap.remove(ede.getEntity().getUniqueId());
			 * } else if (ede.getEntity().getLastDamageCause().getEntity()
			 * instanceof LivingEntity) {
			 * powersSwordOfJupiterMap.get(ede.getEntity
			 * ().getUniqueId()).setTarget
			 * ((LivingEntity)ede.getEntity().getLastDamageCause().getEntity());
			 * log.log(Level.INFO,
			 * "Recvd player dmg event. Player has a SOJ zombie. Set target to damage cause entity."
			 * ); }
			 */

		}

	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onEntityDamageByEntity(final EntityDamageByEntityEvent edbe) {

		// If player falls into the void, heal and teleport them to spawn
		if (edbe.getDamager() instanceof Player
				&& edbe.getDamager().getName().equals("runelynx")) {
			if (Bukkit.getPlayer(edbe.getDamager().getName()).getItemInHand()
					.getType().equals(Material.DIAMOND_SWORD)) {

				RunicPlayerBukkit targetPlayer = new RunicPlayerBukkit(edbe
						.getDamager().getUniqueId());
				targetPlayer.sendMessageToPlayer(ChatColor.GOLD + ""
						+ ChatColor.ITALIC + "Your sword's strike sings!");

				EffectManager em = new EffectManager(instance);

				ExplodeEffect explosionEffect = new ExplodeEffect(em);

				// Blood-particles lays around for 30 ticks (1.5 seconds)
				// Bleeding takes 15 seconds
				// period * iterations = time of effect
				explosionEffect.setLocation(edbe.getEntity().getLocation());
				explosionEffect.start();

				SkyRocketEffect skyRocketEffect = new SkyRocketEffect(em);

				// Blood-particles lays around for 30 ticks (1.5 seconds)
				// Bleeding takes 15 seconds
				// period * iterations = time of effect
				skyRocketEffect.power = 30;
				skyRocketEffect.setTargetEntity(edbe.getEntity());
				skyRocketEffect.start();

				em.disposeOnTermination();
			} else if (Bukkit.getPlayer(edbe.getDamager().getName())
					.getItemInHand().getType().equals(Material.BOW)) {
				RunicPlayerBukkit targetPlayer = new RunicPlayerBukkit(edbe
						.getDamager().getUniqueId());
				targetPlayer.sendMessageToPlayer(ChatColor.GOLD + ""
						+ ChatColor.ITALIC + "Wheeeeeeee!!");

				EffectManager em = new EffectManager(instance);

				ShieldEffect shieldEffect = new ShieldEffect(em);

				// Blood-particles lays around for 30 ticks (1.5 seconds)
				// Bleeding takes 15 seconds
				// period * iterations = time of effect
				shieldEffect.particle = ParticleEffect.NOTE;
				shieldEffect.iterations = 5;
				shieldEffect.setLocation(edbe.getDamager().getLocation());
				shieldEffect.start();

				em.disposeOnTermination();

			}
		}

	}

	@EventHandler
	public void onEntityDeath(final EntityDeathEvent ede) {

		// check for Monsters... Flying=Ghast... Slime=Slime/Magmacube...
		// WaterMob = Squid
		if (ede.getEntity() instanceof LivingEntity) {

			final LivingEntity monsterEnt = (LivingEntity) ede.getEntity();
			if (monsterEnt.getKiller() == null
					|| !(monsterEnt.getKiller() instanceof Player)
					|| ede.getEntity().getWorld().equals("plotworld")) {
				// [RP] Entity death detected but player=null or world=plotworld
				// OR killer not a player
				// so nothing recorded!
				return;
			}

			Bukkit.getServer().getScheduler()
					.runTaskAsynchronously(instance, new Runnable() {
						public void run() {

							String mobType = "";

							// check for elder guardians
							if (monsterEnt.getType() == EntityType.GUARDIAN) {
								if (((Guardian) monsterEnt).isElder()) {
									mobType = "ELDER_GUARDIAN";
								} else {
									mobType = "GUARDIAN";
								}
							} else {
								mobType = monsterEnt.getType().toString();
							}

							switch (mobType) {
							case "ZOMBIE":
								RunicPlayerBukkit.incrementPlayerKillCount(
										monsterEnt.getKiller().getUniqueId(),
										"KillZombie");
								break;
							case "IRON_GOLEM":
								RunicPlayerBukkit.incrementPlayerKillCount(
										monsterEnt.getKiller().getUniqueId(),
										"KillIronGolem");
								break;
							case "WITHER":
								RunicPlayerBukkit.incrementPlayerKillCount(
										monsterEnt.getKiller().getUniqueId(),
										"KillWither");
								break;
							case "SKELETON":
								RunicPlayerBukkit.incrementPlayerKillCount(
										monsterEnt.getKiller().getUniqueId(),
										"KillSkeleton");
								break;
							case "SLIME":
								RunicPlayerBukkit.incrementPlayerKillCount(
										monsterEnt.getKiller().getUniqueId(),
										"KillSlime");
								break;
							case "MAGMA_CUBE":
								RunicPlayerBukkit.incrementPlayerKillCount(
										monsterEnt.getKiller().getUniqueId(),
										"KillMagmaCube");
								break;
							case "WITCH":
								RunicPlayerBukkit.incrementPlayerKillCount(
										monsterEnt.getKiller().getUniqueId(),
										"KillWitch");
								break;
							case "SILVERFISH":
								RunicPlayerBukkit.incrementPlayerKillCount(
										monsterEnt.getKiller().getUniqueId(),
										"KillSilverfish");
								break;
							case "GIANT":
								RunicPlayerBukkit.incrementPlayerKillCount(
										monsterEnt.getKiller().getUniqueId(),
										"KillGiant");
								break;
							case "BLAZE":
								RunicPlayerBukkit.incrementPlayerKillCount(
										monsterEnt.getKiller().getUniqueId(),
										"KillBlaze");
								break;
							case "CREEPER":
								RunicPlayerBukkit.incrementPlayerKillCount(
										monsterEnt.getKiller().getUniqueId(),
										"KillCreeper");
								break;
							case "ENDERMAN":
								RunicPlayerBukkit.incrementPlayerKillCount(
										monsterEnt.getKiller().getUniqueId(),
										"KillEnderman");
								break;
							case "SPIDER":
								RunicPlayerBukkit.incrementPlayerKillCount(
										monsterEnt.getKiller().getUniqueId(),
										"KillSpider");
								break;
							case "CAVE_SPIDER":
								RunicPlayerBukkit.incrementPlayerKillCount(
										monsterEnt.getKiller().getUniqueId(),
										"KillCaveSpider");
								break;
							case "SQUID":
								RunicPlayerBukkit.incrementPlayerKillCount(
										monsterEnt.getKiller().getUniqueId(),
										"KillSquid");
								break;
							case "ENDER_DRAGON":
								RunicPlayerBukkit.incrementPlayerKillCount(
										monsterEnt.getKiller().getUniqueId(),
										"KillEnderDragon");
								break;
							case "PIG_ZOMBIE":
								RunicPlayerBukkit.incrementPlayerKillCount(
										monsterEnt.getKiller().getUniqueId(),
										"KillPigZombie");
								break;
							case "GHAST":
								RunicPlayerBukkit.incrementPlayerKillCount(
										monsterEnt.getKiller().getUniqueId(),
										"KillGhast");
								break;
							case "CHICKEN":
								RunicPlayerBukkit.incrementPlayerKillCount(
										monsterEnt.getKiller().getUniqueId(),
										"KillChicken");
								break;
							case "COW":
								RunicPlayerBukkit.incrementPlayerKillCount(
										monsterEnt.getKiller().getUniqueId(),
										"KillCow");
								break;
							case "SHEEP":
								RunicPlayerBukkit.incrementPlayerKillCount(
										monsterEnt.getKiller().getUniqueId(),
										"KillSheep");
								break;
							case "PIG":
								RunicPlayerBukkit.incrementPlayerKillCount(
										monsterEnt.getKiller().getUniqueId(),
										"KillPig");
								break;
							case "OCELOT":
								RunicPlayerBukkit.incrementPlayerKillCount(
										monsterEnt.getKiller().getUniqueId(),
										"KillOcelot");
								break;
							case "BAT":
								RunicPlayerBukkit.incrementPlayerKillCount(
										monsterEnt.getKiller().getUniqueId(),
										"KillBat");
								break;
							case "MUSHROOM_COW":
								RunicPlayerBukkit.incrementPlayerKillCount(
										monsterEnt.getKiller().getUniqueId(),
										"KillMooshroom");
								break;
							case "RABBIT":
								RunicPlayerBukkit.incrementPlayerKillCount(
										monsterEnt.getKiller().getUniqueId(),
										"KillRabbit");
								break;
							case "WOLF":
								RunicPlayerBukkit.incrementPlayerKillCount(
										monsterEnt.getKiller().getUniqueId(),
										"KillWolf");
								break;
							case "ENDERMITE":
								RunicPlayerBukkit.incrementPlayerKillCount(
										monsterEnt.getKiller().getUniqueId(),
										"KillEndermite");
								break;
							case "GUARDIAN":
								RunicPlayerBukkit.incrementPlayerKillCount(
										monsterEnt.getKiller().getUniqueId(),
										"KillGuardian");
								break;
							case "ELDER_GUARDIAN":
								RunicPlayerBukkit.incrementPlayerKillCount(
										monsterEnt.getKiller().getUniqueId(),
										"KillElderGuardian");
								break;
							case "SNOWMAN":
								RunicPlayerBukkit.incrementPlayerKillCount(
										monsterEnt.getKiller().getUniqueId(),
										"KillSnowGolem");
								break;
							case "VILLAGER":
								RunicPlayerBukkit.incrementPlayerKillCount(
										monsterEnt.getKiller().getUniqueId(),
										"KillVillager");
								break;
							default:
								break;
							}

						}
					}); // delay

		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerDeath(PlayerDeathEvent event) {
		if (event.getEntity() instanceof Player) {

			// Drop a grave!
			RunicPlayerBukkit targetPlayer = new RunicPlayerBukkit(event
					.getEntity().getUniqueId());
			// Only drop a grave if world name contains Realm or Paradise (i.e.
			// not adventure maps or sky !!
			if (event.getEntity().getWorld().toString().contains("RunicRealm")
					|| event.getEntity().getWorld().toString()
							.contains("Paradise")) {
				if (targetPlayer.checkPlayerPermission("rp.graves")
						&& targetPlayer.getPlayerSouls() == 0) {
					// Player has the graves permission and no souls left... so
					// trigger a grave!
					RunicDeathChest.savePlayerDeath((Player) event.getEntity(),
							event.getEntity().getLocation());
					targetPlayer
							.sendMessageToPlayer(ChatColor.DARK_GRAY
									+ "[RunicReaper] "
									+ ChatColor.GRAY
									+ "A grave with your items and exp has been left where you died.");
				} else if (targetPlayer.getPlayerSouls() > 0
						&& targetPlayer.checkPlayerPermission("rp.graves")) {
					// Player has souls left, so decrement their souls and do
					// NOT
					// trigger a grave!
					targetPlayer
							.setPlayerSouls(targetPlayer.getPlayerSouls() - 1);
					targetPlayer.sendMessageToPlayer(ChatColor.DARK_GRAY
							+ "[RunicReaper] " + ChatColor.GRAY
							+ "Used a soul! You have " + ChatColor.AQUA
							+ targetPlayer.getPlayerSouls() + ChatColor.GRAY
							+ " remaining. ");
					targetPlayer.sendMessageToPlayer(ChatColor.GRAY + "Type "
							+ ChatColor.DARK_GRAY + "/warp graves "
							+ ChatColor.GRAY + "for help.");
				}
			}

			final PlayerDeathEvent innerEvent = event;

			Bukkit.getServer().getScheduler()
					.runTaskAsynchronously(instance, new Runnable() {
						public void run() {

							Player player = (Player) innerEvent.getEntity();
							String cause = "";
							String killerName = "";

							if (RunicGateway.getLastEntityDamager(player) != null) {
								Entity killer = RunicGateway
										.getLastEntityDamager(player);

								if (killer instanceof Player) {
									Player k = (Player) killer;
									killerName = k.getName();

									cause = "PLAYER_KILL";
								} else {
									// Not a player... so maybe a mob :)
									killerName = killer.getType().toString();
									EntityDamageEvent ede = player
											.getLastDamageCause();
									DamageCause dc = ede.getCause();
									cause = dc.toString();
								}
							} else {
								// death not caused by an entity; entity check
								// returned null
								EntityDamageEvent ede = player
										.getLastDamageCause();
								DamageCause dc = ede.getCause();
								cause = dc.toString();
								killerName = cause;
							}

							String uuid = player.getUniqueId().toString();
							String name = player.getName();
							String loc = innerEvent.getEntity().getLocation()
									.toString();

							// String pvp =
							// event.getEntity().getKiller().toString();
							long time = new Date().getTime();

							MySQL MySQL = new MySQL(instance, instance
									.getConfig().getString("dbHost"), instance
									.getConfig().getString("dbPort"), instance
									.getConfig().getString("dbDatabase"),
									instance.getConfig().getString("dbUser"),
									instance.getConfig()
											.getString("dbPassword"));
							final Connection e = MySQL.openConnection();
							// do the insert
							try {
								Statement eStmt = e.createStatement();
								eStmt.executeUpdate("INSERT INTO rp_PlayerDeath (`PlayerName`, `UUID`, `TimeStamp`, `CauseOfDeath`, `Killer`, `Location`) VALUES "
										+ "('"
										+ name
										+ "', '"
										+ uuid
										+ "', '"
										+ time
										+ "', '"
										+ cause
										+ "', '"
										+ killerName + "', '" + loc + "');");
							} catch (SQLException err) {
								getLogger().log(
										Level.SEVERE,
										"Cant create new row PlayerDeath for "
												+ name + " because: "
												+ err.getMessage());
							}
							// close the connection
							try {
								e.close();
							} catch (SQLException err) {
								getLogger().log(
										Level.SEVERE,
										"Cant close conn PlayerDeath for "
												+ name + " because: "
												+ err.getMessage());
							}
							player = null;
						}

					}); // end run task async
			targetPlayer = null;
		}
	}

	/*
	 * 
	 * // Maintain table of player info public void updatePlayerInfo(String
	 * name, boolean join, boolean leave) { final Date now = new Date();
	 * 
	 * MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
	 * "dbHost"), instance.getConfig().getString("dbPort"), instance
	 * .getConfig().getString("dbDatabase"), instance.getConfig()
	 * .getString("dbUser"), instance.getConfig().getString( "dbPassword"));
	 * final Connection d = MySQL.openConnection();
	 * 
	 * final String innerName = name; final boolean innerJoin = join; final
	 * boolean innerLeave = leave;
	 * 
	 * Bukkit.getServer().getScheduler() .runTaskAsynchronously(instance, new
	 * Runnable() { public void run() {
	 * 
	 * // run update when players join if (innerJoin) { try {
	 * 
	 * Statement dStmt = d.createStatement(); ResultSet res = dStmt
	 * .executeQuery("SELECT * FROM rp_PlayerInfo WHERE PlayerName = '" +
	 * innerName + "';"); // if this is true - there's no result from DB if
	 * (!res.isBeforeFirst()) { // Need to create a new record // Check for a
	 * date in grieflog to use as // FirstSeen date ResultSet grieflogRes =
	 * dStmt .executeQuery("SELECT * FROM `blacklist_events` WHERE `player` = '"
	 * + innerName + "' ORDER BY `id` ASC LIMIT 1;");
	 * 
	 * // if this is true - there's no result from // DB if
	 * (!grieflogRes.isBeforeFirst()) { // No entries found in grief log, use //
	 * current time as // FirstSeen // Create the new record try {
	 * dStmt.executeUpdate(
	 * "INSERT INTO rp_PlayerInfo (`PlayerName`, `UUID`, `FirstSeen`, `LastSeen`) VALUES ('"
	 * + innerName + "', '" + getServer().getPlayer( innerName) .getUniqueId() +
	 * "', '" + now.getTime() + "', '" + now.getTime() + "');");
	 * getLogger().log( Level.INFO,
	 * "[RP] Created new database entry (without grieflog data) for " +
	 * innerName); } catch (SQLException e) { getLogger().log( Level.SEVERE,
	 * "Cant create new row PlayerInfo for " + innerName + " because: " +
	 * e.getMessage()); } } else { // Entry found in grieflog, use that for //
	 * FirstSeen // Create the new record grieflogRes.next(); try { String
	 * logTime = grieflogRes .getString("time") + "000"; dStmt.executeUpdate(
	 * "INSERT INTO rp_PlayerInfo (`PlayerName`, `UUID`, `FirstSeen`, `LastSeen`) VALUES ('"
	 * + innerName + "', '" + getServer().getPlayer( innerName) .getUniqueId() +
	 * "', '" + logTime + "', '" + now.getTime() + "');"); getLogger().log(
	 * Level.INFO, "[RP] Created new database entry (with grieflog data) for " +
	 * innerName); } catch (SQLException e) { getLogger().log( Level.SEVERE,
	 * "Cant create new row PlayerInfo for " + innerName + " because: " +
	 * e.getMessage()); } } // now process if user does exist already in //
	 * playerinfo // table } else { // Record already exists, just update //
	 * LastSeen try {
	 * dStmt.executeUpdate("UPDATE `rp_PlayerInfo` SET LastSeen='" +
	 * now.getTime() + "' WHERE PlayerName='" + innerName + "';");
	 * getLogger().log( Level.INFO, "[RP] Updated player info record for " +
	 * innerName); } catch (SQLException e) { getLogger().log( Level.SEVERE,
	 * "Cant update LastSeen on exit for " + innerName + " because: " +
	 * e.getMessage()); }
	 * 
	 * } } catch (SQLException e) { getLogger().log( Level.SEVERE,
	 * "Could not check for PlayerInfo record because: " + e.getMessage()); }
	 * 
	 * } // end if running the "onJoin" part // run update when players leave
	 * else if (innerLeave) { try { Statement dStmt = d.createStatement();
	 * ResultSet res = dStmt
	 * .executeQuery("SELECT * FROM rp_PlayerInfo WHERE PlayerName = '" +
	 * innerName + "';");
	 * 
	 * // this if stmt ensures the resultset is empty // if this is true -
	 * there's no result from DB if (!res.isBeforeFirst()) { // no record
	 * exists... somehow. this // shouldnt really happen. // re-run this method
	 * but on the JOIN side // to get a new // record created.
	 * updatePlayerInfo(innerName, true, false); } else { // Record already
	 * exists, good! now just // update LastSeen try {
	 * dStmt.executeUpdate("UPDATE `rp_PlayerInfo` SET LastSeen='" +
	 * now.getTime() + "' WHERE PlayerName='" + innerName + "';");
	 * getLogger().log( Level.INFO, "[RP] Updated player info record for " +
	 * innerName); } catch (SQLException e) { getLogger().log( Level.SEVERE,
	 * "Cant update LastSeen on exit for " + innerName + " because: " +
	 * e.getMessage()); } }
	 * 
	 * } catch (SQLException e) { getLogger().log( Level.SEVERE,
	 * "Could not check for PlayerInfo ONLEAVE record because: " +
	 * e.getMessage()); } } // end if running the "onLeave" part
	 * 
	 * // Close the connection try { d.close(); } catch (SQLException e) {
	 * getLogger().log( Level.SEVERE,
	 * "Cant close mysql conn after playerinfo update: " + e.getMessage()); } }
	 * }); // delay }
	 */
	// Maintain table of player info
	public void updatePlayerInfoOnJoin(String name, UUID pUUID) {
		final Date now = new Date();
		final String playerName = name;
		final UUID playerUUID = pUUID;

		Bukkit.getServer().getScheduler()
				.runTaskAsynchronously(instance, new Runnable() {
					public void run() {

						MySQL MySQL = new MySQL(instance, instance.getConfig()
								.getString("dbHost"), instance.getConfig()
								.getString("dbPort"), instance.getConfig()
								.getString("dbDatabase"), instance.getConfig()
								.getString("dbUser"), instance.getConfig()
								.getString("dbPassword"));
						final Connection dbConn = MySQL.openConnection();
						int rowCount = -1;
						int rowCountnameMatch = -1;

						try {
							PreparedStatement dStmt = dbConn
									.prepareStatement("SELECT COUNT(*) as Total FROM rpgame.rp_PlayerInfo WHERE UUID = ?;");
							dStmt.setString(1, playerUUID.toString());
							ResultSet dbResult = dStmt.executeQuery();
							while (dbResult.next()) {
								rowCount = dbResult.getInt("Total");
							}
							dStmt.close();

							PreparedStatement zStmt = dbConn
									.prepareStatement("SELECT COUNT(*) as Total FROM rpgame.rp_PlayerInfo WHERE PlayerName = ?;");
							zStmt.setString(1, playerName);
							ResultSet zResult = zStmt.executeQuery();
							while (zResult.next()) {
								rowCountnameMatch = zResult.getInt("Total");
							}
							zStmt.close();

						} catch (SQLException e) {
							getLogger().log(
									Level.SEVERE,
									"Cant check for row count in updatePlayerInfoOnJoin for "
											+ playerName + " because: "
											+ e.getMessage());
						}

						if (rowCount != rowCountnameMatch) {
							Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
									"sc Name change detected for " + playerName);
							Bukkit.getLogger().log(
									Level.INFO,
									"[RP] Name change detected for "
											+ playerName);
						}

						try {

							// if this player has no rows in the table yet
							if (rowCount == 0) {
								PreparedStatement dStmt = dbConn
										.prepareStatement("INSERT INTO rp_PlayerInfo (`PlayerName`, `UUID`, `FirstSeen`, `LastSeen`) VALUES "
												+ "(?, ?, ?, ?);");
								dStmt.setString(1, playerName);
								dStmt.setString(2, playerUUID.toString());
								dStmt.setLong(3, now.getTime());
								dStmt.setLong(4, now.getTime());

								dStmt.executeUpdate();
								dStmt.close();

								// if this player has 1 row in the table
							} else if (rowCount == 1) {
								PreparedStatement dStmt = dbConn
										.prepareStatement("UPDATE `rp_PlayerInfo` SET LastSeen=?, PlayerName=? WHERE UUID=?;");
								dStmt.setLong(1, now.getTime());
								dStmt.setString(2, playerName);
								dStmt.setString(3, playerUUID.toString());
								dStmt.executeUpdate();
								dStmt.close();
								Bukkit.getLogger().log(
										Level.INFO,
										"[RP] PlayerInfo data updated for "
												+ playerName);

								// if this player has MORE than 1 row in the
								// table
							} else if (rowCount > 1) {
								int counter = 1;
								PreparedStatement zStmt = dbConn
										.prepareStatement("SELECT * FROM rpgame.rp_PlayerInfo WHERE UUID = ? ORDER BY ID ASC;");
								zStmt.setString(1, playerUUID.toString());
								ResultSet zResult = zStmt.executeQuery();
								while (zResult.next()) {
									// The first row is our valid one - update
									// it!
									if (counter == 1) {
										PreparedStatement dStmt = dbConn
												.prepareStatement("UPDATE `rp_PlayerInfo` SET LastSeen=?, PlayerName=? WHERE UUID=?;");
										dStmt.setLong(1, now.getTime());
										dStmt.setString(2, playerName);
										dStmt.setString(3,
												playerUUID.toString());
										dStmt.executeUpdate();
										dStmt.close();

										Bukkit.getLogger().log(
												Level.INFO,
												"[RP] PlayerInfo data [row "
														+ zResult.getInt("ID")
														+ "] updated for "
														+ playerName);
										// All further rows are invalid, delete
										// them!
									} else if (counter > 1) {
										PreparedStatement dStmt = dbConn
												.prepareStatement("DELETE FROM `rp_PlayerInfo` WHERE ID = ? LIMIT 1;");
										dStmt.setInt(1, zResult.getInt("ID"));
										dStmt.executeUpdate();
										dStmt.close();
										Bukkit.getLogger().log(
												Level.INFO,
												"[RP] PlayerInfo dupe row cleanup (name change?)! Deleted row "
														+ zResult.getInt("ID"));
									}

									counter++;
								}
								zStmt.close();

							}

							dbConn.close();

						} catch (SQLException e) {
							getLogger().log(
									Level.SEVERE,
									"Cant work with DB updatePlayerInfoOnJoin for "
											+ playerName + " because: "
											+ e.getMessage());
						}

					}
				}); // end run task async

	}

	// Maintain table of player info
	public void updatePlayerInfoOnQuit(String name, UUID pUUID) {
		final Date now = new Date();
		final String playerName = name;
		final UUID playerUUID = pUUID;

		Bukkit.getServer().getScheduler()
				.runTaskAsynchronously(instance, new Runnable() {
					public void run() {

						MySQL MySQL = new MySQL(instance, instance.getConfig()
								.getString("dbHost"), instance.getConfig()
								.getString("dbPort"), instance.getConfig()
								.getString("dbDatabase"), instance.getConfig()
								.getString("dbUser"), instance.getConfig()
								.getString("dbPassword"));
						final Connection dbConn = MySQL.openConnection();

						try {

							PreparedStatement dStmt = dbConn
									.prepareStatement("UPDATE `rp_PlayerInfo` SET LastSeen=? WHERE UUID=?;");
							dStmt.setLong(1, now.getTime());
							dStmt.setString(2, playerUUID.toString());
							dStmt.executeUpdate();
							dStmt.close();
							Bukkit.getLogger().log(
									Level.INFO,
									"[RP] PlayerInfo data updated for "
											+ playerName);
							dbConn.close();

						} catch (SQLException e) {
							getLogger().log(
									Level.SEVERE,
									"Cant work with DB updatePlayerInfoOnquit for "
											+ playerName + " because: "
											+ e.getMessage());
						}

					}
				}); // end run task async

	}

	private boolean setupPermissions() {
		RegisteredServiceProvider<Permission> rsp = getServer()
				.getServicesManager().getRegistration(Permission.class);
		perms = rsp.getProvider();
		return perms != null;
	}

	private boolean setupEconomy() {
		RegisteredServiceProvider<Economy> economyProvider = getServer()
				.getServicesManager().getRegistration(
						net.milkbowl.vault.economy.Economy.class);
		if (economyProvider != null) {
			economy = economyProvider.getProvider();
		}

		return (economy != null);
	}

}
