/**
 * 
 */
package io.github.runelynx.runicparadise;

import static org.bukkit.Bukkit.getLogger;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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
public class RunicPlayerBukkit implements RunicPlayer {

	private int tokenBalance;
	private int lifetimeTokens;
	private int hedgeMazeCompletions;
	private int iceMazeCompletions;
	private String playerName;
	private String playerDisplayName;
	private boolean isStaff;
	private UUID playerUUID;
	private int soulCount;
	private int jobsMasteredCount;
	private String jobsMastered;
	private String currentJob;
	private Date joinDate;

	private Plugin instance = RunicParadise.getInstance();

	/**
	 * Start up the player object using player UUID
	 * 
	 * @param playerName
	 *            Player name string
	 */
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
		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));

		Map<String, Integer> resultMap = new HashMap<String, Integer>();

		try {
			// TODO: Change to update DB based on UUID
			final Connection d = MySQL.openConnection();
			Statement dStmt = d.createStatement();

			PreparedStatement dStmt2 = d
					.prepareStatement("SELECT `rp_PlayerInfo`.`KillWither`, `rp_PlayerInfo`.`KillZombie`, `rp_PlayerInfo`.`KillWitch`,"
							+ " `rp_PlayerInfo`.`KillSkeleton`, `rp_PlayerInfo`.`KillSlime`, `rp_PlayerInfo`.`KillMagmaCube`, `rp_PlayerInfo`.`KillSilverfish`, `rp_PlayerInfo`.`KillGiant`, "
							+ "`rp_PlayerInfo`.`KillBlaze`,  `rp_PlayerInfo`.`KillCreeper`, `rp_PlayerInfo`.`KillEnderman`, `rp_PlayerInfo`.`KillSpider`, `rp_PlayerInfo`.`KillCaveSpider`, "
							+ "`rp_PlayerInfo`.`KillSquid`, `rp_PlayerInfo`.`KillEnderDragon`, `rp_PlayerInfo`.`KillPigZombie`, `rp_PlayerInfo`.`KillGhast`, `rp_PlayerInfo`.`KillChicken`, "
							+ "`rp_PlayerInfo`.`KillCow`, `rp_PlayerInfo`.`KillSheep`, `rp_PlayerInfo`.`KillPig`, `rp_PlayerInfo`.`KillOcelot`, `rp_PlayerInfo`.`KillBat`, `rp_PlayerInfo`.`KillMooshroom`, "
							+ "`rp_PlayerInfo`.`KillRabbit`, `rp_PlayerInfo`.`KillWolf`, `rp_PlayerInfo`.`KillEndermite`, `rp_PlayerInfo`.`KillGuardian`, `rp_PlayerInfo`.`KillElderGuardian`, "
							+ "`rp_PlayerInfo`.`KillSnowGolem`, `rp_PlayerInfo`.`KillIronGolem`, `rp_PlayerInfo`.`KillVillager` FROM rpgame.rp_PlayerInfo WHERE UUID = ?;");
			dStmt2.setString(1, this.getPlayerUUID());
			ResultSet playerData = dStmt2.executeQuery();

			while (playerData.next()) {
				resultMap.put("KillWither", playerData.getInt("KillWither"));
				resultMap.put("KillZombie", playerData.getInt("KillZombie"));
				resultMap.put("KillWitch", playerData.getInt("KillWitch"));
				resultMap
						.put("KillSkeleton", playerData.getInt("KillSkeleton"));
				resultMap.put("KillSlime", playerData.getInt("KillSlime"));
				resultMap.put("KillMagmaCube",
						playerData.getInt("KillMagmaCube"));
				resultMap.put("KillSilverfish",
						playerData.getInt("KillSilverfish"));
				resultMap.put("KillGiant", playerData.getInt("KillGiant"));
				resultMap.put("KillBlaze", playerData.getInt("KillBlaze"));
				resultMap.put("KillCreeper", playerData.getInt("KillCreeper"));
				resultMap
						.put("KillEnderman", playerData.getInt("KillEnderman"));
				resultMap.put("KillSpider", playerData.getInt("KillSpider"));
				resultMap.put("KillCaveSpider",
						playerData.getInt("KillCaveSpider"));
				resultMap.put("KillSquid", playerData.getInt("KillSquid"));
				resultMap.put("KillEnderDragon",
						playerData.getInt("KillEnderDragon"));
				resultMap.put("KillPigZombie",
						playerData.getInt("KillPigZombie"));
				resultMap.put("KillGhast", playerData.getInt("KillGhast"));
				resultMap.put("KillChicken", playerData.getInt("KillChicken"));
				resultMap.put("KillCow", playerData.getInt("KillCow"));
				resultMap.put("KillSheep", playerData.getInt("KillSheep"));
				resultMap.put("KillPig", playerData.getInt("KillPig"));
				resultMap.put("KillOcelot", playerData.getInt("KillOcelot"));
				resultMap.put("KillBat", playerData.getInt("KillBat"));
				resultMap.put("KillMooshroom",
						playerData.getInt("KillMooshroom"));
				resultMap.put("KillRabbit", playerData.getInt("KillRabbit"));
				resultMap.put("KillWolf", playerData.getInt("KillWolf"));
				resultMap.put("KillEndermite",
						playerData.getInt("KillEndermite"));
				resultMap
						.put("KillGuardian", playerData.getInt("KillGuardian"));
				resultMap.put("KillElderGuardian",
						playerData.getInt("KillElderGuardian"));
				resultMap.put("KillSnowGolem",
						playerData.getInt("KillSnowGolem"));
				resultMap.put("KillIronGolem",
						playerData.getInt("KillIronGolem"));
				resultMap
						.put("KillVillager", playerData.getInt("KillVillager"));
			}

			d.close();
			return resultMap;

		} catch (SQLException e) {
			getLogger().log(Level.SEVERE,
					"Failed getPlayerKillCount because: " + e.getMessage());
			return resultMap;
		}

	}

	public int getCountGravesCreated() {
		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));

		try {
			// TODO: Change to update DB based on UUID
			final Connection d = MySQL.openConnection();

			PreparedStatement dStmt2 = d
					.prepareStatement("SELECT COUNT(*) FROM rp_PlayerGraves WHERE PlayerName = ?");
			dStmt2.setString(1, this.getPlayerName());
			ResultSet data = dStmt2.executeQuery();

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
					"Failed getCountGravesCreated because: " + e.getMessage());
			return 0;
		}
	}

	public int getCountGravesStolen() {
		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));

		try {
			// TODO: Change to update DB based on UUID
			final Connection d = MySQL.openConnection();

			PreparedStatement dStmt2 = d
					.prepareStatement("SELECT COUNT(*) FROM rp_PlayerGraves WHERE LooterName = ? AND PlayerName!=?");
			dStmt2.setString(1, this.getPlayerName());
			dStmt2.setString(2, this.getPlayerName());
			ResultSet data = dStmt2.executeQuery();

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
		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));

		try {
			// TODO: Change to update DB based on UUID
			final Connection d = MySQL.openConnection();

			PreparedStatement dStmt2 = d
					.prepareStatement("SELECT COUNT(*) FROM rp_PlayerGraves WHERE PlayerName = ? AND Status!=?");
			dStmt2.setString(1, this.getPlayerName());
			dStmt2.setString(2, "Gone");
			ResultSet data = dStmt2.executeQuery();

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
			getLogger()
					.log(Level.SEVERE,
							"Failed getCountGravesRemaining because: "
									+ e.getMessage());
			return 0;
		}
	}

	public boolean incrementPlayerKillCount(String columnName) {
		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));

		if (!columnName.contains("Kill")) {
			getLogger().log(
					Level.SEVERE,
					"Invalid column in incrementPlayerKillCount - given "
							+ columnName + " but expected Kill____");
			return false;
		}

		try {
			// TODO: Change to update DB based on UUID
			final Connection d = MySQL.openConnection();

			PreparedStatement dStmt2 = d
					.prepareStatement("UPDATE rp_PlayerInfo SET " + columnName
							+ "=" + columnName + "+1 WHERE UUID = ?");
			dStmt2.setString(1, this.getPlayerUUID());
			dStmt2.executeUpdate();

			d.close();
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
	@Override
	public String getPlayerDisplayName() {
		return this.playerDisplayName;
	}

	/**
	 * Get mastered jobs
	 * 
	 */
	@Override
	public String getMasteredJobs() {
		return this.jobsMastered;

	}

	public int getMasteredJobCount() {
		return this.jobsMasteredCount;
	}

	/**
	 * Get current job
	 * 
	 */
	@Override
	public String getCurrentJob() {
		return this.currentJob;

	}

	@Override
	public Date getJoinDate() {
		return this.joinDate;
	}

	/**
	 * job table maintenance in DB
	 * 
	 */
	@Override
	public void maintainJobTable() {
		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));
		
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
				"sc Rune's job maintenance has started. This hits the database hard - incoming lag!");

		try {
			// TODO: Change to update DB based on UUID
			final Connection d = MySQL.openConnection();
			Statement dStmt = d.createStatement();

			PreparedStatement dStmt2 = d
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

			PreparedStatement dStmt3 = d
					.prepareStatement("SELECT ID,UUID FROM rp_PlayerInfo WHERE UUID_nodashes IS NULL;");
			getLogger().log(Level.INFO, "rpjobs debug maintenance player table about to execute query");
			ResultSet playerData3 = dStmt3.executeQuery();
			counter = 0;
			getLogger().log(Level.INFO, "rpjobs debug maintenance player table about to start");
			if (playerData3.isBeforeFirst()) {
				// result found!
				getLogger().log(Level.INFO, "rpjobs debug maintenance player table starting");
				while (playerData3.next()) {
					dStmt.executeUpdate("UPDATE `rp_PlayerInfo` SET UUID_nodashes='"
							+ playerData3.getString("UUID").replace("-","")
							+ "' WHERE ID="
							+ playerData3.getInt("ID") + ";");
					counter++;
				}

			}
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
					"sc Rune's job maintenance progress: " + counter
							+ " UUIDs transformed in players table.");

			PreparedStatement dStmt4 = d
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

			d.close();
			getLogger()
					.log(Level.INFO,
							"[RpJobMastery] Success: " + this.playerName + ", "
									+ this.jobsMastered + ", "
									+ this.jobsMasteredCount);
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
	@Override
	public boolean executeJobMastery() {
		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));

		try {
			// TODO: Change to update DB based on UUID
			final Connection d = MySQL.openConnection();
			Statement dStmt = d.createStatement();
			String newJobString = "";
			int newJobCount = 0;
			String oldJob = "";

			if (this.jobsMastered.equals("None")) {
				// This is player's first mastery

				PreparedStatement dStmt2 = d
						.prepareStatement("SELECT job,level FROM Jobs_jobs WHERE player_uuid = UNHEX(?) LIMIT 1;");
				dStmt2.setString(1, this.getPlayerUUID().replace("-", ""));

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
				PreparedStatement dStmt2 = d
						.prepareStatement("SELECT job,level FROM Jobs_jobs WHERE player_uuid = UNHEX(?) LIMIT 1;");
				dStmt2.setString(1, this.getPlayerUUID().replace("-", ""));

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

			d.close();
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
	@Override
	public String getPlayerUUID() {
		return this.playerUUID.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.github.runelynx.runicparadise.RunicPlayer#sendMessageToPlayer()
	 */
	@Override
	public void sendMessageToPlayer(String message) {
		Bukkit.getPlayer(this.playerUUID).sendMessage(message);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.github.runelynx.runicparadise.RunicPlayer#isPlayerStaff()
	 */
	@Override
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
	@Override
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
	@Override
	public int getPlayerIceMazeCompletions() {
		// refreshPlayerObject(Bukkit.getPlayer(this.playerUUID));
		return this.iceMazeCompletions;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.github.runelynx.runicparadise.RunicPlayer#getPlayerLifetimeTokens
	 * ()
	 */
	@Override
	public int getPlayerLifetimeTokens() {
		// refreshPlayerObject(Bukkit.getPlayer(this.playerUUID));
		return this.lifetimeTokens;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.github.runelynx.runicparadise.RunicPlayer#getPlayerSouls ()
	 */
	@Override
	public int getPlayerSouls() {
		// refreshPlayerObject(Bukkit.getPlayer(this.playerUUID));
		return this.soulCount;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.github.runelynx.runicparadise.RunicPlayer#getPlayerHedgeMazeCompletions
	 * ()
	 */
	@Override
	public int getPlayerHedgeMazeCompletions() {
		// refreshPlayerObject(Bukkit.getPlayer(this.playerUUID));
		return this.hedgeMazeCompletions;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.github.runelynx.runicparadise.RunicPlayer#setPlayerLifetimeTokens()
	 */
	@Override
	public boolean setPlayerLifetimeTokens(int increment) {
		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));

		int newBalance = this.getPlayerLifetimeTokens() + increment;

		try {
			// TODO: Change to update DB based on UUID
			final Connection d = MySQL.openConnection();
			Statement dStmt = d.createStatement();
			dStmt.executeUpdate("UPDATE `rp_PlayerInfo` SET LifetimeTokens="
					+ newBalance + " WHERE PlayerName='" + this.playerName
					+ "';");
			d.close();

			// Update object directly since change successful
			this.lifetimeTokens = newBalance;

			return true; // since change was successful
		} catch (SQLException e) {
			getLogger().log(Level.SEVERE,
					"Failed lifetimeTokenUpdate because: " + e.getMessage());
			return false; // since change failed
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.github.runelynx.runicparadise.RunicPlayer#setPlayerMazeCompletions()
	 */
	@Override
	public boolean setPlayerMazeCompletions(String maze, int increment) {
		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));

		String mazeType = "";
		int newBalance = 0;
		if (maze.equals("hedge")) {
			// This must match the DB column name
			mazeType = "HedgeMazeCompletions";
			newBalance = this.getPlayerHedgeMazeCompletions() + increment;
		} else if (maze.equals("ice")) {
			// This must match the DB column name
			mazeType = "IceMazeCompletions";
			newBalance = this.getPlayerIceMazeCompletions() + increment;
		} else {
			// bad format of method call
			getLogger().log(
					Level.SEVERE,
					"Failed mazeCompletionUpdate; dont recognize maze type= "
							+ maze);
			return false;
		}

		try {
			// TODO: Change to update DB based on UUID
			final Connection d = MySQL.openConnection();
			Statement dStmt = d.createStatement();
			dStmt.executeUpdate("UPDATE `rp_PlayerInfo` SET " + mazeType + "="
					+ newBalance + " WHERE PlayerName='" + this.playerName
					+ "';");
			d.close();

			// Update object directly since change successful
			if (maze.equals("Hedge")) {
				// This must match the DB column name
				this.hedgeMazeCompletions = newBalance;
			} else if (maze.equals("Ice")) {
				// This must match the DB column name
				this.iceMazeCompletions = newBalance;
			}
			return true; // since change was successful
		} catch (SQLException e) {
			getLogger().log(Level.SEVERE,
					"Failed mazeCompletionUpdate because: " + e.getMessage());
			return false; // since change failed
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.github.runelynx.runicparadise.RunicPlayer#setPlayerSouls()
	 */
	@Override
	public boolean setPlayerSouls(int newSoulCount) {
		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));

		try {
			// TODO: Change to update DB based on UUID
			final Connection d = MySQL.openConnection();
			Statement dStmt = d.createStatement();
			dStmt.executeUpdate("UPDATE `rp_PlayerInfo` SET SoulCount='"
					+ newSoulCount + "' WHERE PlayerName='" + this.playerName
					+ "';");
			d.close();
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.github.runelynx.runicparadise.RunicPlayer#setPlayerTokenBalance()
	 */
	@Override
	public boolean setPlayerTokenBalance(int newBalance) {
		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));

		int lifetimeBalanceIncrement = newBalance - this.tokenBalance;

		try {
			// TODO: Change to update DB based on UUID
			final Connection d = MySQL.openConnection();
			Statement dStmt = d.createStatement();
			dStmt.executeUpdate("UPDATE `rp_PlayerInfo` SET Tokens='"
					+ newBalance + "' WHERE PlayerName='" + this.playerName
					+ "';");
			d.close();
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
	@Override
	public String translateChatColors() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.github.runelynx.runicparadise.RunicPlayer#getPlayerName()
	 */
	@Override
	public String getPlayerName() {
		return this.playerName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.github.runelynx.runicparadise.RunicPlayer#refreshPlayerObject()
	 */
	@Override
	public void refreshPlayerObject(Player player) {

		this.playerName = player.getName();
		this.playerDisplayName = player.getDisplayName();
		this.isStaff = (player.hasPermission("rp.staff")) ? true : false;
		this.playerUUID = player.getUniqueId();

		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));

		try {
			// TODO: Get data from DB based on UUID to protect vs name changes
			final Connection d = MySQL.openConnection();
			Statement dStmt = d.createStatement();

			ResultSet playerData = dStmt
					.executeQuery("SELECT * FROM `rp_PlayerInfo` WHERE `PlayerName` = '"
							+ this.playerName + "' ORDER BY `id` ASC LIMIT 1;");

			// ResultSet jobData = dStmt2
			// .executeQuery("SELECT job,level FROM `Jobs_jobs` WHERE `player_uuid` = '"+
			// this.playerUUID + "' LIMIT 1;");

			PreparedStatement dStmt2 = d
					.prepareStatement("SELECT job,level FROM Jobs_jobs WHERE player_uuid = UNHEX(?) LIMIT 1;");
			dStmt2.setString(1, this.getPlayerUUID().replace("-", ""));

			getLogger().log(
					Level.INFO,
					"rpjobs debug RPBline 541: " + this.playerName + " - "
							+ this.getPlayerUUID().replace('-', '\u0000'));

			ResultSet jobData = dStmt2.executeQuery();

			// if (!playerData.first() && !playerData.next()) {
			if (jobData.isBeforeFirst()) {
				// Player doesn't exist in the DB!
				// TODO: Need to add them to DB!
				jobData.next();
				this.currentJob = jobData.getString("job");
			} else {
				this.currentJob = "None";
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
				this.tokenBalance = playerData.getInt("Tokens");
				this.soulCount = playerData.getInt("SoulCount");
				this.iceMazeCompletions = playerData
						.getInt("IceMazeCompletions");
				this.hedgeMazeCompletions = playerData
						.getInt("HedgeMazeCompletions");
				this.lifetimeTokens = playerData.getInt("LifetimeTokens");
				this.jobsMasteredCount = playerData.getInt("JobsMasteredCount");
				this.jobsMastered = playerData.getString("JobsMastered");
				this.joinDate = new Date(playerData.getLong("FirstSeen"));
				d.close();
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
	@Override
	public int checkPlayerInventoryItemstackCount() {

		ItemStack[] items = Bukkit.getPlayer(this.playerUUID).getInventory()
				.getContents();

		int playerInvItemCount = 0;

		// Count actual itemstacks in player's inventory
		for (ItemStack item : items) {
			if ((item != null) && (item.getAmount() > 0)
					&& (item.getType() != Material.AIR)) {
				playerInvItemCount++;
			}
		}
		return playerInvItemCount;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.github.runelynx.runicparadise.RunicPlayer#
	 * checkPlayerWearingArmor()
	 */
	@Override
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
	@Override
	public int checkPlayerInventoryForItemDataCount(int id, int dataValue) {
		PlayerInventory inventory = Bukkit.getPlayer(this.playerUUID)
				.getInventory();
		ItemStack[] items = inventory.getContents();
		int has = 0;
		for (ItemStack item : items) {
			if ((item != null) && (item.getTypeId() == id)
					&& (item.getDurability() == dataValue)
					&& (item.getAmount() > 0)) {
				has += item.getAmount();

			}
		}
		return has;
	}

	@Override
	public int removePlayerInventoryItemData(int id, int dataValue) {
		PlayerInventory inventory = Bukkit.getPlayer(this.playerUUID)
				.getInventory();
		int cleared = inventory.clear(id, dataValue);
		return cleared;

	}

	@Override
	public void givePlayerItemData(int count, int id, int dataValue,
			int loreCount, String displayName, String lore1, String lore2,
			String lore3) {
		ItemStack newItem = new ItemStack(id, count, (short) dataValue);
		ItemMeta meta = newItem.getItemMeta();
		meta.setDisplayName(displayName);
		if (loreCount == 1) {
			meta.setLore(Arrays.asList(lore1));
		} else if (loreCount == 2) {
			meta.setLore(Arrays.asList(lore1, lore2));
		} else if (loreCount >= 3) {
			meta.setLore(Arrays.asList(lore1, lore2, lore3));
		}

		newItem.setItemMeta(meta);
		PlayerInventory inventory = Bukkit.getPlayer(this.playerUUID)
				.getInventory();
		inventory.addItem(newItem);
	}

	@Override
	public void givePlayerItemStack(ItemStack[] items) {

		PlayerInventory inventory = Bukkit.getPlayer(this.playerUUID)
				.getInventory();

		for (ItemStack item : items) {
			if (item != null) {
				inventory.addItem(item);
			}

		}
		new BukkitRunnable() {
			@Override
			public void run() {
				for (Player p : Bukkit.getOnlinePlayers())
					p.updateInventory();
			}
		}.runTaskLater(instance, 0);

	}

}
