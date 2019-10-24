package fr.olympa.hub.listeners;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class ProtectListener implements Listener {

	private static boolean canceldrop = true;

	public static void disableCancelDrop() {
		canceldrop = false;
	}

	@EventHandler(ignoreCancelled = false)
	private void BlockBreakEvent(BlockBreakEvent event) {
		Player player = event.getPlayer();
		if (!this.canI(player)) {
			player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = false)
	private void BlockPlaceEvent(BlockPlaceEvent event) {
		Player player = event.getPlayer();
		if (!this.canI(player)) {
			player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
			event.setCancelled(true);
		}
	}

	private boolean canI(Player player) {
		return player.getGameMode() == GameMode.CREATIVE;
	}

	@EventHandler(ignoreCancelled = false)
	private void EntityDamageByEntityEvent(EntityDamageByEntityEvent event) {
		if (event.getDamager() instanceof Player) {
			if (!(event.getEntity() instanceof Player)) {
				if (!this.canI((Player) event.getDamager())) {
					((Player) event.getDamager()).updateInventory();
					event.setCancelled(true);
				}
			}
		} else if (!(event.getEntity() instanceof Player)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = false)
	public void HangingBreakEvent(HangingBreakByEntityEvent event) {
		event.setCancelled(!(event.getEntity() instanceof Player) || !this.canI((Player) event.getEntity()));
	}

	@EventHandler(ignoreCancelled = false)
	public void HangingPlaceEvent(HangingPlaceEvent event) {
		event.setCancelled(!this.canI(event.getPlayer()));
	}

	@EventHandler(ignoreCancelled = false)
	private void PlayerDropItemEvent(PlayerDropItemEvent event) {
		if (!canceldrop) {
			return;
		}
		if (!this.canI(event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void PlayerEggThrowEvent(PlayerEggThrowEvent event) {
		if (!event.isHatching()) {
			return;
		}
		if (!this.canI(event.getPlayer())) {
			event.setHatching(false);
		}
	}

	@EventHandler(ignoreCancelled = false)
	private void PlayerInteractEntityEvent(PlayerInteractEntityEvent event) {
		if (!this.canI(event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void PlayerInteractEvent(PlayerInteractEvent event) {
		if (event.getAction().equals(Action.PHYSICAL) && event.getClickedBlock().getType().equals(Material.FARMLAND)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = false)
	private void PlayerPickupItemEvent(EntityPickupItemEvent event) {
		Entity entity = event.getEntity();
		if (!(entity instanceof Player) || !canceldrop) {
			return;
		}
		if (!this.canI((Player) entity)) {
			event.setCancelled(true);
		}
	}
}
