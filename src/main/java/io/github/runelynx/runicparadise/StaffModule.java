package io.github.runelynx.runicparadise;

import io.github.runelynx.runicuniverse.staff.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Survival-only /rs actions (2026-11-24). These are thin wrappers: each one runs the existing command
 * (/staff xx, /faithweapon, /rpentitydump ...) as the staff member, so the old commands, their permissions and any
 * command blocks keep working exactly as before. What /rs adds: one menu, tab completion, confirmations, audit.
 */
final class StaffModule {

	private StaffModule() {}

	private static final List<String> WEAPONS = Arrays.asList("sword1", "sword2", "sword3", "axe1");

	static void register() {
		ExplorerAdmin.register();

		// ---------------------------------------------------------------- Players
		reg(StaffAction.of("near", "Players").icon("ENDER_EYE").playersOnly().targetsPlayer()
				.desc("Who is near a player (same as /staff np)")
				.usage("<player>")
				.complete((s, a) -> a.length == 1 ? CoreActions.onlineNames() : null)
				.exec((s, a) -> {
					if (a.length != 1) { Ui.error(s, "Usage: /rs near <player>"); return; }
					if (Bukkit.getPlayerExact(a[0]) == null) { Ui.error(s, a[0] + " isn't online."); return; }
					Bukkit.dispatchCommand(s, "staff np " + a[0]);
				}));

		reg(StaffAction.of("promocheck", "Players").icon("EXPERIENCE_BOTTLE").playersOnly()
				.desc("Online players' stats for rank promotions (same as /staff pe)")
				.exec((s, a) -> Bukkit.dispatchCommand(s, "staff pe")));

		reg(StaffAction.of("ranks", "Players").icon("LADDER").playersOnly()
				.desc("Rank promotion requirements (same as /staff sr)")
				.exec((s, a) -> Bukkit.dispatchCommand(s, "staff sr")));

		reg(StaffAction.of("nominate", "Players").icon("GOLDEN_CARROT").playersOnly().targetsPlayer()
				.desc("Nominate a player for faster promotions (same as /staff nm)")
				.usage("<player>")
				.complete((s, a) -> a.length == 1 ? CoreActions.onlineNames() : null)
				.exec((s, a) -> {
					if (a.length != 1) { Ui.error(s, "Usage: /rs nominate <player>"); return; }
					Bukkit.dispatchCommand(s, "staff nm " + a[0]);
					Audit.log(s, "nominate", a[0]);
				}));

		reg(StaffAction.of("farming", "Players").icon("WHEAT").playersOnly()
				.desc("Who is mob farming right now, with rank drop counts (same as /staff cf)")
				.exec((s, a) -> Bukkit.dispatchCommand(s, "staff cf")));

		reg(StaffAction.of("tokens", "Players").icon("SUNFLOWER").playersOnly()
				.desc("Online players' carnival token balances (same as /staff ct)")
				.exec((s, a) -> Bukkit.dispatchCommand(s, "staff ct")));

		List<String> colors = new ArrayList<>();
		for (ChatColor c : ChatColor.values()) {
			if (c.isColor()) colors.add(c.name());
		}
		reg(StaffAction.of("color", "Players").icon("CYAN_DYE").playersOnly().targetsPlayer()
				.desc("Set a player's chat name colour (same as /staff setplayercolor)")
				.usage("<player> <colour>")
				.complete((s, a) -> a.length == 1 ? CoreActions.onlineNames() : a.length == 2 ? colors : null)
				.exec((s, a) -> {
					if (a.length != 2 || !colors.contains(a[1].toUpperCase())) {
						Ui.error(s, "Usage: /rs color <player> <" + String.join("|", colors).toLowerCase() + ">");
						return;
					}
					Bukkit.dispatchCommand(s, "staff setplayercolor " + a[0] + " " + a[1].toUpperCase());
					Audit.log(s, "chat colour", a[0] + " -> " + a[1].toUpperCase());
					Ui.ok(s, "Set " + a[0] + "'s colour to " + a[1].toLowerCase() + ".");
				}));

		reg(StaffAction.of("graves", "Players").icon("SKELETON_SKULL").playersOnly()
				.desc("List recent graves for a player or everyone (same as /staff lg)")
				.usage("[player|all]")
				.complete((s, a) -> {
					if (a.length != 1) return null;
					List<String> l = new ArrayList<>(CoreActions.onlineNames());
					l.add("all");
					return l;
				})
				.menu(p -> { p.closeInventory(); Bukkit.dispatchCommand(p, "staff lg"); graveHint(p); })
				.exec((s, a) -> {
					Bukkit.dispatchCommand(s, a.length > 0 ? "staff lg " + a[0] : "staff lg");
					graveHint(s);
				}));

		reg(StaffAction.of("grave", "Players").icon("CHEST").playersOnly()
				.desc("Teleport to a grave, or unlock a locked one (same as /staff gg and /staff ug)")
				.usage("<tp|unlock> <grave id>")
				.complete((s, a) -> a.length == 1 ? Arrays.asList("tp", "unlock") : null)
				.exec((s, a) -> {
					if (a.length != 2 || !a[1].matches("\\d+")) { Ui.error(s, "Usage: /rs grave <tp|unlock> <grave id>"); return; }
					if (a[0].equalsIgnoreCase("tp")) {
						Bukkit.dispatchCommand(s, "staff gg " + a[1]);
					} else if (a[0].equalsIgnoreCase("unlock")) {
						Confirm.ask(s, "Unlock grave #" + a[1] + " (anyone can then open it)", () -> {
							Bukkit.dispatchCommand(s, "staff ug " + a[1]);
							Audit.log(s, "grave unlock", "#" + a[1]);
						});
					} else {
						Ui.error(s, "Usage: /rs grave <tp|unlock> <grave id>");
					}
				}));

		// ---------------------------------------------------------------- Rewards
		reg(StaffAction.of("souls", "Rewards").icon("SOUL_LANTERN").playersOnly().targetsPlayer()
				.desc("Give a player souls (same as /staff gs)")
				.usage("<player> <amount>")
				.complete((s, a) -> a.length == 1 ? CoreActions.onlineNames() : a.length == 2 ? Arrays.asList("1", "5", "10", "25") : null)
				.exec((s, a) -> {
					if (a.length != 2 || !a[1].matches("\\d+") || Integer.parseInt(a[1]) < 1) {
						Ui.error(s, "Usage: /rs souls <player> <amount>");
						return;
					}
					Confirm.ask(s, "Give " + a[0] + " " + a[1] + " soul(s)", () -> {
						Bukkit.dispatchCommand(s, "staff gs " + a[0] + " " + a[1]);
						Audit.log(s, "give souls", a[0] + " +" + a[1]);
					});
				}));

		reg(StaffAction.of("vote", "Rewards").icon("EMERALD").playersOnly().targetsPlayer()
				.desc("Give a player one vote reward, e.g. when a vote didn't register (same as /staff vt)")
				.usage("<player>")
				.complete((s, a) -> a.length == 1 ? CoreActions.onlineNames() : null)
				.exec((s, a) -> {
					if (a.length != 1) { Ui.error(s, "Usage: /rs vote <player>"); return; }
					Confirm.ask(s, "Give " + a[0] + " one vote reward", () -> {
						Bukkit.dispatchCommand(s, "staff vt " + a[0]);
						Audit.log(s, "give vote reward", a[0]);
					});
				}));

		reg(StaffAction.of("faithweapon", "Rewards").icon("GOLDEN_SWORD").perm("rp.staff.faithweapon").targetsPlayer()
				.desc("Give a player a faith weapon (same as /faithweapon)")
				.usage("<player> <" + String.join("|", WEAPONS) + ">")
				.complete((s, a) -> a.length == 1 ? CoreActions.onlineNames() : a.length == 2 ? WEAPONS : null)
				.exec((s, a) -> {
					if (a.length != 2 || !WEAPONS.contains(a[1].toLowerCase())) {
						Ui.error(s, "Usage: /rs faithweapon <player> <" + String.join("|", WEAPONS) + ">");
						return;
					}
					if (Bukkit.getPlayerExact(a[0]) == null) { Ui.error(s, a[0] + " isn't online."); return; }
					Confirm.ask(s, "Give " + a[0] + " faith weapon " + a[1].toLowerCase(), () -> {
						Bukkit.dispatchCommand(s, "faithweapon " + a[1].toLowerCase() + " " + a[0]);
						Audit.log(s, "give faith weapon", a[0] + " " + a[1].toLowerCase());
					});
				}));

		reg(StaffAction.of("staffpromo", "Players").icon("FIREWORK_STAR").perm("rp.admin").playersOnly().targetsPlayer()
				.desc("Announce a staff promotion to everyone (same as /staff sp)")
				.usage("<player>")
				.complete((s, a) -> a.length == 1 ? CoreActions.onlineNames() : null)
				.exec((s, a) -> {
					if (a.length != 1) { Ui.error(s, "Usage: /rs staffpromo <player>"); return; }
					Confirm.ask(s, "Announce " + a[0] + "'s staff promotion to everyone", () -> {
						Bukkit.dispatchCommand(s, "staff sp " + a[0]);
						Audit.log(s, "staff promotion announcement", a[0]);
					});
				}));

		// ---------------------------------------------------------------- Faith / Events
		reg(StaffAction.of("faithreload", "Faith").icon("NETHER_STAR").playersOnly()
				.desc("Reload the Faith config file (same as /staff rf). Faith powers pause briefly.")
				.menu(p -> Confirm.menu(p, "Reload the Faith config", Collections.singletonList("&7Faith powers pause for a moment."),
						() -> { Bukkit.dispatchCommand(p, "staff rf"); Audit.log(p, "faith reload", "-"); }, () -> StaffMenu.area(p, "Faith")))
				.exec((s, a) -> Confirm.ask(s, "Reload the Faith config (powers pause for a moment)", () -> {
					Bukkit.dispatchCommand(s, "staff rf");
					Audit.log(s, "faith reload", "-");
				})));

		reg(StaffAction.of("rafflereload", "Events & Games").icon("PAPER").playersOnly()
				.desc("Reload the raffle config file (same as /staff rr)")
				.exec((s, a) -> {
					Bukkit.dispatchCommand(s, "staff rr");
					Audit.log(s, "raffle reload", "-");
				}));

		// ---------------------------------------------------------------- Tools
		reg(StaffAction.of("entities", "Tools").icon("ZOMBIE_HEAD").playersOnly()
				.desc("Count entities near each player, and find crowded spots (same as /staff ec and /staff lc)")
				.usage("[crowded]")
				.complete((s, a) -> a.length == 1 ? Collections.singletonList("crowded") : null)
				.menu(p -> { p.closeInventory(); Bukkit.dispatchCommand(p, "staff ec"); })
				.exec((s, a) -> Bukkit.dispatchCommand(s, a.length > 0 && a[0].equalsIgnoreCase("crowded") ? "staff lc" : "staff ec")));

		reg(StaffAction.of("entitydump", "Tools").icon("SPAWNER").perm("rp.entitydump").playersOnly()
				.desc("Write every entity within a radius of you to a file (same as /rpentitydump)")
				.usage("[radius]")
				.complete((s, a) -> a.length == 1 ? Arrays.asList("50", "100", "200") : null)
				.menu(p -> { p.closeInventory(); Bukkit.dispatchCommand(p, "rpentitydump"); })
				.exec((s, a) -> Bukkit.dispatchCommand(s, a.length > 0 ? "rpentitydump " + a[0] : "rpentitydump")));

		reg(StaffAction.of("iteminfo", "Tools").icon("NAME_TAG").perm("rp.iteminfo").playersOnly()
				.desc("Details of the item in your hand (same as /iteminfo)")
				.exec((s, a) -> Bukkit.dispatchCommand(s, "iteminfo")));

		reg(StaffAction.of("fx", "Tools").icon("BLAZE_POWDER").perm("rp.fx").playersOnly()
				.desc("Staff effects: a tornado in front of you, or a beam on you or a player (same as /tornado, /beam)")
				.usage("<tornado <distance> <1-3>|beam <1-3> [player]>")
				.complete((s, a) -> {
					if (a.length == 1) return Arrays.asList("tornado", "beam");
					if (a[0].equalsIgnoreCase("tornado")) return a.length == 2 ? Arrays.asList("5", "10", "20") : a.length == 3 ? Arrays.asList("1", "2", "3") : null;
					if (a[0].equalsIgnoreCase("beam")) return a.length == 2 ? Arrays.asList("1", "2", "3") : a.length == 3 ? CoreActions.onlineNames() : null;
					return null;
				})
				.exec((s, a) -> {
					if (a.length >= 3 && a[0].equalsIgnoreCase("tornado")) {
						Bukkit.dispatchCommand(s, "tornado " + a[1] + " " + a[2]);
					} else if (a.length >= 2 && a[0].equalsIgnoreCase("beam")) {
						Bukkit.dispatchCommand(s, "beam " + a[1] + (a.length > 2 ? " " + a[2] : ""));
					} else {
						Ui.error(s, "Usage: /rs fx tornado <distance> <1-3>  or  /rs fx beam <1-3> [player]");
					}
				}));

		// ---------------------------------------------------------------- player card extras
		StaffRegistry.addPlayerInfo(StaffModule::playerInfo);
	}

