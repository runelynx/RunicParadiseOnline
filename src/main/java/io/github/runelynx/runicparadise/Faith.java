package io.github.runelynx.runicparadise;

import io.github.runelynx.runicuniverse.RunicMessaging;
import io.github.runelynx.runicuniverse.RunicMessaging.RunicFormat;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Crops;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.sql.*;
import java.util.Date;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;

import static org.bukkit.Bukkit.getLogger;

/**
 * @author andrew
 *
 */
public class Faith {

	public static final int NETHER_NETHERBORN_TIMING = 3000;

	private String playerUUID;
	private UUID trueUUID;
	private String playerName;
	private String primaryFaithName;
	private HashMap<String, Integer> faithLevels = new HashMap<String, Integer>();

	private static Plugin instance = RunicParadise.getInstance();

	public Faith(UUID pUUID) {
		this.retrievePlayerData(pUUID);
	}

	public static void deactivateFaiths() {
		for (Player p : Bukkit.getOnlinePlayers()) {

			if (p.hasPermission("rp.faith.user")) {
				p.sendMessage(ChatColor.GRAY + "[" + ChatColor.BLUE + "Runic" + ChatColor.DARK_AQUA + "Faith"
						+ ChatColor.GRAY + "] " + ChatColor.BLUE + "Faith system deactivated!");
			}

		}
		RunicParadise.faithMap.clear();
		RunicParadise.faithSettingsMap.clear();
		RunicParadise.protectedPlayers.clear();
		getLogger().info("RP Faith: Faith map has been cleared.");
	}

	private UUID getUUID() {
		return this.trueUUID;
	}

	/** Shown in the faith picker for a faith staff have closed (/rs Faith). */
	static ItemStack closedFaith(String faith) {
		ItemStack it = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
		ItemMeta m = it.getItemMeta();
		m.setDisplayName(ChatColor.GRAY + faith + " is closed for now");
		m.setLore(Collections.singletonList(ChatColor.DARK_GRAY + "Players already following it keep it."));
		it.setItemMeta(m);
		return it;
	}

	public static void showFaithMenu(Player p) {
		Inventory faithInventory = Bukkit.createInventory(null, 45,
				ChatColor.BLUE + "Runic " + ChatColor.DARK_AQUA + "Faith " + ChatColor.DARK_GRAY + "Selection Menu");

		ItemStack sun = new ItemStack(Material.RED_SANDSTONE);
		ItemMeta sunMeta = sun.getItemMeta();
		sunMeta.setDisplayName("Sun");
		sun.setItemMeta(sunMeta);

		ItemStack moon = new ItemStack(Material.END_STONE);
		ItemMeta moonMeta = moon.getItemMeta();
		moonMeta.setDisplayName("Moon");
		moon.setItemMeta(moonMeta);

		ItemStack flame = new ItemStack(Material.ORANGE_STAINED_GLASS);
		ItemMeta flameMeta = flame.getItemMeta();
		flameMeta.setDisplayName("Fire");
		flame.setItemMeta(flameMeta);

		ItemStack water = new ItemStack(Material.PRISMARINE);
		ItemMeta waterMeta = water.getItemMeta();
		waterMeta.setDisplayName("Water");
		water.setItemMeta(waterMeta);

		ItemStack air = new ItemStack(Material.WHITE_STAINED_GLASS);
		ItemMeta airMeta = air.getItemMeta();
		airMeta.setDisplayName("Air");
		air.setItemMeta(airMeta);

		ItemStack earth = new ItemStack(Material.PODZOL);
		ItemMeta earthMeta = earth.getItemMeta();
		earthMeta.setDisplayName("Earth");
		earth.setItemMeta(earthMeta);

		ItemStack aether = new ItemStack(Material.QUARTZ_BLOCK);
		ItemMeta aetherMeta = aether.getItemMeta();
		aetherMeta.setDisplayName("Aether");
		aether.setItemMeta(aetherMeta);

		ItemStack nether = new ItemStack(Material.NETHERRACK);
		ItemMeta netherMeta = nether.getItemMeta();
		netherMeta.setDisplayName("Nether");
		nether.setItemMeta(netherMeta);

		ItemStack nature = new ItemStack(Material.OAK_LEAVES);
		ItemMeta natureMeta = nature.getItemMeta();
		natureMeta.setDisplayName("Nature");
		nature.setItemMeta(natureMeta);

		ItemStack tech = new ItemStack(Material.BEACON);
		ItemMeta techMeta = tech.getItemMeta();
		techMeta.setDisplayName("Tech");
		tech.setItemMeta(techMeta);

		ItemStack time = new ItemStack(Material.CLOCK);
		ItemMeta timeMeta = time.getItemMeta();
		timeMeta.setDisplayName("Time");
		time.setItemMeta(timeMeta);

		ItemStack fate = new ItemStack(Material.ENDER_PEARL);
		ItemMeta fateMeta = fate.getItemMeta();
		fateMeta.setDisplayName("Fate");
		fate.setItemMeta(fateMeta);

		ItemStack stars = new ItemStack(Material.BLAZE_POWDER);
		ItemMeta starsMeta = stars.getItemMeta();
		starsMeta.setDisplayName("Stars");
		stars.setItemMeta(starsMeta);

		ItemStack blocked = new ItemStack(Material.BARRIER);
		ItemMeta blockedMeta = blocked.getItemMeta();
		blockedMeta.setDisplayName("This faith is not available to you");
		blocked.setItemMeta(blockedMeta);

		if (!FaithConfig.faithOpen("Sun")) {
			faithInventory.setItem(3, closedFaith("Sun")); // staff closed this faith
		} else if (p.hasPermission("rp.faith.sun")) {
			faithInventory.setItem(3, sun);
		} else {
			faithInventory.setItem(3, blocked);
		}
		if (!FaithConfig.faithOpen("Moon")) {
			faithInventory.setItem(4, closedFaith("Moon")); // staff closed this faith
		} else if (p.hasPermission("rp.faith.moon")) {
			faithInventory.setItem(4, moon);
		} else {
			faithInventory.setItem(4, blocked);
		}
		if (!FaithConfig.faithOpen("Fire")) {
			faithInventory.setItem(5, closedFaith("Fire")); // staff closed this faith
		} else if (p.hasPermission("rp.faith.fire")) {
			faithInventory.setItem(5, flame);
		} else {
			faithInventory.setItem(5, blocked);
		}

		if (!FaithConfig.faithOpen("Water")) {
			faithInventory.setItem(12, closedFaith("Water")); // staff closed this faith
		} else if (p.hasPermission("rp.faith.water")) {
			faithInventory.setItem(12, water);
		} else {
			faithInventory.setItem(12, blocked);
		}
		if (!FaithConfig.faithOpen("Air")) {
			faithInventory.setItem(13, closedFaith("Air")); // staff closed this faith
		} else if (p.hasPermission("rp.faith.air")) {
			faithInventory.setItem(13, air);
		} else {
			faithInventory.setItem(13, blocked);
		}
		if (!FaithConfig.faithOpen("Earth")) {
			faithInventory.setItem(14, closedFaith("Earth")); // staff closed this faith
		} else if (p.hasPermission("rp.faith.earth")) {
			faithInventory.setItem(14, earth);
		} else {
			faithInventory.setItem(14, blocked);
		}

		if (!FaithConfig.faithOpen("Aether")) {
			faithInventory.setItem(21, closedFaith("Aether")); // staff closed this faith
		} else if (p.hasPermission("rp.faith.aether")) {
			faithInventory.setItem(21, aether);
		} else {
			faithInventory.setItem(21, blocked);
		}
		if (!FaithConfig.faithOpen("Nether")) {
			faithInventory.setItem(22, closedFaith("Nether")); // staff closed this faith
		} else if (p.hasPermission("rp.faith.nether")) {
			faithInventory.setItem(22, nether);
		} else {
			faithInventory.setItem(22, blocked);
		}
		if (!FaithConfig.faithOpen("Nature")) {
			faithInventory.setItem(23, closedFaith("Nature")); // staff closed this faith
		} else if (p.hasPermission("rp.faith.nature")) {
			faithInventory.setItem(23, nature);
		} else {
			faithInventory.setItem(23, blocked);
		}

		if (!FaithConfig.faithOpen("Tech")) {
			faithInventory.setItem(30, closedFaith("Tech")); // staff closed this faith
		} else if (p.hasPermission("rp.faith.tech")) {
			faithInventory.setItem(30, tech);
		} else {
			faithInventory.setItem(30, blocked);
		}
		if (!FaithConfig.faithOpen("Fate")) {
			faithInventory.setItem(31, closedFaith("Fate")); // staff closed this faith
		} else if (p.hasPermission("rp.faith.fate")) {
			faithInventory.setItem(31, fate);
		} else {
			faithInventory.setItem(31, blocked);
		}
		if (!FaithConfig.faithOpen("Time")) {
			faithInventory.setItem(32, closedFaith("Time")); // staff closed this faith
		} else if (p.hasPermission("rp.faith.time")) {
			faithInventory.setItem(32, time);
		} else {
			faithInventory.setItem(32, blocked);
		}

		if (!FaithConfig.faithOpen("Stars")) {
			faithInventory.setItem(40, closedFaith("Stars")); // staff closed this faith
		} else if (p.hasPermission("rp.faith.stars")) {
			faithInventory.setItem(40, stars);
		} else {
			faithInventory.setItem(40, blocked);
		}

		p.openInventory(faithInventory);

	}

