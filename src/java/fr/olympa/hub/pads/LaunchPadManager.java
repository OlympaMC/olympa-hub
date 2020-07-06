package fr.olympa.hub.pads;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class LaunchPadManager implements Listener {
	
	static {
		ConfigurationSerialization.registerClass(LaunchPad.class);
	}
	
	private Map<Location, LaunchPad> pads = new HashMap<>();
	
	private File file;
	private YamlConfiguration config;
	
	public LaunchPadManager(File file) throws IOException {
		this.file = file;
		file.getParentFile().mkdirs();
		file.createNewFile();
		
		config = YamlConfiguration.loadConfiguration(file);
		config.addDefault("pads", Collections.EMPTY_LIST);
		config.options().copyDefaults(true);
		
		for (LaunchPad pad : (List<LaunchPad>) config.getList("pads")) {
			addLaunchPad(pad, false);
		}
		
		new LaunchPadCommand(this).register();
	}
	
	public void addLaunchPad(LaunchPad pad, boolean update) {
		pads.put(pad.getLocation(), pad);
		if (update) {
			config.set("pads", new ArrayList<>(pads.values()));
			try {
				config.save(file);
			}catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent e) {
		if (e.getAction() == Action.PHYSICAL) {
			Block block = e.getClickedBlock();
			if (block == null) return;
			LaunchPad pad = pads.get(block.getLocation());
			if (pad == null) return;
			pad.step(e.getPlayer());
		}
	}
	
}
