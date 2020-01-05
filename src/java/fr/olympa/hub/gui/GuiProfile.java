package fr.olympa.hub.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import fr.olympa.api.gui.OlympaGUI;
import fr.olympa.api.item.OlympaItemBuild;
import fr.olympa.api.objects.OlympaPlayer;
import fr.olympa.api.provider.AccountProvider;

public class GuiProfile extends OlympaGUI {

	public GuiProfile(Player p) {
		super("&6Olympa &f| &7Profile", 3);

		OlympaPlayer olympaPlayer = AccountProvider.get(p);
		OlympaItemBuild item = new OlympaItemBuild("&7").lore("", "&6Grade: ").skullowner(p);
		item.addName(p.getName());
		item.setLore(1, olympaPlayer.getGroupsToHumainString());
		inv.setItem(0, item.build());
	}

	public boolean onClick(Player p, ItemStack current, int slot, ClickType click) {
		return true;
	}

}
