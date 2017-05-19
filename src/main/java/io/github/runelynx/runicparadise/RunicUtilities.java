package io.github.runelynx.runicparadise;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class RunicUtilities {

	public static Boolean isInteger(String x) {

		if (x == null) {
			return false;
		}

		try {
			Integer.parseInt(x);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}

	}

	public static void fixGroupManager() {

		// save any pending changes
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "sc Saving any pending permissions changes...");
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mansave");

		File userFile = new File(
				Bukkit.getServer().getPluginManager().getPlugin("GroupManager").getDataFolder().getAbsolutePath()
						+ "/worlds/runicrealm",
				"users.yml");

		FileConfiguration userConfig = YamlConfiguration.loadConfiguration(userFile);

		for (Player p : Bukkit.getOnlinePlayers()) {

			if (userConfig.contains("users." + p.getUniqueId().toString())) {
				// user has a UUID branch
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
						"sc Found " + ChatColor.GREEN + "good" + ChatColor.AQUA + " record for " + p.getDisplayName());

				if (userConfig.contains("users." + p.getName())) {
					// user has a name branch too!
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "sc Also found " + ChatColor.RED + "bad"
							+ ChatColor.AQUA + " record for " + p.getDisplayName());
					try {
						userConfig.getConfigurationSection("users").set(p.getName(), null);
						Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "sc Removed " + ChatColor.RED + "bad"
								+ ChatColor.AQUA + " record for " + p.getDisplayName());

					} catch (Exception e) {
						e.printStackTrace();
						Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "sc Failed to remove " + ChatColor.RED + "bad"
								+ ChatColor.AQUA + " record for " + p.getDisplayName());
					}

				}

			} else if (userConfig.contains("users." + p.getName())) {
				// user only has a name branch too!
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
						"sc Found " + ChatColor.GOLD + "OK" + ChatColor.AQUA + " record for " + p.getDisplayName());
			}

		}

		try {
			userConfig.save(userFile);
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "sc Saving cleanup changes back to file...");
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "sc Reloading permissions on server...");
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "manload RunicRealm");

		} catch (IOException e) {
			e.printStackTrace();
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "sc Failed to save changes!");
		}

	}

	public static void silentFixGroupManager() {

		// save any pending changes
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mansave");

		File userFile = new File(
				Bukkit.getServer().getPluginManager().getPlugin("GroupManager").getDataFolder().getAbsolutePath()
						+ "/worlds/runicrealm",
				"users.yml");

		FileConfiguration userConfig = YamlConfiguration.loadConfiguration(userFile);

		for (Player p : Bukkit.getOnlinePlayers()) {

			if (userConfig.contains("users." + p.getUniqueId().toString())) {
				// user has a UUID branch

				if (userConfig.contains("users." + p.getName())) {
					// user has a name branch too!

					try {
						userConfig.getConfigurationSection("users").set(p.getName(), null);

					} catch (Exception e) {
						e.printStackTrace();

					}

				}

			} else if (userConfig.contains("users." + p.getName())) {
				// user only has a name branch too!

			}

		}

		try {
			userConfig.save(userFile);

			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "manload RunicRealm");

		} catch (IOException e) {
			e.printStackTrace();

		}

	}

	public static void convertGroupManager(Player p) {

		if (!p.hasPermission("rp.convertedperms")) {

			File userFile = new File("/home/AMP/.ampdata/instances/Survival/Minecraft/plugins/GroupManager/worlds/runicrealm",
					"users.yml");

			FileConfiguration userConfig = YamlConfiguration.loadConfiguration(userFile);

			String uuidBranch = "users." + p.getUniqueId().toString();
			String nameBranch = "users." + p.getName();
			String primaryGroup = "";
			int UgroupCount = 0;
			int UpermCount = 0;
			int NgroupCount = 0;
			int NpermCount = 0;
			
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "sc " + ChatColor.BLUE+ "Starting conversion for " + p.getDisplayName());
			

			if (userConfig.contains(uuidBranch)) {
				// user has a UUID branch
				
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "sc " + ChatColor.BLUE+ "Found UUID branch for " + p.getDisplayName());

				primaryGroup = userConfig.getString(uuidBranch + ".group");
				RunicParadise.perms.playerAddGroup(null, Bukkit.getOfflinePlayer(p.getUniqueId()), primaryGroup);

				List<String> subgroupList = userConfig.getStringList(uuidBranch + ".subgroups");
				if (!subgroupList.isEmpty()) {
					for (String sg : subgroupList) {
						if (sg != null) {
							RunicParadise.perms.playerAddGroup(null, Bukkit.getOfflinePlayer(p.getUniqueId()), sg);
							UgroupCount++;
						}
					}
				}

				List<String> permList = userConfig.getStringList(uuidBranch + ".permissions");
				if (!permList.isEmpty()) {
					for (String pm : permList) {
						if (pm != null) {
							RunicParadise.perms.playerAdd(null, Bukkit.getOfflinePlayer(p.getUniqueId()), pm);
							UpermCount++;
						}
					}
				}

				if (userConfig.contains(nameBranch)) {
					// user has a name branch too!

					List<String> subgroupList2 = userConfig.getStringList(nameBranch + ".subgroups");
					if (!subgroupList2.isEmpty()) {
						for (String sg : subgroupList2) {
							if (sg != null) {
								RunicParadise.perms.playerAddGroup(null, Bukkit.getOfflinePlayer(p.getUniqueId()), sg);
								NgroupCount++;
							}
						}
					}

					List<String> permList2 = userConfig.getStringList(nameBranch + ".permissions");
					if (!permList2.isEmpty()) {
						for (String pm : permList2) {
							if (pm != null) {
								RunicParadise.perms.playerAdd(null, Bukkit.getOfflinePlayer(p.getUniqueId()), pm);
								
								NpermCount++;
							}
						}
					}

				}

			} else if (userConfig.contains("users." + p.getName())) {
				// user only has a name branch !
				
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "sc " + ChatColor.BLUE+ "Found name branch (only) for " + p.getDisplayName());

				primaryGroup = userConfig.getString(nameBranch + ".group");
				RunicParadise.perms.playerAddGroup(null, Bukkit.getOfflinePlayer(p.getUniqueId()), primaryGroup);

				List<String> subgroupList = userConfig.getStringList(nameBranch + ".subgroups");
				if (!subgroupList.isEmpty()) {
					for (String sg : subgroupList) {
						if (sg != null) {
							RunicParadise.perms.playerAddGroup(null, Bukkit.getOfflinePlayer(p.getUniqueId()), sg);
							NgroupCount++;
						}
					}
				}

				List<String> permList = userConfig.getStringList(nameBranch + ".permissions");
				if (!permList.isEmpty()) {
					for (String pm : permList) {
						if (pm != null) {
							RunicParadise.perms.playerAdd(null, Bukkit.getOfflinePlayer(p.getUniqueId()), pm);
							NpermCount++;
						}
					}
				}

			}
			
			//Conversion complete
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "sc " + ChatColor.BLUE+ "Converted " + p.getDisplayName() + " perms. "+ ChatColor.GREEN + primaryGroup +ChatColor.BLUE+ ", " + ChatColor.GREEN+ UgroupCount + ChatColor.BLUE+ " usg, "+ ChatColor.GREEN + UpermCount + ChatColor.BLUE+ " up, "+ ChatColor.GREEN +
			NgroupCount + ChatColor.BLUE+ " nsg, " + ChatColor.GREEN+ NpermCount + ChatColor.BLUE+ " np");
		}

	}

}
