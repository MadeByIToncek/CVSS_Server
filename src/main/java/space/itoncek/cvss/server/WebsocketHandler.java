package space.itoncek.cvss.server;

import io.javalin.websocket.WsConfig;
import io.javalin.websocket.WsContext;
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
			System.out.println("[WS] -> " + h.message());
		});
	}

	public void broadcastEvent(Event e) {
		wsClients.stream().filter(x -> x.session.isOpen()).forEach(x -> {
			x.send(e.toJson().toString(4));
		});
	}
}
