package io.github.runelynx.runicparadise;

import io.github.runelynx.runicuniverse.staff.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

/**
 * /rs Faith (2026-11-26): manage the Faith system as a whole (not one player). Per faith: max level, descriptions,
 * cast message, chat prefix. Per power: on/off, required level, description, and every tuning value FaithConfig
 * exposes (chance, cooldown, radius, damage, potion effects, armor requirements...). Plus global settings (skill-up
 * rate, which worlds powers work in). Everyone on staff can look; directors and admins can change things.
 * Every change asks Yes/No first, applies immediately and goes to the audit log.
 */
final class FaithSystemAdmin {

	private FaithSystemAdmin() {}

	static final String EDIT_PERM = "rp.staff.director";
	private static final SimpleDateFormat DF = new SimpleDateFormat("d MMM yyyy HH:mm");

	static final class PowerRow { int id; String name; String faith; int level; String desc; }

	static List<PowerRow> powers() {
		List<PowerRow> l = new ArrayList<>();
		MySQL db = RunicUtilities.getMysqlFromPlugin(RunicParadise.getInstance());
		try (Connection c = db.openConnection(); Statement st = c.createStatement();
			 ResultSet rs = st.executeQuery("SELECT ID, PowerName, FaithName, RequiredLevel, Description FROM rp_MasterPowers ORDER BY FaithName, RequiredLevel")) {
			while (rs.next()) {
				PowerRow r = new PowerRow();
				r.id = rs.getInt(1);
				r.name = rs.getString(2);
				r.faith = rs.getString(3);
				r.level = rs.getInt(4);
				r.desc = rs.getString(5);
				l.add(r);
			}
		} catch (SQLException e) {
			RunicParadise.getInstance().getLogger().warning("[rs faith system] " + e.getMessage());
		}
		return l;
	}

	/** rp_MasterPowers says "Star" where rp_MasterFaiths says "Stars". */
	static boolean sameFaith(String a, String b) {
		return a != null && b != null && a.replaceAll("s$", "").equalsIgnoreCase(b.replaceAll("s$", ""));
	}

	static boolean tunable(String power) {
		return FaithConfig.DEFS.containsKey(power);
	}

	static String icon(String faith) {
		return FaithIcons.of(faith);
	}

	// ------------------------------------------------------------------ main

	static void main(Player p) {
		List<PowerRow> all = powers();
		Gui g = new Gui(5, "&8Faith system");
		int overrides = 0;
		for (String power : FaithConfig.DEFS.keySet())
			for (FaithConfig.Setting s : FaithConfig.DEFS.get(power)) if (FaithConfig.isOverridden(power, s.key)) overrides++;
		for (PowerRow r : all) if (tunable(r.name) && FaithConfig.isOverridden(r.name, "enabled")) overrides++;
		g.set(4, Gui.item("NETHER_STAR", "&b&lFaith system", "&7" + RunicParadise.faithSettingsMap.size() + " faiths, " + all.size() + " powers",
				"&7Settings changed from default: &f" + overrides, "", "&8Pick a faith to tune it and its powers.",
				p.hasPermission(EDIT_PERM) ? "&8You can change settings." : "&8View only (directors and admins can change)."), null);
		int[] slots = { 10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25 };
		int i = 0;
		List<String> faiths = new ArrayList<>(RunicParadise.faithSettingsMap.keySet());
		Collections.sort(faiths);
		for (String faith : faiths) {
			if (i >= slots.length) break;
			int count = 0, off = 0;
			for (PowerRow r : all) {
				if (sameFaith(r.faith, faith)) {
					count++;
					if (tunable(r.name) && !FaithConfig.enabled(r.name)) off++;
				}
			}
			String[] fs = RunicParadise.faithSettingsMap.get(faith);
			g.set(slots[i++], Gui.item(icon(faith), "&e&l" + faith, "&7Max level: &f" + fs[4], "&7Powers: &f" + count + (off > 0 ? " &c(" + off + " switched off)" : ""),
					"", "&eClick to open"), (pl, t) -> faith(pl, faith));
		}
		g.set(30, Gui.item("COMPARATOR", "&f&lGlobal settings", "&7Skill-up chance, and which worlds", "&7powers work in"), (pl, t) -> global(pl));
		g.set(31, Gui.item("WRITABLE_BOOK", "&fChanged settings", "&7Everything that differs from", "&7the defaults, with who changed it", "&7(in chat)"), (pl, t) -> {
			pl.closeInventory();
			changes(pl);
		});
		g.set(32, Gui.item("SUNFLOWER", "&fReload from database", "&7Re-reads faiths, powers and", "&7power settings"), (pl, t) -> {
			reload();
			Audit.log(pl, "faith system reload", "-");
			Ui.ok(pl, "Faith settings reloaded.");
			main(pl);
		});
		g.set(36, Gui.item("ARROW", "&7« Staff menu"), (pl, t) -> StaffMenu.main(pl));
		g.frameRow(0).frameRow(4);
		g.openLater(p);
	}

