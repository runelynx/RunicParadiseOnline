package io.github.runelynx.runicparadise;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Action bar (the line above the hotbar) helper, 2026-11-23.
 * - send(): shows a RunicParadise message there and remembers when, per player.
 * - KillerMoney's "+8.0R" kill message: the client keeps any action-bar text for about 3 s and the server can't
 *   set a duration, so we clear it ourselves shortly after each KillerMoney payout, unless RunicParadise has
 *   shown something newer in the meantime (a power cast is never cut short).
 */
final class ActionBars {

	/** How long KillerMoney's kill message stays up before we clear it. */
	static final long KILLERMONEY_TICKS = 20; // 1 second

	private static final Map<UUID, Long> lastOurs = new ConcurrentHashMap<>();

	private ActionBars() {}

	static void send(Player p, String legacyText) {
		if (p == null || !p.isOnline()) {
			return;
		}
		lastOurs.put(p.getUniqueId(), System.currentTimeMillis());
		p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(legacyText));
	}

	/** ZOMBIFIED_PIGLIN -> Zombified Piglin */
	static String prettyName(String enumName) {
		StringBuilder sb = new StringBuilder();
		for (String part : enumName.toLowerCase().split("_")) {
			if (part.isEmpty()) {
				continue;
			}
			if (sb.length() > 0) {
				sb.append(' ');
			}
			sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
		}
		return sb.toString();
	}

	static void forget(UUID id) {
		lastOurs.remove(id);
	}

	/** Hooks KillerMoney's deposit event without a compile-time dependency. Safe if KillerMoney is absent. */
	@SuppressWarnings("unchecked")
	static void registerKillerMoneyShortener(final Plugin plugin) {
		Plugin km = Bukkit.getPluginManager().getPlugin("KillerMoney");
		if (km == null) {
			return;
		}
		try {
			final Class<? extends Event> eventClass = (Class<? extends Event>) Class.forName(
					"net.diecode.killermoney.events.KMEarnMoneyDepositEvent", true, km.getClass().getClassLoader());
			final Method getPlayer = eventClass.getMethod("getPlayer");
			final Method getAmount = eventClass.getMethod("getAmount");
			final Method getVictim = eventClass.getMethod("getVictim");
			// Each player's own "/km enable-messages on|off" choice, read live from KillerMoney's in-memory player
			// data (KMPlayerManager.getKMPlayer(player).isEnableMessages()), so toggling it applies immediately.
			Method getKMPlayer = null, isEnableMessages = null;
			try {
				Class<?> mgr = Class.forName("net.diecode.killermoney.managers.KMPlayerManager", true, km.getClass().getClassLoader());
				Class<?> kmPlayer = Class.forName("net.diecode.killermoney.objects.KMPlayer", true, km.getClass().getClassLoader());
				getKMPlayer = mgr.getMethod("getKMPlayer", Player.class);
				isEnableMessages = kmPlayer.getMethod("isEnableMessages");
			} catch (ReflectiveOperationException | LinkageError e) {
				plugin.getLogger().log(Level.WARNING, "Can't read KillerMoney's per-player message setting; showing payouts to everyone: " + e);
			}
			final Method getKMPlayerF = getKMPlayer, isEnableMessagesF = isEnableMessages;
			Bukkit.getPluginManager().registerEvent(eventClass, new Listener() {}, EventPriority.MONITOR, (listener, event) -> {
				if (!eventClass.isInstance(event) || ((Cancellable) event).isCancelled()) {
					return;
				}
				try {
					final Player p = (Player) getPlayer.invoke(event);
					if (p == null) {
						return;
					}
					if (getKMPlayerF != null) {
						Object kmp = getKMPlayerF.invoke(null, p);
						if (kmp != null && !((Boolean) isEnableMessagesF.invoke(kmp))) {
							return; // the player turned kill messages off with /km enable-messages off
						}
					}
					// KillerMoney 4.6.0 can't use the action bar on this server version (it falls back to chat), so
					// its own messages are DISABLED in its config and we show the payout here instead.
					java.math.BigDecimal amount = (java.math.BigDecimal) getAmount.invoke(event);
					org.bukkit.entity.LivingEntity victim = (org.bukkit.entity.LivingEntity) getVictim.invoke(event);
					String what = victim == null ? "" : victim.getCustomName() != null
							? org.bukkit.ChatColor.stripColor(victim.getCustomName())
							: prettyName(victim.getType().name());
					p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(
							org.bukkit.ChatColor.GOLD + "+" + amount.setScale(1, java.math.RoundingMode.HALF_UP).toPlainString()
									+ "R" + org.bukkit.ChatColor.GRAY + "  ·  " + what));
					final long paidAt = System.currentTimeMillis();
					Bukkit.getScheduler().runTaskLater(plugin, () -> {
						Long ours = lastOurs.get(p.getUniqueId());
						if (!p.isOnline() || (ours != null && ours >= paidAt)) {
							return; // something of ours is showing now; leave it
						}
						p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
					}, KILLERMONEY_TICKS);
				} catch (ReflectiveOperationException ignored) {
					// KillerMoney changed its API; just don't shorten
				}
			}, plugin, true);
			plugin.getLogger().info("KillerMoney payouts will show in the action bar for " + KILLERMONEY_TICKS + " ticks.");
		} catch (ReflectiveOperationException | LinkageError e) {
			plugin.getLogger().log(Level.WARNING, "Could not hook KillerMoney events: " + e);
		}
	}
}
