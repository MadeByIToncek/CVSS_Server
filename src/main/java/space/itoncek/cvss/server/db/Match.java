package space.itoncek.cvss.server.db;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;

import java.util.List;

@Getter
@Setter
@Entity
public class Match {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	int id;
	@ManyToOne(targetEntity = Team.class)
	Team left;
	@ManyToOne(targetEntity = Team.class)
	Team right;
	@OneToMany(targetEntity = ScoreLogEntry.class, cascade = CascadeType.ALL, mappedBy = "match")
	List<ScoreLogEntry> scoreLog;
	@Enumerated(EnumType.STRING)
	MatchState matchState = MatchState.UPCOMING;
	@Enumerated(EnumType.STRING)
	Result result = Result.NOT_FINISHED;

	public static Match newMatch(Team l, Team r) {
		Match m = new Match();
		m.setLeft(l);
		m.setRight(r);
		m.setResult(Result.NOT_FINISHED);
		m.setMatchState(MatchState.UPCOMING);
		return m;
	}

	public JSONObject serialize() {
		return new JSONObject()
				.put("id", getId())
				.put("leftTeamId", getLeft().getId())
				.put("rightTeamId", getRight().getId())
				.put("state", getMatchState().toString())
				.put("result", getResult().toString());
	}

	public void update(JSONObject o, Team l, Team r) {
		setMatchState(o.getEnum(Match.MatchState.class, "matchState"));
		setResult(o.getEnum(Match.Result.class, "result"));
		setLeft(l);
		setRight(r);
	}

	public enum MatchState {
		UPCOMING,
		PLAYING,
		ENDED
	}

	public enum Result {
		LEFT_WON,
		RIGHT_WON,
		NOT_FINISHED
	}
}