	static void reload() {
		Faith.getFaithSettings();
		Faith.getPowerSettings(); // also reloads FaithConfig
	}

	// ------------------------------------------------------------------ one faith

	static void faith(Player p, String faith) {
		String[] fs = RunicParadise.faithSettingsMap.get(faith);
		if (fs == null) {
			main(p);
			return;
		}
		boolean edit = p.hasPermission(EDIT_PERM);
		Gui g = new Gui(6, "&8Faith · " + faith);
		g.set(4, Gui.item(icon(faith), "&e&l" + faith, "&7" + fs[3], "&8Permission: " + fs[2]), null);
		faithField(g, 9, p, faith, "EXPERIENCE_BOTTLE", "Max level", "MaxLevel", fs[4], true, edit);
		faithField(g, 10, p, faith, "BOOK", "Description", "Description", fs[3], false, edit);
		faithField(g, 11, p, faith, "PAPER", "Cast message", "CastMessage", fs[5], false, edit);
		faithField(g, 12, p, faith, "NAME_TAG", "Chat prefix", "ChatPrefix", fs[1], false, edit);
		faithField(g, 13, p, faith, "NAME_TAG", "Chat prefix 2", "ChatPrefix2", fs[6], false, edit);

		int slot = 18;
		for (PowerRow r : powers()) {
			if (!sameFaith(r.faith, faith) || slot >= 45) continue;
			boolean tun = tunable(r.name);
			boolean on = !tun || FaithConfig.enabled(r.name);
			List<String> lore = new ArrayList<>();
			lore.add("&7Required level: &f" + r.level);
			if (tun) {
				lore.add(on ? "&a● On" : "&c● Switched off");
				for (FaithConfig.Setting s : FaithConfig.DEFS.get(r.name)) lore.add("&7" + s.label + ": &f" + show(s, FaithConfig.raw(r.name, s.key)));
			} else {
				lore.add("&8Granted by Slimefun/permissions, or not built yet:");
				lore.add("&8only its description and listed level live here.");
			}
			lore.add("");
			lore.add("&eClick to open");
			ItemStack it = Gui.item(tun ? (on ? "ENCHANTED_BOOK" : "BOOK") : "PAPER", (on ? "&b" : "&c") + r.name, lore);
			g.set(slot++, it, (pl, t) -> power(pl, faith, r.id));
		}
		g.set(45, Gui.item("ARROW", "&7« All faiths"), (pl, t) -> main(pl));
		g.frameRow(0).frameRow(5);
		g.openLater(p);
	}

	private static void faithField(Gui g, int slot, Player p, String faith, String icon, String label, String column, String value, boolean number, boolean edit) {
		g.set(slot, Gui.item(icon, "&f" + label, "&7Now: &f" + (value == null ? "" : value), edit ? "&eClick to change" : null),
				edit ? (pl, t) -> ChatPrompt.ask(pl, "New " + label.toLowerCase() + " for " + faith + (number ? " (whole number)" : "") + ", now: " + value, txt -> {
					String v = txt.trim();
					if (number && (!v.matches("\\d{1,5}") || Integer.parseInt(v) < 1)) {
						Ui.error(pl, "Use a whole number above 0.");
						return;
					}
					if (v.length() > 400) v = v.substring(0, 400);
					final String nv = v;
					Confirm.menu(pl, "Set " + faith + " " + label.toLowerCase(), Arrays.asList("&7From: &f" + value, "&7To: &f" + nv), () -> {
						if (sql("UPDATE rp_MasterFaiths SET " + column + " = ? WHERE FaithName = ?", number ? (Object) Integer.parseInt(nv) : nv, faith)) {
							Faith.getFaithSettings();
							Audit.log(pl, "faith setting", faith + " " + label + ": " + value + " -> " + nv);
						}
						faith(pl, faith);
					}, () -> faith(pl, faith));
				}) : null);
	}

