package fr.olympa.hub.pads;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import fr.olympa.api.spigot.utils.SpigotUtils;

public class LaunchPad implements ConfigurationSerializable {
	
	private Location location;
	
	private Vector vector;
	private Operation operation;
	
	public LaunchPad(Location location, Vector vector, Operation operation) {
		this.location = location;
		this.vector = vector;
		this.operation = operation;
	}
	
	public Location getLocation() {
		return location;
	}

	public void step(Player p) {
		p.setVelocity(getNewVelocity(p));
		p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 1);
		p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation().add(0, 0.5, 0), 5, 0.2f, 0.2f, 0.2f);
	}
	
	private Vector getNewVelocity(Player p) {
		switch (operation) {
		case MULTIPLY_DIRECTION:
			return p.getLocation().getDirection().multiply(vector);
		case MULTIPLY_VELOCITY:
			return p.getVelocity().multiply(vector);
		case SET_VELOCITY:
			return vector;
		default:
			return null;
		}
	}
	 
	public Map<String, Object> serialize() {
		Map<String, Object> map = new HashMap<>();
		
		map.put("location", SpigotUtils.convertLocationToString(location));
		map.put("operation", operation.name());
		map.put("vector", vector);
		
		return map;
	}
	
	public static LaunchPad deserialize(Map<String, Object> map) {
		return new LaunchPad(SpigotUtils.convertStringToLocation((String) map.get("location")), (Vector) map.get("vector"), Operation.valueOf((String) map.get("operation")));
	}
	
}
