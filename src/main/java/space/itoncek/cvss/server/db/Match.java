package space.itoncek.cvss.server.db;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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
	@Enumerated(EnumType.STRING)
	MatchState matchState = MatchState.SETUP;
	@Enumerated(EnumType.STRING)
	Result result = Result.NOT_FINISHED;

	public enum MatchState {
		SETUP,
		PLAYING,
		ENDED
	}

	public enum Result {
		LEFT_WON,
		RIGHT_WON,
		NOT_FINISHED
	}
}
