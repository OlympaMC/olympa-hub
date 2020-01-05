package fr.olympa.hub.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import fr.olympa.api.gui.GuiHandler;
import fr.olympa.api.gui.OlympaGUI;
import fr.olympa.api.item.OlympaItemBuild;
import fr.olympa.api.objects.OlympaPlayer;
import fr.olympa.api.provider.AccountProvider;

public class GuiMenu extends OlympaGUI {

	private static ItemStack ITEM_ZTA = new OlympaItemBuild(Material.DIAMOND_SWORD, "&6ZTA").flag(ItemFlag.HIDE_ATTRIBUTES).lore("", "&7Le capitaliste avant tout.").build();
	private static ItemStack ITEM_PVFAC = new OlympaItemBuild(Material.DIAMOND_SWORD, "&6PvP Faction").flag(ItemFlag.HIDE_ATTRIBUTES).lore("", "&7Crée ta grande famille.", "", "&4En développement").build();

	public GuiMenu(Player p) {
		super("&6Olympa &f| &7Menu", 3);
		int middle = inv.getSize() / 2;

		inv.setItem(middle - 1, ITEM_ZTA);
		inv.setItem(middle + 1, ITEM_PVFAC);

		OlympaPlayer olympaPlayer = AccountProvider.get(p);
		OlympaItemBuild item = new OlympaItemBuild("&7").lore("", "&6Grade: &e").skullowner(p);
		item.addName(p.getName());
		item.setLore(1, olympaPlayer.getGroupsToHumainString());
		inv.setItem(inv.getSize() - 1, item.build());
	}

	public boolean onClick(Player p, ItemStack current, int slot, ClickType click) {
		if (ITEM_ZTA.isSimilar(current)) {
			GuiHandler.cancelItem(p, current, getInventory(), slot);
		}else if (ITEM_PVFAC.isSimilar(current)) {
			GuiHandler.cancelItem(p, current, getInventory(), slot);
		}
		return true;
	}

}
