package fr.olympa.hub.perks.particles;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.craftbukkit.v1_16_R3.CraftParticle;

import fr.olympa.hub.HubPermissions;
import net.minecraft.server.v1_16_R3.PacketPlayOutWorldParticles;

public class PerkSurroundings extends ParticlePerk {
	
	public PerkSurroundings() {
		super(HubPermissions.PERK_FOOT_CLOUD);
	}
	
	@Override
	protected int getPeriod() {
		return 37;
	}
	
	@Override
	protected PacketPlayOutWorldParticles getParticlePacket(Location location) {
		return new PacketPlayOutWorldParticles(CraftParticle.toNMS(Particle.VILLAGER_HAPPY), false, location.getX(), location.getY() + 1, location.getZ(), 0.95f, 0.95f, 0.95f, 0.01f, 3);
	}
	
}
