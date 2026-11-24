package io.github.runelynx.runicparadise;

import io.github.runelynx.runicuniverse.staff.Ui;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * "Type it in chat" prompts for menus (e.g. naming a new Explorer's League location). The next chat line from that
 * player is captured (never shown in public chat) and handed to the callback on the main thread. "cancel" aborts;
 * prompts expire after 2 minutes.
 */
final class ChatPrompt implements Listener {

	private static final class Pending {
		final Consumer<String> callback; final long expires;
		Pending(Consumer<String> callback) { this.callback = callback; this.expires = System.currentTimeMillis() + 120_000; }
	}

	private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();

	static void ask(Player p, String question, Consumer<String> callback) {
		PENDING.put(p.getUniqueId(), new Pending(callback));
		p.closeInventory();
		Ui.info(p, question + "  (type it in chat, or 'cancel')");
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onChat(AsyncPlayerChatEvent e) {
		Pending pd = PENDING.remove(e.getPlayer().getUniqueId());
		if (pd == null) {
			return;
		}
		e.setCancelled(true);
		String text = e.getMessage().trim();
		Player p = e.getPlayer();
		if (pd.expires < System.currentTimeMillis()) {
			return;
		}
		Bukkit.getScheduler().runTask(RunicParadise.getInstance(), () -> {
			if (text.equalsIgnoreCase("cancel")) {
				Ui.info(p, "Cancelled.");
				return;
			}
			pd.callback.accept(text);
		});
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		PENDING.remove(e.getPlayer().getUniqueId());
	}
}
