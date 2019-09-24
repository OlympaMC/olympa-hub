package fr.olympa.hub.listeners;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class DamageListener implements Listener {

	@EventHandler
	public void EntityDamageByEntityEvent(final EntityDamageByEntityEvent event) {
		if (event.isCancelled() || !(event.getEntity() instanceof Player)) {
			return;
		}
		if (event.getDamager() instanceof Player) {

			final Player attacker = (Player) event.getDamager();

			attacker.playSound(attacker.getLocation(), Sound.VILLAGER_NO, 1, 1);
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void EntityDamageEvent(final EntityDamageEvent event) {
		if (event.isCancelled() || !(event.getEntity() instanceof Player)) {
			return;
		}
		if (event.getCause() != DamageCause.SUICIDE) {
			event.setCancelled(true);
		}
	}

}
