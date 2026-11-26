package io.github.runelynx.runicparadise;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tunable Faith power settings (2026-11-26). Every value the powers used to hard-code lives here with that old value
 * as its default; staff overrides are stored in rp_FaithPowerSettings and edited from /rs (Faith menu). Deleting an
 * override returns a setting to its default. "*Global" holds faith-wide settings (skill-up rate, power worlds).
 */
final class FaithConfig {

	private FaithConfig() {}

	enum Kind { BOOL, INT, DECIMAL, EFFECT, WORLDS }

	static final class Setting {
		final String power, key, label, help; final Kind kind; final String def; final double min, max;
		Setting(String power, String key, String label, Kind kind, String def, double min, double max, String help) {
			this.power = power; this.key = key; this.label = label; this.kind = kind; this.def = def;
			this.min = min; this.max = max; this.help = help;
		}
	}

	static final String GLOBAL = "*Global";

	/** power -> its settings, in menu order */
	static final LinkedHashMap<String, List<Setting>> DEFS = new LinkedHashMap<>();
	private static final Map<String, String> VALUES = new ConcurrentHashMap<>();

	private static void def(String power, String key, String label, Kind kind, String def, double min, double max, String help) {
		DEFS.computeIfAbsent(power, k -> new ArrayList<>()).add(new Setting(power, key, label, kind, def, min, max, help));
	}

	/** Standard "chance + cooldown" pair used by most triggered powers. */
	private static void roll(String power, int chance, int cooldownSec) {
		def(power, "chance", "Chance", Kind.INT, String.valueOf(chance), 0, 100, "% chance each time it can trigger");
		def(power, "cooldown", "Cooldown", Kind.INT, String.valueOf(cooldownSec), 0, 3600, "Seconds before it can trigger again for that player (0 = none)");
	}

