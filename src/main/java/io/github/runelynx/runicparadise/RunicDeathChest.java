package io.github.runelynx.runicparadise;

import io.github.runelynx.runicparadise.tempserialization.InventorySerialization;
import io.github.runelynx.runicparadise.tempserialization.SingleItemSerialization;
import io.github.runelynx.runicuniverse.RunicMessaging;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.logging.Level;

import static org.bukkit.Bukkit.getLogger;

public class RunicDeathChest implements Serializable {
	private static HashMap<Long, ItemStack[]> deathArmor = new HashMap<>();
	private static HashMap<Long, ItemStack[]> deathInventory = new HashMap<>();
	private static HashMap<Long, ItemStack> deathOffhand = new HashMap<>();
	private static HashMap<Integer, String> graveLocations = new HashMap<>();
	private static HashMap<Integer, String> graveStatus = new HashMap<>();
	private static HashMap<String, Integer> graveID = new HashMap<>();

	public static String checkLocForDeath(Location loc) {
		final Plugin instance = RunicParadise.getInstance();
		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);

		String strToCheck = loc.getWorld().getName() + "." + loc.getBlockX() + "." + loc.getBlockY() + "."
				+ loc.getBlockZ();

		try {
			// TODO: Get data from DB based on UUID to protect vs name changes
			Connection connection = MySQL.openConnection();
			Statement statement = connection.createStatement();
			ResultSet graveData = statement
					.executeQuery("SELECT PlayerName,Status FROM `rp_PlayerGraves` WHERE Location = '" + strToCheck
							+ "' AND Status != 'Gone' ORDER BY ID ASC LIMIT 1;");
			// AND

			// if (!playerData.first() && !playerData.next()) {
			if (!graveData.isBeforeFirst()) {
				// Location doesn't exist in the DB!
				return "NoGrave";

			} else {
				// Location does exist in the DB

				graveData.next();
				if (graveData.getString("Status").equals("Locked")) {
					// Grave is still locked; return owner name
					String pname = graveData.getString("PlayerName");
					connection.close();
					return pname;
				} else {
					// Grave is OPEN!
					connection.close();
					return "Unlocked";
				}

			}
		} catch (SQLException z) {
			getLogger().log(Level.SEVERE, "Failed DB check for grave locations cuz " + z.getMessage());
		}
		return "NoGrave";

	}

	public static int countItemsInItemstack(ItemStack[] stack) {
		int count = 0;

		for (ItemStack item : stack) {
			if ((item != null) && (item.getAmount() > 0) && (item.getType() != Material.AIR)) {
				count++;
			}
		}

		return count;
	}

	/*
	 * public static void savePlayerDeath(Player player, Location loc) { final
	 * Plugin instance = RunicParadise.getInstance();
	 * 
	 * MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
	 * final Connection e = MySQL.openConnection(); // Establish ID for new
	 * storing this death long newDeathID = new Date().getTime();
	 * 
	 * // Store this death to hashmaps (type ItemStack[])
	 * 
	 * if (player.getInventory().getItemInOffHand() != null) { // save what's in
	 * the offhand slot if there is something deathOffhand.put(newDeathID,
	 * player.getInventory() .getItemInOffHand()); // now clear that slow - if
	 * this slot gets saved in the Inventory // itself it will fail in
	 * Serialization! player.getInventory().setItemInOffHand(new
	 * ItemStack(Material.AIR)); } deathArmor.put(newDeathID,
	 * player.getInventory().getArmorContents()); deathInventory.put(newDeathID,
	 * player.getInventory().getContents());
	 * 
	 * // 12hr expiry time long expiryTime = new Date().getTime() + 43200000;
	 * 
	 * String armorString = InventorySerialization
	 * .serializeInventoryAsString(deathArmor.get(newDeathID)); String invString
	 * = InventorySerialization
	 * .serializeInventoryAsString(deathInventory.get(newDeathID)); String
	 * offhandString = SingleItemSerialization
	 * .serializeItemAsString((deathOffhand.get(newDeathID)));
	 * 
	 * String originalBlockString = loc.getBlock().getType().toString();
	 * Location dummyLoc = loc;
	 * 
	 * if (originalBlockString.equals("CHEST") ||
	 * originalBlockString.equals("SIGN_POST") ||
	 * originalBlockString.equals("WALL_SIGN")) { double newX = 0;
	 * 
	 * Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "sc STARTING " +
	 * loc.getX() + " " + loc.getY() + " " + loc.getZ() + ": ");
	 * 
	 * while (newX < 2) { double newY = -1; while (newY < 4) { double newZ = 0;
	 * while (newZ < 2) {
	 * 
	 * String checkBlock = new Location(loc.getWorld(), loc.getX() + newX,
	 * loc.getY() + newY, loc.getZ() + newZ).getBlock().getType() .toString();
	 * 
	 * if (checkBlock.equals("AIR")) { if (new Location(loc.getWorld(),
	 * loc.getX() + newX, loc.getY() + newY + 1, loc.getZ() + newZ)
	 * .getBlock().getType().toString() .equals("AIR")) {
	 * 
	 * // found a good spot!! change the death loc to // that! loc = new
	 * Location(loc.getWorld(), loc.getX() + newX, loc.getY() + newY, loc.getZ()
	 * + newZ); newX = 3; newY = 3; newZ = 3; }
	 * 
	 * } newZ++; } newY++; } newX++; }
	 * 
	 * }
	 * 
	 * String locString = loc.getWorld().getName() + "." + loc.getBlockX() + "."
	 * + loc.getBlockY() + "." + loc.getBlockZ();
	 * 
	 * // ///////////// try { // Statement eStmt = e.createStatement();
	 * 
	 * PreparedStatement eStmt = e .prepareStatement(
	 * "INSERT INTO rp_PlayerGraves (`Location`, `Status`, `PlayerName`, `ExpiryTime`, `CreationTime`, `LevelsLost`, `OffhandItemStack`, `ArmorItemStack`, `InvItemStack`, `LooterName`, `LootTime`, `PreviousBlock`) VALUES "
	 * + "('" + locString + "', 'Locked', '" + player.getName() + "', " +
	 * expiryTime + ", " + new Date().getTime() + ", '" + player.getLevel() +
	 * "', ?, ?, ?, null, null, '" + loc.getBlock().getType().toString() +
	 * "');"); eStmt.setString(1, offhandString); eStmt.setString(2,
	 * armorString); eStmt.setString(3, invString);
	 * 
	 * eStmt.executeUpdate(); } catch (SQLException err) {
	 * Bukkit.getLogger().log( Level.SEVERE, "Cant create new row Grave for " +
	 * player.getName() + " because: " + err.getMessage()); }
	 * 
	 * // USE CAUTION WITH TESTING !!! player.getInventory().clear();
	 * player.getInventory().setHelmet(null);
	 * player.getInventory().setChestplate(null);
	 * player.getInventory().setLeggings(null);
	 * player.getInventory().setBoots(null); player.setLevel(0);
	 * 
	 * // resync hashmap with graves to keep the redstone lamp on...
	 * syncGraveLocations();
	 * 
	 * loc.getBlock().setType(Material.BEDROCK); Block b = loc.add(0, 1,
	 * 0).getBlock(); b.setType(Material.SIGN_POST); BlockState state =
	 * b.getState(); Sign sign = (Sign) state;
	 * 
	 * // To set sign.setLine(0, ChatColor.DARK_RED + "☠ ☠ GRAVE ☠ ☠");
	 * sign.setLine(1, "RIP"); sign.setLine(2, player.getDisplayName());
	 * sign.setLine(3, ChatColor.DARK_RED + "☠ ☠ ☠ ☠ ☠ ☠ ☠ ☠ ☠");
	 * sign.update(true);
	 * 
	 * }
	 */
	// the Handle methods return true if all is well... false if there is a
	// failure.
	public static boolean handleItems(ItemStack[] items, RunicPlayerBukkit playerAtGrave, int graveID) {

		if (items == null) {
			return true;
		}

		// If the items slots from death storage cant fit in the player's
		// free inventory slots...
		if (((36 - playerAtGrave.checkPlayerInventoryItemstackCount()) - countItemsInItemstack(items)) < 0) {
			playerAtGrave.sendMessageToPlayer(ChatColor.DARK_GRAY + "[RunicReaper] " + ChatColor.GRAY + "Need "
					+ ChatColor.DARK_RED
					+ ((36 - playerAtGrave.checkPlayerInventoryItemstackCount()) - countItemsInItemstack(items)) * -1
					+ ChatColor.GRAY + " more open slots to get your items.");
			return false;
		} else {
			// Player has room in inventory!
			playerAtGrave.givePlayerItemStack(items);

			playerAtGrave.sendMessageToPlayer(ChatColor.DARK_GRAY + "[RunicReaper] " + ChatColor.GRAY
					+ "You have retrieved your items, if there were any to retrieve.");
			// remove data
			Plugin instance = RunicParadise.getInstance();

			MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
			Connection connection = MySQL.openConnection();

			try {
				Statement statement = connection.createStatement();
				statement.executeUpdate(
						"UPDATE `rp_PlayerGraves` SET InvBlob=null, LooterName='" + playerAtGrave.getPlayerName()
								+ "',LootTime=" + new Date().getTime() + " WHERE ID=" + graveID + ";");
				connection.close();
				return true;
			} catch (SQLException err) {
				getLogger().log(Level.SEVERE, "Error updating invBlob because: " + err.getMessage());
				return false;
			}
		}
	}

	public static boolean handleOffhand(ItemStack item, RunicPlayerBukkit playerAtGrave, int graveID) {

		// If the items slots from death storage cant fit in the player's
		// free inventory slots...
		if (Bukkit.getPlayer(playerAtGrave.getPlayerName()).getInventory().getItemInOffHand() != null && Bukkit
				.getPlayer(playerAtGrave.getPlayerName()).getInventory().getItemInOffHand().getType() != Material.AIR) {
			playerAtGrave.sendMessageToPlayer(ChatColor.DARK_GRAY + "[RunicReaper] " + ChatColor.GRAY
					+ "Empty your offhand slot to retrieve your old offhand item!");
			return false;
		} else {
			// Player has room in inventory!
			Bukkit.getPlayer(playerAtGrave.getPlayerName()).getInventory().setItemInOffHand(item);
			Bukkit.getPlayer(playerAtGrave.getPlayerName()).updateInventory();
			;

			playerAtGrave.sendMessageToPlayer(
					ChatColor.DARK_GRAY + "[RunicReaper] " + ChatColor.GRAY + "You have retrieved your offhand item.");
			// remove data
			final Plugin instance = RunicParadise.getInstance();

			MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
			Connection connection = MySQL.openConnection();
			try {
				String emptyJSON = "[{\"amount\":0,\"id\":0,\"index\":0,\"data\":-1}]";
				Statement dStmt = connection.createStatement();
				dStmt.executeUpdate("UPDATE `rp_PlayerGraves` SET OffhandItemStack='" + emptyJSON + "',LooterName='"
						+ playerAtGrave.getPlayerName() + "',LootTime=" + new Date().getTime() + " WHERE ID=" + graveID
						+ ";");
				connection.close();
				return true;
			} catch (SQLException err) {
				getLogger().log(Level.SEVERE, "Error updating OffhandItemStack because: " + err.getMessage());
				return false;
			}
		}
	}

	public static boolean handleArmor(ItemStack[] armor, RunicPlayerBukkit playerAtGrave, int graveID) {

		if (armor == null) {
			return true;
		}

		// If the armor slots from death storage cant fit in the player's
		// free inventory slots...
		if (((36 - playerAtGrave.checkPlayerInventoryItemstackCount()) - countItemsInItemstack(armor)) < 0) {
			playerAtGrave.sendMessageToPlayer(ChatColor.DARK_GRAY + "[RunicReaper] " + ChatColor.GRAY + "Need "
					+ ChatColor.DARK_RED
					+ ((36 - playerAtGrave.checkPlayerInventoryItemstackCount()) - countItemsInItemstack(armor)) * -1
					+ ChatColor.GRAY + " more open slots to get your armor.");
			return false;
		} else {
			// Player has room in inventory!
			playerAtGrave.givePlayerItemStack(armor);
			playerAtGrave.sendMessageToPlayer(ChatColor.DARK_GRAY + "[RunicReaper] " + ChatColor.GRAY
					+ "You have retrieved your armor, if there was any to retrieve.");
			// remove data
			final Plugin instance = RunicParadise.getInstance();

			MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
			Connection connection = MySQL.openConnection();

			try {
				Statement statement = connection.createStatement();
				statement.executeUpdate(
						"UPDATE `rp_PlayerGraves` SET EquipBlob=null,LooterName='" + playerAtGrave.getPlayerName()
								+ "',LootTime=" + new Date().getTime() + " WHERE ID=" + graveID + ";");
				connection.close();
				return true;
			} catch (SQLException err) {
				getLogger().log(Level.SEVERE, "Error updating equipBlob because: " + err.getMessage());
				return false;
			}
		}
	}

	public static boolean handleLevels(int graveID, RunicPlayerBukkit playerAtGrave, int lostLevels, boolean isOwner) {

		if (isOwner && playerAtGrave.checkPlayerPermission("rp.graves.master")) {
			playerAtGrave.sendMessageToPlayer(ChatColor.DARK_GRAY + "[RunicReaper] " + ChatColor.GRAY
					+ "Returning 95% of the lost levels to you.");
			playerAtGrave.setPlayerLevel(playerAtGrave.getPlayerLevel() + (int) (0.95 * lostLevels));
		} else if (isOwner && playerAtGrave.checkPlayerPermission("rp.graves.champion")) {
			playerAtGrave.sendMessageToPlayer(ChatColor.DARK_GRAY + "[RunicReaper] " + ChatColor.GRAY
					+ "Returning 90% of the lost levels to you.");
			playerAtGrave.setPlayerLevel(playerAtGrave.getPlayerLevel() + (int) (0.90 * lostLevels));
		} else if (isOwner && playerAtGrave.checkPlayerPermission("rp.graves.warder")) {
			playerAtGrave.sendMessageToPlayer(ChatColor.DARK_GRAY + "[RunicReaper] " + ChatColor.GRAY
					+ "Returning 85% of the lost levels to you.");
			playerAtGrave.setPlayerLevel(playerAtGrave.getPlayerLevel() + (int) (0.85 * lostLevels));
		} else if (isOwner && playerAtGrave.checkPlayerPermission("rp.graves.slayer")) {
			playerAtGrave.sendMessageToPlayer(ChatColor.DARK_GRAY + "[RunicReaper] " + ChatColor.GRAY
					+ "Returning 80% of the lost levels to you.");
			playerAtGrave.setPlayerLevel(playerAtGrave.getPlayerLevel() + (int) (0.80 * lostLevels));
		} else if (isOwner && playerAtGrave.checkPlayerPermission("rp.graves.hunter")) {
			playerAtGrave.sendMessageToPlayer(ChatColor.DARK_GRAY + "[RunicReaper] " + ChatColor.GRAY
					+ "Returning 75% of the lost levels to you.");
			playerAtGrave.setPlayerLevel(playerAtGrave.getPlayerLevel() + (int) (0.75 * lostLevels));
		} else if (isOwner && playerAtGrave.checkPlayerPermission("rp.graves.guard")) {
			playerAtGrave.sendMessageToPlayer(ChatColor.DARK_GRAY + "[RunicReaper] " + ChatColor.GRAY
					+ "Returning 70% of the lost levels to you.");
			playerAtGrave.setPlayerLevel(playerAtGrave.getPlayerLevel() + (int) (0.70 * lostLevels));
		} else if (isOwner && playerAtGrave.checkPlayerPermission("rp.graves.keeper")) {
			playerAtGrave.sendMessageToPlayer(ChatColor.DARK_GRAY + "[RunicReaper] " + ChatColor.GRAY
					+ "Returning 65% of the lost levels to you.");
			playerAtGrave.setPlayerLevel(playerAtGrave.getPlayerLevel() + (int) (0.65 * lostLevels));
		} else if (isOwner && playerAtGrave.checkPlayerPermission("rp.graves.brawler")) {
			playerAtGrave.sendMessageToPlayer(ChatColor.DARK_GRAY + "[RunicReaper] " + ChatColor.GRAY
					+ "Returning 60% of the lost levels to you.");
			playerAtGrave.setPlayerLevel(playerAtGrave.getPlayerLevel() + (int) (0.60 * lostLevels));
		} else if (isOwner && playerAtGrave.checkPlayerPermission("rp.graves.singer")) {
			playerAtGrave.sendMessageToPlayer(ChatColor.DARK_GRAY + "[RunicReaper] " + ChatColor.GRAY
					+ "Returning 55% of the lost levels to you.");
			playerAtGrave.setPlayerLevel(playerAtGrave.getPlayerLevel() + (int) (0.55 * lostLevels));
		} else if (isOwner && playerAtGrave.checkPlayerPermission("rp.graves.runner")) {
			playerAtGrave.sendMessageToPlayer(ChatColor.DARK_GRAY + "[RunicReaper] " + ChatColor.GRAY
					+ "Returning 50% of the lost levels to you.");
			playerAtGrave.setPlayerLevel(playerAtGrave.getPlayerLevel() + (int) (0.50 * lostLevels));
		} else if (isOwner && playerAtGrave.checkPlayerPermission("rp.graves.seeker")) {
			playerAtGrave.sendMessageToPlayer(ChatColor.DARK_GRAY + "[RunicReaper] " + ChatColor.GRAY
					+ "Returning 45% of the lost levels to you.");
			playerAtGrave.setPlayerLevel(playerAtGrave.getPlayerLevel() + (int) (0.45 * lostLevels));
		} else {
			// PLAYER IS NOT OWNER
			playerAtGrave.sendMessageToPlayer(ChatColor.DARK_GRAY + "[RunicReaper] " + ChatColor.GRAY
					+ "You're not the owner of this grave. Returning 15% of the lost levels to you.");
			playerAtGrave.setPlayerLevel(playerAtGrave.getPlayerLevel() + (int) (0.15 * lostLevels));
			return true;
		}

		return true;
	}

	public static void restoreByCommand(String playerName, int graveNum) {

		RunicPlayerBukkit targetPlayer = new RunicPlayerBukkit(playerName);
		if (graveID.containsValue(graveNum)) {
			// grave exists

			// check ownership
			// Retrieve deathID
			final Plugin instance = RunicParadise.getInstance();
			MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
			Connection connection = MySQL.openConnection();

			try {
				Statement statement = connection.createStatement();
				ResultSet graveData = statement.executeQuery("SELECT * FROM `rp_PlayerGraves` WHERE `ID`=" + graveNum
						+ " AND `PlayerName` = '" + playerName + "' AND `Status` != 'Gone' ORDER BY `id` ASC LIMIT 1;");
				// if (!playerData.first() && !playerData.next()) {
				if (!graveData.isBeforeFirst()) {
					// Location doesn't exist in the DB!
					targetPlayer.sendMessageToPlayer("Could not find a grave you own with that ID");
				} else {
					// Location does exist in the DB and data retrieved!!
					graveData.next();
					String[] locParts = graveData.getString("Location").split("[\\x2E]");
					Location loc = new Location(Bukkit.getWorld(locParts[0]), Integer.parseInt(locParts[1]),
							Integer.parseInt(locParts[2]), Integer.parseInt(locParts[3]));
					graveTeleport(Bukkit.getPlayer(playerName), graveNum);
					restoreFromPlayerDeath(targetPlayer, loc);

				}
			} catch (SQLException z) {
				getLogger().log(Level.SEVERE, "Failed DB check for restoreonCommand cuz " + z.getMessage());
			}

		} else {
			targetPlayer.sendMessageToPlayer("Could not find a grave you own with that ID");
		}
	}

	public static void restoreFromPlayerDeath(RunicPlayerBukkit playerAtGrave, Location loc) {
		// Ownership check is performed in the interact event... if player gets
		// this far, they can access the grave!

		String locString = loc.getWorld().getName() + "." + loc.getBlockX() + "." + loc.getBlockY() + "."
				+ loc.getBlockZ();

		// Retrieve deathID
		final Plugin instance = RunicParadise.getInstance();
		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);

		String strToCheck = loc.getWorld().getName() + "." + loc.getBlockX() + "." + loc.getBlockY() + "."
				+ loc.getBlockZ();

		try {
			// TODO: Get data from DB based on UUID to protect vs name changes
			Connection connection = MySQL.openConnection();
			Statement statement = connection.createStatement();
			ResultSet graveData = statement.executeQuery("SELECT * FROM `rp_PlayerGraves` WHERE `Location` = '" + strToCheck
					+ "' AND `Status` != 'Gone' ORDER BY `id` ASC LIMIT 1;");
			// if (!playerData.first() && !playerData.next()) {
			if (!graveData.isBeforeFirst()) {
				// Location doesn't exist in the DB!
				getLogger().log(Level.SEVERE,
						"[RP] Failure in grave Restore.. couldnt find loc in the DB but prior check succeeded.");
			} else {
				// Location does exist in the DB and data retrieved!!
				graveData.next();

				String tempArmor = graveData.getString("ArmorItemStack").replace("\\'", "\'");
				ItemStack[] armor = InventorySerialization.getInventory(tempArmor, 4);

				String tempInv = graveData.getString("InvItemStack").replace("\\'", "\'");
				ItemStack[] inv = InventorySerialization.getInventory(tempInv, 37);

				String tempOffhand = graveData.getString("OffhandItemStack").replace("\\'", "\'");
				ItemStack offhand = SingleItemSerialization.getItem(tempOffhand);

				boolean isOwner = false;
				if (playerAtGrave.getPlayerName().equals(graveData.getString("PlayerName"))) {
					isOwner = true;
				}

				if (handleArmor(armor, playerAtGrave, graveData.getInt("ID"))
						&& handleItems(inv, playerAtGrave, graveData.getInt("ID"))
						&& handleOffhand(offhand, playerAtGrave, graveData.getInt("ID")) && handleLevels(
								graveData.getInt("ID"), playerAtGrave, graveData.getInt("LevelsLost"), isOwner)) {
					// Everything finished OK!
					// remove the rest of this data

					statement.executeUpdate(
							"UPDATE `rp_PlayerGraves` SET Status='Gone' WHERE ID=" + graveData.getInt("ID") + ";");

					loc.getWorld().playEffect(loc, Effect.MOBSPAWNER_FLAMES, 0);
					loc.getWorld().playEffect(loc, Effect.POTION_BREAK, 0);
					// loc.getWorld().playSound(loc, Sound.BLOCK_CHEST_CLOSE,
					// 10, 1);
					loc.getBlock().setType(Material.AIR);
					loc.add(0, 1, 0).getBlock().setType(Material.AIR);
					loc.getWorld().playEffect(loc, Effect.SMOKE, 0);

					// resync hashmap with graves to keep the redstone lamp
					// on...
					syncGraveLocations();
				}

				connection.close();
			}
		} catch (SQLException z) {
			getLogger().log(Level.SEVERE, "Failed DB check for restore grave cuz " + z.getMessage());
		}

	}

	public static void listDeaths(Player commandSender, String playerSearch) {

		final Plugin instance = RunicParadise.getInstance();
		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);

		try {
			Connection connection = MySQL.openConnection();
			Statement statement = connection.createStatement();
			String searchQuery = "";
			if (!playerSearch.equals("all")) {
				searchQuery = "SELECT * FROM `rp_PlayerGraves` WHERE `PlayerName` LIKE '%" + playerSearch
						+ "%' ORDER BY `id` DESC LIMIT 30;";
			} else {
				searchQuery = "SELECT * FROM `rp_PlayerGraves` ORDER BY `id` DESC LIMIT 30;";
			}
			ResultSet graveData = statement.executeQuery(searchQuery);
			// if (!playerData.first() && !playerData.next()) {
			if (!graveData.isBeforeFirst()) {
				// No results
				commandSender.sendMessage(ChatColor.DARK_GRAY + "[RunicReaper] " + ChatColor.GRAY
						+ " Searching for recent 30 graves owned by " + playerSearch);
				commandSender.sendMessage(ChatColor.GRAY + "No graves found.");
			} else {
				// results found!
				commandSender.sendMessage(ChatColor.DARK_GRAY + "[RunicReaper] " + ChatColor.GRAY
						+ " Searching for recent 30 graves owned by " + playerSearch);

				while (graveData.next()) {

					switch (graveData.getString("Status")) {
					case "Gone":
						/*new FancyMessage("ID").color(GRAY).then("" + graveData.getInt("ID")).color(AQUA).then(" ")
								.color(GRAY).then(graveData.getString("Status")).color(GREEN).then(", Owner ")
								.color(GRAY).then(graveData.getString("PlayerName")).color(YELLOW).then(", Looter ")
								.color(GRAY).then(graveData.getString("LooterName")).color(YELLOW).then(", ")
								.color(GRAY).then("Loc").color(GRAY).tooltip(graveData.getString("Location"))
								.send(commandSender);*/
						break;
					case "Locked":
						/*new FancyMessage("ID").color(GRAY).then("" + graveData.getInt("ID")).color(AQUA).then(" ")
								.color(GRAY).then(graveData.getString("Status")).color(YELLOW).then(", Owner ")
								.color(GRAY).then(graveData.getString("PlayerName")).color(YELLOW).then(", ")
								.color(GRAY).then("Loc").color(GRAY).tooltip(graveData.getString("Location"))
								.send(commandSender);*/
						break;
					case "Unlocked":
						/*new FancyMessage("ID").color(GRAY).then("" + graveData.getInt("ID")).color(AQUA).then(" ")
								.color(GRAY).then(graveData.getString("Status")).color(RED).then(", Owner ").color(GRAY)
								.then(graveData.getString("PlayerName")).color(YELLOW).then(", ").color(GRAY)
								.then("Loc").color(GRAY).tooltip(graveData.getString("Location")).send(commandSender);*/
						break;
					}

					/*
					 * commandSender.sendMessage(ChatColor.GRAY + "ID" +
					 * ChatColor.YELLOW + graveData.getInt("ID") +
					 * ChatColor.GRAY + ", Owner. " + ChatColor.YELLOW +
					 * graveData.getString("PlayerName") + ChatColor.GRAY + ", "
					 * + ChatColor.YELLOW + graveData.getString("Status") +
					 * ChatColor.GRAY + ", Looter " + ChatColor.YELLOW +
					 * graveData.getString("LooterName"));
					 */
				}

				connection.close();
			}

		} catch (SQLException z) {
			getLogger().log(Level.SEVERE, "Failed DB check for RunicDeathChest.ListDeaths cuz " + z.getMessage());
		}

	}

	public static void graveTeleport(Player sender, int ID) {

		final Plugin instance = RunicParadise.getInstance();
		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
		try {
			// TODO: Get data from DB based on UUID to protect vs name changes
			Connection connection = MySQL.openConnection();
			Statement statement = connection.createStatement();
			ResultSet graveData = statement.executeQuery(
					"SELECT * FROM `rp_PlayerGraves` WHERE `ID` = " + ID + " ORDER BY `id` DESC LIMIT 1;");
			// if (!playerData.first() && !playerData.next()) {
			if (!graveData.isBeforeFirst()) {
				// No results
				sender.sendMessage(ChatColor.DARK_GRAY + "[RunicReaper] " + ChatColor.GRAY
						+ "No graves found. Invalid grave ID specified.");
				connection.close();
				return;
			} else {
				// results found!
				graveData.next();
				sender.sendMessage(ChatColor.DARK_GRAY + "[RunicReaper] " + ChatColor.GRAY
						+ "Sending you to grave ID " + ChatColor.YELLOW + ID + ChatColor.GRAY + " owned by "
						+ ChatColor.YELLOW + graveData.getString("PlayerName"));
				if (graveData.getString("Status").equals("Gone")) {
					sender.sendMessage(ChatColor.GRAY + " This grave has been looted by " + ChatColor.YELLOW
							+ graveData.getString("LooterName") + ChatColor.GRAY + " @ " + ChatColor.YELLOW
							+ new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
									.format(new java.util.Date(Long.parseLong(graveData.getString("LootTime")))));
				}
				String[] locParts = graveData.getString("Location").split("[\\x2E]");
				sender.teleport(new Location(Bukkit.getWorld(locParts[0]), Integer.parseInt(locParts[1]),
						Integer.parseInt(locParts[2]), Integer.parseInt(locParts[3])));

				connection.close();
			}

		} catch (SQLException z) {
			getLogger().log(Level.SEVERE, "Failed DB check for restore grave cuz " + z.getMessage());
		}

	}

	public static void syncGraveLocations() {
		int graveCount = 0;

		// reset the hashmap
		graveLocations.clear();

		// retrieve updated grave data
		final Plugin instance = RunicParadise.getInstance();
		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
		try {
			Connection connection = MySQL.openConnection();
			Statement statement = connection.createStatement();
			ResultSet graveData = statement.executeQuery(
					"SELECT Location,ID,Status FROM rp_PlayerGraves WHERE Status != 'Gone' ORDER BY id DESC;");
			// if (!playerData.first() && !playerData.next()) {
			if (!graveData.isBeforeFirst()) {
				// No results
				// do nothing
				connection.close();
				return;
			} else {
				// results found!
				while (graveData.next()) {
					graveLocations.put(graveData.getInt("ID"), graveData.getString("Location"));
					graveID.put(graveData.getString("Location"), graveData.getInt("ID"));
					graveStatus.put(graveData.getInt("ID"), graveData.getString("Status"));
					graveCount++;
				}

				connection.close();

				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "sc " + graveCount + " graves loaded into memory!");
			}

		} catch (SQLException z) {
			getLogger().log(Level.SEVERE, "Failed map sync for grave locs cuz " + z.getMessage());
		}

	}

	public static String checkHashmapForDeathLoc(Location loc) {

		String strToCheck = loc.getWorld().getName() + "." + loc.getBlockX() + "." + loc.getBlockY() + "."
				+ loc.getBlockZ();

		if (graveLocations.containsValue(strToCheck)) {
			return (String) graveStatus.get(graveID.get(strToCheck));
		} else {
			return "NoGrave";
		}

	}

	public static void unlockExpiredGraves(boolean senderIsPlayer) {
		// check for expired graves - set their status to Unlocked
		// retrieve updated grave data
		final Plugin instance = RunicParadise.getInstance();
		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
		try {
			Connection connection = MySQL.openConnection();
			Statement statement = connection.createStatement();
			ResultSet graveData = statement.executeQuery("SELECT Location,ID FROM `rp_PlayerGraves` WHERE ExpiryTime<"
					+ new Date().getTime() + " AND Status='Locked' ORDER BY ID DESC;");
			int expiries = statement.executeUpdate("UPDATE `rp_PlayerGraves` SET Status='Unlocked' WHERE ExpiryTime<"
					+ new Date().getTime() + " AND Status='Locked';");

			if (expiries > 0) {
				getLogger().log(Level.INFO, "[RP] " + expiries + " graves unlocked due to timer expiry");
				if (senderIsPlayer) {
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
							"sc " + expiries + " graves unlocked due to timer expiry");
				}
			}

			connection.close();

			syncGraveLocations();

		} catch (SQLException z) {
			getLogger().log(Level.SEVERE, "Failed map sync for grave locs cuz " + z.getMessage());
		}

	}

	public static void unlockGrave(Player commandSender, int ID) {

		final Plugin instance = RunicParadise.getInstance();
		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
		try {
			Connection connection = MySQL.openConnection();
			Statement statement = connection.createStatement();
			ResultSet graveData = statement.executeQuery(
					"SELECT * FROM `rp_PlayerGraves` WHERE `ID` = " + ID + " ORDER BY `ID` DESC LIMIT 1;");
			// if (!playerData.first() && !playerData.next()) {
			if (!graveData.isBeforeFirst()) {
				// No results
				commandSender.sendMessage(ChatColor.DARK_GRAY + "[RunicReaper] " + ChatColor.GRAY
						+ "No graves found. Invalid grave ID specified.");
				connection.close();
				return;
			} else {
				// results found!
				graveData.next();
				if (graveData.getString("Status").equals("Locked")) {
					commandSender.sendMessage(ChatColor.DARK_GRAY + "[RunicReaper] " + ChatColor.GRAY
							+ "Unlocking grave ID " + ChatColor.YELLOW + ID + ChatColor.GRAY + " owned by "
							+ ChatColor.YELLOW + graveData.getString("PlayerName"));
					Statement cStmt = connection.createStatement();
					int tempC = cStmt
							.executeUpdate("UPDATE rp_PlayerGraves SET Status='Unlocked' WHERE ID=" + ID + ";");

				} else {
					// Grave isnt locked, so cant unlock it
					commandSender.sendMessage(ChatColor.DARK_GRAY + "[RunicReaper] " + ChatColor.GRAY
							+ "That grave isn't locked! Can't unlock it.");
				}

				connection.close();
			}

		} catch (SQLException z) {
			getLogger().log(Level.SEVERE, "Failed DB check for restore grave cuz " + z.getMessage());
		}

	}

	static void savePlayerDeath_v19(Player player, Location loc) {
		final Plugin instance = RunicParadise.getInstance();
		boolean criticalFail = false;

		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
		Connection connection = MySQL.openConnection();

		// define which item types we'll put into the grave
		Integer[] protectedItemsTemp = new Integer[] { 0, 443, 6, 8, 9, 10, 11, 18, 51, 176, 177, 321, 340, 358, 386,
				387, 395, 397, 401, 402, 403, 425, 442 };
		ArrayList<Integer> protectedItems = new ArrayList<>(Arrays.asList(protectedItemsTemp));

		// prepare array of ItemStack to serialize
		ArrayList<ItemStack> itemBuilder = new ArrayList<>();
		int invSlot = 0;
		int itemCounter = 0;

		// Gather all items in player's inventory, stay inside the 36 slots - no
		// offhand/armor
		while (invSlot < 36) {
			if (player.getInventory().getItem(invSlot) != null) {
				if (!protectedItems.contains(player.getInventory().getItem(invSlot).getType().getId()))
					itemBuilder.add(player.getInventory().getItem(invSlot));
				itemCounter++;
			}
			invSlot++;
		}

		invSlot = 0;
		int signCounter = 0;

		// Now iterate through the ArrayList and build our final ItemStack[] for
		// serialization
		ItemStack[] graveItems = new ItemStack[itemCounter];
		for (ItemStack i : itemBuilder) {
			graveItems[invSlot] = i;

			invSlot++;
			signCounter++;
			getLogger().log(Level.INFO, signCounter + " item saved. " + i.getType().toString());

		}

		// ////////////////////////////////////
		// Now handle armor + offhand
		// prepare array of ItemStack to serialize
		ArrayList<ItemStack> equipBuilder = new ArrayList<>();
		int equipCounter = 0;

		// Gather all items in player's inventory, stay inside the 36 slots - no
		// offhand/armor
		if (player.getInventory().getChestplate() != null) {
			if (!protectedItems.contains(player.getInventory().getChestplate().getType().getId())) {
				equipBuilder.add(player.getInventory().getChestplate());

				signCounter++;
				equipCounter++;

				getLogger().log(Level.INFO, signCounter + " armor saved. ");
				player.getInventory().setChestplate(null);
			}
		}
		if (player.getInventory().getBoots() != null) {
			equipBuilder.add(player.getInventory().getBoots());

			signCounter++;
			equipCounter++;
			getLogger().log(Level.INFO, signCounter + " armor saved. ");
			player.getInventory().setBoots(null);
		}

		if (player.getInventory().getHelmet() != null) {
			if (!protectedItems.contains(player.getInventory().getHelmet().getType().getId())) {
				equipBuilder.add(player.getInventory().getHelmet());
				signCounter++;
				equipCounter++;
				getLogger().log(Level.INFO, signCounter + " armor saved. ");
				player.getInventory().setHelmet(null);
			}
		}
		if (player.getInventory().getLeggings() != null) {
			equipBuilder.add(player.getInventory().getLeggings());
			signCounter++;
			equipCounter++;
			getLogger().log(Level.INFO, signCounter + " armor saved. ");
			player.getInventory().setLeggings(null);
		}
		if (player.getInventory().getItemInOffHand() != null) {
			if (!protectedItems.contains(player.getInventory().getItemInOffHand().getType().getId())) {
				equipBuilder.add(player.getInventory().getItemInOffHand());
				signCounter++;
				equipCounter++;
				getLogger().log(Level.INFO, signCounter + " offhand saved. ");
				player.getInventory().setItemInOffHand(null);
			}
		}

		// Now iterate through the ArrayList and build our final ItemStack[] for
		// serialization
		int indexCounter = 0;
		ItemStack[] equipItems = new ItemStack[equipCounter];
		for (ItemStack i : equipBuilder) {
			equipItems[indexCounter] = i;
			indexCounter++;
		}

		// 12hr expiry time
		long expiryTime = new Date().getTime() + 43200000;

		String originalBlockString = loc.getBlock().getType().toString();

		if (originalBlockString.equals("CHEST") || originalBlockString.equals("SIGN_POST")
				|| originalBlockString.equals("WALL_SIGN")) {

			double newX = 0;
			double newY = -1;
			double newZ = 0;

			Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
					"sc STARTING " + loc.getX() + " " + loc.getY() + " " + loc.getZ() + ": ");

			while (newX < 2) {
				while (newY < 4) {
					while (newZ < 2) {

						String checkBlock = new Location(loc.getWorld(), loc.getX() + newX, loc.getY() + newY,
								loc.getZ() + newZ).getBlock().getType().toString();

						if (checkBlock.equals("AIR")) {
							if (new Location(loc.getWorld(), loc.getX() + newX, loc.getY() + newY + 1,
									loc.getZ() + newZ).getBlock().getType().toString().equals("AIR")) {

								// found a good spot!! change the death loc to
								// that!
								loc = new Location(loc.getWorld(), loc.getX() + newX, loc.getY() + newY,
										loc.getZ() + newZ);
								newX = 3;
								newY = 3;
								newZ = 3;
							}

						}
						newZ++;
					}
					newY++;
				}
				newX++;
			}

		}

		String locString = loc.getWorld().getName() + "." + loc.getBlockX() + "." + loc.getBlockY() + "."
				+ loc.getBlockZ();

		// /////////////
		try {

			PreparedStatement eStmt = connection.prepareStatement("INSERT INTO rp_PlayerGraves "
					+ "(`Location`, `Status`, `PlayerName`, `UUID`, `ExpiryTime`, `CreationTime`, "
					// + "`LevelsLost`, `LooterName`, `LootTime`,
					// `PreviousBlock`, `InvBlob`, `EquipBlob`, `InvItemStack`,
					// `ArmorItemStack`) VALUES "
					+ "`LevelsLost`, `LooterName`, `LootTime`, `PreviousBlock`) VALUES "
					// + "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);",
					// Statement.RETURN_GENERATED_KEYS);
					+ "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?);", Statement.RETURN_GENERATED_KEYS);

			eStmt.setString(1, locString);
			eStmt.setString(2, "Locked");
			eStmt.setString(3, player.getName());
			eStmt.setString(4, player.getUniqueId().toString());
			eStmt.setLong(5, expiryTime);
			eStmt.setLong(6, new Date().getTime());
			eStmt.setInt(7, player.getLevel());
			eStmt.setString(8, null);
			eStmt.setString(9, null);
			eStmt.setString(10, loc.getBlock().getType().toString());
			// eStmt.setObject(11,
			// RunicSerialization.serializeItemStackList(graveItems));
			// eStmt.setObject(12,
			// RunicSerialization.serializeItemStackList(equipItems));
			// eStmt.setString(13,
			// RunicSerialization.serializeItemStackList(graveItems).toString());
			// eStmt.setString(14,
			// RunicSerialization.serializeItemStackList(equipItems).toString());

			int affectedRows = eStmt.executeUpdate();
			long newGraveID = 0;

			if (affectedRows == 0) {
				throw new SQLException("Creating user failed, no rows affected.");
			}

			try (ResultSet generatedKeys = eStmt.getGeneratedKeys()) {
				if (generatedKeys.next()) {
					newGraveID = generatedKeys.getLong(1);
					saveItemsToYML(graveItems, newGraveID, Bukkit.getOfflinePlayer(player.getUniqueId()), "Inventory");
					saveItemsToYML(equipItems, newGraveID, Bukkit.getOfflinePlayer(player.getUniqueId()), "Equipment");
				} else {
					throw new SQLException("Retrieving new grave ID failed. no ID obtained.");
				}
			}

			/*
			 * int graveID = 0; try (ResultSet generatedKeys =
			 * eStmt.getGeneratedKeys()) { if (generatedKeys.next()) { graveID =
			 * generatedKeys.getInt(1); } else { throw new SQLException(
			 * "Creating grave FAILED, could not retrieve new grave ID!"); } }
			 */
			eStmt.close();
			connection.close();
		} catch (SQLException err) {
			Bukkit.getLogger().log(Level.SEVERE,
					"Cant create new row Grave for " + player.getName() + " because: " + err.getMessage());

			criticalFail = true;
		}

		// USE CAUTION WITH TESTING !!!

		if (!criticalFail) {

			// player.getInventory().clear();
			int slotChecker = 0;
			while (slotChecker < 36) {
				if (player.getInventory().getItem(slotChecker) != null
						&& !protectedItems.contains(player.getInventory().getItem(slotChecker).getType().getId())) {
					player.getInventory().setItem(slotChecker, null);
				}
				slotChecker++;
			}

			// player.getInventory().setHelmet(null);
			// player.getInventory().setChestplate(null);
			// player.getInventory().setLeggings(null);
			// player.getInventory().setBoots(null);
			player.setLevel(0);

			// resync hashmap with graves
			syncGraveLocations();

			loc.getBlock().setType(Material.BEDROCK);
			Block b = loc.add(0, 1, 0).getBlock();
			b.setType(Material.SIGN);
			BlockState state = b.getState();
			Sign sign = (Sign) state;

			// To set
			sign.setLine(0, ChatColor.DARK_RED + "☠ ☠ GRAVE ☠ ☠");
			sign.setLine(1, "RIP");
			sign.setLine(2, player.getDisplayName());
			sign.setLine(3, ChatColor.DARK_RED + "" + signCounter + " items");
			sign.update(true);

			player = null;
		} else {

			// grave failed to create !! abort !!

			RunicMessaging.sendMessage(player, RunicMessaging.RunicFormat.SYSTEM, ChatColor.DARK_RED
					+ "Failed to create a grave. Don't worry, you got to keep your items and exp. Rune will look into this!");

			// create a debug file

			try {

				File announceFile = new File(Bukkit.getServer().getPluginManager().getPlugin("RunicParadise")
						.getDataFolder().getAbsolutePath(),
						"Grave_Failure" + new Date().getTime() + player.getName() + ".yml");

				announceFile.createNewFile();
				FileConfiguration announceConfig = YamlConfiguration.loadConfiguration(announceFile);

				DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
				Date date = new Date();
				// System.out.println(dateFormat.format(date)); //2014/08/06
				// 15:59:48

				announceConfig.set("Info.Timestamp", dateFormat.format(date));
				announceConfig.set("Info.Player.Name", player.getName());
				announceConfig.set("Info.Player.UUID", player.getUniqueId());

				/*
				 * try { announceConfig.set("Grave.Items.Serialized",
				 * RunicSerialization.serializeItemStackList(graveItems)); }
				 * catch (Exception t) {
				 * announceConfig.set("Grave.Items.Serialized",
				 * t.getStackTrace().toString()); }
				 * 
				 * try { announceConfig.set("Grave.Items.String",
				 * RunicSerialization.serializeItemStackList(graveItems).
				 * toString()); } catch (Exception w) {
				 * announceConfig.set("Grave.Items.String",
				 * w.getStackTrace().toString()); }
				 * 
				 * try { announceConfig.set("Grave.Equip.Serialized",
				 * RunicSerialization.serializeItemStackList(equipItems)); }
				 * catch (Exception q) {
				 * announceConfig.set("Grave.Equip.Serialized",
				 * q.getStackTrace().toString()); }
				 * 
				 * try { announceConfig.set("Grave.Equip.String",
				 * RunicSerialization.serializeItemStackList(equipItems).
				 * toString()); } catch (Exception c) {
				 * announceConfig.set("Grave.Equip.String",
				 * c.getStackTrace().toString()); }
				 */

				announceConfig.save(announceFile);

				Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
						"mail send runelynx Grave failed to create. Check debug file in RunicParadise folder. "
								+ "Grave_Failure" + new Date().getTime() + player.getName());
			} catch (IOException m) {

				Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
						"sc Couldn't create the grave Failed file ... " + m.getMessage());
			}

		}

	}

	public static void restoreFromPlayerDeath_v19(RunicPlayerBukkit playerAtGrave, Location loc) {
		// Ownership check is performed in the interact event... if player gets
		// this far, they can access the grave!

		// Retrieve deathID
		final Plugin instance = RunicParadise.getInstance();
		MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);

		String strToCheck = loc.getWorld().getName() + "." + loc.getBlockX() + "." + loc.getBlockY() + "."
				+ loc.getBlockZ();

		try {
			// TODO: Get data from DB based on UUID to protect vs name changes
			Connection connection = MySQL.openConnection();
			Statement statement = connection.createStatement();
			ResultSet graveData = statement.executeQuery("SELECT * FROM `rp_PlayerGraves` WHERE `Location` = '" + strToCheck
					+ "' AND `Status` != 'Gone' ORDER BY `id` ASC LIMIT 1;");
			// if (!playerData.first() && !playerData.next()) {
			if (!graveData.isBeforeFirst()) {
				// Location doesn't exist in the DB!
				getLogger().log(Level.SEVERE,
						"[RP] Failure in grave Restore.. couldnt find loc in the DB but prior check succeeded.");
			} else {
				getLogger().log(Level.INFO, "[RP] Checked for grave and found it in the DB.");
				// Location does exist in the DB and data retrieved!!
				graveData.next();
				ItemStack[] equip = null;
				ItemStack[] items = null;

				// Account for graves left in world before I changed from DB to
				// file storage logic
				if (graveData.getInt("ID") > 15320) {

					equip = loadItemsFromYML(graveData.getInt("ID"), "Equipment");
					items = loadItemsFromYML(graveData.getInt("ID"), "Inventory");
				} else {

					if (graveData.getObject("InvBlob") != null) {
						byte[] st = (byte[]) graveData.getObject("InvBlob");
						ByteArrayInputStream baip = new ByteArrayInputStream(st);
						ObjectInputStream ois = new ObjectInputStream(baip);
						items = RunicSerialization.deserializeItemStackList(
								((List<HashMap<Map<String, Object>, Map<String, Object>>>) ois.readObject()));
						getLogger().log(Level.INFO, "[RP] InvBlob is not null.");
					} else {
						getLogger().log(Level.INFO, "[RP] InvBlob is null.");
					}

					if (graveData.getObject("EquipBlob") != null) {
						byte[] st2 = (byte[]) graveData.getObject("EquipBlob");
						ByteArrayInputStream baip2 = new ByteArrayInputStream(st2);
						ObjectInputStream ois2 = new ObjectInputStream(baip2);
						equip = RunicSerialization.deserializeItemStackList(
								((List<HashMap<Map<String, Object>, Map<String, Object>>>) ois2.readObject()));
						getLogger().log(Level.INFO, "[RP] EquipBlob is not null.");
					} else {
						getLogger().log(Level.INFO, "[RP] EquipBlob is null.");
					}

				}

				boolean isOwner = false;
				if (playerAtGrave.getPlayerName().equals(graveData.getString("PlayerName"))) {
					isOwner = true;
				}

				if (handleArmor(equip, playerAtGrave, graveData.getInt("ID"))
						&& handleItems(items, playerAtGrave, graveData.getInt("ID")) && handleLevels(
								graveData.getInt("ID"), playerAtGrave, graveData.getInt("LevelsLost"), isOwner)) {
					// Everything finished OK!
					// remove the rest of this data

					statement.executeUpdate(
							"UPDATE `rp_PlayerGraves` SET Status='Gone' WHERE ID=" + graveData.getInt("ID") + ";");

					loc.getWorld().playEffect(loc, Effect.MOBSPAWNER_FLAMES, 0);
					loc.getWorld().playEffect(loc, Effect.POTION_BREAK, 0);
					// loc.getWorld().playSound(loc, Sound.BLOCK_CHEST_CLOSE,
					// 10, 1);
					loc.getBlock().setType(Material.AIR);
					loc.add(0, 1, 0).getBlock().setType(Material.AIR);
					loc.getWorld().playEffect(loc, Effect.SMOKE, 0);

					// resync hashmap with graves to keep the redstone lamp
					// on...
					syncGraveLocations();
				}

				connection.close();
			}
		} catch (SQLException | IOException | ClassNotFoundException z) {
			getLogger().log(Level.SEVERE, "Failed DB check for restore grave cuz " + z.getMessage());
		}
		playerAtGrave = null;

	}

	private static boolean saveItemsToYML(ItemStack[] itemList, long graveID, OfflinePlayer op, String type) {

		/// INVENTORY
		if (type.equals("Inventory")) {
			String filename = graveID + "-inventory.yml";
			File graveFile = new File("/home/AMP/.ampdata/instances/Survival/Minecraft/plugins/RunicParadise/graves",
					filename);
			FileConfiguration graveConfig = YamlConfiguration.loadConfiguration(graveFile);

			graveConfig.set("Owner", op.getUniqueId().toString());
			graveConfig.createSection("Items");
			int itemCounter = 1;
			String tempPath = "";

			try {

				for (ItemStack i : itemList) {
					tempPath = "Items." + itemCounter;
					graveConfig.set(tempPath, i);
					itemCounter++;
				}

				graveConfig.set("ItemCount", itemCounter);
				graveConfig.save(graveFile);

				Bukkit.getLogger().log(Level.INFO, "Saved grave [INV] file for " + op.getName() + " "
						+ op.getUniqueId().toString() + " with " + itemCounter + " items");

			} catch (IOException e) {
				Bukkit.getLogger().log(Level.INFO, "*FAILURE* Saving grave [INV] file for " + op.getName() + " "
						+ op.getUniqueId().toString() + " with " + itemCounter + " items");
				e.printStackTrace();

			}

			/// EQUIPMENT
		} else if (type.equals("Equipment")) {
			String filename = graveID + "-equipment.yml";
			File graveFile = new File("/home/AMP/.ampdata/instances/Survival/Minecraft/plugins/RunicParadise/graves",
					filename);
			FileConfiguration graveConfig = YamlConfiguration.loadConfiguration(graveFile);

			graveConfig.set("Owner", op.getUniqueId().toString());
			graveConfig.createSection("Items");
			int itemCounter = 1;
			String tempPath = "";
			
			try {

				for (ItemStack i : itemList) {
					tempPath = "Items." + itemCounter;
					graveConfig.set(tempPath, i);
					itemCounter++;
				}
				graveConfig.set("ItemCount", itemCounter);
				graveConfig.save(graveFile);

				Bukkit.getLogger().log(Level.INFO, "Saved grave [EQU] file for " + op.getName() + " "
						+ op.getUniqueId().toString() + " with " + itemCounter + " items");

			} catch (IOException e) {
				Bukkit.getLogger().log(Level.INFO, "*FAILURE* Saving grave [EQU] file for " + op.getName() + " "
						+ op.getUniqueId().toString() + " with " + itemCounter + " items");
				e.printStackTrace();

			}
		}

		return true;
	}

	private static ItemStack[] loadItemsFromYML(long graveID, String type) {
		if (type.equals("Inventory")) {

			String filename = graveID + "-inventory.yml";
			File graveFile = new File("/home/AMP/.ampdata/instances/Survival/Minecraft/plugins/RunicParadise/graves",
					filename);
			FileConfiguration graveConfig = YamlConfiguration.loadConfiguration(graveFile);

			Bukkit.getLogger().log(Level.INFO, filename);


			List<ItemStack> inv = (List<ItemStack>) graveConfig.getList("Items");
			return inv.toArray(new ItemStack[0]);
		} else if (type.equals("Equipment")) {
			String filename = graveID + "-equipment.yml";
			File graveFile = new File("/home/AMP/.ampdata/instances/Survival/Minecraft/plugins/RunicParadise/graves",
					filename);
			FileConfiguration graveConfig = YamlConfiguration.loadConfiguration(graveFile);

			Bukkit.getLogger().log(Level.INFO, filename);

			List<ItemStack> armor = (List<ItemStack>) graveConfig.getList("Items");
			return armor.toArray(new ItemStack[0]);
		}

		return null;
	}
}
