package io.github.runelynx.runicparadise;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

/**
 * Visual and sound effects for faith powers (2026-11-23). Effects only play when a power fires, so they are
 * cheap: short bursts, plus a light repeating aura for a few powers while their effect lasts.
 * Particle names are the 1.20.2 API names (REDSTONE = coloured dust, ENCHANTMENT_TABLE = glyphs).
 */
final class FaithEffects {

	private FaithEffects() {}

	private static final Particle.DustOptions SUN_GOLD = new Particle.DustOptions(Color.fromRGB(255, 200, 40), 1.2f);
	private static final Particle.DustOptions MOON_SILVER = new Particle.DustOptions(Color.fromRGB(190, 215, 255), 1.1f);
	private static final Particle.DustOptions MOON_LAVENDER = new Particle.DustOptions(Color.fromRGB(170, 150, 255), 1.3f);

	// ---------------------------------------------------------------- Sun

	/** Solar Power: rising gold spiral, then a faint gold shimmer at the feet while the 15 s buff lasts. */
	static void solarPower(final Player p) {
		final World w = p.getWorld();
		final Location base = p.getLocation();
		for (int i = 0; i < 24; i++) {
			double a = i * (Math.PI / 6);
			double y = i * 0.09;
			Location at = base.clone().add(Math.cos(a) * 0.8, y, Math.sin(a) * 0.8);
			w.spawnParticle(Particle.END_ROD, at, 1, 0, 0, 0, 0.01);
			w.spawnParticle(Particle.REDSTONE, at, 1, 0, 0, 0, 0, SUN_GOLD);
		}
		w.playSound(base, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.6f);
		w.playSound(base, Sound.BLOCK_BEACON_POWER_SELECT, 0.4f, 1.8f);
		aura(p, 300, 10, () -> p.getWorld().spawnParticle(Particle.REDSTONE,
				p.getLocation().add(0, 0.1, 0), 6, 0.35, 0.05, 0.35, 0, SUN_GOLD));
	}

	private static final Particle.DustOptions SUN_WHITE = new Particle.DustOptions(Color.fromRGB(255, 245, 200), 1.6f);

