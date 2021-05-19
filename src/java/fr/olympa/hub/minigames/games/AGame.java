package fr.olympa.hub.minigames.games;

import java.rmi.activation.ActivateFailedException;
import java.sql.PreparedStatement;
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
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

import fr.olympa.api.command.complex.Cmd;
import fr.olympa.api.command.complex.CommandContext;
import fr.olympa.api.command.complex.ComplexCommand;
import fr.olympa.api.editor.RegionEditor;
import fr.olympa.api.holograms.Hologram;
import fr.olympa.api.holograms.Hologram.HologramLine;
import fr.olympa.api.item.ItemUtils;
import fr.olympa.api.lines.DynamicLine;
import fr.olympa.api.lines.FixedLine;
import fr.olympa.api.player.OlympaPlayerInformations;
import fr.olympa.api.provider.AccountProvider;
import fr.olympa.api.redis.RedisAccess;
import fr.olympa.api.redis.RedisChannel;
import fr.olympa.api.region.Region;
import fr.olympa.api.region.shapes.Cuboid;
import fr.olympa.api.region.tracking.ActionResult;
import fr.olympa.api.region.tracking.RegionEvent.EntryEvent;
import fr.olympa.api.region.tracking.RegionEvent.ExitEvent;
import fr.olympa.api.region.tracking.TrackedRegion;
import fr.olympa.api.region.tracking.flags.Flag;
import fr.olympa.api.utils.observable.SimpleObservable;
import fr.olympa.api.utils.spigot.SpigotUtils;
import fr.olympa.core.spigot.OlympaCore;
import fr.olympa.hub.HubPermissions;
import fr.olympa.hub.OlympaHub;
import fr.olympa.hub.minigames.utils.GameType;
import fr.olympa.hub.minigames.utils.MiniGamesManager;
import fr.olympa.hub.minigames.utils.OlympaPlayerHub;
import redis.clients.jedis.Jedis;

public abstract class AGame extends ComplexCommand implements Listener{
	
	private static final int maxDisplayedTopScores = 10;
	public static final int maxTopScoresStored = 100;
	
	protected final boolean isEnabled;
	
	protected OlympaHub plugin;
	protected final AGame instance;
	private SimpleObservable observable = new SimpleObservable();
	
	protected ItemStack[] hotBarContent = new ItemStack[9];
	private Map<Player, ItemStack[]> players = new HashMap<Player, ItemStack[]>();
	
	//scores map, must be sorted from the best to the worst
	//la taille ne doit pas dépasser maxTopScoresStored
	private LinkedHashMap<OlympaPlayerInformations, Double> topScores = new LinkedHashMap<OlympaPlayerInformations, Double>(); 
	
	protected ConfigurationSection config;
	protected final GameType gameType;
	private Region area;
	protected Location startingLoc;
	
	private Region portalRegion;
	private TrackedRegion portalRegionTracked;
	
	private Hologram scoresHolo;
	private Hologram startHolo;
	
	protected Set<Location> allowedTpLocs = new HashSet<Location>();
	
	protected World world;
	
	protected boolean allowFly = false; //if set to true, players will be allowed to use fly mode during the game
	
