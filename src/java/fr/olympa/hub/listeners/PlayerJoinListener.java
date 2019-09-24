package fr.olympa.hub.listeners;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;

import fr.olympa.hub.gui.GuisHub;
import fr.tristiisch.olympa.api.title.Title;
import fr.tristiisch.olympa.api.utils.SpigotUtils;

public class PlayerJoinListener implements Listener {

	public void init(final Player player) {
		SpigotUtils.clearPlayer(player);
		player.setGameMode(GameMode.ADVENTURE);
		for (final PotionEffect effect : player.getActivePotionEffects()) {
			player.removePotionEffect(effect.getType());
		}
		player.setMaxHealth(2);
		player.setHealth(player.getMaxHealth());
		player.setWalkSpeed(0.2f);
		player.setAllowFlight(false);
		player.setFlying(false);
		player.setFlySpeed(0.5f);
		player.setFoodLevel(20);
		player.setExp(0);
		player.setCanPickupItems(false);
		GuisHub.hotbar(player);
		// player.teleport(OlympaSpigot.getSpawn());
	}

	@EventHandler
	public void onPlayerJoin(final PlayerJoinEvent event) {
		final Player player = event.getPlayer();
		Title.sendTitle(player, "&4[&cBETA&4] &6Olympa", "&eGta & PvPFaction", 0, 60, 10);
		this.init(player);
	}

	@EventHandler
	public void onPlayerQuit(final PlayerQuitEvent event) {
		final Player player = event.getPlayer();
		this.init(player);
	}

	@EventHandler
	public void onPlayerRespawn(final PlayerRespawnEvent event) {
		final Player player = event.getPlayer();
		// event.setRespawnLocation(OlympaSpigot.getSpawn());
		this.init(player);
	}

}
