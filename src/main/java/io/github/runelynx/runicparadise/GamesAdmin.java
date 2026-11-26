package io.github.runelynx.runicparadise;

import io.github.runelynx.runicuniverse.staff.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

/**
 * /rs games (2026-11-26): mazes and parkours (rp_RunicGames + rp_RunicGameCompletions).
 * View every game with its rewards and completion stats; look up a player's completions; give a completion (runs the
 * existing /rptokens mazewin, which pays the rewards exactly like the finish command block); and, for directors:
 * edit rewards (used from the next completion on), reset a player's record for a game, add a game, remove an unused one.
 * Command blocks are never touched: the finish line keeps calling /rptokens mazewin <player> <id>.
 */
final class GamesAdmin {

	private GamesAdmin() {}

	static final String EDIT_PERM = "rp.staff.director";

	static final class Game {
		int id; String name; String type; int karma, tokens, souls, cash; double repeat; boolean chest; boolean announce;
		int chestX, chestY, chestZ; int completions; int players; long last;
	}

	static List<Game> load() {
		List<Game> l = new ArrayList<>();
		MySQL db = RunicUtilities.getMysqlFromPlugin(RunicParadise.getInstance());
		try (Connection c = db.openConnection(); Statement st = c.createStatement();
			 ResultSet rs = st.executeQuery("SELECT g.*, COALESCE(SUM(c.Count),0) AS Comp, COUNT(c.ID) AS Players, COALESCE(MAX(c.LastCompletion),0) AS Last"
					 + " FROM rp_RunicGames g LEFT JOIN rp_RunicGameCompletions c ON c.GameID = g.ID GROUP BY g.ID ORDER BY g.GameType, g.GameName")) {
			while (rs.next()) {
				Game g = new Game();
				g.id = rs.getInt("ID");
				g.name = rs.getString("GameName");
				g.type = rs.getString("GameType") == null ? "Other" : rs.getString("GameType");
				g.karma = rs.getInt("KarmaReward");
				g.tokens = rs.getInt("TokenReward");
				g.souls = rs.getInt("SoulReward");
				g.cash = rs.getInt("CashReward");
				g.repeat = rs.getDouble("CashRepeatAdjust");
				g.chest = rs.getInt("ChestReward") == 1;
				g.chestX = rs.getInt("ChestLocationX");
				g.chestY = rs.getInt("ChestLocationY");
				g.chestZ = rs.getInt("ChestLocationZ");
				g.announce = "1".equals(rs.getString("AnnounceFirstCompletion"));
				g.completions = rs.getInt("Comp");
				g.players = rs.getInt("Players");
				g.last = rs.getLong("Last");
				l.add(g);
			}
		} catch (SQLException e) {
			RunicParadise.getInstance().getLogger().warning("[rs games] load failed: " + e.getMessage());
		}
		return l;
	}

	static Game get(int id) {
		for (Game g : load()) if (g.id == id) return g;
		return null;
	}

	private static boolean write(String sql, Object... params) {
		MySQL db = RunicUtilities.getMysqlFromPlugin(RunicParadise.getInstance());
		try (Connection c = db.openConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
			for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
			ps.executeUpdate();
			return true;
		} catch (SQLException e) {
			RunicParadise.getInstance().getLogger().warning("[rs games] write failed: " + e.getMessage());
			return false;
		}
	}

	private static final SimpleDateFormat DF = new SimpleDateFormat("d MMM yyyy");

	static String icon(Game g) {
		return "Parkour".equalsIgnoreCase(g.type) ? "LEATHER_BOOTS" : "Maze".equalsIgnoreCase(g.type) ? "OAK_LEAVES" : "TARGET";
	}

	static List<String> rewardLore(Game g) {
		List<String> l = new ArrayList<>();
		l.add("&7Rewards: &f" + g.tokens + " &7tokens, &f" + g.souls + " &7souls, &f" + g.karma + " &7karma, &f" + g.cash + " &7cash");
		l.add("&7Repeat completions pay &f" + Math.round(g.repeat * 100) + "% &7of the cash" + (g.chest ? ", &fchest reward" : ""));
		return l;
	}

