package fr.olympa.hub.games;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import fr.olympa.api.provider.AccountProvider;
import fr.olympa.api.region.Region;
import fr.olympa.api.region.tracking.ActionResult;
import fr.olympa.api.region.tracking.TrackedRegion;
import fr.olympa.api.region.tracking.flags.Flag;
import fr.olympa.core.spigot.OlympaCore;
import fr.olympa.hub.OlympaHub;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class GameElytra extends IGame {

	private static int startingDelay = 3;
	
	private Location startLoc;
	private Map<Region, Integer> portals = new HashMap<Region, Integer>();
	
	private Map<Player, Integer> nextPortal = new HashMap<Player, Integer>();
	private Map<Player, Long> startTime = new HashMap<Player, Long>();
	
	public GameElytra(OlympaHub plugin, ConfigurationSection config) {
		super(plugin, GameType.ELYTRA, config);
		
		startLoc = getLoc("tp_loc");
		allowedTpLocs.add(startLoc);
		
		List<Region> listReg = new ArrayList<Region>();
		List<Integer> listInd = new ArrayList<Integer>();

		config.getStringList("portals_locs").forEach(s -> listReg.add(getRegion(s)));
		config.getStringList("portals_indexs").forEach(s -> listInd.add(Integer.valueOf(s)));
		
		//register des régions
		listReg.forEach(reg -> {
			OlympaCore.getInstance().getRegionManager().registerRegion(reg, "elytra_anneau_" + reg.getMax().getBlockX(), EventPriority.HIGH, new Flag() {
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
	
	private void isEnteringPortal(Player p, Region reg) {
		int portalIndex = new ArrayList<Region>(portals.keySet()).indexOf(reg);
		
		if (nextPortal.get(p) == portalIndex) {
			p.sendMessage(gameType + "§aPorte " + (portalIndex + 1) + " validée !");
			nextPortal.put(p, portalIndex + 1);
			
		}else if (nextPortal.get(p) < portalIndex)
			p.sendMessage(gameType + "§cVous n'avez pas validé l'une des portes précédentes. §7Retournez en arrière ou réinitialisez la partie.");
		else
			p.sendMessage(gameType + "§7Vous avez déjà validé cette porte !");
	}
	
	@Override
	protected void startGame(OlympaPlayerHub p) {
		super.startGame(p);
				
		p.getPlayer().getInventory().setItem(EquipmentSlot.CHEST, new ItemStack(Material.ELYTRA));
		nextPortal.put(p.getPlayer(), 0);
		startTime.put(p.getPlayer(), System.currentTimeMillis() -startingDelay*1000);
		
		launchPreGame(p.getPlayer(), startingDelay);
	}
	
	@Override
	protected void restartGame(OlympaPlayerHub p) {
		super.restartGame(p);
		p.getPlayer().removePotionEffect(PotionEffectType.LEVITATION);
		nextPortal.put(p.getPlayer(), 0);
		startTime.put(p.getPlayer(), System.currentTimeMillis() -startingDelay*1000);
		
		launchPreGame(p.getPlayer(), startingDelay);
	}
	
	@Override
	protected void endGame(OlympaPlayerHub p, double score, boolean warpToSpawn) {
		super.endGame(p, score, warpToSpawn);
		p.getPlayer().removePotionEffect(PotionEffectType.LEVITATION);
		nextPortal.remove(p.getPlayer());
		startTime.remove(p.getPlayer());
		
		if (!warpToSpawn) {
			p.getPlayer().sendMessage(gameType.getChatPrefix() + "§7Téléportation au spawn dans 5 secondes...");
			plugin.getTask().runTaskLater(() -> p.getPlayer().teleport(startingLoc), 100);
		}
	}
	
	
	
	
	private void launchPreGame(Player p, int timeLeft) {
		if (timeLeft == 3) {
			p.teleport(startingLoc);
			p.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 60, 0, false, false));
		}	
		
		if (timeLeft > 0) {
			p.sendTitle("§c" + timeLeft, "§7La course débute dans...", 0, 22, 0);
			
			plugin.getTask().runTaskLater(() -> launchPreGame(p, timeLeft - 1), 20);
		}else {
			p.removePotionEffect(PotionEffectType.LEVITATION);
			p.sendMessage(gameType.getChatPrefix() + "§eDébut de la course !");
		}
		
	}
	
	
	
	
	
	@EventHandler //actions à effectuer si le joueur atterit
	public void onGlideToogle(EntityToggleGlideEvent e) {
		if (!getPlayers().contains(e.getEntity().getUniqueId()))
			return;
		
		if (!e.isGliding())
			if (nextPortal.get(e.getEntity()) >= portals.size())
				endGame(AccountProvider.get(e.getEntity().getUniqueId()), 
						((double)(System.currentTimeMillis() - startTime.get(e.getEntity())))/1000d, false);
			else {
				e.getEntity().sendMessage(gameType.getChatPrefix() + "§7Ne vous arrêtez pas de voler !");
				restartGame(AccountProvider.get(e.getEntity().getUniqueId()));	
			}
	}
	
	
	
	

}
