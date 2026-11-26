package io.github.runelynx.runicparadise;

import io.github.runelynx.runicuniverse.staff.*;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * /rs faith (2026-11-26): view and manage one player's Faith from a menu. Changes run the existing staff
 * subcommands (/faith enable | setlevel | skillup) as the staff member, so their rules and messages are unchanged.
 * Viewing works for offline players (from the database); changes need the player online (the faith system only
 * holds online players).
 */
final class FaithAdmin {

	private FaithAdmin() {}

	static final String EDIT_PERM = "rp.faith.staff";

	private static final Map<String, String> ICONS = new HashMap<>();
	static {
		ICONS.put("Sun", "SUNFLOWER");
		ICONS.put("Moon", "ENDER_EYE");
		ICONS.put("Stars", "NETHER_STAR");
		ICONS.put("Fire", "BLAZE_POWDER");
		ICONS.put("Water", "WATER_BUCKET");
		ICONS.put("Air", "FEATHER");
		ICONS.put("Earth", "GRASS_BLOCK");
		ICONS.put("Time", "CLOCK");
		ICONS.put("Fate", "TOTEM_OF_UNDYING");
		ICONS.put("Nether", "NETHERRACK");
		ICONS.put("Aether", "GLOWSTONE_DUST");
		ICONS.put("Nature", "OAK_SAPLING");
		ICONS.put("Tech", "REDSTONE");
	}

	static List<String> faithNames() {
		List<String> l = new ArrayList<>(RunicParadise.faithSettingsMap.keySet());
		Collections.sort(l);
		return l;
	}

	static int maxLevel(String faith) {
		String[] s = RunicParadise.faithSettingsMap.get(faith);
		try {
			return s == null ? 0 : Integer.parseInt(s[4]);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	static final class Row { int level; boolean active; long since; }

	static Map<String, Row> load(UUID id) {
		Map<String, Row> m = new HashMap<>();
		MySQL db = RunicUtilities.getMysqlFromPlugin(RunicParadise.getInstance());
		try (Connection c = db.openConnection();
			 PreparedStatement ps = c.prepareStatement("SELECT FaithName, Level, Active, Timestamp FROM rp_PlayerFaiths WHERE UUID = ?")) {
			ps.setString(1, id.toString());
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					Row r = new Row();
					r.level = rs.getInt("Level");
					r.active = rs.getInt("Active") == 1;
					r.since = rs.getLong("Timestamp");
					m.put(rs.getString("FaithName"), r);
				}
			}
		} catch (SQLException e) {
			RunicParadise.getInstance().getLogger().warning("[rs faith] load failed: " + e.getMessage());
		}
		return m;
	}

