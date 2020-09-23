package fr.olympa.hub.minigames.games;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.scheduler.BukkitRunnable;

import fr.olympa.api.command.complex.Cmd;
import fr.olympa.api.command.complex.CommandContext;
import fr.olympa.api.editor.RegionEditor;
import fr.olympa.api.provider.AccountProvider;
import fr.olympa.api.region.shapes.Cuboid;
import fr.olympa.api.region.tracking.ActionResult;
import fr.olympa.api.region.tracking.TrackedRegion;
import fr.olympa.api.region.tracking.flags.Flag;
import fr.olympa.core.spigot.OlympaCore;
import fr.olympa.hub.OlympaHub;
import fr.olympa.hub.minigames.utils.GameType;
import fr.olympa.hub.minigames.utils.OlympaPlayerHub;

public class GameDac extends IGame {

	private final int minPlayers = 2;
	private boolean isGameInProgress = false;
	
	private final int countdownDelay = 3;
	private final int playDelay = 10;
	
	private int currentTurn = -1;
	
	private List<Player> playingPlayers = new ArrayList<Player>();
	
	private Cuboid jumpRegion;
	private Location tpLoc;
	
	private Cuboid barrierRegion;
	
	private Player playingPlayer = null;
	private boolean hasJumped = false;
	
	public GameDac(OlympaHub plugin, ConfigurationSection configFromFile) {
		super(plugin, GameType.DAC, configFromFile);

		jumpRegion = (Cuboid) config.get("jump_region");		
		barrierRegion = (Cuboid) config.get("barrier_region");
		
		tpLoc = config.getLocation("tp_loc");
		allowedTpLocs.add(tpLoc);

		resetLandingArea();

		//cancen region entering if player isn't the one which should jump
		OlympaCore.getInstance().getRegionManager().registerRegion(barrierRegion, "dac_barrier_area",
				EventPriority.HIGH, new Flag() {
			@Override
			public ActionResult enters(Player p, Set<TrackedRegion> to) {
				super.enters(p, to);
				if (p.equals(playingPlayer) && !hasJumped)
					return ActionResult.ALLOW;
				else
					return ActionResult.DENY;
			}
		});
	}

	@Override
	protected void startGame(OlympaPlayerHub p) {
		super.startGame(p);

		if (isGameInProgress)
			p.getPlayer().sendMessage(gameType.getChatPrefix() + "§7Une partie est déjà en cours...");
		else
			tryToInitGame();
	}
	
	
	@Override
	protected void endGame(OlympaPlayerHub p, double score, boolean warpToSpawn) {
		super.endGame(p, score, warpToSpawn);
		
		playingPlayers.remove(p.getPlayer());
	} 
	 
	private void tryToInitGame() {
		if (isGameInProgress || getPlayers().size() < minPlayers)
			getPlayers().forEach(id -> Bukkit.getPlayer(id).sendMessage(gameType.getChatPrefix() + "§aNombres de joueurs : " + getPlayers().size() + "/" + minPlayers));
		else
			startGame(countdownDelay);
	}
	
	private void startGame(int countdown) {
		if (isGameInProgress && countdown == countdownDelay)
			return;
		
		isGameInProgress = true;
		
		if (countdown == countdownDelay) {
			//getPlayers().forEach(uuid -> Bukkit.getPlayer(uuid).teleport(tpLoc));
			playingPlayers.clear();
		}
		
		if (countdown > 0) {
			getPlayers().forEach(uuid -> Bukkit.getPlayer(uuid).sendTitle("§c" + countdown, "§7Début du match dans...", 0, 21, 0));
			getPlayers().forEach(uuid -> Bukkit.getPlayer(uuid).sendMessage(gameType.getChatPrefix() + "§aDébut du match dans " + countdown));
			
			plugin.getTask().runTaskLater(() -> startGame(countdown - 1), 20, TimeUnit.SECONDS);
		}else {
			if (getPlayers().size() <= 1) {
				isGameInProgress =  false;
				getPlayers().forEach(uuid -> Bukkit.getPlayer(uuid).sendMessage(gameType.getChatPrefix() + "§7Plus assez de joueur pour commencer la partie..."));
				return;
			}
			getPlayers().forEach(uuid -> playingPlayers.add(Bukkit.getPlayer(uuid)));
			playingPlayers.forEach(p -> p.teleport(tpLoc));
			
			playGameTurn(true);
		}
	}
	
