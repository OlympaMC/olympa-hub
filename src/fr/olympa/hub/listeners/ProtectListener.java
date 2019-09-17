package fr.olympa.hub.listeners;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class ProtectListener implements Listener {

	@EventHandler
	public void PlayerInteractEvent(final PlayerInteractEvent event) {
		if (event.getAction().equals(Action.PHYSICAL) && event.getClickedBlock().getType().equals(Material.SOIL)) {
			event.setCancelled(true);
		}
	}
}
