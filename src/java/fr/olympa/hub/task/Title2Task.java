package fr.olympa.hub.task;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class Title2Task extends BukkitRunnable {

	int timer = 0;
	private Player player;

	public Title2Task(Player player) {
		this.player = player;
	}

	@Override
	public void run() {
		if (timer == 0)
			player.sendTitle("�e�lOLYMPA", "�e�lZTA �7& �c�lPvPFaction", 0, 60, 0);
		else if (timer == 1)
			player.sendTitle("�6�lOLYMPA", "�e�lZTA �7& �c�lPvPFaction", 0, 60, 0);
		else if (timer == 2)
			player.sendTitle("�e�lOLYMPA", "�e�lZTA �7& �c�lPvPFaction", 0, 60, 0);
		else if (timer == 3)
			player.sendTitle("�6�lOLYMPA", "�e�lZTA �7& �c�lPvPFaction", 0, 60, 0);
		else if (timer == 4)
			player.sendTitle("�e�lOLYMPA", "�e�lZTA �7& �c�lPvPFaction", 0, 60, 0);
		else if (timer == 5)
			player.sendTitle("�6�lOLYMPA", "�e�lZTA �7& �c�lPvPFaction", 0, 60, 0);
		else if (timer == 6)
			player.sendTitle("�e�lOLYMPA", "�e�lZTA �7& �c�lPvPFaction", 0, 60, 0);
		else if (timer == 7)
			player.sendTitle("�e�lOLYMPA", "�e�lZTA �7& �c�lPvPFaction", 0, 600, 0);
		else if (timer == 8) {
			player.sendTitle("�6�lOLYMPA", "�e�lZTA �7& �c�lPvPFaction", 0, 60, 30);
			player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 2, 2);
			cancel();
		}
		timer++;

	}

}
