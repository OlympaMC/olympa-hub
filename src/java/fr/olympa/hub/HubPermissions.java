package fr.olympa.hub;

import fr.olympa.api.groups.OlympaGroup;
import fr.olympa.api.permission.OlympaPermission;

public class HubPermissions {

	public static final OlympaPermission SERVER_CONFIG_COMMAND = new OlympaPermission(OlympaGroup.RESP_TECH);

	public static final OlympaPermission SPAWN_COMMAND = new OlympaPermission(OlympaGroup.PLAYER);

}