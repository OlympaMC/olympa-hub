package fr.olympa.hub.minigames.utils;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import fr.olympa.api.spigot.region.Region;
import fr.olympa.api.spigot.region.shapes.Cuboid;

public class ElytraPortal implements ConfigurationSerializable {

	private Region region;
	private int index;
	
	public ElytraPortal (Region reg, int index) {
		this.region = reg;
		this.index = index;
	}
	
	public Region getRegion() {
		return region;
	}
	
	public int getIndex() {
		return index;
	}
	
	@Override
	public Map<String, Object> serialize() {
		// TODO Auto-generated method stub
		return null;
	}

	public static ElytraPortal deserialize(Map<String, Object> map) {
		//return new ElytraPortal(c.gets);
		return null;
	}

}
