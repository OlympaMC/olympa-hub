package fr.olympa.hub;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.apache.commons.lang.WordUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import fr.olympa.api.groups.OlympaGroup;
import fr.olympa.api.lines.CyclingLine;
import fr.olympa.api.lines.DynamicLine;
import fr.olympa.api.lines.FixedLine;
import fr.olympa.api.lines.TimerLine;
import fr.olympa.api.permission.OlympaAPIPermissions;
import fr.olympa.api.permission.OlympaPermission;
import fr.olympa.api.player.OlympaPlayer;
import fr.olympa.api.plugin.OlympaAPIPlugin;
import fr.olympa.api.provider.AccountProvider;
import fr.olympa.api.redis.RedisAccess;
import fr.olympa.api.redis.RedisChannel;
import fr.olympa.api.region.Region;
import fr.olympa.api.region.tracking.ActionResult;
import fr.olympa.api.region.tracking.TrackedRegion;
import fr.olympa.api.region.tracking.flags.Flag;
import fr.olympa.api.scoreboard.sign.ScoreboardManager;
import fr.olympa.core.spigot.OlympaCore;
import fr.olympa.hub.gui.VanishManager;
import fr.olympa.hub.minigames.utils.GameType;
import fr.olympa.hub.minigames.utils.MiniGamesManager;
import fr.olympa.hub.minigames.utils.OlympaPlayerHub;
import fr.olympa.hub.pads.LaunchPadManager;
import fr.olympa.hub.servers.ServerConfigCommand;
import fr.olympa.hub.servers.ServerInfosListener;
import fr.olympa.hub.servers.ServerTrait;
import fr.skytasul.music.JukeBox;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.trait.TraitInfo;
import net.md_5.bungee.api.ChatMessageType;

public class OlympaHub extends OlympaAPIPlugin implements Listener {

	private static OlympaHub instance;

	public static OlympaHub getInstance() {
		return instance;
	}

	public ServerInfosListener serversInfos;
	public VanishManager vanishManager;
	
	public Location spawn, lightning;

	public MiniGamesManager games;
	public ScoreboardManager<OlympaPlayer> scoreboards;

	@SuppressWarnings("unchecked")
	@Override
	public void onEnable() {
		instance = this;
		super.onEnable();

		AccountProvider.setPlayerProvider(OlympaPlayerHub.class, OlympaPlayerHub::new, "lobby", OlympaPlayerHub.COLUMNS);
		for (GameType game : GameType.values()) game.initBddStatement();
		OlympaPermission.registerPermissions(HubPermissions.class);

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

		OlympaGroup.PLAYER.setRuntimePermission("jumppads.use");
		OlympaGroup.PLAYER.setRuntimePermission("music.favorites", false);

		OlympaCore.getInstance().registerRedisSub(RedisAccess.INSTANCE.connect(), serversInfos = new ServerInfosListener(getConfig().getConfigurationSection("servers")), RedisChannel.BUNGEE_SEND_SERVERSINFOS.name());
		//RedisAccess.INSTANCE.disconnect();

		OlympaCore.getInstance().getRegionManager().registerRegion(getConfig().getSerializable("zone", Region.class), "zone", EventPriority.HIGH, new Flag() {
			@Override
			public ActionResult leaves(Player p, Set<TrackedRegion> to) {
				super.leaves(p, to);
				p.teleport(spawn);
				return ActionResult.TELEPORT_ELSEWHERE;
			}
		}.setMessages(null, "§cNe vous égarez pas !", ChatMessageType.ACTION_BAR));

		CitizensAPI.getTraitFactory().registerTrait(TraitInfo.create(ServerTrait.class).withName("server"));
		CitizensAPI.getTraitFactory().registerTrait(TraitInfo.create(VoteTrait.class).withName("vote"));

		OlympaAPIPermissions.FLY_COMMAND.setMinGroup(OlympaGroup.MINI_YOUTUBER);
		OlympaAPIPermissions.GAMEMODE_COMMAND.setMinGroup(OlympaGroup.MINI_YOUTUBER);

		games = new MiniGamesManager(this);
		vanishManager = new VanishManager();
		
		scoreboards = new ScoreboardManager<>(this, "§e◆ §6§lOlympa§e ◆");
		scoreboards.addLines(
				FixedLine.EMPTY_LINE,
				new FixedLine<>("§6§lProfil:"),
				new DynamicLine<>(x -> "§7● " + x.getOlympaPlayer().getGroup().getColor() + x.getOlympaPlayer().getName()),
				new DynamicLine<>(x -> "§7● " + (x.getOlympaPlayer().getGroup() == null ? "§7Aucun grade" : x.getOlympaPlayer().getGroupNameColored())),
				FixedLine.EMPTY_LINE,
				new FixedLine<>("§d§lRadio:"),
				new TimerLine<>(x -> {
					String songName = JukeBox.getInstance().datas.getDatas(x.getOlympaPlayer().getUniqueId()).getListeningSongName();
					return "§7● " + (songName == null ? "§oarrêtée" : WordUtils.wrap(songName, 20, "\n§7", false));
				}, this, 20));
		scoreboards.addFooters(FixedLine.EMPTY_LINE, CyclingLine.olympaAnimation());
	}

	@Override
	public void onDisable() {
		MiniGamesManager.getInstance().saveConfig(MiniGamesManager.getInstance().getConfig());
	}

}
