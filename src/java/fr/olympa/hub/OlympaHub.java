package fr.olympa.hub;

import org.apache.commons.lang.WordUtils;
import org.bukkit.Location;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;

import fr.olympa.api.common.groups.OlympaGroup;
import fr.olympa.api.common.permission.OlympaPermission;
import fr.olympa.api.common.permission.list.OlympaAPIPermissionsSpigot;
import fr.olympa.api.common.player.OlympaPlayer;
import fr.olympa.api.common.plugin.OlympaAPIPlugin;
import fr.olympa.api.common.provider.AccountProviderAPI;
import fr.olympa.api.common.server.OlympaServer;
import fr.olympa.api.common.server.ServerStatus;
import fr.olympa.api.spigot.lines.CyclingLine;
import fr.olympa.api.spigot.lines.DynamicLine;
import fr.olympa.api.spigot.lines.FixedLine;
import fr.olympa.api.spigot.lines.TimerLine;
import fr.olympa.api.spigot.region.Region;
import fr.olympa.api.spigot.region.tracking.ActionResult;
import fr.olympa.api.spigot.region.tracking.RegionEvent.ExitEvent;
import fr.olympa.api.spigot.region.tracking.flags.Flag;
import fr.olympa.api.spigot.scoreboard.sign.ScoreboardManager;
import fr.olympa.core.spigot.OlympaCore;
import fr.olympa.hub.gui.VanishManager;
import fr.olympa.hub.minigames.utils.MiniGamesManager;
import fr.olympa.hub.minigames.utils.OlympaPlayerHub;
import fr.olympa.hub.pads.LaunchPadManager;
import fr.olympa.hub.servers.ServerConfigCommand;
import fr.olympa.hub.servers.ServerInfosListener;
import fr.olympa.hub.servers.ServerTrait;
import fr.skytasul.music.JukeBox;
import fr.skytasul.music.JukeBoxDatas;
import fr.skytasul.music.PlayerData;
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
		try {
			instance = this;
			super.onEnable();
			OlympaCore.getInstance().setOlympaServer(OlympaServer.LOBBY);
			AccountProviderAPI.getter().setPlayerProvider(OlympaPlayerHub.class, OlympaPlayerHub::new, "lobby", OlympaPlayerHub.COLUMNS);
			OlympaPermission.registerPermissions(HubPermissions.class);

			getConfig().addTask("olympaHubMain", config -> {
				lightning = config.getLocation("lightning");
				spawn = config.getLocation("spawn");
				OlympaCore.getInstance().setSpawn(spawn);
			});

			PluginManager pm = getServer().getPluginManager();
			pm.registerEvents(new HubListener(), this);

			pm.registerEvents(new LaunchPadManager(this, "launchpads.yml"), this);

			new SpawnCommand(this).register();
			new ServerConfigCommand(this).register();

			OlympaGroup.PLAYER.setRuntimePermission("jumppads.use");
			OlympaGroup.PLAYER.setRuntimePermission("music.favorites", false);
			OlympaGroup.PLAYER.setRuntimePermission("music.save-datas", false);
			OlympaGroup.VIP.setRuntimePermission("music.favorites", true);
			OlympaGroup.VIP.setRuntimePermission("music.save-datas", true);

			serversInfos = new ServerInfosListener(config);
			pm.registerEvents(serversInfos, this);

			OlympaCore.getInstance().getRegionManager().registerRegion(getConfig().getSerializable("zone", Region.class), "zone", EventPriority.HIGH, new Flag() {
				@Override
				public ActionResult leaves(ExitEvent event) {
					super.leaves(event);
					event.getPlayer().teleport(spawn);
					return ActionResult.TELEPORT_ELSEWHERE;
				}
			}.setMessages(null, "§cNe vous égarez pas !", ChatMessageType.ACTION_BAR));

			CitizensAPI.getTraitFactory().registerTrait(TraitInfo.create(ServerTrait.class).withName("server"));
			CitizensAPI.getTraitFactory().registerTrait(TraitInfo.create(VoteTrait.class).withName("vote"));

			OlympaAPIPermissionsSpigot.FLY_COMMAND.setMinGroup(OlympaGroup.MINI_YOUTUBER);
			OlympaAPIPermissionsSpigot.GAMEMODE_COMMAND.setMinGroup(OlympaGroup.MINI_YOUTUBER);

			games = new MiniGamesManager(this);
			vanishManager = new VanishManager();

			scoreboards = new ScoreboardManager<>(this, "§e◆ §6§lOlympa§e ◆");
			scoreboards.addLines(
					FixedLine.EMPTY_LINE,
					new FixedLine<>("§6§lProfil:"),
					new DynamicLine<>(x -> "§7● " + (x.getOlympaPlayer().getGroup() == null ? "§7Aucun grade" : x.getOlympaPlayer().getGroupNameColored())),
					new DynamicLine<>(x -> "§7● " + x.getOlympaPlayer().getGroup().getColor() + x.getOlympaPlayer().getName()),
					FixedLine.EMPTY_LINE,
					new FixedLine<>("§d§lRadio:"),
					new TimerLine<>(x -> {
						JukeBoxDatas datasManager = JukeBox.getInstance().datas;
						if (datasManager == null)
							return "§§cnon fonctionnel";
						PlayerData datas = datasManager.getDatas(x.getOlympaPlayer().getUniqueId());
						if (datas == null) {
							sendMessage("§cPas de données lors de la mise à jour du scoreboard musique pour %s.", x.getOlympaPlayer().getName());
							return "§cpas de données";
						}
						String songName = datas.getListeningSongName();
						return "§7● " + (songName == null ? "§oarrêtée" : WordUtils.wrap(songName, 20, "\n§7", false));
					}, this, 20),
					FixedLine.EMPTY_LINE,
					new FixedLine<>("§a§lServeur:"),
					new DynamicLine<>(x -> "§7● " + OlympaCore.getInstance().getServerName()));
			scoreboards.addFooters(FixedLine.EMPTY_LINE, CyclingLine.olympaAnimation());
		} catch (Error | Exception e) {
			OlympaCore.getInstance().setStatus(ServerStatus.MAINTENANCE);
			getLogger().severe(String.format("Une erreur est survenu lors du chargement de %s. Le serveur est désormais en maintenance.", this.getClass().getSimpleName()));
			e.printStackTrace();
		}
	}

	@Override
	public void onDisable() {
		MiniGamesManager miniGameManager = MiniGamesManager.getInstance();
		if (miniGameManager != null)
			miniGameManager.saveConfig();
	}

}
