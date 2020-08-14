package fr.olympa.hub;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import fr.olympa.api.groups.OlympaGroup;
import fr.olympa.api.permission.OlympaCorePermissions;
import fr.olympa.api.plugin.OlympaAPIPlugin;
import fr.olympa.api.redis.RedisChannel;
import fr.olympa.api.region.Region;
import fr.olympa.api.region.tracking.ActionResult;
import fr.olympa.api.region.tracking.TrackedRegion;
import fr.olympa.api.region.tracking.flags.Flag;
import fr.olympa.core.spigot.OlympaCore;
import fr.olympa.hub.pads.LaunchPadManager;
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

	public Location spawn, lightning;

	@Override
	public void onEnable() {
		instance = this;
		super.onEnable();

		spawn = getConfig().getLocation("spawn");
		lightning = getConfig().getLocation("lightning");

		getServer().getPluginManager().registerEvents(new HubListener(), this);

		try {
			getServer().getPluginManager().registerEvents(new LaunchPadManager(new File(getDataFolder(), "launchpads.yml")), this);
		} catch (IOException e) {
			e.printStackTrace();
		}

		new SpawnCommand(this).register();
		new ServerConfigCommand(this).register();
		
		OlympaGroup.PLAYER.runtimePermissions.add("jumppads.use");

		OlympaCore.getInstance().registerRedisSub(serversInfos = new ServerInfosListener(getConfig().getConfigurationSection("servers")), RedisChannel.BUNGEE_SEND_SERVERSINFOS.name());

		OlympaCore.getInstance().getRegionManager().registerRegion(getConfig().getSerializable("zone", Region.class), "zone", EventPriority.HIGH, new Flag() {
			@Override
			public ActionResult leaves(Player p, Set<TrackedRegion> to) {
				super.leaves(p, to);
				p.teleport(spawn);
				return ActionResult.TELEPORT_ELSEWHERE;
			}
		}.setMessages(null, "§cNe vous égarez pas !", ChatMessageType.ACTION_BAR));

		CitizensAPI.getTraitFactory().registerTrait(TraitInfo.create(ServerTrait.class).withName("server"));

		OlympaCorePermissions.FLY_COMMAND.setMinGroup(OlympaGroup.MINI_YOUTUBER);
		OlympaCorePermissions.GAMEMODE_COMMAND.setMinGroup(OlympaGroup.MINI_YOUTUBER);
	}

}