	// ------------------------------------------------------------------ list menu

	private static final Map<UUID, String> FILTER = new HashMap<>();

	static void list(Player p) {
		String filter = FILTER.getOrDefault(p.getUniqueId(), "All");
		List<Game> games = new ArrayList<>();
		for (Game g : load()) if (filter.equals("All") || filter.equalsIgnoreCase(g.type)) games.add(g);
		Gui gui = new Gui(6, "&8Mazes & Parkours &7(" + games.size() + ")");
		int slot = 0;
		for (Game g : games) {
			if (slot >= 45) break;
			List<String> lore = new ArrayList<>();
			lore.add("&7" + g.type + " &8· ID " + g.id);
			lore.addAll(rewardLore(g));
			lore.add("&7Finished by &f" + g.players + " &7player(s), &f" + g.completions + " &7time(s)");
			lore.add("&7Last finish: &f" + (g.last > 0 ? DF.format(new Date(g.last)) : "never"));
			lore.add("");
			lore.add("&eClick to manage");
			gui.set(slot++, Gui.item(icon(g), "&b" + g.name, lore), (pl, t) -> detail(pl, g.id));
		}
		gui.set(45, Gui.item("ARROW", "&7« Back"), (pl, t) -> StaffMenu.area(pl, "Events & Games"));
		List<String> types = Arrays.asList("All", "Maze", "Parkour");
		gui.set(48, Gui.item("HOPPER", "&fShowing: &b" + filter, "&7Click to cycle All / Maze / Parkour"), (pl, t) -> {
			FILTER.put(pl.getUniqueId(), types.get((types.indexOf(filter) + 1) % types.size()));
			list(pl);
		});
		gui.set(49, Gui.item("SPYGLASS", "&fLook up a player", "&7All of one player's maze and", "&7parkour finishes (in chat)"),
				(pl, t) -> ChatPrompt.ask(pl, "Player name to look up", name -> playerSummary(pl, name)));
		if (p.hasPermission(EDIT_PERM)) {
			gui.set(50, Gui.item("EMERALD", "&a&lAdd a new game", "&7Creates a maze or parkour with no", "&7rewards yet; you get its ID for", "&7the finish command block."),
					(pl, t) -> ChatPrompt.ask(pl, "Name of the new maze or parkour", name -> pickType(pl, name.trim())));
		}
		gui.frameRow(5);
		gui.openLater(p);
	}

	static void pickType(Player p, String name) {
		if (name.isEmpty() || name.length() > 50) {
			Ui.error(p, "Names need 1-50 characters.");
			return;
		}
		Gui g = new Gui(3, "&8New game: type");
		g.set(11, Gui.item("OAK_LEAVES", "&fMaze"), (pl, t) -> add(pl, name, "Maze"));
		g.set(15, Gui.item("LEATHER_BOOTS", "&fParkour"), (pl, t) -> add(pl, name, "Parkour"));
		g.frameRow(0).frameRow(2);
		g.openLater(p);
	}

