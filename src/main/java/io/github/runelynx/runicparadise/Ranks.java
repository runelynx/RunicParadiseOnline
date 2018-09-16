package io.github.runelynx.runicparadise;

import com.connorlinfoot.titleapi.TitleAPI;
import io.github.runelynx.runicuniverse.RunicMessaging;
import io.github.runelynx.runicuniverse.RunicMessaging.RunicFormat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

import static io.github.runelynx.runicparadise.RunicParadise.economy;
import static io.github.runelynx.runicparadise.RunicParadise.perms;
import static org.bukkit.Bukkit.getLogger;

/*
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.Item.CustomItem;
import me.mrCookieSlime.Slimefun.Lists.RecipeType;
import me.mrCookieSlime.Slimefun.Objects.Category;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem;

*/

class Ranks {
	private Plugin instance = RunicParadise.getInstance();
	private RunicGateway RG = new RunicGateway();

	final int RUNNER_DAYS = 7;
	final int RUNNER_RUNICS = 2500;
	final String RUNNER_KILLS = "Zombie:30, Spider:15, Skeleton:30";
	final int SINGER_DAYS = 21;
	final int SINGER_RUNICS = 5000;
	final int SINGER_JOB_LEVEL = 5;
	final String SINGER_KILLS = "Zombie:100, Skeleton:100, Spider:50";
	final int BRAWLER_DAYS = 35;
	final int BRAWLER_RUNICS = 10000;
	final int BRAWLER_JOB_LEVEL = 15;
	final String BRAWLER_KILLS = "PigZombie:25, Enderman:15, Witch:2";
	final int KEEPER_DAYS = 49;
	final int KEEPER_RUNICS = 15000;
	final int KEEPER_MASTER_JOBS = 1;
	final String KEEPER_KILLS = "Zombie:200, Skeleton:200, Spider:100, Wither:1";
	final int GUARD_DAYS = 63;
	final int GUARD_RUNICS = 25000;
	final String GUARD_KILLS = "PigZombie:50, Enderman:30, Witch:5";
	final int HUNTER_DAYS = 77;
	final int HUNTER_RUNICS = 40000;
	final int HUNTER_MASTER_JOBS = 2;
	final String HUNTER_KILLS = "Zombie:300, Skeleton:300, Spider:150, Wither:2";
	final int SLAYER_DAYS = 98;
	final int SLAYER_RUNICS = 60000;
	final String SLAYER_KILLS = "PigZombie:100, Enderman:45, Witch:10";
	final int WARDER_DAYS = 17 * 7;
	final int WARDER_RUNICS = 80000;
	final int WARDER_MASTER_JOBS = 3;
	final String WARDER_KILLS = "Zombie:400, Skeleton:400, Spider:200, Wither:3";
	final int CHAMPION_DAYS = 20 * 7;
	final int CHAMPION_RUNICS = 100000;
	final String CHAMPION_KILLS = "PigZombie:150, Enderman:60, Witch:25";
	final int MASTER_DAYS = 23 * 7;
	final int MASTER_RUNICS = 125000;
	final int MASTER_MASTER_JOBS = 6;
	final String MASTER_KILLS = "Zombie:750, Wither:5";

	final int DUKE_RUNICS = 250000;
	final int DUKE_MASTER_JOBS = 10;

	final int BARON_RUNICS = 300000;
	final int BARON_MASTER_JOBS = 12;

	Ranks() { }

	private void congratsPromotion(String promoted, String newRank) {
		// RG.sendMessage(true, promoted, ChatColor.GOLD
		// + "[RunicRanks] Congratulations, " + promoted
		// + ", on promoting to " + newRank + "!");

		for (Player p : Bukkit.getOnlinePlayers()) {
			TitleAPI.sendTitle(p, 2, 3, 2, RunicParadise.rankColors.get(newRank) + "" + ChatColor.BOLD + promoted,
					RunicParadise.rankColors.get(newRank) + "has reached the rank of " + newRank);
		}

		RG.playPortalTravelSound(true, "abc");
	}

	/*
	 * public void playerkillCounts(Player user) { Date now = new Date();
	 * 
	 * MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
	 * "dbHost"), instance.getConfig().getString("dbPort"), instance
	 * .getConfig().getString("dbDatabase"), instance.getConfig()
	 * .getString("dbUser"), instance.getConfig().getString( "dbPassword"));
	 * final Connection d = MySQL.openConnection();
	 * 
	 * user.sendMessage(ChatColor.GRAY +
	 * "[RunicRanks] Listing player kill counts...");
	 * 
	 * for (Player p : Bukkit.getOnlinePlayers()) { try { Statement dStmt =
	 * d.createStatement(); ResultSet playerData = dStmt
	 * .executeQuery("SELECT * FROM `rp_PlayerInfo` WHERE `PlayerName` = '" +
	 * p.getName() + "' ORDER BY `id` ASC LIMIT 1;"); playerData.next(); int
	 * killZombie = playerData.getInt("KillZombie"); int killSkeleton =
	 * playerData.getInt("KillSkeleton"); int killWitch =
	 * playerData.getInt("KillWitch"); int killWither =
	 * playerData.getInt("KillWither"); int killSlime =
	 * playerData.getInt("KillSlime"); int killMagmaCube =
	 * playerData.getInt("KillMagmaCube"); int killSilverfish =
	 * playerData.getInt("KillSilverfish"); int killGiant =
	 * playerData.getInt("KillGiant"); int killBlaze =
	 * playerData.getInt("KillBlaze"); int killCreeper =
	 * playerData.getInt("KillCreeper"); int killEnderman =
	 * playerData.getInt("KillEnderman"); int killSpider =
	 * playerData.getInt("KillSpider"); int killCavespider =
	 * playerData.getInt("KillCaveSpider"); int killSquid =
	 * playerData.getInt("KillSquid"); int killEnderdragon =
	 * playerData.getInt("KillEnderDragon"); int killPigzombie =
	 * playerData.getInt("KillPigZombie"); int killGhast =
	 * playerData.getInt("KillGhast");
	 * 
	 * user.sendMessage(ChatColor.GOLD + p.getName() + ": " + ChatColor.GRAY +
	 * "Zom:" + killZombie + "," + "Ske:" + killSkeleton + "," + "Wch:" +
	 * killWitch + "," + "Wth:" + killWither + "," + "Sli:" + killSlime + "," +
	 * "Mag:" + killMagmaCube + "," + "Slv:" + killSilverfish + "," + "Gia:" +
	 * killGiant + "," + "Blz:" + killBlaze + "," + "Crp:" + killCreeper + "," +
	 * "EnM:" + killEnderman + "," + "EnD:" + killEnderdragon + "," + "Spd:" +
	 * killSpider + "," + "Cav:" + killCavespider + "," + "Sqd:" + killSquid +
	 * "," + "PgZ:" + killPigzombie + "," + "Gha:" + killGhast); } catch
	 * (SQLException e) { getLogger().log( Level.SEVERE,
	 * "Failed DB check for playerStats because: " + e.getMessage()); } }
	 * 
	 * try { d.close(); } catch (SQLException e) { getLogger().log(
	 * Level.SEVERE, "Failed DB close for playerStats because: " +
	 * e.getMessage()); } }
	 */
	void playerStats(Player user) {
		Date now = new Date();

		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
		final Connection d = MySQL.openConnection();
		double daysPlayed = 0;
		int balance = 0;
		int isNominated = 0;

		user.sendMessage(ChatColor.GRAY + "[RunicRanks] Listing player stats...");

		for (Player p : Bukkit.getOnlinePlayers()) {
			try {
				Statement dStmt = d.createStatement();
				ResultSet playerData = dStmt.executeQuery(
						"SELECT `FirstSeen`, `IsNominated`, `Votes` FROM `rp_PlayerInfo` WHERE `PlayerName` = '"
								+ p.getName() + "' ORDER BY `id` ASC LIMIT 1;");
				playerData.next();

				balance = (int) economy.getBalance(p);
				;
				long firstSeenTime = playerData.getLong("FirstSeen");
				isNominated = playerData.getInt("IsNominated");
				daysPlayed = ((now.getTime() - firstSeenTime) / 86400000);
				DecimalFormat df = new DecimalFormat("#,###.##");

				user.sendMessage(
						ChatColor.GRAY + p.getName() + ": " + ChatColor.LIGHT_PURPLE + df.format(daysPlayed) + " days, "
								+ balance + " R, noms(" + isNominated + "), votes(" + playerData.getInt("Votes") + ")");
			} catch (SQLException e) {
				getLogger().log(Level.SEVERE, "Failed DB check for playerStats because: " + e.getMessage());
			}
		}

		try {
			d.close();
		} catch (SQLException e) {
			getLogger().log(Level.SEVERE, "Failed DB close for playerStats because: " + e.getMessage());
		}
	}

