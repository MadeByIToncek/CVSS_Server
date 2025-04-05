package space.itoncek.cvss.server.db;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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
}
