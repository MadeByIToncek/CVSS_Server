package space.itoncek.cvss.server.db;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;

@Getter
@Setter
@Entity
public class GraphicsInstance {
	@Id
	public String ident;
	@Enumerated(value = EnumType.STRING)
	public GraphicsMode mode;

	public static GraphicsInstance generate(String key, GraphicsMode mode) {
		GraphicsInstance i = new GraphicsInstance();
		i.ident = key;
		i.mode = mode;
		return i;
	}

	public JSONObject serialize() {
		return new JSONObject()
				.put("ident", getIdent())
				.put("mode", getMode());
	}

	public void update(JSONObject o) {
		setMode(o.getEnum(GraphicsInstance.GraphicsMode.class,"mode"));
	}

	public enum GraphicsMode {
		NONE,
		STREAM,
		TV_TWO_LEFT,
		TV_TWO_RIGHT,
		TV_THREE_LEFT,
		TV_THREE_RIGHT,
		TV_THREE_TIME
	}
}
