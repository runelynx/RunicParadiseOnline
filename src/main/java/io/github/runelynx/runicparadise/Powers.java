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
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;
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
	
	public boolean setSkill(String skillColumn, int newValue) {
		MySQL MySQL = new MySQL(instance, instance.getConfig().getString(
				"dbHost"), instance.getConfig().getString("dbPort"), instance
				.getConfig().getString("dbDatabase"), instance.getConfig()
				.getString("dbUser"), instance.getConfig().getString(
				"dbPassword"));

		if (!skillColumn.contains("Skill_")) {
			getLogger().log(
					Level.SEVERE,
					"Invalid column in Powers.setSkill - given "
							+ skillColumn + " but expected Skill_xxx");
			return false;
		} else if  (newValue < 0) {
			getLogger().log(
					Level.SEVERE,
					"Invalid newValue in Powers.setSkill - given "
							+ newValue + " less than 0");
			return false;
		}

		try {
			final Connection dbCon = MySQL.openConnection();

			PreparedStatement dbStmt = dbCon
					.prepareStatement("UPDATE rp_Powers SET " + skillColumn
							+ "=" + newValue + " WHERE UUID = ?");
			dbStmt.setString(1, this.playerUUID);
			dbStmt.executeUpdate();

			dbCon.close();

			// Now update the object used by the map!
			if (skillColumn.contains("Beasts")) {
				this.skillBeasts = newValue;
			} else if (skillColumn.contains("Nature")) {
				this.skillNature = newValue;
			} else if (skillColumn.contains("Elements")) {
				this.skillElements = newValue;
			} else if (skillColumn.contains("Stars")) {
				this.skillStars = newValue;
			}

			return true;

		} catch (SQLException e) {
			getLogger().log(Level.SEVERE,
					"Failed Powers.setSkill because: " + e.getMessage());
			return false;
		}
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

	public static void trySkillUp(UUID UUID, String skill, int level) {
		// int randomNum = rand.nextInt((max - min) + 1) + min;
		int randomNum = RunicParadise.randomSeed.nextInt((100 - 0) + 1) + 0;
		

		switch (skill) {
		case "Skill_Beasts":
			if (level == 1 && RunicParadise.powersMap.get(UUID).getStatusBeasts()) {
				if (randomNum <= 5) {
					RunicParadise.powersMap.get(UUID).incrementSkill(skill);
					Bukkit.getPlayer(UUID).sendMessage("Powers SkillUp: weapon rank "  + level + ", rolled " + randomNum + ", SUCCESS!");
				} else {
					Bukkit.getPlayer(UUID).sendMessage("Powers SkillUp: weapon rank "  + level + ", rolled " + randomNum + ", FAILED!" );
				}
				
			} else if (level == 2 && RunicParadise.powersMap.get(UUID).getStatusBeasts()) {
				if (randomNum <= 10) {
					RunicParadise.powersMap.get(UUID).incrementSkill(skill);
					Bukkit.getPlayer(UUID).sendMessage("Powers SkillUp: weapon rank "  + level + ", rolled " + randomNum + ", SUCCESS!" );
				} else {
					Bukkit.getPlayer(UUID).sendMessage("Powers SkillUp: weapon rank "  + level + ", rolled " + randomNum + ", FAILED!" );
				}
			} else {
				getLogger().log(Level.SEVERE,"Failed Powers.trySkillUp - Beasts. Invalid level or Beasts not enabled.");
			}
			break;
		case "Skill_Nature":
			break;
		case "Skill_Elements":
			break;
		case "Skill_Stars":
			break;
		default:
			getLogger().log(Level.SEVERE,
					"Failed Powers.trySkillUp - invalid skill given!! " + skill + "...");
			break;
		}

	}
	
	public static void spellSpiritOfTheBeaver(UUID pUUID, Location loc) {
		int randomNum = RunicParadise.randomSeed.nextInt((100 - 0) + 1) + 0;
		if (randomNum < 6) {
			// 5% chance
			
			//Strength Potion Effect (20 ticks = 10 seconds)
			Bukkit.getPlayer(pUUID).addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 12000, 2));			
			Bukkit.getPlayer(pUUID).sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "The fallen tree empowers you with the spirit of the beaver!");
		} else {
			Bukkit.getPlayer(pUUID).sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "Failed to cast Spirit of the Beaver. Rolled " + randomNum + ", Need <6");
		}

	}
	
	public static void spellSpiritOfTheWolf(UUID pUUID, Location loc) {

			//Runspeed Potion Effect (20 ticks = 10 seconds)
			Bukkit.getPlayer(pUUID).addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 6000, 1));			
			Bukkit.getPlayer(pUUID).sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "The moon's light empowers you with the spirit of the wolf!");

	}
	
	public static void spellSpiritOfTheTiger(EntityDamageByEntityEvent event, UUID pUUID, Location loc) {
		int randomNum = RunicParadise.randomSeed.nextInt((100 - 0) + 1) + 0;
		if (randomNum < 6) {
			// 5% chance
			event.setDamage(DamageModifier.BASE,
					event.getDamage(DamageModifier.BASE) + 5);	
			Bukkit.getPlayer(pUUID).sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "The spirit of the tiger empowers your attack!");
		} else {
			Bukkit.getPlayer(pUUID).sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "Failed to cast Spirit of the Tiger. Rolled " + randomNum + ", Need <6");
		}

	}
	public static void spellSpiritOfTheMole(UUID pUUID, Location loc) {
		int randomNum = RunicParadise.randomSeed.nextInt((100 - 0) + 1) + 0;
		if (randomNum < 3) {
			// 2% chance
			
			//DigSpeed Potion Effect (20 ticks = 10 seconds)
			Bukkit.getPlayer(pUUID).addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 6000, 1));
			Bukkit.getPlayer(pUUID).addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 6000, 1));
			Bukkit.getPlayer(pUUID).sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "The spirit of the mole empowers your arms!");
		} else {
			Bukkit.getPlayer(pUUID).sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "Failed to cast Spirit of the Mole. Rolled " + randomNum + ", Need <3");
		}

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
		Bukkit.getLogger().log(Level.INFO,
				"Created SOJ zombie record in map with key " + pName);

	}

}
