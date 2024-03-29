package fr.olympa.hub.servers;

import org.bukkit.event.EventHandler;

import fr.olympa.api.spigot.holograms.Hologram;
import fr.olympa.api.spigot.lines.DynamicLine;
import fr.olympa.api.spigot.lines.FixedLine;
import fr.olympa.core.spigot.OlympaCore;
import fr.olympa.hub.OlympaHub;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.exception.NPCLoadException;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.util.DataKey;

public class ServerTrait extends Trait {

	private ServerInfoItem server;
	private Hologram hologram;

	public ServerTrait() {
		super("server");
	}

	public void setServer(ServerInfoItem server) {
		this.server = server;
		showHologram();
	}

	private void removeHologram() {
		if (hologram != null) {
			hologram.remove();
			hologram = null;
		}
	}

	private void showHologram() {
		removeHologram();
		if (server != null)
			hologram = OlympaCore.getInstance().getHologramsManager().createHologram(npc.getEntity().getLocation().add(0, npc.getEntity().getHeight() + 0.2, 0), false, true, new FixedLine<>("§e§l"
					+ server.getServerNameCaps()), FixedLine.EMPTY_LINE, new DynamicLine<>(x -> server.getOnlineString(), server));
	}

	@Override
	public void onSpawn() {
		super.onSpawn();
		showHologram();
	}

	@Override
	public void onDespawn() {
		super.onDespawn();
		removeHologram();
	}

	@Override
	public void onRemove() {
		super.onRemove();
		removeHologram();
	}

	@EventHandler
	public void onRightClick(NPCRightClickEvent e) {
		if (e.getNPC() != npc)
			return;
		if (server != null)
			server.connect(e.getClicker());
	}

	@Override
	public void save(DataKey key) {
		super.save(key);
		if (server == null)
			OlympaHub.getInstance().sendMessage(this.getClass().getName() + " §cServeur %s introuvable.", key.getString("server"));
		else
			key.setString("server", server.getItemServerNameKey());
	}

	@Override
	public void load(DataKey key) throws NPCLoadException {
		super.load(key);
		server = OlympaHub.getInstance().serversInfos.getServer(key.getString("server"));
		if (server == null)
			OlympaHub.getInstance().sendMessage(this.getClass().getName() + "§cServeur %s introuvable.", key.getString("server"));
	}

}