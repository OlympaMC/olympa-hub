package fr.olympa.hub.minigames.games;

import java.rmi.activation.ActivateFailedException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.scheduler.BukkitTask;

import fr.olympa.api.command.complex.Cmd;
import fr.olympa.api.command.complex.CommandContext;
import fr.olympa.api.editor.RegionEditor;
import fr.olympa.api.item.ItemUtils;
import fr.olympa.api.provider.AccountProvider;
import fr.olympa.api.region.shapes.Cuboid;
import fr.olympa.core.bungee.servers.WaitingConnection;
import fr.olympa.hub.HubListener;
import fr.olympa.hub.OlympaHub;
import fr.olympa.hub.minigames.utils.GameType;
import fr.olympa.hub.minigames.utils.OlympaPlayerHub;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class GameDac extends AQueuedGame {

	private final List<Material> wools = Arrays.asList(Material.PINK_WOOL, Material.CYAN_WOOL, Material.RED_WOOL, Material.YELLOW_WOOL, Material.BLUE_WOOL, Material.MAGENTA_WOOL, Material.LIME_WOOL, Material.ORANGE_WOOL, Material.WHITE_WOOL, Material.BLACK_WOOL);
	//private boolean isGameInProgress = false;
	private final int playDelay = 10;
	
	private int currentTurn = -1;
	
	private Cuboid jumpRegion;
	private Location tpLoc;
	private int minJumpY;
	
	private Player playingPlayer = null;
	private boolean hasJumped = false;
	
	private int remainingTime;
	private BukkitTask timeTask = null;
	
	private BossBar bar = Bukkit.createBossBar("dac", BarColor.PINK, BarStyle.SEGMENTED_10);
	
	private Map<Player, Material> woolColor = new HashMap<Player, Material>(); 
	
	public GameDac(OlympaHub plugin, ConfigurationSection configFromFile) throws ActivateFailedException {
		super(plugin, GameType.DAC, configFromFile, 2, 10);

		jumpRegion = (Cuboid) config.get("jump_region");
		minJumpY = config.getInt("min_jump_y");
		
		allowedTpLocs.add(tpLoc = config.getLocation("tp_loc"));

		//reset langing area
		endGame();
	}

	@Override
	protected boolean startGame(OlympaPlayerHub p) {
		return super.startGame(p);
	}
	 
	
	@Override
	protected void endGame(OlympaPlayerHub p, double score, boolean warpToSpawn) {
		super.endGame(p, score, warpToSpawn);
		
		bar.removePlayer(p.getPlayer());
		HubListener.bossBar.addPlayer(p.getPlayer());
		if (p.getPlayer() == playingPlayer) playGameTurn();
	}
	
	@Override
	protected void startGame() {
		playingPlayers.clear();
		
		for (int i = 0; i < waitingPlayers.size(); i++) {
			Player dacP = playingPlayers.get(i);
			playingPlayers.add(dacP);
			woolColor.put(playingPlayers.get(i), wools.get(i % wools.size()));
			
			dacP.teleport(tpLoc);
			sendMessage(dacP, "§eLe match de dé à coudre commence ! Sélection du tour...");
			
			HubListener.bossBar.removePlayer(dacP);
			bar.addPlayer(dacP);
			
			dacP.getInventory().setItem(4, ItemUtils.item(woolColor.get(dacP), "§dDé à coudre", "§8> §7Vous êtes le", "  §7joueur §l" + i));
		}

		waitingPlayers.clear();
		bar.setTitle("§5Dé à coudre");
		currentTurn = 0;
		plugin.getTask().runTaskLater(() -> playGameTurn(), 2, TimeUnit.SECONDS);
	}

	@Override
	protected void endGame() {
		//reset landing area
		for (int x = jumpRegion.getMin().getBlockX() ; x <= jumpRegion.getMax().getBlockX() ; x++)
			for (int z = jumpRegion.getMin().getBlockZ() ; z <= jumpRegion.getMax().getBlockZ() ; z++)
				world.getBlockAt(x, jumpRegion.getMax().getBlockY(), z).setType(Material.WATER);
		
		//reset playing player
		playingPlayer = null;
		playingPlayers.clear();

		//si plus qu'un seul joueur en lice, fin de jeu (ou reset du jeu si 0 joueurs restants)
		/*if (playingPlayers.size() == 1)
			endGame(AccountProvider.get(playingPlayers.get(0).getUniqueId()), winnerScore, true);*/
	}
	
	private void playGameTurn() {
		if (playingPlayers.size() == 0)
			return;			
		
		playingPlayer = playingPlayers.get(0);
		currentTurn++;

		bar.setTitle("§5Dé à coudre : §d§l" + playingPlayer.getName());
		bar.setProgress(1);
		
		hasJumped = false;
		
		sendMessage(playingPlayer, "§a§lTour " + currentTurn + " : c'est à vous ! §7Sautez du pont, et tentez d'atterir dans l'eau ! Vous avez " + playDelay + " secondes.");
		playingPlayer.playSound(playingPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1, 0.9f);
		
		playingPlayers.stream().forEach(player -> {
			player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("§dTour de §5§l" + playingPlayer.getName()));
			if (player != playingPlayer) sendMessage(player, "§aTour " + currentTurn + " : c'est à " + playingPlayer.getName() + " de jouer !");
		});
		
		//si le joueur a mis trop de temps à sauter, expulsion
		final int currentTurnBis = currentTurn;
		
		remainingTime = playDelay;
		if (timeTask != null) timeTask.cancel();
		
		timeTask = Bukkit.getScheduler().runTaskTimer(OlympaHub.getInstance(), () -> {
			if (remainingTime <= 0) {
				if (currentTurn == currentTurnBis)
					if (playingPlayer != null && playingPlayer.isOnline()) {
						sendMessage(playingPlayer, "§cVous avez attendu trop longtemps avant de sauter !");
						endGame(AccountProvider.get(playingPlayer.getUniqueId()), 0, true);
					}
				timeTask.cancel();
			}else {
				remainingTime--;
				if (remainingTime == 3 && playingPlayer != null)
					sendMessage(playingPlayer, "§eSautez vite, vous n'avez plus que 3 secondes !");
				
				bar.setProgress((double) remainingTime / (double) playDelay);
			}
		}, 20, 20);
	}
	
	/**
	 * Executes actions when a player lands (in water or on ground)
	 * @param p
	 */
	@Override
	protected void onMoveHandler(Player p, Location from, Location to) {
		if (playingPlayers.isEmpty()) return; // le jeu n'a pas commencé

		//Bukkit.broadcastMessage("TO Y : " + to.getBlockY() + " - minJumpY : " + minJumpY);
		
		//si un joueur a sauté
		World world = from.getWorld();
		int x = from.getBlockX();
		int y = from.getBlockY();
		int z = from.getBlockZ();
		if (to.getBlockY() < minJumpY &&
				world.getBlockAt(x, y - 1, z).getType() == Material.AIR && 
				world.getBlockAt(x, y - 2, z).getType() == Material.AIR &&
				world.getBlockAt(x, y - 3, z).getType() == Material.AIR &&
				world.getBlockAt(x, y - 4, z).getType() == Material.AIR) {
			
			//Bukkit.broadcastMessage("Detected jump : " + p.getName());
			
			//détection de si c'est le joueur qui devait jouer qui a sauté
			if (playingPlayer != null && p.equals(playingPlayer)) {
				//if (!hasJumped) Bukkit.broadcastMessage("Detected jump : " + p.getName());
				hasJumped = true;
				
			}else {
				p.teleport(tpLoc);
				p.sendMessage(gameType.getChatPrefix() + "§7Ce n'est pas votre tour de sauter !");
			}
		}
		
		//si ce n'est pas le bon joueur ou s'il n'a pas sauté
		if (!hasJumped || playingPlayer == null || !p.equals(playingPlayer))
			return;
		
		Block block = to.getBlock();
		if (block.getType() == Material.WATER) {
			p.teleport(tpLoc);
			p.sendMessage(gameType.getChatPrefix() + "§aBien visé !");
			
			//détermination du bloc le plus en haut de la zone
			Location loc = block.getLocation().clone();
			while(loc.clone().add(0, 1, 0).getBlock().getType() == Material.WATER)
				loc = loc.add(0, 1, 0);
			
			loc.getBlock().setType(woolColor.get(playingPlayer));
			
			playingPlayers.add(playingPlayers.remove(0));
			playingPlayer = null;
			
			plugin.getTask().runTaskLater(() -> playGameTurn(), 500, TimeUnit.MILLISECONDS);
			
		}
		
	}
	
	
	//Si le joueur prend des dégâts de chute, c'est qu'il a fail son saut 
	protected void onDamageHandler(EntityDamageEvent e) {
		//return si ce n'est pas le joueur en train de sauter qui a pris des dégâts de chute, ou s'il a fini son saut et s'est fait retp en haut
		if (e.getCause() != DamageCause.FALL || 
				playingPlayer == null || !e.getEntity().equals(playingPlayer) || 
				e.getEntity().getLocation().getBlockY() >= minJumpY)
			return;
		
		playingPlayers.forEach(p -> {
			if (p.equals(playingPlayer))
				sendMessage(p, "§cLoupé ! Vous n'avez pas atteri dans l'eau...");
			else
				sendMessage(p, "§7§l" + playingPlayer.getName() + " §r§7a été éliminé, " + (playingPlayers.size() - 1) + " joueurs restants.");
		});
		
		playingPlayers.remove(0);
		playingPlayer = null;
		
		endGame(AccountProvider.get(e.getEntity().getUniqueId()), 0, true);
		plugin.getTask().runTaskLater(() -> playGameTurn(), 500, TimeUnit.MILLISECONDS);
	}

	
	///////////////////////////////////////////////////////////
	//                      CONFIG INIT                      //
	///////////////////////////////////////////////////////////
	
	
	protected ConfigurationSection initConfig(ConfigurationSection config) {
		config = super.initConfig(config);

		if (!config.contains("jump_region"))
			config.set("jump_region", new Cuboid(world, 0, 0, 0, 1, 1, 1));	
		
		if (!config.contains("tp_loc"))
			config.set("tp_loc", new Location(world, 0, 0, 0));	
		
		if (!config.contains("min_jump_y"))
			config.set("min_jump_y", 0);
		
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
		Player p = getPlayer();
		
		new RegionEditor(p, region -> {
			if (region == null || !(region instanceof Cuboid) || region.getMax().getBlockY() != region.getMin().getBlockY()) {
				p.sendMessage(gameType.getChatPrefix() + "§cLa sélection n'est pas valide. Ce doit être un cuboïde de 1 bloc d'épaisseur.");
				return;
			}
			
			jumpRegion = (Cuboid) region;
			config.set("jump_region", region);
			
			p.sendMessage(gameType.getChatPrefix() + "§aLa nouvelle zone de saut a bien été définie. §7Un redémarage est nécessaire.");
		}).enterOrLeave();
	}

	/**
	 * Internal function, do NOT call it
	 * @param cmd
	 */
	@Cmd (player = true)
	public void tpLoc(CommandContext cmd) {
		Location loc = getPlayer().getLocation();
		
		tpLoc = loc;
		config.set("tp_loc", tpLoc);
		
		getPlayer().sendMessage(gameType.getChatPrefix() + "§aLe point de téléportation a été défini en " +
				loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
	}

	/**
	 * Internal function, do NOT call it
	 * @param cmd
	 */
	@Cmd (player = true)
	public void minYloc(CommandContext cmd) {
		Location loc = getPlayer().getLocation();
		
		minJumpY = loc.getBlockY();
		config.set("min_jump_y", minJumpY);
		
		getPlayer().sendMessage(gameType.getChatPrefix() + "§aLe niveau Y minimal pour valider le saut est maintenant en y = " +
				loc.getBlockY());
	}	
}