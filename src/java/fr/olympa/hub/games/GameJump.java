package fr.olympa.hub.games;

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

import fr.olympa.api.item.ItemUtils;
import fr.olympa.api.provider.AccountProvider;
import fr.olympa.hub.OlympaHub;

public class GameJump extends IGame{

	private List<Location> checkpoints = new ArrayList<Location>();

	private Map<Player, Long> playersCPTimeInit = new HashMap<Player, Long>();
	private Map<Player, Long> playerLastCPTime = new HashMap<Player, Long>();
	
	private Map<Player, Integer> playersLastCheckPoint = new HashMap<Player, Integer>();
	
	public GameJump(OlympaHub plugin, ConfigurationSection config) {
		super(plugin, GameType.JUMP, config);
		
		config.getStringList("checkpoints").forEach(s -> checkpoints.add(getLoc(s)));
		
		hotBarContent[6] = ItemUtils.item(Material.TOTEM_OF_UNDYING, "§2Revenir au checkpoint précédent");
	}

	@Override
	protected void startGame(OlympaPlayerHub p) {
		super.startGame(p);
		
		playersCPTimeInit.put(p.getPlayer(), System.currentTimeMillis());
		playerLastCPTime.put(p.getPlayer(), 0l);
		
		playersLastCheckPoint.put(p.getPlayer(), 0);
		
		p.getPlayer().sendMessage(gameType.getChatPrefix() + "§2Objectif : finissez le jump le plus rapidement possible !");
		//p.getPlayer().sendMessage(gameType.getChatPrefix() + "§2" + checkpoints.size() + " checkpoints sont prévus, n'hésitez pas à les utiliser !");
	}
	
	@Override
	protected void restartGame(OlympaPlayerHub p) {
		super.restartGame(p);
		
		playersCPTimeInit.put(p.getPlayer(), System.currentTimeMillis());
		playerLastCPTime.put(p.getPlayer(), 0l);
		//p.getPlayer().sendMessage(gameType.getChatPrefix() + "§aTéléportation au checkpoint ");
		
		playersLastCheckPoint.put(p.getPlayer(), 0);
		
		p.getPlayer().teleport(startingLoc);
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
				plugin.getTask().runTaskLater(() -> p.teleport(startingLoc), 80);
				
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
				(e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)) {
			
			playersCPTimeInit.put(e.getPlayer(), System.currentTimeMillis());
			
			int check = playersLastCheckPoint.get(e.getPlayer());
			e.getPlayer().teleport(checkpoints.get(check));
			
			e.getPlayer().sendMessage(gameType.getChatPrefix() + "§2Téléportation au checkpoint " + check + 
					". Temps actuel : " + new DecimalFormat("#.##").format(playerLastCPTime.get(e.getPlayer())/1000d) + "s");
		}
	}
}
