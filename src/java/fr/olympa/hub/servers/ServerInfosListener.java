package fr.olympa.hub.servers;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;

import fr.olympa.api.server.OlympaServer;
import fr.olympa.api.server.ServerStatus;
import redis.clients.jedis.JedisPubSub;

public class ServerInfosListener extends JedisPubSub {

	public List<ServerInfo> servers = new ArrayList<>();

	public ServerInfosListener(ConfigurationSection serversConfig) {
		for (String serverName : serversConfig.getKeys(false)) {
			ConfigurationSection server = serversConfig.getConfigurationSection(serverName);
			servers.add(new ServerInfo(OlympaServer.valueOf(serverName), server));
		}
	}

	public ServerInfo getServer(OlympaServer olympaServer) {
		return servers.stream().filter(x -> x.getServer() == olympaServer).findAny().orElse(null);
	}

	@Override
	public void onMessage(String channel, String message) {
		String[] infos = message.split(":");
		ServerInfo info = getServer(OlympaServer.valueOf(infos[0]));
		if (info == null) return;
		info.update(parseInt(infos[1]), ServerStatus.get(Integer.parseInt(infos[2])));
	}

	private int parseInt(String str) {
		try {
			return Integer.parseInt(str);
		}catch (NumberFormatException ex) {
			return -1;
		}
	}
}
