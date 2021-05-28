package fr.olympa.hub.servers;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Listener;

import fr.olympa.api.config.CustomConfig;
import fr.olympa.api.server.MonitorInfo;
import fr.olympa.core.spigot.OlympaCore;
import fr.olympa.core.spigot.redis.receiver.BungeeServerInfoReceiver;

public class ServerInfosListener implements Listener {

	public Map<String, ServerInfoItem> servers = new HashMap<>();

	public ServerInfosListener(CustomConfig config) {
		config.addTask(this.getClass().getName(), configTask -> {

			OlympaCore.getInstance().retreiveMonitorInfos((mi, isInstantData) -> {
				ConfigurationSection serversConfig = configTask.getConfigurationSection("servers");
				//				servers.forEach((name, sii) -> sii.clearObservers());
				servers.clear();
				for (String itemServerConfigKeyName : serversConfig.getKeys(false)) {
					ConfigurationSection configSection = serversConfig.getConfigurationSection(itemServerConfigKeyName);
					ServerInfoItem servInfoItem = new ServerInfoItem(itemServerConfigKeyName, configSection);
					servInfoItem.update(mi);
					servers.put(itemServerConfigKeyName, new ServerInfoItem(itemServerConfigKeyName, configSection));
				}
			}, false);
		});
		BungeeServerInfoReceiver.registerCallback(mis -> updateData(mis));
	}

	public ServerInfoItem getServer(String itemServerConfigKeyName) {
		return servers.get(itemServerConfigKeyName);
	}

	public Collection<ServerInfoItem> getServers() {
		return servers.values();
	}

	public Map<String, ServerInfoItem> getServersInfo() {
		return servers;
	}

	public void updateData(List<MonitorInfo> newServers) {
		for (Entry<String, ServerInfoItem> entry : servers.entrySet())
			entry.getValue().update(newServers);
	}

}