	@SuppressWarnings("unchecked")
	public AGame(OlympaHub plugin, GameType game, ConfigurationSection configFromFile) throws ActivateFailedException {
		super(plugin, game.toString().toLowerCase(), "Accès à la config " + game.getNameWithArticle() + ".", HubPermissions.EDIT_MINIGAMES);
		
		this.plugin = plugin;
		this.instance = this;
		
		this.gameType = game;

		this.world = Bukkit.getWorlds().get(0);
		this.config = initConfig(configFromFile);
		
		this.isEnabled = config.getBoolean("isEnabled");
		if (!isEnabled)
			throw new ActivateFailedException("");

		this.area = (Region) config.get("area");
		
		this.startingLoc = config.getLocation("start_loc");
		
		setPortal(config.getSerializable("portal_region", Region.class, null));
		
		//register listener
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		
		//init best scores
		try (PreparedStatement statement = gameType.getStatement().createStatement()) {
			ResultSet query = gameType.getStatement().executeQuery(statement);
			while (query.next()) {
				OlympaPlayerInformations p = AccountProvider.getPlayerInformations(query.getLong("player_id"));
				
				if (p != null)
					topScores.put(p, query.getDouble(gameType.getBddKey()));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		//création de l'holo scores
		scoresHolo = OlympaCore.getInstance().getHologramsManager().createHologram(config.getLocation("holo_loc"), false, true,
				new FixedLine<HologramLine>("§6Scores " + gameType.getNameWithArticle()), new FixedLine<HologramLine>(" "));
		
		for (int i = 0 ; i < maxDisplayedTopScores ; i++) {
			
			final int index = i;
			
			scoresHolo.addLine(new DynamicLine<HologramLine>(line -> {
				
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
		
		//création de l'holo du début de partie
		startHolo = OlympaCore.getInstance().getHologramsManager().createHologram(startingLoc.clone().add(0, 2, 0), false, true,
				new FixedLine<HologramLine>("§6Début " + gameType.getNameWithArticle()), 
				new FixedLine<HologramLine>("§7Commencez ici"));
		
		//gestion sortie de zone de jeu
		OlympaCore.getInstance().getRegionManager().registerRegion(area, "zone_" + gameType.toString().toLowerCase(), EventPriority.HIGHEST, new Flag() {
			@Override
			public ActionResult leaves(ExitEvent event) {
				super.leaves(event);
				
				if (!players.keySet().contains(event.getPlayer()))
					return ActionResult.ALLOW;
				
				if (exitGameArea(event.getPlayer())) {
					endGame(AccountProvider.get(event.getPlayer().getUniqueId()), -1, false);
					return ActionResult.ALLOW;
				}else
					return ActionResult.TELEPORT_ELSEWHERE;
			}
		});
		
		//définition de la hotbar du jeu
		if (gameType.isRestartable())
			hotBarContent[7] = ItemUtils.item(Material.BELL, "§eRecommencer");
		hotBarContent[8] = ItemUtils.item(Material.BARRIER, "§cSortir du jeu");
		
		//add allowed tp locs
		allowedTpLocs.add(startingLoc);
	}
	
	public Set<Player> getPlayers(){
		return Collections.unmodifiableSet(players.keySet());
	}
	
	/**
	 * Send a message to the player using the game chat prefix
	 * @param p
	 * @param msg
	 */
	protected void sendMessage(Player p, String msg) {
		p.sendMessage(gameType.getChatPrefix() + msg);
	}
	
	/**
	 * Teleport the player to the defined location, bypassing teleport restrictions
	 * @param p
	 * @param loc
	 */
	protected void teleport(Player p, Location loc) {
		allowedTpLocs.add(loc);
		p.teleport(loc);
		allowedTpLocs.remove(loc);
	}
	
	///////////////////////////////////////////////////////////
	//               START, RESTART and ENDGAME              //
	///////////////////////////////////////////////////////////
	
	/**
	 * Start the game for the specified player.
	 * 
	 * @param p
	 * @return true if initialisation has been a success, false otherwise
	 */
	protected boolean startGame(OlympaPlayerHub p) {
		if (gameType == GameType.LABY) return true;
		
		GameType previousGame = MiniGamesManager.getInstance().isPlaying(p.getPlayer());
	
		//cancel previous game if exists
		if (MiniGamesManager.getInstance().getGame(previousGame) != null)
			MiniGamesManager.getInstance().getGame(previousGame).endGame(p, -1, false);
		
		if ((!allowFly && p.getPlayer().isFlying()) || p.getPlayer().getGameMode() != GameMode.ADVENTURE) {
			p.getPlayer().sendMessage(gameType.getChatPrefix() + "§cVous devez être en gamemode aventure et sans fly pour pouvoir jouer !");
			return false;
		}
		
		players.put(p.getPlayer(), p.getPlayer().getInventory().getContents());
		p.getPlayer().getInventory().clear();
		p.getPlayer().getInventory().setContents(hotBarContent);
		
		p.getPlayer().sendMessage(gameType.getChatPrefix() + "§aVous venez de rejoindre le jeu ! Faites de votre mieux !");
		
		return true;
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
		if (!players.keySet().contains(p.getPlayer()))
			return;
		
		p.getPlayer().getInventory().clear();
		p.getPlayer().getInventory().setArmorContents(new ItemStack[] {null, null, null, null});
		p.getPlayer().getInventory().setContents(players.remove(p.getPlayer()));
		
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
			if (score >= 1)
				if (p.getScore(gameType) == 0)
					p.getPlayer().sendMessage(gameType.getChatPrefix() + "§aFélicitations pour votre première victoire ! Vous remportez " + score + " points.");
				else
					p.getPlayer().sendMessage(gameType.getChatPrefix() + "§2Et une victoire de plus ! Nouveau compte de points : " + new DecimalFormat("#").format(p.getScore(gameType) + score));
			else
				p.getPlayer().sendMessage(gameType.getChatPrefix() + "§aC'est perdu, mais vous ferez mieux la prochaine fois !");	
		}
		
		boolean hasScoreBeenImproved = false;
		
		//update du score du joueur en bdd si nécessaire
		if (gameType.isTimerScore()) {
			if ((p.getScore(gameType) == 0 && score > 0) || p.getScore(gameType) > score) {
				p.setScore(gameType, score);
				//new AccountProvider(p.getUniqueId()).saveToDb(p);
				
				hasScoreBeenImproved = true;
			}	
		}else {
			if (score > 0) {
				p.setScore(gameType, p.getScore(gameType) + score);
				//new AccountProvider(p.getUniqueId()).saveToDb(p);
				
				hasScoreBeenImproved = true;
			}	
		}
		
		//si le joueur a vu son score progresser, update des top scores
		if (hasScoreBeenImproved) {
				
			//rang du joueur, 0 si non classé
			int oldPlayerRank = getPlayerRank(p);
			
			String oldPlayerRankString;
			if (oldPlayerRank == 0)
				oldPlayerRankString = "§7aucune";
			else
				oldPlayerRankString = Integer.toString(oldPlayerRank);
			
			if (updateScores(p.getInformation(), p.getScore(gameType), true)) {
				
				if (getPlayerRank(p) < oldPlayerRank || oldPlayerRank == 0)
					p.getPlayer().sendMessage(gameType.getChatPrefix() + "§eVous progressez dans le tableau des scores de la place §c" + 
							oldPlayerRankString + " §eà la place §c" + getPlayerRank(p) + "§e, félicitations !!");
			}	
		}
	}


	///////////////////////////////////////////////////////////
	//                   MANAGE TOP SCORES                   //
	///////////////////////////////////////////////////////////

	public int getPlayerRank(OlympaPlayerHub p) {
		return getPlayerRank(p.getInformation());
	}
	
	/**
	 * Return player rank for this game, 0 if not classed
	 * @param p player
	 * @return player rank (1 is the best, 0 is unranked)
	 */
	public int getPlayerRank(OlympaPlayerInformations p) {
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
	public boolean updateScores(OlympaPlayerInformations p, double score, boolean shareInfoOnRedis) {
		if (score <= 0)
			return false;
			
		
		topScores.put(p, score);
		
		sortScores();
		
		if (getPlayerRank(p) > 0) {
			observable.update();
			
			if (shareInfoOnRedis) {
		        try (Jedis jedis = RedisAccess.INSTANCE.connect()) {
		            jedis.publish(RedisChannel.SPIGOT_LOBBY_MINIGAME_SCORE.name(), gameType.toString() + ":" + p.getId() + ":" + score);
		        } 
		        RedisAccess.INSTANCE.disconnect();	
			}
	        
	        
			return true;
		}
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

	public void beginGame(Player p) {
		p.teleport(startingLoc);
		startGame((OlympaPlayerHub)AccountProvider.get(p.getUniqueId()));
	}
	
	///////////////////////////////////////////////////////////
	//          MOVE, TELEPORT, INTERRACT LISTENERS          //
	///////////////////////////////////////////////////////////

	
	@EventHandler(priority = EventPriority.LOWEST) //handle player interract with its hotbar
	public void onInterract(PlayerInteractEvent e) {
		if (!players.keySet().contains(e.getPlayer()))
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
				plugin.getTask().runTaskLater(() -> endGame(AccountProvider.get(e.getPlayer().getUniqueId()), -1, true), 1);
				
				
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
	public void onInterractInventory(InventoryClickEvent e) {
		if (!players.containsKey(e.getWhoClicked()))
			return;
		
		e.setCancelled(true);
	}
	
	@EventHandler(priority = EventPriority.LOW)
	public void onTeleport(PlayerTeleportEvent e) {
		
		if (players.containsKey(e.getPlayer())) {
			if (e.getFrom() == null || e.getTo() == null || e.isCancelled() || e.getFrom().distance(e.getTo()) < 0.7)
				return;

			boolean allowTp = false;
			
			for (Location loc : allowedTpLocs)
					if (loc.getBlockX() == e.getTo().getBlockX() && 
						loc.getBlockY() == e.getTo().getBlockY() && 
						loc.getBlockZ() == e.getTo().getBlockZ()) {
						
						allowTp = true;
						break;
					}
			
			if (!allowTp) {
				e.getPlayer().sendMessage(gameType.getChatPrefix() + "§cVous téléporter pendant le jeu est interdit !");
				endGame(AccountProvider.get(e.getPlayer().getUniqueId()), -1, false);	
			}	
		}
	}
	
	@EventHandler(priority = EventPriority.LOW)
	public void playerMoveEvent(PlayerMoveEvent e) {
		if (e.isCancelled() || SpigotUtils.isSameLocation(e.getFrom(), e.getTo()))
			return;
  
		Player p = e.getPlayer();
		
		if (!players.containsKey(p)) {
			if (e.getTo().getBlock().equals(startingLoc.getBlock()))
				startGame((OlympaPlayerHub)AccountProvider.get(p.getUniqueId()));
			
		}else {
			if ((!allowFly && p.isFlying()) || p.getGameMode() != GameMode.ADVENTURE) {
				p.sendMessage(gameType.getChatPrefix() + "§cNe profitez pas de vos permissions pour vous mettre en fly !");
				endGame(AccountProvider.get(e.getPlayer().getUniqueId()), -1, false);
				
			}else
				onMoveHandler(p, e.getFrom(), e.getTo());
		}
	}
	
	/**
	 * Execute actions on player move
	 * @param p
	 * @param from
	 * @param to
	 */
	protected void onMoveHandler(Player p, Location from, Location to) {
		
	}
	
	@EventHandler (priority = EventPriority.HIGH)
	public void onDamage(EntityDamageEvent e) {
		if (players.containsKey(e.getEntity()))
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

	/**
	 * Fires when a player goes out of game area. Execution of default logic (ends game) can be cancelled by returning false.
	 * @param p player
	 * @return true if the default logic has to be executed, false otherwise
	 */
	protected boolean exitGameArea(Player p) {
		return true;
	}
	
	/*
	@EventHandler
	public void onChangeGamemode(PlayerGameModeChangeEvent e) {
		if (players.containsKey(e.getPlayer())) {
			e.getPlayer().sendMessage(gameType.getChatPrefix() + "§cNe profitez pas de vos permissions pour changer de gamemode !");
			endGame(AccountProvider.get(e.getPlayer().getUniqueId()), 0, true);	
		}
	}*/
	
	///////////////////////////////////////////////////////////
	//                      CONFIG INIT                      //
	///////////////////////////////////////////////////////////
	
	/**
	 * Create default values in config if nothing has been found for this game
	 * Only for internal use
	 */
	protected ConfigurationSection initConfig(ConfigurationSection config) {
		//plugin.getLogger().log(Level.INFO, "config avant (dans fct) : " + config);
		
		if (config == null)
			config = MiniGamesManager.getInstance().getConfig().createSection(gameType.toString().toLowerCase());
		
		if (!config.getKeys(false).contains("isEnabled")) 
			config.set("isEnabled", true);
	
		if (!config.getKeys(false).contains("area")) 
			config.set("area", new Cuboid(world, 0, 0, 0, 1, 1, 1));
	
		if (!config.getKeys(false).contains("portal_region")) 
			config.set("portal_region", new Cuboid(world, 0, 0, 0, 1, 1, 1));
		
		if (!config.getKeys(false).contains("holo_loc")) 
			config.set("holo_loc", new Location(world, 0, 0, 0));	
		
		if (!config.getKeys(false).contains("start_loc")) 
			config.set("start_loc", new Location(world, 0, 0, 0));	
		
		return config;
	}
	
	public void setPortal(Region region) {
		if (portalRegionTracked != null) portalRegionTracked.unregister();
		portalRegion = region;
		if (region == null) return;
		
		portalRegionTracked = OlympaCore.getInstance().getRegionManager().registerRegion(portalRegion, gameType.name() + "_portal", EventPriority.NORMAL, new Flag() {
			@Override
			public ActionResult enters(EntryEvent event) {
				beginGame(event.getPlayer());
				return ActionResult.TELEPORT_ELSEWHERE;
			}
		});
	}

	
	///////////////////////////////////////////////////////////
	//                       COMMANDS                        //
	///////////////////////////////////////////////////////////

	/**
	 * Internal function, do NOT call it
	 * @param cmd
	 */
	@Cmd (player = true)
	public void area(CommandContext cmd) {
		Player p = getPlayer();
		
		p.sendMessage(gameType.getChatPrefix() + "§aSélectionnez la région du jeu.");
		  
		new RegionEditor(p, region -> {
			  if (region == null) 
				  return;
			  
			  area = region;
			  config.set("area", region);
			p.sendMessage(gameType.getChatPrefix() + "§aRégion mise à jour avec succès.");
			  
			}).enterOrLeave();
	}

	/**
	 * Internal function, do NOT call it
	 * @param cmd
	 */
	@Cmd (player = true)
	public void hololoc(CommandContext cmd) {
		Location loc = getPlayer().getLocation().getBlock().getLocation().add(0.5, 0, 0.5);
		
		scoresHolo.move(loc);
		config.set("holo_loc", loc);
		
		getPlayer().sendMessage(gameType.getChatPrefix() + "§aLa position de holo_loc a été définie en " +
				loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
	}

	/**
	 * Internal function, do NOT call it
	 * @param cmd
	 */
	@Cmd (player = true)
	public void startLoc(CommandContext cmd) {
		Location loc = getPlayer().getLocation().getBlock().getLocation().add(0.5, 0, 0.5);
		
		allowedTpLocs.remove(startingLoc);
		startingLoc = loc;
		allowedTpLocs.add(startingLoc);
		
		startHolo.move(loc.clone().add(0, 2, 0));
		config.set("start_loc", loc);
		
		getPlayer().sendMessage(gameType.getChatPrefix() + "§aLa position de start_loc a été définie en " +
				loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
	}
	
	@Cmd (player = true)
	public void portal(CommandContext cmd) {
		Player p = getPlayer();
		new RegionEditor(p, region -> {
			if (region == null) return;
			config.set("portal_region", region);
			setPortal(region);
			sendMessage(p, "§aLe portail de téléportation a été défini.");
		}).enterOrLeave();
	}
	
	
}