	// ------------------------------------------------------------------ one power

	static void power(Player p, String faith, int powerId) {
		PowerRow r = null;
		for (PowerRow x : powers()) if (x.id == powerId) r = x;
		if (r == null) {
			faith(p, faith);
			return;
		}
		final PowerRow row = r;
		boolean edit = p.hasPermission(EDIT_PERM);
		boolean tun = tunable(row.name);
		Gui g = new Gui(5, "&8" + faith + " · " + row.name);
		g.set(4, Gui.item(tun ? "ENCHANTED_BOOK" : "PAPER", "&b&l" + row.name, "&7" + row.desc, "&8" + faith + " power"), null);

		g.set(9, Gui.item("EXPERIENCE_BOTTLE", "&fRequired level: &e" + row.level,
				tun ? "&7Faith level needed to use it" : "&7Listed level (the Slimefun grant itself uses fixed permissions)",
				edit ? "&eClick to change" : null), edit ? (pl, t) -> ChatPrompt.ask(pl, "New required level for " + row.name + " (now " + row.level + ")", txt -> {
			if (!txt.trim().matches("\\d{1,5}")) { Ui.error(pl, "Use a whole number."); return; }
			int v = Integer.parseInt(txt.trim());
			Confirm.menu(pl, "Set " + row.name + " required level to " + v, Collections.singletonList("&7Was " + row.level), () -> {
				if (sql("UPDATE rp_MasterPowers SET RequiredLevel = ? WHERE ID = ?", v, row.id)) {
					Faith.getPowerSettings();
					Audit.log(pl, "faith power level", row.name + ": " + row.level + " -> " + v);
				}
				power(pl, faith, row.id);
			}, () -> power(pl, faith, row.id));
		}) : null);
		g.set(10, Gui.item("BOOK", "&fDescription", "&7" + row.desc, "&8Shown to players in /faith powers", edit ? "&eClick to change" : null),
				edit ? (pl, t) -> ChatPrompt.ask(pl, "New description for " + row.name, txt -> {
					String v = txt.trim().length() > 500 ? txt.trim().substring(0, 500) : txt.trim();
					Confirm.menu(pl, "Change " + row.name + " description", Arrays.asList("&7To: &f" + v), () -> {
						if (sql("UPDATE rp_MasterPowers SET Description = ? WHERE ID = ?", v, row.id)) {
							Audit.log(pl, "faith power description", row.name + ": " + v);
						}
						power(pl, faith, row.id);
					}, () -> power(pl, faith, row.id));
				}) : null);
		if (tun) {
			boolean on = FaithConfig.enabled(row.name);
			g.set(11, Gui.item(on ? "LIME_CONCRETE" : "RED_CONCRETE", on ? "&a&lOn" : "&c&lSwitched off",
					on ? "&7Players with the level can use it." : "&7Nobody can use it right now.", edit ? "&eClick to switch " + (on ? "off" : "on") : null),
					edit ? (pl, t) -> Confirm.menu(pl, (on ? "Switch off " : "Switch on ") + row.name, null, () -> {
						FaithConfig.set(row.name, "enabled", String.valueOf(!on), pl.getName());
						Audit.log(pl, "faith power " + (on ? "off" : "on"), row.name);
						power(pl, faith, row.id);
					}, () -> power(pl, faith, row.id)) : null);
			int slot = 18;
			for (FaithConfig.Setting s : FaithConfig.DEFS.get(row.name)) {
				if (slot >= 36) break;
				g.set(slot++, settingIcon(p, s, edit), edit ? (pl, t) -> {
					if (t.isRightClick()) resetSetting(pl, s, () -> power(pl, faith, row.id));
					else changeSetting(pl, s, () -> power(pl, faith, row.id));
				} : null);
			}
		}
		g.set(36, Gui.item("ARROW", "&7« " + faith), (pl, t) -> faith(pl, faith));
		g.frameRow(0).frameRow(4);
		g.openLater(p);
	}