	/**
	 * Sunflare, a small sun going off around the player:
	 * 1. a bright flash and a burst of light at chest height,
	 * 2. twelve sunburst rays shooting out to the spell radius over half a second (gold light + flame tips),
	 * 3. a beam of sunlight from the player to each ignited monster, ending in a flame burst on it.
	 */
	static void sunflare(final Player p, final double radius, final List<Entity> ignited) {
		final Location center = p.getLocation().add(0, 1.0, 0);
		final World w = center.getWorld();
		w.spawnParticle(Particle.FLASH, center, 1);
		w.spawnParticle(Particle.END_ROD, center, 40, 0.2, 0.2, 0.2, 0.25);
		w.spawnParticle(Particle.REDSTONE, center, 30, 0.6, 0.6, 0.6, 0, SUN_WHITE);
		w.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.7f);
		w.playSound(center, Sound.ITEM_FIRECHARGE_USE, 0.7f, 0.9f);
		w.playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 0.6f, 2.0f);

		final int rays = 12;
		new BukkitRunnable() {
			double r = 0.8;
			@Override
			public void run() {
				for (int i = 0; i < rays; i++) {
					double a = 2 * Math.PI * i / rays;
					// each tick draws the next stretch of every ray, so the burst visibly travels outward
					for (double d = r; d < r + 1.6 && d <= radius; d += 0.4) {
						Location at = center.clone().add(Math.cos(a) * d, -0.4, Math.sin(a) * d);
						w.spawnParticle(Particle.REDSTONE, at, 1, 0, 0, 0, 0, SUN_GOLD);
					}
					Location tip = center.clone().add(Math.cos(a) * Math.min(r + 1.6, radius), -0.4, Math.sin(a) * Math.min(r + 1.6, radius));
					w.spawnParticle(Particle.FLAME, tip, 2, 0.05, 0.05, 0.05, 0.01);
				}
				r += 1.6;
				if (r > radius) {
					for (Entity e : ignited) {
						if (!e.isValid()) {
							continue;
						}
						Location target = e.getLocation().add(0, e.getHeight() * 0.6, 0);
						beam(w, center, target, Particle.END_ROD);
						w.spawnParticle(Particle.FLAME, target, 16, 0.25, 0.35, 0.25, 0.04);
						w.spawnParticle(Particle.LAVA, target, 3, 0.2, 0.2, 0.2, 0);
					}
					w.playSound(center, Sound.ENTITY_GENERIC_BURN, 0.6f, 1.2f);
					cancel();
				}
			}
		}.runTaskTimer(RunicParadise.getInstance(), 2, 1);
	}

	/** A straight line of particles from a to b (about 3 per block). */
	private static void beam(World w, Location a, Location b, Particle type) {
		double dist = a.distance(b);
		int steps = (int) Math.max(2, Math.min(60, dist * 3));
		for (int i = 0; i <= steps; i++) {
			double t = (double) i / steps;
			Location at = a.clone().add((b.getX() - a.getX()) * t, (b.getY() - a.getY()) * t, (b.getZ() - a.getZ()) * t);
			w.spawnParticle(type, at, 1, 0, 0, 0, 0);
		}
	}

	// ---------------------------------------------------------------- Moon

	/**
	 * Celestial Healing: a moonbeam pours down onto the player, then moonlight keeps falling around them
	 * (following as they move) for the whole 15 s Regeneration, with a heart each time Regeneration I heals.
	 */
	static void celestialHealing(final Player p) {
		final World w = p.getWorld();
		final Location base = p.getLocation();
		// opening moonbeam: a column of light from 4 blocks up down to the player
		for (double y = 4.0; y >= 0.2; y -= 0.25) {
			Location at = base.clone().add(0, y, 0);
			w.spawnParticle(Particle.END_ROD, at, 1, 0.05, 0, 0.05, 0);
			w.spawnParticle(Particle.REDSTONE, at, 2, 0.25, 0.05, 0.25, 0, MOON_SILVER);
		}
		w.spawnParticle(Particle.HEART, base.clone().add(0, 2.0, 0), 3, 0.4, 0.2, 0.4, 0);
		w.playSound(base, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, 0.7f);
		w.playSound(base, Sound.BLOCK_BEACON_POWER_SELECT, 0.35f, 1.4f);

		final int[] tick = { 0 };
		aura(p, 300, 5, () -> {
			Location at = p.getLocation();
			World pw = at.getWorld();
			// moonlight motes drifting down around the player
			for (int i = 0; i < 4; i++) {
				double a = Math.random() * 2 * Math.PI;
				double r = 0.4 + Math.random() * 0.5;
				pw.spawnParticle(Particle.REDSTONE, at.clone().add(Math.cos(a) * r, 2.2 + Math.random() * 0.6, Math.sin(a) * r),
						1, 0, 0, 0, 0, MOON_SILVER);
			}
			pw.spawnParticle(Particle.END_ROD, at.clone().add(0, 2.6, 0), 1, 0.3, 0.1, 0.3, 0.0);
			tick[0] += 5;
			if (tick[0] % 50 == 0) { // Regeneration I heals about every 2.5 s
				pw.spawnParticle(Particle.HEART, at.clone().add(0, 2.1, 0), 1, 0.2, 0.1, 0.2, 0);
			}
		});
	}

	/**
	 * Lunar Calm: a slow lavender ring rippling out to the spell radius, a soft puff over each calmed monster,
	 * and a faint orbit of enchantment glyphs around the player while the 5 s protection lasts.
	 */
	static void lunarCalm(final Player p, final double radius, final List<Entity> calmed, final int protectTicks) {
		final Location center = p.getLocation().add(0, 0.2, 0);
		final World w = center.getWorld();
		w.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 1.6f);
		w.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.7f, 0.8f);
		new BukkitRunnable() {
			double r = 1;
			@Override
			public void run() {
				ring(w, center, r, Particle.REDSTONE, MOON_LAVENDER, 0);
				r += 1.25;
				if (r > radius) {
					cancel();
				}
			}
		}.runTaskTimer(RunicParadise.getInstance(), 0, 1);
		for (Entity e : calmed) {
			Location at = e.getLocation().add(0, e.getHeight() + 0.3, 0);
			w.spawnParticle(Particle.CLOUD, at, 4, 0.15, 0.05, 0.15, 0.005);
			w.spawnParticle(Particle.REDSTONE, at, 5, 0.2, 0.1, 0.2, 0, MOON_SILVER);
		}
		final double[] angle = { 0 };
		aura(p, protectTicks, 3, () -> {
			Location at = p.getLocation().add(Math.cos(angle[0]) * 0.9, 1.0, Math.sin(angle[0]) * 0.9);
			p.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, at, 3, 0.05, 0.1, 0.05, 0.2);
			angle[0] += Math.PI / 5;
		});
	}

	// ---------------------------------------------------------------- shared colours

	private static final Particle.DustOptions EMBER = new Particle.DustOptions(Color.fromRGB(255, 110, 20), 1.3f);
	private static final Particle.DustOptions FROST = new Particle.DustOptions(Color.fromRGB(205, 240, 255), 1.3f);
	private static final Particle.DustOptions WATER_BLUE = new Particle.DustOptions(Color.fromRGB(60, 140, 255), 1.2f);
	private static final Particle.DustOptions BLOOD = new Particle.DustOptions(Color.fromRGB(150, 0, 15), 1.3f);
	private static final Particle.DustOptions LEAF = new Particle.DustOptions(Color.fromRGB(70, 160, 40), 1.3f);
	private static final Particle.DustOptions POISON = new Particle.DustOptions(Color.fromRGB(120, 210, 40), 1.2f);
	private static final Particle.DustOptions LOCUST = new Particle.DustOptions(Color.fromRGB(70, 60, 20), 0.7f);
	private static final Particle.DustOptions DIAMOND = new Particle.DustOptions(Color.fromRGB(90, 235, 235), 1.5f);
	private static final Particle.DustOptions EMERALD = new Particle.DustOptions(Color.fromRGB(40, 220, 90), 1.5f);

	// ---------------------------------------------------------------- Fire

	/** Volcanic Fury: an eruption of lava and flame, then embers swirling around the player while Strength lasts. */
	static void volcanicFury(final Player p, int ticks) {
		World w = p.getWorld();
		Location body = p.getLocation().add(0, 1, 0);
		w.spawnParticle(Particle.LAVA, body, 14, 0.4, 0.3, 0.4, 0);
		w.spawnParticle(Particle.FLAME, body, 35, 0.2, 0.4, 0.2, 0.09);
		w.spawnParticle(Particle.SMOKE_LARGE, body, 8, 0.3, 0.3, 0.3, 0.02);
		w.playSound(body, Sound.ITEM_FIRECHARGE_USE, 0.8f, 0.6f);
		w.playSound(body, Sound.ENTITY_BLAZE_AMBIENT, 0.7f, 0.8f);
		final double[] angle = { 0 };
		aura(p, ticks, 3, () -> {
			Location at = p.getLocation();
			for (int arm = 0; arm < 2; arm++) {
				double a = angle[0] + arm * Math.PI;
				Location spot = at.clone().add(Math.cos(a) * 0.7, 0.9 + Math.sin(angle[0] * 2) * 0.3, Math.sin(a) * 0.7);
				p.getWorld().spawnParticle(Particle.REDSTONE, spot, 2, 0.05, 0.05, 0.05, 0, EMBER);
				p.getWorld().spawnParticle(Particle.FLAME, spot, 1, 0, 0, 0, 0.005);
			}
			angle[0] += Math.PI / 6;
		});
	}

	/** Unstable Embers: a fiery explosion with a double shockwave ring and flying lava. */
	static void unstableEmbers(Player p, double radius) {
		Location c = p.getLocation().add(0, 0.8, 0);
		World w = c.getWorld();
		w.spawnParticle(Particle.EXPLOSION_LARGE, c, 2, 0.3, 0.2, 0.3, 0);
		w.spawnParticle(Particle.FLAME, c, 70, 0.3, 0.3, 0.3, 0.25);
		w.spawnParticle(Particle.LAVA, c, 18, 0.5, 0.3, 0.5, 0);
		w.spawnParticle(Particle.SMOKE_LARGE, c, 20, 0.6, 0.4, 0.6, 0.05);
		ring(w, c.clone().add(0, -0.6, 0), radius, Particle.FLAME, null, 0.02);
		ring(w, c.clone().add(0, -0.6, 0), radius / 2, Particle.REDSTONE, EMBER, 0);
		w.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.2f);
		w.playSound(c, Sound.ENTITY_BLAZE_SHOOT, 0.7f, 0.6f);
	}

	// ---------------------------------------------------------------- Water

	/** Protective Bubble: a shimmering water sphere forms around the player and stays while the absorption lasts. */
	static void protectiveBubble(final Player p, int ticks) {
		World w = p.getWorld();
		Location c = p.getLocation().add(0, 1, 0);
		sphere(w, c, 1.3, 70, Particle.REDSTONE, WATER_BLUE);
		w.spawnParticle(Particle.WATER_SPLASH, c, 40, 0.6, 0.6, 0.6, 0.1);
		w.playSound(c, Sound.ENTITY_PLAYER_SPLASH, 0.6f, 1.4f);
		w.playSound(c, Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_INSIDE, 0.9f, 1.2f);
		auraWhile(p, ticks, 6, () -> p.getAbsorptionAmount() > 0, () -> {
			Location at = p.getLocation().add(0, 1, 0);
			sphere(p.getWorld(), at, 1.2, 10, Particle.BUBBLE_POP, null);
			p.getWorld().spawnParticle(Particle.DRIP_WATER, at.clone().add(0, 1.1, 0), 1, 0.4, 0, 0.4, 0);
		});
	}

	/** Arctic Frost: a frost nova expands outward; slowed monsters keep shedding snowflakes while slowed. */
	static void arcticFrost(final Player p, final double radius, List<Entity> slowed, int ticks) {
		final Location c = p.getLocation().add(0, 0.2, 0);
		final World w = c.getWorld();
		w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 1, 0), 50, 0.3, 0.4, 0.3, 0.18);
		w.spawnParticle(Particle.SNOWBALL, c.clone().add(0, 1, 0), 20, 0.4, 0.4, 0.4, 0.1);
		w.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.7f, 1.6f);
		w.playSound(c, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.9f, 1.0f);
		w.playSound(c, Sound.BLOCK_POWDER_SNOW_STEP, 1.0f, 0.8f);
		new BukkitRunnable() {
			double r = 1;
			@Override
			public void run() {
				ring(w, c, r, Particle.REDSTONE, FROST, 0);
				ring(w, c.clone().add(0, 0.3, 0), r, Particle.SNOWFLAKE, null, 0.0);
				r += 1;
				if (r > radius) {
					cancel();
				}
			}
		}.runTaskTimer(RunicParadise.getInstance(), 0, 1);
		for (Entity e : slowed) {
			follow(e, ticks, 10, at -> {
				at.getWorld().spawnParticle(Particle.SNOWFLAKE, at.clone().add(0, e.getHeight() * 0.7, 0), 4, 0.25, 0.3, 0.25, 0.01);
				at.getWorld().spawnParticle(Particle.REDSTONE, at.clone().add(0, 0.1, 0), 3, 0.3, 0.0, 0.3, 0, FROST);
			});
		}
	}

	/** Deep Wader: a spiral of rising bubbles and a conduit-like glow wraps the player. */
	static void deepWader(Player p) {
		World w = p.getWorld();
		Location base = p.getLocation();
		for (int i = 0; i < 36; i++) {
			double a = i * (Math.PI / 6);
			Location at = base.clone().add(Math.cos(a) * 0.9, i * 0.06, Math.sin(a) * 0.9);
			w.spawnParticle(Particle.BUBBLE_COLUMN_UP, at, 2, 0.05, 0.05, 0.05, 0.05);
			w.spawnParticle(Particle.REDSTONE, at, 1, 0, 0, 0, 0, WATER_BLUE);
		}
		w.spawnParticle(Particle.NAUTILUS, base.clone().add(0, 1.2, 0), 40, 1.2, 0.8, 1.2, 0.6);
		w.playSound(base, Sound.BLOCK_CONDUIT_ACTIVATE, 0.7f, 1.3f);
		w.playSound(base, Sound.ITEM_BUCKET_FILL, 0.8f, 1.0f);
	}

	// ---------------------------------------------------------------- Air

	/** Healing Breeze: a gust spirals around the caster; everyone healed gets a puff of wind and a heart. */
	static void healingBreeze(Player caster, List<LivingEntity> healed) {
		World w = caster.getWorld();
		Location base = caster.getLocation();
		for (int i = 0; i < 30; i++) {
			double a = i * (Math.PI / 7);
			Location at = base.clone().add(Math.cos(a) * 1.1, i * 0.08, Math.sin(a) * 1.1);
			w.spawnParticle(Particle.CLOUD, at, 1, 0, 0, 0, 0.01);
		}
		w.playSound(base, Sound.ITEM_TRIDENT_RIPTIDE_1, 0.6f, 1.5f);
		w.playSound(base, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.8f);
		for (LivingEntity e : healed) {
			Location head = e.getLocation().add(0, e.getHeight() + 0.2, 0);
			e.getWorld().spawnParticle(Particle.CLOUD, head, 6, 0.3, 0.1, 0.3, 0.02);
			e.getWorld().spawnParticle(Particle.HEART, head, 2, 0.3, 0.1, 0.3, 0);
		}
	}

	/** Tempest Armor: a storm burst of wind and electricity; flung monsters crackle until the lightning lands. */
	static void tempestArmor(Player p, double radius, List<Entity> flung) {
		World w = p.getWorld();
		Location c = p.getLocation().add(0, 1, 0);
		w.spawnParticle(Particle.CLOUD, c, 40, 0.3, 0.3, 0.3, 0.25);
		w.spawnParticle(Particle.ELECTRIC_SPARK, c, 50, 0.6, 0.8, 0.6, 0.3);
		ring(w, c.clone().add(0, -0.8, 0), radius, Particle.CLOUD, null, 0.03);
		w.playSound(c, Sound.ITEM_TRIDENT_RIPTIDE_3, 0.7f, 1.0f);
		for (Entity e : flung) {
			follow(e, 20, 2, at -> at.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
					at.clone().add(0, e.getHeight() * 0.6, 0), 5, 0.25, 0.3, 0.25, 0.05));
		}
	}

	// ---------------------------------------------------------------- Aether

	/** Gravity Flux: a reverse-portal burst; launched monsters leave a glowing trail as they rise. */
	static void gravityFlux(Player p, List<Entity> launched) {
		World w = p.getWorld();
		Location c = p.getLocation().add(0, 1, 0);
		w.spawnParticle(Particle.REVERSE_PORTAL, c, 60, 0.5, 0.5, 0.5, 0.3);
		w.spawnParticle(Particle.END_ROD, c, 15, 0.3, 0.2, 0.3, 0.15);
		w.playSound(c, Sound.ENTITY_SHULKER_SHOOT, 0.8f, 1.4f);
		w.playSound(c, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.6f, 1.6f);
		for (Entity e : launched) {
			e.getWorld().spawnParticle(Particle.END_ROD, e.getLocation(), 10, 0.3, 0.1, 0.3, 0.1);
			follow(e, 24, 2, at -> {
				at.getWorld().spawnParticle(Particle.REVERSE_PORTAL, at, 6, 0.2, 0.1, 0.2, 0.02);
				at.getWorld().spawnParticle(Particle.END_ROD, at, 1, 0.1, 0, 0.1, 0.0);
			});
		}
	}

	/** Graceful Steps: a soft landing puff, then a light trail of cloud and ash behind the player while Speed lasts. */
	static void gracefulSteps(final Player p, int ticks) {
		World w = p.getWorld();
		Location feet = p.getLocation().add(0, 0.1, 0);
		ring(w, feet, 1.0, Particle.CLOUD, null, 0.04);
		w.spawnParticle(Particle.WHITE_ASH, feet.clone().add(0, 1, 0), 40, 0.6, 0.6, 0.6, 0);
		w.spawnParticle(Particle.END_ROD, feet, 8, 0.4, 0.1, 0.4, 0.05);
		w.playSound(feet, Sound.ENTITY_PARROT_FLY, 0.9f, 1.2f);
		w.playSound(feet, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 2.0f);
		final Location[] last = { p.getLocation() };
		aura(p, ticks, 3, () -> {
			Location now = p.getLocation();
			if (now.getWorld() == last[0].getWorld() && now.distanceSquared(last[0]) > 0.04) {
				p.getWorld().spawnParticle(Particle.CLOUD, now.clone().add(0, 0.1, 0), 1, 0.1, 0, 0.1, 0.005);
				p.getWorld().spawnParticle(Particle.WHITE_ASH, now.clone().add(0, 0.3, 0), 3, 0.2, 0.1, 0.2, 0);
			}
			last[0] = now;
		});
	}

	// ---------------------------------------------------------------- Nether

	/** Vampirism: a stream of blood is drawn from the fallen monster into the player, with a heartbeat. */
	static void vampirism(final Player p, final Location from) {
		final World w = p.getWorld();
		final Location start = from.clone().add(0, 0.8, 0);
		w.spawnParticle(Particle.DAMAGE_INDICATOR, start, 6, 0.3, 0.3, 0.3, 0.1);
		w.playSound(p.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 1.2f);
		w.playSound(p.getLocation(), Sound.ENTITY_GENERIC_DRINK, 0.6f, 0.8f);
		new BukkitRunnable() {
			int step = 0;
			@Override
			public void run() {
				if (!p.isOnline() || p.getWorld() != w || step > 8) {
					if (p.isOnline()) {
						w.spawnParticle(Particle.CRIMSON_SPORE, p.getLocation().add(0, 1, 0), 25, 0.4, 0.5, 0.4, 0);
						w.spawnParticle(Particle.HEART, p.getLocation().add(0, 2, 0), 1, 0.1, 0.1, 0.1, 0);
					}
					cancel();
					return;
				}
				Location end = p.getLocation().add(0, 1.1, 0);
				double t = step / 8.0;
				Location at = start.clone().add((end.getX() - start.getX()) * t, (end.getY() - start.getY()) * t,
						(end.getZ() - start.getZ()) * t);
				w.spawnParticle(Particle.REDSTONE, at, 6, 0.08, 0.08, 0.08, 0, BLOOD);
				step++;
			}
		}.runTaskTimer(RunicParadise.getInstance(), 0, 1);
	}

	/** Netherborn: soul fire flares up around the player and crimson spores drift as the nether claims them. */
	static void netherborn(Player p) {
		World w = p.getWorld();
		Location feet = p.getLocation().add(0, 0.1, 0);
		ring(w, feet, 1.2, Particle.SOUL_FIRE_FLAME, null, 0.02);
		for (double y = 0; y < 2.2; y += 0.2) {
			w.spawnParticle(Particle.FLAME, feet.clone().add(0, y, 0), 2, 0.25, 0, 0.25, 0.01);
		}
		w.spawnParticle(Particle.CRIMSON_SPORE, feet.clone().add(0, 1, 0), 70, 1.0, 0.8, 1.0, 0);
		w.spawnParticle(Particle.LAVA, feet, 8, 0.5, 0.1, 0.5, 0);
		w.playSound(feet, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.8f, 1.0f);
		w.playSound(feet, Sound.ENTITY_BLAZE_AMBIENT, 0.7f, 0.7f);
	}

	// ---------------------------------------------------------------- Nature

	/** Locust Swarm, each second: a buzzing cloud of insects and spores over the target area. */
	static void locustPulse(Location where) {
		World w = where.getWorld();
		Location c = where.clone().add(0, 1, 0);
		for (int i = 0; i < 35; i++) {
			Location at = c.clone().add((Math.random() - 0.5) * 5, (Math.random() - 0.3) * 2, (Math.random() - 0.5) * 5);
			w.spawnParticle(Particle.REDSTONE, at, 1, 0, 0, 0, 0, LOCUST);
		}
		w.spawnParticle(Particle.SPORE_BLOSSOM_AIR, c, 30, 2, 1, 2, 0);
		w.spawnParticle(Particle.ASH, c, 25, 2, 1, 2, 0);
		w.playSound(c, Sound.ENTITY_BEE_LOOP_AGGRESSIVE, 0.6f, 1.6f);
	}

	/** Forest Armor: leaves burst where the wolves appear, a howl, and sickly green on the poisoned attacker. */
	static void forestArmor(Player p, List<? extends Entity> wolves, LivingEntity poisoned) {
		World w = p.getWorld();
		for (Entity wolf : wolves) {
			Location at = wolf.getLocation().add(0, 0.5, 0);
			w.spawnParticle(Particle.VILLAGER_HAPPY, at, 15, 0.5, 0.4, 0.5, 0);
			w.spawnParticle(Particle.REDSTONE, at, 25, 0.6, 0.5, 0.6, 0, LEAF);
			w.spawnParticle(Particle.COMPOSTER, at, 15, 0.5, 0.3, 0.5, 0);
		}
		w.playSound(p.getLocation(), Sound.ENTITY_WOLF_HOWL, 0.8f, 1.0f);
		w.playSound(p.getLocation(), Sound.BLOCK_AZALEA_LEAVES_BREAK, 0.9f, 1.0f);
		if (poisoned != null) {
			follow(poisoned, 100, 10, at -> {
				at.getWorld().spawnParticle(Particle.REDSTONE, at.clone().add(0, poisoned.getHeight() * 0.6, 0), 6, 0.3, 0.3, 0.3, 0, POISON);
				at.getWorld().spawnParticle(Particle.SNEEZE, at.clone().add(0, poisoned.getHeight(), 0), 2, 0.2, 0.1, 0.2, 0.01);
			});
		}
	}

	/** Forest Armor wolves returning to the forest. */
	static void wolfDepart(Location at) {
		at.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, at.clone().add(0, 0.5, 0), 8, 0.4, 0.3, 0.4, 0);
		at.getWorld().spawnParticle(Particle.REDSTONE, at.clone().add(0, 0.5, 0), 15, 0.4, 0.3, 0.4, 0, LEAF);
	}

	// ---------------------------------------------------------------- Earth

	/** Earth's Bounty: a seismic pulse ripples out through the ground. */
	static void earthsBountyPulse(Player p, BlockData ground) {
		final Location c = p.getLocation().add(0, 0.1, 0);
		final World w = c.getWorld();
		final BlockData data = ground.getMaterial().isSolid() ? ground : Material.STONE.createBlockData();
		w.playSound(c, Sound.BLOCK_DEEPSLATE_BREAK, 0.9f, 0.6f);
		w.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.7f, 0.6f);
		new BukkitRunnable() {
			double r = 0.8;
			@Override
			public void run() {
				ring(w, c, r, Particle.BLOCK_CRACK, null, 0, data);
				r += 0.7;
				if (r > 7) {
					cancel();
				}
			}
		}.runTaskTimer(RunicParadise.getInstance(), 0, 1);
	}

	/** Earth's Bounty result: diamond- and emerald-coloured motes rise from the ground (one per ore, up to 15). */
	static void earthsBountyResult(Player p, int diamonds, int emeralds) {
		World w = p.getWorld();
		Location c = p.getLocation();
		for (int i = 0; i < Math.min(15, diamonds); i++) {
			w.spawnParticle(Particle.REDSTONE, c.clone().add((Math.random() - 0.5) * 2.4, Math.random() * 2, (Math.random() - 0.5) * 2.4), 2, 0, 0.1, 0, 0, DIAMOND);
		}
		for (int i = 0; i < Math.min(15, emeralds); i++) {
			w.spawnParticle(Particle.REDSTONE, c.clone().add((Math.random() - 0.5) * 2.4, Math.random() * 2, (Math.random() - 0.5) * 2.4), 2, 0, 0.1, 0, 0, EMERALD);
		}
		if (diamonds + emeralds > 0) {
			w.spawnParticle(Particle.WAX_ON, c.clone().add(0, 1, 0), 20, 0.8, 0.6, 0.8, 0.1);
			w.playSound(c, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.8f, 1.4f);
		} else {
			w.playSound(c, Sound.BLOCK_GRAVEL_BREAK, 0.6f, 0.8f);
		}
	}

	/**
	 * Earthen Shield: stone erupts from the ground in a ring around the player, blocking the hit, and shards
	 * fly back at the attacker.
	 */
	static void earthenShield(Player p, BlockData ground, LivingEntity attacker) {
		World w = p.getWorld();
		Location feet = p.getLocation().add(0, 0.1, 0);
		BlockData data = ground.getMaterial().isSolid() ? ground : Material.STONE.createBlockData();
		for (double y = 0; y <= 1.8; y += 0.45) {
			ring(w, feet.clone().add(0, y, 0), 1.1, Particle.BLOCK_CRACK, null, 0, data);
		}
		w.spawnParticle(Particle.BLOCK_DUST, feet, 50, 0.6, 0.2, 0.6, 0.25, data);
		w.spawnParticle(Particle.FALLING_DUST, feet.clone().add(0, 2.2, 0), 20, 0.7, 0.2, 0.7, 0, data);
		w.playSound(feet, Sound.ITEM_SHIELD_BLOCK, 1.0f, 0.7f);
		w.playSound(feet, Sound.BLOCK_STONE_BREAK, 1.0f, 0.6f);
		w.playSound(feet, Sound.BLOCK_POINTED_DRIPSTONE_LAND, 0.8f, 0.8f);
		if (attacker != null) {
			Location from = p.getLocation().add(0, 1.2, 0);
			Location to = attacker.getLocation().add(0, attacker.getHeight() * 0.6, 0);
			double dist = from.distance(to);
			int steps = (int) Math.max(2, Math.min(40, dist * 4));
			for (int i = 0; i <= steps; i++) {
				double t = (double) i / steps;
				Location at = from.clone().add((to.getX() - from.getX()) * t, (to.getY() - from.getY()) * t, (to.getZ() - from.getZ()) * t);
				w.spawnParticle(Particle.BLOCK_CRACK, at, 2, 0.05, 0.05, 0.05, 0, data);
			}
			w.spawnParticle(Particle.CRIT, to, 15, 0.3, 0.3, 0.3, 0.2);
		}
	}

	// ---------------------------------------------------------------- Fate / Time

	/** Inevitable Demise: souls rise from each reaped monster and a death knell rings. */
	static void inevitableDemise(Player p, List<Location> reaped) {
		World w = p.getWorld();
		for (Location l : reaped) {
			Location at = l.clone().add(0, 0.8, 0);
			w.spawnParticle(Particle.SOUL, at, 12, 0.3, 0.4, 0.3, 0.04);
			w.spawnParticle(Particle.SCULK_SOUL, at, 6, 0.3, 0.4, 0.3, 0.03);
			w.spawnParticle(Particle.SMOKE_LARGE, at, 6, 0.3, 0.3, 0.3, 0.01);
		}
		w.spawnParticle(Particle.SPELL_WITCH, p.getLocation().add(0, 1, 0), 30, 0.5, 0.6, 0.5, 0.1);
		w.playSound(p.getLocation(), Sound.BLOCK_BELL_USE, 0.9f, 0.5f);
		w.playSound(p.getLocation(), Sound.ENTITY_VEX_CHARGE, 0.6f, 0.6f);
	}

	/** Rewind: a totem burst, then a clock hand sweeping backwards around the player for a second. */
	static void rewind(final Player p) {
		World w = p.getWorld();
		Location c = p.getLocation().add(0, 1, 0);
		w.spawnParticle(Particle.TOTEM, c, 60, 0.4, 0.6, 0.4, 0.5);
		w.spawnParticle(Particle.REVERSE_PORTAL, c, 60, 1.2, 1.0, 1.2, 0.1);
		w.playSound(c, Sound.ITEM_TOTEM_USE, 0.6f, 1.3f);
		w.playSound(c, Sound.BLOCK_BEACON_DEACTIVATE, 0.7f, 1.5f);
		final double[] angle = { Math.PI / 2 };
		aura(p, 24, 1, () -> {
			Location at = p.getLocation().add(0, 1.0, 0);
			ring(p.getWorld(), at, 1.3, Particle.END_ROD, null, 0, null, 12);
			for (double d = 0.2; d <= 1.2; d += 0.25) {
				p.getWorld().spawnParticle(Particle.END_ROD, at.clone().add(Math.cos(angle[0]) * d, 0, Math.sin(angle[0]) * d), 1, 0, 0, 0, 0);
			}
			angle[0] -= Math.PI / 6; // counter-clockwise: time running backwards
		});
	}

	// ---------------------------------------------------------------- helpers

	/** Points spread evenly over a sphere (golden-spiral), for bubbles and shields. */
	private static void sphere(World w, Location c, double r, int points, Particle type, Particle.DustOptions dust) {
		double golden = Math.PI * (3 - Math.sqrt(5));
		for (int i = 0; i < points; i++) {
			double y = 1 - (i / (double) (points - 1)) * 2;
			double rad = Math.sqrt(1 - y * y);
			double t = golden * i;
			Location at = c.clone().add(Math.cos(t) * rad * r, y * r, Math.sin(t) * rad * r);
			if (dust != null) {
				w.spawnParticle(type, at, 1, 0, 0, 0, 0, dust);
			} else {
				w.spawnParticle(type, at, 1, 0, 0, 0, 0);
			}
		}
	}

	/** Particle ring with block data (cracks/dust) and an optional fixed point count. */
	private static void ring(World w, Location c, double r, Particle type, Particle.DustOptions dust, double speed,
			BlockData data) {
		ring(w, c, r, type, dust, speed, data, -1);
	}

	private static void ring(World w, Location c, double r, Particle type, Particle.DustOptions dust, double speed,
			BlockData data, int fixedPoints) {
		int points = fixedPoints > 0 ? fixedPoints : (int) Math.min(90, Math.max(12, Math.round(2 * Math.PI * r * 3)));
		for (int i = 0; i < points; i++) {
			double a = 2 * Math.PI * i / points;
			Location at = c.clone().add(Math.cos(a) * r, 0, Math.sin(a) * r);
			if (data != null) {
				w.spawnParticle(type, at, 2, 0.05, 0.05, 0.05, 0, data);
			} else if (dust != null) {
				w.spawnParticle(type, at, 1, 0, 0, 0, 0, dust);
			} else {
				w.spawnParticle(type, at, 1, 0, 0.02, 0, speed);
			}
		}
	}

	/** Run an effect at an entity's location every intervalTicks for durationTicks while it is alive. */
	private static void follow(final Entity e, final int durationTicks, final int intervalTicks,
			final java.util.function.Consumer<Location> effect) {
		new BukkitRunnable() {
			int elapsed = 0;
			@Override
			public void run() {
				if (!e.isValid() || elapsed >= durationTicks) {
					cancel();
					return;
				}
				effect.accept(e.getLocation());
				elapsed += intervalTicks;
			}
		}.runTaskTimer(RunicParadise.getInstance(), 0, intervalTicks);
	}

	/** Like aura(), but also stops as soon as keepGoing returns false. */
	private static void auraWhile(final Player p, final int durationTicks, final int intervalTicks,
			final java.util.function.BooleanSupplier keepGoing, final Runnable effect) {
		new BukkitRunnable() {
			int elapsed = 0;
			@Override
			public void run() {
				if (!p.isOnline() || p.isDead() || elapsed >= durationTicks || !keepGoing.getAsBoolean()) {
					cancel();
					return;
				}
				effect.run();
				elapsed += intervalTicks;
			}
		}.runTaskTimer(RunicParadise.getInstance(), intervalTicks, intervalTicks);
	}

	/** Horizontal ring of particles; about 3 points per block of circumference, capped. */
	private static void ring(World w, Location c, double r, Particle type, Particle.DustOptions dust, double speed) {
		int points = (int) Math.min(90, Math.max(12, Math.round(2 * Math.PI * r * 3)));
		for (int i = 0; i < points; i++) {
			double a = 2 * Math.PI * i / points;
			Location at = c.clone().add(Math.cos(a) * r, 0, Math.sin(a) * r);
			if (dust != null) {
				w.spawnParticle(type, at, 1, 0, 0, 0, 0, dust);
			} else {
				w.spawnParticle(type, at, 1, 0, 0.02, 0, speed);
			}
		}
	}

	/** Run an effect every intervalTicks for durationTicks while the player stays online. */
	private static void aura(final Player p, final int durationTicks, final int intervalTicks, final Runnable effect) {
		new BukkitRunnable() {
			int elapsed = 0;
			@Override
			public void run() {
				if (!p.isOnline() || p.isDead() || elapsed >= durationTicks) {
					cancel();
					return;
				}
				effect.run();
				elapsed += intervalTicks;
			}
		}.runTaskTimer(RunicParadise.getInstance(), intervalTicks, intervalTicks);
	}
}
