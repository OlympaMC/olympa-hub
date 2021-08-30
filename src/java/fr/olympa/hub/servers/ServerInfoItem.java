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
import java.util.StringJoiner;
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

import fr.olympa.api.common.match.MatcherPattern;
import fr.olympa.api.common.observable.AbstractObservable;
import fr.olympa.api.common.permission.OlympaPermission;
import fr.olympa.api.common.player.OlympaPlayer;
import fr.olympa.api.common.provider.AccountProviderAPI;
import fr.olympa.api.common.redis.RedisClass;
import fr.olympa.api.common.server.ServerInfoAdvanced;
import fr.olympa.api.common.server.ServerInfoAdvanced.ConnectResult;
import fr.olympa.api.common.server.ServerStatus;
import fr.olympa.api.common.sort.Sorting;
import fr.olympa.api.spigot.holograms.Hologram;
import fr.olympa.api.spigot.item.ItemUtils;
import fr.olympa.api.spigot.lines.DynamicLine;
import fr.olympa.api.spigot.lines.FixedLine;
import fr.olympa.api.spigot.region.Region;
import fr.olympa.api.spigot.region.tracking.ActionResult;
import fr.olympa.api.spigot.region.tracking.RegionEvent.EntryEvent;
import fr.olympa.api.spigot.region.tracking.TrackedRegion;
import fr.olympa.api.spigot.region.tracking.flags.Flag;
import fr.olympa.api.spigot.utils.SpigotUtils;
import fr.olympa.api.utils.Prefix;
import fr.olympa.api.utils.Utils;
import fr.olympa.core.spigot.OlympaCore;
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
	private List<ItemStack> items = new ArrayList<>();
	private Portal portal;
	@Nonnull
	private ConfigurationSection config;
	private Map<String, ServerInfoAdvanced> serversInfo = new TreeMap<>();
	private boolean isUniqueMultipleServers;

	public ServerInfoItem(String itemServerKey, ConfigurationSection config) {
		this.itemServerKey = itemServerKey;
		try {
			updateConfig(config);
		} catch (Exception e) {
			e.addSuppressed(new Throwable("Can't load server item " + itemServerKey + " from config"));
			e.printStackTrace();
		}
	}

	public Optional<Entry<String, ServerInfoAdvanced>> getServer(String servName) {
		return serversInfo.entrySet().stream().filter(e -> e.getKey().equals(servName)).findFirst();
	}

	public Set<Entry<String, ServerInfoAdvanced>> getServers() {
		return serversInfo.entrySet();
	}

	public Collection<ServerInfoAdvanced> getServersInfo() {
		return serversInfo.values();
	}

	public boolean containsMinimumOneServer(List<ServerInfoAdvanced> monitorInfos) {
		return monitorInfos.stream().anyMatch(monitorInfo -> serversInfo.keySet().stream().anyMatch(bungeeServerName -> monitorInfo.getName().equals(bungeeServerName)));
	}

	private void updateConfig(ConfigurationSection config) {
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

	public ServerInfoAdvanced getInfo(String bungeeServerName) {
		return serversInfo.get(bungeeServerName);
	}

	private boolean tryUpdate(List<ServerInfoAdvanced> mis) {
		boolean b = false;
		for (ServerInfoAdvanced mi : mis) {
			Optional<Entry<String, ServerInfoAdvanced>> oldServ = getServer(mi.getName());
			if (oldServ.isEmpty())
				continue;
			Entry<String, ServerInfoAdvanced> get = oldServ.get();
			if (get.getValue() == null || !mi.equals(get.getValue())) {
				serversInfo.put(mi.getName(), mi);
				b = true;
			}
		}
		return b;
	}

	public void createMenuChooseItem() {
		items.clear();
		for (ServerInfoAdvanced mi : serversInfo.values()) {
			List<String> lore = new ArrayList<>();
			lore.add(SEPARATOR);
			lore.addAll(description);
			lore.add(SEPARATOR);
			if (mi != null && mi.hasMinimalInfo()) {
				StringJoiner sj = new StringJoiner(" ");
				ItemStack serverChooseItem;
				lore.add(sj.toString());
				if (mi.getStatus() != ServerStatus.OPEN)
					lore.add(mi.getStatus().getNameColored());
				if (mi.getOnlinePlayers() != null && mi.getOnlinePlayers() > 0) {
					int online = mi.getOnlinePlayers();
					lore.add(String.format("§7%s joueur%s", online, Utils.withOrWithoutS(online)));
				}
				if (mi.hasInfoVersions())
					lore.add("§7" + mi.getRangeVersionMinecraft());
				serverChooseItem = ItemUtils.item(item, "§6§l" + mi.getHumanName(), lore.toArray(new String[0]));
				ItemUtils.addEnchant(serverChooseItem, Enchantment.DURABILITY, 0);
				items.add(serverChooseItem);
			}
		}
	}

	public void createMenuItem() {
		Collection<ServerInfoAdvanced> monitorInfo = serversInfo.values();
		List<String> lore = new ArrayList<>();
		lore.add(SEPARATOR);
		lore.addAll(description);
		lore.add(SEPARATOR);
		lore.add(getOnlineString());
		ServerStatus status = monitorInfo.stream().filter(mi -> mi != null).sorted(new Sorting<>(mi -> mi.getStatus().getId(), true)).map(ServerInfoAdvanced::getStatus).findFirst().orElse(ServerStatus.UNKNOWN);
		if (status != ServerStatus.OPEN)
			lore.add("§7Statut : " + status.getNameColored());
		lore.add("");
		monitorInfo.forEach(mi -> {
			if (mi != null && mi.hasMinimalInfo()) {
				StringJoiner sj = new StringJoiner(" ");
				sj.add("§7" + mi.getHumanName());
				if (mi.getStatus() != ServerStatus.OPEN)
					sj.add("(" + mi.getStatus().getNameColored() + "§7)");
				if (mi.getOnlinePlayers() != null && mi.getOnlinePlayers() > 0) {
					int online = mi.getOnlinePlayers();
					sj.add(String.format("- %s joueur%s", online, Utils.withOrWithoutS(online)));
				}
				if (mi.hasInfoVersions())
					sj.add(mi.getRangeVersionMinecraft());
				lore.add(sj.toString());
			}
		});
		lore.add("");
		lore.add("§3CLIQUE GAUCHE §bConnexion directe");
		lore.add("§8CLIQUE  DROIT §7Choix du serveur");
		menuItem = ItemUtils.item(item, "§6§l" + getServerNameCaps(), lore.toArray(new String[0]));
		ItemUtils.addEnchant(menuItem, Enchantment.DURABILITY, 0);
	}

	public void update(List<ServerInfoAdvanced> newMonitorInfo) {
		tryUpdate(newMonitorInfo);
		//		if (!tryUpdate(newMonitorInfo))
		//			return;
		createMenuItem();
		createMenuChooseItem();
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
			ServerInfoAdvanced oneServerInfo = serversInfo.values().stream().filter(mi -> mi != null).findFirst().orElse(null);
			if (oneServerInfo != null)
				//				if (serversInfo.size() == 1)
				//					return oneServerInfo.getHumanName();
				if (serversInfo.values().stream().allMatch(mi -> mi != null && mi.getOlympaServer().isSame(oneServerInfo.getOlympaServer())))
					return oneServerInfo.getOlympaServer().getNameCaps();
		}
		if (defaultServersName != null)
			return defaultServersName;
		return "Serveur Inconnu";
	}

	public Integer getTotalOnlinePlayer() {
		Integer online = null;
		for (ServerInfoAdvanced mi : serversInfo.values())
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
		Set<ServerInfoAdvanced> serversNotOff = serversInfo.values().stream().filter(mi -> mi != null && !mi.getStatus().equals(ServerStatus.CLOSE)).collect(Collectors.toSet());
		if (serversNotOff.isEmpty()) {
			Prefix.DEFAULT_BAD.sendMessage(p, "Ce serveur est fermé. Réessaye plus tard !");
			return false;
		}
		List<ServerInfoAdvanced> serverCanConnect = serversNotOff.stream().filter(mi -> {
			OlympaPermission permission = mi.getStatus().getPermission();
			return permission == null || permission.hasPermission(p.getUniqueId());
		}).sorted(new Sorting<>(true, mi -> mi.getStatus().getId(), ServerInfoAdvanced::getServerId)).collect(Collectors.toList());
		if (serverCanConnect.isEmpty()) {
			Prefix.DEFAULT_BAD.sendMessage(p, "Tu n'as pas la permission de te connecter à ce serveur.");
			return false;
		}
		// TODO choose between multiple serveurs with isUniqueMultipleServers & onlines count of player
		ServerInfoAdvanced serverInfo = serverCanConnect.get(0);
		Prefix.DEFAULT_GOOD.sendMessage(p, "Tu vas être transféré au serveur %s sous peu !", Utils.capitalize(serverInfo.getName()));
		RedisClass.SERVER_SWITCH.sendServerSwitch(p, serverInfo.getName());
		//		RedisSpigotSend.sendServerSwitch(p, serverInfo.getName());
		return true;
	}

	public boolean connect(Player p, ServerInfoAdvanced serverInfo) {
		OlympaPlayer olympaPlayer = AccountProviderAPI.getter().get(p.getUniqueId());
		ConnectResult connectResult = serverInfo.canConnectNew(olympaPlayer);
		switch (connectResult) {
		case BAD_VERSION:
			Prefix.DEFAULT_BAD.sendMessage(p, "Tu dois avoir une version dans &4%s&c. Tu utilise la version &4%s&c.", serverInfo.getRangeVersionMinecraft(), olympaPlayer.getProtocolName());
			return false;
		case NO_PERM:
			Prefix.DEFAULT_BAD.sendMessage(p, "Tu n'as pas la permission de te connecter à ce serveur.");
			return false;
		case NO_PERM_STATUS:
			Prefix.DEFAULT_BAD.sendMessage(p, "Ce serveur est actuellement en maintenance.");
			return false;
		case OFF, ERROR:
			Prefix.DEFAULT_BAD.sendMessage(p, "Ce serveur est fermé. Réessaye plus tard !");
		return false;
		case GOOD:
		default:
			Prefix.DEFAULT_GOOD.sendMessage(p, "Tu vas être transféré au serveur %s sous peu !", Utils.capitalize(serverInfo.getName()));
		}
		RedisClass.SERVER_SWITCH.sendServerSwitch(p, serverInfo.getName());
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

	public boolean hasPermissionToJoin(OlympaPlayer player) {
		boolean out = true;
		ServerInfoAdvanced monitorInfo = null;
		for (Iterator<ServerInfoAdvanced> it = serversInfo.values().iterator(); it.hasNext(); monitorInfo = it.next()) {
			if (monitorInfo == null)
				continue;
			boolean b = hasPermissionToJoin(player, monitorInfo);
			if (b)
				return true;
			else
				out = false;
		}
		return out;
	}

	public boolean hasPermissionToJoin(OlympaPlayer player, ServerInfoAdvanced minitorServer) {
		return minitorServer.getOlympaServer().canConnect(player);
	}

	private void setServerMenuItem(OlympaPlayer player, Inventory inv) {
		//		ItemStack menuItem = getMenuItem();
		//		ItemStack currentItem = inv.getItem(slot);
		//		if (currentItem != null)
		//			return;
		boolean hasPerm = hasPermissionToJoin(player);
		/*if (hasPerm == null)
			inv.setItem(slot, ItemUtils.item(item, "§6§l" + getServerNameCaps(), "§eChargement ..."));
		else */
		if (hasPerm)
			inv.setItem(slot, getMenuItem());
		else
			inv.setItem(slot, new ItemStack(Material.AIR));
	}

	private void setServerChooseItem(OlympaPlayer player, Inventory inv, int slot, ServerInfoAdvanced server) {
		boolean hasPerm = hasPermissionToJoin(player, server);
		if (hasPerm)
			inv.setItem(slot, items.get(slot));
		else
			inv.setItem(slot, new ItemStack(Material.AIR));
	}

	private void printItem(OlympaPlayer player, Inventory inv) {
		setServerMenuItem(player, inv);
		observe("gui_" + slot + itemServerKey + player.getUniqueId(), () -> setServerMenuItem(player, inv));
	}

	public void printItemChooseItem(OlympaPlayer player, Inventory inv, int slot, ServerInfoAdvanced server, boolean isInstantData) {
		setServerChooseItem(player, inv, slot, server);
		observe("gui_" + slot + server.getName() + player.getUniqueId(), () -> setServerChooseItem(player, inv, slot, server));
	}

	public void printMenuItem(OlympaPlayer player, Inventory inv, boolean isInstantData) {
		//		if (!isInstantData)
		//		return;
		//		Boolean canJoin = this.hasPermissionToJoin(player);
		printItem(player, inv);
	}

}
