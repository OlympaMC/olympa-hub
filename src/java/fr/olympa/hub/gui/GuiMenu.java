package fr.olympa.hub.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import fr.olympa.api.gui.CustomInventory;
import fr.olympa.api.gui.GuiHandler;
import fr.olympa.api.gui.OlympaGui;
import fr.olympa.api.item.OlympaItemBuild;
import fr.olympa.api.objects.OlympaPlayer;
import fr.olympa.api.provider.AccountProvider;

public class GuiMenu implements CustomInventory {

	private ItemStack ITEM_ZTA = new OlympaItemBuild(Material.DIAMOND_SWORD, "&6ZTA").flag(ItemFlag.HIDE_ATTRIBUTES).lore("", "&7Le capitaliste avant tout.").build();
	private ItemStack ITEM_PVFAC = new OlympaItemBuild(Material.DIAMOND_SWORD, "&6PvP Faction").flag(ItemFlag.HIDE_ATTRIBUTES).lore("", "&7Crée ta grande famille.", "", "&4En développement").build();
	private OlympaItemBuild ITEM_INFO = new OlympaItemBuild("&7").lore("", "&6Grade: &e");

	@Override
	public boolean onClick(Player p, OlympaGui inv, ItemStack current, int slot, ClickType click) {
		if (this.ITEM_ZTA.isSimilar(current)) {
			GuiHandler.cancelItem(p, current, inv.getInventory(), slot);
		} else if (this.ITEM_PVFAC.isSimilar(current)) {
			GuiHandler.cancelItem(p, current, inv.getInventory(), slot);
		}
		return false;
	}

	public void open(Player player) {
		OlympaGui gui = new OlympaGui("&6Olympa &f| &7Menu", 3);
		int slots = gui.getMiddleSlot();

		gui.setItem(slots - 1, this.ITEM_ZTA);
		gui.setItem(slots + 1, this.ITEM_PVFAC);

		OlympaItemBuild item = this.ITEM_INFO.skullowner(player);
		OlympaPlayer olympaPlayer = AccountProvider.get(player);
		item.addName(player.getName());
		item.addLore(1, olympaPlayer.getGroupsToHumainString());
		gui.setItem(gui.getLastSlot(), item.build());

		gui.openInventory(player);

		this.create(player, gui);
	}

}
