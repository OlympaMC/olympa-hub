package fr.olympa.hub.games;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
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
import fr.olympa.api.region.shapes.Cuboid;
import fr.olympa.api.region.tracking.ActionResult;
import fr.olympa.api.region.tracking.TrackedRegion;
import fr.olympa.api.region.tracking.flags.Flag;
import fr.olympa.api.utils.observable.SimpleObservable;
import fr.olympa.core.spigot.OlympaCore;
import fr.olympa.hub.OlympaHub;

public abstract class IGame implements Listener{
	
	private static final int maxDisplayedTopScores = 10;
	static final int maxTopScoresStored = 100; 
	
	protected OlympaHub plugin;
	protected final IGame instance;
	private SimpleObservable observable = new SimpleObservable();
	
	protected ItemStack[] hotBarContent = new ItemStack[9];
	private Map<UUID, ItemStack[]> players = new HashMap<UUID, ItemStack[]>();
	//private List<UUID> ghostStarters = new ArrayList<UUID>(); //contient les joueurs pour lesquels il ne faut pas lancer le jeu même s'ils le déclenchent
	
	//scores map, must be sorted from the best to the worst
	//la taille ne doit pas dépasser maxTopScoresStored
	private LinkedHashMap<OlympaPlayerInformations, Double> topScores = new LinkedHashMap<OlympaPlayerInformations, Double>(); 
	
	protected ConfigurationSection config;
	protected final GameType gameType;
	private Region area;
	protected TrackedRegion region;
	protected Location startingLoc;
	private Hologram holo;
	
	protected Set<Location> allowedTpLocs = new HashSet<Location>();
	
