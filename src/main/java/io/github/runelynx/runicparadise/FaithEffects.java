package io.github.runelynx.runicparadise;

import org.bukkit.Color;
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

	/** Sunflare: a ring of fire expanding to the spell radius, then a flame puff on each ignited monster. */
	static void sunflare(final Player p, final double radius, final List<Entity> ignited) {
		final Location center = p.getLocation().add(0, 0.3, 0);
		final World w = center.getWorld();
		w.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.7f, 0.8f);
		w.playSound(center, Sound.ITEM_FIRECHARGE_USE, 0.6f, 1.0f);
		new BukkitRunnable() {
			double r = 1;
			@Override
			public void run() {
				ring(w, center, r, Particle.FLAME, null, 0.02);
				r += 1;
				if (r > radius) {
					for (Entity e : ignited) {
						if (e.isValid()) {
							Location at = e.getLocation().add(0, e.getHeight() * 0.6, 0);
							w.spawnParticle(Particle.FLAME, at, 12, 0.25, 0.3, 0.25, 0.03);
							w.spawnParticle(Particle.SMOKE_NORMAL, at, 6, 0.2, 0.3, 0.2, 0.02);
						}
					}
					cancel();
				}
			}
		}.runTaskTimer(RunicParadise.getInstance(), 0, 1);
	}

	// ---------------------------------------------------------------- Moon

	/** Celestial Healing: pale moonlight drifting down onto the player, ending in a few hearts. */
	static void celestialHealing(final Player p) {
		final World w = p.getWorld();
		final Location base = p.getLocation();
		for (int i = 0; i < 30; i++) {
			Location at = base.clone().add((Math.random() - 0.5) * 1.2, 1.2 + Math.random() * 2.3, (Math.random() - 0.5) * 1.2);
			w.spawnParticle(Particle.REDSTONE, at, 1, 0, 0, 0, 0, MOON_SILVER);
		}
		w.spawnParticle(Particle.END_ROD, base.clone().add(0, 2.6, 0), 8, 0.3, 0.4, 0.3, -0.02);
		w.spawnParticle(Particle.HEART, base.clone().add(0, 1.9, 0), 4, 0.4, 0.3, 0.4, 0);
		w.playSound(base, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.7f);
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

	// ---------------------------------------------------------------- helpers

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
