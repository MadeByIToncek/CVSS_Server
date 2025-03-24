package space.itoncek.cvss.server;

import io.javalin.websocket.WsConfig;
import io.javalin.websocket.WsContext;
import io.javalin.websocket.WsMessageContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import org.json.JSONObject;
import space.itoncek.cvss.server.db.Team;
import space.itoncek.cvss.server.types.Event;

import java.util.ArrayList;

public class WebsocketHandler {
	private final CVSS_Server server;
	private final ArrayList<WsContext> wsClients = new ArrayList<>();

	public WebsocketHandler(CVSS_Server server) {
		this.server = server;
	}

	public void handle(WsConfig cfg) {
		cfg.onConnect(wsClients::add);

		cfg.onMessage(h -> {
			JSONObject o = new JSONObject(h.message());
			if (!o.has("op")) {
				h.send(new JSONObject().put("op", 64).put("b", new JSONObject().put("reason", "Missing \"op\" value!")).toString(4));
				return;
			}
			switch (o.getInt("op")) {
				case 1 -> handleLogon(h);
				case 3 -> handleRequest(h, o.getJSONObject("b"));
				case 6 -> handleMethod(h, o.getJSONObject("b"));
				default ->
						h.send(new JSONObject().put("op", 64).put("b", new JSONObject().put("reason", "Wrong \"op\" value! Only 1,3,6 are allowed! Sent " + o.getInt("op"))).toString(4));
			}
		});
	}

	public void broadcastEvent(Event e) {
		wsClients.stream().filter(x -> x.session.isOpen()).forEach(x -> {
			x.send(e.toJson().toString(4));
		});
	}

	private void handleLogon(WsMessageContext h) {
		//h.send(getVersion());
	}

	private void handleRequest(WsMessageContext h, JSONObject o) {
		if (!o.has("rq_op")) {
			h.send(new JSONObject().put("op", 64).put("b", new JSONObject().put("reason", "Missing \"rq_op\" value!")).toString(4));
			return;
		}

		switch (o.getInt("rq_op")) {
			case 1 -> listTeams(h);
		}
	}

	private void listTeams(WsMessageContext h) {
		server.f.runInTransaction(em -> {
			CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<Team> q = cb.createQuery(Team.class);
		});
	}

	private void handleMethod(WsMessageContext h, JSONObject o) {
	}


}
