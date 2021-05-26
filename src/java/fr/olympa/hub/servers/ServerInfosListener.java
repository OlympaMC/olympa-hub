package fr.olympa.hub.servers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import fr.olympa.api.config.CustomConfig;
import fr.olympa.api.customevents.SpigotConfigReloadEvent;
import fr.olympa.api.server.MonitorInfo;
import fr.olympa.core.spigot.redis.receiver.BungeeServerInfoReceiver;

public class ServerInfosListener implements Listener {

	@EventHandler
	public void onSpigotConfigReload(SpigotConfigReloadEvent event) {
		CustomConfig config = event.getConfig();
		if (config.getName().equals("config"))
			for (Entry<String, ServerInfoItem> entry : servers.entrySet())
				entry.getValue().updateConfig(config.getConfigurationSection(entry.getKey()));
	}

	public Map<String, ServerInfoItem> servers = new HashMap<>();

	public ServerInfosListener(ConfigurationSection serversConfig) {
		for (String itemServerConfigKeyName : serversConfig.getKeys(false)) {
			ConfigurationSection configSection = serversConfig.getConfigurationSection(itemServerConfigKeyName);
			servers.put(itemServerConfigKeyName, new ServerInfoItem(itemServerConfigKeyName, configSection));
		}
		BungeeServerInfoReceiver.registerCallback(mis -> updateData(mis));
	}

	public ServerInfoItem getServer(String itemServerConfigKeyName) {
		return servers.get(itemServerConfigKeyName);
	}

	//	public ServerInfoItem getServer(MonitorInfo mi) {
	//		return servers.get(mi.getName());
	//	}

	//	public ServerInfo getServer(OlympaServer olympaServer) {
	//		Entry<String, ServerInfo> entry = servers.entrySet().stream().filter(x -> x.getValue().getServer() == olympaServer).findAny().orElse(null);
	//		if (entry != null)
	//			return entry.getValue();
	//		return null;
	//	}

	public Collection<ServerInfoItem> getServers() {
		return servers.values();
	}

	public Map<String, ServerInfoItem> getServersInfo() {
		return servers;
	}

	public void updateData(List<MonitorInfo> newServers) {
		Map<String, List<MonitorInfo>> newInfos = new HashMap<>();
		for (MonitorInfo newServ : newServers)
			for (Entry<String, ServerInfoItem> entry : servers.entrySet())
				if (entry.getValue().containsServer(newServ)) {
					List<MonitorInfo> list = newInfos.get(entry.getKey());
					if (list == null) {
						list = new ArrayList<>();
						newInfos.put(entry.getKey(), list);
					}
					list.add(newServ);
				}
		//			ServerInfoItem servInfo = servers.get(newServ.getName());
		//			if (servInfo != null)
		//				servInfo.updateInfo(newServ);
		//			servInfo = servers.get(newServ.getClearName());
		//			if (servInfo != null)
		//				servInfo.updateInfo(newServ);
		newInfos.forEach((itemServerConfigKeyName, newServs) -> {
			ServerInfoItem serverInfoItem = servers.get(itemServerConfigKeyName);
			serverInfoItem.tryUpdate(newServs);
		});
	}

	//	private int parseInt(String str) {
	//		try {
	//			return Integer.parseInt(str);
	//		} catch (NumberFormatException ex) {
	//			return -1;
	//		}
	//	}
}
