package fr.olympa.hub.servers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.ChatPaginator;

import fr.olympa.api.holograms.Hologram;
import fr.olympa.api.item.ItemUtils;
import fr.olympa.api.lines.DynamicLine;
import fr.olympa.api.lines.FixedLine;
import fr.olympa.api.match.MatcherPattern;
import fr.olympa.api.permission.OlympaPermission;
import fr.olympa.api.player.OlympaPlayer;
import fr.olympa.api.region.Region;
import fr.olympa.api.region.tracking.ActionResult;
import fr.olympa.api.region.tracking.RegionEvent.EntryEvent;
import fr.olympa.api.region.tracking.TrackedRegion;
import fr.olympa.api.region.tracking.flags.Flag;
import fr.olympa.api.server.MonitorInfo;
import fr.olympa.api.server.ServerStatus;
import fr.olympa.api.sort.Sorting;
import fr.olympa.api.utils.Prefix;
import fr.olympa.api.utils.Utils;
import fr.olympa.api.utils.observable.AbstractObservable;
import fr.olympa.api.utils.spigot.SpigotUtils;
import fr.olympa.core.spigot.OlympaCore;
import fr.olympa.core.spigot.redis.RedisSpigotSend;
import fr.olympa.hub.OlympaHub;

public class ServerInfoItem extends AbstractObservable {

	private static final String SEPARATOR = "§8§m------------------------------------";
	private static final MatcherPattern<Integer> REGEX_SERV_NB = new MatcherPattern<>("\\d+$", s -> Integer.parseInt(s));

	@Nullable
	private String defaultServersName;
	private String itemServerKey;
	public List<String> description;
	public Material item;
	public int slot;

	private ItemStack menuItem = ItemUtils.error;
	private Portal portal;
	@Nonnull
	private ConfigurationSection config;
	private Map<String, MonitorInfo> serversInfo = new TreeMap<>();
	private boolean isUniqueMultipleServers;

	public ServerInfoItem(String itemServerKey, ConfigurationSection config) {
		this.itemServerKey = itemServerKey;
		try {
			updateConfig(config);
		} catch (Exception e) {
			e.addSuppressed(new Throwable("Can't load server item " + itemServerKey));
			e.printStackTrace();
		}
	}

	public boolean containsServer(String servName) {
		return serversInfo.entrySet().stream().anyMatch(e -> e.getValue() != null && e.getKey().equals(servName));
	}

	public Optional<Entry<String, MonitorInfo>> getServer(String servName) {
		return serversInfo.entrySet().stream().filter(e -> e.getKey().equals(servName)).findFirst();
	}

	public boolean containsMinimumOneServer(List<MonitorInfo> monitorInfos) {
		return monitorInfos.stream().anyMatch(monitorInfo -> serversInfo.keySet().stream().anyMatch(bungeeServerName -> monitorInfo.getName().equals(bungeeServerName)));
	}

	private void updateConfig(ConfigurationSection config) throws Exception {
		description = Arrays.asList(ChatPaginator.wordWrap("§8> §7" + config.getString("description"), 40));
		item = Material.valueOf(config.getString("item"));
		slot = config.getInt("slot");
		List<String> bungeeServersNames = config.getStringList("bungeeServersNames");
		bungeeServersNames.forEach(bungeeServerName -> {
			if (!serversInfo.containsKey(bungeeServerName))
				serversInfo.put(bungeeServerName, null);
		});
		defaultServersName = config.getString("defaultServersName");
		isUniqueMultipleServers = config.getBoolean("isUniqueMultipleServers");
		if (config.contains("portal"))
			portal = new Portal(config.getConfigurationSection("portal"));
	}

	public MonitorInfo getInfo(String bungeeServerName) {
		return serversInfo.get(bungeeServerName);
	}

	private boolean tryUpdate(List<MonitorInfo> mis) {
		boolean b = false;
		for (MonitorInfo mi : mis) {
			Optional<Entry<String, MonitorInfo>> oldServ = getServer(mi.getName());
			if (oldServ.isEmpty())
				continue;
			Entry<String, MonitorInfo> get = oldServ.get();
			if (get.getValue() == null || !mi.equals(get.getValue())) {
				serversInfo.put(mi.getName(), mi);
				b = true;
			}
		}
		return b;
	}

