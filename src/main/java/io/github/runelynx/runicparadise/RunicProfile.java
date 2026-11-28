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

	int karmaBalance, zealBalance, lifetimeZeal, voteTotal, masteredJobCount, currentJobLevel;
	String masteredJobsString, currentJob, staffRank;
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
	int activePitContribution, lifetimePitContribution;
	HashMap<EntityType, Integer> mobKillCountsMap = new HashMap<EntityType, Integer>();

	Location farmKillLoc;
	int farmKillCounter;
	Boolean isFarming;

	private static Plugin instance = RunicParadise.getInstance();

	public RunicProfile(UUID playerID) {
		loadProfile(playerID);
	}

	private void setPlayerID(UUID playerid) {
		this.playerUUID = playerid;
	}

	private UUID getPlayerID() {
		return this.playerUUID;
	}

//	void setChatColor(ChatColor newSetting, boolean updateDB) {
//		this.chatColor = newSetting.toString();
//		if (updateDB) {
//			MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
//
//			try {
//				Connection connection = MySQL.openConnection();
//
//				PreparedStatement statement = connection.prepareStatement("UPDATE rp_PlayerInfo SET ChatColor ='" + newSetting.name() + "' WHERE UUID = ?");
//				statement.setString(1, this.getPlayerID().toString());
//				statement.executeUpdate();
//
//				connection.close();
//			} catch (SQLException e) {
//				getLogger().log(Level.SEVERE, "Failed setChatColor because: " + e.getMessage());
//			}
//		}
//	}

	void setChatColor(ChatColor newSetting, boolean updateDB) {
		this.chatColor = newSetting.toString();
		if (updateDB) {
			try (Connection connection = DatabaseConnectionPool.getConnection()) {
				PreparedStatement statement = connection.prepareStatement("UPDATE rp_PlayerInfo SET ChatColor ='" + newSetting.name() + "' WHERE UUID = ?");
				statement.setString(1, this.getPlayerID().toString());
				statement.executeUpdate();
			} catch (SQLException e) {
				RunicUtilities.notifyStaffErrors("DB error. Failed setChatColor because: " + e.getMessage());
				RunicUtilities.notifyLogErrors("DB error. Failed setChatColor because: " + e.getMessage());
				DiscordSRVHook.sendMessage("plugin-errors", ":red_circle: DB error. Failed setChatColor because: " + e.getMessage());
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
				RunicUtilities.notifyStaffErrors("Failed setChatColor because: " + e.getMessage());
				RunicUtilities.notifyLogErrors("Failed setChatColor because: " + e.getMessage());
				DiscordSRVHook.sendMessage("plugin-errors", ":red_circle: Failed setChatColor because: " + e.getMessage());
				return;
			}
		}
		setChatColor(color, updateDB);
	}

	private void setStaffRank(String rank) {
		this.staffRank = rank;
	}

	public String getStaffRank() {
		return this.staffRank;
	}

	public String getChatColor() {
		return this.chatColor;
	}

	// This should only be set by the the retrieve function (this method doesn't update the DB!!)
	private void setSacrificeContribution(int amount, boolean lifetime) {
		if (lifetime) {
			this.lifetimePitContribution = amount;
		} else {
			this.activePitContribution = amount;
		}
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
			try (Connection connection = DatabaseConnectionPool.getConnection()) {
				PreparedStatement statement = connection.prepareStatement("UPDATE rp_PlayerInfo SET Gender ='" + newGender + "' WHERE UUID = ?");
				statement.setString(1, this.getPlayerID().toString());
				statement.executeUpdate();
			} catch (SQLException e) {
				RunicUtilities.notifyStaffErrors("DB error. Failed setGender because: " + e.getMessage());
				RunicUtilities.notifyLogErrors("DB error. Failed setGender because: " + e.getMessage());
				DiscordSRVHook.sendMessage("plugin-errors", "DB error. Failed setGender because: " + e.getMessage());
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

	public void setLifetimeCount(String type, int amount) {
		boolean error = false;

		switch (type) {
			case "Tokens":
				this.lifetimeTokens = amount;
				break;
			case "Zeal":
				this.lifetimeZeal = amount;
				break;
			default:
				error = true;
				break;
		}
	}

	public void setCurrency(String type, int amount) {
		boolean error = false;

		switch (type) {

			case "Souls":
				this.soulCount = amount;
				break;
			case "Karma":
				this.karmaBalance = amount;
				break;
			case "Tokens":
				this.tokenBalance = amount;
				break;
			case "Zeal":
				this.zealBalance = amount;
				break;
			default:
				error = true;
				break;
		}
	}

	public void processPitContribution (int amount) {
		int previousActiveContribution = this.activePitContribution;
		int previousLifetimeContribution = this.lifetimePitContribution;

		int interimContrib = previousActiveContribution + amount;

		if (interimContrib >= 500) {
			// Zeal is owed to the player!
			int zealToGrant = 0;

			while (interimContrib >= 500) {
				zealToGrant++;
				interimContrib = interimContrib - 500;
			}

			grantCurrency("Zeal", zealToGrant);
			this.activePitContribution = interimContrib;
			this.lifetimePitContribution = previousLifetimeContribution + amount;



			MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);

			try {
				Connection connection = MySQL.openConnection();
				Statement statement = connection.createStatement();
				statement.executeUpdate("UPDATE `rp_Faith_PlayerSacrifices` SET `LifetimeContribution`  = " + this.lifetimePitContribution
						+ " WHERE UUID= '" + this.getPlayerID() + "';");

				statement.executeUpdate("UPDATE `rp_Faith_PlayerSacrifices` SET `ActiveContribution`  = " + this.activePitContribution
						+ " WHERE UUID= '" + this.getPlayerID() + "';");

				statement.executeUpdate("UPDATE `rp_Faith_PlayerSacrifices` SET `LastContribution`  = " + (new Date().getTime())
						+ " WHERE UUID= '" + this.getPlayerID() + "';");

				connection.close();

			} catch (SQLException e) {

				RunicUtilities.notifyStaffErrors("Failed processPitContribution update (change " + this.playerName + "  because: " + e.getMessage());
				RunicUtilities.notifyLogErrors("Failed processPitContribution update (change " + this.playerName + "  because: " + e.getMessage());
			}


		}

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
			this.soulCount = this.getSoulCount() + amount;

			RunicMessaging.sendMessage(Bukkit.getPlayer(this.getPlayerID()), RunicMessaging.RunicFormat.AFTERLIFE,
					"Gained " + amount + " soul" + plural);

			break;
		case "Karma":
			column = "Karma";
			this.karmaBalance = this.getKarmaBalance() + amount;

			RunicMessaging.sendMessage(Bukkit.getPlayer(this.getPlayerID()), RunicMessaging.RunicFormat.FAITH,
					"Gained " + amount + " karma");

			break;
		case "Tokens":
			column = "Tokens";
			this.tokenBalance = this.getTokenBalance() + amount;
			this.setLifetimeCount("Tokens", this.lifetimeTokens + amount);

			RunicMessaging.sendMessage(Bukkit.getPlayer(this.getPlayerID()), RunicMessaging.RunicFormat.CASINO,
					"Gained " + amount + " token" + plural);
			break;
		case "Zeal":
			column = "Zeal";
			this.zealBalance = this.zealBalance + amount;
			this.setLifetimeCount("Zeal", this.lifetimeZeal + amount);

			RunicMessaging.sendMessage(Bukkit.getPlayer(this.getPlayerID()), RunicMessaging.RunicFormat.FAITH,
					"Gained " + amount + " zeal");

			break;
		default:
			error = true;
			break;
		}

		try (Connection connection = DatabaseConnectionPool.getConnection()) {
			String sql = "UPDATE `rp_PlayerInfo` SET " + column + " = " + column + " + ? WHERE UUID= ?";
			PreparedStatement statement = connection.prepareStatement(sql);
			statement.setInt(1, amount);
			statement.setString(2, this.getPlayerID().toString());
			statement.executeUpdate();

			if (column.equals("Tokens")) {
				PreparedStatement statement2 = connection.prepareStatement("UPDATE `rp_PlayerInfo` SET LifetimeTokens = LifetimeTokens + ? WHERE UUID= '" + this.getPlayerID() + "';");
				statement2.setInt(1, amount);
				statement2.executeUpdate();
			}

		} catch (SQLException e) {
			RunicUtilities.notifyStaffErrors("Failed grantCurrency update (change " + this.playerName + "  because: " + e.getMessage());
			RunicUtilities.notifyLogErrors("Failed grantCurrency update (change " + this.playerName + "  because: " + e.getMessage());
			DiscordSRVHook.sendMessage("plugin-errors", "Failed grantCurrency update (change " + this.playerName + "  because: " + e.getMessage());
		}

		// Sync up old profile method ... for now ...
		new RunicPlayerBukkit(this.getPlayerID()).refreshPlayerObject(Bukkit.getOfflinePlayer(this.getPlayerID()));
	}

	public void reduceCurrency(String type, int amount) {
		boolean error = false;
		String column = "";
		String plural = "";

		if (amount > 1) {
			plural = "s";
		}

		switch (type) {

			case "Souls":
				column = "SoulCount";
				this.setCurrency("Souls", this.soulCount - amount);

				RunicMessaging.sendMessage(Bukkit.getPlayer(this.getPlayerID()), RunicMessaging.RunicFormat.AFTERLIFE,
						"Lost " + amount + " soul" + plural);

				break;
			case "Karma":
				column = "Karma";
				this.setCurrency("Karma", this.karmaBalance - amount);

				RunicMessaging.sendMessage(Bukkit.getPlayer(this.getPlayerID()), RunicMessaging.RunicFormat.FAITH,
						"Lost " + amount + " karma");

				break;
			case "Tokens":
				column = "Tokens";
				this.setCurrency("Tokens", this.tokenBalance - amount);

				RunicMessaging.sendMessage(Bukkit.getPlayer(this.getPlayerID()), RunicMessaging.RunicFormat.CASINO,
						"Lost " + amount + " token" + plural);

				break;
			case "Zeal":
				column = "Zeal";
				this.setCurrency("Zeal", this.zealBalance - amount);

				RunicMessaging.sendMessage(Bukkit.getPlayer(this.getPlayerID()), RunicMessaging.RunicFormat.FAITH,
						"Lost " + amount + " zeal");

				break;
			default:
				error = true;
				break;
		}

		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);

		try {
			Connection connection = MySQL.openConnection();
			Statement statement = connection.createStatement();
			statement.executeUpdate("UPDATE `rp_PlayerInfo` SET " + column + " = " + column + " - " + amount
					+ " WHERE UUID= '" + this.getPlayerID() + "';");

			connection.close();

		} catch (SQLException e) {
			RunicUtilities.notifyStaffErrors("Failed reduceCurrency update (change " + this.playerName + "  because: " + e.getMessage());
			RunicUtilities.notifyLogErrors("Failed reduceCurrency update (change " + this.playerName + "  because: " + e.getMessage());
		}

		// Sync up old profile method ... for now ...
		new RunicPlayerBukkit(this.getPlayerID()).refreshPlayerObject(Bukkit.getOfflinePlayer(this.getPlayerID()));
	}

