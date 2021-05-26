package fr.olympa.hub.servers;

import java.util.stream.Collectors;

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
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;

public class ServerConfigCommand extends ComplexCommand {

	public ServerConfigCommand(Plugin plugin) {
		super(plugin, "serverConfig", "Permet de configurer les serveurs.", HubPermissions.SERVER_CONFIG_COMMAND);
		addArgumentParser("SERVER", (sender, arg) -> OlympaHub.getInstance().serversInfos.getServers().stream().map(x -> x.getItemServerNameKey()).collect(Collectors.toList()), (x) -> {
			try {
				ServerInfoItem server = OlympaHub.getInstance().serversInfos.getServer(x);
				if (server != null)
					return server;
			} catch (IllegalArgumentException ex) {}
			return null;
		}, x -> String.format("Le serveur %s n'existe pas.", x));
	}

	@Cmd(player = true, min = 1, args = "SERVER", syntax = "<server>")
	public void setPortal(CommandContext cmd) {
		ServerInfoItem server = cmd.getArgument(0);
		Player p = getPlayer();

		sendMessage(Prefix.DEFAULT, "Sélectionnez la zone du portail.");
		new RegionEditor(p, region -> {
			sendMessage(Prefix.DEFAULT, "Sélectionnez l'endroit où apparaîtra l'hologramme.");
			new WaitBlockClick(p, block -> {
				server.setPortal(region, block.getLocation().add(0.5, 1, 0.5));
				sendSuccess("Vous avez créé le portail pour le serveur %s !", server.getItemServerNameKey());
			}, ItemUtils.item(Material.STICK, "§aCliquez sur le bloc")).enterOrLeave();
		}).enterOrLeave();
	}

	@Cmd(player = true, min = 1, args = "SERVER", syntax = "<server>")
	public void setupNPC(CommandContext cmd) {
		NPC npc = CitizensAPI.getDefaultNPCSelector().getSelected(sender);
		if (npc == null) {
			sendError("Vous devez sélectionner un NPC.");
			return;
		}
		ServerTrait trait = npc.getOrAddTrait(ServerTrait.class);
		npc.data().setPersistent(NPC.NAMEPLATE_VISIBLE_METADATA, false);
		ServerInfoItem server = cmd.getArgument(0);
		trait.setServer(server);
		sendSuccess("Le NPC %d est maintenant associé aux serveurs %s.", npc.getId(), server.getItemServerNameKey());
	}

}