	void showRequirements(Player user) {
		int nomRedux;

		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
		Connection d = MySQL.openConnection();
		int nominations = 0;

		try {
			Statement dStmt = d.createStatement();
			ResultSet playerData = dStmt.executeQuery("SELECT * FROM `rp_PlayerInfo` WHERE `PlayerName` = '"
					+ user.getName() + "' ORDER BY `id` ASC LIMIT 1;");
			// if this is true, there is a result!
			if (playerData.isBeforeFirst()) {
				playerData.next();
				nominations = playerData.getInt("IsNominated");
			}
			d.close();
		} catch (SQLException e) {
			getLogger().log(Level.SEVERE, "Failed DB check for showRequirements because: " + e.getMessage());
		}

		if (nominations > 0) {
			nomRedux = 10;
		} else {
			nomRedux = 0;
		}

		user.sendMessage(ChatColor.GRAY + "[RunicRanks] Listing promotion requirements...");
		user.sendMessage(ChatColor.DARK_GREEN + "Runner - " + ChatColor.GRAY + RUNNER_DAYS + " days or 15 votes, "
				+ RUNNER_RUNICS + " R.");
		user.sendMessage(ChatColor.GRAY + RUNNER_KILLS);
		user.sendMessage(ChatColor.YELLOW + "Singer - " + ChatColor.GRAY + SINGER_DAYS + " days, " + SINGER_RUNICS
				+ " R, hedge maze");
		user.sendMessage(ChatColor.GRAY + SINGER_KILLS);
		user.sendMessage(ChatColor.GOLD + "Brawler - " + ChatColor.GRAY + BRAWLER_DAYS + " days, " + BRAWLER_RUNICS
				+ " R, sky maze.");
		user.sendMessage(ChatColor.GRAY + BRAWLER_KILLS);
		user.sendMessage(ChatColor.AQUA + "Keeper - " + ChatColor.GRAY + KEEPER_DAYS + " days, " + KEEPER_RUNICS
				+ " R, " + KEEPER_MASTER_JOBS + " jobs mastered, ice maze, faith power level >100.");
		user.sendMessage(ChatColor.GRAY + KEEPER_KILLS);
		user.sendMessage(ChatColor.DARK_AQUA + "Guard - " + ChatColor.GRAY + GUARD_DAYS + " days, " + GUARD_RUNICS
				+ " R, jungle maze.");
		user.sendMessage(ChatColor.GRAY + GUARD_KILLS);
		user.sendMessage(ChatColor.BLUE + "Hunter - " + ChatColor.GRAY + HUNTER_DAYS + " days, " + HUNTER_RUNICS
				+ " R, " + HUNTER_MASTER_JOBS + " jobs mastered, frost maze, faith power level >150.");
		user.sendMessage(ChatColor.GRAY + HUNTER_KILLS);
		user.sendMessage(
				ChatColor.DARK_BLUE + "Slayer - " + ChatColor.GRAY + SLAYER_DAYS + " days, " + SLAYER_RUNICS + " R.");
		user.sendMessage(ChatColor.GRAY + SLAYER_KILLS);
		user.sendMessage(ChatColor.LIGHT_PURPLE + "Warder - " + ChatColor.GRAY + WARDER_DAYS + " days, " + WARDER_RUNICS
				+ " R, " + WARDER_MASTER_JOBS + " jobs mastered");
		user.sendMessage(ChatColor.GRAY + WARDER_KILLS);
		user.sendMessage(ChatColor.DARK_PURPLE + "Champion - " + ChatColor.GRAY + CHAMPION_DAYS + " days, "
				+ CHAMPION_RUNICS + " R., faith power level 300.");
		user.sendMessage(ChatColor.GRAY + CHAMPION_KILLS);
		user.sendMessage(ChatColor.RED + "Master - " + ChatColor.GRAY + MASTER_DAYS + " days, " + MASTER_RUNICS + " R, "
				+ MASTER_MASTER_JOBS + " jobs mastered, dungeon maze.");
		user.sendMessage(ChatColor.GRAY + MASTER_KILLS);
		user.sendMessage(ChatColor.DARK_GREEN + "Duke - " + ChatColor.GRAY + " 10 mazes, 10 jobs mastered, duke ring (/warp dukering), "
				+ DUKE_RUNICS + " R.");
		user.sendMessage(ChatColor.GOLD + "Baron - " + ChatColor.GRAY + " 12 mazes, 12 jobs mastered, baron pendant (/warp baronpendant), "
				+ BARON_RUNICS + " R.");
	}

