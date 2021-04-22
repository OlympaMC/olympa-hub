package fr.olympa.hub.servers;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;

import fr.olympa.api.config.CustomConfig;
import fr.olympa.api.customevents.SpigotConfigReloadEvent;
import fr.olympa.api.server.MonitorInfo;
import fr.olympa.core.spigot.redis.receiver.BungeeServerInfoReceiver;

public class ServerInfosListener {

	@EventHandler
	public void onSpigotConfigReload(SpigotConfigReloadEvent event) {
		CustomConfig config = event.getConfig();
		if (config.getName().equals("config"))
			for (Entry<String, ServerInfo> entry : servers.entrySet())
				entry.getValue().updateConfig(config.getConfigurationSection(entry.getKey()));
	}

	public Map<String, ServerInfo> servers = new HashMap<>();

	public ServerInfosListener(ConfigurationSection serversConfig) {
		for (String serverName : serversConfig.getKeys(false)) {
			ConfigurationSection configSection = serversConfig.getConfigurationSection(serverName);
			servers.put(serverName, new ServerInfo(serverName, configSection));
		}
		BungeeServerInfoReceiver.registerCallback(mis -> updateData(mis));
	}

	public ServerInfo getServer(String serverName) {
		return servers.get(serverName);
	}

	public ServerInfo getServer(MonitorInfo mi) {
		return servers.get(mi.getName());
	}

	//	public ServerInfo getServer(OlympaServer olympaServer) {
	//		Entry<String, ServerInfo> entry = servers.entrySet().stream().filter(x -> x.getValue().getServer() == olympaServer).findAny().orElse(null);
	//		if (entry != null)
	//			return entry.getValue();
	//		return null;
	//	}

	public Collection<ServerInfo> getServers() {
		return servers.values();
	}

	public Map<String, ServerInfo> getServersInfo() {
		return servers;
	}

	public void updateData(List<MonitorInfo> newServers) {
		for (MonitorInfo newServ : newServers) {
			ServerInfo servInfo = servers.get(newServ.getName());
			if (servInfo != null)
				servInfo.updateInfo(newServ);
			servInfo = servers.get(newServ.getClearName());
			if (servInfo != null)
				servInfo.updateInfo(newServ);
		}
	}

	//	private int parseInt(String str) {
	//		try {
	//			return Integer.parseInt(str);
	//		} catch (NumberFormatException ex) {
	//			return -1;
	//		}
	//	}
}
