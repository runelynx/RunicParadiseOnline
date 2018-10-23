package io.github.runelynx.runicparadise;

import com.connorlinfoot.titleapi.TitleAPI;
import com.xxmicloxx.NoteBlockAPI.NBSDecoder;
import com.xxmicloxx.NoteBlockAPI.RadioSongPlayer;
import com.xxmicloxx.NoteBlockAPI.Song;
import com.xxmicloxx.NoteBlockAPI.SongPlayer;
import io.github.runelynx.runicuniverse.RunicMessaging;
import io.github.runelynx.runicuniverse.RunicMessaging.RunicFormat;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.InputStream;
import java.sql.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class Commands implements CommandExecutor {

	Ranks rank = new Ranks();

	private Plugin instance = RunicParadise.getInstance();

	public static List<Integer> PARTICLE_TASK_IDS = new ArrayList<>();

    private static boolean searchExplorerLocation(Location loc, Player p) {
        int targetID = 0;
        int distance = -1;
        boolean noneFound = false;

        int greenWarmthMultiplier = 2;
        int yellowWarmthMultiplier = 4;
        int redWarmthMultiplier = 6;

        if (RunicParadise.explorerLocations.isEmpty()) {
            // Just in case the map is empty -- load it up! This happened on
            // 8/10/16 somehow. :-/
            Commands.syncExplorerLocations();
        }

        for (Location l : RunicParadise.explorerLocations.values()) {
            if (l == null || l.getWorld() == null || l.getWorld().getName() == null) {
                Commands.syncExplorerLocations();
                Bukkit.getLogger().log(Level.WARNING,
                        "RunicWarning - Runic Explorers League - Someone tried to use /explore and I encountered a null location in the explorerLocations hashmap. Therefore, resyncing the location maps now.");
            }
            if (l.getWorld().getName().equals(loc.getWorld().getName())) {
                // Make sure worlds match before taking distance

                if (distance == -1 || loc.distance(l) < distance) {
                    // Compare with last distance. The idea is to only retain
                    // the closest distance loc via this loop.
                    distance = (int) loc.distance(l);
                    targetID = RunicParadise.explorerLocationsReversed.get(l);
                }
            }
        }

        if (targetID != 0) {
            // A distance & loc were found, so let's run our logic.
            int difficulty = RunicParadise.explorerDifficulties.get(targetID);
            if (distance <= difficulty) {
                // found location!
                RunicParadise.playerProfiles.get(p.getUniqueId()).completePlayerExploration(targetID);
                playNBS(p, "ZeldaSecret");

            } else if (distance <= greenWarmthMultiplier * difficulty) {
                // Green OK!
                TitleAPI.sendFullTitle(p, 1, 2, 1, ChatColor.GREEN + "✸ ✸ ✸",
                        ChatColor.DARK_GREEN + "You are very close to a secret spot!");
                p.sendMessage(ChatColor.DARK_GREEN + "You are very close to a secret spot!");
            } else if (distance <= yellowWarmthMultiplier * difficulty) {
                // Yellow OK!}
                TitleAPI.sendFullTitle(p, 1, 2, 1, ChatColor.YELLOW + "✸ ✸ ✸",
                        ChatColor.GOLD + "You are kinda close to a secret spot!");
                p.sendMessage(ChatColor.GOLD + "You are kinda close to a secret spot!");
            } else if (distance <= redWarmthMultiplier * difficulty) {
                // Red OK!
                TitleAPI.sendFullTitle(p, 1, 2, 1, ChatColor.RED + "✸ ✸ ✸",
                        ChatColor.DARK_RED + "There is a secret spot in your general area!");
                p.sendMessage(ChatColor.DARK_RED + "There is a secret spot in your general area!");
            } else {
                noneFound = true;
            }

        } else {
            noneFound = true;
        }

        if (noneFound) {
            TitleAPI.sendFullTitle(p, 1, 2, 1, ChatColor.AQUA + "✕ ✕ ✕",
                    ChatColor.DARK_AQUA + "There are no secret spots nearby!");
            p.sendMessage(ChatColor.DARK_AQUA + "There are no secret spots nearby!");
        }

        return false;
    }

    static ItemStack[] carnivalChestReward(Location loc) {
        Block b = loc.getBlock();
        org.bukkit.block.Chest chest = (org.bukkit.block.Chest) b.getState();
        return chest.getBlockInventory().getContents();

    }

    private static void playNBS(Player p, String song) {
        try {
            Song s = NBSDecoder.parse(
                    new File("/home/AMP/.ampdata/instances/Survival/Minecraft/plugins/RunicParadise/" + song + ".nbs"));
            SongPlayer sp = new RadioSongPlayer(s);

            sp.setAutoDestroy(true);
            for (Entity listener : p.getNearbyEntities(30, 30, 30)) {
                if (listener instanceof Player) {
                    sp.addPlayer((Player) listener);
                }

            }
            sp.addPlayer(p);
            sp.setPlaying(true);
        } catch (Exception ignored) {}
    }

    static boolean syncExplorerLocations() {
        int locCount = 0;

        // reset the hashmap
        RunicParadise.explorerLocations.clear();
        RunicParadise.explorerDifficulties.clear();
        RunicParadise.explorerRewards.clear();
        RunicParadise.explorerLocationsReversed.clear();
        RunicParadise.explorerIDs.clear();
        RunicParadise.explorerPrereqs.clear();

        // retrieve updated Explorer data
        Plugin instance = RunicParadise.getInstance();
        MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
        try {
            Connection connection = MySQL.openConnection();
            Statement statement = connection.createStatement();
            ResultSet explorerLocData = statement.executeQuery(
                    "SELECT * FROM rp_ExplorerLocations WHERE Status != 'Disabled' ORDER BY `Order` ASC;");
            // if (!playerData.first() && !playerData.next()) {
            if (!explorerLocData.isBeforeFirst()) {
                // No results
                // do nothing
                connection.close();
                return true;
            } else {
                // results found!
                while (explorerLocData.next()) {
                    String[] locParts = explorerLocData.getString("Location").split("[\\x2E]");
                    Location newLoc = new Location(Bukkit.getWorld(locParts[0]), Integer.parseInt(locParts[1]),
                            Integer.parseInt(locParts[2]), Integer.parseInt(locParts[3]));

                    RunicParadise.explorerLocations.put(explorerLocData.getInt("ID"), newLoc);
                    RunicParadise.explorerLocationsReversed.put(newLoc, explorerLocData.getInt("ID"));
                    RunicParadise.explorerDifficulties.put(explorerLocData.getInt("ID"),
                            explorerLocData.getInt("DifficultyRadius"));
                    RunicParadise.explorerRewards.put(explorerLocData.getInt("ID"),
                            explorerLocData.getInt("TokenReward"));
                    RunicParadise.explorerPrereqs.put(explorerLocData.getInt("ID"), explorerLocData.getInt("PreReq"));
                    RunicParadise.explorerIDs.put(explorerLocData.getInt("ID"),
                            explorerLocData.getString("LocationName"));

                    locCount++;
                }

                connection.close();

                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        "sc " + locCount + " explorer locs loaded into memory!");
            }

        } catch (SQLException z) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed map sync for explorer locs cuz " + z.getMessage());
        }
        return true;
    }

    private void carnivalTokenCounts(Player player) {
        MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
        Connection connection = MySQL.openConnection();

        player.sendMessage(ChatColor.GRAY + "[RunicCarnival] Listing player token counts...");

        for (Player p : Bukkit.getOnlinePlayers()) {
            try {
                Statement statement = connection.createStatement();
                ResultSet playerData = statement.executeQuery("SELECT * FROM `rp_PlayerInfo` WHERE `PlayerName` = '"
                        + p.getName() + "' ORDER BY `id` ASC LIMIT 1;");
                playerData.next();
                int tokenCount = playerData.getInt("Tokens");

                player.sendMessage(ChatColor.GRAY + p.getName() + ": " + ChatColor.GREEN + "" + tokenCount);
            } catch (SQLException e) {
                Bukkit.getLogger().log(Level.SEVERE,
                        "Failed token count DB check [staff cmd ct] because: " + e.getMessage());
            }
        }

        try {
            connection.close();
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed DB close for carnivalTokenCounts because: " + e.getMessage());
        }
    }

    private boolean addAttemptedPromotion(String newGuyName, String promoterName) {
        MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);

	    try {
            final Connection dbCon = MySQL.openConnection();

            String simpleProc = "{ call Add_Attempted_Promotion_Record(?, ?) }";
            CallableStatement cs = dbCon.prepareCall(simpleProc);
            cs.setString("NewPlayerName_param", Bukkit.getPlayer(newGuyName).getName());
            cs.setString("PromoterName_param", Bukkit.getPlayer(promoterName).getName());
            cs.executeUpdate();

            cs.close();
            dbCon.close();

            return true;
        } catch (SQLException z) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed addAttemptedPromotion - " + z.getMessage());
            return false;
        }
    }

    private int checkAttemptedPromotion(String newGuyName, String promoterName) {

        MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
        try {
            final Connection dbCon = MySQL.openConnection();

            String simpleProc = "{ call Count_Attempted_Promotion_Records(?, ?, ?) }";
            CallableStatement cs = dbCon.prepareCall(simpleProc);
            cs.setString("NewPlayerName_param", Bukkit.getPlayer(newGuyName).getName());
            cs.setString("PromoterName_param", Bukkit.getPlayer(promoterName).getName());
            cs.registerOutParameter("resultCount", java.sql.Types.INTEGER);
            cs.executeUpdate();

            int result = cs.getInt("resultCount");

            cs.close();
            dbCon.close();

            return result;

        } catch (SQLException z) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed checkAttemptedPromotion - " + z.getMessage());
            return 0;
        }
    }

    private int checkPlayerInventoryItemstackCount(Inventory i) {
		return (int) Arrays.stream(i.getContents()).filter(item -> item != null && item.getType() != Material.AIR).count();
    }

    private ItemStack[] casinoWinOrLose(Location loc) {
        Block b = loc.getBlock();
        org.bukkit.block.Chest chest = (org.bukkit.block.Chest) b.getState();
        return chest.getBlockInventory().getContents();
    }

    private ItemStack giveCasinoToken(String playerName, int count) {
        ItemStack casinoToken = new ItemStack(Material.SUNFLOWER, count);
        ItemMeta casinoTokenMeta = casinoToken.getItemMeta();
        casinoTokenMeta.setDisplayName(ChatColor.GOLD + "Runic Casino Token");
        casinoTokenMeta.setLore(Arrays.asList(ChatColor.GRAY + "A token you can use in the",
                ChatColor.GRAY + "Runic Casino to gamble.", ChatColor.GRAY + "You can buy more with runics",
                ChatColor.GRAY + "or convert these back to runics.", ChatColor.GRAY + "Visit the casino using /games",
                " ", ChatColor.GREEN + "Purchased by " + ChatColor.DARK_GREEN + playerName));
        casinoToken.setItemMeta(casinoTokenMeta);
        return casinoToken;
    }

    private void displayELParticle(Location loc, Player p) {
        for (int degree = 0; degree < 360; degree++) {
            double radians = Math.toRadians(degree);
            double x = Math.cos(radians);
            double z = Math.sin(radians);
            loc.add(x, 1.5, z);
            loc.getWorld().playEffect(loc, Effect.MOBSPAWNER_FLAMES, 1);

            loc.subtract(x, 1.5, z);
        }

        Song s = NBSDecoder.parse(
                new File("/home/AMP/.ampdata/instances/Survival/Minecraft/plugins/RunicParadise/ZeldaTriforce.nbs"));
        SongPlayer sp = new RadioSongPlayer(s);
        sp.setAutoDestroy(true);
        sp.addPlayer(p);
        sp.setPlaying(true);

    }

	@SuppressWarnings("deprecation")
	public boolean onCommand(final CommandSender sender, Command cmd, String label, String[] args) {
		// general approach is that errors will return immediately;
		// successful runs will return after the switch completes
		switch (cmd.getName()) {
		case "rpversion":
			handleRpVersion(sender);
			break;
		case "fixranks":
			RunicUtilities.fixGroupManager();
			break;
		case "rpfix":
			rpFixCommand(sender);
			break;
		case "runicspawntravel":
			runicSpawnTravelCommand(sender);
			break;
		case "raffle":
			raffleCommand(sender, args);
			break;
		case "search":
		case "explore":
			exploreCommand(sender);
			break;
		case "el":
			elCommand(sender, args);
			break;
		case "faithweapons":
			faithWeaponsCommand(sender);
			break;
		case "faithweapon":
			faithWeaponCommand(args);
			break;
		case "casino":
			casinoCommand(sender, args);
			break;
		case "freezemob":
			freezeMob((Player) sender, true);
			break;
		case "unfreezemob":
			freezeMob((Player) sender, false);
			break;
		case "wild":
			wildCommand(sender);
			break;
		case "miningworld":
		case "mw":
			miningWorldCommand(sender);
			break;
		case "iteminfo":
			itemInfoCommand((Player) sender);
			break;
		case "miningreset":
			miningResetCommand(sender, args);
			break;
		case "miningworldreminder":
			miningWorldReminderCommand();
			break;
		case "machinemaze":
			machineMazeCommand(sender, args);
			break;
		case "crocomaze":
			crocoMazeCommand(sender, args);
			break;
		case "adventureparkourprize":
			adventureParkourPrizeCommand(sender, args);
			break;
		case "junglemaze":
			jungleMazeCommand(sender, args);
			break;
		case "voice":
		case "discord":
			discordCommand(sender);
			break;
		case "dailykarma":
			dailyKarmaCommand(sender, args);
			break;
		case "cactifever":
			cactiFeverCommand(sender);
			break;
		case "runiceye":
			RunicParadise.loadRunicEyes();
			break;
		case "faith":
			faithCommand(sender, args);
			break;
		case "rpjobs":
			return rpJobsCommand(sender, args);
		case "rpvote":
			rpVoteCommand(sender, args);
			break;
		case "graves":
		case "grave":
			graveCommand(sender, args);
			break;
		case "rpcrates":
			rpCratesCommand(sender, args);
			break;
		case "rprewards":
			rpRewardsCommand(sender, args);
			break;
		case "rankitem":
			rankItemCommand(sender, args);
			break;
		case "rptokens":
			return rpTokensCommand(sender, args);
		case "consoleseeker":
			consoleSeekerCommand(sender, args);
			break;
		case "settler":
		case "seeker":
			seekerCommand(sender, args);
			break;
		case "ready":
			readyCommand(sender, args);
			break;
		case "radio":
		case "music":
			radioCommand(sender, args);
			break;
		case "punish":
			punishCommand(sender, args);
			break;
		case "staff":
			staffCommand(sender, args);
			break;
		case "rp":
			rpCommand(sender, args);
			break;
		case "rptest":
			rpTestCommand(sender);
			break;
		case "rpreload":
			rpReloadCommand(sender, args);
			break;
		case "say":
			sayCommand(sender, args);
			break;
		case "headofplayer":
		case "face":
			faceCommand(sender, args);
			break;
		case "rpgames":
		case "games":
			gamesCommand(sender, args);
			break;
		case "testerchat":
		case "tc":
			testerChatCommand(sender, args);
			break;
		case "staffchat":
		case "sc":
			staffChatCommand(sender, args);
			break;
		default:
			break;
		}
		return true;
	}

	private static void exploreCommand(CommandSender sender) {
		Player explorePlayer = ((Player) sender);
		searchExplorerLocation(explorePlayer.getLocation(), explorePlayer);
	}

	private static void runicSpawnTravelCommand(CommandSender sender) {
    	Player player = (Player) sender;
		spawnTransportBeacon(player.getLocation(), player);
	}

	private static void rpFixCommand(CommandSender sender) {
    	if (!(sender instanceof Player)) {
    		sender.sendMessage("You must run this command in game");
    		return;
	    }
		Player player = ((Player) sender);
		PlayerInventory inventory = player.getInventory();
		repairCommand(player, inventory.getItemInMainHand(), inventory.getItemInOffHand());
	}

	private static void faithWeaponsCommand(CommandSender sender) {
    	Inventory inventory = ((Player) sender).getInventory();
		inventory.addItem(Recipes.customItemStacks("FAITH_AXE_1"));
		inventory.addItem(Recipes.customItemStacks("FAITH_SWORD_1"));
		inventory.addItem(Recipes.customItemStacks("FAITH_SWORD_2"));
		inventory.addItem(Recipes.customItemStacks("FAITH_SWORD_3"));
	}

	private static void faithWeaponCommand(String[] args) {
		if (args == null || args.length != 2) {
			return;
		}
		Inventory inventory = Bukkit.getPlayer(args[1]).getInventory();
		if (args[0].equalsIgnoreCase("sword1")) {
			inventory.addItem(Recipes.customItemStacks("FAITH_SWORD_1"));
		} else if (args[0].equalsIgnoreCase("sword2")) {
			inventory.addItem(Recipes.customItemStacks("FAITH_SWORD_2"));
		} else if (args[0].equalsIgnoreCase("sword3")) {
			inventory.addItem(Recipes.customItemStacks("FAITH_SWORD_3"));
		} else if (args[0].equalsIgnoreCase("axe1")) {
			inventory.addItem(Recipes.customItemStacks("FAITH_AXE_1"));
		}
	}

	private static void wildCommand(CommandSender sender) {
		((Player) sender).teleport(
				new Location(Bukkit.getWorld("RunicSky"), -308.688, 126, -411.603, 270.8571F, 4.7532725F));
		sender.sendMessage(ChatColor.YELLOW + "There are portals to different areas of the wilderness here - look for a biome you like and head into the portal.\n"
				+ ChatColor.DARK_RED + ChatColor.BOLD + "Borderlands" + ChatColor.RESET + ChatColor.YELLOW + " areas have VERY tough monsters!");
	}

	private static void miningWorldCommand(CommandSender sender) {
		((Player) sender).teleport(
				new Location(Bukkit.getWorld("RunicSky"), -639.232, 64.0, 326.465, 93.31604F, -4.499901F));
		sender.sendMessage(
				ChatColor.YELLOW + "The mining world portal is ahead of you. That world resets sometimes so do not build or leave any items or graves there or you risk losing them!\n"
				+ ChatColor.YELLOW + "Explosions break blocks in the mining world.");
	}

	private static void miningWorldReminderCommand() {
    	String message = ChatColor.GRAY.toString() + ChatColor.ITALIC + "Mining world resets every day! Don't leave anything here; items or graves!";
		Bukkit.getWorld("Mining").getPlayers().forEach(player -> player.sendMessage(message));
	}

	private static void adventureParkourPrizeCommand(CommandSender sender, String[] args) {
		RunicParadise.playerProfiles.get(Bukkit.getPlayer(args[0]).getUniqueId()).addMazeCompletion(7);
	}

	private static void discordCommand(CommandSender sender) {
		sender.sendMessage(ChatColor.LIGHT_PURPLE + "We use Discord as our voice chat system.\n"
				+ ChatColor.LIGHT_PURPLE + "Remember our server rules still apply there! Be respectful to others and keep it clean!\n"
				+ ChatColor.DARK_RED + "Click here to learn how to use Discord: " + ChatColor.GRAY + "http://goo.gl/X1dg8W\n"
				+ ChatColor.DARK_RED + "Click here to join Discord: " + ChatColor.GRAY + "http://www.runic-paradise.com/discord.php");
	}

	private static void cactiFeverCommand(CommandSender sender) {
		// TODO: needs fixing
		ItemStack skull = new ItemStack(Material.PLAYER_HEAD, 1, (short) 3);
		SkullMeta meta1 = (SkullMeta) skull.getItemMeta();

		meta1.setOwner("The_King_Cacti");
		meta1.setDisplayName(ChatColor.AQUA + "The_King_Cacti");
		skull.setItemMeta(meta1);

		Player player = (Player) sender;
		player.getWorld().dropItemNaturally(player.getLocation(), skull);
		sender.sendMessage(ChatColor.YELLOW.toString() + ChatColor.BOLD + "You've been infected with CactiFever!");

	}

	private static void rpVoteCommand(CommandSender sender, String[] args) {
		if (args[0].equals("reward") && args.length == 2) {
			int votecount = 1;
			if (Bukkit.getPlayer(args[1]).hasPermission("rp.xmas")) {
				votecount = 2;
			}

			String command = "crate givekey " + args[1] + " RunicCrate 1";
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

			RunicPlayerBukkit targetPlayer = new RunicPlayerBukkit(args[1]);
			targetPlayer.incrementPlayerVotes();

			/*
			 *
			 * RunicParadise.economy.depositPlayer(Bukkit.getOfflinePlayer(
			 * args[1]), 1000);
			 *
			 * // String command = "eco give " + args[1] + " 1000"; //
			 * Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
			 *
			 * // command = "graves givesouls " + args[1] + " " + votecount;
			 * // Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
			 * command);
			 *
			 * // targetPlayer.setPlayerSouls(targetPlayer.getPlayerSouls()
			 * + // votecount); targetPlayer.addPlayerSouls(1);
			 * RunicMessaging.sendMessage(Bukkit.getPlayer(args[1]),
			 * RunicMessaging.RunicFormat.EMPTY, "You gained a soul!");
			 *
			 * targetPlayer.incrementPlayerVotes();
			 * targetPlayer.adjustPlayerKarma(3); int newTokenBalance =
			 * targetPlayer.getPlayerTokenBalance() + 2;
			 * targetPlayer.setPlayerTokenBalance(newTokenBalance);
			 *
			 * rand = new Random(); // int randomNum = rand.nextInt((max -
			 * min) + 1) + min; int randomNum = rand.nextInt((100 - 1) + 1)
			 * + 1; if (randomNum <= 5) { String command =
			 * "graves givesouls " + args[1] + " 7";
			 * Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
			 * command = "say §4L§cu§6c§ek§2y §av§bo§3t§1e§9!§d! " + args[1]
			 * + " got 7 extra souls!";
			 *
			 * Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
			 *
			 * command = "say §4L§cu§6c§ek§2y §av§bo§3t§1e§9!§d! " + args[1]
			 * + " got 10 extra karma!"; targetPlayer.adjustPlayerKarma(10);
			 * Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command); }
			 */
		}
	}

	private void graveCommand(CommandSender sender, String[] args) {
		boolean showHelp = false;
		if (sender instanceof Player) {
			Player player = (Player) sender;
			RunicPlayerBukkit senderPlayer = new RunicPlayerBukkit(player.getUniqueId());
			if (args.length > 0) {
				if (args[0].equals("expire") && player.hasPermission("rp.staff")) {
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
				} else if (args[0].equals("restore") && sender.hasPermission("rp.admin")) {
					RunicDeathChest.restoreByCommand(args[1], Integer.parseInt(args[2]));
					// } else if (args[0].equals("quit")) {
					// Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
					// "manudelp " + player.getName() + " rp.graves");
					// } else if (args[0].equals("secret") && args.length ==
					// 2) {
					// RunicDeathChest.restoreByCommand(player.getName(),
					// Integer.parseInt(args[1]));
				} else if (args[0].equals("create") && sender.hasPermission("rp.admin")) {
					RunicDeathChest.savePlayerDeath_v19((Player) sender, ((Player) sender).getLocation());

				} else {
					showHelp = true;
				}
			} else {
				showHelp = true;
			}

			// Show help!!
			if (showHelp) {
				player.sendMessage(ChatColor.DARK_GRAY + "[RunicReaper] " + ChatColor.GRAY
						+ "Welcome to the Runic Afterlife...:");
				player.sendMessage(ChatColor.GRAY + "You have " + ChatColor.AQUA + senderPlayer.getPlayerSouls()
						+ ChatColor.GRAY + " souls remaining.");
				player.sendMessage(ChatColor.AQUA + "/graves list " + ChatColor.GRAY + "List your graves");
				// player.sendMessage(ChatColor.GRAY
				// +
				// "Restore a grave to you using its ID number [for
				// testers]");
				// player.sendMessage(ChatColor.AQUA
				// + "/graves secret GraveIDNumber");
				// player.sendMessage(ChatColor.GRAY
				// + "Stop testing graves [for testers]");
				// player.sendMessage(ChatColor.AQUA + "/graves quit");

			}
		} else {
			if (args[0].equals("expire")) {
				RunicDeathChest.unlockExpiredGraves(true);
				Bukkit.getLogger().log(Level.INFO, "[RP] Running the graves expire command.");
			} else if (args[0].equals("givesouls") && args.length == 3 && Integer.parseInt(args[2]) > 0
					&& !(sender instanceof Player)) {
				RunicPlayerBukkit targetPlayer = new RunicPlayerBukkit(args[1]);
				int newSouls = targetPlayer.getPlayerSouls() + Integer.parseInt(args[2]);
				targetPlayer.sendMessageToPlayer(ChatColor.GRAY + "[" + ChatColor.DARK_RED + "Runic" + ChatColor.RED
						+ "Reaper" + ChatColor.GRAY + "] " + ChatColor.LIGHT_PURPLE + "The Reaper grants you "
						+ ChatColor.WHITE + args[2] + ChatColor.LIGHT_PURPLE + " more souls. You have "
						+ ChatColor.WHITE + newSouls + ChatColor.LIGHT_PURPLE + ".");

				targetPlayer.setPlayerSouls(newSouls);
				Bukkit.getLogger().log(Level.INFO, "[RP] Gave " + args[2] + " souls to " + args[1]);
			}
		}
	}

	private void rankItemCommand(CommandSender sender, String[] args) {
		if (args.length == 3) {
			if (args[1].equals("DukeMetal") && args[0].equalsIgnoreCase("Give")) {
				Bukkit.getPlayer(args[2]).getLocation().getWorld().dropItemNaturally(
						Bukkit.getPlayer(args[2]).getLocation(),
						Borderlands.specialLootDrops("DukeMetal", Bukkit.getPlayer(args[2]).getUniqueId()));
				Bukkit.getPlayer(args[2]).getLocation().getWorld().dropItemNaturally(
						Bukkit.getPlayer(args[2]).getLocation(),
						Borderlands.specialLootDrops("DukeMetal", Bukkit.getPlayer(args[2]).getUniqueId()));
			} else if (args[1].equals("BaronMetal") && args[0].equalsIgnoreCase("Give")) {
				Bukkit.getPlayer(args[2]).getLocation().getWorld().dropItemNaturally(
						Bukkit.getPlayer(args[2]).getLocation(),
						Borderlands.specialLootDrops("BaronMetal", Bukkit.getPlayer(args[2]).getUniqueId()));
			} else if (args[0].equalsIgnoreCase("Check")) {
				// rankitem check duke runelynx
				Ranks.craftFeudalJewelry(Bukkit.getPlayer(args[2]), args[1]);
			}
		}
	}

	private void rpReloadCommand(CommandSender sender, String[] args) {
		instance.reloadConfig();
		if (sender instanceof Player) {
			Player player = (Player) sender;
			player.sendMessage(ChatColor.GRAY + "[RP] Runic Paradise plugin reloaded.");
		}

		System.out.println("[RP] Runic Paradise plugin reloaded.");
	}

	private void rpCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player) {
			Player p = ((Player) sender);
			UUID pUUID;
			RunicPlayerBukkit commandPlayer = new RunicPlayerBukkit(sender.getName());

			RunicParadise.playerProfiles.get(p.getUniqueId()).showServerMenu(p);

			if (args.length == 0) {
				pUUID = ((Player) sender).getUniqueId();
			} else {
				pUUID = Bukkit.getOfflinePlayer(args[0]).getUniqueId();
			}

			RunicPlayerBukkit targetPlayer = new RunicPlayerBukkit(pUUID);
			Map<String, Integer> killCounts = targetPlayer.getPlayerKillCounts();

			int daysSinceJoin = (int) ((new Date().getTime() - targetPlayer.getJoinDate().getTime()) / 86400000);

			SimpleDateFormat sdf = new SimpleDateFormat();
			sdf.applyPattern("EEE, MMM d, yyyy");

			DecimalFormat df = new DecimalFormat("#,###,###,##0");

			commandPlayer.sendMessageToPlayer(ChatColor.DARK_AQUA + "Runic Paradise Player Info: "
					+ Bukkit.getPlayer(pUUID).getDisplayName());
			commandPlayer.sendMessageToPlayer(ChatColor.DARK_AQUA + "✦Personal information");
			commandPlayer.sendMessageToPlayer(ChatColor.GRAY + "  Runic balance: " + ChatColor.GOLD
					+ df.format(RunicParadise.economy.getBalance(Bukkit.getPlayer(pUUID)))
					+ ChatColor.GRAY + ", Votes: " + ChatColor.GOLD + targetPlayer.getPlayerVoteCount());
			commandPlayer.sendMessageToPlayer(ChatColor.GRAY + "  Date joined: " + ChatColor.GOLD
					+ sdf.format(targetPlayer.getJoinDate().getTime()) + ChatColor.GRAY + ", Days since joining: "
					+ ChatColor.GOLD + daysSinceJoin);

			commandPlayer.sendMessageToPlayer(ChatColor.DARK_AQUA + "✦Faith & the afterlife");
			commandPlayer.sendMessageToPlayer(
					ChatColor.GRAY + "  Karma available: " + ChatColor.GOLD + targetPlayer.getKarma());
			commandPlayer.sendMessageToPlayer(
					ChatColor.GRAY + "  Souls remaining: " + ChatColor.GOLD + targetPlayer.getPlayerSouls());
			commandPlayer.sendMessageToPlayer(ChatColor.GRAY + "  Graves created: " + ChatColor.GOLD
					+ targetPlayer.getCountGravesCreated() + ChatColor.GRAY + ", Unopened: " + ChatColor.GOLD
					+ targetPlayer.getCountGravesRemaining() + ChatColor.GRAY + ", Stolen: " + ChatColor.GOLD
					+ targetPlayer.getCountGravesStolen());
			commandPlayer.sendMessageToPlayer(ChatColor.DARK_AQUA + "✦Employment history");
			commandPlayer.sendMessageToPlayer(
					ChatColor.GRAY + "  Jobs mastered: " + ChatColor.GOLD + targetPlayer.getMasteredJobCount());

		} else {
			sender.sendMessage("[RP] Command RP must be used by a player");
		}
	}

	private void staffCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player) {
			if (args.length == 0) {
				// show menu
				sender.sendMessage(ChatColor.GRAY + "[" + ChatColor.BLUE + "RunicStaff" + ChatColor.GRAY
						+ "] Welcome to the staff menu!");
				sender.sendMessage(ChatColor.BLUE + "RunicRanks Commands & Tools");
				sender.sendMessage(ChatColor.AQUA + "/staff ec" + ChatColor.GRAY + " Count entities near players");
				sender.sendMessage(
						ChatColor.AQUA + "/staff pe" + ChatColor.GRAY + " Check player stats for rank promotions");
				sender.sendMessage(
						ChatColor.AQUA + "/staff sr" + ChatColor.GRAY + " Show rank promotion requirements");
				sender.sendMessage(ChatColor.AQUA + "/staff nm <name>" + ChatColor.GRAY
						+ " Nominate a player for faster promotions");
				sender.sendMessage(ChatColor.AQUA + "/staff sp <name>" + ChatColor.GRAY
						+ " Announce a staff promotion [admin only]");
				sender.sendMessage(ChatColor.BLUE + "RunicCarnival Commands & Tools");
				sender.sendMessage(
						ChatColor.AQUA + "/staff ct" + ChatColor.GRAY + " Display player carnival token balances");
				sender.sendMessage(ChatColor.BLUE + "RunicReaper Commands & Tools");
				sender.sendMessage(
						ChatColor.AQUA + "/staff vt <name>" + ChatColor.GRAY + " Give a vote reward. DONT ABUSE!");
				sender.sendMessage(
						ChatColor.AQUA + "/staff lg <optional name>" + ChatColor.GRAY + " Display recent graves");
				sender.sendMessage(ChatColor.AQUA + "/staff gg <grave id>" + ChatColor.GRAY
						+ " Teleport to a grave. Find graves with LG.");
				sender.sendMessage(
						ChatColor.AQUA + "/staff ug <grave id>" + ChatColor.GRAY + " Unlocks a locked grave.");
				sender.sendMessage(ChatColor.BLUE + "Misc Commands & Tools");
				sender.sendMessage(
						ChatColor.AQUA + "/staff np <name>" + ChatColor.GRAY + " Find who is near someone");
				sender.sendMessage(
						ChatColor.AQUA + "/punish <name>" + ChatColor.GRAY + " Tool to help with punish commands");
				sender.sendMessage(ChatColor.AQUA + "/staff cf" + ChatColor.GRAY + " Check farming status");
				if (sender.hasPermission("rp.staff.director")) {
					sender.sendMessage(
							ChatColor.AQUA + "/censor" + ChatColor.GRAY + " Chat censor for all servers");
				}
				sender.sendMessage(ChatColor.AQUA + "/staff setplayercolor <player> <color>" + ChatColor.GRAY + " Set player color");
			} else if (args[0].equals("PE") || args[0].equals("pe")) {
				rank.playerStats((Player) sender);
			} else if (args[0].equals("EC") || args[0].equals("ec")) {
				int entityCounter = 0;
				for (Player p : Bukkit.getOnlinePlayers()) {
					ChatColor c;

					int entityCount = p.getNearbyEntities(400, 300, 400).size();

					if (entityCount < 100) {
						c = ChatColor.GREEN;
					} else if (entityCount < 250) {
						c = ChatColor.YELLOW;
					} else if (entityCount < 500) {
						c = ChatColor.GOLD;
					} else if (entityCount < 750) {
						c = ChatColor.RED;
					} else if (entityCount < 1500) {
						c = ChatColor.DARK_RED;
					} else {
						c = ChatColor.DARK_PURPLE;
					}

					entityCounter += entityCount;
					sender.sendMessage(p.getDisplayName() + ": " + c + entityCount + ChatColor.WHITE
							+ " entities within 200 blocks.");
				}
				sender.sendMessage(ChatColor.RED + "Total: " + entityCounter
						+ " found. If players are near each other this may include double-counts.");

			} else if (args[0].equals("LC") || args[0].equals("lc")) {
				for (Player p : Bukkit.getOnlinePlayers()) {
					for (Entity e : p.getNearbyEntities(200, 256, 200)) {
						int size = e.getNearbyEntities(20, 20, 20).size();
						if (size > 150) {
							sender.sendMessage(size + " entities in a small area at " + e.getLocation().toString());

						}
					}

				}
			} else if (args[0].equals("CF") || args[0].equals("cf")) {
				String tag;
				RunicMessaging.sendMessage((Player) sender, RunicFormat.BORDERLANDS, "Player farming status list:");
				for (Player p : Bukkit.getOnlinePlayers()) {
					if (RunicParadise.playerProfiles.get(p.getUniqueId()).isPlayerFarming()) {
						tag = ChatColor.RED + "IS FARMING, RankDrops= "
								+ RunicParadise.playerProfiles.get(p.getUniqueId()).rankDropCountLast24Hours;
					} else {
						tag = ChatColor.GREEN + "NOT FARMING, RankDrops= "
								+ RunicParadise.playerProfiles.get(p.getUniqueId()).rankDropCountLast24Hours;
					}
					RunicMessaging.sendMessage((Player) sender, RunicFormat.EMPTY, p.getDisplayName() + " " + tag);
				}

			} else if (args[0].equals("GG") || args[0].equals("gg")) {
				RunicDeathChest.graveTeleport((Player) sender, Integer.parseInt(args[1]));
			} else if (args[0].equals("LG") || args[0].equals("lg")) {
				if (args.length == 2) {
					RunicDeathChest.listDeaths((Player) sender, args[1]);
				} else if (args.length == 1) {
					RunicDeathChest.listDeaths((Player) sender, "all");
				}
			} else if (args[0].equals("UG") || args[0].equals("ug")) {
				if (args.length == 2) {
					RunicDeathChest.unlockGrave((Player) sender, Integer.parseInt(args[1]));
				} else {
					sender.sendMessage(ChatColor.GRAY + "[ERROR] /staff ug <grgaveID>");
				}
			} else if (args[0].equals("SR") || args[0].equals("sr")) {
				rank.showRequirements((Player) sender);
			} else if (args[0].equals("VT") || args[0].equals("vt")) {
				String command = "rpvote reward " + args[1];
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
			} else if (args[0].equals("CT") || args[0].equals("ct")) {
				carnivalTokenCounts((Player) sender);
			} else if (args[0].equals("NM") || args[0].equals("nm")) {
				if (args[1].isEmpty()) {
					sender.sendMessage(ChatColor.GRAY + "[ERROR] You need to provide a player's name! /staff nm name");
				} else {
					rank.nominatePlayer((Player) sender, args[1]);
				}
			} else if (args[0].equals("SP") || args[0].equals("sp")) {
				if (sender.hasPermission("rp.admin")) {
					if (!args[1].isEmpty()) {
						for (Player p : Bukkit.getOnlinePlayers()) {
							p.sendMessage(ChatColor.DARK_RED + "[RunicRanks] Congratulations, " + ChatColor.WHITE
									+ args[1] + ChatColor.DARK_RED + ", on a staff promotion!");
							// p.getWorld().playSound(p.getLocation(),
							// .BLOCK_CHORUS_FLOWER_GROW, 10, 1);
						}
					} else {
						sender.sendMessage(
								ChatColor.DARK_RED + "[ERROR] You need to provide the name! /staff sp name");
					}
				} else {
					// user doesn't have permission for this command
					sender.sendMessage(ChatColor.DARK_RED
							+ "[ERROR] Only admins can use this command. But Rune doesnt blame you for trying. :)");
				}

			} else if ((args[0].equals("NP") || args[0].equals("np")) && args.length == 2) {
				RunicMessaging.sendMessage(((Player) sender), RunicMessaging.RunicFormat.SYSTEM,
						whoIsNearPlayer(Bukkit.getPlayer(args[1])));
			} else if (args.length == 3 && args[0].equalsIgnoreCase("setplayercolor")) {
				Player player = Bukkit.getPlayer(args[1]);
				if (player == null) {
					sender.sendMessage("No player found with that name");
					return;
				}
				RunicProfile profile = RunicParadise.playerProfiles.get(player.getUniqueId());
				profile.setChatColor(args[2].toUpperCase(), true);
			} else {
				RunicMessaging.sendMessage(((Player) sender), RunicMessaging.RunicFormat.SYSTEM,
						ChatColor.LIGHT_PURPLE + "Hmm... please check your command usage with /staff");
			}
		}
	}

	private static void punishCommand(CommandSender sender, String[] args) {
		if (args.length == 0 && sender instanceof Player) {
			sender.sendMessage(ChatColor.DARK_AQUA + "Correct usage: /punish playername");
		} else if (args.length == 1) {
			sender.sendMessage("This command is not working right now. :(");
		}
	}

	private static void radioCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player) {
			sender.sendMessage(ChatColor.AQUA + "[RunicRadio] " + ChatColor.GRAY
					+ " Click to join > https://www.dubtrack.fm/join/runic-paradise-minecraft-server");
		}
	}

	private static void readyCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player) {
			Player p = ((Player) sender);

			if (p.hasPermission("rp.ready")) {

				RunicMessaging.sendMessage(p, RunicMessaging.RunicFormat.RANKS,
						"You have been promoted to SEEKER!");
				RunicMessaging.sendMessage(p, RunicMessaging.RunicFormat.EMPTY,
						"Get to the tutorial anytime: " + ChatColor.AQUA + "/warp start");
				RunicMessaging.sendMessage(p, RunicMessaging.RunicFormat.EMPTY,
						"Go to the wilderness portals: " + ChatColor.AQUA + "/wild");
				RunicMessaging.sendMessage(p, RunicMessaging.RunicFormat.EMPTY,
						"PvP, Minigames, Contests: " + ChatColor.AQUA + "/games");
				RunicMessaging.sendMessage(p, RunicMessaging.RunicFormat.EMPTY,
						"Learn about jobs: " + ChatColor.AQUA + "/warp jobs");
				RunicMessaging.sendMessage(p, RunicMessaging.RunicFormat.EMPTY, "Learn about religions & powers: "
						+ ChatColor.AQUA + "/warp faith " + ChatColor.GRAY + "or " + ChatColor.AQUA + "/faith");
				RunicMessaging.sendMessage(p, RunicMessaging.RunicFormat.EMPTY,
						"Say hello on our forums! " + ChatColor.AQUA + "www.runic-paradise.com");

				RunicParadise.perms.playerAddGroup(null, p, "Seeker");

				RunicParadise.perms.playerRemove(p, "rp.ready");

				for (Player a : Bukkit.getOnlinePlayers()) {
					RunicMessaging.sendMessage(a, RunicMessaging.RunicFormat.EMPTY,
							ChatColor.DARK_AQUA + p.getName() + ChatColor.GRAY
									+ " has started their adventure as a " + ChatColor.GREEN + "Seeker"
									+ ChatColor.GRAY + "!");

				}
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "faith enable " + p.getName() + " Sun");

				RunicParadise.playerProfiles.get(p.getUniqueId()).setChatColor(ChatColor.GREEN, true);
			}
		}
	}

	private void seekerCommand(CommandSender sender, String[] args) {
		if (Bukkit.getPlayer(args[0]).hasPermission("rp.ready")
				&& checkAttemptedPromotion(args[0], sender.getName()) == 0) {

			RunicParadise.perms.playerAddGroup(null, Bukkit.getPlayer(args[0]), "Seeker");

			RunicParadise.perms.playerRemove(Bukkit.getPlayer(args[0]), "rp.ready");

			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "faith enable " + args[0] + " Sun");

			sender.sendMessage(ChatColor.GREEN + "Command worked! :)");
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "graves givesouls " + sender.getName() + " 2");
			RunicPlayerBukkit senderPlayer = new RunicPlayerBukkit(((Player) sender).getUniqueId());
			senderPlayer.adjustPlayerKarma(1);

			addAttemptedPromotion(args[0], senderPlayer.getPlayerName());
			RunicParadise.playerProfiles.get((Bukkit.getPlayer(args[0])).getUniqueId()).setChatColor(ChatColor.GREEN, true);

		} else if (checkAttemptedPromotion(args[0], sender.getName()) == 0
				&& RunicParadise.newReadyPlayer.containsKey(Bukkit.getPlayer(args[0]).getName())) {
			// command sender has not tried to promote this player yet AND
			// the player is a valid newbie

			Random rand = new Random();
			int randomNum = rand.nextInt((100 - 1) + 1) + 1;
			if (randomNum <= 50) {
				new RunicPlayerBukkit(((Player) sender).getUniqueId()).adjustPlayerKarma(2);
				sender.sendMessage(ChatColor.RED + "You were too slow... but at least you got some karma!");
			} else {
				sender.sendMessage(ChatColor.RED + "You were too slow... and didn't get any karma this time. ");
			}

			addAttemptedPromotion(args[0], sender.getName());
		} else {
			// completely invalid attempt
			sender.sendMessage(ChatColor.RED + "You can't use that command on that player anymore.");
		}
	}

	private static void consoleSeekerCommand(CommandSender sender, String[] args) {
		if (Bukkit.getPlayer(args[0]).hasPermission("rp.ready")) {
			RunicParadise.perms.playerAddGroup(Bukkit.getPlayer(args[0]), "Seeker");
			RunicParadise.perms.playerRemove(Bukkit.getPlayer(args[0]), "rp.ready");

			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "faith setlevel " + args[0] + " Sun 0");
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "faith enable " + args[0] + " Sun");

			RunicParadise.playerProfiles.get((Bukkit.getPlayer(args[0])).getUniqueId()).setChatColor(ChatColor.GREEN, true);
		}
	}

	private static boolean rpTokensCommand(CommandSender sender, String[] args) {
		if (args.length == 0 || args[0].equals("help")) {
			if (sender instanceof Player) {
				RunicPlayerBukkit senderPlayer = new RunicPlayerBukkit((Player) sender);
				senderPlayer.sendMessageToPlayer(ChatColor.GOLD + "[RunicCarnival] How to form rptokens commands:");
				senderPlayer.sendMessageToPlayer(ChatColor.GRAY + "Take tokens and execute command as reward:");
				senderPlayer.sendMessageToPlayer(ChatColor.DARK_GRAY + "/rptokens take PLAYERNAME TOKENCOUNT COMMAND");
				senderPlayer.sendMessageToPlayer(ChatColor.GRAY + "Reward karma:");
				senderPlayer.sendMessageToPlayer(ChatColor.DARK_GRAY + "/rptokens givekarma PLAYERNAME TOKENCOUNT KARMACOUNT");
				senderPlayer.sendMessageToPlayer(ChatColor.GRAY + "Take tokens and give a chest-inv reward:");
				senderPlayer.sendMessageToPlayer(ChatColor.DARK_GRAY + "/rptokens chestreward PLAYERNAME TOKENCOUNT X Y Z");
				senderPlayer.sendMessageToPlayer(ChatColor.GRAY + "Give or take tokens:");
				senderPlayer.sendMessageToPlayer(ChatColor.DARK_GRAY + "/rptokens give/take PLAYERNAME TOKENCOUNT");
				senderPlayer.sendMessageToPlayer(ChatColor.GRAY + "Give trophies:");
				senderPlayer.sendMessageToPlayer(ChatColor.DARK_GRAY + "/rptokens givetrophy PLAYERNAME TROPHYCOUNT");
				senderPlayer.sendMessageToPlayer(ChatColor.GRAY + "Take all trophies and give tokens:");
				senderPlayer.sendMessageToPlayer(ChatColor.DARK_GRAY + "/rptokens taketrophy PLAYERNAME");
				senderPlayer.sendMessageToPlayer(ChatColor.GRAY + "Add maze win to player total:");
				senderPlayer.sendMessageToPlayer(ChatColor.DARK_GRAY + "/rptokens mazewin PLAYERNAME MazeID");
			}
		} else if (args.length == 2 && args[0].equals("checkbalance")) {
			RunicPlayerBukkit targetPlayer = new RunicPlayerBukkit(args[1]);

			TitleAPI.sendTitle(Bukkit.getPlayer(args[1]), 2, 3, 2,
					RunicMessaging.getRandomColor() + "" + ChatColor.BOLD + targetPlayer.getPlayerTokenBalance(),
					RunicMessaging.getRandomColor() + "Your current token balance. Get more in /games");
		} else if (args.length == 4 && Integer.parseInt(args[2]) > -1 && Integer.parseInt(args[3]) > 0
				&& args[0].equals("givekarma")) {

			RunicPlayerBukkit targetPlayer = new RunicPlayerBukkit(args[1]);

			targetPlayer.sendMessageToPlayer(ChatColor.GOLD + "[RunicCarnival] Prize cost: " + args[2] + " tokens");

			if (targetPlayer.getPlayerTokenBalance() >= Integer.parseInt(args[2])) {
				int newBalance = targetPlayer.getPlayerTokenBalance() - Integer.parseInt(args[2]);

				// Update their balance
				if (targetPlayer.setPlayerTokenBalance(newBalance)) {
					// DB update finished successfully, proceed...
					targetPlayer.sendMessageToPlayer(ChatColor.GOLD + "[RunicCarnival] You have turned in "
							+ ChatColor.DARK_RED + args[2] + ChatColor.GOLD + " tokens");
					targetPlayer.sendMessageToPlayer(ChatColor.GOLD + "[RunicCarnival] Your new token balance is "
							+ ChatColor.GREEN + newBalance + ChatColor.GOLD + " tokens");

					targetPlayer.adjustPlayerKarma(Integer.parseInt(args[3]));

				} else {
					// DB update failed!
					targetPlayer.sendMessageToPlayer(ChatColor.DARK_RED
							+ "[ERROR] Something went wrong, couldn't update balance. No tokens have been taken!");
				}

			} else {
				targetPlayer.sendMessageToPlayer(
						ChatColor.GOLD + "[RunicCarnival] Sorry, you don't have enough tokens. You have "
								+ ChatColor.GREEN + targetPlayer.getPlayerTokenBalance() + ChatColor.GOLD + ".");
			}

		} else if (args.length == 6 && Integer.parseInt(args[2]) > -1 && args[0].equals("chestreward")) {

			RunicPlayerBukkit targetPlayer = new RunicPlayerBukkit(args[1]);

			if (Integer.parseInt(args[2]) > 0) {
				targetPlayer
						.sendMessageToPlayer(ChatColor.GOLD + "[RunicCarnival] Prize cost: " + args[2] + " tokens");
			}

			if (targetPlayer.getPlayerTokenBalance() >= Integer.parseInt(args[2])) {
				int newBalance = targetPlayer.getPlayerTokenBalance() - Integer.parseInt(args[2]);

				// Update their balance
				if (targetPlayer.setPlayerTokenBalance(newBalance)) {

					if (Integer.parseInt(args[2]) > 0) {
						// DB update finished successfully, proceed...
						targetPlayer.sendMessageToPlayer(ChatColor.GOLD + "[RunicCarnival] You have turned in "
								+ ChatColor.DARK_RED + args[2] + ChatColor.GOLD + " tokens");
						targetPlayer
								.sendMessageToPlayer(ChatColor.GOLD + "[RunicCarnival] Your new token balance is "
										+ ChatColor.GREEN + newBalance + ChatColor.GOLD + " tokens");
					}

					// process the rest of the command
					int x = Integer.parseInt(args[3]);
					int y = Integer.parseInt(args[4]);
					int z = Integer.parseInt(args[5]);

					ItemStack[] rewards = carnivalChestReward(new Location(Bukkit.getWorld("RunicSky"), x, y, z));

					if (Integer.parseInt(args[2]) > 0) {
						for (ItemStack i : rewards) {
							if (i.getType() != Material.AIR && i.getType() != null) {

								Bukkit.getPlayer(args[1]).getWorld()
										.dropItemNaturally(Bukkit.getPlayer(args[1]).getLocation(), i);
							}
						}
					} else {

						// this is not a token payment reward, its probably
						// a parkour/maze reward so we need to give the
						// items directly
						for (ItemStack i : rewards) {
							if (i.getType() != Material.AIR && i.getType() != null) {

								Bukkit.getPlayer(args[1]).getInventory().addItem(i);
							}
						}
					}

				} else {
					// DB update failed!
					targetPlayer.sendMessageToPlayer(ChatColor.DARK_RED
							+ "[ERROR] Something went wrong, couldn't update balance. No tokens have been taken!");
				}

			} else {
				targetPlayer.sendMessageToPlayer(
						ChatColor.GOLD + "[RunicCarnival] Sorry, you don't have enough tokens. You have "
								+ ChatColor.GREEN + targetPlayer.getPlayerTokenBalance() + ChatColor.GOLD + ".");
			}
			// /////////////////////////////////////////////////////
			// check for rptokens TAKE command; ensure tokencount is
			// valid (positive integer)
			// if command is successful, execute a console command
			// /rptokens take PLAYER COUNT COMMANDTOEXECUTE
		} else if (args.length > 3 && Integer.parseInt(args[2]) > -1 && args[0].equals("take")) {

			RunicPlayerBukkit targetPlayer = new RunicPlayerBukkit(args[1]);

			targetPlayer.sendMessageToPlayer(ChatColor.GOLD + "[RunicCarnival] Prize cost: " + args[2] + " tokens");

			if (targetPlayer.getPlayerTokenBalance() >= Integer.parseInt(args[2])) {
				int newBalance = targetPlayer.getPlayerTokenBalance() - Integer.parseInt(args[2]);

				// Update their balance
				if (targetPlayer.setPlayerTokenBalance(newBalance)) {
					// DB update finished successfully, proceed...
					targetPlayer.sendMessageToPlayer(ChatColor.GOLD + "[RunicCarnival] You have turned in "
							+ ChatColor.DARK_RED + args[2] + ChatColor.GOLD + " tokens");
					targetPlayer.sendMessageToPlayer(ChatColor.GOLD + "[RunicCarnival] Your new token balance is "
							+ ChatColor.GREEN + newBalance + ChatColor.GOLD + " tokens");
					// process the rest of the command
					int counter = 3; // start counter at the right spot
					// (/rptokens take name 5 give
					// name
					// item)
					StringBuilder successCommand = new StringBuilder();
					while (counter <= (args.length - 1)) {
						successCommand.append(args[counter]).append(" ");
						counter++;
					}
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), successCommand.toString());
				} else {
					// DB update failed!
					targetPlayer.sendMessageToPlayer(ChatColor.DARK_RED
							+ "[ERROR] Something went wrong, couldn't update balance. No tokens have been taken!");
				}

			} else {
				targetPlayer.sendMessageToPlayer(
						ChatColor.GOLD + "[RunicCarnival] Sorry, you don't have enough tokens. You have "
								+ ChatColor.GREEN + targetPlayer.getPlayerTokenBalance() + ChatColor.GOLD + ".");
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

			int newBalance = targetPlayer.getPlayerTokenBalance() - Integer.parseInt(args[2]);
			if (newBalance < 0) {
				newBalance = 0;
			}

			// Update their balance
			if (targetPlayer.setPlayerTokenBalance(newBalance)) {
				// DB Update worked
				String senderName = "Someone";
				if (sender instanceof Player) {
					RunicPlayerBukkit senderPlayer = new RunicPlayerBukkit((Player) sender);
					senderName = senderPlayer.getPlayerDisplayName();
				}
				targetPlayer.sendMessageToPlayer(ChatColor.GOLD + "[RunicCarnival] " + senderName + ChatColor.GOLD
						+ " has taken " + ChatColor.DARK_RED + args[2] + ChatColor.GOLD + " of your tokens!");
				targetPlayer.sendMessageToPlayer(ChatColor.GOLD + "[RunicCarnival] Your new token balance: "
						+ ChatColor.GREEN + newBalance + ChatColor.GOLD + " tokens");
				if (sender instanceof Player) {

					RunicPlayerBukkit senderPlayer = new RunicPlayerBukkit((Player) sender);
					senderPlayer.sendMessageToPlayer(
							ChatColor.GOLD + "[RunicCarnival] " + targetPlayer.getPlayerDisplayName()
									+ ChatColor.GOLD + "'s tokens - previous " + ChatColor.DARK_RED
									+ (targetPlayer.getPlayerTokenBalance() + Integer.parseInt(args[2]))
									+ ChatColor.GOLD + ", new " + ChatColor.GREEN + newBalance);
				}
			} else {
				// DB update failed
				targetPlayer.sendMessageToPlayer(ChatColor.DARK_RED
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

			int newBalance = targetPlayer.getPlayerTokenBalance() + Integer.parseInt(args[2]);

			// Update their balance
			if (targetPlayer.setPlayerTokenBalance(newBalance)) {
				// DB update worked
				targetPlayer.sendMessageToPlayer(ChatColor.GOLD + "[RunicCarnival] " + ChatColor.GREEN + args[2]
						+ ChatColor.GOLD + " tokens awarded!");
				targetPlayer.sendMessageToPlayer(ChatColor.GOLD + "[RunicCarnival] Your new token balance: "
						+ ChatColor.GREEN + newBalance + ChatColor.GOLD + " tokens");
				if (sender instanceof Player) {

					RunicPlayerBukkit senderPlayer = new RunicPlayerBukkit((Player) sender);
					senderPlayer.sendMessageToPlayer(
							ChatColor.GOLD + "[RunicCarnival] " + targetPlayer.getPlayerDisplayName()
									+ ChatColor.GOLD + "'s new token balance: " + ChatColor.GREEN + newBalance);
				}
			} else {
				// DB update failed
				targetPlayer.sendMessageToPlayer(ChatColor.DARK_RED
						+ "[ERROR] Something went wrong, couldn't update balance. Find Rune or check your command!");
			}

			// /////////////////////////////////////////////////////
			// take trophy command; takes all trophies on player
			// and gives them that number of tokens
			// /rptokens taketrophy PLAYER
		} else if (args.length == 2 && args[0].equals("taketrophy")) {
			// TODO: fix taketrophy
//				RunicPlayerBukkit targetPlayer = new RunicPlayerBukkit(args[1]);
//
//				int trophyCount = targetPlayer.checkPlayerInventoryForItemDataCount(371, 99);
//
//				if (trophyCount > 0) {
//
//					int newBalance = targetPlayer.getPlayerTokenBalance() + trophyCount;
//					if (newBalance < 0) {
//						newBalance = 0;
//					}
//
//					// Update their balance
//					if (targetPlayer.setPlayerTokenBalance(newBalance)) {
//						// DB update worked
//						targetPlayer.sendMessageToPlayer(ChatColor.GOLD + "[RunicCarnival] You have turned in "
//								+ ChatColor.GREEN + trophyCount + ChatColor.GOLD + " carnival trophies!");
//						targetPlayer.sendMessageToPlayer(ChatColor.GOLD + "[RunicCarnival] Your new token balance: "
//								+ ChatColor.GREEN + +newBalance + ChatColor.GOLD + " tokens");
//						if (sender instanceof Player) {
//
//							RunicPlayerBukkit senderPlayer = new RunicPlayerBukkit((Player) sender);
//							senderPlayer.sendMessageToPlayer(ChatColor.GOLD + "[RunicCarnival] "
//									+ targetPlayer.getPlayerDisplayName() + ChatColor.GOLD
//									+ "'s new token balance after trophy turn-in: " + ChatColor.GREEN + newBalance);
//							int removedTrophies = targetPlayer.removePlayerInventoryItemData(371, 99);
//
//							Bukkit.getLogger().log(Level.INFO,
//									"RunicCarnival gave " + trophyCount + " credits to " + targetPlayer.getPlayerName()
//
//											+ " and removed " + removedTrophies + " trophies");
//							Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
//									"sc RunicCarnival gave " + ChatColor.GREEN + trophyCount + ChatColor.AQUA
//											+ " credits to " + targetPlayer.getPlayerName() + " and removed "
//											+ ChatColor.DARK_RED + removedTrophies + ChatColor.AQUA + " trophies");
//						}
//					} else {
//						// DB update failed
//						targetPlayer.sendMessageToPlayer(ChatColor.DARK_RED
//								+ "[ERROR] Something went wrong, couldn't update balance. Find Rune or check your command!");
//					}
//
//				} else {
//					targetPlayer.sendMessageToPlayer(ChatColor.GOLD
//							+ "[RunicCarnival] You don't have any trophies! Win some games to get more.");
//				}

			// ////////////
			// /////////////////////////////////////////////////////
			// give trophy command; gives specified # trophies to player
			// /rptokens givetrophy PLAYER COUNT
		} else if (args.length == 3 && args[0].equals("givetrophy")) {
			// TODO: fix givetrophy
//				RunicPlayerBukkit targetPlayer = new RunicPlayerBukkit(args[1]);
//
//				if (Integer.parseInt(args[2]) > 0) {
//					targetPlayer.givePlayerItemData(Integer.parseInt(args[2]), 371, 99, 2,
//							ChatColor.GOLD + "Runic Carnival Trophy",
//							ChatColor.GRAY + "Turn these in at the Prize Center",
//							ChatColor.GRAY + "in Runic Carnival for tokens", "");
//
//					targetPlayer.sendMessageToPlayer(ChatColor.GOLD + "[RunicCarnival] You have been awarded "
//							+ ChatColor.GREEN + args[2] + ChatColor.GOLD + " trophies!");
//				} else {
//					Bukkit.getLogger().log(Level.INFO,
//							"Failed to give trophy to player, bad command usage? Tried /rptokens " + args.toString());
//					Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
//							"sc RunicCarnival failed to award trophy. Tried /rptokens " + args.toString());
//				}


			// ////////////
			// /////////////////////////////////////////////////////
			// mazewin command, increments the player's running tab of maze
			// completions
			// /rptokens mazewin [hedge/ice] PLAYER
		} else if (args.length == 3 && args[0].equals("mazewin")) {
			Player p = Bukkit.getPlayer(args[1]);

			RunicParadise.playerProfiles.get(p.getUniqueId()).addMazeCompletion(Integer.parseInt(args[2]));

		} else if (sender instanceof Player) {
			RunicPlayerBukkit senderPlayer = new RunicPlayerBukkit((Player) sender);
			senderPlayer.sendMessageToPlayer(ChatColor.DARK_RED + "Your usage of rptokens seems wrong. :(");
		}
		return true;
	}

	private static void rpRewardsCommand(CommandSender sender, String[] args) {
		// /rprewards type player amount

		boolean rewardError = false;
		int amount = Integer.parseInt(args[2]);

		if (args.length == 3 && amount > 0) {
			switch (args[0]) {
				case "souls":
					RunicParadise.playerProfiles.get(Bukkit.getOfflinePlayer(args[1]).getUniqueId())
							.grantCurrency("Souls", amount);

					break;
				case "tokens":
					RunicParadise.playerProfiles.get(Bukkit.getOfflinePlayer(args[1]).getUniqueId())
							.grantCurrency("Tokens", amount);

					break;
				case "karma":
					RunicParadise.playerProfiles.get(Bukkit.getOfflinePlayer(args[1]).getUniqueId())
							.grantCurrency("Karma", amount);

					break;
				default:
					rewardError = true;
					break;
			}
		} else {
			rewardError = true;
		}

		if (rewardError) {
			// Bad command usage
			Bukkit.getLogger().log(Level.SEVERE,
					"Bad usage of rprewards command. Use /rprewards type player amount ... type = souls/tokens/karma");
		}
	}

	private static void rpCratesCommand(CommandSender sender, String[] args) {
		if (args.length != 2) {
			return;
		}
		switch (args[0]) {
			case "1":
				RunicParadise.playerProfiles.get(Bukkit.getOfflinePlayer(args[1]).getUniqueId())
						.grantCurrency("Souls", 1);
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "eco give " + args[1] + " 400");
				break;
			case "2":
				RunicParadise.playerProfiles.get(Bukkit.getOfflinePlayer(args[1]).getUniqueId())
						.grantCurrency("Souls", 2);
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "eco give " + args[1] + " 600");
				break;
			case "3":
				RunicParadise.playerProfiles.get(Bukkit.getOfflinePlayer(args[1]).getUniqueId())
						.grantCurrency("Souls", 1);
				RunicParadise.playerProfiles.get(Bukkit.getOfflinePlayer(args[1]).getUniqueId())
						.grantCurrency("Karma", 2);
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "eco give " + args[1] + " 100");
				break;
			case "4":
				RunicParadise.playerProfiles.get(Bukkit.getOfflinePlayer(args[1]).getUniqueId())
						.grantCurrency("Souls", 1);
				RunicParadise.playerProfiles.get(Bukkit.getOfflinePlayer(args[1]).getUniqueId())
						.grantCurrency("Karma", 3);
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "eco give " + args[1] + " 250");
				break;
			case "5":
				RunicParadise.playerProfiles.get(Bukkit.getOfflinePlayer(args[1]).getUniqueId())
						.grantCurrency("Souls", 1);
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "eco give " + args[1] + " 900");
				break;
		}
	}

	private boolean rpJobsCommand(CommandSender sender, String[] args) {
		// Master a tier1 job
		if (args[0].equals("master") && args.length == 3) {
			Player q = Bukkit.getPlayer(args[2]);

			// MINER
			if(args[1].equalsIgnoreCase("Miner") && q.hasPermission("rp.jobs.max.Miner") && !q.hasPermission("rp.jobs.mastery.Miner")){
				RunicMessaging.sendMessage(q, RunicFormat.SYSTEM, "Congrats! You've mastered the Miner job!");
				RunicParadise.perms.playerAdd(q, "rp.jobs.mastery.Miner");
			} else if (args[1].equalsIgnoreCase("Miner") && q.hasPermission("rp.jobs.max.Miner")) {
				RunicMessaging.sendMessage(q, RunicFormat.ERROR, "Oops! You already mastered the Miner job.");
			} else if (args[1].equalsIgnoreCase("Miner")) {
				RunicMessaging.sendMessage(q, RunicFormat.ERROR, "You need to be job level 30 and have the Miner job active to master it.");
			}

			// CHEF
			if(args[1].equalsIgnoreCase("Chef") && q.hasPermission("rp.jobs.max.Chef") && !q.hasPermission("rp.jobs.mastery.Chef")){
				RunicMessaging.sendMessage(q, RunicFormat.SYSTEM, "Congrats! You've mastered the Chef job!");
				RunicParadise.perms.playerAdd(q, "rp.jobs.mastery.Chef");
			} else if (args[1].equalsIgnoreCase("Chef") && q.hasPermission("rp.jobs.max.Chef")) {
				RunicMessaging.sendMessage(q, RunicFormat.ERROR, "Oops! You already mastered the Chef job.");
			} else if (args[1].equalsIgnoreCase("Chef")) {
				RunicMessaging.sendMessage(q, RunicFormat.ERROR, "You need to be job level 30 and have the Chef job active to master it.");
			}

			// BLACKSMITH
			if(args[1].equalsIgnoreCase("Blacksmith") && q.hasPermission("rp.jobs.max.Blacksmith") && !q.hasPermission("rp.jobs.mastery.Blacksmith")){
				RunicMessaging.sendMessage(q, RunicFormat.SYSTEM, "Congrats! You've mastered the Blacksmith job!");
				RunicParadise.perms.playerAdd(q, "rp.jobs.mastery.Blacksmith");
			} else if (args[1].equalsIgnoreCase("Blacksmith") && q.hasPermission("rp.jobs.max.Blacksmith")) {
				RunicMessaging.sendMessage(q, RunicFormat.ERROR, "Oops! You already mastered the Blacksmith job.");
			} else if (args[1].equalsIgnoreCase("Blacksmith")) {
				RunicMessaging.sendMessage(q, RunicFormat.ERROR, "You need to be job level 30 and have the Blacksmith job active to master it.");
			}

			// WOODSMAN
			if(args[1].equalsIgnoreCase("Woodsman") && q.hasPermission("rp.jobs.max.Woodsman") && !q.hasPermission("rp.jobs.mastery.Woodsman")){
				RunicMessaging.sendMessage(q, RunicFormat.SYSTEM, "Congrats! You've mastered the Woodsman job!");
				RunicParadise.perms.playerAdd(q, "rp.jobs.mastery.Woodsman");
			} else if (args[1].equalsIgnoreCase("Woodsman") && q.hasPermission("rp.jobs.max.Woodsman")) {
				RunicMessaging.sendMessage(q, RunicFormat.ERROR, "Oops! You already mastered the Woodsman job.");
			} else if (args[1].equalsIgnoreCase("Woodsman")) {
				RunicMessaging.sendMessage(q, RunicFormat.ERROR, "You need to be job level 30 and have the Woodsman job active to master it.");
			}

			// WIZARD
			if(args[1].equalsIgnoreCase("Wizard") && q.hasPermission("rp.jobs.max.Wizard") && !q.hasPermission("rp.jobs.mastery.Wizard")){
				RunicMessaging.sendMessage(q, RunicFormat.SYSTEM, "Congrats! You've mastered the Wizard job!");
				RunicParadise.perms.playerAdd(q, "rp.jobs.mastery.Wizard");
			} else if (args[1].equalsIgnoreCase("Wizard") && q.hasPermission("rp.jobs.max.Wizard")) {
				RunicMessaging.sendMessage(q, RunicFormat.ERROR, "Oops! You already mastered the Wizard job.");
			} else if (args[1].equalsIgnoreCase("Wizard")) {
				RunicMessaging.sendMessage(q, RunicFormat.ERROR, "You need to be job level 30 and have the Wizard job active to master it.");
			}

			// TAMER
			if(args[1].equalsIgnoreCase("Tamer") && q.hasPermission("rp.jobs.max.Tamer") && !q.hasPermission("rp.jobs.mastery.Tamer")){
				RunicMessaging.sendMessage(q, RunicFormat.SYSTEM, "Congrats! You've mastered the Tamer job!");
				RunicParadise.perms.playerAdd(q, "rp.jobs.mastery.Tamer");
			} else if (args[1].equalsIgnoreCase("Tamer") && q.hasPermission("rp.jobs.max.Tamer")) {
				RunicMessaging.sendMessage(q, RunicFormat.ERROR, "Oops! You already mastered the Tamer job.");
			} else if (args[1].equalsIgnoreCase("Tamer")) {
				RunicMessaging.sendMessage(q, RunicFormat.ERROR, "You need to be job level 30 and have the Tamer job active to master it.");
			}

			// SCIENTIST
			if(args[1].equalsIgnoreCase("Scientist") && q.hasPermission("rp.jobs.max.Scientist") && !q.hasPermission("rp.jobs.mastery.Scientist")){
				RunicMessaging.sendMessage(q, RunicFormat.SYSTEM, "Congrats! You've mastered the Scientist job!");
				RunicParadise.perms.playerAdd(q, "rp.jobs.mastery.Scientist");
			} else if (args[1].equalsIgnoreCase("Ranger") && q.hasPermission("rp.jobs.max.Scientist")) {
				RunicMessaging.sendMessage(q, RunicFormat.ERROR, "Oops! You already mastered the Scientist job.");
			} else if (args[1].equalsIgnoreCase("Ranger")) {
				RunicMessaging.sendMessage(q, RunicFormat.ERROR, "You need to be job level 30 and have the Scientist job active to master it.");
			}

			// RANCHER
			if(args[1].equalsIgnoreCase("Rancher") && q.hasPermission("rp.jobs.max.Rancher") && !q.hasPermission("rp.jobs.mastery.Rancher")){
				RunicMessaging.sendMessage(q, RunicFormat.SYSTEM, "Congrats! You've mastered the Rancher job!");
				RunicParadise.perms.playerAdd(q, "rp.jobs.mastery.Rancher");
			} else if (args[1].equalsIgnoreCase("Rancher") && q.hasPermission("rp.jobs.max.Rancher")) {
				RunicMessaging.sendMessage(q, RunicFormat.ERROR, "Oops! You already mastered the Rancher job.");
			} else if (args[1].equalsIgnoreCase("Rancher")) {
				RunicMessaging.sendMessage(q, RunicFormat.ERROR, "You need to be job level 30 and have the Rancher job active to master it.");
			}

			// RANGER
			if(args[1].equalsIgnoreCase("Ranger") && q.hasPermission("rp.jobs.max.ranger") && !q.hasPermission("rp.jobs.mastery.ranger")){
				RunicMessaging.sendMessage(q, RunicFormat.SYSTEM, "Congrats! You've mastered the Ranger job!");
				RunicParadise.perms.playerAdd(q, "rp.jobs.mastery.ranger");
			} else if (args[1].equalsIgnoreCase("Ranger") && q.hasPermission("rp.jobs.max.ranger")) {
				RunicMessaging.sendMessage(q, RunicFormat.ERROR, "Oops! You already mastered the Ranger job.");
			} else if (args[1].equalsIgnoreCase("Ranger")) {
				RunicMessaging.sendMessage(q, RunicFormat.ERROR, "You need to be job level 30 and have the Ranger job active to master it.");
			}

			// FORGEMASTER
			if(args[1].equalsIgnoreCase("Forgemaster") && q.hasPermission("rp.jobs.max.Forgemaster") && !q.hasPermission("rp.jobs.mastery.Forgemaster")){
				RunicMessaging.sendMessage(q, RunicFormat.SYSTEM, "Congrats! You've mastered the Forgemaster job!");
				RunicParadise.perms.playerAdd(q, "rp.jobs.mastery.Forgemaster");
			} else if (args[1].equalsIgnoreCase("Forgemaster") && q.hasPermission("rp.jobs.max.Forgemaster")) {
				RunicMessaging.sendMessage(q, RunicFormat.ERROR, "Oops! You already mastered the Forgemaster job.");
			} else if (args[1].equalsIgnoreCase("Forgemaster")){
				RunicMessaging.sendMessage(q, RunicFormat.ERROR, "You need to be job level 30 and have the Forgemaster job active to master it.");
			}

			// BIOLOGIST
			if(args[1].equalsIgnoreCase("Biologist") && q.hasPermission("rp.jobs.max.Biologist") && !q.hasPermission("rp.jobs.mastery.Biologist")){
				RunicMessaging.sendMessage(q, RunicFormat.SYSTEM, "Congrats! You've mastered the Biologist job!");
				RunicParadise.perms.playerAdd(q, "rp.jobs.mastery.Biologist");
			} else if (args[1].equalsIgnoreCase("Biologist") && q.hasPermission("rp.jobs.max.Biologist")) {
				RunicMessaging.sendMessage(q, RunicFormat.ERROR, "Oops! You already mastered the Biologist job.");
			} else if (args[1].equalsIgnoreCase("Biologist")) {
				RunicMessaging.sendMessage(q, RunicFormat.ERROR, "You need to be job level 30 and have the Biologist job active to master it.");
			}

			// ALCHEMIST
			if(args[1].equalsIgnoreCase("Alchemist") && q.hasPermission("rp.jobs.max.Alchemist") && !q.hasPermission("rp.jobs.mastery.Alchemist")){
				RunicMessaging.sendMessage(q, RunicFormat.SYSTEM, "Congrats! You've mastered the Alchemist job!");
				RunicParadise.perms.playerAdd(q, "rp.jobs.mastery.Alchemist");
			} else if (args[1].equalsIgnoreCase("Alchemist") && q.hasPermission("rp.jobs.max.Alchemist")) {
				RunicMessaging.sendMessage(q, RunicFormat.ERROR, "Oops! You already mastered the Alchemist job.");
			} else if (args[1].equalsIgnoreCase("Alchemist")) {
				RunicMessaging.sendMessage(q, RunicFormat.ERROR, "You need to be job level 30 and have the Alchemist job active to master it.");
			}


			// NOMAD
			if(args[1].equalsIgnoreCase("Nomad") && q.hasPermission("rp.jobs.max.Nomad") && !q.hasPermission("rp.jobs.mastery.Nomad")){
				RunicMessaging.sendMessage(q, RunicFormat.SYSTEM, "Congrats! You've mastered the Nomad job!");
				RunicParadise.perms.playerAdd(q, "rp.jobs.mastery.Nomad");
			} else if (args[1].equalsIgnoreCase("Nomad") && q.hasPermission("rp.jobs.max.Nomad")) {
				RunicMessaging.sendMessage(q, RunicFormat.ERROR, "Oops! You already mastered the Nomad job.");
			} else if (args[1].equalsIgnoreCase("Nomad")) {
				RunicMessaging.sendMessage(q, RunicFormat.ERROR, "You need to be job level 30 and have the Nomad job active to master it.");
			}

			// GEOMANCER
			if(args[1].equalsIgnoreCase("Geomancer") && q.hasPermission("rp.jobs.max.Geomancer") && !q.hasPermission("rp.jobs.mastery.Geomancer")){
				RunicMessaging.sendMessage(q, RunicFormat.SYSTEM, "Congrats! You've mastered the Geomancer job!");
				RunicParadise.perms.playerAdd(q, "rp.jobs.mastery.Geomancer");
			} else if (args[1].equalsIgnoreCase("Geomancer") && q.hasPermission("rp.jobs.max.Geomancer")) {
				RunicMessaging.sendMessage(q, RunicFormat.ERROR, "Oops! You already mastered the Geomancer job.");
			} else if (args[1].equalsIgnoreCase("Geomancer")) {
				RunicMessaging.sendMessage(q, RunicFormat.ERROR, "You need to be job level 30 and have the Geomancer job active to master it.");
			}

			// CONJURER
			if(args[1].equalsIgnoreCase("Conjurer") && q.hasPermission("rp.jobs.max.Conjurer") && !q.hasPermission("rp.jobs.mastery.Conjurer")){
				RunicMessaging.sendMessage(q, RunicFormat.SYSTEM, "Congrats! You've mastered the Conjurer job!");
				RunicParadise.perms.playerAdd(q, "rp.jobs.mastery.Conjurer");
			} else if (args[1].equalsIgnoreCase("Conjurer") && q.hasPermission("rp.jobs.max.Conjurer")) {
				RunicMessaging.sendMessage(q, RunicFormat.ERROR, "Oops! You already mastered the Conjurer job.");
			} else if (args[1].equalsIgnoreCase("Conjurer")) {
				RunicMessaging.sendMessage(q, RunicFormat.ERROR, "You need to be job level 30 and have the Conjurer job active to master it.");
			}

			// DRUID
			if(args[1].equalsIgnoreCase("Druid") && q.hasPermission("rp.jobs.max.Druid") && !q.hasPermission("rp.jobs.mastery.Druid")){
				RunicMessaging.sendMessage(q, RunicFormat.SYSTEM, "Congrats! You've mastered the Druid job!");
				RunicParadise.perms.playerAdd(q, "rp.jobs.mastery.Druid");
			} else if (args[1].equalsIgnoreCase("Druid") && q.hasPermission("rp.jobs.max.Druid")) {
				RunicMessaging.sendMessage(q, RunicFormat.ERROR, "Oops! You already mastered the Druid job.");
			} else if (args[1].equalsIgnoreCase("Druid")) {
				RunicMessaging.sendMessage(q, RunicFormat.ERROR, "You need to be job level 30 and have the Druid job active to master it.");
			}

			// ENGINEER
			if(args[1].equalsIgnoreCase("Engineer") && q.hasPermission("rp.jobs.max.Engineer") && !q.hasPermission("rp.jobs.mastery.Engineer")){
				RunicMessaging.sendMessage(q, RunicFormat.SYSTEM, "Congrats! You've mastered the Engineer job!");
				RunicParadise.perms.playerAdd(q, "rp.jobs.mastery.Engineer");
			} else if (args[1].equalsIgnoreCase("Engineer") && q.hasPermission("rp.jobs.max.Engineer")) {
				RunicMessaging.sendMessage(q, RunicFormat.ERROR, "Oops! You already mastered the Engineer job.");
			} else if (args[1].equalsIgnoreCase("Engineer")) {
				RunicMessaging.sendMessage(q, RunicFormat.ERROR, "You need to be job level 30 and have the Engineer job active to master it.");
			}

			// BEASTMASTER
			if(args[1].equalsIgnoreCase("Beastmaster") && q.hasPermission("rp.jobs.max.Beastmaster") && !q.hasPermission("rp.jobs.mastery.Beastmaster")){
				RunicMessaging.sendMessage(q, RunicFormat.SYSTEM, "Congrats! You've mastered the Beastmaster job!");
				RunicParadise.perms.playerAdd(q, "rp.jobs.mastery.Beastmaster");
			} else if (args[1].equalsIgnoreCase("Beastmaster") && q.hasPermission("rp.jobs.max.Beastmaster")) {
				RunicMessaging.sendMessage(q, RunicFormat.ERROR, "Oops! You already mastered the Beastmaster job.");
			} else if (args[1].equalsIgnoreCase("Beastmaster")) {
				RunicMessaging.sendMessage(q, RunicFormat.ERROR, "You need to be job level 30 and have the Beastmaster job active to master it.");
			}

			// SORCERER
			if(args[1].equalsIgnoreCase("Sorcerer") && q.hasPermission("rp.jobs.max.Sorcerer") && !q.hasPermission("rp.jobs.mastery.Sorcerer")){
				RunicMessaging.sendMessage(q, RunicFormat.SYSTEM, "Congrats! You've mastered the Sorcerer job!");
				RunicParadise.perms.playerAdd(q, "rp.jobs.mastery.Sorcerer");
			} else if (args[1].equalsIgnoreCase("Sorcerer") && q.hasPermission("rp.jobs.max.Sorcerer")) {
				RunicMessaging.sendMessage(q, RunicFormat.ERROR, "Oops! You already mastered the Sorcerer job.");
			} else if (args[1].equalsIgnoreCase("Sorcerer")) {
				RunicMessaging.sendMessage(q, RunicFormat.ERROR, "You need to be job level 30 and have the Sorcerer job active to master it.");
			}

			// GENETICIST
			if(args[1].equalsIgnoreCase("Geneticist") && q.hasPermission("rp.jobs.max.Geneticist") && !q.hasPermission("rp.jobs.mastery.Geneticist")){
				RunicMessaging.sendMessage(q, RunicFormat.SYSTEM, "Congrats! You've mastered the Geneticist job!");
				RunicParadise.perms.playerAdd(q, "rp.jobs.mastery.Geneticist");
			} else if (args[1].equalsIgnoreCase("Geneticist") && q.hasPermission("rp.jobs.max.Geneticist")) {
				RunicMessaging.sendMessage(q, RunicFormat.ERROR, "Oops! You already mastered the Geneticist job.");
			} else if (args[1].equalsIgnoreCase("Geneticist")) {
				RunicMessaging.sendMessage(q, RunicFormat.ERROR, "You need to be job level 30 and have the Geneticist job active to master it.");
			}

			// ARTIFICER
			if(args[1].equalsIgnoreCase("Artificer") && q.hasPermission("rp.jobs.max.Artificer") && !q.hasPermission("rp.jobs.mastery.Artificer")){
				RunicMessaging.sendMessage(q, RunicFormat.SYSTEM, "Congrats! You've mastered the Artificer job!");
				RunicParadise.perms.playerAdd(q, "rp.jobs.mastery.Artificer");
			} else if (args[1].equalsIgnoreCase("Artificer") && q.hasPermission("rp.jobs.max.Artificer")) {
				RunicMessaging.sendMessage(q, RunicFormat.ERROR, "Oops! You already mastered the Artificer job.");
			} else if (args[1].equalsIgnoreCase("Artificer")) {
				RunicMessaging.sendMessage(q, RunicFormat.ERROR, "You need to be job level 30 and have the Artificer job active to master it.");
			}

			// CRAFTSMAN
			if(args[1].equalsIgnoreCase("Craftsman") && q.hasPermission("rp.jobs.max.Craftsman") && !q.hasPermission("rp.jobs.mastery.Craftsman")){
				RunicMessaging.sendMessage(q, RunicFormat.SYSTEM, "Congrats! You've mastered the Craftsman job!");
				RunicParadise.perms.playerAdd(q, "rp.jobs.mastery.Craftsman");
			} else if (args[1].equalsIgnoreCase("Craftsman") && q.hasPermission("rp.jobs.max.Craftsman")) {
				RunicMessaging.sendMessage(q, RunicFormat.ERROR, "Oops! You already mastered the Craftsman job.");
			} else if (args[1].equalsIgnoreCase("Craftsman")) {
				RunicMessaging.sendMessage(q, RunicFormat.ERROR, "You need to be job level 30 and have the Craftsman job active to master it.");
			}

			//  SEAFARER
			if(args[1].equalsIgnoreCase("Seafarer") && q.hasPermission("rp.jobs.max.Seafarer") && !q.hasPermission("rp.jobs.mastery.Seafarer")){
				RunicMessaging.sendMessage(q, RunicFormat.SYSTEM, "Congrats! You've mastered the Seafarer job!");
				RunicParadise.perms.playerAdd(q, "rp.jobs.mastery.Seafarer");
			} else if (args[1].equalsIgnoreCase("Seafarer") && q.hasPermission("rp.jobs.max.Seafarer")) {
				RunicMessaging.sendMessage(q, RunicFormat.ERROR, "Oops! You already mastered the Seafarer job.");
			} else if (args[1].equalsIgnoreCase("Seafarer")) {
				RunicMessaging.sendMessage(q, RunicFormat.ERROR, "You need to be job level 30 and have the Seafarer job active to master it.");
			}

		} else if (args[0].equals("qualify") && args.length == 2 && !(sender instanceof Player)) {
			// Qualify for an upper tier job
			Player p = Bukkit.getPlayer(args[1]);
			String jobTally = "";

			RunicMessaging.sendMessage(p, RunicFormat.EMPTY, ChatColor.BLUE + "*** Tier 2 Jobs ***");

			// RANGER
				if (p.hasPermission("rp.jobs.max.woodsman") && p.hasPermission("rp.jobs.max.rancher")
						&& !p.hasPermission("jobs.join.RANGER")) {
					RunicParadise.perms.playerAdd(p, "jobs.join.RANGER");
					jobTally += ChatColor.GREEN + "RANGER ";
				}
				else if (p.hasPermission("jobs.join.RANGER")) {
					jobTally += ChatColor.GREEN + "RANGER ";
				}
				else {
					jobTally += ChatColor.DARK_RED + "RANGER ";
				}
			// FORGEMASTER
				if (p.hasPermission("rp.jobs.max.blacksmith") && p.hasPermission("rp.jobs.max.miner")
						&& !p.hasPermission("jobs.join.FORGEMASTER")) {
					RunicParadise.perms.playerAdd(p, "jobs.join.FORGEMASTER");
					jobTally += ChatColor.GREEN + "FORGEMASTER ";
				}
				else if (p.hasPermission("jobs.join.FORGEMASTER")) {
					jobTally += ChatColor.GREEN + "FORGEMASTER ";
				}
				else {
					jobTally += ChatColor.DARK_RED + "FORGEMASTER ";
				}
			// BIOLOGIST
				if (p.hasPermission("rp.jobs.max.scientist") && p.hasPermission("rp.jobs.max.rancher")
						&& !p.hasPermission("jobs.join.BIOLOGIST")) {
					RunicParadise.perms.playerAdd(p, "jobs.join.BIOLOGIST");
					jobTally += ChatColor.GREEN + "BIOLOGIST ";
				}
				else if (p.hasPermission("jobs.join.BIOLOGIST")) {
					jobTally += ChatColor.GREEN + "BIOLOGIST ";
				}
				else {
					jobTally += ChatColor.DARK_RED + "BIOLOGIST ";
				}
			// ALCHEMIST
				if (p.hasPermission("rp.jobs.max.wizard") && p.hasPermission("rp.jobs.max.chef")
						&& !p.hasPermission("jobs.join.ALCHEMIST")) {
					RunicParadise.perms.playerAdd(p, "jobs.join.ALCHEMIST");
					jobTally += ChatColor.GREEN + "ALCHEMIST ";
				}
				else if (p.hasPermission("jobs.join.ALCHEMIST")) {
					jobTally += ChatColor.GREEN + "ALCHEMIST ";
				}
				else {
					jobTally += ChatColor.DARK_RED + "ALCHEMIST ";
				}
			// NOMAD
				if (p.hasPermission("rp.jobs.max.chef") && p.hasPermission("rp.jobs.max.rancher")
						&& !p.hasPermission("jobs.join.NOMAD")) {
					RunicParadise.perms.playerAdd(p, "jobs.join.NOMAD");
					jobTally += ChatColor.GREEN + "NOMAD ";
				}
				else if (p.hasPermission("jobs.join.NOMAD")) {
					jobTally += ChatColor.GREEN + "NOMAD ";
				}
				else {
					jobTally += ChatColor.DARK_RED + "NOMAD ";
				}
			// GEOMANCER
				if (p.hasPermission("rp.jobs.max.wizard") && p.hasPermission("rp.jobs.max.miner")
						&& !p.hasPermission("jobs.join.GEOMANCER")) {
					RunicParadise.perms.playerAdd(p, "jobs.join.GEOMANCER");
					jobTally += ChatColor.GREEN + "GEOMANCER ";
				}
				else if (p.hasPermission("jobs.join.GEOMANCER")) {
					jobTally += ChatColor.GREEN + "GEOMANCER ";
				}
				else {
					jobTally += ChatColor.DARK_RED + "GEOMANCER ";
				}
			// CONJURER
				if (p.hasPermission("rp.jobs.max.wizard") && p.hasPermission("rp.jobs.max.blacksmith")
						&& !p.hasPermission("jobs.join.CONJURER")) {
					RunicParadise.perms.playerAdd(p, "jobs.join.CONJURER");
					jobTally += ChatColor.GREEN + "CONJURER ";
				}
				else if (p.hasPermission("jobs.join.CONJURER")) {
					jobTally += ChatColor.GREEN + "CONJURER ";
				}
				else {
					jobTally += ChatColor.DARK_RED + "CONJURER ";
				}
			// DRUID
				if (p.hasPermission("rp.jobs.max.wizard") && p.hasPermission("rp.jobs.max.woodsman")
						&& !p.hasPermission("jobs.join.DRUID")) {
					RunicParadise.perms.playerAdd(p, "jobs.join.DRUID");
					jobTally += ChatColor.GREEN + "DRUID ";
				}
				else if (p.hasPermission("jobs.join.DRUID")) {
					jobTally += ChatColor.GREEN + "DRUID ";
				}
				else {
					jobTally += ChatColor.DARK_RED + "DRUID ";
				}
			// ENGINEER
				if (p.hasPermission("rp.jobs.max.scientist") && p.hasPermission("rp.jobs.max.miner")
						&& !p.hasPermission("jobs.join.ENGINEER")) {
					RunicParadise.perms.playerAdd(p, "jobs.join.ENGINEER");
					jobTally += ChatColor.GREEN + "ENGINEER ";
				}
				else if (p.hasPermission("jobs.join.ENGINEER")) {
					jobTally += ChatColor.GREEN + "ENGINEER ";
				}
				else {
					jobTally += ChatColor.DARK_RED + "ENGINEER ";
				}

			RunicMessaging.sendMessage(p, RunicFormat.EMPTY	, jobTally);
			jobTally = "";

			RunicMessaging.sendMessage(p, RunicFormat.EMPTY, ChatColor.YELLOW + "*** Tier 3 Jobs ***");

			// BEASTMASTER
			if (p.hasPermission("rp.jobs.max.druid") && p.hasPermission("rp.jobs.max.tamer") && p.hasPermission("rp.jobs.max.nomad") && !p.hasPermission("jobs.join.beastmaster")) {
				RunicParadise.perms.playerAdd(p, "jobs.join.beastmaster");
				jobTally += ChatColor.GREEN + "BEASTMASTER ";
			}
			else if (p.hasPermission("jobs.join.beastmaster")) {
				jobTally += ChatColor.GREEN + "BEASTMASTER ";
			}
			else {
				jobTally += ChatColor.DARK_RED + "BEASTMASTER ";
			}
			// SORCERER
			if (p.hasPermission("rp.jobs.max.alchemist") && p.hasPermission("rp.jobs.max.geomancer") && p.hasPermission("rp.jobs.max.conjurer")
					&& !p.hasPermission("jobs.join.sorcerer")) {
				RunicParadise.perms.playerAdd(p, "jobs.join.sorcerer");
				jobTally += ChatColor.GREEN + "SORCERER ";
			}
			else if (p.hasPermission("jobs.join.sorcerer")) {
				jobTally += ChatColor.GREEN + "SORCERER ";
			}
			else {
				jobTally += ChatColor.DARK_RED + "SORCERER ";
			}
			// GENETICIST
			if (p.hasPermission("rp.jobs.max.ranger") && p.hasPermission("rp.jobs.max.nomad") && p.hasPermission("rp.jobs.max.biologist")
					&& !p.hasPermission("jobs.join.GENETICIST")) {
				RunicParadise.perms.playerAdd(p, "jobs.join.GENETICIST");
				jobTally += ChatColor.GREEN + "GENETICIST ";
			}
			else if (p.hasPermission("jobs.join.GENETICIST")) {
				jobTally += ChatColor.GREEN + "GENETICIST ";
			}
			else {
				jobTally += ChatColor.DARK_RED + "GENETICIST ";
			}
			// ARTIFICER
			if (p.hasPermission("rp.jobs.max.engineer") && p.hasPermission("rp.jobs.max.forgemaster") && p.hasPermission("rp.jobs.max.geomancer")
					&& !p.hasPermission("jobs.join.ARTIFICER")) {
				RunicParadise.perms.playerAdd(p, "jobs.join.ARTIFICER");
				jobTally += ChatColor.GREEN + "ARTIFICER ";
			}
			else if (p.hasPermission("jobs.join.ARTIFICER")) {
				jobTally += ChatColor.GREEN + "ARTIFICER ";
			}
			else {
				jobTally += ChatColor.DARK_RED + "ARTIFICER ";
			}

			RunicMessaging.sendMessage(p, RunicFormat.EMPTY	, jobTally);
			jobTally = "";

			RunicMessaging.sendMessage(p, RunicFormat.EMPTY, ChatColor.DARK_RED + "*** Tier 4 Jobs ***");

			// CRAFTSMAN
				if (p.hasPermission("rp.jobs.max.artificer") && p.hasPermission("rp.jobs.max.sorcerer") && !p.hasPermission("jobs.join.craftsman")) {
					RunicParadise.perms.playerAdd(p, "jobs.join.CRAFTSMAN");
					jobTally += ChatColor.GREEN + "CRAFTSMAN ";
				}
				else if (p.hasPermission("jobs.join.CRAFTSMAN")) {
					jobTally += ChatColor.GREEN + "CRAFTSMAN ";
				}
				else {
					jobTally += ChatColor.DARK_RED + "CRAFTSMAN ";
				}
			//  SEAFARER
				if (p.hasPermission("rp.jobs.max.geneticist") && p.hasPermission("rp.jobs.max.beastmaster") && !p.hasPermission("jobs.join.seafarer")) {
					RunicParadise.perms.playerAdd(p, "jobs.join.seafarer");
					jobTally += ChatColor.GREEN + "SEAFARER ";
				}
				else if (p.hasPermission("jobs.join.SEAFARER")) {
					jobTally += ChatColor.GREEN + "SEAFARER ";
				}
				else {
					jobTally += ChatColor.DARK_RED + "SEAFARER ";
				}

			RunicMessaging.sendMessage(p, RunicFormat.EMPTY	, jobTally);
			jobTally = "";


		} else if (args[0].equals("maintenance") && args.length == 1 && !(sender instanceof Player)) {

			RunicPlayerBukkit.maintainJobTable();

		} else if (sender instanceof Player) {

			sender.sendMessage("This command must be run from commandblock or console");
			sender.sendMessage("Format: /rpjobs master playername");
		}
		return true;
	}

	private void faithCommand(CommandSender sender, String[] args) {
    	MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
		if (args.length == 0) {
			sender.sendMessage(ChatColor.GRAY + "[" + ChatColor.BLUE + "Runic" + ChatColor.DARK_AQUA + "Faith"
					+ ChatColor.GRAY + "] " + ChatColor.BLUE + "Available commands:");

			// list staff commands to staff
			if (sender.hasPermission("rp.faith.staff")) {
				sender.sendMessage(ChatColor.AQUA + "/faith reset" + ChatColor.GRAY
						+ " Refresh player faith table, eyes and books");
				sender.sendMessage(
						ChatColor.AQUA + "/faith enable runelynx Sun" + ChatColor.GRAY + " Enable faith");
				sender.sendMessage(ChatColor.AQUA + "/faith listmap" + ChatColor.GRAY + " ???");
				sender.sendMessage(
						ChatColor.AQUA + "/faith setlevel runelynx Sun 5" + ChatColor.GRAY + " Set faith level");
				sender.sendMessage(
						ChatColor.AQUA + "/faith skillup runelynx Sun" + ChatColor.GRAY + " Skill-up faith");
				sender.sendMessage(ChatColor.AQUA + "/faith report" + ChatColor.GRAY + " Reporting");
			}

			// list general commands to everyone
			sender.sendMessage(ChatColor.AQUA + "/faith menu" + ChatColor.GRAY + " Open faith chooser menu");
			sender.sendMessage(ChatColor.AQUA + "/faith stats" + ChatColor.GRAY + " List faith stats");
			sender.sendMessage(
					ChatColor.AQUA + "/faith stats [name]" + ChatColor.GRAY + " List faith stats for someone");
			sender.sendMessage(ChatColor.AQUA + "/faith powers" + ChatColor.GRAY + " List your faith's powers");
			sender.sendMessage(ChatColor.AQUA + "/faith allpowers" + ChatColor.GRAY + " List all powers");
			sender.sendMessage(ChatColor.AQUA + "/faith video" + ChatColor.GRAY + " Youtube video about faiths");
			sender.sendMessage(ChatColor.AQUA + "/faith help" + ChatColor.GRAY + " Get help");

		} else if (args[0].toLowerCase().equals("checkaccess")) {
			if (args[2].equals("nether") || args[2].equals("aether")) {
				if (new RunicPlayerBukkit(Bukkit.getPlayer(args[1])).getFaithPowerLevel() >= 600) {

					TitleAPI.sendTitle(Bukkit.getPlayer(args[1]), 2, 8, 2, ChatColor.GREEN + "",
							ChatColor.GREEN + "You have access to this faith!");

					RunicParadise.perms.playerAdd(Bukkit.getPlayer(args[1]), "rp.faith.nether");
					RunicParadise.perms.playerAdd(Bukkit.getPlayer(args[1]), "rp.faith.aether");
				} else {
					TitleAPI.sendTitle(Bukkit.getPlayer(args[1]), 2, 3, 2, ChatColor.RED + "",
							ChatColor.RED + "You do not have access to this faith! Need 600 faith levels but have "
									+ new RunicPlayerBukkit(Bukkit.getPlayer(args[1])).getFaithPowerLevel() + ".");
				}
			} else if (args[2].equals("air") || args[2].equals("earth")) {
				if (new RunicPlayerBukkit(Bukkit.getPlayer(args[1])).getFaithPowerLevel() >= 300) {

					TitleAPI.sendTitle(Bukkit.getPlayer(args[1]), 2, 8, 2, ChatColor.GREEN + "",
							ChatColor.GREEN + "You have access to this faith!");
					RunicParadise.perms.playerAdd(Bukkit.getPlayer(args[1]), "rp.faith.air");
					RunicParadise.perms.playerAdd(Bukkit.getPlayer(args[1]), "rp.faith.earth");

				} else {
					TitleAPI.sendTitle(Bukkit.getPlayer(args[1]), 2, 3, 2, ChatColor.RED + "",
							ChatColor.RED + "You do not have access to this faith! Need 300 faith levels but have "
									+ new RunicPlayerBukkit(Bukkit.getPlayer(args[1])).getFaithPowerLevel() + ".");
				}
			} else if (args[2].equals("nature") || args[2].equals("tech")) {
				if (new RunicPlayerBukkit(Bukkit.getPlayer(args[1])).getFaithPowerLevel() >= 900) {

					TitleAPI.sendTitle(Bukkit.getPlayer(args[1]), 2, 8, 2, ChatColor.GREEN + "",
							ChatColor.GREEN + "You have access to this faith!");
					RunicParadise.perms.playerAdd(Bukkit.getPlayer(args[1]), "rp.faith.tech");
					RunicParadise.perms.playerAdd(Bukkit.getPlayer(args[1]), "rp.faith.nature");

				} else {
					TitleAPI.sendTitle(Bukkit.getPlayer(args[1]), 2, 3, 2, ChatColor.RED + "",
							ChatColor.RED + "You do not have access to this faith! Need 900 faith levels but have "
									+ new RunicPlayerBukkit(Bukkit.getPlayer(args[1])).getFaithPowerLevel() + ".");
				}
			} else if (args[2].equals("fire") || args[2].equals("water")) {
				if (new RunicPlayerBukkit(Bukkit.getPlayer(args[1])).getFaithPowerLevel() >= 125) {

					TitleAPI.sendTitle(Bukkit.getPlayer(args[1]), 2, 8, 2, ChatColor.GREEN + "",
							ChatColor.GREEN + "You have access to this faith!");
					RunicParadise.perms.playerAdd(Bukkit.getPlayer(args[1]), "rp.faith.fire");
					RunicParadise.perms.playerAdd(Bukkit.getPlayer(args[1]), "rp.faith.water");

				} else {
					TitleAPI.sendTitle(Bukkit.getPlayer(args[1]), 2, 3, 2, ChatColor.RED + "",
							ChatColor.RED + "You do not have access to this faith! Need 125 faith levels but have "
									+ new RunicPlayerBukkit(Bukkit.getPlayer(args[1])).getFaithPowerLevel() + ".");
				}
			} else if (Bukkit.getPlayer(args[1]).hasPermission("rp.faith." + args[2])) {

				TitleAPI.sendTitle(Bukkit.getPlayer(args[1]), 2, 8, 2, ChatColor.GREEN + "",
						ChatColor.GREEN + "You have access to this faith!");
			} else {
				TitleAPI.sendTitle(Bukkit.getPlayer(args[1]), 2, 3, 2, ChatColor.RED + "",
						ChatColor.RED + "You do not have access to this faith!");
			}

		} else if (args[0].toLowerCase().equals("menu")) {
			Faith.showFaithMenu((Player) sender);
		}

		else if (args[0].toLowerCase().equals("video")) {
			sender.sendMessage(ChatColor.GRAY + "[" + ChatColor.BLUE + "Runic" + ChatColor.DARK_AQUA + "Faith"
					+ ChatColor.GRAY + "] " + ChatColor.BLUE + "Faith tutorial video @ " + ChatColor.WHITE
					+ "https://goo.gl/2WEgT8");
		} else if (args[0].toLowerCase().equals("help")) {
			sender.sendMessage(ChatColor.GRAY + "[" + ChatColor.BLUE + "Runic" + ChatColor.DARK_AQUA + "Faith"
					+ ChatColor.GRAY + "] " + ChatColor.BLUE + "Basic Help:");
			sender.sendMessage(ChatColor.AQUA + "Faith Area:" + ChatColor.GRAY + " /warp faith");
			sender.sendMessage(ChatColor.AQUA + "Change Faiths:" + ChatColor.GRAY
					+ " Go to the warp area and step on the plate in the faith temples. New players can use Sun or Moon.");
			sender.sendMessage(ChatColor.AQUA + "Leveling Faiths:" + ChatColor.GRAY
					+ " Kill monsters with a faith sword or sacrifice items via prayers. Check the 2nd floor at the faith area.");
			sender.sendMessage(ChatColor.AQUA + "More Info:" + ChatColor.GRAY
					+ " Get the guide book on the 2nd floor of the faith hub!");
		} else if (args[0].toLowerCase().equals("powers")) {
			RunicParadise.faithMap.get(((Player) sender).getUniqueId()).listPowers(false);

		} else if (args[0].toLowerCase().equals("allpowers")) {
			RunicParadise.faithMap.get(((Player) sender).getUniqueId()).listPowers(true);

		} else if (args[0].toLowerCase().equals("report") && (sender.hasPermission("rp.faith.staff"))) {

			try {

				final Connection dbCon = MySQL.openConnection();
				Statement dbStmt = dbCon.createStatement();
				ResultSet reportResult = dbStmt.executeQuery("SELECT SUM(Level) FROM rp_PlayerFaiths;");

				if (!reportResult.isBeforeFirst()) {
					// No results
					// do nothing

					dbCon.close();
				} else {
					// results found!
					while (reportResult.next()) {
						// Got result
						sender.sendMessage(ChatColor.AQUA + "Total faith levels: " + ChatColor.DARK_AQUA + ""
								+ reportResult.getString(1));

					}
					dbStmt.close();
					dbCon.close();
				}

			} catch (SQLException z) {
				Bukkit.getLogger().log(Level.SEVERE, "Failed faith report. " + z.getMessage());
			}

		} else if (args[0].toLowerCase().equals("reset")
				&& (sender.hasPermission("rp.faith.staff") || (sender instanceof ConsoleCommandSender))) {
			sender.sendMessage("Clearing FaithMap now.");
			Faith.deactivateFaiths();
			sender.sendMessage("Rebuilding FaithMap now.");
			sender.sendMessage("Reloading Runic Eyes now.");
			RunicParadise.loadRunicEyes();
			sender.sendMessage("Reloading Prayer Books now.");
			RunicParadise.loadPrayerBooks();
			int counter = 0;
			for (Player p : Bukkit.getOnlinePlayers()) {
				RunicParadise.faithMap.put(p.getUniqueId(), new Faith(p.getUniqueId()));
				if (p.hasPermission("rp.faith.user")) {
					p.sendMessage(ChatColor.GRAY + "[" + ChatColor.BLUE + "Runic" + ChatColor.DARK_AQUA + "Faith"
							+ ChatColor.GRAY + "] " + ChatColor.BLUE + "Faith system activated!");
				}
				counter++;

			}
			sender.sendMessage(counter + " player entries added.");
			Faith.getFaithSettings();
			Faith.getPowerSettings();

		} else if (args[0].toLowerCase().equals("stats") && args.length == 1) {
			if (sender instanceof Player) {
				Player p = (Player) sender;
				String response = RunicParadise.faithMap.get(p.getUniqueId()).getPlayerStats(p.getUniqueId(),
						p.getUniqueId());
				if (!response.equals("Success")) {
					p.sendMessage(ChatColor.DARK_BLUE + "[RunicFaith] " + ChatColor.BLUE + "Failed! " + response);
				}
			}

		} else if (args[0].toLowerCase().equals("stats") && args.length == 2) {
			if (sender instanceof Player) {

				Player p = (Player) sender;
				String response = "";

				try {
					Player requested = Bukkit.getPlayer(args[1]);
					response = RunicParadise.faithMap.get(requested.getUniqueId())
							.getPlayerStats(requested.getUniqueId(), p.getUniqueId());
				} catch (Exception E) {
					sender.sendMessage("You can only lookup stats for people online now.");
				}

				if (!response.equals("Success")) {
					p.sendMessage(ChatColor.DARK_BLUE + "[RunicFaith] " + ChatColor.BLUE + "Failed! " + response);
				}
			}

		} else if (args[0].toLowerCase().equals("setlevel")
				&& (sender.hasPermission("rp.faith.staff") || (sender instanceof ConsoleCommandSender))) {
			String response = RunicParadise.faithMap.get(Bukkit.getPlayer(args[1]).getUniqueId())
					.setSkill(Bukkit.getPlayer(args[1]), sender.getName(), args[2], Integer.parseInt(args[3]));
			if (response.equals("Success")) {
				// if "Success" returned, the skill was processed
				// successfully
				sender.sendMessage("Skill change succeeded.");
			} else {
				sender.sendMessage("Skill change failed: " + response);
			}
		} else if (args[0].toLowerCase().equals("skillup")
				&& (sender.hasPermission("rp.faith.staff") || (sender instanceof ConsoleCommandSender))) {
			if (RunicParadise.faithMap.get(Bukkit.getPlayer(args[1]).getUniqueId())
					.incrementSkill(Bukkit.getPlayer(args[1]), args[2])) {
				// if true returned, the skill was processed successfully
				sender.sendMessage("Skill increase succeeded.");
			} else {
				sender.sendMessage("Skill increase failed.");
			}
		} else if (args[0].toLowerCase().equals("listmap")
				&& (sender.hasPermission("rp.faith.staff") || (sender instanceof ConsoleCommandSender))) {
			sender.sendMessage("Faith Map Contents:");
			for (java.util.UUID pUUID : RunicParadise.faithMap.keySet()) {
				sender.sendMessage(Bukkit.getPlayer(pUUID).getDisplayName());
			}
		} else if (args[0].toLowerCase().equals("enable") && args.length == 3
				&& (sender.hasPermission("rp.faith.staff") || (sender instanceof ConsoleCommandSender))) {
			String response = RunicParadise.faithMap.get(Bukkit.getPlayer(args[1]).getUniqueId())
					.enableFaith(Bukkit.getPlayer(args[1]).getUniqueId(), args[2]);

			if (response.equals("Success")) {
				if (sender instanceof ConsoleCommandSender) {
					// player used command block, notify player
					Bukkit.getPlayer(args[1])
							.sendMessage(ChatColor.GRAY + "[" + ChatColor.BLUE + "Runic" + ChatColor.DARK_AQUA
									+ "Faith" + ChatColor.GRAY + "] " + ChatColor.AQUA + args[2] + ChatColor.BLUE
									+ " is now your active faith! Level: " + ChatColor.DARK_AQUA
									+ RunicParadise.faithMap.get(Bukkit.getPlayer(args[1]).getUniqueId())
									.getPrimaryFaithLevel()
									+ ChatColor.WHITE + "/" + ChatColor.GRAY
									+ RunicParadise.faithSettingsMap.get(args[2])[4]);
				} else {
					// staff used command, notify them and player
					sender.sendMessage(ChatColor.GRAY + "[" + ChatColor.BLUE + "Runic" + ChatColor.DARK_AQUA
							+ "Faith" + ChatColor.GRAY + "] " + ChatColor.BLUE + "Enabled " + ChatColor.AQUA
							+ args[2] + ChatColor.BLUE + " for " + ChatColor.GRAY + args[1] + ChatColor.BLUE
							+ ". If other faiths were active, they are now disabled.");
					Bukkit.getPlayer(args[1])
							.sendMessage(ChatColor.GRAY + "[" + ChatColor.BLUE + "Runic" + ChatColor.DARK_AQUA
									+ "Faith" + ChatColor.GRAY + "] " + ChatColor.AQUA + args[2] + ChatColor.BLUE
									+ " is now your active faith! Level: " + ChatColor.DARK_AQUA
									+ RunicParadise.faithMap.get(Bukkit.getPlayer(args[1]).getUniqueId())
									.getPrimaryFaithLevel()
									+ ChatColor.WHITE + "/" + ChatColor.GRAY
									+ RunicParadise.faithSettingsMap.get(args[2])[4]);

				}

			} else {
				sender.sendMessage(ChatColor.GRAY + "[" + ChatColor.BLUE + "Runic" + ChatColor.DARK_AQUA + "Faith"
						+ ChatColor.GRAY + "] " + ChatColor.BLUE + "Failed! " + response);

			}
		} else {
			sender.sendMessage(ChatColor.GRAY + "[" + ChatColor.BLUE + "Runic" + ChatColor.DARK_AQUA + "Faith"
					+ ChatColor.GRAY + "] " + ChatColor.RED + "Invalid command or you don't have permission!");
		}
	}

	private void dailyKarmaCommand(CommandSender sender, String[] args) {
		try {
			MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);

			Date date = new Date();
			// get time 24 hours ago
			long timeCheck = date.getTime() - 86400000;

			final Connection d = MySQL.openConnection();
			Statement dStmt = d.createStatement();
			ResultSet playerData = dStmt.executeQuery("SELECT * FROM rp_PlayerInfo WHERE LastSeen > " + timeCheck
					+ " AND KillZombie > 1 ORDER BY RAND() LIMIT 1;");
			if (playerData.isBeforeFirst()) {
				// record returned!
				playerData.next();
				RunicPlayerBukkit.adjustOfflinePlayerKarma(playerData.getString("PlayerName"), 10);

				for (Player p : Bukkit.getOnlinePlayers()) {
					p.sendMessage(ChatColor.GRAY + "[" + ChatColor.BLUE + "Runic" + ChatColor.DARK_AQUA + "Faith"
							+ ChatColor.GRAY + "] " + ChatColor.GREEN + playerData.getString("PlayerName")
							+ ChatColor.BLUE
							+ " won 10 karma in the daily raffle! Login once a day for your chance to win!");

				}

				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mail send " + playerData.getString("PlayerName")
						+ " Congrats! You won 10 karma in the daily raffle. :)");

			}
		} catch (SQLException e) {
			Bukkit.getLogger().log(Level.SEVERE, "Failed ItemTransfers Lookup because " + e.getMessage());
		}
	}

	private void jungleMazeCommand(CommandSender sender, String[] args) {
    	MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
		if (args.length > 0) {
			if (args[0].equals("checkpoint") && args.length == 3) {
				Bukkit.getPlayer(args[2]).sendMessage("Updated your progress for checkpoint " + args[1]);
				if (Integer.parseInt(args[1]) == 1) {
					// if its the first checkpoint
					// create new record
					Bukkit.getLogger().log(Level.INFO, "3333");

					try {
						Bukkit.getLogger().log(Level.INFO, "Creating new jungle maze record for " + args[2]);

						final Connection d = MySQL.openConnection();
						Statement dStmt = d.createStatement();
						PreparedStatement insertStmt = d
								.prepareStatement("INSERT INTO rp_JungleMaze (UUID, CP1) VALUES " + "('"
										+ Bukkit.getPlayer(args[2]).getUniqueId().toString() + "', 1);");
						insertStmt.executeUpdate();
						d.close();
						dStmt.close();

					} catch (SQLException e) {
						Bukkit.getLogger().log(Level.SEVERE, "Failed junglemaze record creation " + e.getMessage());
					}
				} else {
					try {
						final Connection d = MySQL.openConnection();
						Statement dStmt = d.createStatement();
						PreparedStatement insertStmt = d
								.prepareStatement("UPDATE rp_JungleMaze SET CP" + args[1] + "= 1 "
										+ "WHERE UUID='" + Bukkit.getPlayer(args[2]).getUniqueId().toString()
										+ "';");
						insertStmt.executeUpdate();
						d.close();
						dStmt.close();

					} catch (SQLException e) {
						Bukkit.getLogger().log(Level.SEVERE, "Failed junglemaze record creation " + e.getMessage());
					}
				}
			} else if (args[0].equals("complete")) {
				try {
					final Connection d = MySQL.openConnection();
					Statement dStmt = d.createStatement();
					ResultSet playerData = dStmt.executeQuery("SELECT * FROM rp_JungleMaze WHERE UUID = '"
							+ Bukkit.getPlayer(args[1]).getUniqueId().toString() + "';");
					if (!playerData.isBeforeFirst()) {
						// No record returned
						Bukkit.getPlayer(args[1]).sendMessage(
								"Could not check jungle maze status because you don't have any records yet. Start with checkpoint 1!");

					} else {
						// record returned!
						playerData.next();

						if (playerData.getInt("Complete") == 1) {
							Bukkit.getPlayer(args[1]).sendMessage(
									"You already completed the jungle maze. You can only get the prize once!");
							return;
						}

						int cpCounter = 1;
						StringBuilder build1 = new StringBuilder();
						StringBuilder build2 = new StringBuilder();

						int completeCheckPoints = 0;
						ChatColor color;

						while (cpCounter <= 24) {

							if (playerData.getInt("CP" + cpCounter) == 1) {
								// checkpoint is complete
								color = ChatColor.GREEN;
								completeCheckPoints++;
							} else {
								// checkpoint incomplete
								color = ChatColor.BLACK;
							}

							if (cpCounter <= 13) {
								build1.append(ChatColor.DARK_GRAY).append("[").append(color).append(cpCounter).append(ChatColor.DARK_GRAY).append("]");
							} else {
								build2.append(ChatColor.DARK_GRAY).append("[").append(color).append(cpCounter).append(ChatColor.DARK_GRAY).append("] ");
							}

							cpCounter++;
						}
						String completeMessage;

						if (completeCheckPoints < 24) {
							int remaining = 24 - completeCheckPoints;
							completeMessage = ChatColor.GOLD + "You still need " + ChatColor.RED + remaining
									+ ChatColor.GOLD + " more checkpoints.";
						} else {
							completeMessage = ChatColor.DARK_RED
									+ "Congratulations! You've completed the jungle maze!.";

							for (Player p : Bukkit.getOnlinePlayers()) {
								TitleAPI.sendTitle(p, 2, 3, 2, ChatColor.DARK_GRAY + "" + ChatColor.BOLD
										+ Bukkit.getPlayer(args[1]).getDisplayName(), ChatColor.AQUA + "just completed the jungle maze!");
							}

							try {
								PreparedStatement insertStmt = d
										.prepareStatement("UPDATE rp_JungleMaze SET Complete" + "= 1 "
												+ "WHERE UUID='"
												+ Bukkit.getPlayer(args[1]).getUniqueId().toString() + "';");
								insertStmt.executeUpdate();
								d.close();
								dStmt.close();

							} catch (SQLException e) {
								Bukkit.getLogger().log(Level.SEVERE,
										"Failed junglemaze record update " + e.getMessage());
							}

							RunicParadise.playerProfiles.get(Bukkit.getPlayer(args[1]).getUniqueId())
									.addMazeCompletion(4);
							new RunicPlayerBukkit(args[1]).adjustPlayerKarma(20);

						}
						Bukkit.getPlayer(args[1]).sendMessage(new String[] { ChatColor.GRAY + "Your jungle maze completion status:",
								build1.toString(), build2.toString(), completeMessage });

					}
				} catch (SQLException e) {
					Bukkit.getLogger().log(Level.SEVERE, "Failed junglemaze record creation " + e.getMessage());
				}

			} else if (args[0].equals("check")) {

				try {
					final Connection d = MySQL.openConnection();
					Statement dStmt = d.createStatement();
					ResultSet playerData = dStmt.executeQuery("SELECT * FROM rp_JungleMaze WHERE UUID = '"
							+ Bukkit.getPlayer(args[1]).getUniqueId().toString() + "';");
					if (!playerData.isBeforeFirst()) {
						// No record returned
						Bukkit.getPlayer(args[1]).sendMessage(
								"Could not check jungle maze status because you don't have any records yet. Start with checkpoint 1!");

					} else {
						// record returned!
						playerData.next();

						if (playerData.getInt("Complete") == 1) {
							Bukkit.getPlayer(args[1]).sendMessage(
									"You already completed the jungle maze. You can only get the prize once!");
							return;
						}

						int cpCounter = 1;
						StringBuilder build1 = new StringBuilder();
						StringBuilder build2 = new StringBuilder();

						int completeCP = 0;
						ChatColor color;

						while (cpCounter <= 24) {

							if (playerData.getInt("CP" + cpCounter) == 1) {
								// checkpoint is complete
								color = ChatColor.GREEN;
								completeCP++;
							} else {
								// checkpoint incomplete
								color = ChatColor.BLACK;
							}

							if (cpCounter <= 13) {
								build1.append(ChatColor.DARK_GRAY).append(" [").append(color).append(cpCounter).append(ChatColor.DARK_GRAY).append("]");
							} else {
								build2.append(ChatColor.DARK_GRAY).append(" [").append(color).append(cpCounter).append(ChatColor.DARK_GRAY).append("]");
							}

							cpCounter++;
						}
						String completeMessage;

						if (completeCP < 24) {
							int remaining = 24 - completeCP;
							completeMessage = ChatColor.GOLD + "You still need " + ChatColor.RED + remaining
									+ ChatColor.GOLD + " more checkpoints.";
						} else {
							completeMessage = ChatColor.DARK_RED
									+ "Congratulations! You've completed the jungle maze! Head to the finish to claim your prize.";

						}

						Bukkit.getPlayer(args[1])
								.sendMessage(new String[] { ChatColor.GRAY + "Your jungle maze completion status:",
										build1.toString(), build2.toString(), completeMessage });

					}
				} catch (SQLException e) {
					Bukkit.getLogger().log(Level.SEVERE, "Failed junglemaze record creation " + e.getMessage());
				}
			}
		}
	}

	private static void machineMazeCommand(CommandSender sender, String[] args) {
		boolean problemMachine = false;
		boolean emptyMachine = true;

		if (args.length == 2 && args[0].equals("enter")) {
			// check for players in the maze or entry zone
			for (Entity e : Bukkit.getPlayer(args[1]).getNearbyEntities(300, 150, 300)) {
				if (e instanceof Player) {
					if ((e.getLocation().getX() <= 1132 && e.getLocation().getX() >= 1060)
							&& (e.getLocation().getY() <= 40 && e.getLocation().getY() >= 2)
							&& (e.getLocation().getZ() <= -16 && e.getLocation().getZ() >= -93)) {
						// A player is in the maze!
						Bukkit.getPlayer(args[1]).sendMessage(ChatColor.DARK_RED + "MachineMaster Tardip"
								+ ChatColor.GRAY
								+ ": Sorry, someone is already in the maze. Please wait for them to finish (or fail).");
						problemMachine = true;

					}

				}

			}

			// check if the player's inventory is empty
			for (ItemStack item : Bukkit.getPlayer(args[1]).getInventory().getContents()) {
				if (item != null && item.getType() != Material.AIR) {
					problemMachine = true;
					emptyMachine = false;
				}
			}
			// check if the player's inventory is empty
			for (ItemStack armor : Bukkit.getPlayer(args[1]).getInventory().getArmorContents()) {
				if (armor != null && armor.getType() != Material.AIR) {
					problemMachine = true;
					emptyMachine = false;
				}
			}

			if (!emptyMachine) {
				Bukkit.getPlayer(args[1]).sendMessage(ChatColor.DARK_RED + "MachineMaster Tardip" + ChatColor.GRAY
						+ ": Your inventory & armor slots must be empty to enter this maze. Why not use that ender chest over there.");
			}

			if (!problemMachine) {
				Bukkit.getPlayer(args[1]).teleport(
						new Location(Bukkit.getWorld("RunicSky"), 1132.5f, 8f, -82f, -266.14868f, 6.600022f));
				Bukkit.getPlayer(args[1]).sendMessage(ChatColor.DARK_RED + "MachineMaster Tardip" + ChatColor.GRAY
						+ ": Welcome to the Machine Maze! Only one person may be in the maze at a time. Potion effects are removed when you teleport in. You will not lose a soul if you die - but you will have to start over!");
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "heal " + args[1]);
			}
		}
	}

	private static void crocoMazeCommand(CommandSender sender, String[] args) {
		boolean problem = false;
		boolean empty = true;

		if (args.length == 3 && args[0].equals("enter")) {
			Player victim = Bukkit.getPlayer(args[2]);

			// check for players in the maze or entry zone
			for (Entity e : victim.getNearbyEntities(200, 100, 200)) {
				if (e instanceof Player) {
					if (args[1].equalsIgnoreCase("dungeon")) {
						if ((e.getLocation().getX() <= -142 && e.getLocation().getX() >= -192)
								&& (e.getLocation().getY() <= 121 && e.getLocation().getY() >= 107)
								&& (e.getLocation().getZ() <= 513 && e.getLocation().getZ() >= 463)) {
							// A player is in the maze!
							victim.sendMessage(ChatColor.DARK_RED + "DungeonMaster CrocodileHax"
									+ ChatColor.GRAY
									+ ": Sorry, someone is already in the maze. Please wait for them to finish (or fail).");
							problem = true;

						}
						if ((e.getLocation().getX() <= -137.5 && e.getLocation().getX() >= -140.5)
								&& (e.getLocation().getY() <= 120 && e.getLocation().getY() >= 114)
								&& (e.getLocation().getZ() <= 513.5 && e.getLocation().getZ() >= 506.5)) {
							// A player is in the maze!
							victim.sendMessage(ChatColor.DARK_RED + "DungeonMaster CrocodileHax"
									+ ChatColor.GRAY
									+ ": Sorry, someone is already in the maze. Please wait for them to finish (or fail).");

							problem = true;

						}
					} else if (args[1].equalsIgnoreCase("anguish")) {
						if ((e.getLocation().getX() <= 1055 && e.getLocation().getX() >= 1048)
								&& (e.getLocation().getY() <= 126 && e.getLocation().getY() >= 120)
								&& (e.getLocation().getZ() <= 1157 && e.getLocation().getZ() >= 1153)) {
							// A player is in the maze!
							victim.sendMessage(ChatColor.DARK_RED + "DungeonMaster CrocodileHax"
									+ ChatColor.GRAY
									+ ": Sorry, someone is already in the maze. Please wait for them to finish (or fail).");
							problem = true;

						}
						if ((e.getLocation().getX() <= 1171 && e.getLocation().getX() >= 1047)
								&& (e.getLocation().getY() <= 160 && e.getLocation().getY() >= 80)
								&& (e.getLocation().getZ() <= 1152 && e.getLocation().getZ() >= 1053)) {
							// A player is in the maze!
							victim.sendMessage(ChatColor.DARK_RED + "DungeonMaster CrocodileHax"
									+ ChatColor.GRAY
									+ ": Sorry, someone is already in the maze. Please wait for them to finish (or fail).");

							problem = true;

						}
					}
				}

			}

			// check if the player's inventory is empty
			for (ItemStack item : victim.getInventory().getContents()) {
				if (item != null && item.getType() != Material.AIR) {
					problem = true;
					empty = false;
				}
			}
			// check if the player's inventory is empty
			for (ItemStack armor : victim.getInventory().getArmorContents()) {
				if (armor != null && armor.getType() != Material.AIR) {
					problem = true;
					empty = false;
				}
			}

			if (!empty) {
				victim.sendMessage(ChatColor.DARK_RED + "DungeonMaster CrocodileHax"
						+ ChatColor.GRAY
						+ ": Your inventory & armor slots must be empty to enter this maze. Why not use that ender chest over there.");
			}

			if (!problem) {
				if (args[1].equalsIgnoreCase("dungeon")) {
					victim.teleport(new Location(Bukkit.getWorld("RunicSky"), -138.5, 121, 511.5));
					victim.sendMessage(ChatColor.DARK_RED + "DungeonMaster CrocodileHax"
							+ ChatColor.GRAY
							+ ": Welcome to the Dungeon Maze! Only one person may be in the maze at a time. Potion effects are removed when you teleport in. You will not lose a soul if you die - but you will have to start over!");
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "heal " + victim.getName());
				} else if (args[1].equalsIgnoreCase("anguish")) {
					victim.teleport(new Location(Bukkit.getWorld("RunicSky"), 1051, 122, 1155, 180.62622f, 1.7843645f));
					victim.sendMessage(ChatColor.DARK_RED + "DungeonMaster CrocodileHax"
							+ ChatColor.GRAY
							+ ": Welcome to the Anguish Maze! Only one person may be in the maze at a time. Potion effects are removed when you teleport in. You will not lose a soul if you die - but you will have to start over!");
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "heal " +victim.getName());

				}
			}

		}
	}

	private void miningResetCommand(CommandSender sender, String[] args) {
		Bukkit.getServer().getScheduler().scheduleAsyncDelayedTask(instance, () -> {
			Bukkit.broadcast("Mining world is resetting in 2 minutes!", "*");
		}, 20);

		Random rand = new Random();
		int tempRand = rand.nextInt(5001);

		Bukkit.getServer().getScheduler().scheduleAsyncDelayedTask(instance, () -> {
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv load Mining");
			sender.sendMessage("Sent command to load Mining world");
		}, 2200);

		Bukkit.getServer().getScheduler().scheduleAsyncDelayedTask(instance, () -> {
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mvconfirm");
			sender.sendMessage("Confirmed load command");
		}, 2250);

		Bukkit.getServer().getScheduler().scheduleAsyncDelayedTask(instance, () -> {
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv regen Mining -s " + tempRand);
			sender.sendMessage("Sent command to regen Mining world");
			sender.sendMessage("Used: regen Mining -s " + tempRand);
		}, 2400);

		Bukkit.getServer().getScheduler().scheduleAsyncDelayedTask(instance, () -> {
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mvconfirm");
			sender.sendMessage("Confirmed regen command");
		}, 2475);

		Bukkit.getServer().getScheduler().scheduleAsyncDelayedTask(instance, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
				"essentials:bc Mining world reset is now complete!"), 2599);

		Bukkit.getServer().getScheduler().scheduleAsyncDelayedTask(instance, () -> {
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv load Mining");
			sender.sendMessage("Sent command to load Mining world");
		}, 2600);

		Bukkit.getServer().getScheduler().scheduleAsyncDelayedTask(instance, () -> {
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mvconfirm");
			sender.sendMessage("Confirmed load command");
		}, 2640);

		Bukkit.getServer().getScheduler().scheduleAsyncDelayedTask(instance, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mvrule keepInventory true Mining"), 2700);
	}

	private void casinoCommand(CommandSender sender, String[] args) {
		// casino buytokens PLAYER RUNICS TOKENS
		// casino selltokens PLAYER RUNICS TOKENS
		// casino winorlose PLAYER RUNICS TOKENS WINSLOTS TOTALSLOTS
		// TODO: needs more fixing
		if (args != null && args.length > 0) {
			// //////////////////////////////////////////
			if (args[0].equalsIgnoreCase("buytokens") && args.length == 4) {

				// check if player has enough runics
				if (RunicParadise.economy.getBalance(args[1]) >= Integer.parseInt(args[2])) {
					// player has enough money
					// execute transaction

					Bukkit.getPlayer(args[1]).getInventory()
							.addItem(giveCasinoToken(args[1], Integer.parseInt(args[3])));
					RunicParadise.economy.withdrawPlayer(Bukkit.getOfflinePlayer(args[1]),
							Integer.parseInt(args[2]));

					RunicMessaging.sendMessage(Bukkit.getPlayer(args[1]), RunicMessaging.RunicFormat.CASINO,
							"You bought " + args[3] + " tokens for " + args[2] + " runics");

				} else {
					RunicMessaging.sendMessage(Bukkit.getPlayer(args[1]), RunicMessaging.RunicFormat.CASINO,
							"You can't afford those tokens!");
				}
				// ////////////////////////////////////////////
			} else if (args[0].equalsIgnoreCase("selltokens") && args.length == 4) {
				// check if player is holding tokens

				if (Bukkit.getPlayer(args[1]).getInventory().getItemInMainHand().getType() == Material.SUNFLOWER
						&& Bukkit.getPlayer(args[1]).getInventory().getItemInMainHand().getItemMeta().getLore()
						.toString().contains("Purchased")) {
					// player is holding a valid token

					// check if player is holding a proper amount of tokens
					if (Bukkit.getPlayer(args[1]).getInventory().getItemInMainHand().getAmount() > Integer
							.parseInt(args[3])) {
						// player is holding enough tokens

						RunicParadise.economy.depositPlayer(Bukkit.getOfflinePlayer(args[1]),
								Integer.parseInt(args[2]));

						Bukkit.getPlayer(args[1]).getInventory().getItemInMainHand()
								.setAmount(Bukkit.getPlayer(args[1]).getInventory().getItemInMainHand().getAmount()
										- Integer.parseInt(args[3]));

						RunicMessaging.sendMessage(Bukkit.getPlayer(args[1]), RunicMessaging.RunicFormat.CASINO,
								"You sold " + args[3] + " tokens for " + args[2] + " runics");

					} else if (Bukkit.getPlayer(args[1]).getInventory().getItemInMainHand().getAmount() == Integer
							.parseInt(args[3])) {
						// player has exactly the right amount of tokens

						RunicParadise.economy.depositPlayer(Bukkit.getOfflinePlayer(args[1]),
								Integer.parseInt(args[2]));

						Bukkit.getPlayer(args[1]).getInventory().setItemInMainHand(null);

						RunicMessaging.sendMessage(Bukkit.getPlayer(args[1]), RunicMessaging.RunicFormat.CASINO,
								"You sold " + args[3] + " tokens for " + args[2] + " runics");

					} else {
						// player is not holding enough tokens
						RunicMessaging.sendMessage(Bukkit.getPlayer(args[1]), RunicMessaging.RunicFormat.CASINO,
								"You don't have enough tokens in your hand");
					}

				} else {
					// player is not holding a valid token
					RunicMessaging.sendMessage(Bukkit.getPlayer(args[1]), RunicMessaging.RunicFormat.CASINO,
							"You need to hold tokens in your hand to sell them");
				}
				// //////////////////////////////////
			} else if (args[0].equalsIgnoreCase("winorlose") && args.length == 6) {
				// casino winorlose0 PLAYER1 RUNICS2 TOKENS3 WINSLOTS4
				// TOTALSLOTS5
				//

				// check if player has enough runics
				if (RunicParadise.economy.getBalance(args[1]) >= Integer.parseInt(args[2])) {
					// player has enough money
					// execute transaction

					RunicParadise.economy.withdrawPlayer(Bukkit.getOfflinePlayer(args[1]),
							Integer.parseInt(args[2]));

					RunicMessaging.sendMessage(Bukkit.getPlayer(args[1]), RunicMessaging.RunicFormat.CASINO,
							"You spent " + args[2] + " runics");

					// now play the game

					boolean winner;
					if (ThreadLocalRandom.current().nextInt(1, Integer.parseInt(args[5]) + 1) <= Integer
							.parseInt(args[4])) {
						winner = true;
						RunicMessaging.sendMessage(Bukkit.getPlayer(args[1]), RunicMessaging.RunicFormat.CASINO,
								ChatColor.AQUA + "" + ChatColor.MAGIC + "*" + ChatColor.DARK_AQUA + ""
										+ ChatColor.MAGIC + "*" + ChatColor.DARK_GREEN + "" + ChatColor.MAGIC + "*"
										+ ChatColor.GREEN + " WINNER " + ChatColor.DARK_GREEN + "" + ChatColor.MAGIC
										+ "*" + ChatColor.DARK_AQUA + "" + ChatColor.MAGIC + "*" + ChatColor.AQUA
										+ "" + ChatColor.MAGIC + "*");
						RunicMessaging.sendMessage(Bukkit.getPlayer(args[1]), RunicMessaging.RunicFormat.CASINO,
								"You won " + args[3] + " tokens");

						Bukkit.getPlayer(args[1]).getInventory()
								.addItem(giveCasinoToken(args[1], Integer.parseInt(args[3])));

					} else {
						winner = false;
						RunicMessaging.sendMessage(Bukkit.getPlayer(args[1]), RunicMessaging.RunicFormat.CASINO,
								"Sorry, you didn't win this time");
					}

				} else {
					RunicMessaging.sendMessage(Bukkit.getPlayer(args[1]), RunicMessaging.RunicFormat.CASINO,
							"You can't afford this game");
				}

			} else {
				RunicMessaging.sendMessage(Bukkit.getPlayer(args[1]), RunicMessaging.RunicFormat.CASINO,
						"Something went wrong!");
			}
		}
	}

	private void elCommand(CommandSender sender, String[] args) {
    	MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
		if (args != null && args.length == 4 && args[0].equalsIgnoreCase("create") && Integer.parseInt(args[2]) > -1
				&& Integer.parseInt(args[3]) > 0) {
			// CREATE NEW LOCATION

			try {
				Bukkit.getLogger().log(Level.INFO, "Creating new explorer's league location: " + args[0]
						+ ", Tokens: " + args[2] + ", Proximity: " + args[3] + ", Creator: " + sender.getName());

				String locationName = args[1].replace('_', ' ').replace("'", "");
				Location location = ((Player) sender).getLocation();
				String locString = String.format("%s.%d.%d.%d", location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
				int tokenReward = Integer.parseInt(args[2]);
				int difficultyRadius = Integer.parseInt(args[3]);

				Connection d = MySQL.openConnection();
				Statement dStmt = d.createStatement();
				PreparedStatement insertStmt = d.prepareStatement(
						"INSERT INTO rpgame.rp_ExplorerLocations (LocationName, Location, TokenReward, DifficultyRadius, Creator) VALUES "
								+ "('" + locationName + "', '" + locString + "', " + tokenReward + ", "
								+ difficultyRadius + ", '" + sender.getName() + "');");

				insertStmt.executeUpdate();
				d.close();
				dStmt.close();

				RunicMessaging.sendMessage(((Player) sender), RunicMessaging.RunicFormat.EXPLORER,
						"Created new Explorer's League location! http://goo.gl/mjf9S4");

			} catch (SQLException e) {
				Bukkit.getLogger().log(Level.SEVERE, "Failed ExplorerLocation record creation " + e.getMessage());
				RunicMessaging.sendMessage(((Player) sender), RunicMessaging.RunicFormat.EXPLORER,
						"Failed to create location: " + e.getMessage());
			}
		} else if (args != null && args.length == 3 && args[0].equalsIgnoreCase("prereq")
				&& Integer.parseInt(args[1]) > 0 && Integer.parseInt(args[2]) > 0) {
			// SET A PREREQ

			try {
				Bukkit.getLogger().log(Level.INFO, "Setting an EL loc prereq... Loc ID: " + args[1] + ", PreReq: "
						+ args[2] + ", Changer: " + sender.getName());

				final Connection d = MySQL.openConnection();
				Statement dStmt = d.createStatement();
				PreparedStatement insertStmt = d.prepareStatement("UPDATE rp_ExplorerLocations SET PreReq = "
						+ args[2] + ", Changer = '" + sender.getName() + "' WHERE ID = " + args[1] + ";");

				insertStmt.executeUpdate();
				d.close();
				dStmt.close();

				RunicMessaging.sendMessage(((Player) sender), RunicMessaging.RunicFormat.EXPLORER,
						"Successfully edited prereq! http://goo.gl/mjf9S4");

			} catch (SQLException e) {
				Bukkit.getLogger().log(Level.SEVERE, "Failed ExplorerLocation record creation " + e.getMessage());
				RunicMessaging.sendMessage(((Player) sender), RunicMessaging.RunicFormat.EXPLORER,
						"Failed to create location: " + e.getMessage());
			}
		} else if (args != null && args.length == 2 && args[0].equalsIgnoreCase("remove")
				&& Integer.parseInt(args[1]) > 0) {
			// DISABLE A LOCATION

			try {
				Bukkit.getLogger().log(Level.INFO,
						"Disabling explorer's league location: " + args[1] + ", User: " + sender.getName());

				int recordIDtoDelete = Integer.parseInt(args[1]);

				final Connection d = MySQL.openConnection();
				Statement dStmt = d.createStatement();
				PreparedStatement insertStmt = d.prepareStatement(
						"UPDATE rp_ExplorerLocations SET Status = 'Disabled', Changer = '" + sender.getName()
								+ "' WHERE ID = " + recordIDtoDelete + " AND Status = 'Enabled';");

				insertStmt.executeUpdate();
				d.close();
				dStmt.close();

				RunicMessaging.sendMessage(((Player) sender), RunicMessaging.RunicFormat.EXPLORER,
						"Disabled Explorer's League location " + recordIDtoDelete);

			} catch (Exception e) {
				Bukkit.getLogger().log(Level.SEVERE, "Failed ExplorerLocation record disabling. " + e.getMessage());
				RunicMessaging.sendMessage(((Player) sender), RunicMessaging.RunicFormat.EXPLORER,
						"Failed to disable location: " + e.getMessage());
			}
		} else if (args != null && args.length == 2 && args[0].equalsIgnoreCase("enable")
				&& Integer.parseInt(args[1]) > 0) {
			// ENABLE A LOCATION

			try {
				Bukkit.getLogger().log(Level.INFO,
						"Enabling explorer's league location: " + args[1] + ", User: " + sender.getName());

				int recordIDtoDelete = Integer.parseInt(args[1]);

				final Connection d = MySQL.openConnection();
				Statement dStmt = d.createStatement();
				PreparedStatement insertStmt = d.prepareStatement(
						"UPDATE rp_ExplorerLocations SET Status = 'Enabled', Changer = '" + sender.getName()
								+ "' WHERE ID = " + recordIDtoDelete + " AND Status = 'Disabled';");

				insertStmt.executeUpdate();
				d.close();
				dStmt.close();

				RunicMessaging.sendMessage(((Player) sender), RunicMessaging.RunicFormat.EXPLORER,
						"Enabled Explorer's League location " + recordIDtoDelete);

			} catch (Exception e) {
				Bukkit.getLogger().log(Level.SEVERE, "Failed ExplorerLocation record enabling. " + e.getMessage());
				RunicMessaging.sendMessage(((Player) sender), RunicMessaging.RunicFormat.EXPLORER,
						"Failed to enable location: " + e.getMessage());
			}
		} else if (args != null && args.length == 1 && args[0].equalsIgnoreCase("reload")) {
			// RELOAD LOCATION HASHMAP

			Commands.syncExplorerLocations();

		} else {
			// display command help
			RunicMessaging.sendMessage(((Player) sender), RunicMessaging.RunicFormat.EXPLORER,
					"Displaying command help");
			sender.sendMessage(ChatColor.RED + "Create new spot. el create [name] [tokens] [req'd proximity]");
			sender.sendMessage(ChatColor.DARK_GRAY + " /el create Rune's_Secret_Spot 5 5");
			sender.sendMessage(ChatColor.RED + "Disable a spot. Get ID from website. el remove [id]");
			sender.sendMessage(ChatColor.DARK_GRAY + " /el remove 22");
			sender.sendMessage(ChatColor.RED + "Enable a spot. Get ID from website. el enable [id]");
			sender.sendMessage(ChatColor.DARK_GRAY + " /el enable 22");
			sender.sendMessage(ChatColor.RED + "Set a prereq on a location. el prereq ID PreReqID");
			sender.sendMessage(ChatColor.DARK_GRAY + " /el prereq 6 2");
			sender.sendMessage(ChatColor.RED + "Reload location data. Use after changing anything!");
			sender.sendMessage(ChatColor.DARK_GRAY + " /el reload");
		}
	}

	private void raffleCommand(CommandSender sender, String[] args) {
    	MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
		Player rafflePlayer = ((Player) sender);

		int tickets = 0;
		int raffleCount = 0;
		int totalRaffleCount = 0;
		int ticketCost = 5000;
		int maxPurchaseTickets = 1000;
		String raffleNameColor = "&bW&3i&bn&ft&be&3r &bR&fa&bf&3f&bl&fe";

		// HANDLE PURCHASED TICKETS
		if (args.length == 2 && args[0].equalsIgnoreCase("buy") && Integer.parseInt(args[1]) > 0
				&& Integer.parseInt(args[1]) <= maxPurchaseTickets) {
			tickets = Integer.parseInt(args[1]);
			try {
				final Connection d = MySQL.openConnection();
				Statement dStmt = d.createStatement();

				// GET CURRENT PURCHASED TICKET COUNT
				ResultSet playerData = dStmt.executeQuery(
						"SELECT SUM(Quantity) AS ticketcount FROM `rp_RunicRaffleTickets` WHERE `UUID` = '"
								+ rafflePlayer.getUniqueId().toString()
								+ "' AND Source = 'Purchased' ORDER BY `ID` DESC;");

				if (playerData.isBeforeFirst()) {
					playerData.next();

					raffleCount = playerData.getInt("ticketcount");

				}

				ResultSet playerData2 = dStmt.executeQuery(
						"SELECT SUM(Quantity) AS ticketcount FROM `rp_RunicRaffleTickets` WHERE `UUID` = '"
								+ rafflePlayer.getUniqueId().toString() + "' ORDER BY `ID` DESC;");

				if (playerData2.isBeforeFirst()) {
					playerData2.next();

					totalRaffleCount = playerData2.getInt("ticketcount");

				}

				if ((raffleCount + tickets) <= maxPurchaseTickets) {
					// MAX PURCHASE ... TICKETS
					if (RunicParadise.economy.getBalance(
							Bukkit.getOfflinePlayer(rafflePlayer.getUniqueId())) >= (ticketCost * tickets)) {
						RunicParadise.economy.withdrawPlayer(Bukkit.getOfflinePlayer(rafflePlayer.getUniqueId()),
								(ticketCost * tickets));
						RunicMessaging.sendMessage(rafflePlayer, RunicMessaging.RunicFormat.RAFFLE,
								"You spent " + (ticketCost * tickets) + " on " + tickets + " ticket(s) for the "
										+ ChatColor.translateAlternateColorCodes('&', raffleNameColor));

						PreparedStatement insertStmt = d.prepareStatement(
								"INSERT INTO rp_RunicRaffleTickets (PlayerName, UUID, Timestamp, RaffleID, Source, Quantity) VALUES "
										+ "('" + rafflePlayer.getName() + "', '"
										+ rafflePlayer.getUniqueId().toString() + "', " + (new Date().getTime())
										+ ", 1, 'Purchased', " + tickets + ");");
						insertStmt.executeUpdate();

					} else {
						RunicMessaging.sendMessage(rafflePlayer, RunicMessaging.RunicFormat.RAFFLE,
								"A " + ChatColor.translateAlternateColorCodes('&', raffleNameColor)
										+ " ticket costs " + ticketCost + " runics each! Get more money!");
					}

					d.close();
					dStmt.close();
				} else {
					// PLAYER TRYING TO BUY TOO MANY TICKETS
					RunicMessaging.sendMessage(rafflePlayer, RunicMessaging.RunicFormat.RAFFLE,
							"You can only buy up to " + maxPurchaseTickets + " tickets for the "
									+ ChatColor.translateAlternateColorCodes('&', raffleNameColor)
									+ ". You've already bought " + raffleCount + " ticket(s).");
				}
			} catch (SQLException e) {
				Bukkit.getLogger().log(Level.SEVERE, "Failed raffle ticket check" + e.getMessage());
			}
		} else if (args.length == 3 && args[0].equalsIgnoreCase("give") && Integer.parseInt(args[1]) > 0) {
			if (rafflePlayer.hasPermission("rp.rafflestaff")) {
				// STAFF GIVING TICKETS -- OR CMD BLOCK
				try {
					final Connection d = MySQL.openConnection();
					Statement dStmt = d.createStatement();

					PreparedStatement insertStmt = d.prepareStatement(
							"INSERT INTO rpgame.rp_RunicRaffleTickets (PlayerName, UUID, Timestamp, RaffleID, Source, Quantity) VALUES "
									+ "('" + Bukkit.getOfflinePlayer(args[2]).getName() + "', '"
									+ Bukkit.getOfflinePlayer(args[2]).getUniqueId().toString() + "', "
									+ (new Date().getTime()) + ", 1, 'Given', " + Integer.parseInt(args[1]) + ");");
					insertStmt.executeUpdate();
					d.close();
					dStmt.close();

					RunicMessaging.sendMessage(rafflePlayer, RunicMessaging.RunicFormat.RAFFLE,
							"You gave " + Integer.parseInt(args[1]) + " tickets to "
									+ Bukkit.getOfflinePlayer(args[2]).getName());
					RunicMessaging.sendMessage(Bukkit.getPlayer(args[2]), RunicMessaging.RunicFormat.RAFFLE,
							"You just received " + Integer.parseInt(args[1]) + " "
									+ ChatColor.translateAlternateColorCodes('&', raffleNameColor) + " tickets!");
				} catch (SQLException e) {
					Bukkit.getLogger().log(Level.SEVERE, "Failed raffle ticket give" + e.getMessage());
				}
			}
		} else {
			// HANDLE RAFFLE DEFAULT TEXT
			try {
				final Connection d = MySQL.openConnection();
				Statement dStmt = d.createStatement();

				// GET CURRENT PURCHASED TICKET COUNT
				ResultSet playerData = dStmt.executeQuery(
						"SELECT SUM(Quantity) AS ticketcount FROM rpgame.rp_RunicRaffleTickets WHERE UUID = '"
								+ rafflePlayer.getUniqueId().toString()
								+ "' AND Source = 'Purchased' ORDER BY `ID` DESC;");

				if (playerData.isBeforeFirst()) {
					playerData.next();

					raffleCount = playerData.getInt("ticketcount");

				}

				ResultSet playerData2 = dStmt.executeQuery(
						"SELECT SUM(Quantity) AS ticketcount FROM `rp_RunicRaffleTickets` WHERE `UUID` = '"
								+ rafflePlayer.getUniqueId().toString() + "' ORDER BY `ID` DESC;");

				if (playerData2.isBeforeFirst()) {
					playerData2.next();

					totalRaffleCount = playerData2.getInt("ticketcount");

				}
				d.close();
				dStmt.close();
			} catch (SQLException e) {
				Bukkit.getLogger().log(Level.SEVERE, "Failed raffle ticket check" + e.getMessage());
			}

			RunicMessaging.sendMessage(rafflePlayer, RunicMessaging.RunicFormat.RAFFLE,
					" Want to buy tickets for the " + ChatColor.translateAlternateColorCodes('&', raffleNameColor)
							+ ChatColor.GRAY + "? Get more info at " + ChatColor.BLUE + "/warp raffle"
							+ ChatColor.GRAY + " or use " + ChatColor.GREEN + "/raffle buy <# of tickets>");
			RunicMessaging.sendMessage(rafflePlayer, RunicMessaging.RunicFormat.RAFFLE,
					"Tickets cost " + ticketCost + " runics each!");
			RunicMessaging.sendMessage(rafflePlayer, RunicMessaging.RunicFormat.RAFFLE,
					"Purchased tickets: " + ChatColor.YELLOW + raffleCount + ChatColor.GRAY + ", Total tickets: "
							+ ChatColor.GREEN + totalRaffleCount + ChatColor.GRAY + " tickets.");
		}

	}

	private static void sayCommand(CommandSender sender, String[] args) {
		// TODO: needs fixing
		if (sender instanceof ConsoleCommandSender) {
			StringBuilder message = new StringBuilder();
			for (String b : args) {
				message.append(b).append(" ");
			}

			for (Player p : Bukkit.getServer().getOnlinePlayers()) {
				p.sendMessage(message.toString());
			}
		} else {
			BlockCommandSender senderCmd = (BlockCommandSender) sender;
			Block senderBlock = senderCmd.getBlock();

			switch (senderBlock.getWorld().getName()) {
				case "Mansion": {
					StringBuilder message = new StringBuilder();
					for (String b : args) {
						message.append(b).append(" ");
					}
					List<Player> mansionPlayers = Bukkit.getWorld("Mansion").getPlayers();
					for (Player p : mansionPlayers) {
						p.sendMessage(message.toString());
					}
					break;
				}
				case "Razul": {
					StringBuilder message = new StringBuilder();
					for (String b : args) {
						message.append(b).append(" ");
					}
					List<Player> mansionPlayers = Bukkit.getWorld("Razul").getPlayers();
					for (Player p : mansionPlayers) {
						p.sendMessage(message.toString());
					}
					break;
				}
				default: {
					// command block sender in a world not specified above
					String message = Arrays.stream(args).map(b -> b + " ").collect(Collectors.joining());

					for (Player p : Bukkit.getServer().getOnlinePlayers()) {
						p.sendMessage(message);
					}
					break;
				}
			}

		}
	}

	private void gamesCommand(CommandSender sender, String[] args) {
    	if (!(sender instanceof Player)) {
		    sender.sendMessage("[RP] Command must be used by a player");
		    return;
	    }

		Player player = (Player) sender;
		int tokenBal = 0;
		try {
			Connection d = RunicUtilities.getMysqlFromPlugin(instance).openConnection();
			Statement dStmt = d.createStatement();
			ResultSet playerData = dStmt.executeQuery("SELECT * FROM `rp_PlayerInfo` WHERE `PlayerName` = '"
					+ sender.getName() + "' ORDER BY `id` ASC LIMIT 1;");

			d.close();
		} catch (SQLException e) {
			Bukkit.getLogger().log(Level.SEVERE,
					"Failed token count DB check [games] because: " + e.getMessage());
		}

		if (args.length == 0 || args.length > 1) {
			RunicParadise.showRunicCarnivalMenu(player);

			/*
			 * player.sendMessage(ChatColor.YELLOW + "  ✸ " +
			 * ChatColor.GOLD + "✹ " + ChatColor.RED + "✺ " +
			 * ChatColor.RED + "Runic Carnival" + ChatColor.RED + " ✺ "
			 * + ChatColor.GOLD + "✹ " + ChatColor.YELLOW + "✸");
			 * player.sendMessage(ChatColor.GREEN + "      ♪ " +
			 * ChatColor.DARK_AQUA + "♫ " + ChatColor.AQUA + "☾ " +
			 * ChatColor.BLUE + "Tokens: " + tokenBal + ChatColor.AQUA +
			 * " ☽ " + ChatColor.DARK_AQUA + "♫ " + ChatColor.GREEN +
			 * "♪");
			 *
			 * // player.sendMessage(ChatColor.WHITE + "" + //
			 * ChatColor.ITALIC // + "Format: /games [option]");
			 * player.sendMessage(ChatColor.DARK_RED + "[1] " +
			 * ChatColor.GRAY + "Information Center");
			 * player.sendMessage(ChatColor.RED + "[2] " +
			 * ChatColor.GRAY + "Prize Cabin");
			 * player.sendMessage(ChatColor.GOLD + "[3] " +
			 * ChatColor.GRAY + "Puzzle Kiosk");
			 * player.sendMessage(ChatColor.YELLOW + "[4] " +
			 * ChatColor.GRAY + "High Roller Casino" +
			 * ChatColor.DARK_GRAY + " (Coming Soon!)");
			 * player.sendMessage(ChatColor.GREEN + "[5] " +
			 * ChatColor.GRAY + "Game Corner");
			 * player.sendMessage(ChatColor.DARK_AQUA + "[6] " +
			 * ChatColor.GRAY + "Quest Castle" + ChatColor.DARK_GRAY +
			 * " (Adventure Maps)"); player.sendMessage(ChatColor.BLUE +
			 * "[7] " + ChatColor.GRAY + "Battle Tower" +
			 * ChatColor.DARK_GRAY + " (Mob & PVP Arenas)");
			 * player.sendMessage(ChatColor.LIGHT_PURPLE + "[8] " +
			 * ChatColor.GRAY + "Creation Zone" + ChatColor.DARK_GRAY +
			 * " (Build Contests)");
			 */
		} else {
			try {
				Integer.parseInt(args[0]);
			} catch (Exception e) {
				player.sendMessage(ChatColor.GRAY + "[ERROR] Invalid entry. Please check options via /games");
				return;
			}
			World runicSky = Bukkit.getWorld("RunicSky");
			switch (Integer.parseInt(args[0])) {
				case 1:
					player.teleport(new Location(runicSky, 342, 58, 548, 0, (float) 1));
					break;
				case 2:
					player.teleport(new Location(runicSky, 320, 58, 522, (float) 92.50, (float) -16.05));
					break;
				case 3:
					player.teleport(new Location(runicSky, 328, 58, 543, (float) 72.99, (float) -26.40));
					break;
				case 4:
					player.teleport(new Location(runicSky, 328, 58, 507, (float) 135.499, (float) -23.99));
					break;
				case 5:
					player.teleport(new Location(runicSky, 342, 58, 507, (float) 180.35, (float) -28.95));
					break;
				case 6:
					player.teleport(new Location(runicSky, 358, 58, 508, (float) -131.25, (float) -27.600));
					break;
				case 7:
					player.teleport(new Location(runicSky, 359, 58, 522, (float) -90.300, (float) -42.4499));
					break;
				case 8:
					player.teleport(new Location(runicSky, 357, 58, 538, (float) -42.150, (float) -27.85));
					break;
				case 9:
					player.sendMessage("your yaw " + player.getLocation().getYaw());
					player.sendMessage("your pitch " + player.getLocation().getPitch());
					RunicParadise.showRunicCarnivalMenu(player);
					break;
				default:
					player.sendMessage(ChatColor.GRAY + "[ERROR] Invalid entry. Please check options via /games");
					break;
			}
		}
	}

	private static void testerChatCommand(CommandSender sender, String[] args) {
		// Not existent in plugin.yml

		if (args.length == 0) {
			sender.sendMessage(ChatColor.DARK_GRAY + "Staff chat. Usage: /sc [message]");
			return;
		}
		String name = sender.getName();

		String playerText = String.join(" ", args);
		String message = ChatColor.DARK_GRAY + "[" + ChatColor.DARK_PURPLE + "Tester"
				+ ChatColor.LIGHT_PURPLE + "Chat" + ChatColor.DARK_GRAY + "] " + ChatColor.WHITE
				+ name + ": " + ChatColor.LIGHT_PURPLE + playerText;

		Bukkit.getOnlinePlayers().stream()
				.filter(player -> player.hasPermission("rp.testers"))
				.forEach(player -> player.sendMessage(message));

		Bukkit.getLogger().log(Level.INFO, "[TesterChat] " + name + ": " + playerText);
	}

	private static void staffChatCommand(CommandSender sender, String[] args) {
		if (args.length == 0) {
			sender.sendMessage(ChatColor.DARK_GRAY + "Staff chat. Usage: /sc [message]");
			return;
		}

		String name = sender.getName();

		String playerText = String.join(" ", args);
		String message = ChatColor.DARK_GRAY + "[" + ChatColor.DARK_AQUA + "Staff" + ChatColor.AQUA
				+ "Chat" + ChatColor.DARK_GRAY + "] " + ChatColor.WHITE + name + ": "
				+ ChatColor.AQUA + playerText;

		Bukkit.getOnlinePlayers().stream()
				.filter(player -> player.hasPermission("rp.staff"))
				.forEach(player -> player.sendMessage(message));

		Bukkit.getLogger().log(Level.INFO, "[StaffChat] " + name + ": " + playerText);
	}

	private void faceCommand(CommandSender sender, String[] args) {
    	if (!(sender instanceof Player)) {
    		sender.sendMessage("Only in-game players can use this");
    		return;
	    }
	    Player player = (Player) sender;

		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);

		if (args.length != 1) {
			sender.sendMessage(ChatColor.DARK_RED + "Usage: " + ChatColor.AQUA + "/face <playername>"
					+ ChatColor.DARK_RED
					+ " Watch your spelling, you only get ONE chance every 6 hours!! Always enter FULL player names, NOT nicks!");
			return;
		}
		try {
			final Connection d = MySQL.openConnection();
			Statement dStmt = d.createStatement();
			ResultSet playerData = dStmt.executeQuery("SELECT * FROM `rp_HeadCreations` WHERE `PlayerName` = '"
					+ sender.getName() + "' AND `Timestamp` >= " + (new Date().getTime() - 21600000)
					+ " ORDER BY `ID` DESC LIMIT 1;");

			if (playerData.isBeforeFirst()) {
				playerData.next();
				Long currentTime = new Date().getTime();
				Long loggedTime = playerData.getLong("Timestamp");
				double diffHours = (currentTime - loggedTime) / (60.0 * 60 * 1000);
				sender.sendMessage(ChatColor.RED + "You can only use this command once every 6 hours. You last used it "
								+ diffHours + " hours ago.");

			} else {
				// No record found, proceed!
				String command = String.format("give %s minecraft:player_head{SkullOwner:{Name:\"%s\"}} 1", sender.getName(), args[0]);
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
				try {
					PreparedStatement insertStmt = d.prepareStatement(
							"INSERT INTO rp_HeadCreations (PlayerName, UUID, Timestamp, HeadRequested) VALUES "
									+ "('" + sender.getName() + "', '"
									+ player.getUniqueId().toString() + "', "
									+ (new Date().getTime()) + ", '" + args[0] + "');");
					insertStmt.executeUpdate();
					d.close();
					dStmt.close();
				} catch (SQLException e) {
					Bukkit.getLogger().log(Level.SEVERE, "Failed face record creation " + e.getMessage());
				}
			}
			d.close();
		} catch (SQLException e) {
			Bukkit.getLogger().log(Level.SEVERE, "Failed /face check" + e.getMessage());
		}
	}

	private void handleRpVersion(CommandSender sender) {
		try {
			InputStream input = Commands.class.getResourceAsStream("/git.properties");
			JSONObject json = new JSONObject(new JSONTokener(input));
			input.close();

			json.remove("git.build.host");
			json.remove("git.build.user.email");
			json.remove("git.build.user.name");
			json.remove("git.commit.user.email");
			json.remove("git.commit.user.name");
			json.remove("git.remote.origin.url");

			StringBuilder message = new StringBuilder(ChatColor.BLUE + "Version information: \n");
			for (String key : json.keySet()) {
				Object value = json.get(key);
				message.append(ChatColor.GREEN).append(key).append(ChatColor.RESET).append(" : ")
						.append(ChatColor.AQUA).append(value.toString()).append('\n');
			}
			sender.sendMessage(message.toString());
		} catch (Exception e) {
			e.printStackTrace();
			sender.sendMessage(e.toString());
		}
	}

	static void givePlayerExplorationReward(int locID, Player p) {
		int tokenReward = RunicParadise.explorerRewards.get(locID);

		RunicPlayerBukkit targetPlayer = new RunicPlayerBukkit(p.getUniqueId());
		int newBalance = targetPlayer.getPlayerTokenBalance() + tokenReward;
		targetPlayer.setPlayerTokenBalance(newBalance);

		RunicMessaging.sendMessage(p, RunicMessaging.RunicFormat.EXPLORER,
				"Congratulations! You found " + RunicParadise.explorerIDs.get(locID));
	}

	private static void spawnTransportBeacon(Location loc, Player p) {
		Location clayLoc = new Location(loc.getWorld(), loc.getX(), (loc.getY() - 1.0), loc.getZ());
		Location glassLoc = new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ());

		Block clay = clayLoc.getBlock();
		Block glass = glassLoc.getBlock();

		clay.setType(Material.LIGHT_BLUE_TERRACOTTA);
		glass.setType(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
	}

	private static ItemStack getRepairedItem(ItemStack item) {
    	ItemMeta meta = item.getItemMeta();
		Damageable damageable = (Damageable) meta;
		damageable.setDamage(0);
		item.setItemMeta(meta);
    	return item;
	}

	private static boolean isReparable(ItemStack item) {
		return item.getType().getMaxDurability() != 0;
	}

	private static void repairCommand(Player p, ItemStack main, ItemStack off) {
		boolean mainOkToRepair = isReparable(main);
		boolean offOkToRepair = isReparable(off);

		int cooldown = 360;

		if (p.hasPermission("rp.repair.special")) {
			cooldown = 15;
		} else if (p.hasPermission("rp.repair.diamond")) {
			cooldown = 30;
		} else if (p.hasPermission("rp.repair.emerald")) {
			cooldown = 45;
		}

		if (mainOkToRepair || offOkToRepair) {
			try {
				Plugin instance = RunicParadise.getInstance();
				MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
				Connection d = MySQL.openConnection();

				Statement dStmt = d.createStatement();
				ResultSet playerData = dStmt.executeQuery("SELECT * FROM `rp_RepairCommand` WHERE `UUID` = '"
						+ p.getUniqueId().toString() + "' AND `Timestamp` >= "
						+ (new Date().getTime() - (60000 * cooldown)) + " ORDER BY `ID` DESC LIMIT 1;");

				if (playerData.isBeforeFirst()) {
					playerData.next();
					Long currentTime = new Date().getTime();
					Long loggedTime = playerData.getLong("Timestamp");
					Double diff = (currentTime - loggedTime) / (60 * 1000.0);

					DecimalFormat df2 = new DecimalFormat("#,###,###,##0");

					p.sendMessage(ChatColor.RED + "You can only use this command once every " + cooldown
							+ " minutes. You last used it " + df2.format(diff) + " minutes ago.");

				} else {
					// No record found, proceed!

					if (mainOkToRepair) {
						String name = main.getItemMeta().hasDisplayName() ? main.getItemMeta().getDisplayName()
								: main.getType().toString().replace("_", " ").toLowerCase();
						main.setDurability(
								(short) ((main.getType().getMaxDurability()) - (main.getType().getMaxDurability())));
						RunicMessaging.sendMessage(p, RunicMessaging.RunicFormat.SYSTEM,
								ChatColor.ITALIC + "" + ChatColor.GREEN + "Your " + name + ChatColor.ITALIC + ""
										+ ChatColor.GREEN + " has been repaired!");
					}
					if (offOkToRepair) {
						String name = off.getItemMeta().hasDisplayName() ? off.getItemMeta().getDisplayName()
								: off.getType().toString().replace("_", " ").toLowerCase();
						off.setDurability(
								(short) ((off.getType().getMaxDurability()) - (off.getType().getMaxDurability())));
						RunicMessaging.sendMessage(p, RunicMessaging.RunicFormat.SYSTEM,
								ChatColor.ITALIC + "" + ChatColor.GREEN + "Your " + name + ChatColor.ITALIC + ""
										+ ChatColor.GREEN + " has been repaired!");
					}

					try {
						PreparedStatement insertStmt = d
								.prepareStatement("INSERT INTO rp_RepairCommand (PlayerName, UUID, Timestamp) VALUES "
										+ "('" + p.getName() + "', '" + p.getUniqueId().toString() + "', "
										+ (new Date().getTime()) + ");");
						insertStmt.executeUpdate();
						d.close();
						dStmt.close();

					} catch (SQLException e) {
						Bukkit.getLogger().log(Level.SEVERE, "Failed repair command record creation " + e.getMessage());
					}
				}

				d.close();
			} catch (SQLException e) {
				Bukkit.getLogger().log(Level.SEVERE, "Failed /rpfix check" + e.getMessage());
			}
		} else {
			p.sendMessage(ChatColor.DARK_RED
					+ "You don't have anything in your hands to be repaired. Put items you want repaired in both hands before using the command!");
		}
	}

	private static void rpTestCommand(CommandSender sender) {
    	if (!(sender instanceof Player)) {
    		sender.sendMessage("You can only do this in-game");
    		return;
		}
		Player player = (Player) sender;
    	Location location = player.getLocation();
    	World world = location.getWorld();
    	UUID uuid = player.getUniqueId();

		world.dropItemNaturally(location, Borderlands.specialLootDrops("BaronMetal", uuid));
		world.dropItemNaturally(location, Borderlands.specialLootDrops("BaronGem", uuid));

		world.dropItemNaturally(location, Borderlands.specialLootDrops("BaronIngot1", uuid));
		world.dropItemNaturally(location, Borderlands.specialLootDrops("BaronIngot2", uuid));

		world.dropItemNaturally(location, Borderlands.specialLootDrops("DukeGem", uuid));
		world.dropItemNaturally(location, Borderlands.specialLootDrops("DukeMetal", uuid));
		world.dropItemNaturally(location, Borderlands.specialLootDrops("DukeEssence", uuid));

		world.dropItemNaturally(location, Borderlands.specialLootDrops("DukeRing1", uuid));
		world.dropItemNaturally(location, Borderlands.specialLootDrops("DukeRing2", uuid));
		world.dropItemNaturally(location, Borderlands.specialLootDrops("DukeRing3", uuid));
		world.dropItemNaturally(location, Borderlands.specialLootDrops("DukeRing4", uuid));

		player.sendMessage(player.getMaximumAir() + " max air ticks. " + player.getRemainingAir() + " remaining air ticks.");

		/*
		 * sender.sendMessage(EntityType.ARROW.name() + " ... " +
		 * EntityType.HUSK.name());
		 *
		 * Player shooter = ((Player) sender);
		 *
		 * Location pTop = shooter.getLocation().add(0, 2, 0); Location
		 * pLeft = shooter.getLocation().add(1, 2, 0); Location pRight =
		 * shooter.getLocation().add(0, 2, 1);
		 *
		 * Location targetLoc = shooter.getTargetBlock((HashSet<Byte>) null,
		 * 256).getLocation();
		 *
		 * Vector vectorT = targetLoc.toVector().subtract(pTop.toVector());
		 * vectorT.normalize(); vectorT.multiply(2); Vector vectorL =
		 * targetLoc.toVector().subtract(pLeft.toVector());
		 * vectorL.normalize(); vectorL.multiply(2); Vector vectorR =
		 * targetLoc.toVector().subtract(pRight.toVector());
		 * vectorR.normalize(); vectorR.multiply(2);
		 *
		 * shooter.getWorld().spawnArrow(pTop, vectorT, 3,
		 * 3).setShooter(shooter); shooter.getWorld().spawnArrow(pLeft,
		 * vectorL, 3, 3).setShooter(shooter);
		 * shooter.getWorld().spawnArrow(pRight, vectorR, 3,
		 * 3).setShooter(shooter);
		 *
		 * if (args.length > 0) {
		 * sender.sendMessage("Totals stored for checks: " + ChatColor.GRAY
		 * + RunicParadise.playerProfiles.get(Bukkit.getPlayer(args[0]).
		 * getUniqueId()).mobKillCountsMap .toString());
		 * sender.sendMessage("Incrementals for DB save: " + ChatColor.GOLD
		 * + RunicParadise.mobKillTracker.get(Bukkit.getPlayer(args[0]).
		 * getUniqueId()).toString());
		 *
		 * }
		 */
	}

	private static void itemInfoCommandAdd(StringBuilder message, String description, String data) {
    	message.append(ChatColor.GOLD).append(description).append(ChatColor.GRAY).append(data).append("\n");
	}

	private static void itemInfoCommand(Player sender) {
    	ItemStack itemInHand = sender.getInventory().getItemInMainHand();

    	StringBuilder message = new StringBuilder();
		itemInfoCommandAdd(message, "Meta: ", RunicUtilities.toStringOr(itemInHand.getItemMeta(), "no item meta"));
		itemInfoCommandAdd(message, "Data: ", RunicUtilities.toStringOr(itemInHand.getData(), "no item data"));
		itemInfoCommandAdd(message, "Type: ", itemInHand.getType().toString());
		itemInfoCommandAdd(message, "Namespace: ", itemInHand.getType().getKey().toString());
		message.append(ChatColor.GOLD).append(
				Objects.requireNonNull(RunicSerialization.serializeTry(new ItemStack[]{itemInHand})).replace(ChatColor.COLOR_CHAR, '&'));
		sender.sendMessage(message.toString());
	}

	private static void freezeMob(Player near, boolean freeze) {
		for (Entity i : near.getNearbyEntities(2, 2, 2)) {
			if (i instanceof LivingEntity) {
				LivingEntity e = (LivingEntity) i;
				e.setAI(!freeze);
				e.setCollidable(!freeze);
				e.setRemoveWhenFarAway(!freeze);
				e.setInvulnerable(freeze);
			}
		}
	}

	private static String whoIsNearPlayer(Player p) {
		String result = p.getNearbyEntities(50, 50, 50)
				.stream()
				.filter(x -> x instanceof Player)
				.map(x -> (Player) x)
				.map(Player::getDisplayName)
				.collect(Collectors.joining(", "));
		String formatString = ChatColor.GRAY + "Players near %s" + ChatColor.GRAY + ": %s";
		return String.format(formatString, p.getDisplayName(), result.isEmpty() ? "None" : result);
	}
}
