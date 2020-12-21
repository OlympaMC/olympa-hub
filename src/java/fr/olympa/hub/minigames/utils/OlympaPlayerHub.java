package fr.olympa.hub.minigames.utils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import fr.olympa.api.provider.OlympaPlayerObject;
import fr.olympa.api.sql.SQLColumn;

public class OlympaPlayerHub extends OlympaPlayerObject {
	
	public static final List<SQLColumn<OlympaPlayerHub>> COLUMNS = Arrays.stream(GameType.values()).map(GameType::getScoreColumn).collect(Collectors.toList());
	
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
		game.getScoreColumn().updateAsync(this, score, null, null);
	}
}



