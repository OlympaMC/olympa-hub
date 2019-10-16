package fr.olympa.hub.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;

import fr.olympa.api.gui.OlympaGuiBuild;
import fr.olympa.api.item.OlympaItemBuild;

public class GuisHub {

	public static OlympaItemBuild COMPASS = new OlympaItemBuild(Material.COMPASS, "&6Menu");

	public static void hotbar(Player player) {
		OlympaItemBuild item = COMPASS;
		player.getInventory().setItem(4, item.build());
	}

	public static void mainGui(Player player) {
		OlympaGuiBuild gui = new OlympaGuiBuild("&6Olympa &f| &7Menu", GuiHub.MENU.toString(), 3);
		int slots = gui.getMiddleSlot();
		OlympaItemBuild item;

		item = new OlympaItemBuild(Material.DIAMOND_SWORD, "&6ZTA").flag(ItemFlag.HIDE_ATTRIBUTES).lore("", "&7Le capitaliste avant tout.");
		gui.setItem(slots - 1, item.build());

		item = new OlympaItemBuild(Material.DIAMOND_SWORD, "&6PvP Faction").flag(ItemFlag.HIDE_ATTRIBUTES).lore("", "&7Crée ta grande famille.", "", "&4En développement");
		gui.setItem(slots + 1, item.build());

		gui.openInventory(player);
	}
}
