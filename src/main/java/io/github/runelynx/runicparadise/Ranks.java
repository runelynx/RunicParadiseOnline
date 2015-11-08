/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.runelynx.runicparadise;

import static io.github.runelynx.runicparadise.RunicParadise.economy;
import static io.github.runelynx.runicparadise.RunicParadise.perms;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;

import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;

import static org.bukkit.Bukkit.getLogger;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import com.connorlinfoot.titleapi.*;

/**
 *
 * @author Andrew
 */
public class Ranks {

	private Plugin instance = RunicParadise.getInstance();
	private RunicGateway RG = new RunicGateway();

	final int RUNNER_DAYS = 7;
	final int RUNNER_RUNICS = 2500;
	final String RUNNER_KILLS = "Zombie:15, Spider:5, Skeleton:10";
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
	final int MASTER_MASTER_JOBS = 4;
	final String MASTER_KILLS = "TBD";

	final int EXPLORER_DAYS = 7;
	final int EXPLORER_RUNICS = 2500;
	final String EXPLORER_KILLS = "Zombie:15, Spider:5, Skeleton:10";
	final int BUILDER_DAYS = 21;
	final int BUILDER_RUNICS = 5000;
	final int BUILDER_JOB_LEVEL = 5;
	final String BUILDER_KILLS = "Zombie:100, Skeleton:50, Squid:20, Spider:25";
	final int ARCHITECT_DAYS = 42;
	final int ARCHITECT_RUNICS = 10000;
	final int ARCHITECT_JOB_LEVEL = 10;
	final String ARCHITECT_KILLS = "Blaze:15, PigZombie:50, Ghast:10, Zombie:200";
	final int WARDEN_DAYS = 60;
	final int WARDEN_RUNICS = 20000;
	final int WARDEN_JOB_LEVEL = 15;
	final String WARDEN_KILLS = "Skeleton:100, Witch:20, Creeper:20, Cavespider: 25, IronGolem: 3";
	final int PROTECTOR_DAYS = 90;
	final int PROTECTOR_RUNICS = 40000;
	final int PROTECTOR_JOB_LEVEL = 20;
	final String PROTECTOR_KILLS = "Wither:1, Ghast:25, Spider:50, Enderman:25, IronGolem: 10";
	final int GUARDIAN_DAYS = 120;
	final int GUARDIAN_RUNICS = 60000;
	final String GUARDIAN_KILLS = "Wither:2, Zombie:400, Witch:100, Creeper:50, Spider:100, Skeleton:300";

	public Ranks() {

	}

	public void congratsPromotion(String promoted, String newRank) {

		// RG.sendMessage(true, promoted, ChatColor.GOLD
		// + "[RunicRanks] Congratulations, " + promoted
		// + ", on promoting to " + newRank + "!");

		for (Player p : Bukkit.getOnlinePlayers()) {
			TitleAPI.sendTitle(p, 2, 3, 2,
					RunicParadise.rankColors.get(newRank) + "" + ChatColor.BOLD
							+ promoted, RunicParadise.rankColors.get(newRank)
							+ "has reached the rank of " + newRank);
		}

		RG.playPortalTravelSound(true, "abc");
	}

