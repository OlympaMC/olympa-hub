package fr.olympa.hub.servers;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import fr.olympa.api.customevents.MonitorServerInfoReceiveEvent;
import fr.olympa.api.server.MonitorInfo;
import fr.olympa.api.server.OlympaServer;
import fr.olympa.api.server.ServerStatus;
import redis.clients.jedis.JedisPubSub;

public class ServerInfosListener extends JedisPubSub implements Listener {

	public Map<String, ServerInfo> servers = new HashMap<>();
	public static Map<String, ServerInfo> servers2 = new HashMap<>();

	public ServerInfosListener(ConfigurationSection serversConfig) {
		for (String serverName : serversConfig.getKeys(false)) {
			ConfigurationSection server = serversConfig.getConfigurationSection(serverName);
			servers.put(serverName, new ServerInfo(OlympaServer.valueOf(serverName.replaceAll("\\d", "").toUpperCase()), server));
		}
		for (String serverName : serversConfig.getKeys(false)) {
			ConfigurationSection configSection = serversConfig.getConfigurationSection(serverName);
			servers2.put(serverName, new ServerInfo(serverName, configSection));
		}
	}

	public ServerInfo getServer(String serverName) {
		return servers.get(serverName);
	}

	public ServerInfo getServer(OlympaServer olympaServer) {
		Entry<String, ServerInfo> entry = servers.entrySet().stream().filter(x -> x.getValue().getServer() == olympaServer).findAny().orElse(null);
		if (entry != null)
			return entry.getValue();
		return null;
	}

	public Collection<ServerInfo> getServers() {
		return servers.values();
	}

	public Map<String, ServerInfo> getServersInfo() {
		return servers;
	}

	public void updateData(List<MonitorInfo> newServers) {
		for (MonitorInfo newServ : newServers) {
			ServerInfo servInfo = servers2.get(newServ.getName());
			if (servInfo != null)
				servInfo.setInfo(newServ);
			servInfo = servers2.get(newServ.getClearName());
			if (servInfo != null)
				servInfo.setInfo(newServ);
		}
	}

	@EventHandler
	public void onMonitorServerInfoReceive(MonitorServerInfoReceiveEvent event) {
		updateData(event.getServers());
	}

	@Override
	public void onMessage(String channel, String message) {
		String[] infos = message.split(":");
		ServerInfo info = getServer(OlympaServer.valueOf(infos[0]));
		if (info == null)
			return;
		info.update(parseInt(infos[1]), ServerStatus.get(Integer.parseInt(infos[2])));
	}

	private int parseInt(String str) {
		try {
			return Integer.parseInt(str);
		} catch (NumberFormatException ex) {
			return -1;
		}
	}
}