	// ------------------------------------------------------------------ global

	static void global(Player p) {
		boolean edit = p.hasPermission(EDIT_PERM);
		Gui g = new Gui(3, "&8Faith · global settings");
		int slot = 10;
		for (FaithConfig.Setting s : FaithConfig.DEFS.get(FaithConfig.GLOBAL)) {
			g.set(slot++, settingIcon(p, s, edit), edit ? (pl, t) -> {
				if (t.isRightClick()) resetSetting(pl, s, () -> global(pl));
				else changeSetting(pl, s, () -> global(pl));
			} : null);
		}
		g.set(18, Gui.item("ARROW", "&7« All faiths"), (pl, t) -> main(pl));
		g.frameRow(0).frameRow(2);
		g.openLater(p);
	}

	// ------------------------------------------------------------------ settings editing

	static String show(FaithConfig.Setting s, String v) {
		if (v == null) return "";
		switch (s.kind) {
			case BOOL: return Boolean.parseBoolean(v) ? "yes" : "no";
			case EFFECT: return FaithConfig.describeEffect(v);
			case WORLDS: return v.replace(",", ", ");
			default:
				if (s.key.equals("chance")) return v + "%";
				if (s.key.equals("cooldown")) return "0".equals(v) ? "none" : v + "s";
				return v;
		}
	}

	static String iconFor(FaithConfig.Setting s) {
		switch (s.kind) {
			case BOOL: return "LEVER";
			case EFFECT: return "POTION";
			case WORLDS: return "MAP";
			default:
				if (s.key.equals("chance")) return "RABBIT_FOOT";
				if (s.key.equals("cooldown")) return "CLOCK";
				if (s.key.contains("radius")) return "TARGET";
				if (s.key.contains("xp")) return "EXPERIENCE_BOTTLE";
				return "COMPARATOR";
		}
	}

	static ItemStack settingIcon(Player p, FaithConfig.Setting s, boolean edit) {
		String v = FaithConfig.raw(s.power, s.key);
		boolean changed = FaithConfig.isOverridden(s.power, s.key);
		List<String> lore = new ArrayList<>();
		lore.add("&7Now: &f" + show(s, v) + (changed ? " &e(changed)" : ""));
		if (changed) lore.add("&8Default: " + show(s, s.def));
		for (String line : StaffMenu.wrap(s.help, 34)) lore.add("&8" + line);
		if (s.kind == FaithConfig.Kind.INT || s.kind == FaithConfig.Kind.DECIMAL) lore.add("&8Allowed: " + fmt(s.min) + " to " + fmt(s.max));
		if (edit) {
			lore.add("");
			lore.add("&eLeft-click: change");
			if (changed) lore.add("&eRight-click: back to default");
		}
		return Gui.item(iconFor(s), (changed ? "&e" : "&f") + s.label, lore);
	}

	private static String fmt(double d) {
		return d == Math.floor(d) ? String.valueOf((long) d) : String.valueOf(d);
	}

