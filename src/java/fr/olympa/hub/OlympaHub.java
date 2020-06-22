package fr.olympa.hub;

import org.bukkit.Location;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import fr.olympa.api.plugin.OlympaAPIPlugin;
import fr.olympa.api.region.Region;
import fr.olympa.api.region.tracking.flags.Flag;
import fr.olympa.core.spigot.OlympaCore;
import fr.olympa.hub.servers.ServerConfigCommand;
import fr.olympa.hub.servers.ServerInfosListener;
import fr.olympa.hub.servers.ServerTrait;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.trait.TraitInfo;
import net.md_5.bungee.api.ChatMessageType;

public class OlympaHub extends OlympaAPIPlugin implements Listener {

	private static OlympaHub instance;

	public static OlympaHub getInstance() {
		return instance;
	}

	public ServerInfosListener serversInfos;

	public Location spawn;

	@Override
	public void onEnable() {
		instance = this;
		super.onEnable();

		spawn = getConfig().getLocation("spawn");

		getServer().getPluginManager().registerEvents(new HubListener(), this);
		getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

		new SpawnCommand(this).register();
		new ServerConfigCommand(this).register();

		OlympaCore.getInstance().registerRedisSub(serversInfos = new ServerInfosListener(getConfig().getConfigurationSection("servers")), "sendServersInfos");
		
		OlympaCore.getInstance().getRegionManager().registerRegion(getConfig().getSerializable("zone", Region.class), "zone", EventPriority.HIGH, new Flag().setMessages(null, "§cNe vous égarez pas !", ChatMessageType.ACTION_BAR).setEntryExitDenied(false, true));

		CitizensAPI.getTraitFactory().registerTrait(TraitInfo.create(ServerTrait.class).withName("server"));
	}

}
