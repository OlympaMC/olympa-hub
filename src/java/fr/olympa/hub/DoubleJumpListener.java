package fr.olympa.hub;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.util.Vector;

import fr.olympa.api.common.player.OlympaPlayer;
import fr.olympa.api.spigot.customevents.AsyncOlympaPlayerChangeGroupEvent;
import fr.olympa.api.spigot.customevents.OlympaPlayerLoadEvent;
import fr.olympa.api.spigot.utils.SpigotUtils;

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
		e.getPlayer().setAllowFlight(HubPermissions.DOUBLE_JUMP.hasPermission(e.getOlympaPlayer()));
	}
	
	@EventHandler
	public void onToggleFly(PlayerToggleFlightEvent e) {
		Player p = e.getPlayer();
		if (p.getGameMode() != GameMode.CREATIVE) {
			e.setCancelled(true);
			players.add(p);
			Vector direction = p.getLocation().getDirection();
			p.setVelocity(direction.multiply(1.5).setY(1));
			p.playSound(p.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1.0f, -5.0f);
			p.setAllowFlight(false);
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
