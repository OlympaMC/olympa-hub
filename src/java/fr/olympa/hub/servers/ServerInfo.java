package fr.olympa.hub.servers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.ChatPaginator;

import fr.olympa.api.item.ItemUtils;
import fr.olympa.api.server.ServerStatus;

public class ServerInfo {

	private static final String SEPARATOR = "§8§m------------";

	public final String name;
	public final String title;
	public final List<String> description;
	public final Material item;

	private ServerStatus status = ServerStatus.UNKNOWN;

	private ItemStack menuItem = ItemUtils.error;
	
	public ServerInfo(String name, String title, String description, Material item) {
		this.name = name;
		this.title = title;
		this.description = Arrays.asList(ChatPaginator.wordWrap("§8> §7" + description, 40));
		this.item = item;
	}

	public ServerStatus getStatus() {
		return status;
	}

	public void update(int online, int max, ServerStatus status) {
		this.status = status;

		List<String> lore = new ArrayList<>();
		lore.add(SEPARATOR);
		lore.addAll(description);
		lore.add(SEPARATOR);
		lore.add("§7§l" + (online == -1 ? "§cx" : online) + " §7joueurs en ligne");
		lore.add("§7Statut : " + status.getNameColored());
		menuItem = ItemUtils.item(item, "§6§l" + title, lore.toArray(new String[0]));
	}

	public ItemStack getMenuItem() {
		return menuItem;
	}

}
