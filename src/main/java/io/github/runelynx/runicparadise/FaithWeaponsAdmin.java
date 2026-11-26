package io.github.runelynx.runicparadise;

import io.github.runelynx.runicparadise.faith.FaithCore;
import io.github.runelynx.runicuniverse.staff.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * /rs Faith > Faith weapons (2026-11-26): edit the weapon definitions in faithsettings.yml (Faith.Weapons) from a menu.
 * Weapon stats are read when the faith system starts and baked into a weapon when it's crafted, so changes apply after
 * "Apply" (the same reload as /staff rf) and only to weapons crafted afterwards.
 */
final class FaithWeaponsAdmin {

	private FaithWeaponsAdmin() {}

	enum F { TEXT, PERCENT, INT, DECIMAL }

	static final class Field {
		final String key, label, help; final F kind; final double min, max;
		Field(String key, String label, F kind, double min, double max, String help) {
			this.key = key; this.label = label; this.kind = kind; this.min = min; this.max = max; this.help = help;
		}
	}

	static final List<Field> FIELDS = Arrays.asList(
			new Field("DisplayName", "Name", F.TEXT, 0, 0, "Item name (colour codes like &e allowed)"),
			new Field("ChanceToLevelUp", "Level-up chance", F.PERCENT, 0, 100, "% chance per kill to gain a faith level"),
			new Field("ChanceToConsumeCharge", "Charge use chance", F.PERCENT, 0, 100, "% chance per use to spend a charge"),
			new Field("Charges", "Charges", F.INT, 1, 100000, "Charges a new weapon starts with"),
			new Field("ZealRequiredToCraft", "Zeal to craft", F.INT, 0, 1000000, "Zeal needed to craft it"),
			new Field("AddDamage", "Extra damage", F.DECIMAL, -100, 100, "Added attack damage"),
			new Field("AddAttackSpeed", "Attack speed", F.INT, -10, 10, "Attack speed modifier (scalar)"),
			new Field("AddMoveSpeed", "Move speed", F.INT, -10, 10, "Movement speed modifier (scalar)"),
			new Field("AddHealth", "Extra health", F.INT, -100, 100, "Added max health"),
			new Field("AddArmor", "Armor", F.INT, -10, 10, "Armor modifier (scalar)"),
			new Field("AddLuck", "Luck", F.INT, -10, 10, "Luck modifier (scalar)"),
			new Field("Lore1", "Lore line 1", F.TEXT, 0, 0, "Tooltip text"),
			new Field("Lore2", "Lore line 2", F.TEXT, 0, 0, "Tooltip text"),
			new Field("Lore3", "Lore line 3", F.TEXT, 0, 0, "Tooltip text"),
			new Field("Lore4", "Lore line 4", F.TEXT, 0, 0, "Tooltip text (often states the level-up %)"),
			new Field("Lore5", "Lore line 5", F.TEXT, 0, 0, "Tooltip text"));

	static File file() {
		return new File(RunicParadise.getInstance().getDataFolder(), "faithsettings.yml");
	}

	static boolean pendingApply = false;

	static void list(Player p) {
		YamlConfiguration y = YamlConfiguration.loadConfiguration(file());
		ConfigurationSection ws = y.getConfigurationSection("Faith.Weapons");
		Gui g = new Gui(4, "&8Faith weapons");
		g.set(4, Gui.item("GOLDEN_SWORD", "&b&lFaith weapons", "&7Defined in faithsettings.yml.", "&7Changes apply after &fApply&7, and only",
				"&7to weapons crafted afterwards."), null);
		int slot = 9;
		if (ws != null) {
			for (String key : ws.getKeys(false)) {
				if (slot >= 27) break;
				ConfigurationSection w = ws.getConfigurationSection(key);
				if (w == null) continue;
				List<String> lore = new ArrayList<>();
				lore.add("&8" + key + " · " + w.getString("Type"));
				lore.add("&7Level-up: &f" + pct(w.getDouble("ChanceToLevelUp")) + "  &7Charge use: &f" + pct(w.getDouble("ChanceToConsumeCharge")));
				lore.add("&7Charges: &f" + w.getInt("Charges") + "  &7Zeal to craft: &f" + w.getInt("ZealRequiredToCraft"));
				lore.add("");
				lore.add("&eClick to edit");
				String mat = w.getString("Type", "IRON_SWORD");
				g.set(slot++, Gui.item(mat, w.getString("DisplayName", key), lore), (pl, t) -> weapon(pl, key));
			}
		}
		if (p.hasPermission(FaithSystemAdmin.EDIT_PERM)) {
			g.set(31, Gui.item(pendingApply ? "LIME_CONCRETE" : "GRAY_CONCRETE", pendingApply ? "&a&lApply changes" : "&7Apply changes",
					"&7Reloads the faith system (same as", "&7/staff rf) so new crafts use the", "&7saved settings. Faith powers pause", "&7for a moment."),
					(pl, t) -> Confirm.menu(pl, "Reload the faith system to apply weapon changes", null, () -> {
						apply();
						Audit.log(pl, "faith weapons apply", "faith system reloaded");
						Ui.ok(pl, "Faith system reloaded; new crafts use the saved weapon settings.");
						list(pl);
					}, () -> list(pl)));
		}
		g.set(27, Gui.item("ARROW", "&7« Faith system"), (pl, t) -> FaithSystemAdmin.main(pl));
		g.frameRow(0).frameRow(3);
		g.openLater(p);
	}

