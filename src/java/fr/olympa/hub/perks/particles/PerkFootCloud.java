package fr.olympa.hub.perks.particles;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import fr.olympa.hub.HubPermissions;
import fr.olympa.hub.OlympaHub;
import fr.olympa.hub.minigames.utils.OlympaPlayerHub;
import fr.olympa.hub.perks.AbstractPerk;

public class PerkFootCloud extends AbstractPerk {
	
	private BukkitTask task;
	
	public PerkFootCloud() {
		super(HubPermissions.PERK_FOOT_CLOUD);
	}
	
	@Override
	public void start() {
		super.start();
		task = Bukkit.getScheduler().runTaskTimerAsynchronously(OlympaHub.getInstance(), () -> {
			for (OlympaPlayerHub player : getPlayersEnabled()) {
				Location location = ((Player) player.getPlayer()).getLocation();
				location.getWorld().spawnParticle(Particle.CLOUD, location.subtract(0, 0.08, 0), 23, 0.35, 0.02, 0.35, 0.1);
			}
		}, 20, 3);
	}
	
	@Override
	public void stop() {
		super.stop();
		if (task != null) task.cancel();
	}
	
}
