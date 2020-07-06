package fr.olympa.hub.pads;

import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import fr.olympa.api.command.complex.Cmd;
import fr.olympa.api.command.complex.CommandContext;
import fr.olympa.api.command.complex.ComplexCommand;
import fr.olympa.hub.HubPermissions;
import fr.olympa.hub.OlympaHub;

public class LaunchPadCommand extends ComplexCommand {
	
	private LaunchPadManager pads;
	
	public LaunchPadCommand(LaunchPadManager pads) {
		super(OlympaHub.getInstance(), "launchpads", "Permet de gérer les launchpads.", HubPermissions.LAUNCHPADS_COMMAND, "pads");
		super.addArgumentParser("OPERATION", Operation.class);
		this.pads = pads;
	}
	
	@Cmd (player = true, min = 4, args = { "OPERATION", "DOUBLE", "DOUBLE", "DOUBLE" }, syntax = "<type d'opération> <vecteur X> <vecteur Y> <vecteur Z>")
	public void set(CommandContext cmd) {
		Block block = player.getTargetBlockExact(3);
		if (block == null) {
			sendError("Tu dois regarder le bloc lanceur.");
			return;
		}
		
		Operation operation = cmd.getArgument(0);
		Vector vec = new Vector(cmd.<Double>getArgument(1), cmd.<Double>getArgument(2), cmd.<Double>getArgument(3));
		
		pads.addLaunchPad(new LaunchPad(block.getLocation(), vec, operation), true);
		
		sendSuccess("Le launchpad a été créé avec succès.");
	}
	
}
