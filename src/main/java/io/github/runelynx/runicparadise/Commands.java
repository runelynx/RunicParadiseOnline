package io.github.runelynx.runicparadise;

import com.connorlinfoot.titleapi.TitleAPI;
import com.xxmicloxx.NoteBlockAPI.NBSDecoder;
import com.xxmicloxx.NoteBlockAPI.RadioSongPlayer;
import com.xxmicloxx.NoteBlockAPI.Song;
import com.xxmicloxx.NoteBlockAPI.SongPlayer;
import io.github.runelynx.runicparadise.tempserialization.InventorySerialization;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;

import java.io.File;
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

	// pointer to your main class, not required if you don't need methods fromfg
	// the main class
	private Plugin instance = RunicParadise.getInstance();

	public static ArrayList<Integer> PARTICLE_TASK_IDS = new ArrayList<Integer>();

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
        final Plugin instance = RunicParadise.getInstance();
        MySQL MySQL = new MySQL(instance, instance.getConfig().getString("dbHost"),
                instance.getConfig().getString("dbPort"), instance.getConfig().getString("dbDatabase"),
                instance.getConfig().getString("dbUser"), instance.getConfig().getString("dbPassword"));
        try {
            final Connection d = MySQL.openConnection();
            Statement dStmt = d.createStatement();
            ResultSet explorerLocData = dStmt.executeQuery(
                    "SELECT * FROM rp_ExplorerLocations WHERE Status != 'Disabled' ORDER BY `Order` ASC;");
            // if (!playerData.first() && !playerData.next()) {
            if (!explorerLocData.isBeforeFirst()) {
                // No results
                // do nothing
                d.close();
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

                d.close();

                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        "sc " + locCount + " explorer locs loaded into memory!");
            }

        } catch (SQLException z) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed map sync for explorer locs cuz " + z.getMessage());
        }
        return true;
    }

    private void carnivalTokenCounts(Player player) {
        MySQL MySQL = new MySQL(instance, instance.getConfig().getString("dbHost"),
                instance.getConfig().getString("dbPort"), instance.getConfig().getString("dbDatabase"),
                instance.getConfig().getString("dbUser"), instance.getConfig().getString("dbPassword"));
        final Connection d = MySQL.openConnection();

        player.sendMessage(ChatColor.GRAY + "[RunicCarnival] Listing player token counts...");

        for (Player p : Bukkit.getOnlinePlayers()) {
            try {
                Statement dStmt = d.createStatement();
                ResultSet playerData = dStmt.executeQuery("SELECT * FROM `rp_PlayerInfo` WHERE `PlayerName` = '"
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
            d.close();
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed DB close for carnivalTokenCounts because: " + e.getMessage());
        }
    }

    private boolean addAttemptedPromotion(String newGuyName, String promoterName) {

        MySQL MySQL = new MySQL(instance, instance.getConfig().getString("dbHost"),
                instance.getConfig().getString("dbPort"), instance.getConfig().getString("dbDatabase"),
                instance.getConfig().getString("dbUser"), instance.getConfig().getString("dbPassword"));

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

        MySQL MySQL = new MySQL(instance, instance.getConfig().getString("dbHost"),
                instance.getConfig().getString("dbPort"), instance.getConfig().getString("dbDatabase"),
                instance.getConfig().getString("dbUser"), instance.getConfig().getString("dbPassword"));
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
        ItemStack[] items = i.getContents();

        int playerInvItemCount = 0;

        // Count actual itemstacks in player's inventory
        for (ItemStack item : items) {
            if ((item != null) && (item.getType() != Material.AIR)) {
                playerInvItemCount++;
            }
        }
        return playerInvItemCount;
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
		// comment

		MySQL MySQL = new MySQL(instance, instance.getConfig().getString("dbHost"),
				instance.getConfig().getString("dbPort"), instance.getConfig().getString("dbDatabase"),
				instance.getConfig().getString("dbUser"), instance.getConfig().getString("dbPassword"));

		// general approach is that errors will return immediately;
		// successful runs will return after the switch completes
		switch (cmd.getName()) {
			/* Removing the claim command with the intro of RESIDENCE plugin
		case "claim":

			Boolean showClaimHelp = false;

			try {
				if (args.length == 0) {
					showClaimHelp = true;
				} else if (Integer.parseInt(args[0]) >= 5 && Integer.parseInt(args[0]) <= 40) {
					int requestedRadius = Integer.parseInt(args[0]);

					Location claimCenterLoc = ((Player) sender).getLocation();
					RegionContainer container = RunicParadise.wgPlugin.getRegionContainer();
					RegionManager regions = container.get(claimCenterLoc.getWorld());
					// Check to make sure that "regions" is not null
					ApplicableRegionSet set = regions.getApplicableRegions(BukkitUtil.toVector(claimCenterLoc));

					int minX = claimCenterLoc.getBlockX() - requestedRadius;
					int minY = claimCenterLoc.getBlockY() - requestedRadius;
					int minZ = claimCenterLoc.getBlockZ() - requestedRadius;

					int maxX = claimCenterLoc.getBlockX() + requestedRadius;
					int maxY = claimCenterLoc.getBlockY() + requestedRadius;
					int maxZ = claimCenterLoc.getBlockZ() + requestedRadius;

					// claimCenterLoc.getWorld().playEffect(claimCenterLoc,
					// Particle.TOTEM, arg2);

					BlockVector min = new BlockVector(minX, minY, minZ);
					BlockVector max = new BlockVector(maxX, maxY, maxZ);
					ProtectedRegion test = new ProtectedCuboidRegion("dummy", min, max);
					ApplicableRegionSet ars = regions.getApplicableRegions(test);

					if (set.isVirtual() || set.size() == 0) {
						// no overlapping regions were found!
						// create the region!

						int numMod = new Random().nextInt(999) + 100;
						String newRegionName = sender.getName() + numMod;

						ProtectedRegion newRegion = new ProtectedCuboidRegion(newRegionName, min, max);
						RegionContainer rContainer = RunicParadise.wgPlugin.getRegionContainer();
						RegionManager rManager = rContainer.get(((Player) sender).getLocation().getWorld());
						rManager.addRegion(newRegion);

						DefaultDomain owners = newRegion.getOwners();
						owners.addPlayer(((Player) sender).getUniqueId());

						RunicMessaging.sendMessage(((Player) sender), RunicMessaging.RunicFormat.HELP,
								"Your protection is now set! Type " + ChatColor.GREEN + "/rg i" + ChatColor.GRAY
										+ " for details.");

					} else {
						// overlapping regions were found
						String overlappingRegions = "";
						for (ProtectedRegion pr : set.getRegions()) {
							overlappingRegions += ChatColor.GRAY + "[" + ChatColor.BLUE + pr.getId() + ChatColor.GRAY
									+ "] ";
						}

						RunicMessaging.sendMessage(((Player) sender), RunicMessaging.RunicFormat.HELP,
								"Your requested claim overlaps with these regions:");
						RunicMessaging.sendMessage(((Player) sender), RunicMessaging.RunicFormat.EMPTY,
								overlappingRegions);
						RunicMessaging.sendMessage(((Player) sender), RunicMessaging.RunicFormat.EMPTY,
								"Reduce the requested size or ask staff for help.");
					}
				} else {
					showClaimHelp = true;
				}
			} catch (NumberFormatException nfe) {
				// bad command format
				showClaimHelp = true;
			}

			if (showClaimHelp) {
				RunicMessaging.sendMessage(((Player) sender), RunicMessaging.RunicFormat.HELP, "Use " + ChatColor.YELLOW
						+ "/claim X" + ChatColor.GRAY + " to create a protected area around you.");

				RunicMessaging.sendMessage(((Player) sender), RunicMessaging.RunicFormat.EMPTY,
						"Replace X with how many blocks in each direction you want the protection to extend.");
				RunicMessaging.sendMessage(((Player) sender), RunicMessaging.RunicFormat.EMPTY,
						"Example: " + ChatColor.GOLD + "/claim 20" + ChatColor.GRAY
								+ " creates a protected area 20 blocks in each direction from where you stand.");
				RunicMessaging.sendMessage(((Player) sender), RunicMessaging.RunicFormat.EMPTY,
						"You cannot create overlapping regions with this command, and the size must be between 5 and 40.");
			}

			break;   --- END claim command  */
		case "fixranks":
			RunicUtilities.fixGroupManager();
			break;
		case "rpfix":
			Player playerA = ((Player) sender);
			repairCommand(playerA, playerA.getInventory().getItemInMainHand(),
					playerA.getInventory().getItemInOffHand());
			break;
		case "runicspawntravel":
				spawnTransportBeacon(((Player) sender).getLocation(), ((Player) sender));
			break;
		case "sendentity":
			Player q = ((Player) sender);

			for (Entity e : q.getNearbyEntities(2, 2, 2)) {
				if (e instanceof LivingEntity) {
					e.teleport(Bukkit.getPlayer(args[0]));
				}
			}
			break;
		case "raffle":
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

			break;
		case "search":
		case "explore":
			Player explorePlayer = ((Player) sender);
			searchExplorerLocation(explorePlayer.getLocation(), explorePlayer);

			break;
		case "el":
			if (args != null && args.length == 4 && args[0].equalsIgnoreCase("create") && Integer.parseInt(args[2]) > -1
					&& Integer.parseInt(args[3]) > 0) {
				// CREATE NEW LOCATION

				try {
					Bukkit.getLogger().log(Level.INFO, "Creating new explorer's league location: " + args[0]
							+ ", Tokens: " + args[2] + ", Proximity: " + args[3] + ", Creator: " + sender.getName());

					String locationName = args[1].replace('_', ' ').replace("'", "");
					String locString = ((Player) sender).getWorld().getName() + "."
							+ (int) ((Player) sender).getLocation().getX() + "."
							+ (int) ((Player) sender).getLocation().getY() + "."
							+ (int) ((Player) sender).getLocation().getZ();
					int tokenReward = Integer.parseInt(args[2]);
					int difficultyRadius = Integer.parseInt(args[3]);

					final Connection d = MySQL.openConnection();
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
                sender
						.sendMessage(ChatColor.RED + "Create new spot. el create [name] [tokens] [req'd proximity]");
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

			break;
		case "carnivalaxe":
			((Player) sender).getInventory().addItem(Recipes.customItemStacks("FAITH_AXE_1"));
			break;
		case "faithweapons":
			((Player) sender).getInventory().addItem(Recipes.customItemStacks("FAITH_AXE_1"));
			((Player) sender).getInventory().addItem(Recipes.customItemStacks("FAITH_SWORD_1"));
			((Player) sender).getInventory().addItem(Recipes.customItemStacks("FAITH_SWORD_2"));
			((Player) sender).getInventory().addItem(Recipes.customItemStacks("FAITH_SWORD_3"));
			break;
		case "faithweapon":
		 if (args != null && args.length == 2) {
			 if (args[0].equalsIgnoreCase("sword1")) {
				 Bukkit.getPlayer(args[1]).getInventory().addItem(Recipes.customItemStacks("FAITH_SWORD_1"));
			 } else if (args[0].equalsIgnoreCase("sword2")) {
				 Bukkit.getPlayer(args[1]).getInventory().addItem(Recipes.customItemStacks("FAITH_SWORD_2"));
			 } else if (args[0].equalsIgnoreCase("sword3")) {
				 Bukkit.getPlayer(args[1]).getInventory().addItem(Recipes.customItemStacks("FAITH_SWORD_3"));
			 } else if (args[0].equalsIgnoreCase("axe1")) {
				 Bukkit.getPlayer(args[1]).getInventory().addItem(Recipes.customItemStacks("FAITH_AXE_1"));
			 }
		 }
			break;
		case "casinotoken":
			break;
		case "casino":
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

			break;
		case "freezemob":
			freezeMob((Player) sender, true);
			break;
		case "unfreezemob":
			freezeMob((Player) sender, false);
			break;
		case "wild":
			((Player) sender).teleport(
					new Location(Bukkit.getWorld("RunicSky"), -493.195, 64.50, 302.930, 212.86743F, -1.3499908F));
			sender.sendMessage(ChatColor.YELLOW
					+ "There are portals to different areas of the wilderness here - look for a biome you like and head into the portal.");
			sender.sendMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "Borderlands" + ChatColor.RESET + ""
					+ ChatColor.YELLOW + " areas have VERY tough monsters!");
			break;
		case "miningworld":
		case "mw":
			((Player) sender).teleport(
					new Location(Bukkit.getWorld("RunicSky"), -639.232, 64.0, 326.465, 93.31604F, -4.499901F));
			sender.sendMessage(ChatColor.YELLOW
					+ "The mining world portal is ahead of you. That world resets sometimes so do not build or leave any items or graves there or you risk losing them!");
			sender.sendMessage(ChatColor.YELLOW + "Explosions break blocks in the mining world.");
			break;
		case "iteminfo":
			itemInfoCommand((Player) sender);
			break;
		case "miningreset":
			Bukkit.getServer().getScheduler().scheduleAsyncDelayedTask(instance, new Runnable() {
				public void run() {
//					Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
//							"essentials:bc Mining world is resetting in 2 minutes!");
					Bukkit.broadcast("Mining world is resetting in 2 minutes!", "*");
				}
			}, 20);

			Random rand = new Random();
			final int tempRand = rand.nextInt(5001);

			Bukkit.getServer().getScheduler().scheduleAsyncDelayedTask(instance, new Runnable() {
				public void run() {
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv load Mining");
					sender.sendMessage("Sent command to load Mining world");
				}
			}, 2200);

			Bukkit.getServer().getScheduler().scheduleAsyncDelayedTask(instance, new Runnable() {
				public void run() {
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mvconfirm");
					sender.sendMessage("Confirmed load command");
				}
			}, 2250);

			Bukkit.getServer().getScheduler().scheduleAsyncDelayedTask(instance, new Runnable() {
				public void run() {
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv regen Mining -s " + tempRand);
					sender.sendMessage("Sent command to regen Mining world");
					sender.sendMessage("Used: regen Mining -s " + tempRand);
				}
			}, 2400);

			Bukkit.getServer().getScheduler().scheduleAsyncDelayedTask(instance, new Runnable() {
				public void run() {
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mvconfirm");
					sender.sendMessage("Confirmed regen command");
				}
			}, 2475);

			Bukkit.getServer().getScheduler().scheduleAsyncDelayedTask(instance, new Runnable() {
				public void run() {
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
							"essentials:bc Mining world reset is now complete!");
				}
			}, 2599);

			Bukkit.getServer().getScheduler().scheduleAsyncDelayedTask(instance, new Runnable() {
				public void run() {
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv load Mining");
					sender.sendMessage("Sent command to load Mining world");
				}
			}, 2600);

			Bukkit.getServer().getScheduler().scheduleAsyncDelayedTask(instance, new Runnable() {
				public void run() {
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mvconfirm");
					sender.sendMessage("Confirmed load command");
				}
			}, 2640);

			Bukkit.getServer().getScheduler().scheduleAsyncDelayedTask(instance, new Runnable() {
				public void run() {
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mvrule keepInventory true Mining");
				}
			}, 2700);

			break;
		case "miningworldreminder":
			for (Player p : Bukkit.getWorld("Mining").getPlayers()) {
				p.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC
						+ "Mining world resets every day! Don't leave anything here; items or graves!");
			}
			break;
		case "gift":
			if (args.length == 0) {
				sender.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "Welcome to the Christmas Gift Exchange!");
				sender.sendMessage(
						ChatColor.GRAY + "*DISABLED* To send a gift, put an item in your hand and /gift send <name>");
				sender.sendMessage(ChatColor.GRAY + "Be sure to type full player names, use tab complete if possible!");
				sender.sendMessage(ChatColor.GRAY + "To claim a gift, type /gift check");

			} else if (args.length == 1 && args[0].equalsIgnoreCase("check")) {

				new Gift((Player) sender);

			} else if (args.length == 2 && args[0].equalsIgnoreCase("send")) {

				sender.sendMessage(ChatColor.DARK_RED + "" + ChatColor.ITALIC + "Sending gifts has been disabled.");
				return true;
				/*
				 * if (((Player) sender).getItemInHand().equals(null) ||
				 * ((Player) sender).getItemInHand().getTypeId() == 0) {
				 * sender.sendMessage(ChatColor.DARK_RED + "" + ChatColor.ITALIC
				 * +
				 * "You must have the gift in your hand to use that command.");
				 * return true; }
                 *
				 * final Connection e = MySQL.openConnection();
                 *
				 * // ///////////// try { // Statement eStmt =
				 * e.createStatement();
                 *
				 * PreparedStatement eStmt = e .prepareStatement(
				 * "SELECT UUID FROM rp_PlayerInfo WHERE PlayerName = '" +
				 * args[1] + "' LIMIT 1;");
                 *
				 * ResultSet result = eStmt.executeQuery();
                 *
				 * if (!result.isBeforeFirst()) { // no result found
				 * sender.sendMessage(ChatColor.ITALIC + "" + ChatColor.DARK_RED
				 * + "No player found with that name. Please use full names.");
				 * } else { // Location does exist in the DB and data
				 * retrieved!!
                 *
				 * result.next();
                 *
				 * new Gift((Player) sender, UUID.fromString(result
				 * .getString("UUID")));
                 *
				 * Bukkit.getServer().dispatchCommand(
				 * Bukkit.getServer().getConsoleSender(), "mail send " + args[1]
				 * + " You received a gift from " + sender.getName() +
				 * ". Use /gift to claim it!"); }
                 *
				 * } catch (SQLException err) { Bukkit.Bukkit.getLogger()
				 * .log(Level.SEVERE, "Cant check gift Gift because: " +
				 * err.getMessage()); }
				 */
			} else {
				sender.sendMessage(ChatColor.DARK_RED + "" + ChatColor.ITALIC
						+ "Hmm that didn't work. Try /gift send <player> or /gift check.");
			}

			break;
		case "machinemaze":
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
			break;
		case "crocomaze":
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
			break;

		case "adventureparkourprize":

			RunicParadise.playerProfiles.get(Bukkit.getPlayer(args[0]).getUniqueId()).addMazeCompletion(7);

			break;
		case "junglemaze":

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
								return true;
							}

							int cpCounter = 1;
							String build1 = "";
							String build2 = "";

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
									build1 += ChatColor.DARK_GRAY + "[" + color + cpCounter + ChatColor.DARK_GRAY + "]";
								} else {
									build2 += ChatColor.DARK_GRAY + "[" + color + cpCounter + ChatColor.DARK_GRAY
											+ "] ";

								}

								cpCounter++;
							}
							String completeMessage = "";

							if (completeCP < 24) {
								int remaining = 24 - completeCP;
								completeMessage = ChatColor.GOLD + "You still need " + ChatColor.RED + remaining
										+ ChatColor.GOLD + " more checkpoints.";
							} else {
								completeMessage = ChatColor.DARK_RED
										+ "Congratulations! You've completed the jungle maze!.";

								for (Player p : Bukkit.getOnlinePlayers()) {
									TitleAPI.sendTitle(p, 2, 3, 2, ChatColor.DARK_GRAY + "" + ChatColor.BOLD
											+ Bukkit.getPlayer(args[1]).getDisplayName(),

											ChatColor.AQUA + "just completed the jungle maze!");
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

							Bukkit.getPlayer(args[1])
									.sendMessage(new String[] { ChatColor.GRAY + "Your jungle maze completion status:",
											build1, build2, completeMessage });

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
								return true;
							}

							int cpCounter = 1;
							String build1 = "";
							String build2 = "";

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
									build1 += ChatColor.DARK_GRAY + " [" + color + cpCounter + ChatColor.DARK_GRAY
											+ "]";
								} else {
									build2 += ChatColor.DARK_GRAY + " [" + color + cpCounter + ChatColor.DARK_GRAY
											+ "]";
								}

								cpCounter++;
							}
							String completeMessage = "";

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
											build1, build2, completeMessage });

						}
					} catch (SQLException e) {
						Bukkit.getLogger().log(Level.SEVERE, "Failed junglemaze record creation " + e.getMessage());
					}

				}
			}

			break;

		case "voice":
		case "discord":
			sender.sendMessage(ChatColor.LIGHT_PURPLE + "We use Discord as our voice chat system. ");
			sender.sendMessage(ChatColor.LIGHT_PURPLE
					+ "Remember our server rules still apply there! Be respectful to others and keep it clean!");
			sender.sendMessage(ChatColor.DARK_RED + "Click here to learn how to use Discord: " + ChatColor.GRAY
					+ "http://goo.gl/X1dg8W");
			sender.sendMessage(ChatColor.DARK_RED + "Click here to join Discord: " + ChatColor.GRAY
					+ "http://www.runic-paradise.com/discord.php");
			break;
		case "dailykarma":
			try {

				Date date = new Date();
				// get time 24 hours ago
				long timeCheck = date.getTime() - 86400000;

				final Connection d = MySQL.openConnection();
				Statement dStmt = d.createStatement();
				ResultSet playerData = dStmt.executeQuery("SELECT * FROM rp_PlayerInfo WHERE LastSeen > " + timeCheck
						+ " AND KillZombie > 1 ORDER BY RAND() LIMIT 1;");
				if (!playerData.isBeforeFirst()) {
					// No record returned

				} else {
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

			break;
		case "oldrankperks":
			if (args.length != 1) {
				sender.sendMessage(ChatColor.DARK_RED
						+ "Use this command to restore the perks you had from your old highest rank. So if you used to be guardian, type /oldrankperks Guardian. If you used to be Architect, type /oldrankperks Architect. You will not go back to those rank names in chat- but you will regain the perks!");
				return true;
			}

			try {

				final Connection dbCon = MySQL.openConnection();
				Statement dbStmt = dbCon.createStatement();
				ResultSet powerResult = dbStmt.executeQuery("SELECT * FROM rp_PlayerPromotions WHERE PlayerName='"
                        + sender.getName() + "' AND NewRank='" + args[0] + "' LIMIT 1;");
				if (!powerResult.isBeforeFirst()) {
					// No results
					// do nothing
					sender.sendMessage(ChatColor.DARK_RED
							+ "Sorry, couldn't find any record of you having that rank. Be sure you type it EXACTLY right!! "
							+ ChatColor.RED + "Settler, Explorer, Builder, Architect, Warden, Protector, Guardian");
					Bukkit.getLogger().log(Level.INFO,
							"Player " + sender.getName() + " tried to get the perks of " + args[0] + " and failed.");
					dbCon.close();
				} else {
					// results found!
					while (powerResult.next()) {
						// Found powers for a faith that player has
						// equipped!
						Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
								"manuaddsub " + sender.getName() + " " + args[0]);
						sender.sendMessage(ChatColor.GREEN + "You now have the same perks as the " + ChatColor.AQUA
								+ " " + args[0] + ChatColor.GREEN
								+ " rank. Just the perks! You're still on the new rank system. :)");
						Bukkit.getLogger().log(Level.INFO, "Gave " + sender.getName() + " the perks of " + args[0]);
					}
					dbStmt.close();
					dbCon.close();
				}

			} catch (SQLException z) {
				Bukkit.getLogger().log(Level.SEVERE,
						"Failed Faith.listPowers when trying to get powers for a faith: " + z.getMessage());
				sender.sendMessage("Database failure.");
				return true;
			}
			break;
		case "cactifever":
			// TODO: needs fixing
			ItemStack skull = new ItemStack(Material.PLAYER_HEAD, 1, (short) 3);
			SkullMeta meta1 = (SkullMeta) skull.getItemMeta();

			meta1.setOwner("The_King_Cacti");
			meta1.setDisplayName(ChatColor.AQUA + "" + "The_King_Cacti");
			skull.setItemMeta(meta1);
			((Player) sender).getWorld().dropItemNaturally(((Player) sender).getLocation(), skull);
            sender
					.sendMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + "You've been infected with CactiFever!");
			break;
		case "runiceye":
			RunicParadise.loadRunicEyes();
			break;
		case "faith":
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

			break;
		case "rpjobs":
			// Master a tier1 job
			if (args[0].equals("master") && args.length == 2 && !(sender instanceof Player)) {
				RunicPlayerBukkit targetPlayer = new RunicPlayerBukkit(args[1]);
				if (targetPlayer.getCurrentJob().equals("None")) {
					targetPlayer.sendMessageToPlayer(
							ChatColor.GREEN + "You don't have a job. Get a job and get level 25 in it.");
					return false;
				} else {
					if (targetPlayer.getMasteredJobs().contains(targetPlayer.getCurrentJob())) {
						targetPlayer.sendMessageToPlayer(ChatColor.GREEN + "You already mastered this job.");
						return false;
					}
				}

				if (targetPlayer.getCurrentJobLevel() >= 25) {
					// Player has sufficient level in a tier 1 job
					if (targetPlayer.executeJobMastery()) {
						// Execution succeeded!
						targetPlayer.sendMessageToPlayer(
								ChatColor.GREEN + "Success! You have now mastered the following jobs:");
						targetPlayer.sendMessageToPlayer(ChatColor.GRAY + targetPlayer.getMasteredJobs());
					} else {
						// Execution failed!
						targetPlayer.sendMessageToPlayer(
								ChatColor.GREEN + "Error! Something went wrong, please ask an admin for help.");
					}

				} else {
					targetPlayer.sendMessageToPlayer(ChatColor.GRAY + "" + ChatColor.ITALIC
							+ "You must have level 25 in a job to achieve mastery.");
				}
			} else if (args[0].equals("qualifyt4") && args.length == 2 && !(sender instanceof Player)) {
				// Qualify for a tier4 job
				RunicPlayerBukkit targetPlayer = new RunicPlayerBukkit(args[1]);
				boolean showFail = true;

				if (targetPlayer.getMasteredJobCount() > 0 && !targetPlayer.checkPlayerPermission("rp.level.master")) {
					targetPlayer.sendMessageToPlayer(
							ChatColor.YELLOW + "[RunicRanks] Your previous masteries are now visible to Runic Ranks!");

					RunicParadise.perms.playerAdd(((Player) sender), "rp.level.master");
				}

				targetPlayer.sendMessageToPlayer(ChatColor.YELLOW + "[RunicRanks] You have mastered these jobs: "
						+ ChatColor.GOLD + targetPlayer.getMasteredJobs());

				int masteredJobsNeededForTier4 = 21;
				int masteredJobCount = 0;
				String jobTally = "";

				if (!targetPlayer.getMasteredJobs().contains("Wizard")) {
					jobTally += ChatColor.DARK_GRAY + "[" + ChatColor.DARK_RED + "Wizard" + ChatColor.DARK_GRAY + "]";
				} else {
					jobTally += ChatColor.DARK_GRAY + "[" + ChatColor.GREEN + "Wizard" + ChatColor.DARK_GRAY + "]";
					masteredJobCount++;
				}

				if (!targetPlayer.getMasteredJobs().contains("Scientist")) {
					jobTally += ChatColor.DARK_GRAY + "[" + ChatColor.DARK_RED + "Scientist" + ChatColor.DARK_GRAY
							+ "]";
				} else {
					jobTally += ChatColor.DARK_GRAY + "[" + ChatColor.GREEN + "Scientist" + ChatColor.DARK_GRAY + "]";
					masteredJobCount++;
				}

				if (!targetPlayer.getMasteredJobs().contains("Miner")) {
					jobTally += ChatColor.DARK_GRAY + "[" + ChatColor.DARK_RED + "Miner" + ChatColor.DARK_GRAY + "]";
				} else {
					jobTally += ChatColor.DARK_GRAY + "[" + ChatColor.GREEN + "Miner" + ChatColor.DARK_GRAY + "]";
					masteredJobCount++;
				}

				if (!targetPlayer.getMasteredJobs().contains("Chef")) {
					jobTally += ChatColor.DARK_GRAY + "[" + ChatColor.DARK_RED + "Chef" + ChatColor.DARK_GRAY + "]";
				} else {
					jobTally += ChatColor.DARK_GRAY + "[" + ChatColor.GREEN + "Chef" + ChatColor.DARK_GRAY + "]";
					masteredJobCount++;
				}

				if (!targetPlayer.getMasteredJobs().contains("Rancher")) {
					jobTally += ChatColor.DARK_GRAY + "[" + ChatColor.DARK_RED + "Rancher" + ChatColor.DARK_GRAY + "]";
				} else {
					jobTally += ChatColor.DARK_GRAY + "[" + ChatColor.GREEN + "Rancher" + ChatColor.DARK_GRAY + "]";
					masteredJobCount++;
				}

				if (!targetPlayer.getMasteredJobs().contains("Blacksmith")) {
					jobTally += ChatColor.DARK_GRAY + "[" + ChatColor.DARK_RED + "Blacksmith" + ChatColor.DARK_GRAY
							+ "]";
				} else {
					jobTally += ChatColor.DARK_GRAY + "[" + ChatColor.GREEN + "Blacksmith" + ChatColor.DARK_GRAY + "]";
					masteredJobCount++;
				}

				if (!targetPlayer.getMasteredJobs().contains("Woodsman")) {
					jobTally += ChatColor.DARK_GRAY + "[" + ChatColor.DARK_RED + "Woodsman" + ChatColor.DARK_GRAY + "]";
				} else {
					jobTally += ChatColor.DARK_GRAY + "[" + ChatColor.GREEN + "Woodsman" + ChatColor.DARK_GRAY + "]";
					masteredJobCount++;
				}

				if (!targetPlayer.getMasteredJobs().contains("Druid")) {
					jobTally += ChatColor.DARK_GRAY + "[" + ChatColor.DARK_RED + "Druid" + ChatColor.DARK_GRAY + "]";
				} else {
					jobTally += ChatColor.DARK_GRAY + "[" + ChatColor.GREEN + "Druid" + ChatColor.DARK_GRAY + "]";
					masteredJobCount++;
				}

				if (!targetPlayer.getMasteredJobs().contains("Engineer")) {
					jobTally += ChatColor.DARK_GRAY + "[" + ChatColor.DARK_RED + "Engineer" + ChatColor.DARK_GRAY + "]";
				} else {
					jobTally += ChatColor.DARK_GRAY + "[" + ChatColor.GREEN + "Engineer" + ChatColor.DARK_GRAY + "]";
					masteredJobCount++;
				}

				if (!targetPlayer.getMasteredJobs().contains("Conjurer")) {
					jobTally += ChatColor.DARK_GRAY + "[" + ChatColor.DARK_RED + "Conjurer" + ChatColor.DARK_GRAY + "]";
				} else {
					jobTally += ChatColor.DARK_GRAY + "[" + ChatColor.GREEN + "Conjurer" + ChatColor.DARK_GRAY + "]";
					masteredJobCount++;
				}

				if (!targetPlayer.getMasteredJobs().contains("Geomancer")) {
					jobTally += ChatColor.DARK_GRAY + "[" + ChatColor.DARK_RED + "Geomancer" + ChatColor.DARK_GRAY
							+ "]";
				} else {
					jobTally += ChatColor.DARK_GRAY + "[" + ChatColor.GREEN + "Geomancer" + ChatColor.DARK_GRAY + "]";
					masteredJobCount++;
				}

				if (!targetPlayer.getMasteredJobs().contains("Nomad")) {
					jobTally += ChatColor.DARK_GRAY + "[" + ChatColor.DARK_RED + "Nomad" + ChatColor.DARK_GRAY + "]";
				} else {
					jobTally += ChatColor.DARK_GRAY + "[" + ChatColor.GREEN + "Nomad" + ChatColor.DARK_GRAY + "]";
					masteredJobCount++;
				}

				if (!targetPlayer.getMasteredJobs().contains("Alchemist")) {
					jobTally += ChatColor.DARK_GRAY + "[" + ChatColor.DARK_RED + "Alchemist" + ChatColor.DARK_GRAY
							+ "]";
				} else {
					jobTally += ChatColor.DARK_GRAY + "[" + ChatColor.GREEN + "Alchemist" + ChatColor.DARK_GRAY + "]";
					masteredJobCount++;
				}

				if (!targetPlayer.getMasteredJobs().contains("Biologist")) {
					jobTally += ChatColor.DARK_GRAY + "[" + ChatColor.DARK_RED + "Biologist" + ChatColor.DARK_GRAY
							+ "]";
				} else {
					jobTally += ChatColor.DARK_GRAY + "[" + ChatColor.GREEN + "Biologist" + ChatColor.DARK_GRAY + "]";
					masteredJobCount++;
				}

				if (!targetPlayer.getMasteredJobs().contains("Forgemaster")) {
					jobTally += ChatColor.DARK_GRAY + "[" + ChatColor.DARK_RED + "Forgemaster" + ChatColor.DARK_GRAY
							+ "]";
				} else {
					jobTally += ChatColor.DARK_GRAY + "[" + ChatColor.GREEN + "Forgemaster" + ChatColor.DARK_GRAY + "]";
					masteredJobCount++;
				}

				if (!targetPlayer.getMasteredJobs().contains("Ranger")) {
					jobTally += ChatColor.DARK_GRAY + "[" + ChatColor.DARK_RED + "Ranger" + ChatColor.DARK_GRAY + "]";
				} else {
					jobTally += ChatColor.DARK_GRAY + "[" + ChatColor.GREEN + "Ranger" + ChatColor.DARK_GRAY + "]";
					masteredJobCount++;
				}

				if (!targetPlayer.getMasteredJobs().contains("Tamer")) {
					jobTally += ChatColor.DARK_GRAY + "[" + ChatColor.DARK_RED + "Tamer" + ChatColor.DARK_GRAY + "]";
				} else {
					jobTally += ChatColor.DARK_GRAY + "[" + ChatColor.GREEN + "Tamer" + ChatColor.DARK_GRAY + "]";
					masteredJobCount++;
				}

				if (!targetPlayer.getMasteredJobs().contains("Beastmaster")) {
					jobTally += ChatColor.DARK_GRAY + "[" + ChatColor.DARK_RED + "Beastmaster" + ChatColor.DARK_GRAY
							+ "]";
				} else {
					jobTally += ChatColor.DARK_GRAY + "[" + ChatColor.GREEN + "Beastmaster" + ChatColor.DARK_GRAY + "]";
					masteredJobCount++;
				}

				if (!targetPlayer.getMasteredJobs().contains("Sorcerer")) {
					jobTally += ChatColor.DARK_GRAY + "[" + ChatColor.DARK_RED + "Sorcerer" + ChatColor.DARK_GRAY + "]";
				} else {
					jobTally += ChatColor.DARK_GRAY + "[" + ChatColor.GREEN + "Sorcerer" + ChatColor.DARK_GRAY + "]";
					masteredJobCount++;
				}

				if (!targetPlayer.getMasteredJobs().contains("Geneticist")) {
					jobTally += ChatColor.DARK_GRAY + "[" + ChatColor.DARK_RED + "Geneticist" + ChatColor.DARK_GRAY
							+ "]";
				} else {
					jobTally += ChatColor.DARK_GRAY + "[" + ChatColor.GREEN + "Geneticist" + ChatColor.DARK_GRAY + "]";
					masteredJobCount++;
				}

				if (!targetPlayer.getMasteredJobs().contains("Artificer")) {
					jobTally += ChatColor.DARK_GRAY + "[" + ChatColor.DARK_RED + "Artificer" + ChatColor.DARK_GRAY
							+ "]";
				} else {
					jobTally += ChatColor.DARK_GRAY + "[" + ChatColor.GREEN + "Artificer" + ChatColor.DARK_GRAY + "]";
					masteredJobCount++;
				}

				// FAILURE
				if (masteredJobCount < masteredJobsNeededForTier4) {
					targetPlayer.sendMessageToPlayer(ChatColor.RED
							+ "You don't qualify for tier 4 jobs. You must master ALL lower tier jobs first!");
					targetPlayer.sendMessageToPlayer(jobTally);
				} else {
					// SUCCESS
					RunicParadise.perms.playerAdd(Bukkit.getPlayer(args[1]), "jobs.join.craftsman");
					RunicParadise.perms.playerAdd(Bukkit.getPlayer(args[1]), "jobs.join.seafarer");
					RunicParadise.perms.playerAdd(Bukkit.getPlayer(args[1]), "jobs.join.builder");
					RunicParadise.perms.playerAdd(Bukkit.getPlayer(args[1]), "jobs.join.wanderer");
					targetPlayer.sendMessageToPlayer(ChatColor.GREEN + "You qualify for tier 4 jobs!");
				}

			} else if (args[0].equals("qualify") && args.length == 2 && !(sender instanceof Player)) {
				// Qualify for a tier2 job
				RunicPlayerBukkit targetPlayer = new RunicPlayerBukkit(args[1]);
				boolean showFail = true;

				if (targetPlayer.getMasteredJobCount() > 0 && !targetPlayer.checkPlayerPermission("rp.level.master")) {
					targetPlayer.sendMessageToPlayer(
							ChatColor.YELLOW + "[RunicRanks] Your previous masteries are now visible to Runic Ranks!");

					RunicParadise.perms.playerAdd(Bukkit.getPlayer(args[1]), "rp.level.master");
				}

				targetPlayer.sendMessageToPlayer(ChatColor.YELLOW + "[RunicRanks] You have mastered these jobs: "
						+ ChatColor.GOLD + targetPlayer.getMasteredJobs());

				// BEASTMASTER
				if (targetPlayer.getMasteredJobs().contains("Druid") && targetPlayer.getMasteredJobs().contains("Tamer")
						&& targetPlayer.getMasteredJobs().contains("Nomad")) {
					RunicParadise.perms.playerAdd(Bukkit.getPlayer(args[1]), "jobs.join.beastmaster");
					targetPlayer.sendMessageToPlayer(
							ChatColor.GREEN + "You qualify to become a " + ChatColor.DARK_GREEN + "BEASTMASTER");
					showFail = false;
				}
				// SORCEROR
				if (targetPlayer.getMasteredJobs().contains("Alchemist")
						&& targetPlayer.getMasteredJobs().contains("Geomancer")
						&& targetPlayer.getMasteredJobs().contains("Conjurer")) {
					RunicParadise.perms.playerAdd(Bukkit.getPlayer(args[1]), "jobs.join.sorcerer");
					targetPlayer.sendMessageToPlayer(
							ChatColor.GREEN + "You qualify to become a " + ChatColor.DARK_GREEN + "SORCERER");
					showFail = false;
				}
				// GENETICIST
				if (targetPlayer.getMasteredJobs().contains("Ranger")
						&& targetPlayer.getMasteredJobs().contains("Nomad")
						&& targetPlayer.getMasteredJobs().contains("Biologist")) {
					RunicParadise.perms.playerAdd(Bukkit.getPlayer(args[1]), "jobs.join.geneticist");
					targetPlayer.sendMessageToPlayer(
							ChatColor.GREEN + "You qualify to become a " + ChatColor.DARK_GREEN + "GENETICIST");
					showFail = false;
				}
				// ARTIFICER
				if (targetPlayer.getMasteredJobs().contains("Engineer")
						&& targetPlayer.getMasteredJobs().contains("Forgemaster")
						&& targetPlayer.getMasteredJobs().contains("Geomancer")) {
					RunicParadise.perms.playerAdd(Bukkit.getPlayer(args[1]), "jobs.join.artificer");
					targetPlayer.sendMessageToPlayer(
							ChatColor.GREEN + "You qualify to become a " + ChatColor.DARK_GREEN + "ARTIFICER");
					showFail = false;
				}

				// RANGER
				if (targetPlayer.getMasteredJobs().contains("Woodsman")
						&& targetPlayer.getMasteredJobs().contains("Rancher")) {
					RunicParadise.perms.playerAdd(Bukkit.getPlayer(args[1]), "jobs.join.ranger");
					targetPlayer.sendMessageToPlayer(
							ChatColor.GREEN + "You qualify to become a " + ChatColor.DARK_GREEN + "RANGER");
					showFail = false;
				}

				// FORGEMASTER
				if (targetPlayer.getMasteredJobs().contains("Blacksmith")
						&& targetPlayer.getMasteredJobs().contains("Miner")) {
					RunicParadise.perms.playerAdd(Bukkit.getPlayer(args[1]), "jobs.join.forgemaster");
					targetPlayer.sendMessageToPlayer(
							ChatColor.GREEN + "You qualify to become a " + ChatColor.DARK_GREEN + "FORGEMASTER");
					showFail = false;
				}

				// BIOLOGIST
				if (targetPlayer.getMasteredJobs().contains("Scientist")
						&& targetPlayer.getMasteredJobs().contains("Rancher")) {
					RunicParadise.perms.playerAdd(Bukkit.getPlayer(args[1]), "jobs.join.biologist");
					targetPlayer.sendMessageToPlayer(
							ChatColor.GREEN + "You qualify to become a " + ChatColor.DARK_GREEN + "BIOLOGIST");
					showFail = false;
				}

				// ALCHEMIST
				if (targetPlayer.getMasteredJobs().contains("Wizard")
						&& targetPlayer.getMasteredJobs().contains("Chef")) {
					RunicParadise.perms.playerAdd(Bukkit.getPlayer(args[1]), "jobs.join.alchemist");
					targetPlayer.sendMessageToPlayer(
							ChatColor.GREEN + "You qualify to become a " + ChatColor.DARK_GREEN + "ALCHEMIST");
					showFail = false;
				}

				// NOMAD
				if (targetPlayer.getMasteredJobs().contains("Chef")
						&& targetPlayer.getMasteredJobs().contains("Rancher")) {
					RunicParadise.perms.playerAdd(Bukkit.getPlayer(args[1]), "jobs.join.nomad");
					targetPlayer.sendMessageToPlayer(
							ChatColor.GREEN + "You qualify to become a " + ChatColor.DARK_GREEN + "NOMAD");
					showFail = false;
				}

				// GEOMANCER
				if (targetPlayer.getMasteredJobs().contains("Wizard")
						&& targetPlayer.getMasteredJobs().contains("Miner")) {
					RunicParadise.perms.playerAdd(Bukkit.getPlayer(args[1]), "jobs.join.geomancer");
					targetPlayer.sendMessageToPlayer(
							ChatColor.GREEN + "You qualify to become a " + ChatColor.DARK_GREEN + "GEOMANCER");
					showFail = false;
				}

				// CONJURER
				if (targetPlayer.getMasteredJobs().contains("Blacksmith")
						&& targetPlayer.getMasteredJobs().contains("Wizard")) {
					RunicParadise.perms.playerAdd(Bukkit.getPlayer(args[1]), "jobs.join.conjurer");
					targetPlayer.sendMessageToPlayer(
							ChatColor.GREEN + "You qualify to become a " + ChatColor.DARK_GREEN + "CONJURER");
					showFail = false;
				}

				// DRUID
				if (targetPlayer.getMasteredJobs().contains("Wizard")
						&& targetPlayer.getMasteredJobs().contains("Woodsman")) {
					RunicParadise.perms.playerAdd(Bukkit.getPlayer(args[1]), "jobs.join.druid");
					targetPlayer.sendMessageToPlayer(
							ChatColor.GREEN + "You qualify to become a " + ChatColor.DARK_GREEN + "DRUID");
					showFail = false;
				}

				// ENGINEER
				if (targetPlayer.getMasteredJobs().contains("Scientist")
						&& targetPlayer.getMasteredJobs().contains("Miner")) {
					RunicParadise.perms.playerAdd(Bukkit.getPlayer(args[1]), "jobs.join.engineer");
					targetPlayer.sendMessageToPlayer(
							ChatColor.GREEN + "You qualify to become a " + ChatColor.DARK_GREEN + "ENGINEER");
					showFail = false;
				}

				// FAILURE
				if (showFail) {
					targetPlayer.sendMessageToPlayer(ChatColor.RED
							+ "You don't qualify for any additional jobs yet. Master another job and try again!");
				}

			} else if (args[0].equals("maintenance") && args.length == 1 && !(sender instanceof Player)) {

				RunicPlayerBukkit.maintainJobTable();

			} else if (sender instanceof Player) {

				sender.sendMessage("This command must be run from commandblock or console");
				sender.sendMessage("Format: /rpjobs master playername");
			}

			break;
		case "rpvote":
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
			break;
		case "rptransfer":
			final int MAX_UFOTRANSFER_ITEMS = 10;
			Bukkit.getLogger().log(Level.INFO, "[RPTransfer] Command received.");
			if (args.length == 3 && args[0].equals("ufotransfer1.8")) {
				// command is trying to CHECK STATUS of a player for the 1.8
				// world transfer
				Bukkit.getLogger().log(Level.INFO, "[RPTransfer] Valid syntax for status.");
				// First count items in player inventory
				RunicPlayerBukkit targetPlayer = new RunicPlayerBukkit(args[2]);
				int invCount = targetPlayer.checkPlayerInventoryItemstackCount();
				boolean wearingArmor = targetPlayer.checkPlayerWearingArmor();
				int usedSlots = 0;
				int activeSlots = 0;
				boolean active = false;
				String storedItemsString = "";

				// try to lookup player data in DB
				try {
					final Connection d = MySQL.openConnection();
					Statement dStmt = d.createStatement();
					ResultSet playerData = dStmt.executeQuery(
							"SELECT * FROM `rp_ItemTransfers` WHERE UUID = '" + targetPlayer.getPlayerUUID()
									+ "' AND TransferType = 'ufotransfer1.8' ORDER BY ID ASC LIMIT 1;");
					if (!playerData.isBeforeFirst()) {
						// Player doesn't exist in the DB for this transfer
						// type. Create entry.
						PreparedStatement insertStmt = d.prepareStatement(
								"INSERT INTO rp_ItemTransfers (TransferType, PlayerName, UUID, StoredItems) VALUES "
										+ "('ufotransfer1.8', '" + targetPlayer.getPlayerName() + "', '"
										+ targetPlayer.getPlayerUUID() + "', ' ');");

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
					Bukkit.getLogger().log(Level.SEVERE, "Failed ItemTransfers Lookup because " + e.getMessage());
				}

				if (args[1].equals("start")) {
					// proximity trigger upon entering 1.8 world area for first
					// time
					if (targetPlayer.checkPlayerPermission("rp.runicrealm")) {

					} else {
						Player player = Bukkit.getPlayer(args[2]);
						player.teleport(new Location(Bukkit.getWorld("Runic Paradise"), 5644.5, 147.0, -4.5,
								(float) -179.74, (float) 5.70));
					}
				} else if (args[1].equals("ufo")) {
					// proximity trigger upon entering 1.8 world area for first
					// time

					Player player = Bukkit.getPlayer(args[2]);
					player.teleport(new Location(Bukkit.getWorld("Runic Paradise"), 5644.5, 147.0, -4.5,
							(float) -179.74, (float) 5.70));

				} else if (args[1].equals("status")) {
					// debugging for armor worn!
					if (wearingArmor) {
						Bukkit.getLogger().log(Level.INFO,
								"[RPTransfer] " + targetPlayer.getPlayerName() + " is wearing armor.");
						return true;
					} else {
						Bukkit.getLogger().log(Level.INFO,
								"[RPTransfer] " + targetPlayer.getPlayerName() + " is NOT wearing armor.");
					}

					Bukkit.getLogger().log(Level.INFO,
							"[RPTransfer] " + targetPlayer.getPlayerName() + " is has used " + usedSlots + " slots.");

					if (active) {
						Bukkit.getLogger().log(Level.INFO,
								"[RPTransfer] " + targetPlayer.getPlayerName() + " has items stored right now.");
					} else {
						Bukkit.getLogger().log(Level.INFO, "[RPTransfer] " + targetPlayer.getPlayerName()
								+ " does NOT have items stored right now.");
					}
				} // end STATUS check
				else if (args[1].equals("save")) {
					// STOP if player wearing armor!!
					if (wearingArmor) {
						targetPlayer.sendMessageToPlayer(ChatColor.GRAY + "" + ChatColor.ITALIC
								+ "You can't wear armor if you want to sneak items through!");
						return true;
					}
					// STOP if player already used 9 slots!!
					else if (usedSlots >= MAX_UFOTRANSFER_ITEMS) {
						targetPlayer.sendMessageToPlayer(
								ChatColor.GRAY + "" + ChatColor.ITALIC + "You can't sneak any more items through!");
						return true;
					}
					// STOP if player has active slots
					else if (active) {
						targetPlayer
								.sendMessageToPlayer(ChatColor.GRAY + "" + ChatColor.ITALIC + "You must retrieve the "
										+ activeSlots + " items you've tucked away before sneaking more through.");
						return true;
					} // STOP if player has more items on them than they can
						// store
					else if (invCount > (MAX_UFOTRANSFER_ITEMS - usedSlots)) {
						targetPlayer.sendMessageToPlayer(ChatColor.GRAY + "" + ChatColor.ITALIC
								+ "You have more items on you than you can sneak through! You can only take "
								+ (MAX_UFOTRANSFER_ITEMS - usedSlots) + " more item slots!");
						return true;
					}

					// All is ok... so now store some items!!
					String invString = InventorySerialization.serializeInventoryAsString(
							Bukkit.getPlayer(targetPlayer.getPlayerName()).getInventory().getContents());
					int newUsedSlots = usedSlots + invCount;

					try {
						// Statement eStmt = e.createStatement();
						final Connection e = MySQL.openConnection();
						Statement eStmt = e.createStatement();
						PreparedStatement updateStmt = e.prepareStatement("UPDATE rp_ItemTransfers SET ActiveSlots="
								+ invCount + ", UsedSlots=" + newUsedSlots + ", StoredItems=? " + "WHERE UUID='"
								+ targetPlayer.getPlayerUUID() + "' AND TransferType='ufotransfer1.8';");
						updateStmt.setString(1, invString);

						updateStmt.executeUpdate();
						// CLEAR THE INVENTORY!!!!
						Bukkit.getPlayer(targetPlayer.getPlayerName()).getInventory().clear();
						targetPlayer.sendMessageToPlayer(ChatColor.GREEN + "You have successfully hidden " + invCount
								+ " items to take to the 1.8 world!");
					} catch (SQLException err) {
						Bukkit.getLogger().log(Level.SEVERE,
								"Cant update/save for itemtransfers because: " + err.getMessage());
					}

				} // end SAVE command
				else if (args[1].equals("load")) {

					// See if player has any stored items
					if (!active) {
						targetPlayer.sendMessageToPlayer(ChatColor.GRAY + "" + ChatColor.ITALIC
								+ "You haven't stored any items so there is nothing to unpack.");
						return true;
					}

					// check to make sure player has enough room to claim the
					// stored items
					if ((36 - invCount) < activeSlots) {
						// player does NOT have enough room
						targetPlayer.sendMessageToPlayer(ChatColor.GRAY + "" + ChatColor.ITALIC
								+ "You don't have enough slots open to unpack your hidden items. You need "
								+ activeSlots + " free slots.");
						return true;
					} else {
						// player has enough room!
						try {
							// Statement eStmt = e.createStatement();
							final Connection e = MySQL.openConnection();
							Statement eStmt = e.createStatement();
							PreparedStatement updateStmt = e.prepareStatement(
									"UPDATE rp_ItemTransfers SET StoredItems=' ', ActiveSlots=0 WHERE UUID='"
											+ targetPlayer.getPlayerUUID() + "' AND TransferType='ufotransfer1.8';");

							updateStmt.executeUpdate();

							ItemStack[] items = InventorySerialization.getInventory(storedItemsString, 100);
							targetPlayer.givePlayerItemStack(items);
							targetPlayer.sendMessageToPlayer(
									ChatColor.GREEN + "You have successfully unpacked " + activeSlots + " items!");
							if (usedSlots >= MAX_UFOTRANSFER_ITEMS) {
								targetPlayer.sendMessageToPlayer(ChatColor.GRAY + "" + ChatColor.ITALIC
										+ "You can not sneak any more items into the 1.8 world.");
							} else if (usedSlots < MAX_UFOTRANSFER_ITEMS) {
								targetPlayer.sendMessageToPlayer(ChatColor.GRAY + "" + ChatColor.ITALIC
										+ "You can still sneak " + (MAX_UFOTRANSFER_ITEMS - usedSlots)
										+ " more items into the 1.8 world.");
							}
						} catch (SQLException err) {
							Bukkit.getLogger().log(Level.SEVERE,
									"Cant update/save for itemtransfers because: " + err.getMessage());
						}

					}
				} // end LOAD
				else if (args[1].equals("reset")) {
					try {
						// Statement eStmt = e.createStatement();
						final Connection e = MySQL.openConnection();
						Statement eStmt = e.createStatement();
						PreparedStatement updateStmt = e.prepareStatement(
								"UPDATE rp_ItemTransfers SET StoredItems=' ', ActiveSlots=0, UsedSlots=0 WHERE UUID='"
										+ targetPlayer.getPlayerUUID() + "' AND TransferType='ufotransfer1.8';");

						updateStmt.executeUpdate();

						targetPlayer.sendMessageToPlayer(
								ChatColor.GREEN + "" + "Your data has been reset for UFOTransfer1.8. You can now take "
										+ MAX_UFOTRANSFER_ITEMS + " items through.");

					} catch (SQLException err) {
						Bukkit.getLogger().log(Level.SEVERE,
								"Cant update/save for itemtransfers because: " + err.getMessage());
					}

				} // end reset
			}
			break;
		case "graves":
		case "grave":
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
			break;
		case "rpcrates":
			if (args.length == 2) {

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

			break;
		case "rprewards":
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

			break;
		case "rankitem":
			if (args.length == 3 && args[1].equals("DukeMetal") && args[0].equalsIgnoreCase("Give")) {
				Bukkit.getPlayer(args[2]).getLocation().getWorld().dropItemNaturally(
						Bukkit.getPlayer(args[2]).getLocation(),
						Borderlands.specialLootDrops("DukeMetal", Bukkit.getPlayer(args[2]).getUniqueId()));
				Bukkit.getPlayer(args[2]).getLocation().getWorld().dropItemNaturally(
						Bukkit.getPlayer(args[2]).getLocation(),
						Borderlands.specialLootDrops("DukeMetal", Bukkit.getPlayer(args[2]).getUniqueId()));
			} else if (args.length == 3 && args[1].equals("BaronMetal") && args[0].equalsIgnoreCase("Give")) {
				Bukkit.getPlayer(args[2]).getLocation().getWorld().dropItemNaturally(
						Bukkit.getPlayer(args[2]).getLocation(),
						Borderlands.specialLootDrops("BaronMetal", Bukkit.getPlayer(args[2]).getUniqueId()));
			} else if (args.length == 3 && args[0].equalsIgnoreCase("Check")) {

				// rankitem check duke runelynx
				Ranks.craftFeudalJewelry(Bukkit.getPlayer(args[2]), args[1]);
			}

			break;
		case "rptokens":

			if (args.length == 0 || args[0].equals("help")) {
				if (sender instanceof Player) {
					RunicPlayerBukkit senderPlayer = new RunicPlayerBukkit((Player) sender);
					senderPlayer.sendMessageToPlayer(ChatColor.GOLD + "[RunicCarnival] How to form rptokens commands:");
					senderPlayer.sendMessageToPlayer(ChatColor.GRAY + "Take tokens and execute command as reward:");
					senderPlayer
							.sendMessageToPlayer(ChatColor.DARK_GRAY + "/rptokens take PLAYERNAME TOKENCOUNT COMMAND");
					senderPlayer.sendMessageToPlayer(ChatColor.GRAY + "Reward karma:");
					senderPlayer.sendMessageToPlayer(
							ChatColor.DARK_GRAY + "/rptokens givekarma PLAYERNAME TOKENCOUNT KARMACOUNT");
					senderPlayer.sendMessageToPlayer(ChatColor.GRAY + "Take tokens and give a chest-inv reward:");
					senderPlayer.sendMessageToPlayer(
							ChatColor.DARK_GRAY + "/rptokens chestreward PLAYERNAME TOKENCOUNT X Y Z");
					senderPlayer.sendMessageToPlayer(ChatColor.GRAY + "Give or take tokens:");
					senderPlayer.sendMessageToPlayer(ChatColor.DARK_GRAY + "/rptokens give/take PLAYERNAME TOKENCOUNT");
					senderPlayer.sendMessageToPlayer(ChatColor.GRAY + "Give trophies:");
					senderPlayer
							.sendMessageToPlayer(ChatColor.DARK_GRAY + "/rptokens givetrophy PLAYERNAME TROPHYCOUNT");
					senderPlayer.sendMessageToPlayer(ChatColor.GRAY + "Take all trophies and give tokens:");
					senderPlayer.sendMessageToPlayer(ChatColor.DARK_GRAY + "/rptokens taketrophy PLAYERNAME");
					senderPlayer.sendMessageToPlayer(ChatColor.GRAY + "Add maze win to player total:");
					senderPlayer.sendMessageToPlayer(ChatColor.DARK_GRAY + "/rptokens mazewin PLAYERNAME MazeID");
				}

			}

			else if (args.length == 2 && args[0].equals("checkbalance")) {

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
						String successCommand = "";
						int counter = 3; // start counter at the right spot
											// (/rptokens take name 5 give
											// name
											// item)
						while (counter <= (args.length - 1)) {
							successCommand += args[counter] + " ";
							counter++;
						}
						Bukkit.dispatchCommand(Bukkit.getConsoleSender(), successCommand);
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

			break;

		case "rpmail":
			// Not used.
			break;
		case "consoleseeker":
			if (Bukkit.getPlayer(args[0]).hasPermission("rp.ready")) {

				RunicParadise.perms.playerAddGroup(Bukkit.getPlayer(args[0]), "Seeker");

				RunicParadise.perms.playerRemove(Bukkit.getPlayer(args[0]), "rp.ready");

				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "faith setlevel " + args[0] + " Sun 0");
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "faith enable " + args[0] + " Sun");

				RunicParadise.playerProfiles.get((Bukkit.getPlayer(args[0])).getUniqueId()).setChatColor("GREEN", true);

			}
			break;
		case "settler":
		case "seeker":
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
				RunicParadise.playerProfiles.get((Bukkit.getPlayer(args[0])).getUniqueId()).setChatColor("GREEN", true);

            } else if (checkAttemptedPromotion(args[0], sender.getName()) == 0
					&& RunicParadise.newReadyPlayer.containsKey(Bukkit.getPlayer(args[0]).getName())) {
				// command sender has not tried to promote this player yet AND
				// the player is a valid newbie

				rand = new Random();
				int randomNum = rand.nextInt((100 - 1) + 1) + 1;
				if (randomNum <= 50) {
					new RunicPlayerBukkit(((Player) sender).getUniqueId()).adjustPlayerKarma(2);
					sender.sendMessage(ChatColor.RED + "You were too slow... but at least you got some karma!");
				} else {
					sender.sendMessage(ChatColor.RED + "You were too slow... and didn't get any karma this time. ");
				}

                addAttemptedPromotion(args[0], sender.getName());
			} else {
				// completely invalid attmept
				sender.sendMessage(ChatColor.RED + "You can't use that command on that player anymore.");
			}
			break;
		case "ready":

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

					RunicParadise.playerProfiles.get(p.getUniqueId()).setChatColor("GREEN", true);
				}

			}

			/*
			 * OLD /READY LOGIC REQUIRING PLAYER PROMOTION if (sender instanceof
			 * Player) { boolean promoterFound = false; final String
			 * newPlayerName = ((Player) sender).getName();
             *
			 * if (!RunicParadise.newReadyPlayer.containsKey(newPlayerName)) {
			 * RunicParadise.newReadyPlayer.put( ((Player) sender).getName(),
			 * "unused"); Bukkit.getServer().getScheduler()
			 * .scheduleAsyncDelayedTask(instance, new Runnable() { public void
			 * run() { RunicParadise.newReadyPlayer .remove(newPlayerName); } },
			 * 6000); }
             *
			 * for (Player p : Bukkit.getOnlinePlayers()) { if
			 * (p.hasPermission("rp.settlerpromotions")) { promoterFound = true;
			 * p.sendMessage(ChatColor.GOLD + "[RunicRanks] " +
			 * ChatColor.LIGHT_PURPLE + sender.getName() +
			 * " has completed the tutorial.");
			 * p.sendMessage(ChatColor.LIGHT_PURPLE + "Please use " +
			 * ChatColor.AQUA + "/seeker " + sender.getName() +
			 * ChatColor.LIGHT_PURPLE + " to promote them.");
			 * RunicParadise.perms.playerAdd(((Player) sender), "rp.ready");
             *
			 * } }
             *
			 * // tell the other server this one is reconnected to the universe
			 * ByteArrayDataOutput out = ByteStreams.newDataOutput();
			 * out.writeUTF("Forward"); // So BungeeCord knows to forward it
			 * out.writeUTF("ONLINE"); out.writeUTF("PlayerReady"); // The
			 * channel name to check if // this // your data
             *
			 * ByteArrayOutputStream msgbytes = new ByteArrayOutputStream();
			 * DataOutputStream msgout = new DataOutputStream(msgbytes);
             *
			 * try { msgout.writeUTF(((Player) sender).getDisplayName()); // You
			 * // can // do // anything // msgout msgout.writeShort(123); }
			 * catch (IOException e) { }
             *
			 * out.writeShort(msgbytes.toByteArray().length);
			 * out.write(msgbytes.toByteArray());
             *
			 * // If you don't care about the player // Player player =
			 * Iterables.getFirst(Bukkit.getOnlinePlayers(), // null); // Else,
			 * specify them
             *
			 * ((Player) sender).sendPluginMessage(instance, "BungeeCord",
			 * out.toByteArray());
             *
			 * if (promoterFound) { sender.sendMessage(ChatColor.GOLD +
			 * "[RunicRanks] " + ChatColor.LIGHT_PURPLE +
			 * "Staff is online and has been notified that you need a promotion."
			 * ); sender.sendMessage(ChatColor.LIGHT_PURPLE +
			 * "If you don't hear from them soon, they may be AFK. You can try /ready again later."
			 * ); } else { sender.sendMessage(ChatColor.GOLD + "[RunicRanks] " +
			 * ChatColor.LIGHT_PURPLE + "There is no staff online right now.");
			 * sender.sendMessage(ChatColor.LIGHT_PURPLE +
			 * "Post an introduction on our forums and we'll promote you very soon :)"
			 * ); sender.sendMessage(ChatColor.LIGHT_PURPLE +
			 * "Website: http://www.runic-paradise.com"); }
             *
			 * }
			 */
			break;
		case "radio":
		case "music":
			if (sender instanceof Player) {
				sender.sendMessage(ChatColor.AQUA + "[RunicRadio] " + ChatColor.GRAY
						+ " Click to join > https://www.dubtrack.fm/join/runic-paradise-minecraft-server");
			}
			break;
		case "ranks":
			if (sender instanceof Player) {
				rank.showRequirements((Player) sender);
			}
			break;
		case "punish":

			if (args.length == 0 && sender instanceof Player) {
				sender.sendMessage(ChatColor.DARK_AQUA + "Correct usage: /punish playername");
			} else if (args.length == 1) {

				sender.sendMessage("This command is not working right now. :(");
			/*	new FancyMessage(args[0]).color(DARK_RED).then(": ").color(WHITE)
						//
						.then("Info").color(GREEN).style(UNDERLINE).suggest("/bminfo " + args[0])
						.tooltip("Retrieves history of player on RP").then(" ")
						//
						.then("Warn").suggest("/warn " + args[0] + " ")
						.tooltip("Prepares command to issue a warning. Always add a reason!").color(AQUA)
						.style(UNDERLINE).then(" ")
						//
						.then("Kick").suggest("/kick " + args[0] + " ")
						.tooltip("Prepares command to kick. Always add a reason!").color(DARK_AQUA).style(UNDERLINE)
						.then(" ")
						//
						.then("Mute").color(GRAY).then(" ")
						//
						.then("1m").suggest("/tempmute " + args[0] + " 1m ")
						.tooltip("Prepares command to issue a 1 minute mute. Always add a reason!").color(YELLOW)
						.style(UNDERLINE).then(" ")
						//
						.then("5m").suggest("/tempmute " + args[0] + " 5m ")
						.tooltip("Prepares command to issue a 5 minute mute. Always add a reason!").color(GOLD)
						.style(UNDERLINE).then(" ")
						//
						.then("10m").suggest("/tempmute " + args[0] + " 10m ")
						.tooltip("Prepares command to issue a 10 minute mute. Always add a reason!").color(RED)
						.style(UNDERLINE).then(" ").send(Bukkit.getPlayer(sender.getName()));
				//
				new FancyMessage("Ban").color(GRAY).then(" ")
						//
						.then("12h").suggest("/tempban " + args[0] + " 12h ")
						.tooltip("Prepares command to issue a 12 hour tempban. Always add a reason!").color(YELLOW)
						.style(UNDERLINE).then(" ")
						//
						.then("36h").suggest("/tempban " + args[0] + " 36h ")
						.tooltip("Prepares command to issue a 36 hour tempban. Always add a reason!").color(GOLD)
						.style(UNDERLINE).then(" ")
						//
						.then("5d").suggest("/tempban " + args[0] + " 5d ")
						.tooltip("Prepares command to issue a 5 day tempban. Always add a reason!").color(RED)
						.style(UNDERLINE).then(" ")
						//
						.then("Perm").suggest("/ban " + args[0] + " ")
						.tooltip("Prepares command to issue a 5 day tempban. Always add a reason!").color(DARK_RED)
						.style(UNDERLINE).send(Bukkit.getPlayer(sender.getName()));*/
			}
			break;
		case "staff":
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
					sender.sendMessage(ChatColor.AQUA + "/announce" + ChatColor.GRAY + " Manage announcements");
				} else if (args[0].equals("PE") || args[0].equals("pe")) {
					rank.playerStats((Player) sender);
				} else if (args[0].equals("EC") || args[0].equals("ec")) {
					int entityCount = 0;
					int entityCounter = 0;
					for (Player p : Bukkit.getOnlinePlayers()) {
						entityCount = 0;
						ChatColor c;

						entityCount = p.getNearbyEntities(400, 300, 400).size();

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
					if (!args[1].isEmpty()) {
						rank.nominatePlayer((Player) sender, args[1]);
					} else {
						sender.sendMessage(
								ChatColor.GRAY + "[ERROR] You need to provide a player's name! /staff nm name");
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
						// user doesnt have permission for this command
						sender.sendMessage(ChatColor.DARK_RED
								+ "[ERROR] Only admins can use this command. But Rune doesnt blame you for trying. :)");
					}

				} else if ((args[0].equals("NP") || args[0].equals("np")) && args.length == 2) {
					RunicMessaging.sendMessage(((Player) sender), RunicMessaging.RunicFormat.SYSTEM,
							whoIsNearPlayer(Bukkit.getPlayer(args[1])));
				} else {
					RunicMessaging.sendMessage(((Player) sender), RunicMessaging.RunicFormat.SYSTEM,
							ChatColor.LIGHT_PURPLE + "Hmm... please check your command usage with /staff");
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
				} else if (args[0].equals("now")) {
					// player is requesting to activate a promotion
					rank.checkPromotion((Player) sender, true);
				}
			} else {
				sender.sendMessage("[Error] Command must be used by a player");
			}

			break;
		case "rp":
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

				/*
				 * commandPlayer.sendMessageToPlayer(ChatColor.DARK_AQUA +
				 * "✦Status with Runic Security & Runic Farms");
				 * commandPlayer.sendMessageToPlayer(ChatColor.DARK_AQUA +
				 * " Monster Kills"); commandPlayer.sendMessageToPlayer(
				 * ChatColor.GRAY + "  Wither: " + ChatColor.GOLD +
				 * killCounts.get("KillWither") + ChatColor.GRAY + ", Zombie: "
				 * + ChatColor.GOLD + killCounts.get("KillZombie") +
				 * ChatColor.GRAY + ", Witch: " + ChatColor.GOLD +
				 * killCounts.get("KillWitch") + ChatColor.GRAY +
				 * ", Skeletons: " + ChatColor.GOLD +
				 * killCounts.get("KillSkeleton")); commandPlayer
				 * .sendMessageToPlayer(ChatColor.GRAY + "  Slime: " +
				 * ChatColor.GOLD + killCounts.get("KillSlime") + ChatColor.GRAY
				 * + ", MagmaCube: " + ChatColor.GOLD +
				 * killCounts.get("KillMagmaCube") + ChatColor.GRAY +
				 * ", Silverfish: " + ChatColor.GOLD +
				 * killCounts.get("KillSilverfish") + ChatColor.GRAY +
				 * ", Giant: " + ChatColor.GOLD + killCounts.get("KillGiant"));
				 * commandPlayer .sendMessageToPlayer(ChatColor.GRAY +
				 * "  Blaze: " + ChatColor.GOLD + killCounts.get("KillBlaze") +
				 * ChatColor.GRAY + ", Creeper: " + ChatColor.GOLD +
				 * killCounts.get("KillCreeper") + ChatColor.GRAY +
				 * ", Enderman: " + ChatColor.GOLD +
				 * killCounts.get("KillEnderman") + ChatColor.GRAY +
				 * ", Spider: " + ChatColor.GOLD +
				 * killCounts.get("KillSpider"));
				 * commandPlayer.sendMessageToPlayer(ChatColor.GRAY +
				 * "  CaveSpider: " + ChatColor.GOLD +
				 * killCounts.get("KillCaveSpider") + ChatColor.GRAY +
				 * ", Squid: " + ChatColor.GOLD + killCounts.get("KillSquid") +
				 * ChatColor.GRAY + ", EnderDragon: " + ChatColor.GOLD +
				 * killCounts.get("KillEnderDragon") + ChatColor.GRAY +
				 * ", PigZombie: " + ChatColor.GOLD +
				 * killCounts.get("KillPigZombie")); commandPlayer
				 * .sendMessageToPlayer(ChatColor.GRAY + "  Ghast: " +
				 * ChatColor.GOLD + killCounts.get("KillGhast") + ChatColor.GRAY
				 * + ", Bat: " + ChatColor.GOLD + killCounts.get("KillBat") +
				 * ChatColor.GRAY + ", Wolf: " + ChatColor.GOLD +
				 * killCounts.get("KillWolf") + ChatColor.GRAY + ", Endermite: "
				 * + ChatColor.GOLD + killCounts.get("KillEndermite") +
				 * ChatColor.GRAY + ", Guardian: " + ChatColor.GOLD +
				 * killCounts.get("KillGuardian"));
				 * commandPlayer.sendMessageToPlayer( ChatColor.GRAY +
				 * "  E.Guardian: " + ChatColor.GOLD +
				 * killCounts.get("KillElderGuardian") + ChatColor.GRAY +
				 * ", SnowGolem " + ChatColor.GOLD +
				 * killCounts.get("KillSnowGolem") + ChatColor.GRAY +
				 * ", IronGolem: " + ChatColor.GOLD +
				 * killCounts.get("KillIronGolem") + ChatColor.GRAY +
				 * ", Shulker: " + ChatColor.GOLD +
				 * killCounts.get("KillShulker"));
				 * commandPlayer.sendMessageToPlayer( ChatColor.GRAY +
				 * "  W.Skeleton: " + ChatColor.GOLD +
				 * killCounts.get("KillWSkeleton") + ChatColor.GRAY + ", Husk "
				 * + ChatColor.GOLD + killCounts.get("KillHusk") +
				 * ChatColor.GRAY + ", Stray: " + ChatColor.GOLD +
				 * killCounts.get("KillStray"));
				 * commandPlayer.sendMessageToPlayer(ChatColor.DARK_AQUA +
				 * " Animal & People Kills"); commandPlayer.sendMessageToPlayer(
				 * ChatColor.GRAY + "  Chicken: " + ChatColor.GOLD +
				 * killCounts.get("KillChicken") + ChatColor.GRAY + ", Cow: " +
				 * ChatColor.GOLD + killCounts.get("KillCow") + ChatColor.GRAY +
				 * ", Sheep: " + ChatColor.GOLD + killCounts.get("KillSheep") +
				 * ChatColor.GRAY + ", Pig: " + ChatColor.GOLD +
				 * killCounts.get("KillPig") + ChatColor.GRAY + ", Villager: " +
				 * ChatColor.GOLD + killCounts.get("KillVillager"));
				 * commandPlayer.sendMessageToPlayer( ChatColor.GRAY +
				 * "  Ocelot: " + ChatColor.GOLD + killCounts.get("KillOcelot")
				 * + ChatColor.GRAY + ", Rabbit: " + ChatColor.GOLD +
				 * killCounts.get("KillRabbit") + ChatColor.GRAY +
				 * ", Mooshroom: " + ChatColor.GOLD +
				 * killCounts.get("KillMooshroom") + ChatColor.GRAY +
				 * ", PolarBear: " + ChatColor.GOLD +
				 * killCounts.get("KillPolarBear"));
				 */
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
				return true;
			}
			break;
		case "rptest":
			((Player) sender).getLocation().getWorld().dropItemNaturally(((Player) sender).getLocation(),
					Borderlands.specialLootDrops("BaronMetal", ((Player) sender).getUniqueId()));
			((Player) sender).getLocation().getWorld().dropItemNaturally(((Player) sender).getLocation(),
					Borderlands.specialLootDrops("BaronGem", ((Player) sender).getUniqueId()));

			((Player) sender).getLocation().getWorld().dropItemNaturally(((Player) sender).getLocation(),
					Borderlands.specialLootDrops("BaronIngot1", ((Player) sender).getUniqueId()));
			((Player) sender).getLocation().getWorld().dropItemNaturally(((Player) sender).getLocation(),
					Borderlands.specialLootDrops("BaronIngot2", ((Player) sender).getUniqueId()));

			((Player) sender).getLocation().getWorld().dropItemNaturally(((Player) sender).getLocation(),
					Borderlands.specialLootDrops("DukeGem", ((Player) sender).getUniqueId()));
			((Player) sender).getLocation().getWorld().dropItemNaturally(((Player) sender).getLocation(),
					Borderlands.specialLootDrops("DukeMetal", ((Player) sender).getUniqueId()));
			((Player) sender).getLocation().getWorld().dropItemNaturally(((Player) sender).getLocation(),
					Borderlands.specialLootDrops("DukeEssence", ((Player) sender).getUniqueId()));

			((Player) sender).getLocation().getWorld().dropItemNaturally(((Player) sender).getLocation(),
					Borderlands.specialLootDrops("DukeRing1", ((Player) sender).getUniqueId()));
			((Player) sender).getLocation().getWorld().dropItemNaturally(((Player) sender).getLocation(),
					Borderlands.specialLootDrops("DukeRing2", ((Player) sender).getUniqueId()));
			((Player) sender).getLocation().getWorld().dropItemNaturally(((Player) sender).getLocation(),
					Borderlands.specialLootDrops("DukeRing3", ((Player) sender).getUniqueId()));
			((Player) sender).getLocation().getWorld().dropItemNaturally(((Player) sender).getLocation(),
					Borderlands.specialLootDrops("DukeRing4", ((Player) sender).getUniqueId()));

            sender.sendMessage(((Player) sender).getMaximumAir() + " max air ticks. "
					+ ((Player) sender).getRemainingAir() + " remaining air ticks.");

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

			break;
		case "rpreload":
			instance.reloadConfig();
			if (sender instanceof Player) {
				Player player = (Player) sender;
				player.sendMessage(ChatColor.GRAY + "[RP] Runic Paradise plugin reloaded.");
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
				List<Player> mansionPlayers = Bukkit.getWorld("Mansion").getPlayers();
				for (Player p : mansionPlayers) {
					p.sendMessage(message);
				}

			}
			break;
		case "say":
			// TODO: needs fixing
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
					List<Player> mansionPlayers = Bukkit.getWorld("Mansion").getPlayers();
					for (Player p : mansionPlayers) {
						p.sendMessage(message);
					}
				} else if (senderBlock.getWorld().equals("Razul")) {
					String message = "";
					for (String b : args) {
						message += b + " ";
					}
					List<Player> mansionPlayers = Bukkit.getWorld("Razul").getPlayers();
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
		case "headofplayer":
		case "face":
			if (args.length == 1) {
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
						Double diffHours = (currentTime - loggedTime) / (60.0 * 60 * 1000);
						sender.sendMessage(
								ChatColor.RED + "You can only use this command once every 6 hours. You last used it "
										+ diffHours + " hours ago.");

					} else {
						// No record found, proceed!
						String command = String.format("give %s minecraft:player_head{SkullOwner:{Name:\"%s\"}} 1", sender.getName(), args[0]);
						Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
//						Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
//								"give " + sender.getName() + " 397 1 3 {SkullOwner: " + args[0] + "}");
						try {

							PreparedStatement insertStmt = d.prepareStatement(
									"INSERT INTO rp_HeadCreations (PlayerName, UUID, Timestamp, HeadRequested) VALUES "
											+ "('" + sender.getName() + "', '"
											+ ((Player) sender).getUniqueId().toString() + "', "
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
			} // end if checking arg length
			else {
				sender.sendMessage(ChatColor.DARK_RED + "Usage: " + ChatColor.AQUA + "/face <playername>"
						+ ChatColor.DARK_RED
						+ " Watch your spelling, you only get ONE chance every 6 hours!! Always enter FULL player names, NOT nicks!");
			}

			break;
		case "rpgames":
		case "games":
			if (sender instanceof Player) {
				Player player = (Player) sender;
				int tokenBal = 0;
				try {
					final Connection d = MySQL.openConnection();
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
				} else if (args.length == 1) {
					String temp = "";
					try {
						int option = Integer.parseInt(args[0]);
					} catch (Exception e) {
						player.sendMessage(ChatColor.GRAY + "[ERROR] Invalid entry. Please check options via /games");
						return true;
					}
					switch (Integer.parseInt(args[0])) {
					case 1:
						player.teleport(new Location(Bukkit.getWorld("RunicSky"), 342, 58, 548, 0, (float) 1));
						break;
					case 2:
						player.teleport(
								new Location(Bukkit.getWorld("RunicSky"), 320, 58, 522, (float) 92.50, (float) -16.05));
						break;
					case 3:
						player.teleport(
								new Location(Bukkit.getWorld("RunicSky"), 328, 58, 543, (float) 72.99, (float) -26.40));
						break;
					case 4:
						player.teleport(new Location(Bukkit.getWorld("RunicSky"), 328, 58, 507, (float) 135.499,
								(float) -23.99));
						break;
					case 5:
						player.teleport(new Location(Bukkit.getWorld("RunicSky"), 342, 58, 507, (float) 180.35,
								(float) -28.95));
						break;
					case 6:
						player.teleport(new Location(Bukkit.getWorld("RunicSky"), 358, 58, 508, (float) -131.25,
								(float) -27.600));
						break;
					case 7:
						player.teleport(new Location(Bukkit.getWorld("RunicSky"), 359, 58, 522, (float) -90.300,
								(float) -42.4499));
						break;
					case 8:
						player.teleport(new Location(Bukkit.getWorld("RunicSky"), 357, 58, 538, (float) -42.150,
								(float) -27.85));
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
					return true;
				}

			} else {
				sender.sendMessage("[RP] Command must be used by a player");
				return true;
			}
			break;
		case "testerchat":
		case "tc":
			// Not existent in plugin.yml
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

				if (p.hasPermission("rp.testers")) {
					if (args.length == 0) {
						Player player = (Player) sender;
						player.sendMessage(ChatColor.DARK_GRAY + "Tester chat. Usage: /tc [message]");
						return true;
					} else {

						p.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.DARK_PURPLE + "Tester"
								+ ChatColor.LIGHT_PURPLE + "Chat" + ChatColor.DARK_GRAY + "] " + ChatColor.WHITE
								+ senderName + ":" + ChatColor.LIGHT_PURPLE + buffer.toString());

					}
				}
			}
			Bukkit.getLogger().log(Level.INFO, "[TesterChat] " + senderName + ": " + buffer.toString());
			break;
		case "staffchat":
		case "sc":
			String senderName1 = "";
			if (sender instanceof Player) {
				Player player = (Player) sender;
				senderName1 = sender.getName();
			} else {
				senderName1 = "Console";

			}

			StringBuilder buffer1 = new StringBuilder();
			// change the starting i value to pick what argument to start from
			// 1 is the 2nd argument.
			for (int i = 0; i < args.length; i++) {
				buffer1.append(' ').append(args[i]);
			}

			for (Player p : Bukkit.getOnlinePlayers()) {

				if (p.hasPermission("rp.staff")) {
					if (args.length == 0) {
						Player player = (Player) sender;
						player.sendMessage(ChatColor.DARK_GRAY + "Staff chat. Usage: /sc [message]");
						return true;
					} else {

						p.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.DARK_AQUA + "Staff" + ChatColor.AQUA
								+ "Chat" + ChatColor.DARK_GRAY + "] " + ChatColor.WHITE + senderName1 + ":"
								+ ChatColor.AQUA + buffer1.toString());

					}
				}
			}
			Bukkit.getLogger().log(Level.INFO, "[StaffChat] " + senderName1 + ": " + buffer1.toString());
			break;
		default:
			break;
		}

		return true;

	}

	static boolean givePlayerExplorationReward(int locID, Player p) {
		int tokenReward = RunicParadise.explorerRewards.get(locID);

		RunicPlayerBukkit targetPlayer = new RunicPlayerBukkit(p.getUniqueId());
		int newBalance = targetPlayer.getPlayerTokenBalance() + tokenReward;
		targetPlayer.setPlayerTokenBalance(newBalance);

		RunicMessaging.sendMessage(p, RunicMessaging.RunicFormat.EXPLORER,
				"Congratulations! You found " + RunicParadise.explorerIDs.get(locID));

		return false;
	}

	private static void spawnTransportBeacon(Location loc, Player p) {
		Location clayLoc = new Location(loc.getWorld(), loc.getX(), (loc.getY() - 1.0), loc.getZ());
		Location glassLoc = new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ());

		Block clay = clayLoc.getBlock();
		Block glass = glassLoc.getBlock();

		clay.setType(Material.LIGHT_BLUE_TERRACOTTA);
		glass.setType(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
	}

	private static void repairCommand(Player p, ItemStack main, ItemStack off) {
		boolean mainOkToRepair = false;
		boolean offOkToRepair = false;

		if (main != null && RunicParadise.repairableItemTypes.contains(main.getType().getId())) {
			mainOkToRepair = true;
		}

		if (off != null && RunicParadise.repairableItemTypes.contains(off.getType().getId())) {
			offOkToRepair = true;
		}

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
				final Plugin instance = RunicParadise.getInstance();
				MySQL MySQL = new MySQL(instance, instance.getConfig().getString("dbHost"),
						instance.getConfig().getString("dbPort"), instance.getConfig().getString("dbDatabase"),
						instance.getConfig().getString("dbUser"), instance.getConfig().getString("dbPassword"));
				final Connection d = MySQL.openConnection();

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

	private static void itemInfoCommandAdd(StringBuilder message, String description, String data) {
    	message.append(ChatColor.GOLD).append(description).append(ChatColor.GRAY).append(data).append("\n");
	}

	private static void itemInfoCommand(Player sender) {
    	ItemStack itemInHand = sender.getInventory().getItemInMainHand();

    	StringBuilder message = new StringBuilder();
		itemInfoCommandAdd(message, "Meta: ", RunicUtilities.toStringOr(itemInHand.getItemMeta(), "no item meta"));
		itemInfoCommandAdd(message, "Data: ", RunicUtilities.toStringOr(itemInHand.getData(), "no item data"));
		itemInfoCommandAdd(message, "Durability: ", String.valueOf(itemInHand.getDurability()));
		itemInfoCommandAdd(message, "Type: ", itemInHand.getType().toString());
		itemInfoCommandAdd(message, "Type id: ", String.valueOf(itemInHand.getType().getId()));
		message.append(ChatColor.GOLD).append(RunicSerialization.serializeItemStackList(new ItemStack[] { itemInHand }).toString());
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
