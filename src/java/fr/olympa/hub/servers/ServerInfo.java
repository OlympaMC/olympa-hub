package fr.olympa.hub.servers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.ChatPaginator;
import org.jetbrains.annotations.NotNull;

import fr.olympa.api.holograms.Hologram;
import fr.olympa.api.item.ItemUtils;
import fr.olympa.api.lines.DynamicLine;
import fr.olympa.api.lines.FixedLine;
import fr.olympa.api.region.Region;
import fr.olympa.api.region.tracking.ActionResult;
import fr.olympa.api.region.tracking.TrackedRegion;
import fr.olympa.api.region.tracking.flags.Flag;
import fr.olympa.api.server.MonitorInfo;
import fr.olympa.api.server.OlympaServer;
import fr.olympa.api.server.ServerStatus;
import fr.olympa.api.utils.Prefix;
import fr.olympa.api.utils.Utils;
import fr.olympa.api.utils.observable.AbstractObservable;
import fr.olympa.api.utils.spigot.SpigotUtils;
import fr.olympa.core.spigot.OlympaCore;
import fr.olympa.core.spigot.redis.RedisSpigotSend;
import fr.olympa.hub.OlympaHub;

public class ServerInfo extends AbstractObservable {

	private static final String SEPARATOR = "§8§m------------------------------------";

	@Nullable
	private final String servName;
	private OlympaServer server;
	public final List<String> description;
	public final Material item;
	public final int slot;

	@Deprecated
	private int online;
	@Deprecated
	private ServerStatus status = ServerStatus.UNKNOWN;
	@Nullable
	private MonitorInfo info;

	private ItemStack menuItem = ItemUtils.error;
	private Portal portal;
	@NotNull
	private ConfigurationSection config;

	public ServerInfo(String servName, ConfigurationSection config) {
		this.servName = servName;
		description = Arrays.asList(ChatPaginator.wordWrap("§8> §7" + config.getString("description"), 40));
		item = Material.valueOf(config.getString("item"));
		slot = config.getInt("slot");
		if (config.contains("portal"))
			portal = new Portal(config.getConfigurationSection("portal"));
	}

	@Deprecated
	public ServerInfo(OlympaServer server, ConfigurationSection config) {
		this((String) null, config);
		this.server = server;
	}

	public ServerInfo(MonitorInfo monitorServer, ConfigurationSection config) {
		this(monitorServer.getName(), config);
		setInfo(monitorServer);
	}

	public MonitorInfo getInfo() {
		return info;
	}

	public void setInfo(MonitorInfo info) {
		this.info = info;
		server = info.getOlympaServer();
		update(info.getOnlinePlayers(), info.getStatus());
	}

	public OlympaServer getServer() {
		return server;
	}

	@Deprecated
	public String getServerName() {
		if (servName != null)
			return servName;
		return server.getNameCaps().toLowerCase();
	}

	public String getServerNameCaps() {
		if (servName != null)
			return server.getNameCaps() + " n°" + servName.replaceFirst("^[A-Za-z]+", "");
		return server.getNameCaps();
	}

	public int getOnlinePlayers() {
		return online;
	}

	public ServerStatus getStatus() {
		return status;
	}

	public String getOnlineString() {
		return (online == -1 ? "§cx" : "§a§l" + online) + " §7joueur" + Utils.withOrWithoutS(online) + " en ligne";
	}

	public void update(int online, ServerStatus status) {
		if (this.online == online && this.status == status)
			return;
		this.status = status;
		this.online = status == ServerStatus.CLOSE ? -1 : online;

		List<String> lore = new ArrayList<>();
		lore.add(SEPARATOR);
		lore.addAll(description);
		lore.add(SEPARATOR);
		lore.add(getOnlineString());
		if (status != ServerStatus.OPEN)
			lore.add("§7Statut : " + status.getNameColored());
		menuItem = ItemUtils.item(item, "§6§l" + getServerNameCaps(), lore.toArray(new String[0]));
		ItemUtils.addEnchant(menuItem, Enchantment.DURABILITY, 0);

		update();
	}

	public ItemStack getMenuItem() {
		return menuItem;
	}

	public boolean connect(Player p) {
		if (status == ServerStatus.CLOSE)
			Prefix.DEFAULT_BAD.sendMessage(p, "Ce serveur est fermé. Réessaye plus tard !");
		else if (status.getPermission() == null || status.getPermission().hasPermission(p.getUniqueId())) {
			Prefix.DEFAULT_GOOD.sendMessage(p, "Tu vas être transféré au serveur %s sous peu !", getServer().getNameCaps());
			RedisSpigotSend.sendServerSwitch(p, getServer());
			return true;
		} else
			Prefix.DEFAULT_BAD.sendMessage(p, "Tu n'as pas la permission de te connecter à ce serveur.");
		return false;
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
			this.region = OlympaCore.getInstance().getRegionManager().registerRegion(region, "portal_" + getServer().name(), EventPriority.HIGH, new Flag() {
				@Override
				public ActionResult enters(Player p, Set<TrackedRegion> to) {
					connect(p);
					return super.enters(p, to);
				}
			});
			hologram = OlympaCore.getInstance().getHologramsManager().createHologram(holoLocation.clone().add(0, 1, 0), false, true, new FixedLine<>("§e§l"
					+ getServerNameCaps()), FixedLine.EMPTY_LINE, new DynamicLine<>(x -> getOnlineString(), ServerInfo.this));
		}

		public void destroy() {
			region.unregister();
			hologram.remove();
		}

	}

}
