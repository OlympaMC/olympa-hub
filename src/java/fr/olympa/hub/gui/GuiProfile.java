package fr.olympa.hub.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import fr.olympa.api.gui.CustomInventory;
import fr.olympa.api.gui.OlympaGui;
import fr.olympa.api.item.OlympaItemBuild;
import fr.olympa.api.objects.OlympaPlayer;
import fr.olympa.api.provider.AccountProvider;

public class GuiProfile implements CustomInventory {

	private OlympaItemBuild ITEM_INFO = new OlympaItemBuild("&7").lore("", "&6Grade: ");

	@Override
	public boolean onClick(Player p, OlympaGui inv, ItemStack current, int slot, ClickType click) {
		return false;
	}

	public void open(Player player) {
		OlympaGui gui = new OlympaGui("&6Olympa &f| &7Profile", 3);

		OlympaItemBuild item = this.ITEM_INFO.skullowner(player);
		OlympaPlayer olympaPlayer = AccountProvider.get(player);
		item.addName(player.getName());
		item.addLore(1, olympaPlayer.getGroupsToHumainString());
		gui.setItem(gui.getFirstSlot(), item.build());

		gui.openInventory(player);

		this.create(player, gui);
	}

}
