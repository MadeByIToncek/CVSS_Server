package space.itoncek.cvss.server.managers;

import io.javalin.websocket.WsConfig;
import io.javalin.websocket.WsContext;
import space.itoncek.cvss.server.CVSS_Server;
import space.itoncek.cvss.server.types.Event;

import java.util.ArrayList;

public class WebsocketManager {
	private final CVSS_Server server;
	private final ArrayList<WsContext> wsEventClients = new ArrayList<>();
	private final ArrayList<WsContext> wsTimeClients = new ArrayList<>();

	public WebsocketManager(CVSS_Server server) {
		this.server = server;
	}

	public void handleEventStream(WsConfig cfg) {
		cfg.onConnect(wsEventClients::add);

		cfg.onMessage(h -> {
			System.out.println("[WS Event] -> " + h.message());
		});
	}

	public void broadcastEvent(Event e) {
		wsEventClients.stream().filter(x -> x.session.isOpen()).forEach(x -> {
			x.send(e.toString());
		});
	}

	public void handleTimeStream(WsConfig cfg) {
		cfg.onConnect(wsTimeClients::add);

		cfg.onMessage(h -> {
			System.out.println("[WS Time] -> " + h.message());
		});
	}
}
