package fr.olympa.hub.perks.particles;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import fr.olympa.api.common.permission.OlympaSpigotPermission;
import fr.olympa.core.spigot.OlympaCore;
import fr.olympa.hub.OlympaHub;
import fr.olympa.hub.gui.VanishManager;
import fr.olympa.hub.perks.AbstractPerk;
import net.minecraft.server.v1_16_R3.PacketPlayOutWorldParticles;

public abstract class ParticlePerk extends AbstractPerk {
	
	private BukkitTask task;
	
	protected ParticlePerk(OlympaSpigotPermission permission) {
		super(permission);
	}
	
	protected abstract int getPeriod();
	
	protected abstract PacketPlayOutWorldParticles getParticlePacket(Location location);
	
	protected boolean isValid(Player p) {
		return !OlympaCore.getInstance().getVanishApi().isVanished(p) && p.getGameMode() != GameMode.SPECTATOR && p.getPotionEffect(PotionEffectType.INVISIBILITY) == null;
	}
	
	@Override
	public void start() {
		super.start();
		task = Bukkit.getScheduler().runTaskTimerAsynchronously(OlympaHub.getInstance(), () -> {
			List<Player> enabled = getPlayersEnabled().stream().map(x -> (Player) x.getPlayer()).filter(p -> p.isOnline()).filter(this::isValid).toList();
			if (enabled.isEmpty()) return;
			List<Player> receivers = (List<Player>) new ArrayList<>(Bukkit.getOnlinePlayers()).stream().filter(p -> !OlympaHub.getInstance().vanishManager.isUsingVanish(p)).toList();
			for (Player player : enabled) {
				PacketPlayOutWorldParticles packet = getParticlePacket(player.getLocation());
				for (Player receiver : receivers) {
					if (receiver.getWorld() == player.getWorld()) ((CraftPlayer) receiver).getHandle().playerConnection.sendPacket(packet);
				}
			}
		}, 20, getPeriod());
	}
	
	@Override
	public void stop() {
		super.stop();
		if (task != null) task.cancel();
	}
	
}