	public void playerkillCounts(Player user) {
		Date now = new Date();

		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));
		final Connection d = MySQL.openConnection();

		user.sendMessage(ChatColor.GRAY
				+ "[RunicRanks] Listing player kill counts...");

		for (Player p : Bukkit.getOnlinePlayers()) {
			try {
				Statement dStmt = d.createStatement();
				ResultSet playerData = dStmt
						.executeQuery("SELECT * FROM `rp_PlayerInfo` WHERE `PlayerName` = '"
								+ p.getName() + "' ORDER BY `id` ASC LIMIT 1;");
				playerData.next();
				int killZombie = playerData.getInt("KillZombie");
				int killSkeleton = playerData.getInt("KillSkeleton");
				int killWitch = playerData.getInt("KillWitch");
				int killWither = playerData.getInt("KillWither");
				int killSlime = playerData.getInt("KillSlime");
				int killMagmaCube = playerData.getInt("KillMagmaCube");
				int killSilverfish = playerData.getInt("KillSilverfish");
				int killGiant = playerData.getInt("KillGiant");
				int killBlaze = playerData.getInt("KillBlaze");
				int killCreeper = playerData.getInt("KillCreeper");
				int killEnderman = playerData.getInt("KillEnderman");
				int killSpider = playerData.getInt("KillSpider");
				int killCavespider = playerData.getInt("KillCaveSpider");
				int killSquid = playerData.getInt("KillSquid");
				int killEnderdragon = playerData.getInt("KillEnderDragon");
				int killPigzombie = playerData.getInt("KillPigZombie");
				int killGhast = playerData.getInt("KillGhast");

				user.sendMessage(ChatColor.GOLD + p.getName() + ": "
						+ ChatColor.GRAY + "Zom:" + killZombie + "," + "Ske:"
						+ killSkeleton + "," + "Wch:" + killWitch + ","
						+ "Wth:" + killWither + "," + "Sli:" + killSlime + ","
						+ "Mag:" + killMagmaCube + "," + "Slv:"
						+ killSilverfish + "," + "Gia:" + killGiant + ","
						+ "Blz:" + killBlaze + "," + "Crp:" + killCreeper + ","
						+ "EnM:" + killEnderman + "," + "EnD:"
						+ killEnderdragon + "," + "Spd:" + killSpider + ","
						+ "Cav:" + killCavespider + "," + "Sqd:" + killSquid
						+ "," + "PgZ:" + killPigzombie + "," + "Gha:"
						+ killGhast);
			} catch (SQLException e) {
				getLogger().log(
						Level.SEVERE,
						"Failed DB check for playerStats because: "
								+ e.getMessage());
			}
		}

		try {
			d.close();
		} catch (SQLException e) {
			getLogger().log(
					Level.SEVERE,
					"Failed DB close for playerStats because: "
							+ e.getMessage());
		}
	}

	public void playerStats(Player user) {
		Date now = new Date();

		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));
		final Connection d = MySQL.openConnection();
		double daysPlayed = 0;
		int balance = 0;
		int isNominated = 0;

		user.sendMessage(ChatColor.GRAY
				+ "[RunicRanks] Listing player stats...");

		for (Player p : Bukkit.getOnlinePlayers()) {
			try {
				Statement dStmt = d.createStatement();
				ResultSet playerData = dStmt
						.executeQuery("SELECT `FirstSeen`, `IsNominated`, `Votes` FROM `rp_PlayerInfo` WHERE `PlayerName` = '"
								+ p.getName() + "' ORDER BY `id` ASC LIMIT 1;");
				playerData.next();

				balance = (int) economy.getBalance(p);
				;
				long firstSeenTime = playerData.getLong("FirstSeen");
				isNominated = playerData.getInt("IsNominated");
				daysPlayed = ((now.getTime() - firstSeenTime) / 86400000);
				DecimalFormat df = new DecimalFormat("#,###.##");

				user.sendMessage(ChatColor.GRAY + p.getName() + ": "
						+ ChatColor.LIGHT_PURPLE + df.format(daysPlayed)
						+ " days, " + balance + " R, noms(" + isNominated
						+ "), votes(" + playerData.getInt("Votes") + ")");
			} catch (SQLException e) {
				getLogger().log(
						Level.SEVERE,
						"Failed DB check for playerStats because: "
								+ e.getMessage());
			}
		}

		try {
			d.close();
		} catch (SQLException e) {
			getLogger().log(
					Level.SEVERE,
					"Failed DB close for playerStats because: "
							+ e.getMessage());
		}
	}

	public void showRequirements(Player user) {
		int nomRedux;

		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));
		final Connection d = MySQL.openConnection();
		int nominations = 0;

		try {
			Statement dStmt = d.createStatement();
			ResultSet playerData = dStmt
					.executeQuery("SELECT * FROM `rp_PlayerInfo` WHERE `PlayerName` = '"
							+ user.getName() + "' ORDER BY `id` ASC LIMIT 1;");
			// if this is true, there is a result!
			if (playerData.isBeforeFirst()) {
				playerData.next();
				nominations = playerData.getInt("IsNominated");
			}
			d.close();
		} catch (SQLException e) {
			getLogger().log(
					Level.SEVERE,
					"Failed DB check for showRequirements because: "
							+ e.getMessage());
		}

		if (nominations > 0) {
			nomRedux = 10;
		} else {
			nomRedux = 0;
		}

		user.sendMessage(ChatColor.GRAY
				+ "[RunicRanks] Listing promotion requirements...");
		user.sendMessage(ChatColor.DARK_GREEN + "Runner - " + ChatColor.GRAY
				+ RUNNER_DAYS + " days or 20 votes, " + RUNNER_RUNICS + " R.");
		user.sendMessage(ChatColor.GRAY + RUNNER_KILLS);
		user.sendMessage(ChatColor.YELLOW + "Singer - " + ChatColor.GRAY
				+ SINGER_DAYS + " days, " + SINGER_RUNICS
				+ " R, hedge maze, sun or moon faith >50.");
		user.sendMessage(ChatColor.GRAY + SINGER_KILLS);
		user.sendMessage(ChatColor.GOLD + "Brawler - " + ChatColor.GRAY
				+ BRAWLER_DAYS + " days, " + BRAWLER_RUNICS + " R, sky maze.");
		user.sendMessage(ChatColor.GRAY + BRAWLER_KILLS);
		user.sendMessage(ChatColor.AQUA + "Keeper - " + ChatColor.GRAY
				+ KEEPER_DAYS + " days, " + KEEPER_RUNICS + " R, "
				+ KEEPER_MASTER_JOBS
				+ " jobs mastered, ice maze, water or fire faith >100.");
		user.sendMessage(ChatColor.GRAY + KEEPER_KILLS);
		user.sendMessage(ChatColor.DARK_AQUA + "Guard - " + ChatColor.GRAY
				+ GUARD_DAYS + " days, " + GUARD_RUNICS + " R, jungle maze.");
		user.sendMessage(ChatColor.GRAY + GUARD_KILLS);
		user.sendMessage(ChatColor.BLUE + "Hunter - " + ChatColor.GRAY
				+ HUNTER_DAYS + " days, " + HUNTER_RUNICS + " R, "
				+ HUNTER_MASTER_JOBS
				+ " jobs mastered, frost maze, earth or air faith >150.");
		user.sendMessage(ChatColor.GRAY + HUNTER_KILLS);
		user.sendMessage(ChatColor.DARK_BLUE + "Slayer - " + ChatColor.GRAY
				+ SLAYER_DAYS + " days, " + SLAYER_RUNICS + " R.");
		user.sendMessage(ChatColor.GRAY + SLAYER_KILLS);
		user.sendMessage(ChatColor.LIGHT_PURPLE + "Warder - " + ChatColor.GRAY
				+ WARDER_DAYS + " days, " + WARDER_RUNICS + " R, "
				+ WARDER_MASTER_JOBS + " jobs mastered");
		user.sendMessage(ChatColor.GRAY + WARDER_KILLS);
		user.sendMessage(ChatColor.DARK_PURPLE + "Champion - " + ChatColor.GRAY
				+ CHAMPION_DAYS + " days, " + CHAMPION_RUNICS + " R.");
		user.sendMessage(ChatColor.GRAY + CHAMPION_KILLS);
		user.sendMessage(ChatColor.RED + "Master - " + ChatColor.GRAY
				+ MASTER_DAYS + " days, " + MASTER_RUNICS + " R, "
				+ MASTER_MASTER_JOBS + " jobs mastered, dungeon maze.");
		user.sendMessage(ChatColor.GRAY + MASTER_KILLS);

		/*
		 * user.sendMessage(ChatColor.DARK_GREEN + "Explorer - " +
		 * ChatColor.GRAY + EXPLORER_DAYS + " days or 20 votes, " +
		 * EXPLORER_RUNICS + " R."); user.sendMessage(ChatColor.GRAY +
		 * EXPLORER_KILLS); user.sendMessage(ChatColor.YELLOW + "Builder - " +
		 * ChatColor.GRAY + BUILDER_DAYS + " days, " + BUILDER_RUNICS +
		 * " R, joblevel " + BUILDER_JOB_LEVEL); user.sendMessage(ChatColor.GRAY
		 * + BUILDER_KILLS); user.sendMessage(ChatColor.GOLD + "Architect - " +
		 * ChatColor.GRAY + ARCHITECT_DAYS + " days, " + ARCHITECT_RUNICS +
		 * " R, joblevel " + ARCHITECT_JOB_LEVEL + ", hedge maze.");
		 * user.sendMessage(ChatColor.GRAY + ARCHITECT_KILLS);
		 * user.sendMessage(ChatColor.AQUA + "Warden - " + ChatColor.GRAY +
		 * (WARDEN_DAYS - nomRedux) + " days, " + WARDEN_RUNICS +
		 * " R, joblevel " + WARDEN_JOB_LEVEL + ", sky maze.");
		 * user.sendMessage(ChatColor.GRAY + WARDEN_KILLS);
		 * user.sendMessage(ChatColor.DARK_AQUA + "Protector - " +
		 * ChatColor.GRAY + (PROTECTOR_DAYS - nomRedux) + " days, " +
		 * PROTECTOR_RUNICS + " R, joblevel " + PROTECTOR_JOB_LEVEL);
		 * user.sendMessage(ChatColor.GRAY + PROTECTOR_KILLS);
		 * user.sendMessage(ChatColor.BLUE + "Guardian - " + ChatColor.GRAY +
		 * (GUARDIAN_DAYS - nomRedux) + " days, " + GUARDIAN_RUNICS + " R.");
		 * user.sendMessage(ChatColor.GRAY + GUARDIAN_KILLS);
		 * user.sendMessage(ChatColor.GOLD + "Type " + ChatColor.YELLOW +
		 * "/info ranks " + ChatColor.GOLD + "to learn what each rank can do.");
		 * if (nomRedux > 0) { user.sendMessage(ChatColor.AQUA +
		 * "You have a staff nomination! Your time requirements have been reduced."
		 * ); }
		 */
	}

	@SuppressWarnings("deprecation")
	public void convertRanks(final Player user) {
		String rank = perms.getPrimaryGroup(user);
		boolean converted = false;
		String command = "";
		String command2 = "";
		String newRank = "";

		switch (rank) {
		case "Ghosts":
			command = "manuadd " + user.getName() + " Ghost";
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
			command = "manuadd " + user.getName() + " Seeker";
			command2 = "faith enable " + user.getName() + " Sun";
			newRank = "Seeker";
			converted = true;
			break;
		case "Settler":
			if (user.hasPermission("rp.ascend")) {
				command = "manuadd " + user.getName() + " Seeker";
				converted = true;
				newRank = "Seeker";
			}
			break;
		case "Explorer":
		case "Builder":
			if (user.hasPermission("rp.ascend")) {
				command = "manuadd " + user.getName() + " Runner";
				converted = true;
				newRank = "Runner";
			}
			break;
		case "Architect":
		case "Warden":
		case "Protector":
			if (user.hasPermission("rp.ascend")) {
				command = "manuadd " + user.getName() + " Singer";
				converted = true;
				newRank = "Singer";
			}
			break;
		case "Guardian":
			if (user.hasPermission("rp.ascend")) {
				command = "manuadd " + user.getName() + " Brawler";
				newRank = "Brawler";
				converted = true;
			}
			break;

		default:
			break;
		}
		if (converted) {
			user.sendMessage(ChatColor.YELLOW + "[RunicRanks]" + ChatColor.GOLD
					+ " Welcome to the new ranks system!!");
			user.sendMessage(ChatColor.YELLOW + "[RunicRanks]" + ChatColor.GOLD
					+ " To learn how to get to the next rank, type /promote");
			user.sendMessage(ChatColor.YELLOW + "[RunicRanks]" + ChatColor.GOLD
					+ " To see what ranks we have, type /ranks");
			user.sendMessage(ChatColor.YELLOW
					+ "[RunicRanks]"
					+ ChatColor.GOLD
					+ " To learn about perks: http://www.runic-paradise.com/ranks.php");
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "sc Converting "
					+ user.getName() + " from " + rank + " to " + newRank);
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
			Bukkit.getServer()
					.getScheduler()
					.scheduleAsyncDelayedTask(RunicParadise.getInstance(),
							new Runnable() {
								public void run() {
									Bukkit.dispatchCommand(
											Bukkit.getConsoleSender(),
											"faith enable " + user.getName()
													+ " Sun");
								}
							}, 60);

		}

	}

	public void nominatePlayer(Player sender, String nominee) {

		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));
		final Connection d = MySQL.openConnection();
		int nominations = 0;
		boolean playerFound = false;
		try {
			Statement dStmt = d.createStatement();
			ResultSet playerData = dStmt
					.executeQuery("SELECT * FROM `rp_PlayerInfo` WHERE `PlayerName` = '"
							+ nominee + "' ORDER BY `id` ASC LIMIT 1;");
			// if this is true, there is a result!
			if (playerData.isBeforeFirst()) {
				playerData.next();
				nominations = playerData.getInt("IsNominated");
				playerFound = true;
			} else {
				sender.sendMessage(ChatColor.RED
						+ "[ERROR] Nomination failed. Could not find player named "
						+ nominee);
				d.close();
			}
		} catch (SQLException e) {
			getLogger().log(
					Level.SEVERE,
					"Failed DB check for nominatePlayer because: "
							+ e.getMessage());
		}

		if (playerFound) {
			try {
				Statement dStmt = d.createStatement();
				dStmt.executeUpdate("UPDATE `rp_PlayerInfo` SET IsNominated='"
						+ (nominations + 1) + "' WHERE PlayerName='" + nominee
						+ "';");
				d.close();
				sender.sendMessage(ChatColor.DARK_GREEN
						+ "[RunicRanks] Success! " + nominee + " now has "
						+ (nominations + 1) + " nominations");
				sender.getServer()
						.getPlayer(nominee)
						.sendMessage(
								ChatColor.DARK_GREEN
										+ "[RunicRanks] You have just been nominated for faster promotions by "
										+ sender.getDisplayName());
				sender.getServer()
						.getPlayer(nominee)
						.sendMessage(
								ChatColor.WHITE
										+ "Type /ranks to see your new requirements.");
				sender.getServer()
						.getPlayer(nominee)
						.sendMessage(
								ChatColor.WHITE
										+ "While multiple nominations are very nice, only the first one has an impact. :)");

			} catch (SQLException e) {
				getLogger().log(
						Level.SEVERE,
						"Failed DB update for nominatePlayer because: "
								+ e.getMessage());
			}
		}

	}

	public void checkPromotion(Player user, boolean execute) {
		Date now = new Date();

		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));
		final Connection d = MySQL.openConnection();
		double daysPlayed = 0;
		DecimalFormat df = new DecimalFormat("#,###.##");
		String rank = "";
		int balance = (int) economy.getBalance(user);
		int nomRedux = 0;
		boolean checkDays = false;
		boolean checkFaiths = false;
		boolean checkJob = false;
		boolean checkRunics = false;
		boolean checkKills = false;
		boolean checkMazes = false;
		boolean checkJobMasteries = false;
		boolean ineligible = false;
		ArrayList<String> failureResponse = new ArrayList<String>();
		int[] killsArray = new int[13];
		RunicPlayerBukkit targetPlayer = new RunicPlayerBukkit(user);
		// ZOM,SPI,SKE,SQU,BLA,PGZ,GHA,WTH,WCH,ENM,CRE

		// Check how many days played; report days played and current rank to
		// user
		try {
			Statement dStmt = d.createStatement();
			ResultSet playerData = dStmt
					.executeQuery("SELECT * FROM `rp_PlayerInfo` WHERE `PlayerName` = '"
							+ user.getName() + "' ORDER BY `id` ASC LIMIT 1;");
			playerData.next();

			long firstSeenTime = playerData.getLong("FirstSeen");
			if (playerData.getInt("IsNominated") > 0) {
				nomRedux = 10;
			}
			daysPlayed = ((now.getTime() - firstSeenTime) / 86400000);

			rank = perms.getPrimaryGroup(user);
			killsArray[0] = playerData.getInt("KillZombie");
			killsArray[1] = playerData.getInt("KillSpider");
			killsArray[2] = playerData.getInt("KillSkeleton");
			killsArray[3] = playerData.getInt("KillSquid");
			killsArray[4] = playerData.getInt("KillBlaze");
			killsArray[5] = playerData.getInt("KillPigZombie");
			killsArray[6] = playerData.getInt("KillGhast");
			killsArray[7] = playerData.getInt("KillWither");
			killsArray[8] = playerData.getInt("KillWitch");
			killsArray[9] = playerData.getInt("KillEnderman");
			killsArray[10] = playerData.getInt("KillCreeper");
			killsArray[11] = playerData.getInt("KillCaveSpider");
			killsArray[12] = playerData.getInt("KillIronGolem");
			// if you add more here, increase the array definition up above!!!

			user.sendMessage(ChatColor.DARK_GREEN
					+ "You first joined Runic Paradise " + daysPlayed
					+ " days ago. Your current rank is " + rank + ".");
		} catch (SQLException e) {
			getLogger().log(
					Level.SEVERE,
					"Failed DB check for checkPromotion because: "
							+ e.getMessage());
		}

		switch (rank) {
		case "Champion":
			// if player has been on the server long enough for promotion
			if ((daysPlayed - (double) MASTER_DAYS) > -0.4) {
				checkDays = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: " + MASTER_DAYS);
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
						+ ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: " + MASTER_DAYS);
			}
			if (balance >= MASTER_RUNICS) {
				checkRunics = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + MASTER_RUNICS);
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
						+ ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + MASTER_RUNICS);
			}

			if (killsArray[0] >= 400 && killsArray[1] >= 200
					&& killsArray[2] >= 400 && killsArray[7] >= 3) {
				checkKills = true;
				failureResponse
						.add(ChatColor.DARK_GREEN
								+ "[✔ OK] "
								+ ChatColor.GRAY
								+ "You have killed enough monsters for this promotion.");
			} else {
				if (killsArray[0] > 400) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your ZOMBIE kills: "
							+ killsArray[0] + "; Required: 400");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your ZOMBIE kills: "
							+ killsArray[0] + "; Required: 400");
				}
				if (killsArray[1] >= 200) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your SPIDER kills: "
							+ killsArray[1] + "; Required: 200");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your SPIDER kills: "
							+ killsArray[1] + "; Required: 200");
				}
				if (killsArray[2] >= 400) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your SKELETON kills: "
							+ killsArray[2] + "; Required: 400");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your SKELETON kills: "
							+ killsArray[2] + "; Required: 400");
				}
				if (killsArray[7] >= 3) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your WITHER kills: "
							+ killsArray[7] + "; Required: 3");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your WITHER kills: "
							+ killsArray[7] + "; Required: 3");
				}
			}
			if (targetPlayer.getMasteredJobCount() > 3) {
				checkJobMasteries = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY + "You have mastered "
						+ targetPlayer.getMasteredJobCount() + " jobs.");
			} else {
				failureResponse
						.add(ChatColor.DARK_RED
								+ "[✘ FAIL] "
								+ ChatColor.GRAY
								+ "You haven't mastered 4 jobs. Get info @ /warp anchorsdeep");
			}
			
			if (targetPlayer.getPlayerJungleMazeCompletions() > 0 && targetPlayer.getPlayerDungeonMazeCompletions() > 0) {
				checkMazes = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY + "You have completed the jungle & dungeon mazes!");
			} else {
				failureResponse
						.add(ChatColor.DARK_RED
								+ "[✘ FAIL] "
								+ ChatColor.GRAY
								+ "You must complete the jungle and dungeon mazes! See Puzzle Kiosk at /games");
			}

			if (checkDays && checkRunics && checkKills && checkJobMasteries && checkMazes) {
				if (execute == false) {
					// just checking... we're not executing the promotion!!
					user.sendMessage(ChatColor.DARK_GREEN
							+ "[RunicRanks] Congratulations! You qualify for a promotion!");
					user.sendMessage(ChatColor.DARK_GREEN
							+ "Promotion to Master costs " + MASTER_RUNICS
							+ " Runics.");
					user.sendMessage(ChatColor.DARK_GREEN + "Type "
							+ ChatColor.AQUA + "/promote me"
							+ ChatColor.DARK_GREEN
							+ " to accept the promotion.");
				} else {
					// ok now we're executing the promotion.
					economy.withdrawPlayer(user, MASTER_RUNICS);
					perms.playerAddGroup(user, "Master");
					perms.playerRemoveGroup(user, "Champion");
					user.sendMessage(ChatColor.DARK_GREEN
							+ "[RunicRanks] Congratulations! You have been promoted!");
					Ranks tempRank = new Ranks();
					tempRank.congratsPromotion(user.getName(), "Master");
					logPromotion(user.getName(), "Master", new Date().getTime());
				}
			} else {
				ineligible = true;
			}

			break;
		case "Warder":
			// if player has been on the server long enough for promotion
			if ((daysPlayed - (double) CHAMPION_DAYS) > -0.4) {
				checkDays = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: "
						+ CHAMPION_DAYS);
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
						+ ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: "
						+ CHAMPION_DAYS);
			}
			if (balance >= CHAMPION_RUNICS) {
				checkRunics = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + CHAMPION_RUNICS);
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
						+ ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + CHAMPION_RUNICS);
			}

			if (killsArray[5] >= 150 && killsArray[9] >= 60
					&& killsArray[8] >= 25) {
				checkKills = true;
				failureResponse
						.add(ChatColor.DARK_GREEN
								+ "[✔ OK] "
								+ ChatColor.GRAY
								+ "You have killed enough monsters for this promotion.");
			} else {
				if (killsArray[5] > 150) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your PIGZOMBIE kills: "
							+ killsArray[5] + "; Required: 150");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your PIGZOMBIE kills: "
							+ killsArray[5] + "; Required: 150");
				}
				if (killsArray[9] >= 60) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your ENDERMAN kills: "
							+ killsArray[9] + "; Required: 60");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your ENDERMAN kills: "
							+ killsArray[9] + "; Required: 60");
				}
				if (killsArray[8] >= 25) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your WITCH kills: "
							+ killsArray[8] + "; Required: 25");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your WITCH kills: "
							+ killsArray[8] + "; Required: 25");
				}
			}
			
			if (targetPlayer.getPlayerJungleMazeCompletions() > 0) {
				checkMazes = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY + "You have completed the jungle maze!");
			} else {
				failureResponse
						.add(ChatColor.DARK_RED
								+ "[✘ FAIL] "
								+ ChatColor.GRAY
								+ "You must complete the jungle maze! See Puzzle Kiosk at /games");
			}

			if (checkDays && checkRunics && checkKills && checkMazes) {
				if (execute == false) {
					// just checking... we're not executing the promotion!!
					user.sendMessage(ChatColor.DARK_GREEN
							+ "[RunicRanks] Congratulations! You qualify for a promotion!");
					user.sendMessage(ChatColor.DARK_GREEN
							+ "Promotion to Champion costs " + CHAMPION_RUNICS
							+ " Runics.");
					user.sendMessage(ChatColor.DARK_GREEN + "Type "
							+ ChatColor.AQUA + "/promote me"
							+ ChatColor.DARK_GREEN
							+ " to accept the promotion.");
				} else {
					// ok now we're executing the promotion.
					economy.withdrawPlayer(user, CHAMPION_RUNICS);
					perms.playerAddGroup(user, "Champion");
					perms.playerRemoveGroup(user, "Warder");
					user.sendMessage(ChatColor.DARK_GREEN
							+ "[RunicRanks] Congratulations! You have been promoted!");
					Ranks tempRank = new Ranks();
					tempRank.congratsPromotion(user.getName(), "Champion");
					logPromotion(user.getName(), "Champion",
							new Date().getTime());
				}
			} else {
				ineligible = true;
			}
			break;
		case "Slayer":
			// if player has been on the server long enough for promotion
			if ((daysPlayed - (double) WARDER_DAYS) > -0.4) {
				checkDays = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: " + WARDER_DAYS);
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
						+ ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: " + WARDER_DAYS);
			}
			if (balance >= WARDER_RUNICS) {
				checkRunics = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + WARDER_RUNICS);
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
						+ ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + WARDER_RUNICS);
			}

			if (killsArray[0] >= 400 && killsArray[1] >= 200
					&& killsArray[2] >= 400 && killsArray[7] >= 3) {
				checkKills = true;
				failureResponse
						.add(ChatColor.DARK_GREEN
								+ "[✔ OK] "
								+ ChatColor.GRAY
								+ "You have killed enough monsters for this promotion.");
			} else {
				if (killsArray[0] > 400) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your ZOMBIE kills: "
							+ killsArray[0] + "; Required: 400");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your ZOMBIE kills: "
							+ killsArray[0] + "; Required: 400");
				}
				if (killsArray[1] >= 200) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your SPIDER kills: "
							+ killsArray[1] + "; Required: 200");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your SPIDER kills: "
							+ killsArray[1] + "; Required: 200");
				}
				if (killsArray[2] >= 400) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your SKELETON kills: "
							+ killsArray[2] + "; Required: 400");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your SKELETON kills: "
							+ killsArray[2] + "; Required: 400");
				}
				if (killsArray[7] >= 3) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your WITHER kills: "
							+ killsArray[7] + "; Required: 3");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your WITHER kills: "
							+ killsArray[7] + "; Required: 3");
				}
			}
			if (targetPlayer.getMasteredJobCount() > 2) {
				checkJobMasteries = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY + "You have mastered "
						+ targetPlayer.getMasteredJobCount() + " jobs.");
			} else {
				failureResponse
						.add(ChatColor.DARK_RED
								+ "[✘ FAIL] "
								+ ChatColor.GRAY
								+ "You haven't mastered 3 jobs. Get info @ /warp anchorsdeep");
			}
			
			if (targetPlayer.getPlayerJungleMazeCompletions() > 0) {
				checkMazes = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY + "You have completed the jungle maze!");
			} else {
				failureResponse
						.add(ChatColor.DARK_RED
								+ "[✘ FAIL] "
								+ ChatColor.GRAY
								+ "You must complete the jungle maze! See Puzzle Kiosk at /games");
			}

			if (checkDays && checkRunics && checkKills && checkJobMasteries && checkMazes) {
				if (execute == false) {
					// just checking... we're not executing the promotion!!
					user.sendMessage(ChatColor.DARK_GREEN
							+ "[RunicRanks] Congratulations! You qualify for a promotion!");
					user.sendMessage(ChatColor.DARK_GREEN
							+ "Promotion to Warder costs " + WARDER_RUNICS
							+ " Runics.");
					user.sendMessage(ChatColor.DARK_GREEN + "Type "
							+ ChatColor.AQUA + "/promote me"
							+ ChatColor.DARK_GREEN
							+ " to accept the promotion.");
				} else {
					// ok now we're executing the promotion.
					economy.withdrawPlayer(user, WARDER_RUNICS);
					perms.playerAddGroup(user, "Warder");
					perms.playerRemoveGroup(user, "Slayer");
					user.sendMessage(ChatColor.DARK_GREEN
							+ "[RunicRanks] Congratulations! You have been promoted!");
					Ranks tempRank = new Ranks();
					tempRank.congratsPromotion(user.getName(), "Warder");
					logPromotion(user.getName(), "Warder", new Date().getTime());
				}
			} else {
				ineligible = true;
			}
			break;
		case "Hunter":
			// if player has been on the server long enough for promotion
			if ((daysPlayed - (double) SLAYER_DAYS) > -0.4) {
				checkDays = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: " + SLAYER_DAYS);
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
						+ ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: " + SLAYER_DAYS);
			}
			if (balance >= SLAYER_RUNICS) {
				checkRunics = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + SLAYER_RUNICS);
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
						+ ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + SLAYER_RUNICS);
			}

			if (killsArray[5] >= 100 && killsArray[9] >= 45
					&& killsArray[8] >= 10) {
				checkKills = true;
				failureResponse
						.add(ChatColor.DARK_GREEN
								+ "[✔ OK] "
								+ ChatColor.GRAY
								+ "You have killed enough monsters for this promotion.");
			} else {
				if (killsArray[5] > 100) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your PIGZOMBIE kills: "
							+ killsArray[5] + "; Required: 100");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your PIGZOMBIE kills: "
							+ killsArray[5] + "; Required: 100");
				}
				if (killsArray[9] >= 45) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your ENDERMAN kills: "
							+ killsArray[9] + "; Required: 45");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your ENDERMAN kills: "
							+ killsArray[9] + "; Required: 45");
				}
				if (killsArray[8] >= 10) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your WITCH kills: "
							+ killsArray[8] + "; Required: 10");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your WITCH kills: "
							+ killsArray[8] + "; Required: 10");
				}
			}
			
			if (targetPlayer.getPlayerJungleMazeCompletions() > 0) {
				checkMazes = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY + "You have completed the jungle maze!");
			} else {
				failureResponse
						.add(ChatColor.DARK_RED
								+ "[✘ FAIL] "
								+ ChatColor.GRAY
								+ "You must complete the jungle maze! See Puzzle Kiosk at /games");
			}

			if (checkDays && checkRunics && checkKills && checkMazes) {
				if (execute == false) {
					// just checking... we're not executing the promotion!!
					user.sendMessage(ChatColor.DARK_GREEN
							+ "[RunicRanks] Congratulations! You qualify for a promotion!");
					user.sendMessage(ChatColor.DARK_GREEN
							+ "Promotion to Slayer costs " + SLAYER_RUNICS
							+ " Runics.");
					user.sendMessage(ChatColor.DARK_GREEN + "Type "
							+ ChatColor.AQUA + "/promote me"
							+ ChatColor.DARK_GREEN
							+ " to accept the promotion.");
				} else {
					// ok now we're executing the promotion.
					economy.withdrawPlayer(user, SLAYER_RUNICS);
					perms.playerAddGroup(user, "Slayer");
					perms.playerRemoveGroup(user, "Hunter");
					user.sendMessage(ChatColor.DARK_GREEN
							+ "[RunicRanks] Congratulations! You have been promoted!");
					Ranks tempRank = new Ranks();
					tempRank.congratsPromotion(user.getName(), "Slayer");
					logPromotion(user.getName(), "Slayer", new Date().getTime());
				}
			} else {
				ineligible = true;
			}
			break;
		case "Guard":
			// if player has been on the server long enough for promotion
			if ((daysPlayed - (double) HUNTER_DAYS) > -0.4) {
				checkDays = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: " + HUNTER_DAYS);
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
						+ ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: " + HUNTER_DAYS);
			}
			if (balance >= HUNTER_RUNICS) {
				checkRunics = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + HUNTER_RUNICS);
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
						+ ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + HUNTER_RUNICS);
			}

			if (killsArray[0] >= 300 && killsArray[1] >= 150
					&& killsArray[2] >= 300 && killsArray[7] >= 2) {
				checkKills = true;
				failureResponse
						.add(ChatColor.DARK_GREEN
								+ "[✔ OK] "
								+ ChatColor.GRAY
								+ "You have killed enough monsters for this promotion.");
			} else {
				if (killsArray[0] > 300) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your ZOMBIE kills: "
							+ killsArray[0] + "; Required: 300");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your ZOMBIE kills: "
							+ killsArray[0] + "; Required: 300");
				}
				if (killsArray[1] >= 150) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your SPIDER kills: "
							+ killsArray[1] + "; Required: 150");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your SPIDER kills: "
							+ killsArray[1] + "; Required: 150");
				}
				if (killsArray[2] >= 300) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your SKELETON kills: "
							+ killsArray[2] + "; Required: 300");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your SKELETON kills: "
							+ killsArray[2] + "; Required: 300");
				}
				if (killsArray[7] >= 2) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your WITHER kills: "
							+ killsArray[7] + "; Required: 2");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your WITHER kills: "
							+ killsArray[7] + "; Required: 2");
				}
			}
			if (RunicParadise.faithMap.get(user.getUniqueId()).checkEquippedFaithLevel("Air", 150)
					||
					RunicParadise.faithMap.get(user.getUniqueId()).checkEquippedFaithLevel("Earth", 150)) {
				checkFaiths = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY
						+ "You have reached level 150 with Air or Earth Faith");
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
						+ ChatColor.GRAY
						+ "You need level 150 in the Air or Earth Faith. It must be active!");
			}

			if (targetPlayer.getMasteredJobCount() > 1) {
				checkJobMasteries = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY + "You have mastered "
						+ targetPlayer.getMasteredJobCount() + " jobs.");
			} else {
				failureResponse
						.add(ChatColor.DARK_RED
								+ "[✘ FAIL] "
								+ ChatColor.GRAY
								+ "You havent mastered 2 jobs. Get info @ /warp anchorsdeep");
			}
			
			if (targetPlayer.getPlayerFrostMazeCompletions() > 0) {
				checkMazes = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY + "You have completed the frost maze!");
			} else {
				failureResponse
						.add(ChatColor.DARK_RED
								+ "[✘ FAIL] "
								+ ChatColor.GRAY
								+ "You must complete the frost maze! See Puzzle Kiosk at /games");
			}
			

			
			if (checkDays && checkRunics && checkKills && checkJobMasteries && checkMazes
					&& checkFaiths) {
				if (execute == false) {
					// just checking... we're not executing the promotion!!
					user.sendMessage(ChatColor.DARK_GREEN
							+ "[RunicRanks] Congratulations! You qualify for a promotion!");
					user.sendMessage(ChatColor.DARK_GREEN
							+ "Promotion to Hunter costs " + HUNTER_RUNICS
							+ " Runics.");
					user.sendMessage(ChatColor.DARK_GREEN + "Type "
							+ ChatColor.AQUA + "/promote me"
							+ ChatColor.DARK_GREEN
							+ " to accept the promotion.");
				} else {
					// ok now we're executing the promotion.
					economy.withdrawPlayer(user, HUNTER_RUNICS);
					perms.playerAddGroup(user, "Hunter");
					perms.playerRemoveGroup(user, "Guard");
					user.sendMessage(ChatColor.DARK_GREEN
							+ "[RunicRanks] Congratulations! You have been promoted!");
					Ranks tempRank = new Ranks();
					tempRank.congratsPromotion(user.getName(), "Hunter");
					logPromotion(user.getName(), "Hunter", new Date().getTime());
				}
			} else {
				ineligible = true;
			}
			break;
		case "Keeper":
			// if player has been on the server long enough for promotion
			if ((daysPlayed - (double) GUARD_DAYS) > -0.4) {
				checkDays = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: " + GUARD_DAYS);
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
						+ ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: " + GUARD_DAYS);
			}
			if (balance >= GUARD_RUNICS) {
				checkRunics = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + GUARD_RUNICS);
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
						+ ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + GUARD_RUNICS);
			}

			if (killsArray[5] >= 50 && killsArray[9] >= 30
					&& killsArray[8] >= 5) {
				checkKills = true;
				failureResponse
						.add(ChatColor.DARK_GREEN
								+ "[✔ OK] "
								+ ChatColor.GRAY
								+ "You have killed enough monsters for this promotion.");
			} else {
				if (killsArray[5] > 50) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your PIGZOMBIE kills: "
							+ killsArray[5] + "; Required: 50");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your PIGZOMBIE kills: "
							+ killsArray[5] + "; Required: 50");
				}
				if (killsArray[9] >= 30) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your ENDERMAN kills: "
							+ killsArray[9] + "; Required: 30");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your ENDERMAN kills: "
							+ killsArray[9] + "; Required: 30");
				}
				if (killsArray[8] >= 5) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your WITCH kills: "
							+ killsArray[8] + "; Required: 5");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your WITCH kills: "
							+ killsArray[8] + "; Required: 5");
				}
			}
			

			if (targetPlayer.getPlayerJungleMazeCompletions() > 0) {
				checkMazes = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY + "You have completed the jungle maze!");
			} else {
				failureResponse
						.add(ChatColor.DARK_RED
								+ "[✘ FAIL] "
								+ ChatColor.GRAY
								+ "You must complete the jungle maze! See Puzzle Kiosk at /games");
			}

			if (checkDays && checkRunics && checkKills && checkMazes) {
				if (execute == false) {
					// just checking... we're not executing the promotion!!
					user.sendMessage(ChatColor.DARK_GREEN
							+ "[RunicRanks] Congratulations! You qualify for a promotion!");
					user.sendMessage(ChatColor.DARK_GREEN
							+ "Promotion to Guard costs " + GUARD_RUNICS
							+ " Runics.");
					user.sendMessage(ChatColor.DARK_GREEN + "Type "
							+ ChatColor.AQUA + "/promote me"
							+ ChatColor.DARK_GREEN
							+ " to accept the promotion.");
				} else {
					// ok now we're executing the promotion.
					economy.withdrawPlayer(user, GUARD_RUNICS);
					perms.playerAddGroup(user, "Guard");
					perms.playerRemoveGroup(user, "Keeper");
					user.sendMessage(ChatColor.DARK_GREEN
							+ "[RunicRanks] Congratulations! You have been promoted!");
					Ranks tempRank = new Ranks();
					tempRank.congratsPromotion(user.getName(), "Guard");
					logPromotion(user.getName(), "Guard", new Date().getTime());
				}
			} else {
				ineligible = true;
			}
			break;
		case "Brawler":
			// if player has been on the server long enough for promotion
			if ((daysPlayed - (double) KEEPER_DAYS) > -0.4) {
				checkDays = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: " + KEEPER_DAYS);
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
						+ ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: " + KEEPER_DAYS);
			}
			if (balance >= KEEPER_RUNICS) {
				checkRunics = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + KEEPER_RUNICS);
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
						+ ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + KEEPER_RUNICS);
			}
			if (killsArray[0] >= 200 && killsArray[1] >= 100
					&& killsArray[2] >= 200 && killsArray[7] >= 1) {
				checkKills = true;
				failureResponse
						.add(ChatColor.DARK_GREEN
								+ "[✔ OK] "
								+ ChatColor.GRAY
								+ "You have killed enough monsters for this promotion.");
			} else {
				if (killsArray[0] > 200) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your ZOMBIE kills: "
							+ killsArray[0] + "; Required: 200");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your ZOMBIE kills: "
							+ killsArray[0] + "; Required: 200");
				}
				if (killsArray[1] >= 100) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your SPIDER kills: "
							+ killsArray[1] + "; Required: 100");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your SPIDER kills: "
							+ killsArray[1] + "; Required: 100");
				}
				if (killsArray[2] >= 200) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your SKELETON kills: "
							+ killsArray[2] + "; Required: 200");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your SKELETON kills: "
							+ killsArray[2] + "; Required: 200");
				}
				if (killsArray[7] >= 1) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your WITHER kills: "
							+ killsArray[7] + "; Required: 1");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your WITHER kills: "
							+ killsArray[7] + "; Required: 1");
				}
			}

			if (RunicParadise.faithMap.get(user.getUniqueId())
					.checkEquippedFaithLevel("Water", 75)
					|| RunicParadise.faithMap.get(user.getUniqueId())
							.checkEquippedFaithLevel("Fire", 75)) {
				checkFaiths = true;
				failureResponse
						.add(ChatColor.DARK_GREEN
								+ "[✔ OK] "
								+ ChatColor.GRAY
								+ "You have reached level 75 with Water or Fire Faith");
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
						+ ChatColor.GRAY
						+ "You need level 75 in the Water or Fire Faith. It must be active!");
			}

			if (targetPlayer.getMasteredJobCount() > 0) {
				checkJobMasteries = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY + "You have mastered a job.");
			} else {
				failureResponse
						.add(ChatColor.DARK_RED
								+ "[✘ FAIL] "
								+ ChatColor.GRAY
								+ "You need to master a job. Get info @ /warp anchorsdeep");
			}

			if (checkDays && checkRunics && checkKills && checkFaiths
					&& checkJobMasteries) {
				if (execute == false) {
					// just checking... we're not executing the promotion!!
					user.sendMessage(ChatColor.DARK_GREEN
							+ "[RunicRanks] Congratulations! You qualify for a promotion!");
					user.sendMessage(ChatColor.DARK_GREEN
							+ "Promotion to Keeper costs " + KEEPER_RUNICS
							+ " Runics.");
					user.sendMessage(ChatColor.DARK_GREEN + "Type "
							+ ChatColor.AQUA + "/promote me"
							+ ChatColor.DARK_GREEN
							+ " to accept the promotion.");
				} else {
					// ok now we're executing the promotion.
					economy.withdrawPlayer(user, KEEPER_RUNICS);
					perms.playerAddGroup(user, "Keeper");
					perms.playerRemoveGroup(user, "Brawler");
					user.sendMessage(ChatColor.DARK_GREEN
							+ "[RunicRanks] Congratulations! You have been promoted!");
					Ranks tempRank = new Ranks();
					tempRank.congratsPromotion(user.getName(), "Keeper");
					logPromotion(user.getName(), "Keeper", new Date().getTime());
				}
			} else {
				ineligible = true;
			}
			break;
		case "Singer":
			// if player has been on the server long enough for promotion
			if ((daysPlayed - (double) BRAWLER_DAYS) > -0.4) {
				checkDays = true;
				failureResponse
						.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY
								+ "Days on the server: "
								+ df.format(daysPlayed) + "; Required: "
								+ BRAWLER_DAYS);
			} else {
				failureResponse
						.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY
								+ "Days on the server: "
								+ df.format(daysPlayed) + "; Required: "
								+ BRAWLER_DAYS);
			}
			if (balance >= BRAWLER_RUNICS) {
				checkRunics = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + BRAWLER_RUNICS);
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
						+ ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + BRAWLER_RUNICS);
			}
			if (killsArray[5] >= 25 && killsArray[9] >= 15
					&& killsArray[8] >= 2) {
				checkKills = true;
				failureResponse
						.add(ChatColor.DARK_GREEN
								+ "[✔ OK] "
								+ ChatColor.GRAY
								+ "You have killed enough monsters for this promotion.");
			} else {
				if (killsArray[5] > 25) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your PIGZOMBIE kills: "
							+ killsArray[5] + "; Required: 25");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your PIGZOMBIE kills: "
							+ killsArray[5] + "; Required: 25");
				}
				if (killsArray[9] >= 15) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your ENDERMAN kills: "
							+ killsArray[9] + "; Required: 15");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your ENDERMAN kills: "
							+ killsArray[9] + "; Required: 15");
				}
				if (killsArray[8] >= 2) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your WITCH kills: "
							+ killsArray[8] + "; Required: 2");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your WITCH kills: "
							+ killsArray[8] + "; Required: 2");
				}
			}

			if (targetPlayer.getPlayerIceMazeCompletions() > 0) {
				checkMazes = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY + "You have completed the sky maze!");
			} else {
				failureResponse
						.add(ChatColor.DARK_RED
								+ "[✘ FAIL] "
								+ ChatColor.GRAY
								+ "You must complete the sky maze! See Games Portal at /spawn");
			}

			if (checkDays && checkRunics && checkKills && checkMazes) {
				if (execute == false) {
					// just checking... we're not executing the promotion!!
					user.sendMessage(ChatColor.DARK_GREEN
							+ "[RunicRanks] Congratulations! You qualify for a promotion!");
					user.sendMessage(ChatColor.DARK_GREEN
							+ "Promotion to Brawler costs " + BRAWLER_RUNICS
							+ " Runics.");
					user.sendMessage(ChatColor.DARK_GREEN + "Type "
							+ ChatColor.AQUA + "/promote me"
							+ ChatColor.DARK_GREEN
							+ " to accept the promotion.");
				} else {
					// ok now we're executing the promotion.
					economy.withdrawPlayer(user, BRAWLER_RUNICS);
					perms.playerAddGroup(user, "Brawler");
					perms.playerRemoveGroup(user, "Singer");
					user.sendMessage(ChatColor.DARK_GREEN
							+ "[RunicRanks] Congratulations! You have been promoted!");
					Ranks tempRank = new Ranks();
					tempRank.congratsPromotion(user.getName(), "Brawler");
					logPromotion(user.getName(), "Brawler",
							new Date().getTime());
				}
			} else {
				ineligible = true;
			}
			break;
		case "Runner":
			// if player has been on the server long enough for promotion
			if ((daysPlayed - (double) SINGER_DAYS) > -0.4) {
				checkDays = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: " + SINGER_DAYS);
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
						+ ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: " + SINGER_DAYS);
			}
			if (balance >= SINGER_RUNICS) {
				checkRunics = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + SINGER_RUNICS);
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
						+ ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + SINGER_RUNICS);
			}
			if (killsArray[0] >= 100 && killsArray[1] >= 50
					&& killsArray[2] >= 100) {
				checkKills = true;
				failureResponse
						.add(ChatColor.DARK_GREEN
								+ "[✔ OK] "
								+ ChatColor.GRAY
								+ "You have killed enough monsters for this promotion.");
			} else {
				if (killsArray[0] > 100) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your ZOMBIE kills: "
							+ killsArray[0] + "; Required: 100");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your ZOMBIE kills: "
							+ killsArray[0] + "; Required: 100");
				}
				if (killsArray[1] >= 50) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your SPIDER kills: "
							+ killsArray[1] + "; Required: 50");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your SPIDER kills: "
							+ killsArray[1] + "; Required: 50");
				}
				if (killsArray[2] >= 100) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your SKELETON kills: "
							+ killsArray[2] + "; Required: 100");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your SKELETON kills: "
							+ killsArray[2] + "; Required: 100");
				}
			}
			if (targetPlayer.getPlayerHedgeMazeCompletions() > 0) {
				checkMazes = true;
				failureResponse
						.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY
								+ "You have completed the hedge maze!");
			} else {
				failureResponse
						.add(ChatColor.DARK_RED
								+ "[✘ FAIL] "
								+ ChatColor.GRAY
								+ "You must complete the hedge maze! See Games Portal at /spawn");
			}

			if (RunicParadise.faithMap.get(user.getUniqueId())
					.checkEquippedFaithLevel("Sun", 50)
					|| RunicParadise.faithMap.get(user.getUniqueId())
							.checkEquippedFaithLevel("Moon", 50)) {
				checkFaiths = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY
						+ "You have reached level 50 with Sun or Moon Faith");
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
						+ ChatColor.GRAY
						+ "You need level 50 in the Sun or Moon Faith. It must be active!");
			}

			if (checkDays && checkRunics && checkKills && checkFaiths
					&& checkMazes) {
				if (execute == false) {
					// just checking... we're not executing the promotion!!
					user.sendMessage(ChatColor.DARK_GREEN
							+ "[RunicRanks] Congratulations! You qualify for a promotion!");
					user.sendMessage(ChatColor.DARK_GREEN
							+ "Promotion to Singer costs " + SINGER_RUNICS
							+ " Runics.");
					user.sendMessage(ChatColor.DARK_GREEN + "Type "
							+ ChatColor.AQUA + "/promote me"
							+ ChatColor.DARK_GREEN
							+ " to accept the promotion.");
				} else {
					// ok now we're executing the promotion.
					economy.withdrawPlayer(user, SINGER_RUNICS);
					perms.playerAddGroup(user, "Singer");
					perms.playerRemoveGroup(user, "Runner");
					user.sendMessage(ChatColor.DARK_GREEN
							+ "[RunicRanks] Congratulations! You have been promoted!");
					Ranks tempRank = new Ranks();
					tempRank.congratsPromotion(user.getName(), "Singer");
					logPromotion(user.getName(), "Singer", new Date().getTime());
				}
			} else {
				ineligible = true;
			}
			break;
		case "Seeker":
			// if player has been on the server long enough for promotion
			if ((daysPlayed - (double) RUNNER_DAYS) > -0.4) {
				checkDays = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: " + RUNNER_DAYS);
			} else if (targetPlayer.getPlayerVoteCount() >= 20) {
				checkDays = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY + "Votes: "
						+ targetPlayer.getPlayerVoteCount() + "; Required: 20");
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
						+ ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: " + RUNNER_DAYS);
				failureResponse
						.add(ChatColor.DARK_RED
								+ "[✘ FAIL] "
								+ ChatColor.GRAY
								+ "You can skip the days requirement with 20 votes! Your votes: "
								+ targetPlayer.getPlayerVoteCount());
			}
			if (balance >= RUNNER_RUNICS) {
				checkRunics = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + RUNNER_RUNICS);
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
						+ ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + RUNNER_RUNICS);
			}
			if (killsArray[0] >= 15 && killsArray[1] >= 5
					&& killsArray[2] >= 10) {
				checkKills = true;
				failureResponse
						.add(ChatColor.DARK_GREEN
								+ "[✔ OK] "
								+ ChatColor.GRAY
								+ "You have killed enough monsters for this promotion.");
			} else {
				if (killsArray[0] > 15) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your ZOMBIE kills: "
							+ killsArray[0] + "; Required: 15");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your ZOMBIE kills: "
							+ killsArray[0] + "; Required: 15");
				}
				if (killsArray[1] >= 5) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your SPIDER kills: "
							+ killsArray[1] + "; Required: 5");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your SPIDER kills: "
							+ killsArray[1] + "; Required: 5");
				}
				if (killsArray[2] >= 10) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your SKELETON kills: "
							+ killsArray[2] + "; Required: 10");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your SKELETON kills: "
							+ killsArray[2] + "; Required: 10");
				}
			}

			if (checkDays && checkRunics && checkKills) {
				if (execute == false) {
					// just checking... we're not executing the promotion!!
					user.sendMessage(ChatColor.DARK_GREEN
							+ "[RunicRanks] Congratulations! You qualify for a promotion!");
					user.sendMessage(ChatColor.DARK_GREEN
							+ "Promotion to Runner costs " + RUNNER_RUNICS
							+ " Runics.");
					user.sendMessage(ChatColor.DARK_GREEN + "Type "
							+ ChatColor.AQUA + "/promote me"
							+ ChatColor.DARK_GREEN
							+ " to accept the promotion.");
				} else {
					// ok now we're executing the promotion.
					economy.withdrawPlayer(user, RUNNER_RUNICS);
					perms.playerAddGroup(user, "Runner");
					perms.playerRemoveGroup(user, "Settler");
					user.sendMessage(ChatColor.DARK_GREEN
							+ "[RunicRanks] Congratulations! You have been promoted!");
					Ranks tempRank = new Ranks();
					tempRank.congratsPromotion(user.getName(), "Runner");
					logPromotion(user.getName(), "Runner", new Date().getTime());
				}
			} else {
				ineligible = true;
			}
			break;
		case "Settler":
			// if player has been on the server long enough for promotion
			if ((daysPlayed - (double) EXPLORER_DAYS) > -0.4) {
				checkDays = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: "
						+ EXPLORER_DAYS);
			} else if (targetPlayer.getPlayerVoteCount() >= 20) {
				checkDays = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY + "Votes: "
						+ targetPlayer.getPlayerVoteCount() + "; Required: 20");
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
						+ ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: "
						+ EXPLORER_DAYS);
				failureResponse
						.add(ChatColor.DARK_RED
								+ "[✘ FAIL] "
								+ ChatColor.GRAY
								+ "You can skip the 7 day requirement with 20 votes! Your votes: "
								+ targetPlayer.getPlayerVoteCount());
			}
			if (balance >= EXPLORER_RUNICS) {
				checkRunics = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + EXPLORER_RUNICS);
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
						+ ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + EXPLORER_RUNICS);
			}
			if (killsArray[0] >= 15 && killsArray[1] >= 5
					&& killsArray[2] >= 10) {
				checkKills = true;
				failureResponse
						.add(ChatColor.DARK_GREEN
								+ "[✔ OK] "
								+ ChatColor.GRAY
								+ "You have killed enough monsters for this promotion.");
			} else {
				if (killsArray[0] > 15) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your ZOMBIE kills: "
							+ killsArray[0] + "; Required: 15");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your ZOMBIE kills: "
							+ killsArray[0] + "; Required: 15");
				}
				if (killsArray[1] >= 5) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your SPIDER kills: "
							+ killsArray[1] + "; Required: 5");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your SPIDER kills: "
							+ killsArray[1] + "; Required: 5");
				}
				if (killsArray[2] >= 10) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your SKELETON kills: "
							+ killsArray[2] + "; Required: 10");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your SKELETON kills: "
							+ killsArray[2] + "; Required: 10");
				}
			}

			if (checkDays && checkRunics && checkKills) {
				if (execute == false) {
					// just checking... we're not executing the promotion!!
					user.sendMessage(ChatColor.DARK_GREEN
							+ "[RunicRanks] Congratulations! You qualify for a promotion!");
					user.sendMessage(ChatColor.DARK_GREEN
							+ "Promotion to EXPLORER costs " + EXPLORER_RUNICS
							+ " Runics.");
					user.sendMessage(ChatColor.DARK_GREEN + "Type "
							+ ChatColor.AQUA + "/promote me"
							+ ChatColor.DARK_GREEN
							+ " to accept the promotion.");
				} else {
					// ok now we're executing the promotion.
					economy.withdrawPlayer(user, EXPLORER_RUNICS);
					perms.playerAddGroup(user, "Explorer");
					perms.playerRemoveGroup(user, "Settler");
					user.sendMessage(ChatColor.DARK_GREEN
							+ "[RunicRanks] Congratulations! You have been promoted!");
					Ranks tempRank = new Ranks();
					tempRank.congratsPromotion(user.getName(), "Explorer");
					logPromotion(user.getName(), "Explorer",
							new Date().getTime());
				}
			} else {
				ineligible = true;
			}
			break;
		case "Explorer":
			if ((daysPlayed - (double) BUILDER_DAYS) > -0.4) {
				checkDays = true;
				failureResponse
						.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY
								+ "Days on the server: "
								+ df.format(daysPlayed) + "; Required: "
								+ BUILDER_DAYS);
			} else {
				failureResponse
						.add(ChatColor.DARK_RED + "[✘ FAIL] " + ChatColor.GRAY
								+ "Days on the server: "
								+ df.format(daysPlayed) + "; Required: "
								+ BUILDER_DAYS);
			}
			if (balance >= BUILDER_RUNICS) {
				checkRunics = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + BUILDER_RUNICS);
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
						+ ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + BUILDER_RUNICS);
			}
			if (killsArray[0] >= 100 && killsArray[1] >= 25
					&& killsArray[2] >= 50 && killsArray[3] >= 20) {
				checkKills = true;
				failureResponse
						.add(ChatColor.DARK_GREEN
								+ "[✔ OK] "
								+ ChatColor.GRAY
								+ "You have killed enough monsters for this promotion.");
			} else {
				if (killsArray[0] >= 100) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your ZOMBIE kills: "
							+ killsArray[0] + "; Required: 100");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your ZOMBIE kills: "
							+ killsArray[0] + "; Required: 100");
				}
				if (killsArray[1] >= 25) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your SPIDER kills: "
							+ killsArray[1] + "; Required: 25");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your SPIDER kills: "
							+ killsArray[1] + "; Required: 25");
				}
				if (killsArray[2] >= 50) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your SKELETON kills: "
							+ killsArray[2] + "; Required: 50");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your SKELETON kills: "
							+ killsArray[2] + "; Required: 50");
				}
				if (killsArray[3] >= 20) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your SQUID kills: "
							+ killsArray[3] + "; Required: 20");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your SQUID kills: "
							+ killsArray[3] + "; Required: 20");
				}

			}
			if (user.hasPermission("rp.level" + BUILDER_JOB_LEVEL)
					|| user.hasPermission("rp.level.master")) {
				checkJob = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY
						+ "You have obtained at least job level "
						+ BUILDER_JOB_LEVEL);
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
						+ ChatColor.GRAY + "You need at least job level "
						+ BUILDER_JOB_LEVEL);
			}

			if (checkDays && checkRunics && checkKills && checkJob) {
				if (execute == false) {
					// just checking... we're not executing the promotion!!
					user.sendMessage(ChatColor.DARK_GREEN
							+ "[RunicRanks] Congratulations! You qualify for a promotion!");
					user.sendMessage(ChatColor.DARK_GREEN
							+ "Promotion to BUILDER costs " + BUILDER_RUNICS
							+ " Runics.");
					user.sendMessage(ChatColor.DARK_GREEN + "Type "
							+ ChatColor.AQUA + "/promote me"
							+ ChatColor.DARK_GREEN
							+ " to accept the promotion.");
				} else {
					// ok now we're executing the promotion.
					economy.withdrawPlayer(user, BUILDER_RUNICS);
					perms.playerAddGroup(user, "Builder");
					perms.playerRemoveGroup(user, "Explorer");
					user.sendMessage(ChatColor.DARK_GREEN
							+ "[RunicRanks] Congratulations! You have been promoted!");
					Ranks tempRank = new Ranks();
					tempRank.congratsPromotion(user.getName(), "Builder");
					logPromotion(user.getName(), "Builder",
							new Date().getTime());
				}
			} else {
				ineligible = true;
			}

			break;
		case "Builder":
			if ((daysPlayed - (double) ARCHITECT_DAYS) > -0.4) {
				checkDays = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: "
						+ ARCHITECT_DAYS);
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
						+ ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: "
						+ ARCHITECT_DAYS);
			}
			if (balance >= ARCHITECT_RUNICS) {
				checkRunics = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + ARCHITECT_RUNICS);
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
						+ ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + ARCHITECT_RUNICS);
			}
			if (killsArray[0] >= 200 && killsArray[4] >= 15
					&& killsArray[5] >= 50 && killsArray[6] >= 10) {
				checkKills = true;
				failureResponse
						.add(ChatColor.DARK_GREEN
								+ "[✔ OK] "
								+ ChatColor.GRAY
								+ "You have killed enough monsters for this promotion.");
			} else {
				if (killsArray[0] >= 200) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your ZOMBIE kills: "
							+ killsArray[0] + "; Required: 200");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your ZOMBIE kills: "
							+ killsArray[0] + "; Required: 200");
				}
				if (killsArray[4] >= 15) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your BLAZE kills: "
							+ killsArray[4] + "; Required: 15");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your BLAZE kills: "
							+ killsArray[4] + "; Required: 15");
				}
				if (killsArray[5] >= 50) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your PIGZOMBIE kills: "
							+ killsArray[5] + "; Required: 50");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your PIGZOMBIE kills: "
							+ killsArray[5] + "; Required: 50");
				}
				if (killsArray[6] >= 10) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your GHAST kills: "
							+ killsArray[6] + "; Required: 10");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your GHAST kills: "
							+ killsArray[6] + "; Required: 10");
				}

			}
			if (user.hasPermission("rp.level" + ARCHITECT_JOB_LEVEL)
					|| user.hasPermission("rp.level.master")) {
				checkJob = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY
						+ "You have obtained at least job level "
						+ ARCHITECT_JOB_LEVEL);
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
						+ ChatColor.GRAY + "You need at least job level "
						+ ARCHITECT_JOB_LEVEL);
			}

			if (targetPlayer.getPlayerHedgeMazeCompletions() > 0) {
				checkMazes = true;
				failureResponse
						.add(ChatColor.DARK_GREEN + "[✔ OK] " + ChatColor.GRAY
								+ "You have completed the hedge maze!");
			} else {
				failureResponse
						.add(ChatColor.DARK_RED
								+ "[✘ FAIL] "
								+ ChatColor.GRAY
								+ "You must complete the hedge maze! See Games Portal at /spawn");
			}

			if (checkDays && checkRunics && checkKills && checkJob
					&& checkMazes) {
				if (execute == false) {
					// just checking... we're not executing the promotion!!
					user.sendMessage(ChatColor.DARK_GREEN
							+ "[RunicRanks] Congratulations! You qualify for a promotion!");
					user.sendMessage(ChatColor.DARK_GREEN
							+ "Promotion to ARCHITECT costs "
							+ ARCHITECT_RUNICS + " Runics.");
					user.sendMessage(ChatColor.DARK_GREEN + "Type "
							+ ChatColor.AQUA + "/promote me"
							+ ChatColor.DARK_GREEN
							+ " to accept the promotion.");
				} else {
					// ok now we're executing the promotion.
					economy.withdrawPlayer(user, ARCHITECT_RUNICS);
					perms.playerAddGroup(user, "Architect");
					perms.playerRemoveGroup(user, "Builder");
					user.sendMessage(ChatColor.DARK_GREEN
							+ "[RunicRanks] Congratulations! You have been promoted!");
					Ranks tempRank = new Ranks();
					tempRank.congratsPromotion(user.getName(), "Architect");
					logPromotion(user.getName(), "Architect",
							new Date().getTime());
				}
			} else {
				ineligible = true;
			}

			break;
		case "Architect":
			if ((daysPlayed - ((double) WARDEN_DAYS - nomRedux)) > -0.4) {
				checkDays = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: "
						+ (WARDEN_DAYS - nomRedux));
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
						+ ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: "
						+ (WARDEN_DAYS - nomRedux));
			}
			if (balance >= WARDEN_RUNICS) {
				checkRunics = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + WARDEN_RUNICS);
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
						+ ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + WARDEN_RUNICS);
			}
			if (killsArray[11] >= 25 && killsArray[2] >= 100
					&& killsArray[8] >= 20 && killsArray[10] >= 20
					&& killsArray[12] >= 3) {
				checkKills = true;
				failureResponse
						.add(ChatColor.DARK_GREEN
								+ "[✔ OK] "
								+ ChatColor.GRAY
								+ "You have killed enough monsters for this promotion.");
			} else {
				if (killsArray[11] >= 25) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your CAVE SPIDER kills: "
							+ killsArray[11] + "; Required: 25");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your CAVE SPIDER kills: "
							+ killsArray[11] + "; Required: 25");
				}
				if (killsArray[2] >= 100) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your SKELETON kills: "
							+ killsArray[2] + "; Required: 100");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your SKELETON kills: "
							+ killsArray[2] + "; Required: 100");
				}
				if (killsArray[8] >= 20) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your WITCH kills: "
							+ killsArray[8] + "; Required: 20");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your WITCH kills: "
							+ killsArray[8] + "; Required: 20");
				}
				if (killsArray[10] >= 20) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your CREEPER kills: "
							+ killsArray[10] + "; Required: 20");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your CREEPER kills: "
							+ killsArray[10] + "; Required: 20");
				}
				if (killsArray[12] >= 3) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your IRON GOLEM kills: "
							+ killsArray[12] + "; Required: 3");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your IRON GOLEM kills: "
							+ killsArray[12] + "; Required: 3");
				}

			}
			if (user.hasPermission("rp.level" + WARDEN_JOB_LEVEL)
					|| user.hasPermission("rp.level.master")) {
				checkJob = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY
						+ "You have obtained at least job level "
						+ WARDEN_JOB_LEVEL);
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
						+ ChatColor.GRAY + "You need at least job level "
						+ WARDEN_JOB_LEVEL);
			}

			if (targetPlayer.getPlayerIceMazeCompletions() > 0) {
				checkMazes = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY + "You have completed the sky maze!");
			} else {
				failureResponse
						.add(ChatColor.DARK_RED
								+ "[✘ FAIL] "
								+ ChatColor.GRAY
								+ "You must complete the sky maze! See Games Portal at /spawn");
			}
			if (checkDays && checkRunics && checkKills && checkJob
					&& checkMazes) {
				if (execute == false) {
					// just checking... we're not executing the promotion!!
					user.sendMessage(ChatColor.DARK_GREEN
							+ "[RunicRanks] Congratulations! You qualify for a promotion!");
					user.sendMessage(ChatColor.DARK_GREEN
							+ "Promotion to WARDEN costs " + WARDEN_RUNICS
							+ " Runics.");
					user.sendMessage(ChatColor.DARK_GREEN + "Type "
							+ ChatColor.AQUA + "/promote me"
							+ ChatColor.DARK_GREEN
							+ " to accept the promotion.");
				} else {
					// ok now we're executing the promotion.
					economy.withdrawPlayer(user, WARDEN_RUNICS);
					perms.playerAddGroup(user, "Warden");
					perms.playerRemoveGroup(user, "Architect");
					user.sendMessage(ChatColor.DARK_GREEN
							+ "[RunicRanks] Congratulations! You have been promoted!");
					Ranks tempRank = new Ranks();
					tempRank.congratsPromotion(user.getName(), "Warden");
					logPromotion(user.getName(), "Warden", new Date().getTime());
				}
			} else {
				ineligible = true;
			}

			break;
		case "Warden":
			if ((daysPlayed - ((double) PROTECTOR_DAYS - nomRedux)) > -0.4) {
				checkDays = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: "
						+ (PROTECTOR_DAYS - nomRedux));
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
						+ ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: "
						+ (PROTECTOR_DAYS - nomRedux));
			}
			if (balance >= PROTECTOR_RUNICS) {
				checkRunics = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + PROTECTOR_RUNICS);
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
						+ ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + PROTECTOR_RUNICS);
			}
			if (killsArray[7] >= 1 && killsArray[6] >= 25
					&& killsArray[1] >= 50 && killsArray[9] >= 25
					&& killsArray[12] >= 10) {
				checkKills = true;
				failureResponse
						.add(ChatColor.DARK_GREEN
								+ "[✔ OK] "
								+ ChatColor.GRAY
								+ "You have killed enough monsters for this promotion.");
			} else {
				if (killsArray[7] >= 1) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your WITHER kills: "
							+ killsArray[7] + "; Required: 1");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your WITHER kills: "
							+ killsArray[7] + "; Required: 1");
				}
				if (killsArray[6] >= 25) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your GHAST kills: "
							+ killsArray[6] + "; Required: 25");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your GHAST kills: "
							+ killsArray[6] + "; Required: 25");
				}
				if (killsArray[1] >= 50) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your SPIDER kills: "
							+ killsArray[1] + "; Required: 50");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your SPIDER kills: "
							+ killsArray[1] + "; Required: 50");
				}
				if (killsArray[9] >= 25) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your ENDERMAN kills: "
							+ killsArray[9] + "; Required: 25");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your ENDERMAN kills: "
							+ killsArray[9] + "; Required: 25");
				}
				if (killsArray[12] >= 10) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your IRON GOLEM kills: "
							+ killsArray[12] + "; Required: 10");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your IRON GOLEM kills: "
							+ killsArray[12] + "; Required: 10");
				}

			}
			if (user.hasPermission("rp.level" + PROTECTOR_JOB_LEVEL)
					|| user.hasPermission("rp.level.master")) {
				checkJob = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY
						+ "You have obtained at least job level "
						+ PROTECTOR_JOB_LEVEL);
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
						+ ChatColor.GRAY + "You need at least job level "
						+ PROTECTOR_JOB_LEVEL);
			}

			if (checkDays && checkRunics && checkKills && checkJob) {
				if (execute == false) {
					// just checking... we're not executing the promotion!!
					user.sendMessage(ChatColor.DARK_GREEN
							+ "[RunicRanks] Congratulations! You qualify for a promotion!");
					user.sendMessage(ChatColor.DARK_GREEN
							+ "Promotion to PROTECTOR costs "
							+ PROTECTOR_RUNICS + " Runics.");
					user.sendMessage(ChatColor.DARK_GREEN + "Type "
							+ ChatColor.AQUA + "/promote me"
							+ ChatColor.DARK_GREEN
							+ " to accept the promotion.");
				} else {
					// ok now we're executing the promotion.
					economy.withdrawPlayer(user, PROTECTOR_RUNICS);
					perms.playerAddGroup(user, "Protector");
					perms.playerRemoveGroup(user, "Warden");
					user.sendMessage(ChatColor.DARK_GREEN
							+ "[RunicRanks] Congratulations! You have been promoted!");
					Ranks tempRank = new Ranks();
					tempRank.congratsPromotion(user.getName(), "Protector");
					logPromotion(user.getName(), "Protector",
							new Date().getTime());
				}
			} else {
				ineligible = true;
			}

			break;
		case "Protector":
			if ((daysPlayed - ((double) GUARDIAN_DAYS - nomRedux)) > -0.4) {
				checkDays = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: "
						+ (GUARDIAN_DAYS - nomRedux));
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
						+ ChatColor.GRAY + "Days on the server: "
						+ df.format(daysPlayed) + "; Required: "
						+ (GUARDIAN_DAYS - nomRedux));
			}
			if (balance >= GUARDIAN_RUNICS) {
				checkRunics = true;
				failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
						+ ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + GUARDIAN_RUNICS);
			} else {
				failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
						+ ChatColor.GRAY + "Your runics: " + balance
						+ "; Promotion cost: " + GUARDIAN_RUNICS);
			}
			if (killsArray[7] >= 2 && killsArray[0] >= 400
					&& killsArray[8] >= 100 && killsArray[10] >= 50
					&& killsArray[1] >= 100 && killsArray[2] >= 300) {
				checkKills = true;
				failureResponse
						.add(ChatColor.DARK_GREEN
								+ "[✔ OK] "
								+ ChatColor.GRAY
								+ "You have killed enough monsters for this promotion.");
			} else {
				if (killsArray[7] >= 2) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your WITHER kills: "
							+ killsArray[7] + "; Required: 2");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your WITHER kills: "
							+ killsArray[7] + "; Required: 2");
				}
				if (killsArray[0] >= 400) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your ZOMBIE kills: "
							+ killsArray[0] + "; Required: 400");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your ZOMBIE kills: "
							+ killsArray[0] + "; Required: 400");
				}
				if (killsArray[8] >= 100) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your WITCH kills: "
							+ killsArray[8] + "; Required: 100");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your WITCH kills: "
							+ killsArray[8] + "; Required: 100");
				}
				if (killsArray[10] >= 50) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your CREEPER kills: "
							+ killsArray[10] + "; Required: 50");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your CREEPER kills: "
							+ killsArray[10] + "; Required: 50");
				}
				if (killsArray[1] >= 100) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your SPIDER kills: "
							+ killsArray[1] + "; Required: 100");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your SPIDER kills: "
							+ killsArray[1] + "; Required: 100");
				}
				if (killsArray[2] >= 300) {
					failureResponse.add(ChatColor.DARK_GREEN + "[✔ OK] "
							+ ChatColor.GRAY + "Your SKELETON kills: "
							+ killsArray[2] + "; Required: 300");
				} else {
					failureResponse.add(ChatColor.DARK_RED + "[✘ FAIL] "
							+ ChatColor.GRAY + "Your SKELETON kills: "
							+ killsArray[2] + "; Required: 300");
				}

			}

			if (checkDays && checkRunics && checkKills) {
				if (execute == false) {
					// just checking... we're not executing the promotion!!
					user.sendMessage(ChatColor.DARK_GREEN
							+ "[RunicRanks] Congratulations! You qualify for a promotion!");
					user.sendMessage(ChatColor.DARK_GREEN
							+ "Promotion to GUARDIAN costs " + GUARDIAN_RUNICS
							+ " Runics.");
					user.sendMessage(ChatColor.DARK_GREEN + "Type "
							+ ChatColor.AQUA + "/promote me"
							+ ChatColor.DARK_GREEN
							+ " to accept the promotion.");
				} else {
					// ok now we're executing the promotion.
					economy.withdrawPlayer(user, GUARDIAN_RUNICS);
					perms.playerAddGroup(user, "Guardian");
					perms.playerRemoveGroup(user, "Protector");
					user.sendMessage(ChatColor.DARK_GREEN
							+ "[RunicRanks] Congratulations! You have been promoted!");
					Ranks tempRank = new Ranks();
					tempRank.congratsPromotion(user.getName(), "Guardian");
					logPromotion(user.getName(), "Guardian",
							new Date().getTime());
				}
			} else {
				ineligible = true;
			}

			break;
		case "Guardian":
			user.sendMessage(ChatColor.RED
					+ "[RunicRanks] There are no more promotions at your rank!");
			break;
		default:
			user.sendMessage(ChatColor.RED
					+ "[RunicRanks] There are no more promotions at your rank!");
			break;
		}

		// if player isnt eligible, give them all the stored responses to tell
		// them why
		if (ineligible == true) {
			user.sendMessage(ChatColor.RED
					+ "[RunicRanks] You do not qualify for a promotion yet...");
			String[] itemArray = new String[failureResponse.size()];
			String[] returnedArray = failureResponse.toArray(itemArray);

			user.sendMessage(returnedArray);
		}

		// Close the connection
		try {
			d.close();
		} catch (SQLException e) {
			getLogger().log(
					Level.SEVERE,
					"Cant close mysql conn after checkpromotion: "
							+ e.getMessage());
		}

	}

	public static void logPromotion(String playerName, String newRank,
			Long timestamp) {
		final Plugin instance = RunicParadise.getInstance();
		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));
		try {

			final Connection d = MySQL.openConnection();
			Statement dStmt = d.createStatement();
			int tempD = dStmt
					.executeUpdate("INSERT INTO rp_PlayerPromotions (`PlayerName`, `NewRank`, `TimeStamp`) VALUES "
							+ "('"
							+ playerName
							+ "', '"
							+ newRank
							+ "', "
							+ timestamp + ");");
			d.close();

		} catch (SQLException z) {
			getLogger().log(Level.SEVERE,
					"Failed DB check for restore grave cuz " + z.getMessage());
		}
	}

}
