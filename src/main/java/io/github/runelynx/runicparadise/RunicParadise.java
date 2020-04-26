package io.github.runelynx.runicparadise;

import com.gamingmesh.jobs.api.JobsPaymentEvent;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import io.github.runelynx.runicparadise.faith.FaithCore;
import io.github.runelynx.runicuniverse.RunicMessaging;
import io.github.runelynx.runicuniverse.RunicMessaging.RunicFormat;
import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import me.mrCookieSlime.Slimefun.Lists.RecipeType;
import me.mrCookieSlime.Slimefun.Lists.SlimefunItems;
import me.mrCookieSlime.Slimefun.Objects.Category;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem;
import me.mrCookieSlime.Slimefun.api.SlimefunItemStack;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.*;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.craftbukkit.v1_15_R1.metadata.EntityMetadataStore;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.raid.RaidFinishEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class RunicParadise extends JavaPlugin implements Listener, PluginMessageListener, SlimefunAddon {

	public static FaithCore faithSystem;
	private static RunicParadise instance;

	private static final Logger log = Bukkit.getLogger();
	public static Permission perms = null;
	public static Economy economy = null;
	public static final long PUZZLE_REPEAT_TIME = 518400000;

	public static HashMap<UUID, Faith> faithMap = new HashMap<>();
	public static HashMap<String, Integer> powerReqsMap = new HashMap<>();
	public static HashMap<String, String[]> faithSettingsMap = new HashMap<>();
	public static HashMap<String, ChatColor> rankColors = new HashMap<>();
	public static HashMap<UUID, String> protectedPlayers = new HashMap<>();
	public static HashMap<Location, String[]> runicEyes = new HashMap<>();
	public static HashMap<Entity, String> runicEyeEntities = new HashMap<>();
	public static HashMap<Location, String[]> prayerBooks = new HashMap<>();
	public static HashMap<Entity, String> prayerBookEntities = new HashMap<>();
	public static HashMap<String, String> newReadyPlayer = new HashMap<>();
	public static HashMap<Integer, Location> explorerLocations = new HashMap<>();
	public static HashMap<Integer, Integer> explorerDifficulties = new HashMap<>();
	public static HashMap<Integer, Integer> explorerRewards = new HashMap<>();
	public static LinkedHashMap<Location, Integer> explorerLocationsReversed = new LinkedHashMap<>();
	public static HashMap<Integer, String> explorerIDs = new HashMap<>();
	public static HashMap<Integer, Integer> explorerPrereqs = new HashMap<>();
	public static HashMap<EntityType, Integer> trackableEntityKillsMap = new HashMap<>();

	public static HashMap<UUID, HashMap<EntityType, Integer>> mobKillTracker = new HashMap<>();
	public static HashMap<UUID, RunicProfile> playerProfiles = new HashMap<>();

	public static Random randomSeed = new Random();


	Ranks ranks = new Ranks();

	public static RunicParadise getInstance() {
		return instance;
	}


	@Override
	public  JavaPlugin getJavaPlugin() {
		// This is a method that links your SlimefunAddon to your Plugin.
		// Just return "this" in this case, so they are linked
		return this;
	}

	@Override
	public String getBugTrackerURL() {
		// Here you can return a link to your Bug Tracker.
		// This link will be displayed to Server Owners if there is an issue
		// with this Addon. Return null if you have no bug tracker.
		// Normally you can just use GitHub's Issues tab:
		// https://github.com/YOURNAME/YOURPROJECT/issues
		return "https://github.com/runelynx/RunicParadiseOnline/issues";
	}

	@Override
	public void onPluginMessageReceived(String channel, Player player, byte[] message) {
		if (!channel.equals("BungeeCord")) {
			return;
		}
		ByteArrayDataInput in = ByteStreams.newDataInput(message);
		String subchannel = in.readUTF();
	}

	private void initializeRunicSystems() throws IOException, JSONException {
		this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
		Bukkit.getServer().getPluginManager().registerEvents(this, this);
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();

		RunicMessaging.initializeAnnouncements(instance);

		registerSlimefunItems();

		if (setupPermissions()) {
			getLogger().info("[RunicParadise] Hooked into Vault Permissions");
		}

		if (setupEconomy()) {
			getLogger().info("[RunicParadise] Hooked into Vault Economy");
		}

		getConfig().options().copyDefaults(true);
		saveConfig();

		if (Borderlands.initializeBorderlands()) {
			getLogger().info("[RunicParadise] Initialized borderlands");
		}

		if (controlKeepInventory()) {
			getLogger().info("[RunicParadise] Updated KeepInventory settings");
		}

		if (defineEntityTracking()) {
			getLogger().info("[RunicParadise] Defined entity kill tracking rules");
		}

		if (Recipes.customRecipes()) {
			getLogger().info("[RunicParadise] Created custom recipes");
		}

		defineRankColors();
		getLogger().info("[RunicParadise] Defined default rank colors");

		if (startupRunicFaith(scheduler)) {
			getLogger().info("[RunicParadise] Started up Runic Faith");
		}

		if (registerCommands()) {
			getLogger().info("[RunicParadise] Commands registered");
		}

		int loadedProfiles = loadPlayerProfiles();
		if (loadedProfiles >= 0) {
			getLogger().info("[RunicParadise] Loaded " + loadedProfiles + " player profiles");
		}

		if (testDatabaseConnection()) {
			getLogger().info("[RunicParadise] Database connection successful");
		}

		if (Commands.syncExplorerLocations()) {
			getLogger().info("[RunicParadise] Explorer locations are now synchronized");
		}
	}

	private void scheduleFaithTasks(BukkitScheduler scheduler) {
		// Check for spirit of wolf spellcast every 3 minutes
//		scheduler.runTaskTimer(this, () -> {
//			RunicParadise.loadRunicEyes();
//			RunicParadise.loadPrayerBooks();
//		}, 0L, 3600L);

		scheduler.runTaskTimer(this, () -> {
			for (Player p : Bukkit.getWorld("RunicKingdom_nether").getPlayers()) {
				if (faithMap.get(p.getUniqueId()).checkEquippedFaithLevel("Nether",
						RunicParadise.powerReqsMap.get("Netherborn"))) {
					faithMap.get(p.getUniqueId()).castNether_Netherborn(p);
				}
			}
		}, 0L, Faith.NETHER_NETHERBORN_TIMING);
	}

	private void defineRankColors() {
		rankColors.put("ghost", ChatColor.GRAY);
		rankColors.put("seeker", ChatColor.GREEN);
		rankColors.put("runner", ChatColor.DARK_GREEN);
		rankColors.put("singer", ChatColor.YELLOW);
		rankColors.put("brawler", ChatColor.GOLD);
		rankColors.put("keeper", ChatColor.AQUA);
		rankColors.put("guard", ChatColor.DARK_AQUA);
		rankColors.put("hunter", ChatColor.BLUE);
		rankColors.put("slayer", ChatColor.LIGHT_PURPLE);
		rankColors.put("warder", ChatColor.LIGHT_PURPLE);
		rankColors.put("champion", ChatColor.DARK_PURPLE);
		rankColors.put("master", ChatColor.RED);
		rankColors.put("duke", ChatColor.WHITE);
		rankColors.put("baron", ChatColor.WHITE);
		rankColors.put("count", ChatColor.WHITE);
		rankColors.put("lord", ChatColor.WHITE);
		rankColors.put("god", ChatColor.DARK_BLUE);
	}

	private boolean controlKeepInventory() {
		String tempInvSetting = "gamerule keepInventory " + instance.getConfig().getString("keepInventoryOnDeathEnabled");
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), tempInvSetting);
		tempInvSetting = "mvrule keepInventory true RunicSky";
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), tempInvSetting);
//		tempInvSetting = "mvrule keepInventory true RunicKingdom";
//		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), tempInvSetting);
//		tempInvSetting = "mvrule keepInventory true Mining";
//		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), tempInvSetting);
//		tempInvSetting = "mvrule keepInventory true RunicKingdom_nether";
//		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), tempInvSetting);
//		tempInvSetting = "mvrule keepInventory true RunicKingdom_the_end";
//		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), tempInvSetting);
		return true;
	}

	private boolean defineEntityTracking() {
		// Register entity types to track kills for player profile data. 0=dont
		// track, 1=do track hostile, 2=do track passive
		// If you update these, be sure to also update the database retrieval in
		// RunicProfile.mobKill...
		trackableEntityKillsMap.put(EntityType.BAT, 2);
		trackableEntityKillsMap.put(EntityType.BLAZE, 1);
		trackableEntityKillsMap.put(EntityType.CAVE_SPIDER, 1);
		trackableEntityKillsMap.put(EntityType.CHICKEN, 2);
		trackableEntityKillsMap.put(EntityType.COW, 2);
		trackableEntityKillsMap.put(EntityType.CREEPER, 1);
		trackableEntityKillsMap.put(EntityType.DONKEY, 2);
		trackableEntityKillsMap.put(EntityType.ELDER_GUARDIAN, 1);
		trackableEntityKillsMap.put(EntityType.ENDER_DRAGON, 1);
		trackableEntityKillsMap.put(EntityType.ENDERMAN, 1);
		trackableEntityKillsMap.put(EntityType.ENDERMITE, 1);
		trackableEntityKillsMap.put(EntityType.EVOKER, 1);
		trackableEntityKillsMap.put(EntityType.GHAST, 1);
		trackableEntityKillsMap.put(EntityType.GIANT, 1);
		trackableEntityKillsMap.put(EntityType.GUARDIAN, 1);
		trackableEntityKillsMap.put(EntityType.HORSE, 2);
		trackableEntityKillsMap.put(EntityType.HUSK, 1);
		trackableEntityKillsMap.put(EntityType.IRON_GOLEM, 1);
		trackableEntityKillsMap.put(EntityType.MAGMA_CUBE, 1);
		trackableEntityKillsMap.put(EntityType.LLAMA, 2);
		trackableEntityKillsMap.put(EntityType.MULE, 2);
		trackableEntityKillsMap.put(EntityType.MUSHROOM_COW, 2);
		trackableEntityKillsMap.put(EntityType.OCELOT, 2);
		trackableEntityKillsMap.put(EntityType.PIG, 2);
		trackableEntityKillsMap.put(EntityType.PIG_ZOMBIE, 1);
		trackableEntityKillsMap.put(EntityType.WITCH, 1);
		trackableEntityKillsMap.put(EntityType.WITHER, 1);
		trackableEntityKillsMap.put(EntityType.WITHER_SKELETON, 1);
		trackableEntityKillsMap.put(EntityType.WOLF, 2);
		trackableEntityKillsMap.put(EntityType.ZOMBIE, 1);
		trackableEntityKillsMap.put(EntityType.ZOMBIE_HORSE, 2);
		trackableEntityKillsMap.put(EntityType.ZOMBIE_VILLAGER, 1);
		trackableEntityKillsMap.put(EntityType.VEX, 1);
		trackableEntityKillsMap.put(EntityType.VILLAGER, 2);
		trackableEntityKillsMap.put(EntityType.VINDICATOR, 1);
		trackableEntityKillsMap.put(EntityType.RABBIT, 2);
		trackableEntityKillsMap.put(EntityType.SHEEP, 2);
		trackableEntityKillsMap.put(EntityType.SHULKER, 1);
		trackableEntityKillsMap.put(EntityType.SILVERFISH, 1);
		trackableEntityKillsMap.put(EntityType.SKELETON, 1);
		trackableEntityKillsMap.put(EntityType.SKELETON_HORSE, 2);
		trackableEntityKillsMap.put(EntityType.SQUID, 2);
		trackableEntityKillsMap.put(EntityType.STRAY, 1);
		trackableEntityKillsMap.put(EntityType.POLAR_BEAR, 1);
		trackableEntityKillsMap.put(EntityType.SLIME, 1);
		trackableEntityKillsMap.put(EntityType.SNOWMAN, 2);
		trackableEntityKillsMap.put(EntityType.SPIDER, 1);

		trackableEntityKillsMap.put(EntityType.PLAYER, 0);

		trackableEntityKillsMap.put(EntityType.AREA_EFFECT_CLOUD, 0);
		trackableEntityKillsMap.put(EntityType.ARMOR_STAND, 0);
		trackableEntityKillsMap.put(EntityType.ARROW, 0);
		trackableEntityKillsMap.put(EntityType.BOAT, 0);
		trackableEntityKillsMap.put(EntityType.DRAGON_FIREBALL, 0);
		trackableEntityKillsMap.put(EntityType.DROPPED_ITEM, 0);
		trackableEntityKillsMap.put(EntityType.EGG, 0);
		trackableEntityKillsMap.put(EntityType.ENDER_CRYSTAL, 0);
		trackableEntityKillsMap.put(EntityType.ENDER_PEARL, 0);
		trackableEntityKillsMap.put(EntityType.ENDER_SIGNAL, 0);
		trackableEntityKillsMap.put(EntityType.EVOKER_FANGS, 0);
		trackableEntityKillsMap.put(EntityType.EXPERIENCE_ORB, 0);
		trackableEntityKillsMap.put(EntityType.FALLING_BLOCK, 0);
		trackableEntityKillsMap.put(EntityType.FIREBALL, 0);
		trackableEntityKillsMap.put(EntityType.FIREWORK, 0);
		trackableEntityKillsMap.put(EntityType.FISHING_HOOK, 0);
		trackableEntityKillsMap.put(EntityType.ITEM_FRAME, 0);
		trackableEntityKillsMap.put(EntityType.LEASH_HITCH, 0);
		trackableEntityKillsMap.put(EntityType.LIGHTNING, 0);
		trackableEntityKillsMap.put(EntityType.LLAMA_SPIT, 0);
		trackableEntityKillsMap.put(EntityType.MINECART, 0);
		trackableEntityKillsMap.put(EntityType.MINECART_CHEST, 0);
		trackableEntityKillsMap.put(EntityType.MINECART_COMMAND, 0);
		trackableEntityKillsMap.put(EntityType.MINECART_FURNACE, 0);
		trackableEntityKillsMap.put(EntityType.MINECART_HOPPER, 0);
		trackableEntityKillsMap.put(EntityType.MINECART_MOB_SPAWNER, 0);
		trackableEntityKillsMap.put(EntityType.MINECART_TNT, 0);
		trackableEntityKillsMap.put(EntityType.PAINTING, 0);
		trackableEntityKillsMap.put(EntityType.PRIMED_TNT, 0);
		trackableEntityKillsMap.put(EntityType.SHULKER_BULLET, 0);
		trackableEntityKillsMap.put(EntityType.SMALL_FIREBALL, 0);
		trackableEntityKillsMap.put(EntityType.SNOWBALL, 0);
		trackableEntityKillsMap.put(EntityType.SPECTRAL_ARROW, 0);
		trackableEntityKillsMap.put(EntityType.SPLASH_POTION, 0);
		trackableEntityKillsMap.put(EntityType.THROWN_EXP_BOTTLE, 0);
		trackableEntityKillsMap.put(EntityType.UNKNOWN, 0);
		trackableEntityKillsMap.put(EntityType.WITHER_SKULL, 0);
		return true;
	}

	private boolean startupRunicFaith(BukkitScheduler scheduler) {
		for (Player p : Bukkit.getOnlinePlayers()) {
			// Load RunicFaith for players currently online
			faithMap.put(p.getUniqueId(), new Faith(p.getUniqueId()));
			RunicMessaging.sendMessage(p, RunicMessaging.RunicFormat.FAITH, ChatColor.BLUE + "Faith system activated!");
		}
		Faith.getFaithSettings();
		Faith.getPowerSettings();

		scheduleFaithTasks(scheduler);

		return true;
	}

	private int loadPlayerProfiles() {
		Collection<? extends Player> players = Bukkit.getOnlinePlayers();
		// Load RunicProfile
		players.stream()
				.map(Entity::getUniqueId)
				.filter(uuid -> !playerProfiles.containsKey(uuid))
				.forEach(uuid -> playerProfiles.put(uuid, new RunicProfile(uuid)));
		return players.size();
	}

	private boolean testDatabaseConnection() {
		try {
			MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
			return true;
		} catch (Exception b) {
			getLogger().warning("[RunicParadise] FAILED TO CONNECT TO DATABASE!!!");
			return false;
		}
	}

	private boolean registerCommands() throws IOException, JSONException {
		//TODO Move this to its own method and link to init method
		// This will throw a NullPointerException if you don't have the command
		// defined in your plugin.yml file!
		getCommand("butcherbl").setExecutor(new Commands());
		getCommand("rpcrates").setExecutor(new Commands());
		getCommand("rprewards").setExecutor(new Commands());
		getCommand("claim").setExecutor(new Commands());
		getCommand("raffle").setExecutor(new Commands());
		getCommand("search").setExecutor(new Commands());
		getCommand("rpfix").setExecutor(new Commands());
		getCommand("explore").setExecutor(new Commands());
		getCommand("machinemaze").setExecutor(new Commands());
		getCommand("el").setExecutor(new Commands());
		getCommand("pex").setExecutor(new Commands());
		getCommand("fixranks").setExecutor(new Commands());
		getCommand("soul").setExecutor(new Commands());
		getCommand("souls").setExecutor(new Commands());
		getCommand("freezemob").setExecutor(new Commands());
		getCommand("unfreezemob").setExecutor(new Commands());
		getCommand("faithitems").setExecutor(new Commands());
		getCommand("runiceye").setExecutor(new Commands());
		getCommand("holotest").setExecutor(new HologramCommand(getDataFolder(), getLogger()));
		getCommand("runicspawntravel").setExecutor(new Commands());
		getCommand("casino").setExecutor(new Commands());
		getCommand("rp").setExecutor(new Commands());
		getCommand("rptest").setExecutor(new Commands());
		getCommand("rankitem").setExecutor(new Commands());
		getCommand("consoleseeker").setExecutor(new Commands());
		getCommand("miningreset").setExecutor(new Commands());
		getCommand("miningworldreminder").setExecutor(new Commands());
		getCommand("rpreload").setExecutor(new Commands());
		getCommand("faithweapons").setExecutor(new Commands());
		getCommand("faithweapon").setExecutor(new Commands());
		getCommand("headofplayer").setExecutor(new Commands());
		getCommand("iteminfo").setExecutor(new Commands());
		getCommand("face").setExecutor(new Commands());
		getCommand("crocomaze").setExecutor(new Commands());
		getCommand("miningworld").setExecutor(new Commands());
		getCommand("mw").setExecutor(new Commands());
		getCommand("wild").setExecutor(new Commands());
		getCommand("rpgames").setExecutor(new Commands());
		getCommand("games").setExecutor(new Commands());
		getCommand("adventureparkourprize").setExecutor(new Commands());
		getCommand("sc").setExecutor(new Commands());
		getCommand("staffchat").setExecutor(new Commands());
		getCommand("settler").setExecutor(new Commands());
		getCommand("seeker").setExecutor(new Commands());
		getCommand("junglemaze").setExecutor(new Commands());
		getCommand("staff").setExecutor(new Commands());
		getCommand("music").setExecutor(new Commands());
		getCommand("radio").setExecutor(new Commands());
		getCommand("ready").setExecutor(new Commands());
		getCommand("rptokens").setExecutor(new Commands());
		getCommand("dailykarma").setExecutor(new Commands());
		getCommand("say").setExecutor(new Commands());
		getCommand("rpvote").setExecutor(new Commands());
		getCommand("rpjobs").setExecutor(new Commands());
		getCommand("rpeffects").setExecutor(new Commands());
		getCommand("punish").setExecutor(new Commands());
		getCommand("powers").setExecutor(new Commands());
		getCommand("faith").setExecutor(new Commands());
		getCommand("cactifever").setExecutor(new Commands());
		getCommand("voice").setExecutor(new Commands());
		getCommand("discord").setExecutor(new Commands());
		getCommand("rpversion").setExecutor(new Commands());
		getCommand("getrunestones").setExecutor(new Commands());
		return true;
	}



	public void onEnable() {
		instance = this;
		getLogger().info("[RunicParadise] Enabling plugin...");
		for (Player p : Bukkit.getOnlinePlayers()) {
			// Messaging
			RunicMessaging.sendMessage(p, RunicMessaging.RunicFormat.SYSTEM,
					"RunicParadise plugin is " + ChatColor.DARK_GREEN + "starting up" + ChatColor.GRAY + "...");
		}

		try {
			initializeRunicSystems();
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Exception happened", e);
		}

		faithSystem = new FaithCore();
		new Raffle();



		//TODO Move this to its own method and link to init method. Maybe just kill it for RP 5.0.
		/*
		//This stops players from gliding in the RP4.0 big spawn
		scheduler.runTaskTimerAsynchronously(this, new Runnable() {

			@Override
			public void run() {

				for (Player p : Bukkit.getWorld("RunicSky").getPlayers()) {
					if (p.isGliding()) {
						p.setGliding(false);
						p.setVelocity(new Vector(0, -2, 0));
						p.teleport(new Location(p.getWorld(), -438.476, 104.7000, 385.464, 8.159058F, 89.7F));

					}
				}

			}
		}, 0L, 70L);  */
	}

	public void onDisable() {
		// TODO Insert logic to be performed when the plugin is disabled
		RunicMessaging.cancelRepeatingTask();
		Faith.deactivateFaiths();

		faithSystem.shutdownFaithSystem();

		rankColors.clear();
		// Dispose of the EffectManager
		for (Player p : Bukkit.getOnlinePlayers()) {

			RunicMessaging.sendMessage(p, RunicMessaging.RunicFormat.SYSTEM,
					"RunicParadise plugin is " + ChatColor.DARK_RED + "shutting down" + ChatColor.GRAY + "...");

			// Unload RunicProfile

			playerProfiles.get(p.getUniqueId()).saveMobKillsForPlayer();
			playerProfiles.remove(p.getUniqueId());

		}

		getLogger().info("RunicParadise Plugin: onDisable has been invoked!");

		// em.dispose();
	}

	@EventHandler
	public void JobsPaymentEvent(JobsPaymentEvent event) {
		//TODO Log Jobs payments to a DB so we can report on it

	}

	@EventHandler
	public void onEnchantItemEvent(EnchantItemEvent event) {
		ItemStack itemToEnchant = event.getItem();
		ItemMeta meta = itemToEnchant.getItemMeta();
		PersistentDataContainer container = meta.getPersistentDataContainer();

		if(container.has(FaithCore.faithCoreItemDataKeys.get("FaithWeapon"), PersistentDataType.INTEGER)) {
			RunicMessaging.sendMessage(event.getEnchanter(), RunicFormat.FAITH, "You cannot enchant Faith-imbued weapons!");
			event.setCancelled(true);
		}
	}



	@EventHandler
	public void onEntityShootBow(EntityShootBowEvent event) {
		if (event.getEntityType() == EntityType.SKELETON) {
			Borderlands.processSkeletonArrows(event);
		}

		if (event.getEntityType() == EntityType.PLAYER) {
			Player player = (Player) event.getEntity();
			ItemStack bow = player.getEquipment().getItemInMainHand();

			ItemMeta itemMeta = bow.getItemMeta();
			PersistentDataContainer container = itemMeta.getPersistentDataContainer();

			if(container.has(FaithCore.faithCoreItemDataKeys.get("FaithWeapon"), PersistentDataType.INTEGER)) {
				// This is a Faith bow!
				if (event.getProjectile().getType() == EntityType.SPECTRAL_ARROW) {
					// Player is shooting a spectral arrow with their faith bow - good!
				} else {
					// Player not using a spectral arrow with their faith bow - bad!
					event.setCancelled(true);
					RunicMessaging.sendMessage(player, RunicFormat.FAITH, "You can only shoot spectral arrows with a Faith-imbued bow");
				}
			}
		}

	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onCreatureSpawn(CreatureSpawnEvent event) {

		if (event.getSpawnReason() != SpawnReason.SPAWNER) {
			Entity e = event.getEntity();
			NamespacedKey key = new NamespacedKey(this, "NotFromSpawner");
			e.getPersistentDataContainer().set(key, PersistentDataType.STRING, "No");

			// If there's a raffle running, chance to spawn a lucky creeper
			if (Boolean.parseBoolean(Raffle.raffleSettingsMap.get("Enabled"))) {
				if (event.getEntity().getType() == EntityType.CREEPER) {
					Random rand = new Random();
					int randomNum = rand.nextInt((100 - 1) + 1) + 1;
					if (randomNum <= 5) {

						event.getEntity().setCustomName("✿ Lucky Creeper ✿");
						event.getEntity().setCustomNameVisible(true);
						((Creeper) event.getEntity()).setPowered(true);
					}
				}
			}
		}

		if ((event.getLocation().getX() > 7500 || event.getLocation().getX() < -7500 || event.getLocation().getZ() > 7500
				|| event.getLocation().getZ() < -7500)
		&& event.getLocation().getWorld().getName().equalsIgnoreCase("RunicKingdom")) {
			// Confirmed spawn is in the borderlands!!

			if (!event.getSpawnReason().equals(SpawnReason.SPAWNER)) {
				Borderlands.spawnBLMob(event);
				event.setCancelled(false);
			}

		} else {
			//Not in the borderlands, could be in any world

			//Force spawns to work in RunicKingdom
			if (event.getLocation().getWorld().getName().equalsIgnoreCase("RunicKingdom")) {
				event.setCancelled(false);
			}

			//Cancel Phantom spawns that aren't in the borderlands
			if(event.getEntity().getType().toString().equalsIgnoreCase("phantom")) {
				event.setCancelled(true);
			}

			//Force spawns to work in RunicSky
			if (event.getLocation().getWorld().getName().equalsIgnoreCase("RunicSky")) {

				RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
				RegionQuery query = container.createQuery();
				ApplicableRegionSet ars = query.getApplicableRegions(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(event.getLocation()));

				if (ars.queryState(null, Flags.MOB_SPAWNING) == StateFlag.State.DENY) {
					event.setCancelled(true);
				} else {
					event.setCancelled(false);
				}

			}




			//			Bukkit.getLogger().log(Level.INFO, "MobSpawnLog: " + event.getLocation() + " " + event.getEntity().getType().toString() + " " + event.isCancelled());

		}
	}

	@EventHandler
	public void onPrepareItemCraftEvent(PrepareItemCraftEvent event) {
		ItemStack result = Recipes.customFoodRecipesNew(event.getInventory());
		if (result != null) {
			event.getInventory().setResult(result);
		}

		if (event.getRecipe() != null && event.getRecipe().getResult() !=null && event.getRecipe().getResult().getItemMeta() != null) {
			//meta.getPersistentDataContainer().set(FaithCore.faithCoreItemDataKeys.get("KarmaRequiredToCraft"), PersistentDataType.INTEGER, karmaRequired);
			ItemMeta meta = event.getRecipe().getResult().getItemMeta();
			PersistentDataContainer container = meta.getPersistentDataContainer();

			if (container.has(FaithCore.faithCoreItemDataKeys.get("KarmaRequiredToCraft"), PersistentDataType.INTEGER)) {
				//This is a Faith weapon if we've made it this far
				int karmaRequired = container.get(FaithCore.faithCoreItemDataKeys.get("KarmaRequiredToCraft"), PersistentDataType.INTEGER);

				if (karmaRequired > 0) {
					//This is a Faith weapon that requires at least 1 karma to craft
					for (HumanEntity human : event.getViewers()) {
						if (human instanceof Player) {
							Player player = (Player) human;
							if (playerProfiles.get(player.getUniqueId()).getKarmaBalance() >= karmaRequired) {
								RunicMessaging.sendMessage(player, RunicFormat.FAITH, "This weapon will cost " + karmaRequired + " to craft!");
							} else {
								RunicMessaging.sendMessage(player, RunicFormat.FAITH, "This weapon costs " + karmaRequired + " to craft and you don't have enough.");
							}
						}
					}
				}

			}
		}

	}

	@EventHandler
	public void onCraftItemEvent(CraftItemEvent event) {
		//meta.getPersistentDataContainer().set(FaithCore.faithCoreItemDataKeys.get("KarmaRequiredToCraft"), PersistentDataType.INTEGER, karmaRequired);
		ItemMeta meta = event.getRecipe().getResult().getItemMeta();
		PersistentDataContainer container = meta.getPersistentDataContainer();


		if (event.getRecipe() != null && event.getRecipe().getResult() !=null && event.getRecipe().getResult().getItemMeta() != null) {
			if (container.has(FaithCore.faithCoreItemDataKeys.get("KarmaRequiredToCraft"), PersistentDataType.INTEGER)) {
				//This is a Faith weapon if we've made it this far
				int karmaRequired = container.get(FaithCore.faithCoreItemDataKeys.get("KarmaRequiredToCraft"), PersistentDataType.INTEGER);

				if (karmaRequired > 0) {
					//This is a Faith weapon that requires at least 1 karma to craft
					for (HumanEntity human : event.getViewers()) {
						if (human instanceof Player) {
							Player player = (Player) human;
							if (playerProfiles.get(player.getUniqueId()).getKarmaBalance() >= karmaRequired) {
								//RunicMessaging.sendMessage(player, RunicFormat.FAITH, "You spent " + karmaRequired + " karma to craft this weapon");
								playerProfiles.get(player.getUniqueId()).reduceCurrency("Karma", karmaRequired);
							} else {
								RunicMessaging.sendMessage(player, RunicFormat.FAITH, "You can't craft this weapon until you have at least " + karmaRequired + " karma");
								event.setCancelled(true);
							}
						}
					}
				}

			}
		}
	}

	private static void putGlassOnInventory(Inventory inventory, ItemStack main) {
		ItemStack slot1 = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
		ItemMeta meta = slot1.getItemMeta();
		meta.setDisplayName(" ");
		slot1.setItemMeta(meta);
		ItemStack slot2 = new ItemStack(Material.MAGENTA_STAINED_GLASS_PANE);
		meta = slot2.getItemMeta();
		meta.setDisplayName(" ");
		slot2.setItemMeta(meta);
		ItemStack slot3 = new ItemStack(Material.PINK_STAINED_GLASS_PANE);
		meta = slot3.getItemMeta();
		meta.setDisplayName(" ");
		slot3.setItemMeta(meta);
		ItemStack slot4 = new ItemStack(Material.RED_STAINED_GLASS_PANE);
		meta = slot4.getItemMeta();
		meta.setDisplayName(" ");
		slot4.setItemMeta(meta);
		ItemStack slot6 = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
		meta = slot6.getItemMeta();
		meta.setDisplayName(" ");
		slot6.setItemMeta(meta);
		ItemStack slot7 = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
		meta = slot7.getItemMeta();
		meta.setDisplayName(" ");
		slot7.setItemMeta(meta);
		ItemStack slot8 = new ItemStack(Material.CYAN_STAINED_GLASS_PANE);
		meta = slot8.getItemMeta();
		meta.setDisplayName(" ");
		slot8.setItemMeta(meta);
		ItemStack slot9 = new ItemStack(Material.BLUE_STAINED_GLASS_PANE);
		meta = slot9.getItemMeta();
		meta.setDisplayName(" ");
		slot9.setItemMeta(meta);

		inventory.setItem(0, slot1);
		inventory.setItem(1, slot2);
		inventory.setItem(2, slot3);
		inventory.setItem(3, slot4);
		inventory.setItem(4, main);
		inventory.setItem(5, slot6);
		inventory.setItem(6, slot7);
		inventory.setItem(7, slot8);
		inventory.setItem(8, slot9);
	}

	// valid puzzleTypes = M / P
	private static void showRunicCarnivalMenu_Puzzles(Player p, Character puzzleType) {
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
				ChatColor.DARK_RED + "" + ChatColor.BOLD + "Runic Carnival - " + typePlural);

		ItemMeta meta;
		ArrayList<String> mainLore = new ArrayList<>();
		for (int i = 0; i < 4; ++i) {
			mainLore.add(ChatColor.YELLOW + "x");
		}

		ItemStack main = new ItemStack(Material.BEACON, 1);
		meta = main.getItemMeta();
		meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + typeSingle + " Menu");
		meta.setLore(mainLore);
		main.setItemMeta(meta);

		putGlassOnInventory(puzzleMenu, main);

		ItemStack wings;
		wings = new ItemStack(Material.ELYTRA, 1);
		meta = wings.getItemMeta();
		meta.setDisplayName("Warp to Puzzle Kiosk");
		wings.setItemMeta(meta);

		puzzleMenu.setItem(8, wings);

		int mazeSlot = 9;

		try {
			MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
			Connection connection = MySQL.openConnection();
			Statement statement = connection.createStatement();
			ResultSet menuResult = statement
					.executeQuery("SELECT * FROM rp_RunicGames WHERE GameType='" + typeSingle + "';");
			int playerMazeCount = 0;
			long playerLastCompletion = 0;
			SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d yyyy, HH:mm z", Locale.US);

			if (!menuResult.isBeforeFirst()) {
				// No results
				// do nothing
				Bukkit.getLogger().log(Level.INFO,
						"Failed to display carnival menu for puzzles. Found no entries in the database.");
				connection.close();
			} else {
				// results found!
				while (menuResult.next()) {
					playerMazeCount = getPlayerMazeCompletionCount(p, menuResult.getInt("ID"));

					ItemStack mazeIcon = new ItemStack(Material.EMERALD_BLOCK, 1);

					if (playerMazeCount == 0) {
						mazeIcon = new ItemStack(Material.REDSTONE_BLOCK, 1);
					} else {
						playerLastCompletion = getPlayerMazeLastCompletion(p, menuResult.getInt("ID"));

					}

					meta = mazeIcon.getItemMeta();
					meta.setDisplayName(ChatColor.RESET + "" + ChatColor.BLUE + menuResult.getString("GameName"));

					ArrayList<String> mazeLore = new ArrayList<String>();
					mazeLore.add(ChatColor.GREEN + "Times Completed: " + ChatColor.YELLOW + playerMazeCount);
					mazeLore.add(null);
					if (playerMazeCount == 0) {
						mazeLore.add(ChatColor.GREEN + "First-Time Rewards:");
						mazeLore.add(ChatColor.GRAY + "Karma: " + ChatColor.YELLOW + menuResult.getInt("KarmaReward"));
						mazeLore.add(ChatColor.GRAY + "Tokens: " + ChatColor.YELLOW + menuResult.getInt("TokenReward"));
						mazeLore.add(ChatColor.GRAY + "Souls: " + ChatColor.YELLOW + menuResult.getInt("SoulReward"));
						mazeLore.add(ChatColor.GRAY + "Runics: " + ChatColor.YELLOW + menuResult.getInt("CashReward"));
						// 6 days in ms
						// true if >6 days have passed since last completion
					} else if (new Date().getTime() - playerLastCompletion > PUZZLE_REPEAT_TIME
							&& menuResult.getInt("ID") != 4) {
						mazeLore.add(ChatColor.GREEN + "Weekly Rewards:");
						mazeLore.add(ChatColor.GRAY + "Karma: " + ChatColor.YELLOW
								+ (menuResult.getInt("KarmaReward") / 2));
						mazeLore.add(ChatColor.GRAY + "Tokens: " + ChatColor.YELLOW
								+ (menuResult.getInt("TokenReward") / 2));
						mazeLore.add(ChatColor.GRAY + "Souls: " + ChatColor.YELLOW
								+ (menuResult.getInt("SoulReward") / 2));
						mazeLore.add(ChatColor.GRAY + "Runics: " + ChatColor.YELLOW
								+ (menuResult.getInt("CashReward") * menuResult.getDouble("CashRepeatAdjust")));
						mazeLore.add(null);
						mazeLore.add(ChatColor.GREEN + "Last Completion:");
						mazeLore.add(ChatColor.GRAY + sdf.format(new Date(playerLastCompletion)));

						// for dungeon maze / adv parkour - only reward player
						// once due to
						// nature of maze
					} else if (menuResult.getInt("ID") == 4) {
						mazeLore.add(ChatColor.RED + "You only receive rewards for");
						mazeLore.add(ChatColor.RED + "completing this puzzle once!");

						// true if <6 days have passed since last completion
						// i.e. NO prize yet!
					} else {
						mazeLore.add(ChatColor.RED + "You only receive rewards for");
						mazeLore.add(ChatColor.RED + "completing a puzzle once per");
						mazeLore.add(ChatColor.RED + "week!");

						mazeLore.add(null);
						mazeLore.add(ChatColor.RED + "Last Completion:");
						mazeLore.add(ChatColor.GRAY + sdf.format(new Date(playerLastCompletion)));
						// true if <6 days have passed since last completion
						// i.e. NO prize yet!
					}

					// if its spawn maze or spawn parkour ...
					if (menuResult.getInt("ID") == 8 || menuResult.getInt("ID") == 8) {
						mazeLore.add(null);
						mazeLore.add(ChatColor.YELLOW + "Want to try this puzzle? You'll");
						mazeLore.add(ChatColor.YELLOW + "have to find the entrance!");
					} else {
						// its not those, so give the regular advice...
						mazeLore.add(null);
						mazeLore.add(ChatColor.YELLOW + "Want to try this puzzle? Click");
						mazeLore.add(ChatColor.YELLOW + "the Elytra icon to go to the portals!");
					}

					meta.setLore(mazeLore);
					mazeIcon.setItemMeta(meta);
					puzzleMenu.setItem(mazeSlot, mazeIcon);

					/*if (mazeSlot == 25) {
						mazeSlot = 28;
					} else if (mazeSlot == 34) {
						mazeSlot = 37;
					} else {
						mazeSlot++;
					}*/
					mazeSlot++;
				}
				statement.close();
				connection.close();
			}

		} catch (SQLException z) {
			Bukkit.getLogger().log(Level.SEVERE, "Failed displaying runic carnival puzzle menu -" + z.getMessage());
		}

		p.openInventory(puzzleMenu);
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

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void toggleGlideEvent(EntityToggleGlideEvent event) {
		if (event.getEntity().getLocation().getWorld().getName().equalsIgnoreCase("RunicSky") &&
				event.getEntityType().equals(EntityType.PLAYER)) {
			event.setCancelled(true);

			RunicMessaging.sendMessage(((Player)event.getEntity()), RunicFormat.SYSTEM, "Gliding is not permitted here.");
		}


	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerChat(AsyncPlayerChatEvent event) {
		String playerRankColor;

		if (RunicParadise.playerProfiles.get(event.getPlayer().getUniqueId()).getChatColor() == null) {
			playerRankColor = ChatColor.GRAY + "";
		} else {
			playerRankColor = RunicParadise.playerProfiles.get(event.getPlayer().getUniqueId()).getChatColor();
		}

		String staffPrefix = ChatColor.DARK_GRAY + "" + ChatColor.ITALIC + "SV ";
		String donatorPrefix = "";
		String newbiePrefix = "";
		Player player = event.getPlayer();

		// HANDLE DONATORS
		if (player.hasPermission("rp.donator.diamond")) {
			donatorPrefix = ChatColor.AQUA + "✩";
		} else if (player.hasPermission("rp.donator.emerald")) {
			donatorPrefix = ChatColor.GREEN + "✩";
		} else if (player.hasPermission("rp.donator.iron")) {
			donatorPrefix = ChatColor.GRAY + "✩";
		} else if (player.hasPermission("rp.donator.gold")) {
			donatorPrefix = ChatColor.GOLD + "✩";
		}

		// HANDLE NEWBIES
		if (((new Date().getTime() - event.getPlayer().getFirstPlayed()) / 86400000f) < 7.0) {
			newbiePrefix = ChatColor.DARK_GREEN + "✦";
		}

		// //// HANDLE STAFF
		if (player.hasPermission("rp.staff")) {
			staffPrefix += ChatColor.DARK_RED;
			if (player.hasPermission("rp.staff.admin")) {
				staffPrefix += "<Admin> ";
			} else if (player.hasPermission("rp.staff.mod+")) {
				staffPrefix += "<Mod+> ";
			} else if (player.hasPermission("rp.staff.mod")) {
				staffPrefix += "<Mod> ";
			} else if (player.hasPermission("rp.staff.director")) {
				staffPrefix += "<Director> ";
			} else if (player.hasPermission("rp.staff.architect")) {
				staffPrefix += "<Architect> ";
			} else if (player.hasPermission("rp.staff.enforcer")) {
				staffPrefix += "<Enforcer> ";
			} else if (player.hasPermission("rp.staff.helper")) {
				staffPrefix += "<Helper> ";
			}

			// //// HANDLE LEGENDS
		} else if (player.hasPermission("rp.guardian")) {
			staffPrefix += ChatColor.BLUE + "<Legend> ";
		} else if (player.hasPermission("rp.pirate")) {
			staffPrefix += ChatColor.BLUE + "<Pirate> ";
		} else if (player.hasPermission("rp.guide")) {
			staffPrefix += ChatColor.DARK_GREEN + "<Guide> ";
		}

		// //// HANDLE FAITHS FOR ALL BUT GHOSTS
		String faithPrefix = "";

		if (!player.hasPermission("rp.ghost")) {
			String currentFaith = RunicParadise.faithMap.get(event.getPlayer().getUniqueId()).getPrimaryFaith();
			if (RunicParadise.faithMap.get(event.getPlayer().getUniqueId()).checkEquippedFaithLevel(currentFaith,
					Integer.parseInt(RunicParadise.faithSettingsMap.get(currentFaith)[4]))) {

				faithPrefix = RunicParadise.faithSettingsMap.get(currentFaith)[6];
			} else {
				faithPrefix = RunicParadise.faithSettingsMap.get(currentFaith)[1];
			}
		}

		// //// HANDLE LORDS
		if (perms.getPrimaryGroup(player).equalsIgnoreCase("Lord")) {
			String genderRank;
			if (playerProfiles.get(event.getPlayer().getUniqueId()).getGender() == 'M') {
				genderRank = "Lord";
			} else {
				genderRank = "Lady";
			}

			event.setFormat(staffPrefix + donatorPrefix + playerRankColor + faithPrefix + playerRankColor + genderRank.toLowerCase() +
					" " + playerRankColor + event.getPlayer().getDisplayName() + ChatColor.WHITE + ": %2$s");
		} else
		// //// HANDLE COUNTS
		if (perms.getPrimaryGroup(event.getPlayer()).equalsIgnoreCase("Count")) {
			String genderRank;
			if (playerProfiles.get(event.getPlayer().getUniqueId()).getGender() == 'M') {
				genderRank = "Count";
			} else {
				genderRank = "Countess";
			}

			event.setFormat(staffPrefix + donatorPrefix + playerRankColor + faithPrefix + playerRankColor + genderRank.toLowerCase() +
					" " + playerRankColor + event.getPlayer().getDisplayName() + ChatColor.WHITE + ": %2$s");
		} else
		// //// HANDLE BARONS
		if (perms.getPrimaryGroup(player).equalsIgnoreCase("Baron")) {
			String genderRank;
			if (playerProfiles.get(player.getUniqueId()).getGender() == 'M') {
				genderRank = "Baron";
			} else {
				genderRank = "Baroness";
			}

			event.setFormat(staffPrefix + donatorPrefix + playerRankColor + faithPrefix + playerRankColor + genderRank.toLowerCase() +
					" " + playerRankColor + event.getPlayer().getDisplayName() + ChatColor.WHITE + ": %2$s");
		} else
		// //// HANDLE DUKES
		if (perms.getPrimaryGroup(player).equalsIgnoreCase("Duke")) {
			String genderRank;
			if (playerProfiles.get(player.getUniqueId()).getGender() == 'M') {
				genderRank = "Duke";
			} else {
				genderRank = "Duchess";
			}

			event.setFormat(staffPrefix + donatorPrefix + playerRankColor + faithPrefix + playerRankColor + genderRank.toLowerCase() +
					" " + playerRankColor + event.getPlayer().getDisplayName() + ChatColor.WHITE + ": %2$s");

			// //// HANDLE MULTICOLORED SLAYER TITLE
		} else if (perms.getPrimaryGroup(event.getPlayer()).equals("Slayer")) {
			event.setFormat(staffPrefix + donatorPrefix + ChatColor.BLUE + faithPrefix
					+ RunicParadise.rankColors.get(perms.getPrimaryGroup(event.getPlayer()))
					+ perms.getPrimaryGroup(event.getPlayer()).toLowerCase() + " "
					+ ChatColor.BLUE + event.getPlayer().getDisplayName() + ChatColor.WHITE + ": %2$s");
			// //// HANDLE FAITHS FOR ALL BUT GHOSTS SLAYERS
		} else if (!event.getPlayer().hasPermission("rp.GHOST")) {
			event.setFormat(staffPrefix + donatorPrefix + newbiePrefix + playerRankColor + faithPrefix
					+ perms.getPrimaryGroup(event.getPlayer()).toLowerCase() + ChatColor.GRAY + " "
					+ playerRankColor + event.getPlayer().getDisplayName() + ChatColor.WHITE + ": %2$s");
		} else {
			// //// HANDLE GHOSTS
			event.setFormat(staffPrefix + donatorPrefix + playerRankColor + perms.getPrimaryGroup(event.getPlayer())
					+ ChatColor.GRAY + " "
					+ playerRankColor + event.getPlayer().getDisplayName() + ChatColor.WHITE + ": %2$s");
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onBreakBlock(final BlockBreakEvent event) {

// Old custom Runic Graves Logic
/*
		if ((event.getBlock().getType() == Material.REDSTONE_LAMP_ON
				|| event.getBlock().getType() == Material.REDSTONE_LAMP_OFF)
				&& !RunicDeathChest.checkHashmapForDeathLoc(event.getBlock().getLocation()).equals("NoGrave")) {

			event.setCancelled(true);
			event.getPlayer().sendMessage(ChatColor.DARK_GRAY + "[RunicReaper] " + ChatColor.GRAY
					+ "Knocking over graves is bad luck! Right click the bottom!");
			getServer().dispatchCommand(getServer().getConsoleSender(),
					"effect " + event.getPlayer().getName() + " 9 10 10");
		} else if ((event.getBlock().getType() == Material.SIGN || event.getBlock().getType() == Material.SIGN_POST)
				&& !RunicDeathChest.checkHashmapForDeathLoc(event.getBlock().getLocation().subtract(0, 1, 0))
						.equals("NoGrave")) {
			event.setCancelled(true);
			event.getPlayer().sendMessage(ChatColor.DARK_GRAY + "[RunicReaper] " + ChatColor.GRAY
					+ "Knocking over graves is bad luck! Right click the bottom!");
			getServer().dispatchCommand(getServer().getConsoleSender(),
					"effect " + event.getPlayer().getName() + " 15 3 5");
						} else */

		if (event.getBlock().getType() == Material.SPAWNER && !event.getPlayer().hasPermission("rp.staff")) {
			event.setCancelled(true);
			event.getPlayer().sendMessage(ChatColor.DARK_RED + "Hey put that back! Only staff can break that.");
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
	public void onEntityTargetLivingEntityEvent(EntityTargetLivingEntityEvent event) {
		Borderlands.handleEntityTargetEventBL(event);

		if (event.getEntity() instanceof Player && protectedPlayers.containsKey(event.getTarget().getUniqueId())) {
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

		if (Bukkit.getOnlinePlayers().contains(event.getPlayer())) {

			if (event.getPlayer().hasPermission("rp.ranks.duke")) {
				Ranks.applyFeudalBonus(event.getPlayer(), event.getTo().getWorld().getName(),
						event.getFrom().getWorld().getName());
			}

			Faith.tryCast_PlayerTeleported(event);

			if (event.getFrom().getWorld().getName().equals("RunicSky")) {
				if ((event.getFrom().getX() <= -142 && event.getFrom().getX() >= -192)
						&& (event.getFrom().getY() <= 200 && event.getFrom().getY() >= 0)
						&& (event.getFrom().getZ() <= 513 && event.getFrom().getZ() >= 463)) {
					// A player is leaving the maze!
					event.getPlayer().sendMessage(
							ChatColor.DARK_RED + "DungeonMaster CrocodileHax" + ChatColor.GRAY + ": See you next time!");
					event.getPlayer().setGameMode(GameMode.SURVIVAL);

				} else if ((event.getFrom().getX() <= 1146 && event.getFrom().getX() >= 1047)
						&& (event.getFrom().getY() <= 160 && event.getFrom().getY() >= 85)
						&& (event.getFrom().getZ() <= 1152 && event.getFrom().getZ() >= 1053)) {
					event.getPlayer().sendMessage(
							ChatColor.DARK_RED + "DungeonMaster CrocodileHax" + ChatColor.GRAY + ": See you next time!");
					event.getPlayer().setGameMode(GameMode.SURVIVAL);
				}

			}
			if (event.getTo().getWorld().getName().equals("RunicSky")) {
				if ((event.getTo().getX() <= -142 && event.getTo().getX() >= -192)
						&& (event.getTo().getY() <= 121 && event.getTo().getY() >= 107)
						&& (event.getTo().getZ() <= 513 && event.getTo().getZ() >= 463)) {
					// A player is in the maze!
					event.getPlayer().sendMessage(ChatColor.DARK_RED + "DungeonMaster CrocodileHax" + ChatColor.GRAY
							+ ": Teleporting into my maze is cheating. So your teleport has been cancelled. :)");
					event.setCancelled(true);

				} else if ((event.getTo().getX() <= -137.5 && event.getTo().getX() >= -140.5)
						&& (event.getTo().getY() <= 120.5 && event.getTo().getY() >= 114)
						&& (event.getTo().getZ() <= 513.59 && event.getTo().getZ() >= 506.5)) {
					// A player is in the maze!
					event.getPlayer().sendMessage(ChatColor.DARK_RED + "DungeonMaster CrocodileHax" + ChatColor.GRAY
							+ ": Teleporting into my maze is cheating. So your teleport has been cancelled. :)");
					event.setCancelled(true);

				} else if ((event.getTo().getX() <= 1131 && event.getTo().getX() >= 1060)
						&& (event.getTo().getY() <= 40 && event.getTo().getY() >= 2)
						&& (event.getTo().getZ() <= -16 && event.getTo().getZ() >= -93)) {
					// A player is in the maze!
					event.getPlayer().sendMessage(ChatColor.DARK_RED + "MachineMaster Tardip" + ChatColor.GRAY
							+ ": Teleporting into my maze is cheating. So your teleport has been cancelled. :D");
					event.setCancelled(true);

				} else if ((event.getTo().getX() <= 1171 && event.getTo().getX() >= 1047)
						&& (event.getTo().getY() <= 160 && event.getTo().getY() >= 80)
						&& (event.getTo().getZ() <= 1152 && event.getTo().getZ() >= 1053)) {
					// A player is in the maze!
					event.getPlayer().sendMessage(ChatColor.DARK_RED + "DungeonMaster CrocodileHax" + ChatColor.GRAY
							+ ": Teleporting into my maze is cheating. So your teleport has been cancelled. :D");
					event.setCancelled(true);
				} /*else if ((event.getTo().getX() <= 1055 && event.getTo().getX() >= 1048)
					&& (event.getTo().getY() <= 126 && event.getTo().getY() >= 120)
					&& (event.getTo().getZ() <= 1157 && event.getTo().getZ() >= 1153)) {
				// A player is in the maze!
				event.getPlayer().sendMessage(ChatColor.DARK_RED + "DungeonMaster CrocodileHax" + ChatColor.GRAY
						+ ": Teleporting into my maze is cheating. So your teleport has been cancelled. :D");
				event.setCancelled(true);

			}*/
			}
		}
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {


		// Runic Profile - Main Menu
		if (event.getView().getTitle().contains("Profile :: Main Menu")) {
			if (event.getSlot() == 10) {
				// Player wants to open Kill Counts menu
				// Close the inventory screen
				event.getWhoClicked().closeInventory();
				// Open new menu
				playerProfiles.get(event.getWhoClicked().getUniqueId())
						.showKillCountMenu(((Player) event.getWhoClicked()));

			} else if (event.getSlot() == 11) {
				// Player wants to open Chat Options menu
				// Close the inventory screen
				event.getWhoClicked().closeInventory();
				// Open new menu
				playerProfiles.get(event.getWhoClicked().getUniqueId())
						.showProfileOptionsMenu(((Player) event.getWhoClicked()));

			}
			event.setCancelled(true);

		} else
		// Runic Profile - Kill Counts
		if (event.getView().getTitle().contains("Profile :: Kill Counts")) {

			if (event.getSlot() == 53) {
				// Player is returning to main menu
				// Close the inventory screen
				event.getWhoClicked().closeInventory();
				// Open main menu
				playerProfiles.get(event.getWhoClicked().getUniqueId())
						.showServerMenu(((Player) event.getWhoClicked()), ((Player) event.getWhoClicked()));

			}

			event.setCancelled(true);

		} else
		// Runic Profile - Chat Options
		if (event.getView().getTitle().contains("Profile :: Chat Options")) {

			if (event.getSlot() == 12) {
				// Player is toggling their gender setting
				// Close the inventory screen
				event.getWhoClicked().closeInventory();
				// Change the setting
				playerProfiles.get(event.getWhoClicked().getUniqueId()).changeGender("T", true);
				// Reopen the inventory screen
				playerProfiles.get(event.getWhoClicked().getUniqueId())
						.showProfileOptionsMenu(((Player) event.getWhoClicked()));

			} else if (event.getSlot() == 53) {
				// Player is returning to main menu
				// Close the inventory screen
				event.getWhoClicked().closeInventory();
				// Open main menu
				playerProfiles.get(event.getWhoClicked().getUniqueId())
						.showServerMenu(((Player) event.getWhoClicked()), ((Player) event.getWhoClicked()));

			} else if (event.getSlot() == 28 && event.getWhoClicked().hasPermission("rp.chatcolors.green")) {

				playerProfiles.get(event.getWhoClicked().getUniqueId()).setChatColor(ChatColor.GREEN, true);

			} else if (event.getSlot() == 29
					&& event.getWhoClicked().hasPermission("rp.chatcolors.dark_green")) {

				playerProfiles.get(event.getWhoClicked().getUniqueId()).setChatColor(ChatColor.DARK_GREEN, true);

			} else if (event.getSlot() == 30
					&& event.getWhoClicked().hasPermission("rp.chatcolors.yellow")) {

				playerProfiles.get(event.getWhoClicked().getUniqueId()).setChatColor(ChatColor.YELLOW, true);

			} else if (event.getSlot() == 31 && event.getWhoClicked().hasPermission("rp.chatcolors.gold")) {

				playerProfiles.get(event.getWhoClicked().getUniqueId()).setChatColor(ChatColor.GOLD, true);

			} else if (event.getSlot() == 32 && event.getWhoClicked().hasPermission("rp.chatcolors.aqua")) {

				playerProfiles.get(event.getWhoClicked().getUniqueId()).setChatColor(ChatColor.AQUA, true);

			} else if (event.getSlot() == 33
					&& event.getWhoClicked().hasPermission("rp.chatcolors.dark_aqua")) {

				playerProfiles.get(event.getWhoClicked().getUniqueId()).setChatColor(ChatColor.DARK_AQUA, true);

			} else if (event.getSlot() == 34 && event.getWhoClicked().hasPermission("rp.chatcolors.blue")) {

				playerProfiles.get(event.getWhoClicked().getUniqueId()).setChatColor(ChatColor.BLUE, true);

			} else if (event.getSlot() == 38
					&& event.getWhoClicked().hasPermission("rp.chatcolors.light_purple")) {

				playerProfiles.get(event.getWhoClicked().getUniqueId()).setChatColor(ChatColor.LIGHT_PURPLE, true);

			} else if (event.getSlot() == 39
					&& event.getWhoClicked().hasPermission("rp.chatcolors.dark_purple")) {

				playerProfiles.get(event.getWhoClicked().getUniqueId()).setChatColor(ChatColor.DARK_PURPLE, true);

			} else if (event.getSlot() == 41 && event.getWhoClicked().hasPermission("rp.chatcolors.red")) {

				playerProfiles.get(event.getWhoClicked().getUniqueId()).setChatColor(ChatColor.RED, true);

			} else if (event.getSlot() == 42 && event.getWhoClicked().hasPermission("rp.chatcolors.white")) {

				playerProfiles.get(event.getWhoClicked().getUniqueId()).setChatColor(ChatColor.WHITE, true);

			}

			event.setCancelled(true);

		} else
		// Skynet Spawn Menu
		if (event.getView().getTitle().contains("SkyNet Warp Orb")) {
			switch (event.getSlot()) {

			case 30:
				// teleport to library
				event.getWhoClicked().teleport(new Location(Bukkit.getWorld("RunicSky"), -917, 94.00, -413,
						(float) 91.49997, (float) -050000125));
				break;
			case 19:
				// teleport to jobs
				event.getWhoClicked().teleport(new Location(Bukkit.getWorld("RunicSky"), -911, 94.0, -430,
						(float) -88.65011, (float) -2.6000035));
				break;
			case 20:
				// teleport to donation
				event.getWhoClicked().teleport(new Location(Bukkit.getWorld("RunicSky"), -927, 94.0, -350,
						(float) -271.05072, (float) -0.9500019));
				break;
			case 21:
				// teleport to towns n shops
				event.getWhoClicked().teleport(new Location(Bukkit.getWorld("RunicSky"), -922, 94.0, -349.0,
						(float) -88.200745, (float) -0.65000284));
				break;
			case 22:
				// teleport to wild
				event.getWhoClicked().teleport(
						new Location(Bukkit.getWorld("RunicSky"), -309, 126.0, -412.0, 268.15707F, -0.49672678F));
				break;
			case 23:
				// teleport to hub
				event.getWhoClicked().teleport(
						new Location(Bukkit.getWorld("RunicSky"), -967, 89.0, 120.0, -178.53664F, 5.8302865F));
				break;
			case 24:
				// teleport to faith
				event.getWhoClicked().teleport(
						new Location(Bukkit.getWorld("RunicSky"), -1013, 89.0, 164.0, -270.03625F, -29.269753F));
				break;
			case 25:
				// teleport to mining
				event.getWhoClicked().teleport(
						new Location(Bukkit.getWorld("RunicSky"), -943, 85.0, 189, -43.536087F, -2.5697594F));
				break;
			case 31:
					// teleport to pet shop
					event.getWhoClicked().teleport(
							new Location(Bukkit.getWorld("RunicSky"), -911, 94, -392, -90.14988F, -0.7999952F));
					break;
			case 32:
				// teleport to graves
				event.getWhoClicked().teleport(
						new Location(Bukkit.getWorld("RunicSky"), -934, 82, 132, -134.8862F, -3.769757F));
				break;
			case 33:
				// teleport to soda
				event.getWhoClicked().teleport(
						new Location(Bukkit.getWorld("RunicSky"), -870, 94, -367.0, -180.14984F, -10.999985F));
				break;
			case 29:
				// teleport to crates
				event.getWhoClicked().teleport(
						new Location(Bukkit.getWorld("RunicSky"), -983, 84, 181, 47.21338F, -1.5197498F));
				break;
			case 39:
				// teleport to colosseum
				event.getWhoClicked().teleport(
						new Location(Bukkit.getWorld("RunicSky"), -776, 94, -375, -176.5498F, 2.0500402F));
				break;
			case 41:
				// teleport to jail
				event.getWhoClicked().teleport(
						new Location(Bukkit.getWorld("RunicSky"), -790, 94, -365, -357.74982F, 4.15004F));
				break;
			default:
				break;

			}
			event.setCancelled(true);
		} else
		// Carnival menu - MAZES
		if (event.getView().getTitle().contains("Runic Carnival - Mazes")) {
			switch (event.getSlot()) {
			case 8:
				// teleport to puzzle Kiosk
				event.getWhoClicked().teleport(
						new Location(Bukkit.getWorld("RunicSky"), 328, 58, 543, (float) 72.99, (float) -26.40));
				break;
			default:
				break;

			}
			event.setCancelled(true);
		} else
		// Carnival menu - EXPLORERS
		if (event.getView().getTitle().contains("Explorer's League")) {
			switch (event.getSlot()) {
			case 47:
			case 51:
				if (event.getCurrentItem() != null) {
					String itemDispName = event.getCurrentItem().getItemMeta().getDisplayName();
					int pageNumber = itemDispName.charAt(itemDispName.length() - 1) - '0';
					RunicParadise.showRunicCarnivalMenu_Explorers((Player) event.getWhoClicked(), pageNumber);
				}

				break;
			default:
				break;

			}
			event.setCancelled(true);
		} else
		// Carnival menu - PARKOURS
		if (event.getView().getTitle().contains("Runic Carnival - Parkours")) {
			switch (event.getSlot()) {
			case 17:
				// teleport to puzzle Kiosk
				event.getWhoClicked().teleport(
						new Location(Bukkit.getWorld("RunicSky"), 328, 58, 543, (float) 72.99, (float) -26.40));
				break;
			default:
				break;
			}
			event.setCancelled(true);
		} else
		// Carnival menu - ARENAS
		if (event.getView().getTitle().contains("Runic Carnival - Arenas")) {
			switch (event.getSlot()) {
			case 20:
				// teleport to paintball entry
				event.getWhoClicked().teleport(new Location(Bukkit.getWorld("RunicSky"), 377.563, 56.00, 516.323,
						(float) -180.29913, (float) -1.4999065));
				break;
			case 21:
				// teleport to blockhunt entry
				event.getWhoClicked().teleport(new Location(Bukkit.getWorld("RunicSky"), 381.653, 56.00, 516.313,
						(float) -181.64929, (float) -2.6999066));
				break;
			case 22:
				// teleport to ctf entry
				event.getWhoClicked().teleport(new Location(Bukkit.getWorld("RunicSky"), 385.650, 56.00, 516.22,
						(float) -180.74915, (float) -1.3499054));
				break;
			case 23:
				// teleport to pvparena entry
				event.getWhoClicked().teleport(new Location(Bukkit.getWorld("RunicSky"), 385.650, 56.00, 516.22,
						(float) -180.74915, (float) -1.3499054));
				break;

			case 24:
				// teleport to spleef entry
				event.getWhoClicked().teleport(new Location(Bukkit.getWorld("RunicSky"), 377.450, 56.00, 528.886,
						(float) -359.8496, (float) -0.44990847));
				break;

			case 31:
				// teleport to mobarena entry
				event.getWhoClicked().teleport(new Location(Bukkit.getWorld("RunicSky"), 381.530, 56.00, 528.890,
						(float) -359.09937, (float) 1.2000911));
				RunicMessaging.sendMessage((Player) event.getWhoClicked(), RunicFormat.HELP, ChatColor.RED + "Empty your inventory before ppentering the arena!!");
				break;

			default:
				break;

			}
			event.setCancelled(true);
		} else if (event.getView().getTitle().contains("Runic Carnival Menu")) {
			switch (event.getSlot()) {
			case 19:
				// teleport to info center
				event.getWhoClicked().teleport(new Location(Bukkit.getWorld("RunicSky"), 342, 58, 548, 0, (float) 1));
				break;
			case 20:
				// teleport to prize cabin
				event.getWhoClicked().teleport(
						new Location(Bukkit.getWorld("RunicSky"), 320, 58, 522, (float) 92.50, (float) -16.05));
				break;
			case 21:
				// open maze menu
				RunicParadise.showRunicCarnivalMenu_Puzzles(((Player) event.getWhoClicked()), 'M');
				break;
			case 22:
				// open parkour menu
				RunicParadise.showRunicCarnivalMenu_Puzzles(((Player) event.getWhoClicked()), 'P');
				break;
			case 31:
				// explorer's league
				RunicParadise.showRunicCarnivalMenu_Explorers((Player) event.getWhoClicked(), 1);
				break;
			case 32:
				// adventure islands
				event.getWhoClicked().sendMessage("Croc's adventure islands are coming soon!");
				break;
			case 33:
				// open battle tower menu
				RunicParadise.showRunicCarnivalMenu_BattleTower(((Player) event.getWhoClicked()));
				break;
			case 34:
				// teleport to creation zone
				event.getWhoClicked().teleport(
						new Location(Bukkit.getWorld("RunicSky"), 357, 58, 538, (float) -42.150, (float) -27.85));
				break;
			default:
				break;

			}
			event.setCancelled(true);
		} else

		// handle Slimefun inventory; initial 1.9 allows players to take the
		// items, so stop them!
		if (event.getView().getTitle().contains("Slimefun") && event.getView().getTitle().contains("Guide")) {

			event.setCancelled(true);
		} else

		// handle faith inventory
		if (event.getView().getTitle().contains("Runic") && event.getView().getTitle().contains("Faith")) {

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
				getServer().dispatchCommand(getServer().getConsoleSender(),
						"faith enable " + event.getWhoClicked().getName() + " " + faith);
			}
		}
	}

	@EventHandler
	public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
		if (event.getItem().getType() == Material.CHORUS_FRUIT
				&& event.getPlayer().getWorld().getName().equalsIgnoreCase("RunicSky")) {
			event.setCancelled(true);
			event.getPlayer().sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "You can't eat that in this world.");
		}

		if (event.getItem() != null && event.getItem().hasItemMeta()) {
			if (event.getItem().getItemMeta().hasLore()) {
				if (event.getItem().getItemMeta().getLore().toString().contains("Vanilla ice cream between")) {
					event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 3600, 1));
					event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 3600, 1));
				}
			}
		}

		CustomItems customItem = CustomItems.getCustomItem(event.getItem());
		if (customItem != null) {
			Player player = event.getPlayer();
			if (customItem.isRegenerationRunestone()) {
				player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 36000, 2));
			} else if (customItem.isHasteRunestone()) {
				event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 18000, 1));
			} else if (customItem.isSpeedRunestone()) {
				event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 18000, 1));
			} else if (customItem.isStrengthRunestone()) {
				event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 24000, 2));
			} else if (customItem.isNightvisionRunestone()) {
				event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 72000, 1));
			}
		}
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onPlayerJoin(final PlayerJoinEvent pje) {
		if (!playerProfiles.containsKey(pje.getPlayer().getUniqueId())) {
			playerProfiles.put(pje.getPlayer().getUniqueId(), new RunicProfile(pje.getPlayer().getUniqueId()));
		}

		if (pje.getPlayer().hasPermission("rp.chatfilterwarning1") && !pje.getPlayer().hasPermission("rp.admin")) {
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + pje.getPlayer().getName() + " permission unset rp.chatfilterwarning1");
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + pje.getPlayer().getName() + " permission unset -essentials.me");



		}

		if (pje.getPlayer().hasPermission("rp.chatfilterwarning2") && !pje.getPlayer().hasPermission("rp.admin")) {
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + pje.getPlayer().getName() + " permission unset rp.chatfilterwarning2");
		}

		//RunicUtilities.convertGroupManager(pje.getPlayer());
