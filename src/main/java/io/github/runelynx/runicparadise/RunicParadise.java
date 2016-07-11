/*


 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.runelynx.runicparadise;

import io.github.runelynx.runicparadise.RunicMessaging.RunicFormat;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Guardian;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Skeleton.SkeletonType;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;

import com.connorlinfoot.titleapi.TitleAPI;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

/**
 *
 * @author runelynx
 */
public final class RunicParadise extends JavaPlugin implements Listener,
		PluginMessageListener {

	private static Plugin instance;
	private static final Logger log = Logger.getLogger("Minecraft");
	public static Permission perms = null;
	public static Economy economy = null;
	private static final long PUZZLE_REPEAT_TIME = 518400000;

	public static HashMap<UUID, Faith> faithMap = new HashMap<UUID, Faith>();
	public static HashMap<String, Integer> powerReqsMap = new HashMap<String, Integer>();
	public static HashMap<String, String[]> faithSettingsMap = new HashMap<String, String[]>();
	public static HashMap<String, ChatColor> rankColors = new HashMap<String, ChatColor>();
	public static HashMap<UUID, String> protectedPlayers = new HashMap<UUID, String>();
	public static HashMap<Location, String[]> runicEyes = new HashMap<Location, String[]>();
	public static HashMap<Entity, String> runicEyeEntities = new HashMap<Entity, String>();
	public static HashMap<Location, String[]> prayerBooks = new HashMap<Location, String[]>();
	public static HashMap<Entity, String> prayerBookEntities = new HashMap<Entity, String>();
	public static HashMap<String, String> newReadyPlayer = new HashMap<String, String>();
	public static HashMap<UUID, Integer> giftIDTracker = new HashMap<UUID, Integer>();
	
	public static Random randomSeed = new Random();

	Ranks ranks = new Ranks();
	private Entity monsterEnt;

	public static Plugin getInstance() {
		return instance;
	}

	@Override
	public void onPluginMessageReceived(String channel, Player player,
			byte[] message) {
		if (!channel.equals("BungeeCord")) {
			return;
		}
		ByteArrayDataInput in = ByteStreams.newDataInput(message);
		String subchannel = in.readUTF();

	}

	public void onEnable() {
		instance = this;

		getConfig().options().copyDefaults(true);
		saveConfig();

		// get the object from vault API for Permission class
		setupPermissions();
		setupEconomy();

		this.getServer().getMessenger()
				.registerOutgoingPluginChannel(this, "BungeeCord");

		getLogger().info("RunicParadise Plugin: onEnable has been invoked!");
		String tempInvSetting = "gamerule keepInventory "
				+ instance.getConfig().getString("keepInventoryOnDeathEnabled");
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), tempInvSetting);
		tempInvSetting = "mvrule keepInventory true RunicSky";
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), tempInvSetting);
		tempInvSetting = "mvrule keepInventory true RunicRealm";
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), tempInvSetting);
		tempInvSetting = "mvrule keepInventory true Mining";
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), tempInvSetting);
		tempInvSetting = "mvrule keepInventory true RunicRealm_nether";
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), tempInvSetting);
		tempInvSetting = "mvrule keepInventory true RunicRealm_the_end";
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), tempInvSetting);

		for (Player p : Bukkit.getOnlinePlayers()) {
		
			RunicMessaging.sendMessage(p, RunicFormat.SYSTEM, "RunicParadise plugin is "+ ChatColor.DARK_GREEN + "starting up" + ChatColor.GRAY + "...");
		}

		// Initialize a new EffectManager

		Recipes.customFoodRecipes();

		RunicDeathChest.syncGraveLocations();

		for (Player p : Bukkit.getOnlinePlayers()) {
			faithMap.put(p.getUniqueId(), new Faith(p.getUniqueId()));
			if (p.hasPermission("rp.faith.user")) {
				p.sendMessage(ChatColor.GRAY + "[" + ChatColor.BLUE + "Runic"
						+ ChatColor.DARK_AQUA + "Faith" + ChatColor.GRAY + "] "
						+ ChatColor.BLUE + "Faith system activated!");
			}

		}

		Faith.getFaithSettings();
		Faith.getPowerSettings();
		// loadRunicEyes();
		
		Borderlands.startScheduledTasks();


		rankColors.put("Ghost", ChatColor.GRAY);
		rankColors.put("Seeker", ChatColor.GREEN);
		rankColors.put("Runner", ChatColor.DARK_GREEN);
		rankColors.put("Singer", ChatColor.YELLOW);
		rankColors.put("Brawler", ChatColor.GOLD);
		rankColors.put("Keeper", ChatColor.AQUA);
		rankColors.put("Guard", ChatColor.DARK_AQUA);
		rankColors.put("Hunter", ChatColor.BLUE);
		rankColors.put("Slayer", ChatColor.LIGHT_PURPLE);
		rankColors.put("Warder", ChatColor.LIGHT_PURPLE);
		rankColors.put("Champion", ChatColor.DARK_PURPLE);
		rankColors.put("Master", ChatColor.RED);

		// This will throw a NullPointerException if you don't have the command
		// defined in your plugin.yml file!
		getCommand("el").setExecutor(new Commands());
		getCommand("freezemob").setExecutor(new Commands());
		getCommand("unfreezemob").setExecutor(new Commands());
		getCommand("runiceye").setExecutor(new Commands());
		getCommand("holotest").setExecutor(new Commands());
		getCommand("casino").setExecutor(new Commands());
		getCommand("rp").setExecutor(new Commands());
		getCommand("rptest").setExecutor(new Commands());
		getCommand("consoleseeker").setExecutor(new Commands());
		getCommand("sendentity").setExecutor(new Commands());
		getCommand("miningreset").setExecutor(new Commands());
		getCommand("miningworldreminder").setExecutor(new Commands());
		getCommand("rpreload").setExecutor(new Commands());
		getCommand("oldrankperks").setExecutor(new Commands());
		getCommand("carnivalaxe").setExecutor(new Commands());
		getCommand("headofplayer").setExecutor(new Commands());
		getCommand("iteminfo").setExecutor(new Commands());
		getCommand("face").setExecutor(new Commands());
		getCommand("crocomaze").setExecutor(new Commands());
		getCommand("miningworld").setExecutor(new Commands());
		getCommand("mw").setExecutor(new Commands());
		getCommand("wild").setExecutor(new Commands());
		getCommand("gift").setExecutor(new Commands());
		getCommand("rpgames").setExecutor(new Commands());
		getCommand("games").setExecutor(new Commands());
		getCommand("hmsay").setExecutor(new Commands());
		getCommand("adventureparkourprize").setExecutor(new Commands());
		getCommand("tc").setExecutor(new Commands());
		getCommand("promote").setExecutor(new Commands());
		getCommand("rankup").setExecutor(new Commands());
		getCommand("ranks").setExecutor(new Commands());
		getCommand("sc").setExecutor(new Commands());
		getCommand("staffchat").setExecutor(new Commands());
		getCommand("settler").setExecutor(new Commands());
		getCommand("seeker").setExecutor(new Commands());
		getCommand("junglemaze").setExecutor(new Commands());
		getCommand("staff").setExecutor(new Commands());
		getCommand("music").setExecutor(new Commands());
		getCommand("radio").setExecutor(new Commands());
		getCommand("ready").setExecutor(new Commands());
		getCommand("rpmail").setExecutor(new Commands());
		getCommand("rptokens").setExecutor(new Commands());
		getCommand("dailykarma").setExecutor(new Commands());
		getCommand("say").setExecutor(new Commands());
		getCommand("grave").setExecutor(new Commands());
		getCommand("graves").setExecutor(new Commands());
		getCommand("rptransfer").setExecutor(new Commands());
		getCommand("rpvote").setExecutor(new Commands());
		getCommand("rpjobs").setExecutor(new Commands());
		getCommand("rpeffects").setExecutor(new Commands());
		getCommand("punish").setExecutor(new Commands());
		getCommand("powers").setExecutor(new Commands());
		getCommand("faith").setExecutor(new Commands());
		getCommand("cactifever").setExecutor(new Commands());
		getCommand("voice").setExecutor(new Commands());
		getCommand("discord").setExecutor(new Commands());

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

		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();

		// Check for spirit of wolf spellcast every 3 minutes
		scheduler.runTaskTimer(this, new Runnable() {

			@Override
			public void run() {
				RunicParadise.loadRunicEyes();
				RunicParadise.loadPrayerBooks();

			}

		}, 0L, 3600L);

		/*
		 * // Check for spirit of wolf spellcast every 3 minutes
		 * scheduler.runTaskTimer(this, new Runnable() {
		 * 
		 * @Override public void run() { for (Entry<UUID, Powers> entry :
		 * powersMap.entrySet()) { UUID pUUID = entry.getKey(); Powers powerObj
		 * = entry.getValue();
		 * 
		 * Long currentTime = Bukkit.getWorld("RunicRealm").getTime();
		 * 
		 * if (currentTime > 14000 && currentTime < 23000) { // Check player's
		 * Beasts skill for casting Spirit of // Wolf
		 * Bukkit.getConsoleSender().sendMessage(
		 * "Current time in Spirit of Wolf check: " + currentTime); if
		 * (powerObj.getSkillBeasts() >= 300) {
		 * Powers.spellSpiritOfTheWolf(pUUID, Bukkit
		 * .getPlayer(pUUID).getLocation()); } }
		 * 
		 * }
		 * 
		 * } }, 0L, 3600L);
		 */

		scheduler.runTaskTimer(this, new Runnable() {

			@Override
			public void run() {

				for (Player p : Bukkit.getWorld("RunicRealm_nether")
						.getPlayers()) {
					if (faithMap.get(p.getUniqueId()).checkEquippedFaithLevel(
							"Nether",
							RunicParadise.powerReqsMap.get("Netherborn"))) {
						faithMap.get(p.getUniqueId()).castNether_Netherborn(p);
					}

				}

			}
		}, 0L, Faith.NETHER_NETHERBORN_TIMING);

		scheduler.runTaskTimerAsynchronously(this, new Runnable() {

			@Override
			public void run() {

				for (Player p : Bukkit.getWorld("RunicSky").getPlayers()) {
					if (p.isGliding()) {
						p.setGliding(false);
						p.setVelocity(new Vector(0, -2, 0));
					}
				}

			}
		}, 0L, 70L);
	}

	public void onDisable() {
		// TODO Insert logic to be performed when the plugin is disabled
		Faith.deactivateFaiths();
		rankColors.clear();
		// Dispose of the EffectManager
		for (Player p : Bukkit.getOnlinePlayers()) {
			
			RunicMessaging.sendMessage(p, RunicFormat.SYSTEM, "RunicParadise plugin is "+ ChatColor.DARK_RED + "shutting down" + ChatColor.GRAY + "...");
			
		}

		getLogger().info("RunicParadise Plugin: onDisable has been invoked!");

		// em.dispose();
	}

	@EventHandler
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		if (event.getLocation().getX() > 7500
				|| event.getLocation().getX() < -7500
				|| event.getLocation().getZ() > 7500
				|| event.getLocation().getZ() < -7500) {
			// Confirmed spawn is in the borderlands!!
			if (event.getEntityType() == EntityType.ZOMBIE) {

				// nextInt is normally exclusive of the top value,
				// so add 1 to make it inclusive
				int random = ThreadLocalRandom.current().nextInt(1, 100 + 1);
				int randomSupport = ThreadLocalRandom.current().nextInt(1,
						100 + 1);

				if (random <= 50) {
					Borderlands.zombieMarauder((Zombie) event.getEntity());

				} else if (random <= 90) {
					Borderlands.zombieFallenKnight((Zombie) event.getEntity());
					// 10% chance to spawn a support zombie
					if (randomSupport <= 10) {
						Entity z = event
								.getLocation()
								.getWorld()
								.spawnEntity(event.getLocation(),
										EntityType.ZOMBIE);
						Borderlands.zombieShaman((Zombie) z);
					}

				} else {
					Borderlands.zombieGoliath((Zombie) event.getEntity());
					// 20% chance to spawn a support zombie
					if (randomSupport <= 20) {
						Entity z = event
								.getLocation()
								.getWorld()
								.spawnEntity(event.getLocation(),
										EntityType.ZOMBIE);
						Borderlands.zombieShaman((Zombie) z);
					}
				}
			}
		}
	}

	@EventHandler
	public void onPlayerChat(AsyncPlayerChatEvent event) throws IOException {
		String sword = "";
		/*
		 * if (event.getPlayer().hasPermission("rp.guardian")) { sword =
		 * RunicParadise.rankColors.get(perms.getPrimaryGroup(event
		 * .getPlayer())) + "★"; }
		 */

		// Auto ban for chat spam

		/*
		 * if (event.getMessage().toLowerCase().replaceAll(" ", "")
		 * .contains("glade") &&
		 * event.getMessage().toLowerCase().replaceAll(" ", "") .contains("mc")
		 * && event.getMessage().toLowerCase().contains("io")) {
		 * 
		 * getServer().dispatchCommand( getServer().getConsoleSender(), "ban " +
		 * event.getPlayer().getName() + " AutoBan. Spamming advertisement.");
		 * 
		 * Bukkit.getLogger().log( Level.INFO, "Spam blocked! " +
		 * event.getPlayer().getName() + ": " + event.getMessage());
		 * event.setCancelled(true);
		 */

		if (!event.getPlayer().getWorld().getName().equals("plotworld")) {

			String staffPrefix = ChatColor.DARK_GRAY + "" + ChatColor.ITALIC
					+ "SV ";

			// //// HANDLE STAFF
			if (event.getPlayer().hasPermission("rp.staff")) {
				if ((event.getPlayer().hasPermission("rp.staff.admin"))) {
					staffPrefix += ChatColor.DARK_RED + "<Admin> ";
				} else if ((event.getPlayer().hasPermission("rp.staff.mod+"))) {
					staffPrefix += ChatColor.DARK_RED + "<Mod+> ";
				} else if ((event.getPlayer().hasPermission("rp.staff.mod"))) {
					staffPrefix += ChatColor.DARK_RED + "<Mod> ";
				} else if ((event.getPlayer()
						.hasPermission("rp.staff.director"))) {
					staffPrefix += ChatColor.DARK_RED + "<Director> ";
				} else if ((event.getPlayer()
						.hasPermission("rp.staff.architect"))) {
					staffPrefix += ChatColor.DARK_RED + "<Architect> ";
				} else if ((event.getPlayer()
						.hasPermission("rp.staff.recruiter"))) {
					staffPrefix += ChatColor.DARK_RED + "<Recruiter> ";
				} else if ((event.getPlayer()
						.hasPermission("rp.staff.enforcer"))) {
					staffPrefix += ChatColor.DARK_RED + "<Enforcer> ";
				} else if ((event.getPlayer().hasPermission("rp.staff.helper"))) {
					staffPrefix += ChatColor.DARK_RED + "<Helper> ";
				}

				// //// HANDLE LEGENDS
			} else if (event.getPlayer().hasPermission("rp.guardian")) {
				staffPrefix += ChatColor.BLUE + "<Legend> ";
			} else if (event.getPlayer().hasPermission("rp.pirate")) {
				staffPrefix += ChatColor.BLUE + "<Pirate> ";
			}

			// //// HANDLE FAITHS FOR ALL BUT GHOSTS
			String faithPrefix = "";

			if (!event.getPlayer().hasPermission("rp.ghost")) {
				String currentFaith = RunicParadise.faithMap.get(
						event.getPlayer().getUniqueId()).getPrimaryFaith();
				if (RunicParadise.faithMap.get(event.getPlayer().getUniqueId())
						.checkEquippedFaithLevel(
								currentFaith,
								Integer.parseInt(RunicParadise.faithSettingsMap
										.get(currentFaith)[4]))) {
					faithPrefix = RunicParadise.faithSettingsMap
							.get(currentFaith)[6];
				} else {
					faithPrefix = RunicParadise.faithSettingsMap
							.get(currentFaith)[1];
				}
			}

			// //// HANDLE MULTICOLORED SLAYER TITLE
			if (perms.getPrimaryGroup(event.getPlayer()).equals("Slayer")) {
				event.setFormat(staffPrefix
						+ ChatColor.BLUE
						+ faithPrefix
						+ RunicParadise.rankColors.get(perms
								.getPrimaryGroup(event.getPlayer()))
						+ perms.getPrimaryGroup(event.getPlayer())
								.toLowerCase() + " "
						// + ChatColor.GRAY + "{jobs}"
						+ ChatColor.BLUE + event.getPlayer().getDisplayName()
						+ ChatColor.WHITE + ": %2$s");
				// //// HANDLE FAITHS FOR ALL BUT GHOSTS SLAYERS
			} else if (!event.getPlayer().hasPermission("rp.GHOST")) {
				event.setFormat(staffPrefix
						+ RunicParadise.rankColors.get(perms
								.getPrimaryGroup(event.getPlayer()))
						+ faithPrefix
						+ perms.getPrimaryGroup(event.getPlayer())
								.toLowerCase()
						+ ChatColor.GRAY
						+ " "
						// + "{jobs}"

						+ RunicParadise.rankColors.get(perms
								.getPrimaryGroup(event.getPlayer()))
						+ event.getPlayer().getDisplayName() + ChatColor.WHITE
						+ ": %2$s");
			} else {
				// //// HANDLE GHOSTS
				event.setFormat(staffPrefix
						+ RunicParadise.rankColors.get(perms
								.getPrimaryGroup(event.getPlayer()))
						+ perms.getPrimaryGroup(event.getPlayer())
						+ ChatColor.GRAY
						+ " "
						// + "{jobs}"

						+ RunicParadise.rankColors.get(perms
								.getPrimaryGroup(event.getPlayer()))
						+ event.getPlayer().getDisplayName() + ChatColor.WHITE
						+ ": %2$s");
			}

		} else {
			// handles chat from Plotworld
			String staffPrefix = "";
			String rank = "";
			if (event.getPlayer().hasPermission("rp.staff")) {
				if ((event.getPlayer().hasPermission("rp.staff.admin"))) {
					staffPrefix = ChatColor.DARK_RED + "<Admin> ";
				} else if ((event.getPlayer().hasPermission("rp.staff"))) {
					staffPrefix = ChatColor.DARK_RED + "<Staff> ";
				}
			}

			if (event.getPlayer().hasPermission("rp.staff.admin")) {
				rank = ChatColor.LIGHT_PURPLE + "Judge";
			} else {
				rank = ChatColor.BLUE + "Contestant";
			}

			event.setFormat(staffPrefix
					+ rank
					// + ChatColor.GRAY
					+ " "
					// + "{jobs}"
					// + RunicParadise.rankColors.get(perms
					// .getPrimaryGroup(event.getPlayer()))

					+ event.getPlayer().getDisplayName() + ChatColor.WHITE
					+ ": %2$s");
		}

		// rf[5Adminf] {jobs} 5{name}f: %2$s
		// ADMINS
		/*
		 * if (event.getPlayer().hasPermission("rp.staff.admin")) {
		 * 
		 * event.setFormat(ChatColor.DARK_RED + "★Admin★" + " " +
		 * perms.getPrimaryGroup(event.getPlayer()) + ChatColor.GRAY + " {jobs}"
		 * + ChatColor.DARK_RED + event.getPlayer().getDisplayName() +
		 * ChatColor.WHITE + ": %2$s"); // ELDER MOD } else if
		 * (event.getPlayer().hasPermission("rp.staff.mod+")) {
		 * event.setFormat(ChatColor.DARK_RED + "✩Mod+✩" + " " +
		 * perms.getPrimaryGroup(event.getPlayer()) + ChatColor.GRAY + " {jobs}"
		 * + ChatColor.DARK_RED + event.getPlayer().getDisplayName() +
		 * ChatColor.WHITE + ": %2$s"); // MOD } else if
		 * (event.getPlayer().hasPermission("rp.staff.mod")) {
		 * event.setFormat(ChatColor.DARK_RED + "♒Mod♒" + " " +
		 * perms.getPrimaryGroup(event.getPlayer()) + ChatColor.GRAY + " {jobs}"
		 * + ChatColor.DARK_RED + event.getPlayer().getDisplayName() +
		 * ChatColor.WHITE + ": %2$s"); // HELPER } else if
		 * (event.getPlayer().hasPermission("rp.staff.helper")) {
		 * event.setFormat(ChatColor.DARK_RED + "" + "♦Helper♦" + " " +
		 * perms.getPrimaryGroup(event.getPlayer()) + ChatColor.GRAY + " {jobs}"
		 * + ChatColor.DARK_RED + event.getPlayer().getDisplayName() +
		 * ChatColor.WHITE + ": %2$s"); // EVERYONE ELSE } else {
		 * event.setFormat(event.getFormat().replace("{staff} ", "")
		 * .replace("{name}", event.getPlayer().getDisplayName())); }
		 */

		// CENSOR!
		if (event.getMessage().toLowerCase().contains("fuck")
				|| event.getMessage().toLowerCase().contains("shit")
				|| event.getMessage().toLowerCase().contains("nigga")
				|| event.getMessage().toLowerCase().contains("bitch")
				|| event.getMessage().toLowerCase().contains("faggot")
				|| event.getMessage().toLowerCase().contains("dick")
				|| event.getMessage().toLowerCase().contains("cunt")) {
			event.setMessage(event.getMessage().toLowerCase()
					.replace("fuck", "✗✗✗✗"));
			event.setMessage(event.getMessage().toLowerCase()
					.replace("shit", "✗✗✗✗"));
			event.setMessage(event.getMessage().toLowerCase()
					.replace("nigga", "✗✗✗✗"));
			event.setMessage(event.getMessage().toLowerCase()
					.replace("bitch", "✗✗✗✗"));
			event.setMessage(event.getMessage().toLowerCase()
					.replace("faggot", "✗✗✗✗"));
			event.setMessage(event.getMessage().toLowerCase()
					.replace("dick", "✗✗✗✗"));
			event.setMessage(event.getMessage().toLowerCase()
					.replace("cunt", "✗✗✗✗"));

			if (event.getPlayer().hasPermission("rp.chatfilterwarning2")) {

				getServer()
						.dispatchCommand(
								getServer().getConsoleSender(),
								"tempmute "
										+ event.getPlayer().getName()
										+ " 3m Auto mute for vulgar chat after warning.");

				for (Player p : Bukkit.getOnlinePlayers()) {
					p.sendMessage(ChatColor.WHITE
							+ "["
							+ ChatColor.YELLOW
							+ "Runic"
							+ ChatColor.GOLD
							+ "Justice"
							+ ChatColor.WHITE
							+ "] "
							+ ChatColor.GRAY
							+ "Another criminal behind bars. Keep chat clean please!");
				}

			} else if (event.getPlayer().hasPermission("rp.chatfilterwarning1")) {
				getServer()
						.dispatchCommand(
								getServer().getConsoleSender(),
								"tempmute "
										+ event.getPlayer().getName()
										+ " 1m Auto mute for vulgar chat after warning.");
				getServer().dispatchCommand(
						getServer().getConsoleSender(),
						"manuaddp " + event.getPlayer().getName()
								+ " rp.chatfilterwarning2");

				getServer().dispatchCommand(
						getServer().getConsoleSender(),
						"manuaddp " + event.getPlayer().getName()
								+ " -essentials.me");

				for (Player p : Bukkit.getOnlinePlayers()) {
					p.sendMessage(ChatColor.WHITE + "[" + ChatColor.YELLOW
							+ "Runic" + ChatColor.GOLD + "Justice"
							+ ChatColor.WHITE + "] " + ChatColor.GRAY
							+ "Silence is " + ChatColor.GOLD + "golden"
							+ ChatColor.GRAY + ". Keep chat clean please!");
				}

			} else {
				getServer().dispatchCommand(
						getServer().getConsoleSender(),
						"manuaddp " + event.getPlayer().getName()
								+ " rp.chatfilterwarning1");
				event.getPlayer()
						.sendMessage(
								ChatColor.BOLD
										+ ""
										+ ChatColor.DARK_RED
										+ "This is a kid-friendly server, do not use vulgar language here! Repeat offenders will be punished.");

			}

		}

	}

	/*
	 * @EventHandler public void onBlockRedstone(BlockRedstoneEvent event) {
	 * 
	 * if (event.getBlock().getType() == Material.REDSTONE_LAMP_ON &&
	 * RunicDeathChest.checkHashmapForDeathLoc(
	 * event.getBlock().getLocation()).equals("Locked")) {
	 * event.setNewCurrent(100); }
	 * 
	 * }
	 */

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {

		// handle gift inventory
		if (event.getSlot() == 4
				&& event.getInventory().getTitle().contains("Gift from")) {

			Gift.removeGift((Player) event.getWhoClicked(),
					RunicParadise.giftIDTracker.get(event.getWhoClicked()
							.getUniqueId()));

			((Player) event.getWhoClicked()).getWorld().dropItemNaturally(
					((Player) event.getWhoClicked()).getLocation(),
					event.getCurrentItem());
			event.getInventory().setItem(4, new ItemStack(Material.AIR, 1));

			((Player) event.getWhoClicked()).sendMessage(ChatColor.DARK_GREEN
					+ "" + ChatColor.ITALIC
					+ "The gift has been dropped at your location!");

		} else
		// Carnival menu - MAZES
		if (event.getInventory().getTitle().contains("Runic Carnival - Mazes")) {
			switch (event.getSlot()) {
			case 17:
				// teleport to puzzle Kiosk
				event.getWhoClicked().teleport(
						new Location(Bukkit.getWorld("RunicSky"), 328, 58, 543,
								(float) 72.99, (float) -26.40));
				break;
			default:
				break;

			}
			event.setCancelled(true);
		} else
		// Carnival menu - PARKOURS
		if (event.getInventory().getTitle()
				.contains("Runic Carnival - Parkours")) {
			switch (event.getSlot()) {
			case 17:
				// teleport to puzzle Kiosk
				event.getWhoClicked().teleport(
						new Location(Bukkit.getWorld("RunicSky"), 328, 58, 543,
								(float) 72.99, (float) -26.40));
				break;
			default:
				break;

			}
			event.setCancelled(true);
		} else
		// Carnival menu - ARENAS
		if (event.getInventory().getTitle().contains("Runic Carnival - Arenas")) {
			switch (event.getSlot()) {
			case 20:
				// teleport to paintball entry
				event.getWhoClicked().teleport(
						new Location(Bukkit.getWorld("RunicSky"), 377.563,
								56.00, 516.323, (float) -180.29913,
								(float) -1.4999065));
				break;
			case 21:
				// teleport to blockhunt entry
				event.getWhoClicked().teleport(
						new Location(Bukkit.getWorld("RunicSky"), 381.653,
								56.00, 516.313, (float) -181.64929,
								(float) -2.6999066));
				break;
			case 22:
				// teleport to ctf entry
				event.getWhoClicked().teleport(
						new Location(Bukkit.getWorld("RunicSky"), 385.650,
								56.00, 516.22, (float) -180.74915,
								(float) -1.3499054));
				break;
			case 23:
				// teleport to pvparena entry
				event.getWhoClicked().teleport(
						new Location(Bukkit.getWorld("RunicSky"), 385.650,
								56.00, 516.22, (float) -180.74915,
								(float) -1.3499054));
				break;

			case 24:
				// teleport to spleef entry
				event.getWhoClicked().teleport(
						new Location(Bukkit.getWorld("RunicSky"), 377.450,
								56.00, 528.886, (float) -359.8496,
								(float) -0.44990847));
				break;

			case 31:
				// teleport to mobarena entry
				event.getWhoClicked().teleport(
						new Location(Bukkit.getWorld("RunicSky"), 381.530,
								56.00, 528.890, (float) -359.09937,
								(float) 1.2000911));
				TitleAPI.sendFullTitle(
						(Player) event.getWhoClicked(),
						1,
						4,
						1,
						ChatColor.RED + "Empty Your Inventory",
						ChatColor.LIGHT_PURPLE
								+ "Before entering any arena or you will lose your stuff!");
				break;

			default:
				break;

			}
			event.setCancelled(true);
		} else
		// Carnival menu
		if (event.getInventory().getTitle().contains("Runic Carnival Menu")) {
			switch (event.getSlot()) {
			case 19:
				// teleport to info center
				event.getWhoClicked().teleport(
						new Location(Bukkit.getWorld("RunicSky"), 342, 58, 548,
								0, (float) 1));
				break;
			case 20:
				// teleport to prize cabin
				event.getWhoClicked().teleport(
						new Location(Bukkit.getWorld("RunicSky"), 320, 58, 522,
								(float) 92.50, (float) -16.05));
				break;
			case 21:
				// open maze menu
				RunicParadise.showRunicCarnivalMenu_Puzzles(
						((Player) event.getWhoClicked()), 'M');
				break;
			case 22:
				// open parkour menu
				RunicParadise.showRunicCarnivalMenu_Puzzles(
						((Player) event.getWhoClicked()), 'P');
				break;
			case 23:
				// adventure islands
				event.getWhoClicked().sendMessage(
						"Croc's adventure islands are coming soon!");
				break;
			case 24:
				// open battle tower menu
				RunicParadise.showRunicCarnivalMenu_BattleTower(((Player) event
						.getWhoClicked()));
				break;
			case 25:
				// teleport to creation zone
				event.getWhoClicked().teleport(
						new Location(Bukkit.getWorld("RunicSky"), 357, 58, 538,
								(float) -42.150, (float) -27.85));
				break;
			default:
				break;

			}
			event.setCancelled(true);
		} else

		// handle Slimefun inventory; initial 1.9 allows players to take the
		// items, so stop them!
		if (event.getInventory().getTitle().contains("Slimefun")
				&& event.getInventory().getTitle().contains("Guide")) {

			event.setCancelled(true);
		} else

		// handle faith inventory
		if (event.getInventory().getTitle().contains("Runic")
				&& event.getInventory().getTitle().contains("Faith")) {

			event.setCancelled(true);

			String faith = "";

			switch (event.getSlot()) {
			case 3:
				if (event.getWhoClicked().hasPermission("rp.faith.sun")) {
					faith = "Sun";
				}
				break;
			case 4:
				if (event.getWhoClicked().hasPermission("rp.faith.moon")) {
					faith = "Moon";
				}
				break;
			case 5:
				if (event.getWhoClicked().hasPermission("rp.faith.fire")) {
					faith = "Fire";
				}
				break;

			case 12:
				if (event.getWhoClicked().hasPermission("rp.faith.water")) {
					faith = "Water";
				}
				break;
			case 13:
				if (event.getWhoClicked().hasPermission("rp.faith.air")) {
					faith = "Air";
				}
				break;
			case 14:
				if (event.getWhoClicked().hasPermission("rp.faith.earth")) {
					faith = "Earth";
				}
				break;

			case 21:
				if (event.getWhoClicked().hasPermission("rp.faith.aether")) {
					faith = "Aether";
				}
				break;
			case 22:
				if (event.getWhoClicked().hasPermission("rp.faith.nether")) {
					faith = "Nether";
				}
				break;
			case 23:
				if (event.getWhoClicked().hasPermission("rp.faith.nature")) {
					faith = "Nature";
				}
				break;

			case 30:
				if (event.getWhoClicked().hasPermission("rp.faith.tech")) {
					faith = "Tech";
				}
				break;
			case 31:
				if (event.getWhoClicked().hasPermission("rp.faith.fate")) {
					faith = "Fate";
				}
				break;
			case 32:
				if (event.getWhoClicked().hasPermission("rp.faith.time")) {
					faith = "Time";
				}
				break;

			case 40:
				if (event.getWhoClicked().hasPermission("rp.faith.stars")) {
					faith = "Stars";
				}
				break;
			default:
				break;

			}

			if (!faith.equals("")) {
				getServer().dispatchCommand(
						getServer().getConsoleSender(),
						"faith enable " + event.getWhoClicked().getName() + " "
								+ faith);
			}

		}

	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void onBreakBlock(final BlockBreakEvent event) {

		if ((event.getBlock().getType() == Material.REDSTONE_LAMP_ON || event
				.getBlock().getType() == Material.REDSTONE_LAMP_OFF)
				&& !RunicDeathChest.checkHashmapForDeathLoc(
						event.getBlock().getLocation()).equals("NoGrave")) {

			event.setCancelled(true);
			event.getPlayer()
					.sendMessage(
							ChatColor.DARK_GRAY
									+ "[RunicReaper] "
									+ ChatColor.GRAY
									+ "Knocking over graves is bad luck! Right click the bottom!");
			getServer().dispatchCommand(getServer().getConsoleSender(),
					"effect " + event.getPlayer().getName() + " 9 10 10");
		} else if ((event.getBlock().getType() == Material.SIGN || event
				.getBlock().getType() == Material.SIGN_POST)
				&& !RunicDeathChest.checkHashmapForDeathLoc(
						event.getBlock().getLocation().subtract(0, 1, 0))
						.equals("NoGrave")) {
			event.setCancelled(true);
			event.getPlayer()
					.sendMessage(
							ChatColor.DARK_GRAY
									+ "[RunicReaper] "
									+ ChatColor.GRAY
									+ "Knocking over graves is bad luck! Right click the bottom!");
			getServer().dispatchCommand(getServer().getConsoleSender(),
					"effect " + event.getPlayer().getName() + " 15 3 5");
		} else if (event.getBlock().getType() == Material.MOB_SPAWNER
				&& !event.getPlayer().hasPermission("rp.staff")) {
			event.setCancelled(true);
			event.getPlayer().sendMessage(
					ChatColor.DARK_RED
							+ "Hey put that back! Only staff can break that.");
			// ATTEMPT CASTING BEAST-POWER / SPIRIT OF BEAVER
		}
		/*
		 * Bukkit.getServer().getScheduler() .runTaskAsynchronously(instance,
		 * new Runnable() { public void run() {
		 * 
		 * if ((event.getBlock().getType() == Material.DIRT || event
		 * .getBlock().getType() == Material.STONE) &&
		 * RunicParadise.powersMap.get( event.getPlayer().getUniqueId())
		 * .getSkillBeasts() >= 400) { // Player broke dirt/stone AND has
		 * sufficient skill, // so attempt cast
		 * Powers.spellSpiritOfTheMole(event.getPlayer() .getUniqueId(),
		 * event.getPlayer() .getLocation());
		 * 
		 * }
		 * 
		 * } // end run() }); // delay // end task method
		 */
	}

	@EventHandler
	public void onEntityTargetLivingEntityEvent(
			EntityTargetLivingEntityEvent event) {
		if (event.getEntity() instanceof Player
				&& protectedPlayers
						.containsKey(event.getTarget().getUniqueId())) {
			event.setCancelled(true);
		}
	}

	/*
	 * @EventHandler public void onPlayerPickupItem(PlayerPickupItemEvent event)
	 * { if (runicEyes.containsKey(event.getItem().getUniqueId())) {
	 * event.getPlayer().sendMessage( ChatColor.GREEN +
	 * "Hey don't poke Rune in the eye!!"); event.setCancelled(true); } }
	 */

	/*
	 * @SuppressWarnings("deprecation")
	 * 
	 * @EventHandler public void onEntityShootBowEvent(EntityShootBowEvent
	 * event) { if (event.getEntity() instanceof Player &&
	 * event.getEntity().getName().equals("runelynx")) {
	 * 
	 * Projectile proj = (Projectile) event.getProjectile();
	 * 
	 * ((Player) event.getEntity()).launchProjectile(Fireball.class,
	 * proj.getVelocity()); ((Player)
	 * event.getEntity()).launchProjectile(Snowball.class, proj.getVelocity());
	 * ((Player) event.getEntity()).launchProjectile(Arrow.class,
	 * proj.getVelocity()); ((Player)
	 * event.getEntity()).launchProjectile(Arrow.class, proj.getVelocity());
	 * ((Player) event.getEntity()).launchProjectile(Arrow.class,
	 * proj.getVelocity()); ((Player)
	 * event.getEntity()).launchProjectile(Arrow.class, proj.getVelocity());
	 * ((Player) event.getEntity()).launchProjectile(Fireball.class,
	 * proj.getVelocity()); ((Player)
	 * event.getEntity()).launchProjectile(Fireball.class, proj.getVelocity());
	 * }
	 * 
	 * }
	 */

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerTeleport(PlayerTeleportEvent event) {

		Faith.tryCast_PlayerTeleported(event);

		if (event.getFrom().getWorld().getName().equals("RunicSky")) {
			if ((event.getFrom().getX() <= -142 && event.getFrom().getX() >= -192)
					&& (event.getFrom().getY() <= 200 && event.getFrom().getY() >= 0)
					&& (event.getFrom().getZ() <= 513 && event.getFrom().getZ() >= 463)) {
				// A player is leaving the maze!
				event.getPlayer().sendMessage(
						ChatColor.DARK_RED + "DungeonMaster CrocodileHax"
								+ ChatColor.GRAY + ": See you next time!");
				event.getPlayer().setGameMode(GameMode.SURVIVAL);

			}
		}
		if (event.getTo().getWorld().getName().equals("RunicSky")) {
			if ((event.getTo().getX() <= -142 && event.getTo().getX() >= -192)
					&& (event.getTo().getY() <= 121 && event.getTo().getY() >= 107)
					&& (event.getTo().getZ() <= 513 && event.getTo().getZ() >= 463)) {
				// A player is in the maze!
				event.getPlayer()
						.sendMessage(
								ChatColor.DARK_RED
										+ "DungeonMaster CrocodileHax"
										+ ChatColor.GRAY
										+ ": Teleporting into my maze is cheating. So your teleport has been cancelled. :)");
				event.setCancelled(true);

			} else if ((event.getTo().getX() <= -137.5 && event.getTo().getX() >= -140.5)
					&& (event.getTo().getY() <= 120.5 && event.getTo().getY() >= 114)
					&& (event.getTo().getZ() <= 513.59 && event.getTo().getZ() >= 506.5)) {
				// A player is in the maze!
				event.getPlayer()
						.sendMessage(
								ChatColor.DARK_RED
										+ "DungeonMaster CrocodileHax"
										+ ChatColor.GRAY
										+ ": Teleporting into my maze is cheating. So your teleport has been cancelled. :)");
				event.setCancelled(true);

			}
		}

	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {

			if (event.getClickedBlock().getType().equals(Material.BEDROCK)) {

				String graveOwnerName = RunicDeathChest.checkLocForDeath(event
						.getClickedBlock().getLocation());

				if (graveOwnerName.equals(event.getPlayer().getName())
						|| graveOwnerName.equals("Unlocked")) {
					// Player at grave is owner... or grave is unlocked!
					RunicDeathChest.restoreFromPlayerDeath_v19(
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
			} else if (event.getClickedBlock().getType()
					.equals(Material.getMaterial(98))
					&& event.getPlayer().hasPermission("rp.admin")
					&& event.getPlayer().getItemInHand().getType() == Material.BLAZE_ROD) {
				placeRunicEye(event.getClickedBlock().getLocation(),
						event.getPlayer());

			} else if (event.getClickedBlock().getType()
					.equals(Material.getMaterial(98))
					&& runicEyes.containsKey(event.getClickedBlock()
							.getLocation())) {
				// player has right clicked a stone block and its a runic eye
				// location!
				event.getPlayer()
						.sendMessage(
								ChatColor.GOLD
										+ ""
										+ ChatColor.ITALIC
										+ "You hear the "
										+ runicEyes.get(event.getClickedBlock()
												.getLocation())[0]
										+ " in your mind...");
				event.getPlayer().sendMessage(
						ChatColor.GRAY
								+ runicEyes.get(event.getClickedBlock()
										.getLocation())[2]);
			} else if (event.getClickedBlock().getType()
					.equals(Material.getMaterial(98))
					&& event.getPlayer().hasPermission("rp.admin")
					&& event.getPlayer().getItemInHand().getType() == Material.BOOK) {
				placePrayerBook(event.getClickedBlock().getLocation(),
						event.getPlayer());

			} else if (event.getClickedBlock().getType()
					.equals(Material.getMaterial(98))
					&& prayerBooks.containsKey(event.getClickedBlock()
							.getLocation())) {
				// player has right clicked a stone block and its a prayer book
				// location!
				Faith.pray(event.getClickedBlock().getLocation(),
						event.getPlayer());
			} else {
				// not a redstone lamp

				if (faithMap.get(event.getPlayer().getUniqueId())
						.checkEquippedFaithLevel(
								"Earth",
								RunicParadise.powerReqsMap
										.get("Earth's Bounty"))) {
					faithMap.get(event.getPlayer().getUniqueId())
							.castEarth_EarthsBounty(event);
				}

				if (faithMap.get(event.getPlayer().getUniqueId())
						.checkEquippedFaithLevel("Water",
								RunicParadise.powerReqsMap.get("Deep Wader"))) {
					faithMap.get(event.getPlayer().getUniqueId())
							.castWater_DeepWader(event);
				}
			}
		} else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
			if (event.getClickedBlock().getType()
					.equals(Material.getMaterial(98))
					&& prayerBooks.containsKey(event.getClickedBlock()
							.getLocation())) {
				// player has left clicked a stone block and its a prayer book
				// location!
				event.getPlayer().sendMessage(
						ChatColor.GOLD
								+ ""
								+ ChatColor.ITALIC
								+ prayerBooks.get(event.getClickedBlock()
										.getLocation())[2]);
				event.getPlayer().sendMessage(
						ChatColor.GRAY
								+ ""
								+ ChatColor.ITALIC
								+ "This prayer requires that you sacrifice: "
								+ ChatColor.RESET
								+ ChatColor.DARK_AQUA
								+ prayerBooks.get(event.getClickedBlock()
										.getLocation())[3]);
			}
		}

		// event.getPlayer().sendMessage(runicEyes.toString());

	}

	@EventHandler
	public void onPlayerItemConsume(final PlayerItemConsumeEvent pice) {

		if (pice.getItem().getType() == Material.CHORUS_FRUIT
				&& pice.getPlayer().getWorld().getName()
						.equalsIgnoreCase("RunicSky")) {
			pice.setCancelled(true);
			pice.getPlayer().sendMessage(
					ChatColor.GRAY + "" + ChatColor.ITALIC
							+ "You can't eat that in this world.");
		}

		if (pice.getItem() != null && pice.getItem().hasItemMeta()) {
			if (pice.getItem().getItemMeta().hasLore()) {

				if (pice.getItem().getItemMeta().getLore().toString()
						.contains("Vanilla ice cream between")) {
					pice.getPlayer().addPotionEffect(
							new PotionEffect(PotionEffectType.SPEED, 3600, 1));
					pice.getPlayer().addPotionEffect(
							new PotionEffect(PotionEffectType.JUMP, 3600, 1));
				}
			}
		}

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

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onPlayerJoin(final PlayerJoinEvent pje) {

		if (pje.getPlayer().hasPermission("rp.chatfilterwarning1")
				&& !pje.getPlayer().hasPermission("rp.admin")) {
			getServer().dispatchCommand(
					getServer().getConsoleSender(),
					"manudelp " + pje.getPlayer().getName()
							+ " rp.chatfilterwarning1");
			getServer()
					.dispatchCommand(
							getServer().getConsoleSender(),
							"manudelp " + pje.getPlayer().getName()
									+ " -essentials.me");
		}

		if (pje.getPlayer().hasPermission("rp.chatfilterwarning2")
				&& !pje.getPlayer().hasPermission("rp.admin")) {
			getServer().dispatchCommand(
					getServer().getConsoleSender(),
					"manudelp " + pje.getPlayer().getName()
							+ " rp.chatfilterwarning2");
		}

		// Launch Firework on player join

		updatePlayerInfoOnJoin(pje.getPlayer().getName(), pje.getPlayer()
				.getUniqueId());

		ranks.convertRanks(pje.getPlayer());

		faithMap.put(pje.getPlayer().getUniqueId(), new Faith(pje.getPlayer()
				.getUniqueId()));

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

		Bukkit.getServer().getScheduler()
				.scheduleAsyncDelayedTask(instance, new Runnable() {
					public void run() {
						if (!pje.getPlayer().hasPermission(
								"rp.slimefun.smallbackpack")
								&& !pje.getPlayer().getWorld().getName()
										.equalsIgnoreCase("plotworld")) {

							RunicPlayerBukkit target = new RunicPlayerBukkit(
									pje.getPlayer().getUniqueId());

							if (target.getPlayerVoteCount() > 125) {
								perms.playerAdd(pje.getPlayer(),
										"rp.slimefun.smallbackpack");

								for (Player p : Bukkit.getOnlinePlayers()) {
									p.sendMessage(ChatColor.DARK_PURPLE
											+ ""
											+ pje.getPlayer().getDisplayName()
											+ " has voted 125 times! They can now make small backpacks! Check your votes with /rp and vote today!!");
								}

							}

						}
					}
				}, 120);

	}

	@EventHandler
	public void onPlayerQuit(final PlayerQuitEvent pje) {

		faithMap.remove(pje.getPlayer().getUniqueId());
		log.log(Level.INFO, "RP Faith: Removed " + pje.getPlayer().getName()
				+ " from faith map.");

		updatePlayerInfoOnQuit(pje.getPlayer().getName(), pje.getPlayer()
				.getUniqueId());

	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerDamage(final EntityDamageEvent ede) {

		boolean daytime;
		if ((Bukkit.getWorld("RunicRealm").getTime() > 14000 && Bukkit
				.getWorld("RunicRealm").getTime() < 23000)) {
			daytime = false;
		} else {
			daytime = true;
		}

		// handle deaths in the dungeon maze
		if (ede.getEntity() instanceof Player) {

			Location deathLoc = ede.getEntity().getLocation();

			if (((Player) ede.getEntity()).getHealth() - ede.getDamage() < 1) {
				if (deathLoc.getWorld().getName().equals("RunicSky")) {
					if ((deathLoc.getX() <= -142 && deathLoc.getX() >= -192)
							&& (deathLoc.getY() <= 200 && deathLoc.getY() >= -64)
							&& (deathLoc.getZ() <= 513 && deathLoc.getZ() >= 463)) {
						ede.setCancelled(true);
						ede.getEntity()
								.sendMessage(
										ChatColor.DARK_RED
												+ "DungeonMaster CrocodileHax"
												+ ChatColor.GRAY
												+ ": Looks like the score is Traps 1, You 0. Better luck next time.");
						ede.getEntity().teleport(
								new Location(Bukkit.getWorld("RunicSky"), -131,
										116, 509));
						((Player) ede.getEntity())
								.setGameMode(GameMode.SURVIVAL);
					}
				}
			}
		}

		// If player falls into the void, heal and teleport them to spawn
		if (ede.getCause() == DamageCause.VOID) {
			if (ede.getEntity() instanceof Player) {
				Player player = (Player) ede.getEntity();
				player.setHealth(20);
				// player.teleport(player.getWorld().getSpawnLocation());
				String cmd = "sudo " + player.getName() + " warp spawn";
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);

				player.sendMessage(ChatColor.AQUA + "" + ChatColor.ITALIC
						+ "Found you lost in the void... watch your step!");
			}
			/*
			 * } else if ( { //check if player has a sword of jupiter zombie in
			 * the hashmap log.log(Level.INFO,
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

		} else if (ede.getCause() == DamageCause.FALL
				&& ede.getEntity() instanceof Player) {

			Faith.tryCast_PlayerTookFallDamage((Player) ede.getEntity());

		} else if (ede.getEntity() instanceof Player
				&& faithMap.containsKey(ede.getEntity().getUniqueId())) {

			Faith.tryCast_PlayerTookEntityDamage(ede,
					((Player) ede.getEntity()));

		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent edbe) {

		if (edbe.getDamager() instanceof Player) {
			if (((Player) edbe.getDamager()).getItemInHand().getType()
					.equals(Material.NETHER_STAR)) {
				edbe.setCancelled(true);
				((Player) edbe.getDamager())
						.sendMessage("That cannot be used as a weapon anymore.");
				return;
			}
		}
		// If this ia player attacking a Monster and player has faiths
		if (edbe.getDamager() instanceof Player
				&& edbe.getEntity() instanceof Monster
				&& faithMap.containsKey(edbe.getDamager().getUniqueId())) {

			Faith.tryCast_PlayerHitMonster(edbe, edbe.getEntity(),
					(Player) edbe.getDamager());

		}

		/*
		 * if (edbe.getDamager() instanceof Player &&
		 * edbe.getDamager().getName().equals("runelynx")) { if
		 * (Bukkit.getPlayer(edbe.getDamager().getName()).getItemInHand()
		 * .getType().equals(Material.DIAMOND_SWORD)) {
		 * 
		 * RunicPlayerBukkit targetPlayer = new RunicPlayerBukkit(edbe
		 * .getDamager().getUniqueId());
		 * targetPlayer.sendMessageToPlayer(ChatColor.GOLD + "" +
		 * ChatColor.ITALIC + "Your sword's strike sings!");
		 * 
		 * EffectManager em = new EffectManager(instance);
		 * 
		 * ExplodeEffect explosionEffect = new ExplodeEffect(em);
		 * 
		 * // Blood-particles lays around for 30 ticks (1.5 seconds) // Bleeding
		 * takes 15 seconds // period * iterations = time of effect
		 * explosionEffect.setLocation(edbe.getEntity().getLocation());
		 * explosionEffect.start();
		 * 
		 * em.disposeOnTermination(); } }
		 */

	}

	@EventHandler
	public void onEntityDeath(final EntityDeathEvent ede) {

		if (ede.getEntity().getCustomName() != null
				&& ede.getEntity().getCustomName()
						.contains("Fallen Knight Zombie")) {
			ede.setDroppedExp(ede.getDroppedExp()
					* Borderlands.ZOMBIE_KNIGHT_EXP_MULT);
		} else if (ede.getEntity().getCustomName() != null
				&& ede.getEntity().getCustomName().contains("Marauder Zombie")) {
			ede.setDroppedExp(ede.getDroppedExp()
					* Borderlands.ZOMBIE_MARAUDER_EXP_MULT);
		} else if (ede.getEntity().getCustomName() != null
				&& ede.getEntity().getCustomName().contains("Goliath Zombie")) {
			ede.setDroppedExp(ede.getDroppedExp()
					* Borderlands.ZOMBIE_GOLIATH_EXP_MULT);
		} else if (ede.getEntity().getCustomName() != null
				&& ede.getEntity().getCustomName().contains("Zombie Shaman")) {
			ede.setDroppedExp(ede.getDroppedExp()
					* Borderlands.ZOMBIE_SHAMAN_EXP_MULT);
		}

		// check for Monsters... Flying=Ghast... Slime=Slime/Magmacube...
		// WaterMob = Squid
		if (ede.getEntity() instanceof LivingEntity) {

			final LivingEntity monsterEnt = (LivingEntity) ede.getEntity();

			// if a monster has died and killer was player
			if (monsterEnt.getLastDamageCause() instanceof EntityDamageByEntityEvent
					&& ((EntityDamageByEntityEvent) monsterEnt
							.getLastDamageCause()).getDamager() instanceof Player) {

				EntityDamageByEntityEvent nEvent = (EntityDamageByEntityEvent) monsterEnt
						.getLastDamageCause();

				Faith.tryCast_PlayerKilledMonster(ede,
						(Player) nEvent.getDamager());

			}

			EntityDamageEvent e = monsterEnt.getLastDamageCause();
			if (e instanceof EntityDamageByEntityEvent) {
				EntityDamageByEntityEvent ee = (EntityDamageByEntityEvent) e;
			}

			if (monsterEnt.getKiller() == null
					|| !(monsterEnt.getKiller() instanceof Player)
					|| ede.getEntity().getWorld().equals("plotworld")) {
				// [RP] Entity death detected but player=null or world=plotworld
				// OR killer not a player
				// so nothing recorded!

			} else {

				Bukkit.getServer().getScheduler()
						.runTaskAsynchronously(instance, new Runnable() {
							public void run() {

								String mobType = "";
								Boolean attemptPowersSkillUp = false;
								Boolean heldWeaponHasLore = false;
								int faithStrength = 0;

								if (monsterEnt.getKiller().getItemInHand() != null
										&& monsterEnt.getKiller()
												.getItemInHand().hasItemMeta()) {
									if (monsterEnt.getKiller().getItemInHand()
											.getItemMeta().hasLore()) {

										if (monsterEnt
												.getKiller()
												.getItemInHand()
												.getItemMeta()
												.getLore()
												.toString()
												.contains(
														"A blessed blade with a faint glow")) {
											heldWeaponHasLore = true;
											faithStrength = 1;
										} else if (monsterEnt
												.getKiller()
												.getItemInHand()
												.getItemMeta()
												.getLore()
												.toString()
												.contains(
														"A blessed blade with a pulsing glow")) {
											heldWeaponHasLore = true;
											faithStrength = 2;
										} else if (monsterEnt
												.getKiller()
												.getItemInHand()
												.getItemMeta()
												.getLore()
												.toString()
												.contains(
														"A blessed blade with a blinding glow")) {
											heldWeaponHasLore = true;
											faithStrength = 3;
										} else if (monsterEnt
												.getKiller()
												.getItemInHand()
												.getItemMeta()
												.getLore()
												.toString()
												.contains(
														"A corrupted axe with a crimson glow")) {
											heldWeaponHasLore = true;
											faithStrength = 4;
										} else {
											heldWeaponHasLore = false;

										}
									} else {
										heldWeaponHasLore = false;
									}
								}

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
								case "SHULKER":
									RunicPlayerBukkit.incrementPlayerKillCount(
											monsterEnt.getKiller()
													.getUniqueId(),
											"KillShulker");
									attemptPowersSkillUp = true;
									break;
								case "ZOMBIE":

									if (((org.bukkit.entity.Zombie) monsterEnt)
											.getVillagerProfession() == Profession.HUSK) {
										RunicPlayerBukkit
												.incrementPlayerKillCount(
														monsterEnt.getKiller()
																.getUniqueId(),
														"KillHusk");
										attemptPowersSkillUp = true;
									} else {
										RunicPlayerBukkit
												.incrementPlayerKillCount(
														monsterEnt.getKiller()
																.getUniqueId(),
														"KillZombie");
										attemptPowersSkillUp = true;
									}

									break;
								case "IRON_GOLEM":
									RunicPlayerBukkit.incrementPlayerKillCount(
											monsterEnt.getKiller()
													.getUniqueId(),
											"KillIronGolem");
									attemptPowersSkillUp = true;
									break;
								case "WITHER":
									RunicPlayerBukkit.incrementPlayerKillCount(
											monsterEnt.getKiller()
													.getUniqueId(),
											"KillWither");
									attemptPowersSkillUp = true;
									break;
								case "SKELETON":

									if (((org.bukkit.entity.Skeleton) monsterEnt)
											.getSkeletonType() == SkeletonType.STRAY) {
										RunicPlayerBukkit
												.incrementPlayerKillCount(
														monsterEnt.getKiller()
																.getUniqueId(),
														"KillStray");
										attemptPowersSkillUp = true;
									} else if (((org.bukkit.entity.Skeleton) monsterEnt)
											.getSkeletonType() == SkeletonType.WITHER) {
										RunicPlayerBukkit
												.incrementPlayerKillCount(
														monsterEnt.getKiller()
																.getUniqueId(),
														"KillWSkeleton");
										attemptPowersSkillUp = true;
									} else {
										RunicPlayerBukkit
												.incrementPlayerKillCount(
														monsterEnt.getKiller()
																.getUniqueId(),
														"KillSkeleton");
										attemptPowersSkillUp = true;
									}

									break;
								case "SLIME":
									RunicPlayerBukkit
											.incrementPlayerKillCount(
													monsterEnt.getKiller()
															.getUniqueId(),
													"KillSlime");
									attemptPowersSkillUp = true;
									break;
								case "MAGMA_CUBE":
									RunicPlayerBukkit.incrementPlayerKillCount(
											monsterEnt.getKiller()
													.getUniqueId(),
											"KillMagmaCube");
									attemptPowersSkillUp = true;
									break;
								case "WITCH":
									RunicPlayerBukkit
											.incrementPlayerKillCount(
													monsterEnt.getKiller()
															.getUniqueId(),
													"KillWitch");
									attemptPowersSkillUp = true;
									break;
								case "SILVERFISH":
									RunicPlayerBukkit.incrementPlayerKillCount(
											monsterEnt.getKiller()
													.getUniqueId(),
											"KillSilverfish");
									attemptPowersSkillUp = true;
									break;
								case "GIANT":
									RunicPlayerBukkit
											.incrementPlayerKillCount(
													monsterEnt.getKiller()
															.getUniqueId(),
													"KillGiant");
									break;
								case "BLAZE":
									RunicPlayerBukkit
											.incrementPlayerKillCount(
													monsterEnt.getKiller()
															.getUniqueId(),
													"KillBlaze");
									attemptPowersSkillUp = true;
									break;
								case "CREEPER":
									RunicPlayerBukkit.incrementPlayerKillCount(
											monsterEnt.getKiller()
													.getUniqueId(),
											"KillCreeper");
									attemptPowersSkillUp = true;
									break;
								case "ENDERMAN":
									RunicPlayerBukkit.incrementPlayerKillCount(
											monsterEnt.getKiller()
													.getUniqueId(),
											"KillEnderman");
									attemptPowersSkillUp = true;
									break;
								case "SPIDER":
									RunicPlayerBukkit.incrementPlayerKillCount(
											monsterEnt.getKiller()
													.getUniqueId(),
											"KillSpider");
									attemptPowersSkillUp = true;
									break;
								case "CAVE_SPIDER":
									RunicPlayerBukkit.incrementPlayerKillCount(
											monsterEnt.getKiller()
													.getUniqueId(),
											"KillCaveSpider");
									attemptPowersSkillUp = true;
									break;
								case "SQUID":
									RunicPlayerBukkit
											.incrementPlayerKillCount(
													monsterEnt.getKiller()
															.getUniqueId(),
													"KillSquid");
									attemptPowersSkillUp = true;
									break;
								case "ENDER_DRAGON":
									RunicPlayerBukkit.incrementPlayerKillCount(
											monsterEnt.getKiller()
													.getUniqueId(),
											"KillEnderDragon");
									attemptPowersSkillUp = true;
									break;
								case "PIG_ZOMBIE":
									RunicPlayerBukkit.incrementPlayerKillCount(
											monsterEnt.getKiller()
													.getUniqueId(),
											"KillPigZombie");
									attemptPowersSkillUp = true;
									break;
								case "GHAST":
									RunicPlayerBukkit
											.incrementPlayerKillCount(
													monsterEnt.getKiller()
															.getUniqueId(),
													"KillGhast");
									attemptPowersSkillUp = true;
									break;
								case "CHICKEN":
									RunicPlayerBukkit.incrementPlayerKillCount(
											monsterEnt.getKiller()
													.getUniqueId(),
											"KillChicken");
									break;
								case "COW":
									RunicPlayerBukkit.incrementPlayerKillCount(
											monsterEnt.getKiller()
													.getUniqueId(), "KillCow");
									break;
								case "SHEEP":
									RunicPlayerBukkit
											.incrementPlayerKillCount(
													monsterEnt.getKiller()
															.getUniqueId(),
													"KillSheep");
									break;
								case "PIG":
									RunicPlayerBukkit.incrementPlayerKillCount(
											monsterEnt.getKiller()
													.getUniqueId(), "KillPig");
									break;
								case "OCELOT":
									RunicPlayerBukkit.incrementPlayerKillCount(
											monsterEnt.getKiller()
													.getUniqueId(),
											"KillOcelot");
									break;
								case "BAT":
									RunicPlayerBukkit.incrementPlayerKillCount(
											monsterEnt.getKiller()
													.getUniqueId(), "KillBat");
									attemptPowersSkillUp = true;
									break;
								case "MUSHROOM_COW":
									RunicPlayerBukkit.incrementPlayerKillCount(
											monsterEnt.getKiller()
													.getUniqueId(),
											"KillMooshroom");
									break;
								case "RABBIT":
									RunicPlayerBukkit.incrementPlayerKillCount(
											monsterEnt.getKiller()
													.getUniqueId(),
											"KillRabbit");
									break;
								case "POLAR_BEAR":
									RunicPlayerBukkit.incrementPlayerKillCount(
											monsterEnt.getKiller()
													.getUniqueId(),
											"KillPolarBear");
									break;
								case "WOLF":
									RunicPlayerBukkit.incrementPlayerKillCount(
											monsterEnt.getKiller()
													.getUniqueId(), "KillWolf");
									attemptPowersSkillUp = true;
									break;
								case "ENDERMITE":
									RunicPlayerBukkit.incrementPlayerKillCount(
											monsterEnt.getKiller()
													.getUniqueId(),
											"KillEndermite");
									attemptPowersSkillUp = true;
									break;
								case "GUARDIAN":
									RunicPlayerBukkit.incrementPlayerKillCount(
											monsterEnt.getKiller()
													.getUniqueId(),
											"KillGuardian");
									attemptPowersSkillUp = true;
									break;
								case "ELDER_GUARDIAN":
									RunicPlayerBukkit.incrementPlayerKillCount(
											monsterEnt.getKiller()
													.getUniqueId(),
											"KillElderGuardian");
									attemptPowersSkillUp = true;
									break;
								case "SNOWMAN":
									RunicPlayerBukkit.incrementPlayerKillCount(
											monsterEnt.getKiller()
													.getUniqueId(),
											"KillSnowGolem");
									break;
								case "VILLAGER":
									RunicPlayerBukkit.incrementPlayerKillCount(
											monsterEnt.getKiller()
													.getUniqueId(),
											"KillVillager");
									break;
								default:
									break;
								}

								if (attemptPowersSkillUp && heldWeaponHasLore) {
									faithMap.get(
											monsterEnt.getKiller()
													.getUniqueId()).trySkillUp(
											(Player) monsterEnt.getKiller(),
											faithMap.get(
													monsterEnt.getKiller()
															.getUniqueId())
													.getPrimaryFaith(),
											faithStrength, "FaithWeapon");
								}

								/*
								 * if (attemptPowersSkillUp &&
								 * heldWeaponHasLore) { // new //
								 * Powers(monsterEnt.getKiller().getUniqueId())
								 * // .trySkillUp(UUID, skill);
								 * 
								 * if (monsterEnt.getKiller().getItemInHand()
								 * .getItemMeta().getLore().toString()
								 * .contains("Empowered Spirit")) {
								 * 
								 * String weaponName = monsterEnt
								 * .getKiller().getItemInHand()
								 * .getItemMeta().getDisplayName();
								 * 
								 * if (weaponName.contains("Beastfang")) { if
								 * (weaponName.length() == 13) { // 1 star
								 * Powers.trySkillUp(monsterEnt .getKiller()
								 * .getUniqueId(), "Skill_Beasts", 1);
								 * 
								 * } else if (weaponName.length() == 14) { // 2
								 * stars Powers.trySkillUp(monsterEnt
								 * .getKiller() .getUniqueId(), "Skill_Beasts",
								 * 2); } else { getLogger() .log(Level.SEVERE,
								 * "DEBUG: Invalid weapon name length: " +
								 * weaponName .length()); } } else { getLogger()
								 * .log(Level.SEVERE,
								 * "DEBUG: Invalid weapon name. Skill up attempt aborted."
								 * ); }
								 * 
								 * } // end if - skillUp }// end if checking for
								 * specific lore
								 */
							} // end run()
						}); // delay // end task method
			} // end else
		} // end LivingEntity check (if)
	} // end method

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
							.contains("Mining")) {

				// force Keep Inventory on the event!! because the world-level
				// setting is unreliable with multiverse :(
				event.setKeepInventory(true);

				if (targetPlayer.checkPlayerPermission("rp.graves")
						&& targetPlayer.getPlayerSouls() == 0) {
					// Player has the graves permission and no souls left... so
					// trigger a grave!

					RunicDeathChest.savePlayerDeath_v19((Player) event
							.getEntity(), event.getEntity().getLocation());

					// Old version, not used anymore!!
					// RunicDeathChest.savePlayerDeath((Player)
					// event.getEntity(),
					// event.getEntity().getLocation());
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
	@SuppressWarnings("deprecation")
	public void updatePlayerInfoOnJoin(String name, UUID pUUID) {
		final Date now = new Date();
		final String playerName = name;
		final UUID playerUUID = pUUID;

		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));
		final Connection dbConn = MySQL.openConnection();
		int rowCount = -1;
		int rowCountnameMatch = -1;

		try {
			PreparedStatement dStmt = dbConn
					.prepareStatement("SELECT COUNT(*) as Total FROM rp_PlayerInfo WHERE UUID = ?;");
			dStmt.setString(1, playerUUID.toString());
			ResultSet dbResult = dStmt.executeQuery();
			while (dbResult.next()) {
				rowCount = dbResult.getInt("Total");
			}
			dStmt.close();

			PreparedStatement zStmt = dbConn
					.prepareStatement("SELECT COUNT(*) as Total FROM rp_PlayerInfo WHERE PlayerName = ?;");
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
							+ playerName + " because: " + e.getMessage());
		}

		if (rowCount != rowCountnameMatch) {
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
					"sc Name change detected for " + playerName);
			Bukkit.getLogger().log(Level.INFO,
					"[RP] Name change detected for " + playerName);
		}

		try {

			// if this player has no rows in the table yet

			if (rowCount == 0) {

				Bukkit.getServer().getScheduler()
						.scheduleAsyncDelayedTask(instance, new Runnable() {
							public void run() {
								// tell the other server this one is reconnected
								// to the universe
								ByteArrayDataOutput out = ByteStreams
										.newDataOutput();
								out.writeUTF("Forward"); // So BungeeCord knows
															// to forward it
								out.writeUTF("ONLINE");
								out.writeUTF("NewPlayer"); // The channel name
															// to check if this
															// your data

								ByteArrayOutputStream msgbytes = new ByteArrayOutputStream();
								DataOutputStream msgout = new DataOutputStream(
										msgbytes);

								try {
									msgout.writeUTF(Bukkit
											.getPlayer(playerUUID)
											.getDisplayName()); // You can do
																// anything
									// msgout
									msgout.writeShort(123);
								} catch (IOException e) {
								}

								out.writeShort(msgbytes.toByteArray().length);
								out.write(msgbytes.toByteArray());

								// If you don't care about the player
								// Player player =
								// Iterables.getFirst(Bukkit.getOnlinePlayers(),
								// null);
								// Else, specify them

								Bukkit.getPlayer(playerUUID).sendPluginMessage(
										instance, "BungeeCord",
										out.toByteArray());
							}
						}, 140);

				// /////////////////////
				PreparedStatement dStmt = dbConn
						.prepareStatement("INSERT INTO rp_PlayerInfo (`PlayerName`, `UUID`, `ActiveFaith`, `LastIP`, `FirstSeen`, `LastSeen`) VALUES "
								+ "(?, ?, ?, ?, ?, ?);");
				dStmt.setString(1, playerName);
				dStmt.setString(2, playerUUID.toString());
				dStmt.setString(3, "Sun");
				dStmt.setString(4, Bukkit.getPlayer(playerUUID).getAddress()
						.getAddress().getHostAddress());
				dStmt.setLong(5, now.getTime());
				dStmt.setLong(6, now.getTime());

				dStmt.executeUpdate();
				dStmt.close();

				// if this player has 1 row in the table
			} else if (rowCount == 1) {
				PreparedStatement dStmt = dbConn
						.prepareStatement("UPDATE `rp_PlayerInfo` SET LastSeen=?, PlayerName=?, LastIP=? WHERE UUID=?;");
				dStmt.setLong(1, now.getTime());
				dStmt.setString(2, playerName);
				dStmt.setString(3, Bukkit.getPlayer(playerUUID).getAddress()
						.getAddress().getHostAddress());
				dStmt.setString(4, playerUUID.toString());
				dStmt.executeUpdate();
				dStmt.close();
				Bukkit.getLogger().log(Level.INFO,
						"[RP] PlayerInfo data updated for " + playerName);

				// if this player has MORE than 1 row in the
				// table
			} else if (rowCount > 1) {
				int counter = 1;
				PreparedStatement zStmt = dbConn
						.prepareStatement("SELECT * FROM rp_PlayerInfo WHERE UUID = ? ORDER BY ID ASC;");
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
						dStmt.setString(3, playerUUID.toString());
						dStmt.executeUpdate();
						dStmt.close();

						Bukkit.getLogger().log(
								Level.INFO,
								"[RP] PlayerInfo data [row "
										+ zResult.getInt("ID")
										+ "] updated for " + playerName);
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
							+ playerName + " because: " + e.getMessage());
		}

	}

	// Add Runic Eye
	public void placeRunicEye(Location loc, Player p) {
		String locString = loc.getWorld().getName() + "." + loc.getBlockX()
				+ "." + loc.getBlockY() + "." + loc.getBlockZ();

		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));
		try {
			final Connection dbCon = MySQL.openConnection();

			String simpleProc = "{ call Add_Runic_Eye(?) }";
			CallableStatement cs = dbCon.prepareCall(simpleProc);
			cs.setString("Loc_param", locString);
			cs.executeUpdate();

			cs.close();
			dbCon.close();
			p.sendMessage("Runic Eye successfully created.");

		} catch (SQLException z) {
			getLogger().log(
					Level.SEVERE,
					"Failed RP.placeRunicEye " + loc.toString() + "- "
							+ z.getMessage());
			p.sendMessage("Runic Eye creation failed..");
		}

	}

	// Add Prayer Book
	public void placePrayerBook(Location loc, Player p) {
		String locString = loc.getWorld().getName() + "." + loc.getBlockX()
				+ "." + loc.getBlockY() + "." + loc.getBlockZ();

		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));
		try {
			final Connection dbCon = MySQL.openConnection();

			String simpleProc = "{ call Add_Prayer_Book(?) }";
			CallableStatement cs = dbCon.prepareCall(simpleProc);
			cs.setString("Loc_param", locString);
			cs.executeUpdate();

			cs.close();
			dbCon.close();
			p.sendMessage("PrayerBook successfully created.");

		} catch (SQLException z) {
			getLogger().log(
					Level.SEVERE,
					"Failed RP.placePrayerBook " + loc.toString() + "- "
							+ z.getMessage());
			p.sendMessage("Prayer Book creation failed..");
		}

	}

	// Add Runic Eye
	public static void loadPrayerBooks() {

		prayerBooks.clear();

		// remove the eyes before we add them again
		for (Entity item : prayerBookEntities.keySet()) {
			item.remove();
		}

		prayerBookEntities.clear();

		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));

		try {

			final Connection dbCon = MySQL.openConnection();
			Statement dbStmt = dbCon.createStatement();
			ResultSet bookResult = dbStmt
					.executeQuery("SELECT * FROM rp_PrayerBooks;");
			if (!bookResult.isBeforeFirst()) {
				// No results
				// do nothing
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
						"sc Tried to load Book settings, but couldn't find them in the DB!");
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
						"sc This is a critical problem; Prayer Books will not work :(");

				dbCon.close();
				return;
			} else {
				// results found!
				while (bookResult.next()) {

					String[] locParts = bookResult.getString("Location").split(
							"[\\x2E]");
					Location targetLoc = new Location(
							Bukkit.getWorld(locParts[0]),
							Integer.parseInt(locParts[1]),
							Integer.parseInt(locParts[2]),
							Integer.parseInt(locParts[3]));

					Item item;

					if (bookResult.getString("Type").equals("Paper")) {
						item = targetLoc.getWorld().dropItemNaturally(
								targetLoc, new ItemStack(Material.PAPER));
					} else {
						item = targetLoc.getWorld().dropItemNaturally(
								targetLoc, new ItemStack(Material.BOOK));
					}

					item.setCustomName(bookResult.getString("Name"));
					item.setCustomNameVisible(true);
					item.setPickupDelay(90000);
					item.setVelocity(new Vector(0, 0, 0));
					item.teleport(targetLoc.add(0.5, 1, 0.5));

					prayerBookEntities.put(item, bookResult.getString("Name"));

					RunicParadise.prayerBooks.put(
							targetLoc.add(-0.5, -1, -0.5), new String[] {
									bookResult.getString("Name"), // 0
									item.getUniqueId().toString(), // 1
									bookResult.getString("Message"), // 2
									bookResult.getString("Requirements"), // 3
									bookResult.getString("ItemCount"), // 4
									bookResult.getString("Item1ID"), // 5
									bookResult.getString("Item1Data"), // 6
									bookResult.getString("Item1Count"), // 7
									bookResult.getString("Item2ID"), // 8
									bookResult.getString("Item2Data"), // 9
									bookResult.getString("Item2Count"), // 10
									bookResult.getString("Item3ID"), // 11
									bookResult.getString("Item3Data"), // 12
									bookResult.getString("Item3Count"), // 13
									bookResult.getString("Item4ID"), // 14
									bookResult.getString("Item4Data"), // 15
									bookResult.getString("Item4Count"), // 16
									bookResult.getString("Type"), // 17
									bookResult.getString("FaithName") }); // 18

				}

				dbCon.close();
			}

		} catch (SQLException z) {
			Bukkit.getLogger().log(Level.SEVERE,
					"Failed RP.loadPrayerBook " + z.getMessage());
		}

	}

	// Add Runic Eye
	public static void loadRunicEyes() {

		runicEyes.clear();

		// remove the eyes before we add them again
		for (Entity item : runicEyeEntities.keySet()) {
			item.remove();
		}

		runicEyeEntities.clear();

		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));
		String eyeList = "";
		try {

			final Connection dbCon = MySQL.openConnection();
			Statement dbStmt = dbCon.createStatement();
			ResultSet eyeResult = dbStmt
					.executeQuery("SELECT * FROM rp_RunicEyes;");
			if (!eyeResult.isBeforeFirst()) {
				// No results
				// do nothing
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
						"sc Tried to load Eye settings, but couldn't find them in the DB!");
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
						"sc This is a critical problem; Runic Eyes will not work :(");

				dbCon.close();
				return;
			} else {
				// results found!
				while (eyeResult.next()) {

					String[] locParts = eyeResult.getString("Location").split(
							"[\\x2E]");
					Location targetLoc = new Location(
							Bukkit.getWorld(locParts[0]),
							Integer.parseInt(locParts[1]),
							Integer.parseInt(locParts[2]),
							Integer.parseInt(locParts[3]));

					Item item = targetLoc.getWorld().dropItemNaturally(
							targetLoc, new ItemStack(Material.EYE_OF_ENDER));

					item.setCustomName(eyeResult.getString("Name"));
					item.setCustomNameVisible(true);
					item.setPickupDelay(90000);
					item.setVelocity(new Vector(0, 0, 0));
					item.teleport(targetLoc.add(0.5, 1, 0.5));

					runicEyeEntities.put(item, eyeResult.getString("Name"));

					RunicParadise.runicEyes.put(
							targetLoc.add(-0.5, -1, -0.5),
							new String[] { eyeResult.getString("Name"),
									item.getUniqueId().toString(),
									eyeResult.getString("Message") });
					eyeList += eyeResult.getString("Name") + ". ";
				}

				dbCon.close();
			}

		} catch (SQLException z) {
			Bukkit.getLogger().log(Level.SEVERE,
					"Failed Faith.faithSettings " + z.getMessage());
		}

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
				.getServicesManager().getRegistration(
						net.milkbowl.vault.permission.Permission.class);
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

	public static BlockFace getPlayerFacing(Player player) {

		float y = player.getLocation().getYaw();
		if (y < 0)
			y += 360;
		y %= 360;
		int i = (int) ((y + 8) / 22.5);

		if (i == 0)
			return BlockFace.WEST;
		else if (i == 1)
			return BlockFace.NORTH_WEST;
		else if (i == 2)
			return BlockFace.NORTH_WEST;
		else if (i == 3)
			return BlockFace.NORTH_WEST;
		else if (i == 4)
			return BlockFace.NORTH;
		else if (i == 5)
			return BlockFace.NORTH_EAST;
		else if (i == 6)
			return BlockFace.NORTH_EAST;
		else if (i == 7)
			return BlockFace.NORTH_EAST;
		else if (i == 8)
			return BlockFace.EAST;
		else if (i == 9)
			return BlockFace.SOUTH_EAST;
		else if (i == 10)
			return BlockFace.SOUTH_EAST;
		else if (i == 11)
			return BlockFace.SOUTH_EAST;
		else if (i == 12)
			return BlockFace.SOUTH;
		else if (i == 13)
			return BlockFace.SOUTH_WEST;
		else if (i == 14)
			return BlockFace.SOUTH_WEST;
		else if (i == 15)
			return BlockFace.SOUTH_WEST;

		return BlockFace.WEST;

	}

	public static void showRunicCarnivalMenu(Player p) {

		Inventory carnivalMenu = Bukkit.createInventory(null, 45,
				ChatColor.DARK_RED + "" + ChatColor.BOLD
						+ "Runic Carnival Menu");

		ItemMeta meta;
		ArrayList<String> mainLore = new ArrayList<String>();
		mainLore.add(ChatColor.YELLOW + "The Runic Carnival is where you'll");
		mainLore.add(ChatColor.YELLOW + "find minigames, mazes, parkours,");
		mainLore.add(ChatColor.YELLOW + "and arenas. Click the blocks below");
		mainLore.add(ChatColor.YELLOW + "to join in the fun!");

		ItemStack main = new ItemStack(Material.BEACON, 1);
		meta = main.getItemMeta();
		meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD
				+ "Carnival Menu");
		meta.setLore(mainLore);
		main.setItemMeta(meta);

		ItemStack slot1 = new ItemStack(Material.STAINED_GLASS_PANE, 1,
				(short) 10);
		meta = slot1.getItemMeta();
		meta.setDisplayName(" ");
		slot1.setItemMeta(meta);
		ItemStack slot2 = new ItemStack(Material.STAINED_GLASS_PANE, 1,
				(short) 2);
		meta = slot2.getItemMeta();
		meta.setDisplayName(" ");
		slot2.setItemMeta(meta);
		ItemStack slot3 = new ItemStack(Material.STAINED_GLASS_PANE, 1,
				(short) 6);
		meta = slot3.getItemMeta();
		meta.setDisplayName(" ");
		slot3.setItemMeta(meta);
		ItemStack slot4 = new ItemStack(Material.STAINED_GLASS_PANE, 1,
				(short) 14);
		meta = slot4.getItemMeta();
		meta.setDisplayName(" ");
		slot4.setItemMeta(meta);
		ItemStack slot6 = new ItemStack(Material.STAINED_GLASS_PANE, 1,
				(short) 4);
		meta = slot6.getItemMeta();
		meta.setDisplayName(" ");
		slot6.setItemMeta(meta);
		ItemStack slot7 = new ItemStack(Material.STAINED_GLASS_PANE, 1,
				(short) 13);
		meta = slot7.getItemMeta();
		meta.setDisplayName(" ");
		slot7.setItemMeta(meta);
		ItemStack slot8 = new ItemStack(Material.STAINED_GLASS_PANE, 1,
				(short) 9);
		meta = slot8.getItemMeta();
		meta.setDisplayName(" ");
		slot8.setItemMeta(meta);
		ItemStack slot9 = new ItemStack(Material.STAINED_GLASS_PANE, 1,
				(short) 11);
		meta = slot9.getItemMeta();
		meta.setDisplayName(" ");
		slot9.setItemMeta(meta);

		carnivalMenu.setItem(0, slot1);
		carnivalMenu.setItem(1, slot2);
		carnivalMenu.setItem(2, slot3);
		carnivalMenu.setItem(3, slot4);
		carnivalMenu.setItem(4, main);
		carnivalMenu.setItem(5, slot6);
		carnivalMenu.setItem(6, slot7);
		carnivalMenu.setItem(7, slot8);
		carnivalMenu.setItem(8, slot9);

		ItemStack infoCenter;
		infoCenter = new ItemStack(Material.WOOL, 1, (short) 1);
		meta = infoCenter.getItemMeta();
		meta.setDisplayName("Info Center");
		infoCenter.setItemMeta(meta);

		ItemStack prizeCabin;
		prizeCabin = new ItemStack(Material.WOOL, 1, (short) 4);
		meta = prizeCabin.getItemMeta();
		meta.setDisplayName("Prize Cabin");
		prizeCabin.setItemMeta(meta);

		ItemStack mazes;
		mazes = new ItemStack(Material.WOOL, 1, (short) 2);
		meta = mazes.getItemMeta();
		meta.setDisplayName("Mazes");
		mazes.setItemMeta(meta);

		ItemStack adventureIslands;
		adventureIslands = new ItemStack(Material.WOOL, 1, (short) 7);
		meta = adventureIslands.getItemMeta();
		meta.setDisplayName("Adventure Islands");
		adventureIslands.setItemMeta(meta);

		ItemStack parkours;
		parkours = new ItemStack(Material.WOOL, 1, (short) 3);
		meta = parkours.getItemMeta();
		meta.setDisplayName("Parkours");
		parkours.setItemMeta(meta);

		ItemStack battleTower;
		battleTower = new ItemStack(Material.WOOL, 1, (short) 5);
		meta = battleTower.getItemMeta();
		meta.setDisplayName("Battle Tower");
		battleTower.setItemMeta(meta);

		ItemStack creationZone;
		creationZone = new ItemStack(Material.WOOL, 1, (short) 6);
		meta = creationZone.getItemMeta();
		meta.setDisplayName("Creation Zone");
		creationZone.setItemMeta(meta);

		carnivalMenu.setItem(19, infoCenter);
		carnivalMenu.setItem(20, prizeCabin);
		carnivalMenu.setItem(21, mazes);
		carnivalMenu.setItem(22, parkours);
		carnivalMenu.setItem(23, adventureIslands);
		carnivalMenu.setItem(24, battleTower);
		carnivalMenu.setItem(25, creationZone);

		ItemStack token = new ItemStack(Material.DOUBLE_PLANT, 1, (short) 0);
		ItemMeta meta1 = token.getItemMeta();
		ArrayList<String> tokenLore = new ArrayList<>();

		tokenLore.add(ChatColor.GRAY + "Current Tokens Available");
		tokenLore.add(ChatColor.GREEN
				+ ""
				+ +new RunicPlayerBukkit(p.getUniqueId())
						.getPlayerTokenBalance());
		tokenLore.add(ChatColor.GRAY + "Lifetime Tokens:");
		tokenLore.add(ChatColor.YELLOW
				+ ""
				+ +new RunicPlayerBukkit(p.getUniqueId())
						.getPlayerLifetimeTokens());
		meta1.setLore(tokenLore);
		meta1.setDisplayName(ChatColor.RESET + "" + ChatColor.BLUE
				+ "Runic Carnival Tokens");
		token.setItemMeta(meta1);

		carnivalMenu.setItem(17, token);

		p.openInventory(carnivalMenu);

	}

	// valid puzzleTypes = M / P
	public static void showRunicCarnivalMenu_Puzzles(Player p,
			Character puzzleType) {

		String typeSingle;
		String typePlural;
		if (puzzleType == 'M') {
			typePlural = "Mazes";
			typeSingle = "Maze";
		} else {
			typePlural = "Parkours";
			typeSingle = "Parkour";
		}
		Inventory puzzleMenu = Bukkit.createInventory(null, 45,
				ChatColor.DARK_RED + "" + ChatColor.BOLD + "Runic Carnival - "
						+ typePlural);

		ItemMeta meta;
		ArrayList<String> mainLore = new ArrayList<String>();
		mainLore.add(ChatColor.YELLOW + "x");
		mainLore.add(ChatColor.YELLOW + "x");
		mainLore.add(ChatColor.YELLOW + "x");
		mainLore.add(ChatColor.YELLOW + "x");

		ItemStack main = new ItemStack(Material.BEACON, 1);
		meta = main.getItemMeta();
		meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + typeSingle
				+ " Menu");
		meta.setLore(mainLore);
		main.setItemMeta(meta);

		ItemStack slot1 = new ItemStack(Material.STAINED_GLASS_PANE, 1,
				(short) 10);
		meta = slot1.getItemMeta();
		meta.setDisplayName(" ");
		slot1.setItemMeta(meta);
		ItemStack slot2 = new ItemStack(Material.STAINED_GLASS_PANE, 1,
				(short) 2);
		meta = slot2.getItemMeta();
		meta.setDisplayName(" ");
		slot2.setItemMeta(meta);
		ItemStack slot3 = new ItemStack(Material.STAINED_GLASS_PANE, 1,
				(short) 6);
		meta = slot3.getItemMeta();
		meta.setDisplayName(" ");
		slot3.setItemMeta(meta);
		ItemStack slot4 = new ItemStack(Material.STAINED_GLASS_PANE, 1,
				(short) 14);
		meta = slot4.getItemMeta();
		meta.setDisplayName(" ");
		slot4.setItemMeta(meta);
		ItemStack slot6 = new ItemStack(Material.STAINED_GLASS_PANE, 1,
				(short) 4);
		meta = slot6.getItemMeta();
		meta.setDisplayName(" ");
		slot6.setItemMeta(meta);
		ItemStack slot7 = new ItemStack(Material.STAINED_GLASS_PANE, 1,
				(short) 13);
		meta = slot7.getItemMeta();
		meta.setDisplayName(" ");
		slot7.setItemMeta(meta);
		ItemStack slot8 = new ItemStack(Material.STAINED_GLASS_PANE, 1,
				(short) 9);
		meta = slot8.getItemMeta();
		meta.setDisplayName(" ");
		slot8.setItemMeta(meta);
		ItemStack slot9 = new ItemStack(Material.STAINED_GLASS_PANE, 1,
				(short) 11);
		meta = slot9.getItemMeta();
		meta.setDisplayName(" ");
		slot9.setItemMeta(meta);

		puzzleMenu.setItem(0, slot1);
		puzzleMenu.setItem(1, slot2);
		puzzleMenu.setItem(2, slot3);
		puzzleMenu.setItem(3, slot4);
		puzzleMenu.setItem(4, main);
		puzzleMenu.setItem(5, slot6);
		puzzleMenu.setItem(6, slot7);
		puzzleMenu.setItem(7, slot8);
		puzzleMenu.setItem(8, slot9);

		ItemStack wings;
		wings = new ItemStack(Material.ELYTRA, 1);
		meta = wings.getItemMeta();
		meta.setDisplayName("Warp to Puzzle Kiosk");
		wings.setItemMeta(meta);

		puzzleMenu.setItem(17, wings);

		int mazeSlot = 19;

		try {
			MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
					"dbHost"), instance.getConfig().getString("dbPort"),
					instance.getConfig().getString("dbDatabase"), instance
							.getConfig().getString("dbUser"), instance
							.getConfig().getString("dbPassword"));
			final Connection dbCon = MySQL.openConnection();
			Statement dbStmt = dbCon.createStatement();
			ResultSet menuResult = dbStmt
					.executeQuery("SELECT * FROM rp_RunicGames WHERE GameType='"
							+ typeSingle + "';");
			int playerMazeCount = 0;
			long playerLastCompletion = 0;
			SimpleDateFormat sdf = new SimpleDateFormat(
					"EEE, MMM d yyyy, HH:mm z", Locale.US);

			if (!menuResult.isBeforeFirst()) {
				// No results
				// do nothing
				Bukkit.getLogger()
						.log(Level.INFO,
								"Failed to display carnival menu for puzzles. Found no entries in the database.");
				dbCon.close();
			} else {
				// results found!
				while (menuResult.next()) {
					playerMazeCount = getPlayerMazeCompletionCount(p,
							menuResult.getInt("ID"));

					ItemStack mazeIcon = new ItemStack(Material.EMERALD_BLOCK,
							1);

					if (playerMazeCount == 0) {
						mazeIcon = new ItemStack(Material.REDSTONE_BLOCK, 1);
					} else {
						playerLastCompletion = getPlayerMazeLastCompletion(p,
								menuResult.getInt("ID"));

					}

					meta = mazeIcon.getItemMeta();
					meta.setDisplayName(ChatColor.RESET + "" + ChatColor.BLUE
							+ menuResult.getString("GameName"));

					ArrayList<String> mazeLore = new ArrayList<String>();
					mazeLore.add(ChatColor.GREEN + "Times Completed: "
							+ ChatColor.YELLOW + playerMazeCount);
					mazeLore.add(null);
					if (playerMazeCount == 0) {
						mazeLore.add(ChatColor.GREEN + "First-Time Rewards:");
						mazeLore.add(ChatColor.GRAY + "Karma: "
								+ ChatColor.YELLOW
								+ menuResult.getInt("KarmaReward"));
						mazeLore.add(ChatColor.GRAY + "Tokens: "
								+ ChatColor.YELLOW
								+ menuResult.getInt("TokenReward"));
						mazeLore.add(ChatColor.GRAY + "Souls: "
								+ ChatColor.YELLOW
								+ menuResult.getInt("SoulReward"));
						mazeLore.add(ChatColor.GRAY + "Runics: "
								+ ChatColor.YELLOW
								+ menuResult.getInt("CashReward"));
						// 6 days in ms
						// true if >6 days have passed since last completion
					} else if (new Date().getTime() - playerLastCompletion > PUZZLE_REPEAT_TIME
							&& menuResult.getInt("ID") != 4) {
						mazeLore.add(ChatColor.GREEN + "Weekly Rewards:");
						mazeLore.add(ChatColor.GRAY
								+ "Karma: "
								+ ChatColor.YELLOW
								+ (Integer) (menuResult.getInt("KarmaReward") / 2));
						mazeLore.add(ChatColor.GRAY
								+ "Tokens: "
								+ ChatColor.YELLOW
								+ (Integer) (menuResult.getInt("TokenReward") / 2));
						mazeLore.add(ChatColor.GRAY
								+ "Souls: "
								+ ChatColor.YELLOW
								+ (Integer) (menuResult.getInt("SoulReward") / 2));
						mazeLore.add(ChatColor.GRAY
								+ "Runics: "
								+ ChatColor.YELLOW
								+ (Integer) (menuResult.getInt("CashReward") / 2));
						mazeLore.add(null);
						mazeLore.add(ChatColor.GREEN + "Last Completion:");
						mazeLore.add(ChatColor.GRAY
								+ sdf.format(new Date(playerLastCompletion)));

						// for dungeon maze / adv parkour - only reward player
						// once due to
						// nature of maze
					} else if (menuResult.getInt("ID") == 4) {
						mazeLore.add(ChatColor.RED
								+ "You only receive rewards for");
						mazeLore.add(ChatColor.RED
								+ "completing this puzzle once!");

						// true if <6 days have passed since last completion
						// i.e. NO prize yet!
					} else {
						mazeLore.add(ChatColor.RED
								+ "You only receive rewards for");
						mazeLore.add(ChatColor.RED
								+ "completing a puzzle once per");
						mazeLore.add(ChatColor.RED + "week!");

						mazeLore.add(null);
						mazeLore.add(ChatColor.RED + "Last Completion:");
						mazeLore.add(ChatColor.GRAY
								+ sdf.format(new Date(playerLastCompletion)));
						// true if <6 days have passed since last completion
						// i.e. NO prize yet!
					}

					// if its spawn maze or spawn parkour ...
					if (menuResult.getInt("ID") == 8
							|| menuResult.getInt("ID") == 8) {
						mazeLore.add(null);
						mazeLore.add(ChatColor.YELLOW
								+ "Want to try this puzzle? You'll");
						mazeLore.add(ChatColor.YELLOW
								+ "have to find the entrance!");
					} else {
						// its not those, so give the regular advice...
						mazeLore.add(null);
						mazeLore.add(ChatColor.YELLOW
								+ "Want to try this puzzle? Click");
						mazeLore.add(ChatColor.YELLOW
								+ "the Elytra icon to go to the portals!");
					}

					meta.setLore(mazeLore);
					mazeIcon.setItemMeta(meta);
					puzzleMenu.setItem(mazeSlot, mazeIcon);

					if (mazeSlot == 25) {
						mazeSlot = 28;
					} else if (mazeSlot == 34) {
						mazeSlot = 37;
					} else {
						mazeSlot++;
					}
				}
				dbStmt.close();
				dbCon.close();
			}

		} catch (SQLException z) {
			Bukkit.getLogger().log(
					Level.SEVERE,
					"Failed displaying runic carnival puzzle menu -"
							+ z.getMessage());
		}

		p.openInventory(puzzleMenu);
	}

	public static void showRunicCarnivalMenu_BattleTower(Player p) {

		Inventory carnivalMenu = Bukkit.createInventory(null, 45,
				ChatColor.DARK_RED + "" + ChatColor.BOLD
						+ "Runic Carnival - Arenas");

		ItemMeta meta;
		ArrayList<String> mainLore = new ArrayList<String>();
		mainLore.add(ChatColor.YELLOW + "x");
		mainLore.add(ChatColor.YELLOW + "x");
		mainLore.add(ChatColor.YELLOW + "x");
		mainLore.add(ChatColor.YELLOW + "x");

		ItemStack main = new ItemStack(Material.BEACON, 1);
		meta = main.getItemMeta();
		meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD
				+ "Carnival Menu");
		meta.setLore(mainLore);
		main.setItemMeta(meta);

		ItemStack slot1 = new ItemStack(Material.STAINED_GLASS_PANE, 1,
				(short) 10);
		meta = slot1.getItemMeta();
		meta.setDisplayName(" ");
		slot1.setItemMeta(meta);
		ItemStack slot2 = new ItemStack(Material.STAINED_GLASS_PANE, 1,
				(short) 2);
		meta = slot2.getItemMeta();
		meta.setDisplayName(" ");
		slot2.setItemMeta(meta);
		ItemStack slot3 = new ItemStack(Material.STAINED_GLASS_PANE, 1,
				(short) 6);
		meta = slot3.getItemMeta();
		meta.setDisplayName(" ");
		slot3.setItemMeta(meta);
		ItemStack slot4 = new ItemStack(Material.STAINED_GLASS_PANE, 1,
				(short) 14);
		meta = slot4.getItemMeta();
		meta.setDisplayName(" ");
		slot4.setItemMeta(meta);
		ItemStack slot6 = new ItemStack(Material.STAINED_GLASS_PANE, 1,
				(short) 4);
		meta = slot6.getItemMeta();
		meta.setDisplayName(" ");
		slot6.setItemMeta(meta);
		ItemStack slot7 = new ItemStack(Material.STAINED_GLASS_PANE, 1,
				(short) 13);
		meta = slot7.getItemMeta();
		meta.setDisplayName(" ");
		slot7.setItemMeta(meta);
		ItemStack slot8 = new ItemStack(Material.STAINED_GLASS_PANE, 1,
				(short) 9);
		meta = slot8.getItemMeta();
		meta.setDisplayName(" ");
		slot8.setItemMeta(meta);
		ItemStack slot9 = new ItemStack(Material.STAINED_GLASS_PANE, 1,
				(short) 11);
		meta = slot9.getItemMeta();
		meta.setDisplayName(" ");
		slot9.setItemMeta(meta);

		carnivalMenu.setItem(0, slot1);
		carnivalMenu.setItem(1, slot2);
		carnivalMenu.setItem(2, slot3);
		carnivalMenu.setItem(3, slot4);
		carnivalMenu.setItem(4, main);
		carnivalMenu.setItem(5, slot6);
		carnivalMenu.setItem(6, slot7);
		carnivalMenu.setItem(7, slot8);
		carnivalMenu.setItem(8, slot9);

		ArrayList<String> pvpLore = new ArrayList<String>();
		pvpLore.add(ChatColor.RED + "PvP");
		ArrayList<String> pveLore = new ArrayList<String>();
		pveLore.add(ChatColor.AQUA + "No PvP (PvE)");

		ItemStack paintball;
		paintball = new ItemStack(Material.SNOW_BALL, 1);
		meta = paintball.getItemMeta();
		meta.setDisplayName("Paintball");
		meta.setLore(pvpLore);
		paintball.setItemMeta(meta);

		ItemStack blockhunt;
		blockhunt = new ItemStack(Material.BOOKSHELF, 1);
		meta = blockhunt.getItemMeta();
		meta.setDisplayName("Blockhunt");
		meta.setLore(pvpLore);
		blockhunt.setItemMeta(meta);

		ItemStack ctf;
		ctf = new ItemStack(Material.BANNER, 1);
		meta = ctf.getItemMeta();
		meta.setDisplayName("Capture the Flag");
		meta.setLore(pvpLore);
		ctf.setItemMeta(meta);

		ItemStack pvpArena;
		pvpArena = new ItemStack(Material.IRON_AXE, 1);
		meta = pvpArena.getItemMeta();
		meta.setDisplayName("PvP Arena");
		meta.setLore(pvpLore);
		pvpArena.setItemMeta(meta);

		ItemStack spleef;
		spleef = new ItemStack(Material.GOLD_SPADE, 1);
		meta = spleef.getItemMeta();
		meta.setDisplayName("Spleef");
		meta.setLore(pvpLore);
		spleef.setItemMeta(meta);

		ItemStack mobarena;
		mobarena = new ItemStack(Material.SKULL_ITEM, 1,
				(short) SkullType.ZOMBIE.ordinal());
		meta = mobarena.getItemMeta();
		meta.setDisplayName("Mob Arena");
		mobarena.setItemMeta(meta);

		carnivalMenu.setItem(20, paintball);
		carnivalMenu.setItem(21, blockhunt);
		carnivalMenu.setItem(22, ctf);
		carnivalMenu.setItem(23, pvpArena);
		carnivalMenu.setItem(24, spleef);

		carnivalMenu.setItem(31, mobarena);

		p.openInventory(carnivalMenu);

	}

	public static long getPlayerMazeLastCompletion(Player p, int puzzleID) {
		long lastCom = 0;

		try {
			MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
					"dbHost"), instance.getConfig().getString("dbPort"),
					instance.getConfig().getString("dbDatabase"), instance
							.getConfig().getString("dbUser"), instance
							.getConfig().getString("dbPassword"));
			final Connection dbCon = MySQL.openConnection();
			Statement dbStmt = dbCon.createStatement();
			ResultSet mcResult = dbStmt
					.executeQuery("SELECT * FROM rp_RunicGameCompletions WHERE UUID='"
							+ p.getUniqueId()
							+ "' AND GameID="
							+ puzzleID
							+ ";");
			if (!mcResult.isBeforeFirst()) {
				// No results
				// do nothing
				dbStmt.close();
				dbCon.close();

				return 0;

			} else {
				// results found!
				while (mcResult.next()) {

					lastCom = mcResult.getLong("LastCompletion");

				}
				dbStmt.close();
				dbCon.close();
			}

		} catch (SQLException z) {
			Bukkit.getLogger()
					.log(Level.SEVERE,
							"Failed getting puzzle completion count -"
									+ z.getMessage());
		}

		return lastCom;
	}

	public static int getPlayerMazeCompletionCount(Player p, int puzzleID) {
		int completionCount = 0;

		try {
			MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
					"dbHost"), instance.getConfig().getString("dbPort"),
					instance.getConfig().getString("dbDatabase"), instance
							.getConfig().getString("dbUser"), instance
							.getConfig().getString("dbPassword"));
			final Connection dbCon = MySQL.openConnection();
			Statement dbStmt = dbCon.createStatement();
			ResultSet mcResult = dbStmt
					.executeQuery("SELECT * FROM rp_RunicGameCompletions WHERE UUID='"
							+ p.getUniqueId()
							+ "' AND GameID="
							+ puzzleID
							+ ";");
			if (!mcResult.isBeforeFirst()) {
				// No results
				// do nothing
				dbStmt.close();
				dbCon.close();

				return 0;

			} else {
				// results found!
				while (mcResult.next()) {

					completionCount = mcResult.getInt("Count");

				}
				dbStmt.close();
				dbCon.close();
			}

		} catch (SQLException z) {
			Bukkit.getLogger()
					.log(Level.SEVERE,
							"Failed getting puzzle completion count -"
									+ z.getMessage());
		}

		return completionCount;
	}

	public static void addMazeCompletion(Player p, int puzzleID) {
		try {
			MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
					"dbHost"), instance.getConfig().getString("dbPort"),
					instance.getConfig().getString("dbDatabase"), instance
							.getConfig().getString("dbUser"), instance
							.getConfig().getString("dbPassword"));
			final Connection dbCon = MySQL.openConnection();

			// Get player puzzle results
			PreparedStatement dbStmt = dbCon
					.prepareStatement("SELECT * FROM rp_RunicGameCompletions WHERE UUID = ? AND GameID = ?;");

			dbStmt.setString(1, p.getUniqueId().toString());
			dbStmt.setInt(2, puzzleID);
			ResultSet mcResult = dbStmt.executeQuery();

			// Get puzzle data (for prizes) - and validate the puzzleID is ok
			PreparedStatement mzStmt = dbCon
					.prepareStatement("SELECT * FROM rp_RunicGames WHERE ID = ?;");

			mzStmt.setInt(1, puzzleID);
			ResultSet mzResult = mzStmt.executeQuery();

			PreparedStatement updStmt;

			int prizeKarma = 0;
			int prizeSouls = 0;
			int prizeRunics = 0;
			int prizeTokens = 0;

			if (!mzResult.isBeforeFirst()) {
				// No results, invalid puzzleID given

				p.sendMessage(ChatColor.GRAY
						+ "This maze has been configured incorrectly. Ask an admin to check the game ID# in the command block");
				dbCon.close();
				return;

			} else {
				// results found!
				mzResult.next();
				prizeKarma = mzResult.getInt("KarmaReward");
				prizeSouls = mzResult.getInt("SoulReward");
				prizeRunics = mzResult.getInt("CashReward");
				prizeTokens = mzResult.getInt("TokenReward");
			}

			if (!mcResult.isBeforeFirst()) {
				// No results, add a record
				// This is player's FIRST completion!!
				mcResult.next();
				updStmt = dbCon
						.prepareStatement("INSERT INTO rp_RunicGameCompletions (UUID, GameID, Count, LastCompletion) "
								+ "VALUES (?, ?, ?, ?);");
				updStmt.setString(1, p.getUniqueId().toString());
				updStmt.setInt(2, puzzleID);
				updStmt.setInt(3, 1);
				updStmt.setLong(4, new Date().getTime());
				updStmt.executeUpdate();

				RunicPlayerBukkit target = new RunicPlayerBukkit(
						p.getUniqueId());

				if (prizeKarma > 0) {
					new RunicPlayerBukkit(p.getUniqueId())
							.adjustPlayerKarma(prizeKarma);
				}
				if (prizeTokens > 0) {
					target.setPlayerTokenBalance(target.getPlayerTokenBalance()
							+ prizeTokens);
				}
				if (prizeSouls > 0) {
					target.setPlayerSouls(target.getPlayerSouls() + prizeSouls);
					p.sendMessage(ChatColor.GREEN + "You gained " + prizeSouls
							+ " souls!");
				}
				if (prizeRunics > 0) {
					RunicParadise.economy.depositPlayer(p, prizeRunics);
					p.sendMessage(ChatColor.GREEN + "You gained " + prizeRunics
							+ " runics!");
				}
				if (mzResult.getInt("ID") == 6) {
					// Dungeon Maze
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "give "
							+ p.getName() + " beacon 1");
					p.sendMessage(ChatColor.GOLD
							+ "You received a beacon for first completion here!");
				}
				if (mzResult.getInt("ID") == 7) {
					// Adventure Parkour
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "give "
							+ p.getName() + " witherskull 1");
					p.sendMessage(ChatColor.GOLD
							+ "You received a wither skull for first completion here!");
				}

				for (Player q : Bukkit.getOnlinePlayers()) {
					TitleAPI.sendTitle(
							q,
							2,
							3,
							2,
							ChatColor.GRAY + "" + ChatColor.BOLD
									+ p.getDisplayName(),
							ChatColor.YELLOW + "just completed the "
									+ mzResult.getString("GameName")
									+ " for the first time!");
				}

				dbStmt.close();
				dbCon.close();

			} else {
				// results found!
				// not player's first completion!
				mcResult.next();
				updStmt = dbCon
						.prepareStatement("UPDATE rp_RunicGameCompletions SET Count = ?, LastCompletion = ? WHERE "
								+ "UUID = ? AND GameID = ?;");
				updStmt.setInt(1, getPlayerMazeCompletionCount(p, puzzleID) + 1);
				updStmt.setLong(2, new Date().getTime());
				updStmt.setString(3, p.getUniqueId().toString());
				updStmt.setInt(4, puzzleID);
				updStmt.executeUpdate();

				if (new Date().getTime() - mcResult.getLong("LastCompletion") > PUZZLE_REPEAT_TIME) {

					RunicPlayerBukkit target = new RunicPlayerBukkit(
							p.getUniqueId());

					if (prizeKarma > 0) {
						new RunicPlayerBukkit(p.getUniqueId())
								.adjustPlayerKarma((int) (prizeKarma / 2));
					}
					if (prizeTokens > 0) {
						target.setPlayerTokenBalance(target
								.getPlayerTokenBalance()
								+ (int) (prizeTokens / 2));
					}
					if (prizeSouls > 0) {
						target.setPlayerSouls(target.getPlayerSouls()
								+ (int) (prizeSouls / 2));
						p.sendMessage(ChatColor.GREEN + "You gained "
								+ ((int) (prizeSouls / 2)) + " souls!");
					}
					if (prizeRunics > 0) {
						RunicParadise.economy.depositPlayer(p,
								(int) (prizeRunics / 2));
						p.sendMessage(ChatColor.GREEN + "You gained "
								+ ((int) (prizeRunics / 2)) + " runics!");
					}
				} else {
					// The required time hasnt passed yet to receive a reward
					// again ...
					p.sendMessage(ChatColor.RED
							+ "Congrats!! You received a reward for this puzzle less than a week ago. Check /games to see when you can claim this reward again!");
				}

			}

			dbStmt.close();

			dbCon.close();
		} catch (SQLException z) {
			Bukkit.getLogger().log(Level.SEVERE,
					"Failed adding maze completion " + z.getMessage());
		}
	}

	public static void mazeMigration() {
		int count = 0;

		try {
			MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
					"dbHost"), instance.getConfig().getString("dbPort"),
					instance.getConfig().getString("dbDatabase"), instance
							.getConfig().getString("dbUser"), instance
							.getConfig().getString("dbPassword"));
			final Connection dbCon = MySQL.openConnection();
			Statement dbStmt = dbCon.createStatement();
			ResultSet mcResult = dbStmt
					.executeQuery("SELECT * FROM rp_PlayerInfo;");

			Statement zStmt = dbCon.createStatement();

			if (!mcResult.isBeforeFirst()) {
				// No results
				// do nothing
				dbStmt.close();
				dbCon.close();

			} else {
				// results found!
				while (mcResult.next()) {
					if (mcResult.getInt("HedgeMazeCompletions") > 0) {
						zStmt.executeUpdate("INSERT INTO rp_RunicGameCompletions (UUID, GameID, Count) "
								+ "VALUES ('"
								+ mcResult.getString("UUID")
								+ "', 1, "
								+ mcResult.getInt("HedgeMazeCompletions")
								+ ");");
						count++;
					}

					if (mcResult.getInt("IceMazeCompletions") > 0) {
						zStmt.executeUpdate("INSERT INTO rp_RunicGameCompletions (UUID, GameID, Count) "
								+ "VALUES ('"
								+ mcResult.getString("UUID")
								+ "', 2, "
								+ mcResult.getInt("IceMazeCompletions") + ");");
						count++;
					}

					if (mcResult.getInt("XmasMazeCompletions") > 0) {
						zStmt.executeUpdate("INSERT INTO rp_RunicGameCompletions (UUID, GameID, Count) "
								+ "VALUES ('"
								+ mcResult.getString("UUID")
								+ "', 3, "
								+ mcResult.getInt("XmasMazeCompletions") + ");");
						count++;
					}

					if (mcResult.getInt("JungleMazeCompletions") > 0) {
						zStmt.executeUpdate("INSERT INTO rp_RunicGameCompletions (UUID, GameID, Count) "
								+ "VALUES ('"
								+ mcResult.getString("UUID")
								+ "', 4, "
								+ mcResult.getInt("JungleMazeCompletions")
								+ ");");
						count++;
					}

					if (mcResult.getInt("FrostMazeCompletions") > 0) {
						zStmt.executeUpdate("INSERT INTO rp_RunicGameCompletions (UUID, GameID, Count) "
								+ "VALUES ('"
								+ mcResult.getString("UUID")
								+ "', 5, "
								+ mcResult.getInt("FrostMazeCompletions")
								+ ");");
						count++;
					}

					if (mcResult.getInt("DungeonMazeCompletions") > 0) {
						zStmt.executeUpdate("INSERT INTO rp_RunicGameCompletions (UUID, GameID, Count) "
								+ "VALUES ('"
								+ mcResult.getString("UUID")
								+ "', 6, "
								+ mcResult.getInt("DungeonMazeCompletions")
								+ ");");
						count++;
					}

					if (mcResult.getInt("AdventureParkour") > 0) {
						zStmt.executeUpdate("INSERT INTO rp_RunicGameCompletions (UUID, GameID, Count) "
								+ "VALUES ('"
								+ mcResult.getString("UUID")
								+ "', 7, "
								+ mcResult.getInt("AdventureParkour") + ");");
						count++;
					}
				}

			}

			dbStmt.close();
			zStmt.close();
			dbCon.close();
		} catch (SQLException z) {
			Bukkit.getLogger()
					.log(Level.SEVERE,
							"Failed getting puzzle completion count -"
									+ z.getMessage());
		}

		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "sc Created " + count
				+ " new records");

	}

}
