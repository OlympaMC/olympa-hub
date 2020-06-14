package fr.olympa.hub;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import fr.olympa.api.command.OlympaCommand;

public class SpawnCommand extends OlympaCommand {

	public SpawnCommand(Plugin plugin) {
		super(plugin, "spawn", "Permet de se téléporter au spawn.", HubPermissions.SPAWN_COMMAND, "hub");
		setAllowConsole(false);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		player.teleport(player.getWorld().getSpawnLocation());
		return false;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
		return null;
	}

}
