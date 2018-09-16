package io.github.runelynx.runicparadise;

import static org.bukkit.Bukkit.getLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * @author ABO055
 *
 */
public class RunicPlayerBukkit {

	private int tokenBalance;
	private int karma;
	private int lifetimeTokens;

	private String playerIP;
	private String playerName;
	private String playerDisplayName;
	private boolean isStaff;
	private UUID playerUUID;
	private int soulCount;
	private int jobsMasteredCount;
	private String jobsMastered;
	private String currentJob;
	private Date joinDate;
	private int voteCount;
	private String activeFaith;
	private int currentJobLevel;
	private int faithPowerLevel;

	private static Plugin instance = RunicParadise.getInstance();

	public RunicPlayerBukkit(UUID playerID) {
		refreshPlayerObject(Bukkit.getPlayer(playerID));

	}

	/**
	 * Start up the player object using player name
	 * 
	 * @param playerName
	 *            Player name string
	 */
	public RunicPlayerBukkit(String playerName) {
		refreshPlayerObject(Bukkit.getPlayer(playerName));
	}

	public int getKarma() {
		return this.karma;
	}
	
	public String getIP() {
		return this.playerIP;
	}

	public int getFaithPowerLevel() {
		return this.faithPowerLevel;
	}

	/**
	 * Start up the player object [RP] using player object [API]
	 * 
	 * @param player
	 *            Player object
	 */
	public RunicPlayerBukkit(Player player) {
		refreshPlayerObject(player);
	}

	public Map<String, Integer> getPlayerKillCounts() {
		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);

		Map<String, Integer> resultMap = new HashMap<>();

