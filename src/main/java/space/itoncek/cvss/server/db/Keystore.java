package space.itoncek.cvss.server.db;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Keystore {
	@Id
	public String key;
	public String value;

	public static Keystore generateKeystore(String key, String value) {
		Keystore ks = new Keystore();
		ks.key= key;
		ks.value = value;
		return ks;
	}
}
