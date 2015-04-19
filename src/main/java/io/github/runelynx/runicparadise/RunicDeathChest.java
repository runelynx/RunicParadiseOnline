package io.github.runelynx.runicparadise;

import static org.bukkit.Bukkit.getLogger;
import static org.bukkit.ChatColor.AQUA;
import static org.bukkit.ChatColor.DARK_AQUA;
import static org.bukkit.ChatColor.DARK_RED;
import static org.bukkit.ChatColor.GOLD;
import static org.bukkit.ChatColor.GRAY;
import static org.bukkit.ChatColor.GREEN;
import static org.bukkit.ChatColor.RED;
import static org.bukkit.ChatColor.UNDERLINE;
import static org.bukkit.ChatColor.WHITE;
import static org.bukkit.ChatColor.YELLOW;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Level;

import mkremins.fanciful.FancyMessage;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;

import com.kill3rtaco.tacoserialization.InventorySerialization;

public class RunicDeathChest {

	private static HashMap<Long, ItemStack[]> deathArmor = new HashMap<Long, ItemStack[]>();
	private static HashMap<Long, ItemStack[]> deathInventory = new HashMap<Long, ItemStack[]>();
	private static HashMap<Integer, String> graveLocations = new HashMap<Integer, String>();
	private static HashMap<Integer, String> graveStatus = new HashMap<Integer, String>();
	private static HashMap<String, Integer> graveID = new HashMap<String, Integer>();

