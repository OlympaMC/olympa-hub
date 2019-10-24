package fr.olympa.hub.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import fr.olympa.api.item.OlympaItemBuild;

public class ItemHotbar {

	public static OlympaItemBuild ITEM_COMPASS = new OlympaItemBuild(Material.COMPASS, "&6Menu");

	public static void set(Player player) {
		OlympaItemBuild item = ITEM_COMPASS;
		player.getInventory().setItem(4, item.build());
	}
}
