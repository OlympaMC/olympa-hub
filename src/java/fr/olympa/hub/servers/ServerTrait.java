package fr.olympa.hub.servers;

import org.bukkit.event.EventHandler;

import fr.olympa.api.holograms.Hologram;
import fr.olympa.api.lines.DynamicLine;
import fr.olympa.api.lines.FixedLine;
import fr.olympa.api.server.OlympaServer;
import fr.olympa.core.spigot.OlympaCore;
import fr.olympa.hub.OlympaHub;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.exception.NPCLoadException;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.util.DataKey;

public class ServerTrait extends Trait {

	private ServerInfo server;
	private Hologram hologram;

	public ServerTrait() {
		super("server");
	}

	public void setServer(ServerInfo server) {
		this.server = server;
		showHologram();
	}

	private void removeHologram() {
		if (hologram != null) {
			OlympaCore.getInstance().getHologramsManager().deleteHologram(hologram);
			hologram = null;
		}
	}

	private void showHologram() {
		removeHologram();
		if (server != null) {
			this.hologram = new Hologram(npc.getEntity().getLocation().add(0, npc.getEntity().getHeight() + 0.2, 0).clone(), new FixedLine<>("§e§l" + server.getServer().getNameCaps()), FixedLine.EMPTY_LINE, new DynamicLine<>((x) -> "§7§l" + (server.getOnlinePlayers() == -1 ? "§cx" : server.getOnlinePlayers()) + " §7joueurs en ligne", server));
			OlympaCore.getInstance().getHologramsManager().addHologram(hologram);
		}
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
		if (server != null) server.connect(e.getClicker());
	}

	@Override
	public void save(DataKey key) {
		super.save(key);
		key.setString("server", server.getServer().getNameCaps());
	}

	@Override
	public void load(DataKey key) throws NPCLoadException {
		super.load(key);
		server = OlympaHub.getInstance().serversInfos.getServer(OlympaServer.valueOf(key.getString("server")));
	}

}