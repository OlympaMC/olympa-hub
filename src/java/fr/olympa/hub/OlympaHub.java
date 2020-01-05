package fr.olympa.hub;

import org.bukkit.plugin.PluginManager;

import fr.olympa.api.plugin.OlympaPlugin;
import fr.olympa.hub.commands.SpawnCommand;
import fr.olympa.hub.gui.GuiHubListener;
import fr.olympa.hub.listeners.DamageListener;
import fr.olympa.hub.listeners.PlayerJoinListener;
import fr.olympa.hub.listeners.ProtectListener;

public class OlympaHub extends OlympaPlugin {

	private static OlympaHub instance;
	public static OlympaHub getInstance() {
		return instance;
	}

	@Override
	public void onDisable() {
		super.disable();
	}

	@Override
	public void onEnable() {
		instance = this;
		super.onEnable();
		new SpawnCommand(this).register();

		PluginManager pluginManager = this.getServer().getPluginManager();
		pluginManager.registerEvents(new PlayerJoinListener(), this);
		pluginManager.registerEvents(new ProtectListener(), this);
		pluginManager.registerEvents(new DamageListener(), this);
		pluginManager.registerEvents(new GuiHubListener(), this);
	}
}
