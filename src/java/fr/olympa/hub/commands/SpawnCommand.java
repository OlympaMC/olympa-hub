package fr.olympa.hub.commands;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import fr.olympa.api.command.OlympaCommand;
import fr.olympa.api.objects.OlympaServerSettings;
import fr.olympa.api.utils.Prefix;
import fr.olympa.hub.permission.OlympaHubPermissions;

public class SpawnCommand extends OlympaCommand {

	public SpawnCommand(Plugin plugin) {
		super(plugin, "spawn");
		this.setAllowConsole(false);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		Player player = this.player;
		if (args.length == 0) {
			this.sendMessage(Prefix.DEFAULT, "&7Tu es téléporté(e) au spawn.");
			player.teleport(OlympaServerSettings.getInstance().getSpawn());
		} else if (args[0].equalsIgnoreCase("set")) {

			if (!OlympaHubPermissions.SPAWN_SPAWN_COMMAND_SET.hasPermission(player)) {
				this.sendDoNotHavePermission();
			}
			OlympaServerSettings serverSettings = OlympaServerSettings.getInstance();
			Location location = player.getLocation().clone();
			location.setYaw(180);
			location.setPitch(0);
			serverSettings.setSpawn(location);
			this.sendMessage(Prefix.DEFAULT, "Le nouveau spawn a bien été modifié.");
		}
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
		return null;
	}

}
