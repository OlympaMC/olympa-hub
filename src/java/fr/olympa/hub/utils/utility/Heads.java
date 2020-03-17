package fr.olympa.hub.utils.utility;

import java.lang.reflect.Field;
import java.util.Base64;
import java.util.UUID;

import org.bukkit.inventory.meta.ItemMeta;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

public class Heads {

	public static void setHead(ItemMeta im, String playerSkullTexture) {
		GameProfile profile = new GameProfile(UUID.randomUUID(), null);
		byte[] encodedData = Base64.getEncoder().encode(String.format("{textures:{SKIN:{url:\"%s\"}}}", playerSkullTexture).getBytes());
		profile.getProperties().put("textures", new Property("textures", new String(encodedData)));
		Field profileField = null;
		try {
			profileField = im.getClass().getDeclaredField("profile");
			profileField.setAccessible(true);
			profileField.set(im, profile);
		}catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e1) {
			e1.printStackTrace();
		}
	}

}