	private static void reg(StaffAction a) {
		StaffRegistry.register(a);
	}

	private static void graveHint(CommandSender s) {
		s.sendMessage(Component.text("   ")
				.append(Ui.suggest("Teleport to a grave", NamedTextColor.YELLOW, "/rs grave tp ", null))
				.append(Ui.space())
				.append(Ui.suggest("Unlock a grave", NamedTextColor.YELLOW, "/rs grave unlock ", null)));
	}

	private static List<Component> playerInfo(OfflinePlayer op) {
		List<Component> out = new ArrayList<>();
		MySQL db = RunicUtilities.getMysqlFromPlugin(RunicParadise.getInstance());
		try (Connection c = db.openConnection();
			 PreparedStatement ps = c.prepareStatement("SELECT ActiveFaith, Tokens, SoulCount, Karma, Votes, StaffRank, ChatColor, IsNominated,"
					 + " (SELECT COUNT(*) FROM rp_ExplorerCompletions e WHERE e.UUID = p.UUID) AS ELFinds FROM rp_PlayerInfo p WHERE UUID = ?")) {
			ps.setString(1, op.getUniqueId().toString());
			try (ResultSet rs = ps.executeQuery()) {
				if (!rs.next()) {
					return out;
				}
				out.add(line("Faith", nz(rs.getString("ActiveFaith"), "none") + "   Staff rank: " + nz(rs.getString("StaffRank"), "none")));
				out.add(line("Tokens / souls / karma", rs.getInt("Tokens") + " / " + rs.getInt("SoulCount") + " / " + rs.getInt("Karma")));
				out.add(line("Votes", rs.getInt("Votes") + "   Nominations: " + rs.getInt("IsNominated")
						+ "   Chat colour: " + nz(rs.getString("ChatColor"), "default").toLowerCase()));
				out.add(line("Explorer's League", rs.getInt("ELFinds") + " location(s) found"));
			}
		} catch (SQLException e) {
			RunicParadise.getInstance().getLogger().warning("[rs] player info failed: " + e.getMessage());
		}
		return out;
	}

	private static Component line(String label, String value) {
		return Component.text(label + ": ", NamedTextColor.GRAY).append(Component.text(value, NamedTextColor.WHITE));
	}

	private static String nz(String s, String dflt) {
		return s == null || s.isEmpty() ? dflt : s;
	}

	@SuppressWarnings("unused")
	private static boolean isPlayer(CommandSender s) { return s instanceof Player; }
}