	public String listPowers(boolean showAll) {

		if (!showAll) {
			Bukkit.getPlayer(this.getUUID())
					.sendMessage(ChatColor.GRAY + "[" + ChatColor.BLUE + "Runic" + ChatColor.DARK_AQUA + "Faith"
							+ ChatColor.GRAY + "] " + ChatColor.BLUE + "Listing powers granted by your faiths:");
		} else {
			Bukkit.getPlayer(this.getUUID())
					.sendMessage(ChatColor.GRAY + "[" + ChatColor.BLUE + "Runic" + ChatColor.DARK_AQUA + "Faith"
							+ ChatColor.GRAY + "] " + ChatColor.BLUE + "Listing all powers available:");
		}

		// Iterate through all available faiths... check each one to see if
		// player has it equipped. If they do, list those powers.
		for (Entry<String, String[]> entry : RunicParadise.faithSettingsMap.entrySet()) {
			String faithName = entry.getKey();

			boolean show = false;
			String faithColor = "";

			if (this.checkEquippedFaithLevel(faithName, 0)) {
				// Check if player has the faith equipped
				show = true;
				faithColor = ChatColor.DARK_GREEN + "";

			} else if (showAll) {
				// allpowers command was used, so show all!
				show = true;
				faithColor = ChatColor.GRAY + "";
			} else {
				// only showing player what they have equipped, and this isnt
				// one of them... so dont show!
				show = false;
			}

			if (show) {
				// Player has this faith equipped, so list its powers!

				Bukkit.getPlayer(this.getUUID())
						.sendMessage(ChatColor.GRAY + " " + faithColor + faithName + ChatColor.BLUE + " Powers:");

				MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);

				try {
					Connection connection = MySQL.openConnection();
					Statement statement = connection.createStatement();
					ResultSet powerResult = statement.executeQuery("SELECT * FROM rp_MasterPowers WHERE FaithName='"
							+ faithName + "' ORDER BY RequiredLevel ASC;");
					if (!powerResult.isBeforeFirst()) {
						// No results
						// do nothing
						Bukkit.getPlayer(this.getUUID())
								.sendMessage("Oops! Couldn't find any powers for the " + faithName + " faith.");
						connection.close();
					} else {
						// results found!
						while (powerResult.next()) {
							// Found powers for a faith that player has
							// equipped!

							ChatColor levelColor;
							if (this.checkEquippedFaithLevel(faithName, powerResult.getInt("RequiredLevel"))) {
								// player qualifies for this power
								levelColor = ChatColor.GREEN;
							} else {
								// player does NOT qualify for this power
								levelColor = ChatColor.GRAY;
							}
							// One line per power; hover shows the description (this block was commented out)
							String desc = powerResult.getString("Description");
							TextComponent line = new TextComponent(TextComponent.fromLegacyText(ChatColor.BLUE + "   Level "
									+ levelColor + powerResult.getString("RequiredLevel") + ChatColor.WHITE + ": "
									+ levelColor + powerResult.getString("PowerName")));
							if (desc != null && !desc.trim().isEmpty()) {
								line.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
										net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
										new net.md_5.bungee.api.chat.hover.content.Text(ChatColor.GRAY + desc)));
							}
							Bukkit.getPlayer(this.getUUID()).spigot().sendMessage(line);

						}
						statement.close();
						connection.close();
					}

				} catch (SQLException z) {
					getLogger().log(Level.SEVERE,
							"Failed Faith.listPowers when trying to get powers for a faith: " + z.getMessage());
					return "Error: Database Failure";
				}
			} // end if checking whether player has the faith equipped
			else {
				// Player does not have this faith equipped
			}
		} // end for looping through all possible faiths
		Bukkit.getPlayer(this.getUUID())
				.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "Hover mouse over power name for details");
		return "Success";

	} // end method

	public static void getPowerSettings() {
		FaithConfig.load(); // staff power tuning (/rs Faith); defaults apply for anything not overridden
		RunicParadise.powerReqsMap.clear();

		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
		String powerList = "";
		try {
			Connection connection = MySQL.openConnection();
			Statement statement = connection.createStatement();
			ResultSet faithResult = statement.executeQuery("SELECT * FROM rp_MasterPowers;");
			if (!faithResult.isBeforeFirst()) {
				// No results
				// do nothing
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
						"sc Tried to load Power settings, but couldn't find them in the DB!");
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
						"sc This is a critical problem; Powers will not work :(");

				connection.close();
				return;
			} else {
				// results found!
				while (faithResult.next()) {
					RunicParadise.powerReqsMap.put(faithResult.getString("PowerName"),
							faithResult.getInt("RequiredLevel"));
					powerList += faithResult.getString("PowerName") + ". ";
				}

				connection.close();
			}

		} catch (SQLException z) {
			getLogger().log(Level.SEVERE, "Failed Faith.powerSettings " + z.getMessage());
		}
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "sc Loaded Power Req Levels: " + powerList);
	}

	public static void getFaithSettings() {
		RunicParadise.faithSettingsMap.clear();

		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
		StringBuilder faithList = new StringBuilder();
		try {
			Connection connection = MySQL.openConnection();
			Statement statement = connection.createStatement();
			ResultSet faithResult = statement.executeQuery("SELECT * FROM rp_MasterFaiths;");
			if (!faithResult.isBeforeFirst()) {
				// No results
				// do nothing
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
						"sc Tried to load Faith settings, but couldn't find them in the DB!");
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
						"sc This is a critical problem; Faiths will not work :(");

				connection.close();
				return;
			} else {
				// results found!
				while (faithResult.next()) {
					RunicParadise.faithSettingsMap.put(faithResult.getString("FaithName"),
							new String[] { faithResult.getString("FaithName"), faithResult.getString("ChatPrefix"),
									faithResult.getString("Permission"), faithResult.getString("Description"),
									faithResult.getString("MaxLevel"), faithResult.getString("CastMessage"),
									faithResult.getString("ChatPrefix2") });
					faithList.append(faithResult.getString("faithName")).append(". ");
				}

				connection.close();
			}

		} catch (SQLException z) {
			getLogger().log(Level.SEVERE, "Failed Faith.faithSettings " + z.getMessage());
		}
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "sc Loaded Faiths: " + faithList);
	}

	public boolean checkEquippedFaithLevel(String faithName, int level) {
		// Using this map means faith must be "equipped" -- NOT inactive!
		if (this.faithLevels.containsKey(faithName)) {
			if (this.faithLevels.get(faithName) >= level) {
				// ensure the faith is ACTIVE (cached; this used to load the player from MySQL on every call)
				return faithName.equals(this.primaryFaithName);
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	@SuppressWarnings("deprecation")
	public String enableFaith(final UUID nUUID, final String faithName) {

		RunicPlayerBukkit targetPlayer = new RunicPlayerBukkit(nUUID);

		if (!RunicParadise.faithSettingsMap.containsKey(faithName)) {
			return "That is not a valid Faith";
		} else if (!Bukkit.getPlayer(nUUID).hasPermission(RunicParadise.faithSettingsMap.get(faithName)[2])) {
			// check if player has the permission required for this faith
			return "Player is not eligible for that Faith";
		} else {
			// it's a valid faith name!
			// check if player already has a record for this faith in the DB
			MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
			try {
				Date now = new Date();
				Connection connection = MySQL.openConnection();
				Statement statement = connection.createStatement();
				ResultSet faithResult = statement.executeQuery("SELECT * FROM rp_PlayerFaiths WHERE UUID = '"
						+ nUUID.toString() + "' AND FaithName = '" + faithName + "';");
				if (!faithResult.isBeforeFirst()) {
					// No results
					// add the faith
					statement.executeUpdate(
							"INSERT INTO rp_PlayerFaiths (UUID, Active, FaithName, Level, Timestamp) VALUES ('"
									+ nUUID.toString() + "', 1, '" + faithName + "', 0, " + now.getTime() + ");");

					targetPlayer.setActiveFaith(faithName);

					statement.close();

					connection.close();

					this.retrievePlayerData(nUUID);
					return "Success";
				} else {
					// results found!

					targetPlayer.setActiveFaith(faithName);

					statement.close();

					connection.close();

					this.retrievePlayerData(nUUID);
					return "Success";
				}

			} catch (SQLException z) {
				getLogger().log(Level.SEVERE, "Failed Faith.enableFaith " + nUUID.toString() + "- " + z.getMessage());
				return "Database Failure :(";
			}
		}
	}

	public String getPlayerStats(UUID nUUID, UUID senderUUID) {
		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
		try {
			Connection connection = MySQL.openConnection();
			Statement statement = connection.createStatement();
			ResultSet faithResult = statement.executeQuery(
					"SELECT * FROM rp_PlayerFaiths WHERE UUID = '" + nUUID.toString() + "' ORDER BY FaithName ASC;");
			if (!faithResult.isBeforeFirst()) {
				getLogger().log(Level.INFO, "No Faiths found for " + Bukkit.getPlayer(nUUID).getDisplayName());

				// No results
				// do nothing
				connection.close();
				return "No faiths found.";
			} else {
				// results found!
				boolean activeStarted = false;
				boolean inactiveStarted = false;

				RunicMessaging.sendMessage(Bukkit.getPlayer(senderUUID), RunicMessaging.RunicFormat.FAITH,
						"Displaying faith stats for " + Bukkit.getOfflinePlayer(nUUID).getName());

				Bukkit.getPlayer(senderUUID)
						.sendMessage(ChatColor.BLUE + "Faith power level: " + ChatColor.GRAY
								+ new RunicPlayerBukkit(this.getUUID()).getFaithPowerLevel() + ChatColor.DARK_GRAY + ""
								+ ChatColor.ITALIC + " (Combined total faith level)");

				String activeFaith = new RunicPlayerBukkit(nUUID).getActiveFaith(); // once, not per row
				while (faithResult.next()) {

					String color;
					if (faithResult.getString("FaithName").equals(activeFaith)) {
						color = ChatColor.GREEN + "";
					} else {
						color = ChatColor.DARK_RED + "";
					}

					Bukkit.getPlayer(senderUUID)
							.sendMessage(displayLevelBar(((double) faithResult.getInt("Level") / Integer.parseInt(
									RunicParadise.faithSettingsMap.get(faithResult.getString("FaithName"))[4])) * 50)
									+ " " + color + faithResult.getString("FaithName") + " " + ChatColor.GRAY
									+ faithResult.getInt("Level") + "/"
									+ RunicParadise.faithSettingsMap.get(faithResult.getString("FaithName"))[4]);

					/*
					 * 
					 * if (faithResult.getString("FaithName").equals( new
					 * RunicPlayerBukkit(nUUID).getActiveFaith()) &&
					 * !activeStarted) { // Print Active header
					 * 
					 * Bukkit.getPlayer(senderUUID).sendMessage( ChatColor.GREEN
					 * + "Active Faiths ✔");
					 * 
					 * activeStarted = true;
					 * 
					 * } else if (faithResult.getString("FaithName").equals( new
					 * RunicPlayerBukkit(nUUID).getActiveFaith()) &&
					 * activeStarted) { Bukkit.getPlayer(senderUUID)
					 * .sendMessage( displayLevelBar(((double) faithResult
					 * .getInt("Level") / Integer
					 * .parseInt(RunicParadise.faithSettingsMap.get(faithResult
					 * .getString("FaithName"))[4])) * 50) + " " +
					 * ChatColor.AQUA + faithResult .getString("FaithName") +
					 * " " + ChatColor.GRAY + faithResult.getInt("Level") + "/"
					 * + RunicParadise.faithSettingsMap.get(faithResult
					 * .getString("FaithName"))[4]);
					 * 
					 * } else if (!faithResult.getString("FaithName").equals(
					 * new RunicPlayerBukkit(nUUID).getActiveFaith()) &&
					 * !inactiveStarted) { // Print Inactive header
					 * 
					 * Bukkit.getPlayer(senderUUID).sendMessage(
					 * ChatColor.DARK_RED + "Inactive Faiths ✗");
					 * Bukkit.getPlayer(senderUUID) .sendMessage(
					 * displayLevelBar(((double) faithResult .getInt("Level") /
					 * Integer
					 * .parseInt(RunicParadise.faithSettingsMap.get(faithResult
					 * .getString("FaithName"))[4])) * 50) + " " +
					 * ChatColor.AQUA + faithResult .getString("FaithName") +
					 * " " + ChatColor.GRAY + faithResult.getInt("Level") + "/"
					 * + RunicParadise.faithSettingsMap.get(faithResult
					 * .getString("FaithName"))[4]); inactiveStarted = true;
					 * double test = (((double) faithResult.getInt("Level") /
					 * Integer .parseInt(RunicParadise.faithSettingsMap
					 * .get(faithResult.getString("FaithName"))[4])) * 50);
					 * getLogger() .log(Level.INFO, Integer.toString(faithResult
					 * .getInt("Level")) + "." +
					 * RunicParadise.faithSettingsMap.get(faithResult
					 * .getString("FaithName"))[4] + "." +
					 * Double.toString(test)); } else if
					 * (!faithResult.getString("FaithName").equals( new
					 * RunicPlayerBukkit(nUUID).getActiveFaith()) &&
					 * inactiveStarted) {
					 * 
					 * Bukkit.getPlayer(senderUUID) .sendMessage(
					 * displayLevelBar(((double) faithResult .getInt("Level") /
					 * Integer
					 * .parseInt(RunicParadise.faithSettingsMap.get(faithResult
					 * .getString("FaithName"))[4])) * 50) + " " +
					 * ChatColor.AQUA + faithResult .getString("FaithName") +
					 * " " + ChatColor.GRAY + faithResult.getInt("Level") + "/"
					 * + RunicParadise.faithSettingsMap.get(faithResult
					 * .getString("FaithName"))[4]);
					 * 
					 * }
					 */
				}

				connection.close();
				return "Success";
			}

		} catch (SQLException z) {
			getLogger().log(Level.SEVERE,
					"Failed Faith.getPlayerStats for player " + nUUID.toString() + "- " + z.getMessage());
			return "Database failure";
		}

	}

	private String displayLevelBar(double input) {
		StringBuilder bar = new StringBuilder();
		bar.append(ChatColor.AQUA);
		int counter = 0;

		while (counter <= input) {
			bar.append("|");
			counter++;
		}

		bar.append(ChatColor.DARK_BLUE).append("|");
		counter++;

		bar.append(ChatColor.GRAY);
		while (counter <= 50) {
			bar.append("|");
			counter++;
		}

		return bar.toString();
	}

	public void retrievePlayerData(UUID nUUID) {
		this.playerUUID = nUUID.toString();
		this.trueUUID = nUUID;
		RunicPlayerBukkit rpb = new RunicPlayerBukkit(nUUID); // load once (was once per faith row)
		this.playerName = rpb.getPlayerName();

		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
		try {
			Connection connection = MySQL.openConnection();
			Statement statement = connection.createStatement();
			ResultSet faithResult = statement
					.executeQuery("SELECT * FROM rp_PlayerFaiths WHERE UUID = '" + nUUID.toString() + "';");
			if (!faithResult.isBeforeFirst()) {
				getLogger().log(Level.INFO, "No Faiths found for " + Bukkit.getPlayer(nUUID).getDisplayName());

				this.faithLevels.clear();

				// No results
				// do nothing
				connection.close();
			} else {
				// results found!

				// start fresh!
				this.faithLevels.clear();

				while (faithResult.next()) {
					this.faithLevels.put(faithResult.getString("FaithName"), faithResult.getInt("Level"));
					getLogger().log(Level.INFO, "Faiths - Added: " + Bukkit.getPlayer(nUUID).getDisplayName() + " - "
							+ faithResult.getString("faithName") + " L" + faithResult.getInt("Level"));
					this.primaryFaithName = rpb.getActiveFaith();
				}

				connection.close();
			}
		} catch (SQLException z) {
			getLogger().log(Level.SEVERE,
					"Failed Faith.retrievePlayerData for player " + nUUID.toString() + "- " + z.getMessage());
		}

		if (this.primaryFaithName != null) {
			resetFaithSlimefunPermissions(nUUID, this.primaryFaithName);
		}

	}

	public String setSkill(Player p, String adminName, String faithName, int newValue) {
		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);

		if (newValue < 0) {
			getLogger().log(Level.SEVERE, "Invalid newValue in Faith.setSkill - given " + newValue + " less than 0");
			return "Error NewValueLessThanZero";
		} else if (newValue > Integer.parseInt(RunicParadise.faithSettingsMap.get(faithName)[4])) {
			getLogger().log(Level.SEVERE,
					"Invalid newValue in Faith.setSkill - given " + newValue + " greater than max allowed");
			return "Error NewValueGreaterThanMaxLevelForThisFaith";
		}

		// Now update the object used by the map!
		if (this.faithLevels.containsKey(faithName)) {
			this.faithLevels.put(faithName, newValue);
			p.sendMessage(ChatColor.GRAY + "[" + ChatColor.BLUE + "Runic" + ChatColor.DARK_AQUA + "Faith"
					+ ChatColor.GRAY + "] " + ChatColor.BLUE + "Your " + faithName + ChatColor.BLUE
					+ " faith has been changed by " + adminName + "! " + ChatColor.AQUA
					+ this.faithLevels.get(faithName) + ChatColor.GRAY + "/"
					+ RunicParadise.faithSettingsMap.get(faithName)[4]);

		}

		try {
			Connection connection = MySQL.openConnection();

			PreparedStatement statement = connection.prepareStatement(
					"UPDATE rp_PlayerFaiths SET Level=" + newValue + " WHERE UUID = ? AND FaithName = ?");
			statement.setString(1, this.playerUUID);
			statement.setString(2, faithName);
			statement.executeUpdate();

			connection.close();
		} catch (SQLException e) {
			getLogger().log(Level.SEVERE, "Failed Powers.setSkill because: " + e.getMessage());
			return "Error DatabaseError or PlayerDoesntHaveThatFaith!";
		}

		return "Success";
	}

	public int getPrimaryFaithLevel() {
		if (this.faithLevels.get(this.getPrimaryFaith()) != null) {
			return this.faithLevels.get(this.getPrimaryFaith());
		} else {
			return 0;
		}
	}

	public boolean incrementSkill(Player p, String faithName) {
		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);

		// Update the hashmap first;
		if (!this.faithLevels.containsKey(faithName)) {
			// Given faith is invalid for this player, so stop here!
			return false;
		}
		// Never go past the faith's MaxLevel (players already above it keep their level, they just stop growing)
		int maxLevel = Integer.parseInt(RunicParadise.faithSettingsMap.get(faithName)[4]);
		if (this.faithLevels.get(faithName) >= maxLevel) {
			return false;
		}
		this.faithLevels.put(faithName, this.faithLevels.get(faithName) + 1);

		try {
			Connection connection = MySQL.openConnection();

			PreparedStatement statement = connection.prepareStatement(
					"UPDATE rp_PlayerFaiths SET Level=Level+1 WHERE UUID = ? AND FaithName = ? AND Level < ?");
			statement.setString(1, this.playerUUID);
			statement.setString(2, faithName);
			statement.setInt(3, maxLevel);
			statement.executeUpdate();

			connection.close();

			String msg1 = ChatColor.GRAY + "[" + ChatColor.BLUE + "Runic" + ChatColor.DARK_AQUA + "Faith"
					+ ChatColor.GRAY + "] " + ChatColor.BLUE + "Your " + faithName + ChatColor.BLUE
					+ " faith grows stronger! " + ChatColor.GREEN + this.faithLevels.get(faithName) + ChatColor.GRAY
					+ "/" + RunicParadise.faithSettingsMap.get(faithName)[4];
			ActionBars.send(p, ChatColor.translateAlternateColorCodes('&', msg1));

			if (this.faithLevels.get(faithName) == maxLevel) {
				for (Player q : Bukkit.getOnlinePlayers()) {

					q.sendMessage(ChatColor.GRAY + "[" + ChatColor.BLUE + "Runic" + ChatColor.DARK_AQUA + "Faith"
							+ ChatColor.GRAY + "] " + ChatColor.BLUE + p.getDisplayName() + ChatColor.BLUE
							+ " just maxxed their " + ChatColor.WHITE + faithName + ChatColor.BLUE + " faith!");
					q.getWorld().playSound(q.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 10, 1);

				}
			}

			return true;

		} catch (SQLException e) {
			getLogger().log(Level.SEVERE, "Failed Faith.incrementSkill because: " + e.getMessage());
			this.faithLevels.put(faithName, this.faithLevels.get(faithName) - 1);
			return false;
		}
	}

	/** True when the player's level meets the power's required level (>=, matching /faith powers). Unknown power = false. */
	static boolean hasPower(int level, String powerName) {
		Integer req = RunicParadise.powerReqsMap.get(powerName);
		return req != null && level >= req && FaithConfig.enabled(powerName); // staff can switch powers off (/rs Faith)
	}

	private static void runLater(Runnable r, long ticks) {
		// Bukkit API calls must run on the main thread (these used scheduleAsyncDelayedTask, which Paper rejects)
		Bukkit.getScheduler().runTaskLater(instance, r, ticks);
	}

	public static void tryCast_PlayerTookFallDamage(Player p) {
		if (!powersAllowed(p.getWorld())) {
			return; // no faith powers in RunicSky, its nether/end, or the Museum
		}
		Faith pfo = RunicParadise.faithMap.get(p.getUniqueId());
		if (pfo == null || pfo.getPrimaryFaith() == null) {
			return;
		}
		String faith = pfo.getPrimaryFaith();
		int level = pfo.getPrimaryFaithLevel();

		switch (faith) {

		case "Aether":
			if (hasPower(level, "Graceful Steps") && inMainWorld(p)) { // main overworld + Mining only
				// Cast Graceful Steps if player took fall damage
				pfo.castAether_GracefulSteps(p);
			}

			break;
		default:
			break;
		}

	}

	public static int tryCast_CropsAreGrowing(BlockGrowEvent event, Player p) {
		if (!powersAllowed(p.getWorld())) {
			return 0;
		}
		Faith pfo = RunicParadise.faithMap.get(p.getUniqueId());
		if (pfo == null || pfo.getPrimaryFaith() == null) {
			return 0;
		}
		String faith = pfo.getPrimaryFaith();
		int level = pfo.getPrimaryFaithLevel();

		switch (faith) {

		case "Nature":
			if (hasPower(level, "Wild Growth")) {
				//
				pfo.castNature_WildGrowth(event, p);
				return 1;
			}

			break;
		default:
			break;
		}
		return 0;

	}

	public static void tryCast_PlayerTookEntityDamage(final EntityDamageEvent event, Player p) {
		if (!powersAllowed(p.getWorld())) {
			return; // no faith powers in RunicSky, its nether/end, or the Museum
		}

		Faith pfo = RunicParadise.faithMap.get(p.getUniqueId());
		if (pfo == null || pfo.getPrimaryFaith() == null) {
			return;
		}
		String faith = pfo.getPrimaryFaith();
		int level = pfo.getPrimaryFaithLevel();
		final boolean daytime = isDay(p.getWorld());

		// Only damage dealt by an entity (melee, sweep, projectile, explosion) triggers these powers.
		// (Was "&&", which can never be true, so none of these powers had ever fired.)
		DamageCause cause = event.getCause();
		if (!(cause == DamageCause.ENTITY_EXPLOSION || cause == DamageCause.ENTITY_ATTACK
				|| cause == DamageCause.ENTITY_SWEEP_ATTACK || cause == DamageCause.PROJECTILE)) {
			return;
		}

		switch (faith) {

		case "Sun":
			if (hasPower(level, "Sunflare") && daytime) {

				// Cast Sunflare if player got hit and its daytime
				if (inMainWorld(p)) {
					pfo.castSun_Sunflare(p.getUniqueId(), p);
				}

			}

			break;
		case "Moon":
			if (hasPower(level, "Lunar Calm") && !daytime) {

				// Cast Lunar Calm if player got hit and its night (was mistakenly casting Sunflare)
				if (inMainWorld(p)) {
					pfo.castMoon_LunarCalm(p.getUniqueId(), p);
				}

			}
			break;
		case "Fire": // the DB faith name is "Fire" (this case used to say "Flame" and never matched)
			if (hasPower(level, "Unstable Embers")) {

				// Cast Unstable Embers if player got hit
				if (inMainWorld(p)) {
					pfo.castFlame_UnstableEmbers(p);
				}
			}

			break;
		case "Water":
			if (hasPower(level, "Arctic Frost")) {

				// Cast Unstable Embers if player got hit
				if (inMainWorld(p)) {
					pfo.castWater_ArcticFrost(p);
				}
			}

			if (hasPower(level, "Protective Bubble")) {

				// Cast Unstable Embers if player got hit
				if (inMainWorld(p)) {
					pfo.castWater_ProtectiveBubble(p);
				}
			}

			break;
		case "Air":
			if (hasPower(level, "Healing Breeze")) {

				// Cast Healing Breeze if player got hit
				if (inMainWorld(p)) {
					pfo.castAir_HealingBreeze(p);
				}
			}

			break;
		case "Time":
			if (hasPower(level, "Rewind") && inMainWorld(p)) { // main overworld + Mining only

				// Cast Rewind if this hit would kill the player
				pfo.castTime_Rewind(event, p);

			}

			break;
		case "Earth":
			if (hasPower(level, "Earthen Shield") && inMainWorld(p)) { // main overworld + Mining only
				pfo.castEarth_EarthenShield(event, p);
			}
			break;
		case "Nature":
			if (hasPower(level, "Forest Armor") && inMainWorld(p)) { // main overworld + Mining only

				// Cast Forest Armor if player got hit
				pfo.castNature_ForestArmor(event, p);

			}
			break;
		default:
			break;
		}

	}

	// ---- Where faith powers work (2026-11-23, per Rune) ----
	/** No faith power ever works in these worlds. */
	private static final Set<String> NO_POWER_WORLDS = new HashSet<>(
			Arrays.asList("RunicSky", "RunicSkyNether", "RunicSkyEnd", "RunicMuseum"));
	/** Powers marked "main world" (Sun, Moon, Fire, Water, Air damage powers, Locust Swarm) work in these. */
	private static final Set<String> MAIN_WORLDS = new HashSet<>(Arrays.asList("RunicKingdom", "Mining"));
	/** Netherborn applies in these nethers. */
	public static final Set<String> NETHER_WORLDS = new HashSet<>(Arrays.asList("RunicKingdom_nether", "MiningNether"));

	// World lists are staff-editable (/rs Faith > Global); the sets above are the defaults FaithConfig starts from.
	static boolean powersAllowed(World w) {
		return w != null && !FaithConfig.worlds("noPowerWorlds").contains(w.getName());
	}

	static boolean inMainWorld(Player p) {
		return FaithConfig.worlds("mainWorlds").contains(p.getWorld().getName());
	}

	/** Day by the player's own world clock (Mining has its own day cycle). */
	static boolean isDay(World w) {
		long t = w.getTime();
		return t <= 14000 || t >= 23000;
	}

	/**
	 * Netherborn: on ANY world change (portals included; the old teleport-event version missed portal travel)
	 * grant the buff when entering a Netherborn nether and remove it when leaving for a non-nether world.
	 */
	public static void tryCast_PlayerChangedWorld(Player p, World from) {
		Set<String> nethers = FaithConfig.worlds("netherWorlds");
		boolean nowInNether = nethers.contains(p.getWorld().getName());
		boolean wasInNether = from != null && nethers.contains(from.getName());
		if (wasInNether && !nowInNether) {
			// Remove only Netherborn's never-ending effects (so normal potions are kept), whatever the faith now is
			Set<PotionEffectType> types = new HashSet<>(Arrays.asList(PotionEffectType.HEALTH_BOOST, PotionEffectType.DAMAGE_RESISTANCE));
			for (String k : new String[] { "effect1", "effect2" }) {
				PotionEffect configured = FaithConfig.effect("Netherborn", k);
				if (configured != null) types.add(configured.getType());
			}
			for (PotionEffectType t : types) {
				PotionEffect fx = p.getPotionEffect(t);
				if (fx != null && fx.isInfinite()) {
					p.removePotionEffect(t);
				}
			}
			return;
		}
		Faith pfo = RunicParadise.faithMap.get(p.getUniqueId());
		if (pfo == null || pfo.getPrimaryFaith() == null || !pfo.getPrimaryFaith().equals("Nether")) {
			return;
		}
		if (nowInNether && !wasInNether && hasPower(pfo.getPrimaryFaithLevel(), "Netherborn")) {
			pfo.castNether_Netherborn(p);
		}
	}

	/** Kept for the existing teleport listener; Netherborn is now handled by tryCast_PlayerChangedWorld. */
	public static void tryCast_PlayerTeleported(final PlayerTeleportEvent event) {
	}

	/** Nature: Locust Swarm - the player's projectile hit a monster. */
	public static void tryCast_PlayerShotMonster(EntityDamageByEntityEvent event, LivingEntity monster, Player p) {
		if (!powersAllowed(p.getWorld())) {
			return; // no faith powers in RunicSky, its nether/end, or the Museum
		}
		Faith pfo = RunicParadise.faithMap.get(p.getUniqueId());
		if (pfo == null || pfo.getPrimaryFaith() == null) {
			return;
		}
		if (pfo.getPrimaryFaith().equals("Nature") && hasPower(pfo.getPrimaryFaithLevel(), "Locust Swarm")
				&& inMainWorld(p)) {
			pfo.castNature_LocustSwarm(p, monster.getLocation());
		}
	}

	public static void tryCast_PlayerHitMonster(EntityDamageByEntityEvent event, Entity monster, Player p) {
		if (!powersAllowed(p.getWorld())) {
			return; // no faith powers in RunicSky, its nether/end, or the Museum
		}
		Faith pfo = RunicParadise.faithMap.get(p.getUniqueId());
		if (pfo == null || pfo.getPrimaryFaith() == null) {
			return;
		}
		String faith = pfo.getPrimaryFaith();
		int level = pfo.getPrimaryFaithLevel();
		final boolean daytime = isDay(p.getWorld());

		switch (faith) {

		case "Sun":
			if (hasPower(level, "Solar Power") && daytime) {

				// Cast Solar Power if player hit a monster
				if (inMainWorld(p)) {
					pfo.castSun_SolarPower(p.getUniqueId(), p);
				}

			}

			break;
		case "Fire":
			if (hasPower(level, "Volcanic Fury")) {

				// Cast Volcanic Fury if player hit a monster (was casting Solar Power, daytime only)
				if (inMainWorld(p)) {
					pfo.castFlame_VolcanicFury(p.getUniqueId(), p);
				}

			}

			break;
		case "Moon":
			if (hasPower(level, "Celestial Healing") && !daytime) {

				// Cast Celestial Healing if player got hit and its night
				if (inMainWorld(p)) {
					pfo.castMoon_CelestialHealing(p.getUniqueId(), p);
				}

			}

			break;

		case "Aether":
			if (hasPower(level, "Gravity Flux") && inMainWorld(p)) { // main overworld + Mining only

				// Cast Gravity Flux if player hit a monster
				pfo.castAether_GravityFlux(p);

			}

			break;

		case "Air":
			if (hasPower(level, "Tempest Armor") && inMainWorld(p)) { // main overworld + Mining only

				// Cast Tempest Armor
				pfo.castAir_TempestArmor(p);
			}

			break;
		case "Fate":

			// Use the monster's CURRENT health (was max health, so it only fired on near one-shots)
			LivingEntity lm = (LivingEntity) monster;
			double newHealthRatio = (lm.getHealth() - event.getFinalDamage()) / lm.getMaxHealth();

			// Check if mob is a monster, but not a wither (EnderDragon is not
			// Monster) --- Check for MONSTER was done in the event call itself
			// !!
			// and that new health of mob is <20%
			// and that player has proper Fate level

			if (hasPower(level, "Inevitable Demise") && inMainWorld(p)) { // main overworld + Mining only
				if (newHealthRatio > 0 && newHealthRatio <= 0.20 && !(monster instanceof Wither)) {
					pfo.castFate_InevitableDemise(p.getUniqueId(), p, (LivingEntity) monster);
				}

			}

			break;

		default:
			break;
		}

	}

	public static void tryCast_PlayerKilledMonster(final EntityDeathEvent ede, Player p) {
		if (!powersAllowed(p.getWorld())) {
			return; // no faith powers in RunicSky, its nether/end, or the Museum
		}
		Faith pfo = RunicParadise.faithMap.get(p.getUniqueId());
		String faith = pfo == null ? null : pfo.getPrimaryFaith();
		int level = pfo == null ? 0 : pfo.getPrimaryFaithLevel();

		if (faith == null) {
			Bukkit.getLogger().log(Level.WARNING,
					"Error in tryCast_PlayerKilledMonster - failed to find faith object for " + p.getName());
			return;
		}

		switch (faith) {

		case "Nether":
			if (hasPower(level, "Vampirism")) {
				// Cast Vampirism
				pfo.castNether_Vampirism(ede);
			}

			break;
		default:
			break;
		}
	}

	public int trySkillUp(Player p, String faithName, int chance, String type) {
		// int randomNum = rand.nextInt((max - min) + 1) + min;
		int randomNum = RunicParadise.randomSeed.nextInt(101);

		if (!this.checkEquippedFaithLevel(faithName,
				Integer.parseInt(RunicParadise.faithSettingsMap.get(faithName)[4]))) {
			if (randomNum <= (FaithConfig.num(FaithConfig.GLOBAL, "skillupPercent") * chance)) { // was 5% per point
				this.incrementSkill(p, faithName);

				// check for rank item drop
				tryForRankItem(p, "Faith SkillUp");

				if (p.hasPermission("killermoney.multiplier.2")) {
					this.incrementSkill(p, faithName);
				}

				if (RunicParadise.randomSeed.nextInt(101) <= 50 && chance == 1) {
					p.getWorld().playSound(p.getLocation(), Sound.BLOCK_ANVIL_BREAK, 10, 1);
					p.setItemInHand(null);
					p.sendMessage(ChatColor.GRAY + "[" + ChatColor.BLUE + "Runic" + ChatColor.DARK_AQUA + "Faith"
							+ ChatColor.GRAY + "] " + ChatColor.BLUE + "Your faith sword shatters into dust!");
				} else if (RunicParadise.randomSeed.nextInt(101) <= 30 && chance == 2) {
					p.getWorld().playSound(p.getLocation(), Sound.BLOCK_ANVIL_BREAK, 10, 1);
					p.setItemInHand(null);
					p.sendMessage(ChatColor.GRAY + "[" + ChatColor.BLUE + "Runic" + ChatColor.DARK_AQUA + "Faith"
							+ ChatColor.GRAY + "] " + ChatColor.BLUE + "Your faith sword shatters into dust!");

				} else if (RunicParadise.randomSeed.nextInt(101) <= 10 && chance == 3) {
					p.getWorld().playSound(p.getLocation(), Sound.BLOCK_ANVIL_BREAK, 10, 1);

					p.setItemInHand(null);
					p.sendMessage(ChatColor.GRAY + "[" + ChatColor.BLUE + "Runic" + ChatColor.DARK_AQUA + "Faith"
							+ ChatColor.GRAY + "] " + ChatColor.BLUE + "Your faith sword shatters into dust!");

				} else if (RunicParadise.randomSeed.nextInt(101) <= 20 && chance == 4) {
					ItemMeta metaQ = p.getItemInHand().getItemMeta();
					if (metaQ.getLore().get(6).contains("Charges: 5")) {
						metaQ.setLore(Arrays.asList(ChatColor.GRAY + "A corrupted axe with a crimson glow", " ",
								ChatColor.BLUE + "20% to increase faith",
								ChatColor.BLUE + "20% chance to lose a charge",
								ChatColor.BLUE + "Shatters when charges reach zero", " ",
								ChatColor.GREEN + "Charges: 4"));
						p.getItemInHand().setItemMeta(metaQ);
						p.getWorld().playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 10, 1);
					} else if (metaQ.getLore().get(6).contains("Charges: 4")) {
						metaQ.setLore(Arrays.asList(ChatColor.GRAY + "A corrupted axe with a crimson glow", " ",
								ChatColor.BLUE + "20% to increase faith",
								ChatColor.BLUE + "20% chance to lose a charge",
								ChatColor.BLUE + "Shatters when charges reach zero", " ",
								ChatColor.YELLOW + "Charges: 3"));
						p.getItemInHand().setItemMeta(metaQ);
						p.getWorld().playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 10, 1);
					} else if (metaQ.getLore().get(6).contains("Charges: 3")) {
						metaQ.setLore(Arrays.asList(ChatColor.GRAY + "A corrupted axe with a crimson glow", " ",
								ChatColor.BLUE + "20% to increase faith",
								ChatColor.BLUE + "20% chance to lose a charge",
								ChatColor.BLUE + "Shatters when charges reach zero", " ",
								ChatColor.RED + "Charges: 2"));
						p.getItemInHand().setItemMeta(metaQ);
						p.getWorld().playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 10, 1);
					} else if (metaQ.getLore().get(6).contains("Charges: 2")) {
						metaQ.setLore(Arrays.asList(ChatColor.GRAY + "A corrupted axe with a crimson glow", " ",
								ChatColor.BLUE + "20% to increase faith",
								ChatColor.BLUE + "20% chance to lose a charge",
								ChatColor.BLUE + "Shatters when charges reach zero", " ",
								ChatColor.DARK_RED + "Charges: 1"));
						p.getItemInHand().setItemMeta(metaQ);
						p.getWorld().playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 10, 1);
					} else if (metaQ.getLore().get(6).contains("Charges: 1")) {
						p.setItemInHand(null);
						p.sendMessage(ChatColor.GRAY + "[" + ChatColor.BLUE + "Runic" + ChatColor.DARK_AQUA + "Faith"
								+ ChatColor.GRAY + "] " + ChatColor.BLUE + "Your faith hatchet shatters into dust!");
						p.getWorld().playSound(p.getLocation(), Sound.BLOCK_ANVIL_BREAK, 10, 1);
					}

				}

				return 2;
			} else {
				return 1;
			}

		} else {
			// Player is maxxed. Faith-weapon kills used to repeat this in chat on EVERY kill; now it shows in the
			// action bar at most once every 10 minutes per player.
			long now = System.currentTimeMillis();
			Long last = MAXED_NOTICE.get(p.getUniqueId());
			if (last == null || now - last > 10 * 60 * 1000L) {
				MAXED_NOTICE.put(p.getUniqueId(), now);
				ActionBars.send(p, ChatColor.BLUE + "Your "
						+ faithName + " faith is at its maximum level and cannot grow any stronger.");
			}
			return 0;
		}
	}

	private static final Map<UUID, Long> MAXED_NOTICE = new HashMap<>();

	private static final Map<String, String> FAITH_ICONS = new HashMap<>();
	static {
		FAITH_ICONS.put("Sun", ChatColor.GOLD + "☀");
		FAITH_ICONS.put("Moon", ChatColor.AQUA + "☾");
		FAITH_ICONS.put("Fire", ChatColor.RED + "✹");
		FAITH_ICONS.put("Water", ChatColor.BLUE + "≈");
		FAITH_ICONS.put("Air", ChatColor.WHITE + "≋");
		FAITH_ICONS.put("Earth", ChatColor.DARK_GREEN + "◆");
		FAITH_ICONS.put("Nature", ChatColor.GREEN + "❀");
		FAITH_ICONS.put("Nether", ChatColor.DARK_RED + "✦");
		FAITH_ICONS.put("Aether", ChatColor.YELLOW + "✧");
		FAITH_ICONS.put("Time", ChatColor.LIGHT_PURPLE + "⌛");
		FAITH_ICONS.put("Fate", ChatColor.DARK_PURPLE + "✪");
	}

	private int trySkillUpViaPrayer(Player p, String faithName, boolean withKarma) {

		if (!this.checkEquippedFaithLevel(faithName,
				Integer.parseInt(RunicParadise.faithSettingsMap.get(faithName)[4]))) {

			int counter = 0;
			int limit;

			if (withKarma) {
				limit = FaithConfig.num(FaithConfig.GLOBAL, "prayerKarmaSkillups"); // was 3
				while (counter < limit) {
					this.incrementSkill(p, faithName);
					if (this.checkEquippedFaithLevel(faithName,
							Integer.parseInt(RunicParadise.faithSettingsMap.get(faithName)[4]))) {
						return 1;
					}
					counter++;
				}

			} else {
				limit = FaithConfig.num(FaithConfig.GLOBAL, "prayerSkillups"); // was 1

				while (counter < limit) {
					this.incrementSkill(p, faithName);
					if (this.checkEquippedFaithLevel(faithName,
							Integer.parseInt(RunicParadise.faithSettingsMap.get(faithName)[4]))) {
						return 1;
					}
					counter++;
				}

			}

			return 1;

		} else {
			// Player is maxxed
			p.sendMessage(ChatColor.GRAY + "[" + ChatColor.BLUE + "Runic" + ChatColor.DARK_AQUA + "Faith"
					+ ChatColor.GRAY + "] " + ChatColor.BLUE + "Your " + faithName + ChatColor.BLUE
					+ " faith cannot grow any stronger!");
			return 0;
		}
	}

	String getPrimaryFaith() {
		return this.primaryFaithName;
	}

	/**
	 * Power cast notice in the action bar (above the hotbar) instead of two lines of chat:
	 * "☀ Solar Power  You feel the explosive fury of the sun within you."
	 */
	private void sendCastMessage(Player p, String spellName, String faithName) {
		if (!FaithConfig.flag(spellName, "castMessage")) {
			return; // staff switched this power's cast message off
		}
		String icon = FAITH_ICONS.getOrDefault(faithName, ChatColor.BLUE + "✦");
		String flavour = RunicParadise.faithSettingsMap.containsKey(faithName)
				? RunicParadise.faithSettingsMap.get(faithName)[5] : "";
		ActionBars.send(p, icon + " " + ChatColor.WHITE
				+ ChatColor.BOLD + spellName + "  " + ChatColor.GRAY + ChatColor.ITALIC + flavour);
	}

	private static final Map<UUID, Long> SUNFLARE_LAST = new HashMap<>();
	private static final Map<UUID, Long> LUNARCALM_LAST = new HashMap<>();
	private static final Map<UUID, Long> CELESTIAL_LAST = new HashMap<>();

	/** percent% chance, but never within cooldownMs of this player's last cast; records the cast when it fires. */
	private static boolean rollWithCooldown(Map<UUID, Long> last, Player p, int percent, long cooldownMs) {
		Long prev = last.get(p.getUniqueId());
		long now = System.currentTimeMillis();
		if (prev != null && now - prev < cooldownMs) {
			return false;
		}
		if (RunicParadise.randomSeed.nextInt(100) >= percent) {
			return false;
		}
		last.put(p.getUniqueId(), now);
		return true;
	}

	private void castSun_Sunflare(UUID pUUID, Player p) {
		// 15% chance on receiving damage, at most once every 30 s per player (was ~3% with no cooldown)
		String spellName = "Sunflare";
		if (FaithConfig.roll(spellName, SUNFLARE_LAST, p)) {
			int r = FaithConfig.num(spellName, "radius");
			int fire = (int) Math.round(FaithConfig.dec(spellName, "fireSeconds") * 20);
			List<Entity> ignited = new ArrayList<>();
			for (Entity tmp : p.getNearbyEntities(r, r, r)) {
				if (tmp instanceof Monster) {
					tmp.setFireTicks(fire);
					ignited.add(tmp);
				}
			}
			FaithEffects.sunflare(p, r, ignited);
			sendCastMessage(p, spellName, "Sun");
			tryForRankItem(p, "Sun Sunflare");
			getLogger().log(Level.INFO, "Faith Info: " + p.getName() + " just cast " + spellName + "!");
		}
	}

	@SuppressWarnings("deprecation")
	public void castMoon_LunarCalm(UUID pUUID, final Player p) {
		// 8% chance on receiving damage, at most once every 30 s per player (was ~2%, no cooldown)
		String spellName = "Lunar Calm";
		if (FaithConfig.roll(spellName, LUNARCALM_LAST, p)) {
			int r = FaithConfig.num(spellName, "radius");
			long calmTicks = Math.round(FaithConfig.dec(spellName, "seconds") * 20);

			// register protection now (the target listener in RunicParadise cancels monster targeting while protected)
			RunicParadise.protectedPlayers.put(p.getUniqueId(), p.getDisplayName());

			// remove protection after the calm time (main thread)
			runLater(() -> RunicParadise.protectedPlayers.remove(p.getUniqueId()), calmTicks);

			// stop all nearby monsters that are currently targeting the player
			final List<Entity> nearby = p.getNearbyEntities(r, r, r);
			for (Entity tmp : nearby) {
				if (tmp instanceof Monster && p.equals(((Monster) tmp).getTarget())) {
					((Monster) tmp).setTarget(null);
				}
			}
			List<Entity> calmed = new ArrayList<>();
			for (Entity tmp : nearby) {
				if (tmp instanceof Monster) {
					calmed.add(tmp);
				}
			}
			FaithEffects.lunarCalm(p, r, calmed, (int) calmTicks);

			int count = nearby.size();
			boolean playerProtected = RunicParadise.protectedPlayers.containsKey(p.getUniqueId());
			sendCastMessage(p, spellName, "Moon");
			tryForRankItem(p, "Moon LunarCalm");
			getLogger().log(Level.INFO, "Faith Info: " + p.getName() + " just cast " + spellName + "! Calmed " + count
					+ " mobs. Player protected= " + playerProtected);

		}
	}

	private void castSun_SolarPower(UUID pUUID, Player p) {
		// 2% chance to empower player when dealing damage to monsters
		String spellName = "Solar Power";
		if (FaithConfig.roll(spellName, SOLAR_LAST, p)) {
			give(p, spellName, "effect1");
			give(p, spellName, "effect2");
			FaithEffects.solarPower(p);

			sendCastMessage(p, spellName, "Sun");

			tryForRankItem(p, "Sun SolarPower");
			getLogger().log(Level.INFO, "Faith Info: " + p.getName() + " just cast " + spellName + "!");
		}
	}


	private void castMoon_CelestialHealing(UUID pUUID, Player p) {
		// 5% chance on hitting a monster, at most once every 20 s per player (was ~3%, no cooldown)
		String spellName = "Celestial Healing";
		if (FaithConfig.roll(spellName, CELESTIAL_LAST, p)) {
			give(p, spellName, "effect1");
			if (FaithConfig.bool(spellName, "instantHeal")) {
				p.addPotionEffect(new PotionEffect(PotionEffectType.HEAL, 1, 0));
			}
			FaithEffects.celestialHealing(p);

			sendCastMessage(p, spellName, "Moon");
			tryForRankItem(p, "Moon CelestialHealing");
			getLogger().log(Level.INFO, "Faith Info: " + p.getName() + " just cast " + spellName + "!");
		}
	}





	// ==========================
	// Wind : TempestArmor


	// ==========================
	// Wind : Healing Breeze


	// ==========================
	// Earth : Earth's Bounty



	// ==========================
	// Water OneWithTheSea


	// ===========================
	// Water : Protective Bubble


	// ==========================
	// Nether : Vampirism


	// ==========================
	// Aether : Gravity Flux


	// ==========================
	// Aether : Graceful Steps


	// ==========================
	// Nature: Wild Growth

	private static CropState nextGrowthState(CropState original) {
		switch (original) {
			case SEEDED:
				return CropState.GERMINATED;
			case GERMINATED:
				return CropState.VERY_SMALL;
			case VERY_SMALL:
				return CropState.SMALL;
			case SMALL:
				return CropState.MEDIUM;
			case MEDIUM:
				return CropState.TALL;
			case TALL:
				return CropState.VERY_TALL;
			case VERY_TALL:
				return CropState.RIPE;
			default:
				return CropState.RIPE;
		}
	}

	private void castNature_WildGrowth(BlockGrowEvent event, Player p) {
		MaterialData data = event.getNewState().getData();
		if (data instanceof Crops) {
			Crops crops = (Crops) data;
			crops.setState(crops.getState());
		}
	}

	// =========================
	// Nature : Forest Armor



	// =========================
	// Water :: Arctic Frost


	// ======================================================================
	// Power casts (rewritten 2026-11-23: chances/cooldowns per Rune, particle effects in FaithEffects)

	private static final Map<UUID, Long> BUBBLE_LAST = new HashMap<>();
	private static final Map<UUID, Long> FROST_LAST = new HashMap<>();
	private static final Map<UUID, Long> BREEZE_LAST = new HashMap<>();
	private static final Map<UUID, Long> SHIELD_LAST = new HashMap<>();
	private static final Map<UUID, Long> BOUNTY_LAST = new HashMap<>();

	private static void logCast(Player p, String spell) {
		getLogger().log(Level.INFO, "Faith Info: " + p.getName() + " just cast " + spell + "!");
	}

	// 2026-11-26: cooldown trackers for powers that had none before (cooldown defaults to 0 = off)
	private static final Map<UUID, Long> SOLAR_LAST = new HashMap<>();
	private static final Map<UUID, Long> FURY_LAST = new HashMap<>();
	private static final Map<UUID, Long> EMBERS_LAST = new HashMap<>();
	private static final Map<UUID, Long> DEMISE_LAST = new HashMap<>();
	private static final Map<UUID, Long> REWIND_LAST = new HashMap<>();
	private static final Map<UUID, Long> VAMP_LAST = new HashMap<>();
	private static final Map<UUID, Long> TEMPEST_LAST = new HashMap<>();
	private static final Map<UUID, Long> FLUX_LAST = new HashMap<>();
	private static final Map<UUID, Long> STEPS_LAST = new HashMap<>();
	private static final Map<UUID, Long> LOCUST_LAST = new HashMap<>();
	private static final Map<UUID, Long> FOREST_LAST = new HashMap<>();

	/** Apply a staff-configured potion effect ("effect1" etc.) to an entity; returns its duration in ticks (0 if none). */
	private static int give(LivingEntity e, String power, String key) {
		PotionEffect fx = FaithConfig.effect(power, key);
		if (fx == null) {
			return 0;
		}
		e.addPotionEffect(fx);
		return fx.isInfinite() ? 0 : fx.getDuration();
	}

	// Fire : Volcanic Fury - 5% on hitting a monster: Strength III for 7.5 s
	public void castFlame_VolcanicFury(UUID pUUID, Player p) {
		if (FaithConfig.roll("Volcanic Fury", FURY_LAST, p)) {
			int ticks = give(p, "Volcanic Fury", "effect1");
			FaithEffects.volcanicFury(p, ticks > 0 ? ticks : 150);
			sendCastMessage(p, "Volcanic Fury", "Fire");
			tryForRankItem(p, "Fire VolcanicFury");
			logCast(p, "Volcanic Fury");
		}
	}

	// Fire : Unstable Embers - 11% on taking damage: 4 damage to monsters within 4 blocks
	private void castFlame_UnstableEmbers(Player p) {
		if (FaithConfig.roll("Unstable Embers", EMBERS_LAST, p)) {
			int r = FaithConfig.num("Unstable Embers", "radius");
			double dmg = FaithConfig.dec("Unstable Embers", "damage");
			sendCastMessage(p, "Unstable Embers", "Fire");
			tryForRankItem(p, "Flame UnstableEmbers");
			FaithEffects.unstableEmbers(p, r);
			for (Entity entity : p.getNearbyEntities(r, r, r)) {
				if (entity instanceof Monster) {
					((LivingEntity) entity).damage(dmg, p);
				}
			}
			logCast(p, "Unstable Embers");
		}
	}

	// Fate : Inevitable Demise - at 3 hearts or less, 21% to kill monsters within 2 blocks
	private void castFate_InevitableDemise(UUID pUUID, Player player, LivingEntity monster) {
		if (player.getHealth() > FaithConfig.dec("Inevitable Demise", "hearts") * 2
				|| !FaithConfig.roll("Inevitable Demise", DEMISE_LAST, player)) {
			return;
		}
		int r = FaithConfig.num("Inevitable Demise", "radius");
		List<Location> reaped = new ArrayList<>();
		for (final Entity entity : player.getNearbyEntities(r, r, r)) {
			if (entity instanceof Monster && !(entity instanceof Wither)) {
				reaped.add(entity.getLocation());
				((LivingEntity) entity).setHealth(0);
			}
		}
		if (!monster.isDead() && monster instanceof Monster) {
			reaped.add(monster.getLocation());
			monster.setHealth(0);
		}
		FaithEffects.inevitableDemise(player, reaped);
		sendCastMessage(player, "Inevitable Demise", "Fate");
		tryForRankItem(player, "Fate InevitableDemise");
		logCast(player, "Inevitable Demise");
	}

	// Time : Rewind - 10% of killing blows are undone
	private void castTime_Rewind(EntityDamageEvent event, Player p) {
		if (p.getHealth() - event.getFinalDamage() > 0) {
			return; // not a killing blow
		}
		if (FaithConfig.roll("Rewind", REWIND_LAST, p)) {
			event.setCancelled(true);
			p.setHealth(Math.min(FaithConfig.dec("Rewind", "health"), p.getMaxHealth()));
			give(p, "Rewind", "effect1");
			FaithEffects.rewind(p);
			sendCastMessage(p, "Rewind", "Time");
			tryForRankItem(p, "Time Rewind");
			logCast(p, "Rewind");
		}
	}

	// Nether : Netherborn - lasts while in a Netherborn nether
	void castNether_Netherborn(Player p) {
		give(p, "Netherborn", "effect1");
		give(p, "Netherborn", "effect2");
		FaithEffects.netherborn(p);
		sendCastMessage(p, "Netherborn", "Nether");
		logCast(p, "Netherborn");
	}

	// Nether : Vampirism - 21% on killing a monster: heal 3 HP
	private void castNether_Vampirism(EntityDeathEvent event) {
		Entity e = event.getEntity();
		if (!(e.getLastDamageCause() instanceof EntityDamageByEntityEvent)) {
			return;
		}
		Entity damager = ((EntityDamageByEntityEvent) e.getLastDamageCause()).getDamager();
		if (!(damager instanceof Player) || !FaithConfig.roll("Vampirism", VAMP_LAST, (Player) damager)) {
			return;
		}
		Player p = (Player) damager;
		p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + FaithConfig.dec("Vampirism", "heal")));
		FaithEffects.vampirism(p, e.getLocation());
		sendCastMessage(p, "Vampirism", "Nether");
		tryForRankItem(p, "Nether Vampirism");
	}

	// Air : Tempest Armor - FULL GOLD ARMOUR required (all 4 pieces); 5% on hitting a monster
	private void castAir_TempestArmor(Player player) {
		if ((FaithConfig.bool("Tempest Armor", "fullGold") && !wearingFullSet(player, Material.GOLDEN_HELMET,
				Material.GOLDEN_CHESTPLATE, Material.GOLDEN_LEGGINGS, Material.GOLDEN_BOOTS))
				|| !FaithConfig.roll("Tempest Armor", TEMPEST_LAST, player)) {
			return;
		}
		int r = FaithConfig.num("Tempest Armor", "radius");
		boolean lightning = FaithConfig.bool("Tempest Armor", "lightning");
		sendCastMessage(player, "Tempest Armor", "Air");
		tryForRankItem(player, "Air TempestArmor");
		List<Entity> flung = new ArrayList<>();
		for (final Entity entity : player.getNearbyEntities(r, r, r)) {
			if (entity instanceof Monster) {
				flung.add(entity);
				entity.setVelocity(entity.getLocation().getDirection().multiply(2.5D).setY(0.7D));
				if (lightning) {
					runLater(() -> {
						if (entity.isValid()) {
							entity.getWorld().strikeLightning(entity.getLocation());
						}
					}, 20);
				}
			}
		}
		FaithEffects.tempestArmor(player, r, flung);
		logCast(player, "Tempest Armor");
	}

	// Air : Healing Breeze - 5% on taking damage, 30 s cooldown: Regeneration II 10 s for you, players within 40, tamed wolves
	private void castAir_HealingBreeze(Player player) {
		if (!FaithConfig.roll("Healing Breeze", BREEZE_LAST, player)) {
			return;
		}
		int r = FaithConfig.num("Healing Breeze", "radius");
		sendCastMessage(player, "Healing Breeze", "Air");
		tryForRankItem(player, "Air HealingBreeze");
		List<Entity> targets = new ArrayList<>(player.getNearbyEntities(r, r, r));
		targets.add(player);
		List<LivingEntity> healed = new ArrayList<>();
		for (Entity entity : targets) {
			if (entity instanceof Player) {
				give((LivingEntity) entity, "Healing Breeze", "effect1");
				healed.add((LivingEntity) entity);
				if (entity != player) {
					ActionBars.send((Player) entity, ChatColor.WHITE + "≋ " + ChatColor.GRAY + ChatColor.ITALIC
							+ "You feel refreshed by a soothing breeze");
				}
			} else if (entity instanceof Wolf && ((Wolf) entity).isTamed()) {
				give((Wolf) entity, "Healing Breeze", "effect1");
				healed.add((LivingEntity) entity);
			}
		}
		FaithEffects.healingBreeze(player, healed);
		logCast(player, "Healing Breeze");
	}

	// Water : Protective Bubble - 10% on taking damage, 20 s cooldown: Absorption I for 15 s
	private void castWater_ProtectiveBubble(final Player player) {
		if (!FaithConfig.roll("Protective Bubble", BUBBLE_LAST, player)) {
			return;
		}
		PotionEffect bubble = FaithConfig.effect("Protective Bubble", "effect1");
		if (bubble == null) {
			return;
		}
		player.removePotionEffect(bubble.getType());
		player.addPotionEffect(bubble);
		int ticks = bubble.isInfinite() ? 300 : bubble.getDuration();
		FaithEffects.protectiveBubble(player, ticks);
		sendCastMessage(player, "Protective Bubble", "Water");
		tryForRankItem(player, "Water ProtectiveBubble");
		if (!bubble.isInfinite()) {
			runLater(() -> player.removePotionEffect(bubble.getType()), ticks + 20);
		}
		logCast(player, "Protective Bubble");
	}

	// Water : Arctic Frost - 8% on taking damage, 25 s cooldown: Slowness III 7.5 s on monsters within 7 blocks
	private void castWater_ArcticFrost(Player p) {
		if (!FaithConfig.roll("Arctic Frost", FROST_LAST, p)) {
			return;
		}
		int r = FaithConfig.num("Arctic Frost", "radius");
		sendCastMessage(p, "Arctic Frost", "Water");
		tryForRankItem(p, "Water ArcticFrost");
		List<Entity> slowed = new ArrayList<>();
		int ticks = 150;
		for (Entity e : p.getNearbyEntities(r, r, r)) {
			if (e instanceof Monster) {
				int t = give((Monster) e, "Arctic Frost", "effect1");
				if (t > 0) ticks = t;
				slowed.add(e);
			}
		}
		FaithEffects.arcticFrost(p, r, slowed, ticks);
		logCast(p, "Arctic Frost");
	}

	// Water : Deep Wader - right-click (air or block) with glowstone dust while in water: costs 1 level,
	// Water Breathing for 10 minutes. (Was 2 levels, and only worked when right-clicking a block while the
	// feet block was plain water; waterlogged blocks and clicking into open water did nothing.)
	void castWater_DeepWader(PlayerInteractEvent e) {
		Player p = e.getPlayer();
		if (!powersAllowed(p.getWorld()) || !inMainWorld(p)) { // main overworld + Mining only
			return;
		}
		if (e.getItem() == null || e.getItem().getType() != Material.GLOWSTONE_DUST || !p.isInWater()) {
			return;
		}
		int cost = FaithConfig.num("Deep Wader", "xpCost");
		if (p.getLevel() < cost) {
			ActionBars.send(p, ChatColor.RED + "Deep Wader needs " + cost + " XP level" + (cost == 1 ? "." : "s."));
			return;
		}
		p.setLevel(p.getLevel() - cost);
		give(p, "Deep Wader", "effect1");
		FaithEffects.deepWader(p);
		sendCastMessage(p, "Deep Wader", "Water");
		tryForRankItem(p, "Water DeepWader");
		logCast(p, "Deep Wader");
	}

	// Earth : Earth's Bounty - right-click stone/deepslate/etc. with glowstone dust: costs 1 level, counts diamond
	// and emerald ores within 30 blocks of the player in every direction. (Was 2 levels, and scanned a 30-block cube
	// centred 15 blocks IN FRONT of the player.) The count runs off the main thread on chunk snapshots.
	static final int EARTHS_BOUNTY_RADIUS = 30;
	private static final Set<Material> EARTHS_BOUNTY_BLOCKS = EnumSet.of(Material.STONE, Material.DEEPSLATE,
			Material.TUFF, Material.GRANITE, Material.DIORITE, Material.ANDESITE, Material.COBBLED_DEEPSLATE);

	void castEarth_EarthsBounty(PlayerInteractEvent e) {
		final Player p = e.getPlayer();
		if (!powersAllowed(p.getWorld()) || !inMainWorld(p)) { // main overworld + Mining only
			return;
		}
		if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getItem() == null
				|| e.getItem().getType() != Material.GLOWSTONE_DUST
				|| !EARTHS_BOUNTY_BLOCKS.contains(e.getClickedBlock().getType())) {
			return;
		}
		Long last = BOUNTY_LAST.get(p.getUniqueId());
		if (last != null && System.currentTimeMillis() - last < 3000) {
			return; // stops double clicks from charging twice
		}
		int cost = FaithConfig.num("Earth's Bounty", "xpCost");
		if (p.getLevel() < cost) {
			ActionBars.send(p, ChatColor.RED + "Earth's Bounty needs " + cost + " XP level" + (cost == 1 ? "." : "s."));
			return;
		}
		BOUNTY_LAST.put(p.getUniqueId(), System.currentTimeMillis());
		p.setLevel(p.getLevel() - cost);
		FaithEffects.earthsBountyPulse(p, e.getClickedBlock().getBlockData());
		sendCastMessage(p, "Earth's Bounty", "Earth");

		final int r = FaithConfig.num("Earth's Bounty", "radius");
		final Location c = p.getLocation();
		final World w = c.getWorld();
		final int px = c.getBlockX(), py = c.getBlockY(), pz = c.getBlockZ();
		final int minY = Math.max(w.getMinHeight(), py - r);
		final int maxY = Math.min(w.getMaxHeight() - 1, py + r);
		final List<ChunkSnapshot> snaps = new ArrayList<>();
		for (int cx = (px - r) >> 4; cx <= (px + r) >> 4; cx++) {
			for (int cz = (pz - r) >> 4; cz <= (pz + r) >> 4; cz++) {
				if (w.isChunkLoaded(cx, cz)) {
					snaps.add(w.getChunkAt(cx, cz).getChunkSnapshot(false, false, false));
				}
			}
		}
		Bukkit.getScheduler().runTaskAsynchronously(instance, () -> {
			int diamonds = 0, emeralds = 0;
			for (ChunkSnapshot s : snaps) {
				int bx = s.getX() << 4, bz = s.getZ() << 4;
				for (int x = 0; x < 16; x++) {
					if (Math.abs(bx + x - px) > r) {
						continue;
					}
					for (int z = 0; z < 16; z++) {
						if (Math.abs(bz + z - pz) > r) {
							continue;
						}
						for (int y = minY; y <= maxY; y++) {
							Material m = s.getBlockType(x, y, z);
							if (m == Material.DIAMOND_ORE || m == Material.DEEPSLATE_DIAMOND_ORE) {
								diamonds++;
							} else if (m == Material.EMERALD_ORE || m == Material.DEEPSLATE_EMERALD_ORE) {
								emeralds++;
							}
						}
					}
				}
			}
			final int d = diamonds, em = emeralds;
			Bukkit.getScheduler().runTask(instance, () -> {
				if (!p.isOnline()) {
					return;
				}
				p.sendMessage(ChatColor.DARK_GREEN + "◆ " + ChatColor.GREEN + "Within " + r + " blocks: " + ChatColor.AQUA
						+ d + " diamond ore" + ChatColor.GREEN + ", " + ChatColor.DARK_GREEN + em + " emerald ore");
				FaithEffects.earthsBountyResult(p, d, em);
			});
		});
		logCast(p, "Earth's Bounty");
	}

	// Earth : Earthen Shield (new, level 150) - while standing on natural ground, 10% on taking a hit (25 s cooldown):
	// the earth rises and BLOCKS the hit completely, and the blocked damage (2-8) is thrown back at a mob attacker
	// with a knockback. Unique among the "on damage" powers: it negates the hit instead of buffing or debuffing.
	private static final Set<Material> EARTH_GROUND = EnumSet.of(Material.STONE, Material.COBBLESTONE,
			Material.MOSSY_COBBLESTONE, Material.DEEPSLATE, Material.COBBLED_DEEPSLATE, Material.TUFF, Material.CALCITE,
			Material.GRANITE, Material.DIORITE, Material.ANDESITE, Material.DIRT, Material.COARSE_DIRT, Material.ROOTED_DIRT,
			Material.GRASS_BLOCK, Material.PODZOL, Material.MYCELIUM, Material.DIRT_PATH, Material.FARMLAND, Material.MUD,
			Material.PACKED_MUD, Material.CLAY, Material.GRAVEL, Material.SAND, Material.RED_SAND, Material.SANDSTONE,
			Material.RED_SANDSTONE, Material.TERRACOTTA, Material.MOSS_BLOCK, Material.DRIPSTONE_BLOCK,
			Material.SMOOTH_BASALT, Material.BASALT, Material.BLACKSTONE, Material.NETHERRACK);

	static boolean isEarthGround(Material m) {
		return EARTH_GROUND.contains(m) || m.name().endsWith("_ORE") || m.name().endsWith("_TERRACOTTA");
	}

	private void castEarth_EarthenShield(EntityDamageEvent event, Player p) {
		Block below = p.getLocation().subtract(0, 0.2, 0).getBlock();
		if (!isEarthGround(below.getType()) || !FaithConfig.roll("Earthen Shield", SHIELD_LAST, p)) {
			return;
		}
		double blocked = event.getDamage();
		event.setCancelled(true);
		LivingEntity attacker = null;
		if (event instanceof EntityDamageByEntityEvent) {
			Entity d = ((EntityDamageByEntityEvent) event).getDamager();
			if (d instanceof Projectile && ((Projectile) d).getShooter() instanceof Entity) {
				d = (Entity) ((Projectile) d).getShooter();
			}
			if (d instanceof LivingEntity && !(d instanceof Player)) {
				attacker = (LivingEntity) d;
			}
		}
		if (attacker != null) {
			attacker.damage(Math.min(FaithConfig.dec("Earthen Shield", "reflectMax"),
					Math.max(FaithConfig.dec("Earthen Shield", "reflectMin"), blocked)), p);
			Vector push = attacker.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0);
			if (push.lengthSquared() > 0.001) {
				attacker.setVelocity(push.normalize().multiply(0.9).setY(0.35));
			}
		}
		FaithEffects.earthenShield(p, below.getBlockData(), attacker);
		sendCastMessage(p, "Earthen Shield", "Earth");
		tryForRankItem(p, "Earth EarthenShield");
		logCast(p, "Earthen Shield");
	}

	// Aether : Gravity Flux - 4% on hitting a monster: launch monsters within 7 blocks
	private void castAether_GravityFlux(Player p) {
		if (!FaithConfig.roll("Gravity Flux", FLUX_LAST, p)) {
			return;
		}
		int r = FaithConfig.num("Gravity Flux", "radius");
		double launch = FaithConfig.dec("Gravity Flux", "launch");
		sendCastMessage(p, "Gravity Flux", "Aether");
		tryForRankItem(p, "Aether GravityFlux");
		List<Entity> launched = new ArrayList<>();
		for (Entity e : p.getNearbyEntities(r, r, r)) {
			if (e instanceof Monster) {
				e.setVelocity(new Vector(e.getVelocity().getX(), launch, e.getVelocity().getZ()));
				launched.add(e);
			}
		}
		FaithEffects.gravityFlux(p, launched);
		logCast(p, "Gravity Flux");
	}

	// Aether : Graceful Steps - 20% on taking fall damage: Speed I for 15 s
	private void castAether_GracefulSteps(Player p) {
		if (!FaithConfig.roll("Graceful Steps", STEPS_LAST, p)) {
			return;
		}
		int ticks = give(p, "Graceful Steps", "effect1");
		FaithEffects.gracefulSteps(p, ticks > 0 ? ticks : 300);
		sendCastMessage(p, "Graceful Steps", "Aether");
	}

	// Nature : Locust Swarm - 8% per arrow hit: monsters within 3 blocks of the hit take 2 damage/s for 5 s
	void castNature_LocustSwarm(final Player p, final Location where) {
		if (!FaithConfig.roll("Locust Swarm", LOCUST_LAST, p)) {
			return;
		}
		final int r = FaithConfig.num("Locust Swarm", "radius");
		final double dmg = FaithConfig.dec("Locust Swarm", "damage");
		final int seconds = Math.max(1, FaithConfig.num("Locust Swarm", "seconds"));
		sendCastMessage(p, "Locust Swarm", "Nature");
		tryForRankItem(p, "Nature LocustSwarm");
		where.getWorld().playSound(where, Sound.ENTITY_BEE_LOOP_AGGRESSIVE, 1f, 0.6f);
		FaithEffects.locustPulse(where);
		for (int i = 0; i < seconds; i++) {
			runLater(() -> {
				if (!p.isOnline() || where.getWorld() == null) {
					return;
				}
				FaithEffects.locustPulse(where);
				for (Entity e : where.getWorld().getNearbyEntities(where, r, r, r)) {
					if (e instanceof Monster && !e.isDead()) {
						((LivingEntity) e).damage(dmg, p);
					}
				}
			}, 20L * (i + 1));
		}
		logCast(p, "Locust Swarm");
	}

	// Nature : Forest Armor - FULL LEATHER ARMOUR required (all 4 pieces); 5% on taking damage: 2 wolves for 20 s,
	// attacker poisoned
	private void castNature_ForestArmor(EntityDamageEvent event, Player player) {
		if ((FaithConfig.bool("Forest Armor", "fullLeather") && !wearingFullSet(player, Material.LEATHER_HELMET,
				Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS))
				|| !FaithConfig.roll("Forest Armor", FOREST_LAST, player)) {
			return;
		}
		sendCastMessage(player, "Forest Armor", "Nature");
		final List<Wolf> wolves = new ArrayList<>();
		for (int i = 0; i < FaithConfig.num("Forest Armor", "wolves"); i++) {
			Wolf wolf = (Wolf) player.getWorld().spawnEntity(player.getLocation(), EntityType.WOLF);
			wolf.setOwner(player);
			wolf.setAdult();
			wolf.setTamed(true);
			wolf.setCustomName(ChatColor.YELLOW + player.getName() + "'s Wolf");
			wolf.setCustomNameVisible(true);
			wolves.add(wolf);
		}
		LivingEntity poisoned = null;
		if (event instanceof EntityDamageByEntityEvent) {
			Entity damager = ((EntityDamageByEntityEvent) event).getDamager();
			if (damager instanceof Projectile && ((Projectile) damager).getShooter() instanceof Entity) {
				damager = (Entity) ((Projectile) damager).getShooter();
			}
			if (damager instanceof LivingEntity && !(damager instanceof Player)) {
				poisoned = (LivingEntity) damager;
				give(poisoned, "Forest Armor", "effect1");
				for (Wolf wolf : wolves) {
					wolf.setTarget(poisoned);
				}
			}
		}
		FaithEffects.forestArmor(player, wolves, poisoned);
		runLater(() -> {
			for (Wolf wolf : wolves) {
				if (wolf.isValid()) {
					FaithEffects.wolfDepart(wolf.getLocation());
				}
				wolf.remove();
			}
		}, 20L * FaithConfig.num("Forest Armor", "wolfSeconds"));
		logCast(player, "Forest Armor");
	}

	private static boolean wearingFullSet(Player p, Material helmet, Material chest, Material legs, Material boots) {
		ItemStack h = p.getInventory().getHelmet(), c = p.getInventory().getChestplate(),
				l = p.getInventory().getLeggings(), b = p.getInventory().getBoots();
		return h != null && c != null && l != null && b != null && h.getType() == helmet && c.getType() == chest
				&& l.getType() == legs && b.getType() == boots;
	}

	/**
	 * Prayer books store items as pre-1.13 numeric id + data value. Material.getId() throws for modern materials, so
	 * the old lookup failed on every prayer. Convert through the legacy material table instead.
	 */
	@SuppressWarnings("deprecation")
	static ItemStack prayerItem(String[] book, int idIdx) {
		int id = Integer.parseInt(book[idIdx]);
		byte data = Byte.parseByte(book[idIdx + 1]);
		int amount = Integer.parseInt(book[idIdx + 2]);
		Material legacy = null;
		for (Material m : Material.values()) {
			if (m.isLegacy() && m.getId() == id) {
				legacy = m;
				break;
			}
		}
		if (legacy == null) {
			throw new RuntimeException("no legacy material with id " + id);
		}
		Material modern = Bukkit.getUnsafe().fromLegacy(new MaterialData(legacy, data));
		if (modern == null || modern == Material.AIR) {
			modern = Bukkit.getUnsafe().fromLegacy(legacy);
		}
		if (modern == null || modern == Material.AIR) {
			throw new RuntimeException("legacy id " + id + ":" + data + " has no modern material");
		}
		return new ItemStack(modern, amount);
	}

	static void pray(Location locKey, Player p) {
		RunicPlayerBukkit targetPlayer = new RunicPlayerBukkit(p.getUniqueId());
		boolean needKarma = false;
		int numItems = Integer.parseInt(RunicParadise.prayerBooks.get(locKey)[4]);

		if (RunicParadise.prayerBooks.get(locKey)[17].equals("Book")) {
			needKarma = true;
		}

		if (!RunicParadise.prayerBooks.get(locKey)[18]
				.equals(RunicParadise.faithMap.get(p.getUniqueId()).getPrimaryFaith())) {

			p.sendMessage(
					"Your prayer is rejected! You must be loyal to your faith - pray at your active faith's shrines! "
							+ ChatColor.BLUE + "Left-click the block to learn what this prayer needs!");
			return;
		}

		String[] book = RunicParadise.prayerBooks.get(locKey);
		ItemStack item1, item2, item3, item4;
		try {
			// Only build the items this prayer actually uses (unused slots may hold id 0)
			item1 = prayerItem(book, 5);
			item2 = numItems >= 2 ? prayerItem(book, 8) : null;
			item3 = numItems >= 3 ? prayerItem(book, 11) : null;
			item4 = numItems >= 4 ? prayerItem(book, 14) : null;
		} catch (RuntimeException ex) {
			getLogger().log(Level.WARNING, "Faith prayer at " + locKey + " has an item that can't be converted: " + ex.getMessage());
			p.sendMessage("This shrine's prayer is misconfigured. Please report it to staff.");
			return;
		}
		ItemStack[] item1a = new ItemStack[] { item1 == null ? null : item1.clone() };
		ItemStack[] item2a = new ItemStack[] { item2 == null ? null : item2.clone() };
		ItemStack[] item3a = new ItemStack[] { item3 == null ? null : item3.clone() };
		ItemStack[] item4a = new ItemStack[] { item4 == null ? null : item4.clone() };

		switch (numItems) {
		case 1:
			if (p.getInventory().contains(item1)) {
				// Player has enough of first item!

				if (needKarma && targetPlayer.getKarma() >= FaithConfig.num(FaithConfig.GLOBAL, "prayerKarmaCost")) {
					// Player is trying to pray at a Book - costs 2 karma! they
					// have enough karma, so take it!

					if (RunicParadise.faithMap.get(p.getUniqueId()).trySkillUpViaPrayer(p,
							RunicParadise.faithMap.get(p.getUniqueId()).getPrimaryFaith(), true) == 1) {
						targetPlayer.adjustPlayerKarma(-FaithConfig.num(FaithConfig.GLOBAL, "prayerKarmaCost"));
						p.getInventory().removeItem(item1a);
						p.sendMessage("Your karma prayer has been answered!");
					} else {
						// player is at max level, prayer failed!!
						return;
					}
				} else if (!needKarma) {
					// Karma not needed for this prayer

					if (RunicParadise.faithMap.get(p.getUniqueId()).trySkillUpViaPrayer(p,
							RunicParadise.faithMap.get(p.getUniqueId()).getPrimaryFaith(), false) == 1) {
						p.getInventory().removeItem(item1a);
						p.sendMessage("Your prayer has been answered!");
					} else {
						// player is at max level, prayer failed!!
						return;
					}

				} else {
					// Karma is needed for this prayer and player doesnt have
					// enough!
					p.sendMessage("This prayer costs " + FaithConfig.num(FaithConfig.GLOBAL, "prayerKarmaCost") + " karma; you don't have enough!!");
					return;
				}
			} else {
				// player doesn't have enough items :(
				p.sendMessage(
						"Your prayer is rejected! Do you have the right items? Be sure each stack is just the right amount!");
				return;
			}
			break;
		case 2:
			if (p.getInventory().contains(item1) && p.getInventory().contains(item2)) {
				// Player has enough of first item!

				if (needKarma && targetPlayer.getKarma() >= FaithConfig.num(FaithConfig.GLOBAL, "prayerKarmaCost")) {
					// Player is trying to pray at a Book - costs 2 karma! they
					// have enough karma, so take it!

					if (RunicParadise.faithMap.get(p.getUniqueId()).trySkillUpViaPrayer(p,
							RunicParadise.faithMap.get(p.getUniqueId()).getPrimaryFaith(), true) == 1) {
						targetPlayer.adjustPlayerKarma(-FaithConfig.num(FaithConfig.GLOBAL, "prayerKarmaCost"));
						p.getInventory().removeItem(item1a);
						p.getInventory().removeItem(item2a);
						p.sendMessage("Your karma prayer has been answered!");
					} else {
						// player is at max level, prayer failed!!
						return;
					}
				} else if (!needKarma) {
					// Karma not needed for this prayer

					if (RunicParadise.faithMap.get(p.getUniqueId()).trySkillUpViaPrayer(p,
							RunicParadise.faithMap.get(p.getUniqueId()).getPrimaryFaith(), false) == 1) {
						p.getInventory().removeItem(item1a);
						p.getInventory().removeItem(item2a);
						p.sendMessage("Your prayer has been answered!");
					} else {
						// player is at max level, prayer failed!!
						return;
					}
				} else {
					// Karma is needed for this prayer and player doesnt have
					// enough!
					p.sendMessage("This prayer costs " + FaithConfig.num(FaithConfig.GLOBAL, "prayerKarmaCost") + " karma; you don't have enough!!");
					return;
				}
			} else {
				// player doesnt have enough items :(
				p.sendMessage(
						"Your prayer is rejected! Do you have the right items? Be sure each stack is just the right amount!");
				return;
			}
			break;
		case 3:
			if (p.getInventory().contains(item1) && p.getInventory().contains(item2)
					&& p.getInventory().contains(item3)) {
				// Player has enough of first item!

				if (needKarma && targetPlayer.getKarma() >= FaithConfig.num(FaithConfig.GLOBAL, "prayerKarmaCost")) {
					// Player is trying to pray at a Book - costs 2 karma! they
					// have enough karma, so take it!

					if (RunicParadise.faithMap.get(p.getUniqueId()).trySkillUpViaPrayer(p,
							RunicParadise.faithMap.get(p.getUniqueId()).getPrimaryFaith(), true) == 1) {
						targetPlayer.adjustPlayerKarma(-FaithConfig.num(FaithConfig.GLOBAL, "prayerKarmaCost"));
						p.getInventory().removeItem(item1a);
						p.getInventory().removeItem(item2a);
						p.getInventory().removeItem(item3a);
						p.sendMessage("Your karma prayer has been answered!");
					} else {
						// player is at max level, prayer failed!!
						return;
					}

				} else if (!needKarma) {
					// Karma not needed for this prayer

					if (RunicParadise.faithMap.get(p.getUniqueId()).trySkillUpViaPrayer(p,
							RunicParadise.faithMap.get(p.getUniqueId()).getPrimaryFaith(), false) == 1) {
						p.getInventory().removeItem(item1a);
						p.getInventory().removeItem(item2a);
						p.getInventory().removeItem(item3a);
						p.sendMessage("Your prayer has been answered!");
					} else {
						// player is at max level, prayer failed!!
						return;
					}

				} else {
					// Karma is needed for this prayer and player doesnt have
					// enough!
					p.sendMessage("This prayer costs " + FaithConfig.num(FaithConfig.GLOBAL, "prayerKarmaCost") + " karma; you don't have enough!!");
					return;
				}
			} else {
				// player doesnt have enough items :(
				p.sendMessage(
						"Your prayer is rejected! Do you have the right items? Be sure each stack is just the right amount!");
				return;
			}
			break;
		case 4:
			if (p.getInventory().contains(item1) && p.getInventory().contains(item2) && p.getInventory().contains(item3)
					&& p.getInventory().contains(item4)) {
				// Player has enough of first item!

				if (needKarma && targetPlayer.getKarma() >= FaithConfig.num(FaithConfig.GLOBAL, "prayerKarmaCost")) {
					// Player is trying to pray at a Book - costs 2 karma! they
					// have enough karma, so take it!

					if (RunicParadise.faithMap.get(p.getUniqueId()).trySkillUpViaPrayer(p,
							RunicParadise.faithMap.get(p.getUniqueId()).getPrimaryFaith(), true) == 1) {
						targetPlayer.adjustPlayerKarma(-FaithConfig.num(FaithConfig.GLOBAL, "prayerKarmaCost"));
						p.getInventory().removeItem(item1a);
						p.getInventory().removeItem(item2a);
						p.getInventory().removeItem(item3a);
						p.getInventory().removeItem(item4a);
						p.sendMessage("Your karma prayer has been answered!");
					} else {
						// player is at max level, prayer failed!!
						return;
					}
				} else if (!needKarma) {
					// Karma not needed for this prayer

					if (RunicParadise.faithMap.get(p.getUniqueId()).trySkillUpViaPrayer(p,
							RunicParadise.faithMap.get(p.getUniqueId()).getPrimaryFaith(), false) == 1) {
						p.getInventory().removeItem(item1a);
						p.getInventory().removeItem(item2a);
						p.getInventory().removeItem(item3a);
						p.getInventory().removeItem(item4a);
						p.sendMessage("Your prayer has been answered!");
					} else {
						// player is at max level, prayer failed!!
						return;
					}

				} else {
					// Karma is needed for this prayer and player doesnt have
					// enough!
					p.sendMessage("This prayer costs " + FaithConfig.num(FaithConfig.GLOBAL, "prayerKarmaCost") + " karma; you don't have enough!!");
					return;
				}
			} else {
				// player doesnt have enough items :(
				p.sendMessage(
						"Your prayer is rejected! Do you have the right items? Be sure each stack is just the right amount!");
				return;
			}
			break;
		default:
			p.sendMessage("Invalid numItems! Should be 1-4. Report this to Rune please.");
			break;
		}
	}

	private void resetFaithSlimefunPermissions(UUID pUUID, String activeFaith) {
		if (activeFaith != null && this.faithLevels.get(activeFaith) != null) {
			Player p = Bukkit.getPlayer(pUUID);

			Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"lp user "+ Bukkit.getPlayer(pUUID).getName() +" permission unset rp.slimefun.sun100");
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"lp user "+ Bukkit.getPlayer(pUUID).getName() +" permission unset rp.slimefun.sun250");
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"lp user "+ Bukkit.getPlayer(pUUID).getName() +" permission unset rp.slimefun.moon100");
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"lp user "+ Bukkit.getPlayer(pUUID).getName() +" permission unset rp.slimefun.moon250");
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"lp user "+ Bukkit.getPlayer(pUUID).getName() +" permission unset rp.slimefun.earth150");
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"lp user "+ Bukkit.getPlayer(pUUID).getName() +" permission unset rp.slimefun.earth250");
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"lp user "+ Bukkit.getPlayer(pUUID).getName() +" permission unset rp.slimefun.earth375");
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"lp user "+ Bukkit.getPlayer(pUUID).getName() +" permission unset rp.slimefun.nether300");
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"lp user "+ Bukkit.getPlayer(pUUID).getName() +" permission unset rp.slimefun.nether375");
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"lp user "+ Bukkit.getPlayer(pUUID).getName() +" permission unset rp.slimefun.water300");
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"lp user "+ Bukkit.getPlayer(pUUID).getName() +" permission unset rp.slimefun.water375");
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"lp user "+ Bukkit.getPlayer(pUUID).getName() +" permission unset rp.slimefun.fire275");
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"lp user "+ Bukkit.getPlayer(pUUID).getName() +" permission unset rp.slimefun.fire350");
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"lp user "+ Bukkit.getPlayer(pUUID).getName() +" permission unset rp.slimefun.air275");
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"lp user "+ Bukkit.getPlayer(pUUID).getName() +" permission unset rp.slimefun.nature75");
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"lp user "+ Bukkit.getPlayer(pUUID).getName() +" permission unset rp.slimefun.tech100");
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"lp user "+ Bukkit.getPlayer(pUUID).getName() +" permission unset rp.slimefun.tech200");
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"lp user "+ Bukkit.getPlayer(pUUID).getName() +" permission unset rp.slimefun.tech300");
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"lp user "+ Bukkit.getPlayer(pUUID).getName() +" permission unset rp.slimefun.tech400");
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"lp user "+ Bukkit.getPlayer(pUUID).getName() +" permission unset rp.slimefun.aether300");


			if (this.faithLevels.get(activeFaith) >= 100 && activeFaith.equals("Sun")) {
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"lp user "+ Bukkit.getPlayer(pUUID).getName() +" permission set rp.slimefun.sun100");

				if (this.faithLevels.get(activeFaith) >= 250) {
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"lp user "+ Bukkit.getPlayer(pUUID).getName() +" permission set rp.slimefun.sun250");
				}
			} else if (this.faithLevels.get(activeFaith) >= 100 && activeFaith.equals("Moon")) {
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"lp user "+ Bukkit.getPlayer(pUUID).getName() +" permission set rp.slimefun.moon100");

				if (this.faithLevels.get(activeFaith) >= 250) {
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"lp user "+ Bukkit.getPlayer(pUUID).getName() +" permission set rp.slimefun.moon250");
				}
			} else if (this.faithLevels.get(activeFaith) >= 150 && activeFaith.equals("Earth")) {
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"lp user "+ Bukkit.getPlayer(pUUID).getName() +" permission set rp.slimefun.earth150");

				if (this.faithLevels.get(activeFaith) >= 250) {
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"lp user "+ Bukkit.getPlayer(pUUID).getName() +" permission set rp.slimefun.earth250");
				}
				if (this.faithLevels.get(activeFaith) >= 375) {
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"lp user "+ Bukkit.getPlayer(pUUID).getName() +" permission set rp.slimefun.earth375");
				}

			} else if (this.faithLevels.get(activeFaith) >= 300 && activeFaith.equals("Nether")) {
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"lp user "+ Bukkit.getPlayer(pUUID).getName() +" permission set rp.slimefun.nether300");

				if (this.faithLevels.get(activeFaith) >= 375) {
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"lp user "+ Bukkit.getPlayer(pUUID).getName() +" permission set rp.slimefun.nether375");
				}
			} else if (this.faithLevels.get(activeFaith) >= 300 && activeFaith.equals("Water")) {
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"lp user "+ Bukkit.getPlayer(pUUID).getName() +" permission set rp.slimefun.water300");

				if (this.faithLevels.get(activeFaith) >= 375) {
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"lp user "+ Bukkit.getPlayer(pUUID).getName() +" permission set rp.slimefun.water375");
				}
			} else if (this.faithLevels.get(activeFaith) >= 275 && activeFaith.equals("Fire")) {
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"lp user "+ Bukkit.getPlayer(pUUID).getName() +" permission set rp.slimefun.fire275");

				if (this.faithLevels.get(activeFaith) >= 350) {
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"lp user "+ Bukkit.getPlayer(pUUID).getName() +" permission set rp.slimefun.fire350");
				}
			} else if (this.faithLevels.get(activeFaith) >= 275 && activeFaith.equals("Air")) {
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"lp user "+ Bukkit.getPlayer(pUUID).getName() +" permission set rp.slimefun.air275");
			} else if (this.faithLevels.get(activeFaith) >= 75 && activeFaith.equals("Nature")) {
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"lp user "+ Bukkit.getPlayer(pUUID).getName() +" permission set rp.slimefun.nature75");

			} else if (this.faithLevels.get(activeFaith) >= 100 && activeFaith.equals("Tech")) {
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"lp user "+ Bukkit.getPlayer(pUUID).getName() +" permission set rp.slimefun.tech100");


				if (this.faithLevels.get(activeFaith) >= 200) {
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"lp user "+ Bukkit.getPlayer(pUUID).getName() +" permission set rp.slimefun.tech200");

				}
				if (this.faithLevels.get(activeFaith) >= 300) {
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"lp user "+ Bukkit.getPlayer(pUUID).getName() +" permission set rp.slimefun.tech300");

				}
				if (this.faithLevels.get(activeFaith) >= 400) {
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"lp user "+ Bukkit.getPlayer(pUUID).getName() +" permission set rp.slimefun.tech400");

				}
			} else if (this.faithLevels.get(activeFaith) >= 300 && activeFaith.equals("Aether")) {
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"lp user "+ Bukkit.getPlayer(pUUID).getName() +" permission set rp.slimefun.aether300");
			}
		}
	}

	private void tryForRankItem(Player p, String info) {
		String rank = RunicParadise.perms.getPrimaryGroup(p);

		if ((RunicParadise.playerProfiles.get(p.getUniqueId()).isPlayerFarming())) {
			return;
			// player is farming, don't even try for a drop
		}

		Random rand = new Random();
		int value = rand.nextInt(1000);

		if (rank.equalsIgnoreCase("Master")) {
			if (value < FaithConfig.num(FaithConfig.GLOBAL, "rankItemChance") * 10) { // staff-set % (was 30%)
				if (p.getInventory().firstEmpty() != -1) {
					p.getInventory().addItem(Borderlands.specialLootDrops("DukeEssence", p.getUniqueId()));
					RunicMessaging.sendMessage(p, RunicFormat.RANKS,
							"✦ Your faith crystallized a memory!");
					RunicParadise.playerProfiles.get(p.getUniqueId()).logSpecialRankDrop("DukeEssence",
							"Faith " + info);
				} else {
					RunicMessaging.sendMessage(p, RunicFormat.ERROR,
							"Inventory full! A faith memory could not drop.");
				}

			}

		} else {

		}

	}

	/*
	 * public static void spellSpiritOfTheBeaver(UUID pUUID, Location loc) { int
	 * randomNum = RunicParadise.randomSeed.nextInt((100 - 0) + 1) + 0; if
	 * (randomNum < 6) { // 5% chance
	 * 
	 * // Strength Potion Effect (20 ticks = 10 seconds)
	 * Bukkit.getPlayer(pUUID).addPotionEffect( new
	 * PotionEffect(PotionEffectType.FAST_DIGGING, 12000, 2));
	 * Bukkit.getPlayer(pUUID) .sendMessage( ChatColor.GRAY + "" +
	 * ChatColor.ITALIC +
	 * "The fallen tree empowers you with the spirit of the beaver!"); } else {
	 * Bukkit.getPlayer(pUUID).sendMessage( ChatColor.GRAY + "" +
	 * ChatColor.ITALIC + "Failed to cast Spirit of the Beaver. Rolled " +
	 * randomNum + ", Need <6"); }
	 * 
	 * }
	 * 
	 * public static void spellSpiritOfTheWolf(UUID pUUID, Location loc) {
	 * 
	 * // Runspeed Potion Effect (20 ticks = 10 seconds)
	 * Bukkit.getPlayer(pUUID).addPotionEffect( new
	 * PotionEffect(PotionEffectType.SPEED, 6000, 1)); Bukkit.getPlayer(pUUID)
	 * .sendMessage( ChatColor.GRAY + "" + ChatColor.ITALIC +
	 * "The moon's light empowers you with the spirit of the wolf!");
	 * 
	 * }
	 * 
	 * public static void spellSpiritOfTheTiger(EntityDamageByEntityEvent event,
	 * UUID pUUID, Location loc) { int randomNum =
	 * RunicParadise.randomSeed.nextInt((100 - 0) + 1) + 0; if (randomNum < 6) {
	 * // 5% chance event.setDamage(DamageModifier.BASE,
	 * event.getDamage(DamageModifier.BASE) + 5);
	 * Bukkit.getPlayer(pUUID).sendMessage( ChatColor.GRAY + "" +
	 * ChatColor.ITALIC + "The spirit of the tiger empowers your attack!"); }
	 * else { Bukkit.getPlayer(pUUID).sendMessage( ChatColor.GRAY + "" +
	 * ChatColor.ITALIC + "Failed to cast Spirit of the Tiger. Rolled " +
	 * randomNum + ", Need <6"); }
	 * 
	 * }
	 * 
	 * public static void spellSpiritOfTheMole(UUID pUUID, Location loc) { int
	 * randomNum = RunicParadise.randomSeed.nextInt((100 - 0) + 1) + 0; if
	 * (randomNum < 3) { // 2% chance
	 * 
	 * // DigSpeed Potion Effect (20 ticks = 10 seconds)
	 * Bukkit.getPlayer(pUUID).addPotionEffect( new
	 * PotionEffect(PotionEffectType.FAST_DIGGING, 6000, 1));
	 * Bukkit.getPlayer(pUUID).addPotionEffect( new
	 * PotionEffect(PotionEffectType.NIGHT_VISION, 6000, 1));
	 * Bukkit.getPlayer(pUUID).sendMessage( ChatColor.GRAY + "" +
	 * ChatColor.ITALIC + "The spirit of the mole empowers your arms!"); } else
	 * { Bukkit.getPlayer(pUUID).sendMessage( ChatColor.GRAY + "" +
	 * ChatColor.ITALIC + "Failed to cast Spirit of the Mole. Rolled " +
	 * randomNum + ", Need <3"); }
	 * 
	 * }
	 */
}
