package io.github.runelynx.runicparadise;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Random;

/**
 * Staff-only showcase effects (2026-11-23). Not gameplay: purely visual toys for Rune / staff.
 * /tornado [distance] [version] : a ~3-block-tall, fast-spinning tornado on the ground in front of you.
 */
final class StaffFX {

	private StaffFX() {}

	static final int TORNADO_TICKS = 200; // 10 seconds
	static final double TORNADO_HEIGHT = 3.2;
	private static final Random RNG = new Random();

	static void tornadoCommand(Player p, String[] args) {
		if (!p.hasPermission("rp.fx")) {
			p.sendMessage(ChatColor.RED + "That's a staff effect.");
			return;
		}
		double distance = 6;
		int version = 1;
		try {
			if (args.length >= 1) {
				distance = Double.parseDouble(args[0]);
			}
			if (args.length >= 2) {
				version = Integer.parseInt(args[1]);
			}
		} catch (NumberFormatException e) {
			p.sendMessage(ChatColor.RED + "Usage: /tornado <distance 1-50> <version 1-3>");
			return;
		}
		distance = Math.max(1, Math.min(50, distance));
		if (version < 1 || version > 3) {
			p.sendMessage(ChatColor.RED + "Versions: 1 Dust Devil, 2 Storm Cell, 3 Aurora Spirit");
			return;
		}
		Vector dir = p.getLocation().getDirection().setY(0);
		if (dir.lengthSquared() < 1e-6) {
			dir = new Vector(0, 0, 1);
		}
		Location target = p.getLocation().add(dir.normalize().multiply(distance));
		Location base = groundAt(target);
		tornado(base, version);
		String[] names = { "", "Dust Devil", "Storm Cell", "Aurora Spirit" };
		RunicMessagingShim.actionBar(p, ChatColor.AQUA + "🌪 " + ChatColor.WHITE + names[version]);
	}

	/** First solid block at or below the target (searching from 4 above to 12 below); stands the effect on it. */
	private static Location groundAt(Location target) {
		World w = target.getWorld();
		int x = target.getBlockX(), z = target.getBlockZ();
		for (int y = target.getBlockY() + 4; y >= target.getBlockY() - 12 && y > w.getMinHeight(); y--) {
			Block b = w.getBlockAt(x, y, z);
			if (b.getType().isSolid()) {
				return new Location(w, x + 0.5, y + 1.0, z + 0.5);
			}
		}
		return new Location(w, x + 0.5, target.getY(), z + 0.5);
	}

	static void tornado(final Location origin, final int version) {
		final World w = origin.getWorld();
		Block under = origin.clone().subtract(0, 0.5, 0).getBlock();
		final BlockData ground = under.getType().isSolid() ? under.getBlockData() : Material.DIRT.createBlockData();

		switch (version) {
			case 1:
				w.playSound(origin, Sound.ITEM_ELYTRA_FLYING, 0.9f, 1.3f);
				break;
			case 2:
				w.playSound(origin, Sound.ITEM_ELYTRA_FLYING, 1.0f, 0.7f);
				w.playSound(origin, Sound.WEATHER_RAIN_ABOVE, 1.0f, 0.8f);
				break;
			default:
				w.playSound(origin, Sound.BLOCK_BEACON_AMBIENT, 1.0f, 1.6f);
				w.playSound(origin, Sound.ITEM_ELYTRA_FLYING, 0.6f, 1.8f);
				break;
		}

		new BukkitRunnable() {
			int t = 0;

			@Override
			public void run() {
				if (t >= TORNADO_TICKS) {
					cancel();
					return;
				}
				// grow in over 0.5 s, die away over the last 0.75 s
				double scale = Math.min(1.0, t / 10.0) * Math.min(1.0, (TORNADO_TICKS - t) / 15.0);
				// slow wandering drift around the spawn point
				Location base = origin.clone().add(Math.sin(t * 0.05) * 1.2, 0, Math.cos(t * 0.037) * 1.2);
				switch (version) {
					case 1:
						dustDevil(w, base, t, scale, ground);
						break;
					case 2:
						stormCell(w, base, t, scale, ground);
						break;
					default:
						auroraSpirit(w, base, t, scale);
						break;
				}
				t++;
			}
		}.runTaskTimer(RunicParadise.getInstance(), 0, 1);
	}

	// ------------------------------------------------------------------ 1: Dust Devil

