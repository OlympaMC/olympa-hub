package fr.olympa.hub.servers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.ChatPaginator;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import fr.olympa.api.holograms.Hologram;
import fr.olympa.api.item.ItemUtils;
import fr.olympa.api.lines.DynamicLine;
import fr.olympa.api.lines.FixedLine;
import fr.olympa.api.region.Region;
import fr.olympa.api.region.tracking.TrackedRegion;
import fr.olympa.api.region.tracking.flags.Flag;
import fr.olympa.api.server.ServerStatus;
import fr.olympa.api.utils.Prefix;
import fr.olympa.api.utils.observable.AbstractObservable;
import fr.olympa.api.utils.spigot.SpigotUtils;
import fr.olympa.core.spigot.OlympaCore;
import fr.olympa.hub.OlympaHub;

public class ServerInfo extends AbstractObservable {

	private static final String SEPARATOR = "§8§m------------------------------------";

	public final String name;
	public final String title;
	public final List<String> description;
	public final Material item;

	private int online;
	private int max;
	private ServerStatus status = ServerStatus.UNKNOWN;

	private ItemStack menuItem = ItemUtils.error;
	private Portal portal;

	private ConfigurationSection config;

	public ServerInfo(String name, ConfigurationSection config) {
		this.name = name;
		this.config = config;
		this.title = config.getString("name");
		this.description = Arrays.asList(ChatPaginator.wordWrap("§8> §7" + config.getString("description"), 40));
		this.item = Material.valueOf(config.getString("item"));

		if (config.contains("portal")) portal = new Portal(config.getConfigurationSection("portal"));
	}

	public ServerStatus getStatus() {
		return status;
	}

	public void update(int online, int max, ServerStatus status) {
		if (this.online == online && this.max == max && this.status == status) return;
		this.online = online;
		this.max = max;
		this.status = status;

		List<String> lore = new ArrayList<>();
		lore.add(SEPARATOR);
		lore.addAll(description);
		lore.add(SEPARATOR);
		lore.add("§7§l" + (online == -1 ? "§cx" : online) + " §7joueurs en ligne");
		if (status != ServerStatus.OPEN) lore.add("§7Statut : " + status.getNameColored());
		menuItem = ItemUtils.item(item, "§6§l" + title, lore.toArray(new String[0]));

		update();
	}

	public ItemStack getMenuItem() {
		return menuItem;
	}

	public boolean connect(Player p) {
		if (status == ServerStatus.CLOSE) {
			Prefix.DEFAULT_BAD.sendMessage(p, "Ce serveur est fermé. Réessaye plus tard !");
		}else {
			if (status.getPermission() == null || status.getPermission().hasPermission(p.getUniqueId())) {
				Prefix.DEFAULT_GOOD.sendMessage(p, "Tu vas être transféré au serveur %s sous peu !", name);
				ByteArrayDataOutput out = ByteStreams.newDataOutput();
				out.writeUTF("Connect");
				out.writeUTF(name);
				p.sendPluginMessage(OlympaHub.getInstance(), "BungeeCord", out.toByteArray());
				return true;
			}else {
				Prefix.DEFAULT_BAD.sendMessage(p, "Tu n'as pas la permission de te connecter à ce serveur.");
			}
		}
		return false;
	}

	public void setPortal(Region region, Location holoLocation) {
		if (portal != null) portal.destroy();
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
			this.region = OlympaCore.getInstance().getRegionManager().registerRegion(region, "portal_" + name, EventPriority.HIGH, new Flag() {
				@Override
				public boolean enters(Player p, Set<TrackedRegion> to) {
					connect(p);
					return super.enters(p, to);
				}
			});
			this.hologram = new Hologram(holoLocation.add(0, 1, 0), new FixedLine<>("§e§l" + title), FixedLine.EMPTY_LINE, new DynamicLine<>((x) -> "§7§l" + (online == -1 ? "§cx" : online) + " §7joueurs en ligne", ServerInfo.this));
			OlympaHub.getInstance().holograms.addHologram(hologram);
		}

		public void destroy() {
			region.unregister();
			hologram.destroy();
		}

	}

}