	private void playGameTurn(boolean isInit) {
		
		//si plus qu'un seul joueur en lice, fin de jeu (ou reset du jeu si 0 joueurs restants)
		if (playingPlayers.size() <= 1) {
			if (playingPlayers.size() == 1)
				endGame(AccountProvider.get(playingPlayers.get(0).getUniqueId()), 1, true);
			
			resetLandingArea();
			isGameInProgress = false;
			tryToInitGame();
			return;
		}

		hasJumped = false;
		
		if (isInit) {
			playingPlayer = playingPlayers.get(0);
			plugin.getTask().runTaskLater(() -> playGameTurn(false), 2, TimeUnit.SECONDS);
			
			currentTurn = 1;
			return;
		}else {
			playingPlayer = playingPlayers.get(0);
			currentTurn++;
		}
		
		playingPlayer.sendMessage(gameType + "§aTour " + currentTurn + " : c'est à vous ! §7Sautez dans le vide, et tentez d'atterir dans l'eau !");
		
		final int currentTurnBis = currentTurn;
		
		//si le joueur a mis trop de temps à sauter, forfait
		plugin.getTask().runTaskLater(() -> {
			if (currentTurn == currentTurnBis)
				if (getPlayers().contains(playingPlayer.getUniqueId()))
					endGame(AccountProvider.get(playingPlayer.getUniqueId()), -1, true);
			
		}, playDelay, TimeUnit.SECONDS);
	}
	
	/**
	 * Executes actions when a player lands (in water or on ground)
	 * @param p
	 */
	@Override
	protected void onMoveHandler(Player p, Location from, Location to) {
		if (p.equals(playingPlayer))
			return;
		
		Block block = to.add(0, -1, 0).getBlock();
		
		if (!hasJumped && block.getType() == Material.AIR) {
			hasJumped = true;
			return;
		}
		
		if (hasJumped)
			if (block.getType() == Material.WATER && jumpRegion.isIn(block.getLocation())) {
				playingPlayers.add(playingPlayers.remove(0));
				p.teleport(tpLoc);
				p.sendMessage(gameType.getChatPrefix() + "§aBien visé !");
				
				block.setType(Material.BEDROCK);
				
				playGameTurn(false);
				
			}else if (block.getType() != Material.AIR) {
				endGame(AccountProvider.get(p.getUniqueId()), 0, true);
				playGameTurn(false);
			}
	}
	
	private void resetLandingArea() {
		jumpRegion.getLocations().forEach(loc -> loc.getBlock().setType(Material.WATER));
	}

	
	///////////////////////////////////////////////////////////
	//                      CONFIG INIT                      //
	///////////////////////////////////////////////////////////
	
	
	protected ConfigurationSection initConfig(ConfigurationSection config) {
		config = super.initConfig(config);

		if (!config.contains("jump_region"))
			config.set("jump_region", new Cuboid(world, 0, 0, 0, 1, 1, 1));	
		if (!config.contains("barrier_region"))
			config.set("barrier_region", new Cuboid(world, 0, 0, 0, 1, 1, 1));	
		if (!config.contains("tp_loc"))
			config.set("tp_loc", new Location(world, 0, 0, 0));	
		
		return config;
	}


	///////////////////////////////////////////////////////////
	//                       COMMANDS                        //
	///////////////////////////////////////////////////////////

	
	/**
	 * Internal function, do NOT call it
	 * @param cmd
	 */
	@Cmd (player = true)
	public void jumpArea(CommandContext cmd) {

		new RegionEditor(cmd.command.getPlayer(), region -> {
			if (region == null || !(region instanceof Cuboid) || region.getMax().getBlockY() != region.getMin().getBlockY()) {
				cmd.command.getPlayer().sendMessage(gameType.getChatPrefix() + "§cLa sélection n'est pas valide. Ce doit être un cuboïde de 1 bloc d'épaisseur.");
				return;
			}
			
			jumpRegion = (Cuboid) region;
			config.set("jump_region", region);
			
			cmd.command.getPlayer().sendMessage(gameType.getChatPrefix() + "§aLa nouvelle zone de saut a bien été définie. §7Un redémarage est nécessaire.");
		}).enterOrLeave();
	}
	
	
	/**
	 * Internal function, do NOT call it
	 * @param cmd
	 */
	@Cmd (player = true)
	public void barrierArea(CommandContext cmd) {

		new RegionEditor(cmd.command.getPlayer(), region -> {
			if (region == null || !(region instanceof Cuboid)) {
				cmd.command.getPlayer().sendMessage(gameType.getChatPrefix() + "§cLa sélection n'est pas valide. Ce doit être un cuboïde.");
				return;
			}
			
			barrierRegion = (Cuboid) region;
			config.set("barrier_region", region);
			
			cmd.command.getPlayer().sendMessage(gameType.getChatPrefix() + "§aLa nouvelle région barrière a bien été définie. §7Un redémarage est nécessaire.");
		}).enterOrLeave();
	}

	/**
	 * Internal function, do NOT call it
	 * @param cmd
	 */
	@Cmd (player = true)
	public void tpLoc(CommandContext cmd) {
		Location loc = cmd.command.getPlayer().getLocation();
		
		tpLoc = loc;
		config.set("tp_loc", tpLoc);
		
		cmd.command.getPlayer().sendMessage(gameType.getChatPrefix() + "§aLe point de téléportation a été défini en " + 
				loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
	}

}











