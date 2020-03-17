package fr.olympa.hub.listeners;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;

import fr.olympa.api.objects.OlympaServerSettings;
import fr.olympa.api.utils.SpigotUtils;
import fr.olympa.hub.OlympaHub;
import fr.olympa.hub.gui.ItemHotbar;
import fr.olympa.hub.task.TitleTask;

public class PlayerJoinListener implements Listener {

	{
		Bukkit.getOnlinePlayers().forEach(player -> this.join(player));
	}

	@SuppressWarnings ("deprecation")
	public void init(Player player) {

		/*OlympaPlayer p = AccountProvider.get(player.getUniqueId());
		
		if (p.getLastConnection() == 0)FireWorkUtils.spawnWelcomeFireworks(player.getLocation());*/
		SpigotUtils.clearPlayer(player);
		player.setGameMode(GameMode.ADVENTURE);
		for (PotionEffect effect : player.getActivePotionEffects()) {
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
		ItemHotbar.set(player);
		Location spawn = OlympaServerSettings.getInstance().getSpawn();
		if (spawn != null) {
			player.teleport(spawn);
		}
	}

	public void join(Player player) {
		new TitleTask(player).runTaskTimer(OlympaHub.getInstance(), 6, 6);
		this.init(player);
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		this.join(player);
		event.setJoinMessage(null);
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		this.init(player);
	}

	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		Player player = event.getPlayer();
		this.init(player);
	}

}
