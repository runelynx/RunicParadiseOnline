package io.github.runelynx.runicparadise;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * /rpentitydump [radius] (2026-11-24): saves the EXACT live data of every entity within 100 blocks (default) of the
 * sender to plugins/RunicParadise/entity-dumps/, uncut, for investigating odd mobs. Data comes from Paper's
 * UnsafeValues.serializeEntity (the same data the server writes to disk), captured on the main thread at the moment
 * the command runs. Read-only: nothing about any entity is changed.
 */
final class EntityDump {

	private EntityDump() {}

	static final int DEFAULT_RADIUS = 100;
	static final int MAX_RADIUS = 200;

	private static final class Captured {
		String type, uuid, name, world, liveInfo;
		double x, y, z, distance;
		String snbt;
		String error;
		List<String> flags = new ArrayList<>();
	}

	static void command(Player p, String[] args) {
		if (!p.hasPermission("rp.entitydump")) {
			p.sendMessage(ChatColor.RED + "You don't have permission for that.");
			return;
		}
		int radius = DEFAULT_RADIUS;
		if (args.length >= 1) {
			try {
				radius = Math.max(1, Math.min(MAX_RADIUS, Integer.parseInt(args[0])));
			} catch (NumberFormatException e) {
				p.sendMessage(ChatColor.RED + "Usage: /rpentitydump [radius, default 100, max 200]");
				return;
			}
		}
		final Location at = p.getLocation();
		final long started = System.currentTimeMillis();
		final List<Captured> captured = new ArrayList<>();

		// Capture everything on the main thread right now, so the data is exactly what the server has at this moment.
		for (Entity e : p.getNearbyEntities(radius, radius, radius)) {
			if (e instanceof Player) {
				continue;
			}
			double dist = e.getLocation().distance(at);
			if (dist > radius) {
				continue;
			}
			Captured c = new Captured();
			c.type = e.getType().getKey().toString();
			c.uuid = e.getUniqueId().toString();
			c.name = e.getCustomName();
			c.world = e.getWorld().getName();
			c.x = e.getLocation().getX();
			c.y = e.getLocation().getY();
			c.z = e.getLocation().getZ();
			c.distance = dist;
			c.liveInfo = "class=" + e.getClass().getSimpleName()
					+ " valid=" + e.isValid() + " dead=" + e.isDead() + " persistent=" + e.isPersistent()
					+ " ticksLived=" + e.getTicksLived()
					+ " vehicle=" + (e.getVehicle() == null ? "none" : e.getVehicle().getType().getKey() + " " + e.getVehicle().getUniqueId())
					+ " passengers=" + describe(e.getPassengers());
			try {
				byte[] nbt = Bukkit.getUnsafe().serializeEntity(e);
				c.snbt = Snbt.fromCompressed(nbt);
			} catch (Throwable t) {
				c.error = t.getClass().getSimpleName() + ": " + t.getMessage();
			}
			// Flag data that doesn't match the live type (the reason this tool exists)
			if (c.snbt != null) {
				String idInData = Snbt.lastId;
				if (idInData != null && !idInData.equals(c.type)) {
					c.flags.add("data id " + idInData + " does not match live type " + c.type);
				}
				if (c.snbt.contains("StrayConversionTime") && !c.type.equals("minecraft:skeleton") && !c.snbt.contains("Passengers")) {
					c.flags.add("skeleton-only field StrayConversionTime on a " + c.type);
				}
			}
			captured.add(c);
		}
		captured.sort(Comparator.comparingDouble(c -> c.distance));
		final long captureMs = System.currentTimeMillis() - started;
		final int flagged = (int) captured.stream().filter(c -> !c.flags.isEmpty()).count();

		String stamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
		final File dir = new File(RunicParadise.getInstance().getDataFolder(), "entity-dumps");
		final File out = new File(dir, stamp + "_" + p.getName() + ".txt");
		final String who = p.getName();
		final int r = radius;

		p.sendMessage(ChatColor.GREEN + "Captured " + captured.size() + " entities within " + radius + " blocks"
				+ ChatColor.GRAY + " (" + captureMs + " ms). Saving to entity-dumps/" + out.getName());
		if (flagged > 0) {
			p.sendMessage(ChatColor.GOLD + "⚠ " + flagged + " entit" + (flagged == 1 ? "y has" : "ies have")
					+ " data that doesn't match its type:");
			captured.stream().filter(c -> !c.flags.isEmpty()).limit(5).forEach(c -> p.sendMessage(ChatColor.YELLOW + "  "
					+ c.type + " at " + (int) c.x + " " + (int) c.y + " " + (int) c.z + ChatColor.GRAY + " - " + String.join("; ", c.flags)));
		}

		// Write the file off the main thread
		Bukkit.getScheduler().runTaskAsynchronously(RunicParadise.getInstance(), () -> {
			try {
				dir.mkdirs();
				try (Writer w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(out), StandardCharsets.UTF_8))) {
					w.write("RunicParadise entity dump\n");
					w.write("Taken by: " + who + "\n");
					w.write("Time: " + new Date(started) + "\n");
					w.write(String.format("Centre: %s %.2f %.2f %.2f   radius: %d blocks%n", at.getWorld().getName(), at.getX(), at.getY(), at.getZ(), r));
					w.write("Entities: " + captured.size() + "   flagged: " + flagged + "\n");
					w.write("Data = the entity's full saved form (Paper serializeEntity), captured live on the main thread.\n");
					w.write("====================================================================================\n\n");
					int i = 0;
					for (Captured c : captured) {
						i++;
						w.write(String.format("#%d  %s  %s%n", i, c.type, c.name == null ? "" : "name=" + c.name));
						w.write(String.format("    uuid=%s  at %s %.2f %.2f %.2f  (%.1f blocks away)%n", c.uuid, c.world, c.x, c.y, c.z, c.distance));
						w.write("    live: " + c.liveInfo + "\n");
						if (!c.flags.isEmpty()) {
							w.write("    !!! FLAGS: " + String.join("; ", c.flags) + "\n");
						}
						if (c.error != null) {
							w.write("    (could not serialize: " + c.error + ")\n");
						} else {
							w.write("    data: " + c.snbt + "\n");
						}
						w.write("\n");
					}
				}
				Bukkit.getScheduler().runTask(RunicParadise.getInstance(), () -> {
					Player back = Bukkit.getPlayerExact(who);
					if (back != null) {
						back.sendMessage(ChatColor.GREEN + "Entity dump saved: " + ChatColor.WHITE + "plugins/RunicParadise/entity-dumps/" + out.getName());
					}
				});
				RunicParadise.getInstance().getLogger().info("[EntityDump] " + who + " saved " + captured.size()
						+ " entities (" + flagged + " flagged) to " + out.getName());
			} catch (IOException ex) {
				RunicParadise.getInstance().getLogger().warning("[EntityDump] could not write " + out + ": " + ex);
			}
		});
	}

	private static String describe(List<Entity> list) {
		if (list.isEmpty()) {
			return "none";
		}
		StringBuilder sb = new StringBuilder("[");
		for (Entity e : list) {
			if (sb.length() > 1) {
				sb.append(", ");
			}
			sb.append(e.getType().getKey()).append(' ').append(e.getUniqueId());
		}
		return sb.append(']').toString();
	}

	/** Minimal NBT reader that turns serialized entity bytes into SNBT text (same style as /data get). */
	static final class Snbt {
		static String lastId; // root "id" of the last converted compound (main-thread use only)

		static String fromCompressed(byte[] bytes) throws IOException {
			lastId = null;
			InputStream in = new ByteArrayInputStream(bytes);
			if (bytes.length > 2 && (bytes[0] & 0xff) == 0x1f && (bytes[1] & 0xff) == 0x8b) {
				in = new GZIPInputStream(in);
			}
			try (DataInputStream dis = new DataInputStream(new BufferedInputStream(in))) {
				int type = dis.readByte();
				if (type != 10) {
					throw new IOException("root tag is not a compound (" + type + ")");
				}
				dis.readUTF();
				StringBuilder sb = new StringBuilder();
				readPayload(dis, 10, sb, true);
				return sb.toString();
			}
		}

		private static void readPayload(DataInputStream in, int type, StringBuilder sb, boolean root) throws IOException {
			switch (type) {
				case 1: sb.append(in.readByte()).append('b'); break;
				case 2: sb.append(in.readShort()).append('s'); break;
				case 3: sb.append(in.readInt()); break;
				case 4: sb.append(in.readLong()).append('L'); break;
				case 5: sb.append(in.readFloat()).append('f'); break;
				case 6: sb.append(in.readDouble()).append('d'); break;
				case 7: {
					int n = in.readInt();
					sb.append("[B;");
					for (int i = 0; i < n; i++) sb.append(i == 0 ? "" : ",").append(in.readByte()).append('B');
					sb.append(']');
					break;
				}
				case 8: sb.append('"').append(in.readUTF().replace("\\", "\\\\").replace("\"", "\\\"")).append('"'); break;
				case 9: {
					int et = in.readByte();
					int n = in.readInt();
					sb.append('[');
					for (int i = 0; i < n; i++) {
						if (i > 0) sb.append(", ");
						readPayload(in, et, sb, false);
					}
					sb.append(']');
					break;
				}
				case 10: {
					sb.append('{');
					boolean first = true;
					while (true) {
						int t = in.readByte();
						if (t == 0) break;
						String name = in.readUTF();
						if (!first) sb.append(", ");
						first = false;
						sb.append(name.matches("[A-Za-z0-9_.+-]+") ? name : "\"" + name + "\"").append(": ");
						int before = sb.length();
						readPayload(in, t, sb, false);
						if (root && t == 8 && name.equals("id")) {
							String v = sb.substring(before);
							lastId = v.length() >= 2 ? v.substring(1, v.length() - 1) : v;
						}
					}
					sb.append('}');
					break;
				}
				case 11: {
					int n = in.readInt();
					sb.append("[I;");
					for (int i = 0; i < n; i++) sb.append(i == 0 ? "" : ",").append(in.readInt());
					sb.append(']');
					break;
				}
				case 12: {
					int n = in.readInt();
					sb.append("[L;");
					for (int i = 0; i < n; i++) sb.append(i == 0 ? "" : ",").append(in.readLong()).append('L');
					sb.append(']');
					break;
				}
				default:
					throw new IOException("bad tag type " + type);
			}
		}
	}
}
