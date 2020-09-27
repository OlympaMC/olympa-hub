package fr.olympa.hub.minigames.games;

import java.rmi.activation.ActivateFailedException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import fr.olympa.api.command.complex.Cmd;
import fr.olympa.api.command.complex.CommandContext;
import fr.olympa.api.editor.RegionEditor;
import fr.olympa.api.provider.AccountProvider;
import fr.olympa.api.region.Region;
import fr.olympa.api.region.shapes.Cuboid;
import fr.olympa.api.region.tracking.ActionResult;
import fr.olympa.api.region.tracking.TrackedRegion;
import fr.olympa.api.region.tracking.flags.Flag;
import fr.olympa.core.spigot.OlympaCore;
import fr.olympa.hub.OlympaHub;
import fr.olympa.hub.minigames.utils.GameType;
import fr.olympa.hub.minigames.utils.OlympaPlayerHub;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class GameElytra extends IGame {

	private static final int startingDelay = 3;
	
	
	private Location startRaceLoc;
	private Map<Region, Integer> portals = new LinkedHashMap<Region, Integer>();
	
	private Map<Player, Integer> nextPortal = new HashMap<Player, Integer>();
	private Map<Player, Long> startTime = new HashMap<Player, Long>();
	
	public GameElytra(OlympaHub plugin, ConfigurationSection configFromFile) throws ActivateFailedException {
		super(plugin, GameType.ELYTRA, configFromFile);
		
		//getLoc(config.getString("tp_loc"));
		allowedTpLocs.add(startRaceLoc = config.getLocation("tp_loc"));
		
		List<Region> listReg = new ArrayList<Region>();
		List<Integer> listInd = new ArrayList<Integer>();

		config.getList("portals_locs").forEach(r -> listReg.add((Region) r));
		config.getList("portals_indexs").forEach(i -> listInd.add((Integer) i));
		
		//config.getStringList("portals_locs").forEach(s -> listReg.add(getRegion(s)));
		//config.getStringList("portals_indexs").forEach(s -> listInd.add(Integer.valueOf(s)));
		
		//register des régions
		listReg.forEach(reg -> {
			OlympaCore.getInstance().getRegionManager().registerRegion(reg, "elytra_anneau_" + reg.hashCode(),
					EventPriority.HIGH, new Flag() {
				@Override
				public ActionResult enters(Player p, Set<TrackedRegion> to) {
					super.enters(p, to);
					if (getPlayers().contains(p.getUniqueId()))
						isEnteringPortal(p, reg);
					
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
	}
	
	private int getMaxPortalIndex() {
		int i = -1;
		for (int i2 : portals.values())
			if (i2 > i)
				i = i2;
		
		return i;
	} 
	
	private void isEnteringPortal(Player p, Region reg) {
		//int portalIndex = new ArrayList<Region>(portals.keySet()).indexOf(reg);
		int portalIndex = portals.get(reg);
		
		if (nextPortal.get(p) == portalIndex) {
			p.sendMessage(gameType.getChatPrefix() + "§aPorte " + (portalIndex + 1) + " validée !");
			nextPortal.put(p, portalIndex + 1);
			
			if (portalIndex + 1 == getMaxPortalIndex())
				p.setGliding(!p.isGliding());
			
		}else if (nextPortal.get(p) < portalIndex)
			p.sendMessage(gameType.getChatPrefix() + "§cVous n'avez pas validé l'une des portes précédentes.§7 Retournez en arrière ou réinitialisez la partie.");
		else
			p.sendMessage(gameType.getChatPrefix() + "§7Vous avez déjà validé cette porte !");
	}
	
	@Override
	protected void startGame(OlympaPlayerHub p) {
		super.startGame(p);
		
		ItemStack elytras = new ItemStack(Material.ELYTRA);
		ItemMeta meta = elytras.getItemMeta();
		meta.setUnbreakable(true);
		elytras.setItemMeta(meta);
		
		p.getPlayer().getInventory().setItem(EquipmentSlot.CHEST, elytras);
		//nextPortal.put(p.getPlayer(), 0);
		//startTime.put(p.getPlayer(), System.currentTimeMillis() -startingDelay*1000);
		
		launchPreGame(p.getPlayer(), startingDelay);
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
		nextPortal.remove(p.getPlayer());
		startTime.remove(p.getPlayer());
		
		if (score > 0 && !warpToSpawn) {
			p.getPlayer().sendMessage(gameType.getChatPrefix() + "§7Téléportation au spawn dans 5 secondes...");
			plugin.getTask().runTaskLater(() -> p.getPlayer().teleport(startingLoc), 100);
		}
	}
	
	
	
	
	private void launchPreGame(Player p, int timeLeft) {
		if (!getPlayers().contains(p.getUniqueId()))
			return;
		
		if (timeLeft == startingDelay) {
			p.getPlayer().removePotionEffect(PotionEffectType.LEVITATION);
			p.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, startingDelay*20, 0, false, false));
			
			nextPortal.put(p.getPlayer(), 0);
			startTime.put(p.getPlayer(), System.currentTimeMillis() + startingDelay*1000);
			
			p.setGliding(false);
			p.teleport(startRaceLoc);
		}	
		
		if (timeLeft > 0) {
			p.sendTitle("§c" + timeLeft, "§7La course débute dans...", 0, 22, 0);
			
			plugin.getTask().runTaskLater(() -> launchPreGame(p, timeLeft - 1), 20);
		}else {
			//p.removePotionEffect(PotionEffectType.LEVITATION);
			p.sendMessage(gameType.getChatPrefix() + "§eDébut de la course !");
		}
		
	}
	
	
	
	
	
	@EventHandler //actions à effectuer si le joueur atterit
	public void onGlideToogle(EntityToggleGlideEvent e) {
		if (!getPlayers().contains(e.getEntity().getUniqueId()))
			return;
		
		if (System.currentTimeMillis() - startTime.get(e.getEntity()) < 0)
			e.setCancelled(true);
		
		if (!e.isGliding())
			if (nextPortal.get(e.getEntity()) >= getMaxPortalIndex())
				endGame(AccountProvider.get(e.getEntity().getUniqueId()), 
						((double)(System.currentTimeMillis() - startTime.get(e.getEntity())))/1000d, false);
			else {
				e.getEntity().sendMessage(gameType.getChatPrefix() + "§7Ne vous arrêtez pas de voler !");
				restartGame(AccountProvider.get(e.getEntity().getUniqueId()));	
			}
	}

	
	///////////////////////////////////////////////////////////
	//                      CONFIG INIT                      //
	///////////////////////////////////////////////////////////
	
	
	protected ConfigurationSection initConfig(ConfigurationSection config) {
		config = super.initConfig(config);
		
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
	

}
