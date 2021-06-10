package fr.olympa.hub.pads;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;

import fr.olympa.api.spigot.config.CustomConfig;

public class LaunchPadManager implements Listener {

	static {
		ConfigurationSerialization.registerClass(LaunchPad.class);
	}

	private Map<Location, LaunchPad> pads = new HashMap<>();

	private CustomConfig config;

	public LaunchPadManager(Plugin plugin, String fileName) {

		config = new CustomConfig(plugin, fileName);
		config.load();
		config.addDefault("pads", Collections.emptyList());
		config.options().copyDefaults(true);

		for (LaunchPad pad : (List<LaunchPad>) config.getList("pads"))
			addLaunchPad(pad, false);

		new LaunchPadCommand(this).register();
	}

	public void addLaunchPad(LaunchPad pad, boolean update) {
		pads.put(pad.getLocation(), pad);
		if (update) {
			config.set("pads", new ArrayList<>(pads.values()));
			config.save();
		}
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent e) {
		if (e.getAction() == Action.PHYSICAL) {
			Block block = e.getClickedBlock();
			if (block == null)
				return;
			LaunchPad pad = pads.get(block.getLocation());
			if (pad == null)
				return;
			pad.step(e.getPlayer());
		}
	}

}
