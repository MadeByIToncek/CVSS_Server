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
	public String nickname;
	@Enumerated(value = EnumType.STRING)
	public GraphicsMode mode;
	public boolean updating;

	public static GraphicsInstance generate(String key, GraphicsMode mode, boolean updating) {
		GraphicsInstance i = new GraphicsInstance();
		i.ident = key;
		i.nickname = "";
		i.mode = mode;
		i.updating = updating;
		return i;
	}

	public JSONObject serialize() {
		return new JSONObject()
				.put("ident", getIdent())
				.put("nickname", getNickname())
				.put("mode", getMode())
				.put("updating",isUpdating());
	}

	public void update(JSONObject o) {
		setNickname(o.getString("nickname"));
		setMode(o.getEnum(GraphicsInstance.GraphicsMode.class,"mode"));
		setUpdating(o.getBoolean("updating"));
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
