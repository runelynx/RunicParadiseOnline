package io.github.runelynx.runicparadise;

import static org.bukkit.Bukkit.getLogger;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import de.slikey.effectlib.EffectManager;
import de.slikey.effectlib.effect.ExplodeEffect;

/**
 * @author andrew
 *
 */
public class Faith {

	public static final int SUN_BURNING_VENGEANCE_LEVEL = 100;
	public static final int SUN_SOLAR_FURY_LEVEL = 50;
	public static final int MOON_LUNAR_CALM_LEVEL = 100;
	public static final int MOON_CELESTIAL_HEALING_LEVEL = 50;
	public static final int STAR_SPELL_1_LEVEL = 50;
	public static final int STAR_SPELL_2_LEVEL = 100;

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
									faithResult.getString("CastMessage") });
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
		//Using this map means faith must be "equipped" -- NOT inactive! 
		if (this.faithLevels.containsKey(faithName)) {
			if (this.faithLevels.get(faithName) >= level) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	public String enableFaith(UUID nUUID, String faithName) {
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
					dbStmt.executeUpdate("INSERT INTO rp_PlayerFaiths (UUID, Active, FaithName, Level) VALUES ('"
							+ nUUID.toString()
							+ "', 1, '"
							+ faithName
							+ "', 0);");
					dbCon.close();
					this.retrievePlayerData(nUUID);
					return "Success";
				} else {
					// results found!

					String simpleProc = "{ call Activate_Faith(?, ?) }";
					CallableStatement cs = dbCon.prepareCall(simpleProc);
					cs.setString("param1", nUUID.toString());
					cs.setString("param2",
							WordUtils.capitalize(faithName.toLowerCase()));
					cs.executeUpdate();

					cs.close();
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
							+ nUUID.toString() + "' ORDER BY Active DESC;");
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

				while (faithResult.next()) {
					if (faithResult.getInt("Active") == 1 && !activeStarted) {
						// Print Active header

						Bukkit.getPlayer(senderUUID).sendMessage(
								ChatColor.GREEN + "Active Faiths ✔");
						Bukkit.getPlayer(senderUUID)
								.sendMessage(
										displayLevelBar(((double) faithResult
												.getInt("Level") / Integer
												.parseInt(RunicParadise.faithSettingsMap.get(faithResult
														.getString("FaithName"))[4])) * 50)
												+ " "
												+ ChatColor.AQUA
												+ faithResult
														.getString("FaithName")
												+ " "
												+ ChatColor.GRAY
												+ faithResult.getInt("Level")
												+ "/"
												+ RunicParadise.faithSettingsMap.get(faithResult
														.getString("FaithName"))[4]);
						activeStarted = true;

					} else if (faithResult.getInt("Active") == 1
							&& activeStarted) {
						Bukkit.getPlayer(senderUUID)
								.sendMessage(
										displayLevelBar(((double) faithResult
												.getInt("Level") / Integer
												.parseInt(RunicParadise.faithSettingsMap.get(faithResult
														.getString("FaithName"))[4])) * 50)
												+ " "
												+ ChatColor.AQUA
												+ faithResult
														.getString("FaithName")
												+ " "
												+ ChatColor.GRAY
												+ faithResult.getInt("Level")
												+ "/"
												+ RunicParadise.faithSettingsMap.get(faithResult
														.getString("FaithName"))[4]);

					} else if (faithResult.getInt("Active") == 0
							&& !inactiveStarted) {
						// Print Inactive header

						Bukkit.getPlayer(senderUUID).sendMessage(
								ChatColor.DARK_RED + "Inactive Faiths ✗");
						Bukkit.getPlayer(senderUUID)
								.sendMessage(
										displayLevelBar(((double) faithResult
												.getInt("Level") / Integer
												.parseInt(RunicParadise.faithSettingsMap.get(faithResult
														.getString("FaithName"))[4])) * 50)
												+ " "
												+ ChatColor.AQUA
												+ faithResult
														.getString("FaithName")
												+ " "
												+ ChatColor.GRAY
												+ faithResult.getInt("Level")
												+ "/"
												+ RunicParadise.faithSettingsMap.get(faithResult
														.getString("FaithName"))[4]);
						inactiveStarted = true;
						double test = (((double) faithResult.getInt("Level") / Integer
								.parseInt(RunicParadise.faithSettingsMap
										.get(faithResult.getString("FaithName"))[4])) * 50);
						getLogger()
								.log(Level.INFO,
										Integer.toString(faithResult
												.getInt("Level"))
												+ "."
												+ RunicParadise.faithSettingsMap.get(faithResult
														.getString("FaithName"))[4]
												+ "." + Double.toString(test));
					} else if (faithResult.getInt("Active") == 0
							&& inactiveStarted) {

						Bukkit.getPlayer(senderUUID)
								.sendMessage(
										displayLevelBar(((double) faithResult
												.getInt("Level") / Integer
												.parseInt(RunicParadise.faithSettingsMap.get(faithResult
														.getString("FaithName"))[4])) * 50)
												+ " "
												+ ChatColor.AQUA
												+ faithResult
														.getString("FaithName")
												+ " "
												+ ChatColor.GRAY
												+ faithResult.getInt("Level")
												+ "/"
												+ RunicParadise.faithSettingsMap.get(faithResult
														.getString("FaithName"))[4]);

					}
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
							+ nUUID.toString() + "' AND Active=1;");
			if (!faithResult.isBeforeFirst()) {
				getLogger().log(
						Level.INFO,
						"No Faiths found for "
								+ Bukkit.getPlayer(nUUID).getDisplayName());

				// No results
				// do nothing
				dbCon.close();
			} else {
				// results found!
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
					this.primaryFaithName = faithResult.getString("faithName");
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
					if (q.hasPermission("rp.faith.user")) {
						q.sendMessage(ChatColor.GRAY + "[" + ChatColor.BLUE
								+ "Runic" + ChatColor.DARK_AQUA + "Faith"
								+ ChatColor.GRAY + "] " + ChatColor.BLUE
								+ p.getDisplayName() + ChatColor.BLUE + " just maxxed their "
								+ ChatColor.WHITE + faithName + ChatColor.BLUE
								+ " faith!");
						q.getWorld().playSound(q.getLocation(), Sound.AMBIENCE_THUNDER, 10, 1);
					}
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
					p.getWorld().playSound(p.getLocation(), Sound.ANVIL_BREAK, 10, 1);
					p.setItemInHand(null);
					p.sendMessage(ChatColor.GRAY + "[" + ChatColor.BLUE
							+ "Runic" + ChatColor.DARK_AQUA + "Faith"
							+ ChatColor.GRAY + "] " + ChatColor.BLUE
							+ "Your faith sword shatters into dust!");
				} else if (RunicParadise.randomSeed.nextInt((100 - 0) + 1) + 0 <= 30
						&& chance == 2) {
					p.getWorld().playSound(p.getLocation(), Sound.ANVIL_BREAK, 10, 1);
					p.setItemInHand(null);
					p.sendMessage(ChatColor.GRAY + "[" + ChatColor.BLUE
							+ "Runic" + ChatColor.DARK_AQUA + "Faith"
							+ ChatColor.GRAY + "] " + ChatColor.BLUE
							+ "Your faith sword shatters into dust!");

				} else if (RunicParadise.randomSeed.nextInt((100 - 0) + 1) + 0 <= 15
						&& chance == 3) {
					p.getWorld().playSound(p.getLocation(), Sound.ANVIL_BREAK, 10, 1);
					
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
		// 2% chance on receiving damage to set all nearby monsters on fire
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