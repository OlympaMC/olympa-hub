package fr.olympa.hub.minigames.games;

import java.rmi.activation.ActivateFailedException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import fr.olympa.api.common.command.complex.Cmd;
import fr.olympa.api.common.command.complex.CommandContext;
import fr.olympa.api.spigot.editor.RegionEditor;
import fr.olympa.api.common.provider.AccountProvider;
import fr.olympa.api.spigot.region.Region;
import fr.olympa.api.spigot.region.shapes.Cuboid;
import fr.olympa.api.spigot.region.tracking.ActionResult;
import fr.olympa.api.spigot.region.tracking.RegionEvent.EntryEvent;
import fr.olympa.api.spigot.region.tracking.flags.Flag;
import fr.olympa.core.spigot.OlympaCore;
import fr.olympa.hub.OlympaHub;
import fr.olympa.hub.minigames.utils.GameType;
import fr.olympa.hub.minigames.utils.OlympaPlayerHub;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class GameElytra extends AGame {

	private static final int startingDelay = 3;
	
	
	private Location startRaceLoc;
	private Location endRaceLoc;
	
	private Map<Region, Integer> portals = new LinkedHashMap<Region, Integer>();
	
	private Map<Player, Integer> nextPortal = new HashMap<Player, Integer>();
	private Map<Player, Long> startTime = new HashMap<Player, Long>();
	private Map<Player, Location> lastKnownLoc = new HashMap<Player, Location>();
	
	private int lastPortalIndex = 0;
	
	public GameElytra(OlympaHub plugin, ConfigurationSection configFromFile) throws ActivateFailedException {
		super(plugin, GameType.ELYTRA, configFromFile);
		
		//getLoc(config.getString("tp_loc"));
		allowedTpLocs.add(startRaceLoc = config.getLocation("tp_loc"));
		allowedTpLocs.add(endRaceLoc = config.getLocation("end_tp_loc"));
		
		List<Region> listReg = new ArrayList<Region>();
		List<Integer> listInd = new ArrayList<Integer>();

		config.getList("portals_locs").forEach(r -> listReg.add((Region) r));
		config.getList("portals_indexs").forEach(i -> listInd.add((Integer) i));
		
		listInd.forEach(i -> {if (i > lastPortalIndex) lastPortalIndex = i;});
		
		//register des régions
		listReg.forEach(reg -> {
			OlympaCore.getInstance().getRegionManager().registerRegion(reg, "elytra_anneau_" + reg.hashCode(), EventPriority.HIGH, new Flag() {
				@Override
				public ActionResult enters(EntryEvent event) {
					super.enters(event);
					if (getPlayers().contains(event.getPlayer()))
						enterPortal(event.getPlayer(), reg);
					
					return ActionResult.ALLOW;
				}
			});
		});
		
		for (int i = 0 ; i < listReg.size() ; i++)
			portals.put(listReg.get(i), listInd.get(i));
		
		//task d'affichage du temps
		new BukkitRunnable() {
			
			@Override
			public void run() {
				startTime.forEach((p, tps) -> {
					if (tps > 0)
						p.spigot().sendMessage(ChatMessageType.ACTION_BAR, 
								TextComponent.fromLegacyText("§aTemps : §e" + 
								new DecimalFormat("#.##").format(((double)(System.currentTimeMillis() - tps)/1000d)) + "§as"));
				});
			}
		}.runTaskTimer(plugin, 10, 2);
		
		//task de test si le joueur est toujours en train de voler ou non (test effectué sur ses positions successives)
		new BukkitRunnable() {

			@Override
			public void run() {
				getPlayers().forEach(p -> {
					if (p.getLocation().distance(lastKnownLoc.get(p)) <= 0.5 && getPlayerTimeMillis(p) > 1500) {
						p.sendMessage(gameType.getChatPrefix() + "§7Ne vous arrêtez pas de voler !");
						restartGame(AccountProvider.getter().get(p.getUniqueId()));
					}
					
					lastKnownLoc.put(p, p.getLocation().clone());
				});
			}
		}.runTaskTimer(plugin, 20, 10);
	}
	
	private void enterPortal(Player p, Region reg) {
		//int portalIndex = new ArrayList<Region>(portals.keySet()).indexOf(reg);
		int portalIndex = portals.get(reg);
		
		if (nextPortal.get(p) == portalIndex) {
			p.sendMessage(gameType.getChatPrefix() + "§aPorte " + (portalIndex + 1) + " validée !");
			nextPortal.put(p, portalIndex + 1);
			
			if (portalIndex == lastPortalIndex)
				fireEndGameWithSuccess(p);
			
		}else if (nextPortal.get(p) < portalIndex)
			p.sendMessage(gameType.getChatPrefix() + "§cVous n'avez pas validé l'une des portes précédentes.§7 Retournez en arrière ou réinitialisez la partie.");
		else
			p.sendMessage(gameType.getChatPrefix() + "§7Vous avez déjà validé cette porte !");
	}
	
	@Override
	protected boolean startGame(OlympaPlayerHub p) {
		if (!super.startGame(p))
			return false;
		
		launchPreGame(p.getPlayer(), startingDelay);
		
		return true;
	}
	
	@Override
	protected void restartGame(OlympaPlayerHub p) {
		super.restartGame(p);
		
		launchPreGame(p.getPlayer(), startingDelay);
	}
	
	@Override
	protected void endGame(OlympaPlayerHub p, double score, boolean warpToSpawn) {
		super.endGame(p, score, warpToSpawn);
		
		p.getPlayer().removePotionEffect(PotionEffectType.LEVITATION);
		p.getPlayer().setGliding(false);
		p.getPlayer().getInventory().setChestplate(new ItemStack(Material.AIR));
		p.getPlayer().setGliding(false);
		
		nextPortal.remove(p.getPlayer());
		startTime.remove(p.getPlayer());
		lastKnownLoc.remove(p.getPlayer());
		
		if (score > 0 && !warpToSpawn) {
			p.getPlayer().sendMessage(gameType.getChatPrefix() + "§7Téléportation au spawn dans 5 secondes...");
			
			p.getPlayer().teleport(endRaceLoc);
			
			plugin.getTask().runTaskLater(() -> {
				p.getPlayer().teleport(startingLoc);
				p.getPlayer().setGliding(false);	
			}, 100);
		}
	}
	
	
	private void launchPreGame(Player p, int timeLeft) {
		if (!getPlayers().contains(p))
			return;
		
		//actions exécutées une seule fois au début du timer
		if (timeLeft == startingDelay) {
			p.getPlayer().getInventory().setItem(EquipmentSlot.CHEST, new ItemStack(Material.AIR));
			p.setGliding(false);
			
			p.getPlayer().removePotionEffect(PotionEffectType.LEVITATION);
			p.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, startingDelay*20, 0, false, false));
			
			nextPortal.put(p.getPlayer(), 0);
			startTime.put(p.getPlayer(), System.currentTimeMillis() + startingDelay*1000);
			lastKnownLoc.put(p, p.getLocation().clone());

			p.teleport(startRaceLoc);
		}
		
		if (timeLeft > 0) {
			p.sendTitle("§c" + timeLeft, "§7La course débute dans...", 0, 22, 0);
			
			plugin.getTask().runTaskLater(() -> launchPreGame(p, timeLeft - 1), 20);
		}else {
			p.getPlayer().getInventory().setItem(EquipmentSlot.CHEST, new ItemStack(Material.ELYTRA));
			p.sendMessage(gameType.getChatPrefix() + "§eDébut de la course !");
		}
		
	}


	@Override //relance le jeu pour le joueur s'il sort de la zone de jeu alors qu'il était en vol
	protected boolean exitGameArea(Player p) {
		if (getPlayerTimeMillis(p) > 0) {
			restartGame(AccountProvider.getter().get(p.getUniqueId()));
			return false;	
		}else {
			return super.exitGameArea(p);
		}
	}
	
	/*@Override
	protected void onMoveHandler(Player p, Location from, Location to) {
		if (getPlayerTimeMillis(p) < 0 && from.getBlockX() != to.getBlockX() && from.getBlockZ() != to.getBlockZ())
			p.setGliding(false);
	}*/
	
	
	/*
	@EventHandler //actions à effectuer si le joueur atterit
	public void onGlideToogle(EntityToggleGlideEvent e) {
		if (!getPlayers().contains(e.getEntity()))
			return;
		
		if (!e.isGliding())
			if (nextPortal.get(e.getEntity()) > lastPortalIndex)
				fireEndGameWithSuccess((Player) e.getEntity());
			else {
				e.getEntity().sendMessage(gameType.getChatPrefix() + "§7Ne vous arrêtez pas de voler !");
				restartGame(AccountProvider.getter().get(e.getEntity().getUniqueId()));	
			}
	}*/
	
	private long getPlayerTimeMillis(Player p) {
		if (!startTime.containsKey(p))
			return -1;
		
		return System.currentTimeMillis() - startTime.get(p);
	}
	
	private void fireEndGameWithSuccess(Player p) {
		endGame(AccountProvider.getter().get(p.getUniqueId()), 
				((double)(getPlayerTimeMillis(p)))/1000d, false);
	}

	
	///////////////////////////////////////////////////////////
	//                      CONFIG INIT                      //
	///////////////////////////////////////////////////////////
	
	
	protected ConfigurationSection initConfig(ConfigurationSection config) {
		config = super.initConfig(config);
		
		if (!config.contains("end_tp_loc"))
			config.set("end_tp_loc", new Location(world, 0, 0, 0));
		
		if (!config.contains("tp_loc"))
			config.set("tp_loc", new Location(world, 0, 0, 0));
		
		if (!config.contains("portals_locs")) {
			List<Region> list = new ArrayList<Region>();
			list.add(new Cuboid(world, 0, 0, 0, 1, 1, 1));
			list.add(new Cuboid(world, 0, 0, 0, 1, 1, 1));
			list.add(new Cuboid(world, 0, 0, 0, 1, 1, 1));
			list.add(new Cuboid(world, 0, 0, 0, 1, 1, 1));
			config.set("portals_locs", list);	
		}
		
		if (!config.contains("portals_indexs")) {
			List<Integer> list = new ArrayList<Integer>();
			list.add(0);
			list.add(1);
			list.add(1);
			list.add(2);
			config.set("portals_indexs", list);
		}
		
		return config;
	}


	///////////////////////////////////////////////////////////
	//                       COMMANDS                        //
	///////////////////////////////////////////////////////////
	
	
	/**
	 * Internal function, do NOT call it
	 * @param cmd
	 */
	@Cmd (player = true, args = "INTEGER", min = 1)
	public void addPortal(CommandContext cmd) {
		Player p = getPlayer();
		int portalIndex;
		
		if (cmd.getArgument(0) == null)
			return;
		
		portalIndex = cmd.getArgument(0);
		
		p.sendMessage(gameType.getChatPrefix() + "§aSélectionnez la région de l'anneau " + cmd.getArgument(0));
		
		new RegionEditor(p, region -> {
			  if (region == null) 
				  return;
			  
			  portals.put(region, portalIndex);
			  //regs.add(region);
			  
			  //indexs.add(portalIndex);

			  List<Region> regs = new ArrayList<Region>();
			  List<Integer> ids = new ArrayList<Integer>();
			  
			  portals.forEach((reg, id) -> {
				  regs.add(reg);
				  ids.add(id);
			  });
			  
			  config.set("portals_locs", regs);
			  config.set("portals_indexs", ids);
			  
			p.sendMessage(gameType.getChatPrefix() + "§aAnneau ajouté avec succès." +
						"\n§7Attention si l'ordre des anneaux n'est pas respecté ! Soit aucun anneau 0 n'a été trouvé, soit "
						+ "les indexs d'anneaux sautent une étape (par exemple, il y a deux aneaux 0 et 2 mais aucun anneau 1). "
						+ "Veuillez vérifier la configuration ou des erreurs pouaient se produire.");
			  
			}).enterOrLeave();
	}
	
	
	@Cmd (player = true)
	public void listPortals(CommandContext cmd) {
		
		String msg = gameType.getChatPrefix() + "§aListe des anneaux :";
		
		List<Region> list = new ArrayList<Region>(portals.keySet());
		
		for (int i = 0 ; i < list.size() ; i++) {
			Location min = list.get(i).getMin();
			Location max = list.get(i).getMax();
			
			msg += "\n§2Id " + i + " : §eanneau " + min.getBlockX() + "," + min.getBlockY() + "," + min.getBlockZ() + " | " +
						max.getBlockX() + "," + max.getBlockY() + "," + max.getBlockZ() + " §7(étape " + portals.get(list.get(i)) + ")";
			//.event(new ClickEvent(Action.RUN_COMMAND, "/" + gameType.toString().toLowerCase() + " removeportal " + i));
		}
		
		getPlayer().sendMessage(msg);
	}
	
	@Cmd (player = true, min = 1, args = "INTEGER")
	public void removePortal(CommandContext cmd) {
		
		List<Region> regs = new ArrayList<Region>();
		List<Integer> indexs = new ArrayList<Integer>();
		
		portals.forEach((r, i) -> {
			regs.add(r);
			indexs.add(i);
		});
		
		if (cmd.<Integer>getArgument(0) >= regs.size()) {
			getPlayer().sendMessage(gameType.getChatPrefix() + "§cIndex non valide, faites /elytra listportal pour avoir la liste des portails.");
			return;
		}

		regs.remove(((Integer) cmd.getArgument(0)).intValue());
		indexs.remove(((Integer) cmd.getArgument(0)).intValue());

		portals.clear();
		for (int i = 0 ; i < regs.size() ; i++)
			portals.put(regs.get(i), indexs.get(i));
		
		config.set("portals_locs", regs);
		config.set("portals_indexs", indexs);
		
		getPlayer().sendMessage(gameType.getChatPrefix() + "§aPortail " + cmd.getArgument(0) + " supprimé. §7Réexécutez /elytra listportals pour afficher les nouveaux IDs des portails." +
				"\n§7Attention si l'ordre des anneaux n'est pas respecté ! Soit aucun anneau 0 n'a été trouvé, soit "
				+ "les indexs d'anneaux sautent une étape (par exemple, il y a deux aneaux 0 et 2 mais aucun anneau 1). "
				+ "Veuillez vérifier la configuration ou des erreurs pouaient se produire.");
		
	}
	
	
	@Cmd (player = true)
	public void raceStartLoc(CommandContext cmd) {
		Location loc = getPlayer().getLocation();//.getBlock().getLocation().add(0.5, 0, 0.5);
		
		allowedTpLocs.remove(startRaceLoc);
		startRaceLoc = loc;
		allowedTpLocs.add(startRaceLoc);
		
		config.set("tp_loc", loc);
		
		getPlayer().sendMessage(gameType.getChatPrefix() + "§aLa position de tp_loc a été définie en " +
				loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
	}
	
	
	@Cmd (player = true)
	public void raceEndTpLoc(CommandContext cmd) {
		Location loc = getPlayer().getLocation();//.getBlock().getLocation().add(0.5, 0, 0.5);
		
		allowedTpLocs.remove(endRaceLoc);
		endRaceLoc = loc;
		allowedTpLocs.add(endRaceLoc);
		
		config.set("end_race_loc", loc);
		
		getPlayer().sendMessage(gameType.getChatPrefix() + "§aLa position de end_race_loc a été définie en " +
				loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
	}
	

}
