package space.itoncek.cvss.server.db;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

@Setter
@Getter
@Entity
public class Team {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	private String name;
	private String colorBright;
	private String colorDark;
	@ElementCollection
	public List<String> members;

	public static Team newTeam(String name, String colorBright, String colorDark) {
		var course = new Team();
		course.setName(name);
		course.setColorBright(colorBright);
		course.setColorDark(colorDark);
		return course;
	}

	public JSONObject serialize() {
		return new JSONObject()
				.put("id", getId())
				.put("name", getName())
				.put("colorBright", getColorBright())
				.put("colorDark", getColorDark())
				.put("members", new JSONArray(getMembers()));
	}

	public void update(JSONObject body) {
		setName(body.getString("name"));
		setColorBright(body.getString("colorBright"));
		setColorDark(body.getString("colorDark"));
	}

	public void updateMembers(JSONArray a) {
		members.clear();
		for (int i = 0; i < a.length(); i++) {
			members.add(a.getString(i));
		}
	}
}