	@SuppressWarnings("unchecked")
	public IGame(OlympaHub plugin, GameType game, ConfigurationSection config) {
		this.plugin = plugin;
		this.instance = this;
		
		this.gameType = game;
		
		this.config = config;
		this.area = getRegion(config.getString("area"));
		this.startingLoc = getLoc(config.getString("start_loc"));

		Location holoLoc = getLoc(config.getString("holo_loc"));
		
		//register listener
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		
		//init best scores
		try {
			ResultSet query = gameType.getStatement().executeQuery();
			while (query.next()) {
				//Bukkit.getLogger().log(Level.SEVERE, "jump score : " + AccountProvider.getPlayerInformations(query.getLong("player_id")).getName() + " - " + query.getDouble(2));
				topScores.put(AccountProvider.getPlayerInformations(query.getLong("player_id")), query.getDouble(gameType.getBddKey()));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		//Bukkit.getLogger().log(Level.SEVERE, "scores " + gameType + " : " + topScores);
		
		//création de l'holo scores
		holo = OlympaCore.getInstance().getHologramsManager().createHologram(holoLoc, false, 
				new FixedLine<HologramLine>("§6Scores " + gameType.getNameWithArticle()), new FixedLine<HologramLine>(" "));
		
		for (int i = 0 ; i < maxDisplayedTopScores ; i++) {
			
			final int index = i;
			
			//Bukkit.getLogger().log(Level.WARNING, "CHARGEMENT DE LA LIGNE " + i);
			
			holo.addLine(new DynamicLine<HologramLine>(line -> {
				
				if (topScores.size() > index) {
					
					OlympaPlayerInformations p = (OlympaPlayerInformations) topScores.keySet().toArray()[index];
					
					if (gameType.isTimerScore())
						return "§a" + (index + 1) + ". §e" + p.getName() + " §a- " + new DecimalFormat("#.##").format(topScores.get(p)) + "s";
					else
						return "§a" + (index + 1) + ". §e" + p.getName() + " §a- " + (int) (double) topScores.get(p) + " victoires";
				}else
					return "§a" + (index + 1) + ". §7indéfini";
			}, observable));
		}
		
		//observable.update();
		
		//création de l'holo du début de partie
		OlympaCore.getInstance().getHologramsManager().createHologram(startingLoc.clone().add(0, 2, 0), false, 
				new FixedLine<HologramLine>("§6Début " + gameType.getNameWithArticle()), 
				new FixedLine<HologramLine>("§7Commencez ici"));
		
		//gestion sortie de zone de jeu
		region = OlympaCore.getInstance().getRegionManager().registerRegion(area, "zone_" + gameType.toString().toLowerCase(), EventPriority.HIGHEST,
			new Flag() {
				@Override
				public ActionResult leaves(Player p, Set<TrackedRegion> to) {
					super.leaves(p, to);
					endGame(AccountProvider.get(p.getUniqueId()), -1, false);
					return ActionResult.ALLOW;
				}
			});
		
		//définition de la hotbar du jeu
		if (gameType.isRestartable())
			hotBarContent[7] = ItemUtils.item(Material.BELL, "§eRecommencer");
		hotBarContent[8] = ItemUtils.item(Material.BARRIER, "§cSortir du jeu");
		
		//add allowed tp locs
		allowedTpLocs.add(startingLoc);
	}
	
	public Set<UUID> getPlayers(){
		return Collections.unmodifiableSet(players.keySet());
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
		
		p.getPlayer().sendMessage(gameType.getChatPrefix() + "§aVous venez de rejoindre le jeu ! Faites de votre mieux !");
	}
	
	/**
	 * Restart the game for the specified player.
	 * 
	 * @param p
	 */
	protected void restartGame(OlympaPlayerHub p) {
		if (gameType.isRestartable())
			p.getPlayer().sendMessage(gameType.getChatPrefix() + "§7Remise à 0 des scores...");
	}
	
	/**
	 * End game and save score for specified player.
	 * Use -1 as score to abort game. 
	 * 
	 * @param p player
	 * @param score obtained score
	 * @return true if player finished the game, false otherwise
	 */
	protected void endGame(OlympaPlayerHub p, double score, boolean warpToSpawn) {
		if (!players.keySet().contains(p.getUniqueId()))
			return;
		
		p.getPlayer().getInventory().clear();
		p.getPlayer().getInventory().setContents(players.remove(p.getUniqueId()));
		
		if (warpToSpawn)
			p.getPlayer().teleport(startingLoc);
		
		if (score == -1) {
			p.getPlayer().sendMessage(gameType.getChatPrefix() + "§7Partie annulée.");
			return;	
		}
		
		//Messages de victoire/défaite
		if (gameType.isTimerScore()) {
			if (p.getScore(gameType) == 0)
				p.getPlayer().sendMessage(gameType.getChatPrefix() + "§aPremière partie terminée ! Votre temps est de " + 
						new DecimalFormat("#.##").format(score) + "s.");
		
			else if (p.getScore(gameType) <= score)
				p.getPlayer().sendMessage(gameType.getChatPrefix() + "§7Vous n'avez pas battu votre précédent record de " + 
						new DecimalFormat("#.##").format(p.getScore(gameType)) + "s ! Temps obtenu : " + new DecimalFormat("#.##").format(score) + "s.");
		
			else 
				p.getPlayer().sendMessage(gameType.getChatPrefix() + "§2Record personnel battu ! Passage de " + 
						new DecimalFormat("#.##").format(p.getScore(gameType)) + "s à " + new DecimalFormat("#.##").format(score) + "s. Félicitations !");
			
		}else {
			if (score == 1)
				if (p.getScore(gameType) == 0)
					p.getPlayer().sendMessage(gameType.getChatPrefix() + "§aFélicitations pour votre première victoire !");
				else
					p.getPlayer().sendMessage(gameType.getChatPrefix() + "§2Et une victoire de plus ! Nouveau compte de victoires : " + new DecimalFormat("#").format(p.getScore(gameType) + 1));
			else
				p.getPlayer().sendMessage(gameType.getChatPrefix() + "§aC'est perdu, mais vous ferez mieux la prochaine fois !");	
		}
		
		if (gameType.isTimerScore()) {
			if (p.getScore(gameType) == 0 || p.getScore(gameType) > score)
				p.setScore(gameType, score);	
		}else {
			if (score > 0)
				p.setScore(gameType, p.getScore(gameType) + 1);	
		}
		
		//rang du joueur, 0 si non classé
		int oldPlayerRank = getPlayerRank(p);
		
		String oldPlayerRankString;
		if (oldPlayerRank == 0)
			oldPlayerRankString = "§7aucune§e";
		else
			oldPlayerRankString = Integer.toString(oldPlayerRank);
		
		//Bukkit.broadcastMessage(oldPlayerRankString);
		
		if (updateScores(p, score)) {
			observable.update();
			
			if (getPlayerRank(p) < oldPlayerRank || oldPlayerRank == 0)
				p.getPlayer().sendMessage(gameType.getChatPrefix() + "§eVous progressez dans le tableau des scores de la place " + oldPlayerRankString + 
						" à la place §c" + getPlayerRank(p) + "§e, félicitations !!");
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
		//return new ArrayList<OlympaPlayerInformations>(topScores.keySet()).indexOf(p.getInformation()) + 1;
		List<OlympaPlayerInformations> list = new ArrayList<OlympaPlayerInformations>(topScores.keySet());
		for (int i = 0 ; i < topScores.keySet().size() ; i++)
			if (list.get(i).getId() == p.getId())
				return i + 1;
		
		return 0;
	}
	
	/**
	 * Update player score and top scores map
	 * @param target player
	 * @param new player score
	 * @return true if top scores have been changed, false otherwise
	 */
	private boolean updateScores(OlympaPlayerHub p, double score) {
		//Bukkit.broadcastMessage("try to update scores. old score : " + p.getScore(gameType) + " - new score : " + score);
		
		//int previousRank = getPlayerRank(p);
		
		if (score <= 0)
			return false;

		topScores.put(p.getInformation(), p.getScore(gameType));
		
		sortScores();
		
		//Bukkit.broadcastMessage("old rank : " + previousRank + " - new rank : " + getPlayerRank(p));
		
		if (getPlayerRank(p) > 0)
			return true;
		else
			return false;
	}
	
	private void sortScores() {
		//get all entries from the LinkedHashMap and convert it to a List
		List<Map.Entry<OlympaPlayerInformations, Double>> list = new ArrayList<Map.Entry<OlympaPlayerInformations, Double>>( topScores.entrySet() );
		
		Collections.sort(list, new Comparator<Map.Entry<OlympaPlayerInformations, Double>>(){
			 
		    public int compare(Entry<OlympaPlayerInformations, Double> e1, Entry<OlympaPlayerInformations, Double> e2) {
		    	if (gameType.isTimerScore())
		    		return (int) (e1.getValue()*1000d - e2.getValue()*1000d);
		    	else 
		    		return (int) (e2.getValue()*1000d - e1.getValue()*1000d);
		    }
		});
		
		//store sorted data into top scores map 
		topScores.clear();
		int i = 0;
		while(topScores.size() < maxTopScoresStored && list.size() > i) {
			topScores.put(list.get(i).getKey(), list.get(i).getValue());
			i++;
		}
		//entries.forEach(e -> topScores.put(e.getKey(), e.getValue()));
	}
	
	public GameType getType() {
		return gameType;
	}
	
	public Region getRegion() {
		return area;
	}

	
	///////////////////////////////////////////////////////////
	//          MOVE, TELEPORT, INTERRACT LISTENERS          //
	///////////////////////////////////////////////////////////

	
	@EventHandler(priority = EventPriority.LOWEST) //handle player interract with its hotbar
	public void onInterract(PlayerInteractEvent e) {
		if (!players.keySet().contains(e.getPlayer().getUniqueId()))
			return;
		
		if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)
			switch(e.getPlayer().getInventory().getHeldItemSlot()) {
			case 7:
				if (gameType.isRestartable() && !e.getPlayer().getLocation().getBlock().equals(startingLoc.getBlock())) {
					restartGame(AccountProvider.get(e.getPlayer().getUniqueId()));
					
					e.setCancelled(true);
					return;
				}
				break;
			case 8:
				endGame(AccountProvider.get(e.getPlayer().getUniqueId()), -1, true);
				
				e.setCancelled(true);
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
	public void onInterractInventory(InventoryInteractEvent e) {
		if (!players.containsKey(e.getWhoClicked().getUniqueId()))
			return;
		e.setCancelled(true);
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onTeleport(PlayerTeleportEvent e) {
		if (players.containsKey(e.getPlayer().getUniqueId())) {
			boolean allowTp = false;
			
			for (Location loc : allowedTpLocs)
				if (!allowTp) 
					if (loc.getBlock().equals(e.getTo().getBlock()))
						allowTp = true;
			
			if (!allowTp) {
				e.getPlayer().sendMessage(gameType.getChatPrefix() + "§cVous téléporter pendant le jeu est interdit !");
				endGame(AccountProvider.get(e.getPlayer().getUniqueId()), -1, false);	
			}	
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void playerMoveEvent(PlayerMoveEvent e) {
		if (e.getFrom().getBlock().equals(e.getTo().getBlock()))
			return;

		Player p = e.getPlayer();
		//Bukkit.broadcastMessage("starting loc : " + startingLoc);
		//Bukkit.broadcastMessage(p + " moved - from : " + e.getTo().getBlock().getLocation() + " - to : " + startingLoc.getBlock());
		
		if (e.getTo().getBlock().equals(startingLoc.getBlock())) {
			
			if (!players.containsKey(p.getUniqueId()))
				startGame(AccountProvider.get(p.getUniqueId()));
			//else
				//restartGame(AccountProvider.get(p.getUniqueId()));
			
		}else if (players.containsKey(p.getUniqueId()))
			onMoveHandler(p, e.getFrom(), e.getTo());
	}
	
	/**
	 * Execute actions on player move
	 * @param p
	 * @param from
	 * @param to
	 */
	protected void onMoveHandler(Player p, Location from, Location to) {
		
	}
	
	@EventHandler
	public void onDamage(EntityDamageEvent e) {
		if (players.containsKey(e.getEntity().getUniqueId()))
			onDamageHandler(e);
	}
	
	/**
	 * Execute actions when a player takes damages
	 * @param e
	 */
	protected void onDamageHandler(EntityDamageEvent e) {
		
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		endGame(AccountProvider.get(e.getPlayer().getUniqueId()), -1, false);
	}

	
	///////////////////////////////////////////////////////////
	//                         UTILS                         //
	///////////////////////////////////////////////////////////
	

	protected final Location getLoc(String str) {
		String[] args = str.split(" ");
		
		if (args.length < 4)
			return null;
		
		try {
			if (args.length == 4)
				return new Location(Bukkit.getWorld(args[0]), Double.valueOf(args[1]) + 0.5, Double.valueOf(args[2]), Double.valueOf(args[3]) + 0.5);
			else if (args.length == 6)
				return new Location(Bukkit.getWorld(args[0]), Double.valueOf(args[1]) + 0.5, Double.valueOf(args[2]), Double.valueOf(args[3]) + 0.5, Float.valueOf(args[4]), Float.valueOf(args[5]));
			else
				return null;
		}catch(NumberFormatException e) {
			return null;
		}
	}
	
	protected final Cuboid getRegion(String str) {
		String[] args = str.split(" ");
		
		if (args.length != 7)
			return null;

		try {
			return new Cuboid(Bukkit.getWorld(args[0]), Integer.valueOf(args[1]), Integer.valueOf(args[2]), Integer.valueOf(args[3]), 
					Integer.valueOf(args[4]), Integer.valueOf(args[5]), Integer.valueOf(args[6]));
		}catch(NumberFormatException e) {
			return null;
		}
		
	}
}
