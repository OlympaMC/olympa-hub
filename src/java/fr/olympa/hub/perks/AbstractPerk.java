package fr.olympa.hub.perks;

import java.util.HashSet;
import java.util.Set;

import fr.olympa.api.common.permission.OlympaSpigotPermission;
import fr.olympa.hub.minigames.utils.OlympaPlayerHub;

public abstract class AbstractPerk {
	
	private OlympaSpigotPermission permission;
	
	private Set<OlympaPlayerHub> enabled = new HashSet<>();
	
	protected AbstractPerk(OlympaSpigotPermission permission) {
		this.permission = permission;
	}
	
	public OlympaSpigotPermission getPermission() {
		return permission;
	}
	
	public void start() {}
	
	public void stop() {}
	
	public boolean isEnabled(OlympaPlayerHub player) {
		return enabled.contains(player);
	}
	
	public boolean enable(OlympaPlayerHub player) {
		return enabled.add(player);
	}
	
	public boolean disable(OlympaPlayerHub player) {
		return enabled.remove(player);
	}
	
	public Set<OlympaPlayerHub> getPlayersEnabled() {
		return enabled;
	}
	
}