/*
		RunicParadise.getInstance().getServer().getScheduler().scheduleAsyncDelayedTask(RunicParadise.getInstance(), new Runnable() {
			public void run() {
				checkMcmmoUserCreated(pje.getPlayer());
			}
		}, 100L);
*/

		updatePlayerInfoOnJoin(pje.getPlayer().getName(), pje.getPlayer().getUniqueId());

		ranks.convertRanks(pje.getPlayer());

		updatePlayerCMIRank(pje.getPlayer());

		faithMap.put(pje.getPlayer().getUniqueId(), new Faith(pje.getPlayer().getUniqueId()));

		refreshCMIRank(pje.getPlayer());
		eliminateDefaultGroup(pje.getPlayer());


			if (!pje.getPlayer().hasPermission("rp.slimefun.smallbackpack")
					&& !pje.getPlayer().getWorld().getName().equalsIgnoreCase("plotworld")) {

				RunicPlayerBukkit target = new RunicPlayerBukkit(pje.getPlayer().getUniqueId());

				if (target.getPlayerVoteCount() >= 125) {
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + pje.getPlayer().getName() + " permission set rp.slimefun.smallbackpack");
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "cmi toast " + pje.getPlayer().getName() + " -t:challenge -icon:chest Small backpack unlocked!");

					for (Player p : Bukkit.getOnlinePlayers()) {
						p.sendMessage(ChatColor.DARK_PURPLE + "" + pje.getPlayer().getDisplayName()
								+ ChatColor.DARK_PURPLE
								+ " has voted 125 times! They can now make small backpacks! Check your votes with /rp and vote today!!");
					}
				}
			}



			if (!pje.getPlayer().hasPermission("rp.slimefun.mediumbackpack")
					&& !pje.getPlayer().getWorld().getName().equalsIgnoreCase("plotworld")) {

				RunicPlayerBukkit target = new RunicPlayerBukkit(pje.getPlayer().getUniqueId());

				if (target.getPlayerVoteCount() >= 250) {
					perms.playerAdd("", pje.getPlayer(), "rp.slimefun.mediumbackpack");
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + pje.getPlayer().getName() + " permission set rp.slimefun.mediumbackpack");
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "cmi toast " + pje.getPlayer().getName() + " -t:challenge -icon:chest Medium backpack unlocked!");

					for (Player p : Bukkit.getOnlinePlayers()) {
						p.sendMessage(ChatColor.LIGHT_PURPLE + "" + pje.getPlayer().getDisplayName()
								+ ChatColor.LIGHT_PURPLE
								+ " has voted 250 times! They can now make medium backpacks! Check your votes with /rp and vote today!!");
					}
				}
			}

	}

	@EventHandler
	public void onPlayerQuit(final PlayerQuitEvent pje) {
		playerProfiles.get(pje.getPlayer().getUniqueId()).saveMobKillsForPlayer();
		playerProfiles.remove(pje.getPlayer().getUniqueId());

		faithMap.remove(pje.getPlayer().getUniqueId());
		log.log(Level.INFO, "RP Faith: Removed " + pje.getPlayer().getName() + " from faith map.");

		updatePlayerInfoOnQuit(pje.getPlayer().getName(), pje.getPlayer().getUniqueId());

	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {

			// process Spawn SKynet menu clicks -- right click
			if (event.getClickedBlock().getWorld().getName().equals("RunicSky"))
				if (((event.getClickedBlock().getType() == Material.PLAYER_HEAD) && (event.getClickedBlock().getLocation()
						.subtract(0, 1, 0).getBlock().getType() == Material.PURPLE_STAINED_GLASS_PANE))
						|| ((event.getClickedBlock().getType() == Material.ARMOR_STAND) && ((event.getClickedBlock()
						.getLocation().add(0, 1, 0).getBlock().getType() == Material.PURPLE_STAINED_GLASS_PANE)
						|| (event.getClickedBlock().getLocation().getBlock()
						.getType() == Material.PURPLE_STAINED_GLASS_PANE)))) {
					RunicParadise.showSpawnSkynetMenu(event.getPlayer());
					event.setCancelled(true);
				}

			if (event.getClickedBlock().getType().equals(Material.SPAWNER)
					&& event.getPlayer().getInventory().getItemInMainHand().getType().getId() == 383) {
				event.setCancelled(true);
				RunicMessaging.sendMessage(event.getPlayer(), RunicMessaging.RunicFormat.ERROR, "You cannot do that!");

			} else if (event.getClickedBlock().getType().equals(Material.STONE_BRICKS)
					&& event.getPlayer().hasPermission("rp.admin")
					&& event.getPlayer().getInventory().getItemInMainHand().getType() == Material.BLAZE_ROD) {
				placeRunicEye(event.getClickedBlock().getLocation(), event.getPlayer());

			} else if (event.getClickedBlock().getType().equals(Material.STONE_BRICKS)
					&& runicEyes.containsKey(event.getClickedBlock().getLocation())) {
				// player has right clicked a stone block and its a runic eye
				// location!
				event.getPlayer().sendMessage(ChatColor.GOLD + "" + ChatColor.ITALIC + "You hear the "
						+ runicEyes.get(event.getClickedBlock().getLocation())[0] + " in your mind...");
				event.getPlayer().sendMessage(ChatColor.GRAY + runicEyes.get(event.getClickedBlock().getLocation())[2]);
			} else if (event.getClickedBlock().getType().equals(Material.STONE_BRICKS)
					&& event.getPlayer().hasPermission("rp.admin")
					&& event.getPlayer().getItemInHand().getType() == Material.BOOK) {
				placePrayerBook(event.getClickedBlock().getLocation(), event.getPlayer());

			} else if (event.getClickedBlock().getType().equals(Material.STONE_BRICKS)
					&& prayerBooks.containsKey(event.getClickedBlock().getLocation())) {
				// player has right clicked a stone block and its a prayer book
				// location!
				Faith.pray(event.getClickedBlock().getLocation(), event.getPlayer());
			} else {
				// not a redstone lamp

				if (faithMap.get(event.getPlayer().getUniqueId()).checkEquippedFaithLevel("Earth",
						RunicParadise.powerReqsMap.get("Earth's Bounty"))) {
					faithMap.get(event.getPlayer().getUniqueId()).castEarth_EarthsBounty(event);
				}

				if (faithMap.get(event.getPlayer().getUniqueId()).checkEquippedFaithLevel("Water",
						RunicParadise.powerReqsMap.get("Deep Wader"))) {
					faithMap.get(event.getPlayer().getUniqueId()).castWater_DeepWader(event);
				}
			}
		} else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
			// process Spawn SKynet menu clicks -- right click
			if (event.getClickedBlock().getType() == Material.PLAYER_HEAD
					&& event.getClickedBlock().getWorld().getName().equals("RunicSky") && event.getClickedBlock()
							.getLocation().subtract(0, 1, 0).getBlock().getType() == Material.PURPLE_STAINED_GLASS_PANE) {
				RunicParadise.showSpawnSkynetMenu(event.getPlayer());
				event.setCancelled(true);
			} else if (event.getClickedBlock().getType().equals(Material.CHISELED_STONE_BRICKS)
					&& prayerBooks.containsKey(event.getClickedBlock().getLocation())) {
				// player has left clicked a stone block and its a prayer book
				// location!
				event.getPlayer().sendMessage(ChatColor.GOLD + "" + ChatColor.ITALIC
						+ prayerBooks.get(event.getClickedBlock().getLocation())[2]);
				event.getPlayer()
						.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC
								+ "This prayer requires that you sacrifice: " + ChatColor.RESET + ChatColor.DARK_AQUA
								+ prayerBooks.get(event.getClickedBlock().getLocation())[3]);
			} else if (event.getClickedBlock().getType().equals(Material.OBSERVER)
					&& event.getClickedBlock().getWorld().getName().equalsIgnoreCase("RunicSky")) {

				Player p = event.getPlayer();
				Integer currentSouls = playerProfiles.get(p.getUniqueId()).getSoulCount();
				//RunicMessaging.sendMessage(p, RunicFormat.AFTERLIFE, "[DEBUG] You just left clicked an OBSERVER!");


				try {
					if (p.getInventory().getItemInMainHand().getItemMeta().getLore().toString().contains("Redeem")) {
						 if (p.getInventory().getItemInMainHand().getItemMeta().getDisplayName().contains("Soul Cheque")) {
							 playerProfiles.get(p.getUniqueId()).grantCurrency("Souls", 1);

							 p.getInventory().getItemInMainHand().setAmount(p.getInventory().getItemInMainHand().getAmount() - 1);

							 //RunicMessaging.sendMessage(p, RunicFormat.AFTERLIFE, "[DEBUG] Found a cheque for 1 soul");

						} else {
							 RunicMessaging.sendMessage(p, RunicFormat.AFTERLIFE, "[DEBUG] Found a cheque but not sure what to do!");
						 }

					} else {
						RunicMessaging.sendMessage(p, RunicFormat.AFTERLIFE, "You must have a " + ChatColor.BOLD + "Soul Cheque" + ChatColor.RESET + "" + ChatColor.GRAY + " in your hand to use this machine!!");
					}


				}  catch(NullPointerException e){
						RunicMessaging.sendMessage(p, RunicFormat.AFTERLIFE, "You must have a " + ChatColor.BOLD + "Soul Cheque" + ChatColor.RESET + "" + ChatColor.GRAY + " in your hand to use this machine!");
				}


			}

		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerDamage(final EntityDamageEvent ede) {
		boolean daytime = (Bukkit.getWorld("RunicKingdom").getTime() <= 14000 || Bukkit.getWorld("RunicKingdom").getTime() >= 23000);

		// handle deaths in the dungeon maze
		if (ede.getEntity() instanceof Player) {

			Location deathLoc = ede.getEntity().getLocation();

			if (((Player) ede.getEntity()).getHealth() - ede.getDamage() < 1) {
				if (deathLoc.getWorld().getName().equals("RunicSky")) {
					if ((deathLoc.getX() <= -142 && deathLoc.getX() >= -192)
							&& (deathLoc.getY() <= 200 && deathLoc.getY() >= -64)
							&& (deathLoc.getZ() <= 513 && deathLoc.getZ() >= 463)) {
						((Player) ede.getEntity()).setHealth(((Player) ede.getEntity()).getMaxHealth());
						ede.setCancelled(true);
						ede.getEntity().sendMessage(ChatColor.DARK_RED + "DungeonMaster CrocodileHax" + ChatColor.GRAY
								+ ": Looks like the score is Traps 1, You 0. Better luck next time.");
						ede.getEntity().teleport(new Location(Bukkit.getWorld("RunicSky"), -131, 116, 509));

						((Player) ede.getEntity()).setGameMode(GameMode.SURVIVAL);
					} else if ((deathLoc.getX() <= 1171 && deathLoc.getX() >= 1047)
							&& (deathLoc.getY() <= 160 && deathLoc.getY() >= 80)
							&& (deathLoc.getZ() <= 1152 && deathLoc.getZ() >= 1053)) {
						((Player) ede.getEntity()).setHealth(((Player) ede.getEntity()).getMaxHealth());
						ede.setCancelled(true);
						ede.getEntity().sendMessage(ChatColor.DARK_RED + "DungeonMaster CrocodileHax" + ChatColor.GRAY
								+ ": Another soul lost to Anguish!");
						ede.getEntity().teleport(new Location(Bukkit.getWorld("RunicSky"), 1054, 123, 1166, 89.714f, 16.2f));

						((Player) ede.getEntity()).setGameMode(GameMode.SURVIVAL);

					} else if ((deathLoc.getX() <= 1131 && deathLoc.getX() >= 1060)
					&& (deathLoc.getY() <= 40 && deathLoc.getY() >= 2)
					&& (deathLoc.getZ() <= -16 && deathLoc.getZ() >= -93)) {
						((Player) ede.getEntity()).setHealth(((Player) ede.getEntity()).getMaxHealth());
						ede.setCancelled(true);
						ede.getEntity().sendMessage(ChatColor.DARK_RED + "MachineMaster Tardip" + ChatColor.GRAY
								+ ": Machines win every time!!");
						ede.getEntity().teleport(new Location(Bukkit.getWorld("RunicSky"), 326.007, 55.000, 543.292, 64.890015f, -9.449995f));

						((Player) ede.getEntity()).setGameMode(GameMode.SURVIVAL);
					} else if ((deathLoc.getX() <= 83 && deathLoc.getX() >= 5)
							&& (deathLoc.getY() <= 138 && deathLoc.getY() >= 75)
							&& (deathLoc.getZ() <= 1365 && deathLoc.getZ() >= 1287)) {
						((Player) ede.getEntity()).setHealth(((Player) ede.getEntity()).getMaxHealth());
						ede.setCancelled(true);
						ede.getEntity().sendMessage(ChatColor.DARK_RED + "Foreman IcyPenguin" + ChatColor.GRAY
								+ ": You fell victim to the mines... but you can retry your escape!");
						ede.getEntity().teleport(new Location(Bukkit.getWorld("RunicSky"), 73, 85, 1326, -269.94138f, 0.90029997f));

						((Player) ede.getEntity()).setGameMode(GameMode.SURVIVAL);
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
				String cmd = "spawn " + player.getName();
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);

				player.sendMessage(
						ChatColor.AQUA + "" + ChatColor.ITALIC + "Found you lost in the void... watch your step!");
			}

		} else if (ede.getCause() == DamageCause.FALL && ede.getEntity() instanceof Player) {
			Faith.tryCast_PlayerTookFallDamage((Player) ede.getEntity());
		} else if (ede.getEntity() instanceof Player && faithMap.containsKey(ede.getEntity().getUniqueId())) {
			Faith.tryCast_PlayerTookEntityDamage(ede, ((Player) ede.getEntity()));
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent edbe) {
		if (edbe.getDamager() instanceof Player) {
			if (((Player) edbe.getDamager()).getInventory().getItemInMainHand().getType().equals(Material.NETHER_STAR)) {
				edbe.setCancelled(true);
				edbe.getDamager().sendMessage("That cannot be used as a weapon anymore.");
				return;
			}
		}
		// If this ia player attacking a Monster and player has faiths
		if (edbe.getDamager() instanceof Player && edbe.getEntity() instanceof Monster
				&& faithMap.containsKey(edbe.getDamager().getUniqueId())) {

			Faith.tryCast_PlayerHitMonster(edbe, edbe.getEntity(), (Player) edbe.getDamager());
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
	public void onRaidFinishEvent (RaidFinishEvent event) {
		List<Player> players = event.getWinners();
		int omenLevel = event.getRaid().getBadOmenLevel();
		int chance = 7 - omenLevel;
		float target = ((float)1/chance) * 1000;

		for (Player p : players) {
			p.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "You find some loose tokens in the pockets of the pillagers");
			playerProfiles.get(p.getUniqueId()).grantCurrency("Tokens", (2*(omenLevel+1)));

			Random rand = new Random();
			// Obtain a number between [0 - 999].
			int n = rand.nextInt(1000);
			Bukkit.getLogger().log(Level.INFO, "DEBUG: LairKey raid attempt! Rolled a " + n + " - Target result is < " + target + ". Omen Level = " + omenLevel );
			if (n <= target) {
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "cr give to " + p.getName() + " LairKey 1");
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "cmi mail send CrocodileHax " + p.getName() + " got a lair key from a raid!");
				p.sendMessage(ChatColor.YELLOW + "" + ChatColor.ITALIC + "The last pillager seems to have been holding something of value...");
			}
		}
	}

	@EventHandler
	public void onEntityDeath(EntityDeathEvent ede) {
		boolean farming = false;

		// check for Monsters... Flying=Ghast... Slime=Slime/Magmacube...
		// WaterMob = Squid
		if (ede.getEntity() != null) {
			LivingEntity monsterEnt = ede.getEntity();

			// if a monster has died and killer was player
			if (monsterEnt.getLastDamageCause() instanceof EntityDamageByEntityEvent
					&& ((EntityDamageByEntityEvent) monsterEnt.getLastDamageCause()).getDamager() instanceof Player) {

				EntityDamageByEntityEvent nEvent = (EntityDamageByEntityEvent) monsterEnt.getLastDamageCause();
				Player q = (Player)nEvent.getDamager();

				if (ede.getEntity().getCustomName() != null && ede.getEntity().getCustomName().contains("Lucky Creeper")) {
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "raffle give 5 " + q.getName());
					for (Player a : Bukkit.getOnlinePlayers()) {
						RunicMessaging.sendMessage(a, RunicFormat.RAFFLE, q.getDisplayName() + ChatColor.GOLD + " just got 5 raffle tickets from a lucky creeper!");
					}
				}


				Faith.tryCast_PlayerKilledMonster(ede, (Player) nEvent.getDamager());

				// check for farming
				if (playerProfiles.get(nEvent.getDamager().getUniqueId())
						.checkFarming(ede.getEntity().getLocation())) {
					farming = true;
					//Bukkit.getLogger().log(Level.INFO, ((Player) nEvent.getDamager()).getDisplayName() + " failed BL loot logic due to farming = true!");
				} else {
					//Bukkit.getLogger().log(Level.INFO, ((Player) nEvent.getDamager()).getDisplayName() + " has triggered a BL loot logic check; farming = false!");
					Borderlands.adjustRewardsforBLMobs(ede, ((Player) nEvent.getDamager()));

					NamespacedKey key = new NamespacedKey(this, "NotFromSpawner");
					PersistentDataContainer container = ede.getEntity().getPersistentDataContainer();

					if(container.has(key , PersistentDataType.STRING)) {
						if (nEvent.getEntityType() == EntityType.ZOMBIE || nEvent.getEntityType() == EntityType.SKELETON) {
							Random rand = new Random();
							// Obtain a number between [0 - 899].
							int n = rand.nextInt(900);
							Bukkit.getLogger().log(Level.INFO, "DEBUG: LairKey attempt! Rolled a " + n + " - Target result is 0, 1, or 2");
							if (n >= 0 && n <= 2) {
								Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "cr give to "+ ((Player) nEvent.getDamager()).getName() +" LairKey 1");
								Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "cmi mail send CrocodileHax "+ ((Player) nEvent.getDamager()).getName() +" got a lair key!");
								((Player) nEvent.getDamager()).sendMessage(ChatColor.YELLOW + "" + ChatColor.ITALIC + "The fallen monster has dropped a key!");
							}
						}
					}


				}

			}

			EntityDamageEvent e = monsterEnt.getLastDamageCause();
			if (e instanceof EntityDamageByEntityEvent) {
				EntityDamageByEntityEvent ee = (EntityDamageByEntityEvent) e;
			}

			if (monsterEnt.getKiller() == null || !(monsterEnt.getKiller() instanceof Player)
					|| ede.getEntity().getWorld().getName().equals("plotworld")) {
				// [RP] Entity death detected but player=null or world=plotworld
				// OR killer not a player
				// so nothing recorded!

			} else {
				// end run()

				if (RunicParadise.playerProfiles.get(monsterEnt.getKiller().getUniqueId()) == null) {
					Bukkit.getLogger().log(Level.SEVERE, monsterEnt.getKiller().getDisplayName() + " is not in the playerProfiles array! :(");
				}

				Bukkit.getServer().getScheduler().runTaskAsynchronously(instance, () -> {
							RunicParadise.playerProfiles.get(monsterEnt.getKiller().getUniqueId())
									.trackMobKill(ede);

					String mobType = "";
					boolean attemptPowersSkillUp = false;
					boolean heldWeaponHasLore = false;
					int faithStrength = 0;

					if (monsterEnt.getKiller().getItemInHand() != null
							&& monsterEnt.getKiller().getItemInHand().hasItemMeta()) {
						if (monsterEnt.getKiller().getItemInHand().getItemMeta().hasLore()) {

							if (monsterEnt.getKiller().getItemInHand().getItemMeta().getLore().toString()
									.contains("A blessed blade with a faint glow")) {
								heldWeaponHasLore = true;
								faithStrength = 1;
							} else if (monsterEnt.getKiller().getItemInHand().getItemMeta().getLore().toString()
									.contains("A blessed blade with a pulsing glow")) {
								heldWeaponHasLore = true;
								faithStrength = 2;
							} else if (monsterEnt.getKiller().getItemInHand().getItemMeta().getLore().toString()
									.contains("A blessed blade with a blinding glow")) {
								heldWeaponHasLore = true;
								faithStrength = 3;
							} else if (monsterEnt.getKiller().getItemInHand().getItemMeta().getLore().toString()
									.contains("A corrupted axe with a crimson glow")) {
								heldWeaponHasLore = true;
								faithStrength = 4;
							} else {
								heldWeaponHasLore = false;

							}
						} else {
							heldWeaponHasLore = false;
						}
					}

					// 1 in the trackable entities map represents a hostile
					// mob
					if (RunicParadise.trackableEntityKillsMap.get(ede.getEntityType()) == 1 && heldWeaponHasLore) {
						faithMap.get(monsterEnt.getKiller().getUniqueId()).trySkillUp(
								monsterEnt.getKiller(),
								faithMap.get(monsterEnt.getKiller().getUniqueId()).getPrimaryFaith(), faithStrength,
								"FaithWeapon");
					}

				}); // delay // end task method
			} // end else
		} // end LivingEntity check (if)
	} // end method

	private void updatePlayerInfoOnJoin(String name, UUID pUUID) {
		Date now = new Date();
		String playerName = name;
		UUID playerUUID = pUUID;

		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
		Connection connection = MySQL.openConnection();
		int rowCount = -1;
		int rowCountnameMatch = -1;
		String previousName = "";

		try {
			PreparedStatement statement = connection
					.prepareStatement("SELECT COUNT(*) as Total, PlayerName FROM rp_PlayerInfo WHERE UUID = ?;");
			statement.setString(1, playerUUID.toString());
			ResultSet dbResult = statement.executeQuery();
			while (dbResult.next()) {
				rowCount = dbResult.getInt("Total");
				previousName = dbResult.getString("PlayerName");
			}
			statement.close();

			PreparedStatement zStmt = connection
					.prepareStatement("SELECT COUNT(*) as Total FROM rp_PlayerInfo WHERE PlayerName = ?;");
			zStmt.setString(1, playerName);
			ResultSet zResult = zStmt.executeQuery();
			while (zResult.next()) {
				rowCountnameMatch = zResult.getInt("Total");
			}
			zStmt.close();

		} catch (SQLException e) {
			getLogger().log(Level.SEVERE, "Cant check for row count in updatePlayerInfoOnJoin for " + playerName
					+ " because: " + e.getMessage());
		}

		if (rowCount != rowCountnameMatch) {
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
					"sc Name change detected for " + playerName + " (" + previousName + ")");
			Bukkit.getLogger().log(Level.INFO,
					"[RP] Name change detected for " + playerName + " (" + previousName + ")");
		}

		try {

			// if this player has no rows in the table yet

			if (rowCount == 0) {

				Bukkit.getServer().getScheduler().scheduleAsyncDelayedTask(instance, () -> {
					// tell the other server this one is reconnected
					// to the universe
					ByteArrayDataOutput out = ByteStreams.newDataOutput();
					out.writeUTF("Forward"); // So BungeeCord knows
												// to forward it
					out.writeUTF("ONLINE");
					out.writeUTF("NewPlayer"); // The channel name
												// to check if this
												// your data

					ByteArrayOutputStream msgbytes = new ByteArrayOutputStream();
					DataOutputStream msgout = new DataOutputStream(msgbytes);

					try {
						msgout.writeUTF(Bukkit.getPlayer(playerUUID).getDisplayName()); // You
																						// can
																						// do
																						// anything
						// msgout
						msgout.writeShort(123);
					} catch (IOException ignored) {}

					out.writeShort(msgbytes.toByteArray().length);
					out.write(msgbytes.toByteArray());

					// If you don't care about the player
					// Player player =
					// Iterables.getFirst(Bukkit.getOnlinePlayers(),
					// null);
					// Else, specify them

					Bukkit.getPlayer(playerUUID).sendPluginMessage(instance, "BungeeCord", out.toByteArray());
				}, 140);

				// /////////////////////
				PreparedStatement dStmt = connection.prepareStatement(
						"INSERT INTO rp_PlayerInfo (`PlayerName`, `UUID`, `ActiveFaith`, `LastIP`, `FirstSeen`, `LastSeen`) VALUES "
								+ "(?, ?, ?, ?, ?, ?);");
				dStmt.setString(1, playerName);
				dStmt.setString(2, playerUUID.toString());
				dStmt.setString(3, "Sun");
				dStmt.setString(4, Bukkit.getPlayer(playerUUID).getAddress().getAddress().getHostAddress());
				dStmt.setLong(5, now.getTime());
				dStmt.setLong(6, now.getTime());

				dStmt.executeUpdate();

				PreparedStatement pStmt = connection
						.prepareStatement("INSERT INTO rp_PlayerMobKills (`UUID`) VALUES " + "(?);");
				pStmt.setString(1, playerUUID.toString());
				pStmt.executeUpdate();

				pStmt.close();
				dStmt.close();

				// if this player has 1 row in the table
			} else if (rowCount == 1) {
				PreparedStatement dStmt = connection.prepareStatement(
						"UPDATE `rp_PlayerInfo` SET LastSeen=?, PlayerName=?, LastIP=? WHERE UUID=?;");
				dStmt.setLong(1, now.getTime());
				dStmt.setString(2, playerName);
				dStmt.setString(3, Bukkit.getPlayer(playerUUID).getAddress().getAddress().getHostAddress());
				dStmt.setString(4, playerUUID.toString());
				dStmt.executeUpdate();
				dStmt.close();
				Bukkit.getLogger().log(Level.INFO, "[RP] PlayerInfo data updated for " + playerName);

				// if this player has MORE than 1 row in the
				// table
			} else if (rowCount > 1) {
				int counter = 1;
				PreparedStatement zStmt = connection
						.prepareStatement("SELECT * FROM rp_PlayerInfo WHERE UUID = ? ORDER BY ID ASC;");
				zStmt.setString(1, playerUUID.toString());
				ResultSet zResult = zStmt.executeQuery();
				while (zResult.next()) {
					// The first row is our valid one - update
					// it!
					if (counter == 1) {
						PreparedStatement dStmt = connection
								.prepareStatement("UPDATE `rp_PlayerInfo` SET LastSeen=?, PlayerName=? WHERE UUID=?;");
						dStmt.setLong(1, now.getTime());
						dStmt.setString(2, playerName);
						dStmt.setString(3, playerUUID.toString());
						dStmt.executeUpdate();
						dStmt.close();

						Bukkit.getLogger().log(Level.INFO,
								"[RP] PlayerInfo data [row " + zResult.getInt("ID") + "] updated for " + playerName);
						// All further rows are invalid, delete
						// them!
					} else if (counter > 1) {
						PreparedStatement dStmt = connection
								.prepareStatement("DELETE FROM `rp_PlayerInfo` WHERE ID = ? LIMIT 1;");
						dStmt.setInt(1, zResult.getInt("ID"));
						dStmt.executeUpdate();
						dStmt.close();
						Bukkit.getLogger().log(Level.INFO,
								"[RP] PlayerInfo dupe row cleanup (name change?)! Deleted row " + zResult.getInt("ID"));
					}

					counter++;
				}
				zStmt.close();

			}
			connection.close();
		} catch (SQLException e) {
			getLogger().log(Level.SEVERE,
					"Cant work with DB updatePlayerInfoOnJoin for " + playerName + " because: " + e.getMessage());
		}
	}

	// Add Runic Eye
	private void placeRunicEye(Location loc, Player p) {
		String locString = loc.getWorld().getName() + "." + loc.getBlockX() + "." + loc.getBlockY() + "."
				+ loc.getBlockZ();

		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
		try {
			Connection connection = MySQL.openConnection();

			String simpleProc = "{ call Add_Runic_Eye(?) }";
			CallableStatement cs = connection.prepareCall(simpleProc);
			cs.setString("Loc_param", locString);
			cs.executeUpdate();

			cs.close();
			connection.close();
			p.sendMessage("Runic Eye successfully created.");

		} catch (SQLException z) {
			getLogger().log(Level.SEVERE, "Failed RP.placeRunicEye " + loc.toString() + "- " + z.getMessage());
			p.sendMessage("Runic Eye creation failed..");
		}

	}

	// Add Prayer Book
	private void placePrayerBook(Location loc, Player p) {
		String locString = loc.getWorld().getName() + "." + loc.getBlockX() + "." + loc.getBlockY() + "."
				+ loc.getBlockZ();

		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
		try {
			Connection connection = MySQL.openConnection();

			String simpleProc = "{ call Add_Prayer_Book(?) }";
			CallableStatement cs = connection.prepareCall(simpleProc);
			cs.setString("Loc_param", locString);
			cs.executeUpdate();

			cs.close();
			connection.close();
			p.sendMessage("PrayerBook successfully created.");

		} catch (SQLException z) {
			getLogger().log(Level.SEVERE, "Failed RP.placePrayerBook " + loc.toString() + "- " + z.getMessage());
			p.sendMessage("Prayer Book creation failed..");
		}

	}

	// Add Runic Eye
	static void loadPrayerBooks() {
		prayerBooks.clear();

		// remove the eyes before we add them again
		for (Entity item : prayerBookEntities.keySet()) {
			item.remove();
		}

		prayerBookEntities.clear();

		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);

		try {
			Connection connection = MySQL.openConnection();
			Statement statement = connection.createStatement();
			ResultSet bookResult = statement.executeQuery("SELECT * FROM rp_PrayerBooks;");
			if (!bookResult.isBeforeFirst()) {
				// No results
				// do nothing
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
						"sc Tried to load Book settings, but couldn't find them in the DB!");
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
						"sc This is a critical problem; Prayer Books will not work :(");

				connection.close();
				return;
			} else {
				// results found!
				while (bookResult.next()) {

					String[] locParts = bookResult.getString("Location").split("[\\x2E]");
					Location targetLoc = new Location(Bukkit.getWorld(locParts[0]), Integer.parseInt(locParts[1]),
							Integer.parseInt(locParts[2]), Integer.parseInt(locParts[3]));

					Item item;

					if (bookResult.getString("Type").equals("Paper")) {
						item = targetLoc.getWorld().dropItemNaturally(targetLoc, new ItemStack(Material.PAPER));
					} else {
						item = targetLoc.getWorld().dropItemNaturally(targetLoc, new ItemStack(Material.BOOK));
					}

					item.setCustomName(bookResult.getString("Name"));
					item.setCustomNameVisible(true);
					item.setPickupDelay(90000);
					item.setVelocity(new Vector(0, 0, 0));
					item.teleport(targetLoc.add(0.5, 1, 0.5));

					prayerBookEntities.put(item, bookResult.getString("Name"));

					RunicParadise.prayerBooks.put(targetLoc.add(-0.5, -1, -0.5),
							new String[] { bookResult.getString("Name"), // 0
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

				connection.close();
			}

		} catch (SQLException z) {
			Bukkit.getLogger().log(Level.SEVERE, "Failed RP.loadPrayerBook " + z.getMessage());
		}

	}

	// Add Runic Eye
	static void loadRunicEyes() {
		runicEyes.clear();

		// remove the eyes before we add them again
		for (Entity item : runicEyeEntities.keySet()) {
			item.remove();
		}

		runicEyeEntities.clear();

		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
		StringBuilder eyeList = new StringBuilder();
		try {
			Connection connection = MySQL.openConnection();
			Statement statement = connection.createStatement();
			ResultSet eyeResult = statement.executeQuery("SELECT * FROM rp_RunicEyes;");
			if (!eyeResult.isBeforeFirst()) {
				// No results
				// do nothing
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
						"sc Tried to load Eye settings, but couldn't find them in the DB!");
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
						"sc This is a critical problem; Runic Eyes will not work :(");

				connection.close();
				return;
			} else {
				// results found!
				while (eyeResult.next()) {

					String[] locParts = eyeResult.getString("Location").split("[\\x2E]");
					Location targetLoc = new Location(Bukkit.getWorld(locParts[0]), Integer.parseInt(locParts[1]),
							Integer.parseInt(locParts[2]), Integer.parseInt(locParts[3]));

					Item item = targetLoc.getWorld().dropItemNaturally(targetLoc, new ItemStack(Material.ENDER_EYE));

					item.setCustomName(eyeResult.getString("Name"));
					item.setCustomNameVisible(true);
					item.setPickupDelay(90000);
					item.setVelocity(new Vector(0, 0, 0));
					item.teleport(targetLoc.add(0.5, 1, 0.5));

					runicEyeEntities.put(item, eyeResult.getString("Name"));

					RunicParadise.runicEyes.put(targetLoc.add(-0.5, -1, -0.5),
							new String[] { eyeResult.getString("Name"), item.getUniqueId().toString(),
									eyeResult.getString("Message") });
					eyeList.append(eyeResult.getString("Name")).append(". ");
				}
				connection.close();
			}
		} catch (SQLException z) {
			Bukkit.getLogger().log(Level.SEVERE, "Failed Faith.faithSettings " + z.getMessage());
		}

	}

	// Maintain table of player info
	private void updatePlayerInfoOnQuit(String name, UUID pUUID) {
		final Date now = new Date();
		final String playerName = name;
		final UUID playerUUID = pUUID;

		Bukkit.getServer().getScheduler().runTaskAsynchronously(instance, () -> {
			MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
			Connection connection = MySQL.openConnection();

			try {

				PreparedStatement statement = connection
						.prepareStatement("UPDATE `rp_PlayerInfo` SET LastSeen=? WHERE UUID=?;");
				statement.setLong(1, now.getTime());
				statement.setString(2, playerUUID.toString());
				statement.executeUpdate();
				statement.close();
				Bukkit.getLogger().log(Level.INFO, "[RP] PlayerInfo data updated for " + playerName);
				connection.close();

			} catch (SQLException e) {
				getLogger().log(Level.SEVERE, "Cant work with DB updatePlayerInfoOnquit for " + playerName
						+ " because: " + e.getMessage());
			}
		});
	}

	private boolean setupPermissions() {
		RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager()
				.getRegistration(net.milkbowl.vault.permission.Permission.class);
		perms = rsp.getProvider();
		return perms != null;
	}

	private boolean setupEconomy() {
		RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager()
				.getRegistration(net.milkbowl.vault.economy.Economy.class);
		if (economyProvider != null) {
			economy = economyProvider.getProvider();
		}

		return (economy != null);
	}

	static void showRunicCarnivalMenu(Player p) {
		Inventory carnivalMenu = Bukkit.createInventory(null, 45,
				ChatColor.DARK_RED + "" + ChatColor.BOLD + "Runic Carnival Menu");

		ItemMeta meta;
		ArrayList<String> mainLore = new ArrayList<>();
		mainLore.add(ChatColor.YELLOW + "The Runic Carnival is where you'll");
		mainLore.add(ChatColor.YELLOW + "find minigames, mazes, parkours,");
		mainLore.add(ChatColor.YELLOW + "and arenas. Click the blocks below");
		mainLore.add(ChatColor.YELLOW + "to join in the fun!");

		ItemStack main = new ItemStack(Material.BEACON, 1);
		meta = main.getItemMeta();
		meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Carnival Menu");
		meta.setLore(mainLore);
		main.setItemMeta(meta);

		putGlassOnInventory(carnivalMenu, main);

		ItemStack infoCenter = createHead(
				ChatColor.RESET + "" + ChatColor.BOLD + "" + ChatColor.DARK_PURPLE + "Info Center",
				"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjM4YzUzZTY2ZjI4Y2YyYzdmYjE1MjNjOWU1ZGUxYWUwY2Y0ZDdhMWZhZjU1M2U3NTI0OTRhOGQ2ZDJlMzIifX19");

		ItemStack prizeCabin = createHead(
				ChatColor.RESET + "" + ChatColor.BOLD + "" + ChatColor.LIGHT_PURPLE + "Prize Cabin",
				"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzJmMDQ4OWNhMTI2YTZlOWY5YWZhNTllYjQ5MWIxODUzMzk1YjU4MmI0NTRmYzJhZDQ4MDI3MjI2MjUyZDEyMSJ9fX0=");

		ItemStack mazes = createHead(ChatColor.RESET + "" + ChatColor.BOLD + "" + ChatColor.RED + "Mazes",
				"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2ZlOWFmMTBlZjM4MjI5ZjM4MzFkYjhmNTJmZGZhMWY5ODllOGNmNGYzNWI0ZTkyMTA0ZmZmZjcyODIyMCJ9fX0=");

		ItemStack adventureIslands = createHead(
				ChatColor.RESET + "" + ChatColor.BOLD + "" + ChatColor.DARK_GREEN + "Adventure Islands",
				"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTY4MTIxZWU0NmI1MGIwYWJkYjUzYzVhYmJkMTExYmEyYmY5MzY1MWMwN2Q0NzIzYWU5YWYzNzE5NTljIn19fQ==");

		ItemStack explorersLeague = createHead(
				ChatColor.RESET + "" + ChatColor.BOLD + "" + ChatColor.YELLOW + "Explorer's League",
				"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjFkZDRmZTRhNDI5YWJkNjY1ZGZkYjNlMjEzMjFkNmVmYTZhNmI1ZTdiOTU2ZGI5YzVkNTljOWVmYWIyNSJ9fX0=");

		ItemStack parkours = createHead(ChatColor.RESET + "" + ChatColor.BOLD + "" + ChatColor.DARK_RED + "Parkours",
				"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNThmZTI1MWE0MGU0MTY3ZDM1ZDA4MWMyNzg2OWFjMTUxYWY5NmI2YmQxNmRkMjgzNGQ1ZGM3MjM1ZjQ3NzkxZCJ9fX0=");

		ItemStack battleTower = createHead(ChatColor.RESET + "" + ChatColor.BOLD + "" + ChatColor.AQUA + "Battle Tower",
				"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWFlMzg1NWY5NTJjZDRhMDNjMTQ4YTk0NmUzZjgxMmE1OTU1YWQzNWNiY2I1MjYyN2VhNGFjZDQ3ZDMwODEifX19");

		ItemStack creationZone = createHead(
				ChatColor.RESET + "" + ChatColor.BOLD + "" + ChatColor.BLUE + "Creative Contest",
				"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTM0YTU5MmE3OTM5N2E4ZGYzOTk3YzQzMDkxNjk0ZmMyZmI3NmM4ODNhNzZjY2U4OWYwMjI3ZTVjOWYxZGZlIn19fQ==");

		carnivalMenu.setItem(19, infoCenter);
		carnivalMenu.setItem(20, prizeCabin);
		carnivalMenu.setItem(21, mazes);
		carnivalMenu.setItem(22, parkours);
		carnivalMenu.setItem(31, explorersLeague);
		carnivalMenu.setItem(32, adventureIslands);
		carnivalMenu.setItem(33, battleTower);
		carnivalMenu.setItem(34, creationZone);

		ItemStack token = new ItemStack(Material.SUNFLOWER);
		ItemMeta meta1 = token.getItemMeta();
		ArrayList<String> tokenLore = new ArrayList<>();

		tokenLore.add(ChatColor.GRAY + "Current Tokens Available");
		tokenLore.add(ChatColor.GREEN + "" + +new RunicPlayerBukkit(p.getUniqueId()).getPlayerTokenBalance());
		tokenLore.add(ChatColor.GRAY + "Lifetime Tokens:");
		tokenLore.add(ChatColor.YELLOW + "" + +new RunicPlayerBukkit(p.getUniqueId()).getPlayerLifetimeTokens());
		meta1.setLore(tokenLore);
		meta1.setDisplayName(ChatColor.RESET + "" + ChatColor.BLUE + "Runic Carnival Tokens");
		token.setItemMeta(meta1);

		carnivalMenu.setItem(17, token);

		p.openInventory(carnivalMenu);

	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerDeath(PlayerDeathEvent event) {
		if (event.getEntity() != null) {

			final PlayerDeathEvent innerEvent = event;

			Bukkit.getServer().getScheduler().runTaskAsynchronously(instance, () -> {
				Player player = innerEvent.getEntity();
				String cause = "";
				String killerName = "";

				if (RunicGateway.getLastEntityDamager(player) != null) {
					Entity killer = RunicGateway.getLastEntityDamager(player);

					if (killer instanceof Player) {
						Player k = (Player) killer;
						killerName = k.getName();

						cause = "PLAYER_KILL";
					} else {
						// Not a player... so maybe a mob :)
						killerName = killer.getType().toString();
						EntityDamageEvent ede = player.getLastDamageCause();
						DamageCause dc = ede.getCause();
						cause = dc.toString();
					}
				} else {
					// death not caused by an entity; entity check
					// returned null
					EntityDamageEvent ede = player.getLastDamageCause();
					DamageCause dc = ede.getCause();
					cause = dc.toString();
					killerName = cause;
				}

				String uuid = player.getUniqueId().toString();
				String name = player.getName();
				String loc = innerEvent.getEntity().getLocation().toString();

				// String pvp =
				// event.getEntity().getKiller().toString();
				long time = new Date().getTime();

				MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
				Connection connection = MySQL.openConnection();
				// do the insert
				try {
					Statement statement = connection.createStatement();
					statement.executeUpdate(
							"INSERT INTO rp_PlayerDeath (`PlayerName`, `UUID`, `TimeStamp`, `CauseOfDeath`, `Killer`, `Location`) VALUES "
									+ "('" + name + "', '" + uuid + "', '" + time + "', '" + cause + "', '"
									+ killerName + "', '" + loc + "');");
				} catch (SQLException err) {
					getLogger().log(Level.SEVERE,
							"Cant create new row PlayerDeath for " + name + " because: " + err.getMessage());
				}
				// close the connection
				try {
					connection.close();
				} catch (SQLException err) {
					getLogger().log(Level.SEVERE,
							"Cant close conn PlayerDeath for " + name + " because: " + err.getMessage());
				}
				player = null;
			}); // end run task async
		}
	}

	private static void showRunicCarnivalMenu_Explorers(Player p, int page) {
		Inventory exploreMenu = Bukkit.createInventory(null, 54,
				ChatColor.BOLD + "" + ChatColor.GOLD + "Explorer's League");

		ItemMeta meta;
		List<String> mainLore = new ArrayList<>();
		mainLore.add(ChatColor.YELLOW + "x");
		mainLore.add(ChatColor.YELLOW + "x");
		mainLore.add(ChatColor.YELLOW + "x");
		mainLore.add(ChatColor.YELLOW + "x");

		int countLocs = 1;
		int slotTracker = 18;
		ChatColor textColor;
		short iconColor;
		List<String> iconLore = new ArrayList<>();
		// get all active explore locs and loop through them to build the
		// inventory
		for (int i : RunicParadise.explorerLocationsReversed.values()) {
			// To paginate, only allow 27 locations per page
			if ((page == 1 && countLocs < 28)
					|| (page > 1 && countLocs > ((page - 1) * 27) && countLocs < ((page * 27) + 1))) {

				if (RunicParadise.playerProfiles.get(p.getUniqueId()).checkPlayerExploration(i)) {
					// player completed this location
					textColor = ChatColor.DARK_GREEN;
					iconColor = 13;
					iconLore.add(ChatColor.GREEN + "You found this location already!");
					iconLore.add(" ");
				} else {
					// player has not completed this location
					textColor = ChatColor.DARK_RED;
					iconColor = 14;
					iconLore.add(ChatColor.RED + "You still need to find this spot!");
					iconLore.add(" ");

					if (RunicParadise.explorerDifficulties.get(i) == 1) {
						iconLore.add(ChatColor.GRAY + "Difficulty: " + ChatColor.RED + "Hard");
					} else if (RunicParadise.explorerDifficulties.get(i) >= 2
							&& RunicParadise.explorerDifficulties.get(i) <= 4) {
						iconLore.add(ChatColor.GRAY + "Difficulty: " + ChatColor.YELLOW + "Medium");
					} else {
						iconLore.add(ChatColor.GRAY + "Difficulty: " + ChatColor.GREEN + "Easy");
					}

					iconLore.add(" ");
					iconLore.add(ChatColor.GRAY + "Reward: " + ChatColor.GREEN + RunicParadise.explorerRewards.get(i)
							+ " Tokens");
				}

				ItemStack icon = new ItemStack(RunicUtilities.getGlassColor(iconColor));
				meta = icon.getItemMeta();

				meta.setDisplayName(textColor + RunicParadise.explorerIDs.get(i));
				meta.setLore(iconLore);
				icon.setItemMeta(meta);
				exploreMenu.setItem(slotTracker, icon);

				// reset the lore array
				iconLore.clear();

				slotTracker++;
			}

			countLocs++;
		}

		// handle pagination

		int lastPage = page - 1;
		int nextPage = page + 1;

		ItemStack backIcon = new ItemStack(Material.GOLDEN_CARROT, 1);
		ItemStack nextIcon = new ItemStack(Material.FEATHER, 1);
		ItemMeta tempMeta = backIcon.getItemMeta();
		tempMeta.setDisplayName(ChatColor.RESET + "" + ChatColor.RED + "Back to Page " + lastPage);
		backIcon.setItemMeta(tempMeta);

		tempMeta = nextIcon.getItemMeta();
		tempMeta.setDisplayName(ChatColor.RESET + "" + ChatColor.RED + "Go to Page " + nextPage);
		nextIcon.setItemMeta(tempMeta);

		if (RunicParadise.explorerLocationsReversed.size() > 27 && page == 1) {
			exploreMenu.setItem(51, nextIcon);
		} else if (RunicParadise.explorerLocationsReversed.size() > 54 && page == 2) {
			exploreMenu.setItem(51, nextIcon);
		} else if (RunicParadise.explorerLocationsReversed.size() > 81 && page == 3) {
			exploreMenu.setItem(51, nextIcon);
		} else if (RunicParadise.explorerLocationsReversed.size() > 108 && page == 4) {
			exploreMenu.setItem(51, nextIcon);
		}

		if (page != 1) {
			exploreMenu.setItem(47, backIcon);
		}

		/*
		 * if (page == 1) {
		 * 
		 * } else if (slotTracker < 80 || / 27 == page)) { // if slotTracker <
		 * 44, it couldnt fill the menu, so this should be // the last page //
		 * worst case scenario it filled it perfectly and the next page will //
		 * be blank :-/ // ... which is why the OR is added.
		 * exploreMenu.setItem(47, backIcon); } else { exploreMenu.setItem(47,
		 * backIcon); exploreMenu.setItem(51, nextIcon); }
		 */

		// handle menu main stuff

		ItemStack main = new ItemStack(Material.BEACON, 1);
		meta = main.getItemMeta();
		meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Carnival Menu");
		main.setItemMeta(meta);

		putGlassOnInventory(exploreMenu, main);

		p.openInventory(exploreMenu);
	}

	private static void showRunicCarnivalMenu_BattleTower(Player p) {
		Inventory carnivalMenu = Bukkit.createInventory(null, 45,
				ChatColor.DARK_RED + "" + ChatColor.BOLD + "Runic Carnival - Arenas");

		ItemMeta meta;
		ArrayList<String> mainLore = new ArrayList<>();
		mainLore.add(ChatColor.YELLOW + "x");
		mainLore.add(ChatColor.YELLOW + "x");
		mainLore.add(ChatColor.YELLOW + "x");
		mainLore.add(ChatColor.YELLOW + "x");

		ItemStack main = new ItemStack(Material.BEACON, 1);
		meta = main.getItemMeta();
		meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Carnival Menu");
		meta.setLore(mainLore);
		main.setItemMeta(meta);

		putGlassOnInventory(carnivalMenu, main);

		ArrayList<String> pvpLore = new ArrayList<>();
		pvpLore.add(ChatColor.RED + "PvP");
		ArrayList<String> pveLore = new ArrayList<>();
		pveLore.add(ChatColor.AQUA + "No PvP (PvE)");

		ItemStack paintball;
		paintball = new ItemStack(Material.SNOWBALL);
		meta = paintball.getItemMeta();
		meta.setDisplayName("Paintball");
		meta.setLore(pvpLore);
		paintball.setItemMeta(meta);

		ItemStack blockhunt;
		blockhunt = new ItemStack(Material.BOOKSHELF);
		meta = blockhunt.getItemMeta();
		meta.setDisplayName("Blockhunt");
		meta.setLore(pvpLore);
		blockhunt.setItemMeta(meta);

		ItemStack ctf;
		ctf = new ItemStack(Material.BLACK_BANNER);
		meta = ctf.getItemMeta();
		meta.setDisplayName("Capture the Flag");
		meta.setLore(pvpLore);
		ctf.setItemMeta(meta);

		ItemStack pvpArena;
		pvpArena = new ItemStack(Material.IRON_AXE);
		meta = pvpArena.getItemMeta();
		meta.setDisplayName("PvP Arena");
		meta.setLore(pvpLore);
		pvpArena.setItemMeta(meta);

		ItemStack spleef;
		spleef = new ItemStack(Material.GOLDEN_SHOVEL);
		meta = spleef.getItemMeta();
		meta.setDisplayName("Spleef");
		meta.setLore(pvpLore);
		spleef.setItemMeta(meta);

		ItemStack mobarena;
		mobarena = new ItemStack(Material.ZOMBIE_HEAD);
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

	private static long getPlayerMazeLastCompletion(Player p, int puzzleID) {
		long lastCom = 0;

		try {
			MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
			Connection connection = MySQL.openConnection();
			Statement statement = connection.createStatement();
			ResultSet mcResult = statement.executeQuery("SELECT * FROM rp_RunicGameCompletions WHERE UUID='"
					+ p.getUniqueId() + "' AND GameID=" + puzzleID + ";");
			if (!mcResult.isBeforeFirst()) {
				// No results
				// do nothing
				statement.close();
				connection.close();

				return 0;

			} else {
				// results found!
				while (mcResult.next()) {

					lastCom = mcResult.getLong("LastCompletion");

				}
				statement.close();
				connection.close();
			}

		} catch (SQLException z) {
			Bukkit.getLogger().log(Level.SEVERE, "Failed getting puzzle completion count -" + z.getMessage());
		}

		return lastCom;
	}

	static int getPlayerMazeCompletionCount(Player p, int puzzleID) {
		int completionCount = 0;

		try {
			MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
			Connection dbCon = MySQL.openConnection();
			Statement dbStmt = dbCon.createStatement();
			ResultSet mcResult = dbStmt.executeQuery("SELECT * FROM rp_RunicGameCompletions WHERE UUID='"
					+ p.getUniqueId() + "' AND GameID=" + puzzleID + ";");
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
			Bukkit.getLogger().log(Level.SEVERE, "Failed getting puzzle completion count -" + z.getMessage());
		}

		return completionCount;
	}

	static int getPlayerDistinctMazeCompletionCount(Player p) {
		int completionCount = 0;

		try {
			MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
			Connection connection = MySQL.openConnection();
			Statement statement = connection.createStatement();
			ResultSet mcResult = statement.executeQuery(
					"SELECT COUNT(rp_RunicGameCompletions.ID) AS Count FROM rp_RunicGameCompletions INNER JOIN rp_RunicGames on rp_RunicGameCompletions.GameID = rp_RunicGames.ID "
							+ "WHERE rp_RunicGameCompletions.UUID='" + p.getUniqueId()
							+ "' AND rp_RunicGames.GameType = 'Maze';");
			if (!mcResult.isBeforeFirst()) {
				// No results
				// do nothing
				statement.close();
				connection.close();

				return 0;

			} else {
				// results found!
				while (mcResult.next()) {

					completionCount = mcResult.getInt("Count");

				}
				statement.close();
				connection.close();
			}

		} catch (SQLException z) {
			Bukkit.getLogger().log(Level.SEVERE,
					"Failed getting getPlayerDistinctMazeCompletionCount count -" + z.getMessage());
		}

		return completionCount;
	}

	public static void mazeMigration() {
		int count = 0;

		try {
			MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
			Connection connection = MySQL.openConnection();
			Statement statement = connection.createStatement();
			ResultSet mcResult = statement.executeQuery("SELECT * FROM rp_PlayerInfo;");

			Statement zStmt = connection.createStatement();

			if (!mcResult.isBeforeFirst()) {
				// No results
				// do nothing
				statement.close();
				connection.close();

			} else {
				// results found!
				while (mcResult.next()) {
					if (mcResult.getInt("HedgeMazeCompletions") > 0) {
						zStmt.executeUpdate("INSERT INTO rp_RunicGameCompletions (UUID, GameID, Count) " + "VALUES ('"
								+ mcResult.getString("UUID") + "', 1, " + mcResult.getInt("HedgeMazeCompletions")
								+ ");");
						count++;
					}

					if (mcResult.getInt("IceMazeCompletions") > 0) {
						zStmt.executeUpdate("INSERT INTO rp_RunicGameCompletions (UUID, GameID, Count) " + "VALUES ('"
								+ mcResult.getString("UUID") + "', 2, " + mcResult.getInt("IceMazeCompletions") + ");");
						count++;
					}

					if (mcResult.getInt("XmasMazeCompletions") > 0) {
						zStmt.executeUpdate("INSERT INTO rp_RunicGameCompletions (UUID, GameID, Count) " + "VALUES ('"
								+ mcResult.getString("UUID") + "', 3, " + mcResult.getInt("XmasMazeCompletions")
								+ ");");
						count++;
					}

					if (mcResult.getInt("JungleMazeCompletions") > 0) {
						zStmt.executeUpdate("INSERT INTO rp_RunicGameCompletions (UUID, GameID, Count) " + "VALUES ('"
								+ mcResult.getString("UUID") + "', 4, " + mcResult.getInt("JungleMazeCompletions")
								+ ");");
						count++;
					}

					if (mcResult.getInt("FrostMazeCompletions") > 0) {
						zStmt.executeUpdate("INSERT INTO rp_RunicGameCompletions (UUID, GameID, Count) " + "VALUES ('"
								+ mcResult.getString("UUID") + "', 5, " + mcResult.getInt("FrostMazeCompletions")
								+ ");");
						count++;
					}

					if (mcResult.getInt("DungeonMazeCompletions") > 0) {
						zStmt.executeUpdate("INSERT INTO rp_RunicGameCompletions (UUID, GameID, Count) " + "VALUES ('"
								+ mcResult.getString("UUID") + "', 6, " + mcResult.getInt("DungeonMazeCompletions")
								+ ");");
						count++;
					}

					if (mcResult.getInt("AdventureParkour") > 0) {
						zStmt.executeUpdate("INSERT INTO rp_RunicGameCompletions (UUID, GameID, Count) " + "VALUES ('"
								+ mcResult.getString("UUID") + "', 7, " + mcResult.getInt("AdventureParkour") + ");");
						count++;
					}
				}

			}

			statement.close();
			zStmt.close();
			connection.close();
		} catch (SQLException z) {
			Bukkit.getLogger().log(Level.SEVERE, "Failed getting puzzle completion count -" + z.getMessage());
		}

		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "sc Created " + count + " new records");

	}

	private static void showSpawnSkynetMenu(Player p) {
		Inventory skynetMenu = Bukkit.createInventory(null, 45,
				ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "SkyNet Warp Orb");

		ItemMeta meta;
		ArrayList<String> mainLore = new ArrayList<>();
		mainLore.add(ChatColor.YELLOW + "The Spawn SkyNet is a travel");
		mainLore.add(ChatColor.YELLOW + "network that gets you to places");
		mainLore.add(ChatColor.YELLOW + "around spawn quickly! Click a");
		mainLore.add(ChatColor.YELLOW + "location below to warp there.");

		ItemStack main = new ItemStack(Material.BEACON);
		meta = main.getItemMeta();
		meta.setDisplayName(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "SkyNet Warp Menu");
		meta.setLore(mainLore);
		main.setItemMeta(meta);

		ItemStack slot1 = new ItemStack(Material.COMPASS);
		meta = slot1.getItemMeta();
		meta.setDisplayName(" ");
		slot1.setItemMeta(meta);
		ItemStack slot2 = new ItemStack(Material.COMPASS);
		meta = slot2.getItemMeta();
		meta.setDisplayName(" ");
		slot2.setItemMeta(meta);
		ItemStack slot3 = new ItemStack(Material.COMPASS);
		meta = slot3.getItemMeta();
		meta.setDisplayName(" ");
		slot3.setItemMeta(meta);
		ItemStack slot4 = new ItemStack(Material.COMPASS);
		meta = slot4.getItemMeta();
		meta.setDisplayName(" ");
		slot4.setItemMeta(meta);
		ItemStack slot6 = new ItemStack(Material.COMPASS);
		meta = slot6.getItemMeta();
		meta.setDisplayName(" ");
		slot6.setItemMeta(meta);
		ItemStack slot7 = new ItemStack(Material.COMPASS);
		meta = slot7.getItemMeta();
		meta.setDisplayName(" ");
		slot7.setItemMeta(meta);
		ItemStack slot8 = new ItemStack(Material.COMPASS);
		meta = slot8.getItemMeta();
		meta.setDisplayName(" ");
		slot8.setItemMeta(meta);
		ItemStack slot9 = new ItemStack(Material.COMPASS);
		meta = slot9.getItemMeta();
		meta.setDisplayName(" ");
		slot9.setItemMeta(meta);

		skynetMenu.setItem(0, slot1);
		skynetMenu.setItem(1, slot2);
		skynetMenu.setItem(2, slot3);
		skynetMenu.setItem(3, slot4);
		skynetMenu.setItem(4, main);
		skynetMenu.setItem(5, slot6);
		skynetMenu.setItem(6, slot7);
		skynetMenu.setItem(7, slot8);
		skynetMenu.setItem(8, slot9);

		ItemStack library = new ItemStack(Material.WRITABLE_BOOK);
		meta = library.getItemMeta();
		meta.setDisplayName(
				ChatColor.RESET + "" + ChatColor.BOLD + "" + ChatColor.LIGHT_PURPLE + "Runic Public Library");
		library.setItemMeta(meta);

		ItemStack jobs = new ItemStack(Material.ANVIL);
		meta = jobs.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + "" + ChatColor.BOLD + "" + ChatColor.LIGHT_PURPLE + "Jobs Tower");
		jobs.setItemMeta(meta);

		ItemStack donation = new ItemStack(Material.LEATHER_CHESTPLATE);
		meta = donation.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + "" + ChatColor.BOLD + "" + ChatColor.LIGHT_PURPLE + "Donation Center");
		donation.setItemMeta(meta);

		ItemStack townsShops = new ItemStack(Material.EMERALD);
		meta = townsShops.getItemMeta();
		meta.setDisplayName(
				ChatColor.RESET + "" + ChatColor.BOLD + "" + ChatColor.LIGHT_PURPLE + "Town & Shop Portals");
		townsShops.setItemMeta(meta);

		ItemStack hub = new ItemStack(Material.ELYTRA);
		meta = hub.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + "" + ChatColor.BOLD + "" + ChatColor.LIGHT_PURPLE + "Hub & Skyblock");
		hub.setItemMeta(meta);

		ItemStack faith = new ItemStack(Material.NETHER_STAR);
		meta = faith.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + "" + ChatColor.BOLD + "" + ChatColor.LIGHT_PURPLE + "Faith Cathedral");
		faith.setItemMeta(meta);

		ItemStack mining = new ItemStack(Material.DIAMOND_PICKAXE);
		meta = mining.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + "" + ChatColor.BOLD + "" + ChatColor.LIGHT_PURPLE + "Mining World");
		mining.setItemMeta(meta);

		ItemStack petshop = new ItemStack(Material.LEAD);
		meta = petshop.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + "" + ChatColor.BOLD + "" + ChatColor.LIGHT_PURPLE + "Pet Shop");
		petshop.setItemMeta(meta);

		ItemStack graves = new ItemStack(Material.TOTEM_OF_UNDYING);
		meta = graves.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + "" + ChatColor.BOLD + "" + ChatColor.LIGHT_PURPLE + "Death & Graves");
		graves.setItemMeta(meta);

		ItemStack wild = new ItemStack(Material.DARK_OAK_SAPLING);
		meta = wild.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + "" + ChatColor.BOLD + "" + ChatColor.LIGHT_PURPLE + "Wilderness Portals");
		wild.setItemMeta(meta);

		ItemStack tavern = new ItemStack(Material.POTION);
		meta = tavern.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + "" + ChatColor.BOLD + "" + ChatColor.LIGHT_PURPLE + "Soda Brewery");
		tavern.setItemMeta(meta);

		ItemStack crates = new ItemStack(Material.CHEST);
		meta = crates.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + "" + ChatColor.BOLD + "" + ChatColor.LIGHT_PURPLE + "Runic Crates");
		crates.setItemMeta(meta);

		ItemStack jail = new ItemStack(Material.IRON_BARS);
		meta = jail.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + "" + ChatColor.BOLD + "" + ChatColor.LIGHT_PURPLE + "Prison " + ChatColor.RESET + "" + ChatColor.DARK_RED + "[PvP]");
		jail.setItemMeta(meta);

		ItemStack colosseum = new ItemStack(Material.DIAMOND_SWORD);
		meta = colosseum.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + "" + ChatColor.BOLD + "" + ChatColor.LIGHT_PURPLE + "Colosseum " + ChatColor.RESET + "" + ChatColor.DARK_RED + "[PvP]");
		colosseum.setItemMeta(meta);

		skynetMenu.setItem(30, library);
		skynetMenu.setItem(19, jobs);
		skynetMenu.setItem(20, donation);
		skynetMenu.setItem(21, townsShops);

		skynetMenu.setItem(22, wild);

		skynetMenu.setItem(23, hub);
		skynetMenu.setItem(24, faith);
		skynetMenu.setItem(25, mining);
		skynetMenu.setItem(31, petshop);
		skynetMenu.setItem(32, graves);

		skynetMenu.setItem(29, crates);
		skynetMenu.setItem(33, tavern);

		skynetMenu.setItem(39, colosseum);
		skynetMenu.setItem(41, jail);
		/*
		 * ItemStack token = new ItemStack(Material.DOUBLE_PLANT, 1, (short) 0);
		 * ItemMeta meta1 = token.getItemMeta(); ArrayList<String> tokenLore =
		 * new ArrayList<>();
		 * 
		 * tokenLore.add(ChatColor.GRAY + "Current Tokens Available");
		 * tokenLore.add(ChatColor.GREEN + "" + +new
		 * RunicPlayerBukkit(p.getUniqueId()) .getPlayerTokenBalance());
		 * tokenLore.add(ChatColor.GRAY + "Lifetime Tokens:");
		 * tokenLore.add(ChatColor.YELLOW + "" + +new
		 * RunicPlayerBukkit(p.getUniqueId()) .getPlayerLifetimeTokens());
		 * meta1.setLore(tokenLore); meta1.setDisplayName(ChatColor.RESET + "" +
		 * ChatColor.BLUE + "Runic Carnival Tokens"); token.setItemMeta(meta1);
		 */
		p.openInventory(skynetMenu);

	}

	private void updatePlayerCMIRank(Player p) {
		String primaryGroup = perms.getPrimaryGroup(p);
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "cmi rankset " +p.getName() + " " + primaryGroup);
	}

	static ItemStack createHead(String name, String data) {
		ItemStack item = new ItemStack(Material.PLAYER_HEAD);
		item.setDurability((short) 3);
		SkullMeta headMeta = (SkullMeta) item.getItemMeta();
		headMeta.setDisplayName(name);
		GameProfile profile = new GameProfile(UUID.randomUUID(), null);
		profile.getProperties().put("textures", new Property("textures", data, "signed"));
		try {
			Field profileField = headMeta.getClass().getDeclaredField("profile");
			profileField.setAccessible(true);
			profileField.set(headMeta, profile);
		} catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException ignored) {}
		item.setItemMeta(headMeta);
		return item;
	}

	// Calculate amount of EXP needed to level up
	public static int getExpToLevelUp(int level){
		if(level <= 15){
			return 2*level+7;
		} else if(level <= 30){
			return 5*level-38;
		} else {
			return 9*level-158;
		}
	}

	// Calculate total experience up to a level
	public static int getExpAtLevel(int level){
		if(level <= 16){
			return (int) (Math.pow(level,2) + 6*level);
		} else if(level <= 31){
			return (int) (2.5*Math.pow(level,2) - 40.5*level + 360.0);
		} else {
			return (int) (4.5*Math.pow(level,2) - 162.5*level + 2220.0);
		}
	}

	// Calculate player's current EXP amount
	public static int getPlayerExp(Player player){
		int exp = 0;
		int level = player.getLevel();

		// Get the amount of XP in past levels
		exp += getExpAtLevel(level);

		// Get amount of XP towards next level
		exp += Math.round(getExpToLevelUp(level) * player.getExp());

		return exp;
	}

	// Give or take EXP
	public static int changePlayerExp(Player player, int exp){
		// Get player's current exp
		int currentExp = getPlayerExp(player);

		// Reset player's current exp to 0
		player.setExp(0);
		player.setLevel(0);

		// Give the player their exp back, with the difference
		int newExp = currentExp + exp;
		player.giveExp(newExp);

		// Return the player's new exp amount
		return newExp;
	}

	private void refreshCMIRank (Player p) {
		String groupName = perms.getPrimaryGroup(p);
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "cmi rankset " + p.getName() + " " + groupName);
	}

	private void eliminateDefaultGroup (Player p) {
		String groupName = perms.getPrimaryGroup(p);
		if (groupName.equalsIgnoreCase("default")) {
			perms.playerAddGroup("", Bukkit.getOfflinePlayer(p.getUniqueId()), "Ghost");
			Bukkit.getLogger().log(Level.INFO, "Found default group! Changing " + p.getName() + " to Ghost!");
		}
	}

	private void checkMcmmoUserCreated (Player p) {
		if (RunicDB.callSP_Count_McmmoUsers(p.getUniqueId().toString()) < 1) {
			//No Mcmmo user found
			RunicMessaging.sendMessage(p, RunicFormat.SYSTEM, "Could not find your McMMO profile. Trying to create one...");
			Bukkit.getLogger().log(Level.SEVERE, "Could not find McMMO profile. Trying to create one via RunicParadise. " + p.getName());

			int newID = RunicDB.callSP_Add_McmmoUser(p.getName(), p.getUniqueId().toString());
			Bukkit.getLogger().log(Level.SEVERE, "New user ID " + newID);

			if (newID != 0) {
				if(RunicDB.callSP_Add_McmmoUserRecords(newID)) {
					RunicMessaging.sendMessage(p, RunicFormat.SYSTEM, "McMMO profile successfully created. Please logout for 10 seconds and come back.");
					Bukkit.getLogger().log(Level.SEVERE, "New McMMO profile created successfully. " + p.getName());
				} else {
					RunicMessaging.sendMessage(p, RunicFormat.SYSTEM, "McMMO profile creation failed. We'll try again next time. :(");
					Bukkit.getLogger().log(Level.SEVERE, "New McMMO profile creation FAILED during AddMcmmoUserRecords. " + p.getName());
				}
			}



		}

	}


	public void registerSlimefunItems() {

		NamespacedKey categoryId = new NamespacedKey(RunicParadise.getInstance(), "RunicSpecialties");
		me.mrCookieSlime.Slimefun.cscorelib2.item.CustomItem categoryItem =
				new me.mrCookieSlime.Slimefun.cscorelib2.item.CustomItem(
						Material.ANVIL, "&4Runic Specialties", "", "&a> Click to open");

		Category category = new Category(categoryId, categoryItem);

		category.register();

		SlimefunItem baronDiamondPart = new SlimefunItem(
				category,
				new SlimefunItemStack("MIST_INFUSED_DIAMOND", Material.DIAMOND, "&eMist-Infused Diamond"),
				RecipeType.ANCIENT_ALTAR,
				new ItemStack[] {
						SlimefunItems.RUNE_WATER,    SlimefunItems.SYNTHETIC_DIAMOND,    SlimefunItems.RUNE_AIR,
						SlimefunItems.SILVER_DUST,          new ItemStack(Material.LIGHT_BLUE_DYE, 1),    SlimefunItems.SILVER_DUST,
						SlimefunItems.RUNE_AIR,    SlimefunItems.SYNTHETIC_DIAMOND,        SlimefunItems.RUNE_WATER
				});
		baronDiamondPart.register(this);

		SlimefunItem baronDiamond2Part = new SlimefunItem(
				category,
				new SlimefunItemStack("LAVA_INFUSED_DIAMOND", Material.DIAMOND, "&eLava-Infused Diamond"),
				RecipeType.ANCIENT_ALTAR,
				new ItemStack[] {
						SlimefunItems.RUNE_FIRE,    SlimefunItems.SYNTHETIC_DIAMOND,    SlimefunItems.RUNE_EARTH,
						SlimefunItems.SILVER_DUST,          new ItemStack(Material.ORANGE_DYE, 1),    SlimefunItems.SILVER_DUST,
						SlimefunItems.RUNE_EARTH,    SlimefunItems.SYNTHETIC_DIAMOND,        SlimefunItems.RUNE_FIRE
				});
		baronDiamond2Part.register(this);

		SlimefunItem baronEmeraldPart = new SlimefunItem(
				category,
				new SlimefunItemStack("MIST_INFUSED_EMERALD", Material.EMERALD, "&eMist-Infused Emerald"),
				RecipeType.ANCIENT_ALTAR,
				new ItemStack[] {
						SlimefunItems.RUNE_WATER,    SlimefunItems.SYNTHETIC_EMERALD,    SlimefunItems.RUNE_AIR,
						SlimefunItems.ZINC_DUST,          new ItemStack(Material.LIGHT_BLUE_DYE, 1),    SlimefunItems.ZINC_DUST,
						SlimefunItems.RUNE_AIR,    SlimefunItems.SYNTHETIC_EMERALD,        SlimefunItems.RUNE_WATER
				});
		baronEmeraldPart.register(this);

		SlimefunItem baronEmerald2Part = new SlimefunItem(
				category,
				new SlimefunItemStack("LAVA_INFUSED_EMERALD", Material.EMERALD, "&eLava-Infused Emerald"),
				RecipeType.ANCIENT_ALTAR,
				new ItemStack[] {
						SlimefunItems.RUNE_FIRE,    SlimefunItems.SYNTHETIC_EMERALD,    SlimefunItems.RUNE_EARTH,
						SlimefunItems.ZINC_DUST,          new ItemStack(Material.ORANGE_DYE, 1),    SlimefunItems.ZINC_DUST,
						SlimefunItems.RUNE_EARTH,    SlimefunItems.SYNTHETIC_EMERALD,        SlimefunItems.RUNE_FIRE
				});
		baronEmerald2Part.register(this);

		SlimefunItem baronSapphirePart = new SlimefunItem(
				category,
				new SlimefunItemStack("MIST_INFUSED_SAPPHIRE", Material.LIGHT_BLUE_DYE, "&eMist-Infused Sapphire"),
				RecipeType.ANCIENT_ALTAR,
				new ItemStack[] {
						SlimefunItems.RUNE_WATER,    SlimefunItems.SYNTHETIC_SAPPHIRE,    SlimefunItems.RUNE_AIR,
						SlimefunItems.ALUMINUM_DUST,          new ItemStack(Material.LIGHT_BLUE_DYE, 1),    SlimefunItems.ALUMINUM_DUST,
						SlimefunItems.RUNE_AIR,    SlimefunItems.SYNTHETIC_SAPPHIRE,        SlimefunItems.RUNE_WATER
				});
		baronSapphirePart.register(this);

		SlimefunItem baronSapphire2Part = new SlimefunItem(
				category,
				new SlimefunItemStack("LAVA_INFUSED_SAPPHIRE", Material.LAPIS_LAZULI, "&eLava-Infused Sapphire"),
				RecipeType.ANCIENT_ALTAR,
				new ItemStack[] {
						SlimefunItems.RUNE_FIRE,    SlimefunItems.SYNTHETIC_SAPPHIRE,    SlimefunItems.RUNE_EARTH,
						SlimefunItems.ALUMINUM_DUST,          new ItemStack(Material.ORANGE_DYE, 1),    SlimefunItems.ALUMINUM_DUST,
						SlimefunItems.RUNE_EARTH,    SlimefunItems.SYNTHETIC_SAPPHIRE,        SlimefunItems.RUNE_FIRE
				});
		baronSapphire2Part.register(this);

		SlimefunItem baronMistyTopaz = new SlimefunItem(
				category,
				new SlimefunItemStack("PRISMATIC_TOPAZ", Material.LIGHT_BLUE_DYE, "&ePrismatic Topaz"),
				RecipeType.MAGIC_WORKBENCH,
				new ItemStack[] {
						SlimefunItems.RUNE_RAINBOW,    new SlimefunItemStack("MIST_INFUSED_EMERALD", Material.EMERALD, "&eMist-Infused Emerald"),    SlimefunItems.RUNE_RAINBOW,
						null,          new ItemStack(Material.LIGHT_BLUE_DYE, 1),   null,
						new SlimefunItemStack("MIST_INFUSED_DIAMOND", Material.DIAMOND, "&eMist-Infused Diamond"),    SlimefunItems.RUNE_RAINBOW,   new SlimefunItemStack("MIST_INFUSED_SAPPHIRE", Material.LIGHT_BLUE_DYE, "&eMist-Infused Sapphire")
				});
		baronMistyTopaz.register(this);

		SlimefunItem baronMistyCitrine = new SlimefunItem(
				category,
				new SlimefunItemStack("PRISMATIC_CITRINE", Material.ORANGE_DYE, "&ePrismatic Citrine"),
				RecipeType.MAGIC_WORKBENCH,
				new ItemStack[] {
						SlimefunItems.RUNE_RAINBOW,    new SlimefunItemStack("LAVA_INFUSED_EMERALD", Material.EMERALD, "&eLava-Infused Emerald"),    SlimefunItems.RUNE_RAINBOW,
						null,          new ItemStack(Material.YELLOW_DYE, 1),   null,
						new SlimefunItemStack("LAVA_INFUSED_DIAMOND", Material.DIAMOND, "&eLava-Infused Diamond"),    SlimefunItems.RUNE_RAINBOW,   new SlimefunItemStack("LAVA_INFUSED_SAPPHIRE", Material.LAPIS_LAZULI, "&eLava-Infused Sapphire")
				});
		baronMistyCitrine.register(this);

		SlimefunItem baronJewel = new SlimefunItem(
				category,
				new SlimefunItemStack("BARON_JEWEL", Borderlands.specialLootDrops("BaronGem", null)),
				RecipeType.MAGIC_WORKBENCH,
				new ItemStack[] {
						new ItemStack(Material.NETHER_STAR),   new SlimefunItemStack("PRISMATIC_CITRINE", Material.ORANGE_DYE, "&ePrismatic Citrine"),    new ItemStack(Material.NETHER_STAR),
						SlimefunItems.RUNE_ENDER,          new ItemStack(Material.LIGHT_BLUE_DYE, 1),   SlimefunItems.RUNE_ENDER,
						new ItemStack(Material.NETHER_STAR),   new SlimefunItemStack("PRISMATIC_TOPAZ", Material.LIGHT_BLUE_DYE, "&ePrismatic Topaz"),   new ItemStack(Material.NETHER_STAR)
				});
		baronJewel.register(this);

		SlimefunItem baronIngot1 = new SlimefunItem(
				category,
				new SlimefunItemStack("EMPOWERED_SILVER_INGOT", Borderlands.specialLootDrops("BaronIngot1", null)),
				RecipeType.SMELTERY,
				new ItemStack[] {
						Borderlands.specialLootDrops("BaronMetal", null), Borderlands.specialLootDrops("BaronMetal", null),   Borderlands.specialLootDrops("BaronMetal", null),
						SlimefunItems.NETHER_ICE,   SlimefunItems.NETHER_ICE,   SlimefunItems.NETHER_ICE,
						Borderlands.specialLootDrops("BaronMetal", null), Borderlands.specialLootDrops("BaronMetal", null),   Borderlands.specialLootDrops("BaronMetal", null)
				});
		baronIngot1.register(this);

		SlimefunItem baronIngot2 = new SlimefunItem(
				category,
				new SlimefunItemStack("CARVED_SILVER_INGOT", Borderlands.specialLootDrops("BaronIngot2", null)),
				RecipeType.SMELTERY,
				new ItemStack[] {
						SlimefunItems.BLISTERING_INGOT_2, null, SlimefunItems.BLISTERING_INGOT_2,
						SlimefunItems.ENRICHED_NETHER_ICE, SlimefunItems.ENRICHED_NETHER_ICE, SlimefunItems.ENRICHED_NETHER_ICE,
						SlimefunItems.BLISTERING_INGOT_2, null, SlimefunItems.BLISTERING_INGOT_2
				});
		baronIngot2.register(this);

	}

}
