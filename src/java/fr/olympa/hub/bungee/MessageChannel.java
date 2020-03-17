package fr.olympa.hub.bungee;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import fr.olympa.hub.OlympaHub;
import fr.olympa.hub.commonerrors.Messages;

public class MessageChannel implements PluginMessageListener {

	@Override
	public void onPluginMessageReceived(String arg0, Player arg1, byte[] arg2) {

	}

	public void connectToServer(Player player, String server) {
		try {
			ByteArrayOutputStream b = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(b);
			try {
				out.writeUTF("Connect");
				out.writeUTF(server);
			}catch (Exception e) {
				e.printStackTrace();
			}
			player.sendPluginMessage(OlympaHub.getInstance(), "BungeeCord", b.toByteArray());
		}catch (org.bukkit.plugin.messaging.ChannelNotRegisteredException e) {
			player.sendMessage(Messages.SERVER_NOT_FOUND.getErreur());
			Bukkit.getLogger().warning(" ERROR - Usage of bungeecord connect effects is not possible. Your server is not having bungeecord support (Bungeecord channel is not registered in your minecraft server)!");
		}
	}

}
