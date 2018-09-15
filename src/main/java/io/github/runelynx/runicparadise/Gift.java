package io.github.runelynx.runicparadise;

import io.github.runelynx.runicparadise.tempserialization.InventorySerialization;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Level;

public class Gift {

	private static Plugin instance = RunicParadise.getInstance();
	private int ID;

	public Gift(Player sender, UUID recip) {

		Inventory giftInventory = Bukkit.createInventory(sender, 9,
				"Temp inventory");
		giftInventory.addItem(Bukkit.getPlayer(sender.getUniqueId())
				.getInventory().getItemInMainHand());
		

		String giftString = InventorySerialization
				.serializeInventoryAsString(giftInventory);
		
		//check if JSON string is bad! (because it's happening sometimes :( 
		if (!isJSONValid(giftString)) { 
			sender.sendMessage(
					ChatColor.DARK_RED + "" + ChatColor.ITALIC
							+ "That item can't be sent as a gift. Maybe you've customized the name somehow or it's a complicated book? Please try another item.");
		} else {
			MySQL MySQL = RunicUtilities.getMysqlFromPlugin(instance);
			Connection e = MySQL.openConnection();

			// /////////////
			try {
				// Statement eStmt = e.createStatement();

				PreparedStatement eStmt = e
						.prepareStatement("INSERT INTO rp_RunicGifts (`Giver`, `Recipient`, `Inventory`, `Timestamp`) VALUES "
								+ "('"
								+ sender.getUniqueId().toString()
								+ "', '"
								+ recip.toString()
								+ "', '"
								+ giftString
								+ "', "
								+ new Date().getTime() + ");");

				eStmt.executeUpdate();
				eStmt.close();
				e.close();
			} catch (SQLException err) {
				Bukkit.getLogger().log(Level.SEVERE,
						"Cant create new row Gift because: " + err.getMessage());
			}

			sender.getInventory().removeItem(sender.getItemInHand());
			sender.updateInventory();

			sender.sendMessage(
					ChatColor.DARK_GREEN + "" + ChatColor.ITALIC
							+ "Your gift is on its way!");

			if (Bukkit.getPlayer(recip).isOnline()) {
				Bukkit.getPlayer(recip).sendMessage(
						ChatColor.DARK_GREEN + "" + ChatColor.ITALIC
								+ "You've received a gift! Type /gift check");
			}
		}

		

	}

	Gift(Player recipient) {
		Connection connection = RunicUtilities.getMysqlFromPlugin(instance).openConnection();

		// /////////////
		try {
			// Statement eStmt = e.createStatement();

			PreparedStatement statement = connection
					.prepareStatement("SELECT * FROM rp_RunicGifts WHERE Recipient = '"
							+ recipient.getUniqueId().toString()
							+ "' AND Status = 'Waiting' LIMIT 1;");

			ResultSet result = statement.executeQuery();

			if (!result.isBeforeFirst()) {
				// no result found
				recipient.sendMessage(ChatColor.DARK_GREEN + ""
						+ ChatColor.ITALIC
						+ "There are no gifts waiting for you.");
				statement.close();
				connection.close();
			} else {
				// Location does exist in the DB and data retrieved!!

				result.next();
				this.setID(result.getInt("GiftID"));
				OfflinePlayer giver = Bukkit.getOfflinePlayer(UUID
						.fromString(result.getString("Giver")));

				Inventory giftInventory = Bukkit.createInventory(
						null,
						9,
						ChatColor.DARK_GREEN + "" + ChatColor.BOLD
								+ "Gift from " + ChatColor.DARK_RED
								+ giver.getName());

				ItemStack[] tempGift = InventorySerialization.getInventory(
						result.getString("Inventory"), 1);
				ItemStack gift = null;

				for (ItemStack i : tempGift) {
					gift = i;
				}

				giftInventory.setItem(4, gift);

				RunicParadise.giftIDTracker.put(recipient.getUniqueId(),
						result.getInt("GiftID"));

				recipient.openInventory(giftInventory);

				statement.close();
				connection.close();

			}

		} catch (SQLException err) {
			Bukkit.getLogger().log(Level.SEVERE,
					"Cant check gift Gift because: " + err.getMessage());
		}
	}

	private void setID(int newID) {
		this.ID = newID;
	}

	private int getID() {
		return this.ID;
	}

	static void removeGift(Player p, int ID) {
		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));
		final Connection e = MySQL.openConnection();

		// /////////////
		try {
			// Statement eStmt = e.createStatement();

			PreparedStatement statement = e
					.prepareStatement("UPDATE rp_RunicGifts SET `Status` = 'Taken' WHERE `GiftID` = "
							+ ID + ";");

			statement.executeUpdate();

			statement.close();
			e.close();
		} catch (SQLException err) {
			Bukkit.getLogger().log(Level.SEVERE,
					"Cant remove gift because : " + err.getMessage());
		}

		RunicParadise.giftIDTracker.remove(p.getUniqueId());
	}
	
	public boolean isJSONValid(String test) {
	    try {
	        new JSONObject(test);
	    } catch (JSONException ex) {
	        // edited, to include @Arthur's comment
	        // e.g. in case JSONArray is valid as well...
	        try {
	            new JSONArray(test);
	        } catch (JSONException ex1) {
	            return false;
	        }
	    }
	    return true;
	}

}
