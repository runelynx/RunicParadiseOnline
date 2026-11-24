package io.github.runelynx.runicparadise;

import io.github.runelynx.runicuniverse.staff.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

/**
 * Explorer's League management for staff (2026-11-24): /rs el menu + typed subcommands + "view mode".
 *
 * Data lives in rp_ExplorerLocations (Status Enabled / Disabled / Archived) and rp_ExplorerCompletions.
 * Players' finds are never deleted: a location that anyone has found can only be archived (hidden, kept as a
 * trophy record). Only never-found locations can be deleted. New locations start Disabled.
 * Gameplay (/explore, /games) reads Status = 'Enabled' only, refreshed via Commands.syncExplorerLocations().
 */
final class ExplorerAdmin implements Listener {

	static final String PERM = "rp.staff.explorersleague";
	static final int DEFAULT_REWARD = 20;
	static final int DEFAULT_RADIUS = 3;

	// ------------------------------------------------------------------ model

	static final class Loc {
		int id; String name; String world; int x, y, z; int reward; int radius; int prereq; String status;
		String creator; String changer; int finds;

		boolean enabled() { return "Enabled".equals(status); }
		boolean archived() { return "Archived".equals(status); }
		World bukkitWorld() { return Bukkit.getWorld(world); }
		Location location() {
			World w = bukkitWorld();
			return w == null ? null : new Location(w, x + 0.5, y, z + 0.5);
		}
		String difficulty() { return radius <= 1 ? "Hard" : radius <= 4 ? "Medium" : "Easy"; }
		String diffColor() { return radius <= 1 ? "&c" : radius <= 4 ? "&e" : "&a"; }
	}

	private static final Map<Integer, Loc> CACHE = new LinkedHashMap<>();
	private static long cacheLoadedAt = 0;

	static synchronized Collection<Loc> all(boolean forceReload) {
		if (forceReload || System.currentTimeMillis() - cacheLoadedAt > 30_000) {
			reload();
		}
		return CACHE.values();
	}

	static Loc get(int id) {
		all(false);
		return CACHE.get(id);
	}

	private static void reload() {
		Map<Integer, Loc> fresh = new LinkedHashMap<>();
		MySQL db = RunicUtilities.getMysqlFromPlugin(RunicParadise.getInstance());
		try (Connection c = db.openConnection();
			 Statement st = c.createStatement();
			 ResultSet rs = st.executeQuery("SELECT l.*, (SELECT COUNT(*) FROM rp_ExplorerCompletions c WHERE c.LocID = l.ID) AS Finds"
					 + " FROM rp_ExplorerLocations l ORDER BY l.ID")) {
			while (rs.next()) {
				Loc l = new Loc();
				l.id = rs.getInt("ID");
				l.name = rs.getString("LocationName");
				String[] p = String.valueOf(rs.getString("Location")).split("\\.");
				l.world = p.length > 0 ? p[0] : "?";
				l.x = p.length > 1 ? parse(p[1]) : 0;
				l.y = p.length > 2 ? parse(p[2]) : 0;
				l.z = p.length > 3 ? parse(p[3]) : 0;
				l.reward = rs.getInt("TokenReward");
				l.radius = rs.getInt("DifficultyRadius");
				l.prereq = rs.getInt("PreReq");
				l.status = rs.getString("Status") == null ? "Disabled" : rs.getString("Status");
				l.creator = rs.getString("Creator");
				l.changer = rs.getString("Changer");
				l.finds = rs.getInt("Finds");
				fresh.put(l.id, l);
			}
		} catch (SQLException e) {
			RunicParadise.getInstance().getLogger().warning("[EL admin] load failed: " + e.getMessage());
			return;
		}
		CACHE.clear();
		CACHE.putAll(fresh);
		cacheLoadedAt = System.currentTimeMillis();
	}

