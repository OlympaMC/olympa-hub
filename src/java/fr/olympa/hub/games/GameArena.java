package fr.olympa.hub.games;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import fr.olympa.api.item.ItemUtils;
import fr.olympa.api.provider.AccountProvider;
import fr.olympa.api.region.Region;
import fr.olympa.hub.OlympaHub;

public class GameArena extends IGame{
	
	private List<Player> queuedPlayers = new ArrayList<Player>();

	private List<Player> startingPlayers = new ArrayList<Player>();
	private List<Player> playingPlayers = new ArrayList<Player>();

	//private Region arena;
	
	private Location pos1;
	private Location pos2;
	
	private final int queueCountInvIndex = 7;
	
	public GameArena(OlympaHub plugin, ConfigurationSection config) {
		super(plugin, GameType.ARENA, config);

		//arena = getRegion(config.getString("arena"));
		
		pos1 = getLoc(config.getString("player_1_spawn"));
		pos2 = getLoc(config.getString("player_2_spawn"));
		
		hotBarContent[queueCountInvIndex] = ItemUtils.item(Material.SUNFLOWER, "");
	}
	
	@Override
	protected void startGame(OlympaPlayerHub p) {
		super.startGame(p);
		
		queuedPlayers.add(p.getPlayer());
		updateGameStartDelay();
	}
	
	@Override
	protected void endGame(OlympaPlayerHub p, double score, boolean warpToSpawn) {
		super.endGame(p, score, warpToSpawn);

		queuedPlayers.remove(p.getPlayer());
		startingPlayers.remove(p.getPlayer());
		playingPlayers.remove(p.getPlayer());
		
		updateGameStartDelay();
	}
	
	@Override
	protected void onInterractHandler(PlayerInteractEvent e) {
		if (!playingPlayers.contains(e.getPlayer()))
			return;
		
		e.setCancelled(false);
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onDamage(EntityDamageEvent e) {
		if (e.getEntityType() != EntityType.PLAYER)
			return;
		
		if (!playingPlayers.contains(e.getEntity()))
			return;
		
		Player p = (Player) e.getEntity();
		
		if (p.getHealth() <= p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue())
			playingPlayers.forEach(pp -> {
				if (!pp.equals(p))
					fireWinFor(pp);
			});
		else
			e.setCancelled(false);
	}
	
	@EventHandler
	public void onHeal(EntityRegainHealthEvent e) {
		if (!playingPlayers.contains(e.getEntity()))
			return;
		
		if (e.getRegainReason() == RegainReason.MAGIC || e.getRegainReason() == RegainReason.MAGIC_REGEN)
			return;
		
		e.setCancelled(true);
	}
	
	private void fireWinFor(Player p) {
		playingPlayers.forEach(pp -> pp.setHealth(20d));
		
		endGame(AccountProvider.get(p.getUniqueId()), 1, true);
		
		playingPlayers.forEach(pp -> {
			if (!pp.equals(p))
				endGame(AccountProvider.get(pp.getUniqueId()), 0, true);
		});
		
		playingPlayers.clear();
		tryToInitGame();
	}
	
	/**
	 * MAJ le compteur des joueurs qui joueront avant le joueur concerné
	 */
	private void updateGameStartDelay() {
		for (int i = 0 ; i < queuedPlayers.size() ; i++) {
			ItemStack item = queuedPlayers.get(i).getInventory().getItem(queueCountInvIndex);
			item = ItemUtils.name(item, "§Place dans la file : " + (i + 1));
		}	
	}
	
	private void tryToInitGame() {
		updateGameStartDelay();
		
		if (playingPlayers.size() > 0 || startingPlayers.size() > 0 || queuedPlayers.size() < 2)
			return;

		startingPlayers.add(queuedPlayers.remove(0));
		startingPlayers.add(queuedPlayers.remove(0));
		startGame(3);
	}
	
	private void startGame(int countdown) {
		//gestion timer avant début partie
		if (countdown > 0) {
			startingPlayers.forEach(p -> p.sendTitle("§c" + countdown, "§7Début du match dans...", 0, 20, 0));
			startingPlayers.forEach(p -> p.sendMessage(gameType.getChatPrefix() + "§aDébut du match dans " + countdown));
			plugin.getTask().runTaskLater(() -> startGame(countdown - 1), 20);
		
		//lancement de la partie
		}else {
			if (startingPlayers.size() == 2 && startingPlayers.get(0).isOnline() && startingPlayers.get(1).isOnline()) {
				startingPlayers.forEach(p -> p.getInventory().setItem(queueCountInvIndex, ItemUtils.item(Material.AIR, "")));
				
				ItemStack potHeal = new ItemStack(Material.POTION);
				PotionMeta meta = (PotionMeta) potHeal.getItemMeta();
				meta.addCustomEffect(new PotionEffect(PotionEffectType.HEAL, 1, 0), true);
				meta.setDisplayName("§ePotion de soin I");
				potHeal.setItemMeta(meta);
				potHeal.setAmount(2);
				
				startingPlayers.forEach(p -> p.getInventory().addItem(ItemUtils.item(Material.IRON_SWORD, "§7Epée en fer")));
				startingPlayers.forEach(p -> p.getInventory().addItem(potHeal.clone()));
				startingPlayers.forEach(p -> p.getInventory().setItem(EquipmentSlot.HEAD, ItemUtils.item(Material.IRON_HELMET, "§7Casque")));
				startingPlayers.forEach(p -> p.getInventory().setItem(EquipmentSlot.CHEST, ItemUtils.item(Material.IRON_CHESTPLATE, "§7Plastron")));
				startingPlayers.forEach(p -> p.getInventory().setItem(EquipmentSlot.LEGS, ItemUtils.item(Material.IRON_LEGGINGS, "§7Jambières")));
				startingPlayers.forEach(p -> p.getInventory().setItem(EquipmentSlot.FEET, ItemUtils.item(Material.IRON_BOOTS, "§7Bottes")));

				playingPlayers.addAll(startingPlayers);
				
				startingPlayers.remove(0).teleport(pos1);
				startingPlayers.remove(0).teleport(pos2);
				
			//si l'un des joueurs n'est plus en ligne, annulation du lancement de la partie
			}else {
				startingPlayers.forEach(p -> {
					if (p.isOnline()) {
						p.sendMessage(gameType.getChatPrefix() + "§cVotre adversaire a annulé la partie ! §7En attente d'un nouvau joueur...");
						queuedPlayers.add(0, p);
					}
				});
				startingPlayers.clear();
				tryToInitGame();
			}
		}
	}
}










