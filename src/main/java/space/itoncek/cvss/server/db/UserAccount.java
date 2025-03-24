package space.itoncek.cvss.server.db;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class UserAccount {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	int id;
	String accessHash;
	@Enumerated(EnumType.STRING)
	Permissions permissions;

	public enum Permissions {
		ADMIN,
		SCOREKEEPER,
		REFEREE,
		OVERLAY_ADMIN,
		OVERLAY_TECHNICIAN,
		READONLY
	}
}