	private static int parse(String s) {
		try {
			return (int) Math.floor(Double.parseDouble(s));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	/** Run an UPDATE/INSERT/DELETE, refresh the admin cache and the gameplay maps. */
	private static boolean write(String sql, Object... params) {
		MySQL db = RunicUtilities.getMysqlFromPlugin(RunicParadise.getInstance());
		try (Connection c = db.openConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
			for (int i = 0; i < params.length; i++) {
				ps.setObject(i + 1, params[i]);
			}
			ps.executeUpdate();
		} catch (SQLException e) {
			RunicParadise.getInstance().getLogger().warning("[EL admin] write failed: " + e.getMessage());
			return false;
		}
		reload();
		Commands.syncExplorerLocations();
		return true;
	}

	// ------------------------------------------------------------------ operations (shared by menu and commands)

	static void setStatus(CommandSender s, Loc l, String status) {
		if (write("UPDATE rp_ExplorerLocations SET Status = ?, Changer = ? WHERE ID = ?", status, s.getName(), l.id)) {
			Ui.ok(s, "#" + l.id + " " + l.name + " is now " + status.toLowerCase() + ".");
			Audit.log(s, "EL " + status.toLowerCase(), "#" + l.id + " " + l.name);
		} else {
			Ui.error(s, "Couldn't save that change.");
		}
	}

	static void create(Player p, String name) {
		String clean = name.replace("'", "").trim();
		if (clean.isEmpty() || clean.length() > 100) {
			Ui.error(p, "Names need 1-100 characters.");
			return;
		}
		Location at = p.getLocation();
		String locStr = at.getWorld().getName() + "." + at.getBlockX() + "." + at.getBlockY() + "." + at.getBlockZ();
		MySQL db = RunicUtilities.getMysqlFromPlugin(RunicParadise.getInstance());
		int newId = -1;
		try (Connection c = db.openConnection();
			 PreparedStatement ps = c.prepareStatement("INSERT INTO rp_ExplorerLocations (LocationName, Location, TokenReward,"
					 + " DifficultyRadius, PreReq, Status, Creator, Changer) VALUES (?, ?, ?, ?, 0, 'Disabled', ?, ?)",
					 Statement.RETURN_GENERATED_KEYS)) {
			ps.setString(1, clean);
			ps.setString(2, locStr);
			ps.setInt(3, DEFAULT_REWARD);
			ps.setInt(4, DEFAULT_RADIUS);
			ps.setString(5, p.getName());
			ps.setString(6, p.getName());
			ps.executeUpdate();
			try (ResultSet k = ps.getGeneratedKeys()) {
				if (k.next()) {
					newId = k.getInt(1);
				}
			}
		} catch (SQLException e) {
			Ui.error(p, "Couldn't create it: " + e.getMessage());
			return;
		}
		reload();
		Audit.log(p, "EL create", "#" + newId + " " + clean + " at " + locStr + " (disabled)");
		Ui.ok(p, "Created #" + newId + " " + clean + " here. It starts disabled: test it, then enable it.");
		Loc l = get(newId);
		if (l != null) {
			detail(p, l, null, null);
		}
	}

	// ------------------------------------------------------------------ list menu

	static final class ListState {
		String world = "All";       // or a world name
		String status = "Active";   // Active (enabled+disabled), Enabled, Disabled, Archived
		String search = null;
		int page = 0;
	}

	private static final Map<UUID, ListState> LIST_STATE = new HashMap<>();

	static void list(Player p) {
		list(p, LIST_STATE.computeIfAbsent(p.getUniqueId(), k -> new ListState()));
	}

	static void list(Player p, ListState st) {
		List<Loc> shown = new ArrayList<>();
		for (Loc l : all(false)) {
			if (!st.world.equals("All") && !st.world.equals(l.world)) continue;
			switch (st.status) {
				case "Active": if (l.archived()) continue; break;
				case "Enabled": if (!l.enabled()) continue; break;
				case "Disabled": if (!"Disabled".equals(l.status)) continue; break;
				case "Archived": if (!l.archived()) continue; break;
				default: break;
			}
			if (st.search != null && !l.name.toLowerCase().contains(st.search.toLowerCase())) continue;
			shown.add(l);
		}
		shown.sort(Comparator.comparing((Loc l) -> l.world).thenComparing(l -> l.name.toLowerCase()));
		int pages = Math.max(1, (shown.size() + 44) / 45);
		st.page = Math.max(0, Math.min(st.page, pages - 1));

		Gui g = new Gui(6, "&8Explorer's League &7(" + shown.size() + ") &8" + (st.page + 1) + "/" + pages);
		for (int i = 0; i < 45; i++) {
			int idx = st.page * 45 + i;
			if (idx >= shown.size()) break;
			Loc l = shown.get(idx);
			g.set(i, icon(l), (pl, click) -> {
				if (click.isRightClick()) {
					teleport(pl, l);
				} else {
					detail(pl, l, null, null);
				}
			});
		}
		g.set(45, Gui.item("ARROW", "&7« Staff menu"), (pl, t) -> StaffMenu.main(pl));
		if (st.page > 0) {
			g.set(46, Gui.item("SPECTRAL_ARROW", "&fPrevious page"), (pl, t) -> { st.page--; list(pl, st); });
		}
		List<String> worlds = new ArrayList<>();
		worlds.add("All");
		for (Loc l : all(false)) if (!worlds.contains(l.world)) worlds.add(l.world);
		g.set(47, Gui.item("MAP", "&fWorld: &b" + st.world, "&7Click to cycle through worlds"), (pl, t) -> {
			st.world = worlds.get((worlds.indexOf(st.world) + 1) % worlds.size());
			st.page = 0;
			list(pl, st);
		});
		List<String> statuses = Arrays.asList("Active", "Enabled", "Disabled", "Archived");
		g.set(48, Gui.item("HOPPER", "&fShowing: &b" + st.status, "&7Active = enabled + disabled", "&7Click to cycle"), (pl, t) -> {
			st.status = statuses.get((statuses.indexOf(st.status) + 1) % statuses.size());
			st.page = 0;
			list(pl, st);
		});
		g.set(49, Gui.item("OAK_SIGN", "&fSearch" + (st.search == null ? "" : ": &b" + st.search),
				"&7Left-click: search by name", st.search == null ? null : "&7Right-click: clear search"), (pl, t) -> {
			if (t.isRightClick()) {
				st.search = null;
				st.page = 0;
				list(pl, st);
			} else {
				ChatPrompt.ask(pl, "Type part of a location name to search for", txt -> {
					st.search = txt;
					st.page = 0;
					list(pl, st);
				});
			}
		});
		g.set(50, Gui.item("EMERALD", "&a&lCreate a location here", "&7Creates a new location where you", "&7are standing. It starts &cdisabled&7.",
				"&7Defaults: " + DEFAULT_REWARD + " tokens, radius " + DEFAULT_RADIUS), (pl, t) ->
				ChatPrompt.ask(pl, "Name for the new location at your position", name -> create(pl, name)));
		boolean viewing = ExplorerView.isOn(p);
		g.set(51, Gui.item(viewing ? "ENDER_EYE" : "ENDER_PEARL", "&fView mode: " + (viewing ? "&aON" : "&7off"),
				"&7Shows nearby locations and their", "&7message zones with particles,", "&7only to you.", "&eClick to turn " + (viewing ? "off" : "on")),
				(pl, t) -> {
					ExplorerView.toggle(pl);
					list(pl, st);
				});
		if (st.page < pages - 1) {
			g.set(52, Gui.item("SPECTRAL_ARROW", "&fNext page"), (pl, t) -> { st.page++; list(pl, st); });
		}
		g.frameRow(5);
		g.openLater(p);
	}

	static ItemStack icon(Loc l) {
		String mat = l.bukkitWorld() == null ? "BARRIER" : l.enabled() ? "LIME_DYE" : l.archived() ? "BROWN_DYE" : "GRAY_DYE";
		List<String> lore = new ArrayList<>();
		lore.add((l.enabled() ? "&a● Enabled" : l.archived() ? "&6● Archived" : "&7● Disabled")
				+ (l.bukkitWorld() == null ? "  &c(world missing)" : ""));
		lore.add("&7" + l.world + " &f" + l.x + " " + l.y + " " + l.z);
		lore.add("&7Difficulty: " + l.diffColor() + l.difficulty() + " &8(radius " + l.radius + ")");
		lore.add("&7Reward: &f" + l.reward + " tokens");
		if (l.prereq > 0) {
			Loc pr = CACHE.get(l.prereq);
			lore.add("&7Needs first: &f#" + l.prereq + (pr == null ? "" : " " + pr.name));
		}
		lore.add("&7Found by: &f" + l.finds + " player" + (l.finds == 1 ? "" : "s"));
		lore.add("&8Created by " + l.creator + (l.changer == null ? "" : ", changed by " + l.changer));
		lore.add("");
		lore.add("&eLeft-click: manage  &8·  &eRight-click: teleport");
		return Gui.item(mat, "&f#" + l.id + " &b" + l.name, lore);
	}

	static void teleport(Player p, Loc l) {
		Location to = l.location();
		if (to == null) {
			Ui.error(p, "World " + l.world + " doesn't exist on this server.");
			return;
		}
		p.closeInventory();
		p.teleport(to);
		Ui.info(p, "Teleported to #" + l.id + " " + l.name + ".");
	}

	// ------------------------------------------------------------------ detail menu

	/** reward/radius are staged in the menu and saved together with "Save changes" (one audit line). */
	static void detail(Player p, Loc l, Integer stagedReward, Integer stagedRadius) {
		int reward = stagedReward != null ? stagedReward : l.reward;
		int radius = stagedRadius != null ? stagedRadius : l.radius;
		boolean dirty = reward != l.reward || radius != l.radius;
		Gui g = new Gui(5, "&8#" + l.id + " &0" + (l.name.length() > 26 ? l.name.substring(0, 26) + "…" : l.name));
		g.set(4, icon(l), null);

		if (!l.archived()) {
			g.set(19, Gui.item(l.enabled() ? "LIME_DYE" : "GRAY_DYE", l.enabled() ? "&a&lEnabled" : "&7&lDisabled",
					l.enabled() ? "&7Players can find it now." : "&7Players can't find it yet.",
					"&eClick to " + (l.enabled() ? "disable" : "enable")), (pl, t) -> {
				setStatus(pl, l, l.enabled() ? "Disabled" : "Enabled");
				detail(pl, get(l.id), null, null);
			});
		} else {
			g.set(19, Gui.item("BROWN_DYE", "&6&lArchived", "&7Hidden from players; finds are kept.", "&eClick to restore (as disabled)"),
					(pl, t) -> Confirm.menu(pl, "Restore #" + l.id + " " + l.name, Collections.singletonList("&7It comes back disabled."),
							() -> { setStatus(pl, l, "Disabled"); detail(pl, get(l.id), null, null); }, () -> detail(pl, l, null, null)));
		}
		g.set(20, Gui.item("ENDER_PEARL", "&fTeleport there"), (pl, t) -> teleport(pl, l));
		g.set(21, Gui.item("NAME_TAG", "&fRename", "&7Now: &f" + l.name), (pl, t) ->
				ChatPrompt.ask(pl, "New name for #" + l.id + " (now \"" + l.name + "\")", txt -> {
					String clean = txt.replace("'", "").trim();
					if (write("UPDATE rp_ExplorerLocations SET LocationName = ?, Changer = ? WHERE ID = ?", clean, pl.getName(), l.id)) {
						Audit.log(pl, "EL rename", "#" + l.id + " " + l.name + " -> " + clean);
						Ui.ok(pl, "Renamed to " + clean + ".");
					}
					detail(pl, get(l.id), null, null);
				}));
		g.set(22, Gui.item("RECOVERY_COMPASS", "&fMove it to where I'm standing", "&7Now at &f" + l.world + " " + l.x + " " + l.y + " " + l.z),
				(pl, t) -> {
					Location at = pl.getLocation();
					String locStr = at.getWorld().getName() + "." + at.getBlockX() + "." + at.getBlockY() + "." + at.getBlockZ();
					Confirm.menu(pl, "Move #" + l.id + " to your position", Arrays.asList("&7From: &f" + l.world + " " + l.x + " " + l.y + " " + l.z,
							"&7To: &f" + locStr.replace('.', ' ')), () -> {
						if (write("UPDATE rp_ExplorerLocations SET Location = ?, Changer = ? WHERE ID = ?", locStr, pl.getName(), l.id)) {
							Audit.log(pl, "EL move", "#" + l.id + " " + l.name + " -> " + locStr);
							Ui.ok(pl, "Moved #" + l.id + ".");
						}
						detail(pl, get(l.id), null, null);
					}, () -> detail(pl, l, null, null));
				});
		Loc pre = l.prereq > 0 ? CACHE.get(l.prereq) : null;
		g.set(23, Gui.item("CHAIN", "&fPrerequisite", "&7Players must find this first:", "&f" + (l.prereq > 0 ? "#" + l.prereq + (pre == null ? "" : " " + pre.name) : "none"),
				"&eClick to change"), (pl, t) -> pickPrereq(pl, l));
		g.set(24, Gui.item("BOOK", "&fWho found it", "&7" + l.finds + " find(s)", "&eClick for details in chat"), (pl, t) -> {
			pl.closeInventory();
			stats(pl, l);
		});
		boolean viewing = ExplorerView.isOn(p);
		g.set(25, Gui.item(viewing ? "ENDER_EYE" : "ENDER_PEARL", "&fView mode: " + (viewing ? "&aON" : "&7off"), "&eClick to toggle"),
				(pl, t) -> { ExplorerView.toggle(pl); detail(pl, l, stagedReward, stagedRadius); });

		// reward
		g.set(28, Gui.item("RED_STAINED_GLASS_PANE", "&cReward -10"), (pl, t) -> detail(pl, l, Math.max(0, reward - 10), radius));
		g.set(29, Gui.item("PINK_STAINED_GLASS_PANE", "&cReward -1"), (pl, t) -> detail(pl, l, Math.max(0, reward - 1), radius));
		g.set(30, Gui.item("SUNFLOWER", "&6Reward: &f" + reward + " tokens" + (reward != l.reward ? " &e(was " + l.reward + ")" : "")), null);
		g.set(31, Gui.item("LIME_STAINED_GLASS_PANE", "&aReward +1"), (pl, t) -> detail(pl, l, reward + 1, radius));
		g.set(32, Gui.item("GREEN_STAINED_GLASS_PANE", "&aReward +10"), (pl, t) -> detail(pl, l, reward + 10, radius));
		// difficulty (radius: smaller = harder)
		g.set(33, Gui.item("RED_STAINED_GLASS_PANE", "&cHarder", "&7radius -1"), (pl, t) -> detail(pl, l, reward, Math.max(1, radius - 1)));
		g.set(34, Gui.item("TARGET", "&eDifficulty: " + (radius <= 1 ? "&cHard" : radius <= 4 ? "&eMedium" : "&aEasy") + " &7(radius " + radius + ")"
						+ (radius != l.radius ? " &e(was " + l.radius + ")" : ""),
				"&6Found within &f" + radius + " &6blocks", "&aGreen \"very close\" within &f" + (2 * radius),
				"&eYellow \"kinda close\" within &f" + (4 * radius), "&cRed \"general area\" within &f" + (6 * radius)), null);
		g.set(35, Gui.item("LIME_STAINED_GLASS_PANE", "&aEasier", "&7radius +1"), (pl, t) -> detail(pl, l, reward, Math.min(50, radius + 1)));

		g.set(36, Gui.item("ARROW", "&7« Back to list" + (dirty ? " &c(discards changes)" : "")), (pl, t) -> list(pl));
		if (dirty) {
			g.set(40, Gui.item("LIME_CONCRETE", "&a&lSave changes", "&7Reward: &f" + l.reward + " → " + reward,
					"&7Radius: &f" + l.radius + " → " + radius), (pl, t) -> {
				if (write("UPDATE rp_ExplorerLocations SET TokenReward = ?, DifficultyRadius = ?, Changer = ? WHERE ID = ?", reward, radius, pl.getName(), l.id)) {
					Audit.log(pl, "EL edit", "#" + l.id + " " + l.name + ": reward " + l.reward + "→" + reward + ", radius " + l.radius + "→" + radius);
					Ui.ok(pl, "Saved.");
				}
				detail(pl, get(l.id), null, null);
			});
		}
		if (!l.archived()) {
			if (l.finds > 0) {
				g.set(44, Gui.item("CHEST", "&6Archive", "&7Hides it from players for good,", "&7keeping the " + l.finds + " player find(s)",
						"&7as their trophy record."), (pl, t) -> Confirm.menu(pl, "Archive #" + l.id + " " + l.name,
						Arrays.asList("&7Hidden from players.", "&7" + l.finds + " find(s) are kept.", "&7Can be restored later."),
						() -> { setStatus(pl, l, "Archived"); list(pl); }, () -> detail(pl, l, null, null)));
			} else {
				g.set(44, Gui.item("TNT", "&cDelete", "&7Nobody has found it yet,", "&7so it can be removed completely."),
						(pl, t) -> Confirm.menu(pl, "Delete #" + l.id + " " + l.name, Collections.singletonList("&cThis can't be undone."), () -> {
							if (get(l.id) != null && get(l.id).finds == 0
									&& write("DELETE FROM rp_ExplorerLocations WHERE ID = ? AND NOT EXISTS (SELECT 1 FROM rp_ExplorerCompletions c WHERE c.LocID = ?)", l.id, l.id)) {
								Audit.log(pl, "EL delete", "#" + l.id + " " + l.name);
								Ui.ok(pl, "Deleted #" + l.id + ".");
							}
							list(pl);
						}, () -> detail(pl, l, null, null)));
			}
		}
		g.frameRow(0).frameRow(4);
		g.openLater(p);
	}

	static void pickPrereq(Player p, Loc target) {
		Gui g = new Gui(6, "&8Needs first: pick for #" + target.id);
		g.set(0, Gui.item("BARRIER", "&fNo prerequisite"), (pl, t) -> setPrereq(pl, target, 0));
		int slot = 1;
		List<Loc> sameWorld = new ArrayList<>();
		for (Loc l : all(false)) {
			if (l.id != target.id && !l.archived() && l.world.equals(target.world)) sameWorld.add(l);
		}
		sameWorld.sort(Comparator.comparing(l -> l.name.toLowerCase()));
		for (Loc l : sameWorld) {
			if (slot >= 45) break;
			g.set(slot++, Gui.item(l.enabled() ? "LIME_DYE" : "GRAY_DYE", "&f#" + l.id + " &b" + l.name, "&eClick to require this first"),
					(pl, t) -> setPrereq(pl, target, l.id));
		}
		g.set(45, Gui.item("ARROW", "&7« Back"), (pl, t) -> detail(pl, target, null, null));
		g.frameRow(5);
		g.openLater(p);
	}

	static void setPrereq(Player p, Loc l, int prereq) {
		if (write("UPDATE rp_ExplorerLocations SET PreReq = ?, Changer = ? WHERE ID = ?", prereq, p.getName(), l.id)) {
			Audit.log(p, "EL prereq", "#" + l.id + " " + l.name + " needs " + (prereq == 0 ? "nothing" : "#" + prereq));
			Ui.ok(p, "Saved.");
		}
		detail(p, get(l.id), null, null);
	}

	static void stats(CommandSender s, Loc l) {
		Ui.send(s, Component.text("#" + l.id + " " + l.name, NamedTextColor.WHITE, TextDecoration.BOLD)
				.append(Component.text("  " + l.finds + " find(s)", NamedTextColor.GRAY)));
		MySQL db = RunicUtilities.getMysqlFromPlugin(RunicParadise.getInstance());
		SimpleDateFormat df = new SimpleDateFormat("d MMM yyyy");
		try (Connection c = db.openConnection();
			 PreparedStatement ps = c.prepareStatement("SELECT PlayerName, Timestamp FROM rp_ExplorerCompletions WHERE LocID = ? ORDER BY Timestamp ASC")) {
			ps.setInt(1, l.id);
			try (ResultSet rs = ps.executeQuery()) {
				List<String> names = new ArrayList<>();
				String first = null, last = null;
				while (rs.next()) {
					String line = rs.getString(1) + " (" + df.format(new Date(rs.getLong(2))) + ")";
					if (first == null) first = line;
					last = line;
					names.add(rs.getString(1));
				}
				if (first != null) {
					CoreActions.row(s, "First", first);
					CoreActions.row(s, "Most recent", last);
					List<String> recent = names.subList(Math.max(0, names.size() - 10), names.size());
					CoreActions.row(s, "Last 10", String.join(", ", recent));
				}
			}
		} catch (SQLException e) {
			Ui.error(s, "Couldn't load finds: " + e.getMessage());
		}
	}

	// ------------------------------------------------------------------ /rs el (typed)

	static final List<String> SUBS = Arrays.asList("menu", "view", "list", "info", "tp", "create", "enable", "disable",
			"rename", "reward", "difficulty", "prereq", "move", "archive", "restore", "delete", "finds");

	static void register() {
		StaffRegistry.register(StaffAction.of("el", "Explorer's League").icon("COMPASS").perm(PERM)
				.desc("Manage Explorer's League locations: menu, view mode, create, enable/disable, edit, archive")
				.usage("[menu|view|list|info|tp|create|enable|disable|rename|reward|difficulty|prereq|move|archive|restore|delete|finds] ...")
				.menu(ExplorerAdmin::list)
				.complete((s, a) -> {
					if (a.length == 1) return SUBS;
					String sub = a[0].toLowerCase();
					if (a.length == 2) {
						if (sub.equals("view")) return Arrays.asList("on", "off");
						if (sub.equals("list")) {
							List<String> o = new ArrayList<>(Arrays.asList("enabled", "disabled", "archived"));
							for (Loc l : all(false)) if (!o.contains(l.world)) o.add(l.world);
							return o;
						}
						if (sub.equals("create") || sub.equals("menu")) return null;
						List<String> ids = new ArrayList<>();
						for (Loc l : all(false)) {
							boolean wantArchived = sub.equals("restore");
							if (l.archived() == wantArchived) ids.add(String.valueOf(l.id));
						}
						return ids;
					}
					if (a.length == 3) {
						if (sub.equals("reward")) return Arrays.asList("10", "15", "20", "25", "30", "50");
						if (sub.equals("difficulty")) return Arrays.asList("1", "2", "3", "4", "5", "6", "8", "10");
						if (sub.equals("prereq")) {
							List<String> ids = new ArrayList<>();
							ids.add("none");
							for (Loc l : all(false)) if (!l.archived()) ids.add(String.valueOf(l.id));
							return ids;
						}
					}
					return null;
				})
				.exec(ExplorerAdmin::command));
	}

	static void command(CommandSender s, String[] a) {
		String sub = a.length == 0 ? "menu" : a[0].toLowerCase();
		Player p = s instanceof Player ? (Player) s : null;
		switch (sub) {
			case "menu":
				if (p != null) list(p);
				return;
			case "view":
				if (p == null) return;
				if (a.length > 1) {
					boolean want = a[1].equalsIgnoreCase("on");
					if (want != ExplorerView.isOn(p)) ExplorerView.toggle(p);
				} else {
					ExplorerView.toggle(p);
				}
				return;
			case "list": {
				String f = a.length > 1 ? a[1] : null;
				int n = 0;
				for (Loc l : all(true)) {
					if (f == null && l.archived()) continue;
					if (f != null && !(l.world.equalsIgnoreCase(f) || l.status.equalsIgnoreCase(f))) continue;
					Component line = Component.text("   ")
							.append(Ui.link("#" + l.id + " " + l.name, l.enabled() ? NamedTextColor.GREEN : l.archived() ? NamedTextColor.GOLD : NamedTextColor.GRAY,
									"/rs el info " + l.id, "Show details"))
							.append(Component.text("  " + l.world + " " + l.x + " " + l.y + " " + l.z + "  r" + l.radius + "  " + l.reward + "t  " + l.finds + " finds",
									NamedTextColor.DARK_GRAY));
					s.sendMessage(line);
					n++;
				}
				Ui.info(s, n + " location(s). Green = enabled, grey = disabled, gold = archived. Click one for details.");
				return;
			}
			case "create":
				if (p == null) return;
				if (a.length < 2) {
					Ui.error(s, "Usage: /rs el create <name...>  (creates it where you stand, disabled)");
					return;
				}
				create(p, String.join(" ", Arrays.copyOfRange(a, 1, a.length)));
				return;
			default:
				break;
		}
		// everything below takes an id
		if (a.length < 2) {
			Ui.error(s, "Usage: /rs el " + sub + " <id>");
			return;
		}
		Loc l;
		try {
			l = all(true).stream().filter(x -> x.id == Integer.parseInt(a[1])).findFirst().orElse(null);
		} catch (NumberFormatException e) {
			l = null;
		}
		if (l == null) {
			Ui.error(s, "No Explorer's League location with id " + a[1] + ".");
			return;
		}
		final Loc loc = l;
		switch (sub) {
			case "info":
				s.sendMessage(Component.text(" "));
				Ui.send(s, Component.text("#" + loc.id + " " + loc.name + "  ", NamedTextColor.WHITE, TextDecoration.BOLD)
						.append(Component.text(loc.status, loc.enabled() ? NamedTextColor.GREEN : loc.archived() ? NamedTextColor.GOLD : NamedTextColor.GRAY)));
				CoreActions.row(s, "Where", loc.world + " " + loc.x + " " + loc.y + " " + loc.z);
				CoreActions.row(s, "Difficulty", loc.difficulty() + " (found within " + loc.radius + ", green " + 2 * loc.radius + ", yellow "
						+ 4 * loc.radius + ", red " + 6 * loc.radius + ")");
				CoreActions.row(s, "Reward", loc.reward + " tokens");
				CoreActions.row(s, "Needs first", loc.prereq > 0 ? "#" + loc.prereq : "none");
				CoreActions.row(s, "Found by", loc.finds + " player(s)");
				Component btns = Component.text("   ");
				if (p != null) {
					btns = btns.append(Ui.button("Teleport", NamedTextColor.AQUA, "/rs el tp " + loc.id, "Go there")).append(Ui.space())
							.append(Ui.button("Manage", NamedTextColor.AQUA, "/rs el menu", "Open the menu")).append(Ui.space());
				}
				if (!loc.archived()) {
					btns = btns.append(loc.enabled()
							? Ui.button("Disable", NamedTextColor.GRAY, "/rs el disable " + loc.id, "Hide it from players")
							: Ui.button("Enable", NamedTextColor.GREEN, "/rs el enable " + loc.id, "Let players find it"));
				}
				s.sendMessage(btns);
				return;
			case "tp":
				if (p != null) teleport(p, loc);
				return;
			case "enable":
			case "disable":
				if (loc.archived()) {
					Ui.error(s, "#" + loc.id + " is archived. Restore it first: /rs el restore " + loc.id);
					return;
				}
				setStatus(s, loc, sub.equals("enable") ? "Enabled" : "Disabled");
				return;
			case "restore":
				if (!loc.archived()) {
					Ui.info(s, "#" + loc.id + " isn't archived.");
					return;
				}
				setStatus(s, loc, "Disabled");
				return;
			case "archive":
				if (loc.archived()) {
					Ui.info(s, "Already archived.");
					return;
				}
				Confirm.ask(s, "Archive #" + loc.id + " " + loc.name + " (hidden from players, " + loc.finds + " find(s) kept)",
						() -> setStatus(s, loc, "Archived"));
				return;
			case "delete":
				if (loc.finds > 0) {
					Ui.error(s, "#" + loc.id + " has been found " + loc.finds + " time(s), so it can only be archived (keeps players' trophies).");
					return;
				}
				Confirm.ask(s, "Delete #" + loc.id + " " + loc.name + " permanently", () -> {
					if (write("DELETE FROM rp_ExplorerLocations WHERE ID = ? AND NOT EXISTS (SELECT 1 FROM rp_ExplorerCompletions c WHERE c.LocID = ?)", loc.id, loc.id)) {
						Audit.log(s, "EL delete", "#" + loc.id + " " + loc.name);
						Ui.ok(s, "Deleted #" + loc.id + ".");
					}
				});
				return;
			case "rename":
				if (a.length < 3) {
					Ui.error(s, "Usage: /rs el rename <id> <new name...>");
					return;
				}
				String name = String.join(" ", Arrays.copyOfRange(a, 2, a.length)).replace("'", "").trim();
				if (write("UPDATE rp_ExplorerLocations SET LocationName = ?, Changer = ? WHERE ID = ?", name, s.getName(), loc.id)) {
					Audit.log(s, "EL rename", "#" + loc.id + " " + loc.name + " -> " + name);
					Ui.ok(s, "Renamed #" + loc.id + " to " + name + ".");
				}
				return;
			case "reward":
			case "difficulty": {
				if (a.length < 3) {
					Ui.error(s, "Usage: /rs el " + sub + " <id> <number>");
					return;
				}
				int v = Integer.parseInt(a[2]);
				if (v < (sub.equals("reward") ? 0 : 1) || v > (sub.equals("reward") ? 1000 : 50)) {
					Ui.error(s, sub.equals("reward") ? "Reward must be 0-1000 tokens." : "Radius must be 1-50 (smaller = harder).");
					return;
				}
				String col = sub.equals("reward") ? "TokenReward" : "DifficultyRadius";
				int old = sub.equals("reward") ? loc.reward : loc.radius;
				if (write("UPDATE rp_ExplorerLocations SET " + col + " = ?, Changer = ? WHERE ID = ?", v, s.getName(), loc.id)) {
					Audit.log(s, "EL " + sub, "#" + loc.id + " " + loc.name + ": " + old + " -> " + v);
					Ui.ok(s, "#" + loc.id + " " + sub + " is now " + v + ".");
				}
				return;
			}
			case "prereq": {
				if (a.length < 3) {
					Ui.error(s, "Usage: /rs el prereq <id> <id|none>");
					return;
				}
				int pr = a[2].equalsIgnoreCase("none") ? 0 : Integer.parseInt(a[2]);
				if (write("UPDATE rp_ExplorerLocations SET PreReq = ?, Changer = ? WHERE ID = ?", pr, s.getName(), loc.id)) {
					Audit.log(s, "EL prereq", "#" + loc.id + " needs " + (pr == 0 ? "nothing" : "#" + pr));
					Ui.ok(s, "Saved.");
				}
				return;
			}
			case "move":
				if (p == null) return;
				Location at = p.getLocation();
				String locStr = at.getWorld().getName() + "." + at.getBlockX() + "." + at.getBlockY() + "." + at.getBlockZ();
				Confirm.ask(s, "Move #" + loc.id + " " + loc.name + " to your position (" + locStr.replace('.', ' ') + ")", () -> {
					if (write("UPDATE rp_ExplorerLocations SET Location = ?, Changer = ? WHERE ID = ?", locStr, s.getName(), loc.id)) {
						Audit.log(s, "EL move", "#" + loc.id + " " + loc.name + " -> " + locStr);
						Ui.ok(s, "Moved #" + loc.id + ".");
					}
				});
				return;
			case "finds":
				stats(s, loc);
				return;
			default:
				Ui.error(s, "Unknown: /rs el " + sub + ". Try /rs help \"Explorer's League\" or just /rs el");
		}
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		LIST_STATE.remove(e.getPlayer().getUniqueId());
		ExplorerView.off(e.getPlayer());
	}

	// ================================================================== view mode

	static final class ExplorerView {
		private static final Set<UUID> VIEWERS = new HashSet<>();
		private static BukkitRunnable task;

		static boolean isOn(Player p) { return VIEWERS.contains(p.getUniqueId()); }

		static void off(Player p) { VIEWERS.remove(p.getUniqueId()); }

		static void toggle(Player p) {
			if (VIEWERS.remove(p.getUniqueId())) {
				Ui.info(p, "Explorer's League view mode is off.");
				return;
			}
			VIEWERS.add(p.getUniqueId());
			p.sendMessage(Ui.prefix().append(Component.text("View mode on. ", NamedTextColor.GREEN))
					.append(Component.text("Green column = enabled, red = disabled. Zones (enabled only): ", NamedTextColor.GRAY))
					.append(Component.text("found ", TextColor6.GOLD)).append(Component.text("· very close ", NamedTextColor.GREEN))
					.append(Component.text("· kinda close ", NamedTextColor.YELLOW)).append(Component.text("· general area", NamedTextColor.RED)));
			start();
		}

		private static final Particle.DustOptions ENABLED = new Particle.DustOptions(Color.fromRGB(40, 230, 90), 1.4f);
		private static final Particle.DustOptions DISABLED = new Particle.DustOptions(Color.fromRGB(235, 50, 50), 1.4f);
		private static final Particle.DustOptions[] BANDS = {
				new Particle.DustOptions(Color.fromRGB(255, 200, 40), 1.1f),  // found (r)
				new Particle.DustOptions(Color.fromRGB(60, 220, 80), 1.0f),   // very close (2r)
				new Particle.DustOptions(Color.fromRGB(240, 230, 60), 1.0f),  // kinda close (4r)
				new Particle.DustOptions(Color.fromRGB(230, 60, 50), 1.0f) }; // general area (6r)
		private static final int[] MULT = { 1, 2, 4, 6 };
		private static final String[] BAND_NAMES = { "&6found zone", "&agreen \"very close\"", "&eyellow \"kinda close\"", "&cred \"general area\"" };

		private static void start() {
			if (task != null) return;
			task = new BukkitRunnable() {
				@Override
				public void run() {
					if (VIEWERS.isEmpty()) {
						cancel();
						task = null;
						return;
					}
					Collection<Loc> locs = all(false);
					for (UUID id : new ArrayList<>(VIEWERS)) {
						Player p = Bukkit.getPlayer(id);
						if (p == null) {
							VIEWERS.remove(id);
							continue;
						}
						draw(p, locs);
					}
				}
			};
			task.runTaskTimer(RunicParadise.getInstance(), 0, 10);
		}

		private static void draw(Player p, Collection<Loc> locs) {
			Location me = p.getLocation();
			String world = me.getWorld().getName();
			Loc nearest = null;
			double nearestD = Double.MAX_VALUE;
			int budget = 2500;
			for (Loc l : locs) {
				if (l.archived() || !l.world.equals(world)) continue;
				double dx = l.x + 0.5 - me.getX(), dz = l.z + 0.5 - me.getZ(), dy = l.y - me.getY();
				double horiz = Math.sqrt(dx * dx + dz * dz);
				double d3 = Math.sqrt(dx * dx + dy * dy + dz * dz);
				if (d3 < nearestD) {
					nearestD = d3;
					nearest = l;
				}
				// marker on the location's block
				if (horiz <= 64) {
					Particle.DustOptions c = l.enabled() ? ENABLED : DISABLED;
					for (double h = 0.1; h <= 2.6; h += 0.25) {
						p.spawnParticle(Particle.REDSTONE, l.x + 0.5, l.y + h, l.z + 0.5, 1, 0, 0, 0, 0, c);
					}
					if (l.enabled()) {
						p.spawnParticle(Particle.END_ROD, l.x + 0.5, l.y + 2.8, l.z + 0.5, 1, 0, 0, 0, 0);
					}
				}
				// zone walls, enabled locations only, when we're anywhere near the outer zone
				if (!l.enabled() || horiz > 6 * l.radius + 24) continue;
				for (int b = 3; b >= 0 && budget > 0; b--) {
					double R = l.radius * MULT[b];
					for (int hy = -1; hy <= 2; hy++) {
						double wy = Math.floor(me.getY()) + hy + 0.1;
						double ddy = wy - l.y;
						if (Math.abs(ddy) >= R) continue;
						double slice = Math.sqrt(R * R - ddy * ddy);
						int points = (int) Math.max(12, Math.min(2000, 2 * Math.PI * slice / 0.7));
						for (int i = 0; i < points && budget > 0; i++) {
							double ang = 2 * Math.PI * i / points;
							double px = l.x + 0.5 + Math.cos(ang) * slice, pz = l.z + 0.5 + Math.sin(ang) * slice;
							double ex = px - me.getX(), ez = pz - me.getZ();
							if (ex * ex + ez * ez > 40 * 40) continue; // only draw the part of the wall near you
							p.spawnParticle(Particle.REDSTONE, px, wy, pz, 1, 0, 0, 0, 0, BANDS[b]);
							budget--;
						}
					}
				}
			}
			if (nearest != null) {
				String zone;
				if (!nearest.enabled()) {
					zone = "&7disabled (players can't find it)";
				} else {
					zone = "&8outside all zones";
					for (int b = 0; b < 4; b++) {
						if (nearestD <= nearest.radius * MULT[b]) {
							zone = BAND_NAMES[b];
							break;
						}
					}
				}
				ActionBars.send(p, ChatColor.translateAlternateColorCodes('&', "&b#" + nearest.id + " " + nearest.name + " &8· &f"
						+ (int) Math.round(nearestD) + " blocks &8· " + zone));
			} else {
				ActionBars.send(p, ChatColor.GRAY + "No Explorer's League locations in this world.");
			}
		}
	}

	/** Small helper so this file doesn't need another import for one colour. */
	private static final class TextColor6 {
		static final net.kyori.adventure.text.format.TextColor GOLD = net.kyori.adventure.text.format.TextColor.color(0xFFC828);
	}
}
