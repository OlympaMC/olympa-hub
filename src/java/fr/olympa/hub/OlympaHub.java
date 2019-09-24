package fr.olympa.hub;

import org.bukkit.plugin.PluginManager;

import fr.olympa.hub.gui.GuiHubListener;
import fr.olympa.hub.listeners.DamageListener;
import fr.olympa.hub.listeners.PlayerJoinListener;
import fr.olympa.hub.listeners.ProtectListener;
import fr.tristiisch.olympa.api.plugin.OlympaPlugin;
import fr.tristiisch.olympa.api.task.TaskManager;

public final class OlympaHub extends OlympaPlugin {

	@Override
	public void onDisable() {
		this.sendMessage("§4" + this.getDescription().getName() + "§c by Tristiisch (" + this.getDescription().getVersion() + ") is disabled.");
	}

	@Override
	public void onEnable() {
		new TaskManager(this);

		final PluginManager pluginManager = this.getServer().getPluginManager();
		pluginManager.registerEvents(new PlayerJoinListener(), this);
		pluginManager.registerEvents(new ProtectListener(), this);
		pluginManager.registerEvents(new DamageListener(), this);
		pluginManager.registerEvents(new GuiHubListener(), this);

		this.sendMessage("§2" + this.getDescription().getName() + "§a by Tristiisch (" + this.getDescription().getVersion() + ") is activated.");
	}
}