	static {
		def(GLOBAL, "skillupPercent", "Skill-up chance", Kind.INT, "5", 0, 100,
				"% chance per skill-up point when killing monsters (a mob worth 2 points = 2x this)");
		def(GLOBAL, "noPowerWorlds", "Worlds with no powers", Kind.WORLDS, "RunicSky,RunicSkyNether,RunicSkyEnd,RunicMuseum", 0, 0,
				"Powers never trigger in these worlds");
		def(GLOBAL, "mainWorlds", "Main-world-only worlds", Kind.WORLDS, "RunicKingdom,Mining", 0, 0,
				"Powers marked main-world-only (Graceful Steps, Rewind, Earthen Shield, Forest Armor, Gravity Flux, Tempest Armor, Inevitable Demise, Deep Wader, Earth's Bounty) only work here");
		def(GLOBAL, "netherWorlds", "Netherborn nethers", Kind.WORLDS, "RunicKingdom_nether,MiningNether", 0, 0,
				"Netherborn applies while in these worlds");

		roll("Sunflare", 15, 30);
		def("Sunflare", "radius", "Radius", Kind.INT, "8", 1, 32, "Blocks around the player");
		def("Sunflare", "fireSeconds", "Burn time", Kind.DECIMAL, "3", 0.5, 30, "Seconds monsters burn");

		roll("Lunar Calm", 8, 30);
		def("Lunar Calm", "radius", "Radius", Kind.INT, "15", 1, 48, "Blocks around the player");
		def("Lunar Calm", "seconds", "Calm time", Kind.DECIMAL, "5", 1, 60, "Seconds monsters ignore the player");

		roll("Solar Power", 3, 0);
		def("Solar Power", "effect1", "Effect 1", Kind.EFFECT, "DAMAGE_RESISTANCE,2,15", 0, 0, "Given to the player");
		def("Solar Power", "effect2", "Effect 2", Kind.EFFECT, "INCREASE_DAMAGE,2,15", 0, 0, "Given to the player");

		roll("Celestial Healing", 5, 20);
		def("Celestial Healing", "effect1", "Effect", Kind.EFFECT, "REGENERATION,1,15", 0, 0, "Given to the player");
		def("Celestial Healing", "instantHeal", "Instant heal too", Kind.BOOL, "true", 0, 0, "Also heals instantly");

		roll("Volcanic Fury", 5, 0);
		def("Volcanic Fury", "effect1", "Effect", Kind.EFFECT, "INCREASE_DAMAGE,3,7.5", 0, 0, "Given to the player");

		roll("Unstable Embers", 11, 0);
		def("Unstable Embers", "radius", "Radius", Kind.INT, "4", 1, 16, "Blocks around the player");
		def("Unstable Embers", "damage", "Damage", Kind.DECIMAL, "4", 0, 40, "Damage to each monster (2 = 1 heart)");

		roll("Inevitable Demise", 21, 0);
		def("Inevitable Demise", "hearts", "Only below", Kind.DECIMAL, "3", 0.5, 20, "Only works when the player has this many hearts or fewer");
		def("Inevitable Demise", "radius", "Radius", Kind.INT, "2", 0, 16, "Monsters this close are also killed");

		roll("Rewind", 10, 0);
		def("Rewind", "health", "Health restored", Kind.DECIMAL, "16", 1, 40, "Health after rewinding (2 = 1 heart)");
		def("Rewind", "effect1", "Effect", Kind.EFFECT, "DAMAGE_RESISTANCE,2,10", 0, 0, "Given to the player");

		def("Netherborn", "effect1", "Effect 1", Kind.EFFECT, "HEALTH_BOOST,2,-1", 0, 0, "While in a Netherborn nether (-1 seconds = as long as they stay)");
		def("Netherborn", "effect2", "Effect 2", Kind.EFFECT, "DAMAGE_RESISTANCE,1,-1", 0, 0, "While in a Netherborn nether (-1 seconds = as long as they stay)");

		roll("Vampirism", 21, 0);
		def("Vampirism", "heal", "Health stolen", Kind.DECIMAL, "3", 0, 40, "Health gained per kill (2 = 1 heart)");

		roll("Tempest Armor", 5, 0);
		def("Tempest Armor", "radius", "Radius", Kind.INT, "5", 1, 24, "Blocks around the player");
		def("Tempest Armor", "lightning", "Lightning", Kind.BOOL, "true", 0, 0, "Strike flung monsters with lightning");
		def("Tempest Armor", "fullGold", "Needs full gold armor", Kind.BOOL, "true", 0, 0, "All four gold armor pieces required");

		roll("Healing Breeze", 5, 30);
		def("Healing Breeze", "radius", "Radius", Kind.INT, "40", 1, 64, "Players and tamed wolves within this range");
		def("Healing Breeze", "effect1", "Effect", Kind.EFFECT, "REGENERATION,2,10", 0, 0, "Given to everyone healed");

		roll("Protective Bubble", 10, 20);
		def("Protective Bubble", "effect1", "Effect", Kind.EFFECT, "ABSORPTION,1,15", 0, 0, "Given to the player");

		roll("Arctic Frost", 8, 25);
		def("Arctic Frost", "radius", "Radius", Kind.INT, "7", 1, 32, "Blocks around the player");
		def("Arctic Frost", "effect1", "Effect", Kind.EFFECT, "SLOW,3,7.5", 0, 0, "Given to nearby monsters");

		def("Deep Wader", "xpCost", "XP level cost", Kind.INT, "1", 0, 30, "Levels spent per use");
		def("Deep Wader", "effect1", "Effect", Kind.EFFECT, "WATER_BREATHING,1,600", 0, 0, "Given to the player");

		def("Earth's Bounty", "xpCost", "XP level cost", Kind.INT, "1", 0, 30, "Levels spent per use");
		def("Earth's Bounty", "radius", "Scan radius", Kind.INT, "30", 4, 64, "Blocks scanned around the player");

		roll("Earthen Shield", 10, 25);
		def("Earthen Shield", "reflectMin", "Reflect min", Kind.DECIMAL, "2", 0, 40, "Least damage thrown back at the attacker");
		def("Earthen Shield", "reflectMax", "Reflect max", Kind.DECIMAL, "8", 0, 40, "Most damage thrown back at the attacker");

		roll("Gravity Flux", 4, 0);
		def("Gravity Flux", "radius", "Radius", Kind.INT, "7", 1, 24, "Blocks around the player");
		def("Gravity Flux", "launch", "Launch strength", Kind.DECIMAL, "1", 0.1, 4, "How hard monsters are thrown up");

		roll("Graceful Steps", 20, 0);
		def("Graceful Steps", "effect1", "Effect", Kind.EFFECT, "SPEED,1,15", 0, 0, "Given to the player");

		roll("Locust Swarm", 8, 0);
		def("Locust Swarm", "radius", "Radius", Kind.INT, "3", 1, 16, "Blocks around where the arrow hit");
		def("Locust Swarm", "damage", "Damage per second", Kind.DECIMAL, "2", 0, 20, "Damage each second (2 = 1 heart)");
		def("Locust Swarm", "seconds", "Duration", Kind.INT, "5", 1, 30, "Seconds the swarm lasts");

		roll("Forest Armor", 5, 0);
		def("Forest Armor", "wolves", "Wolves", Kind.INT, "2", 0, 6, "Wolves summoned");
		def("Forest Armor", "wolfSeconds", "Wolf time", Kind.INT, "20", 1, 120, "Seconds before the wolves leave");
		def("Forest Armor", "effect1", "Attacker effect", Kind.EFFECT, "POISON,2,5", 0, 0, "Given to the attacker");
		def("Forest Armor", "fullLeather", "Needs full leather armor", Kind.BOOL, "true", 0, 0, "All four leather armor pieces required");
	}

