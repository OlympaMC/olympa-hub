package fr.olympa.hub.servers;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import fr.olympa.api.command.complex.Cmd;
import fr.olympa.api.command.complex.CommandContext;
import fr.olympa.api.command.complex.ComplexCommand;
import fr.olympa.api.editor.RegionEditor;
import fr.olympa.api.editor.WaitBlockClick;
import fr.olympa.api.item.ItemUtils;
import fr.olympa.api.utils.Prefix;
import fr.olympa.hub.HubPermissions;
import fr.olympa.hub.OlympaHub;

public class ServerConfigCommand extends ComplexCommand {

	public ServerConfigCommand(Plugin plugin) {
		super(plugin, "serverConfig", "Permet de configurer les serveurs", HubPermissions.SERVER_CONFIG_COMMAND);
	}

	@Cmd (player = true, min = 1, syntax = "<server>")
	public void setPortal(CommandContext cmd) {
		ServerInfo server = OlympaHub.getInstance().serversInfos.getServer(cmd.getArgument(0));
		if (server == null) {
			sendError("Le serveur %s n'existe pas.", cmd.getArgument(0));
			return;
		}

		Player p = getPlayer();
		sendMessage(Prefix.DEFAULT, "Sélectionnez la zone du portail.");
		new RegionEditor(p, region -> {
			sendMessage(Prefix.DEFAULT, "Sélectionnez l'endroit où apparaîtra l'hologramme.");
			new WaitBlockClick(p, block -> {
				server.setPortal(region, block.getLocation().add(0.5, 1, 0.5));
				sendSuccess("Vous avez créé le portail pour le serveur %s !", server.name);
			}, ItemUtils.item(Material.STICK, "§aCliquez sur le bloc")).enterOrLeave();
		}).enterOrLeave();
	}

}
