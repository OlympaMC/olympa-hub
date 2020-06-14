package fr.olympa.hub.servers;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import fr.olympa.api.server.ServerStatus;
import redis.clients.jedis.JedisPubSub;

public class ServerInfosListener extends JedisPubSub {

	public Map<String, ServerInfo> servers = new HashMap<>();

	public ServerInfosListener(ConfigurationSection serversConfig) {
		for (String serverName : serversConfig.getKeys(false)) {
			ConfigurationSection server = serversConfig.getConfigurationSection(serverName);
			servers.put(serverName, new ServerInfo(serverName, server.getString("name"), server.getString("description"), Material.valueOf(server.getString("item"))));
		}
	}

	@Override
	public void onMessage(String channel, String message) {
		String[] serversInfos = message.split("\\|");
		for (String server : serversInfos) {
			String[] infos = server.split(":");
			ServerInfo info = servers.get(infos[0]);
			if (info == null) continue;
			info.update(Integer.parseInt(infos[1]), Integer.parseInt(infos[2]), ServerStatus.get(Integer.parseInt(infos[3])));
		}
	}
}