	static void changeSetting(Player p, FaithConfig.Setting s, Runnable back) {
		String now = FaithConfig.raw(s.power, s.key);
		String who = s.power.equals(FaithConfig.GLOBAL) ? "global" : s.power;
		if (s.kind == FaithConfig.Kind.BOOL) {
			boolean v = !Boolean.parseBoolean(now);
			Confirm.menu(p, who + " " + s.label + ": " + (v ? "yes" : "no"), null, () -> save(p, s, String.valueOf(v), now, back), back);
			return;
		}
		String question;
		switch (s.kind) {
			case EFFECT:
				question = "New " + s.label.toLowerCase() + " for " + who + " as: effect level seconds (e.g. strength 2 15, now "
						+ FaithConfig.describeEffect(now) + (s.def.endsWith("-1") ? "; -1 seconds = while active" : "") + ")";
				break;
			case WORLDS:
				question = "World names separated by commas (now: " + now + ")";
				break;
			default:
				question = "New " + s.label.toLowerCase() + " for " + who + " (" + fmt(s.min) + "-" + fmt(s.max) + ", now " + show(s, now) + ")";
		}
		ChatPrompt.ask(p, question, txt -> {
			String v = validate(p, s, txt.trim());
			if (v == null) return;
			Confirm.menu(p, "Set " + who + " " + s.label.toLowerCase(), Arrays.asList("&7From: &f" + show(s, now), "&7To: &f" + show(s, v)),
					() -> save(p, s, v, now, back), back);
		});
	}

	/** The value to store, or null (after telling the staff member why) if it's not valid. */
	static String validate(Player p, FaithConfig.Setting s, String in) {
		switch (s.kind) {
			case INT:
			case DECIMAL: {
				double d;
				try {
					d = Double.parseDouble(in);
				} catch (NumberFormatException e) {
					Ui.error(p, "That's not a number.");
					return null;
				}
				if (d < s.min || d > s.max) {
					Ui.error(p, s.label + " must be " + fmt(s.min) + " to " + fmt(s.max) + ".");
					return null;
				}
				return s.kind == FaithConfig.Kind.INT ? String.valueOf((long) Math.round(d)) : fmt(d);
			}
			case EFFECT: {
				String[] parts = in.replace(",", " ").trim().split("\\s+");
				if (parts.length != 3) {
					Ui.error(p, "Type three things: effect level seconds, e.g. speed 1 15");
					return null;
				}
				PotionEffectType t = FaithConfig.effectType(parts[0]);
				if (t == null) {
					Ui.error(p, "Unknown effect \"" + parts[0] + "\". Use names like speed, strength, regeneration, resistance, slowness, absorption.");
					return null;
				}
				int level;
				double secs;
				try {
					level = Integer.parseInt(parts[1]);
					secs = Double.parseDouble(parts[2]);
				} catch (NumberFormatException e) {
					Ui.error(p, "Level and seconds must be numbers.");
					return null;
				}
				if (level < 1 || level > 10) {
					Ui.error(p, "Level must be 1-10.");
					return null;
				}
				boolean infiniteOk = s.def.endsWith("-1");
				if (!(secs == -1 && infiniteOk) && (secs <= 0 || secs > 3600)) {
					Ui.error(p, "Seconds must be above 0 and at most 3600" + (infiniteOk ? " (or -1 for while active)" : "") + ".");
					return null;
				}
				return t.getName() + "," + level + "," + fmt(secs);
			}
			case WORLDS: {
				List<String> ok = new ArrayList<>();
				for (String w : in.split("[,\\s]+")) {
					if (w.isEmpty()) continue;
					if (Bukkit.getWorld(w) == null) {
						Ui.error(p, "There's no world called \"" + w + "\" on this server.");
						return null;
					}
					ok.add(Bukkit.getWorld(w).getName());
				}
				return String.join(",", ok);
			}
			default:
				return in;
		}
	}

	static void save(Player p, FaithConfig.Setting s, String value, String old, Runnable back) {
		if (FaithConfig.set(s.power, s.key, value, p.getName())) {
			Audit.log(p, "faith setting", s.power + " " + s.label + ": " + old + " -> " + value);
			Ui.ok(p, "Saved. It applies from the next time the power triggers.");
		} else {
			Ui.error(p, "Couldn't save that change.");
		}
		back.run();
	}

	static void resetSetting(Player p, FaithConfig.Setting s, Runnable back) {
		if (!FaithConfig.isOverridden(s.power, s.key)) {
			back.run();
			return;
		}
		String now = FaithConfig.raw(s.power, s.key);
		Confirm.menu(p, "Reset " + s.label.toLowerCase() + " to default", Arrays.asList("&7From: &f" + show(s, now), "&7To: &f" + show(s, s.def)), () -> {
			FaithConfig.reset(s.power, s.key);
			Audit.log(p, "faith setting reset", s.power + " " + s.label + " -> default " + s.def);
			back.run();
		}, back);
	}

