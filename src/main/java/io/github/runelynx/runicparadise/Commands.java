/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.github.runelynx.runicparadise;

import static io.github.runelynx.runicparadise.RunicParadise.economy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;

import static org.bukkit.Bukkit.getLogger;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

import com.kill3rtaco.tacoserialization.InventorySerialization;

/**
 *
 * @author Andrewxwsaa
 */
public class Commands implements CommandExecutor {

	Ranks rank = new Ranks();

	// pointer to your main class, not required if you don't need methods fromfg
	// the main class
	private Plugin instance = RunicParadise.getInstance();

	public static ArrayList<Integer> PARTICLE_TASK_IDS = new ArrayList<Integer>();

	public boolean onCommand(CommandSender sender, Command cmd, String label,

	String[] args) {
		// comment

		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));

		// general approach is that errors will return immediately;
		// successful runs will return after the switch completes
		switch (cmd.getName()) {
		case "rpjobs":
			// Master a tier1 job
			if (args[0].equals("master") && args.length == 2
					&& !(sender instanceof Player)) {
				RunicPlayerBukkit targetPlayer = new RunicPlayerBukkit(args[1]);
				if (targetPlayer.getCurrentJob().equals("None")) {
					targetPlayer
							.sendMessageToPlayer(ChatColor.GREEN
									+ "You don't have a job. Get a job and get level 25 in it.");
					return false;
				} else {
					if (targetPlayer.getMasteredJobs().contains(
							targetPlayer.getCurrentJob())) {
						targetPlayer.sendMessageToPlayer(ChatColor.GREEN
								+ "You already mastered this job.");
						return false;
					}
				}

				if (targetPlayer.checkPlayerPermission("rp.jobs.level25")) {
					// Player has sufficient level in a tier 1 job
					if (targetPlayer.executeJobMastery()) {
						// Execution succeeded!
						targetPlayer
								.sendMessageToPlayer(ChatColor.GREEN
										+ "Success! You have now mastered the following jobs:");
						targetPlayer.sendMessageToPlayer(ChatColor.GRAY
								+ targetPlayer.getMasteredJobs());
					} else {
						// Execution failed!
						targetPlayer
								.sendMessageToPlayer(ChatColor.GREEN
										+ "Error! Something went wrong, please ask an admin for help.");
					}

				} else {
					targetPlayer
							.sendMessageToPlayer(ChatColor.GRAY
									+ ""
									+ ChatColor.ITALIC
									+ "You must have level 25 in a job to achieve mastery.");
				}
			} else if (args[0].equals("qualify") && args.length == 2
					&& !(sender instanceof Player)) {
				// Qualify for a tier2 job
				RunicPlayerBukkit targetPlayer = new RunicPlayerBukkit(args[1]);
				boolean showFail = true;

				// RANGER
				if (targetPlayer.getMasteredJobs().contains("Woodsman")
						&& targetPlayer.getMasteredJobs().contains("Rancher")) {
					RunicParadise.perms.playerAdd(Bukkit.getPlayer(args[1]),
							"jobs.join.ranger");
					targetPlayer.sendMessageToPlayer(ChatColor.GREEN
							+ "You qualify to become a " + ChatColor.DARK_GREEN
							+ "RANGER");
					showFail = false;
				}

				// FORGEMASTER
				if (targetPlayer.getMasteredJobs().contains("Blacksmith")
						&& targetPlayer.getMasteredJobs().contains("Miner")) {
					RunicParadise.perms.playerAdd(Bukkit.getPlayer(args[1]),
							"jobs.join.forgemaster");
					targetPlayer.sendMessageToPlayer(ChatColor.GREEN
							+ "You qualify to become a " + ChatColor.DARK_GREEN
							+ "FORGEMASTER");
					showFail = false;
				}

				// BIOLOGIST
				if (targetPlayer.getMasteredJobs().contains("Scientist")
						&& targetPlayer.getMasteredJobs().contains("Rancher")) {
					RunicParadise.perms.playerAdd(Bukkit.getPlayer(args[1]),
							"jobs.join.biologist");
					targetPlayer.sendMessageToPlayer(ChatColor.GREEN
							+ "You qualify to become a " + ChatColor.DARK_GREEN
							+ "BIOLOGIST");
					showFail = false;
				}

				// ALCHEMIST
				if (targetPlayer.getMasteredJobs().contains("Wizard")
						&& targetPlayer.getMasteredJobs().contains("Chef")) {
					RunicParadise.perms.playerAdd(Bukkit.getPlayer(args[1]),
							"jobs.join.alchemist");
					targetPlayer.sendMessageToPlayer(ChatColor.GREEN
							+ "You qualify to become a " + ChatColor.DARK_GREEN
							+ "ALCHEMIST");
					showFail = false;
				}

				// NOMAD
				if (targetPlayer.getMasteredJobs().contains("Chef")
						&& targetPlayer.getMasteredJobs().contains("Rancher")) {
					RunicParadise.perms.playerAdd(Bukkit.getPlayer(args[1]),
							"jobs.join.nomad");
					targetPlayer.sendMessageToPlayer(ChatColor.GREEN
							+ "You qualify to become a " + ChatColor.DARK_GREEN
							+ "NOMAD");
					showFail = false;
				}

				// GEOMANCER
				if (targetPlayer.getMasteredJobs().contains("Wizard")
						&& targetPlayer.getMasteredJobs().contains("Miner")) {
					RunicParadise.perms.playerAdd(Bukkit.getPlayer(args[1]),
							"jobs.join.geomancer");
					targetPlayer.sendMessageToPlayer(ChatColor.GREEN
							+ "You qualify to become a " + ChatColor.DARK_GREEN
							+ "GEOMANCER");
					showFail = false;
				}

				// CONJURER
				if (targetPlayer.getMasteredJobs().contains("Blacksmith")
						&& targetPlayer.getMasteredJobs().contains("Wizard")) {
					RunicParadise.perms.playerAdd(Bukkit.getPlayer(args[1]),
							"jobs.join.conjurer");
					targetPlayer.sendMessageToPlayer(ChatColor.GREEN
							+ "You qualify to become a " + ChatColor.DARK_GREEN
							+ "CONJURER");
					showFail = false;
				}

				// DRUID
				if (targetPlayer.getMasteredJobs().contains("Wizard")
						&& targetPlayer.getMasteredJobs().contains("Woodsman")) {
					RunicParadise.perms.playerAdd(Bukkit.getPlayer(args[1]),
							"jobs.join.druid");
					targetPlayer.sendMessageToPlayer(ChatColor.GREEN
							+ "You qualify to become a " + ChatColor.DARK_GREEN
							+ "DRUID");
					showFail = false;
				}

				// ENGINEER
				if (targetPlayer.getMasteredJobs().contains("Scientist")
						&& targetPlayer.getMasteredJobs().contains("Miner")) {
					RunicParadise.perms.playerAdd(Bukkit.getPlayer(args[1]),
							"jobs.join.engineer");
					targetPlayer.sendMessageToPlayer(ChatColor.GREEN
							+ "You qualify to become a " + ChatColor.DARK_GREEN
							+ "ENGINEER");
					showFail = false;
				}

				// FAILURE
				if (showFail) {
					targetPlayer
							.sendMessageToPlayer(ChatColor.RED
									+ "You don't qualify for any tier 2 jobs yet. Master another job and try again!");
				}

			} else if (args[0].equals("maintenance") && args.length == 1
					&& !(sender instanceof Player)) {
				RunicPlayerBukkit fakeUser = new RunicPlayerBukkit("runelynx");
				fakeUser.maintainJobTable();

			} else if (sender instanceof Player) {

				sender.sendMessage("This command must be run from commandblock or console");
				sender.sendMessage("Format: /rpjobs master playername");
			}

			break;
		case "rpvote":
			if (args[0].equals("reward") && args.length == 2) {
				String command = "eco give " + args[1] + " 1000";
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
				command = "graves givesouls " + args[1] + " 1";
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

				Random rand = new Random();
				// int randomNum = rand.nextInt((max - min) + 1) + min;
				int randomNum = rand.nextInt((100 - 1) + 1) + 1;
				if (randomNum <= 2) {
					command = "graves givesouls " + args[1] + " 3";
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
					command = "say Lucky vote!! " + args[1]
							+ " got 3 extra souls!";
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
				}
			}
			break;
		case "rptransfer":

			final int MAX_UFOTRANSFER_ITEMS = 10;
			getLogger().log(Level.INFO, "[RPTransfer] Command received.");
			if (args.length == 3 && args[0].equals("ufotransfer1.8")) {
				// command is trying to CHECK STATUS of a player for the 1.8
				// world transfer
				getLogger().log(Level.INFO,
						"[RPTransfer] Valid syntax for status.");
				// First count items in player inventory
				RunicPlayerBukkit targetPlayer = new RunicPlayerBukkit(args[2]);
				int invCount = targetPlayer
						.checkPlayerInventoryItemstackCount();
				boolean wearingArmor = targetPlayer.checkPlayerWearingArmor();
				int usedSlots = 0;
				int activeSlots = 0;
				boolean active = false;
				String storedItemsString = "";

				// try to lookup player data in DB
				try {
					final Connection d = MySQL.openConnection();
					Statement dStmt = d.createStatement();
					ResultSet playerData = dStmt
							.executeQuery("SELECT * FROM `rp_ItemTransfers` WHERE UUID = '"
									+ targetPlayer.getPlayerUUID()
									+ "' AND TransferType = 'ufotransfer1.8' ORDER BY ID ASC LIMIT 1;");
					if (!playerData.isBeforeFirst()) {
						// Player doesn't exist in the DB for this transfer
						// type. Create entry.
						PreparedStatement insertStmt = d
								.prepareStatement("INSERT INTO rp_ItemTransfers (TransferType, PlayerName, UUID, StoredItems) VALUES "
										+ "('ufotransfer1.8', '"
										+ targetPlayer.getPlayerName()
										+ "', '"
										+ targetPlayer.getPlayerUUID()
										+ "', ' ');");

						insertStmt.executeUpdate();

					} else {
						// Player does exist in the DB and data retrieved!!
						playerData.next();
						usedSlots = playerData.getInt("UsedSlots");
						activeSlots = playerData.getInt("ActiveSlots");
						storedItemsString = playerData.getString("StoredItems");
						if (activeSlots > 0) {
							active = true;
						}
					}
				} catch (SQLException e) {
					getLogger().log(
							Level.SEVERE,
							"Failed ItemTransfers Lookup because "
									+ e.getMessage());
				}

				if (args[1].equals("start")) {
					// proximity trigger upon entering 1.8 world area for first
					// time
					if (targetPlayer.checkPlayerPermission("rp.runicrealm")) {

					} else {
						Player player = Bukkit.getPlayer(args[2]);
						player.teleport(new Location(Bukkit
								.getWorld("Runic Paradise"), 5644.5, 147.0,
								-4.5, (float) -179.74, (float) 5.70));
					}
				} else if (args[1].equals("ufo")) {
					// proximity trigger upon entering 1.8 world area for first
					// time

					Player player = Bukkit.getPlayer(args[2]);
					player.teleport(new Location(Bukkit
							.getWorld("Runic Paradise"), 5644.5, 147.0, -4.5,
							(float) -179.74, (float) 5.70));

				} else if (args[1].equals("status")) {
					// debugging for armor worn!
					if (wearingArmor) {
						getLogger().log(
								Level.INFO,
								"[RPTransfer] " + targetPlayer.getPlayerName()
										+ " is wearing armor.");
						return true;
					} else {
						getLogger().log(
								Level.INFO,
								"[RPTransfer] " + targetPlayer.getPlayerName()
										+ " is NOT wearing armor.");
					}

					getLogger().log(
							Level.INFO,
							"[RPTransfer] " + targetPlayer.getPlayerName()
									+ " is has used " + usedSlots + " slots.");

					if (active) {
						getLogger().log(
								Level.INFO,
								"[RPTransfer] " + targetPlayer.getPlayerName()
										+ " has items stored right now.");
					} else {
						getLogger()
								.log(Level.INFO,
										"[RPTransfer] "
												+ targetPlayer.getPlayerName()
												+ " does NOT have items stored right now.");
					}
				} // end STATUS check
				else if (args[1].equals("save")) {
					// STOP if player wearing armor!!
					if (wearingArmor) {
						targetPlayer
								.sendMessageToPlayer(ChatColor.GRAY
										+ ""
										+ ChatColor.ITALIC
										+ "You can't wear armor if you want to sneak items through!");
						return true;
					}
					// STOP if player already used 9 slots!!
					else if (usedSlots >= MAX_UFOTRANSFER_ITEMS) {
						targetPlayer.sendMessageToPlayer(ChatColor.GRAY + ""
								+ ChatColor.ITALIC
								+ "You can't sneak any more items through!");
						return true;
					}
					// STOP if player has active slots
					else if (active) {
						targetPlayer
								.sendMessageToPlayer(ChatColor.GRAY
										+ ""
										+ ChatColor.ITALIC
										+ "You must retrieve the "
										+ activeSlots
										+ " items you've tucked away before sneaking more through.");
						return true;
					} // STOP if player has more items on them than they can
						// store
					else if (invCount > (MAX_UFOTRANSFER_ITEMS - usedSlots)) {
						targetPlayer
								.sendMessageToPlayer(ChatColor.GRAY
										+ ""
										+ ChatColor.ITALIC
										+ "You have more items on you than you can sneak through! You can only take "
										+ (MAX_UFOTRANSFER_ITEMS - usedSlots)
										+ " more item slots!");
						return true;
					}

					// All is ok... so now store some items!!
					String invString = InventorySerialization
							.serializeInventoryAsString(Bukkit
									.getPlayer(targetPlayer.getPlayerName())
									.getInventory().getContents());
					int newUsedSlots = usedSlots + invCount;

					try {
						// Statement eStmt = e.createStatement();
						final Connection e = MySQL.openConnection();
						Statement eStmt = e.createStatement();
						PreparedStatement updateStmt = e
								.prepareStatement("UPDATE rp_ItemTransfers SET ActiveSlots="
										+ invCount
										+ ", UsedSlots="
										+ newUsedSlots
										+ ", StoredItems=? "
										+ "WHERE UUID='"
										+ targetPlayer.getPlayerUUID()
										+ "' AND TransferType='ufotransfer1.8';");
						updateStmt.setString(1, invString);

						updateStmt.executeUpdate();
						// CLEAR THE INVENTORY!!!!
						Bukkit.getPlayer(targetPlayer.getPlayerName())
								.getInventory().clear();
						targetPlayer.sendMessageToPlayer(ChatColor.GREEN
								+ "You have successfully hidden " + invCount
								+ " items to take to the 1.8 world!");
					} catch (SQLException err) {
						Bukkit.getLogger().log(
								Level.SEVERE,
								"Cant update/save for itemtransfers because: "
										+ err.getMessage());
					}

				} // end SAVE command
				else if (args[1].equals("load")) {

					// See if player has any stored items
					if (!active) {
						targetPlayer
								.sendMessageToPlayer(ChatColor.GRAY
										+ ""
										+ ChatColor.ITALIC
										+ "You haven't stored any items so there is nothing to unpack.");
						return true;
					}

					// check to make sure player has enough room to claim the
					// stored items
					if ((36 - invCount) < activeSlots) {
						// player does NOT have enough room
						targetPlayer
								.sendMessageToPlayer(ChatColor.GRAY
										+ ""
										+ ChatColor.ITALIC
										+ "You don't have enough slots open to unpack your hidden items. You need "
										+ activeSlots + " free slots.");
						return true;
					} else {
						// player has enough room!
						try {
							// Statement eStmt = e.createStatement();
							final Connection e = MySQL.openConnection();
							Statement eStmt = e.createStatement();
							PreparedStatement updateStmt = e
									.prepareStatement("UPDATE rp_ItemTransfers SET StoredItems=' ', ActiveSlots=0 WHERE UUID='"
											+ targetPlayer.getPlayerUUID()
											+ "' AND TransferType='ufotransfer1.8';");

							updateStmt.executeUpdate();

							ItemStack[] items = InventorySerialization
									.getInventory(storedItemsString, 100);
							targetPlayer.givePlayerItemStack(items);
							targetPlayer.sendMessageToPlayer(ChatColor.GREEN
									+ "You have successfully unpacked "
									+ activeSlots + " items!");
							if (usedSlots >= MAX_UFOTRANSFER_ITEMS) {
								targetPlayer
										.sendMessageToPlayer(ChatColor.GRAY
												+ ""
												+ ChatColor.ITALIC
												+ "You can not sneak any more items into the 1.8 world.");
							} else if (usedSlots < MAX_UFOTRANSFER_ITEMS) {
								targetPlayer.sendMessageToPlayer(ChatColor.GRAY
										+ "" + ChatColor.ITALIC
										+ "You can still sneak "
										+ (MAX_UFOTRANSFER_ITEMS - usedSlots)
										+ " more items into the 1.8 world.");
							}
						} catch (SQLException err) {
							Bukkit.getLogger().log(
									Level.SEVERE,
									"Cant update/save for itemtransfers because: "
											+ err.getMessage());
						}

					}
				} // end LOAD
				else if (args[1].equals("reset")) {
					try {
						// Statement eStmt = e.createStatement();
						final Connection e = MySQL.openConnection();
						Statement eStmt = e.createStatement();
						PreparedStatement updateStmt = e
								.prepareStatement("UPDATE rp_ItemTransfers SET StoredItems=' ', ActiveSlots=0, UsedSlots=0 WHERE UUID='"
										+ targetPlayer.getPlayerUUID()
										+ "' AND TransferType='ufotransfer1.8';");

						updateStmt.executeUpdate();

						targetPlayer
								.sendMessageToPlayer(ChatColor.GREEN
										+ ""
										+ "Your data has been reset for UFOTransfer1.8. You can now take "
										+ MAX_UFOTRANSFER_ITEMS
										+ " items through.");

					} catch (SQLException err) {
						Bukkit.getLogger().log(
								Level.SEVERE,
								"Cant update/save for itemtransfers because: "
										+ err.getMessage());
					}

				} // end reset
			}
			break;
		case "graves":
		case "grave":
			boolean showHelp = false;
			if (sender instanceof Player) {
				Player player = (Player) sender;
				RunicPlayerBukkit senderPlayer = new RunicPlayerBukkit(
						player.getUniqueId());
				if (args.length > 0) {
					if (args[0].equals("expire")
							&& player.hasPermission("rp.staff")) {
						RunicDeathChest.unlockExpiredGraves(true);
					} else if (args[0].equals("list")) {
						RunicDeathChest.listDeaths(player, player.getName());
						// } else if (args[0].equals("quit")) {
						// Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
						// "manudelp " + player.getName() + " rp.graves");
						// } else if (args[0].equals("secret") && args.length ==
						// 2) {
						// RunicDeathChest.restoreByCommand(player.getName(),
						// Integer.parseInt(args[1]));
					} else {
						showHelp = true;
					}
				} else {
					showHelp = true;
				}

				// Show help!!
				if (showHelp) {
					player.sendMessage(ChatColor.DARK_GRAY + "[RunicReaper] "
							+ ChatColor.GRAY
							+ "Welcome to the Runic Afterlife...:");
					player.sendMessage(ChatColor.GRAY + "You have "
							+ ChatColor.AQUA + senderPlayer.getPlayerSouls()
							+ ChatColor.GRAY + " souls remaining.");
					player.sendMessage(ChatColor.AQUA + "/graves list "
							+ ChatColor.GRAY + "List your graves");
					// player.sendMessage(ChatColor.GRAY
					// +
					// "Restore a grave to you using its ID number [for testers]");
					// player.sendMessage(ChatColor.AQUA
					// + "/graves secret GraveIDNumber");
					// player.sendMessage(ChatColor.GRAY
					// + "Stop testing graves [for testers]");
					// player.sendMessage(ChatColor.AQUA + "/graves quit");

				}
			} else {
				if (args[0].equals("expire")) {
					RunicDeathChest.unlockExpiredGraves(true);
					getLogger().log(Level.INFO,
							"[RP] Running the graves expire command.");
				} else if (args[0].equals("givesouls") && args.length == 3
						&& Integer.parseInt(args[2]) > 0
						&& !(sender instanceof Player)) {
					RunicPlayerBukkit targetPlayer = new RunicPlayerBukkit(
							args[1]);
					targetPlayer.setPlayerSouls(targetPlayer.getPlayerSouls()
							+ Integer.parseInt(args[2]));
					getLogger().log(Level.INFO,
							"[RP] Gave " + args[2] + " souls to " + args[1]);
				}
			}
			break;
		case "rpchest":
			Player player1 = (Player) sender;
			PlayerInventory playerInvItems = player1.getInventory();
			ItemStack[] items = playerInvItems.getContents();
			Block playerBlock = player1.getLocation().getBlock();
			int playerInvItemCount = 0;

			if (args.length > 0) {
				if (args[0].equals("save")) {
					RunicDeathChest.savePlayerDeath(player1,
							playerBlock.getLocation());
				} else {
					player1.sendMessage("Usage: /rpchest save/load");
				}
			} else {
				player1.sendMessage("Usage: /rpchest save/load");
			}

			break;

		case "rptokens":

			if (args.length == 0 || args[0].equals("help")) {
				if (sender instanceof Player) {
					RunicPlayerBukkit senderPlayer = new RunicPlayerBukkit(
							(Player) sender);
					senderPlayer.sendMessageToPlayer(ChatColor.GOLD
							+ "[RunicCarnival] How to form rptokens commands:");
					senderPlayer.sendMessageToPlayer(ChatColor.GRAY
							+ "Take tokens and execute command as reward:");
					senderPlayer.sendMessageToPlayer(ChatColor.DARK_GRAY
							+ "/rptokens take PLAYERNAME TOKENCOUNT COMMAND");
					senderPlayer.sendMessageToPlayer(ChatColor.GRAY
							+ "Give or take tokens:");
					senderPlayer.sendMessageToPlayer(ChatColor.DARK_GRAY
							+ "/rptokens give/take PLAYERNAME TOKENCOUNT");
					senderPlayer.sendMessageToPlayer(ChatColor.GRAY
							+ "Give trophies:");
					senderPlayer.sendMessageToPlayer(ChatColor.DARK_GRAY
							+ "/rptokens givetrophy PLAYERNAME TROPHYCOUNT");
					senderPlayer.sendMessageToPlayer(ChatColor.GRAY
							+ "Take all trophies and give tokens:");
					senderPlayer.sendMessageToPlayer(ChatColor.DARK_GRAY
							+ "/rptokens taketrophy PLAYERNAME");
					senderPlayer.sendMessageToPlayer(ChatColor.GRAY
							+ "Add maze win to player total:");
					senderPlayer.sendMessageToPlayer(ChatColor.DARK_GRAY
							+ "/rptokens mazewin [hedge/ice] PLAYERNAME");
				}
				// /////////////////////////////////////////////////////
				// check for rptokens TAKE command; ensure tokencount is
				// valid (positive integer)
				// if command is successful, execute a console command
				// /rptokens take PLAYER COUNT COMMANDTOEXECUTE
			} else if (args.length > 3 && Integer.parseInt(args[2]) > -1
					&& args[0].equals("take")) {

				RunicPlayerBukkit targetPlayer = new RunicPlayerBukkit(args[1]);

				targetPlayer.sendMessageToPlayer(ChatColor.GOLD
						+ "[RunicCarnival] Prize cost: " + args[2] + " tokens");

				if (targetPlayer.getPlayerTokenBalance() >= Integer
						.parseInt(args[2])) {
					int newBalance = targetPlayer.getPlayerTokenBalance()
							- Integer.parseInt(args[2]);

					// Update their balance
					if (targetPlayer.setPlayerTokenBalance(newBalance)) {
						// DB update finished successfully, proceed...
						targetPlayer.sendMessageToPlayer(ChatColor.GOLD
								+ "[RunicCarnival] You have turned in "
								+ ChatColor.DARK_RED + args[2] + ChatColor.GOLD
								+ " tokens");
						targetPlayer.sendMessageToPlayer(ChatColor.GOLD
								+ "[RunicCarnival] Your new token balance is "
								+ ChatColor.GREEN + newBalance + ChatColor.GOLD
								+ " tokens");
						// process the rest of the command
						String successCommand = "";
						int counter = 3; // start counter at the right spot
											// (/rptokens take name 5 give
											// name
											// item)
						while (counter <= (args.length - 1)) {
							successCommand += args[counter] + " ";
							counter++;
						}
						Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
								successCommand);
					} else {
						// DB update failed!
						targetPlayer
								.sendMessageToPlayer(ChatColor.DARK_RED
										+ "[ERROR] Something went wrong, couldn't update balance. No tokens have been taken!");
					}

				} else {
					targetPlayer
							.sendMessageToPlayer(ChatColor.GOLD
									+ "[RunicCarnival] Sorry, you don't have enough tokens. You have "
									+ ChatColor.GREEN
									+ targetPlayer.getPlayerTokenBalance()
									+ ChatColor.GOLD + ".");
				}

				// /////////////////////////////////////////////////////
				// simple take command to just remove tokens without doing
				// anything else
				// token count must be postive integer
				// /rptokens take PLAYER COUNT
			} else if (args.length == 3 && args[0].equals("take")) {
				// Player target = instance.getServer().getPlayer(args[1]);
				// String targetName = target.getName();
				// int tokenBal = -1;

				if (Integer.parseInt(args[2]) < 0) {
					return false;
				}

				RunicPlayerBukkit targetPlayer = new RunicPlayerBukkit(args[1]);

				int newBalance = targetPlayer.getPlayerTokenBalance()
						- Integer.parseInt(args[2]);
				if (newBalance < 0) {
					newBalance = 0;
				}

				// Update their balance
				if (targetPlayer.setPlayerTokenBalance(newBalance)) {
					// DB Update worked
					String senderName = "Someone";
					if (sender instanceof Player) {
						RunicPlayerBukkit senderPlayer = new RunicPlayerBukkit(
								(Player) sender);
						senderName = senderPlayer.getPlayerDisplayName();
					}
					targetPlayer.sendMessageToPlayer(ChatColor.GOLD
							+ "[RunicCarnival] " + senderName + ChatColor.GOLD
							+ " has taken " + ChatColor.DARK_RED + args[2]
							+ ChatColor.GOLD + " of your tokens!");
					targetPlayer.sendMessageToPlayer(ChatColor.GOLD
							+ "[RunicCarnival] Your new token balance: "
							+ ChatColor.GREEN + newBalance + ChatColor.GOLD
							+ " tokens");
					if (sender instanceof Player) {

						RunicPlayerBukkit senderPlayer = new RunicPlayerBukkit(
								(Player) sender);
						senderPlayer
								.sendMessageToPlayer(ChatColor.GOLD
										+ "[RunicCarnival] "
										+ targetPlayer.getPlayerDisplayName()
										+ ChatColor.GOLD
										+ "'s tokens - previous "
										+ ChatColor.DARK_RED
										+ (targetPlayer.getPlayerTokenBalance() + Integer
												.parseInt(args[2]))
										+ ChatColor.GOLD + ", new "
										+ ChatColor.GREEN + newBalance);
					}
				} else {
					// DB update failed
					targetPlayer
							.sendMessageToPlayer(ChatColor.DARK_RED
									+ "[ERROR] Something went wrong, couldn't update balance. Find Rune or check your command!");
				}

				// /////////////////////////////////////////////////////
				// simple give command to just remove tokens without doing
				// anything else
				// token count must be postive integer
				// /rptokens give PLAYER COUNT
			} else if (args.length == 3 && args[0].equals("give")) {

				if (Integer.parseInt(args[2]) < 0) {
					return false;
				}

				RunicPlayerBukkit targetPlayer = new RunicPlayerBukkit(args[1]);

				int newBalance = targetPlayer.getPlayerTokenBalance()
						+ Integer.parseInt(args[2]);

				// Update their balance
				if (targetPlayer.setPlayerTokenBalance(newBalance)) {
					// DB update worked
					targetPlayer.sendMessageToPlayer(ChatColor.GOLD
							+ "[RunicCarnival] " + ChatColor.GREEN + args[2]
							+ ChatColor.GOLD + " tokens awarded!");
					targetPlayer.sendMessageToPlayer(ChatColor.GOLD
							+ "[RunicCarnival] Your new token balance: "
							+ ChatColor.GREEN + newBalance + ChatColor.GOLD
							+ " tokens");
					if (sender instanceof Player) {

						RunicPlayerBukkit senderPlayer = new RunicPlayerBukkit(
								(Player) sender);
						senderPlayer.sendMessageToPlayer(ChatColor.GOLD
								+ "[RunicCarnival] "
								+ targetPlayer.getPlayerDisplayName()
								+ ChatColor.GOLD + "'s new token balance: "
								+ ChatColor.GREEN + newBalance);
					}
				} else {
					// DB update failed
					targetPlayer
							.sendMessageToPlayer(ChatColor.DARK_RED
									+ "[ERROR] Something went wrong, couldn't update balance. Find Rune or check your command!");
				}

				// /////////////////////////////////////////////////////
				// take trophy command; takes all trophies on player
				// and gives them that number of tokens
				// /rptokens taketrophy PLAYER
			} else if (args.length == 2 && args[0].equals("taketrophy")) {

				RunicPlayerBukkit targetPlayer = new RunicPlayerBukkit(args[1]);

				int trophyCount = targetPlayer
						.checkPlayerInventoryForItemDataCount(371, 99);

				if (trophyCount > 0) {

					int newBalance = targetPlayer.getPlayerTokenBalance()
							+ trophyCount;
					if (newBalance < 0) {
						newBalance = 0;
					}

					// Update their balance
					if (targetPlayer.setPlayerTokenBalance(newBalance)) {
						// DB update worked
						targetPlayer.sendMessageToPlayer(ChatColor.GOLD
								+ "[RunicCarnival] You have turned in "
								+ ChatColor.GREEN + trophyCount
								+ ChatColor.GOLD + " carnival trophies!");
						targetPlayer.sendMessageToPlayer(ChatColor.GOLD
								+ "[RunicCarnival] Your new token balance: "
								+ ChatColor.GREEN + +newBalance
								+ ChatColor.GOLD + " tokens");
						if (sender instanceof Player) {

							RunicPlayerBukkit senderPlayer = new RunicPlayerBukkit(
									(Player) sender);
							senderPlayer
									.sendMessageToPlayer(ChatColor.GOLD
											+ "[RunicCarnival] "
											+ targetPlayer
													.getPlayerDisplayName()
											+ ChatColor.GOLD
											+ "'s new token balance after trophy turn-in: "
											+ ChatColor.GREEN + newBalance);
							int removedTrophies = targetPlayer
									.removePlayerInventoryItemData(371, 99);

							getLogger().log(
									Level.INFO,
									"RunicCarnival gave " + trophyCount
											+ " credits to "
											+ targetPlayer.getPlayerName()

											+ " and removed " + removedTrophies
											+ " trophies");
							Bukkit.dispatchCommand(
									Bukkit.getConsoleSender(),
									"sc RunicCarnival gave " + ChatColor.GREEN
											+ trophyCount + ChatColor.AQUA
											+ " credits to "
											+ targetPlayer.getPlayerName()
											+ " and removed "
											+ ChatColor.DARK_RED
											+ removedTrophies + ChatColor.AQUA
											+ " trophies");
						}
					} else {
						// DB update failed
						targetPlayer
								.sendMessageToPlayer(ChatColor.DARK_RED
										+ "[ERROR] Something went wrong, couldn't update balance. Find Rune or check your command!");
					}

				} else {
					targetPlayer
							.sendMessageToPlayer(ChatColor.GOLD
									+ "[RunicCarnival] You don't have any trophies! Win some games to get more.");
				}

				// ////////////
				// /////////////////////////////////////////////////////
				// give trophy command; gives specified # trophies to player
				// /rptokens givetrophy PLAYER COUNT
			} else if (args.length == 3 && args[0].equals("givetrophy")) {
				RunicPlayerBukkit targetPlayer = new RunicPlayerBukkit(args[1]);

				if (Integer.parseInt(args[2]) > 0) {
					targetPlayer
							.givePlayerItemData(
									Integer.parseInt(args[2]),
									371,
									99,
									2,
									ChatColor.GOLD + "Runic Carnival Trophy",
									ChatColor.GRAY
											+ "Turn these in at the Prize Center",
									ChatColor.GRAY
											+ "in Runic Carnival for tokens",
									"");

					targetPlayer.sendMessageToPlayer(ChatColor.GOLD
							+ "[RunicCarnival] You have been awarded "
							+ ChatColor.GREEN + args[2] + ChatColor.GOLD
							+ " trophies!");
				} else {
					getLogger().log(
							Level.INFO,
							"Failed to give trophy to player, bad command usage? Tried /rptokens "
									+ args.toString());
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
							"sc RunicCarnival failed to award trophy. Tried /rptokens "
									+ args.toString());
				}
				// ////////////
				// /////////////////////////////////////////////////////
				// mazewin command, increments the player's running tab of maze
				// completions
				// /rptokens mazewin [hedge/ice] PLAYER
			} else if (args.length == 3 && args[0].equals("mazewin")) {
				RunicPlayerBukkit targetPlayer = new RunicPlayerBukkit(args[2]);

				if (targetPlayer.setPlayerMazeCompletions(args[1], 1)) {

					targetPlayer.sendMessageToPlayer(ChatColor.GOLD
							+ "[RunicCarnival] Congrats on completing the "
							+ args[1] + " maze!");
				} else {
					getLogger().log(
							Level.INFO,
							"Failed to add mazewin to player, bad command usage? Tried /rptokens "
									+ args.toString());
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
							"sc RunicCarnival failed to award mazewin. Tried /rptokens "
									+ args.toString());
				}

			} else if (sender instanceof Player) {

				RunicPlayerBukkit senderPlayer = new RunicPlayerBukkit(
						(Player) sender);
				senderPlayer.sendMessageToPlayer(ChatColor.DARK_RED
						+ "Your usage of rptokens seems wrong. :(");
			}

			break;

		case "rpmail":
			// Not used.
			break;
		case "ready":
			if (sender instanceof Player) {
				boolean promoterFound = false;

				for (Player p : Bukkit.getOnlinePlayers()) {
					if (p.hasPermission("groupmanager.manpromote")) {
						promoterFound = true;
						p.sendMessage(ChatColor.GOLD + "[RunicRanks] "
								+ ChatColor.LIGHT_PURPLE + sender.getName()
								+ " has completed the tutorial.");
						p.sendMessage(ChatColor.LIGHT_PURPLE + "Please use "
								+ ChatColor.AQUA + "/manpromote "
								+ sender.getName() + " Settler "
								+ ChatColor.LIGHT_PURPLE + "to promote them.");
					}
				}

				if (promoterFound) {
					sender.sendMessage(ChatColor.GOLD
							+ "[RunicRanks] "
							+ ChatColor.LIGHT_PURPLE
							+ "Staff is online and has been notified that you need a promotion.");
					sender.sendMessage(ChatColor.LIGHT_PURPLE
							+ "If you don't hear from them soon, they may be AFK. You can try /ready again later.");
				} else {
					sender.sendMessage(ChatColor.GOLD + "[RunicRanks] "
							+ ChatColor.LIGHT_PURPLE
							+ "There is no staff online right now.");
					sender.sendMessage(ChatColor.LIGHT_PURPLE
							+ "Post an introduction our forums and we'll promote you very soon :)");
					sender.sendMessage(ChatColor.LIGHT_PURPLE
							+ "Website: http://www.runic-paradise.com");
				}
			}
			break;
		case "radio":
		case "music":
			if (sender instanceof Player) {
				sender.sendMessage(ChatColor.AQUA + "[RunicRadio] "
						+ ChatColor.GRAY
						+ " Click to join > https://plug.dj/runic-paradise");
			}
			break;
		case "ranks":
			if (sender instanceof Player) {
				rank.showRequirements((Player) sender);
			}
			break;
		case "staff":
			if (sender instanceof Player) {
				if (args.length == 0 || args.length > 2) {
					// show menu
					sender.sendMessage(ChatColor.GRAY + "[" + ChatColor.BLUE
							+ "RunicStaff" + ChatColor.GRAY
							+ "] Welcome to the staff menu!");
					sender.sendMessage(ChatColor.BLUE
							+ "RunicRanks Commands & Tools");
					sender.sendMessage(ChatColor.AQUA + "/staff pe"
							+ ChatColor.GRAY
							+ " Check player stats for rank promotions");
					sender.sendMessage(ChatColor.AQUA + "/staff sr"
							+ ChatColor.GRAY
							+ " Show rank promotion requirements");
					sender.sendMessage(ChatColor.AQUA + "/staff kc"
							+ ChatColor.GRAY + " Show kill counts");
					sender.sendMessage(ChatColor.AQUA + "/staff nm <name>"
							+ ChatColor.GRAY
							+ " Nominate a player for faster promotions");
					sender.sendMessage(ChatColor.AQUA + "/staff sp <name>"
							+ ChatColor.GRAY
							+ " Announce a staff promotion [admin only]");
					sender.sendMessage(ChatColor.BLUE
							+ "RunicCarnival Commands & Tools");
					sender.sendMessage(ChatColor.AQUA + "/staff ct"
							+ ChatColor.GRAY
							+ " Display player carnival token balances");
					sender.sendMessage(ChatColor.BLUE
							+ "RunicReaper Commands & Tools");
					sender.sendMessage(ChatColor.AQUA
							+ "/staff lg <optional name>" + ChatColor.GRAY
							+ " Display recent graves");
					sender.sendMessage(ChatColor.AQUA + "/staff gg <grave id>"
							+ ChatColor.GRAY
							+ " Teleport to a grave. Find graves with LG.");
					sender.sendMessage(ChatColor.AQUA + "/staff ug <grave id>"
							+ ChatColor.GRAY + " Unlocks a locked grave.");
				} else if (args[0].equals("PE") || args[0].equals("pe")) {
					rank.playerStats((Player) sender);
				} else if (args[0].equals("GG") || args[0].equals("gg")) {
					RunicDeathChest.graveTeleport((Player) sender,
							Integer.parseInt(args[1]));
				} else if (args[0].equals("LG") || args[0].equals("lg")) {
					if (args.length == 2) {
						RunicDeathChest.listDeaths((Player) sender, args[1]);
					} else if (args.length == 1) {
						RunicDeathChest.listDeaths((Player) sender, "all");
					}
				} else if (args[0].equals("UG") || args[0].equals("ug")) {
					if (args.length == 2) {
						RunicDeathChest.unlockGrave((Player) sender,
								Integer.parseInt(args[1]));
					} else {
						sender.sendMessage(ChatColor.GRAY
								+ "[ERROR] /staff ug <graveID>");
					}
				} else if (args[0].equals("SR") || args[0].equals("sr")) {
					rank.showRequirements((Player) sender);
				} else if (args[0].equals("KC") || args[0].equals("kc")) {
					rank.playerkillCounts((Player) sender);
				} else if (args[0].equals("CT") || args[0].equals("ct")) {
					carnivalTokenCounts((Player) sender);
				} else if (args[0].equals("NM") || args[0].equals("nm")) {
					if (!args[1].isEmpty()) {
						rank.nominatePlayer((Player) sender, args[1]);
					} else {
						sender.sendMessage(ChatColor.GRAY
								+ "[ERROR] You need to provide a player's name! /staff nm name");
					}
				} else if (args[0].equals("SP") || args[0].equals("sp")) {
					if (sender.hasPermission("rp.admin")) {
						if (!args[1].isEmpty()) {
							for (Player p : Bukkit.getOnlinePlayers()) {
								p.sendMessage(ChatColor.DARK_RED
										+ "[RunicRanks] Congratulations, "
										+ args[1] + ChatColor.WHITE
										+ ", on a staff promotion!");
								p.getWorld().playSound(p.getLocation(),
										Sound.PORTAL_TRAVEL, 1, 0);
							}
						} else {
							sender.sendMessage(ChatColor.DARK_RED
									+ "[ERROR] You need to provide the name! /staff sp name");
						}
					} else {
						// user doesnt have permission for this command
						sender.sendMessage(ChatColor.DARK_RED
								+ "[ERROR] Only admins can use this command. But Rune doesnt blame you for trying. :)");
					}

				}
			}
			break;

		case "promote":
		case "rankup":
			if (sender instanceof Player) {
				// if no args provided, run a check to tell player if they
				// qualify for a promotion
				if (args.length == 0) {
					rank.checkPromotion((Player) sender, false);
				} else if (args[0].equals("me")) {
					// player is requesting to activate a promotion
					rank.checkPromotion((Player) sender, true);
				}
			} else {
				sender.sendMessage("[Error] Command must be used by a player");
			}

			break;
		case "rp":
		case "RP":
		case "Rp":
			if (sender instanceof Player) {
				RunicPlayerBukkit targetPlayer = new RunicPlayerBukkit(
						((Player) sender).getUniqueId());
				Map<String, Integer> killCounts = targetPlayer
						.getPlayerKillCounts();

				int daysSinceJoin = (int) ((new Date().getTime() - targetPlayer
						.getJoinDate().getTime()) / 86400000);

				SimpleDateFormat sdf = new SimpleDateFormat();
				sdf.applyPattern("EEE, MMM d, yyyy");

				DecimalFormat df = new DecimalFormat("#,###,###,##0");

				targetPlayer.sendMessageToPlayer(ChatColor.DARK_AQUA
						+ "Runic Paradise Player Info: <PlayerName>");
				targetPlayer.sendMessageToPlayer(ChatColor.DARK_AQUA
						+ "✦Your account details at Runic Central Bank");
				targetPlayer.sendMessageToPlayer(ChatColor.GRAY
						+ "  Runic balance: "
						+ ChatColor.GOLD
						+ df.format(RunicParadise.economy
								.getBalance((OfflinePlayer) sender)));
				targetPlayer.sendMessageToPlayer(ChatColor.GRAY
						+ "  Date joined: " + ChatColor.GOLD
						+ sdf.format(targetPlayer.getJoinDate().getTime())
						+ ChatColor.GRAY + ", Days since joining: "
						+ ChatColor.GOLD + daysSinceJoin);
				targetPlayer.sendMessageToPlayer(ChatColor.DARK_AQUA
						+ "✦Your status with Runic Security & Runic Farms");
				targetPlayer.sendMessageToPlayer(ChatColor.DARK_AQUA
						+ " Monster Kills");
				targetPlayer.sendMessageToPlayer(ChatColor.GRAY + "  Wither: "
						+ ChatColor.GOLD + killCounts.get("KillWither")
						+ ChatColor.GRAY + ", Zombie: " + ChatColor.GOLD
						+ killCounts.get("KillZombie") + ChatColor.GRAY
						+ ", Witch: " + ChatColor.GOLD
						+ killCounts.get("KillWitch") + ChatColor.GRAY
						+ ", Skeletons: " + ChatColor.GOLD
						+ killCounts.get("KillSkeleton"));
				targetPlayer.sendMessageToPlayer(ChatColor.GRAY + "  Slime: "
						+ ChatColor.GOLD + killCounts.get("KillSlime")
						+ ChatColor.GRAY + ", MagmaCube: " + ChatColor.GOLD
						+ killCounts.get("KillMagmaCube") + ChatColor.GRAY
						+ ", Silverfish: " + ChatColor.GOLD
						+ killCounts.get("KillSilverfish") + ChatColor.GRAY
						+ ", Giant: " + ChatColor.GOLD
						+ killCounts.get("KillGiant"));
				targetPlayer.sendMessageToPlayer(ChatColor.GRAY + "  Blaze: "
						+ ChatColor.GOLD + killCounts.get("KillBlaze")
						+ ChatColor.GRAY + ", Creeper: " + ChatColor.GOLD
						+ killCounts.get("KillCreeper") + ChatColor.GRAY
						+ ", Enderman: " + ChatColor.GOLD
						+ killCounts.get("KillEnderman") + ChatColor.GRAY
						+ ", Spider: " + ChatColor.GOLD
						+ killCounts.get("KillSpider"));
				targetPlayer.sendMessageToPlayer(ChatColor.GRAY
						+ "  CaveSpider: " + ChatColor.GOLD
						+ killCounts.get("KillCaveSpider") + ChatColor.GRAY
						+ ", Squid: " + ChatColor.GOLD
						+ killCounts.get("KillSquid") + ChatColor.GRAY
						+ ", EnderDragon: " + ChatColor.GOLD
						+ killCounts.get("KillEnderDragon") + ChatColor.GRAY
						+ ", PigZombie: " + ChatColor.GOLD
						+ killCounts.get("KillPigZombie"));
				targetPlayer.sendMessageToPlayer(ChatColor.GRAY + "  Ghast: "
						+ ChatColor.GOLD + killCounts.get("KillGhast")
						+ ChatColor.GRAY + ", Bat: " + ChatColor.GOLD
						+ killCounts.get("KillBat") + ChatColor.GRAY
						+ ", Wolf: " + ChatColor.GOLD
						+ killCounts.get("KillWolf") + ChatColor.GRAY
						+ ", Endermite: " + ChatColor.GOLD
						+ killCounts.get("KillEndermite") + ChatColor.GRAY
						+ ", Guardian: " + ChatColor.GOLD
						+ killCounts.get("KillGuardian"));
				targetPlayer.sendMessageToPlayer(ChatColor.GRAY
						+ "  ElderGuardian: " + ChatColor.GOLD
						+ killCounts.get("KillElderGuardian") + ChatColor.GRAY
						+ ", SnowGolem " + ChatColor.GOLD
						+ killCounts.get("KillSnowGolem") + ChatColor.GRAY
						+ ", IronGolem: " + ChatColor.GOLD
						+ killCounts.get("KillIronGolem"));
				targetPlayer.sendMessageToPlayer(ChatColor.DARK_AQUA
						+ " Animal & People Kills");
				targetPlayer.sendMessageToPlayer(ChatColor.GRAY + "  Chicken: "
						+ ChatColor.GOLD + killCounts.get("KillChicken")
						+ ChatColor.GRAY + ", Cow: " + ChatColor.GOLD
						+ killCounts.get("KillCow") + ChatColor.GRAY
						+ ", Sheep: " + ChatColor.GOLD
						+ killCounts.get("KillSheep") + ChatColor.GRAY
						+ ", Pig: " + ChatColor.GOLD
						+ killCounts.get("KillPig") + ChatColor.GRAY
						+ ", Villager: " + ChatColor.GOLD
						+ killCounts.get("KillVillager"));
				targetPlayer.sendMessageToPlayer(ChatColor.GRAY + "  Ocelot: "
						+ ChatColor.GOLD + killCounts.get("KillOcelot")
						+ ChatColor.GRAY + ", Rabbit: " + ChatColor.GOLD
						+ killCounts.get("KillRabbit") + ChatColor.GRAY
						+ ", Mooshroom: " + ChatColor.GOLD
						+ killCounts.get("KillMooshroom"));
				targetPlayer.sendMessageToPlayer(ChatColor.DARK_AQUA
						+ "✦Your relationship with Runic Reaper");
				targetPlayer.sendMessageToPlayer(ChatColor.GRAY
						+ "  Souls remaining: " + ChatColor.GOLD
						+ targetPlayer.getPlayerSouls());
				targetPlayer.sendMessageToPlayer(ChatColor.GRAY
						+ "  Graves created: " + ChatColor.GOLD
						+ targetPlayer.getCountGravesCreated() + ChatColor.GRAY
						+ ", Unopened: " + ChatColor.GOLD
						+ targetPlayer.getCountGravesRemaining()
						+ ChatColor.GRAY + ", Stolen: " + ChatColor.GOLD
						+ targetPlayer.getCountGravesStolen());
				targetPlayer.sendMessageToPlayer(ChatColor.DARK_AQUA
						+ "✦Your employment history");
				targetPlayer.sendMessageToPlayer(ChatColor.GRAY
						+ "  Jobs mastered: " + ChatColor.GOLD
						+ targetPlayer.getMasteredJobCount());

			} else {
				sender.sendMessage("[RP] Command RP must be used by a player");
				return true;
			}
			break;
		case "rptest":
		case "RPTEST":
		case "Rptest":

			if (args.length == 0) {
				Bukkit.getServer().broadcastMessage(
						ChatColor.GRAY
								+ instance.getConfig().getString(
										"testAnnouncement"));
				if (sender instanceof Player) {
					final Player player = (Player) sender;
					player.sendMessage(ChatColor.GRAY
							+ "[RunicTester] "
							+ instance.getConfig().getString(
									"testPersonalMessage"));

					BukkitScheduler scheduler = Bukkit.getServer()
							.getScheduler();
					int taskId = 0;
					taskId = scheduler.scheduleAsyncRepeatingTask(instance,
							new Runnable() {
								@Override
								public void run() {
									player.getWorld().playEffect(
											player.getLocation(),
											Effect.MOBSPAWNER_FLAMES, 0);
								}
							}, 0L, 20L);

					PARTICLE_TASK_IDS.add(taskId);

				}
			} else if (args.length == 1 && args[0].equals("listtasks")) {

				Player p = (Player) sender;
				for (int taskToKill : PARTICLE_TASK_IDS) {
					p.sendMessage(ChatColor.GRAY
							+ "Repeating Particle Task Running: " + taskToKill);
				}
				p.sendMessage(ChatColor.GRAY + "listtasks argument received");

			} else if (args.length == 1 && args[0].equals("killtasks")) {

				Player p = (Player) sender;
				for (int taskToKill : PARTICLE_TASK_IDS) {
					p.sendMessage(ChatColor.GRAY + "Killing Particle Task: "
							+ taskToKill);
					Bukkit.getServer().getScheduler().cancelTask(taskToKill);
				}
				p.sendMessage(ChatColor.GRAY + "killtasks argument received");

			}

			System.out
					.println("[RunicTester] Runic Paradise plugin test executed.");
			break;
		case "rpreload":
		case "RPRELOAD":
		case "Rpreload":
			instance.reloadConfig();
			if (sender instanceof Player) {
				Player player = (Player) sender;
				player.sendMessage(ChatColor.GRAY
						+ "[RP] Runic Paradise plugin reloaded.");
			}

			System.out.println("[RP] Runic Paradise plugin reloaded.");
			break;
		case "hmsay":
			if (sender instanceof Player) {
				Player s = (Player) sender;
				s.sendMessage(ChatColor.RED + "Players cannot use /hmsay");
			} else {
				String message = "";
				for (String b : args) {
					message += b + " ";
				}
				List<Player> mansionPlayers = Bukkit.getWorld("Mansion")
						.getPlayers();
				for (Player p : mansionPlayers) {
					p.sendMessage(message);
				}

			}
			break;
		case "say":
			if (sender instanceof ConsoleCommandSender) {
				String message = "";
				for (String b : args) {
					message += b + " ";
				}

				for (Player p : Bukkit.getServer().getOnlinePlayers()) {
					p.sendMessage(message);
				}
			} else {
				BlockCommandSender senderCmd = (BlockCommandSender) sender;
				Block senderBlock = senderCmd.getBlock();

				if (senderBlock.getWorld().equals("Mansion")) {
					String message = "";
					for (String b : args) {
						message += b + " ";
					}
					List<Player> mansionPlayers = Bukkit.getWorld("Mansion")
							.getPlayers();
					for (Player p : mansionPlayers) {
						p.sendMessage(message);
					}
				} else if (senderBlock.getWorld().equals("Razul")) {
					String message = "";
					for (String b : args) {
						message += b + " ";
					}
					List<Player> mansionPlayers = Bukkit.getWorld("Razul")
							.getPlayers();
					for (Player p : mansionPlayers) {
						p.sendMessage(message);
					}
				} else {
					// command block sender in a world not specified above
					String message = "";
					for (String b : args) {
						message += b + " ";
					}

					for (Player p : Bukkit.getServer().getOnlinePlayers()) {
						p.sendMessage(message);
					}
				}

			}
			break;
		case "events":
		case "EVENTS":
			if (args.length == 0) {
				Player player = (Player) sender;
				player.sendMessage(ChatColor.DARK_GRAY
						+ "StartTownInvasion, StopTownInvasion");
			} else if (args.length == 1) {
				if (args[0].equals("StartTownInvasion")) {
					Events s = new Events();
					s.spawntonInvasionStartup();
				}
				if (args[0].equals("StopTownInvasion")) {
					Events s = new Events();
					s.spawntonInvasionStop();
				}
				if (args[0].equals("TriggerTownInvasion")) {
					Events s = new Events();
					s.spawntonInvasionTrigger();
				}
			}
			break;
		case "rpgames":
		case "RPGAMES":
		case "games":
		case "GAMES":
			if (sender instanceof Player) {
				Player player = (Player) sender;
				int tokenBal = 0;
				try {
					final Connection d = MySQL.openConnection();
					Statement dStmt = d.createStatement();
					ResultSet playerData = dStmt
							.executeQuery("SELECT * FROM `rp_PlayerInfo` WHERE `PlayerName` = '"
									+ sender.getName()
									+ "' ORDER BY `id` ASC LIMIT 1;");
					playerData.next();
					tokenBal = playerData.getInt("Tokens");
					d.close();
				} catch (SQLException e) {
					getLogger().log(
							Level.SEVERE,
							"Failed token count DB check [games] because: "
									+ e.getMessage());
				}

				if (args.length == 0 || args.length > 1) {
					player.sendMessage(ChatColor.YELLOW + "  ✸ "
							+ ChatColor.GOLD + "✹ " + ChatColor.RED + "✺ "
							+ ChatColor.RED + "Runic Carnival" + ChatColor.RED
							+ " ✺ " + ChatColor.GOLD + "✹ " + ChatColor.YELLOW
							+ "✸");
					player.sendMessage(ChatColor.GREEN + "      ♪ "
							+ ChatColor.DARK_AQUA + "♫ " + ChatColor.AQUA
							+ "☾ " + ChatColor.BLUE + "Tokens: " + tokenBal
							+ ChatColor.AQUA + " ☽ " + ChatColor.DARK_AQUA
							+ "♫ " + ChatColor.GREEN + "♪");

					// player.sendMessage(ChatColor.WHITE + "" +
					// ChatColor.ITALIC
					// + "Format: /games [option]");
					player.sendMessage(ChatColor.DARK_RED + "[1] "
							+ ChatColor.GRAY + "Information Center");
					player.sendMessage(ChatColor.RED + "[2] " + ChatColor.GRAY
							+ "Prize Cabin");
					player.sendMessage(ChatColor.GOLD + "[3] " + ChatColor.GRAY
							+ "Puzzle Kiosk");
					player.sendMessage(ChatColor.YELLOW + "[4] "
							+ ChatColor.GRAY + "High Roller Casino"
							+ ChatColor.DARK_GRAY + " (Coming Soon!)");
					player.sendMessage(ChatColor.GREEN + "[5] "
							+ ChatColor.GRAY + "Game Corner");
					player.sendMessage(ChatColor.DARK_AQUA + "[6] "
							+ ChatColor.GRAY + "Quest Castle"
							+ ChatColor.DARK_GRAY + " (Adventure Maps)");
					player.sendMessage(ChatColor.BLUE + "[7] " + ChatColor.GRAY
							+ "Battle Tower" + ChatColor.DARK_GRAY
							+ " (Mob & PVP Arenas)");
					player.sendMessage(ChatColor.LIGHT_PURPLE + "[8] "
							+ ChatColor.GRAY + "Creation Zone"
							+ ChatColor.DARK_GRAY + " (Build Contests)");
				} else if (args.length == 1) {
					String temp = "";
					try {
						int option = Integer.parseInt(args[0]);
					} catch (Exception e) {
						player.sendMessage(ChatColor.GRAY
								+ "[ERROR] Invalid entry. Please check options via /games");
						return true;
					}
					switch (Integer.parseInt(args[0])) {
					case 1:
						player.teleport(new Location(Bukkit
								.getWorld("RunicSky"), 342, 58, 548, 0,
								(float) 1));
						break;
					case 2:
						player.teleport(new Location(Bukkit
								.getWorld("RunicSky"), 320, 58, 522,
								(float) 92.50, (float) -16.05));
						break;
					case 3:
						player.teleport(new Location(Bukkit
								.getWorld("RunicSky"), 328, 58, 543,
								(float) 72.99, (float) -26.40));
						break;
					case 4:
						player.teleport(new Location(Bukkit
								.getWorld("RunicSky"), 328, 58, 507,
								(float) 135.499, (float) -23.99));
						break;
					case 5:
						player.teleport(new Location(Bukkit
								.getWorld("RunicSky"), 342, 58, 507,
								(float) 180.35, (float) -28.95));
						break;
					case 6:
						player.teleport(new Location(Bukkit
								.getWorld("RunicSky"), 358, 58, 508,
								(float) -131.25, (float) -27.600));
						break;
					case 7:
						player.teleport(new Location(Bukkit
								.getWorld("RunicSky"), 359, 58, 522,
								(float) -90.300, (float) -42.4499));
						break;
					case 8:
						player.teleport(new Location(Bukkit
								.getWorld("RunicSky"), 357, 58, 538,
								(float) -42.150, (float) -27.85));
						break;
					case 9:
						player.sendMessage("your yaw "
								+ player.getLocation().getYaw());
						player.sendMessage("your pitch "
								+ player.getLocation().getPitch());
						break;
					default:
						player.sendMessage(ChatColor.GRAY
								+ "[ERROR] Invalid entry. Please check options via /games");
						break;
					}
					return true;
				}

			} else {
				sender.sendMessage("[RP] Command must be used by a player");
				return true;
			}
			break;
		case "staffchat":
		case "sc":

			String senderName = "";
			if (sender instanceof Player) {
				Player player = (Player) sender;
				senderName = sender.getName();
			} else {
				senderName = "Console";

			}

			StringBuilder buffer = new StringBuilder();
			// change the starting i value to pick what argument to start from
			// 1 is the 2nd argument.
			for (int i = 0; i < args.length; i++) {
				buffer.append(' ').append(args[i]);
			}

			for (Player p : Bukkit.getOnlinePlayers()) {

				if (p.hasPermission("rp.staff")) {
					if (args.length == 0) {
						Player player = (Player) sender;
						player.sendMessage(ChatColor.DARK_GRAY
								+ "Staff chat. Usage: /sc [message]");
					} else {

						p.sendMessage(ChatColor.DARK_GRAY + "["
								+ ChatColor.DARK_AQUA + "Staff"
								+ ChatColor.AQUA + "Chat" + ChatColor.DARK_GRAY
								+ "] " + ChatColor.WHITE + senderName + ":"
								+ ChatColor.AQUA + buffer.toString());
						getLogger().log(
								Level.INFO,
								"[StaffChat] " + senderName + ": "
										+ buffer.toString());
					}
				}
			}
		default:
			break;
		}

		return true;
	}

	public void carnivalTokenCounts(Player player) {
		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));
		final Connection d = MySQL.openConnection();

		player.sendMessage(ChatColor.GRAY
				+ "[RunicCarnival] Listing player token counts...");

		for (Player p : Bukkit.getOnlinePlayers()) {
			try {
				Statement dStmt = d.createStatement();
				ResultSet playerData = dStmt
						.executeQuery("SELECT * FROM `rp_PlayerInfo` WHERE `PlayerName` = '"
								+ p.getName() + "' ORDER BY `id` ASC LIMIT 1;");
				playerData.next();
				int tokenCount = playerData.getInt("Tokens");

				player.sendMessage(ChatColor.GRAY + p.getName() + ": "
						+ ChatColor.GREEN + "" + tokenCount);
			} catch (SQLException e) {
				getLogger().log(
						Level.SEVERE,
						"Failed token count DB check [staff cmd ct] because: "
								+ e.getMessage());
			}
		}

		try {
			d.close();
		} catch (SQLException e) {
			getLogger().log(
					Level.SEVERE,
					"Failed DB close for carnivalTokenCounts because: "
							+ e.getMessage());
		}
	}

}
