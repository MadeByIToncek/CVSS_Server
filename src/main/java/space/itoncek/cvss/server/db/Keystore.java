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

	public static Keystore generateKeystore(KeystoreKeys key, String value) {
		Keystore ks = new Keystore();
		ks.key = key.name();
		ks.value = value;
		return ks;
	}

	public enum KeystoreKeys {
		PROBE,
		CURRENT_MATCH,
		MATCH_STATE
	}
}
