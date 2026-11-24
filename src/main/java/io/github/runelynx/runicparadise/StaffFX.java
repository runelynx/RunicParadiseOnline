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

	// ================================================================== /beam

	static final int BEAM_CHARGE_END = 25;   // sparkles / low aura build up
	static final int BEAM_DESCEND_END = 40;  // beam comes down from the sky to the ground
	static final int BEAM_VANISH_AT = 50;    // player disappears inside the solid beam
	static final int BEAM_FULL_END = 80;     // fully charged, opaque column
	static final int BEAM_END = 110;         // dissipated
	static final int BEAM_AFTER_TICKS = 100; // player stays invisible 5 s after the beam has gone
	static final double BEAM_HEIGHT = 14;
	static final double BEAM_RADIUS = 0.95;

	static void beamCommand(Player sender, String[] args) {
		if (!sender.hasPermission("rp.fx")) {
			sender.sendMessage(ChatColor.RED + "That's a staff effect.");
			return;
		}
		int version = 1;
		try {
			if (args.length >= 1) {
				version = Integer.parseInt(args[0]);
			}
		} catch (NumberFormatException e) {
			sender.sendMessage(ChatColor.RED + "Usage: /beam <version 1-3> [player]");
			return;
		}
		if (version < 1 || version > 3) {
			sender.sendMessage(ChatColor.RED + "Versions: 1 Arcane Transporter, 2 Solar Lance, 3 Void Rift");
			return;
		}
		Player target = sender;
		if (args.length >= 2) {
			target = org.bukkit.Bukkit.getPlayerExact(args[1]);
			if (target == null) {
				sender.sendMessage(ChatColor.RED + "No online player named " + args[1] + ".");
				return;
			}
		}
		beam(target, version);
		String[] names = { "", "Arcane Transporter", "Solar Lance", "Void Rift" };
		ActionBars.send(sender, ChatColor.AQUA + "⇩ " + ChatColor.WHITE + names[version]
				+ (target == sender ? "" : ChatColor.GRAY + " on " + target.getName()));
	}

	static void beam(final Player target, final int version) {
		final Location base = target.getLocation().clone();
		base.setX(base.getBlockX() + 0.5);
		base.setZ(base.getBlockZ() + 0.5);
		final World w = base.getWorld();
		final java.util.List<Player> hiddenFrom = new java.util.ArrayList<>();

		switch (version) {
			case 1:
				w.playSound(base, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.4f);
				break;
			case 2:
				w.playSound(base, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, 1.2f);
				break;
			default:
				w.playSound(base, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 1.0f, 0.6f);
				w.playSound(base, Sound.ENTITY_WARDEN_SONIC_CHARGE, 0.6f, 1.3f);
				break;
		}

		new BukkitRunnable() {
			int t = 0;

			@Override
			public void run() {
				if (t > BEAM_END) {
					cancel();
					// reappear 5 s after the beam is fully gone (only to viewers we hid them from)
					new BukkitRunnable() {
						@Override
						public void run() {
							for (Player viewer : hiddenFrom) {
								if (viewer.isOnline() && target.isOnline()) {
									viewer.showPlayer(RunicParadise.getInstance(), target);
								}
							}
						}
					}.runTaskLater(RunicParadise.getInstance(), BEAM_AFTER_TICKS);
					return;
				}

				if (t == BEAM_VANISH_AT && target.isOnline()) {
					// fully hidden: armour, held items and name tag too. Viewers who already couldn't see the target
					// (e.g. a vanished staff member) are left alone, so this never reveals anyone.
					for (Player viewer : org.bukkit.Bukkit.getOnlinePlayers()) {
						if (viewer != target && viewer.canSee(target)) {
							viewer.hidePlayer(RunicParadise.getInstance(), target);
							hiddenFrom.add(viewer);
						}
					}
					target.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY,
							(BEAM_END - BEAM_VANISH_AT) + BEAM_AFTER_TICKS, 0, false, false, true));
					beamVanish(w, base, version);
				}

				if (t < BEAM_CHARGE_END) {
					beamCharge(w, base, t, t / (double) BEAM_CHARGE_END, version);
				} else if (t < BEAM_DESCEND_END) {
					double k = (t - BEAM_CHARGE_END) / (double) (BEAM_DESCEND_END - BEAM_CHARGE_END);
					beamCharge(w, base, t, 1.0, version);
					// the beam's lower edge travels from the sky down to the ground
					double bottom = BEAM_HEIGHT * (1 - k);
					beamColumn(w, base, bottom, BEAM_HEIGHT, 0.35 + 0.45 * k, t, version);
					if (t == BEAM_DESCEND_END - 1) {
						beamImpact(w, base, version);
					}
				} else if (t <= BEAM_FULL_END) {
					beamColumn(w, base, 0, BEAM_HEIGHT, 1.0, t, version);
				} else {
					double k = (t - BEAM_FULL_END) / (double) (BEAM_END - BEAM_FULL_END); // 0 -> 1
					beamDissipate(w, base, t, k, version);
				}
				t++;
			}
		}.runTaskTimer(RunicParadise.getInstance(), 0, 1);
	}

	/** Low-energy build-up around the target; k = 0..1 intensity. */
	private static void beamCharge(World w, Location b, int t, double k, int version) {
		int n = (int) (6 + 30 * k);
		for (int i = 0; i < n; i++) {
			double a = RNG.nextDouble() * 2 * Math.PI;
			double r = 0.4 + RNG.nextDouble() * 1.1;
			double y = RNG.nextDouble() * 2.4;
			Location at = b.clone().add(Math.cos(a) * r, y, Math.sin(a) * r);
			switch (version) {
				case 1:
					w.spawnParticle(Particle.END_ROD, at, 0, -Math.cos(a) * 0.3, 0.2, -Math.sin(a) * 0.3, 0.05);
					w.spawnParticle(Particle.REDSTONE, at, 1, 0, 0, 0, 0,
							new Particle.DustOptions(Color.fromRGB(120, 200, 255), 0.9f));
					break;
				case 2:
					w.spawnParticle(Particle.WAX_ON, at, 1, 0, 0, 0, 0.05);
					if (i % 3 == 0) {
						w.spawnParticle(Particle.FLAME, at, 0, 0, 1, 0, 0.04);
					}
					break;
				default:
					// portal particles drift toward their spawn offset, so they visibly converge on the target
					w.spawnParticle(Particle.PORTAL, b.clone().add(0, y, 0), 0, Math.cos(a) * r * 1.5, 0.2, Math.sin(a) * r * 1.5, 1);
					if (i % 3 == 0) {
						w.spawnParticle(Particle.REVERSE_PORTAL, at, 1, 0, 0, 0, 0.02);
					}
					break;
			}
		}
		// rotating ring on the ground that tightens as the charge builds
		double ringR = 1.6 - 0.6 * k;
		for (int i = 0; i < 24; i++) {
			double a = i * (2 * Math.PI / 24) + t * 0.25;
			Location at = b.clone().add(Math.cos(a) * ringR, 0.05, Math.sin(a) * ringR);
			switch (version) {
				case 1:
					w.spawnParticle(Particle.REDSTONE, at, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(80, 170, 255), 1.1f));
					break;
				case 2:
					w.spawnParticle(Particle.ENCHANTMENT_TABLE, at, 1, 0, 0.3, 0, 0.3);
					w.spawnParticle(Particle.REDSTONE, at, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(255, 190, 40), 1.1f));
					break;
				default:
					w.spawnParticle(Particle.DRAGON_BREATH, at, 1, 0, 0, 0, 0.005);
					break;
			}
		}
		if (t % 5 == 0) {
			Sound s = version == 1 ? Sound.BLOCK_AMETHYST_BLOCK_CHIME : version == 2 ? Sound.BLOCK_BEACON_POWER_SELECT : Sound.BLOCK_PORTAL_AMBIENT;
			w.playSound(b, s, 0.5f, (float) (0.8 + k * 1.0));
		}
	}

	/** Solid beam between two heights; density 0..1 (1 = opaque). */
	private static void beamColumn(World w, Location b, double from, double to, double density, int t, int version) {
		Particle.DustTransition core;
		Particle.DustOptions shell;
		switch (version) {
			case 1:
				core = new Particle.DustTransition(Color.fromRGB(245, 252, 255), Color.fromRGB(140, 215, 255), 3.0f);
				shell = new Particle.DustOptions(Color.fromRGB(60, 160, 255), 2.2f);
				break;
			case 2:
				core = new Particle.DustTransition(Color.fromRGB(255, 255, 235), Color.fromRGB(255, 205, 70), 3.0f);
				shell = new Particle.DustOptions(Color.fromRGB(255, 150, 20), 2.2f);
				break;
			default:
				core = new Particle.DustTransition(Color.fromRGB(70, 0, 110), Color.fromRGB(10, 0, 20), 3.2f);
				shell = new Particle.DustOptions(Color.fromRGB(150, 40, 230), 2.0f);
				break;
		}
		int perLayer = (int) (2 + 9 * density);
		for (double y = from; y <= to; y += 0.16) {
			for (int i = 0; i < perLayer; i++) {
				double a = RNG.nextDouble() * 2 * Math.PI;
				double r = Math.sqrt(RNG.nextDouble()) * BEAM_RADIUS; // uniform over the disc
				w.spawnParticle(Particle.DUST_COLOR_TRANSITION, b.clone().add(Math.cos(a) * r, y, Math.sin(a) * r), 1, 0, 0, 0, 0, core);
			}
			// bright shell spinning around the outside of the beam
			double a = t * 0.7 + y * 1.3;
			w.spawnParticle(Particle.REDSTONE, b.clone().add(Math.cos(a) * BEAM_RADIUS * 1.08, y, Math.sin(a) * BEAM_RADIUS * 1.08), 1, 0, 0, 0, 0, shell);
			w.spawnParticle(Particle.REDSTONE, b.clone().add(-Math.cos(a) * BEAM_RADIUS * 1.08, y, -Math.sin(a) * BEAM_RADIUS * 1.08), 1, 0, 0, 0, 0, shell);
		}
		// accents
		for (int i = 0; i < (int) (8 * density); i++) {
			double a = RNG.nextDouble() * 2 * Math.PI;
			Location at = b.clone().add(Math.cos(a) * BEAM_RADIUS, from + RNG.nextDouble() * (to - from), Math.sin(a) * BEAM_RADIUS);
			switch (version) {
				case 1:
					w.spawnParticle(Particle.END_ROD, at, 0, 0, -1, 0, 0.6); // streaks pouring down the beam
					break;
				case 2:
					w.spawnParticle(Particle.FIREWORKS_SPARK, at, 0, Math.cos(a), 0.1, Math.sin(a), 0.15);
					break;
				default:
					w.spawnParticle(Particle.SQUID_INK, at, 0, Math.cos(a), 0, Math.sin(a), 0.08);
					w.spawnParticle(Particle.DRAGON_BREATH, at, 1, 0, 0, 0, 0.01);
					break;
			}
		}
		if (density >= 1.0 && t % 12 == 0) {
			Sound s = version == 1 ? Sound.BLOCK_BEACON_AMBIENT : version == 2 ? Sound.BLOCK_CONDUIT_AMBIENT : Sound.BLOCK_END_PORTAL_FRAME_FILL;
			w.playSound(b, s, 0.9f, version == 3 ? 0.5f : 1.5f);
		}
	}

	/** The moment the beam hits the ground. */
	private static void beamImpact(World w, Location b, int version) {
		Location g = b.clone().add(0, 0.1, 0);
		switch (version) {
			case 1:
				ringBurst(w, g, Particle.END_ROD, 2.5, 48, 0.25);
				w.spawnParticle(Particle.FLASH, b.clone().add(0, 1, 0), 1);
				w.playSound(b, Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 0.8f);
				break;
			case 2:
				ringBurst(w, g, Particle.FLAME, 2.8, 56, 0.3);
				ringBurst(w, g, Particle.FIREWORKS_SPARK, 2.0, 32, 0.35);
				w.spawnParticle(Particle.FLASH, b.clone().add(0, 1, 0), 1);
				w.playSound(b, Sound.BLOCK_CONDUIT_ACTIVATE, 1.0f, 1.2f);
				break;
			default:
				ringBurst(w, g, Particle.REVERSE_PORTAL, 2.5, 60, 0.4);
				w.spawnParticle(Particle.SONIC_BOOM, b.clone().add(0, 1, 0), 1);
				w.playSound(b, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.5f, 1.6f);
				break;
		}
	}

	/** The instant the player disappears inside the beam. */
	private static void beamVanish(World w, Location b, int version) {
		Location c = b.clone().add(0, 1, 0);
		switch (version) {
			case 1:
				w.spawnParticle(Particle.END_ROD, c, 60, 0.3, 0.6, 0.3, 0.3);
				w.playSound(b, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.4f);
				break;
			case 2:
				w.spawnParticle(Particle.TOTEM, c, 80, 0.3, 0.6, 0.3, 0.6);
				w.playSound(b, Sound.ITEM_TOTEM_USE, 0.6f, 1.5f);
				break;
			default:
				w.spawnParticle(Particle.DRAGON_BREATH, c, 80, 0.3, 0.6, 0.3, 0.08);
				w.spawnParticle(Particle.SCULK_SOUL, c, 20, 0.3, 0.5, 0.3, 0.05);
				w.playSound(b, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
				break;
		}
	}

	/** Beam breaking up; k = 0..1 through the dissipation. */
	private static void beamDissipate(World w, Location b, int t, double k, int version) {
		switch (version) {
			case 1: {
				// the column lifts off the ground and thins out upwards, shedding rising rings of light
				double bottom = BEAM_HEIGHT * k;
				beamColumn(w, b, bottom, BEAM_HEIGHT, 1.0 - k, t, version);
				if (t % 3 == 0) {
					ringBurst(w, b.clone().add(0, bottom + 0.2, 0), Particle.END_ROD, BEAM_RADIUS * 1.3, 24, 0.0);
				}
				w.spawnParticle(Particle.END_ROD, b.clone().add(0, 1.2, 0), (int) (12 * (1 - k)), 0.5, 0.8, 0.5, 0.05);
				break;
			}
			case 2: {
				// the lance fades top-down while golden rays burst outward and dust settles
				double top = BEAM_HEIGHT * (1 - k);
				beamColumn(w, b, 0, top, 1.0 - k, t, version);
				if (t % 4 == 0) {
					for (int i = 0; i < 12; i++) {
						double a = i * (Math.PI / 6) + t * 0.1;
						w.spawnParticle(Particle.FIREWORKS_SPARK, b.clone().add(0, 1.2, 0), 0, Math.cos(a), 0, Math.sin(a), 0.4);
					}
				}
				w.spawnParticle(Particle.FALLING_DUST, b.clone().add(0, 3, 0), 10, 1.2, 1.5, 1.2, 0,
						Material.GOLD_BLOCK.createBlockData());
				break;
			}
			default: {
				// the rift collapses: the column shrinks down into a point on the ground and implodes
				double top = BEAM_HEIGHT * (1 - k) + 0.2;
				beamColumn(w, b, 0, top, 1.0 - 0.6 * k, t, version);
				w.spawnParticle(Particle.REVERSE_PORTAL, b.clone().add(0, top / 2, 0), 25, 0.6, top / 3, 0.6, 0.05);
				if (t == BEAM_END) {
					w.spawnParticle(Particle.SCULK_SOUL, b.clone().add(0, 0.3, 0), 30, 0.3, 0.2, 0.3, 0.08);
					w.spawnParticle(Particle.FLASH, b.clone().add(0, 0.5, 0), 1);
					w.playSound(b, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 0.6f);
				}
				break;
			}
		}
		if (t == BEAM_FULL_END + 1) {
			Sound s = version == 1 ? Sound.BLOCK_BEACON_DEACTIVATE : version == 2 ? Sound.BLOCK_CONDUIT_DEACTIVATE : Sound.BLOCK_PORTAL_TRAVEL;
			w.playSound(b, s, version == 3 ? 0.35f : 1.0f, version == 3 ? 1.6f : 1.2f);
		}
	}

	/** Particles thrown outward in a flat ring. */
	private static void ringBurst(World w, Location c, Particle type, double r, int points, double speed) {
		for (int i = 0; i < points; i++) {
			double a = 2 * Math.PI * i / points;
			Location at = c.clone().add(Math.cos(a) * r * 0.3, 0, Math.sin(a) * r * 0.3);
			if (speed > 0) {
				w.spawnParticle(type, at, 0, Math.cos(a), 0.05, Math.sin(a), speed);
			} else {
				w.spawnParticle(type, c.clone().add(Math.cos(a) * r, 0, Math.sin(a) * r), 1, 0, 0, 0, 0);
			}
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
