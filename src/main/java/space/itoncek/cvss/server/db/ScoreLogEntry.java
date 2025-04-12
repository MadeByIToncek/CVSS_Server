package space.itoncek.cvss.server.db;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;

@Getter
@Setter
@Entity(name = "scorelog")
public class ScoreLogEntry {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	int id;
	@ManyToOne(targetEntity = Match.class)
	Match match;
	/**
	 * Left = true, Right = false
	 */
	boolean leftSide;
	@ManyToOne(targetEntity = ScoringEvent.class)
	ScoringEvent event;

	public static ScoreLogEntry createNewScoreLogEntry(Match match, boolean side, ScoringEvent event) {
		ScoreLogEntry sle = new ScoreLogEntry();
		sle.match = match;
		sle.leftSide = side;
		sle.event = event;
		return sle;
	}

	public JSONObject serialize() {
		return new JSONObject()
				.put("id",getId())
				.put("matchId", getMatch().getId())
				.put("leftSide", isLeftSide())
				.put("event", getEvent().getId());
	}
}
