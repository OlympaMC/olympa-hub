package fr.olympa.hub.gui;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import fr.olympa.api.utils.Prefix;
import fr.olympa.hub.OlympaHub;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.server.v1_16_R3.EntityHuman;
import net.minecraft.server.v1_16_R3.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_16_R3.PacketPlayOutNamedEntitySpawn;

public class VanishManager implements Listener {

	private Set<Player> playersUsingVanish = new HashSet<>();
	private Map<Player, Long> lastToogle = new HashMap<>();

	public VanishManager() {
		OlympaHub.getInstance().getServer().getPluginManager().registerEvents(this, OlympaHub.getInstance());
	}

	public boolean isUsingVanish(Player p) {
		return playersUsingVanish.contains(p);
	}

	/**
	 * Toogle vanish mode for player p
	 * @param p player to which hide others players
	 * @return true if action was executed, false otherwiee. May be false if player spams too fast the option
	 */
	public boolean toogleVanish(Player p) {
		if (System.currentTimeMillis() < lastToogle.get(p) + 5000) {
			Prefix.DEFAULT_BAD.sendMessage(p, "Attends un peu avant de réutiliser cette option !");
			return false;
		}

		boolean vanish = false;
		if (!playersUsingVanish.remove(p)) {
			playersUsingVanish.add(p);
			vanish = true;
		}

		Material material;
		if (vanish) {
			Prefix.DEFAULT_GOOD.sendMessage(p, "Les joueurs ont été cachés !");
			material = Material.ENDER_EYE;
			
			((CraftPlayer) p).getHandle().playerConnection.networkManager.channel.pipeline().addBefore("packet_handler", "hub_vanish", new ChannelDuplexHandler() {
				@Override
				public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
					if (msg instanceof PacketPlayOutNamedEntitySpawn packet) return;
					super.write(ctx, msg, promise);
				}
			});
		}else {
			Prefix.DEFAULT_GOOD.sendMessage(p, "Les joueurs sont réapparus !");
			material = Material.SUNFLOWER;
			((CraftPlayer) p).getHandle().playerConnection.networkManager.channel.pipeline().remove("hub_vanish");
		}
		
		sendPackets(p, Bukkit.getOnlinePlayers().stream().filter(x -> x != p).collect(Collectors.toList()), vanish);
		p.setCooldown(material, 5 * 20);
		
		lastToogle.put(p, System.currentTimeMillis());
		return true;
	}

	private void sendPackets(Player p, Collection<Player> players, boolean hide) {
		if (hide) {
			int[] ps = players.stream().mapToInt(target -> ((CraftPlayer) target).getHandle().getId()).toArray();
			
			((CraftPlayer) p).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityDestroy(ps));
		} else
			players.forEach(pp -> ((CraftPlayer) p).getHandle().playerConnection
					.sendPacket(new PacketPlayOutNamedEntitySpawn((EntityHuman) ((CraftEntity) pp).getHandle())));

	}

	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		lastToogle.put(e.getPlayer(), System.currentTimeMillis() - 10000);
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		lastToogle.remove(e.getPlayer());
	}
}