//	private void setKarmaBalance(int newKarma) {
//		this.karmaBalance = newKarma;
//	}

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

	public int getKarmaBalance() {
		return this.karmaBalance;
	}

//	public void setTokenBalance(int newToken) {
//		this.tokenBalance = newToken;
//	}

    public int getTokenBalance() {
		return this.tokenBalance;
	}

//	private void setLifetimeToken(int lifetimeTokens) {
//		this.lifetimeTokens = lifetimeTokens;
//	}

	private int getLifetimeToken() {
		return this.lifetimeTokens;
	}

	public void setJobMasteryCount(int jobMasteryCount) {
		this.masteredJobCount = jobMasteryCount;

		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
		Connection connection = MySQL.openConnection();
		try {
			PreparedStatement statement = connection.prepareStatement("UPDATE rp_PlayerInfo SET JobsMasteredCount = ? WHERE UUID = ?");
			statement.setInt(1, jobMasteryCount);
			statement.setString(2, this.getPlayerID().toString());
			statement.executeUpdate();

		} catch (SQLException e) {

			RunicUtilities.notifyStaffErrors("Failed setJobMasteryCount because: " + e.getMessage());
			RunicUtilities.notifyLogErrors("Failed setJobMasteryCount because: " + e.getMessage());
		}

		try {
			connection.close();
		} catch (SQLException e) {
			RunicUtilities.notifyStaffErrors("Failed setJobMasteryCount because: " + e.getMessage());
			RunicUtilities.notifyLogErrors("Failed setJobMasteryCount because: " + e.getMessage());
		}

	}

	public int getJobMasteryCount() {
		return this.masteredJobCount;
	}

	public void setJobMasteryString(String jobMasteryString) {
		this.masteredJobsString = jobMasteryString;

		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
		Connection connection = MySQL.openConnection();

			try {
					PreparedStatement statement = connection.prepareStatement("UPDATE rp_PlayerInfo SET JobsMastered='"
							+ jobMasteryString + "' WHERE UUID = ?");
					statement.setString(1, this.getPlayerID().toString());
					statement.executeUpdate();

			} catch (SQLException e) {

				RunicUtilities.notifyStaffErrors("Failed setJobMasteryString because: " + e.getMessage());
				RunicUtilities.notifyLogErrors("Failed setJobMasteryString because: " + e.getMessage());
			}

		try {
			connection.close();
		} catch (SQLException e) {
			RunicUtilities.notifyStaffErrors("Failed setJobMasteryString because: " + e.getMessage());
			RunicUtilities.notifyLogErrors("Failed setJobMasteryString because: " + e.getMessage());
		}

	}

	public String getJobMasteryString() {
		return this.masteredJobsString;
	}