	static String activeFaith(UUID id, Map<String, Row> rows) {
		MySQL db = RunicUtilities.getMysqlFromPlugin(RunicParadise.getInstance());
		try (Connection c = db.openConnection();
			 PreparedStatement ps = c.prepareStatement("SELECT ActiveFaith FROM rp_PlayerInfo WHERE UUID = ?")) {
			ps.setString(1, id.toString());
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next() && rs.getString(1) != null && !rs.getString(1).isEmpty()) return rs.getString(1);
			}
		} catch (SQLException ignored) {
		}
		for (Map.Entry<String, Row> e : rows.entrySet()) if (e.getValue().active) return e.getKey();
		return null;
	}

	static long[] zeal(UUID id) {
		MySQL db = RunicUtilities.getMysqlFromPlugin(RunicParadise.getInstance());
		try (Connection c = db.openConnection();
			 PreparedStatement ps = c.prepareStatement("SELECT Zeal, LifetimeZeal, SacrificeZeal, LifetimeSacrifice FROM rp_PlayerInfo WHERE UUID = ?")) {
			ps.setString(1, id.toString());
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) return new long[] { rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getLong(4) };
			}
		} catch (SQLException ignored) {
		}
		return new long[4];
	}

	// ------------------------------------------------------------------ menu

	static void menu(Player staff, OfflinePlayer target) {
		Player online = target.getPlayer();
		String name = target.getName() == null ? target.getUniqueId().toString() : target.getName();
		Map<String, Row> rows = load(target.getUniqueId());
		String active = activeFaith(target.getUniqueId(), rows);
		long[] z = zeal(target.getUniqueId());
		boolean canEdit = staff.hasPermission(EDIT_PERM) && online != null;
		SimpleDateFormat df = new SimpleDateFormat("d MMM yyyy");

		Gui g = new Gui(5, "&8Faith &7· &0" + name);
		List<String> head = new ArrayList<>();
		head.add("&7Active faith: &f" + (active == null ? "none" : active)
				+ (active != null && rows.containsKey(active) ? " &7(level &f" + rows.get(active).level + "&7/" + maxLevel(active) + ")" : ""));
		head.add("&7Faiths started: &f" + rows.size() + " &7of " + faithNames().size());
		head.add("&7Zeal: &f" + z[0] + " &7(lifetime &f" + z[1] + "&7)");
		head.add("&7Sacrifice zeal: &f" + z[2] + " &7(lifetime &f" + z[3] + "&7)");
		head.add(online == null ? "&cOffline: view only" : "&aOnline");
		g.set(4, Gui.head(target, "&b&l" + name, head), null);

		int[] slots = { 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34 };
		int i = 0;
		for (String faith : faithNames()) {
			if (i >= slots.length) break;
			Row r = rows.get(faith);
			int max = maxLevel(faith);
			String perm = RunicParadise.faithSettingsMap.get(faith)[2];
			Boolean unlocked = online == null ? null : online.hasPermission(perm);
			boolean isActive = faith.equals(active);
			List<String> lore = new ArrayList<>();
			lore.add(isActive ? "&a● Active faith" : r != null ? "&7● Started, not active" : "&8● Never used");
			lore.add("&7Level: &f" + (r == null ? 0 : r.level) + " &7/ " + max + bar(r == null ? 0 : r.level, max));
			if (r != null && r.since > 0) lore.add("&8First used " + df.format(new Date(r.since)));
			lore.add(unlocked == null ? "&8Unlocked: unknown (player offline)" : unlocked ? "&7Unlocked: &ayes" : "&7Unlocked: &cno &8(" + perm + ")");
			if (canEdit) {
				lore.add("");
				if (!isActive) lore.add("&eLeft-click: make this the active faith");
				lore.add("&eRight-click: set level");
				lore.add("&eShift-click: skill up +1");
			}
			String icon = ICONS.getOrDefault(faith, "BOOK");
			final String f = faith;
			org.bukkit.inventory.ItemStack faithIcon = Gui.item(icon, (isActive ? "&a&l" : r != null ? "&f" : "&7") + faith, lore);
			if (isActive) {
				// enchanted shimmer on the active faith (hidden enchant, same trick as the Chat Options menu)
				org.bukkit.inventory.meta.ItemMeta im = faithIcon.getItemMeta();
				im.addEnchant(org.bukkit.enchantments.Enchantment.LUCK, 1, true);
				im.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
				faithIcon.setItemMeta(im);
			}
			g.set(slots[i++], faithIcon, canEdit ? (pl, t) -> click(pl, online, f, t) : null);
		}
		g.set(40, Gui.item("BOOK", "&fFaith stats in chat", "&7Same as /faith stats " + name), online == null ? null : (pl, t) -> {
			pl.closeInventory();
			Bukkit.dispatchCommand(pl, "faith stats " + name);
		});
		g.set(36, Gui.item("ARROW", "&7« Back to Players"), (pl, t) -> StaffMenu.area(pl, "Players"));
		g.set(44, Gui.item("SUNFLOWER", "&fRefresh"), (pl, t) -> menu(pl, target));
		g.frameRow(0).frameRow(4);
		g.openLater(staff);
	}

	private static String bar(int level, int max) {
		if (max <= 0) return "";
		int filled = (int) Math.round(10.0 * Math.min(level, max) / max);
		return "  &a" + "▮".repeat(filled) + "&8" + "▮".repeat(10 - filled);
	}

	private static void click(Player staff, Player target, String faith, ClickType t) {
		if (!target.isOnline()) {
			Ui.error(staff, target.getName() + " has gone offline.");
			return;
		}
		if (t.isShiftClick()) {
			Confirm.menu(staff, "Skill up " + target.getName() + "'s " + faith + " by 1", null, () -> {
				Bukkit.dispatchCommand(staff, "faith skillup " + target.getName() + " " + faith);
				Audit.log(staff, "faith skillup", target.getName() + " " + faith);
				menu(staff, target);
			}, () -> menu(staff, target));
		} else if (t.isRightClick()) {
			ChatPrompt.ask(staff, "New " + faith + " level for " + target.getName() + " (0-" + maxLevel(faith) + ")", txt -> {
				int lvl;
				try {
					lvl = Integer.parseInt(txt.trim());
				} catch (NumberFormatException e) {
					Ui.error(staff, "That's not a number.");
					return;
				}
				if (lvl < 0 || lvl > maxLevel(faith)) {
					Ui.error(staff, "Level must be 0-" + maxLevel(faith) + ".");
					return;
				}
				Confirm.menu(staff, "Set " + target.getName() + "'s " + faith + " to level " + lvl, null, () -> {
					Bukkit.dispatchCommand(staff, "faith setlevel " + target.getName() + " " + faith + " " + lvl);
					Audit.log(staff, "faith setlevel", target.getName() + " " + faith + " -> " + lvl);
					menu(staff, target);
				}, () -> menu(staff, target));
			});
		} else {
			Confirm.menu(staff, "Make " + faith + " " + target.getName() + "'s active faith",
					Collections.singletonList("&7Their other faiths keep their levels."), () -> {
						Bukkit.dispatchCommand(staff, "faith enable " + target.getName() + " " + faith);
						Audit.log(staff, "faith enable", target.getName() + " " + faith);
						menu(staff, target);
					}, () -> menu(staff, target));
		}
	}

	// ------------------------------------------------------------------ /rs faith

	static void register() {
		StaffRegistry.register(StaffAction.of("faith", "Players").icon("NETHER_STAR").targetsPlayer()
				.desc("View and manage a player's Faith: active faith, levels, unlocks, zeal")
				.usage("<player> [enable|setlevel|skillup] [faith] [level]")
				.menu(p -> StaffMenu.pickPlayer(p, "&8Faith: pick a player", t -> menu(p, t), () -> StaffMenu.area(p, "Players")))
				.complete((s, a) -> {
					if (a.length == 1) return CoreActions.onlineNames();
					if (a.length == 2) return Arrays.asList("enable", "setlevel", "skillup");
					if (a.length == 3) return faithNames();
					if (a.length == 4 && a[1].equalsIgnoreCase("setlevel")) {
						int max = maxLevel(a[2]);
						return Arrays.asList("0", String.valueOf(max / 4), String.valueOf(max / 2), String.valueOf(max));
					}
					return null;
				})
				.exec(FaithAdmin::command));
	}

	static void command(CommandSender s, String[] a) {
		if (a.length == 0) {
			Ui.error(s, "Usage: /rs faith <player> [enable|setlevel|skillup] [faith] [level]");
			return;
		}
		Player online = Bukkit.getPlayerExact(a[0]);
		OfflinePlayer target = online != null ? online : Bukkit.getOfflinePlayerIfCached(a[0]);
		if (target == null) {
			Ui.error(s, "I don't know a player called " + a[0] + ".");
			return;
		}
		if (a.length == 1) {
			if (s instanceof Player) {
				Player p = (Player) s;
				menu(p, target);
			} else if (online != null) {
				Bukkit.dispatchCommand(s, "faith stats " + a[0]);
			}
			return;
		}
		if (!s.hasPermission(EDIT_PERM)) {
			Ui.error(s, "You can view Faith but not change it.");
			return;
		}
		if (online == null) {
			Ui.error(s, a[0] + " must be online to change their Faith.");
			return;
		}
		String sub = a[1].toLowerCase();
		String faith = a.length > 2 ? faithNames().stream().filter(f -> f.equalsIgnoreCase(a[2])).findFirst().orElse(null) : null;
		if (faith == null) {
			Ui.error(s, "Pick a faith: " + String.join(", ", faithNames()));
			return;
		}
		switch (sub) {
			case "enable":
				Confirm.ask(s, "Make " + faith + " " + online.getName() + "'s active faith", () -> {
					Bukkit.dispatchCommand(s, "faith enable " + online.getName() + " " + faith);
					Audit.log(s, "faith enable", online.getName() + " " + faith);
				});
				break;
			case "skillup":
				Confirm.ask(s, "Skill up " + online.getName() + "'s " + faith + " by 1", () -> {
					Bukkit.dispatchCommand(s, "faith skillup " + online.getName() + " " + faith);
					Audit.log(s, "faith skillup", online.getName() + " " + faith);
				});
				break;
			case "setlevel":
				if (a.length < 4 || !a[3].matches("\\d+") || Integer.parseInt(a[3]) > maxLevel(faith)) {
					Ui.error(s, "Usage: /rs faith <player> setlevel <faith> <0-" + maxLevel(faith) + ">");
					return;
				}
				Confirm.ask(s, "Set " + online.getName() + "'s " + faith + " to level " + a[3], () -> {
					Bukkit.dispatchCommand(s, "faith setlevel " + online.getName() + " " + faith + " " + a[3]);
					Audit.log(s, "faith setlevel", online.getName() + " " + faith + " -> " + a[3]);
				});
				break;
			default:
				Ui.error(s, "Usage: /rs faith <player> [enable|setlevel|skillup] [faith] [level]");
		}
	}
}
