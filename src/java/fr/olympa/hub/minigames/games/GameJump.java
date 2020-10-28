package fr.olympa.hub.minigames.games;

import java.rmi.activation.ActivateFailedException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import fr.olympa.api.command.complex.Cmd;
import fr.olympa.api.command.complex.CommandContext;
import fr.olympa.api.item.ItemUtils;
import fr.olympa.api.provider.AccountProvider;
import fr.olympa.hub.OlympaHub;
import fr.olympa.hub.minigames.utils.GameType;
import fr.olympa.hub.minigames.utils.OlympaPlayerHub;

public class GameJump extends IGame{

	private Location tpLoc;
	private List<Location> checkpoints = new ArrayList<Location>();

	private Map<Player, Long> playersCPTimeInit = new HashMap<Player, Long>();
	private Map<Player, Long> playerLastCPTime = new HashMap<Player, Long>();
	
	private Map<Player, Integer> playersLastCheckPoint = new HashMap<Player, Integer>();
	
	public GameJump(OlympaHub plugin, ConfigurationSection fileConfig) throws ActivateFailedException {
		super(plugin, GameType.JUMP, fileConfig);
		
		config.getList("checkpoints").forEach(loc -> checkpoints.add((Location) loc));
		
		hotBarContent[6] = ItemUtils.item(Material.TOTEM_OF_UNDYING, "§2Revenir au checkpoint précédent");
		tpLoc = config.getLocation("tp_loc");
		
		allowedTpLocs.add(tpLoc);
		allowedTpLocs.addAll(checkpoints);
	}

	@Override
	protected void startGame(OlympaPlayerHub p) {
		super.startGame(p);
		
		playersCPTimeInit.put(p.getPlayer(), System.currentTimeMillis());
		playerLastCPTime.put(p.getPlayer(), 0l);
		
		playersLastCheckPoint.put(p.getPlayer(), 0);
		
		p.getPlayer().teleport(tpLoc);
		p.getPlayer().sendMessage(gameType.getChatPrefix() + "§2Objectif : finissez le jump le plus rapidement possible !");
		//p.getPlayer().sendMessage(gameType.getChatPrefix() + "§2" + checkpoints.size() + " checkpoints sont prévus, n'hésitez pas à les utiliser !");
	}
	
	@Override
	protected void restartGame(OlympaPlayerHub p) {
		super.restartGame(p);
		
		playersCPTimeInit.put(p.getPlayer(), System.currentTimeMillis());
		playerLastCPTime.put(p.getPlayer(), 0l);
		
		playersLastCheckPoint.put(p.getPlayer(), 0);
		
		p.getPlayer().teleport(tpLoc);
	}
	
	@Override
	protected void endGame(OlympaPlayerHub p, double score, boolean warpToSpawn) {
		super.endGame(p, score, warpToSpawn);
		
		playersCPTimeInit.remove(p.getPlayer());
		playerLastCPTime.remove(p.getPlayer());
		
		playersLastCheckPoint.remove(p.getPlayer());
	}
	
	@Override
	protected void onMoveHandler(Player p, Location from, Location to) {
		int check = getCheckpointIndex(to);
		
		if (check == 0 || getCurrentCPTime(p) == -1)
			return;
		
		if (playersLastCheckPoint.get(p) + 1 == check) 
			if (check == checkpoints.size() - 1) {
				endGame(AccountProvider.get(p.getUniqueId()), (playerLastCPTime.get(p) + getCurrentCPTime(p))/1000d, false);
				
				p.getPlayer().sendMessage(gameType.getChatPrefix() + "§7Téléportation au spawn dans 5 secondes...");
				plugin.getTask().runTaskLater(() -> p.teleport(startingLoc), 100);
				
			}else {
				playerLastCPTime.put(p, playerLastCPTime.get(p) + getCurrentCPTime(p));
				playersCPTimeInit.put(p, System.currentTimeMillis());
				
				playersLastCheckPoint.put(p, check);
				
				p.sendMessage(gameType.getChatPrefix() + "§aCheckpoint " + check + " atteint ! Temps total : " + new DecimalFormat("#.##").format(playerLastCPTime.get(p)/1000d) + "s");
			}
		else if (playersLastCheckPoint.get(p) > check)
			p.sendMessage(gameType.getChatPrefix() + "§7Vous avez déjà validé ce checkpoint !");
		else if (playersLastCheckPoint.get(p) + 1 < check)
			p.sendMessage(gameType.getChatPrefix() + "§cNe brûlez pas les étapes ! §7Vous devez d'abord vous rendre au point " + (playersLastCheckPoint.get(p) + 1));
	}
	
