package io.github.runelynx.runicparadise;

import com.connorlinfoot.titleapi.TitleAPI;
import io.github.runelynx.runicuniverse.RunicMessaging;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.sql.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.Date;
import java.util.logging.Level;

import static org.bukkit.Bukkit.getLogger;

public class RunicProfile {
	final static int FARMING_COUNT_THRESHOLD = 7;

	int karmaBalance;
	int currentIP;
	int voteTotal;
	String masteredJobsString;
	int masteredJobCount;
	String currentJob;
	int currentJobLevel;
	Date joinDate;
	String playerName;
	String playerDisplayName;
	UUID playerUUID;
	double playerExpLevel;
	int gravesCreated;
	int gravesStolen;
	int gravesActive;
	int tokenBalance;
	int lifetimeTokens;
	int soulCount;
	char gender;
	String chatColor;
	OfflinePlayer op;
	int faithPowerLevel;
	String playerIP;
	String activeFaith;
	int skyblockRankNum;
	String skyblockRank;
	int explorerLocsFound;
	int mazesAndParkoursCompletedFirstTime;
	int rankDropCountLast24Hours;
	HashMap<EntityType, Integer> mobKillCountsMap = new HashMap<EntityType, Integer>();

	Location farmKillLoc;
	int farmKillCounter;
	Boolean isFarming;

	private static Plugin instance = RunicParadise.getInstance();

	RunicProfile(UUID playerID) {
		loadProfile(playerID);
	}

	private void setPlayerID(UUID playerid) {
		this.playerUUID = playerid;
	}

	private UUID getPlayerID() {
		return this.playerUUID;
	}

