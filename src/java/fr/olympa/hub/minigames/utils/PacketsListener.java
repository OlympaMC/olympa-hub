package fr.olympa.hub.minigames.utils;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;

import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import fr.olympa.hub.OlympaHub;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;

public class PacketsListener implements Listener{

	private Set<BiFunction<Player, Object, Boolean>> onRecieve = new HashSet<BiFunction<Player,Object,Boolean>>();
	private Set<BiFunction<Player, Object, Boolean>> onSend = new HashSet<BiFunction<Player,Object,Boolean>>();
	
	public PacketsListener(OlympaHub plugin) {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		handlePlayerPackets(e.getPlayer());
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		unhandlePlayerPacket(e.getPlayer());
	}
	

    private void unhandlePlayerPacket(Player player) {
        Channel channel = ((CraftPlayer) player).getHandle().playerConnection.networkManager.channel;
        channel.eventLoop().submit(() -> {
            channel.pipeline().remove(player.getName());
            return null;
        });
    }

    /**
     * Execute the function when a packet is recieved by the server
     * @param function
     */
    public void onPacketRecieve(BiFunction<Player, Object, Boolean> function) {
    	onRecieve.add(function);
    }

    /**
     * Execute the function when a packet is send by the server
     * @param function
     */
    public void onPacketSend(BiFunction<Player, Object, Boolean> function) {
    	onSend.add(function);
    }
    
    private void handlePlayerPackets(Player player) {
    	
        ChannelDuplexHandler channelDuplexHandler = new ChannelDuplexHandler() {
        	
            @Override
            public void channelRead(ChannelHandlerContext channelHandlerContext, Object handledPacket) throws Exception {
            	for (BiFunction<Player, Object, Boolean> fun : onRecieve)
            		if (!fun.apply(player, handledPacket))
            			return;
            	
            	super.channelRead(channelHandlerContext, handledPacket);
            }

            @Override
            public void write(ChannelHandlerContext channelHandlerContext, Object packet, ChannelPromise channelPromise) throws Exception {
            	for (BiFunction<Player, Object, Boolean> fun : onSend)
            		if (!fun.apply(player, packet))
            			return;
            	
            	super.write(channelHandlerContext, packet, channelPromise);
            }
        };

        ChannelPipeline pipeline = ((CraftPlayer) player).getHandle().playerConnection.networkManager.channel.pipeline();
        pipeline.addBefore("packet_handler", player.getName(), channelDuplexHandler);

    }
}
