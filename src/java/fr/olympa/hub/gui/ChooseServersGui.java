package fr.olympa.hub.gui;

import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import fr.olympa.api.common.player.OlympaPlayer;
import fr.olympa.api.common.server.ServerInfoAdvanced;
import fr.olympa.api.spigot.gui.OlympaGUI;
import fr.olympa.hub.servers.ServerInfoItem;

public class ChooseServersGui extends OlympaGUI {

	private Map<Integer, ServerInfoAdvanced> servers;
	private ServerInfoItem serverInfoItem;
	private OlympaPlayer player;

	public ChooseServersGui(OlympaPlayer player, ServerInfoItem serverInfoItem) {
		super("Serveurs " + serverInfoItem.getServerNameCaps(), 6);
		this.serverInfoItem = serverInfoItem;
		this.player = player;
		int i = 0;
		for (Entry<String, ServerInfoAdvanced> serv : serverInfoItem.getServers())
			servers.put(i++, serv.getValue());
		for (ItemStack serv : serverInfoItem.getItemsSelect())
			inv.setItem(i++, serv);
	}

	@Override
	public boolean onClick(Player p, ItemStack current, int slot, ClickType click) {
		if (slot >= 0 && servers.size() > slot) {
			ServerInfoAdvanced serverInfo = servers.get(slot);
			if (serverInfo != null) {
				serverInfoItem.connect(p, serverInfo);
				p.closeInventory();
				return true;
			}
		}
		new MenuGUI(player).create(p);
		return true;
	}



	@Override
	public boolean onClose(Player p) {
		return super.onClose(p);
	}

}
