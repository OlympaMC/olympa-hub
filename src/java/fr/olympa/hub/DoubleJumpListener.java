package fr.olympa.hub;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;

import fr.olympa.api.common.player.OlympaPlayer;
import fr.olympa.api.spigot.customevents.AsyncOlympaPlayerChangeGroupEvent;
import fr.olympa.api.spigot.customevents.OlympaPlayerLoadEvent;
import fr.olympa.api.spigot.utils.SpigotUtils;
import fr.olympa.hub.minigames.utils.MiniGamesManager;

public class DoubleJumpListener implements Listener {
	
	private Set<Player> players = new HashSet<>();
	
	@EventHandler
	public void onOlympaJoin(OlympaPlayerLoadEvent e) {
		if (HubPermissions.DOUBLE_JUMP.hasPermission(e.<OlympaPlayer>getOlympaPlayer())) {
			e.getPlayer().setAllowFlight(true);
		}
	}
	
	@EventHandler
	public void onGroupChange(AsyncOlympaPlayerChangeGroupEvent e) {
		e.getPlayer().setAllowFlight(HubPermissions.DOUBLE_JUMP.hasPermission(e.<OlympaPlayer>getOlympaPlayer()));
	}
	
	@EventHandler
	public void onToggleFly(PlayerToggleFlightEvent e) {
		Player p = e.getPlayer();
		if (p.getGameMode() != GameMode.CREATIVE) {
			e.setCancelled(true);
			if (MiniGamesManager.getInstance().isPlaying(e.getPlayer()) == null) {
				players.add(p);
				Location location = p.getLocation();
				location.setPitch(Math.min(location.getPitch(), 85));
				p.setAllowFlight(false);
				p.setVelocity(location.getDirection().multiply(1.5).setY(1));
				p.getWorld().playSound(location, Sound.ENTITY_BAT_TAKEOFF, 0.25f, 0.74f);
			}
		}
	}
	
	@EventHandler
	public void onMove(PlayerMoveEvent e) {
		Player p = e.getPlayer();
		if (!p.isFlying() && !SpigotUtils.isSameLocation(e.getFrom(), e.getTo()) && players.contains(p) && !SpigotUtils.isInAir(e.getTo())) {
			players.remove(p);
			p.setAllowFlight(true);
		}
	}
	
}
