package fr.olympa.hub;

import java.util.Arrays;

import org.bukkit.event.EventHandler;

import fr.olympa.api.spigot.holograms.Hologram;
import fr.olympa.api.spigot.lines.CyclingLine;
import fr.olympa.api.spigot.lines.FixedLine;
import fr.olympa.api.utils.Prefix;
import fr.olympa.core.spigot.OlympaCore;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.trait.Trait;

public class VoteTrait extends Trait {

	private Hologram hologram;

	public VoteTrait() {
		super("vote");
	}

	private void removeHologram() {
		if (hologram != null) {
			hologram.remove();
			hologram = null;
		}
	}

	private void showHologram() {
		removeHologram();
		OlympaCore.getInstance().getHologramsManager().createHologram(npc.getEntity().getLocation().add(0, npc.getEntity().getHeight() + 0.1, 0), false, true,
				new CyclingLine<>(Arrays.asList("§6§lVote", "§e§lVote"), 50),
				FixedLine.EMPTY_LINE,
				new FixedLine<>("§7www.olympa.fr/vote"));
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
		if (e.getNPC() != npc) return;
		Prefix.DEFAULT.sendMessage(e.getClicker(), "§6Tu peux voter sur https://olympa.fr/vote");
	}

}