//	public void setSoulCount(int newSouls) {
//		// Plajer's DeathChest plugin uses this!! Don't change name or parameters.
//		this.soulCount = newSouls;
//
//		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
//
//		try {
//			Connection connection = MySQL.openConnection();
//
//			PreparedStatement statement = connection.prepareStatement("UPDATE rp_PlayerInfo SET SoulCount =" + newSouls + " WHERE UUID = ?");
//			statement.setString(1, this.getPlayerID().toString());
//			statement.executeUpdate();
//
//			connection.close();
//		} catch (SQLException e) {
//			getLogger().log(Level.SEVERE, "Failed setSoulCount because: " + e.getMessage());
//		}
//		Bukkit.getLogger().log(Level.INFO, "RunicProfile: Updated soul count to " + newSouls + " for player " + this.getPlayerName(false));
//
//	}

    public int getSoulCount() {
		// Plajer's DeathChest plugin uses this!! Don't change name or parameters.
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

			RunicUtilities.notifyStaffErrors("Failed logSpecialRankDrop " + e.getMessage());
			RunicUtilities.notifyLogErrors("Failed logSpecialRankDrop " + e.getMessage());
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

			RunicUtilities.notifyStaffErrors("Failed getCountGraves because: " + e.getMessage());
			RunicUtilities.notifyLogErrors("Failed getCountGraves because: " + e.getMessage());
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

		try (Connection connection = MySQL.openConnection()) { // auto-closed (it never was)
			// TODO: Get data from DB based on UUID to protect vs name changes
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
			RunicUtilities.notifyStaffErrors("Failed DB check [refreshPlayerObject] because: " + e.getMessage());
			RunicUtilities.notifyLogErrors("Failed DB check [refreshPlayerObject] because: " + e.getMessage());
		}
	}

	private void retrieveBasicData() {
		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
		String tempGender = "";
		String tempStaffRank = "";

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

			PreparedStatement dStmt4 = connection
					.prepareStatement("SELECT ActiveContribution, LifetimeContribution FROM rp_Faith_PlayerSacrifices WHERE UUID = ?;");
			dStmt4.setString(1, this.getPlayerID().toString());
			ResultSet sacrificeData = dStmt4.executeQuery();

			if (faithData.isBeforeFirst()) {
				faithData.next();
				this.setFaithPowerLevel(faithData.getInt("FPL"));
			} else {
				this.setFaithPowerLevel(0);
			}

			if (sacrificeData.isBeforeFirst()) {
				sacrificeData.next();
				this.setSacrificeContribution(sacrificeData.getInt("ActiveContribution"), false);
				this.setSacrificeContribution(sacrificeData.getInt("LifetimeContribution"), true);
			} else {
				//  ???
			}

			if (!playerData.isBeforeFirst()) {
				// Player doesn't exist in the DB!
				// TODO: Need to add them to DB!
				this.tokenBalance = 0;
				getLogger().log(Level.INFO, "Player " + this.playerName + " isn't in our DB yet.");
			} else {
				// Player does exist in the DB

				playerData.next();
				if (playerData.getString("StaffRank") == null) {
					tempStaffRank = "None";
				} else {
					tempStaffRank = playerData.getString("StaffRank");
				}
				this.setStaffRank(tempStaffRank);
				this.setPlayerIP(playerData.getString("LastIP"));
				this.setCurrency("Karma", (playerData.getInt("Karma")));
				this.setCurrency("Tokens", (playerData.getInt("Tokens")));
				this.setCurrency("Souls", (playerData.getInt("SoulCount")));
				this.setCurrency("Zeal", (playerData.getInt("Zeal")));
				this.setLifetimeCount("Tokens", playerData.getInt("LifetimeTokens"));
				this.setLifetimeCount("Zeal", playerData.getInt("LifetimeZeal"));
				this.setJobMasteryString(playerData.getString("JobsMastered"));
				this.setJobMasteryCount(playerData.getInt("JobsMasteredCount"));
				this.setJoinDate(new Date(playerData.getLong("FirstSeen")));
				this.setVoteCount(playerData.getInt("Votes"));
				this.setActiveFaith(playerData.getString("ActiveFaith"));
				if (playerData.getString("Gender") == null) {
					tempGender = "M";
				} else {
					tempGender = playerData.getString("Gender");
				}
				this.changeGender(tempGender, false);
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

		try (Connection connection = MySQL.openConnection()) { // auto-closed (it never was)

			PreparedStatement statement = connection.prepareStatement("SELECT * FROM rp_PlayerMobKills WHERE UUID = ?;");
			statement.setString(1, this.getPlayerID().toString());
			ResultSet playerData = statement.executeQuery();

			while (playerData.next()) {
				// A fresh start
				this.mobKillCountsMap.clear();

				// Load data!
				// Load every tracked mob from its column (one list: RunicParadise.trackableEntityKillsMap)
				for (Map.Entry<EntityType, Integer> tracked : RunicParadise.trackableEntityKillsMap.entrySet()) {
					if (tracked.getValue() > 0) {
						this.mobKillCountsMap.put(tracked.getKey(), playerData.getInt(killColumn(tracked.getKey())));
					}
				}
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

	public void addMazeCompletion(int puzzleID) throws SQLException {

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

			int prizeKarma = 0;
			int prizeSouls = 0;
			int prizeRunics = 0;
			int prizeTokens = 0;
			double prizeAdjust = 1;

			if (!mzResult.isBeforeFirst()) {
				// No results, invalid puzzleID given

				p.sendMessage(ChatColor.GRAY
						+ "This maze has been configured incorrectly. Ask an admin to check the game ID# in the command block");
				connection.close();
				return;

			} else {
				// results found!
				mzResult.next();
				// 2026-11-26: a game closed by staff (/rs games) neither counts nor pays; staff "Give completion" overrides.
				if ("Closed".equalsIgnoreCase(mzResult.getString("Status")) && !GamesAdmin.override) {
					p.sendMessage(GamesAdmin.closedText(mzResult.getString("GameName"), mzResult.getString("ClosedReason"), true));
					connection.close();
					return;
				}
				prizeKarma = mzResult.getInt("KarmaReward");
				prizeSouls = mzResult.getInt("SoulReward");
				prizeRunics = mzResult.getInt("CashReward");
				prizeTokens = mzResult.getInt("TokenReward");
				prizeAdjust = mzResult.getDouble("CashRepeatAdjust");
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
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "give " + p.getName() + " wither_skeleton_skull 1");
					p.sendMessage(ChatColor.GOLD + "You received a wither skull for first completion here!");
				}

				if (mzResult.getInt("ID") == 4) {
					// Jungle Maze
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "give " + p.getName() + " totem_of_undying 1");
					p.sendMessage(ChatColor.GOLD + "You received a prize for first completion here!");
				}


				if (mzResult.getInt("ID") == 28) {
					// Cake Maze
					p.getInventory().addItem(Recipes.customItemStacks("FC_EXTREME_MAZE_1"));
					p.sendMessage(ChatColor.GOLD + "You received a trophy for first completion here!");
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

				if (mzResult.getInt("ChestReward") == 1) {
					int x = mzResult.getInt("ChestLocationX");
					int y = mzResult.getInt("ChestLocationY");
					int z = mzResult.getInt("ChestLocationZ");
					String rewardWorld = "RunicSky";

					if (rewardWorld != null) {
						Location chestLocation = new Location(Bukkit.getWorld(rewardWorld), x, y, z);
						ItemStack[] rewards = Commands.carnivalChestReward(chestLocation);

						for (ItemStack i : rewards) {
							if (i != null && i.getType() != null && i.getType() != Material.AIR) {
								p.getWorld().dropItemNaturally(p.getLocation(), i);
							}
						}

						p.sendMessage(ChatColor.GOLD + "Congratulations! You've earned a special reward for this first-time completion!");

					} else {
						p.sendMessage(ChatColor.RED + "Error: Reward chest world not found. Please notify an admin.");
					}
				}

				if (mzResult.getInt("AnnounceFirstCompletion") == 1) {
					for (Player q : Bukkit.getOnlinePlayers()) {
						Bukkit.dispatchCommand(
								Bukkit.getConsoleSender(),
								"cmi titlemsg all " + ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + p.getName() +
										" %subtitle% " + ChatColor.GRAY + "just completed the " + mzResult.getString("GameName") + " for the first time!"
						);
					}
				}

//				if (mzResult.getInt("ID") == 21) {
//					// Anguish Maze
//
//					ItemStack[] rewards = Commands
//							.carnivalChestReward(new Location(Bukkit.getWorld("RunicSky"), 1098, 121, 1166));
//
//					for (ItemStack i : rewards) {
//						if ( i != null && i.getType() != null && i.getType() != Material.AIR) {
//
//							p.getWorld().dropItemNaturally(p.getLocation(), i);
//						}
//					}
//
//					p.sendMessage(ChatColor.GOLD
//							+ "Congratulations! You've earned a special reward for this first-time completion!");
//					for (Player q : Bukkit.getOnlinePlayers()) {
//						Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "cmi titlemsg all "+ ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + p.getName() +" %subtitle% "+ ChatColor.GRAY
//								+ "just completed the " + mzResult.getString("GameName")	+ " for the first time!");
//
//					}
//				} else 	if (mzResult.getInt("ID") == 22) {
//					// Heart of Anguish
//					p.sendMessage(ChatColor.GOLD
//							+ "Congratulations! You've earned a special reward for this first-time completion!");
//					ItemStack[] rewards = Commands
//							.carnivalChestReward(new Location(Bukkit.getWorld("RunicSky"), 1098, 121, 1162));
//
//					for (ItemStack i : rewards) {
//						if ( i != null && i.getType() != null && i.getType() != Material.AIR) {
//
//							p.getWorld().dropItemNaturally(p.getLocation(), i);
//						}
//					}
//
//					for (Player q : Bukkit.getOnlinePlayers()) {
//						Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "cmi titlemsg all "+ ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + p.getName() +" %subtitle% "+ ChatColor.GRAY + "has freed the Souls of Anguish!");
//					}
//				}  else 	if (mzResult.getInt("ID") == 32) {  //Glitch Maze
//					// Heart of Anguish
//					p.sendMessage(ChatColor.GOLD
//							+ "Congratulations! You've earned a special reward for this first-time completion!");
//					ItemStack[] rewards = Commands
//							.carnivalChestReward(new Location(Bukkit.getWorld("RunicSky"), -1043, 22, 14));
//
//					for (ItemStack i : rewards) {
//						if ( i != null && i.getType() != null && i.getType() != Material.AIR) {
//
//							p.getWorld().dropItemNaturally(p.getLocation(), i);
//						}
//					}
//
//					for (Player q : Bukkit.getOnlinePlayers()) {
//						Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "cmi titlemsg all "+ ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + p.getName() +" %subtitle% "+ ChatColor.YELLOW + "has debugged the Glitch Maz"+
//								ChatColor.MAGIC + ChatColor.DARK_RED
//								+"e!1234561"+ ChatColor.RESET +"" + ChatColor.YELLOW + "" + ChatColor.BOLD + "" + ChatColor.ITALIC + " Uh oh...");
//					}
//				}else {
//
//					for (Player q : Bukkit.getOnlinePlayers()) {
//						Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "cmi titlemsg all "+ ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + p.getName() +" %subtitle% "+ ChatColor.GRAY
//								+ "just completed the " + mzResult.getString("GameName")	+ " for the first time!");
//					}
//				}

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

	/** DB column for a tracked mob. Zombified piglins keep their historical "PIG_ZOMBIE" column. */
	static String killColumn(EntityType type) {
		return type == EntityType.ZOMBIFIED_PIGLIN ? "PIG_ZOMBIE" : type.name();
	}

	private static final java.util.regex.Pattern KILL_LINE = java.util.regex.Pattern
			.compile("^" + ChatColor.BLUE + "(.+?): " + ChatColor.WHITE + "(\\d+)(.*)$");

	/**
	 * Kill-count tooltip polish: "1,002" separators, zero counts dimmed, then a category total and
	 * how many of the category's mobs the player has killed at least once.
	 */
	static List<String> polishKillLore(List<String> lore) {
		List<String> out = new ArrayList<>();
		long total = 0;
		int types = 0;
		int discovered = 0;
		for (String line : lore) {
			java.util.regex.Matcher m = KILL_LINE.matcher(line);
			if (!m.matches()) {
				out.add(line);
				continue;
			}
			long n = Long.parseLong(m.group(2));
			types++;
			total += n;
			if (n > 0) {
				discovered++;
				out.add(ChatColor.BLUE + m.group(1) + ": " + ChatColor.WHITE + String.format("%,d", n) + m.group(3));
			} else {
				out.add(ChatColor.DARK_GRAY + m.group(1) + ": 0" + ChatColor.stripColor(m.group(3)));
			}
		}
		if (types > 0) {
			out.add("");
			out.add(ChatColor.GOLD + "Total: " + ChatColor.WHITE + String.format("%,d", total));
			out.add(ChatColor.GOLD + "Discovered: " + ChatColor.WHITE + discovered + "/" + types);
		}
		return out;
	}

	/** Kill count for a mob type (0 if none / not tracked). */
	public int getKillCount(EntityType type) {
		return this.mobKillCountsMap.getOrDefault(type, 0);
	}

	public Boolean trackMobKill(EntityDeathEvent mobKillEvent) {
		// Untracked types (and anything not in the map) are simply ignored.
		// (Was map.get(...) unboxed directly: newer mobs like Drowned or Phantom threw a NullPointerException.)
		EntityType type = mobKillEvent.getEntityType();
		if (RunicParadise.trackableEntityKillsMap.getOrDefault(type, 0) == 0) {
			return false;
		}

		// Only kills by a player, outside plotworld (this compared a World to a String, so it never excluded it)
		if (mobKillEvent.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent
				&& mobKillEvent.getEntity().getKiller() != null
				&& !mobKillEvent.getEntity().getWorld().getName().equalsIgnoreCase("plotworld")) {

			// Pending increments, added to the DB total on save (quit / shutdown)
			RunicParadise.mobKillTracker.computeIfAbsent(this.getPlayerID(), k -> new HashMap<>())
					.merge(type, 1, Integer::sum);

			// In-memory running total, shown in the profile menu and used for rank checks
			this.mobKillCountsMap.merge(type, 1, Integer::sum);
			return true;
		}
		return false;
	}

	public boolean saveMobKillsForPlayer() {
		HashMap<EntityType, Integer> pending = RunicParadise.mobKillTracker.get(this.getPlayerID());
		if (pending == null || pending.isEmpty()) {
			return true;
		}

		// One UPDATE for all pending types. (Was one per type, and the first failure - e.g. PIGLIN, which had no
		// column - aborted the rest, so those kills were never saved.)
		StringBuilder sql = new StringBuilder("UPDATE rp_PlayerMobKills SET ");
		List<EntityType> types = new ArrayList<>();
		for (Map.Entry<EntityType, Integer> entry : pending.entrySet()) {
			if (entry.getValue() == null || entry.getValue() == 0
					|| RunicParadise.trackableEntityKillsMap.getOrDefault(entry.getKey(), 0) == 0) {
				continue;
			}
			String col = killColumn(entry.getKey());
			if (!types.isEmpty()) {
				sql.append(", ");
			}
			sql.append('`').append(col).append("` = `").append(col).append("` + ?");
			types.add(entry.getKey());
		}
		if (types.isEmpty()) {
			pending.clear();
			return true;
		}
		sql.append(" WHERE UUID = ?");

		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
		try (Connection connection = MySQL.openConnection();
			 PreparedStatement statement = connection.prepareStatement(sql.toString())) {
			int i = 1;
			for (EntityType t : types) {
				statement.setInt(i++, pending.get(t));
			}
			statement.setString(i, this.getPlayerID().toString());
			statement.executeUpdate();
			pending.clear();
			return true;
		} catch (SQLException e) {
			getLogger().log(Level.SEVERE, "Failed saveMobKillsForPlayer because: " + e.getMessage());
			return false;
		}
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
		farmLore.add(ChatColor.BLUE + "Cows: " + ChatColor.WHITE + this.getKillCount(EntityType.COW));
		farmLore.add(ChatColor.BLUE + "Chickens: " + ChatColor.WHITE + this.getKillCount(EntityType.CHICKEN));
		farmLore.add(ChatColor.BLUE + "Mushroom Cows: " + ChatColor.WHITE
				+ this.getKillCount(EntityType.MUSHROOM_COW));
		farmLore.add(ChatColor.BLUE + "Pigs: " + ChatColor.WHITE + this.getKillCount(EntityType.PIG));
		farmLore.add(ChatColor.BLUE + "Rabbits: " + ChatColor.WHITE + this.getKillCount(EntityType.RABBIT));
		farmLore.add(ChatColor.BLUE + "Sheep: " + ChatColor.WHITE + this.getKillCount(EntityType.SHEEP));
		farmLore.add(ChatColor.BLUE + "Goats: " + ChatColor.WHITE + this.getKillCount(EntityType.GOAT));

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
		wildLore.add(ChatColor.BLUE + "Bats: " + ChatColor.WHITE + this.getKillCount(EntityType.BAT));
		wildLore.add(ChatColor.BLUE + "Llamas: " + ChatColor.WHITE + this.getKillCount(EntityType.LLAMA));
		wildLore.add(ChatColor.BLUE + "Ocelots: " + ChatColor.WHITE + this.getKillCount(EntityType.OCELOT));
		wildLore.add(
				ChatColor.BLUE + "Polar Bears: " + ChatColor.WHITE + this.getKillCount(EntityType.POLAR_BEAR));
		wildLore.add(ChatColor.BLUE + "Wolves: " + ChatColor.WHITE + this.getKillCount(EntityType.WOLF));
		wildLore.add(ChatColor.BLUE + "Foxes: " + ChatColor.WHITE + this.getKillCount(EntityType.FOX));
		wildLore.add(ChatColor.BLUE + "Cats: " + ChatColor.WHITE + this.getKillCount(EntityType.CAT));
		wildLore.add(ChatColor.BLUE + "Pandas: " + ChatColor.WHITE + this.getKillCount(EntityType.PANDA));
		wildLore.add(ChatColor.BLUE + "Parrots: " + ChatColor.WHITE + this.getKillCount(EntityType.PARROT));
		wildLore.add(ChatColor.BLUE + "Bees: " + ChatColor.WHITE + this.getKillCount(EntityType.BEE));
		wildLore.add(ChatColor.BLUE + "Frogs: " + ChatColor.WHITE + this.getKillCount(EntityType.FROG));
		wildLore.add(ChatColor.BLUE + "Trader Llamas: " + ChatColor.WHITE + this.getKillCount(EntityType.TRADER_LLAMA));
		wildLore.add(ChatColor.BLUE + "Sniffers: " + ChatColor.WHITE + this.getKillCount(EntityType.SNIFFER));
		wildLore.add(ChatColor.BLUE + "Allays: " + ChatColor.WHITE + this.getKillCount(EntityType.ALLAY));

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
		horseLore.add(ChatColor.BLUE + "Donkeys: " + ChatColor.WHITE + this.getKillCount(EntityType.DONKEY));
		horseLore.add(ChatColor.BLUE + "Horses: " + ChatColor.WHITE + this.getKillCount(EntityType.HORSE));
		horseLore.add(ChatColor.BLUE + "Mules: " + ChatColor.WHITE + this.getKillCount(EntityType.MULE));
		horseLore.add(ChatColor.BLUE + "Skeleton Horses: " + ChatColor.WHITE
				+ this.getKillCount(EntityType.SKELETON_HORSE));
		horseLore.add(ChatColor.BLUE + "Zombie Horses: " + ChatColor.WHITE
				+ this.getKillCount(EntityType.ZOMBIE_HORSE));
		horseLore.add(ChatColor.BLUE + "Camels: " + ChatColor.WHITE + this.getKillCount(EntityType.CAMEL));

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
				+ this.getKillCount(EntityType.ELDER_GUARDIAN));
		oceanLore
				.add(ChatColor.BLUE + "Guardians: " + ChatColor.WHITE + this.getKillCount(EntityType.GUARDIAN));
		oceanLore.add(ChatColor.BLUE + "Squids: " + ChatColor.WHITE + this.getKillCount(EntityType.SQUID));
		oceanLore.add(ChatColor.BLUE + "Glow Squids: " + ChatColor.WHITE + this.getKillCount(EntityType.GLOW_SQUID));
		oceanLore.add(ChatColor.BLUE + "Dolphins: " + ChatColor.WHITE + this.getKillCount(EntityType.DOLPHIN));
		oceanLore.add(ChatColor.BLUE + "Turtles: " + ChatColor.WHITE + this.getKillCount(EntityType.TURTLE));
		oceanLore.add(ChatColor.BLUE + "Axolotls: " + ChatColor.WHITE + this.getKillCount(EntityType.AXOLOTL));
		oceanLore.add(ChatColor.BLUE + "Fish: " + ChatColor.WHITE + (this.getKillCount(EntityType.COD)
				+ this.getKillCount(EntityType.SALMON) + this.getKillCount(EntityType.PUFFERFISH)
				+ this.getKillCount(EntityType.TROPICAL_FISH)) + ChatColor.GRAY + " (cod, salmon, puffer, tropical)");

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
		netherLore.add(ChatColor.BLUE + "Blazes: " + ChatColor.WHITE + this.getKillCount(EntityType.BLAZE));
		netherLore.add(ChatColor.BLUE + "Ghasts: " + ChatColor.WHITE + this.getKillCount(EntityType.GHAST));
		netherLore.add(
				ChatColor.BLUE + "Magma Cubes: " + ChatColor.WHITE + this.getKillCount(EntityType.MAGMA_CUBE));
		// "Piglins" here used to show the old zombie pigman column; zombified piglins are now listed correctly
		netherLore.add(ChatColor.BLUE + "Zombified Piglins: " + ChatColor.WHITE
				+ this.getKillCount(EntityType.ZOMBIFIED_PIGLIN));
		netherLore.add(
				ChatColor.BLUE + "Piglins: " + ChatColor.WHITE + this.getKillCount(EntityType.PIGLIN));
		netherLore.add(ChatColor.BLUE + "Piglin Brutes: " + ChatColor.WHITE + this.getKillCount(EntityType.PIGLIN_BRUTE));
		netherLore.add(ChatColor.BLUE + "Hoglins: " + ChatColor.WHITE + this.getKillCount(EntityType.HOGLIN));
		netherLore.add(ChatColor.BLUE + "Zoglins: " + ChatColor.WHITE + this.getKillCount(EntityType.ZOGLIN));
		netherLore.add(ChatColor.BLUE + "Striders: " + ChatColor.WHITE + this.getKillCount(EntityType.STRIDER));
		netherLore.add(ChatColor.BLUE + "Withers: " + ChatColor.WHITE + this.getKillCount(EntityType.WITHER));

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
		endLore.add(ChatColor.BLUE + "Endermen: " + ChatColor.WHITE + this.getKillCount(EntityType.ENDERMAN));
		endLore.add(ChatColor.BLUE + "Ender Dragons: " + ChatColor.WHITE
				+ this.getKillCount(EntityType.ENDER_DRAGON));
		endLore.add(ChatColor.BLUE + "Shulkers: " + ChatColor.WHITE + this.getKillCount(EntityType.SHULKER));

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
		peopleLore.add(ChatColor.BLUE + "Evokers: " + ChatColor.WHITE + this.getKillCount(EntityType.EVOKER));
		peopleLore.add(
				ChatColor.BLUE + "Iron Golems: " + ChatColor.WHITE + this.getKillCount(EntityType.IRON_GOLEM));
		peopleLore.add(ChatColor.BLUE + "Snowmen: " + ChatColor.WHITE + this.getKillCount(EntityType.SNOWMAN));
		peopleLore.add(ChatColor.BLUE + "Vex: " + ChatColor.WHITE + this.getKillCount(EntityType.VEX));
		peopleLore
				.add(ChatColor.BLUE + "Villagers: " + ChatColor.WHITE + this.getKillCount(EntityType.VILLAGER));
		peopleLore.add(
				ChatColor.BLUE + "Vindicators: " + ChatColor.WHITE + this.getKillCount(EntityType.VINDICATOR));
		peopleLore.add(ChatColor.BLUE + "Witches: " + ChatColor.WHITE + this.getKillCount(EntityType.WITCH));
		peopleLore.add(ChatColor.BLUE + "Pillagers: " + ChatColor.WHITE + this.getKillCount(EntityType.PILLAGER));
		peopleLore.add(ChatColor.BLUE + "Ravagers: " + ChatColor.WHITE + this.getKillCount(EntityType.RAVAGER));
		peopleLore.add(ChatColor.BLUE + "Illusioners: " + ChatColor.WHITE + this.getKillCount(EntityType.ILLUSIONER));
		peopleLore.add(ChatColor.BLUE + "Wandering Traders: " + ChatColor.WHITE
				+ this.getKillCount(EntityType.WANDERING_TRADER));

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
				.add(ChatColor.BLUE + "Creepers: " + ChatColor.WHITE + this.getKillCount(EntityType.CREEPER));
		basemobsLore.add(ChatColor.BLUE + "Giants: " + ChatColor.WHITE + this.getKillCount(EntityType.GIANT));
		basemobsLore.add(ChatColor.BLUE + "Slime: " + ChatColor.WHITE + this.getKillCount(EntityType.SLIME));
		basemobsLore.add(ChatColor.BLUE + "Phantoms: " + ChatColor.WHITE + this.getKillCount(EntityType.PHANTOM));
		basemobsLore.add(ChatColor.BLUE + "Wardens: " + ChatColor.WHITE + this.getKillCount(EntityType.WARDEN));

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
				+ this.getKillCount(EntityType.CAVE_SPIDER));
		spiderLore.add(
				ChatColor.BLUE + "Endermites: " + ChatColor.WHITE + this.getKillCount(EntityType.ENDERMITE));
		spiderLore.add(
				ChatColor.BLUE + "Silverfish: " + ChatColor.WHITE + this.getKillCount(EntityType.SILVERFISH));
		spiderLore.add(ChatColor.BLUE + "Spiders: " + ChatColor.WHITE + this.getKillCount(EntityType.SPIDER));

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

		zombieLore.add(ChatColor.BLUE + "Husks: " + ChatColor.WHITE + this.getKillCount(EntityType.HUSK));
		zombieLore.add(ChatColor.BLUE + "Zombies: " + ChatColor.WHITE + this.getKillCount(EntityType.ZOMBIE));
		zombieLore.add(ChatColor.BLUE + "Zombie Villagers: " + ChatColor.WHITE
				+ this.getKillCount(EntityType.ZOMBIE_VILLAGER));
		zombieLore.add(ChatColor.BLUE + "Drowned: " + ChatColor.WHITE + this.getKillCount(EntityType.DROWNED));

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
				.add(ChatColor.BLUE + "Skeletons: " + ChatColor.WHITE + this.getKillCount(EntityType.SKELETON));
		skellyLore.add(ChatColor.BLUE + "Strays: " + ChatColor.WHITE + this.getKillCount(EntityType.STRAY));
		skellyLore.add(ChatColor.BLUE + "Wither Skeletons: " + ChatColor.WHITE
				+ this.getKillCount(EntityType.WITHER_SKELETON));

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

		// Polish every category's tooltip: thousands separators, dim zero counts, category total and
		// "Discovered a/b"; plus the all-time total on the center head.
		for (int slot = 0; slot < kcMenu.getSize(); slot++) {
			if (slot == 4) {
				continue;
			}
			ItemStack item = kcMenu.getItem(slot);
			if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) {
				continue;
			}
			ItemMeta im = item.getItemMeta();
			im.setLore(polishKillLore(im.getLore()));
			item.setItemMeta(im);
		}
		ItemStack center = kcMenu.getItem(4);
		if (center != null && center.hasItemMeta()) {
			ItemMeta cm = center.getItemMeta();
			List<String> lore = cm.hasLore() ? new ArrayList<>(cm.getLore()) : new ArrayList<>();
			long allTime = 0;
			for (int v : this.mobKillCountsMap.values()) {
				allTime += v;
			}
			lore.add("");
			lore.add(ChatColor.GOLD + "All-time kills: " + ChatColor.WHITE + String.format("%,d", allTime));
			cm.setLore(lore);
			center.setItemMeta(cm);
		}

		p.openInventory(kcMenu);

	}

	/** A chat colour choice in the Chat Options menu. rank == null means permission (donor) only. */
	static final class ChatColorOption {
		final int slot; final ChatColor color; final Material wool; final String name; final String perm; final String rank;
		ChatColorOption(int slot, ChatColor color, Material wool, String name, String perm, String rank) {
			this.slot = slot; this.color = color; this.wool = wool; this.name = name; this.perm = perm; this.rank = rank;
		}
		String rankLabel() {
			return rank.substring(0, 1).toUpperCase() + rank.substring(1);
		}
	}

	static final List<ChatColorOption> CHAT_COLOR_OPTIONS = Arrays.asList(
			new ChatColorOption(28, ChatColor.GREEN, Material.LIME_WOOL, "Green", "rp.chatcolors.green", "seeker"),
			new ChatColorOption(29, ChatColor.DARK_GREEN, Material.GREEN_WOOL, "Dark Green", "rp.chatcolors.dark_green", "runner"),
			new ChatColorOption(30, ChatColor.YELLOW, Material.YELLOW_WOOL, "Yellow", "rp.chatcolors.yellow", "singer"),
			new ChatColorOption(31, ChatColor.GOLD, Material.ORANGE_WOOL, "Gold", "rp.chatcolors.gold", "brawler"),
			new ChatColorOption(32, ChatColor.AQUA, Material.LIGHT_BLUE_WOOL, "Aqua", "rp.chatcolors.aqua", "keeper"),
			new ChatColorOption(33, ChatColor.DARK_AQUA, Material.CYAN_WOOL, "Dark Aqua", "rp.chatcolors.dark_aqua", "guard"),
			new ChatColorOption(34, ChatColor.BLUE, Material.BLUE_WOOL, "Blue", "rp.chatcolors.blue", "hunter"),
			new ChatColorOption(38, ChatColor.LIGHT_PURPLE, Material.MAGENTA_WOOL, "Light Purple", "rp.chatcolors.light_purple", "warder"),
			new ChatColorOption(39, ChatColor.DARK_PURPLE, Material.PURPLE_WOOL, "Dark Purple", "rp.chatcolors.dark_purple", "champion"),
			new ChatColorOption(41, ChatColor.RED, Material.RED_WOOL, "Red", "rp.chatcolors.red", "master"),
			new ChatColorOption(42, ChatColor.WHITE, Material.WHITE_WOOL, "White", "rp.chatcolors.white", null));

	static boolean canUseChatColor(Player p, ChatColorOption opt) {
		return p != null && (p.hasPermission(opt.perm) || (opt.rank != null && p.hasPermission("group." + opt.rank)));
	}

	static ChatColorOption chatColorOptionAt(int slot) {
		for (ChatColorOption o : CHAT_COLOR_OPTIONS) {
			if (o.slot == slot) {
				return o;
			}
		}
		return null;
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

		// CHAT COLORS (2026-11-23): one table drives what is shown AND what a click allows.
		// A colour is available with its rp.chatcolors.* permission (donor groups) or once the player has
		// reached its rank: "group.<rank>" is true for the player's rank and every rank below it.
		Player player = Bukkit.getPlayer(this.getPlayerID());
		for (ChatColorOption opt : CHAT_COLOR_OPTIONS) {
			if (!canUseChatColor(player, opt)) {
				continue;
			}
			boolean active = opt.color.toString().equals(this.getChatColor());
			ItemStack colorIcon = new ItemStack(opt.wool);
			ItemMeta colorMeta = colorIcon.getItemMeta();
			colorMeta.setDisplayName(opt.color + "" + ChatColor.BOLD + opt.name);
			List<String> colorLore = new ArrayList<>();
			if (active) {
				colorLore.add(ChatColor.GREEN + "✔ Currently active");
				colorMeta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK, 1, true);
				colorMeta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
			} else {
				colorLore.add(opt.color + "Click here to make your rank");
				colorLore.add(opt.color + "and name in chat appear " + opt.name.toLowerCase() + "!");
			}
			colorLore.add(ChatColor.DARK_GRAY + (opt.rank == null ? "Donor colour" : "Unlocked at rank " + opt.rankLabel()));
			colorMeta.setLore(colorLore);
			colorIcon.setItemMeta(colorMeta);
			poMenu.setItem(opt.slot, colorIcon);
		}

		p.openInventory(poMenu);

	}

	public void showServerMenu(Player p, Player pSender) {

		Inventory poMenu = Bukkit.createInventory(null, 36,
				ChatColor.DARK_RED + "" + ChatColor.BOLD + "Profile :: Main Menu :: " + p.getName());

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

		pSender.openInventory(poMenu);
	}
}