	public String getCleanName(MonitorInfo info) {
		String cleanName;
		if (REGEX_SERV_NB.contains(info.getName()))
			cleanName = info.getOlympaServer().getNameCaps() + " " + Utils.intToSymbole(REGEX_SERV_NB.extractAndParse(info.getName()));
		else
			cleanName = info.getOlympaServer().getNameCaps();
		return cleanName;
	}

	public void update(List<MonitorInfo> newMonitorInfo) {
		tryUpdate(newMonitorInfo);
		//		if (!tryUpdate(newMonitorInfo))
		//			return;
		Collection<MonitorInfo> monitorInfo = serversInfo.values();
		List<String> lore = new ArrayList<>();
		lore.add(SEPARATOR);
		lore.addAll(description);
		lore.add(SEPARATOR);
		lore.add(getOnlineString());
		ServerStatus status = monitorInfo.stream().filter(mi -> mi != null).sorted(new Sorting<>(mi -> mi.getStatus().getId(), true)).map(MonitorInfo::getStatus).findFirst().orElse(ServerStatus.UNKNOWN);
		if (status != ServerStatus.OPEN)
			lore.add("§7Statut : " + status.getNameColored());
		lore.add("");
		monitorInfo.forEach(mi -> {
			if (mi != null) {
				StringBuilder sb = new StringBuilder();
				sb.append("§7" + mi.getOlympaServer().getNameCaps());
				String symbole = mi.getIdSymbole();
				if (!symbole.isBlank())
					sb.append(" " + symbole);
				if (mi.getStatus() != ServerStatus.OPEN)
					sb.append(" " + mi.getStatus().getNameColored());
				if (mi.getOnlinePlayers() != null) {
					int online = mi.getOnlinePlayers();
					sb.append(String.format(" - %s§7 joueur%s ", online, Utils.withOrWithoutS(online)));
				}
				lore.add(sb.toString());
			}
		});
		menuItem = ItemUtils.item(item, "§6§l" + getServerNameCaps(), lore.toArray(new String[0]));
		ItemUtils.addEnchant(menuItem, Enchantment.DURABILITY, 0);
		update();
	}

	@Nonnull
	public String getItemServerNameKey() {
		return itemServerKey;
	}

	@Nonnull
	public String getServerNameOrItemKey() {
		if (defaultServersName != null)
			return defaultServersName;
		return itemServerKey;
	}

	@Nonnull
	public String getServerNameCaps() {
		if (!serversInfo.isEmpty()) {
			MonitorInfo oneServerInfo = serversInfo.values().stream().filter(mi -> mi != null).findFirst().orElse(null);
			if (oneServerInfo != null && serversInfo.values().stream().allMatch(mi -> mi != null && mi.getOlympaServer().isSame(oneServerInfo.getOlympaServer())))
				return oneServerInfo.getOlympaServer().getNameCaps();
		}
		if (defaultServersName != null)
			return defaultServersName;
		return "Serveur Inconnu";
	}

	public Integer getTotalOnlinePlayer() {
		Integer online = null;
		for (MonitorInfo mi : serversInfo.values())
			if (mi != null && mi.getOnlinePlayers() != null) {
				if (online == null)
					online = 0;
				online += mi.getOnlinePlayers();
			}
		return online;
	}

	public String getOnlineString() {
		Integer online = getTotalOnlinePlayer();
		return (online == null ? "§cx" : "§a§l" + online) + " §7joueur" + (online == null || online < 2 ? "" : "s") + " en ligne";
	}

	public ItemStack getMenuItem() {
		return menuItem;
	}

