package fr.olympa.hub.task;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import fr.olympa.hub.OlympaHub;

public class TitleTask extends BukkitRunnable {

	int timer = 0;
	private Player player;

	public TitleTask(Player player) {
		this.player = player;
	}

	@Override
	public void run() {
		timer++;
		if (timer == 1 && timer < 6)
			player.sendTitle("�6�lOLYMPA", "�e�lZTA �7& �c�lPvPFaction", 0, 60, 0);
		else if (timer == 7)
			player.sendTitle("�f�lO�6�lLYMPA", "�e�lZTA �7& �c�lPvPFaction", 0, 60, 0);
		else if (timer == 8)
			player.sendTitle("�e�lO�f�lL�6�lYMPA", "�e�lZTA �7& �c�lPvPFaction", 0, 60, 0);
		else if (timer == 9)
			player.sendTitle("�f�lO�e�lL�f�lY�6�lMPA", "�e�lZTA �7& �c�lPvPFaction", 0, 60, 0);
		else if (timer == 10)
			player.sendTitle("�6�lO�f�lL�e�lY�f�lM�6�lPA", "�e�lZTA �7& �c�lPvPFaction", 0, 60, 0);
		else if (timer == 11)
			player.sendTitle("�6�lOL�F�lY�e�lM�f�lP�6�lA", "�e�lZTA �7& �c�lPvPFaction", 0, 60, 0);
		else if (timer == 12)
			player.sendTitle("�6�lOLY�f�lM�e�lP�f�lA", "�e�lZTA �7& �c�lPvPFaction", 0, 60, 0);
		else if (timer == 13)
			player.sendTitle("�6�lOLYM�f�lP�e�lA", "�e�lZTA �7& �c�lPvPFaction", 0, 60, 0);
		else if (timer == 14)
			player.sendTitle("�6�lOLYMP�f�lA", "�e�lZTA �7& �c�lPvPFaction", 0, 60, 0);
		else if (timer == 15)
			player.sendTitle("�6�lOLYMPA", "�e�lZTA �7& �c�lPvPFaction", 0, 60, 0);
		else if (timer == 17)
			new Title2Task(player).runTaskTimer(OlympaHub.getInstance(), 8, 8);
		else if (timer == 18) cancel();
	}

}