	static void add(Player p, String name, String type) {
		Confirm.menu(p, "Add " + type + " \"" + name + "\"", Collections.singletonList("&7Starts with no rewards."), () -> {
			MySQL db = RunicUtilities.getMysqlFromPlugin(RunicParadise.getInstance());
			int id = -1;
			try (Connection c = db.openConnection();
				 PreparedStatement ps = c.prepareStatement("INSERT INTO rp_RunicGames (GameName, GameType) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
				ps.setString(1, name);
				ps.setString(2, type);
				ps.executeUpdate();
				try (ResultSet k = ps.getGeneratedKeys()) {
					if (k.next()) id = k.getInt(1);
				}
			} catch (SQLException e) {
				Ui.error(p, "Couldn't add it: " + e.getMessage());
				return;
			}
			Audit.log(p, "game add", "#" + id + " " + type + " " + name);
			Ui.ok(p, "Added " + name + " with ID " + id + ". Finish command block: /rptokens mazewin @p " + id);
			detail(p, id);
		}, () -> list(p));
	}

	// ------------------------------------------------------------------ detail menu

	static void detail(Player p, int id) {
		Game g = get(id);
		if (g == null) {
			Ui.error(p, "Game #" + id + " no longer exists.");
			list(p);
			return;
		}
		boolean edit = p.hasPermission(EDIT_PERM);
		Gui gui = new Gui(5, "&8#" + g.id + " &0" + g.name);
		List<String> head = new ArrayList<>();
		head.add("&7" + g.type + " &8· ID " + g.id);
		head.add("&7Finished by &f" + g.players + " &7player(s), &f" + g.completions + " &7time(s)");
		head.add("&7Last finish: &f" + (g.last > 0 ? DF.format(new Date(g.last)) : "never"));
		head.add("&7First finish announced: &f" + (g.announce ? "yes" : "no"));
		if (g.chest) head.add("&7Chest reward at &f" + g.chestX + " " + g.chestY + " " + g.chestZ);
		head.add("&8Finish command: /rptokens mazewin <player> " + g.id);
		gui.set(4, Gui.item(icon(g), "&b&l" + g.name, head), null);

		reward(gui, 19, "SUNFLOWER", "Tokens", "TokenReward", g.tokens, g, edit);
		reward(gui, 20, "SOUL_LANTERN", "Souls", "SoulReward", g.souls, g, edit);
		reward(gui, 21, "GOLDEN_APPLE", "Karma", "KarmaReward", g.karma, g, edit);
		reward(gui, 22, "GOLD_INGOT", "Cash", "CashReward", g.cash, g, edit);
		gui.set(23, Gui.item("CLOCK", "&fRepeat cash: &e" + Math.round(g.repeat * 100) + "%", "&7Share of the cash paid for", "&7finishing again",
				edit ? "&eClick to change" : null), edit ? (pl, t) -> ChatPrompt.ask(pl, "Repeat cash share in % (0-100) for " + g.name, txt -> {
			if (!txt.trim().matches("\\d{1,3}") || Integer.parseInt(txt.trim()) > 100) { Ui.error(pl, "Use 0-100."); return; }
			double v = Integer.parseInt(txt.trim()) / 100.0;
			Confirm.menu(pl, "Set " + g.name + " repeat cash to " + txt.trim() + "%", null, () -> {
				write("UPDATE rp_RunicGames SET CashRepeatAdjust = ? WHERE ID = ?", v, g.id);
				Audit.log(pl, "game edit", "#" + g.id + " " + g.name + " repeat cash " + Math.round(g.repeat * 100) + "% -> " + txt.trim() + "%");
				detail(pl, g.id);
			}, () -> detail(pl, g.id));
		}) : null);
		gui.set(24, Gui.item(g.announce ? "BELL" : "GRAY_DYE", "&fAnnounce first finishes: " + (g.announce ? "&aon" : "&7off"),
				edit ? "&eClick to switch" : null), edit ? (pl, t) -> {
			write("UPDATE rp_RunicGames SET AnnounceFirstCompletion = ? WHERE ID = ?", g.announce ? "0" : "1", g.id);
			Audit.log(pl, "game edit", "#" + g.id + " " + g.name + " announce " + (g.announce ? "off" : "on"));
			detail(pl, g.id);
		} : null);

		gui.set(28, Gui.item("BOOK", "&fRecent finishers", "&7The last 15, in chat"), (pl, t) -> {
			pl.closeInventory();
			recent(pl, g, 15);
		});
		gui.set(29, Gui.item("SPYGLASS", "&fLook up a player", "&7Their record for this game,", "&7with give / reset buttons"),
				(pl, t) -> ChatPrompt.ask(pl, "Player name to look up for " + g.name, name -> playerForGame(pl, name.trim(), g.id)));
		gui.set(30, Gui.item("EMERALD", "&fGive a completion", "&7Pays this game's rewards to an", "&7online player, exactly like the", "&7finish command block"),
				(pl, t) -> StaffMenu.pickPlayer(pl, "&8" + g.name + ": give to", target ->
						Confirm.menu(pl, "Give " + target.getName() + " a " + g.name + " completion", rewardLore(g),
								() -> giveCompletion(pl, target.getName(), g), () -> detail(pl, g.id)), () -> detail(pl, g.id)));
		if (edit && g.players == 0) {
			gui.set(44, Gui.item("TNT", "&cRemove this game", "&7Nobody has finished it, so it", "&7can be removed completely."),
					(pl, t) -> Confirm.menu(pl, "Remove " + g.name + " (#" + g.id + ")", Collections.singletonList("&cThis can't be undone."), () -> {
						if (write("DELETE FROM rp_RunicGames WHERE ID = ? AND NOT EXISTS (SELECT 1 FROM rp_RunicGameCompletions c WHERE c.GameID = ?)", g.id, g.id)) {
							Audit.log(pl, "game remove", "#" + g.id + " " + g.name);
						}
						list(pl);
					}, () -> detail(pl, g.id)));
		} else if (edit) {
			gui.set(44, Gui.item("BARRIER", "&7Can't remove", "&7" + g.players + " player(s) have finished it;", "&7their records count toward ranks."), null);
		}
		gui.set(36, Gui.item("ARROW", "&7« Back to list"), (pl, t) -> list(pl));
		gui.frameRow(0).frameRow(4);
		gui.openLater(p);
	}

	private static void reward(Gui gui, int slot, String icon, String label, String column, int value, Game g, boolean edit) {
		gui.set(slot, Gui.item(icon, "&f" + label + ": &e" + value, "&7Paid on every finish", edit ? "&eClick to change" : null),
				edit ? (pl, t) -> ChatPrompt.ask(pl, "New " + label.toLowerCase() + " reward for " + g.name + " (now " + value + ")", txt -> {
					if (!txt.trim().matches("\\d{1,9}")) { Ui.error(pl, "Use a whole number, 0 or more."); return; }
					int v = Integer.parseInt(txt.trim());
					Confirm.menu(pl, "Set " + g.name + " " + label.toLowerCase() + " to " + v, Collections.singletonList("&7Was " + value + "."), () -> {
						write("UPDATE rp_RunicGames SET " + column + " = ? WHERE ID = ?", v, g.id);
						Audit.log(pl, "game edit", "#" + g.id + " " + g.name + " " + label.toLowerCase() + " " + value + " -> " + v);
						detail(pl, g.id);
					}, () -> detail(pl, g.id));
				}) : null);
	}

	// ------------------------------------------------------------------ chat views and actions

	static void recent(CommandSender s, Game g, int limit) {
		Ui.send(s, Component.text(g.name + " · recent finishers", NamedTextColor.WHITE, TextDecoration.BOLD));
		MySQL db = RunicUtilities.getMysqlFromPlugin(RunicParadise.getInstance());
		try (Connection c = db.openConnection();
			 PreparedStatement ps = c.prepareStatement("SELECT COALESCE(p.PlayerName, c.UUID) AS Name, c.Count, c.LastCompletion FROM rp_RunicGameCompletions c"
					 + " LEFT JOIN rp_PlayerInfo p ON p.UUID = c.UUID WHERE c.GameID = ? ORDER BY c.LastCompletion DESC LIMIT ?")) {
			ps.setInt(1, g.id);
			ps.setInt(2, limit);
			try (ResultSet rs = ps.executeQuery()) {
				int n = 0;
				while (rs.next()) {
					CoreActions.row(s, rs.getString("Name"), rs.getInt("Count") + "× , last " + DF.format(new Date(rs.getLong("LastCompletion"))));
					n++;
				}
				if (n == 0) Ui.info(s, "Nobody has finished it yet.");
			}
		} catch (SQLException e) {
			Ui.error(s, "Couldn't load: " + e.getMessage());
		}
	}

	static UUID uuidOf(String name) {
		Player on = Bukkit.getPlayerExact(name);
		if (on != null) return on.getUniqueId();
		OfflinePlayer op = Bukkit.getOfflinePlayerIfCached(name);
		return op == null ? null : op.getUniqueId();
	}

	static void playerSummary(CommandSender s, String name) {
		UUID id = uuidOf(name);
		if (id == null) {
			Ui.error(s, "I don't know a player called " + name + ".");
			return;
		}
		Ui.send(s, Component.text(name + " · mazes & parkours", NamedTextColor.WHITE, TextDecoration.BOLD));
		Map<Integer, String> names = new HashMap<>();
		for (Game g : load()) names.put(g.id, g.name);
		MySQL db = RunicUtilities.getMysqlFromPlugin(RunicParadise.getInstance());
		try (Connection c = db.openConnection();
			 PreparedStatement ps = c.prepareStatement("SELECT GameID, Count, LastCompletion FROM rp_RunicGameCompletions WHERE UUID = ? ORDER BY LastCompletion DESC")) {
			ps.setString(1, id.toString());
			try (ResultSet rs = ps.executeQuery()) {
				int n = 0;
				while (rs.next()) {
					int gid = rs.getInt("GameID");
					s.sendMessage(Component.text("   ")
							.append(Ui.link(names.getOrDefault(gid, "#" + gid + " (removed)"), NamedTextColor.AQUA, "/rs games player " + name + " " + gid, "Details and buttons"))
							.append(Component.text("  " + rs.getInt("Count") + "×, last " + DF.format(new Date(rs.getLong("LastCompletion"))), NamedTextColor.GRAY)));
					n++;
				}
				Ui.info(s, n + " of " + names.size() + " games finished.");
			}
		} catch (SQLException e) {
			Ui.error(s, "Couldn't load: " + e.getMessage());
		}
	}

	static void playerForGame(CommandSender s, String name, int gameId) {
		Game g = get(gameId);
		UUID id = uuidOf(name);
		if (g == null || id == null) {
			Ui.error(s, g == null ? "No game #" + gameId + "." : "I don't know a player called " + name + ".");
			return;
		}
		int count = 0;
		long last = 0;
		MySQL db = RunicUtilities.getMysqlFromPlugin(RunicParadise.getInstance());
		try (Connection c = db.openConnection();
			 PreparedStatement ps = c.prepareStatement("SELECT Count, LastCompletion FROM rp_RunicGameCompletions WHERE UUID = ? AND GameID = ?")) {
			ps.setString(1, id.toString());
			ps.setInt(2, gameId);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					count = rs.getInt(1);
					last = rs.getLong(2);
				}
			}
		} catch (SQLException e) {
			Ui.error(s, "Couldn't load: " + e.getMessage());
			return;
		}
		Ui.send(s, Component.text(name + " · " + g.name, NamedTextColor.WHITE, TextDecoration.BOLD));
		CoreActions.row(s, "Finished", count == 0 ? "never" : count + " time(s), last " + DF.format(new Date(last)));
		Component btns = Component.text("   ");
		if (Bukkit.getPlayerExact(name) != null) {
			btns = btns.append(Ui.button("Give a completion", NamedTextColor.GREEN, "/rs games give " + name + " " + gameId, "Pays the rewards")).append(Ui.space());
		}
		if (count > 0 && s.hasPermission(EDIT_PERM)) {
			btns = btns.append(Ui.button("Reset their record", NamedTextColor.RED, "/rs games reset " + name + " " + gameId,
					"Removes this game from their completions (affects rank requirements)"));
		}
		s.sendMessage(btns);
	}

	static void giveCompletion(CommandSender s, String player, Game g) {
		if (Bukkit.getPlayerExact(player) == null) {
			Ui.error(s, player + " must be online.");
			return;
		}
		Bukkit.dispatchCommand(s, "rptokens mazewin " + player + " " + g.id);
		Audit.log(s, "game completion given", player + " " + g.name + " (#" + g.id + ")");
	}

	static void reset(CommandSender s, String player, Game g) {
		UUID id = uuidOf(player);
		if (id == null) {
			Ui.error(s, "I don't know a player called " + player + ".");
			return;
		}
		if (write("DELETE FROM rp_RunicGameCompletions WHERE UUID = ? AND GameID = ?", id.toString(), g.id)) {
			Audit.log(s, "game record reset", player + " " + g.name + " (#" + g.id + ")");
		}
	}

	// ------------------------------------------------------------------ /rs games

	static void register() {
		StaffRegistry.register(StaffAction.of("games", "Events & Games").icon("OAK_LEAVES")
				.desc("Mazes and parkours: rewards, who finished, give or reset completions, add games")
				.usage("[list|info <id>|player <name> [id]|give <player> <id>|reset <player> <id>]")
				.menu(GamesAdmin::list)
				.complete((s, a) -> {
					if (a.length == 1) return Arrays.asList("list", "info", "player", "give", "reset");
					List<String> ids = new ArrayList<>();
					for (Game g : load()) ids.add(String.valueOf(g.id));
					if (a.length == 2) return a[0].equalsIgnoreCase("info") ? ids : CoreActions.onlineNames();
					if (a.length == 3 && !a[0].equalsIgnoreCase("info")) return ids;
					return null;
				})
				.exec(GamesAdmin::command));
	}

	static void command(CommandSender s, String[] a) {
		String sub = a.length == 0 ? (s instanceof Player ? "menu" : "list") : a[0].toLowerCase();
		switch (sub) {
			case "menu":
				list((Player) s);
				return;
			case "list":
				for (Game g : load()) {
					s.sendMessage(Component.text("   ")
							.append(Ui.link("#" + g.id + " " + g.name, NamedTextColor.AQUA, "/rs games info " + g.id, "Details"))
							.append(Component.text("  " + g.type + ", " + g.players + " players", NamedTextColor.GRAY)));
				}
				return;
			case "player":
				if (a.length == 2) playerSummary(s, a[1]);
				else if (a.length == 3 && a[2].matches("\\d+")) playerForGame(s, a[1], Integer.parseInt(a[2]));
				else Ui.error(s, "Usage: /rs games player <name> [id]");
				return;
			default:
				break;
		}
		int idArg = sub.equals("info") ? 1 : 2;
		Game g = a.length > idArg && a[idArg].matches("\\d+") ? get(Integer.parseInt(a[idArg])) : null;
		if (g == null) {
			Ui.error(s, "Usage: /rs games [list|info <id>|player <name> [id]|give <player> <id>|reset <player> <id>]");
			return;
		}
		switch (sub) {
			case "info":
				if (s instanceof Player) {
					Player p = (Player) s;
					detail(p, g.id);
				} else {
					Ui.send(s, Component.text("#" + g.id + " " + g.name + " (" + g.type + ")", NamedTextColor.WHITE));
					for (String l : rewardLore(g)) s.sendMessage(Ui.legacy("   " + l));
				}
				return;
			case "give":
				Confirm.ask(s, "Give " + a[1] + " a " + g.name + " completion (pays its rewards)", () -> giveCompletion(s, a[1], g));
				return;
			case "reset":
				if (!s.hasPermission(EDIT_PERM)) {
					Ui.error(s, "Only directors can reset completion records.");
					return;
				}
				Confirm.ask(s, "Reset " + a[1] + "'s " + g.name + " record (can't be undone; affects rank requirements)", () -> reset(s, a[1], g));
				return;
			default:
				Ui.error(s, "Usage: /rs games [list|info <id>|player <name> [id]|give <player> <id>|reset <player> <id>]");
		}
	}
}
