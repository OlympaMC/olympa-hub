package fr.olympa.hub;

import fr.olympa.api.common.groups.OlympaGroup;
import fr.olympa.api.common.permission.OlympaSpigotPermission;

public class HubPermissions {

	public static final OlympaSpigotPermission SERVER_CONFIG_COMMAND = new OlympaSpigotPermission(OlympaGroup.RESP_TECH);
	public static final OlympaSpigotPermission LAUNCHPADS_COMMAND = new OlympaSpigotPermission(OlympaGroup.DEV);

	public static final OlympaSpigotPermission SPAWN_COMMAND = new OlympaSpigotPermission(OlympaGroup.PLAYER);
 
	public static final OlympaSpigotPermission EDIT_MINIGAMES = new OlympaSpigotPermission(OlympaGroup.DEV);

	public static final OlympaSpigotPermission DOUBLE_JUMP = new OlympaSpigotPermission(OlympaGroup.VIP);
	
	public static final OlympaSpigotPermission PERK_FOOT_CLOUD = new OlympaSpigotPermission(OlympaGroup.VIP);
	
}
