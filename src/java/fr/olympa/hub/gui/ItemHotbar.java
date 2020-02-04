package fr.olympa.hub.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import fr.olympa.api.item.OlympaItemBuild;

public class ItemHotbar {

	public static OlympaItemBuild ITEM_COMPASS = new OlympaItemBuild(Material.CHEST, "&6Menu");
	public static OlympaItemBuild ITEM_PROFILE = new OlympaItemBuild("&aProfile");

	public static void set(Player player) {
		OlympaItemBuild item1 = ITEM_COMPASS;
		OlympaItemBuild item2 = ITEM_PROFILE;
		item2.skullowner(player);
		player.getInventory().setItem(2, item1.build());
		player.getInventory().setItem(6, item2.build());

	}
}
