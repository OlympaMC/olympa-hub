package fr.olympa.hub.perks.particles;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.craftbukkit.v1_16_R3.CraftParticle;
import org.bukkit.entity.Player;

import fr.olympa.hub.HubPermissions;
import net.minecraft.server.v1_16_R3.PacketPlayOutWorldParticles;

public class PerkFootCloud extends ParticlePerk {
	
	public PerkFootCloud() {
		super(HubPermissions.PERK_FOOT_CLOUD);
	}
	
	@Override
	protected int getPeriod() {
		return 3;
	}
	
	@Override
	protected boolean isValid(Player p) {
		return super.isValid(p) && !p.isOnGround();
	}
	
	@Override
	protected PacketPlayOutWorldParticles getParticlePacket(Location location) {
		return new PacketPlayOutWorldParticles(CraftParticle.toNMS(Particle.CLOUD), false, location.getX(), location.getY() - 0.08, location.getZ(), 0.35f, 0.02f, 0.35f, 0.01f, 16);
	}
	
}
