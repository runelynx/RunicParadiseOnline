package io.github.runelynx.runicparadise;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class RunicMessaging {
	
	public enum RunicFormat {
		FAITH(ChatColor.LIGHT_PURPLE + "RunicFaith" + ChatColor.GRAY + "> " + ChatColor.AQUA),
		CASINO(ChatColor.GOLD + "RunicCasino" + ChatColor.GRAY + "> " + ChatColor.GRAY),
		RANKS(ChatColor.GREEN + "RunicRanks" + ChatColor.GRAY + "> " + ChatColor.GRAY),
		EXPLORER(ChatColor.YELLOW + "ExplorersLeague" + ChatColor.GRAY + "> " + ChatColor.GRAY),
		EMPTY(ChatColor.GRAY + ""),
		SYSTEM(ChatColor.DARK_AQUA + "RunicEngine" + ChatColor.GRAY + "> " + ChatColor.GRAY);
		private String text;

		private RunicFormat(String txt) {
			this.text = txt;
		}
	}
	
	
	public static void sendMessage(Player p, RunicFormat format, String msg) { 
		
		p.sendMessage(format.text + msg);
		
	}


}
