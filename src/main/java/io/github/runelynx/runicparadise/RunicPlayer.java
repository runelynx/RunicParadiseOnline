package io.github.runelynx.runicparadise;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface RunicPlayer {

	/**
	 * Retrieve a player's name
	 * 
	 * @return player's name
	 */
	public String getPlayerName();

	/**
	 * Retrieve a player's display name
	 * 
	 * @return player's display name
	 */
	public String getPlayerDisplayName();
	
	public Map<String, Integer> getPlayerKillCounts ();
	
	public boolean incrementPlayerKillCount (String columnName);
	
	public int getMasteredJobCount();
	
	public int getCountGravesCreated();
	public int getCountGravesStolen();
	public int getCountGravesRemaining();
	
	public Date getJoinDate();
	
	/**
	 * Sends a message to an individual player.
	 * 
	 * @param message
	 *            Message to send to the player
	 * @return true is player is online; false if not (and msg failed)
	 */
	public void sendMessageToPlayer(String message);

	/**
	 * Check if a player is staff, has permission rp.staff
	 * 
	 * @return true if player is staff
	 */
	public boolean isPlayerStaff();

	/**
	 * Retrieve player's token balance
	 * 
	 * @return int player's token balance
	 */
	public int getPlayerIceMazeCompletions();

	/**
	 * Retrieve count of successful runs thru ice maze
	 * 
	 * @return int player's ice maze wins
	 */
	public int getPlayerHedgeMazeCompletions();

	/**
	 * Retrieve count of successful runs thru hedge maze
	 * 
	 * @return int player's hedge maze wins
	 */
	public int getPlayerTokenBalance();

	/**
	 * Set player's token balance.
	 * 
	 * @return true if player found and balance updated.
	 */
	public boolean setPlayerTokenBalance(int newBalance);

	/**
	 * Update player's maze completion count
	 * 
	 * @param increment
	 *            how much to add to the existing int
	 * @return true if player found and count updated.
	 */
	public boolean setPlayerLifetimeTokens(int increment);

	/**
	 * Gives player an itemstack. Does not check for space!!
	 * 
	 * @return nothing!
	 */
	public void givePlayerItemStack(ItemStack[] item);

	/**
	 * Returns used slots in player's inventory [out of 36]. Ignores armor worn.
	 * 
	 * @return count of itemstacks in player's inventory
	 */
	public int checkPlayerInventoryItemstackCount();

	/**
	 * Get players free death count
	 * 
	 * @return players free death count
	 */
	public int getPlayerSouls();
	
	public void maintainJobTable();
	
	/**
	 * Master a player's job
	 * 
	 * @return success
	 */
	public boolean executeJobMastery();
	
	public String getCurrentJob();
	
	/**
	 * Get list of mastered jobs
	 * 
	 * @return String of mastered jobs
	 */
	public String getMasteredJobs();
	
	public int getPlayerLifetimeTokens();
	
	/**
	 * Set players free deaths 
	 * 
	 * @return nothing
	 */
	public boolean setPlayerSouls(int newSoulCount);

	/**
	 * Update player's maze completion count
	 * 
	 * @param maze
	 *            type of maze to update; initial options are ice or hedge
	 * @param increment
	 *            how much to add to the existing int
	 * @return true if player found and count updated.
	 */
	public boolean setPlayerMazeCompletions(String maze, int increment);

	/**
	 * Converts chat colors in message strings using API-specific enums/objects.
	 * 
	 * @return String using API-friendly chat colors
	 */
	public String translateChatColors();

	/**
	 * Sets all fields in the player object
	 * 
	 * @param player
	 *            takes in player object [API]
	 */
	public void refreshPlayerObject(Player player);

	/**
	 * Counts the quantity of an item player has (including data element)
	 * 
	 * @param id
	 *            Item ID to look for
	 * @param dataValue
	 *            Item Data to look for
	 * @return
	 */
	public int checkPlayerInventoryForItemDataCount(int id, int dataValue);

	/**
	 * Check player's permission
	 * 
	 * @param permission  "rp.staff" etc to check
	 * @return boolean
	 */
	public boolean checkPlayerPermission(String permission);

	/**
	 * True if player is wearing any armor
	 * 
	 * @return boolean
	 */
	public boolean checkPlayerWearingArmor();
	
	/**
	 * Returns player's UUID, as string
	 * 
	 * @return String
	 */
	public String getPlayerUUID();
	
	/**
	 * Set player's level
	 * 
	 * @param newLevel
	 *            int to set players new level to
	 * @return void
	 */
	public void setPlayerLevel(int newLevel);
	
	/**
	 * Get player's level
	 * @return int
	 */
	public int getPlayerLevel();

	/**
	 * Removes all of a specific item/data from a player's inventory
	 * 
	 * @param id
	 *            Item ID to remove
	 * @param dataValue
	 *            Item data to remove
	 * @return
	 */
	public int removePlayerInventoryItemData(int id, int dataValue);

	/**
	 * Gives a player a specific amount of an item/data
	 * 
	 * @param count
	 * @param id
	 * @param dataValue
	 * @param loreCount
	 * @param displayName
	 * @param lore1
	 * @param lore2
	 * @param lore3
	 */
	public void givePlayerItemData(int count, int id, int dataValue,
			int loreCount, String displayName, String lore1, String lore2,
			String lore3);

}
