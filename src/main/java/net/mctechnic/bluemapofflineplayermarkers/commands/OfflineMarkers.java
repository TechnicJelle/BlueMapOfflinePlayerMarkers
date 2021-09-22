package net.mctechnic.bluemapofflineplayermarkers.commands;

import net.mctechnic.bluemapofflineplayermarkers.main;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class OfflineMarkers implements CommandExecutor, TabCompleter {

	private static final String MARKERS_RESET = "All markers have been deleted";
	private static final String MARKERS_LOADED = "Offline player markers have been loaded and created";

	private main plugin;

	public OfflineMarkers(main plugin){
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if(args.length != 1) return false;
		if(args[0].equalsIgnoreCase("load")){
			plugin.resetMarkers();
			sender.sendMessage(ChatColor.GREEN + MARKERS_RESET);
			plugin.loadMarkers();
			sender.sendMessage(ChatColor.GREEN + MARKERS_LOADED);
			return true;
		}else if(args[0].equalsIgnoreCase("reset")){
			plugin.resetMarkers();
			sender.sendMessage(ChatColor.GREEN + MARKERS_RESET);
			return true;
		}
		return false;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		List<String> completions = new ArrayList<>();
		if(args.length == 1){
			completions.add("load");
			completions.add("reset");
		}
		if(args.length >= 1 && args[args.length -1].length() > 0){
			String arg = args[args.length -1];
			completions.removeIf(suggestion -> !suggestion.startsWith(arg));
		}
		return completions;
	}
}
