package io.github.runelynx.runicparadise;

import static org.bukkit.Bukkit.getLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Powers {

	private String playerUUID;
	private String playerName;
	private int skillBeasts;
	private int skillNature;
	private int skillElements;
	private int skillStars;
	private boolean statusBeasts = false;
	private boolean statusNature = false;
	private boolean statusElements = false;
	private boolean statusStars = false;

	private static Plugin instance = RunicParadise.getInstance();

	public Powers(UUID pUUID) {
		this.retrievePlayerData(pUUID);
	}

	public void retrievePlayerData(UUID nUUID) {
		this.playerUUID = nUUID.toString();
		this.playerName = new RunicPlayerBukkit(nUUID).getPlayerName();

		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));
		try {
			final Connection dbCon = MySQL.openConnection();
			Statement dbStmt = dbCon.createStatement();
			ResultSet powerResult = dbStmt
					.executeQuery("SELECT * FROM rp_Powers WHERE UUID = '"
							+ nUUID.toString() + "';");
			if (!powerResult.isBeforeFirst()) {
				// No results
				// do nothing
				dbCon.close();
				return;
			} else {
				// results found!
				while (powerResult.next()) {

					this.skillBeasts = powerResult.getInt("Skill_Beasts");
					this.skillNature = powerResult.getInt("Skill_Nature");
					this.skillElements = powerResult.getInt("Skill_Elements");
					this.skillStars = powerResult.getInt("Skill_Stars");

					this.statusBeasts = (powerResult.getString("Status_Beasts")
							.equals("Off")) ? false : true;
					this.statusNature = (powerResult.getString("Status_Nature")
							.equals("Off")) ? false : true;
					this.statusElements = (powerResult
							.getString("Status_Elements").equals("Off")) ? false
							: true;
					this.statusStars = (powerResult.getString("Status_Stars")
							.equals("Off")) ? false : true;

				}

				dbCon.close();
			}

		} catch (SQLException z) {
			getLogger().log(
					Level.SEVERE,
					"Failed retrievePlayerData power data retrieval for player "
							+ nUUID.toString() + "- " + z.getMessage());
		}

	}

	public int getSkillBeasts() {
		return this.skillBeasts;
	}

	public int getSkillNature() {
		return this.skillNature;
	}

	public int getSkillElements() {
		return this.skillElements;
	}

	public int getSkillStars() {
		return this.skillStars;
	}

	public boolean getStatusBeasts() {
		return this.statusBeasts;
	}

	public boolean getStatusNature() {
		return this.statusNature;
	}

	public boolean getStatusElements() {
		return this.statusElements;
	}

	public boolean getStatusStars() {
		return this.statusStars;
	}

	public boolean incrementSkill(String skillColumn) {
		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));

		if (!skillColumn.contains("Skill_")) {
			getLogger().log(
					Level.SEVERE,
					"Invalid column in Powers.incrementSkill - given "
							+ skillColumn + " but expected Skill_xxx");
			return false;
		}

		try {
			final Connection dbCon = MySQL.openConnection();

			PreparedStatement dbStmt = dbCon
					.prepareStatement("UPDATE rp_Powers SET " + skillColumn
							+ "=" + skillColumn + "+1 WHERE UUID = ?");
			dbStmt.setString(1, this.playerUUID);
			dbStmt.executeUpdate();

			dbCon.close();

			// Now update the object used by the map!
			if (skillColumn.contains("Beasts")) {
				this.skillBeasts = this.skillBeasts + 1;
			} else if (skillColumn.contains("Nature")) {
				this.skillNature = this.skillNature + 1;
			} else if (skillColumn.contains("Elements")) {
				this.skillElements = this.skillElements + 1;
			} else if (skillColumn.contains("Stars")) {
				this.skillStars = this.skillStars + 1;
			}

			return true;

		} catch (SQLException e) {
			getLogger().log(Level.SEVERE,
					"Failed Powers.incrementSkill because: " + e.getMessage());
			return false;
		}
	}

	public boolean enableSkill(String statusColumn) {
		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));

		if (!statusColumn.contains("Status_")) {
			getLogger().log(
					Level.SEVERE,
					"Invalid column in Powers.enableSkill - given "
							+ statusColumn + " but expected Status_xxx");
			return false;
		}

		try {
			final Connection dbCon = MySQL.openConnection();

			PreparedStatement dbStmt = dbCon
					.prepareStatement("UPDATE rp_Powers SET " + statusColumn
							+ "= 'On' WHERE UUID = ?");
			dbStmt.setString(1, this.playerUUID);
			dbStmt.executeUpdate();

			dbCon.close();

			// Now update the object used by the map!
			if (statusColumn.contains("Beasts")) {
				this.statusBeasts = true;
			} else if (statusColumn.contains("Nature")) {
				this.statusNature = true;
			} else if (statusColumn.contains("Elements")) {
				this.statusElements = true;
			} else if (statusColumn.contains("Stars")) {
				this.statusStars = true;
			}

			return true;

		} catch (SQLException e) {
			getLogger().log(Level.SEVERE,
					"Failed Powers.enableSkill because: " + e.getMessage());
			return false;
		}
	}

	public boolean disableSkill(String statusColumn) {
		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));

		if (!statusColumn.contains("Status_")) {
			getLogger().log(
					Level.SEVERE,
					"Invalid column in Powers.disableSkill - given "
							+ statusColumn + " but expected Status_xxx");
			return false;
		}

		try {
			final Connection dbCon = MySQL.openConnection();

			PreparedStatement dbStmt = dbCon
					.prepareStatement("UPDATE rp_Powers SET " + statusColumn
							+ "= 'Off' WHERE UUID = ?");
			dbStmt.setString(1, this.playerUUID);
			dbStmt.executeUpdate();

			dbCon.close();

			// Now update the object used by the map!
			if (statusColumn.contains("Beasts")) {
				this.statusBeasts = false;
			} else if (statusColumn.contains("Nature")) {
				this.statusNature = false;
			} else if (statusColumn.contains("Elements")) {
				this.statusElements = false;
			} else if (statusColumn.contains("Stars")) {
				this.statusStars = false;
			}

			return true;

		} catch (SQLException e) {
			getLogger().log(Level.SEVERE,
					"Failed Powers.disableSkill because: " + e.getMessage());
			return false;
		}
	}

	public static void trySkillUp(String UUID, String skill) {

	}

	public static void spellSwordOfJupiter(String pName, Location loc) {
		Zombie zombie = (Zombie) loc.getWorld().spawnEntity(loc,
				EntityType.ZOMBIE);
		zombie.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,
				10000000, 2));
		zombie.setCustomName("Sword of Jupiter");
		zombie.setCustomNameVisible(true);
		zombie.getEquipment().setItemInHand(
				new ItemStack(Material.DIAMOND_SWORD));
		zombie.setTicksLived(15 * 20);
		RunicParadise.powersSwordOfJupiterMap.put(pName, zombie);
		Bukkit.getLogger().log(Level.INFO, "Created SOJ zombie record in map with key " + pName);

	}

}