		try {
			// TODO: Change to update DB based on UUID
			Connection connection = MySQL.openConnection();

			PreparedStatement statement = connection
					.prepareStatement("SELECT * FROM rp_PlayerInfo WHERE UUID = ?;");
			statement.setString(1, this.getPlayerUUID());
			ResultSet playerData = statement.executeQuery();

			while (playerData.next()) {
				resultMap.put("KillWither", playerData.getInt("KillWither"));
				resultMap.put("KillZombie", playerData.getInt("KillZombie"));
				resultMap.put("KillWitch", playerData.getInt("KillWitch"));
				resultMap.put("KillSkeleton", playerData.getInt("KillSkeleton"));
				resultMap.put("KillSlime", playerData.getInt("KillSlime"));
				resultMap.put("KillMagmaCube", playerData.getInt("KillMagmaCube"));
				resultMap.put("KillSilverfish", playerData.getInt("KillSilverfish"));
				resultMap.put("KillGiant", playerData.getInt("KillGiant"));
				resultMap.put("KillBlaze", playerData.getInt("KillBlaze"));
				resultMap.put("KillCreeper", playerData.getInt("KillCreeper"));
				resultMap.put("KillEnderman", playerData.getInt("KillEnderman"));
				resultMap.put("KillSpider", playerData.getInt("KillSpider"));
				resultMap.put("KillCaveSpider", playerData.getInt("KillCaveSpider"));
				resultMap.put("KillSquid", playerData.getInt("KillSquid"));
				resultMap.put("KillEnderDragon", playerData.getInt("KillEnderDragon"));
				resultMap.put("KillPigZombie", playerData.getInt("KillPigZombie"));
				resultMap.put("KillGhast", playerData.getInt("KillGhast"));
				resultMap.put("KillChicken", playerData.getInt("KillChicken"));
				resultMap.put("KillCow", playerData.getInt("KillCow"));
				resultMap.put("KillSheep", playerData.getInt("KillSheep"));
				resultMap.put("KillPig", playerData.getInt("KillPig"));
				resultMap.put("KillOcelot", playerData.getInt("KillOcelot"));
				resultMap.put("KillBat", playerData.getInt("KillBat"));
				resultMap.put("KillMooshroom", playerData.getInt("KillMooshroom"));
				resultMap.put("KillRabbit", playerData.getInt("KillRabbit"));
				resultMap.put("KillWolf", playerData.getInt("KillWolf"));
				resultMap.put("KillEndermite", playerData.getInt("KillEndermite"));
				resultMap.put("KillGuardian", playerData.getInt("KillGuardian"));
				resultMap.put("KillElderGuardian", playerData.getInt("KillElderGuardian"));
				resultMap.put("KillSnowGolem", playerData.getInt("KillSnowGolem"));
				resultMap.put("KillIronGolem", playerData.getInt("KillIronGolem"));
				resultMap.put("KillVillager", playerData.getInt("KillVillager"));
				resultMap.put("KillShulker", playerData.getInt("KillShulker"));
				resultMap.put("KillWSkeleton", playerData.getInt("KillWSkeleton"));
				resultMap.put("KillStray", playerData.getInt("KillStray"));
				resultMap.put("KillHusk", playerData.getInt("KillHusk"));
				resultMap.put("KillPolarBear", playerData.getInt("KillPolarBear"));
			}

			connection.close();
			return resultMap;

		} catch (SQLException e) {
			getLogger().log(Level.SEVERE,
					"Failed getPlayerKillCount because: " + e.getMessage());
			return resultMap;
		}
	}

	public String getActiveFaith() {
		return this.activeFaith;
	}

	public void setActiveFaith(String faithName) {
		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);

		try {
			// TODO: Change to update DB based on UUID
			Connection connection = MySQL.openConnection();

			PreparedStatement statement = connection
					.prepareStatement("UPDATE rp_PlayerInfo SET ActiveFaith = '"
							+ faithName + "' WHERE UUID = ?");
			statement.setString(1, this.getPlayerUUID());
			statement.executeUpdate();

			connection.close();

		} catch (SQLException e) {
			getLogger().log(Level.SEVERE,
					"Failed setActiveFaith because: " + e.getMessage());
		}

		this.refreshPlayerObject(Bukkit.getPlayer(this.getPlayerName()));
	}

	public boolean incrementPlayerVotes() {
		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);

		try {
			// TODO: Change to update DB based on UUID
			Connection connection = MySQL.openConnection();
			Date now = new Date();

			PreparedStatement statement = connection
					.prepareStatement("UPDATE rp_PlayerInfo SET Votes = Votes+1 WHERE LastIP = ?");
			statement.setString(1, this.getIP());
			statement.executeUpdate();

			PreparedStatement dStmt3 = connection
					.prepareStatement("INSERT INTO rp_Votes (`PlayerName`, `UUID`, `Timestamp`) VALUES "
							+ "(?, ?, ?);");
			dStmt3.setString(1, this.playerName);
			dStmt3.setString(2, this.getPlayerUUID());
			dStmt3.setLong(3, now.getTime());

			dStmt3.executeUpdate();

			connection.close();
			return true;

		} catch (SQLException e) {
			getLogger().log(Level.SEVERE,
					"Failed incrementPlayerVotes because: " + e.getMessage());
			return false;
		}

	}

	public int getCountGravesCreated() {
		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);

		try {
			// TODO: Change to update DB based on UUID
			Connection connection = MySQL.openConnection();

			PreparedStatement statement = connection
					.prepareStatement("SELECT COUNT(*) FROM rp_PlayerGraves WHERE PlayerName = ?");
			statement.setString(1, this.getPlayerName());
			ResultSet data = statement.executeQuery();

			if (data.isBeforeFirst()) {
				// result found
				data.next();
				int temp = data.getInt("COUNT(*)");
				connection.close();
				return temp;
			} else {
				connection.close();
				return 0;
			}

		} catch (SQLException e) {
			getLogger().log(Level.SEVERE,
					"Failed getCountGravesCreated because: " + e.getMessage());
			return 0;
		}
	}

	public int getCountGravesStolen() {
		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);

		try {
			// TODO: Change to update DB based on UUID
			Connection d = MySQL.openConnection();

			PreparedStatement statement = d
					.prepareStatement("SELECT COUNT(*) FROM rp_PlayerGraves WHERE LooterName = ? AND PlayerName!=?");
			statement.setString(1, this.getPlayerName());
			statement.setString(2, this.getPlayerName());
			ResultSet data = statement.executeQuery();

			if (data.isBeforeFirst()) {
				// result found
				data.next();
				int temp = data.getInt("COUNT(*)");
				d.close();
				return temp;
			} else {
				d.close();
				return 0;
			}

		} catch (SQLException e) {
			getLogger().log(Level.SEVERE,
					"Failed getCountGravesStolen because: " + e.getMessage());
			return 0;
		}
	}

	public int getCountGravesRemaining() {
		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);

		try {
			// TODO: Change to update DB based on UUID
			Connection connection = MySQL.openConnection();

			PreparedStatement statement = connection
					.prepareStatement("SELECT COUNT(*) FROM rp_PlayerGraves WHERE PlayerName = ? AND Status!=?");
			statement.setString(1, this.getPlayerName());
			statement.setString(2, "Gone");
			ResultSet data = statement.executeQuery();

			if (data.isBeforeFirst()) {
				// result found
				data.next();
				int temp = data.getInt("COUNT(*)");
				connection.close();
				return temp;
			} else {
				connection.close();
				return 0;
			}

		} catch (SQLException e) {
			getLogger().log(Level.SEVERE, "Failed getCountGravesRemaining because: " + e.getMessage());
			return 0;
		}
	}

	public static boolean incrementPlayerKillCount(UUID playerUUID, String columnName) {
		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);

		if (!columnName.contains("Kill")) {
			getLogger().log(Level.SEVERE, "Invalid column in incrementPlayerKillCount - given "
					+ columnName + " but expected Kill____");
			return false;
		}

		try {
			Connection connection = MySQL.openConnection();

			PreparedStatement statement = connection
					.prepareStatement("UPDATE rp_PlayerInfo SET " + columnName
							+ "=" + columnName + "+1 WHERE UUID = ?");
			statement.setString(1, playerUUID.toString());
			statement.executeUpdate();

			connection.close();
			return true;

		} catch (SQLException e) {
			getLogger().log(Level.SEVERE,
					"Failed getPlayerKillCount because: " + e.getMessage());
			return false;
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.github.runelynx.runicparadise.RunicPlayer#getPlayerDisplayName()
	 */

	public String getPlayerDisplayName() {
		return this.playerDisplayName;
	}

	/**
	 * Get mastered jobs
	 * 
	 */

	public String getMasteredJobs() {
		return this.jobsMastered;

	}

	public int getMasteredJobCount() {
		return this.jobsMasteredCount;
	}

	public int getPlayerVoteCount() {
		return this.voteCount;
	}

	/**
	 * Get current job
	 * 
	 */

	public String getCurrentJob() {
		return this.currentJob;

	}

	public int getCurrentJobLevel() {
		return this.currentJobLevel;

	}

	public Date getJoinDate() {
		return this.joinDate;
	}

	/**
	 * job table maintenance in DB
	 * 
	 */

	public static void maintainJobTable() {
		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);

		Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
				"sc Rune's job maintenance has started. This hits the database hard - incoming lag!");

		try {
			// TODO: Change to update DB based on UUID
			Connection connection = MySQL.openConnection();
			Statement dStmt = connection.createStatement();

			PreparedStatement dStmt2 = connection
					.prepareStatement("SELECT hex(player_uuid) as player_uuid,id FROM Jobs_jobs WHERE runes_uuid_field IS NULL;");

			getLogger().log(Level.INFO, "rpjobs debug maintenance starting");

			ResultSet playerData2 = dStmt2.executeQuery();
			int counter = 0;

			if (playerData2.isBeforeFirst()) {
				// result found!
				while (playerData2.next()) {
					dStmt.executeUpdate("UPDATE `Jobs_jobs` SET runes_uuid_field='"
							+ playerData2.getString("player_uuid")
							+ "' WHERE id=" + playerData2.getInt("id") + ";");
					counter++;
				}
			}
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
					"sc Rune's job maintenance progress: " + counter
							+ " UUIDs transformed in jobs table.");

			PreparedStatement dStmt3 = connection
					.prepareStatement("SELECT ID,UUID FROM rp_PlayerInfo WHERE UUID_nodashes IS NULL;");
			getLogger()
					.log(Level.INFO,
							"rpjobs debug maintenance player table about to execute query");
			ResultSet playerData3 = dStmt3.executeQuery();
			counter = 0;
			getLogger().log(Level.INFO,
					"rpjobs debug maintenance player table about to start");
			if (playerData3.isBeforeFirst()) {
				// result found!
				getLogger().log(Level.INFO,
						"rpjobs debug maintenance player table starting");
				while (playerData3.next()) {
					dStmt.executeUpdate("UPDATE `rp_PlayerInfo` SET UUID_nodashes='"
							+ playerData3.getString("UUID").replace("-", "")
							+ "' WHERE ID=" + playerData3.getInt("ID") + ";");
					counter++;
				}

			}
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
					"sc Rune's job maintenance progress: " + counter
							+ " UUIDs transformed in players table.");

			PreparedStatement dStmt4 = connection
					.prepareStatement("SELECT UUID_nodashes,PlayerName FROM rp_PlayerInfo");
			ResultSet playerData4 = dStmt4.executeQuery();
			int dupeCounter = 0;
			int noJobCounter = 0;

			if (playerData4.isBeforeFirst()) {
				// result found!
				while (playerData4.next()) {
					ResultSet jobTableCheck = dStmt3
							.executeQuery("SELECT COUNT(id) FROM `Jobs_jobs` WHERE runes_uuid_field='"
									+ playerData4.getString("UUID_nodashes")
									+ "';");
					jobTableCheck.next();
					if (jobTableCheck.getInt("COUNT(id)") > 1) {
						// duplicate found!
						dupeCounter++;
						Bukkit.dispatchCommand(
								Bukkit.getConsoleSender(),
								"sc "
										+ ChatColor.DARK_RED
										+ "Duplicate job record!! "
										+ playerData4
												.getString("UUID_nodashes")
										+ " : "
										+ playerData4.getString("PlayerName"));
					} else if (jobTableCheck.getInt("COUNT(id)") == 0) {
						noJobCounter++;
					}
				}
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
						"sc Rune's job maintenance progress: " + dupeCounter
								+ " duplicates in job table.");
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
						"sc Rune's job maintenance progress: " + noJobCounter
								+ " unemployed players.");
			}

			connection.close();
			getLogger().log(Level.INFO,
					"[RpJobMastery] Maintenance completed. ");
			// Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "sudo " +
			// this.playerName + " jobs leave " + oldJob);

		} catch (SQLException e) {
			getLogger().log(Level.SEVERE,
					"Failed JobMastery maintenance because: " + e.getMessage());

		}
	}

	/**
	 * Master a job
	 * 
	 */

	public boolean executeJobMastery() {
		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);

		try {
			// TODO: Change to update DB based on UUID
			Connection connection = MySQL.openConnection();
			Statement dStmt = connection.createStatement();
			String newJobString = "";
			int newJobCount = 0;
			String oldJob = "";

			if (this.jobsMastered.equals("None")) {
				// This is player's first mastery

				int jobsID = 0;
				ResultSet jobPlayerIDResult = dStmt
						.executeQuery("SELECT ID FROM `Jobs_users` WHERE `player_uuid` = '"
								+ this.getPlayerUUID().toString()
								+ "' ORDER BY `id` ASC LIMIT 1;");
				if (jobPlayerIDResult.isBeforeFirst()) {
					// Player doesn't exist in the DB!
					// TODO: Need to add them to DB!
					jobPlayerIDResult.next();
					jobsID = jobPlayerIDResult.getInt("id");
				}

				PreparedStatement dStmt2 = connection
						.prepareStatement("SELECT job, level FROM Jobs_jobs WHERE userid = ?");
				dStmt2.setInt(1, jobsID);

				getLogger().log(
						Level.INFO,
						"rpjobs debug executeJobMastery: " + this.playerName
								+ " - "
								+ this.getPlayerUUID().replace('-', '\u0000'));

				ResultSet playerData = dStmt2.executeQuery();

				if (playerData.isBeforeFirst()) {
					// result found!
					playerData.next();
					if (playerData.getInt("level") < 25) {
						// Player permission check for level25 passed but actual
						// level in job DB is <25... this is probably an admin!
						this.sendMessageToPlayer("Job mastery FAILURE. Jobs plugin believes you have level 25... but the database does not!");
						this.sendMessageToPlayer("This might be because you're an admin? Contact Rune for help.");
						return false;
					}
					oldJob = playerData.getString("job");
					dStmt.executeUpdate("UPDATE `rp_PlayerInfo` SET JobsMastered='"
							+ oldJob
							+ "', JobsMasteredCount=1 WHERE PlayerName='"
							+ this.playerName + "';");
					// Update object directly since change successful
					this.jobsMastered = playerData.getString("job");
					this.jobsMasteredCount = 1;

				} else {
					// record not found in jobs table... critical error!
					getLogger().log(
							Level.SEVERE,
							"[RPJobMastery] Couldnt find record for "
									+ this.playerName + " in Jobs_jobs table!");
					return false;
				}
			} else {
				// Player has already mastered a job!
				int jobsID = 0;
				ResultSet jobPlayerIDResult = dStmt
						.executeQuery("SELECT ID FROM `Jobs_users` WHERE `player_uuid` = '"
								+ this.getPlayerUUID().toString()
								+ "' ORDER BY `id` ASC LIMIT 1;");
				if (jobPlayerIDResult.isBeforeFirst()) {
					// Player doesn't exist in the DB!
					// TODO: Need to add them to DB!
					jobPlayerIDResult.next();
					jobsID = jobPlayerIDResult.getInt("id");
				}

				PreparedStatement dStmt2 = connection
						.prepareStatement("SELECT job, level FROM Jobs_jobs WHERE userid = ?");
				dStmt2.setInt(1, jobsID);

				getLogger().log(
						Level.INFO,
						"rpjobs debug executeJobMastery: " + this.playerName
								+ " - "
								+ this.getPlayerUUID().replace('-', '\u0000'));

				ResultSet playerData = dStmt2.executeQuery();

				if (playerData.isBeforeFirst()) {
					// result found!
					playerData.next();
					if (playerData.getInt("level") < 25) {
						// Player permission check for level25 passed but actual
						// level in job DB is <25... this is probably an admin!
						this.sendMessageToPlayer("You don't have level 25 in your job. Perm check failed - user probably admin. This is OK!");
						return false;
					}
					newJobString = this.jobsMastered + ", "
							+ playerData.getString("job");
					newJobCount = this.jobsMasteredCount + 1;
					oldJob = playerData.getString("job");
					dStmt.executeUpdate("UPDATE `rp_PlayerInfo` SET JobsMastered='"
							+ newJobString
							+ "', JobsMasteredCount="
							+ newJobCount
							+ " WHERE PlayerName='"
							+ this.playerName + "';");

					// Update object directly since change successful
					this.jobsMastered = newJobString;
					this.jobsMasteredCount = newJobCount;

				} else {
					// record not found in jobs table... critical error!
					getLogger().log(
							Level.SEVERE,
							"[RPJobMastery] Couldnt find record for "
									+ this.playerName + " in Jobs_jobs table!");
					this.sendMessageToPlayer(ChatColor.RED
							+ "Couldn't find a job for you. Do you have level 25 in a tier1 job?");
					return false;
				}
			}

			connection.close();
			getLogger()
					.log(Level.INFO,
							"[RpJobMastery] Success: " + this.playerName + ", "
									+ this.jobsMastered + ", "
									+ this.jobsMasteredCount);
			// Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "sudo " +
			// this.playerName + " jobs leave " + oldJob);
			return true;

		} catch (SQLException e) {
			getLogger().log(Level.SEVERE,
					"Failed JobMastery because: " + e.getMessage());
			return false; // since change failed
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.github.runelynx.runicparadise.RunicPlayer#getPlayerUUID()
	 */

	public String getPlayerUUID() {
		return this.playerUUID.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.github.runelynx.runicparadise.RunicPlayer#sendMessageToPlayer()
	 */

	public void sendMessageToPlayer(String message) {
		Bukkit.getPlayer(this.playerUUID).sendMessage(message);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.github.runelynx.runicparadise.RunicPlayer#isPlayerStaff()
	 */

	public boolean isPlayerStaff() {
		if (this.isStaff) {
			return true;
		} else {
			return false;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.github.runelynx.runicparadise.RunicPlayer#getPlayerTokenBalance()
	 */

	public int getPlayerTokenBalance() {
		// refreshPlayerObject(Bukkit.getPlayer(this.playerUUID));
		return this.tokenBalance;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.github.runelynx.runicparadise.RunicPlayer#getPlayerIceMazeCompletions
	 * ()
	 */

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.github.runelynx.runicparadise.RunicPlayer#getPlayerLifetimeTokens
	 * ()
	 */

	public int getPlayerLifetimeTokens() {
		// refreshPlayerObject(Bukkit.getPlayer(this.playerUUID));
		return this.lifetimeTokens;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.github.runelynx.runicparadise.RunicPlayer#getPlayerSouls ()
	 */

	public int getPlayerSouls() {
		// refreshPlayerObject(Bukkit.getPlayer(this.playerUUID));
		return this.soulCount;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.github.runelynx.runicparadise.RunicPlayer#setPlayerLifetimeTokens()
	 */

	public boolean setPlayerLifetimeTokens(int increment) {
		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);

		int newBalance = this.getPlayerLifetimeTokens() + increment;

		try {
			// TODO: Change to update DB based on UUID
			Connection connection = MySQL.openConnection();
			Statement statement = connection.createStatement();
			statement.executeUpdate("UPDATE `rp_PlayerInfo` SET LifetimeTokens="
					+ newBalance + " WHERE PlayerName='" + this.playerName
					+ "';");
			connection.close();

			// Update object directly since change successful
			this.lifetimeTokens = newBalance;

			return true; // since change was successful
		} catch (SQLException e) {
			getLogger().log(Level.SEVERE,
					"Failed lifetimeTokenUpdate because: " + e.getMessage());
			return false; // since change failed
		}
	}

	public static boolean adjustOfflinePlayerKarma(String name, int change) {
		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);

		try {
			// TODO: Change to update DB based on UUID
			Connection connection = MySQL.openConnection();
			Statement statement = connection.createStatement();
			statement.executeUpdate("UPDATE `rp_PlayerInfo` SET Karma=Karma+"
					+ change + " WHERE PlayerName='" + name + "';");
			connection.close();
			// Update the player object directly since the DB change was
			// successful

			if (Bukkit.getPlayer(name) != null) {

				Bukkit.getPlayer(name).sendMessage(
						ChatColor.GRAY + "[" + ChatColor.BLUE + "Runic"
								+ ChatColor.DARK_AQUA + "Faith"
								+ ChatColor.GRAY + "] " + ChatColor.GRAY
								+ "Your karma changed by " + ChatColor.AQUA
								+ change + ChatColor.GRAY + ".");
			}

			return true; // since change was successful
		} catch (SQLException e) {
			getLogger().log(Level.SEVERE, "Failed karma update (change " + name + " . ) because: " + e.getMessage());
			return false; // since change failed
		}
	}

	public boolean adjustPlayerKarma(int change) {
		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);

		try {
			// TODO: Change to update DB based on UUID
			Connection d = MySQL.openConnection();
			Statement statement = d.createStatement();
			statement.executeUpdate("UPDATE `rp_PlayerInfo` SET Karma=Karma+"
					+ change + " WHERE PlayerName='" + this.playerName + "';");
			d.close();
			// Update the player object directly since the DB change was
			// successful
			this.karma = this.karma + change;

			Bukkit.getPlayer(this.playerUUID)
					.sendMessage(
							ChatColor.GRAY + "[" + ChatColor.BLUE + "Runic"
									+ ChatColor.DARK_AQUA + "Faith"
									+ ChatColor.GRAY + "] " + ChatColor.GRAY
									+ "Your karma changed by " + ChatColor.AQUA
									+ change + ChatColor.GRAY + ". It is now "
									+ ChatColor.DARK_AQUA + this.karma);

			return true; // since change was successful
		} catch (SQLException e) {
			getLogger().log(
					Level.SEVERE,
					"Failed karma update (change " + this.playerName + " to "
							+ this.karma + ") because: " + e.getMessage());
			return false; // since change failed
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.github.runelynx.runicparadise.RunicPlayer#setPlayerSouls()
	 */

	public boolean setPlayerSouls(int newSoulCount) {
		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);

		try {
			// TODO: Change to update DB based on UUID
			Connection connection = MySQL.openConnection();
			Statement statement = connection.createStatement();
			statement.executeUpdate("UPDATE `rp_PlayerInfo` SET SoulCount='"
					+ newSoulCount + "' WHERE PlayerName='" + this.playerName
					+ "';");
			connection.close();
			// Update the player object directly since the DB change was
			// successful
			this.soulCount = newSoulCount;
			return true; // since change was successful
		} catch (SQLException e) {
			getLogger().log(
					Level.SEVERE,
					"Failed soulcount update (change " + this.playerName
							+ " to " + newSoulCount + ") because: "
							+ e.getMessage());
			return false; // since change failed
		}
	}

	public boolean addPlayerSouls(int addition) {
		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);

		try {
			// TODO: Change to update DB based on UUID
			Connection connection = MySQL.openConnection();
			Statement statement = connection.createStatement();
			statement.executeUpdate("UPDATE `rp_PlayerInfo` SET SoulCount=SoulCount+"
					+ addition + " WHERE LastIP='" + this.getIP()
					+ "';");
			connection.close();
			// Update the player object directly since the DB change was
			// successful
			this.soulCount =  this.soulCount  + addition;
			return true; // since change was successful
		} catch (SQLException e) {
			getLogger().log(
					Level.SEVERE,
					"Failed soulcount addition (change " + this.playerName
							+ " to " + addition + ") because: "
							+ e.getMessage());
			return false; // since change failed
		}
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see io.github.runelynx.runicparadise.RunicPlayer#setPlayerTokenBalance()
	 */

	public boolean setPlayerTokenBalance(int newBalance) {
		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);

		int lifetimeBalanceIncrement = newBalance - this.tokenBalance;

		try {
			// TODO: Change to update DB based on UUID
			Connection connection = MySQL.openConnection();
			Statement statement = connection.createStatement();
			statement.executeUpdate("UPDATE `rp_PlayerInfo` SET Tokens='"
					+ newBalance + "' WHERE PlayerName='" + this.playerName
					+ "';");
			connection.close();
			this.sendMessageToPlayer("Your new token balance is " + newBalance
					+ "! Spend them at /games 2");
			// Update the player object directly since the DB change was
			// successful
			this.tokenBalance = newBalance;
			this.setPlayerLifetimeTokens(lifetimeBalanceIncrement);
			return true; // since change was successful
		} catch (SQLException e) {
			getLogger().log(
					Level.SEVERE,
					"Failed tokenbalance update (change " + this.playerName
							+ " to " + newBalance + ") because: "
							+ e.getMessage());
			return false; // since change failed
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.github.runelynx.runicparadise.RunicPlayer#translateChatColors()
	 */

	public String translateChatColors() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.github.runelynx.runicparadise.RunicPlayer#getPlayerName()
	 */

	public String getPlayerName() {
		return this.playerName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.github.runelynx.runicparadise.RunicPlayer#refreshPlayerObject()
	 */

	public void refreshPlayerObject(OfflinePlayer player) {
		this.playerName = player.getName();
		
		try {
			this.playerDisplayName = player.getPlayer().getDisplayName();
			this.isStaff = player.getPlayer().hasPermission("rp.staff");
		} catch (Exception e) {
			this.playerDisplayName = player.getName();
			this.isStaff = false;
		}
		
		
		this.playerUUID = player.getUniqueId();

		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);

		try {
			// TODO: Get data from DB based on UUID to protect vs name changes
			Connection connection = MySQL.openConnection();
			Statement statement = connection.createStatement();

			int jobsID = 0;
			ResultSet jobPlayerIDResult = statement
					.executeQuery("SELECT ID FROM `Jobs_users` WHERE `player_uuid` = '"
							+ this.getPlayerUUID().toString()
							+ "' ORDER BY `id` ASC LIMIT 1;");
			if (jobPlayerIDResult.isBeforeFirst()) {
				// Player doesn't exist in the DB!
				// TODO: Need to add them to DB!
				jobPlayerIDResult.next();
				jobsID = jobPlayerIDResult.getInt("id");
			}

			ResultSet playerData = statement
					.executeQuery("SELECT * FROM `rp_PlayerInfo` WHERE `PlayerName` = '"
							+ this.playerName + "' ORDER BY `id` ASC LIMIT 1;");

			// ResultSet jobData = dStmt2
			// .executeQuery("SELECT job,level FROM `Jobs_jobs` WHERE `player_uuid` = '"+
			// this.playerUUID + "' LIMIT 1;");

			PreparedStatement dStmt2 = connection
					.prepareStatement("SELECT job,level FROM Jobs_jobs WHERE userid = "
							+ jobsID + " LIMIT 1;");

			ResultSet jobData = dStmt2.executeQuery();

			// if (!playerData.first() && !playerData.next()) {
			if (jobData.isBeforeFirst()) {
				// Player doesn't exist in the DB!
				// TODO: Need to add them to DB!
				jobData.next();
				this.currentJob = jobData.getString("job");
				this.currentJobLevel = jobData.getInt("level");
			} else {
				this.currentJob = "None";
				this.currentJobLevel = 0;
			}

			PreparedStatement dStmt3 = connection
					.prepareStatement("SELECT SUM(Level) AS FPL FROM rp_PlayerFaiths WHERE UUID = ?;");
			dStmt3.setString(1, this.getPlayerUUID());

			ResultSet faithData = dStmt3.executeQuery();
			// if (!playerData.first() && !playerData.next()) {
			if (faithData.isBeforeFirst()) {
				// Player doesn't exist in the DB!
				// TODO: Need to add them to DB!
				faithData.next();
				this.faithPowerLevel = faithData.getInt("FPL");

			} else {

				this.faithPowerLevel = 0;
			}

			if (!playerData.isBeforeFirst()) {
				// Player doesn't exist in the DB!
				// TODO: Need to add them to DB!
				this.tokenBalance = 0;
				getLogger().log(
						Level.INFO,
						"[RP] Player " + this.playerName
								+ " isn't in our DB yet.");
			} else {
				// Player does exist in the DB
				playerData.next();
				this.playerIP = playerData.getString("LastIP");
				this.karma = playerData.getInt("Karma");
				this.tokenBalance = playerData.getInt("Tokens");
				this.soulCount = playerData.getInt("SoulCount");

				this.lifetimeTokens = playerData.getInt("LifetimeTokens");
				this.jobsMasteredCount = playerData.getInt("JobsMasteredCount");
				this.jobsMastered = playerData.getString("JobsMastered");
				this.joinDate = new Date(playerData.getLong("FirstSeen"));
				this.voteCount = playerData.getInt("Votes");
				this.activeFaith = playerData.getString("ActiveFaith");

				connection.close();
			}
		} catch (SQLException e) {
			getLogger().log(
					Level.SEVERE,
					"Failed DB check [refreshPlayerObject] because: "
							+ e.getMessage());
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.github.runelynx.runicparadise.RunicPlayer# setPlayerLevel()
	 */
	public void setPlayerLevel(int newLevel) {
		Bukkit.getPlayer(this.playerUUID).setLevel(newLevel);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.github.runelynx.runicparadise.RunicPlayer#
	 * checkPlayerPermission()
	 */
	public boolean checkPlayerPermission(String permission) {
		return Bukkit.getPlayer(this.playerUUID).hasPermission(permission);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.github.runelynx.runicparadise.RunicPlayer# getPlayerLevel()
	 */
	public int getPlayerLevel() {
		return Bukkit.getPlayer(this.playerUUID).getLevel();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.github.runelynx.runicparadise.RunicPlayer#
	 * checkPlayerInventoryItemstackCount()
	 */

	public int checkPlayerInventoryItemstackCount() {

		int invSlot = 0;
		int itemCounter = 0;

		while (invSlot < 36) {
			if (Bukkit.getPlayer(this.playerUUID).getInventory()
					.getItem(invSlot) != null
					&& Bukkit.getPlayer(this.playerUUID).getInventory()
							.getItem(invSlot).getType() != Material.AIR) {

				itemCounter++;
			}
			invSlot++;
		}

		return itemCounter;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.github.runelynx.runicparadise.RunicPlayer#
	 * checkPlayerWearingArmor()
	 */

	public boolean checkPlayerWearingArmor() {

		ItemStack[] items = Bukkit.getPlayer(this.playerUUID).getInventory()
				.getArmorContents();

		int playerArmorCount = 0;

		// Count actual itemstacks in player's inventory
		for (ItemStack item : items) {
			if ((item != null) && (item.getAmount() > 0)
					&& (item.getType() != Material.AIR)) {
				playerArmorCount++;
			}
		}

		if (playerArmorCount > 0) {
			return true;
		} else {
			return false;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.github.runelynx.runicparadise.RunicPlayer#
	 * checkPlayerInventoryForItemDataCount()
	 */

//	public int checkPlayerInventoryForItemDataCount(int id, int dataValue) {
//		PlayerInventory inventory = Bukkit.getPlayer(this.playerUUID)
//				.getInventory();
//		ItemStack[] items = inventory.getContents();
//		int has = 0;
//		for (ItemStack item : items) {
//			if ((item != null) && (item.getTypeId() == id)
//					&& (item.getDurability() == dataValue)
//					&& (item.getAmount() > 0)) {
//				has += item.getAmount();
//
//			}
//		}
//		return has;
//	}

//	public int removePlayerInventoryItemData(int id, int dataValue) {
//		PlayerInventory inventory = Bukkit.getPlayer(this.playerUUID)
//				.getInventory();
//		int cleared = inventory.clear(id, dataValue);
//		return cleared;
//
//	}

//	public void givePlayerItemData(int count, int id, int dataValue,
//			int loreCount, String displayName, String lore1, String lore2,
//			String lore3) {
//		ItemStack newItem = new ItemStack(id, count, (short) dataValue);
//		ItemMeta meta = newItem.getItemMeta();
//		meta.setDisplayName(displayName);
//		if (loreCount == 1) {
//			meta.setLore(Arrays.asList(lore1));
//		} else if (loreCount == 2) {
//			meta.setLore(Arrays.asList(lore1, lore2));
//		} else if (loreCount >= 3) {
//			meta.setLore(Arrays.asList(lore1, lore2, lore3));
//		}
//
//		newItem.setItemMeta(meta);
//		PlayerInventory inventory = Bukkit.getPlayer(this.playerUUID)
//				.getInventory();
//		inventory.addItem(newItem);
//	}

	public void givePlayerItemStack(ItemStack[] items) {
		PlayerInventory inventory = Bukkit.getPlayer(this.playerUUID).getInventory();

		for (ItemStack item : items) {
			if (item != null) {
				inventory.addItem(item);
			}

		}
		new BukkitRunnable() {
			public void run() {
				for (Player p : Bukkit.getOnlinePlayers())
					p.updateInventory();
			}
		}.runTaskLater(instance, 0);
	}
}
