package fr.olympa.hub.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import fr.olympa.api.utils.Prefix;
import fr.olympa.hub.OlympaHub;
import net.minecraft.server.v1_15_R1.EntityHuman;
import net.minecraft.server.v1_15_R1.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_15_R1.PacketPlayOutNamedEntitySpawn;
import net.minecraft.server.v1_15_R1.PacketPlayOutSpawnEntity;

public class VanishManager implements Listener {
	
	private Set<Player> playersUsingVanish = new HashSet<Player>();
	private Map<Player, Long> lastToogle = new HashMap<Player, Long>();
	
	public VanishManager(){
		OlympaHub.getInstance().getServer().getPluginManager().registerEvents(this, OlympaHub.getInstance());
	}

	public boolean isUsingVanish(Player p) {
		return playersUsingVanish.contains(p);
	}
	
	/**
	 * Toogle vanish mode for player p
	 * @param p player to which hide others players
	 * @return true if action was executed, false otherwiee. May be false if player spams too fast the option
	 */
	public boolean toogleVanish(Player p) {
		if (System.currentTimeMillis() < lastToogle.get(p) + 5000) {
			Prefix.DEFAULT_BAD.sendMessage(p, "Attends un peu avant de réutiliser cette option !");
			return false;
		}
		
		if (!playersUsingVanish.remove(p))
			playersUsingVanish.add(p);
		
		if (isUsingVanish(p))
			Prefix.DEFAULT_GOOD.sendMessage(p, "Les joueurs ont été cachés !");
		else
			Prefix.DEFAULT_GOOD.sendMessage(p, "Les joueurs sont réapparus !");
		
		Bukkit.getOnlinePlayers().forEach(pp -> sendPacket(p, pp));
		
		lastToogle.put(p, System.currentTimeMillis());
		return true;
	}
	
	private void sendPacket(Player p, Player target) {
		List<Player> list = new ArrayList<Player>();
		list.add(target);
		sendPacket(p, list);
	}
	
	private void sendPacket(Player p, List<Player> targets) {
		targets.remove(p);
        //EntityPlayer targ = ((CraftPlayer)target).getHandle(); //Target entity
        
        /*DataWatcher w = targ.getDataWatcher(); //Target entity datawatcher
        
        if (isUsingVanish(p))
        	w.set(new DataWatcherObject<>(0, DataWatcherRegistry.a), (byte) 0x20); //0x20 makes player invisible but still rendered
        else
        	w.set(new DataWatcherObject<>(0, DataWatcherRegistry.a), (byte) 0x0); //0x0 makes player visible
        
        ((CraftPlayer)p).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityMetadata(targ.getId(), w, true));*/
        //((CraftPlayer)p).getHandle().playerConnection.sendPacket(new packetplayout
        
		if (isUsingVanish(p)) {
	        int[] ps = new int[targets.size()];
	        
	        for (int i = 0 ; i < targets.size() ; i++)
	        	ps[i] = ((CraftPlayer)targets.get(i)).getHandle().getId();
	        
	        //delete entities packet
	        ((CraftPlayer)p).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityDestroy(ps));	
		}else 
			targets.forEach(pp -> ((CraftPlayer)p).getHandle().playerConnection
					.sendPacket(new PacketPlayOutNamedEntitySpawn((EntityHuman) ((CraftEntity)pp).getHandle())));
		
	}
	
	/*private void sendVisiblePacket(Player p, Player target) {
		if (p.equals(target))
			return;
		//new PacketPlayOutEntityMetadata
		PacketPlayOutRemoveEntityEffect packet = new PacketPlayOutRemoveEntityEffect(((CraftPlayer)target).getHandle().getId(), 
				MobEffects.INVISIBILITY);
		((CraftPlayer)p).getHandle().playerConnection.sendPacket(packet);
	}*/
	
	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		lastToogle.put(e.getPlayer(), System.currentTimeMillis() - 10000);
		
		OlympaHub.getInstance().getTask().runTaskLater(() -> playersUsingVanish.forEach(p -> sendPacket(p, e.getPlayer())), 5);
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		lastToogle.remove(e.getPlayer());
	}
}
