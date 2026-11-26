package io.github.runelynx.runicparadise;

import io.github.runelynx.runicuniverse.staff.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * /rs raffle (2026-11-26): the raffle on one screen. Status and ticket counts for the current raffle, top ticket
 * holders, giving tickets (runs the existing /raffle give), drawing a winner (weighted by tickets; shown to staff only,
 * nothing is announced or changed), settings (written to rafflesettings.yml, then reloaded like /staff rr).
 */
final class RaffleAdmin {

	private RaffleAdmin() {}

	static final String STAFF_PERM = "rp.rafflestaff";
	static final String SETTINGS_PERM = "rp.staff.director";
	private static final SecureRandom RNG = new SecureRandom();

	static String setting(String k) {
		String v = Raffle.raffleSettingsMap.get(k);
		return v == null ? "" : v;
	}

	static boolean enabled() {
		return Boolean.parseBoolean(setting("Enabled"));
	}

	/** [uuid, name, tickets] for the current raffle, most tickets first. */
	static List<Object[]> holders() {
		List<Object[]> l = new ArrayList<>();
		MySQL db = RunicUtilities.getMysqlFromPlugin(RunicParadise.getInstance());
		try (Connection c = db.openConnection();
			 PreparedStatement ps = c.prepareStatement("SELECT UUID, MAX(PlayerName) AS Name, SUM(Quantity) AS T FROM rp_RunicRaffleTickets"
					 + " WHERE RaffleID = ? GROUP BY UUID HAVING T > 0 ORDER BY T DESC")) {
			ps.setString(1, setting("CurrentRaffleID"));
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) l.add(new Object[] { rs.getString("UUID"), rs.getString("Name"), rs.getLong("T") });
			}
		} catch (SQLException e) {
			RunicParadise.getInstance().getLogger().warning("[rs raffle] " + e.getMessage());
		}
		return l;
	}

	// ------------------------------------------------------------------ menu

	static void menu(Player p) {
		List<Object[]> h = holders();
		long tickets = h.stream().mapToLong(x -> (Long) x[2]).sum();
		boolean on = enabled();
		Gui g = new Gui(4, "&8Raffle");
		g.set(4, Gui.item(on ? "LIME_CONCRETE" : "RED_CONCRETE", "&f" + setting("RaffleName"),
				on ? "&a● Running: players can buy tickets" : "&c● Not running: /raffle shows the 'no raffle' message",
				"&7Raffle ID: &f" + setting("CurrentRaffleID"),
				"&7Ticket price: &f" + setting("TicketCost") + "   &7Max per player: &f" + setting("MaxPurchaseTickets"),
				"&7Tickets so far: &f" + tickets + " &7from &f" + h.size() + " &7player(s)"), null);

		g.set(10, Gui.item("BOOK", "&fTop ticket holders", "&7Who has the most tickets in", "&7this raffle (in chat)"), (pl, t) -> {
			pl.closeInventory();
			top(pl, 15);
		});
		if (p.hasPermission(STAFF_PERM)) {
			g.set(11, Gui.item("PAPER", "&fGive tickets", on ? "&7Pick a player, then an amount" : "&cTurn the raffle on first",
					StaffPrefs.cmd(p, "&8(same as /raffle give)")), on ? (pl, t) -> StaffMenu.pickPlayer(pl, "&8Raffle: give tickets to", target ->
					pickAmount(pl, target.getName()), () -> menu(pl)) : null);
			g.set(12, Gui.item("NETHER_STAR", "&6&lDraw a winner", "&7Picks one player at random, weighted",
					"&7by tickets. Shown to you only:", "&7nothing is announced or changed.", "&7You can draw again for runners-up."),
					h.isEmpty() ? null : (pl, t) -> Confirm.menu(pl, "Draw a winner from " + tickets + " tickets", null, () -> draw(pl), () -> menu(pl)));
		}
		g.set(13, Gui.item("COMPARATOR", "&fReload from file", "&7Re-read rafflesettings.yml", StaffPrefs.cmd(p, "&8(same as /staff rr)")), (pl, t) -> {
			reload();
			Audit.log(pl, "raffle reload", "-");
			Ui.ok(pl, "Raffle settings reloaded.");
			menu(pl);
		});
		if (p.hasPermission(SETTINGS_PERM)) {
			g.set(19, Gui.item(on ? "REDSTONE_TORCH" : "LEVER", on ? "&cTurn the raffle off" : "&aTurn the raffle on",
					on ? "&7Players can no longer buy tickets." : "&7Players can buy tickets with /raffle."),
					(pl, t) -> Confirm.menu(pl, (on ? "Turn off" : "Turn on") + " the raffle", null, () -> {
						save("Enabled", !on);
						Audit.log(pl, "raffle " + (on ? "off" : "on"), setting("CurrentRaffleID"));
						menu(pl);
					}, () -> menu(pl)));
			g.set(20, Gui.item("NAME_TAG", "&fRename", "&7Now: " + setting("RaffleName"), "&8Colour codes are allowed"),
					(pl, t) -> prompt(pl, "RaffleName", "New raffle name (colour codes like &3 allowed)", false));
			g.set(21, Gui.item("GOLD_INGOT", "&fTicket price", "&7Now: &f" + setting("TicketCost")),
					(pl, t) -> prompt(pl, "TicketCost", "New ticket price", true));
			g.set(22, Gui.item("HOPPER", "&fMax tickets per player", "&7Now: &f" + setting("MaxPurchaseTickets")),
					(pl, t) -> prompt(pl, "MaxPurchaseTickets", "New maximum tickets a player can buy", true));
			g.set(23, Gui.item("WRITABLE_BOOK", "&fStart a new raffle", "&7Sets a new Raffle ID so ticket", "&7counts start from zero.",
					"&7Old tickets stay in the database", "&7under the old ID."), (pl, t) ->
					ChatPrompt.ask(pl, "New raffle ID, one word, never used before (e.g. Winter2026)", txt -> {
						String id = txt.trim();
						if (!id.matches("[A-Za-z0-9_-]{3,40}")) {
							Ui.error(pl, "Use 3-40 letters, digits, - or _.");
							return;
						}
						if (idUsed(id)) {
							Ui.error(pl, "Raffle ID " + id + " already has tickets. Pick a new one.");
							return;
						}
						Confirm.menu(pl, "Start raffle " + id, Arrays.asList("&7Ticket counts start from zero.",
								"&7Turn it on when you're ready."), () -> {
							save("CurrentRaffleID", id);
							Audit.log(pl, "raffle new", id);
							menu(pl);
						}, () -> menu(pl));
					}));
		}
		g.set(27, Gui.item("ARROW", "&7« Back"), (pl, t) -> StaffMenu.area(pl, "Events & Games"));
		g.frameRow(0).frameRow(3);
		g.openLater(p);
	}

	static void pickAmount(Player p, String target) {
		Gui g = new Gui(2, "&8Tickets for " + target);
		int slot = 0;
		for (int n : new int[] { 1, 5, 10, 25, 50, 100 }) {
			g.set(slot++, Gui.item("PAPER", "&f" + n + " ticket" + (n == 1 ? "" : "s")), (pl, t) ->
					Confirm.menu(pl, "Give " + target + " " + n + " raffle ticket(s)", null, () -> give(pl, target, n), () -> menu(pl)));
		}
		g.set(9, Gui.item("ARROW", "&7« Back"), (pl, t) -> menu(pl));
		g.frameRow(1);
		g.openLater(p);
	}

	private static void prompt(Player p, String key, String question, boolean number) {
		ChatPrompt.ask(p, question, txt -> {
			String v = txt.trim();
			if (number && (!v.matches("\\d{1,9}") || Integer.parseInt(v) < 1)) {
				Ui.error(p, "That needs to be a whole number above 0.");
				return;
			}
			Confirm.menu(p, "Set raffle " + key + " to " + v, null, () -> {
				save(key, number ? (Object) Integer.parseInt(v) : v);
				Audit.log(p, "raffle set", key + " = " + v);
				menu(p);
			}, () -> menu(p));
		});
	}

	// ------------------------------------------------------------------ actions

	static void give(CommandSender s, String target, int n) {
		if (!enabled()) {
			Ui.error(s, "The raffle is turned off, so tickets can't be given right now.");
			return;
		}
		Bukkit.dispatchCommand(s, "raffle give " + n + " " + target);
		Audit.log(s, "raffle give", target + " +" + n + " (" + setting("CurrentRaffleID") + ")");
	}

	static void top(CommandSender s, int limit) {
		List<Object[]> h = holders();
		long total = h.stream().mapToLong(x -> (Long) x[2]).sum();
		Ui.send(s, Ui.legacy(setting("RaffleName")).append(Component.text("  " + total + " tickets, " + h.size() + " players", NamedTextColor.GRAY)));
		for (int i = 0; i < Math.min(limit, h.size()); i++) {
			long t = (Long) h.get(i)[2];
			CoreActions.row(s, "#" + (i + 1) + " " + h.get(i)[1], t + " tickets (" + String.format(Locale.ROOT, "%.1f", 100.0 * t / total) + "% chance)");
		}
		if (h.isEmpty()) Ui.info(s, "No tickets yet for " + setting("CurrentRaffleID") + ".");
	}

	static void draw(CommandSender s) {
		List<Object[]> h = holders();
		long total = h.stream().mapToLong(x -> (Long) x[2]).sum();
		if (total == 0) {
			Ui.error(s, "No tickets to draw from.");
			return;
		}
		long pick = (long) (RNG.nextDouble() * total);
		for (Object[] x : h) {
			pick -= (Long) x[2];
			if (pick < 0) {
				long t = (Long) x[2];
				Ui.send(s, Component.text("Winner: ", NamedTextColor.GOLD, TextDecoration.BOLD)
						.append(Component.text(String.valueOf(x[1]), NamedTextColor.WHITE, TextDecoration.BOLD))
						.append(Component.text("  (" + t + " of " + total + " tickets, "
								+ String.format(Locale.ROOT, "%.1f", 100.0 * t / total) + "% chance)", NamedTextColor.GRAY)));
				Ui.info(s, "Nothing was announced or changed. Announce it yourself; draw again for runners-up.");
				Audit.log(s, "raffle draw", setting("CurrentRaffleID") + ": " + x[1] + " (" + t + "/" + total + " tickets)");
				return;
			}
		}
	}

	static boolean idUsed(String id) {
		MySQL db = RunicUtilities.getMysqlFromPlugin(RunicParadise.getInstance());
		try (Connection c = db.openConnection();
			 PreparedStatement ps = c.prepareStatement("SELECT 1 FROM rp_RunicRaffleTickets WHERE RaffleID = ? LIMIT 1")) {
			ps.setString(1, id);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next();
			}
		} catch (SQLException e) {
			return true;
		}
	}

	static void reload() {
		Raffle.shutdownRaffleSystem();
		new Raffle();
	}

	/** Change one setting in rafflesettings.yml (comments are kept) and reload. */
	static void save(String key, Object value) {
		File f = new File(RunicParadise.getInstance().getDataFolder(), "rafflesettings.yml");
		YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
		y.set("Raffle." + key, value);
		try {
			y.save(f);
		} catch (IOException e) {
			throw new IllegalStateException("couldn't save rafflesettings.yml: " + e.getMessage());
		}
		reload();
	}

	// ------------------------------------------------------------------ /rs raffle

	static void register() {
		StaffRegistry.register(StaffAction.of("raffle", "Events & Games").icon("PAPER")
				.desc("The raffle: status, top ticket holders, give tickets, draw a winner, settings")
				.usage("[status|top|give <player> <amount>|draw|reload|on|off]")
				.menu(RaffleAdmin::menu)
				.complete((s, a) -> {
					if (a.length == 1) return Arrays.asList("status", "top", "give", "draw", "reload", "on", "off");
					if (a.length == 2 && a[0].equalsIgnoreCase("give")) return CoreActions.onlineNames();
					if (a.length == 3 && a[0].equalsIgnoreCase("give")) return Arrays.asList("1", "5", "10", "25", "50");
					return null;
				})
				.exec((s, a) -> {
					String sub = a.length == 0 ? (s instanceof Player ? "menu" : "status") : a[0].toLowerCase();
					switch (sub) {
						case "menu":
							menu((Player) s);
							break;
						case "status":
						case "top":
							top(s, sub.equals("top") ? 15 : 5);
							break;
						case "reload":
							reload();
							Audit.log(s, "raffle reload", "-");
							Ui.ok(s, "Raffle settings reloaded.");
							break;
						case "give":
							if (!s.hasPermission(STAFF_PERM)) { Ui.error(s, "You can't give raffle tickets."); return; }
							if (a.length != 3 || !a[2].matches("\\d+") || Integer.parseInt(a[2]) < 1) {
								Ui.error(s, "Usage: /rs raffle give <player> <amount>");
								return;
							}
							Confirm.ask(s, "Give " + a[1] + " " + a[2] + " raffle ticket(s)", () -> give(s, a[1], Integer.parseInt(a[2])));
							break;
						case "draw":
							if (!s.hasPermission(STAFF_PERM)) { Ui.error(s, "You can't draw the raffle."); return; }
							Confirm.ask(s, "Draw a raffle winner (shown to you only)", () -> draw(s));
							break;
						case "on":
						case "off":
							if (!s.hasPermission(SETTINGS_PERM)) { Ui.error(s, "Only directors can switch the raffle on or off."); return; }
							Confirm.ask(s, "Turn the raffle " + sub, () -> {
								save("Enabled", sub.equals("on"));
								Audit.log(s, "raffle " + sub, setting("CurrentRaffleID"));
							});
							break;
						default:
							Ui.error(s, "Usage: /rs raffle [status|top|give <player> <amount>|draw|reload|on|off]");
					}
				}));
	}
}