	public static String checkLocForDeath(Location loc) {
		final Plugin instance = RunicParadise.getInstance();
		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));

		String strToCheck = loc.getWorld().getName() + "." + loc.getBlockX()
				+ "." + loc.getBlockY() + "." + loc.getBlockZ();

		try {
			// TODO: Get data from DB based on UUID to protect vs name changes
			final Connection d = MySQL.openConnection();
			Statement dStmt = d.createStatement();
			ResultSet graveData = dStmt
					.executeQuery("SELECT PlayerName,Status FROM `rp_PlayerGraves` WHERE Location = '"
							+ strToCheck
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
					d.close();
					return pname;
				} else {
					// Grave is OPEN!
					d.close();
					return "Unlocked";
				}

			}
		} catch (SQLException z) {
			getLogger()
					.log(Level.SEVERE,
							"Failed DB check for grave locations cuz "
									+ z.getMessage());
		}
		return "NoGrave";

	}

	public static int countItemsInItemstack(ItemStack[] stack) {
		int count = 0;

		for (ItemStack item : stack) {
			if ((item != null) && (item.getAmount() > 0)
					&& (item.getType() != Material.AIR)) {
				count++;
			}
		}

		return count;
	}

	public static void savePlayerDeath(Player player, Location loc) {
		final Plugin instance = RunicParadise.getInstance();

		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));
		final Connection e = MySQL.openConnection();
		// Establish ID for new storing this death
		long newDeathID = new Date().getTime();

		String locString = loc.getWorld().getName() + "." + loc.getBlockX()
				+ "." + loc.getBlockY() + "." + loc.getBlockZ();

		// Store this death to hashmaps (type ItemStack[])
		deathArmor.put(newDeathID, player.getInventory().getArmorContents());
		deathInventory.put(newDeathID, player.getInventory().getContents());

		// 48hr expiry time
		long expiryTime = new Date().getTime() + 172800000;

		String armorString = InventorySerialization
				.serializeInventoryAsString(deathArmor.get(newDeathID));
		String invString = InventorySerialization
				.serializeInventoryAsString(deathInventory.get(newDeathID));

		// /////////////
		try {
			// Statement eStmt = e.createStatement();

			PreparedStatement eStmt = e
					.prepareStatement("INSERT INTO rp_PlayerGraves (`Location`, `Status`, `PlayerName`, `ExpiryTime`, `CreationTime`, `LevelsLost`, `ArmorItemStack`, `InvItemStack`, `LooterName`, `LootTime`, `PreviousBlock`) VALUES "
							+ "('"
							+ locString
							+ "', 'Locked', '"
							+ player.getName()
							+ "', "
							+ expiryTime
							+ ", "
							+ new Date().getTime()
							+ ", '"
							+ player.getLevel()
							+ "', ?, ?, null, null, '"
							+ loc.getBlock().getType().toString() + "');");
			eStmt.setString(1, armorString);
			eStmt.setString(2, invString);

			eStmt.executeUpdate();
		} catch (SQLException err) {
			Bukkit.getLogger().log(
					Level.SEVERE,
					"Cant create new row Grave for " + player.getName()
							+ " because: " + err.getMessage());
		}

		// USE CAUTION WITH TESTING !!!
		player.getInventory().clear();
		player.getInventory().setHelmet(null);
		player.getInventory().setChestplate(null);
		player.getInventory().setLeggings(null);
		player.getInventory().setBoots(null);
		player.setLevel(0);

		// resync hashmap with graves to keep the redstone lamp on...
		syncGraveLocations();

		loc.getBlock().setType(Material.REDSTONE_LAMP_ON);
		Block b = loc.add(0, 1, 0).getBlock();
		b.setType(Material.SIGN_POST);
		BlockState state = b.getState();
		Sign sign = (Sign) state;

		// To set
		sign.setLine(0, ChatColor.DARK_RED + "☠ ☠ GRAVE ☠ ☠");
		sign.setLine(1, "RIP");
		sign.setLine(2, player.getDisplayName());
		sign.setLine(3, ChatColor.DARK_RED + "☠ ☠ ☠ ☠ ☠ ☠ ☠ ☠ ☠");
		sign.update(true);

	}

	// the Handle methods return true if all is well... false if there is a
	// failure.
	public static boolean handleItems(ItemStack[] items,
			RunicPlayerBukkit playerAtGrave, int graveID) {

		// If the items slots from death storage cant fit in the player's
		// free inventory slots...
		if (((36 - playerAtGrave.checkPlayerInventoryItemstackCount()) - countItemsInItemstack(items)) < 0) {
			playerAtGrave
					.sendMessageToPlayer(ChatColor.DARK_GRAY
							+ "[RunicReaper] "
							+ ChatColor.GRAY
							+ "Need "
							+ ChatColor.DARK_RED
							+ ((36 - playerAtGrave
									.checkPlayerInventoryItemstackCount()) - countItemsInItemstack(items))
							* -1 + ChatColor.GRAY
							+ " more open slots to get your items.");
			return false;
		} else {
			// Player has room in inventory!
			playerAtGrave.givePlayerItemStack(items);

			playerAtGrave.sendMessageToPlayer(ChatColor.DARK_GRAY
					+ "[RunicReaper] " + ChatColor.GRAY
					+ "You have retrieved your items.");
			// remove data
			final Plugin instance = RunicParadise.getInstance();

			MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
					"dbHost"), instance.getConfig().getString("dbPort"),
					instance.getConfig().getString("dbDatabase"), instance
							.getConfig().getString("dbUser"), instance
							.getConfig().getString("dbPassword"));
			final Connection d = MySQL.openConnection();
			String emptyJSON = "";
			try {
				emptyJSON = "[{\"amount\":0,\"id\":0,\"index\":0,\"data\":-1}]";
				Statement dStmt = d.createStatement();
				dStmt.executeUpdate("UPDATE `rp_PlayerGraves` SET InvItemStack='"
						+ emptyJSON
						+ "',LooterName='"
						+ playerAtGrave.getPlayerName()
						+ "',LootTime="
						+ new Date().getTime() + " WHERE ID=" + graveID + ";");
				d.close();
				return true;
			} catch (SQLException err) {
				getLogger().log(
						Level.SEVERE,
						"Error updating invItemStack because: "
								+ err.getMessage());
				return false;
			}

		}

	}

	public static boolean handleArmor(ItemStack[] armor,
			RunicPlayerBukkit playerAtGrave, int graveID) {

		// If the armor slots from death storage cant fit in the player's
		// free inventory slots...
		if (((36 - playerAtGrave.checkPlayerInventoryItemstackCount()) - countItemsInItemstack(armor)) < 0) {
			playerAtGrave
					.sendMessageToPlayer(ChatColor.DARK_GRAY
							+ "[RunicReaper] "
							+ ChatColor.GRAY
							+ "Need "
							+ ChatColor.DARK_RED
							+ ((36 - playerAtGrave
									.checkPlayerInventoryItemstackCount()) - countItemsInItemstack(armor))
							* -1 + ChatColor.GRAY
							+ " more open slots to get your armor.");
			return false;
		} else {
			// Player has room in inventory!
			playerAtGrave.givePlayerItemStack(armor);
			playerAtGrave.sendMessageToPlayer(ChatColor.DARK_GRAY
					+ "[RunicReaper] " + ChatColor.GRAY
					+ "You have retrieved your armor.");
			// remove data
			final Plugin instance = RunicParadise.getInstance();

			MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
					"dbHost"), instance.getConfig().getString("dbPort"),
					instance.getConfig().getString("dbDatabase"), instance
							.getConfig().getString("dbUser"), instance
							.getConfig().getString("dbPassword"));
			final Connection d = MySQL.openConnection();
			String emptyJSON = "";
			try {
				emptyJSON = "[{\"amount\":0,\"id\":0,\"index\":0,\"data\":-1}]";
				Statement dStmt = d.createStatement();
				dStmt.executeUpdate("UPDATE `rp_PlayerGraves` SET ArmorItemStack='"
						+ emptyJSON
						+ "',LooterName='"
						+ playerAtGrave.getPlayerName()
						+ "',LootTime="
						+ new Date().getTime() + " WHERE ID=" + graveID + ";");
				d.close();
				return true;
			} catch (SQLException err) {
				getLogger().log(
						Level.SEVERE,
						"Error updating armorItemStack because: "
								+ err.getMessage());
				return false;
			}

		}

	}

	public static boolean handleLevels(int graveID,
			RunicPlayerBukkit playerAtGrave, int lostLevels, boolean isOwner) {

		if (isOwner
				&& playerAtGrave.checkPlayerPermission("rp.graves.levels50")) {
			// PLAYER IS OWNER AND ONLY GETS 50% EXP
			playerAtGrave.sendMessageToPlayer(ChatColor.DARK_GRAY
					+ "[RunicReaper] " + ChatColor.GRAY
					+ "Returning 50% of the lost levels to you.");
			playerAtGrave.setPlayerLevel(playerAtGrave.getPlayerLevel()
					+ (int) (0.50 * lostLevels));
		} else if (isOwner
				&& playerAtGrave.checkPlayerPermission("rp.graves.levels60")) {
			playerAtGrave.sendMessageToPlayer(ChatColor.DARK_GRAY
					+ "[RunicReaper] " + ChatColor.GRAY
					+ "Returning 60% of the lost levels to you.");
			playerAtGrave.setPlayerLevel(playerAtGrave.getPlayerLevel()
					+ (int) (0.60 * lostLevels));
		} else if (isOwner
				&& playerAtGrave.checkPlayerPermission("rp.graves.levels70")) {
			playerAtGrave.sendMessageToPlayer(ChatColor.DARK_GRAY
					+ "[RunicReaper] " + ChatColor.GRAY
					+ "Returning 70% of the lost levels to you.");
			playerAtGrave.setPlayerLevel(playerAtGrave.getPlayerLevel()
					+ (int) (0.70 * lostLevels));
		} else if (isOwner
				&& playerAtGrave.checkPlayerPermission("rp.graves.levels75")) {
			playerAtGrave.sendMessageToPlayer(ChatColor.DARK_GRAY
					+ "[RunicReaper] " + ChatColor.GRAY
					+ "Returning 75% of the lost levels to you.");
			playerAtGrave.setPlayerLevel(playerAtGrave.getPlayerLevel()
					+ (int) (0.75 * lostLevels));
		} else {
			// PLAYER IS NOT OWNER
			playerAtGrave
					.sendMessageToPlayer(ChatColor.DARK_GRAY
							+ "[RunicReaper] "
							+ ChatColor.GRAY
							+ "You're not the owner of this grave. Returning 15% of the lost levels to you.");
			playerAtGrave.setPlayerLevel(playerAtGrave.getPlayerLevel()
					+ (int) (0.15 * lostLevels));
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
			MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
					"dbHost"), instance.getConfig().getString("dbPort"),
					instance.getConfig().getString("dbDatabase"), instance
							.getConfig().getString("dbUser"), instance
							.getConfig().getString("dbPassword"));
			final Connection d = MySQL.openConnection();

			try {
				Statement dStmt = d.createStatement();
				ResultSet graveData = dStmt
						.executeQuery("SELECT * FROM `rp_PlayerGraves` WHERE `ID`="
								+ graveNum
								+ " AND `PlayerName` = '"
								+ playerName
								+ "' AND `Status` != 'Gone' ORDER BY `id` ASC LIMIT 1;");
				// if (!playerData.first() && !playerData.next()) {
				if (!graveData.isBeforeFirst()) {
					// Location doesn't exist in the DB!
					targetPlayer
							.sendMessageToPlayer("Could not find a grave you own with that ID");
				} else {
					// Location does exist in the DB and data retrieved!!
					graveData.next();
					String[] locParts = graveData.getString("Location").split(
							"[\\x2E]");
					Location loc = new Location(Bukkit.getWorld(locParts[0]),
							Integer.parseInt(locParts[1]),
							Integer.parseInt(locParts[2]),
							Integer.parseInt(locParts[3]));
					graveTeleport(Bukkit.getPlayer(playerName), graveNum);
					restoreFromPlayerDeath(targetPlayer, loc);

				}
			} catch (SQLException z) {
				getLogger().log(
						Level.SEVERE,
						"Failed DB check for restoreonCommand cuz "
								+ z.getMessage());
			}

		} else {
			targetPlayer
					.sendMessageToPlayer("Could not find a grave you own with that ID");
		}
	}

	public static void restoreFromPlayerDeath(RunicPlayerBukkit playerAtGrave,
			Location loc) {
		// Ownership check is performed in the interact event... if player gets
		// this far, they can access the grave!

		String locString = loc.getWorld().getName() + "." + loc.getBlockX()
				+ "." + loc.getBlockY() + "." + loc.getBlockZ();

		// Retrieve deathID
		final Plugin instance = RunicParadise.getInstance();
		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));

		String strToCheck = loc.getWorld().getName() + "." + loc.getBlockX()
				+ "." + loc.getBlockY() + "." + loc.getBlockZ();

		try {
			// TODO: Get data from DB based on UUID to protect vs name changes
			final Connection d = MySQL.openConnection();
			Statement dStmt = d.createStatement();
			ResultSet graveData = dStmt
					.executeQuery("SELECT * FROM `rp_PlayerGraves` WHERE `Location` = '"
							+ strToCheck
							+ "' AND `Status` != 'Gone' ORDER BY `id` ASC LIMIT 1;");
			// if (!playerData.first() && !playerData.next()) {
			if (!graveData.isBeforeFirst()) {
				// Location doesn't exist in the DB!
				getLogger()
						.log(Level.SEVERE,
								"[RP] Failure in grave Restore.. couldnt find loc in the DB but prior check succeeded.");
			} else {
				// Location does exist in the DB and data retrieved!!
				graveData.next();

				String tempArmor = graveData.getString("ArmorItemStack")
						.replace("\\'", "\'");
				ItemStack[] armor = InventorySerialization.getInventory(
						tempArmor, 4);

				String tempInv = graveData.getString("InvItemStack").replace(
						"\\'", "\'");
				ItemStack[] inv = InventorySerialization.getInventory(tempInv,
						37);

				boolean isOwner = false;
				if (playerAtGrave.getPlayerName().equals(
						graveData.getString("PlayerName"))) {
					isOwner = true;
				}

				if (handleArmor(armor, playerAtGrave, graveData.getInt("ID"))
						&& handleItems(inv, playerAtGrave,
								graveData.getInt("ID"))
						&& handleLevels(graveData.getInt("ID"), playerAtGrave,
								graveData.getInt("LevelsLost"), isOwner)) {
					// Everything finished OK!
					// remove the rest of this data

					dStmt.executeUpdate("UPDATE `rp_PlayerGraves` SET Status='Gone' WHERE ID="
							+ graveData.getInt("ID") + ";");

					loc.getWorld().playEffect(loc, Effect.MOBSPAWNER_FLAMES, 0);
					loc.getBlock().setType(Material.AIR);
					loc.add(0, 1, 0).getBlock().setType(Material.AIR);
					loc.getWorld().playEffect(loc, Effect.SMOKE, 0);

					// resync hashmap with graves to keep the redstone lamp
					// on...
					syncGraveLocations();
				}

				d.close();
			}
		} catch (SQLException z) {
			getLogger().log(Level.SEVERE,
					"Failed DB check for restore grave cuz " + z.getMessage());
		}

	}

	public static void listDeaths(Player commandSender, String playerSearch) {

		final Plugin instance = RunicParadise.getInstance();
		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));

		try {
			final Connection d = MySQL.openConnection();
			Statement dStmt = d.createStatement();
			String searchQuery = "";
			if (!playerSearch.equals("all")) {
				searchQuery = "SELECT * FROM `rp_PlayerGraves` WHERE `PlayerName` LIKE '%"
						+ playerSearch + "%' ORDER BY `id` DESC LIMIT 30;";
			} else {
				searchQuery = "SELECT * FROM `rp_PlayerGraves` ORDER BY `id` DESC LIMIT 30;";
			}
			ResultSet graveData = dStmt.executeQuery(searchQuery);
			// if (!playerData.first() && !playerData.next()) {
			if (!graveData.isBeforeFirst()) {
				// No results
				commandSender.sendMessage(ChatColor.DARK_GRAY
						+ "[RunicReaper] " + ChatColor.GRAY
						+ " Searching for recent 30 graves owned by "
						+ playerSearch);
				commandSender.sendMessage(ChatColor.GRAY + "No graves found.");
			} else {
				// results found!
				commandSender.sendMessage(ChatColor.DARK_GRAY
						+ "[RunicReaper] " + ChatColor.GRAY
						+ " Searching for recent 30 graves owned by "
						+ playerSearch);
				
				while (graveData.next()) {
					
					switch(graveData.getString("Status")) {
					case "Gone":
						new FancyMessage("ID").color(GRAY)
						.then("" + graveData.getInt("ID")).color(AQUA)
						.then(" ").color(GRAY)
						.then(graveData.getString("Status"))
						.color(GREEN).then(", Owner ").color(GRAY)
						.then(graveData.getString("PlayerName"))
						.color(YELLOW).then(", Looter ").color(GRAY)
						.then(graveData.getString("LooterName"))
						.color(YELLOW).then(", ").color(GRAY)
						.then("Loc").color(GRAY)
						.tooltip(graveData.getString("Location"))
						.send(commandSender);
						break;
					case "Locked":
						new FancyMessage("ID").color(GRAY)
						.then("" + graveData.getInt("ID")).color(AQUA)
						.then(" ").color(GRAY)
						.then(graveData.getString("Status"))
						.color(YELLOW).then(", Owner ").color(GRAY)
						.then(graveData.getString("PlayerName"))
						.color(YELLOW).then(", ").color(GRAY)
						.then("Loc").color(GRAY)
						.tooltip(graveData.getString("Location"))
						.send(commandSender);
						break;
					case "Unlocked":
						new FancyMessage("ID").color(GRAY)
						.then("" + graveData.getInt("ID")).color(AQUA)
						.then(" ").color(GRAY)
						.then(graveData.getString("Status"))
						.color(RED).then(", Owner ").color(GRAY)
						.then(graveData.getString("PlayerName"))
						.color(YELLOW).then(", ").color(GRAY)
						.then("Loc").color(GRAY)
						.tooltip(graveData.getString("Location"))
						.send(commandSender);
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

				d.close();
			}

		} catch (SQLException z) {
			getLogger().log(
					Level.SEVERE,
					"Failed DB check for RunicDeathChest.ListDeaths cuz "
							+ z.getMessage());
		}

	}

	public static void graveTeleport(Player commandSender, int ID) {

		final Plugin instance = RunicParadise.getInstance();
		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));
		try {
			// TODO: Get data from DB based on UUID to protect vs name changes
			final Connection d = MySQL.openConnection();
			Statement dStmt = d.createStatement();
			ResultSet graveData = dStmt
					.executeQuery("SELECT * FROM `rp_PlayerGraves` WHERE `ID` = "
							+ ID + " ORDER BY `id` DESC LIMIT 1;");
			// if (!playerData.first() && !playerData.next()) {
			if (!graveData.isBeforeFirst()) {
				// No results
				commandSender.sendMessage(ChatColor.DARK_GRAY
						+ "[RunicReaper] " + ChatColor.GRAY
						+ "No graves found. Invalid grave ID specified.");
				d.close();
				return;
			} else {
				// results found!
				graveData.next();
				commandSender.sendMessage(ChatColor.DARK_GRAY
						+ "[RunicReaper] " + ChatColor.GRAY
						+ "Sending you to grave ID " + ChatColor.YELLOW + ID
						+ ChatColor.GRAY + " owned by " + ChatColor.YELLOW
						+ graveData.getString("PlayerName"));
				if (graveData.getString("Status").equals("Gone")) {
					commandSender.sendMessage(ChatColor.GRAY
							+ " This grave has been looted by "
							+ ChatColor.YELLOW
							+ graveData.getString("LooterName")
							+ ChatColor.GRAY
							+ " @ "
							+ ChatColor.YELLOW
							+ new java.text.SimpleDateFormat(
									"dd/MM/yyyy HH:mm:ss")
									.format(new java.util.Date(Long
											.parseLong(graveData
													.getString("LootTime")))));
				}
				String[] locParts = graveData.getString("Location").split(
						"[\\x2E]");
				commandSender.teleport(new Location(Bukkit
						.getWorld(locParts[0]), Integer.parseInt(locParts[1]),
						Integer.parseInt(locParts[2]), Integer
								.parseInt(locParts[3])));

				d.close();
			}

		} catch (SQLException z) {
			getLogger().log(Level.SEVERE,
					"Failed DB check for restore grave cuz " + z.getMessage());
		}

	}

	public static void syncGraveLocations() {
		// reset the hashmap
		graveLocations.clear();

		// retrieve updated grave data
		final Plugin instance = RunicParadise.getInstance();
		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));
		try {
			final Connection d = MySQL.openConnection();
			Statement dStmt = d.createStatement();
			ResultSet graveData = dStmt
					.executeQuery("SELECT Location,ID,Status FROM `rp_PlayerGraves` WHERE `Status` != 'Gone' ORDER BY `id` DESC;");
			// if (!playerData.first() && !playerData.next()) {
			if (!graveData.isBeforeFirst()) {
				// No results
				// do nothing
				d.close();
				return;
			} else {
				// results found!
				while (graveData.next()) {
					graveLocations.put(graveData.getInt("ID"),
							graveData.getString("Location"));
					graveID.put(graveData.getString("Location"),
							graveData.getInt("ID"));
					graveStatus.put(graveData.getInt("ID"),
							graveData.getString("Status"));
				}

				d.close();
			}

		} catch (SQLException z) {
			getLogger().log(Level.SEVERE,
					"Failed map sync for grave locs cuz " + z.getMessage());
		}

	}

	public static String checkHashmapForDeathLoc(Location loc) {

		String strToCheck = loc.getWorld().getName() + "." + loc.getBlockX()
				+ "." + loc.getBlockY() + "." + loc.getBlockZ();

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
		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));
		try {
			final Connection d = MySQL.openConnection();
			Statement dStmt = d.createStatement();
			ResultSet graveData = dStmt
					.executeQuery("SELECT Location,ID FROM `rp_PlayerGraves` WHERE ExpiryTime<"
							+ new Date().getTime()
							+ " AND Status='Locked' ORDER BY ID DESC;");
			int expiries = dStmt
					.executeUpdate("UPDATE `rp_PlayerGraves` SET Status='Unlocked' WHERE ExpiryTime<"
							+ new Date().getTime() + " AND Status='Locked';");

			if (expiries > 0) {
				getLogger().log(
						Level.INFO,
						"[RP] " + expiries
								+ " graves unlocked due to timer expiry");
				if (senderIsPlayer) {
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "sc "
							+ expiries + " graves unlocked due to timer expiry");
				}
			}

			d.close();

			syncGraveLocations();

		} catch (SQLException z) {
			getLogger().log(Level.SEVERE,
					"Failed map sync for grave locs cuz " + z.getMessage());
		}

	}

	public static void unlockGrave(Player commandSender, int ID) {

		final Plugin instance = RunicParadise.getInstance();
		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));
		try {

			final Connection d = MySQL.openConnection();
			Statement dStmt = d.createStatement();
			ResultSet graveData = dStmt
					.executeQuery("SELECT * FROM `rp_PlayerGraves` WHERE `ID` = "
							+ ID + " ORDER BY `ID` DESC LIMIT 1;");
			// if (!playerData.first() && !playerData.next()) {
			if (!graveData.isBeforeFirst()) {
				// No results
				commandSender.sendMessage(ChatColor.DARK_GRAY
						+ "[RunicReaper] " + ChatColor.GRAY
						+ "No graves found. Invalid grave ID specified.");
				d.close();
				return;
			} else {
				// results found!
				graveData.next();
				if (graveData.getString("Status").equals("Locked")) {
					commandSender.sendMessage(ChatColor.DARK_GRAY
							+ "[RunicReaper] " + ChatColor.GRAY
							+ "Unlocking grave ID " + ChatColor.YELLOW + ID
							+ ChatColor.GRAY + " owned by " + ChatColor.YELLOW
							+ graveData.getString("PlayerName"));
					Statement cStmt = d.createStatement();
					int tempC = cStmt
							.executeUpdate("UPDATE rp_PlayerGraves SET Status='Unlocked' WHERE ID="
									+ ID + ";");

				} else {
					// Grave isnt locked, so cant unlock it
					commandSender.sendMessage(ChatColor.DARK_GRAY
							+ "[RunicReaper] " + ChatColor.GRAY
							+ "That grave isn't locked! Can't unlock it.");
				}

				d.close();
			}

		} catch (SQLException z) {
			getLogger().log(Level.SEVERE,
					"Failed DB check for restore grave cuz " + z.getMessage());
		}

	}
}