	static void weapon(Player p, String key) {
		YamlConfiguration y = YamlConfiguration.loadConfiguration(file());
		ConfigurationSection w = y.getConfigurationSection("Faith.Weapons." + key);
		if (w == null) {
			list(p);
			return;
		}
		boolean edit = p.hasPermission(FaithSystemAdmin.EDIT_PERM);
		Gui g = new Gui(4, "&8Weapon · " + key);
		g.set(4, Gui.item(w.getString("Type", "IRON_SWORD"), w.getString("DisplayName", key), "&8" + key), null);
		int slot = 9;
		for (Field f : FIELDS) {
			if (slot >= 27) break;
			Object raw = w.get(f.key);
			List<String> lore = new ArrayList<>();
			lore.add("&7Now: &f" + show(f, raw));
			lore.add("&8" + f.help);
			if (f.kind != F.TEXT) lore.add("&8Allowed: " + fmt(f.min) + " to " + fmt(f.max) + (f.kind == F.PERCENT ? "%" : ""));
			if (edit) lore.add("&eClick to change");
			String icon = f.kind == F.TEXT ? "NAME_TAG" : f.kind == F.PERCENT ? "RABBIT_FOOT" : "COMPARATOR";
			g.set(slot++, Gui.item(icon, "&f" + f.label, lore), edit ? (pl, t) -> change(pl, key, f, raw) : null);
		}
		g.set(27, Gui.item("ARROW", "&7« Faith weapons"), (pl, t) -> list(pl));
		g.frameRow(0).frameRow(3);
		g.openLater(p);
	}

	static String pct(double d) {
		double v = Math.round(d * 1000) / 10.0;
		return (v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v)) + "%";
	}

	static String fmt(double d) {
		return d == Math.floor(d) ? String.valueOf((long) d) : String.valueOf(d);
	}

	static String show(Field f, Object raw) {
		if (raw == null) return "(not set)";
		if (f.kind == F.PERCENT) {
			try {
				return pct(Double.parseDouble(raw.toString()));
			} catch (NumberFormatException e) {
				return raw.toString();
			}
		}
		return raw.toString();
	}

	static void change(Player p, String key, Field f, Object old) {
		ChatPrompt.ask(p, "New " + f.label.toLowerCase() + " for " + key + (f.kind == F.PERCENT ? " (in %, e.g. 6)" : "")
				+ (f.kind == F.TEXT ? " (type - to clear the line)" : "") + ", now: " + show(f, old), txt -> {
			String in = txt.trim();
			Object value;
			if (f.kind == F.TEXT) {
				value = in.equals("-") ? "" : in;
			} else {
				double d;
				try {
					d = Double.parseDouble(in.replace("%", ""));
				} catch (NumberFormatException e) {
					Ui.error(p, "That's not a number.");
					return;
				}
				if (d < f.min || d > f.max) {
					Ui.error(p, f.label + " must be " + fmt(f.min) + " to " + fmt(f.max) + ".");
					return;
				}
				value = f.kind == F.PERCENT ? Math.round(d * 10) / 1000.0 : f.kind == F.INT ? (Object) (int) Math.round(d) : d;
			}
			final Object nv = value;
			Confirm.menu(p, "Set " + key + " " + f.label.toLowerCase(), Arrays.asList("&7From: &f" + show(f, old), "&7To: &f" + show(f, nv)), () -> {
				YamlConfiguration y = YamlConfiguration.loadConfiguration(file());
				y.set("Faith.Weapons." + key + "." + f.key, nv);
				try {
					y.save(file());
					pendingApply = true;
					Audit.log(p, "faith weapon edit", key + " " + f.key + ": " + old + " -> " + nv);
					Ui.ok(p, "Saved. Press Apply in the weapons menu when you're done editing.");
				} catch (IOException e) {
					Ui.error(p, "Couldn't save faithsettings.yml: " + e.getMessage());
				}
				weapon(p, key);
			}, () -> weapon(p, key));
		});
	}

	static void apply() {
		RunicParadise.faithSystem.shutdownFaithSystem();
		RunicParadise.faithSystem = null;
		RunicParadise.faithSystem = new FaithCore();
		pendingApply = false;
	}
}
