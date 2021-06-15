package fr.olympa.hub.servers;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Listener;

import fr.olympa.api.common.redis.RedisClass;
import fr.olympa.api.common.server.ServerInfoBasic;
import fr.olympa.api.spigot.config.CustomConfig;
import fr.olympa.core.spigot.OlympaCore;

public class ServerInfosListener implements Listener {

	private Map<String, ServerInfoItem> servers = new HashMap<>();

	public ServerInfosListener(CustomConfig config) {
		config.addTask(this.getClass().getName(), configTask -> {
			OlympaCore.getInstance().retreiveMonitorInfos((serverInfoBasic, isInstantData) -> {
				ConfigurationSection serversConfig = configTask.getConfigurationSection("servers");
				servers.clear();
				for (String itemServerConfigKeyName : serversConfig.getKeys(false)) {
					ConfigurationSection configSection = serversConfig.getConfigurationSection(itemServerConfigKeyName);
					ServerInfoItem servInfoItem = new ServerInfoItem(itemServerConfigKeyName, configSection);
					servInfoItem.update(serverInfoBasic);
					servers.put(itemServerConfigKeyName, servInfoItem);
				}
			}, false);
		});
		RedisClass.SERVER_INFO.registerCallback(mis -> updateData(mis));
	}

	ServerInfoItem getServer(String itemServerConfigKeyName) {
		return servers.get(itemServerConfigKeyName);
	}

	public Collection<ServerInfoItem> getServers() {
		return servers.values();
	}

	public Map<String, ServerInfoItem> getServersInfo() {
		return servers;
	}

	private void updateData(List<ServerInfoBasic> newServers) {
		for (Entry<String, ServerInfoItem> entry : servers.entrySet())
			entry.getValue().update(newServers);
	}

}