	@SuppressWarnings("deprecation")
	void convertRanks(final Player user) {
		String rank = perms.getPrimaryGroup(user);
		boolean converted = false;
		String command = "";
		String command2 = "";
		String newRank = "";

		switch (rank) {
		case "Ghosts":
			promotePlayer(user, "Ghost");
			converted = true;
			break;
		case "Engineers":
		case "Adventurers":
		case "GameMasters":
		case "Farmers":
		case "Merchants":
		case "Citizens2":
		case "Miners":
		case "Architects":
		case "Citizens":
		case "Guides":
		case "Mayors":
		case "Mentors":
		case "Creators":
		case "Ambassadors":
		case "CityPlanners":
		case "Settler":
		case "Explorer":
		case "Builder":
		case "Warden":
		case "Protector":
		case "Guardian":

			promotePlayer(user, "Seeker");
			command2 = "faith enable " + user.getName() + " Sun";
			newRank = "Seeker";
			converted = true;
			break;
		default:
			break;
		}
		if (converted) {
			user.sendMessage(ChatColor.YELLOW + "[RunicRanks]" + ChatColor.GOLD + " Welcome to the new ranks system!!");
			user.sendMessage(ChatColor.YELLOW + "[RunicRanks]" + ChatColor.GOLD
					+ " To learn how to get to the next rank, type /promote");
			user.sendMessage(
					ChatColor.YELLOW + "[RunicRanks]" + ChatColor.GOLD + " To see what ranks we have, type /ranks");
			user.sendMessage(ChatColor.YELLOW + "[RunicRanks]" + ChatColor.GOLD
					+ " To learn about perks: http://www.runic-paradise.com/ranks.php");
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
					"sc Converting " + user.getName() + " from " + rank + " to " + newRank);
			Bukkit.getServer().getScheduler().scheduleAsyncDelayedTask(RunicParadise.getInstance(),
					() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "faith enable " + user.getName() + " Sun"), 60);
		}
	}

	void nominatePlayer(Player sender, String nominee) {
		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
		Connection d = MySQL.openConnection();
		int nominations = 0;
		boolean playerFound = false;
		try {
			Statement dStmt = d.createStatement();
			ResultSet playerData = dStmt.executeQuery(
					"SELECT * FROM `rp_PlayerInfo` WHERE `PlayerName` = '" + nominee + "' ORDER BY `id` ASC LIMIT 1;");
			// if this is true, there is a result!
			if (playerData.isBeforeFirst()) {
				playerData.next();
				nominations = playerData.getInt("IsNominated");
				playerFound = true;
			} else {
				sender.sendMessage(ChatColor.RED + "[ERROR] Nomination failed. Could not find player named " + nominee);
				d.close();
			}
		} catch (SQLException e) {
			getLogger().log(Level.SEVERE, "Failed DB check for nominatePlayer because: " + e.getMessage());
		}

		if (playerFound) {
			try {
				Statement dStmt = d.createStatement();
				dStmt.executeUpdate("UPDATE `rp_PlayerInfo` SET IsNominated='" + (nominations + 1)
						+ "' WHERE PlayerName='" + nominee + "';");
				d.close();
				sender.sendMessage(ChatColor.DARK_GREEN + "[RunicRanks] Success! " + nominee + " now has "
						+ (nominations + 1) + " nominations");
				sender.getServer().getPlayer(nominee)
						.sendMessage(ChatColor.DARK_GREEN
								+ "[RunicRanks] You have just been nominated for faster promotions by "
								+ sender.getDisplayName());
				sender.getServer().getPlayer(nominee)
						.sendMessage(ChatColor.WHITE + "Type /ranks to see your new requirements.");
				sender.getServer().getPlayer(nominee).sendMessage(ChatColor.WHITE
						+ "While multiple nominations are very nice, only the first one has an impact. :)");

			} catch (SQLException e) {
				getLogger().log(Level.SEVERE, "Failed DB update for nominatePlayer because: " + e.getMessage());
			}
		}
	}

	void checkPromotion(Player user, boolean execute) {
		Date now = new Date();

		MySQL MySQL = new MySQL(instance, instance.getConfig().getString("dbHost"),
				instance.getConfig().getString("dbPort"), instance.getConfig().getString("dbDatabase"),
				instance.getConfig().getString("dbUser"), instance.getConfig().getString("dbPassword"));
		final Connection d = MySQL.openConnection();
		float daysPlayed = 0f;
		DecimalFormat df = new DecimalFormat("#.00");

		String rank = "";
		int balance = (int) economy.getBalance(user);
		int nomRedux = 0;

		int skyblockRedux = 0;

		if (RunicParadise.playerProfiles.get(user.getUniqueId()).getSkyblockRankNum() > 1) {
			skyblockRedux = RunicParadise.playerProfiles.get(user.getUniqueId()).getSkyblockRankNum() * 2;
		}

		boolean checkDays = false;
		boolean checkFaiths = false;
		boolean checkJob = false;
		boolean checkRunics = false;
		boolean checkKills = false;
		boolean checkMazes = false;
		boolean checkJobMasteries = false;
		boolean checkFeudalJewelry = false;
		boolean ineligible = false;
		int mazeCount = 0;

		ArrayList<String> failureResponse = new ArrayList<String>();
		int[] killsArray = new int[13];
		RunicPlayerBukkit targetPlayer = new RunicPlayerBukkit(user);
		// ZOM,SPI,SKE,SQU,BLA,PGZ,GHA,WTH,WCH,ENM,CRE

		// Check how many days played; report days played and current rank to
		// user
		try {
			Statement dStmt = d.createStatement();
			ResultSet playerData = dStmt.executeQuery("SELECT * FROM `rp_PlayerInfo` WHERE `PlayerName` = '"
					+ user.getName() + "' ORDER BY `id` ASC LIMIT 1;");
			playerData.next();

			long firstSeenTime = playerData.getLong("FirstSeen");
			if (playerData.getInt("IsNominated") > 0) {
				nomRedux = 10;
			}
			daysPlayed = ((now.getTime() - firstSeenTime) / 86400000f);

			rank = perms.getPrimaryGroup(user);
			killsArray[0] = RunicParadise.playerProfiles.get(user.getUniqueId()).mobKillCountsMap
					.get(EntityType.ZOMBIE);
			killsArray[1] = RunicParadise.playerProfiles.get(user.getUniqueId()).mobKillCountsMap
					.get(EntityType.SPIDER);
			killsArray[2] = RunicParadise.playerProfiles.get(user.getUniqueId()).mobKillCountsMap
					.get(EntityType.SKELETON);
			killsArray[3] = RunicParadise.playerProfiles.get(user.getUniqueId()).mobKillCountsMap.get(EntityType.SQUID);
			killsArray[4] = RunicParadise.playerProfiles.get(user.getUniqueId()).mobKillCountsMap.get(EntityType.BLAZE);
			killsArray[5] = RunicParadise.playerProfiles.get(user.getUniqueId()).mobKillCountsMap
					.get(EntityType.PIG_ZOMBIE);
			killsArray[6] = RunicParadise.playerProfiles.get(user.getUniqueId()).mobKillCountsMap.get(EntityType.GHAST);
			killsArray[7] = RunicParadise.playerProfiles.get(user.getUniqueId()).mobKillCountsMap
					.get(EntityType.WITHER);
			killsArray[8] = RunicParadise.playerProfiles.get(user.getUniqueId()).mobKillCountsMap.get(EntityType.WITCH);
			killsArray[9] = RunicParadise.playerProfiles.get(user.getUniqueId()).mobKillCountsMap
					.get(EntityType.ENDERMAN);
			killsArray[10] = RunicParadise.playerProfiles.get(user.getUniqueId()).mobKillCountsMap
					.get(EntityType.CREEPER);
			killsArray[11] = RunicParadise.playerProfiles.get(user.getUniqueId()).mobKillCountsMap
					.get(EntityType.CAVE_SPIDER);
			killsArray[12] = RunicParadise.playerProfiles.get(user.getUniqueId()).mobKillCountsMap
					.get(EntityType.IRON_GOLEM);
			// if you add more here, increase the array definition up above!!!

			user.sendMessage(ChatColor.DARK_GREEN + "You first joined Runic Paradise " + daysPlayed
					+ " days ago. Your current rank is " + rank + ".");
		} catch (SQLException e) {
			getLogger().log(Level.SEVERE, "Failed DB check for checkPromotion because: " + e.getMessage());
		}

		float daysRequired;

		switch (rank) {
		case "Duke":

			if (balance >= BARON_RUNICS) {
				checkRunics = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + BARON_RUNICS);
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + BARON_RUNICS);
			}

			if (targetPlayer.getMasteredJobCount() >= BARON_MASTER_JOBS) {
				checkJobMasteries = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "You have mastered "
						+ targetPlayer.getMasteredJobCount() + " jobs.");
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "You haven't mastered "
						+ BARON_MASTER_JOBS + " jobs. Get info @ /warp jobs");
			}

			mazeCount = RunicParadise.getPlayerDistinctMazeCompletionCount(user);

			if (mazeCount >= 12) {
				checkMazes = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "You have completed "
						+ mazeCount + " mazes!");
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY
						+ "You must complete at least 12 mazes. Check /games");
			}

			if (user.getEnderChest().contains(Borderlands.specialLootDrops("BaronPendant1", user.getUniqueId())) || user
					.getEnderChest().contains(Borderlands.specialLootDrops("BaronPendant2", user.getUniqueId()))) {
				checkFeudalJewelry = true;
				failureResponse
						.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "You have one of the Baron Pendants!");
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY
						+ "You must be wearing a Baron Pendant! Be sure it's in your ender chest. See crafting info at /warp BaronPendant");
			}

			if (checkFeudalJewelry && checkRunics && checkJobMasteries && checkMazes) {
				if (execute) {
					// ok now we're executing the promotion.
					economy.withdrawPlayer(user.getName(), BARON_RUNICS);
					promotePlayer(user, "Baron");
					perms.playerRemoveGroup(user, "Duke");
					user.sendMessage(ChatColor.DARK_GREEN + "[RunicRanks] Congratulations! You have been promoted!");
					Ranks tempRank = new Ranks();
					tempRank.congratsPromotion(user.getName(), "Baron");
					logPromotion(user.getName(), "Baron", new Date().getTime());
					RunicParadise.playerProfiles.get(user.getUniqueId()).setChatColor(ChatColor.YELLOW, true);
				} else {
					// just checking... we're not executing the promotion!!
					user.sendMessage(
							ChatColor.DARK_GREEN + "[RunicRanks] Congratulations! You qualify for a promotion!");
					user.sendMessage(ChatColor.DARK_GREEN + "Promotion to Baron costs " + BARON_RUNICS + " Runics.");
					user.sendMessage(ChatColor.DARK_GREEN + "Type " + ChatColor.AQUA + "/rankup now"
							+ ChatColor.DARK_GREEN + " to accept the promotion.");
				}
			} else {
				ineligible = true;
			}

			break;
		case "Master":

			if (balance >= DUKE_RUNICS) {
				checkRunics = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + DUKE_RUNICS);
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + DUKE_RUNICS);
			}

			if (targetPlayer.getMasteredJobCount() >= DUKE_MASTER_JOBS) {
				checkJobMasteries = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "You have mastered "
						+ targetPlayer.getMasteredJobCount() + " jobs.");
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "You haven't mastered "
						+ DUKE_MASTER_JOBS + " jobs. Get info @ /warp jobs");
			}

			mazeCount = RunicParadise.getPlayerDistinctMazeCompletionCount(user);

			if (mazeCount >= 10) {
				checkMazes = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "You have completed "
						+ mazeCount + " mazes!");
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY
						+ "You must complete at least 10 mazes. Check /games");
			}

			if (user.getEnderChest().contains(Borderlands.specialLootDrops("DukeRing1", user.getUniqueId()))
					|| user.getEnderChest().contains(Borderlands.specialLootDrops("DukeRing2", user.getUniqueId()))
					|| user.getEnderChest().contains(Borderlands.specialLootDrops("DukeRing3", user.getUniqueId()))
					|| user.getEnderChest().contains(Borderlands.specialLootDrops("DukeRing4", user.getUniqueId()))) {
				checkFeudalJewelry = true;
				failureResponse
						.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "You have one of the Duke Rings!");
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY
						+ "You must be wearing a Duke Ring! Be sure it's in your ender chest. See crafting info at /warp DukeRing");
			}

			if (checkFeudalJewelry && checkRunics && checkJobMasteries && checkMazes) {
				if (execute == false) {
					// just checking... we're not executing the promotion!!
					user.sendMessage(
							ChatColor.DARK_GREEN + "[RunicRanks] Congratulations! You qualify for a promotion!");
					user.sendMessage(ChatColor.DARK_GREEN + "Promotion to Duke costs " + DUKE_RUNICS + " Runics.");
					user.sendMessage(ChatColor.DARK_GREEN + "Type " + ChatColor.AQUA + "/rankup now"
							+ ChatColor.DARK_GREEN + " to accept the promotion.");
				} else {
					// ok now we're executing the promotion.
					economy.withdrawPlayer(user.getName(), DUKE_RUNICS);
					promotePlayer(user, "Duke");
					perms.playerRemoveGroup(user, "Master");
					user.sendMessage(ChatColor.DARK_GREEN + "[RunicRanks] Congratulations! You have been promoted!");
					Ranks tempRank = new Ranks();
					tempRank.congratsPromotion(user.getName(), "Duke");
					logPromotion(user.getName(), "Duke", new Date().getTime());
					RunicParadise.playerProfiles.get(user.getUniqueId()).setChatColor(ChatColor.GREEN, true);
				}
			} else {
				ineligible = true;
			}

			break;
		case "Champion":
			// if player has been on the server long enough for promotion
			daysRequired = ((float) MASTER_DAYS) - skyblockRedux;

			if (daysPlayed - daysRequired > -0.4) {
				checkDays = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: " + daysRequired);
				if (skyblockRedux > 0) {
					failureResponse
							.add(ChatColor.BLUE + "Skyblock rank reduced requirement by " + skyblockRedux + " days");
				}
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: " + daysRequired);
				if (skyblockRedux > 0) {
					failureResponse
							.add(ChatColor.BLUE + "Skyblock rank reduced requirement by " + skyblockRedux + " days");
				}
			}
			if (balance >= MASTER_RUNICS) {
				checkRunics = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + MASTER_RUNICS);
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + MASTER_RUNICS);
			}

			if (killsArray[0] >= 750 && killsArray[7] >= 5) {
				checkKills = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY
						+ "You have killed enough monsters for this promotion.");
			} else {
				if (killsArray[0] > 750) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Your ZOMBIE kills: "
							+ killsArray[0] + "; Required: 750");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Your ZOMBIE kills: "
							+ killsArray[0] + "; Required: 750");
				}

				if (killsArray[7] >= 5) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Your WITHER kills: "
							+ killsArray[7] + "; Required: 5");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Your WITHER kills: "
							+ killsArray[7] + "; Required: 5");
				}
			}
			if (targetPlayer.getMasteredJobCount() >= MASTER_MASTER_JOBS) {
				checkJobMasteries = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "You have mastered "
						+ targetPlayer.getMasteredJobCount() + " jobs.");
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "You haven't mastered "
						+ MASTER_MASTER_JOBS + " jobs. Get info @ /warp jobs");
			}

			if (RunicParadise.getPlayerMazeCompletionCount(user, 4) > 0
					&& RunicParadise.getPlayerMazeCompletionCount(user, 6) > 0) {
				checkMazes = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY
						+ "You have completed the jungle & dungeon mazes!");
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY
						+ "You must complete the jungle and dungeon mazes! See Puzzle Kiosk at /games");
			}

			if (checkDays && checkRunics && checkKills && checkJobMasteries && checkMazes) {
				if (execute == false) {
					// just checking... we're not executing the promotion!!
					user.sendMessage(
							ChatColor.DARK_GREEN + "[RunicRanks] Congratulations! You qualify for a promotion!");
					user.sendMessage(ChatColor.DARK_GREEN + "Promotion to Master costs " + MASTER_RUNICS + " Runics.");
					user.sendMessage(ChatColor.DARK_GREEN + "Type " + ChatColor.AQUA + "/rankup now"
							+ ChatColor.DARK_GREEN + " to accept the promotion.");
				} else {
					// ok now we're executing the promotion.
					economy.withdrawPlayer(user.getName(), MASTER_RUNICS);
					promotePlayer(user, "Master");
					perms.playerRemoveGroup(user, "Champion");
					user.sendMessage(ChatColor.DARK_GREEN + "[RunicRanks] Congratulations! You have been promoted!");
					Ranks tempRank = new Ranks();
					tempRank.congratsPromotion(user.getName(), "Master");
					logPromotion(user.getName(), "Master", new Date().getTime());
					RunicParadise.playerProfiles.get(user.getUniqueId()).setChatColor(ChatColor.RED, true);
				}
			} else {
				ineligible = true;
			}

			break;
		case "Warder":

			daysRequired = ((float) CHAMPION_DAYS) - skyblockRedux;

			// if player has been on the server long enough for promotion
			if (daysPlayed - daysRequired > -0.4) {
				checkDays = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: " + daysRequired);
				if (skyblockRedux > 0) {
					failureResponse
							.add(ChatColor.BLUE + "Skyblock rank reduced requirement by " + skyblockRedux + " days");
				}
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: " + daysRequired);
				if (skyblockRedux > 0) {
					failureResponse
							.add(ChatColor.BLUE + "Skyblock rank reduced requirement by " + skyblockRedux + " days");
				}
			}
			if (balance >= CHAMPION_RUNICS) {
				checkRunics = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + CHAMPION_RUNICS);
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + CHAMPION_RUNICS);
			}

			if (killsArray[5] >= 150 && killsArray[9] >= 60 && killsArray[8] >= 25) {
				checkKills = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY
						+ "You have killed enough monsters for this promotion.");
			} else {
				if (killsArray[5] > 150) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Your PIGZOMBIE kills: "
							+ killsArray[5] + "; Required: 150");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Your PIGZOMBIE kills: "
							+ killsArray[5] + "; Required: 150");
				}
				if (killsArray[9] >= 60) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Your ENDERMAN kills: "
							+ killsArray[9] + "; Required: 60");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Your ENDERMAN kills: "
							+ killsArray[9] + "; Required: 60");
				}
				if (killsArray[8] >= 25) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Your WITCH kills: "
							+ killsArray[8] + "; Required: 25");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Your WITCH kills: "
							+ killsArray[8] + "; Required: 25");
				}
			}
			if (targetPlayer.getFaithPowerLevel() >= 300) {
				checkFaiths = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY
						+ "You have faith power level of at least 300");
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY
						+ "You need faith power level 300 or more. Power level is the combined level of all your faiths - check it in /faith stats.");
			}
			if (RunicParadise.getPlayerMazeCompletionCount(user, 4) > 0) {
				checkMazes = true;
				failureResponse
						.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "You have completed the jungle maze!");
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY
						+ "You must complete the jungle maze! See Puzzle Kiosk at /games");
			}

			if (checkDays && checkRunics && checkKills && checkMazes && checkFaiths) {
				if (execute == false) {
					// just checking... we're not executing the promotion!!
					user.sendMessage(
							ChatColor.DARK_GREEN + "[RunicRanks] Congratulations! You qualify for a promotion!");
					user.sendMessage(
							ChatColor.DARK_GREEN + "Promotion to Champion costs " + CHAMPION_RUNICS + " Runics.");
					user.sendMessage(ChatColor.DARK_GREEN + "Type " + ChatColor.AQUA + "/rankup now"
							+ ChatColor.DARK_GREEN + " to accept the promotion.");
				} else {
					// ok now we're executing the promotion.
					economy.withdrawPlayer(user.getName(), CHAMPION_RUNICS);
					promotePlayer(user, "Champion");
					perms.playerRemoveGroup(user, "Warder");
					user.sendMessage(ChatColor.DARK_GREEN + "[RunicRanks] Congratulations! You have been promoted!");
					Ranks tempRank = new Ranks();
					tempRank.congratsPromotion(user.getName(), "Champion");
					logPromotion(user.getName(), "Champion", new Date().getTime());
					RunicParadise.playerProfiles.get(user.getUniqueId()).setChatColor(ChatColor.DARK_PURPLE, true);
				}
			} else {
				ineligible = true;
			}
			break;
		case "Slayer":

			daysRequired = ((float) WARDER_DAYS) - skyblockRedux;

			// if player has been on the server long enough for promotion
			if (daysPlayed - daysRequired > -0.4) {
				checkDays = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: " + daysRequired);
				if (skyblockRedux > 0) {
					failureResponse
							.add(ChatColor.BLUE + "Skyblock rank reduced requirement by " + skyblockRedux + " days");
				}
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: " + daysRequired);
				if (skyblockRedux > 0) {
					failureResponse
							.add(ChatColor.BLUE + "Skyblock rank reduced requirement by " + skyblockRedux + " days");
				}
			}
			if (balance >= WARDER_RUNICS) {
				checkRunics = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + WARDER_RUNICS);
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + WARDER_RUNICS);
			}

			if (killsArray[0] >= 400 && killsArray[1] >= 200 && killsArray[2] >= 400 && killsArray[7] >= 3) {
				checkKills = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY
						+ "You have killed enough monsters for this promotion.");
			} else {
				if (killsArray[0] > 400) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Your ZOMBIE kills: "
							+ killsArray[0] + "; Required: 400");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Your ZOMBIE kills: "
							+ killsArray[0] + "; Required: 400");
				}
				if (killsArray[1] >= 200) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Your SPIDER kills: "
							+ killsArray[1] + "; Required: 200");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Your SPIDER kills: "
							+ killsArray[1] + "; Required: 200");
				}
				if (killsArray[2] >= 400) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Your SKELETON kills: "
							+ killsArray[2] + "; Required: 400");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Your SKELETON kills: "
							+ killsArray[2] + "; Required: 400");
				}
				if (killsArray[7] >= 3) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Your WITHER kills: "
							+ killsArray[7] + "; Required: 3");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Your WITHER kills: "
							+ killsArray[7] + "; Required: 3");
				}
			}
			if (targetPlayer.getMasteredJobCount() > 2) {
				checkJobMasteries = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "You have mastered "
						+ targetPlayer.getMasteredJobCount() + " jobs.");
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY
						+ "You haven't mastered 3 jobs. Get info @ /warp jobs");
			}

			if (RunicParadise.getPlayerMazeCompletionCount(user, 4) > 0) {
				checkMazes = true;
				failureResponse
						.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "You have completed the jungle maze!");
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY
						+ "You must complete the jungle maze! See Puzzle Kiosk at /games");
			}

			if (checkDays && checkRunics && checkKills && checkJobMasteries && checkMazes) {
				if (execute == false) {
					// just checking... we're not executing the promotion!!
					user.sendMessage(
							ChatColor.DARK_GREEN + "[RunicRanks] Congratulations! You qualify for a promotion!");
					user.sendMessage(ChatColor.DARK_GREEN + "Promotion to Warder costs " + WARDER_RUNICS + " Runics.");
					user.sendMessage(ChatColor.DARK_GREEN + "Type " + ChatColor.AQUA + "/rankup now"
							+ ChatColor.DARK_GREEN + " to accept the promotion.");
				} else {
					// ok now we're executing the promotion.
					economy.withdrawPlayer(user.getName(), WARDER_RUNICS);
					promotePlayer(user, "Warder");
					perms.playerRemoveGroup(user, "Slayer");
					user.sendMessage(ChatColor.DARK_GREEN + "[RunicRanks] Congratulations! You have been promoted!");
					Ranks tempRank = new Ranks();
					tempRank.congratsPromotion(user.getName(), "Warder");
					logPromotion(user.getName(), "Warder", new Date().getTime());
					RunicParadise.playerProfiles.get(user.getUniqueId()).setChatColor(ChatColor.LIGHT_PURPLE, true);
				}
			} else {
				ineligible = true;
			}
			break;
		case "Hunter":

			daysRequired = ((float) SLAYER_DAYS) - skyblockRedux;

			// if player has been on the server long enough for promotion
			if (daysPlayed - daysRequired > -0.4) {
				checkDays = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: " + daysRequired);
				if (skyblockRedux > 0) {
					failureResponse
							.add(ChatColor.BLUE + "Skyblock rank reduced requirement by " + skyblockRedux + " days");
				}
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: " + daysRequired);
				if (skyblockRedux > 0) {
					failureResponse
							.add(ChatColor.BLUE + "Skyblock rank reduced requirement by " + skyblockRedux + " days");
				}
			}
			if (balance >= SLAYER_RUNICS) {
				checkRunics = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + SLAYER_RUNICS);
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + SLAYER_RUNICS);
			}

			if (killsArray[5] >= 100 && killsArray[9] >= 45 && killsArray[8] >= 10) {
				checkKills = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY
						+ "You have killed enough monsters for this promotion.");
			} else {
				if (killsArray[5] > 100) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Your PIGZOMBIE kills: "
							+ killsArray[5] + "; Required: 100");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Your PIGZOMBIE kills: "
							+ killsArray[5] + "; Required: 100");
				}
				if (killsArray[9] >= 45) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Your ENDERMAN kills: "
							+ killsArray[9] + "; Required: 45");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Your ENDERMAN kills: "
							+ killsArray[9] + "; Required: 45");
				}
				if (killsArray[8] >= 10) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Your WITCH kills: "
							+ killsArray[8] + "; Required: 10");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Your WITCH kills: "
							+ killsArray[8] + "; Required: 10");
				}
			}

			if (RunicParadise.getPlayerMazeCompletionCount(user, 4) > 0) {
				checkMazes = true;
				failureResponse
						.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "You have completed the jungle maze!");
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY
						+ "You must complete the jungle maze! See Puzzle Kiosk at /games");
			}

			if (checkDays && checkRunics && checkKills && checkMazes) {
				if (execute == false) {
					// just checking... we're not executing the promotion!!
					user.sendMessage(
							ChatColor.DARK_GREEN + "[RunicRanks] Congratulations! You qualify for a promotion!");
					user.sendMessage(ChatColor.DARK_GREEN + "Promotion to Slayer costs " + SLAYER_RUNICS + " Runics.");
					user.sendMessage(ChatColor.DARK_GREEN + "Type " + ChatColor.AQUA + "/rankup now"
							+ ChatColor.DARK_GREEN + " to accept the promotion.");
				} else {
					// ok now we're executing the promotion.
					economy.withdrawPlayer(user.getName(), SLAYER_RUNICS);
					promotePlayer(user, "Slayer");
					perms.playerRemoveGroup(user, "Hunter");
					user.sendMessage(ChatColor.DARK_GREEN + "[RunicRanks] Congratulations! You have been promoted!");
					Ranks tempRank = new Ranks();
					tempRank.congratsPromotion(user.getName(), "Slayer");
					logPromotion(user.getName(), "Slayer", new Date().getTime());
					RunicParadise.playerProfiles.get(user.getUniqueId()).setChatColor(ChatColor.BLUE, true);
				}
			} else {
				ineligible = true;
			}
			break;
		case "Guard":

			daysRequired = ((float) HUNTER_DAYS) - skyblockRedux;

			// if player has been on the server long enough for promotion
			if (daysPlayed - daysRequired > -0.4) {
				checkDays = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: " + daysRequired);
				if (skyblockRedux > 0) {
					failureResponse
							.add(ChatColor.BLUE + "Skyblock rank reduced requirement by " + skyblockRedux + " days");
				}
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: " + daysRequired);
				if (skyblockRedux > 0) {
					failureResponse
							.add(ChatColor.BLUE + "Skyblock rank reduced requirement by " + skyblockRedux + " days");
				}
			}
			if (balance >= HUNTER_RUNICS) {
				checkRunics = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + HUNTER_RUNICS);
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + HUNTER_RUNICS);
			}

			if (killsArray[0] >= 300 && killsArray[1] >= 150 && killsArray[2] >= 300 && killsArray[7] >= 2) {
				checkKills = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY
						+ "You have killed enough monsters for this promotion.");
			} else {
				if (killsArray[0] > 300) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Your ZOMBIE kills: "
							+ killsArray[0] + "; Required: 300");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Your ZOMBIE kills: "
							+ killsArray[0] + "; Required: 300");
				}
				if (killsArray[1] >= 150) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Your SPIDER kills: "
							+ killsArray[1] + "; Required: 150");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Your SPIDER kills: "
							+ killsArray[1] + "; Required: 150");
				}
				if (killsArray[2] >= 300) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Your SKELETON kills: "
							+ killsArray[2] + "; Required: 300");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Your SKELETON kills: "
							+ killsArray[2] + "; Required: 300");
				}
				if (killsArray[7] >= 2) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Your WITHER kills: "
							+ killsArray[7] + "; Required: 2");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Your WITHER kills: "
							+ killsArray[7] + "; Required: 2");
				}
			}
			if (targetPlayer.getFaithPowerLevel() >= 150) {
				checkFaiths = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY
						+ "You have faith power level of at least 150");
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY
						+ "You need faith power level 150 or more. Power level is the combined level of all your faiths - check it in /faith stats.");
			}

			if (targetPlayer.getMasteredJobCount() > 1) {
				checkJobMasteries = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "You have mastered "
						+ targetPlayer.getMasteredJobCount() + " jobs.");
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY
						+ "You havent mastered 2 jobs. Get info @ /warp jobs");
			}

			if (RunicParadise.getPlayerMazeCompletionCount(user, 5) > 0) {
				checkMazes = true;
				failureResponse
						.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "You have completed the frost maze!");
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY
						+ "You must complete the frost maze! See Puzzle Kiosk at /games");
			}

			if (checkDays && checkRunics && checkKills && checkJobMasteries && checkMazes && checkFaiths) {
				if (execute == false) {
					// just checking... we're not executing the promotion!!
					user.sendMessage(
							ChatColor.DARK_GREEN + "[RunicRanks] Congratulations! You qualify for a promotion!");
					user.sendMessage(ChatColor.DARK_GREEN + "Promotion to Hunter costs " + HUNTER_RUNICS + " Runics.");
					user.sendMessage(ChatColor.DARK_GREEN + "Type " + ChatColor.AQUA + "/rankup now"
							+ ChatColor.DARK_GREEN + " to accept the promotion.");
				} else {
					// ok now we're executing the promotion.
					economy.withdrawPlayer(user.getName(), HUNTER_RUNICS);
					promotePlayer(user, "Hunter");
					perms.playerRemoveGroup(user, "Guard");
					user.sendMessage(ChatColor.DARK_GREEN + "[RunicRanks] Congratulations! You have been promoted!");
					Ranks tempRank = new Ranks();
					tempRank.congratsPromotion(user.getName(), "Hunter");
					logPromotion(user.getName(), "Hunter", new Date().getTime());
					RunicParadise.playerProfiles.get(user.getUniqueId()).setChatColor(ChatColor.BLUE, true);
				}
			} else {
				ineligible = true;
			}
			break;
		case "Keeper":

			daysRequired = ((float) GUARD_DAYS) - skyblockRedux;

			// if player has been on the server long enough for promotion
			if (daysPlayed - daysRequired > -0.4) {
				checkDays = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: " + daysRequired);
				if (skyblockRedux > 0) {
					failureResponse
							.add(ChatColor.BLUE + "Skyblock rank reduced requirement by " + skyblockRedux + " days");
				}
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: " + daysRequired);
				if (skyblockRedux > 0) {
					failureResponse
							.add(ChatColor.BLUE + "Skyblock rank reduced requirement by " + skyblockRedux + " days");
				}
			}
			if (balance >= GUARD_RUNICS) {
				checkRunics = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + GUARD_RUNICS);
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + GUARD_RUNICS);
			}

			if (killsArray[5] >= 50 && killsArray[9] >= 30 && killsArray[8] >= 5) {
				checkKills = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY
						+ "You have killed enough monsters for this promotion.");
			} else {
				if (killsArray[5] > 50) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Your PIGZOMBIE kills: "
							+ killsArray[5] + "; Required: 50");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Your PIGZOMBIE kills: "
							+ killsArray[5] + "; Required: 50");
				}
				if (killsArray[9] >= 30) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Your ENDERMAN kills: "
							+ killsArray[9] + "; Required: 30");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Your ENDERMAN kills: "
							+ killsArray[9] + "; Required: 30");
				}
				if (killsArray[8] >= 5) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Your WITCH kills: "
							+ killsArray[8] + "; Required: 5");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Your WITCH kills: "
							+ killsArray[8] + "; Required: 5");
				}
			}

			if (RunicParadise.getPlayerMazeCompletionCount(user, 4) > 0) {
				checkMazes = true;
				failureResponse
						.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "You have completed the jungle maze!");
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY
						+ "You must complete the jungle maze! See Puzzle Kiosk at /games");
			}

			if (checkDays && checkRunics && checkKills && checkMazes) {
				if (execute == false) {
					// just checking... we're not executing the promotion!!
					user.sendMessage(
							ChatColor.DARK_GREEN + "[RunicRanks] Congratulations! You qualify for a promotion!");
					user.sendMessage(ChatColor.DARK_GREEN + "Promotion to Guard costs " + GUARD_RUNICS + " Runics.");
					user.sendMessage(ChatColor.DARK_GREEN + "Type " + ChatColor.AQUA + "/rankup now"
							+ ChatColor.DARK_GREEN + " to accept the promotion.");
				} else {
					// ok now we're executing the promotion.
					economy.withdrawPlayer(user.getName(), GUARD_RUNICS);
					promotePlayer(user, "Guard");
					perms.playerRemoveGroup(user, "Keeper");
					user.sendMessage(ChatColor.DARK_GREEN + "[RunicRanks] Congratulations! You have been promoted!");
					Ranks tempRank = new Ranks();
					tempRank.congratsPromotion(user.getName(), "Guard");
					logPromotion(user.getName(), "Guard", new Date().getTime());
					RunicParadise.playerProfiles.get(user.getUniqueId()).setChatColor(ChatColor.DARK_AQUA, true);
				}
			} else {
				ineligible = true;
			}
			break;
		case "Brawler":

			daysRequired = ((float) KEEPER_DAYS) - skyblockRedux;

			// if player has been on the server long enough for promotion
			if (daysPlayed - daysRequired > -0.4) {
				checkDays = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: " + daysRequired);
				if (skyblockRedux > 0) {
					failureResponse
							.add(ChatColor.BLUE + "Skyblock rank reduced requirement by " + skyblockRedux + " days");
				}
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: " + daysRequired);
				if (skyblockRedux > 0) {
					failureResponse
							.add(ChatColor.BLUE + "Skyblock rank reduced requirement by " + skyblockRedux + " days");
				}
			}
			if (balance >= KEEPER_RUNICS) {
				checkRunics = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + KEEPER_RUNICS);
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + KEEPER_RUNICS);
			}
			if (killsArray[0] >= 200 && killsArray[1] >= 100 && killsArray[2] >= 200 && killsArray[7] >= 1) {
				checkKills = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY
						+ "You have killed enough monsters for this promotion.");
			} else {
				if (killsArray[0] > 200) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Your ZOMBIE kills: "
							+ killsArray[0] + "; Required: 200");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Your ZOMBIE kills: "
							+ killsArray[0] + "; Required: 200");
				}
				if (killsArray[1] >= 100) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Your SPIDER kills: "
							+ killsArray[1] + "; Required: 100");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Your SPIDER kills: "
							+ killsArray[1] + "; Required: 100");
				}
				if (killsArray[2] >= 200) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Your SKELETON kills: "
							+ killsArray[2] + "; Required: 200");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Your SKELETON kills: "
							+ killsArray[2] + "; Required: 200");
				}
				if (killsArray[7] >= 1) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Your WITHER kills: "
							+ killsArray[7] + "; Required: 1");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Your WITHER kills: "
							+ killsArray[7] + "; Required: 1");
				}
			}

			if (targetPlayer.getFaithPowerLevel() >= 100) {
				checkFaiths = true;
				failureResponse.add(
						ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "You have at least 100 faith power level");
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY
						+ "You need faith power level 100 or more. Power level is the combined level of all your faiths - check it in /faith stats.");
			}

			if (targetPlayer.getMasteredJobCount() > 0) {
				checkJobMasteries = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "You have mastered a job.");
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY
						+ "You need to master a job. Get info @ /warp jobs");
			}

			if (checkDays && checkRunics && checkKills && checkFaiths && checkJobMasteries) {
				if (execute == false) {
					// just checking... we're not executing the promotion!!
					user.sendMessage(
							ChatColor.DARK_GREEN + "[RunicRanks] Congratulations! You qualify for a promotion!");
					user.sendMessage(ChatColor.DARK_GREEN + "Promotion to Keeper costs " + KEEPER_RUNICS + " Runics.");
					user.sendMessage(ChatColor.DARK_GREEN + "Type " + ChatColor.AQUA + "/rankup now"
							+ ChatColor.DARK_GREEN + " to accept the promotion.");
				} else {
					// ok now we're executing the promotion.
					economy.withdrawPlayer(user.getName(), KEEPER_RUNICS);
					promotePlayer(user, "Keeper");
					perms.playerRemoveGroup(user, "Brawler");
					user.sendMessage(ChatColor.DARK_GREEN + "[RunicRanks] Congratulations! You have been promoted!");
					Ranks tempRank = new Ranks();
					tempRank.congratsPromotion(user.getName(), "Keeper");
					logPromotion(user.getName(), "Keeper", new Date().getTime());
					RunicParadise.playerProfiles.get(user.getUniqueId()).setChatColor(ChatColor.AQUA, true);
				}
			} else {
				ineligible = true;
			}
			break;
		case "Singer":

			daysRequired = ((float) BRAWLER_DAYS) - skyblockRedux;

			// if player has been on the server long enough for promotion
			if (daysPlayed - daysRequired > -0.4) {
				checkDays = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: " + daysRequired);
				if (skyblockRedux > 0) {
					failureResponse
							.add(ChatColor.BLUE + "Skyblock rank reduced requirement by " + skyblockRedux + " days");
				}
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: " + daysRequired);
				if (skyblockRedux > 0) {
					failureResponse
							.add(ChatColor.BLUE + "Skyblock rank reduced requirement by " + skyblockRedux + " days");
				}
			}
			if (balance >= BRAWLER_RUNICS) {
				checkRunics = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + BRAWLER_RUNICS);
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + BRAWLER_RUNICS);
			}
			if (killsArray[5] >= 25 && killsArray[9] >= 15 && killsArray[8] >= 2) {
				checkKills = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY
						+ "You have killed enough monsters for this promotion.");
			} else {
				if (killsArray[5] > 25) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Your PIGZOMBIE kills: "
							+ killsArray[5] + "; Required: 25");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Your PIGZOMBIE kills: "
							+ killsArray[5] + "; Required: 25");
				}
				if (killsArray[9] >= 15) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Your ENDERMAN kills: "
							+ killsArray[9] + "; Required: 15");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Your ENDERMAN kills: "
							+ killsArray[9] + "; Required: 15");
				}
				if (killsArray[8] >= 2) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Your WITCH kills: "
							+ killsArray[8] + "; Required: 2");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Your WITCH kills: "
							+ killsArray[8] + "; Required: 2");
				}
			}

			if (RunicParadise.getPlayerMazeCompletionCount(user, 2) > 0) {
				checkMazes = true;
				failureResponse
						.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "You have completed the sky maze!");
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY
						+ "You must complete the sky maze! See Games Portal at /spawn");
			}

			if (checkDays && checkRunics && checkKills && checkMazes) {
				if (execute == false) {
					// just checking... we're not executing the promotion!!
					user.sendMessage(
							ChatColor.DARK_GREEN + "[RunicRanks] Congratulations! You qualify for a promotion!");
					user.sendMessage(
							ChatColor.DARK_GREEN + "Promotion to Brawler costs " + BRAWLER_RUNICS + " Runics.");
					user.sendMessage(ChatColor.DARK_GREEN + "Type " + ChatColor.AQUA + "/rankup now"
							+ ChatColor.DARK_GREEN + " to accept the promotion.");
				} else {
					// ok now we're executing the promotion.
					economy.withdrawPlayer(user.getName(), BRAWLER_RUNICS);
					promotePlayer(user, "Brawler");
					perms.playerRemoveGroup(user, "Singer");
					user.sendMessage(ChatColor.DARK_GREEN + "[RunicRanks] Congratulations! You have been promoted!");
					Ranks tempRank = new Ranks();
					tempRank.congratsPromotion(user.getName(), "Brawler");
					logPromotion(user.getName(), "Brawler", new Date().getTime());
					RunicParadise.playerProfiles.get(user.getUniqueId()).setChatColor(ChatColor.GOLD, true);
				}
			} else {
				ineligible = true;
			}
			break;
		case "Runner":

			daysRequired = ((float) SINGER_DAYS) - skyblockRedux;

			// if player has been on the server long enough for promotion
			if (daysPlayed - daysRequired > -0.4) {
				checkDays = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: " + daysRequired);
				if (skyblockRedux > 0) {
					failureResponse
							.add(ChatColor.BLUE + "Skyblock rank reduced requirement by " + skyblockRedux + " days");
				}
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: " + daysRequired);
				if (skyblockRedux > 0) {
					failureResponse
							.add(ChatColor.BLUE + "Skyblock rank reduced requirement by " + skyblockRedux + " days");
				}
			}
			if (balance >= SINGER_RUNICS) {
				checkRunics = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + SINGER_RUNICS);
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + SINGER_RUNICS);
			}
			if (killsArray[0] >= 100 && killsArray[1] >= 50 && killsArray[2] >= 100) {
				checkKills = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY
						+ "You have killed enough monsters for this promotion.");
			} else {
				if (killsArray[0] > 100) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Your ZOMBIE kills: "
							+ killsArray[0] + "; Required: 100");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Your ZOMBIE kills: "
							+ killsArray[0] + "; Required: 100");
				}
				if (killsArray[1] >= 50) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Your SPIDER kills: "
							+ killsArray[1] + "; Required: 50");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Your SPIDER kills: "
							+ killsArray[1] + "; Required: 50");
				}
				if (killsArray[2] >= 100) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Your SKELETON kills: "
							+ killsArray[2] + "; Required: 100");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Your SKELETON kills: "
							+ killsArray[2] + "; Required: 100");
				}
			}
			if (RunicParadise.getPlayerMazeCompletionCount(user, 1) > 0) {
				checkMazes = true;
				failureResponse
						.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "You have completed the hedge maze!");
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY
						+ "You must complete the hedge maze! See Games Portal at /spawn");
			}

			if (checkDays && checkRunics && checkKills // && checkFaiths
					&& checkMazes) {
				if (execute == false) {
					// just checking... we're not executing the promotion!!
					user.sendMessage(
							ChatColor.DARK_GREEN + "[RunicRanks] Congratulations! You qualify for a promotion!");
					user.sendMessage(ChatColor.DARK_GREEN + "Promotion to Singer costs " + SINGER_RUNICS + " Runics.");
					user.sendMessage(ChatColor.DARK_GREEN + "Type " + ChatColor.AQUA + "/rankup now"
							+ ChatColor.DARK_GREEN + " to accept the promotion.");
				} else {
					// ok now we're executing the promotion.
					economy.withdrawPlayer(user.getName(), SINGER_RUNICS);
					promotePlayer(user, "Singer");
					perms.playerRemoveGroup(user, "Runner");
					user.sendMessage(ChatColor.DARK_GREEN + "[RunicRanks] Congratulations! You have been promoted!");
					Ranks tempRank = new Ranks();
					tempRank.congratsPromotion(user.getName(), "Singer");
					logPromotion(user.getName(), "Singer", new Date().getTime());
					RunicParadise.playerProfiles.get(user.getUniqueId()).setChatColor(ChatColor.YELLOW, true);
				}
			} else {
				ineligible = true;
			}
			break;
		case "Seeker":

			daysRequired = ((float) RUNNER_DAYS) - skyblockRedux;

			// if player has been on the server long enough for promotion
			if (daysPlayed - daysRequired > -0.4) {
				checkDays = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: " + daysRequired);
				if (skyblockRedux > 0) {
					failureResponse
							.add(ChatColor.BLUE + "Skyblock rank reduced requirement by " + skyblockRedux + " days");
				}
			} else if (targetPlayer.getPlayerVoteCount() >= 15) {
				checkDays = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Votes: "
						+ targetPlayer.getPlayerVoteCount() + "; Required: 15");
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: " + daysRequired);
				if (skyblockRedux > 0) {
					failureResponse
							.add(ChatColor.BLUE + "Skyblock rank reduced requirement by " + skyblockRedux + " days");
				}
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY
						+ "You can skip the days requirement with 15 votes! Your votes: "
						+ targetPlayer.getPlayerVoteCount());
			}
			if (balance >= RUNNER_RUNICS) {
				checkRunics = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + RUNNER_RUNICS);
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + RUNNER_RUNICS);
			}
			if (killsArray[0] >= 30 && killsArray[1] >= 15 && killsArray[2] >= 30) {
				checkKills = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY
						+ "You have killed enough monsters for this promotion.");
			} else {
				if (killsArray[0] > 30) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Your ZOMBIE kills: "
							+ killsArray[0] + "; Required: 30");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Your ZOMBIE kills: "
							+ killsArray[0] + "; Required: 30");
				}
				if (killsArray[1] >= 15) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Your SPIDER kills: "
							+ killsArray[1] + "; Required: 15");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Your SPIDER kills: "
							+ killsArray[1] + "; Required: 15");
				}
				if (killsArray[2] >= 30) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY + "Your SKELETON kills: "
							+ killsArray[2] + "; Required: 30");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY + "Your SKELETON kills: "
							+ killsArray[2] + "; Required: 30");
				}
			}

			if (checkDays && checkRunics && checkKills) {
				if (execute) {
					// ok now we're executing the promotion.
					economy.withdrawPlayer(user.getName(), RUNNER_RUNICS);
					promotePlayer(user, "Runner");
					perms.playerRemoveGroup(user, "Settler");
					user.sendMessage(ChatColor.DARK_GREEN + "[RunicRanks] Congratulations! You have been promoted!");
					Ranks tempRank = new Ranks();
					tempRank.congratsPromotion(user.getName(), "Runner");
					logPromotion(user.getName(), "Runner", new Date().getTime());
					RunicParadise.playerProfiles.get(user.getUniqueId()).setChatColor(ChatColor.DARK_GREEN, true);
				} else {
					// just checking... we're not executing the promotion!!
					user.sendMessage(
							ChatColor.DARK_GREEN + "[RunicRanks] Congratulations! You qualify for a promotion!");
					user.sendMessage(ChatColor.DARK_GREEN + "Promotion to Runner costs " + RUNNER_RUNICS + " Runics.");
					user.sendMessage(ChatColor.DARK_GREEN + "Type " + ChatColor.AQUA + "/rankup now"
							+ ChatColor.DARK_GREEN + " to accept the promotion.");
				}
			} else {
				ineligible = true;
			}
			break;
		default:
			user.sendMessage(ChatColor.RED + "[RunicRanks] There are no more promotions at your rank!");
			break;
		}

		// if player isnt eligible, give them all the stored responses to tell
		// them why
		if (ineligible) {
			user.sendMessage(ChatColor.RED + "[RunicRanks] You do not qualify for a promotion yet...");
			String[] itemArray = new String[failureResponse.size()];
			String[] returnedArray = failureResponse.toArray(itemArray);

			user.sendMessage(returnedArray);
		}

		// Close the connection
		try {
			d.close();
		} catch (SQLException e) {
			getLogger().log(Level.SEVERE, "Cant close mysql conn after checkpromotion: " + e.getMessage());
		}
	}

	private static void logPromotion(String playerName, String newRank, Long timestamp) {
		final Plugin instance = RunicParadise.getInstance();
		MySQL MySQL = new MySQL(instance, instance.getConfig().getString("dbHost"),
				instance.getConfig().getString("dbPort"), instance.getConfig().getString("dbDatabase"),
				instance.getConfig().getString("dbUser"), instance.getConfig().getString("dbPassword"));
		try {

			final Connection d = MySQL.openConnection();
			Statement dStmt = d.createStatement();
			int tempD = dStmt
					.executeUpdate("INSERT INTO rp_PlayerPromotions (`PlayerName`, `NewRank`, `TimeStamp`) VALUES "
							+ "('" + playerName + "', '" + newRank + "', " + timestamp + ");");
			d.close();
		} catch (SQLException z) {
			getLogger().log(Level.SEVERE, "Failed DB check for restore grave cuz " + z.getMessage());
		}
	}

	private static void promotePlayer(Player p, String newRank) {
		RunicParadise.perms.playerAddGroup(null, Bukkit.getOfflinePlayer(p.getUniqueId()), newRank);
	}

	static boolean craftFeudalJewelry(Player p, String rank) {
		boolean checkGem = false;
		boolean checkMetal = false;
		boolean checkEssence = false;
		List<String> messages = new ArrayList<>();

		if (rank.equalsIgnoreCase("Duke")) {
			if (p.getInventory().containsAtLeast(Borderlands.specialLootDrops("DukeGem", p.getUniqueId()), 64)) {
				checkGem = true;
				messages.add(ChatColor.GREEN + "You have enough gems!");
			} else {
				messages.add(ChatColor.RED + "You don't have enough gems!");
			}
			if (p.getInventory().containsAtLeast(Borderlands.specialLootDrops("DukeMetal", p.getUniqueId()), 64)) {
				checkMetal = true;
				messages.add(ChatColor.GREEN + "You have enough bits of metal!");
			} else {
				messages.add(ChatColor.RED + "You don't have enough bits of metal!");
			}
			if (p.getInventory().containsAtLeast(Borderlands.specialLootDrops("DukeEssence", p.getUniqueId()), 64)) {
				checkEssence = true;
				messages.add(ChatColor.GREEN + "You have enough piles of essence!");
			} else {
				messages.add(ChatColor.RED + "You don't have enough piles of essence!");
			}

			for (String s : messages) {
				p.sendMessage(s);
			}

			if (checkGem && checkMetal && checkEssence) {
				// player has all needed materials!

				int counter = 0;

				ItemStack essence = Borderlands.specialLootDrops("DukeEssence", p.getUniqueId());
				essence.setAmount(64);
				ItemStack metal = Borderlands.specialLootDrops("DukeMetal", p.getUniqueId());
				metal.setAmount(64);
				ItemStack gem = Borderlands.specialLootDrops("DukeGem", p.getUniqueId());
				gem.setAmount(64);

				p.getInventory().removeItem(essence);
				p.getInventory().removeItem(metal);
				p.getInventory().removeItem(gem);

				p.updateInventory();

				int random = ThreadLocalRandom.current().nextInt(1, 100 + 1);
				if (random < 25) {
					Bukkit.getWorld(p.getLocation().getWorld().getUID()).dropItemNaturally(p.getLocation(),
							Borderlands.specialLootDrops("DukeRing1", p.getUniqueId()));
				} else if (random < 50) {
					Bukkit.getWorld(p.getLocation().getWorld().getUID()).dropItemNaturally(p.getLocation(),
							Borderlands.specialLootDrops("DukeRing2", p.getUniqueId()));
				} else if (random < 75) {
					Bukkit.getWorld(p.getLocation().getWorld().getUID()).dropItemNaturally(p.getLocation(),
							Borderlands.specialLootDrops("DukeRing3", p.getUniqueId()));
				} else {
					Bukkit.getWorld(p.getLocation().getWorld().getUID()).dropItemNaturally(p.getLocation(),
							Borderlands.specialLootDrops("DukeRing4", p.getUniqueId()));
				}

				return true;

			} else {
				return false;
			}
		} else if (rank.equalsIgnoreCase("Baron")) {
			if (p.getInventory().containsAtLeast(Borderlands.specialLootDrops("BaronGem", null), 1)) {
				checkGem = true;
				messages.add(ChatColor.GREEN + "You have the jewel!");
			} else {
				messages.add(ChatColor.RED
						+ "You don't have the prismatic jewel! Check the slimefun guide in the anvil category.");
			}
			if (p.getInventory().containsAtLeast(Borderlands.specialLootDrops("BaronIngot1", null), 1)) {
				checkMetal = true;
				messages.add(ChatColor.GREEN + "You have the empowered ingot!");
			} else {
				messages.add(ChatColor.RED
						+ "You don't have the empowered ingot. Check the slimefun guide in the anvil category.");
			}
			if (p.getInventory().containsAtLeast(Borderlands.specialLootDrops("BaronIngot2", null), 1)) {
				checkEssence = true;
				messages.add(ChatColor.GREEN + "You have the unstable ingot!");
			} else {
				messages.add(ChatColor.RED
						+ "You don't have the unstable ingot. Check the slimefun guide in the anvil category.");
			}

			for (String s : messages) {
				p.sendMessage(s);
			}

			if (checkGem && checkMetal && checkEssence) {
				// player has all needed materials!

				ItemStack essence = Borderlands.specialLootDrops("BaronGem", null);
				essence.setAmount(1);
				ItemStack metal = Borderlands.specialLootDrops("BaronIngot1", null);
				metal.setAmount(1);
				ItemStack gem = Borderlands.specialLootDrops("BaronIngot2", null);
				gem.setAmount(1);

				p.getInventory().removeItem(essence);
				p.getInventory().removeItem(metal);
				p.getInventory().removeItem(gem);

				p.updateInventory();

				int random = ThreadLocalRandom.current().nextInt(1, 100 + 1);

				if (random < 50) {
					Bukkit.getWorld(p.getLocation().getWorld().getUID()).dropItemNaturally(p.getLocation(),
							Borderlands.specialLootDrops("BaronPendant1", p.getUniqueId()));
				} else {
					Bukkit.getWorld(p.getLocation().getWorld().getUID()).dropItemNaturally(p.getLocation(),
							Borderlands.specialLootDrops("BaronPendant2", p.getUniqueId()));
				}
				return true;
			} else {
				return false;
			}
		}

		return false;
	}

	private static String[] ALLOWED_WORLDS_FOR_DUKE_BARON = new String[] {
			"RunicRealm",
			"_nether",
			"the_end",
			"Mining"
	};

	static void applyFeudalBonus(Player p, String toWorld, String fromWorld) {
		boolean hasDukeRing = p.getEnderChest().contains(Borderlands.specialLootDrops("DukeRing1", p.getUniqueId()))
				|| p.getEnderChest().contains(Borderlands.specialLootDrops("DukeRing2", p.getUniqueId()))
				|| p.getEnderChest().contains(Borderlands.specialLootDrops("DukeRing3", p.getUniqueId()))
				|| p.getEnderChest().contains(Borderlands.specialLootDrops("DukeRing4", p.getUniqueId()));
		boolean toWorldIsNormal = Arrays.stream(ALLOWED_WORLDS_FOR_DUKE_BARON).anyMatch(toWorld::contains);
		boolean fromWorldIsNormal = Arrays.stream(ALLOWED_WORLDS_FOR_DUKE_BARON).anyMatch(fromWorld::contains);

		if (hasDukeRing && p.hasPermission("rp.ranks.duke")) {
			if (toWorldIsNormal) {
				if (p.getMaxHealth() < 24) {
					p.setMaxHealth(24);
					if (!fromWorldIsNormal) {
						RunicMessaging.sendMessage(p, RunicFormat.RANKS, "Your ring glows brightly!");
					}
				}
			} else {
				p.resetMaxHealth();
			}
		} else {
			p.resetMaxHealth();
		}

		if ((p.getEnderChest().contains(Borderlands.specialLootDrops("BaronPendant1", p.getUniqueId())))
				|| p.getEnderChest().contains(Borderlands.specialLootDrops("BaronPendant2", p.getUniqueId()))) {
			if (toWorldIsNormal) {
				if (p.getMaximumAir() < 360) {
					p.setMaximumAir(360);
					p.setRemainingAir(360);
					if (!fromWorldIsNormal) {
						RunicMessaging.sendMessage(p, RunicFormat.RANKS, "Your pendant buzzes with energy!");
					}
				} else {
					p.setMaximumAir(300);
				}
			}
		}
	}
