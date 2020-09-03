package fr.olympa.hub.games;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;

import com.google.common.collect.ImmutableMap;

import fr.olympa.api.provider.OlympaPlayerObject;

public class OlympaPlayerHub extends OlympaPlayerObject {

	public static final Map<String, String> COLUMNS = ImmutableMap.<String, String>builder()
			.put("score_elytra", "DOUBLE NOT NULL DEFAULT 0")
			.put("score_jump", "DOUBLE NOT NULL DEFAULT 0")
			.put("score_arena", "DOUBLE NOT NULL DEFAULT 0")
			.put("score_laby", "DOUBLE NOT NULL DEFAULT 0")
			.put("score_dac", "DOUBLE NOT NULL DEFAULT 0")
			
			.build();
	
	private Map<GameType, Double> scores = new HashMap<GameType, Double>();
	
	public OlympaPlayerHub(UUID uuid, String name, String ip) {
		super(uuid, name, ip);
		
		for (GameType game : GameType.values())
			scores.put(game, 0d);
	}
	
	@Override
	public void loadDatas(ResultSet resultSet) throws SQLException {
		for (GameType game : GameType.values())
			scores.put(game, resultSet.getDouble(game.getBddKey()));
	}
	
	@Override
	public void saveDatas(PreparedStatement statement) throws SQLException {
		for (int i = 0 ; i < GameType.values().length ; i++)
			statement.setDouble(i + 1, scores.get(GameType.values()[i]));
	}
	
	/**
	 * Get player score for the specified game.
	 * @param game
	 * @return player score (0 if never played)
	 */
	public double getScore(GameType game) {
		return scores.get(game);
	}
	
	/**
	 * Set player score for specified game.
	 * 
	 * @param game
	 * @param score (musn't be < 0)
	 */
	public void setScore(GameType game, double score) {
		scores.put(game, score);
	}
}



