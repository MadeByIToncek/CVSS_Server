package space.itoncek.cvss.server.db;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;

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

	public JSONObject serialize() {
		return new JSONObject()
				.put("id",getId())
				.put("name", getName())
				.put("pointAmount", getPointAmount());
	}

	public void update(JSONObject o) {
		setName(o.getString("name"));
		setPointAmount(o.getInt("points"));
	}
}
