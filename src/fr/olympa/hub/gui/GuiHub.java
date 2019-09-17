package fr.olympa.hub.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;

import fr.tristiisch.olympa.api.gui.OlympaGuiBuild;
import fr.tristiisch.olympa.api.item.OlympaItemBuild;

public class GuiHub {

	public static OlympaItemBuild COMPASS = new OlympaItemBuild(Material.COMPASS, "&6Menu");

	public static void hotbar(Player player) {
		OlympaItemBuild item = COMPASS;
		player.getInventory().setItem(4, item.build());
	}

	public static void mainGui(Player player) {
		OlympaGuiBuild gui = new OlympaGuiBuild("&6Olympa &f| &7Menu", "olympa.menu", 4);
		OlympaItemBuild item = new OlympaItemBuild(Material.DIAMOND_SWORD, "&6Gta").flag(ItemFlag.HIDE_ATTRIBUTES).lore("", "&7Un serveur de guerre intense");
		gui.setItem(gui.getMiddleSlot(), item.build());

		gui.openInventory(player);
	}
}
