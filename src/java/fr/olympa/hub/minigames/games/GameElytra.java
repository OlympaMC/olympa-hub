package fr.olympa.hub.minigames.games;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.Bukkit;
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

import fr.olympa.api.provider.AccountProvider;
import fr.olympa.api.region.Region;
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
	private Map<Region, Integer> portals = new HashMap<Region, Integer>();
	
	private Map<Player, Integer> nextPortal = new HashMap<Player, Integer>();
	private Map<Player, Long> startTime = new HashMap<Player, Long>();
	
	public GameElytra(OlympaHub plugin, ConfigurationSection config) {
		super(plugin, GameType.ELYTRA, config);
		
		startRaceLoc = getLoc(config.getString("tp_loc"));
		allowedTpLocs.add(startRaceLoc);
		
		List<Region> listReg = new ArrayList<Region>();
		List<Integer> listInd = new ArrayList<Integer>();

		config.getStringList("portals_locs").forEach(s -> listReg.add(getRegion(s)));
		config.getStringList("portals_indexs").forEach(s -> listInd.add(Integer.valueOf(s)));
		
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
	
	private void isEnteringPortal(Player p, Region reg) {
		//int portalIndex = new ArrayList<Region>(portals.keySet()).indexOf(reg);
		int portalIndex = portals.get(reg);
		
		if (nextPortal.get(p) == portalIndex) {
			p.sendMessage(gameType.getChatPrefix() + "§aPorte " + (portalIndex + 1) + " validée !");
			nextPortal.put(p, portalIndex + 1);
			
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
	
	private int getMaxPortalIndex() {
		int i = -1;
		for (int i2 : portals.values())
			if (i2 > i)
				i = i2;
		
		return i;
	}
	
	

}
