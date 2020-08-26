package fr.olympa.hub.games;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

import fr.olympa.api.holograms.Hologram;
import fr.olympa.api.holograms.Hologram.HologramLine;
import fr.olympa.api.item.ItemUtils;
import fr.olympa.api.lines.DynamicLine;
import fr.olympa.api.lines.FixedLine;
import fr.olympa.api.player.OlympaPlayerInformations;
import fr.olympa.api.provider.AccountProvider;
import fr.olympa.api.region.Region;
import fr.olympa.api.utils.observable.Observable;
import fr.olympa.core.spigot.OlympaCore;
import fr.olympa.hub.OlympaHub;

public abstract class IGame implements Observable, Listener{
	
	private static final int maxTopScores = 10;

	private OlympaHub plugin;
	private Map<String, Observer> observers = new HashMap<String, Observer>();
	
	private ItemStack[] hotBarContent = new ItemStack[9];
	private Map<UUID, ItemStack[]> players = new HashMap<UUID, ItemStack[]>();
	private List<UUID> ghostStarters = new ArrayList<UUID>(); //contient les joueurs pour lesquels il ne faut pas lancer le jeu même s'ils le déclenchent
	
	//scores map, must be sorted from the best to the worst
	private LinkedHashMap<OlympaPlayerInformations, Double> topScores = new LinkedHashMap<OlympaPlayerInformations, Double>(); 
	
	private final GameType gameType;
	private Region area;
	private Location startingLoc;
	private Hologram holo;
	
	@SuppressWarnings("unchecked")
	public IGame(OlympaHub plugin, GameType game, Region area, Location holoLoc, Location startingLoc) {
		this.plugin = plugin;
		this.gameType = game;
		
		this.area = area;
		this.startingLoc = startingLoc;
		
		holo = OlympaCore.getInstance().getHologramsManager().createHologram(holoLoc, false, 
				new FixedLine<HologramLine>(gameType.getName()), new FixedLine<HologramLine>(" "));
		
		for (int i = 0 ; i < maxTopScores ; i++) {
			
			final int index = i;
			
			holo.addLine(new DynamicLine<HologramLine>(
					new Function<HologramLine, String>() {
						
						@Override
						public String apply(HologramLine line) {
							if (topScores.size() > index) {
								OlympaPlayerInformations p = (OlympaPlayerInformations) topScores.keySet().toArray()[index];
								
								return "§a" + index + ". §2" + p.getName() + " - " + topScores.get(p);
							}else
								return "§a" + index + ". §7indéfini";
						}
					}, this));	
		}
	}
	
	protected void setHotBar(ItemStack... items) {
		for (int i = 0 ; i < hotBarContent.length ; i++)
			if (items.length > i)
				hotBarContent[i] = items[i];
			else
				hotBarContent[i] = null;

		hotBarContent[7] = ItemUtils.item(Material.ENDER_PEARL, "§eRecommencer");
		hotBarContent[8] = ItemUtils.item(Material.BARRIER, "§cSortir du jeu");
				
	}
	
	
	//START, RESTART AND ENDGAME
	
	/**
	 * Start the game for the specified player.
	 * 
	 * @param p
	 */
	protected void startGame(OlympaPlayerHub p) {
		players.put(p.getUniqueId(), p.getPlayer().getInventory().getContents());
		p.getPlayer().getInventory().clear();
		p.getPlayer().getInventory().setContents(hotBarContent);
	}
	
	/**
	 * Restart the game for the specified player.
	 * 
	 * @param p
	 */
	protected abstract void restartGame(OlympaPlayerHub p);
	
	/**
	 * End game and save score for specified player.
	 * Use -1 as score to abort game. 
	 * 
	 * @param p player
	 * @param score obtained score
	 */
	protected void endGame(OlympaPlayerHub p, double score) {
		p.getPlayer().getInventory().setContents(players.remove(p.getUniqueId()));
		
		if (score == -1)
			return;
		
		p.setScore(gameType, score);
		
		if (updateTopScores(p, score))
			observers.values().forEach(o -> o.changed());
	}
	
