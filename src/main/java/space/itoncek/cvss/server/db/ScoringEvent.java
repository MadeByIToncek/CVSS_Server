package space.itoncek.cvss.server.db;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class ScoringEvent {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	int id;
	String name;
	@Positive
	int pointAmount;

	public static ScoringEvent createNewScoringEvent(String name, int pointAmount) {
		ScoringEvent se = new ScoringEvent();
		se.pointAmount = pointAmount;
		se.name = name;
		return se;
	}
}