	private static void dustDevil(World w, Location b, int t, double scale, BlockData ground) {
		double H = TORNADO_HEIGHT * scale;
		int strands = 4;
		for (double h = 0; h <= H; h += 0.11) {
			double f = h / TORNADO_HEIGHT;
			double r = (0.22 + Math.pow(f, 1.5) * 1.45) * scale;
			// sand at the bottom fading to pale grey dust at the top
			Color c = lerp(Color.fromRGB(196, 160, 110), Color.fromRGB(205, 205, 200), f);
			Particle.DustOptions dust = new Particle.DustOptions(c, (float) (1.0 + f * 0.8));
			for (int s = 0; s < strands; s++) {
				double a = t * 0.95 + h * 2.3 + s * (2 * Math.PI / strands);
				Location at = b.clone().add(Math.cos(a) * r, h, Math.sin(a) * r);
				w.spawnParticle(Particle.REDSTONE, at, 1, 0, 0, 0, 0, dust);
				if (((int) (h * 10) + s + t) % 3 == 0) {
					// count 0 = directional: a cloud puff thrown along the spin (tangent)
					w.spawnParticle(Particle.CLOUD, at, 0, -Math.sin(a), 0.15, Math.cos(a), 0.18);
				}
			}
		}
		// debris kicked up from the real ground block, swirling at the base
		for (int i = 0; i < 10; i++) {
			double a = RNG.nextDouble() * 2 * Math.PI;
			double r = (0.5 + RNG.nextDouble() * 1.3) * scale;
			Location at = b.clone().add(Math.cos(a) * r, 0.1 + RNG.nextDouble() * 0.4, Math.sin(a) * r);
			w.spawnParticle(Particle.BLOCK_CRACK, at, 1, 0.05, 0.05, 0.05, 0, ground);
		}
		w.spawnParticle(Particle.BLOCK_DUST, b.clone().add(0, 0.2, 0), 6, 0.6 * scale, 0.1, 0.6 * scale, 0.15, ground);
		if (t % 4 == 0) {
			w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, b.clone().add(0, 0.2, 0), 1, 0.3, 0, 0.3, 0.02);
		}
		// top flare: puffs flung outward from the lip of the funnel
		for (int i = 0; i < 3; i++) {
			double a = t * 0.95 + i * 2.1;
			Location at = b.clone().add(Math.cos(a) * 1.6 * scale, H, Math.sin(a) * 1.6 * scale);
			w.spawnParticle(Particle.CLOUD, at, 0, Math.cos(a), 0.05, Math.sin(a), 0.12);
		}
		if (t % 20 == 0) {
			w.playSound(b, Sound.ITEM_TRIDENT_RIPTIDE_2, 0.5f, 1.4f);
		}
	}

	// ------------------------------------------------------------------ 2: Storm Cell

	private static void stormCell(World w, Location b, int t, double scale, BlockData ground) {
		double H = TORNADO_HEIGHT * scale;
		for (double h = 0; h <= H; h += 0.1) {
			double f = h / TORNADO_HEIGHT;
			// rope-thin at the ground, flaring into the cloud base
			double r = (0.15 + Math.pow(f, 2.2) * 1.7) * scale;
			Particle.DustTransition dt = new Particle.DustTransition(
					lerp(Color.fromRGB(35, 40, 55), Color.fromRGB(70, 80, 100), f),
					lerp(Color.fromRGB(95, 110, 140), Color.fromRGB(140, 155, 185), f), (float) (1.3 + f));
			for (int s = 0; s < 3; s++) {
				double a = t * 1.35 + h * 2.9 + s * (2 * Math.PI / 3);
				Location at = b.clone().add(Math.cos(a) * r, h, Math.sin(a) * r);
				w.spawnParticle(Particle.DUST_COLOR_TRANSITION, at, 1, 0, 0, 0, 0, dt);
				if ((s + t) % 2 == 0 && f > 0.3) {
					w.spawnParticle(Particle.SMOKE_NORMAL, at, 0, -Math.sin(a), 0.1, Math.cos(a), 0.12);
				}
			}
		}
		// smoky core rising up the middle
		w.spawnParticle(Particle.SMOKE_LARGE, b.clone().add(0, 0.3, 0), 0, 0, 1, 0, 0.12 * scale);
		w.spawnParticle(Particle.SMOKE_LARGE, b.clone().add(0, 1.4 * scale, 0), 0, 0, 1, 0, 0.1 * scale);
		// wall cloud: a broad, slowly turning shelf at the top
		double top = H + 0.25;
		for (int i = 0; i < 26; i++) {
			double a = i * (2 * Math.PI / 26) + t * 0.08;
			double r = (1.8 + 0.35 * Math.sin(i * 1.7 + t * 0.2)) * scale;
			Location at = b.clone().add(Math.cos(a) * r, top + 0.15 * Math.sin(i + t * 0.3), Math.sin(a) * r);
			w.spawnParticle(Particle.CLOUD, at, 0, -Math.sin(a), 0, Math.cos(a), 0.05);
			if (i % 3 == 0) {
				w.spawnParticle(Particle.SMOKE_LARGE, at, 0, 0, 0.2, 0, 0.02);
			}
		}
		// rain falling out of the wall cloud
		for (int i = 0; i < 6; i++) {
			double a = RNG.nextDouble() * 2 * Math.PI;
			double r = (0.8 + RNG.nextDouble() * 1.6) * scale;
			w.spawnParticle(Particle.FALLING_WATER, b.clone().add(Math.cos(a) * r, top, Math.sin(a) * r), 1, 0, 0, 0, 0);
		}
		// wind shear slashes swinging around the funnel
		if (t % 3 == 0) {
			double a = t * 1.35;
			double h = 0.6 + RNG.nextDouble() * 1.8 * scale;
			double r = (0.15 + Math.pow(h / TORNADO_HEIGHT, 2.2) * 1.7) * scale + 0.3;
			w.spawnParticle(Particle.SWEEP_ATTACK, b.clone().add(Math.cos(a) * r, h, Math.sin(a) * r), 1, 0, 0, 0, 0);
		}
		// lightning crackle inside the funnel, and the odd flash in the cloud
		if (RNG.nextInt(5) == 0) {
			double h = RNG.nextDouble() * H;
			w.spawnParticle(Particle.ELECTRIC_SPARK, b.clone().add(0, h, 0), 25, 0.3, 0.4, 0.3, 0.35);
		}
		if (t % 37 == 11) {
			w.spawnParticle(Particle.FLASH, b.clone().add(0, top, 0), 1);
			w.playSound(b, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.35f, 1.6f);
		}
		// debris at the touchdown point
		w.spawnParticle(Particle.BLOCK_CRACK, b.clone().add(0, 0.15, 0), 8, 0.5 * scale, 0.1, 0.5 * scale, 0, ground);
		if (t % 25 == 0) {
			w.playSound(b, Sound.ITEM_TRIDENT_RIPTIDE_3, 0.5f, 0.8f);
		}
	}

	// ------------------------------------------------------------------ 3: Aurora Spirit

	private static void auroraSpirit(World w, Location b, int t, double scale) {
		double H = TORNADO_HEIGHT * scale;
		for (double h = 0; h <= H; h += 0.09) {
			double f = h / TORNADO_HEIGHT;
			double r = (0.3 + f * 1.25) * scale;
			for (int helix = 0; helix < 2; helix++) {
				// two helices spinning in opposite directions, colours cycling cyan -> blue -> violet
				double dirSign = helix == 0 ? 1 : -1;
				double a = dirSign * (t * 1.1 + h * 2.6) + helix * Math.PI;
				float hue = (float) (0.45 + 0.4 * (0.5 + 0.5 * Math.sin(f * 3 + t * 0.12 + helix)));
				java.awt.Color awt = java.awt.Color.getHSBColor(hue, 0.75f, 1.0f);
				Particle.DustOptions dust = new Particle.DustOptions(
						Color.fromRGB(awt.getRed(), awt.getGreen(), awt.getBlue()), 1.25f);
				Location at = b.clone().add(Math.cos(a) * r, h, Math.sin(a) * r);
				w.spawnParticle(Particle.REDSTONE, at, 1, 0, 0, 0, 0, dust);
			}
		}
		// light streaks shed outward along the spin
		for (int i = 0; i < 3; i++) {
			double h = RNG.nextDouble() * H;
			double a = t * 1.1 + h * 2.6 + RNG.nextDouble();
			double r = (0.3 + h / TORNADO_HEIGHT * 1.25) * scale;
			Location at = b.clone().add(Math.cos(a) * r, h, Math.sin(a) * r);
			w.spawnParticle(Particle.END_ROD, at, 0, -Math.sin(a) + Math.cos(a) * 0.4, 0.1, Math.cos(a) + Math.sin(a) * 0.4, 0.22);
		}
		// glittering crown spun off the top
		for (int i = 0; i < 4; i++) {
			double a = t * 0.6 + i * (Math.PI / 2);
			Location at = b.clone().add(Math.cos(a) * 1.5 * scale, H + 0.1, Math.sin(a) * 1.5 * scale);
			w.spawnParticle(Particle.FIREWORKS_SPARK, at, 0, Math.cos(a), 0.25, Math.sin(a), 0.12);
			w.spawnParticle(Particle.GLOW, at, 1, 0.1, 0.1, 0.1, 0);
		}
		// snowflakes swirling around the base, with silver glints
		for (int i = 0; i < 6; i++) {
			double a = RNG.nextDouble() * 2 * Math.PI;
			double r = (0.6 + RNG.nextDouble() * 1.1) * scale;
			Location at = b.clone().add(Math.cos(a) * r, 0.1 + RNG.nextDouble() * 0.6, Math.sin(a) * r);
			w.spawnParticle(Particle.SNOWFLAKE, at, 0, -Math.sin(a), 0.15, Math.cos(a), 0.15);
		}
		if (t % 2 == 0) {
			w.spawnParticle(Particle.WAX_OFF, b.clone().add(0, RNG.nextDouble() * H, 0), 2, 0.5, 0.3, 0.5, 0.2);
		}
		if (t % 5 == 0) {
			w.playSound(b, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.45f, 0.8f + RNG.nextFloat() * 1.2f);
		}
	}

	private static Color lerp(Color a, Color b, double f) {
		f = Math.max(0, Math.min(1, f));
		return Color.fromRGB((int) (a.getRed() + (b.getRed() - a.getRed()) * f),
				(int) (a.getGreen() + (b.getGreen() - a.getGreen()) * f),
				(int) (a.getBlue() + (b.getBlue() - a.getBlue()) * f));
	}

	/** Small adapter so this class doesn't depend on the action-bar helper's internals. */
	private static final class RunicMessagingShim {
		static void actionBar(Player p, String text) {
			ActionBars.send(p, text);
		}
	}
}