/*
	public static void registerSlimefunItems() {

		Category category = new Category(
				new CustomItem(new MaterialData(Material.ANVIL), "&4Runic Specialties", "", "&a> Click to open"));

		category.register();

		SlimefunItem baronDiamondPart = new SlimefunItem(category,
				new CustomItem(new MaterialData(Material.DIAMOND), "&eMist-Infused Diamond"), "MIST_INFUSED_DIAMOND",
				RecipeType.ANCIENT_ALTAR,
				new ItemStack[] { SlimefunItem.getItem("ANCIENT_RUNE_WATER"), SlimefunItem.getItem("SYNTHETIC_DIAMOND"),
						SlimefunItem.getItem("ANCIENT_RUNE_AIR"), SlimefunItem.getItem("SILVER_DUST"),
						new ItemStack(Material.INK_SACK, 1, (short) DyeColor.LIGHT_BLUE.getDyeData()),
						SlimefunItem.getItem("SILVER_DUST"), SlimefunItem.getItem("ANCIENT_RUNE_AIR"),
						SlimefunItem.getItem("SYNTHETIC_DIAMOND"), SlimefunItem.getItem("ANCIENT_RUNE_WATER") });
		baronDiamondPart.register();

		SlimefunItem baronDiamond2Part = new SlimefunItem(category,
				new CustomItem(new MaterialData(Material.DIAMOND), "&eLava-Infused Diamond"), "LAVA_INFUSED_DIAMOND",
				RecipeType.ANCIENT_ALTAR,
				new ItemStack[] { SlimefunItem.getItem("ANCIENT_RUNE_FIRE"), SlimefunItem.getItem("SYNTHETIC_DIAMOND"),
						SlimefunItem.getItem("ANCIENT_RUNE_EARTH"), SlimefunItem.getItem("SILVER_DUST"),
						new ItemStack(Material.INK_SACK, 1, (short) DyeColor.ORANGE.getDyeData()),
						SlimefunItem.getItem("SILVER_DUST"), SlimefunItem.getItem("ANCIENT_RUNE_EARTH"),
						SlimefunItem.getItem("SYNTHETIC_DIAMOND"), SlimefunItem.getItem("ANCIENT_RUNE_FIRE") });
		baronDiamond2Part.register();

		SlimefunItem baronEmeraldPart = new SlimefunItem(category,
				new CustomItem(new MaterialData(Material.EMERALD), "&eMist-Infused Emerald"), "MIST_INFUSED_EMERALD",
				RecipeType.ANCIENT_ALTAR,
				new ItemStack[] { SlimefunItem.getItem("ANCIENT_RUNE_WATER"), SlimefunItem.getItem("SYNTHETIC_EMERALD"),
						SlimefunItem.getItem("ANCIENT_RUNE_AIR"), SlimefunItem.getItem("ZINC_DUST"),
						new ItemStack(Material.INK_SACK, 1, (short) DyeColor.LIGHT_BLUE.getDyeData()),
						SlimefunItem.getItem("ZINC_DUST"), SlimefunItem.getItem("ANCIENT_RUNE_AIR"),
						SlimefunItem.getItem("SYNTHETIC_EMERALD"), SlimefunItem.getItem("ANCIENT_RUNE_WATER") });
		baronEmeraldPart.register();

		SlimefunItem baronEmerald2Part = new SlimefunItem(category,
				new CustomItem(new MaterialData(Material.EMERALD), "&eLava-Infused Emerald"), "LAVA_INFUSED_EMERALD",
				RecipeType.ANCIENT_ALTAR,
				new ItemStack[] { SlimefunItem.getItem("ANCIENT_RUNE_FIRE"), SlimefunItem.getItem("SYNTHETIC_EMERALD"),
						SlimefunItem.getItem("ANCIENT_RUNE_EARTH"), SlimefunItem.getItem("ZINC_DUST"),
						new ItemStack(Material.INK_SACK, 1, (short) DyeColor.ORANGE.getDyeData()),
						SlimefunItem.getItem("ZINC_DUST"), SlimefunItem.getItem("ANCIENT_RUNE_EARTH"),
						SlimefunItem.getItem("SYNTHETIC_EMERALD"), SlimefunItem.getItem("ANCIENT_RUNE_FIRE") });
		baronEmerald2Part.register();

		SlimefunItem baronSapphirePart = new SlimefunItem(category,
				new CustomItem(new ItemStack(Material.INK_SACK, 1, (short) DyeColor.BLUE.getDyeData()),
						"&eMist-Infused Sapphire"),
				"MIST_INFUSED_SAPPHIRE", RecipeType.ANCIENT_ALTAR,
				new ItemStack[] { SlimefunItem.getItem("ANCIENT_RUNE_WATER"),
						SlimefunItem.getItem("SYNTHETIC_SAPPHIRE"), SlimefunItem.getItem("ANCIENT_RUNE_AIR"),
						SlimefunItem.getItem("ALUMINUM_DUST"),
						new ItemStack(Material.INK_SACK, 1, (short) DyeColor.LIGHT_BLUE.getDyeData()),
						SlimefunItem.getItem("ALUMINUM_DUST"), SlimefunItem.getItem("ANCIENT_RUNE_AIR"),
						SlimefunItem.getItem("SYNTHETIC_SAPPHIRE"), SlimefunItem.getItem("ANCIENT_RUNE_WATER") });
		baronSapphirePart.register();

		SlimefunItem baronSapphire2Part = new SlimefunItem(category,
				new CustomItem(new ItemStack(Material.INK_SACK, 1, (short) DyeColor.BLUE.getDyeData()),
						"&eLava-Infused Sapphire"),
				"LAVA_INFUSED_SAPPHIRE", RecipeType.ANCIENT_ALTAR,
				new ItemStack[] { SlimefunItem.getItem("ANCIENT_RUNE_FIRE"), SlimefunItem.getItem("SYNTHETIC_SAPPHIRE"),
						SlimefunItem.getItem("ANCIENT_RUNE_EARTH"), SlimefunItem.getItem("ALUMINUM_DUST"),
						new ItemStack(Material.INK_SACK, 1, (short) DyeColor.ORANGE.getDyeData()),
						SlimefunItem.getItem("ALUMINUM_DUST"), SlimefunItem.getItem("ANCIENT_RUNE_EARTH"),
						SlimefunItem.getItem("SYNTHETIC_SAPPHIRE"), SlimefunItem.getItem("ANCIENT_RUNE_FIRE") });
		baronSapphire2Part.register();

		SlimefunItem baronMistyTopaz = new SlimefunItem(category,
				new CustomItem(new ItemStack(Material.INK_SACK, 1, (short) DyeColor.LIGHT_BLUE.getDyeData()),
						"&ePrismatic Topaz"),
				"PRISMATIC_TOPAZ", RecipeType.MAGIC_WORKBENCH,
				new ItemStack[] { SlimefunItem.getItem("ANCIENT_RUNE_RAINBOW"),
						SlimefunItem.getItem("MIST_INFUSED_EMERALD"), SlimefunItem.getItem("ANCIENT_RUNE_RAINBOW"),
						null, new ItemStack(Material.INK_SACK, 1, (short) DyeColor.LIGHT_BLUE.getDyeData()), null,
						SlimefunItem.getItem("MIST_INFUSED_DIAMOND"), SlimefunItem.getItem("ANCIENT_RUNE_RAINBOW"),
						SlimefunItem.getItem("MIST_INFUSED_SAPPHIRE") });
		baronMistyTopaz.register();

		SlimefunItem baronMistyCitrine = new SlimefunItem(category,
				new CustomItem(new ItemStack(Material.INK_SACK, 1, (short) DyeColor.ORANGE.getDyeData()),
						"&ePrismatic Citrine"),
				"PRISMATIC_CITRINE", RecipeType.MAGIC_WORKBENCH,
				new ItemStack[] { SlimefunItem.getItem("ANCIENT_RUNE_RAINBOW"),
						SlimefunItem.getItem("LAVA_INFUSED_EMERALD"), SlimefunItem.getItem("ANCIENT_RUNE_RAINBOW"),
						null, new ItemStack(Material.INK_SACK, 1, (short) DyeColor.YELLOW.getDyeData()), null,
						SlimefunItem.getItem("LAVA_INFUSED_DIAMOND"), SlimefunItem.getItem("ANCIENT_RUNE_RAINBOW"),
						SlimefunItem.getItem("LAVA_INFUSED_SAPPHIRE") });
		baronMistyCitrine.register();

		SlimefunItem baronJewel = new SlimefunItem(category, Borderlands.specialLootDrops("BaronGem", null),
				"BARON_JEWEL", RecipeType.MAGIC_WORKBENCH,
				new ItemStack[] { new ItemStack(Material.NETHER_STAR), SlimefunItem.getItem("PRISMATIC_CITRINE"),
						new ItemStack(Material.NETHER_STAR), SlimefunItem.getItem("ANCIENT_RUNE_ENDER"),
						new ItemStack(Material.INK_SACK, 1, (short) 12), SlimefunItem.getItem("ANCIENT_RUNE_ENDER"),
						new ItemStack(Material.NETHER_STAR), SlimefunItem.getItem("PRISMATIC_TOPAZ"),
						new ItemStack(Material.NETHER_STAR) });
		baronJewel.register();

		SlimefunItem baronIngot1 = new SlimefunItem(category, Borderlands.specialLootDrops("BaronIngot1", null),
				"EMPOWERED_SILVER_INGOT", RecipeType.SMELTERY,
				new ItemStack[] { Borderlands.specialLootDrops("BaronMetal", null),
						Borderlands.specialLootDrops("BaronMetal", null),
						Borderlands.specialLootDrops("BaronMetal", null), SlimefunItem.getItem("NETHER_ICE"),
						SlimefunItem.getItem("NETHER_ICE"), SlimefunItem.getItem("NETHER_ICE"),
						Borderlands.specialLootDrops("BaronMetal", null),
						Borderlands.specialLootDrops("BaronMetal", null),
						Borderlands.specialLootDrops("BaronMetal", null) });
		baronIngot1.register();

		SlimefunItem baronIngot2 = new SlimefunItem(category, Borderlands.specialLootDrops("BaronIngot2", null),
				"CARVED_SILVER_INGOT", RecipeType.SMELTERY,
				new ItemStack[] { SlimefunItem.getItem("BLISTERING_INGOT_2"), null,
						SlimefunItem.getItem("BLISTERING_INGOT_2"), SlimefunItem.getItem("ENRICHED_NETHER_ICE"),
						SlimefunItem.getItem("ENRICHED_NETHER_ICE"), SlimefunItem.getItem("ENRICHED_NETHER_ICE"),
						SlimefunItem.getItem("BLISTERING_INGOT_2"), null, SlimefunItem.getItem("BLISTERING_INGOT_2") });
		baronIngot2.register();

	}*/

}