	public boolean connect(Player p) {
		Set<MonitorInfo> serversNotOff = serversInfo.values().stream().filter(mi -> mi != null && !mi.getStatus().equals(ServerStatus.CLOSE)).collect(Collectors.toSet());
		if (serversNotOff.isEmpty()) {
			Prefix.DEFAULT_BAD.sendMessage(p, "Ce serveur est fermé. Réessaye plus tard !");
			return false;
		}
		List<MonitorInfo> serverCanConnect = serversNotOff.stream().filter(mi -> {
			OlympaPermission permission = mi.getStatus().getPermission();
			return permission == null || permission.hasPermission(p.getUniqueId());
		}).sorted(new Sorting<>(true, mi -> mi.getStatus().getId(), MonitorInfo::getId)).collect(Collectors.toList());
		if (serverCanConnect.isEmpty()) {
			Prefix.DEFAULT_BAD.sendMessage(p, "Tu n'as pas la permission de te connecter à ce serveur.");
			return false;
		}
		// TODO choose between multiple serveurs with isUniqueMultipleServers & onlines count of player
		MonitorInfo serverInfo = serverCanConnect.get(0);
		Prefix.DEFAULT_GOOD.sendMessage(p, "Tu vas être transféré au serveur %s sous peu !", Utils.capitalize(serverInfo.getName()));
		RedisSpigotSend.sendServerSwitch(p, serverInfo.getName());
		return true;
	}

	public void setPortal(Region region, Location holoLocation) {
		if (portal != null)
			portal.destroy();
		portal = new Portal(region, holoLocation);

		ConfigurationSection portalConfig = config.contains("portal") ? config.getConfigurationSection("portal") : config.createSection("portal");
		portalConfig.set("region", region);
		portalConfig.set("holoLocation", SpigotUtils.convertLocationToString(holoLocation));
		OlympaHub.getInstance().getConfig().save();
	}

	class Portal {

		private TrackedRegion region;
		private Hologram hologram;

		public Portal(ConfigurationSection config) {
			this((Region) config.get("region"), SpigotUtils.convertStringToLocation(config.getString("holoLocation")));
		}

		public Portal(Region region, Location holoLocation) {
			this.region = OlympaCore.getInstance().getRegionManager().registerRegion(region, "portal_" + itemServerKey, EventPriority.HIGH, new Flag() {
				@Override
				public ActionResult enters(EntryEvent event) {
					connect(event.getPlayer());
					return super.enters(event);
				}
			});
			hologram = OlympaCore.getInstance().getHologramsManager().createHologram(holoLocation.clone().add(0, 1, 0), false, true, new FixedLine<>("§e§l"
					+ getServerNameCaps()), FixedLine.EMPTY_LINE, new DynamicLine<>(x -> getOnlineString(), ServerInfoItem.this));
		}

		public void destroy() {
			region.unregister();
			hologram.remove();
		}

	}

	public Boolean hasPermissionToJoin(OlympaPlayer player) {
		Boolean out = null;
		MonitorInfo monitorInfo = null;
		for (Iterator<MonitorInfo> it = serversInfo.values().iterator(); it.hasNext(); monitorInfo = it.next()) {
			if (monitorInfo == null)
				continue;
			boolean b = hasPermissionToJoin(player, monitorInfo);
			if (b)
				return true;
			else if (out == null && !b)
				out = false;
		}
		return out;
	}

	public boolean hasPermissionToJoin(OlympaPlayer player, MonitorInfo minitorServer) {
		return minitorServer.getOlympaServer().canConnect(player);
	}

	private void setServerItem(OlympaPlayer player, Inventory inv) {
		//		ItemStack menuItem = getMenuItem();
		//		ItemStack currentItem = inv.getItem(slot);
		//		if (currentItem != null)
		//			return;
		Boolean hasPerm = hasPermissionToJoin(player);
		if (hasPerm == null)
			inv.setItem(slot, ItemUtils.item(item, "§6§l" + getServerNameCaps(), "§eChargement ..."));
		else if (hasPerm == true)
			inv.setItem(slot, getMenuItem());
		else
			inv.setItem(slot, new ItemStack(Material.AIR));
	}

	private void printItem(OlympaPlayer player, Inventory inv, Boolean canJoin) {
		setServerItem(player, inv);
		observe("gui_" + slot + itemServerKey + player.getUniqueId(), () -> setServerItem(player, inv));
	}

	public void printItem(OlympaPlayer player, Inventory inv, boolean isInstantData) {
		//		if (!isInstantData)
		//		return;
		Boolean canJoin = this.hasPermissionToJoin(player);
		printItem(player, inv, canJoin);
	}

}