	private int getCheckpointIndex(Location toCheck) {
		for (Location loc : checkpoints)
			if (loc.getBlock().equals(toCheck.getBlock()))
				return checkpoints.indexOf(loc);
		
		return 0;
	}
	
	/**
	 * Return seconds player took since the begining of the last checkpoint
	 * @param p
	 * @return
	 */
	private long getCurrentCPTime(Player p) {
		if (!playersCPTimeInit.containsKey(p))
			return -1;
		
		return (long) (System.currentTimeMillis() - playersCPTimeInit.get(p));
	}
	
	@Override
	protected void onInterractHandler(PlayerInteractEvent e) {
		//tp player to last achieved checkpoint
		if (e.getPlayer().getInventory().getHeldItemSlot() == 6 && 
				(e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)) 
					tpToLastCheckpoint(e.getPlayer());
	}

	@Override
	protected boolean exitGameArea(Player p) {
		tpToLastCheckpoint(p);
		return false;
	}
	
	private void tpToLastCheckpoint(Player p) {
		playersCPTimeInit.put(p, System.currentTimeMillis());
		
		int check = playersLastCheckPoint.get(p);
		p.teleport(checkpoints.get(check));
		
		p.sendMessage(gameType.getChatPrefix() + "§2Téléportation au checkpoint " + check + 
				". Temps actuel : " + new DecimalFormat("#.##").format(playerLastCPTime.get(p)/1000d) + "s");
	}
	
	///////////////////////////////////////////////////////////
	//                      CONFIG INIT                      //
	///////////////////////////////////////////////////////////
	
	
	protected ConfigurationSection initConfig(ConfigurationSection config) {
		config = super.initConfig(config);
		
		if (!config.contains("checkpoints")) {
			List<Location> list = new ArrayList<Location>();
			list.add(new Location(world, 0, 0, 0));
			list.add(new Location(world, 1, 1, 1));
			
			config.set("checkpoints", list);	
			config.set("tp_loc", new Location(world, 1, 1, 1));
		}
		
		return config;
	}


	///////////////////////////////////////////////////////////
	//                       COMMANDS                        //
	///////////////////////////////////////////////////////////
	
	@Override
	public void startLoc(CommandContext cmd) {
		super.startLoc(cmd);
		
		checkpoints.set(0, startingLoc);
		config.set("checkpoints", checkpoints);
	}

	/**
	 * Internal function, do NOT call it
	 * @param cmd
	 */
	@Cmd (player = true)
	public void tpLoc(CommandContext cmd) {
		tpLoc = getPlayer().getLocation();
		allowedTpLocs.add(tpLoc);
		
		config.set("tp_loc", tpLoc);
		
		getPlayer().sendMessage(gameType.getChatPrefix() + "§aLe point de téléportation a été défini en " +
				tpLoc.getBlockX() + ", " + tpLoc.getBlockY() + ", " + tpLoc.getBlockZ());
	}

	
	/**
	 * Internal function, do NOT call it
	 * @param cmd
	 */
	@Cmd (player = true)
	public void addCheckPoint(CommandContext cmd) {
		Location loc = getPlayer().getLocation();
		
		checkpoints.add(loc);
		
		config.set("checkpoints", checkpoints);
		
		getPlayer().sendMessage(gameType.getChatPrefix() + "§aLe checkpoint " + (checkpoints.size() - 1) + " a été défini en " +
				loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
	}
	
	/**
	 * Internal function, do NOT call it
	 * @param cmd
	 */
	@Cmd (player = true)
	public void clearCheckPoints(CommandContext cmd) {
		checkpoints.clear();
		checkpoints.add(tpLoc);
		
		config.set("checkpoints", checkpoints);
		
		getPlayer().sendMessage(gameType.getChatPrefix() + "§aLes checkpoints ont été supprimés.");
	}
	
	/**
	 * Internal function, do NOT call it
	 * @param cmd
	 */
	@Cmd (player = true)
	public void listCheckPoints(CommandContext cmd) {
		String msg = gameType.getChatPrefix() + "§aListe des checkpoints : ";
		
		for (int i = 1 ; i < checkpoints.size() ; i++)
			msg += "\n§2 " + i + ". §a" + checkpoints.get(i).getBlockX() + ", " + checkpoints.get(i).getBlockY() + ", " + checkpoints.get(i).getBlockZ();
		
		getPlayer().sendMessage(msg);
	}
}






