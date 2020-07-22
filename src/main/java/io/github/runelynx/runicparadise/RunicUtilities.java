package io.github.runelynx.runicparadise;

import io.github.runelynx.runicuniverse.RunicMessaging;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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

	public static void sendDebugMessages (String type, String message) {
		// playerjoin

		for (Player p : Bukkit.getOnlinePlayers()) {
			if (p.hasPermission("rp.staff.debug." + type)) {
				RunicMessaging.sendMessage(p, RunicMessaging.RunicFormat.SYSTEM, message + ChatColor.DARK_GRAY + "rp.staff.debug." + type);
			}
		}

	}

	public static ArrayList<String> processLoreStringsToArray(String lore1, String lore2, String lore3, String lore4, String lore5) {
		ArrayList<String> loreList = new ArrayList<String>();
		loreList.add(ChatColor.translateAlternateColorCodes('&', lore1));

		if (lore2 != null) {
			loreList.add(ChatColor.translateAlternateColorCodes('&', lore2));
		}

		if (lore3 != null) {
			loreList.add(ChatColor.translateAlternateColorCodes('&', lore3));
		}

		if (lore4 != null) {
			loreList.add(ChatColor.translateAlternateColorCodes('&', lore4));
		}

		if (lore5 != null) {
			loreList.add(ChatColor.translateAlternateColorCodes('&', lore5));
		}

		return loreList;
	}

	static void fixGroupManager() {

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
			} else {
				userConfig.contains("users." + p.getName());
			}
		}

		try {
			userConfig.save(userFile);
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "manload RunicRealm");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static void convertGroupManager(Player p) {

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

	static List<Entity> getTargetList(Location loc, int radius) {
		List<Entity> target = new ArrayList<>();
		int rs = radius * radius;
		Location tmp = new Location(loc.getWorld(), 0.0D, 0.0D, 0.0D);
		for (Entity entity : loc.getWorld().getEntities()) {
			if (entity.getLocation(tmp).distanceSquared(loc) < rs) {
				target.add(entity);
			}
		}
		return target;
	}

	static String toStringOr(Object obj, String or) {
		if (obj == null) {
			return or;
		}
		return obj.toString();
	}

	static Material getGlassColor(short type) {
		final Material[] glassTypes = new Material[] {
				Material.WHITE_STAINED_GLASS,
				Material.ORANGE_STAINED_GLASS,
				Material.MAGENTA_STAINED_GLASS,
				Material.LIGHT_BLUE_STAINED_GLASS,
				Material.YELLOW_STAINED_GLASS,
				Material.LIME_STAINED_GLASS,
				Material.PINK_STAINED_GLASS,
				Material.GRAY_STAINED_GLASS,
				Material.LIGHT_GRAY_STAINED_GLASS,
				Material.CYAN_STAINED_GLASS,
				Material.PURPLE_STAINED_GLASS,
				Material.BLUE_STAINED_GLASS,
				Material.BROWN_STAINED_GLASS,
				Material.GREEN_STAINED_GLASS,
				Material.RED_STAINED_GLASS,
				Material.BLACK_STAINED_GLASS
		};
		return glassTypes[type];
	}

	static Material idToMaterial(int id) {
		final Material[] materials = Material.values();
		Optional<Material> result = Arrays.stream(materials).filter(x -> x.getId() == id).findFirst();
		if (result.isPresent()) {
			return result.get();
		}
		throw new RuntimeException("No material with that id");
	}

	static MySQL getMysqlFromPlugin(Plugin instance) {
		FileConfiguration config = instance.getConfig();
		return new MySQL(instance, config.getString("dbHost"),
				config.getString("dbPort"), config.getString("dbDatabase"),
				config.getString("dbUser"), config.getString("dbPassword"));
	}
}
