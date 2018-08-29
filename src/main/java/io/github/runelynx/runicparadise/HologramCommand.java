package io.github.runelynx.runicparadise;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

class HologramCommand implements CommandExecutor {
	private Map<String, String> presets = new HashMap<>();
	private String filePath;
	private Logger logger;

	HologramCommand(File directory, Logger logger) throws IOException, JSONException {
		Files.createDirectories(directory.toPath());
		File presetsFile = new File(directory, "holo.json");

		this.logger = logger;
		filePath = presetsFile.getAbsolutePath();
		loadHoloPresets(presetsFile);
	}

	private static String transformToString(String[] args, int index) {
		String joined = Arrays.stream(args).skip(index).collect(Collectors.joining(" "));
		boolean isEscaped = false;
		StringBuilder result = new StringBuilder();

		for (int i = 0; i < joined.length(); ++i) {
			char ch = joined.charAt(i);
			if (isEscaped) {
				if (ch == 'n') {
					result.append('\n');
				} else if (ch == 's') {
					result.append(' ');
				} else {
					throw new RuntimeException("Invalid escaped character");
				}
				isEscaped = false;
			} else if (ch == '\\') {
				isEscaped = true;
			} else {
				result.append(ch);
			}
		}
		return ChatColor.translateAlternateColorCodes('&', result.toString());
	}

	private static void sendHelp(CommandSender sender) {

	}

	private static String formatOne(String key, String value) {
		return String.format("\"%s\" => \"%s\"", key, value);
	}

	private static void createArmorStand(Location location, String[] text) {
		for (String str : text) {
			ArmorStand stand = location.getWorld().spawn(location.subtract(0, .25, 0), ArmorStand.class);
			stand.setGravity(false);
			stand.setVisible(false);
			stand.setCustomName(str);
			stand.setCustomNameVisible(true);
		}
	}

	private void loadHoloPresets(File presetsFile) throws IOException, JSONException {
		if (presetsFile.exists()) {
			JSONObject object = new JSONObject(new String(Files.readAllBytes(Paths.get(presetsFile.getAbsolutePath()))));
			for (Object keyObj : object.keySet()) {
				String key = (String) keyObj;
				String value = (String) object.get(key);
				presets.put(key, ChatColor.translateAlternateColorCodes('&', value));
			}
		} else {
			Files.write(presetsFile.toPath(), "{}".getBytes());
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length == 0) {
			sendHelp(sender);
			return false;
		}
		try {
			String subcommand = args[0];
			if (subcommand.equalsIgnoreCase("preset")) {
				return presetCommand(sender, args[1], args);
			}
			if (subcommand.equalsIgnoreCase("preview")) {
				sender.sendMessage("Preview:\n" + transformToString(args, 1));
				return true;
			}
			if (subcommand.equalsIgnoreCase("create")) {
				return createCommand(sender, args[1]);
			}
		} catch (Exception e) {
			logger.log(Level.INFO, e.toString());
		}
		return true;
	}

	private boolean createCommand(CommandSender commandSender, String arg) {
		if (!(commandSender instanceof Player)) {
			commandSender.sendMessage("Sender must be player");
			return true;
		}
		Player sender = (Player) commandSender;

		arg = arg.toLowerCase();
		if (presets.containsKey(arg)) {
			createArmorStand(sender.getLocation(), presets.get(arg).split("\n"));
			return true;
		}
		return true;
	}

	private boolean presetCommand(CommandSender sender, String originalCommand, String[] args) throws IOException, JSONException {
		if (args.length < 2) {
			sendHelp(sender);
			return false;
		}
		String command = originalCommand.toLowerCase();
		if (command.equals("show")) {
			StringBuilder result = new StringBuilder();
			int count = 1;
			for (Map.Entry<String, String> entry : presets.entrySet()) {
				result.append(count++).append(". ").append(formatOne(entry.getKey(), entry.getValue())).append(ChatColor.RESET).append('\n');
			}
			sender.sendMessage(result.toString());
		} else {
			String presetName = args[2].toLowerCase();
			if (command.equals("add")) {
				if (args.length < 4) {
					sendHelp(sender);
					return false;
				}
				String value = transformToString(args, 3);
				presets.put(presetName, value);
				sender.sendMessage("Added " + formatOne(presetName, value));
			} else if (command.equals("remove")) {
				String removedValue = presets.remove(presetName);
				sender.sendMessage("Removed " + formatOne(presetName, removedValue));
			}

			updateFile();
		}
		return true;
	}

	private void updateFile() throws JSONException, IOException {
		JSONObject result = new JSONObject();
		for (Map.Entry<String, String> entry : presets.entrySet()) {
			result.put(entry.getKey(), entry.getValue());
		}
		Files.write(Paths.get(filePath), result.toString().getBytes());
	}
}