	static void changes(CommandSender s) {
		Ui.send(s, Component.text("Faith settings changed from default", NamedTextColor.WHITE, TextDecoration.BOLD));
		MySQL db = RunicUtilities.getMysqlFromPlugin(RunicParadise.getInstance());
		int n = 0;
		try (Connection c = db.openConnection(); Statement st = c.createStatement();
			 ResultSet rs = st.executeQuery("SELECT PowerName, SettingKey, Value, UpdatedBy, UpdatedAt FROM rp_FaithPowerSettings ORDER BY PowerName, SettingKey")) {
			while (rs.next()) {
				String power = rs.getString(1), key = rs.getString(2);
				FaithConfig.Setting def = FaithConfig.find(power, key);
				String label = key.equals("enabled") ? "On" : def == null ? key : def.label;
				String val = def == null ? (key.equals("enabled") ? (Boolean.parseBoolean(rs.getString(3)) ? "yes" : "no") : rs.getString(3)) : show(def, rs.getString(3));
				CoreActions.row(s, (power.equals(FaithConfig.GLOBAL) ? "Global" : power) + " · " + label,
						val + (def != null ? "  (default " + show(def, def.def) + ")" : "") + "  by " + rs.getString(4)
								+ (rs.getLong(5) > 0 ? ", " + DF.format(new Date(rs.getLong(5))) : ""));
				n++;
			}
		} catch (SQLException e) {
			Ui.error(s, "Couldn't load: " + e.getMessage());
			return;
		}
		if (n == 0) Ui.info(s, "Nothing changed: every faith power uses its default.");
	}

	private static boolean sql(String q, Object... params) {
		MySQL db = RunicUtilities.getMysqlFromPlugin(RunicParadise.getInstance());
		try (Connection c = db.openConnection(); PreparedStatement ps = c.prepareStatement(q)) {
			for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
			ps.executeUpdate();
			return true;
		} catch (SQLException e) {
			RunicParadise.getInstance().getLogger().warning("[rs faith system] write failed: " + e.getMessage());
			return false;
		}
	}

	// ------------------------------------------------------------------ registration

	static void register() {
		StaffRegistry.area("Faith", "NETHER_STAR", "Explorer's League");
		StaffRegistry.register(StaffAction.of("faithsystem", "Faith").icon("NETHER_STAR")
				.desc("Tune faiths and every power: on/off, levels, chances, cooldowns, effects, radiuses, worlds")
				.usage("[changes|reload]")
				.menu(FaithSystemAdmin::main)
				.complete((s, a) -> a.length == 1 ? Arrays.asList("changes", "reload") : null)
				.exec((s, a) -> {
					String sub = a.length == 0 ? (s instanceof Player ? "menu" : "changes") : a[0].toLowerCase();
					if (sub.equals("menu")) {
						main((Player) s);
					} else if (sub.equals("changes")) {
						changes(s);
					} else if (sub.equals("reload")) {
						reload();
						Audit.log(s, "faith system reload", "-");
						Ui.ok(s, "Faith settings reloaded.");
					} else {
						Ui.error(s, "Usage: /rs faithsystem [changes|reload]");
					}
				}));
	}

	/** Shared faith -> icon lookup (also used by the per-player Faith menu). */
	static final class FaithIcons {
		static String of(String faith) {
			switch (faith == null ? "" : faith) {
				case "Sun": return "SUNFLOWER";
				case "Moon": return "ENDER_EYE";
				case "Stars": case "Star": return "NETHER_STAR";
				case "Fire": return "BLAZE_POWDER";
				case "Water": return "WATER_BUCKET";
				case "Air": return "FEATHER";
				case "Earth": return "GRASS_BLOCK";
				case "Time": return "CLOCK";
				case "Fate": return "TOTEM_OF_UNDYING";
				case "Nether": return "NETHERRACK";
				case "Aether": return "GLOWSTONE_DUST";
				case "Nature": return "OAK_SAPLING";
				case "Tech": return "REDSTONE";
				default: return "BOOK";
			}
		}
	}
}