	void setChatColor(ChatColor newSetting, boolean updateDB) {
		this.chatColor = newSetting.toString();
		if (updateDB) {
			MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);

			try {
				Connection connection = MySQL.openConnection();

				PreparedStatement statement = connection.prepareStatement("UPDATE rp_PlayerInfo SET ChatColor ='" + newSetting.name() + "' WHERE UUID = ?");
				statement.setString(1, this.getPlayerID().toString());
				statement.executeUpdate();

				connection.close();
			} catch (SQLException e) {
				getLogger().log(Level.SEVERE, "Failed setChatColor because: " + e.getMessage());
			}
		}
	}

	void setChatColor(String newSetting, boolean updateDB) {
		ChatColor color;
		try {
			color = ChatColor.valueOf(newSetting);
		} catch (IllegalArgumentException e) {
			Optional<Map.Entry<String, ChatColor>> result  = RunicParadise.rankColors.entrySet().stream().filter(x -> x.getKey().equalsIgnoreCase(newSetting)).findFirst();
			if (result.isPresent()) {
				color = result.get().getValue();
			} else {
				return;
			}
		}
		setChatColor(color, updateDB);
	}

	public String getChatColor() {
		return this.chatColor;
	}

	public void changeGender(String T_toggle_M_male_F_female, boolean updateDB) {
		char newGender = 'M';

		char entry = T_toggle_M_male_F_female.charAt(0);

		if (entry == 'T') {
			if (this.gender == 'M') {
				newGender = 'F';
			} else {
				newGender = 'M';
			}
		} else {
			newGender = entry;
		}

		this.gender = newGender;

		if (updateDB) {

			MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);

			try {
				Connection connection = MySQL.openConnection();

				PreparedStatement statement = connection.prepareStatement("UPDATE rp_PlayerInfo SET Gender ='" + newGender + "' WHERE UUID = ?");
				statement.setString(1, this.getPlayerID().toString());
				statement.executeUpdate();

				connection.close();
			} catch (SQLException e) {
				getLogger().log(Level.SEVERE, "Failed setGender because: " + e.getMessage());
			}
		}
	}

	public char getGender() {
		return this.gender;
	}

	private OfflinePlayer getOfflinePlayer() {
		return this.op;
	}

	private void setOfflinePlayer() {
		this.op = Bukkit.getOfflinePlayer(this.getPlayerID());
	}

	// if player is online, set both display and real names.
	private void setPlayerNames(boolean online) {

		if (online) {
			this.playerName = Bukkit.getOfflinePlayer(this.getPlayerID()).getName();
			this.playerDisplayName = Bukkit.getPlayer(this.getPlayerID()).getDisplayName();
		} else {
			this.playerName = Bukkit.getOfflinePlayer(this.getPlayerID()).getName();
			this.playerDisplayName = Bukkit.getPlayer(this.getPlayerID()).getName();
		}

	}

	private String getPlayerName(boolean getRealName) {
		// this will only return the true display name if player is online.
		// Controlled via setPlayerNames method.
		if (getRealName) {
			return this.playerName;
		} else {
			return this.playerDisplayName;
		}
	}

	private void setFaithPowerLevel(int fpl) {
		this.faithPowerLevel = fpl;
	}

	private int getFaithPowerLevel() {
		return this.faithPowerLevel;
	}

	public void grantCurrency(String type, int amount) {
		boolean error = false;
		String column = "";
		String plural = "";

		if (amount > 1) {
			plural = "s";
		}

		switch (type) {

		case "Souls":
			column = "SoulCount";
			this.setSoulCount(this.getSoulCount() + amount);

			RunicMessaging.sendMessage(Bukkit.getPlayer(this.getPlayerID()), RunicMessaging.RunicFormat.AFTERLIFE,
					"Gained " + amount + " soul" + plural);

			break;
		case "Karma":
			column = "Karma";
			this.setKarmaBalance(this.getKarmaBalance() + amount);

			RunicMessaging.sendMessage(Bukkit.getPlayer(this.getPlayerID()), RunicMessaging.RunicFormat.FAITH,
					"Gained " + amount + " karma");

			break;
		case "Tokens":
			column = "Tokens";
			this.setTokenBalance(this.getTokenBalance() + amount);
			this.setLifetimeToken(this.getLifetimeToken() + amount);

			RunicMessaging.sendMessage(Bukkit.getPlayer(this.getPlayerID()), RunicMessaging.RunicFormat.CASINO,
					"Gained " + amount + " token" + plural);

			break;
		default:
			error = true;
			break;
		}

		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);

		try {
			Connection connection = MySQL.openConnection();
			Statement statement = connection.createStatement();
			statement.executeUpdate("UPDATE `rp_PlayerInfo` SET " + column + " = " + column + " + " + amount
					+ " WHERE UUID= '" + this.getPlayerID() + "';");

			if (column.equals("Tokens")) {
				statement.executeUpdate("UPDATE `rp_PlayerInfo` SET LifetimeTokens = LifetimeTokens + " + amount
						+ " WHERE UUID= '" + this.getPlayerID() + "';");
			}

			connection.close();

		} catch (SQLException e) {
			getLogger().log(Level.SEVERE,
					"Failed grantCurrency update (change " + this.playerName + "  because: " + e.getMessage());
		}

		// Sync up old profile method ... for now ...
		new RunicPlayerBukkit(this.getPlayerID()).refreshPlayerObject(Bukkit.getOfflinePlayer(this.getPlayerID()));
	}

	private void setKarmaBalance(int newKarma) {
		this.karmaBalance = newKarma;
	}

	public int getSkyblockRankNum() {
		return this.skyblockRankNum;
	}

	public String getSkyblockRank() {
		return this.skyblockRank;
	}

	private void setSkyblockRankNum(int newNum) {
		this.skyblockRankNum = newNum;
	}

	private void setSkyblockRank(String newRank) {
		this.skyblockRank = newRank;
	}

	private int getKarmaBalance() {
		return this.karmaBalance;
	}

	private void setTokenBalance(int newToken) {
		this.tokenBalance = newToken;
	}

	private int getTokenBalance() {
		return this.tokenBalance;
	}

	private void setLifetimeToken(int lifetimeTokens) {
		this.lifetimeTokens = lifetimeTokens;
	}

	private int getLifetimeToken() {
		return this.lifetimeTokens;
	}

	private void setJobMasteryCount(int jobMasteryCount) {
		this.masteredJobCount = jobMasteryCount;
	}

	private int getJobMasteryCount() {
		return this.masteredJobCount;
	}

	private void setJobMasteryString(String jobMasteryString) {
		this.masteredJobsString = jobMasteryString;
	}

	private String getJobMasteryString() {
		return this.masteredJobsString;
	}

	private void setSoulCount(int newSouls) {
		this.soulCount = newSouls;
	}

	private int getSoulCount() {
		return this.soulCount;
	}

	private void setVoteCount(int newVotes) {
		this.voteTotal = newVotes;
	}

	private int getVoteCount() {
		return this.voteTotal;
	}

	private void setJoinDate(Date newJoinDate) {
		this.joinDate = newJoinDate;
	}

	private Date getJoinDate() {
		return this.joinDate;
	}

	private void setActiveFaith(String newFaith) {
		this.activeFaith = newFaith;
	}

	private String getActiveFaith() {
		return this.activeFaith;
	}

	private int getSpecialRankDrop24HrCount() {
		return this.rankDropCountLast24Hours;
	}

	private void setSpecialRankDrop24HrCount(int newValue) {
		this.rankDropCountLast24Hours = newValue;
	}

	public void incrementSpecialRankDrop24HrCount() {
		this.rankDropCountLast24Hours = this.getSpecialRankDrop24HrCount() + 1;
	}

	public void logSpecialRankDrop(String item, String source) {
		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);

		try {
			Connection connection = MySQL.openConnection();

			PreparedStatement statement = connection.prepareStatement(
					"INSERT INTO rp_SpecialDrops (PlayerName, UUID, Timestamp, DropType, Source) VALUES " + "('"
							+ this.getPlayerName(true) + "', '" + this.getPlayerID().toString() + "', "
							+ (new Date().getTime()) + ", '" + item + "', '" + source + "');");
			statement.executeUpdate();
			connection.close();
		} catch (SQLException e) {
			Bukkit.getLogger().log(Level.SEVERE, "Failed logSpecialRankDrop " + e.getMessage());
		}
	}

	private void setPlayerIP(String playerIP) {
		this.playerIP = playerIP;
	}

	private String getPlayerIP() {
		return this.playerIP;
	}

	public int getCountGraves(String query) {
		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);

		String sqlQuery = "";

		switch (query) {
		case "Created":
			sqlQuery = "SELECT COUNT(*) FROM rp_PlayerGraves WHERE PlayerName = '" + this.getPlayerName(true)
					+ "' OR UUID = '" + this.getPlayerID().toString() + "'";
			break;
		case "StolenFromYou":
			sqlQuery = "SELECT COUNT(*) FROM rp_PlayerGraves WHERE (PlayerName = '" + this.getPlayerName(true)
					+ "' OR UUID = '" + this.getPlayerID().toString() + "') AND Status = 'Gone' AND LooterUUID != '"
					+ this.getPlayerID().toString() + "'";
			break;
		case "StolenByYou":
			sqlQuery = "SELECT COUNT(*) FROM rp_PlayerGraves WHERE (PlayerName != '" + this.getPlayerName(true)
					+ "' AND UUID != '" + this.getPlayerID().toString() + "') AND Status = 'Gone' AND LooterUUID = '"
					+ this.getPlayerID().toString() + "'";
			break;
		case "Opened":
			sqlQuery = "SELECT COUNT(*) FROM rp_PlayerGraves WHERE (PlayerName = '" + this.getPlayerName(true)
					+ "' OR UUID = '" + this.getPlayerID().toString() + "') AND Status = 'Gone' AND LooterUUID = '"
					+ this.getPlayerID().toString() + "'";
			break;
		case "Unopened":
			sqlQuery = "SELECT COUNT(*) FROM rp_PlayerGraves WHERE (PlayerName = '" + this.getPlayerName(true)
					+ "' OR UUID = '" + this.getPlayerID().toString() + "') AND Status != 'Gone'";
			break;
		default:
			return 0;

		}

		try {
			Connection connection = MySQL.openConnection();

			PreparedStatement statement = connection.prepareStatement(sqlQuery);
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
			getLogger().log(Level.SEVERE, "Failed getCountGraves because: " + e.getMessage());
			return 0;
		}
	}

	private void loadProfile(UUID playerid) {

		this.setPlayerID(playerid);

		if (Bukkit.getOfflinePlayer(this.getPlayerID()).isOnline()) {
			this.setPlayerNames(true);
		} else {
			this.setPlayerNames(false);
		}

		retrieveBasicData();
		retrieveJobsData();
		retrieveMobKillsData();
		retrieveDropData();

		getTotalExplorerCompletions();
		getMazeAndParkoursCompletedFirstTime();

		// Add a dummy entry for all players to prevent NPEs down the line
		HashMap<EntityType, Integer> tempSecondaryMap = new HashMap<EntityType, Integer>();
		tempSecondaryMap.put(EntityType.BAT, 0);
		RunicParadise.mobKillTracker.put(this.getPlayerID(), tempSecondaryMap);

		setOfflinePlayer();
	}

	private void restartAntiFarming() {
		this.farmKillLoc = Bukkit.getPlayer(this.getPlayerID()).getLocation();
		this.farmKillCounter = 0;
	}

	public Boolean checkFarming(Location loc) {

		if (this.farmKillLoc == null || !this.farmKillLoc.getWorld().equals(loc.getWorld())) {
			restartAntiFarming();
		}

		if (loc.distance(this.farmKillLoc) < 3.0) {
			// Player hasn't moved much
			this.farmKillCounter++;

		} else {
			// Player has moved enough
			this.farmKillCounter = 0;
			this.farmKillLoc = loc;
		}

		if (this.farmKillCounter >= FARMING_COUNT_THRESHOLD) {
			// Player is farming
			this.isFarming = true;
			return true;
		} else {
			// Player is not farming
			this.isFarming = false;
			return false;
		}
	}

	boolean isPlayerFarming() {
		return this.farmKillCounter >= FARMING_COUNT_THRESHOLD;
	}

	private void retrieveJobsData() {
		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);

		try {
			// TODO: Get data from DB based on UUID to protect vs name changes
			Connection connection = MySQL.openConnection();
			Statement dStmt = connection.createStatement();

			int jobsID = 0;
			ResultSet jobPlayerIDResult = dStmt.executeQuery("SELECT ID FROM `Jobs_users` WHERE `player_uuid` = '"
					+ this.getPlayerID().toString() + "' ORDER BY `id` ASC LIMIT 1;");
			if (jobPlayerIDResult.isBeforeFirst()) {
				jobPlayerIDResult.next();
				jobsID = jobPlayerIDResult.getInt("id");
			}

			PreparedStatement dStmt2 = connection
					.prepareStatement("SELECT job,level FROM Jobs_jobs WHERE userid = " + jobsID + " LIMIT 1;");

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
		} catch (SQLException e) {
			getLogger().log(Level.SEVERE, "Failed DB check [refreshPlayerObject] because: " + e.getMessage());
		}
	}

	private void retrieveBasicData() {
		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);

		try {
			// TODO: Get data from DB based on UUID to protect vs name changes
			Connection connection = MySQL.openConnection();
			Statement statement = connection.createStatement();

			ResultSet playerData = statement.executeQuery("SELECT * FROM `rp_PlayerInfo` WHERE `UUID` = '"
					+ this.getPlayerID().toString() + "' ORDER BY `id` ASC LIMIT 1;");

			PreparedStatement dStmt3 = connection
					.prepareStatement("SELECT SUM(Level) AS FPL FROM rp_PlayerFaiths WHERE UUID = ?;");
			dStmt3.setString(1, this.getPlayerID().toString());

			ResultSet faithData = dStmt3.executeQuery();

			if (faithData.isBeforeFirst()) {
				faithData.next();
				this.setFaithPowerLevel(faithData.getInt("FPL"));
			} else {
				this.setFaithPowerLevel(0);
			}

			if (!playerData.isBeforeFirst()) {
				// Player doesn't exist in the DB!
				// TODO: Need to add them to DB!
				this.tokenBalance = 0;
				getLogger().log(Level.INFO, "[RP] Player " + this.playerName + " isn't in our DB yet.");
			} else {
				// Player does exist in the DB
				playerData.next();
				this.setPlayerIP(playerData.getString("LastIP"));
				this.setKarmaBalance(playerData.getInt("Karma"));
				this.setTokenBalance(playerData.getInt("Tokens"));
				this.setSoulCount(playerData.getInt("SoulCount"));
				this.setLifetimeToken(playerData.getInt("LifetimeTokens"));
				this.setJobMasteryString(playerData.getString("JobsMastered"));
				this.setJobMasteryCount(playerData.getInt("JobsMasteredCount"));
				this.setJoinDate(new Date(playerData.getLong("FirstSeen")));
				this.setVoteCount(playerData.getInt("Votes"));
				this.setActiveFaith(playerData.getString("ActiveFaith"));
				this.changeGender(playerData.getString("Gender"), false);
				this.setSkyblockRank(playerData.getString("SkyblockRankText"));
				this.setSkyblockRankNum(playerData.getInt("SkyblockRank"));

				if (Bukkit.getPlayer(this.getPlayerID()).hasPermission("rp.chatcolors.options")) {
					// Player is in a rank with permissions to choose color, so
					// get color from DB
					this.setChatColor(playerData.getString("ChatColor"), false);
				} else {
					// Player is in a rank WITHOUT choice of color, so just give
					// them their basic rank color
					this.setChatColor(RunicParadise.perms.getPrimaryGroup(Bukkit.getPlayer(this.getPlayerID())), false);
				}

				connection.close();
			}
		} catch (SQLException e) {
			getLogger().log(Level.SEVERE, "Failed DB check [refreshPlayerObject] because: " + e.getMessage());
		}

	}

	private void retrieveDropData() {
		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);

		try {
			// TODO: Get data from DB based on UUID to protect vs name changes
			Connection connection = MySQL.openConnection();
			Statement statement = connection.createStatement();

			Long oneDayAgo = new Date().getTime() - 86400000;

			ResultSet playerData = statement.executeQuery(
					"SELECT COUNT(ID) AS Count FROM `rp_SpecialDrops` WHERE `UUID` = '" + this.getPlayerID().toString()
							+ "' AND `DropType` LIKE '%Gem' AND `Timestamp` > " + oneDayAgo + ";");

			if (!playerData.isBeforeFirst()) {
				// Player doesn't exist in the DB!
				// TODO: Need to add them to DB!

				this.setSpecialRankDrop24HrCount(0);

			} else {
				// Player does exist in the DB
				playerData.next();
				this.setSpecialRankDrop24HrCount(playerData.getInt("Count"));

				connection.close();
			}
		} catch (SQLException e) {
			getLogger().log(Level.SEVERE, "Failed DB check [retrieveDropData] because: " + e.getMessage());
		}
	}

	private void retrieveMobKillsData() {
		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);

		try {
			Connection connection = MySQL.openConnection();

			PreparedStatement statement = connection.prepareStatement("SELECT * FROM rp_PlayerMobKills WHERE UUID = ?;");
			statement.setString(1, this.getPlayerID().toString());
			ResultSet playerData = statement.executeQuery();

			while (playerData.next()) {
				// A fresh start
				this.mobKillCountsMap.clear();

				// Load data!
				this.mobKillCountsMap.put(EntityType.BLAZE, playerData.getInt("BLAZE"));
				this.mobKillCountsMap.put(EntityType.CAVE_SPIDER, playerData.getInt("CAVE_SPIDER"));
				this.mobKillCountsMap.put(EntityType.CREEPER, playerData.getInt("CREEPER"));
				this.mobKillCountsMap.put(EntityType.ELDER_GUARDIAN, playerData.getInt("ELDER_GUARDIAN"));
				this.mobKillCountsMap.put(EntityType.ENDER_DRAGON, playerData.getInt("ENDER_DRAGON"));
				this.mobKillCountsMap.put(EntityType.ENDERMAN, playerData.getInt("ENDERMAN"));
				this.mobKillCountsMap.put(EntityType.ENDERMITE, playerData.getInt("ENDERMITE"));
				this.mobKillCountsMap.put(EntityType.EVOKER, playerData.getInt("EVOKER"));
				this.mobKillCountsMap.put(EntityType.GHAST, playerData.getInt("GHAST"));
				this.mobKillCountsMap.put(EntityType.GIANT, playerData.getInt("GIANT"));
				this.mobKillCountsMap.put(EntityType.GUARDIAN, playerData.getInt("GUARDIAN"));
				this.mobKillCountsMap.put(EntityType.MAGMA_CUBE, playerData.getInt("MAGMA_CUBE"));
				this.mobKillCountsMap.put(EntityType.PIG_ZOMBIE, playerData.getInt("PIG_ZOMBIE"));
				this.mobKillCountsMap.put(EntityType.SHULKER, playerData.getInt("SHULKER"));
				this.mobKillCountsMap.put(EntityType.SILVERFISH, playerData.getInt("SILVERFISH"));
				this.mobKillCountsMap.put(EntityType.SLIME, playerData.getInt("SLIME"));
				this.mobKillCountsMap.put(EntityType.SPIDER, playerData.getInt("SPIDER"));
				this.mobKillCountsMap.put(EntityType.VEX, playerData.getInt("VEX"));
				this.mobKillCountsMap.put(EntityType.VINDICATOR, playerData.getInt("VINDICATOR"));
				this.mobKillCountsMap.put(EntityType.WITCH, playerData.getInt("WITCH"));
				this.mobKillCountsMap.put(EntityType.WITHER, playerData.getInt("WITHER"));

				this.mobKillCountsMap.put(EntityType.BAT, playerData.getInt("BAT"));
				this.mobKillCountsMap.put(EntityType.CHICKEN, playerData.getInt("CHICKEN"));
				this.mobKillCountsMap.put(EntityType.COW, playerData.getInt("COW"));
				this.mobKillCountsMap.put(EntityType.IRON_GOLEM, playerData.getInt("IRON_GOLEM"));
				this.mobKillCountsMap.put(EntityType.LLAMA, playerData.getInt("LLAMA"));
				this.mobKillCountsMap.put(EntityType.MULE, playerData.getInt("MULE"));
				this.mobKillCountsMap.put(EntityType.MUSHROOM_COW, playerData.getInt("MUSHROOM_COW"));
				this.mobKillCountsMap.put(EntityType.OCELOT, playerData.getInt("OCELOT"));
				this.mobKillCountsMap.put(EntityType.PIG, playerData.getInt("PIG"));
				this.mobKillCountsMap.put(EntityType.POLAR_BEAR, playerData.getInt("POLAR_BEAR"));
				this.mobKillCountsMap.put(EntityType.RABBIT, playerData.getInt("RABBIT"));
				this.mobKillCountsMap.put(EntityType.SHEEP, playerData.getInt("SHEEP"));
				this.mobKillCountsMap.put(EntityType.SNOWMAN, playerData.getInt("SNOWMAN"));
				this.mobKillCountsMap.put(EntityType.SQUID, playerData.getInt("SQUID"));
				this.mobKillCountsMap.put(EntityType.VILLAGER, playerData.getInt("VILLAGER"));
				this.mobKillCountsMap.put(EntityType.WOLF, playerData.getInt("WOLF"));

				this.mobKillCountsMap.put(EntityType.DONKEY, playerData.getInt("DONKEY"));
				this.mobKillCountsMap.put(EntityType.HORSE, playerData.getInt("HORSE"));
				this.mobKillCountsMap.put(EntityType.SKELETON_HORSE, playerData.getInt("SKELETON_HORSE"));
				this.mobKillCountsMap.put(EntityType.ZOMBIE_HORSE, playerData.getInt("ZOMBIE_HORSE"));

				this.mobKillCountsMap.put(EntityType.HUSK, playerData.getInt("HUSK"));
				this.mobKillCountsMap.put(EntityType.ZOMBIE, playerData.getInt("ZOMBIE"));
				this.mobKillCountsMap.put(EntityType.ZOMBIE_VILLAGER, playerData.getInt("ZOMBIE_VILLAGER")); // 3

				this.mobKillCountsMap.put(EntityType.SKELETON, playerData.getInt("SKELETON")); // 3
				this.mobKillCountsMap.put(EntityType.STRAY, playerData.getInt("STRAY"));
				this.mobKillCountsMap.put(EntityType.WITHER_SKELETON, playerData.getInt("WITHER_SKELETON"));
			}

		} catch (SQLException e) {
			getLogger().log(Level.SEVERE, "Failed getPlayerKillCount because: " + e.getMessage());
		}
	}

	private boolean getMazeAndParkoursCompletedFirstTime() {
		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
		Connection connection = MySQL.openConnection();
		try {
			Statement statement = connection.createStatement();
			ResultSet explorerLocData = statement
					.executeQuery("SELECT COUNT(UUID) AS Count FROM rp_RunicGameCompletions WHERE UUID='"
							+ this.getPlayerID().toString() + "';");

			if (!explorerLocData.isBeforeFirst()) {
				// No results
				// player hasnt completed this exploration yet
				return false;

			} else {
				// results found!
				while (explorerLocData.next()) {
					if (explorerLocData.getInt("Count") > 0) {

						this.mazesAndParkoursCompletedFirstTime = explorerLocData.getInt("Count");
						connection.close();
						statement.close();
						return true;
					} else {
						this.mazesAndParkoursCompletedFirstTime = explorerLocData.getInt("Count");
						connection.close();
						statement.close();
						return false;
					}
				}
			}

		} catch (SQLException err) {
			Bukkit.getLogger().log(Level.SEVERE,
					"Error checking getMazeAndParkoursCompletedFirstTime because: " + err.getMessage());
			this.explorerLocsFound = 0;
		}
		return false;
	}

	public void incrementGamesCompletion() {
		// this is to temporarily raise the memory variable only so player can
		// enjoy the benefit right away.
		// On relogging, the new player profile will pull the real number from
		// the DB

		this.mazesAndParkoursCompletedFirstTime = this.mazesAndParkoursCompletedFirstTime + 1;
	}

	public void incrementExplorerCompletion() {
		// this is to temporarily raise the memory variable only so player can
		// enjoy the benefit right away.
		// On relogging, the new player profile will pull the real number from
		// the DB

		this.explorerLocsFound = this.explorerLocsFound + 1;
	}

	public void getTotalExplorerCompletions() {
		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
		Connection connection = MySQL.openConnection();
		try {
			Statement statement = connection.createStatement();
			ResultSet explorerLocData = statement
					.executeQuery("SELECT COUNT(ID) AS Count FROM rp_ExplorerCompletions WHERE UUID='"
							+ this.getPlayerID().toString() + "';");

			if (!explorerLocData.isBeforeFirst()) {
				// No results
				// player hasnt completed this exploration yet

				this.explorerLocsFound = 0;
			} else {
				// results found!
				while (explorerLocData.next()) {
					if (explorerLocData.getInt("Count") > 0) {

						this.explorerLocsFound = explorerLocData.getInt("Count");

					} else {
						this.explorerLocsFound = 0;

					}
				}
			}

			connection.close();
			statement.close();

		} catch (SQLException err) {
			Bukkit.getLogger().log(Level.SEVERE,
					"Error checking getTotalExplorerCompletions because: " + err.getMessage());
			this.explorerLocsFound = 0;
		}
	}

	public void addMazeCompletion(int puzzleID) {

		Player p = Bukkit.getPlayer(this.getPlayerID());

		try {
			MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
			Connection connection = MySQL.openConnection();

			// Get player puzzle results
			PreparedStatement statement = connection
					.prepareStatement("SELECT * FROM rp_RunicGameCompletions WHERE UUID = ? AND GameID = ?;");

			statement.setString(1, p.getUniqueId().toString());
			statement.setInt(2, puzzleID);
			ResultSet mcResult = statement.executeQuery();

			// Get puzzle data (for prizes) - and validate the puzzleID is ok
			PreparedStatement mzStmt = connection.prepareStatement("SELECT * FROM rp_RunicGames WHERE ID = ?;");

			mzStmt.setInt(1, puzzleID);
			ResultSet mzResult = mzStmt.executeQuery();

			PreparedStatement updStmt;

			double prizeAdjust = 1;

			int prizeKarma = 0;
			int prizeSouls = 0;
			int prizeRunics = 0;
			int prizeTokens = 0;

			if (!mzResult.isBeforeFirst()) {
				// No results, invalid puzzleID given

				p.sendMessage(ChatColor.GRAY
						+ "This maze has been configured incorrectly. Ask an admin to check the game ID# in the command block");
				connection.close();
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
				updStmt = connection
						.prepareStatement("INSERT INTO rp_RunicGameCompletions (UUID, GameID, Count, LastCompletion) "
								+ "VALUES (?, ?, ?, ?);");
				updStmt.setString(1, p.getUniqueId().toString());
				updStmt.setInt(2, puzzleID);
				updStmt.setInt(3, 1);
				updStmt.setLong(4, new Date().getTime());
				updStmt.executeUpdate();

				RunicPlayerBukkit target = new RunicPlayerBukkit(p.getUniqueId());

				if (prizeKarma > 0) {
					new RunicPlayerBukkit(p.getUniqueId()).adjustPlayerKarma((int) (prizeKarma * 1));
				}
				if (prizeTokens > 0) {
					target.setPlayerTokenBalance(target.getPlayerTokenBalance() + (int) (prizeTokens * 1));
				}
				if (prizeSouls > 0) {
					target.setPlayerSouls(target.getPlayerSouls() + (int) (prizeSouls * 1));
					p.sendMessage(ChatColor.GREEN + "You gained " + prizeSouls + " souls!");
				}
				if (prizeRunics > 0) {
					RunicParadise.economy.depositPlayer(p, (int) (prizeRunics * 1));
					p.sendMessage(ChatColor.GREEN + "You gained " + (int) (prizeRunics * 1) + " runics!");
				}
				if (mzResult.getInt("ID") == 6) {
					// Dungeon Maze
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "give " + p.getName() + " beacon 1");
					p.sendMessage(ChatColor.GOLD + "You received a beacon for first completion here!");
				}
				if (mzResult.getInt("ID") == 7) {
					// Adventure Parkour
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "give " + p.getName() + " witherskull 1");
					p.sendMessage(ChatColor.GOLD + "You received a wither skull for first completion here!");
				}
				if (mzResult.getInt("ID") == 7) {
					// Adventure Parkour
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "give " + p.getName() + " witherskull 1");
					p.sendMessage(ChatColor.GOLD + "You received a wither skull for first completion here!");
				}
				if (mzResult.getInt("ID") == 23) {
					// Cake Maze
					p.getInventory().addItem(Recipes.customItemStacks("CAKE_MAZE_1"));
					p.sendMessage(ChatColor.GOLD + "You received a cake trophy for first completion here!");

					if (LocalDate.now().isBefore(LocalDate.of(2018, 11, 23))) {
						p.getInventory().addItem(Recipes.customItemStacks("CAKE_MAZE_2"));
						p.sendMessage(ChatColor.GOLD + "You received a Thanksgiving pie trophy for first completion here before Thanksgiving 2018!");
					}
				}
				
				if (mzResult.getInt("ID") == 21) {
					// Anguish Maze

					ItemStack[] rewards = Commands
							.carnivalChestReward(new Location(Bukkit.getWorld("RunicSky"), 1098, 121, 1166));

					for (ItemStack i : rewards) {
						if ( i != null && i.getType() != null && i.getType() != Material.AIR) {

							p.getWorld().dropItemNaturally(p.getLocation(), i);
						}
					}

					p.sendMessage(ChatColor.GOLD
							+ "Congratulations! You've earned a special reward for this first-time completion!");
					for (Player q : Bukkit.getOnlinePlayers()) {
						Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "cmi titlemsg all "+ ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + p.getName() +" %subtitle% "+ ChatColor.GRAY
								+ "just completed the " + mzResult.getString("GameName")	+ " for the first time!");

					}
				} else 	if (mzResult.getInt("ID") == 22) {
					// Heart of Anguish
					p.sendMessage(ChatColor.GOLD
							+ "Congratulations! You've earned a special reward for this first-time completion!");
					ItemStack[] rewards = Commands
							.carnivalChestReward(new Location(Bukkit.getWorld("RunicSky"), 1098, 121, 1162));

					for (ItemStack i : rewards) {
						if ( i != null && i.getType() != null && i.getType() != Material.AIR) {

							p.getWorld().dropItemNaturally(p.getLocation(), i);
						}
					}

					for (Player q : Bukkit.getOnlinePlayers()) {
						Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "cmi titlemsg all "+ ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + p.getName() +" %subtitle% "+ ChatColor.GRAY + "has freed the Souls of Anguish!");
					}
				} else {

					for (Player q : Bukkit.getOnlinePlayers()) {
						Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "cmi titlemsg all "+ ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + p.getName() +" %subtitle% "+ ChatColor.GRAY
								+ "just completed the " + mzResult.getString("GameName")	+ " for the first time!");
					}
				}

				statement.close();
				connection.close();

			} else {
				// results found!
				// not player's first completion!
				mcResult.next();
				updStmt = connection
						.prepareStatement("UPDATE rp_RunicGameCompletions SET Count = ?, LastCompletion = ? WHERE "
								+ "UUID = ? AND GameID = ?;");
				updStmt.setInt(1, RunicParadise.getPlayerMazeCompletionCount(p, puzzleID) + 1);
				updStmt.setLong(2, new Date().getTime());
				updStmt.setString(3, p.getUniqueId().toString());
				updStmt.setInt(4, puzzleID);
				updStmt.executeUpdate();

				if (mzResult.getInt("ID") == 21) {
					prizeAdjust = 0.10;
				} else if (mzResult.getInt("ID") == 22) {
					prizeAdjust = 0.15;
				} else {
					prizeAdjust = 0.5;
				}

				if (new Date().getTime() - mcResult.getLong("LastCompletion") > RunicParadise.PUZZLE_REPEAT_TIME) {

					RunicPlayerBukkit target = new RunicPlayerBukkit(p.getUniqueId());

					if (prizeKarma > 0) {
						new RunicPlayerBukkit(p.getUniqueId()).adjustPlayerKarma((int) (prizeKarma * prizeAdjust));
					}
					if (prizeTokens > 0) {
						target.setPlayerTokenBalance(
								target.getPlayerTokenBalance() + (int) (prizeTokens * prizeAdjust));
					}
					if (prizeSouls > 0) {
						target.setPlayerSouls(target.getPlayerSouls() + (int) (prizeSouls * prizeAdjust));
						p.sendMessage(ChatColor.GREEN + "You gained " + ((int) (prizeSouls * prizeAdjust)) + " souls!");
					}
					if (prizeRunics > 0) {
						RunicParadise.economy.depositPlayer(p, (int) (prizeRunics * prizeAdjust));
						p.sendMessage(
								ChatColor.GREEN + "You gained " + ((int) (prizeRunics * prizeAdjust)) + " runics!");
					}
				} else {
					// The required time hasnt passed yet to receive a reward
					// again ...
					p.sendMessage(ChatColor.RED
							+ "Congrats!! You received a reward for this puzzle less than a week ago. Check /games to see when you can claim this reward again!");
				}

			}

			statement.close();

			connection.close();
		} catch (SQLException z) {
			Bukkit.getLogger().log(Level.SEVERE, "Failed adding maze completion " + z.getMessage());
		}
	}

	public Boolean checkPlayerExploration(int locID) {
		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
		Connection connection = MySQL.openConnection();
		try {
			Statement statement = connection.createStatement();
			ResultSet explorerLocData = statement
					.executeQuery("SELECT COUNT(ID) AS Count FROM rp_ExplorerCompletions WHERE UUID='"
							+ this.getPlayerID().toString() + "' AND LocID= " + locID + ";");

			if (!explorerLocData.isBeforeFirst()) {
				// No results
				// player hasnt completed this exploration yet
				return false;

			} else {
				// results found!
				while (explorerLocData.next()) {
					if (explorerLocData.getInt("Count") > 0) {
						connection.close();
						statement.close();
						return true;
					} else {
						connection.close();
						statement.close();
						return false;
					}
				}
			}

		} catch (SQLException err) {
			Bukkit.getLogger().log(Level.SEVERE, "Error checking checkPlayerExploration because: " + err.getMessage());
		}
		return false;
	}

	boolean completePlayerExploration(int locID) {
		Player p = Bukkit.getPlayer(this.getPlayerID());

		if (locID <= 0) {
			RunicMessaging.sendMessage(Bukkit.getPlayer(this.getPlayerID()), RunicMessaging.RunicFormat.EXPLORER,
					"Something went wrong. Invalid location ID used.");
			return false;
		}

		if (this.checkPlayerExploration(locID)) {
			// player already completed!
			RunicMessaging.sendMessage(p, RunicMessaging.RunicFormat.EXPLORER, "You already found this location!");
			return false;
		}

		if (RunicParadise.explorerPrereqs.get(locID) != 0) {
			// loc has a prereq!!
			if (!this.checkPlayerExploration(RunicParadise.explorerPrereqs.get(locID))) {
				// player has not completed prereq
				RunicMessaging.sendMessage(p, RunicMessaging.RunicFormat.EXPLORER,
						"You cannot receive the reward for this location because you have not found "
								+ RunicParadise.explorerIDs.get(RunicParadise.explorerPrereqs.get(locID)) + " yet.");
				return false;
			}
		}

		Plugin instance = RunicParadise.getInstance();
		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
		Connection connection = MySQL.openConnection();
		try {
			Statement statement = connection.createStatement();
			statement.executeUpdate("INSERT INTO rp_ExplorerCompletions (`UUID`, `LocID`, `Timestamp`, `PlayerName`) "
					+ "VALUES ('" + p.getUniqueId().toString() + "', " + locID + ", " + new Date().getTime() + ", '"
					+ p.getName() + "');");
			connection.close();
			statement.close();

			// Give player reward if you've come this far...
			Commands.givePlayerExplorationReward(locID, p);
			this.incrementExplorerCompletion();
			TitleAPI.sendFullTitle(p, 1, 2, 1, ChatColor.GREEN + "✔ ✔ ✔",
					ChatColor.DARK_GREEN + "You found a secret spot!");

		} catch (SQLException err) {
			Bukkit.getLogger().log(Level.SEVERE,
					"Error updating completePlayerExploration because: " + err.getMessage());
		}

		return false;
	}

	public Boolean trackMobKill(EntityDeathEvent mobKillEvent) {

		int trackStatus = RunicParadise.trackableEntityKillsMap.get(mobKillEvent.getEntityType());
		String keyString = "";
		int newValue;

		// Is the dead entity one to track?
		if (trackStatus == 0) {
			// We don't want to track this entity
			return false;
		}

		// Is the killer a player?
		if (mobKillEvent.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent
				&& mobKillEvent.getEntity().getKiller() != null
				&& mobKillEvent.getEntity().getKiller() instanceof Player
				&& !mobKillEvent.getEntity().getWorld().equals("plotworld")) {
			// The killer is a player and world is NOT plotworld
			// Record!

			keyString = mobKillEvent.getEntityType().toString()
					+ ((Player) mobKillEvent.getEntity().getKiller()).getName();

			// INCREMENT THE HASHMAP THAT WILL EVENTUALLY UPDATE THE DB RECORD
			// Remember this hashmap holds the count that will periodically add
			// onto the player's DB-stored total
			// it is not their total kills for each entity.

			// Check if the tracker HashMap already has an entry for this player
			if (RunicParadise.mobKillTracker.containsKey(this.getPlayerID())) {
				// Entry found, increment that record

				if (RunicParadise.mobKillTracker.get(this.getPlayerID()).containsKey(mobKillEvent.getEntityType())) {
					// Entry found for this entity type, so just increment it
					newValue = RunicParadise.mobKillTracker.get(this.getPlayerID()).get(mobKillEvent.getEntityType())
							+ 1;
					RunicParadise.mobKillTracker.get(this.getPlayerID()).replace(mobKillEvent.getEntityType(),
							newValue);

				} else {
					// No entry found for this entity type. Need to create a new
					// record.
					HashMap<EntityType, Integer> tempSecondaryMap = new HashMap<EntityType, Integer>();
					tempSecondaryMap.put(mobKillEvent.getEntityType(), 1);

					RunicParadise.mobKillTracker.get(this.getPlayerID()).put(mobKillEvent.getEntityType(), 1);
				}

			} else {
				// No entry found in main map for this player, so we need to
				// create one!
				HashMap<EntityType, Integer> tempSecondaryMap = new HashMap<EntityType, Integer>();
				tempSecondaryMap.put(mobKillEvent.getEntityType(), 1);

				RunicParadise.mobKillTracker.put(this.getPlayerID(), tempSecondaryMap);
			}

			// INCREMENT THE PLAYER'S LOADED PROFILE [so they can see the kill
			// without waiting for the hashmap tracker to update]
			// Remember this is the memory-stored total kill count for each
			// entity to show in profile checks, rank up checks, etc.

			if (mobKillEvent.getEntity() != null) {
				int newTrackerValue = this.mobKillCountsMap.get(mobKillEvent.getEntityType()) + 1;
				this.mobKillCountsMap.replace(mobKillEvent.getEntityType(), newTrackerValue);
			}

		}

		// if you got this far, then you must have recorded the death. So return
		// true!
		return true;
	}

	public boolean saveMobKillsForPlayer() {
		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);

		Connection connection = MySQL.openConnection();

		for (EntityType et : RunicParadise.mobKillTracker.get(this.getPlayerID()).keySet()) {
			try {

				int killIncrement = RunicParadise.mobKillTracker.get(this.getPlayerID()).get(et);

				// Zeroes shouldn't really exist except for BAT, which I use as
				// a dummy to force creation of the map for everyone.
				// Restricting the zeroes out just keeps things clean.
				if (killIncrement != 0) {
					PreparedStatement statement = connection.prepareStatement("UPDATE rp_PlayerMobKills SET " + et.toString() + "="
							+ et.toString() + " + " + killIncrement + " WHERE UUID = ?");
					statement.setString(1, this.getPlayerID().toString());
					statement.executeUpdate();

				}
			} catch (SQLException e) {
				getLogger().log(Level.SEVERE, "Failed saveMobKillsForPlayer because: " + e.getMessage());
				return false;
			}

		}

		// Updated for this entity... so remove the records from the hashmap
		RunicParadise.mobKillTracker.get(this.getPlayerID()).clear();

		try {
			connection.close();
		} catch (SQLException e) {
			getLogger().log(Level.SEVERE, "Failed saveMobKillsForPlayer because: " + e.getMessage());
			return false;
		}

		return true;
	}

	public void showKillCountMenu(Player p) {

		Inventory kcMenu = Bukkit.createInventory(null, 54,
				ChatColor.DARK_RED + "" + ChatColor.BOLD + "Profile :: Kill Counts");

		// Top line & back buttons

		ItemMeta meta;
		ArrayList<String> mainLore = new ArrayList<String>();
		mainLore.add(ChatColor.YELLOW + "Move your mouse over each icon below");
		mainLore.add(ChatColor.YELLOW + "to see how many mobs of each type");
		mainLore.add(ChatColor.YELLOW + "you've killed. They're broken into");
		mainLore.add(ChatColor.YELLOW + "categories.");

		ItemStack mainCenter = RunicParadise.createHead(ChatColor.DARK_RED + "" + ChatColor.BOLD + "Kill Counts",
				"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWY0NDZhOGY5Mjg0YzYyY2Y4ZDQ5MWZiZGIzMzhmZDM5ZWJiZWJlMzVlOTU5YzJmYzRmNzg2YzY3NTIyZWZiIn19fQ==");
		meta = mainCenter.getItemMeta();
		meta.setDisplayName(ChatColor.DARK_RED + "" + ChatColor.BOLD + "Kill Counts");
		meta.setLore(mainLore);
		mainCenter.setItemMeta(meta);
		kcMenu.setItem(4, mainCenter);

		meta = null;
		ItemStack backButton = RunicParadise.createHead(ChatColor.GRAY + "Return to Previous Menu",
				"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjI1OTliZDk4NjY1OWI4Y2UyYzQ5ODg1MjVjOTRlMTlkZGQzOWZhZDA4YTM4Mjg0YTE5N2YxYjcwNjc1YWNjIn19fQ==");
		meta = backButton.getItemMeta();
		meta.setDisplayName(ChatColor.GRAY + "Return to Previous Menu");
		backButton.setItemMeta(meta);
		kcMenu.setItem(53, backButton);

		// FARM ANIMALS

		ItemMeta farmMeta;
		ArrayList<String> farmLore = new ArrayList<String>();
		farmLore.add("");
		farmLore.add(ChatColor.BLUE + "Cows: " + ChatColor.WHITE + this.mobKillCountsMap.get(EntityType.COW));
		farmLore.add(ChatColor.BLUE + "Chickens: " + ChatColor.WHITE + this.mobKillCountsMap.get(EntityType.CHICKEN));
		farmLore.add(ChatColor.BLUE + "Mushroom Cows: " + ChatColor.WHITE
				+ this.mobKillCountsMap.get(EntityType.MUSHROOM_COW));
		farmLore.add(ChatColor.BLUE + "Pigs: " + ChatColor.WHITE + this.mobKillCountsMap.get(EntityType.PIG));
		farmLore.add(ChatColor.BLUE + "Rabbits: " + ChatColor.WHITE + this.mobKillCountsMap.get(EntityType.RABBIT));
		farmLore.add(ChatColor.BLUE + "Sheep: " + ChatColor.WHITE + this.mobKillCountsMap.get(EntityType.SHEEP));

		ItemStack farmAnimals = RunicParadise.createHead(ChatColor.GOLD + "Farm Animals",
				"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWU5NWM0MWMxNmFiY2RkMzdhNDAxZjBmZjFiNGUxMDIxMTgzNGQ0YzY4YTgzOGM2ODkxYzNhMWUyZTM5NjEifX19");
		farmMeta = farmAnimals.getItemMeta();
		farmMeta.setDisplayName(ChatColor.GOLD + "Farm Animals");
		farmMeta.setLore(farmLore);
		farmAnimals.setItemMeta(farmMeta);
		kcMenu.setItem(10, farmAnimals);

		// WILD ANIMALS

		ItemMeta wildMeta;
		ArrayList<String> wildLore = new ArrayList<String>();
		wildLore.add("");
		wildLore.add(ChatColor.BLUE + "Bats: " + ChatColor.WHITE + this.mobKillCountsMap.get(EntityType.BAT));
		wildLore.add(ChatColor.BLUE + "Llamas: " + ChatColor.WHITE + this.mobKillCountsMap.get(EntityType.LLAMA));
		wildLore.add(ChatColor.BLUE + "Ocelots: " + ChatColor.WHITE + this.mobKillCountsMap.get(EntityType.OCELOT));
		wildLore.add(
				ChatColor.BLUE + "Polar Bears: " + ChatColor.WHITE + this.mobKillCountsMap.get(EntityType.POLAR_BEAR));
		wildLore.add(ChatColor.BLUE + "Wolves: " + ChatColor.WHITE + this.mobKillCountsMap.get(EntityType.WOLF));

		ItemStack wildAnimals = RunicParadise.createHead(ChatColor.GOLD + "Wild Animals",
				"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTM4MTM3ZWEzMTI4NmJiMGEyNGI4YTZkYjkxZmMwMWVlMGJiYWQ4NTFkNWUxOGFmMGViZTI5YTk3ZTcifX19");
		wildMeta = wildAnimals.getItemMeta();
		wildMeta.setDisplayName(ChatColor.GOLD + "Wild Animals");
		wildMeta.setLore(wildLore);
		wildAnimals.setItemMeta(wildMeta);
		kcMenu.setItem(11, wildAnimals);

		// HORSE ANIMALS

		ItemMeta horseMeta;
		ArrayList<String> horseLore = new ArrayList<String>();
		horseLore.add("");
		horseLore.add(ChatColor.BLUE + "Donkeys: " + ChatColor.WHITE + this.mobKillCountsMap.get(EntityType.DONKEY));
		horseLore.add(ChatColor.BLUE + "Horses: " + ChatColor.WHITE + this.mobKillCountsMap.get(EntityType.HORSE));
		horseLore.add(ChatColor.BLUE + "Mules: " + ChatColor.WHITE + this.mobKillCountsMap.get(EntityType.MULE));
		horseLore.add(ChatColor.BLUE + "Skeleton Horses: " + ChatColor.WHITE
				+ this.mobKillCountsMap.get(EntityType.SKELETON_HORSE));
		horseLore.add(ChatColor.BLUE + "Zombie Horses: " + ChatColor.WHITE
				+ this.mobKillCountsMap.get(EntityType.ZOMBIE_HORSE));

		ItemStack horseAnimals = RunicParadise.createHead(ChatColor.GOLD + "Horse Animals",
				"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmU3OGM0NzYyNjc0ZGRlOGIxYTVhMWU4NzNiMzNmMjhlMTNlN2MxMDJiMTkzZjY4MzU0OWIzOGRjNzBlMCJ9fX0=");
		horseMeta = horseAnimals.getItemMeta();
		horseMeta.setDisplayName(ChatColor.GOLD + "Horse Animals");
		horseMeta.setLore(horseLore);
		horseAnimals.setItemMeta(horseMeta);
		kcMenu.setItem(12, horseAnimals);

		// OCEAN

		ItemMeta oceanMeta;
		ArrayList<String> oceanLore = new ArrayList<String>();
		oceanLore.add("");
		oceanLore.add(ChatColor.BLUE + "Elder Guardians: " + ChatColor.WHITE
				+ this.mobKillCountsMap.get(EntityType.ELDER_GUARDIAN));
		oceanLore
				.add(ChatColor.BLUE + "Guardians: " + ChatColor.WHITE + this.mobKillCountsMap.get(EntityType.GUARDIAN));
		oceanLore.add(ChatColor.BLUE + "Squids: " + ChatColor.WHITE + this.mobKillCountsMap.get(EntityType.SQUID));

		ItemStack oceanAnimals = RunicParadise.createHead(ChatColor.GOLD + "Creatures of the Sea",
				"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTBiZjM0YTcxZTc3MTViNmJhNTJkNWRkMWJhZTVjYjg1Zjc3M2RjOWIwZDQ1N2I0YmZjNWY5ZGQzY2M3Yzk0In19fQ==");
		oceanMeta = oceanAnimals.getItemMeta();
		oceanMeta.setDisplayName(ChatColor.GOLD + "Creatures of the Sea");
		oceanMeta.setLore(oceanLore);
		oceanAnimals.setItemMeta(oceanMeta);
		kcMenu.setItem(14, oceanAnimals);

		// NETHER

		ItemMeta netherMeta;
		ArrayList<String> netherLore = new ArrayList<String>();
		netherLore.add("");
		netherLore.add(ChatColor.BLUE + "Blazes: " + ChatColor.WHITE + this.mobKillCountsMap.get(EntityType.BLAZE));
		netherLore.add(ChatColor.BLUE + "Ghasts: " + ChatColor.WHITE + this.mobKillCountsMap.get(EntityType.GHAST));
		netherLore.add(
				ChatColor.BLUE + "Magma Cubes: " + ChatColor.WHITE + this.mobKillCountsMap.get(EntityType.MAGMA_CUBE));
		netherLore.add(
				ChatColor.BLUE + "Pig Zombies: " + ChatColor.WHITE + this.mobKillCountsMap.get(EntityType.PIG_ZOMBIE));
		netherLore.add(ChatColor.BLUE + "Withers: " + ChatColor.WHITE + this.mobKillCountsMap.get(EntityType.WITHER));

		ItemStack netherAnimals = RunicParadise.createHead(ChatColor.GOLD + "Nether Demons",
				"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzg5NTdkNTAyM2M5MzdjNGM0MWFhMjQxMmQ0MzQxMGJkYTIzY2Y3OWE5ZjZhYjM2Yjc2ZmVmMmQ3YzQyOSJ9fX0=");
		netherMeta = netherAnimals.getItemMeta();
		netherMeta.setDisplayName(ChatColor.GOLD + "Nether Demons");
		netherMeta.setLore(netherLore);
		netherAnimals.setItemMeta(netherMeta);
		kcMenu.setItem(15, netherAnimals);

		// END

		ItemMeta endMeta;
		ArrayList<String> endLore = new ArrayList<String>();
		endLore.add("");
		endLore.add(ChatColor.BLUE + "Endermen: " + ChatColor.WHITE + this.mobKillCountsMap.get(EntityType.ENDERMAN));
		endLore.add(ChatColor.BLUE + "Ender Dragons: " + ChatColor.WHITE
				+ this.mobKillCountsMap.get(EntityType.ENDER_DRAGON));
		endLore.add(ChatColor.BLUE + "Shulkers: " + ChatColor.WHITE + this.mobKillCountsMap.get(EntityType.SHULKER));

		ItemStack endAnimals = RunicParadise.createHead(ChatColor.GOLD + "Monsters of the End",
				"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjFkMzUzNGQyMWZlODQ5OTI2MmRlODdhZmZiZWFjNGQyNWZmZGUzNWM4YmRjYTA2OWU2MWUxNzg3ZmYyZiJ9fX0=");
		endMeta = endAnimals.getItemMeta();
		endMeta.setDisplayName(ChatColor.GOLD + "Monsters of the End");
		endMeta.setLore(endLore);
		endAnimals.setItemMeta(endMeta);
		kcMenu.setItem(16, endAnimals);

		// PEOPLE

		ItemMeta peopleMeta;
		ArrayList<String> peopleLore = new ArrayList<String>();
		peopleLore.add("");
		peopleLore.add(ChatColor.BLUE + "Evokers: " + ChatColor.WHITE + this.mobKillCountsMap.get(EntityType.EVOKER));
		peopleLore.add(
				ChatColor.BLUE + "Iron Golems: " + ChatColor.WHITE + this.mobKillCountsMap.get(EntityType.IRON_GOLEM));
		peopleLore.add(ChatColor.BLUE + "Snowmen: " + ChatColor.WHITE + this.mobKillCountsMap.get(EntityType.SNOWMAN));
		peopleLore.add(ChatColor.BLUE + "Vex: " + ChatColor.WHITE + this.mobKillCountsMap.get(EntityType.VEX));
		peopleLore
				.add(ChatColor.BLUE + "Villagers: " + ChatColor.WHITE + this.mobKillCountsMap.get(EntityType.VILLAGER));
		peopleLore.add(
				ChatColor.BLUE + "Vindicators: " + ChatColor.WHITE + this.mobKillCountsMap.get(EntityType.VINDICATOR));
		peopleLore.add(ChatColor.BLUE + "Witches: " + ChatColor.WHITE + this.mobKillCountsMap.get(EntityType.WITCH));

		ItemStack peopleAnimals = RunicParadise.createHead(ChatColor.GOLD + "Humans & Servants",
				"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODIyZDhlNzUxYzhmMmZkNGM4OTQyYzQ0YmRiMmY1Y2E0ZDhhZThlNTc1ZWQzZWIzNGMxOGE4NmU5M2IifX19");
		peopleMeta = peopleAnimals.getItemMeta();
		peopleMeta.setDisplayName(ChatColor.GOLD + "Humans & Servants");
		peopleMeta.setLore(peopleLore);
		peopleAnimals.setItemMeta(peopleMeta);
		kcMenu.setItem(18, peopleAnimals);

		// BASE MONSTERS

		ItemMeta basemobsMeta;
		ArrayList<String> basemobsLore = new ArrayList<String>();
		basemobsLore.add("");
		basemobsLore
				.add(ChatColor.BLUE + "Creepers: " + ChatColor.WHITE + this.mobKillCountsMap.get(EntityType.CREEPER));
		basemobsLore.add(ChatColor.BLUE + "Giants: " + ChatColor.WHITE + this.mobKillCountsMap.get(EntityType.GIANT));
		basemobsLore.add(ChatColor.BLUE + "Slime: " + ChatColor.WHITE + this.mobKillCountsMap.get(EntityType.SLIME));

		ItemStack basemobsAnimals = RunicParadise.createHead(ChatColor.GOLD + "Overworld Monsters",
				"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzBkZjI2ZTc0NzUyZDU0YTk0Zjg4NWQxNzk5ODU5YjZjYTRhZGIwMTA5ODFmNTIyMDk4ZDNlMzFlMTU3NCJ9fX0=");
		basemobsMeta = basemobsAnimals.getItemMeta();
		basemobsMeta.setDisplayName(ChatColor.GOLD + "Overworld Monsters");
		basemobsMeta.setLore(basemobsLore);
		basemobsAnimals.setItemMeta(basemobsMeta);
		kcMenu.setItem(20, basemobsAnimals);

		// SPIDERS

		ItemMeta spiderMeta;
		ArrayList<String> spiderLore = new ArrayList<String>();
		spiderLore.add("");

		spiderLore.add(ChatColor.BLUE + "Cave Spiders: " + ChatColor.WHITE
				+ this.mobKillCountsMap.get(EntityType.CAVE_SPIDER));
		spiderLore.add(
				ChatColor.BLUE + "Endermites: " + ChatColor.WHITE + this.mobKillCountsMap.get(EntityType.ENDERMITE));
		spiderLore.add(
				ChatColor.BLUE + "Silverfish: " + ChatColor.WHITE + this.mobKillCountsMap.get(EntityType.SILVERFISH));
		spiderLore.add(ChatColor.BLUE + "Spiders: " + ChatColor.WHITE + this.mobKillCountsMap.get(EntityType.SPIDER));

		ItemStack spiderAnimals = RunicParadise.createHead(ChatColor.GOLD + "Creepy Crawly Things",
				"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzY1MmJkNzYxN2U1NmM3NTY1MTRmODNjMjhkZDFkOTZhN2U1ZTE2N2JmN2ZiNTkzNjkzZmM2NTA0NmY3OTkifX19");
		spiderMeta = spiderAnimals.getItemMeta();
		spiderMeta.setDisplayName(ChatColor.GOLD + "Creepy Crawly Things");
		spiderMeta.setLore(spiderLore);
		spiderAnimals.setItemMeta(spiderMeta);
		kcMenu.setItem(22, spiderAnimals);

		// ZOMBIES

		ItemMeta zombieMeta;
		ArrayList<String> zombieLore = new ArrayList<String>();
		zombieLore.add("");

		zombieLore.add(ChatColor.BLUE + "Husks: " + ChatColor.WHITE + this.mobKillCountsMap.get(EntityType.HUSK));
		zombieLore.add(ChatColor.BLUE + "Zombies: " + ChatColor.WHITE + this.mobKillCountsMap.get(EntityType.ZOMBIE));
		zombieLore.add(ChatColor.BLUE + "Zombie Villagers: " + ChatColor.WHITE
				+ this.mobKillCountsMap.get(EntityType.ZOMBIE_VILLAGER));

		ItemStack zombieAnimals = RunicParadise.createHead(ChatColor.GOLD + "Zombies",
				"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzNmYjRlNWRiOTdmNDc5YzY2YTQyYmJkOGE3ZDc4MWRhZjIwMWE4ZGRhZjc3YWZjZjRhZWY4Nzc3OWFhOGI0In19fQ==");
		zombieMeta = zombieAnimals.getItemMeta();
		zombieMeta.setDisplayName(ChatColor.GOLD + "Zombies");
		zombieMeta.setLore(zombieLore);
		zombieAnimals.setItemMeta(zombieMeta);
		kcMenu.setItem(24, zombieAnimals);

		// SKELETONS

		ItemMeta skellyMeta;
		ArrayList<String> skellyLore = new ArrayList<String>();
		skellyLore.add("");

		skellyLore
				.add(ChatColor.BLUE + "Skeletons: " + ChatColor.WHITE + this.mobKillCountsMap.get(EntityType.SKELETON));
		skellyLore.add(ChatColor.BLUE + "Strays: " + ChatColor.WHITE + this.mobKillCountsMap.get(EntityType.STRAY));
		skellyLore.add(ChatColor.BLUE + "Wither Skeletons: " + ChatColor.WHITE
				+ this.mobKillCountsMap.get(EntityType.WITHER_SKELETON));

		ItemStack skellyAnimals = RunicParadise.createHead(ChatColor.GOLD + "Skellies",
				"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjJkMjJhNTEzNTJiYTQxMWVjNjg3MzI3NmI2MjgxOTVlODg4MzZjNjk3NWYyZDQ1MTJlNjE2Nzg4OWE1ZiJ9fX0=");
		skellyMeta = skellyAnimals.getItemMeta();
		skellyMeta.setDisplayName(ChatColor.GOLD + "Skellies");
		skellyMeta.setLore(skellyLore);
		skellyAnimals.setItemMeta(skellyMeta);
		kcMenu.setItem(26, skellyAnimals);

		// bl tier 1

		ItemMeta bl1Meta;
		ArrayList<String> bl1Lore = new ArrayList<String>();
		bl1Lore.add("");

		ItemStack bl1Animals = RunicParadise.createHead(ChatColor.GOLD + "Borderlands Tier 1",
				"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjhjMDE2NWU5YjJkYmQ3OGRhYzkxMjc3ZTk3ZDlhMDI2NDhmMzA1OWUxMjZhNTk0MWE4NGQwNTQyOWNlIn19fQ==");
		bl1Meta = bl1Animals.getItemMeta();
		bl1Meta.setDisplayName(ChatColor.GOLD + "Borderlands Tier 1");
		bl1Meta.setLore(bl1Lore);
		bl1Animals.setItemMeta(bl1Meta);
		kcMenu.setItem(37, bl1Animals);

		// bl tier 2

		ItemMeta bl2Meta;
		ArrayList<String> bl2Lore = new ArrayList<String>();
		bl2Lore.add("");

		ItemStack bl2Animals = RunicParadise.createHead(ChatColor.GOLD + "Borderlands Tier 2",
				"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTM0MTU2NjdkZTNmYjg5YzVmNDBjODgwYzM5ZTQ5NzFhMGNhYTdmM2E5ZDJjOGY3MTJiYTM3ZmFkY2VlIn19fQ==");
		bl2Meta = bl2Animals.getItemMeta();
		bl2Meta.setDisplayName(ChatColor.GOLD + "Borderlands Tier 2");
		bl2Meta.setLore(bl2Lore);
		bl2Animals.setItemMeta(bl2Meta);
		kcMenu.setItem(39, bl2Animals);

		// bl tier 3

		ItemMeta bl3Meta;
		ArrayList<String> bl3Lore = new ArrayList<String>();
		bl3Lore.add("");

		ItemStack bl3Animals = RunicParadise.createHead(ChatColor.GOLD + "Borderlands Tier 3",
				"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjJkNjFjOWU3MDdhOTE1N2M3ZDBlMzI4MTk0ODY5ZjZiZGZlZTY4Y2RlZWZhNDhhODdlOWQzOGVjMTlhNGMyIn19fQ==");
		bl3Meta = bl3Animals.getItemMeta();
		bl3Meta.setDisplayName(ChatColor.GOLD + "Borderlands Tier 3");
		bl3Meta.setLore(bl3Lore);
		bl3Animals.setItemMeta(bl3Meta);
		kcMenu.setItem(41, bl3Animals);

		// bl tier 4

		ItemMeta bl4Meta;
		ArrayList<String> bl4Lore = new ArrayList<String>();
		bl4Lore.add("");

		ItemStack bl4Animals = RunicParadise.createHead(ChatColor.GOLD + "Borderlands Tier 4",
				"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGM3OGQyMTAyZGI3NWYxYjM3NDRhNWU3ZTliYWNjZjg4ZmRhNGNjNDk3OWViYzBhODFiN2Q5ZWI1NzIxYzAifX19");
		bl4Meta = bl4Animals.getItemMeta();
		bl4Meta.setDisplayName(ChatColor.GOLD + "Borderlands Tier 4");
		bl4Meta.setLore(bl4Lore);
		bl4Animals.setItemMeta(bl4Meta);
		kcMenu.setItem(43, bl4Animals);

		p.openInventory(kcMenu);

	}

	public void showProfileOptionsMenu(Player p) {
		Inventory poMenu = Bukkit.createInventory(null, 54,
				ChatColor.DARK_RED + "" + ChatColor.BOLD + "Profile :: Chat Options");

		// Top line & back buttons

		ItemMeta meta;
		ArrayList<String> mainLore = new ArrayList<String>();
		mainLore.add(ChatColor.YELLOW + "Move your mouse over each icon below");
		mainLore.add(ChatColor.YELLOW + "to see what options are available");
		mainLore.add(ChatColor.YELLOW + "and how to change each of them.");

		ItemStack mainCenter = new ItemStack(Material.LIGHT_GRAY_WOOL);
		meta = mainCenter.getItemMeta();
		meta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Chat Options");
		meta.setLore(mainLore);
		mainCenter.setItemMeta(meta);
		poMenu.setItem(4, mainCenter);

		meta = null;
		ItemStack backButton = RunicParadise.createHead(ChatColor.GRAY + "Return to Previous Menu",
				"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjI1OTliZDk4NjY1OWI4Y2UyYzQ5ODg1MjVjOTRlMTlkZGQzOWZhZDA4YTM4Mjg0YTE5N2YxYjcwNjc1YWNjIn19fQ==");
		meta = backButton.getItemMeta();
		meta.setDisplayName(ChatColor.GRAY + "Return to Previous Menu");
		backButton.setItemMeta(meta);
		poMenu.setItem(53, backButton);

		// GENDER

		ArrayList<String> genderLore = new ArrayList<>();
		genderLore.add("");
		genderLore.add(ChatColor.WHITE + "This setting controls how your rank");
		genderLore.add(ChatColor.WHITE + "displays for dukes, barons, and lords.");
		genderLore.add(ChatColor.WHITE + "Example: Lord vs Lady");
		genderLore.add("");

		ItemStack genderIcon;
		ItemMeta genderMeta;

		if (this.getGender() == 'M') {

			genderIcon = new ItemStack(Material.LIGHT_BLUE_WOOL);
			genderMeta = genderIcon.getItemMeta();
			genderMeta.setDisplayName(
					ChatColor.GRAY + "Currently Active: " + ChatColor.BLUE + "" + ChatColor.BOLD + "Boy");
			genderLore.add(ChatColor.WHITE + "Click here to change to " + ChatColor.RED + "girl");
			genderMeta.setLore(genderLore);
			genderIcon.setItemMeta(genderMeta);

		} else if (this.getGender() == 'F') {

			genderIcon = new ItemStack(Material.PINK_WOOL);
			genderMeta = genderIcon.getItemMeta();
			genderMeta.setDisplayName(
					ChatColor.GRAY + "Currently Active: " + ChatColor.RED + "" + ChatColor.BOLD + "Girl");
			genderLore.add(ChatColor.WHITE + "Click here to change to " + ChatColor.BLUE + "boy");
			genderMeta.setLore(genderLore);
			genderIcon.setItemMeta(genderMeta);

		} else {
			genderIcon = new ItemStack(Material.BLACK_WOOL);
			genderMeta = genderIcon.getItemMeta();
			genderMeta.setDisplayName(
					ChatColor.GRAY + "Currently Active: " + ChatColor.YELLOW + "" + ChatColor.BOLD + "ERROR");
			genderMeta.setLore(genderLore);
			genderIcon.setItemMeta(genderMeta);
		}

		poMenu.setItem(12, genderIcon);

		// CHAT COLORS

		ItemStack colorIcon;
		ItemMeta colorMeta;
		ArrayList<String> colorLore = new ArrayList<String>();
		colorLore.add(ChatColor.YELLOW + "Move your mouse over each icon below");
		colorLore.add(ChatColor.YELLOW + "to see what options are available");
		colorLore.add(ChatColor.YELLOW + "and how to change each of them.");

		Player player = Bukkit.getPlayer(this.getPlayerID());

		// 28 GREEN
		if (player.hasPermission("rp.chatcolors.green")
				|| RunicParadise.perms.getPrimaryGroup(player).equals("Seeker")) {
			colorIcon = new ItemStack(Material.LIME_WOOL);
			colorMeta = colorIcon.getItemMeta();
			colorMeta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Green");

			colorLore.clear();
			colorLore.add(ChatColor.GREEN + "Click here to make your rank");
			colorLore.add(ChatColor.GREEN + "and name in chat appear green!");

			colorMeta.setLore(colorLore);
			colorIcon.setItemMeta(colorMeta);
			poMenu.setItem(28, colorIcon);
		}

		// 29 D GREEN
		if (player.hasPermission("rp.chatcolors.dark_green")
				|| RunicParadise.perms.getPrimaryGroup(player).equals("Runner")) {
			colorIcon = new ItemStack(Material.GREEN_WOOL);
			colorMeta = colorIcon.getItemMeta();
			colorMeta.setDisplayName(ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Dark Green");

			colorLore.clear();
			colorLore.add(ChatColor.DARK_GREEN + "Click here to make your rank");
			colorLore.add(ChatColor.DARK_GREEN + "and name in chat appear dark green!");

			colorMeta.setLore(colorLore);
			colorIcon.setItemMeta(colorMeta);
			poMenu.setItem(29, colorIcon);
		}

		// 30 YELLOW
		if (player.hasPermission("rp.chatcolors.yellow")
				|| RunicParadise.perms.getPrimaryGroup(player).equals("Singer")) {
			colorIcon = new ItemStack(Material.YELLOW_WOOL);
			colorMeta = colorIcon.getItemMeta();
			colorMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Yellow");

			colorLore.clear();
			colorLore.add(ChatColor.YELLOW + "Click here to make your rank");
			colorLore.add(ChatColor.YELLOW + "and name in chat appear yellow!");

			colorMeta.setLore(colorLore);
			colorIcon.setItemMeta(colorMeta);
			poMenu.setItem(30, colorIcon);
		}

		// 31 GOLD
		if (player.hasPermission("rp.chatcolors.gold")
				|| RunicParadise.perms.getPrimaryGroup(player).equals("Brawler")) {
			colorIcon = new ItemStack(Material.ORANGE_WOOL);
			colorMeta = colorIcon.getItemMeta();
			colorMeta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Gold");

			colorLore.clear();
			colorLore.add(ChatColor.GOLD + "Click here to make your rank");
			colorLore.add(ChatColor.GOLD + "and name in chat appear gold!");

			colorMeta.setLore(colorLore);
			colorIcon.setItemMeta(colorMeta);
			poMenu.setItem(31, colorIcon);
		}

		// 32 AQUA
		if (player.hasPermission("rp.chatcolors.aqua")
				|| RunicParadise.perms.getPrimaryGroup(player).equals("Keeper")) {
			colorIcon = new ItemStack(Material.LIGHT_BLUE_WOOL);
			colorMeta = colorIcon.getItemMeta();
			colorMeta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Aqua");

			colorLore.clear();
			colorLore.add(ChatColor.AQUA + "Click here to make your rank");
			colorLore.add(ChatColor.AQUA + "and name in chat appear aqua!");

			colorMeta.setLore(colorLore);
			colorIcon.setItemMeta(colorMeta);
			poMenu.setItem(32, colorIcon);
		}

		// 33 D AQUA
		if (player.hasPermission("rp.chatcolors.dark_aqua")
				|| RunicParadise.perms.getPrimaryGroup(player).equals("Guard")) {
			colorIcon = new ItemStack(Material.CYAN_WOOL);
			colorMeta = colorIcon.getItemMeta();
			colorMeta.setDisplayName(ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Dark Aqua");

			colorLore.clear();
			colorLore.add(ChatColor.DARK_AQUA + "Click here to make your rank");
			colorLore.add(ChatColor.DARK_AQUA + "and name in chat appear dark aqua!");

			colorMeta.setLore(colorLore);
			colorIcon.setItemMeta(colorMeta);
			poMenu.setItem(33, colorIcon);
		}

		// 34 BLUE
		if (player.hasPermission("rp.chatcolors.blue")
				|| RunicParadise.perms.getPrimaryGroup(player).equals("Hunter")) {
			colorIcon = new ItemStack(Material.BLUE_WOOL);
			colorMeta = colorIcon.getItemMeta();
			colorMeta.setDisplayName(ChatColor.BLUE + "" + ChatColor.BOLD + "Blue");

			colorLore.clear();
			colorLore.add(ChatColor.BLUE + "Click here to make your rank");
			colorLore.add(ChatColor.BLUE + "and name in chat appear blue!");

			colorMeta.setLore(colorLore);
			colorIcon.setItemMeta(colorMeta);
			poMenu.setItem(34, colorIcon);
		}

		// 38 L PURPLE
		if (player.hasPermission("rp.chatcolors.light_purple")
				|| RunicParadise.perms.getPrimaryGroup(player).equals("Warder")) {
			colorIcon = new ItemStack(Material.MAGENTA_WOOL);
			colorMeta = colorIcon.getItemMeta();
			colorMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Light Purple");

			colorLore.clear();
			colorLore.add(ChatColor.LIGHT_PURPLE + "Click here to make your rank");
			colorLore.add(ChatColor.LIGHT_PURPLE + "and name in chat appear light purple!");

			colorMeta.setLore(colorLore);
			colorIcon.setItemMeta(colorMeta);
			poMenu.setItem(38, colorIcon);
		}

		// 39 D PURPLE
		if (player.hasPermission("rp.chatcolors.dark_purple")
				|| RunicParadise.perms.getPrimaryGroup(player).equals("Champion")) {
			colorIcon = new ItemStack(Material.PURPLE_WOOL);
			colorMeta = colorIcon.getItemMeta();
			colorMeta.setDisplayName(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Dark Purple");

			colorLore.clear();
			colorLore.add(ChatColor.DARK_PURPLE + "Click here to make your rank");
			colorLore.add(ChatColor.DARK_PURPLE + "and name in chat appear dark purple!");

			colorMeta.setLore(colorLore);
			colorIcon.setItemMeta(colorMeta);
			poMenu.setItem(39, colorIcon);
		}

		// 41 RED
		if (player.hasPermission("rp.chatcolors.red") || RunicParadise.perms.getPrimaryGroup(player).equals("Master")) {
			colorIcon = new ItemStack(Material.RED_WOOL);
			colorMeta = colorIcon.getItemMeta();
			colorMeta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Red");

			colorLore.clear();
			colorLore.add(ChatColor.RED + "Click here to make your rank");
			colorLore.add(ChatColor.RED + "and name in chat appear red!");

			colorMeta.setLore(colorLore);
			colorIcon.setItemMeta(colorMeta);
			poMenu.setItem(41, colorIcon);
		}

		// 42 WHITE
		if (player.hasPermission("rp.chatcolors.white")) {
			colorIcon = new ItemStack(Material.WHITE_WOOL);
			colorMeta = colorIcon.getItemMeta();
			colorMeta.setDisplayName(ChatColor.WHITE + "" + ChatColor.BOLD + "White");

			colorLore.clear();
			colorLore.add(ChatColor.WHITE + "Click here to make your rank");
			colorLore.add(ChatColor.WHITE + "and name in chat appear white!");

			colorMeta.setLore(colorLore);
			colorIcon.setItemMeta(colorMeta);
			poMenu.setItem(42, colorIcon);
		}

		p.openInventory(poMenu);

	}

	public void showServerMenu(Player p) {

		Inventory poMenu = Bukkit.createInventory(null, 36,
				ChatColor.DARK_RED + "" + ChatColor.BOLD + "Profile :: Main Menu");

		// Top line & back buttons

		ItemMeta meta;
		ArrayList<String> mainLore = new ArrayList<String>();
		mainLore.add(ChatColor.YELLOW + "Move your mouse over each icon below");
		mainLore.add(ChatColor.YELLOW + "to see what menus are available here.");
		mainLore.add(ChatColor.YELLOW + "Click an icon to open that menu.");

		ItemStack mainCenter = new ItemStack(Material.NETHER_STAR, 1);
		meta = mainCenter.getItemMeta();
		meta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Main Menu");
		meta.setLore(mainLore);
		mainCenter.setItemMeta(meta);
		poMenu.setItem(4, mainCenter);

		// KILL COUNTS

		ArrayList<String> killcountLore = new ArrayList<String>();
		killcountLore.add("");
		killcountLore.add(ChatColor.WHITE + "Click here to see how many");
		killcountLore.add(ChatColor.WHITE + "mobs you have killed.");

		ItemStack killcountIcon = new ItemStack(Material.IRON_SWORD, 1);
		ItemMeta killcountMeta = killcountIcon.getItemMeta();
		killcountMeta.setDisplayName(ChatColor.DARK_RED + "Kill Counts");
		killcountMeta.setLore(killcountLore);
		killcountIcon.setItemMeta(killcountMeta);
		poMenu.setItem(10, killcountIcon);

		// CHAT OPTIONS

		ArrayList<String> chatoptionsLore = new ArrayList<String>();
		chatoptionsLore.add("");
		chatoptionsLore.add(ChatColor.WHITE + "Click here to change your");
		chatoptionsLore.add(ChatColor.WHITE + "chat options.");

		ItemStack chatoptionsIcon = new ItemStack(Material.HOPPER, 1);
		ItemMeta chatoptionsMeta = chatoptionsIcon.getItemMeta();
		chatoptionsMeta.setDisplayName(ChatColor.DARK_RED + "Chat Options");
		chatoptionsMeta.setLore(chatoptionsLore);
		chatoptionsIcon.setItemMeta(chatoptionsMeta);
		poMenu.setItem(11, chatoptionsIcon);

		// DEATH AND GRAVES

		ArrayList<String> afterlifeLore = new ArrayList<String>();
		afterlifeLore.add("");
		afterlifeLore.add(ChatColor.YELLOW + "Graves not found: "
				+ RunicParadise.playerProfiles.get(p.getUniqueId()).getCountGraves("Unopened"));
		afterlifeLore.add(ChatColor.GREEN + "Graves found: "
				+ RunicParadise.playerProfiles.get(p.getUniqueId()).getCountGraves("Opened"));
		afterlifeLore.add(ChatColor.AQUA + "Graves stolen by you: "
				+ RunicParadise.playerProfiles.get(p.getUniqueId()).getCountGraves("StolenByYou"));
		afterlifeLore.add(ChatColor.RED + "Graves stolen from you: "
				+ RunicParadise.playerProfiles.get(p.getUniqueId()).getCountGraves("StolenFromYou"));
		afterlifeLore.add("");
		afterlifeLore.add(ChatColor.LIGHT_PURPLE + "Souls remaining: "
				+ RunicParadise.playerProfiles.get(p.getUniqueId()).getSoulCount());
		afterlifeLore.add("");
		afterlifeLore.add(ChatColor.GRAY + "/warp graves " + ChatColor.WHITE + "for more info.");

		ItemStack afterlifeIcon = new ItemStack(Material.TOTEM_OF_UNDYING);
		ItemMeta afterlifeMeta = afterlifeIcon.getItemMeta();
		afterlifeMeta.setDisplayName(ChatColor.DARK_RED + "Death & Graves");
		afterlifeMeta.setLore(afterlifeLore);
		afterlifeIcon.setItemMeta(afterlifeMeta);

		poMenu.setItem(12, afterlifeIcon);

		// JOBS

		ArrayList<String> jobsLore = new ArrayList<>();
		jobsLore.add("");
		jobsLore.add(ChatColor.YELLOW + "# of jobs mastered: " + ChatColor.GRAY
				+ +RunicParadise.playerProfiles.get(p.getUniqueId()).getJobMasteryCount());
		jobsLore.add(ChatColor.YELLOW + "Jobs mastered: ");

		String masteryList = "";
		if (RunicParadise.playerProfiles.get(p.getUniqueId()).getJobMasteryString().contains(",")) {

			String[] tempBuilder = RunicParadise.playerProfiles.get(p.getUniqueId()).getJobMasteryString().split(",");
			int count = 0;

			if (tempBuilder.length <= 4) {
				jobsLore.add(ChatColor.GRAY + RunicParadise.playerProfiles.get(p.getUniqueId()).getJobMasteryString());
			} else if (tempBuilder.length <= 7) {

				while (count <= 3) {
					masteryList += tempBuilder[count] + ", ";
					count++;
				}
				jobsLore.add(ChatColor.GRAY + masteryList);
				masteryList = "";

				while (count <= tempBuilder.length - 1) {
					masteryList += tempBuilder[count] + ", ";
					count++;
				}
				jobsLore.add(ChatColor.GRAY + masteryList);
				masteryList = "";

			} else if (tempBuilder.length <= 11) {

				while (count <= 3) {
					masteryList += tempBuilder[count] + ", ";
					count++;
				}
				jobsLore.add(ChatColor.GRAY + masteryList);
				masteryList = "";

				while (count <= 7) {
					masteryList += tempBuilder[count] + ", ";
					count++;
				}
				jobsLore.add(ChatColor.GRAY + masteryList);
				masteryList = "";

				while (count <= tempBuilder.length - 1) {
					masteryList += tempBuilder[count] + ", ";
					count++;
				}
				jobsLore.add(ChatColor.GRAY + masteryList);
				masteryList = "";

			} else if (tempBuilder.length <= 15) {

				while (count <= 3) {
					masteryList += tempBuilder[count] + ", ";
					count++;
				}
				jobsLore.add(ChatColor.GRAY + masteryList);
				masteryList = "";

				while (count <= 7) {
					masteryList += tempBuilder[count] + ", ";
					count++;
				}
				jobsLore.add(ChatColor.GRAY + masteryList);
				masteryList = "";

				while (count <= 11) {
					masteryList += tempBuilder[count] + ", ";
					count++;
				}
				jobsLore.add(ChatColor.GRAY + masteryList);
				masteryList = "";

				while (count <= tempBuilder.length - 1) {
					masteryList += tempBuilder[count] + ", ";
					count++;
				}
				jobsLore.add(ChatColor.GRAY + masteryList);
				masteryList = "";

			} else if (tempBuilder.length <= 19) {

				while (count <= 3) {
					masteryList += tempBuilder[count] + ", ";
					count++;
				}
				jobsLore.add(ChatColor.GRAY + masteryList);
				masteryList = "";

				while (count <= 7) {
					masteryList += tempBuilder[count] + ", ";
					count++;
				}
				jobsLore.add(ChatColor.GRAY + masteryList);
				masteryList = "";

				while (count <= 11) {
					masteryList += tempBuilder[count] + ", ";
					count++;
				}
				jobsLore.add(ChatColor.GRAY + masteryList);
				masteryList = "";

				while (count <= 15) {
					masteryList += tempBuilder[count] + ", ";
					count++;
				}
				jobsLore.add(ChatColor.GRAY + masteryList);
				masteryList = "";

				while (count <= tempBuilder.length - 1) {
					masteryList += tempBuilder[count] + ", ";
					count++;
				}
				jobsLore.add(ChatColor.GRAY + masteryList);
				masteryList = "";

			} else {

				while (count <= 3) {
					masteryList += tempBuilder[count] + ", ";
					count++;
				}
				jobsLore.add(ChatColor.GRAY + masteryList);
				masteryList = "";

				while (count <= 7) {
					masteryList += tempBuilder[count] + ", ";
					count++;
				}
				jobsLore.add(ChatColor.GRAY + masteryList);
				masteryList = "";

				while (count <= 11) {
					masteryList += tempBuilder[count] + ", ";
					count++;
				}
				jobsLore.add(ChatColor.GRAY + masteryList);
				masteryList = "";

				while (count <= 15) {
					masteryList += tempBuilder[count] + ", ";
					count++;
				}
				jobsLore.add(ChatColor.GRAY + masteryList);
				masteryList = "";

				while (count <= 19) {
					masteryList += tempBuilder[count] + ", ";
					count++;
				}
				jobsLore.add(ChatColor.GRAY + masteryList);
				masteryList = "";

				while (count <= tempBuilder.length - 1) {
					masteryList += tempBuilder[count] + ", ";
					count++;
				}
				jobsLore.add(ChatColor.GRAY + masteryList);
				masteryList = "";

			}

		} else {
			jobsLore.add(ChatColor.GRAY + RunicParadise.playerProfiles.get(p.getUniqueId()).getJobMasteryString());
		}

		jobsLore.add("");
		jobsLore.add(ChatColor.GRAY + "/warp jobs " + ChatColor.WHITE + "for more info.");

		ItemStack jobsIcon = new ItemStack(Material.ANVIL, 1);
		ItemMeta jobsMeta = jobsIcon.getItemMeta();
		jobsMeta.setDisplayName(ChatColor.DARK_RED + "Job Mastery");
		jobsMeta.setLore(jobsLore);
		jobsIcon.setItemMeta(jobsMeta);

		poMenu.setItem(13, jobsIcon);

		// SKYBLOCK

		ArrayList<String> skyblockLore = new ArrayList<String>();
		skyblockLore.add("");
		skyblockLore.add(ChatColor.YELLOW + "Skyblock Rank: " + ChatColor.BLUE
				+ RunicParadise.playerProfiles.get(p.getUniqueId()).getSkyblockRank());

		int sbDays = RunicParadise.playerProfiles.get(p.getUniqueId()).getSkyblockRankNum() * 2;

		if (RunicParadise.playerProfiles.get(p.getUniqueId()).getSkyblockRankNum() > 1) {
			skyblockLore.add(ChatColor.YELLOW + "Days required for all");
			skyblockLore.add(ChatColor.YELLOW + "survival promotions reduced");
			skyblockLore.add(ChatColor.YELLOW + "by: " + ChatColor.BLUE + sbDays);
		} else {
			// Player is a Swabbie
			skyblockLore.add(ChatColor.YELLOW + "Days required for all");
			skyblockLore.add(ChatColor.YELLOW + "survival promotions reduced");
			skyblockLore.add(ChatColor.YELLOW + "by: " + ChatColor.RED + "0");
		}
		skyblockLore.add("");
		skyblockLore.add(ChatColor.GRAY + "Rank up in Skyblock to");
		skyblockLore.add(ChatColor.GRAY + "reduce more days!");
		skyblockLore.add(ChatColor.WHITE + "Use /goto for Skyblock");

		ItemStack skyblockIcon = new ItemStack(Material.FEATHER, 1);
		ItemMeta skyblockMeta = skyblockIcon.getItemMeta();
		skyblockMeta.setDisplayName(ChatColor.DARK_RED + "Skyblock Rank");
		skyblockMeta.setLore(skyblockLore);
		skyblockIcon.setItemMeta(skyblockMeta);

		poMenu.setItem(14, skyblockIcon);

		// BASIC INFO

		ArrayList<String> basicinfoLore = new ArrayList<String>();
		basicinfoLore.add("");

		int daysSinceJoin = (int) ((new Date().getTime() - this.getJoinDate().getTime()) / 86400000);
		SimpleDateFormat sdf = new SimpleDateFormat();
		sdf.applyPattern("EEE, MMM d, yyyy");
		DecimalFormat df = new DecimalFormat("#,###,###,##0");

		basicinfoLore.add(ChatColor.DARK_RED + "Joined: " + ChatColor.GRAY
				+ sdf.format(RunicParadise.playerProfiles.get(p.getUniqueId()).getJoinDate()));
		basicinfoLore.add(ChatColor.RED + "Days since joining: " + ChatColor.GRAY + df.format(daysSinceJoin));
		basicinfoLore.add(ChatColor.LIGHT_PURPLE + "Runics ($): " + ChatColor.GRAY
				+ df.format(RunicParadise.economy.getBalance(Bukkit.getOfflinePlayer(this.getPlayerID()))));
		basicinfoLore.add(ChatColor.DARK_PURPLE + "Votes: " + ChatColor.GRAY + df.format(this.getVoteCount()));

		ItemStack basicinfoIcon = new ItemStack(Material.BOOK, 1);
		ItemMeta basicinfoMeta = basicinfoIcon.getItemMeta();
		basicinfoMeta.setDisplayName(ChatColor.DARK_RED + "Player Stats");
		basicinfoMeta.setLore(basicinfoLore);
		basicinfoIcon.setItemMeta(basicinfoMeta);

		poMenu.setItem(15, basicinfoIcon);

		// CARNIVAL STATS

		ArrayList<String> carnivalLore = new ArrayList<String>();
		carnivalLore.add("");

		carnivalLore.add(ChatColor.GOLD + "Tokens: " + ChatColor.GRAY
				+ df.format(RunicParadise.playerProfiles.get(p.getUniqueId()).getTokenBalance()));
		carnivalLore.add(ChatColor.GOLD + "Lifetime Tokens: " + ChatColor.DARK_GRAY
				+ df.format(RunicParadise.playerProfiles.get(p.getUniqueId()).getLifetimeToken()));
		carnivalLore.add(ChatColor.YELLOW + "Explorer's League");
		carnivalLore.add(ChatColor.YELLOW + "Locations Found: " + ChatColor.GRAY + df.format(this.explorerLocsFound));
		carnivalLore.add(ChatColor.RED + "Maze & Parkour First");
		carnivalLore
				.add(ChatColor.RED + "Time Completions: " + ChatColor.GRAY + this.mazesAndParkoursCompletedFirstTime);

		carnivalLore.add("");
		carnivalLore.add(ChatColor.GRAY + "Type /games to learn more");
		carnivalLore.add(ChatColor.GRAY + "about Runic Carnival");

		ItemStack carnivalIcon = new ItemStack(Material.PUFFERFISH);
		ItemMeta carnivalMeta = carnivalIcon.getItemMeta();
		carnivalMeta.setDisplayName(ChatColor.DARK_RED + "Carnival Stats");
		carnivalMeta.setLore(carnivalLore);
		carnivalIcon.setItemMeta(carnivalMeta);

		poMenu.setItem(16, carnivalIcon);

		p.openInventory(poMenu);
	}
}
