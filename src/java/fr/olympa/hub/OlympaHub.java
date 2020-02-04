package fr.olympa.hub;

import org.bukkit.plugin.PluginManager;

import fr.olympa.api.plugin.OlympaAPIPlugin;
import fr.olympa.hub.bungee.MessageChannel;
import fr.olympa.hub.commands.SpawnCommand;
import fr.olympa.hub.gui.GuiHubListener;
import fr.olympa.hub.listeners.DamageListener;
import fr.olympa.hub.listeners.PlayerJoinListener;
import fr.olympa.hub.listeners.ProtectListener;

public class OlympaHub extends OlympaAPIPlugin {

	private static OlympaHub instance;

	public static OlympaHub getInstance() {
		return instance;
	}

	private MessageChannel mc;

	public MessageChannel getMessageChannel() {
		return this.mc;
	}

	@Override
	public void onDisable() {
	}

	@Override
	public void onEnable() {
		instance = this;
		this.mc = new MessageChannel();
		new SpawnCommand(this).register();
		this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
		this.getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this.mc);
		PluginManager pluginManager = this.getServer().getPluginManager();
		pluginManager.registerEvents(new PlayerJoinListener(), this);
		pluginManager.registerEvents(new ProtectListener(), this);
		pluginManager.registerEvents(new DamageListener(), this);
		pluginManager.registerEvents(new GuiHubListener(), this);
	}

}
