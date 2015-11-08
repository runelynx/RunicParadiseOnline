package io.github.runelynx.runicparadise;

import static org.bukkit.Bukkit.getLogger;
import static org.bukkit.ChatColor.AQUA;
import static org.bukkit.ChatColor.DARK_AQUA;
import static org.bukkit.ChatColor.DARK_RED;
import static org.bukkit.ChatColor.GOLD;
import static org.bukkit.ChatColor.GRAY;
import static org.bukkit.ChatColor.GREEN;
import static org.bukkit.ChatColor.RED;
import static org.bukkit.ChatColor.UNDERLINE;
import static org.bukkit.ChatColor.WHITE;
import static org.bukkit.ChatColor.YELLOW;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;

import mkremins.fanciful.FancyMessage;

import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.util.Vector;

import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.ExplodeEffect;
import de.slikey.effectlib.effect.ShieldEffect;

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
				p.sendMessage(ChatColor.GRAY + "[" + ChatColor.BLUE + "Runic"
						+ ChatColor.DARK_AQUA + "Faith" + ChatColor.GRAY + "] "
						+ ChatColor.BLUE + "Faith system deactivated!");
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

	public String listPowers(boolean showAll) {

		if (!showAll) {
			Bukkit.getPlayer(this.getUUID()).sendMessage(
					ChatColor.GRAY + "[" + ChatColor.BLUE + "Runic"
							+ ChatColor.DARK_AQUA + "Faith" + ChatColor.GRAY
							+ "] " + ChatColor.BLUE
							+ "Listing powers granted by your faiths:");
		} else {
			Bukkit.getPlayer(this.getUUID()).sendMessage(
					ChatColor.GRAY + "[" + ChatColor.BLUE + "Runic"
							+ ChatColor.DARK_AQUA + "Faith" + ChatColor.GRAY
							+ "] " + ChatColor.BLUE
							+ "Listing all powers available:");
		}

		// Iterate through all available faiths... check each one to see if
		// player has it equipped. If they do, list those powers.
		for (Entry<String, String[]> entry : RunicParadise.faithSettingsMap
				.entrySet()) {
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

				Bukkit.getPlayer(this.getUUID()).sendMessage(
						ChatColor.GRAY + " " + faithColor + faithName
								+ ChatColor.BLUE + " Powers:");

				MySQL MySQL = new MySQL(instance, instance.getConfig()
						.getString("dbHost"), instance.getConfig().getString(
						"dbPort"),
						instance.getConfig().getString("dbDatabase"), instance
								.getConfig().getString("dbUser"), instance
								.getConfig().getString("dbPassword"));

				try {

					final Connection dbCon = MySQL.openConnection();
					Statement dbStmt = dbCon.createStatement();
					ResultSet powerResult = dbStmt
							.executeQuery("SELECT * FROM rp_MasterPowers WHERE FaithName='"
									+ faithName
									+ "' ORDER BY RequiredLevel ASC;");
					if (!powerResult.isBeforeFirst()) {
						// No results
						// do nothing
						Bukkit.getPlayer(this.getUUID()).sendMessage(
								"Oops! Couldn't find any powers for the "
										+ faithName + " faith.");
						dbCon.close();
					} else {
						// results found!
						while (powerResult.next()) {
							// Found powers for a faith that player has
							// equipped!

							ChatColor levelColor;
							if (this.checkEquippedFaithLevel(faithName,
									powerResult.getInt("RequiredLevel"))) {
								// player qualifies for this power
								levelColor = ChatColor.GREEN;
							} else {
								// player does NOT qualify for this power
								levelColor = ChatColor.GRAY;
							}

							new FancyMessage("   Level ")
									.color(ChatColor.BLUE)
									//
									.then(powerResult
											.getString("RequiredLevel"))
									.color(levelColor)
									//
									.then(": ")
									.color(WHITE)
									//
									.then(powerResult.getString("PowerName"))
									.color(levelColor)
									.tooltip(
											powerResult
													.getString("Description")
													.substring(
															0,
															powerResult
																	.getString(
																			"Description")
																	.length() / 2),
											powerResult
													.getString("Description")
													.substring(
															powerResult
																	.getString(
																			"Description")
																	.length() / 2,
															powerResult
																	.getString(
																			"Description")
																	.length()))
									.send(Bukkit.getPlayer(this.getUUID()));

						}
						dbStmt.close();
						dbCon.close();
					}

				} catch (SQLException z) {
					getLogger().log(
							Level.SEVERE,
							"Failed Faith.listPowers when trying to get powers for a faith: "
									+ z.getMessage());
					return "Error: Database Failure";
				}
			} // end if checking whether player has the faith equipped
			else {
				// Player does not have this faith equipped
			}
		} // end for looping through all possible faiths
		Bukkit.getPlayer(this.getUUID()).sendMessage(
				ChatColor.GRAY + "" + ChatColor.ITALIC
						+ "Hover mouse over power name for details");
		return "Success";

	} // end method

	public static void getPowerSettings() {
		RunicParadise.powerReqsMap.clear();

		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));
		String powerList = "";
		try {

			final Connection dbCon = MySQL.openConnection();
			Statement dbStmt = dbCon.createStatement();
			ResultSet faithResult = dbStmt
					.executeQuery("SELECT * FROM rp_MasterPowers;");
			if (!faithResult.isBeforeFirst()) {
				// No results
				// do nothing
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
						"sc Tried to load Power settings, but couldn't find them in the DB!");
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
						"sc This is a critical problem; Powers will not work :(");

				dbCon.close();
				return;
			} else {
				// results found!
				while (faithResult.next()) {
					RunicParadise.powerReqsMap.put(
							faithResult.getString("PowerName"),
							faithResult.getInt("RequiredLevel"));
					powerList += faithResult.getString("PowerName") + ". ";
				}

				dbCon.close();
			}

		} catch (SQLException z) {
			getLogger().log(Level.SEVERE,
					"Failed Faith.powerSettings " + z.getMessage());
		}
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
				"sc Loaded Power Req Levels: " + powerList);
	}

	public static void getFaithSettings() {
		RunicParadise.faithSettingsMap.clear();

		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));
		String faithList = "";
		try {

			final Connection dbCon = MySQL.openConnection();
			Statement dbStmt = dbCon.createStatement();
			ResultSet faithResult = dbStmt
					.executeQuery("SELECT * FROM rp_MasterFaiths;");
			if (!faithResult.isBeforeFirst()) {
				// No results
				// do nothing
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
						"sc Tried to load Faith settings, but couldn't find them in the DB!");
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
						"sc This is a critical problem; Faiths will not work :(");

				dbCon.close();
				return;
			} else {
				// results found!
				while (faithResult.next()) {
					RunicParadise.faithSettingsMap.put(
							faithResult.getString("FaithName"), new String[] {
									faithResult.getString("FaithName"),
									faithResult.getString("ChatPrefix"),
									faithResult.getString("Permission"),
									faithResult.getString("Description"),
									faithResult.getString("MaxLevel"),
									faithResult.getString("CastMessage"),
									faithResult.getString("ChatPrefix2")});
					faithList += faithResult.getString("faithName") + ". ";
				}

				dbCon.close();
			}

		} catch (SQLException z) {
			getLogger().log(Level.SEVERE,
					"Failed Faith.faithSettings " + z.getMessage());
		}
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "sc Loaded Faiths: "
				+ faithList);
	}

	public boolean checkEquippedFaithLevel(String faithName, int level) {
		// Using this map means faith must be "equipped" -- NOT inactive!
		if (this.faithLevels.containsKey(faithName)) {
			if (this.faithLevels.get(faithName) >= level) {
				// ensure the faith is ACTIVE

				if (new RunicPlayerBukkit(this.getUUID()).getActiveFaith()
						.equals(faithName)) {
					return true;
				} else {
					return false;
				}
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
		} else if (!Bukkit.getPlayer(nUUID).hasPermission(
				RunicParadise.faithSettingsMap.get(faithName)[2])) {
			// check if player has the permission required for this faith
			return "Player is not eligible for that Faith";
		} else {
			// it's a valid faith name!
			// check if player already has a record for this faith in the DB
			MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
					"dbHost"), instance.getConfig().getString("dbPort"),
					instance.getConfig().getString("dbDatabase"), instance
							.getConfig().getString("dbUser"), instance
							.getConfig().getString("dbPassword"));
			try {

				Date now = new Date();
				final Connection dbCon = MySQL.openConnection();
				Statement dbStmt = dbCon.createStatement();
				ResultSet faithResult = dbStmt
						.executeQuery("SELECT * FROM rp_PlayerFaiths WHERE UUID = '"
								+ nUUID.toString()
								+ "' AND FaithName = '"
								+ faithName + "';");
				if (!faithResult.isBeforeFirst()) {
					// No results
					// add the faith
					dbStmt.executeUpdate("INSERT INTO rp_PlayerFaiths (UUID, Active, FaithName, Level, Timestamp) VALUES ('"
							+ nUUID.toString()
							+ "', 1, '"
							+ faithName
							+ "', 0, " + now.getTime() + ");");

					targetPlayer.setActiveFaith(faithName);

					dbStmt.close();

					dbCon.close();

					this.retrievePlayerData(nUUID);
					return "Success";
				} else {
					// results found!

					targetPlayer.setActiveFaith(faithName);

					dbStmt.close();

					dbCon.close();

					this.retrievePlayerData(nUUID);
					return "Success";
				}

			} catch (SQLException z) {
				getLogger().log(
						Level.SEVERE,
						"Failed Faith.enableFaith " + nUUID.toString() + "- "
								+ z.getMessage());
				return "Database Failure :(";
			}
		}
	}

	public String getPlayerStats(UUID nUUID, UUID senderUUID) {
		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));
		try {
			final Connection dbCon = MySQL.openConnection();
			Statement dbStmt = dbCon.createStatement();
			ResultSet faithResult = dbStmt
					.executeQuery("SELECT * FROM rp_PlayerFaiths WHERE UUID = '"
							+ nUUID.toString() + "' ORDER BY FaithName ASC;");
			if (!faithResult.isBeforeFirst()) {
				getLogger().log(
						Level.INFO,
						"No Faiths found for "
								+ Bukkit.getPlayer(nUUID).getDisplayName());

				// No results
				// do nothing
				dbCon.close();
				return "No faiths found.";
			} else {
				// results found!
				boolean activeStarted = false;
				boolean inactiveStarted = false;

				Bukkit.getPlayer(senderUUID).sendMessage(
						ChatColor.DARK_BLUE + "[RunicFaith] " + ChatColor.BLUE
								+ "Displaying faith stats for "
								+ ChatColor.GRAY
								+ Bukkit.getOfflinePlayer(nUUID).getName());
				Bukkit.getPlayer(senderUUID).sendMessage(
						ChatColor.BLUE
								+ "Faith power level: "
								+ ChatColor.GRAY
								+ new RunicPlayerBukkit(this.getUUID())
										.getFaithPowerLevel()
								+ ChatColor.DARK_GRAY + "" + ChatColor.ITALIC
								+ " (Combined total faith level)");

				while (faithResult.next()) {

					String color;
					if (faithResult.getString("FaithName").equals(
							new RunicPlayerBukkit(nUUID).getActiveFaith())) {
						color = ChatColor.GREEN + "";
					} else {
						color = ChatColor.DARK_RED + "";
					}

					Bukkit.getPlayer(senderUUID)
							.sendMessage(
									displayLevelBar(((double) faithResult
											.getInt("Level") / Integer
											.parseInt(RunicParadise.faithSettingsMap.get(faithResult
													.getString("FaithName"))[4])) * 50)
											+ " "
											+ color
											+ faithResult
													.getString("FaithName")
											+ " "
											+ ChatColor.GRAY
											+ faithResult.getInt("Level")
											+ "/"
											+ RunicParadise.faithSettingsMap.get(faithResult
													.getString("FaithName"))[4]);

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

				dbCon.close();
				return "Success";
			}

		} catch (SQLException z) {
			getLogger().log(
					Level.SEVERE,
					"Failed Faith.getPlayerStats for player "
							+ nUUID.toString() + "- " + z.getMessage());
			return "Database failure";
		}

	}

	private String displayLevelBar(double input) {
		String bar = ChatColor.AQUA + "";
		int counter = 0;

		while (counter <= input) {
			bar += "|";
			counter++;
		}

		bar += ChatColor.DARK_BLUE + "|";
		counter++;

		bar += ChatColor.GRAY + "";
		while (counter <= 50) {
			bar += "|";
			counter++;
		}

		return bar;

	}

	public void retrievePlayerData(UUID nUUID) {
		this.playerUUID = nUUID.toString();
		this.trueUUID = nUUID;
		this.playerName = new RunicPlayerBukkit(nUUID).getPlayerName();

		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));
		try {
			final Connection dbCon = MySQL.openConnection();
			Statement dbStmt = dbCon.createStatement();
			ResultSet faithResult = dbStmt
					.executeQuery("SELECT * FROM rp_PlayerFaiths WHERE UUID = '"
							+ nUUID.toString() + "';");
			if (!faithResult.isBeforeFirst()) {
				getLogger().log(
						Level.INFO,
						"No Faiths found for "
								+ Bukkit.getPlayer(nUUID).getDisplayName());

				this.faithLevels.clear();

				// No results
				// do nothing
				dbCon.close();
			} else {
				// results found!

				// start fresh!
				this.faithLevels.clear();

				while (faithResult.next()) {
					this.faithLevels.put(faithResult.getString("FaithName"),
							faithResult.getInt("Level"));
					getLogger().log(
							Level.INFO,
							"Faiths - Added: "
									+ Bukkit.getPlayer(nUUID).getDisplayName()
									+ " - "
									+ faithResult.getString("faithName") + " L"
									+ faithResult.getInt("Level"));
					this.primaryFaithName = new RunicPlayerBukkit(nUUID)
							.getActiveFaith();
				}

				dbCon.close();
			}
		} catch (SQLException z) {
			getLogger().log(
					Level.SEVERE,
					"Failed Faith.retrievePlayerData for player "
							+ nUUID.toString() + "- " + z.getMessage());
		}

	}

	public String setSkill(Player p, String adminName, String faithName,
			int newValue) {
		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));

		if (newValue < 0) {
			getLogger().log(
					Level.SEVERE,
					"Invalid newValue in Faith.setSkill - given " + newValue
							+ " less than 0");
			return "Error NewValueLessThanZero";
		} else if (newValue > Integer.parseInt(RunicParadise.faithSettingsMap
				.get(faithName)[4])) {
			getLogger().log(
					Level.SEVERE,
					"Invalid newValue in Faith.setSkill - given " + newValue
							+ " greater than max allowed");
			return "Error NewValueGreaterThanMaxLevelForThisFaith";
		}

		// Now update the object used by the map!
		if (this.faithLevels.containsKey(faithName)) {
			this.faithLevels.replace(faithName, newValue);
			p.sendMessage(ChatColor.GRAY + "[" + ChatColor.BLUE + "Runic"
					+ ChatColor.DARK_AQUA + "Faith" + ChatColor.GRAY + "] "
					+ ChatColor.BLUE + "Your " + faithName + ChatColor.BLUE
					+ " faith has been changed by " + adminName + "! "
					+ ChatColor.AQUA + this.faithLevels.get(faithName)
					+ ChatColor.GRAY + "/"
					+ RunicParadise.faithSettingsMap.get(faithName)[4]);

		}

		try {
			final Connection dbCon = MySQL.openConnection();

			PreparedStatement dbStmt = dbCon
					.prepareStatement("UPDATE rp_PlayerFaiths SET Level="
							+ newValue + " WHERE UUID = ? AND FaithName = ?");
			dbStmt.setString(1, this.playerUUID);
			dbStmt.setString(2, faithName);
			dbStmt.executeUpdate();

			dbCon.close();

		} catch (SQLException e) {
			getLogger().log(Level.SEVERE,
					"Failed Powers.setSkill because: " + e.getMessage());
			return "Error DatabaseError or PlayerDoesntHaveThatFaith!";
		}

		return "Success";
	}

	public int getPrimaryFaithLevel() {
		return this.faithLevels.get(this.getPrimaryFaith());
	}

	public boolean incrementSkill(Player p, String faithName) {
		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));

		// Update the hashmap first;
		if (this.faithLevels.containsKey(faithName)) {
			this.faithLevels.replace(faithName,
					this.faithLevels.get(faithName) + 1);
		} else {
			// Given faith is invalid for this player, so stop here!
			return false;
		}

		try {
			final Connection dbCon = MySQL.openConnection();

			PreparedStatement dbStmt = dbCon
					.prepareStatement("UPDATE rp_PlayerFaiths SET Level=Level+1 WHERE UUID = ? AND FaithName = ?");
			dbStmt.setString(1, this.playerUUID);
			dbStmt.setString(2, faithName);
			dbStmt.executeUpdate();

			dbCon.close();

			p.sendMessage(ChatColor.GRAY + "[" + ChatColor.BLUE + "Runic"
					+ ChatColor.DARK_AQUA + "Faith" + ChatColor.GRAY + "] "
					+ ChatColor.BLUE + "Your " + faithName + ChatColor.BLUE
					+ " faith grows stronger! " + ChatColor.GREEN
					+ this.faithLevels.get(faithName) + ChatColor.GRAY + "/"
					+ RunicParadise.faithSettingsMap.get(faithName)[4]);

			if (this.checkEquippedFaithLevel(faithName, Integer
					.parseInt(RunicParadise.faithSettingsMap.get(faithName)[4]))) {
				for (Player q : Bukkit.getOnlinePlayers()) {

					q.sendMessage(ChatColor.GRAY + "[" + ChatColor.BLUE
							+ "Runic" + ChatColor.DARK_AQUA + "Faith"
							+ ChatColor.GRAY + "] " + ChatColor.BLUE
							+ p.getDisplayName() + ChatColor.BLUE
							+ " just maxxed their " + ChatColor.WHITE
							+ faithName + ChatColor.BLUE + " faith!");
					q.getWorld().playSound(q.getLocation(),
							Sound.FIREWORK_LARGE_BLAST, 10, 1);

				}
			}

			return true;

		} catch (SQLException e) {
			getLogger().log(Level.SEVERE,
					"Failed Faith.incrementSkill because: " + e.getMessage());
			this.faithLevels.replace(faithName,
					this.faithLevels.get(faithName) - 1);
			return false;
		}
	}

	/**
	 * 
	 * @param faithName
	 * @param chance
	 *            Integer chance multiplier... 3% * Chance
	 * @return 0=Error/Failure, 1=Success/NoSkillUp, 2=Success/SkillUp
	 */
	public int trySkillUp(Player p, String faithName, int chance, String type) {
		// int randomNum = rand.nextInt((max - min) + 1) + min;
		int randomNum = RunicParadise.randomSeed.nextInt((100 - 0) + 1) + 0;

		if (!this.checkEquippedFaithLevel(faithName, Integer
				.parseInt(RunicParadise.faithSettingsMap.get(faithName)[4]))) {
			if (randomNum <= (3 * chance)) {
				this.incrementSkill(p, faithName);

				p.sendMessage(ChatColor.GRAY + "[" + ChatColor.BLUE + "Runic"
						+ ChatColor.DARK_AQUA + "Faith" + ChatColor.GRAY + "] "
						+ ChatColor.BLUE + "Your " + faithName + ChatColor.BLUE
						+ " faith grows stronger! " + ChatColor.GREEN
						+ this.faithLevels.get(faithName) + ChatColor.GRAY
						+ "/"
						+ RunicParadise.faithSettingsMap.get(faithName)[4]);

				if (RunicParadise.randomSeed.nextInt((100 - 0) + 1) + 0 <= 60
						&& chance == 1) {
					p.getWorld().playSound(p.getLocation(), Sound.ANVIL_BREAK,
							10, 1);
					p.setItemInHand(null);
					p.sendMessage(ChatColor.GRAY + "[" + ChatColor.BLUE
							+ "Runic" + ChatColor.DARK_AQUA + "Faith"
							+ ChatColor.GRAY + "] " + ChatColor.BLUE
							+ "Your faith sword shatters into dust!");
				} else if (RunicParadise.randomSeed.nextInt((100 - 0) + 1) + 0 <= 30
						&& chance == 2) {
					p.getWorld().playSound(p.getLocation(), Sound.ANVIL_BREAK,
							10, 1);
					p.setItemInHand(null);
					p.sendMessage(ChatColor.GRAY + "[" + ChatColor.BLUE
							+ "Runic" + ChatColor.DARK_AQUA + "Faith"
							+ ChatColor.GRAY + "] " + ChatColor.BLUE
							+ "Your faith sword shatters into dust!");

				} else if (RunicParadise.randomSeed.nextInt((100 - 0) + 1) + 0 <= 15
						&& chance == 3) {
					p.getWorld().playSound(p.getLocation(), Sound.ANVIL_BREAK,
							10, 1);

					p.setItemInHand(null);
					p.sendMessage(ChatColor.GRAY + "[" + ChatColor.BLUE
							+ "Runic" + ChatColor.DARK_AQUA + "Faith"
							+ ChatColor.GRAY + "] " + ChatColor.BLUE
							+ "Your faith sword shatters into dust!");

				}

				return 2;
			} else {
				return 1;
			}

		} else {
			// Player is maxxed
			p.sendMessage(ChatColor.GRAY + "[" + ChatColor.BLUE + "Runic"
					+ ChatColor.DARK_AQUA + "Faith" + ChatColor.GRAY + "] "
					+ ChatColor.BLUE + "Your " + faithName + ChatColor.BLUE
					+ " faith cannot grow any stronger!");
			return 0;
		}
	}

	public int trySkillUpViaPrayer(Player p, String faithName, boolean withKarma) {

		if (!this.checkEquippedFaithLevel(faithName, Integer
				.parseInt(RunicParadise.faithSettingsMap.get(faithName)[4]))) {

			int counter = 0;
			int limit;

			if (withKarma) {

				limit = 3;

				while (counter < limit) {
					this.incrementSkill(p, faithName);
					if (this.checkEquippedFaithLevel(faithName, Integer
							.parseInt(RunicParadise.faithSettingsMap
									.get(faithName)[4]))) {
						return 1;
					}
					counter++;
				}

			} else {
				limit = 1;

				while (counter < limit) {
					this.incrementSkill(p, faithName);
					if (this.checkEquippedFaithLevel(faithName, Integer
							.parseInt(RunicParadise.faithSettingsMap
									.get(faithName)[4]))) {
						return 1;
					}
					counter++;
				}

			}

			return 1;

		} else {
			// Player is maxxed
			p.sendMessage(ChatColor.GRAY + "[" + ChatColor.BLUE + "Runic"
					+ ChatColor.DARK_AQUA + "Faith" + ChatColor.GRAY + "] "
					+ ChatColor.BLUE + "Your " + faithName + ChatColor.BLUE
					+ " faith cannot grow any stronger!");
			return 0;
		}
	}

	public String getPrimaryFaith() {
		return this.primaryFaithName;

	}

	private void sendCastMessage(Player p, String spellName, String faithName) {
		p.sendMessage(ChatColor.GRAY + "[" + ChatColor.BLUE + "Runic"
				+ ChatColor.DARK_AQUA + "Faith" + ChatColor.GRAY + "] "
				+ ChatColor.BLUE
				+ RunicParadise.faithSettingsMap.get(faithName)[5]
				+ " Casting " + ChatColor.GRAY + spellName + ChatColor.BLUE
				+ "!");

	}

	public void castSun_Sunflare(UUID pUUID, Player p) {
		// 2% chance on receiving damage to set all nearby monsters on fire
		String spellName = "Sunflare";
		if ((RunicParadise.randomSeed.nextInt((100 - 0) + 1) + 0) <= 2) {
			List<Entity> nearby = p.getNearbyEntities(8, 8, 8);
			for (Entity tmp : nearby) {
				if (tmp instanceof Monster)
					tmp.setFireTicks(60);
			}
			nearby = null;
			sendCastMessage(p, spellName, "Sun");
			getLogger().log(
					Level.INFO,
					"Faith Info: " + p.getName() + " just cast " + spellName
							+ "!");
		}
	}

	@SuppressWarnings("deprecation")
	public void castMoon_LunarCalm(UUID pUUID, final Player p) {
		// 1% chance to stop mobs from targeting player for 4 seconds
		String spellName = "Lunar Calm";
		if ((RunicParadise.randomSeed.nextInt((100 - 0) + 1) + 0) <= 1) {

			// register protection now
			RunicParadise.protectedPlayers.put(p.getUniqueId(),
					p.getDisplayName());

			// schedule removal of protection 80 tickets out
			Bukkit.getServer().getScheduler()
					.scheduleAsyncDelayedTask(instance, new Runnable() {
						public void run() {
							RunicParadise.protectedPlayers.remove(p
									.getUniqueId());
						}
					}, 100);

			// stop all nearby monsters from targeting players
			final List<Entity> nearby = p.getNearbyEntities(15, 15, 15);
			for (Entity tmp : nearby) {
				if (tmp instanceof Monster) {
					((Monster) tmp).setTarget(null);
					((Monster) tmp).setTarget(null);
				}
			}

			// schedule removal of protection 80 tickets out
			Bukkit.getServer().getScheduler()
					.scheduleAsyncDelayedTask(instance, new Runnable() {
						public void run() {
							for (Entity tmp : nearby) {
								if (tmp instanceof Monster) {
									((Monster) tmp).setTarget(null);
									((Monster) tmp).setTarget(null);
								}
							}
						}
					}, 20);
			Bukkit.getServer().getScheduler()
					.scheduleAsyncDelayedTask(instance, new Runnable() {
						public void run() {
							for (Entity tmp : nearby) {
								if (tmp instanceof Monster) {
									((Monster) tmp).setTarget(null);
									((Monster) tmp).setTarget(null);
								}
							}
						}
					}, 40);
			Bukkit.getServer().getScheduler()
					.scheduleAsyncDelayedTask(instance, new Runnable() {
						public void run() {
							for (Entity tmp : nearby) {
								if (tmp instanceof Monster) {
									((Monster) tmp).setTarget(null);
									((Monster) tmp).setTarget(null);
								}
							}
						}
					}, 60);

			int count = nearby.size();
			boolean playerProtected;
			if (RunicParadise.protectedPlayers.containsKey(p.getUniqueId())) {
				playerProtected = true;
			} else {
				playerProtected = false;
			}
			sendCastMessage(p, spellName, "Moon");
			getLogger().log(
					Level.INFO,
					"Faith Info: " + p.getName() + " just cast " + spellName
							+ "! Calmed " + count + " mobs. Player protected= "
							+ playerProtected);

		}
	}

	public void castSun_SolarPower(UUID pUUID, Player p) {
		// 2% chance to empower player when dealing damage to monsters
		String spellName = "Solar Power";
		if ((RunicParadise.randomSeed.nextInt((100 - 0) + 1) + 0) <= 2) {
			p.addPotionEffect(new PotionEffect(
					PotionEffectType.DAMAGE_RESISTANCE, 300, 1));
			p.addPotionEffect(new PotionEffect(
					PotionEffectType.INCREASE_DAMAGE, 300, 1));

			sendCastMessage(p, spellName, "Sun");
			getLogger().log(
					Level.INFO,
					"Faith Info: " + p.getName() + " just cast " + spellName
							+ "!");
			ParticleEffect.DRIP_LAVA
					.display(0, 1, 0, 1, 20, p.getLocation(), 2);
		}
	}

	public void castMoon_CelestialHealing(UUID pUUID, Player p) {
		// 1% chance to gain regeneration
		String spellName = "Celestial Healing";
		if ((RunicParadise.randomSeed.nextInt((100 - 0) + 1) + 0) <= 1) {
			p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,
					300, 0));
			p.addPotionEffect(new PotionEffect(PotionEffectType.HEAL, 1, 0));

			sendCastMessage(p, spellName, "Moon");
			getLogger().log(
					Level.INFO,
					"Faith Info: " + p.getName() + " just cast " + spellName
							+ "!");
		}
	}

	public void castFate_InevitableDemise(UUID pUUID, Player player,
			LivingEntity monster) {

		if (player.getHealth() <= 6) {
			Random rand = new Random();
			int number = rand.nextInt(100) + 1;
			if (number >= 80) {
				for (final Entity entity : getTargets.getTargetList(
						player.getLocation(), 2)) {
					if (((entity instanceof LivingEntity))
							&& (entity != player)) {
						((LivingEntity) entity).setHealth(0);
					}
				}
			}
		}

	}

	public void castTime_Rewind(Player p) {
		Random rand1 = new Random();
		if (rand1.nextInt(100) + 1 <= 10) {

			if (p.getHealth() <= 3) {
				Random rand = new Random();
				int number = rand.nextInt(100) + 1;
				if (number >= 80) {
					p.addPotionEffect(new PotionEffect(
							PotionEffectType.DAMAGE_RESISTANCE, 200, 1));
					p.setHealth(16);
				}
			}
		}

	}

	public void castNether_Netherborn(Player p) {
		// Boost player whenever theyre in the nether
		String spellName = "Netherborn";

		p.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST,
				NETHER_NETHERBORN_TIMING, 1));
		p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE,
				NETHER_NETHERBORN_TIMING, 0));
		sendCastMessage(p, spellName, "Nether");
		getLogger().log(Level.INFO,
				"Faith Info: " + p.getName() + " just cast " + spellName + "!");

	}

	// Flame : Unstable Embers
	public void castFlame_UnstableEmbers(Player p) {

		Random rand = new Random();
		int number = rand.nextInt(100) + 1;
		if (number >= 90) {

			String spellName = "Unstable Embers";
			sendCastMessage(p, spellName, "Fire");

			ParticleEffect.EXPLOSION_HUGE.display(0.1f, 0.1f, 0.1f, 0.1f, 50,
					p.getLocation(), 18.0);
			for (Entity entity : getTargets.getTargetList(p.getLocation(), 4)) {
				if (((entity instanceof LivingEntity)) && (entity != p)) {
					((LivingEntity) entity).damage(4);
				}
			}
		}

	}

	// ==========================
	// Wind : TempestArmor

	@SuppressWarnings("deprecation")
	public void castWind_TempestArmor(Player player) {

		ItemStack i = player.getInventory().getHelmet();
		ItemStack j = player.getInventory().getChestplate();
		ItemStack k = player.getInventory().getLeggings();
		ItemStack l = player.getInventory().getBoots();
		if (i == null || j == null || k == null || l == null) {
			return;
		} else {
			if (i.getType() == Material.GOLD_HELMET
					&& j.getType() == Material.GOLD_CHESTPLATE
					&& k.getType() == Material.GOLD_LEGGINGS
					&& l.getType() == Material.GOLD_BOOTS) {
				Random rand = new Random();
				int number = rand.nextInt(100) + 1;
				if (number >= 96) {

					String spellName = "Tempest Armor";
					sendCastMessage(player, spellName, "Wind");

					for (final Entity entity : getTargets.getTargetList(
							player.getLocation(), 5)) {
						if (((entity instanceof Monster)) && (entity != player)) {
							((LivingEntity) entity)
									.setVelocity(((LivingEntity) entity)
											.getLocation().getDirection()
											.multiply(2.5D).setY(0.7D));
							final World w = ((LivingEntity) entity).getWorld();
							Bukkit.getServer()
									.getScheduler()
									.scheduleAsyncDelayedTask(instance,
											new Runnable() {
												public void run() {
													w.strikeLightning(((LivingEntity) entity)
															.getLocation());
												}
											}, 20);
						}
					}
				}
			}
		}

	}

	// ==========================
	// Wind : Healing Breeze

	public void castWind_HealingBreeze(Player player) {

		Random rand = new Random();
		int number = rand.nextInt(100) + 1;
		if (number >= 97) {
			for (Entity entity : getTargets.getTargetList(player.getLocation(),
					40)) {
				if (entity instanceof Player) {
					String spellName = "Healing Breeze";
					sendCastMessage(player, spellName, "Wind");
					((LivingEntity) entity).addPotionEffect(new PotionEffect(
							PotionEffectType.REGENERATION, 200, 1));
					((Player) entity).sendMessage(ChatColor.GRAY + ""
							+ ChatColor.ITALIC
							+ "You feel refreshed by a soothing breeze");
				}
			}
		}

	}

	// ==========================
	// Earth : Earth's Bounty

	public void castEarth_EarthsBounty(PlayerInteractEvent e) {

		Player p = e.getPlayer();
		if ((p.getItemInHand() != null)
				&& (p.getItemInHand().getType().equals(Material.GLOWSTONE_DUST))) {
			ItemStack glowstone = p.getItemInHand();
			if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
				Block block = e.getClickedBlock();
				if (block.getType() == Material.STONE) {
					if (p.getLevel() >= 2) {
						int removel = 2;
						int plevel = p.getLevel() - removel;
						p.setLevel(plevel);
						p.playSound(p.getLocation(), Sound.LEVEL_UP, 1, 1);
						int radius = 15;
						Location location = p.getLocation();
						int diamonds = 0;
						int emeralds = 0;
						Block center = location.getBlock().getRelative(
								RunicParadise.getPlayerFacing(p), radius);
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
						p.sendMessage(ChatColor.GREEN + "There are " + diamonds
								+ " diamond(s) and " + emeralds
								+ " emerald(s) near you!");

					}
				}
			}
		}
	}

	// ==========================
	// Water OneWithTheSea

	public void castWater_DeepWader(PlayerInteractEvent e) {

		Player p = e.getPlayer();
		if ((p.getItemInHand() != null)
				&& (p.getItemInHand().getType().equals(Material.GLOWSTONE_DUST))) {

			Block block = e.getPlayer().getLocation().getBlock();
			if (block.getType() == Material.WATER
					|| block.getType() == Material.STATIONARY_WATER) {
				if (p.getLevel() >= 2) {
					int removel = 2;
					int plevel = p.getLevel() - removel;
					p.setLevel(plevel);
					p.playSound(p.getLocation(), Sound.LEVEL_UP, 1, 1);
					p.addPotionEffect(new PotionEffect(
							PotionEffectType.WATER_BREATHING, 12000, 1));

					String spellName = "Deep Wader";
					ParticleEffect.WATER_WAKE.display((float)10.0, (float)10.0, (float)10.0, (float)1.0, 25, p.getLocation(), 30.0);
					
					sendCastMessage(e.getPlayer(), spellName, "Water");

				}
			}

		}
	}

	// ===========================
	// Water : Protective Bubble

	public void castWater_ProtectiveBubble(final Player player) {

		Random rand = new Random();
		int number = rand.nextInt(100) + 1;
		if (number >= 97) {
			player.removePotionEffect(PotionEffectType.ABSORPTION);
			player.addPotionEffect(new PotionEffect(
					PotionEffectType.ABSORPTION, 300, 0));
			String spellName = "Protective Bubble";
			
		
			sendCastMessage(player, spellName, "Water");

			Bukkit.getServer().getScheduler()
					.scheduleAsyncDelayedTask(instance, new Runnable() {
						public void run() {
							player.removePotionEffect(PotionEffectType.ABSORPTION);
						}
					}, 320);

		}

	}

	// ==========================
	// Nether : Vampirism

	public void castNether_Vampirism(EntityDeathEvent event) {
		Entity e = event.getEntity();

		EntityDamageByEntityEvent nEvent = (EntityDamageByEntityEvent) e
				.getLastDamageCause();

		Random rand = new Random();
		int number = rand.nextInt(100) + 1;
		if (number >= 80) {
			Player p = (Player) nEvent.getDamager();
			p.setHealth(p.getHealth() + 3);
			String spellName = "Vampirism";
			sendCastMessage(p, spellName, "Nether");
		}

	}

	// ==========================
	// Aether : Gravity Flux

	public void castAether_GravityFlux(Player p) {

		Random rand = new Random();
		int number = rand.nextInt(100) + 1;
		if (number <= 2) {
			String spellName = "Gravity Flux";
			sendCastMessage(p, spellName, "Aether");
			for (Entity e : p.getNearbyEntities(7, 7, 7)) {
				if (e instanceof Monster) {
					e.setVelocity(new Vector(e.getVelocity().getX(), 1, e
							.getVelocity().getZ()));
				}
			}
		}
	}

	// ==========================
	// Aether : Graceful Steps

	public void castAether_GracefulSteps(Player p) {

		Random rand = new Random();
		int number = rand.nextInt(100) + 1;
		if (number <= 20) {
			String spellName = "Graceful Steps";
			sendCastMessage(p, spellName, "Aether");

			p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 300, 0));
		}
	}

	// =========================
	// Nature : Forest Armor

	@SuppressWarnings("deprecation")
	public void castNature_ForestArmor(Player player) {

		ItemStack i = player.getInventory().getHelmet();
		ItemStack j = player.getInventory().getChestplate();
		ItemStack k = player.getInventory().getLeggings();
		ItemStack l = player.getInventory().getBoots();
		if (i == null || j == null || k == null || l == null) {
			return;
		} else {
			if (i.getType() == Material.LEATHER_HELMET
					&& j.getType() == Material.LEATHER_CHESTPLATE
					&& k.getType() == Material.LEATHER_LEGGINGS
					&& l.getType() == Material.LEATHER_BOOTS) {
				Random rand = new Random();
				int number = rand.nextInt(100) + 1;
				if (number >= 96) {

					String spellName = "Forest Armor";
					sendCastMessage(player, spellName, "Nature");
					final Wolf wolf1 = (Wolf) player.getWorld().spawnEntity(
							player.getLocation(), EntityType.WOLF);
					final Wolf wolf2 = (Wolf) player.getWorld().spawnEntity(
							player.getLocation(), EntityType.WOLF);
					wolf1.setOwner(player);
					wolf1.setAdult();
					wolf1.setTamed(true);
					wolf1.setCustomName(ChatColor.YELLOW + player.getName()
							+ "'s Wolf");
					wolf1.setCustomNameVisible(true);
					wolf2.setOwner(player);
					wolf2.setAdult();
					wolf2.setTamed(true);
					wolf2.setCustomName(ChatColor.YELLOW + player.getName()
							+ "'s Wolf");
					wolf2.setCustomNameVisible(true);
					Bukkit.getServer().getScheduler()
							.scheduleAsyncDelayedTask(instance, new Runnable() {
								public void run() {
									wolf1.setHealth(0);
									wolf2.setHealth(0);
								}
							}, 400);
				}
			}
		}

	}

	// =========================
	// Water :: Arctic Frost

	@SuppressWarnings("deprecation")
	public void castWater_ArcticFrost(Player p) {

		Random rand = new Random();
		int number = rand.nextInt(100) + 1;
		if (number <= 3) {
			String spellName = "Arctic Frost";
			sendCastMessage(p, spellName, "Water");
			for (Entity e : p.getNearbyEntities(7, 7, 7)) {
				if (e instanceof Monster) {
					((Monster) e).addPotionEffect(new PotionEffect(
							PotionEffectType.SLOW, 150, 2));
				}
			}
		}

	}

	public static void pray(Location locKey, Player p) {

		RunicPlayerBukkit targetPlayer = new RunicPlayerBukkit(p.getUniqueId());
		boolean needKarma = false;
		int numItems = Integer
				.parseInt(RunicParadise.prayerBooks.get(locKey)[4]);

		if (RunicParadise.prayerBooks.get(locKey)[17].equals("Book")) {
			needKarma = true;
		}

		if (!RunicParadise.prayerBooks.get(locKey)[18]
				.equals(RunicParadise.faithMap.get(p.getUniqueId())
						.getPrimaryFaith())) {

			p.sendMessage("Your prayer is rejected! You must be loyal to your faith - pray at your active faith's shrines! "
					+ ChatColor.BLUE
					+ "Left-click the block to learn what this prayer needs!");
			return;
		}

		ItemStack item1 = new ItemStack(Material.getMaterial(Integer
				.parseInt(RunicParadise.prayerBooks.get(locKey)[5])),
				Integer.parseInt(RunicParadise.prayerBooks.get(locKey)[7]),
				Short.parseShort((RunicParadise.prayerBooks.get(locKey)[6])));
		ItemStack[] item1a = new ItemStack[] { new ItemStack(
				Material.getMaterial(Integer.parseInt(RunicParadise.prayerBooks
						.get(locKey)[5])),
				Integer.parseInt(RunicParadise.prayerBooks.get(locKey)[7]),
				Short.parseShort((RunicParadise.prayerBooks.get(locKey)[6]))) };
		ItemStack item2 = new ItemStack(Material.getMaterial(Integer
				.parseInt(RunicParadise.prayerBooks.get(locKey)[8])),
				Integer.parseInt(RunicParadise.prayerBooks.get(locKey)[10]),
				Short.parseShort((RunicParadise.prayerBooks.get(locKey)[9])));
		ItemStack[] item2a = new ItemStack[] { new ItemStack(
				Material.getMaterial(Integer.parseInt(RunicParadise.prayerBooks
						.get(locKey)[8])),
				Integer.parseInt(RunicParadise.prayerBooks.get(locKey)[10]),
				Short.parseShort((RunicParadise.prayerBooks.get(locKey)[9]))) };
		ItemStack item3 = new ItemStack(Material.getMaterial(Integer
				.parseInt(RunicParadise.prayerBooks.get(locKey)[11])),
				Integer.parseInt(RunicParadise.prayerBooks.get(locKey)[13]),
				Short.parseShort((RunicParadise.prayerBooks.get(locKey)[12])));
		ItemStack[] item3a = new ItemStack[] { new ItemStack(
				Material.getMaterial(Integer.parseInt(RunicParadise.prayerBooks
						.get(locKey)[11])),
				Integer.parseInt(RunicParadise.prayerBooks.get(locKey)[13]),
				Short.parseShort((RunicParadise.prayerBooks.get(locKey)[12]))) };
		ItemStack item4 = new ItemStack(Material.getMaterial(Integer
				.parseInt(RunicParadise.prayerBooks.get(locKey)[14])),
				Integer.parseInt(RunicParadise.prayerBooks.get(locKey)[16]),
				Short.parseShort((RunicParadise.prayerBooks.get(locKey)[15])));
		ItemStack[] item4a = new ItemStack[] { new ItemStack(
				Material.getMaterial(Integer.parseInt(RunicParadise.prayerBooks
						.get(locKey)[14])),
				Integer.parseInt(RunicParadise.prayerBooks.get(locKey)[16]),
				Short.parseShort((RunicParadise.prayerBooks.get(locKey)[15]))) };

		switch (numItems) {

		case 1:
			if (p.getInventory().contains(item1)) {
				// Player has enough of first item!

				if (needKarma && targetPlayer.getKarma() >= 2) {
					// Player is trying to pray at a Book - costs 2 karma! they
					// have enough karma, so take it!

					if (RunicParadise.faithMap.get(p.getUniqueId())
							.trySkillUpViaPrayer(
									p,
									RunicParadise.faithMap.get(p.getUniqueId())
											.getPrimaryFaith(), true) == 1) {
						targetPlayer.adjustPlayerKarma(-2);
						p.getInventory().removeItem(item1a);
						p.sendMessage("Your karma prayer has been answered!");
					} else {
						// player is at max level, prayer failed!!
						return;
					}

				} else if (!needKarma) {
					// Karma not needed for this prayer

					if (RunicParadise.faithMap.get(p.getUniqueId())
							.trySkillUpViaPrayer(
									p,
									RunicParadise.faithMap.get(p.getUniqueId())
											.getPrimaryFaith(), false) == 1) {
						p.getInventory().removeItem(item1a);
						p.sendMessage("Your prayer has been answered!");
					} else {
						// player is at max level, prayer failed!!
						return;
					}

				} else if (needKarma) {
					// Karma is needed for this prayer and player doesnt have
					// enough!
					p.sendMessage("This prayer costs 2 karma; you don't have enough!!");
				}

			} else {
				// player doesnt have enough items :(
				p.sendMessage("Your prayer is rejected! Do you have the right items? Be sure each stack is just the right amount!");
			}

			break;
		case 2:

			if (p.getInventory().contains(item1)
					&& p.getInventory().contains(item2)) {
				// Player has enough of first item!

				if (needKarma && targetPlayer.getKarma() >= 2) {
					// Player is trying to pray at a Book - costs 2 karma! they
					// have enough karma, so take it!

					if (RunicParadise.faithMap.get(p.getUniqueId())
							.trySkillUpViaPrayer(
									p,
									RunicParadise.faithMap.get(p.getUniqueId())
											.getPrimaryFaith(), true) == 1) {
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

					if (RunicParadise.faithMap.get(p.getUniqueId())
							.trySkillUpViaPrayer(
									p,
									RunicParadise.faithMap.get(p.getUniqueId())
											.getPrimaryFaith(), false) == 1) {
						p.getInventory().removeItem(item1a);
						p.getInventory().removeItem(item2a);
						p.sendMessage("Your prayer has been answered!");
					} else {
						// player is at max level, prayer failed!!
						return;
					}

				} else if (needKarma) {
					// Karma is needed for this prayer and player doesnt have
					// enough!
					p.sendMessage("This prayer costs 2 karma; you don't have enough!!");
				}

			} else {
				// player doesnt have enough items :(
				p.sendMessage("Your prayer is rejected! Do you have the right items? Be sure each stack is just the right amount!");
			}
			break;
		case 3:

			if (p.getInventory().contains(item1)
					&& p.getInventory().contains(item2)
					&& p.getInventory().contains(item3)) {
				// Player has enough of first item!

				if (needKarma && targetPlayer.getKarma() >= 2) {
					// Player is trying to pray at a Book - costs 2 karma! they
					// have enough karma, so take it!

					if (RunicParadise.faithMap.get(p.getUniqueId())
							.trySkillUpViaPrayer(
									p,
									RunicParadise.faithMap.get(p.getUniqueId())
											.getPrimaryFaith(), true) == 1) {
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

					if (RunicParadise.faithMap.get(p.getUniqueId())
							.trySkillUpViaPrayer(
									p,
									RunicParadise.faithMap.get(p.getUniqueId())
											.getPrimaryFaith(), false) == 1) {
						p.getInventory().removeItem(item1a);
						p.getInventory().removeItem(item2a);
						p.getInventory().removeItem(item3a);
						p.sendMessage("Your prayer has been answered!");
					} else {
						// player is at max level, prayer failed!!
						return;
					}

				} else if (needKarma) {
					// Karma is needed for this prayer and player doesnt have
					// enough!
					p.sendMessage("This prayer costs 2 karma; you don't have enough!!");
				}

			} else {
				// player doesnt have enough items :(
				p.sendMessage("Your prayer is rejected! Do you have the right items? Be sure each stack is just the right amount!");
			}
			break;
		case 4:
			if (p.getInventory().contains(item1)
					&& p.getInventory().contains(item2)
					&& p.getInventory().contains(item3)
					&& p.getInventory().contains(item4)) {
				// Player has enough of first item!

				if (needKarma && targetPlayer.getKarma() >= 2) {
					// Player is trying to pray at a Book - costs 2 karma! they
					// have enough karma, so take it!

					if (RunicParadise.faithMap.get(p.getUniqueId())
							.trySkillUpViaPrayer(
									p,
									RunicParadise.faithMap.get(p.getUniqueId())
											.getPrimaryFaith(), true) == 1) {
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

					if (RunicParadise.faithMap.get(p.getUniqueId())
							.trySkillUpViaPrayer(
									p,
									RunicParadise.faithMap.get(p.getUniqueId())
											.getPrimaryFaith(), false) == 1) {
						p.getInventory().removeItem(item1a);
						p.getInventory().removeItem(item2a);
						p.getInventory().removeItem(item3a);
						p.getInventory().removeItem(item4a);
						p.sendMessage("Your prayer has been answered!");
					} else {
						// player is at max level, prayer failed!!
						return;
					}

				} else if (needKarma) {
					// Karma is needed for this prayer and player doesnt have
					// enough!
					p.sendMessage("This prayer costs 2 karma; you don't have enough!!");
				}

			} else {
				// player doesnt have enough items :(
				p.sendMessage("Your prayer is rejected! Do you have the right items? Be sure each stack is just the right amount!");
			}
			break;
		default:
			p.sendMessage("Invalid numItems! Should be 1-4. Report this to Rune please.");
			break;

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