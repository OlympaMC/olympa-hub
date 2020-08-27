package fr.olympa.hub.games;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
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
import fr.olympa.api.region.tracking.ActionResult;
import fr.olympa.api.region.tracking.TrackedRegion;
import fr.olympa.api.region.tracking.flags.Flag;
import fr.olympa.api.sql.OlympaStatement;
import fr.olympa.api.utils.observable.Observable;
import fr.olympa.core.spigot.OlympaCore;
import fr.olympa.hub.OlympaHub;

public abstract class IGame implements Observable, Listener{
	
	private static final int maxDisplayedTopScores = 10;
	private static final int maxTopScoresStored = maxDisplayedTopScores * maxDisplayedTopScores; 
	
	private OlympaHub plugin;
	private Map<String, Observer> observers = new HashMap<String, Observer>();
	
	private ItemStack[] hotBarContent = new ItemStack[9];
	private Map<UUID, ItemStack[]> players = new HashMap<UUID, ItemStack[]>();
	private List<UUID> ghostStarters = new ArrayList<UUID>(); //contient les joueurs pour lesquels il ne faut pas lancer le jeu même s'ils le déclenchent
	
	//scores map, must be sorted from the best to the worst
	//la taille ne doit pas dépasser maxTopScoresStored
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
		
		//init best scores
		try {
			ResultSet query = gameType.getStatement().executeQuery();
			while (query.next()) {
				topScores.put(AccountProvider.getPlayerInformations(query.getLong("player_id")), query.getDouble(gameType.getBddKey()));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		//création de l'holo scores
		holo = OlympaCore.getInstance().getHologramsManager().createHologram(holoLoc, false, 
				new FixedLine<HologramLine>("§6Scores " + gameType.getNameWithArticle()), new FixedLine<HologramLine>(" "));
		
		for (int i = 0 ; i < maxDisplayedTopScores ; i++) {
			
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
		
		//création de l'holo du début de partie
		OlympaCore.getInstance().getHologramsManager().createHologram(startingLoc.clone().add(0, 1.5, 0), false, 
				new FixedLine<HologramLine>("§6Début " + gameType.getNameWithArticle()), 
				new FixedLine<HologramLine>("§7(marchez ici pour commencer)"));
		
		//gestion sortie de zone de jeu
		OlympaCore.getInstance().getRegionManager().registerRegion(area, "zone_" + gameType.toString().toLowerCase(), EventPriority.HIGH, new Flag() {
			@Override
			public ActionResult leaves(Player p, Set<TrackedRegion> to) {
				super.leaves(p, to);
				endGame(AccountProvider.get(p.getUniqueId()), -1, false);
				return ActionResult.ALLOW;
			}
		});
	}
	
	protected void setHotBar(ItemStack... items) {
		for (int i = 0 ; i < hotBarContent.length ; i++)
			if (items.length > i)
				hotBarContent[i] = items[i];
			else
				hotBarContent[i] = null;
		
		if (gameType.isRestartable())
			hotBarContent[7] = ItemUtils.item(Material.ENDER_PEARL, "§eRecommencer");
		hotBarContent[8] = ItemUtils.item(Material.BARRIER, "§cSortir du jeu");
				
	}
	
	///////////////////////////////////////////////////////////
	//               START, RESTART and ENDGAME              //
	///////////////////////////////////////////////////////////
	
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
	protected void endGame(OlympaPlayerHub p, double score, boolean teleportToGameSpawn) {
		if (!players.keySet().contains(p.getUniqueId()))
			return;
		
		p.getPlayer().getInventory().setContents(players.remove(p.getUniqueId()));
		
		if (teleportToGameSpawn) {
			ghostStarters.add(p.getUniqueId());
			p.getPlayer().teleport(startingLoc);	
		}
		
		if (score == -1) {
			p.getPlayer().sendMessage(gameType.getChatPrefix() + "§7Partie annulée.");
			return;	
		}
		
		//Messages de victoire/défaite
		if (gameType.isTimerScore())
			if (p.getScore(gameType) == 0)
				p.getPlayer().sendMessage(gameType.getChatPrefix() + "§aPremière partie terminée ! Votre temps est de " + 
						new DecimalFormat("#.##").format(score) + "s.");
		
			else if (p.getScore(gameType) <= score)
				p.getPlayer().sendMessage(gameType.getChatPrefix() + "§a7Vous n'avez pas battu votre précédent record de " + 
						new DecimalFormat("#.##").format(p.getScore(gameType)) + "s ! Temps obtenu : " + new DecimalFormat("#.##").format(score) + "s.");
		
			else 
				p.getPlayer().sendMessage(gameType.getChatPrefix() + "§2Record personnel battu ! Passage de " + 
						new DecimalFormat("#.##").format(p.getScore(gameType)) + "s à " + new DecimalFormat("#.##").format(score) + "s. Félicitations !");
		
		else
			if (score == 1)
				if (p.getScore(gameType) == 0)
					p.getPlayer().sendMessage(gameType.getChatPrefix() + "§aFélicitations pour votre première victoire !");
				else
					p.getPlayer().sendMessage(gameType.getChatPrefix() + "§2Et une victoire de plus ! Nouveau compte de victoires : " + p.getScore(gameType) + 1);
			else
				p.getPlayer().sendMessage(gameType.getChatPrefix() + "§aC'est perdu, mais vous ferez mieux la prochaine fois !");
					

		p.setScore(gameType, score);
		
		//rang du joueur, 0 si non classé
		int oldPlayerRank = getPlayerRank(p);
		
		String oldPlayerRankString;
		if (oldPlayerRank == 0)
			oldPlayerRankString = "§7aucune§6";
		else
			oldPlayerRankString = Integer.toString(oldPlayerRank);
		
		if (updateTopScores(p, score)) {
			observers.values().forEach(o -> o.changed());
			
			if (getPlayerRank(p) < oldPlayerRank)
				p.getPlayer().sendMessage(gameType.getChatPrefix() + "§6Vous progressez dans le tableau des scores de la place " + oldPlayerRankString + 
						" à la place " + getPlayerRank(p) + ", félicitations !!");
		}	
	}

	
	///////////////////////////////////////////////////////////
	//                   MANAGE TOP SCORES                   //
	///////////////////////////////////////////////////////////
	
	/**
	 * Return player rank for this game, 0 if not classed
	 * @param p player
	 * @return player rank (1 is the best, 0 is unranked)
	 */
	public int getPlayerRank(OlympaPlayerHub p) {
		return new ArrayList<OlympaPlayerInformations>(topScores.keySet()).indexOf(p.getInformation()) + 1;
	}
	
	/**
	 * Update scores map
	 * @return true if top scores have been changed
	 */
	private boolean updateTopScores(OlympaPlayerHub p, double score) {
		LinkedHashMap<OlympaPlayerInformations, Double> oldScores = new LinkedHashMap<OlympaPlayerInformations, Double>(topScores);
		oldScores.put(p.getInformation(), score);
		
		LinkedHashMap<OlympaPlayerInformations, Double> newScores = new LinkedHashMap<OlympaPlayerInformations, Double>();

		if (gameType.isTimerScore()) 
			oldScores.entrySet().stream().sorted(Map.Entry. comparingByValue()).forEach(e -> newScores.put(e.getKey(), e.getValue()));
		else
			oldScores.entrySet().stream().sorted(Map.Entry. comparingByValue(new Comparator<Double>(){

				@Override
				public int compare(Double o1, Double o2) {
					return (int) (o2-o1);
				}
			})).forEach(e -> newScores.put(e.getKey(), e.getValue()));
		
		if (newScores.size() > maxTopScoresStored) {
			List<OlympaPlayerInformations> keys = new ArrayList<OlympaPlayerInformations>(newScores.keySet());
			newScores.remove(keys.get(keys.size() - 1));
		}
		
		int oldRank = getPlayerRank(p);
		int newRank = new ArrayList<OlympaPlayerInformations>(newScores.keySet()).indexOf(p.getInformation()) + 1;

		topScores = newScores;
		
		if (newRank != oldRank)
			return true;
		else
			return false;			
	}
	
	public GameType getGameType() {
		return gameType;
	}
	
	public Region getRegion() {
		return area;
	}

	
	///////////////////////////////////////////////////////////
	//          MOVE, TELEPORT, INTERRACT LISTENERS          //
	///////////////////////////////////////////////////////////
	
	
	@EventHandler //handle player interract with its hotbar
	private void onInterract(PlayerInteractEvent e) {
		if (!players.keySet().contains(e.getPlayer().getUniqueId()))
			return;
		
		switch(e.getPlayer().getInventory().getHeldItemSlot()) {
		case 7:
			if (gameType.isRestartable()) {
				restartGame(AccountProvider.get(e.getPlayer().getUniqueId()));
				return;
			}
			break;
		case 8:
			endGame(AccountProvider.get(e.getPlayer().getUniqueId()), -1, true);
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
		//si le joueur marche sur le bloc qui commence la partie
		if (to.getBlockX() == startingLoc.getBlockX() && to.getBlockY() == startingLoc.getBlockY() && to.getBlockZ() == startingLoc.getBlockZ()) {
			
			if (!ghostStarters.contains(p.getUniqueId()))
				if (players.containsKey(p.getUniqueId()))
					restartGame(AccountProvider.get(p.getUniqueId()));
				else
					startGame(AccountProvider.get(p.getUniqueId()));	
			else
				ghostStarters.remove(p.getUniqueId());
			
		}else if (players.containsKey(p.getUniqueId()))
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

	
	///////////////////////////////////////////////////////////
	//               OBSERVABLE IMPLEMENTATION               //
	///////////////////////////////////////////////////////////
	
	@Override
	public void observe(String name, Observer observer) {
		observers.put(name, observer);
	}
	
	@Override
	public void unobserve(String name) {
		observers.remove(name);
	}

	
	///////////////////////////////////////////////////////////
	//                  GAMETYPE SUB CLASS                   //
	///////////////////////////////////////////////////////////
	
	
	public enum GameType {
		ELYTRA("scoreElytra", "§6Course d'élytra", "de la ", true, true),
		JUMP("scoreJump", "§6Jump", "du ", true, true),
		ARENA("scoreArena", "Arène 1vs1", "de l'", false, false),
		LABY("scoreLaby", "Labyrinthe", "du ", false, true),
		DAC("scoreDac", "Dé-à-coudre", "du ", false, false);
		
		private String bddKey;
		private String name;
		private String article;
		private boolean isRestartable;
		private boolean isTimerScore;
		
		private PreparedStatement bddStatement;
		
		GameType(String bddKey, String name, String art, boolean isRestartable, boolean isTimerScore){
			this.bddKey = bddKey;
			this.name = name;
			this.article = art;
			this.isRestartable = isRestartable;
			this.isTimerScore = isTimerScore;
			
			String sort;
			if (isTimerScore)
				sort = "ASC";
			else
				sort = "DESC";
			
			try {
				bddStatement = new OlympaStatement("SELECT player_id, " + bddKey + " FROM " + AccountProvider.getPlayerProviderTableName() + 
				 " WHERE " + bddKey + " != 0 ORDER BY " + bddKey + " " + sort + " LIMIT " + maxTopScoresStored + ";").getStatement();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		public String getBddKey() {
			return bddKey;
		}
		
		public String getName() {
			return name;
		}
		
		public boolean isRestartable() {
			return isRestartable;
		}
		
		public boolean isTimerScore() {
			return isTimerScore;
		}
		
		public PreparedStatement getStatement() {
			return bddStatement;
		}
		
		public String getNameWithArticle() {
			return article + name.toLowerCase();
		}
		
		public String getChatPrefix() {
			return name + " > ";
		}
	}	
}
