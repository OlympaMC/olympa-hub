package fr.olympa.hub.servers;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import fr.olympa.api.item.ItemUtils;
import fr.olympa.api.server.ServerStatus;

public class ServerInfo {

	private static final String SEPARATOR = "§8§n------------";

	public final String name;
	public final String title;
	public final String description;
	public final Material item;

	private ServerStatus status = ServerStatus.UNKNOWN;

	private ItemStack menuItem = ItemUtils.error;
	
	public ServerInfo(String name, String title, String description, Material item) {
		this.name = name;
		this.title = title;
		this.description = description;
		this.item = item;
	}

	public ServerStatus getStatus() {
		return status;
	}

	public void update(int online, int max, ServerStatus status) {
		this.status = status;

		menuItem = ItemUtils.item(item, "§6§l" + title, SEPARATOR, "§8> §7" + description, SEPARATOR, "§7§l" + online + " §7joueurs en ligne", "§eStatut : " + status.getNameColored());
	}

	public ItemStack getMenuItem() {
		return menuItem;
	}

}