	// ------------------------------------------------------------------ storage

	static void load() {
		Map<String, String> fresh = new HashMap<>();
		MySQL db = RunicUtilities.getMysqlFromPlugin(RunicParadise.getInstance());
		try (Connection c = db.openConnection(); Statement st = c.createStatement();
			 ResultSet rs = st.executeQuery("SELECT PowerName, SettingKey, Value FROM rp_FaithPowerSettings")) {
			while (rs.next()) fresh.put(rs.getString(1) + "|" + rs.getString(2), rs.getString(3));
		} catch (SQLException e) {
			RunicParadise.getInstance().getLogger().warning("[Faith settings] load failed, using defaults: " + e.getMessage());
			return;
		}
		VALUES.clear();
		VALUES.putAll(fresh);
	}

	static Setting find(String power, String key) {
		List<Setting> l = DEFS.get(power);
		if (l != null) for (Setting s : l) if (s.key.equals(key)) return s;
		return null;
	}

	static String raw(String power, String key) {
		String v = VALUES.get(power + "|" + key);
		if (v != null) return v;
		Setting s = find(power, key);
		return s == null ? null : s.def;
	}

	static boolean isOverridden(String power, String key) {
		return VALUES.containsKey(power + "|" + key);
	}

	static boolean set(String power, String key, String value, String by) {
		MySQL db = RunicUtilities.getMysqlFromPlugin(RunicParadise.getInstance());
		try (Connection c = db.openConnection();
			 PreparedStatement ps = c.prepareStatement("INSERT INTO rp_FaithPowerSettings (PowerName, SettingKey, Value, UpdatedBy, UpdatedAt) VALUES (?,?,?,?,?)"
					 + " ON DUPLICATE KEY UPDATE Value = VALUES(Value), UpdatedBy = VALUES(UpdatedBy), UpdatedAt = VALUES(UpdatedAt)")) {
			ps.setString(1, power);
			ps.setString(2, key);
			ps.setString(3, value);
			ps.setString(4, by);
			ps.setLong(5, System.currentTimeMillis());
			ps.executeUpdate();
		} catch (SQLException e) {
			RunicParadise.getInstance().getLogger().warning("[Faith settings] save failed: " + e.getMessage());
			return false;
		}
		VALUES.put(power + "|" + key, value);
		return true;
	}

