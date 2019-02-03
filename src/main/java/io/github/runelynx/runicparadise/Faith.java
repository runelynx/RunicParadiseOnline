package io.github.runelynx.runicparadise;

import io.github.runelynx.runicuniverse.RunicMessaging;
import io.github.runelynx.runicuniverse.RunicMessaging.RunicFormat;
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
import java.util.*;
import java.util.Date;
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

		if (p.hasPermission("rp.faith.sun")) {
			faithInventory.setItem(3, sun);
		} else {
			faithInventory.setItem(3, blocked);
		}
		if (p.hasPermission("rp.faith.moon")) {
			faithInventory.setItem(4, moon);
		} else {
			faithInventory.setItem(4, blocked);
		}
		if (p.hasPermission("rp.faith.fire")) {
			faithInventory.setItem(5, flame);
		} else {
			faithInventory.setItem(5, blocked);
		}

		if (p.hasPermission("rp.faith.water")) {
			faithInventory.setItem(12, water);
		} else {
			faithInventory.setItem(12, blocked);
		}
		if (p.hasPermission("rp.faith.air")) {
			faithInventory.setItem(13, air);
		} else {
			faithInventory.setItem(13, blocked);
		}
		if (p.hasPermission("rp.faith.earth")) {
			faithInventory.setItem(14, earth);
		} else {
			faithInventory.setItem(14, blocked);
		}

		if (p.hasPermission("rp.faith.aether")) {
			faithInventory.setItem(21, aether);
		} else {
			faithInventory.setItem(21, blocked);
		}
		if (p.hasPermission("rp.faith.nether")) {
			faithInventory.setItem(22, nether);
		} else {
			faithInventory.setItem(22, blocked);
		}
		if (p.hasPermission("rp.faith.nature")) {
			faithInventory.setItem(23, nature);
		} else {
			faithInventory.setItem(23, blocked);
		}

		if (p.hasPermission("rp.faith.tech")) {
			faithInventory.setItem(30, tech);
		} else {
			faithInventory.setItem(30, blocked);
		}
		if (p.hasPermission("rp.faith.fate")) {
			faithInventory.setItem(31, fate);
		} else {
			faithInventory.setItem(31, blocked);
		}
		if (p.hasPermission("rp.faith.time")) {
			faithInventory.setItem(32, time);
		} else {
			faithInventory.setItem(32, blocked);
		}

		if (p.hasPermission("rp.faith.stars")) {
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
/*
							Bukkit.getPlayer(this.getUUID()).sendMessage( new ComponentBuilder( "   Level " ).color( ChatColor.BLUE ).
									append(powerResult.getString("RequiredLevel")).color( levelColor ).
									append( ": " ).color( ChatColor.WHITE ).
									.create() );

							.sendMessage(ChatColor.BLUE + "   Level " + levelColor
									+ powerResult.getString("RequiredLevel") + ChatColor.WHITE + ": " + levelColor
									+ powerResult.getString("PowerName"));
			*/
							
									/*
									.tooltip(
											powerResult.getString("Description").substring(0,
													powerResult.getString("Description").length() / 2),
											powerResult.getString("Description").substring(
													powerResult.getString("Description").length() / 2,
													powerResult.getString("Description").length()))
									.*/

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
				// ensure the faith is ACTIVE
				return new RunicPlayerBukkit(this.getUUID()).getActiveFaith().equals(faithName);
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

				while (faithResult.next()) {

					String color;
					if (faithResult.getString("FaithName").equals(new RunicPlayerBukkit(nUUID).getActiveFaith())) {
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
		this.playerName = new RunicPlayerBukkit(nUUID).getPlayerName();

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
					this.primaryFaithName = new RunicPlayerBukkit(nUUID).getActiveFaith();
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
		if (this.faithLevels.containsKey(faithName)) {
			this.faithLevels.put(faithName, this.faithLevels.get(faithName) + 1);
		} else {
			// Given faith is invalid for this player, so stop here!
			return false;
		}

		try {
			Connection connection = MySQL.openConnection();

			PreparedStatement statement = connection
					.prepareStatement("UPDATE rp_PlayerFaiths SET Level=Level+1 WHERE UUID = ? AND FaithName = ?");
			statement.setString(1, this.playerUUID);
			statement.setString(2, faithName);
			statement.executeUpdate();

			connection.close();

			p.sendMessage(ChatColor.GRAY + "[" + ChatColor.BLUE + "Runic" + ChatColor.DARK_AQUA + "Faith"
					+ ChatColor.GRAY + "] " + ChatColor.BLUE + "Your " + faithName + ChatColor.BLUE
					+ " faith grows stronger! " + ChatColor.GREEN + this.faithLevels.get(faithName) + ChatColor.GRAY
					+ "/" + RunicParadise.faithSettingsMap.get(faithName)[4]);

			if (this.checkEquippedFaithLevel(faithName,
					Integer.parseInt(RunicParadise.faithSettingsMap.get(faithName)[4]))) {
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

	public static void tryCast_PlayerTookFallDamage(Player p) {
		Faith pfo = RunicParadise.faithMap.get(p.getUniqueId());
		String faith = pfo.getPrimaryFaith();
		int level = pfo.getPrimaryFaithLevel();

		switch (faith) {

		case "Aether":
			if (level > RunicParadise.powerReqsMap.get("Graceful Steps")) {
				// Cast Graceful Steps if player took fall damage
				pfo.castAether_GracefulSteps(p);
			}

			break;
		default:
			break;
		}

	}

	public static int tryCast_CropsAreGrowing(BlockGrowEvent event, Player p) {
		Faith pfo = RunicParadise.faithMap.get(p.getUniqueId());
		String faith = pfo.getPrimaryFaith();
		int level = pfo.getPrimaryFaithLevel();

		switch (faith) {

		case "Nature":
			if (level > RunicParadise.powerReqsMap.get("Wild Growth")) {
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

		Faith pfo = RunicParadise.faithMap.get(p.getUniqueId());
		String faith = pfo.getPrimaryFaith();
		int level = pfo.getPrimaryFaithLevel();
		final boolean daytime = (Bukkit.getWorld("RunicKingdom").getTime() <= 14000 || Bukkit.getWorld("RunicKingdom").getTime() >= 23000);

		if (!(event.getCause() == DamageCause.ENTITY_EXPLOSION && event.getCause() == DamageCause.ENTITY_ATTACK
				&& event.getCause() == DamageCause.PROJECTILE)) {
			// the damage was not caused by an entity! abort!
			return;
		}

		switch (faith) {

		case "Sun":
			if (level > RunicParadise.powerReqsMap.get("Sunflare") && daytime) {

				// Cast Sunflare if player got hit and its daytime
				if (p.getWorld().getName().equals("RunicKingdom")) {
					pfo.castSun_Sunflare(p.getUniqueId(), p);
				}

			}

			break;
		case "Moon":
			if (level > RunicParadise.powerReqsMap.get("Lunar Calm") && !daytime) {

				// Cast Lunar Calm if player got hit and its night
				if (p.getWorld().getName().equals("RunicKingdom")) {
					pfo.castSun_Sunflare(p.getUniqueId(), p);
				}

			}
			break;
		case "Flame":
			if (level > RunicParadise.powerReqsMap.get("Unstable Embers")) {

				// Cast Unstable Embers if player got hit
				if (p.getWorld().getName().equals("RunicKingdom")) {
					pfo.castFlame_UnstableEmbers(p);
				}
			}

			break;
		case "Water":
			if (level > RunicParadise.powerReqsMap.get("Arctic Frost")) {

				// Cast Unstable Embers if player got hit
				if (p.getWorld().getName().equals("RunicKingdom")) {
					pfo.castWater_ArcticFrost(p);
				}
			}

			if (level > RunicParadise.powerReqsMap.get("Protective Bubble")) {

				// Cast Unstable Embers if player got hit
				if (p.getWorld().getName().equals("RunicKingdom")) {
					pfo.castWater_ProtectiveBubble(p);
				}
			}

			break;
		case "Air":
			if (level > RunicParadise.powerReqsMap.get("Healing Breeze")) {

				// Cast Healing Breeze if player got hit
				if (p.getWorld().getName().equals("RunicKingdom")) {
					pfo.castAir_HealingBreeze(p);
				}
			}

			break;
		case "Time":
			if (level > RunicParadise.powerReqsMap.get("Rewind")) {

				// Cast Rewind if player got hit
				pfo.castTime_Rewind(p);

			}

			break;
		case "Nature":
			if (level > RunicParadise.powerReqsMap.get("Forest Armor")) {

				// Cast Forest Armor if player got hit
				pfo.castNature_ForestArmor(p);

			}
			break;
		default:
			break;
		}

	}

	public static void tryCast_PlayerTeleported(final PlayerTeleportEvent event) {

		Faith pfo = RunicParadise.faithMap.get(event.getPlayer().getUniqueId());
		String faith = pfo.getPrimaryFaith();
		int level = pfo.getPrimaryFaithLevel();

		switch (faith) {

		case "Nether":
			if (level > RunicParadise.powerReqsMap.get("Netherborn")) {

				// Cast Netherborn if player is going into the nether
				if (event.getTo().getWorld().getName().equals("RunicKingdom_nether")
						&& !event.getFrom().getWorld().getName().equals("RunicKingdom_nether")) {
					pfo.castNether_Netherborn(event.getPlayer());
					// Remove Netherborn if player is leaving the nether
				} else if (event.getFrom().getWorld().getName().equals("RunicKingdom_nether")) {
					event.getPlayer().removePotionEffect(PotionEffectType.HEALTH_BOOST);
					event.getPlayer().removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
				}
			}

			break;
		default:
			break;
		}
	}

	public static void tryCast_PlayerHitMonster(EntityDamageByEntityEvent event, Entity monster, Player p) {
		Faith pfo = RunicParadise.faithMap.get(p.getUniqueId());
		String faith = pfo.getPrimaryFaith();
		int level = pfo.getPrimaryFaithLevel();
		final boolean daytime = (Bukkit.getWorld("RunicKingdom").getTime() <= 14000 || Bukkit.getWorld("RunicKingdom").getTime() >= 23000);

		switch (faith) {

		case "Sun":
			if (level > RunicParadise.powerReqsMap.get("Solar Power") && daytime) {

				// Cast Solar Power if player hit a monster
				if (p.getWorld().getName().equals("RunicKingdom")) {
					pfo.castSun_SolarPower(p.getUniqueId(), p);
				}

			}

			break;
		case "Fire":
			if (level > RunicParadise.powerReqsMap.get("Volcanic Fury") && daytime) {

				// Cast Volcanic Fury if player hit a monster
				if (p.getWorld().getName().equals("RunicKingdom")) {
					pfo.castSun_SolarPower(p.getUniqueId(), p);
				}

			}

			break;
		case "Moon":
			if (level > RunicParadise.powerReqsMap.get("Celestial Healing") && !daytime) {

				// Cast Celestial Healing if player got hit and its night
				if (p.getWorld().getName().equals("RunicKingdom")) {
					pfo.castMoon_CelestialHealing(p.getUniqueId(), p);
				}

			}

			break;

		case "Aether":
			if (level > RunicParadise.powerReqsMap.get("Gravity Flux")) {

				// Cast Gravity Flux if player hit a monster
				pfo.castAether_GravityFlux(p);

			}

			break;

		case "Air":
			if (level > RunicParadise.powerReqsMap.get("Tempest Armor")) {

				// Cast Tempest Armor
				pfo.castAir_TempestArmor(p);
			}

			break;
		case "Fate":

			double newHealthRatio = (((LivingEntity) monster).getMaxHealth() - event.getDamage())
					/ ((LivingEntity) monster).getMaxHealth();

			// Check if mob is a monster, but not a wither (EnderDragon is not
			// Monster) --- Check for MONSTER was done in the event call itself
			// !!
			// and that new health of mob is <20%
			// and that player has proper Fate level

			if (level > RunicParadise.powerReqsMap.get("Inevitable Demise")) {
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
		Faith pfo = RunicParadise.faithMap.get(p.getUniqueId());
		String faith = pfo.getPrimaryFaith();
		int level = pfo.getPrimaryFaithLevel();

		if (faith == null) {
			Bukkit.getLogger().log(Level.WARNING,
					"Error in tryCast_PlayerKilledMonster - failed to find faith object for " + p.getName());
			return;
		}

		switch (faith) {

		case "Nether":
			if (level > RunicParadise.powerReqsMap.get("Vampirism")) {
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
			if (randomNum <= (5 * chance)) {
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
			// Player is maxxed
			p.sendMessage(ChatColor.GRAY + "[" + ChatColor.BLUE + "Runic" + ChatColor.DARK_AQUA + "Faith"
					+ ChatColor.GRAY + "] " + ChatColor.BLUE + "Your " + faithName + ChatColor.BLUE
					+ " faith cannot grow any stronger!");
			return 0;
		}
	}

	private int trySkillUpViaPrayer(Player p, String faithName, boolean withKarma) {

		if (!this.checkEquippedFaithLevel(faithName,
				Integer.parseInt(RunicParadise.faithSettingsMap.get(faithName)[4]))) {

			int counter = 0;
			int limit;

			if (withKarma) {
				limit = 3;
				while (counter < limit) {
					this.incrementSkill(p, faithName);
					if (this.checkEquippedFaithLevel(faithName,
							Integer.parseInt(RunicParadise.faithSettingsMap.get(faithName)[4]))) {
						return 1;
					}
					counter++;
				}

			} else {
				limit = 1;

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

	private void sendCastMessage(Player p, String spellName, String faithName) {
		p.sendMessage(ChatColor.GRAY + "[" + ChatColor.BLUE + "Runic" + ChatColor.DARK_AQUA + "Faith" + ChatColor.GRAY
				+ "] " + ChatColor.BLUE + RunicParadise.faithSettingsMap.get(faithName)[5] + " Casting "
				+ ChatColor.GRAY + spellName + ChatColor.BLUE + "!");

	}

	private void castSun_Sunflare(UUID pUUID, Player p) {
		// 2% chance on receiving damage to set all nearby monsters on fire
		String spellName = "Sunflare";
		if ((RunicParadise.randomSeed.nextInt(101)) <= 2) {
			List<Entity> nearby = p.getNearbyEntities(8, 8, 8);
			for (Entity tmp : nearby) {
				if (tmp instanceof Monster)
					tmp.setFireTicks(60);
			}
			nearby = null;
			sendCastMessage(p, spellName, "Sun");
			tryForRankItem(p, "Sun Sunflare");
			getLogger().log(Level.INFO, "Faith Info: " + p.getName() + " just cast " + spellName + "!");
		}
	}

	@SuppressWarnings("deprecation")
	public void castMoon_LunarCalm(UUID pUUID, final Player p) {
		// 1% chance to stop mobs from targeting player for 4 seconds
		String spellName = "Lunar Calm";
		if ((RunicParadise.randomSeed.nextInt(101)) <= 1) {

			// register protection now
			RunicParadise.protectedPlayers.put(p.getUniqueId(), p.getDisplayName());

			// schedule removal of protection 80 tickets out
			Bukkit.getServer().getScheduler().scheduleAsyncDelayedTask(instance, () -> RunicParadise.protectedPlayers.remove(p.getUniqueId()), 100);

			// stop all nearby monsters from targeting players
			final List<Entity> nearby = p.getNearbyEntities(15, 15, 15);
			for (Entity tmp : nearby) {
				if (tmp instanceof Monster) {
					((Monster) tmp).setTarget(null);
					((Monster) tmp).setTarget(null);
				}
			}

			// schedule removal of protection 80 tickets out
			Bukkit.getServer().getScheduler().scheduleAsyncDelayedTask(instance, () -> {
				for (Entity tmp : nearby) {
					if (tmp instanceof Monster) {
						((Monster) tmp).setTarget(null);
						((Monster) tmp).setTarget(null);
					}
				}
			}, 20);
			Bukkit.getServer().getScheduler().scheduleAsyncDelayedTask(instance, () -> {
				for (Entity tmp : nearby) {
					if (tmp instanceof Monster) {
						((Monster) tmp).setTarget(null);
						((Monster) tmp).setTarget(null);
					}
				}
			}, 40);
			Bukkit.getServer().getScheduler().scheduleAsyncDelayedTask(instance, () -> {
				for (Entity tmp : nearby) {
					if (tmp instanceof Monster) {
						((Monster) tmp).setTarget(null);
						((Monster) tmp).setTarget(null);
					}
				}
			}, 60);

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
		if ((RunicParadise.randomSeed.nextInt(101)) <= 2) {
			p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 300, 1));
			p.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 300, 1));

			sendCastMessage(p, spellName, "Sun");

			tryForRankItem(p, "Sun SolarPower");
			getLogger().log(Level.INFO, "Faith Info: " + p.getName() + " just cast " + spellName + "!");
		}
	}

	public void castFlame_VolcanicFury(UUID pUUID, Player p) {
		// 2% chance to +damage when dealing damage to monsters
		String spellName = "Volcanic Fury";
		if ((RunicParadise.randomSeed.nextInt(101)) <= 4) {
			p.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 150, 2));

			sendCastMessage(p, spellName, "Fire");

			tryForRankItem(p, "Fire VolcanicFury");
			getLogger().log(Level.INFO, "Faith Info: " + p.getName() + " just cast " + spellName + "!");

		}
	}

	private void castMoon_CelestialHealing(UUID pUUID, Player p) {
		// 1% chance to gain regeneration
		String spellName = "Celestial Healing";
		if ((RunicParadise.randomSeed.nextInt(101)) <= 2) {
			p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 300, 0));
			p.addPotionEffect(new PotionEffect(PotionEffectType.HEAL, 1, 0));

			sendCastMessage(p, spellName, "Moon");
			tryForRankItem(p, "Moon CelestialHealing");
			getLogger().log(Level.INFO, "Faith Info: " + p.getName() + " just cast " + spellName + "!");
		}
	}

	private void castFate_InevitableDemise(UUID pUUID, Player player, LivingEntity monster) {
		if (player.getHealth() <= 6) {
			Random rand = new Random();
			int number = rand.nextInt(100) + 1;
			if (number >= 80) {

				tryForRankItem(player, "Fate InevitableDemise");

				for (final Entity entity : RunicUtilities.getTargetList(player.getLocation(), 2)) {
					if (((entity instanceof LivingEntity)) && (entity != player)) {
						((LivingEntity) entity).setHealth(0);
					}
				}
			}
		}

	}

	private void castTime_Rewind(Player p) {
		Random rand1 = new Random();
		if (rand1.nextInt(100) + 1 <= 10) {
			if (p.getHealth() <= 3) {
				Random rand = new Random();
				int number = rand.nextInt(100) + 1;
				if (number >= 80) {
					p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 200, 1));
					p.setHealth(16);
					tryForRankItem(p, "Time Rewind");
				}
			}
		}
	}

	void castNether_Netherborn(Player p) {
		// Boost player whenever theyre in the nether
		String spellName = "Netherborn";

		p.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, NETHER_NETHERBORN_TIMING, 1));
		p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, NETHER_NETHERBORN_TIMING, 0));
		sendCastMessage(p, spellName, "Nether");
		getLogger().log(Level.INFO, "Faith Info: " + p.getName() + " just cast " + spellName + "!");
	}

	// Flame : Unstable Embers
	private void castFlame_UnstableEmbers(Player p) {
		Random rand = new Random();
		int number = rand.nextInt(100) + 1;
		if (number >= 90) {
			String spellName = "Unstable Embers";
			sendCastMessage(p, spellName, "Fire");
			tryForRankItem(p, "Flame UnstableEmbers");

			for (Entity entity : RunicUtilities.getTargetList(p.getLocation(), 4)) {
				if (((entity instanceof LivingEntity)) && (entity != p)) {
					((LivingEntity) entity).damage(4);
				}
			}
		}
	}

	// ==========================
	// Wind : TempestArmor

	@SuppressWarnings("deprecation")
	private void castAir_TempestArmor(Player player) {
		ItemStack i = player.getInventory().getHelmet();
		ItemStack j = player.getInventory().getChestplate();
		ItemStack k = player.getInventory().getLeggings();
		ItemStack l = player.getInventory().getBoots();
		if (i != null && j != null && k != null && l != null) {
			if (i.getType() == Material.GOLDEN_HELMET && j.getType() == Material.GOLDEN_CHESTPLATE
					&& k.getType() == Material.GOLDEN_LEGGINGS && l.getType() == Material.GOLDEN_BOOTS) {
				Random rand = new Random();
				int number = rand.nextInt(100) + 1;
				if (number >= 96) {

					String spellName = "Tempest Armor";
					sendCastMessage(player, spellName, "Air");
					tryForRankItem(player, "Air TempestArmor");

					for (final Entity entity : RunicUtilities.getTargetList(player.getLocation(), 5)) {
						if (((entity instanceof Monster)) && (entity != player)) {
							entity.setVelocity(entity.getLocation().getDirection().multiply(2.5D).setY(0.7D));
							final World w = entity.getWorld();
							Bukkit.getServer().getScheduler().scheduleAsyncDelayedTask(instance, () -> w.strikeLightning(entity.getLocation()), 20);
						}
					}
				}
			}
		}
	}

	// ==========================
	// Wind : Healing Breeze

	private void castAir_HealingBreeze(Player player) {
		Random rand = new Random();
		int number = rand.nextInt(100) + 1;
		if (number >= 97) {
			for (Entity entity : RunicUtilities.getTargetList(player.getLocation(), 40)) {
				if (entity instanceof Player) {
					String spellName = "Healing Breeze";
					sendCastMessage(player, spellName, "Air");

					tryForRankItem(player, "Air HealingBreeze");

					((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 1));
					entity.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "You feel refreshed by a soothing breeze");
				}
			}
		}
	}

	// ==========================
	// Earth : Earth's Bounty

	void castEarth_EarthsBounty(PlayerInteractEvent e) {

		Player p = e.getPlayer();
		if ((p.getItemInHand() != null) && (p.getItemInHand().getType().equals(Material.GLOWSTONE_DUST))) {
			ItemStack glowstone = p.getItemInHand();
			if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
				Block block = e.getClickedBlock();
				if (block.getType() == Material.STONE) {
					if (p.getLevel() >= 2) {
						int removel = 2;
						int plevel = p.getLevel() - removel;
						p.setLevel(plevel);
						p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
						int radius = 15;
						Location location = p.getLocation();
						int diamonds = 0;
						int emeralds = 0;
						Block center = location.getBlock().getRelative(p.getFacing(), radius);
						for (int x = -radius; x < radius; x++) {
							for (int y = -radius; y < radius; y++) {
								for (int z = -radius; z < radius; z++) {
									if (center.getRelative(x, y, z).getType() == Material.DIAMOND_ORE) {
										diamonds++;
									}
									if (center.getRelative(x, y, z).getType() == Material.EMERALD_ORE) {
										emeralds++;
									}
								}
							}
						}
						String spellName = "Earths Bounty";
						sendCastMessage(e.getPlayer(), spellName, "Earth");

						p.sendMessage(ChatColor.GREEN + "There are " + diamonds + " diamond(s) and " + emeralds
								+ " emerald(s) near you!");

					}
				}
			}
		}
	}

	// ==========================
	// Water OneWithTheSea

	void castWater_DeepWader(PlayerInteractEvent e) {
		Player p = e.getPlayer();
		if ((p.getItemInHand() != null) && (p.getItemInHand().getType().equals(Material.GLOWSTONE_DUST))) {

			Block block = e.getPlayer().getLocation().getBlock();
			// TODO: might need fixing
			if (block.getType() == Material.WATER || block.getType() == Material.WATER) {
				if (p.getLevel() >= 2) {
					int removel = 2;
					int plevel = p.getLevel() - removel;
					p.setLevel(plevel);
					p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
					p.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 12000, 1));

					String spellName = "Deep Wader";

					sendCastMessage(e.getPlayer(), spellName, "Water");
					tryForRankItem(e.getPlayer(), "Water DeepWader");
				}
			}
		}
	}

	// ===========================
	// Water : Protective Bubble

	private void castWater_ProtectiveBubble(final Player player) {
		Random rand = new Random();
		int number = rand.nextInt(100) + 1;
		if (number >= 97) {
			player.removePotionEffect(PotionEffectType.ABSORPTION);
			player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 300, 0));
			String spellName = "Protective Bubble";

			sendCastMessage(player, spellName, "Water");
			tryForRankItem(player, "Water ProtectiveBubble");

			Bukkit.getServer().getScheduler().scheduleAsyncDelayedTask(instance,
					() -> player.removePotionEffect(PotionEffectType.ABSORPTION), 320);
		}
	}

	// ==========================
	// Nether : Vampirism

	private void castNether_Vampirism(EntityDeathEvent event) {
		Entity e = event.getEntity();

		EntityDamageByEntityEvent nEvent = (EntityDamageByEntityEvent) e.getLastDamageCause();

		Random rand = new Random();
		int number = rand.nextInt(100) + 1;
		if (number >= 80) {
			Player p = (Player) nEvent.getDamager();

			if (p.getHealth() >= p.getMaxHealth() - 3) {
				p.setHealth(p.getMaxHealth());
			} else {
				p.setHealth(p.getHealth() + 3);
			}
			String spellName = "Vampirism";
			sendCastMessage(p, spellName, "Nether");
			tryForRankItem(p, "Nether Vampirism");
		}
	}

	// ==========================
	// Aether : Gravity Flux

	private void castAether_GravityFlux(Player p) {
		Random rand = new Random();
		int number = rand.nextInt(100) + 1;
		if (number <= 4) {
			String spellName = "Gravity Flux";
			sendCastMessage(p, spellName, "Aether");
			tryForRankItem(p, "Aether GravityFlux");

			for (Entity e : p.getNearbyEntities(7, 7, 7)) {
				if (e instanceof Monster) {
					e.setVelocity(new Vector(e.getVelocity().getX(), 1, e.getVelocity().getZ()));
				}
			}
		}
	}

	// ==========================
	// Aether : Graceful Steps

	private void castAether_GracefulSteps(Player p) {
		Random rand = new Random();
		int number = rand.nextInt(100) + 1;
		if (number <= 20) {
			String spellName = "Graceful Steps";
			sendCastMessage(p, spellName, "Aether");

			// tryForRankItem(p, "Aether GracefulSteps");

			p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 300, 0));
		}
	}

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

	@SuppressWarnings("deprecation")
	private void castNature_ForestArmor(Player player) {
		ItemStack i = player.getInventory().getHelmet();
		ItemStack j = player.getInventory().getChestplate();
		ItemStack k = player.getInventory().getLeggings();
		ItemStack l = player.getInventory().getBoots();
		if (i != null && j != null && k != null && l != null) {
			if (i.getType() == Material.LEATHER_HELMET && j.getType() == Material.LEATHER_CHESTPLATE
					&& k.getType() == Material.LEATHER_LEGGINGS && l.getType() == Material.LEATHER_BOOTS) {
				Random rand = new Random();
				int number = rand.nextInt(100) + 1;
				if (number >= 96) {
					String spellName = "Forest Armor";
					sendCastMessage(player, spellName, "Nature");
					final Wolf wolf1 = (Wolf) player.getWorld().spawnEntity(player.getLocation(), EntityType.WOLF);
					final Wolf wolf2 = (Wolf) player.getWorld().spawnEntity(player.getLocation(), EntityType.WOLF);
					wolf1.setOwner(player);
					wolf1.setAdult();
					wolf1.setTamed(true);
					wolf1.setCustomName(ChatColor.YELLOW + player.getName() + "'s Wolf");
					wolf1.setCustomNameVisible(true);
					wolf2.setOwner(player);
					wolf2.setAdult();
					wolf2.setTamed(true);
					wolf2.setCustomName(ChatColor.YELLOW + player.getName() + "'s Wolf");
					wolf2.setCustomNameVisible(true);
					Bukkit.getServer().getScheduler().scheduleAsyncDelayedTask(instance, () -> {
						wolf1.setHealth(0);
						wolf2.setHealth(0);
					}, 400);
				}
			}
		}
	}

	// =========================
	// Water :: Arctic Frost

	@SuppressWarnings("deprecation")
	private void castWater_ArcticFrost(Player p) {
		Random rand = new Random();
		int number = rand.nextInt(100) + 1;
		if (number <= 3) {
			String spellName = "Arctic Frost";
			sendCastMessage(p, spellName, "Water");

			tryForRankItem(p, "Water ArcticFrost");

			for (Entity e : p.getNearbyEntities(7, 7, 7)) {
				if (e instanceof Monster) {
					((Monster) e).addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 150, 2));
				}
			}
		}
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

		ItemStack item1 = new ItemStack(
				RunicUtilities.idToMaterial(Integer.parseInt(RunicParadise.prayerBooks.get(locKey)[5])),
				Integer.parseInt(RunicParadise.prayerBooks.get(locKey)[7]),
				Short.parseShort((RunicParadise.prayerBooks.get(locKey)[6])));
		ItemStack[] item1a = new ItemStack[] {
				new ItemStack(RunicUtilities.idToMaterial(Integer.parseInt(RunicParadise.prayerBooks.get(locKey)[5])),
						Integer.parseInt(RunicParadise.prayerBooks.get(locKey)[7]),
						Short.parseShort((RunicParadise.prayerBooks.get(locKey)[6]))) };
		ItemStack item2 = new ItemStack(
				RunicUtilities.idToMaterial(Integer.parseInt(RunicParadise.prayerBooks.get(locKey)[8])),
				Integer.parseInt(RunicParadise.prayerBooks.get(locKey)[10]),
				Short.parseShort((RunicParadise.prayerBooks.get(locKey)[9])));
		ItemStack[] item2a = new ItemStack[] {
				new ItemStack(RunicUtilities.idToMaterial(Integer.parseInt(RunicParadise.prayerBooks.get(locKey)[8])),
						Integer.parseInt(RunicParadise.prayerBooks.get(locKey)[10]),
						Short.parseShort((RunicParadise.prayerBooks.get(locKey)[9]))) };
		ItemStack item3 = new ItemStack(
				RunicUtilities.idToMaterial(Integer.parseInt(RunicParadise.prayerBooks.get(locKey)[11])),
				Integer.parseInt(RunicParadise.prayerBooks.get(locKey)[13]),
				Short.parseShort((RunicParadise.prayerBooks.get(locKey)[12])));
		ItemStack[] item3a = new ItemStack[] {
				new ItemStack(RunicUtilities.idToMaterial(Integer.parseInt(RunicParadise.prayerBooks.get(locKey)[11])),
						Integer.parseInt(RunicParadise.prayerBooks.get(locKey)[13]),
						Short.parseShort((RunicParadise.prayerBooks.get(locKey)[12]))) };
		ItemStack item4 = new ItemStack(
				RunicUtilities.idToMaterial(Integer.parseInt(RunicParadise.prayerBooks.get(locKey)[14])),
				Integer.parseInt(RunicParadise.prayerBooks.get(locKey)[16]),
				Short.parseShort((RunicParadise.prayerBooks.get(locKey)[15])));
		ItemStack[] item4a = new ItemStack[] {
				new ItemStack(RunicUtilities.idToMaterial(Integer.parseInt(RunicParadise.prayerBooks.get(locKey)[14])),
						Integer.parseInt(RunicParadise.prayerBooks.get(locKey)[16]),
						Short.parseShort((RunicParadise.prayerBooks.get(locKey)[15]))) };

		switch (numItems) {
		case 1:
			if (p.getInventory().contains(item1)) {
				// Player has enough of first item!

				if (needKarma && targetPlayer.getKarma() >= 2) {
					// Player is trying to pray at a Book - costs 2 karma! they
					// have enough karma, so take it!

					if (RunicParadise.faithMap.get(p.getUniqueId()).trySkillUpViaPrayer(p,
							RunicParadise.faithMap.get(p.getUniqueId()).getPrimaryFaith(), true) == 1) {
						targetPlayer.adjustPlayerKarma(-2);
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
					p.sendMessage("This prayer costs 2 karma; you don't have enough!!");
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

				if (needKarma && targetPlayer.getKarma() >= 2) {
					// Player is trying to pray at a Book - costs 2 karma! they
					// have enough karma, so take it!

					if (RunicParadise.faithMap.get(p.getUniqueId()).trySkillUpViaPrayer(p,
							RunicParadise.faithMap.get(p.getUniqueId()).getPrimaryFaith(), true) == 1) {
						targetPlayer.adjustPlayerKarma(-2);
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
					p.sendMessage("This prayer costs 2 karma; you don't have enough!!");
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

				if (needKarma && targetPlayer.getKarma() >= 2) {
					// Player is trying to pray at a Book - costs 2 karma! they
					// have enough karma, so take it!

					if (RunicParadise.faithMap.get(p.getUniqueId()).trySkillUpViaPrayer(p,
							RunicParadise.faithMap.get(p.getUniqueId()).getPrimaryFaith(), true) == 1) {
						targetPlayer.adjustPlayerKarma(-2);
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
					p.sendMessage("This prayer costs 2 karma; you don't have enough!!");
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

				if (needKarma && targetPlayer.getKarma() >= 2) {
					// Player is trying to pray at a Book - costs 2 karma! they
					// have enough karma, so take it!

					if (RunicParadise.faithMap.get(p.getUniqueId()).trySkillUpViaPrayer(p,
							RunicParadise.faithMap.get(p.getUniqueId()).getPrimaryFaith(), true) == 1) {
						targetPlayer.adjustPlayerKarma(-2);
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
					p.sendMessage("This prayer costs 2 karma; you don't have enough!!");
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

			RunicParadise.perms.playerRemove(p, "rp.slimefun.sun100");
			RunicParadise.perms.playerRemove(p, "rp.slimefun.sun250");
			RunicParadise.perms.playerRemove(p, "rp.slimefun.moon100");
			RunicParadise.perms.playerRemove(p, "rp.slimefun.moon250");
			RunicParadise.perms.playerRemove(p, "rp.slimefun.earth150");
			RunicParadise.perms.playerRemove(p, "rp.slimefun.earth250");
			RunicParadise.perms.playerRemove(p, "rp.slimefun.earth375");
			RunicParadise.perms.playerRemove(p, "rp.slimefun.nether300");
			RunicParadise.perms.playerRemove(p, "rp.slimefun.nether375");
			RunicParadise.perms.playerRemove(p, "rp.slimefun.water300");
			RunicParadise.perms.playerRemove(p, "rp.slimefun.water375");
			RunicParadise.perms.playerRemove(p, "rp.slimefun.fire275");
			RunicParadise.perms.playerRemove(p, "rp.slimefun.fire350");
			RunicParadise.perms.playerRemove(p, "rp.slimefun.air275");
			RunicParadise.perms.playerRemove(p, "rp.slimefun.nature75");
			RunicParadise.perms.playerRemove(p, "rp.slimefun.tech100");
			RunicParadise.perms.playerRemove(p, "rp.slimefun.tech200");
			RunicParadise.perms.playerRemove(p, "rp.slimefun.tech300");
			RunicParadise.perms.playerRemove(p, "rp.slimefun.tech400");
			RunicParadise.perms.playerRemove(p, "rp.slimefun.aether300");

			if (this.faithLevels.get(activeFaith) >= 100 && activeFaith.equals("Sun")) {
				RunicParadise.perms.playerAdd(p, "rp.slimefun.sun100");

				if (this.faithLevels.get(activeFaith) >= 250) {
					RunicParadise.perms.playerAdd(p, "rp.slimefun.sun250");
				}
			} else if (this.faithLevels.get(activeFaith) >= 100 && activeFaith.equals("Moon")) {
				RunicParadise.perms.playerAdd(p, "rp.slimefun.moon100");

				if (this.faithLevels.get(activeFaith) >= 250) {
					RunicParadise.perms.playerAdd(p, "rp.slimefun.moon250");
				}
			} else if (this.faithLevels.get(activeFaith) >= 150 && activeFaith.equals("Earth")) {
				RunicParadise.perms.playerAdd(p, "rp.slimefun.earth150");

				if (this.faithLevels.get(activeFaith) >= 250) {
					RunicParadise.perms.playerAdd(p, "rp.slimefun.earth250");
				}
				if (this.faithLevels.get(activeFaith) >= 375) {
					RunicParadise.perms.playerAdd(p, "rp.slimefun.earth375");
				}

			} else if (this.faithLevels.get(activeFaith) >= 300 && activeFaith.equals("Nether")) {
				RunicParadise.perms.playerAdd(p, "rp.slimefun.nether300");

				if (this.faithLevels.get(activeFaith) >= 375) {
					RunicParadise.perms.playerAdd(p, "rp.slimefun.nether375");
				}
			} else if (this.faithLevels.get(activeFaith) >= 300 && activeFaith.equals("Water")) {
				RunicParadise.perms.playerAdd(p, "rp.slimefun.water300");

				if (this.faithLevels.get(activeFaith) >= 375) {
					RunicParadise.perms.playerAdd(p, "rp.slimefun.water375");
				}
			} else if (this.faithLevels.get(activeFaith) >= 275 && activeFaith.equals("Fire")) {
				RunicParadise.perms.playerAdd(p, "rp.slimefun.fire275");

				if (this.faithLevels.get(activeFaith) >= 350) {
					RunicParadise.perms.playerAdd(p, "rp.slimefun.fire350");
				}
			} else if (this.faithLevels.get(activeFaith) >= 275 && activeFaith.equals("Air")) {
				RunicParadise.perms.playerAdd(p, "rp.slimefun.air275");
			} else if (this.faithLevels.get(activeFaith) >= 75 && activeFaith.equals("Nature")) {
				RunicParadise.perms.playerAdd(p, "rp.slimefun.nature75");
			} else if (this.faithLevels.get(activeFaith) >= 100 && activeFaith.equals("Tech")) {
				RunicParadise.perms.playerAdd(p, "rp.slimefun.tech100");

				if (this.faithLevels.get(activeFaith) >= 200) {
					RunicParadise.perms.playerAdd(p, "rp.slimefun.tech200");
				}
				if (this.faithLevels.get(activeFaith) >= 300) {
					RunicParadise.perms.playerAdd(p, "rp.slimefun.tech300");
				}
				if (this.faithLevels.get(activeFaith) >= 400) {
					RunicParadise.perms.playerAdd(p, "rp.slimefun.tech400");
				}
			} else if (this.faithLevels.get(activeFaith) >= 300 && activeFaith.equals("Aether")) {
				RunicParadise.perms.playerAdd(p, "rp.slimefun.aether300");
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

		if (rank.equals("Master")) {
			if (value >= 250 && value <= (250 + (1000 * .30))) {
				if (p.getInventory().firstEmpty() != -1) {
					p.getInventory().addItem(Borderlands.specialLootDrops("DukeEssence", p.getUniqueId()));
					RunicMessaging.sendMessage(p, RunicFormat.RANKS,
							"Your faith has crystallized a memory into your inventory!");
					RunicParadise.playerProfiles.get(p.getUniqueId()).logSpecialRankDrop("DukeEssence",
							"Faith " + info);
				} else {
					RunicMessaging.sendMessage(p, RunicFormat.ERROR,
							"Make room in your inventory for memory drops please.");
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