	/**
	 * Update scores map
	 * @return true if top scores have been changed
	 */
	private boolean updateTopScores(OlympaPlayerHub p, double score) {
		List<OlympaPlayerInformations> keys = new ArrayList<OlympaPlayerInformations>(topScores.keySet());
		
		Map<OlympaPlayerInformations, Double> newScores = new LinkedHashMap<OlympaPlayerInformations, Double>();
		
		if (topScores.size() > 0 && topScores.get(keys.get(keys.size() - 1)) > score)
			return false;
		
		boolean hasBeenAdded = false;
		
		for (OlympaPlayerInformations key : keys)
			if (topScores.get(key) > score || hasBeenAdded) {
				if (newScores.size() < maxTopScores)
					newScores.put(key, topScores.get(key));
				
			}else {
				hasBeenAdded = true;
				newScores.put(p.getInformation(), score);
				
				if (newScores.size() < maxTopScores)
					newScores.put(key, topScores.get(key));
			}
		
		return true;			
	}
	
	public GameType getGameType() {
		return gameType;
	}
	
	public Region getRegion() {
		return area;
	}
	
	
	//LISTENER PART
	
	@EventHandler //handle player interract with its hotbar
	private void onInterract(PlayerInteractEvent e) {
		if (!players.keySet().contains(e.getPlayer().getUniqueId()))
			return;
		
		switch(e.getPlayer().getInventory().getHeldItemSlot()) {
		case 7:
			e.getPlayer().teleport(startingLoc);
			return;
		case 8:
			e.getPlayer().teleport(startingLoc);
			ghostStarters.add(e.getPlayer().getUniqueId());
			return;
		}
		
		onInterractHandler(e);
	}
	
	/**
	 * Override the method to execute actions on player interract event 
	 * @param e
	 */
	protected void onInterractHandler(PlayerInteractEvent e) {
		
	}
	
	@EventHandler
	private void playerMoveEvent(PlayerMoveEvent e) {
		if (e.getFrom().getBlockX() == e.getTo().getBlockX() && e.getFrom().getBlockZ() == e.getTo().getBlockZ())
			return;
		
		onMove(e.getPlayer(), e.getFrom(), e.getTo());
	}
	
	@EventHandler
	private void playerTeleportEvent(PlayerTeleportEvent e) {
		onMove(e.getPlayer(), e.getFrom(), e.getTo());
	}
	
	private void onMove(Player p, Location from, Location to) {
		if (!ghostStarters.contains(p.getUniqueId()))
			if (players.containsKey(p.getUniqueId()))
				restartGame(AccountProvider.get(p.getUniqueId()));
			else
				startGame(AccountProvider.get(p.getUniqueId()));
		
		if (players.containsKey(p.getUniqueId()))
			onMoveHandler(p, from, to);
	}
	
	/**
	 * Override the method to execute actions on player move or teleport
	 * @param p
	 * @param from
	 * @param to
	 */
	protected void onMoveHandler(Player p, Location from, Location to) {
		
	}
	
	@Override
	public void observe(String name, Observer observer) {
		observers.put(name, observer);
	}
	
	@Override
	public void unobserve(String name) {
		observers.remove(name);
	}
	
	public enum GameType {
		ELYTRA("scoreElytra", "§6Course d'élytra"),
		JUMP("scoreJump", "§6Jump"),
		ARENA("scoreArena", "Arène 1vs1"),
		LABY("scoreLaby", "Labyrinthe"),
		DAC("scoreDac", "Dé-à-coudre");
		
		private String key;
		private String name;
		
		GameType(String bddKey, String name){
			this.key = bddKey;
			this.name = name;
		}
		
		public String getBddKey() {
			return key;
		}
		
		public String getName() {
			return name;
		}
	}	
}
