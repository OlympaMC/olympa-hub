package fr.olympa.hub;

import org.bukkit.plugin.PluginManager;

import fr.olympa.api.plugin.OlympaPlugin;
import fr.olympa.hub.commands.SpawnCommand;
import fr.olympa.hub.gui.GuiHubListener;
import fr.olympa.hub.listeners.DamageListener;
import fr.olympa.hub.listeners.PlayerJoinListener;
import fr.olympa.hub.listeners.ProtectListener;

public class OlympaHub extends OlympaPlugin {

	public static OlympaHub getInstance() {
		return (OlympaHub) instance;
	}

	@Override
	public void onDisable() {
		this.disable();
		this.sendMessage("§4" + this.getDescription().getName() + "§c by Tristiisch (" + this.getDescription().getVersion() + ") is disabled.");
	}

	@Override
	public void onEnable() {
		this.enable(this);
		new SpawnCommand(this).register();

		PluginManager pluginManager = this.getServer().getPluginManager();
		pluginManager.registerEvents(new PlayerJoinListener(), this);
		pluginManager.registerEvents(new ProtectListener(), this);
		pluginManager.registerEvents(new DamageListener(), this);
		pluginManager.registerEvents(new GuiHubListener(), this);

		this.sendMessage("§2" + this.getDescription().getName() + "§a by Tristiisch (" + this.getDescription().getVersion() + ") is activated.");
	}
}