	static boolean reset(String power, String key) {
		MySQL db = RunicUtilities.getMysqlFromPlugin(RunicParadise.getInstance());
		try (Connection c = db.openConnection();
			 PreparedStatement ps = c.prepareStatement("DELETE FROM rp_FaithPowerSettings WHERE PowerName = ? AND SettingKey = ?")) {
			ps.setString(1, power);
			ps.setString(2, key);
			ps.executeUpdate();
		} catch (SQLException e) {
			return false;
		}
		VALUES.remove(power + "|" + key);
		return true;
	}

	// ------------------------------------------------------------------ typed access used by the powers

	/** Power switched on (staff can disable any power). */
	static boolean enabled(String power) {
		String v = VALUES.get(power + "|enabled");
		return v == null || Boolean.parseBoolean(v);
	}

	static boolean bool(String power, String key) {
		return Boolean.parseBoolean(raw(power, key));
	}

	static int num(String power, String key) {
		try {
			return (int) Math.round(Double.parseDouble(raw(power, key)));
		} catch (RuntimeException e) {
			return 0;
		}
	}

	static double dec(String power, String key) {
		try {
			return Double.parseDouble(raw(power, key));
		} catch (RuntimeException e) {
			return 0;
		}
	}

	static Set<String> worlds(String key) {
		Set<String> s = new HashSet<>();
		String v = raw(GLOBAL, key);
		if (v != null) for (String w : v.split(",")) if (!w.trim().isEmpty()) s.add(w.trim());
		return s;
	}

	/** Chance + cooldown check for a power; records the cast time when it fires. */
	static boolean roll(String power, Map<UUID, Long> last, Player p) {
		long cd = num(power, "cooldown") * 1000L;
		long now = System.currentTimeMillis();
		Long prev = last.get(p.getUniqueId());
		if (cd > 0 && prev != null && now - prev < cd) return false;
		if (RunicParadise.randomSeed.nextInt(100) >= num(power, "chance")) return false;
		last.put(p.getUniqueId(), now);
		return true;
	}

	// ------------------------------------------------------------------ potion effects: "TYPE,level,seconds"

	static PotionEffectType effectType(String name) {
		if (name == null) return null;
		String n = name.trim();
		PotionEffectType t = PotionEffectType.getByName(n.toUpperCase(Locale.ROOT));
		if (t == null) {
			try {
				t = PotionEffectType.getByKey(NamespacedKey.minecraft(n.toLowerCase(Locale.ROOT)));
			} catch (IllegalArgumentException ignored) {
			}
		}
		return t;
	}

	/** The configured effect, or null if it's blank/invalid. Level is 1-based; seconds -1 means infinite. */
	static PotionEffect effect(String power, String key) {
		String[] p = String.valueOf(raw(power, key)).split(",");
		if (p.length < 3) return null;
		PotionEffectType t = effectType(p[0]);
		if (t == null) return null;
		try {
			int level = Math.max(1, Integer.parseInt(p[1].trim()));
			double secs = Double.parseDouble(p[2].trim());
			int ticks = secs < 0 ? PotionEffect.INFINITE_DURATION : (int) Math.round(secs * 20);
			return new PotionEffect(t, ticks, level - 1);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/** "Strength II, 15s" style text for menus. */
	static String describeEffect(String value) {
		String[] p = String.valueOf(value).split(",");
		if (p.length < 3) return "none";
		PotionEffectType t = effectType(p[0]);
		String name = t == null ? p[0] + " (unknown!)" : pretty(t.getKey().getKey());
		String secs = p[2].trim().startsWith("-") ? "while active" : p[2].trim() + "s";
		return name + " " + roman(p[1].trim()) + ", " + secs;
	}

	static String pretty(String key) {
		StringBuilder b = new StringBuilder();
		for (String w : key.split("_")) {
			if (w.isEmpty()) continue;
			if (b.length() > 0) b.append(' ');
			b.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
		}
		return b.toString();
	}

	private static String roman(String n) {
		switch (n) {
			case "1": return "I";
			case "2": return "II";
			case "3": return "III";
			case "4": return "IV";
			case "5": return "V";
			default: return n;
		}
	}
}
