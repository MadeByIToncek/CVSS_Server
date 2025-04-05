package space.itoncek.cvss.server.managers;

import io.javalin.websocket.WsConfig;
import io.javalin.websocket.WsContext;
import lombok.extern.slf4j.Slf4j;
import space.itoncek.cvss.server.CVSS_Server;
import space.itoncek.cvss.server.types.Event;

import java.time.Duration;
import java.util.ArrayList;

@Slf4j
public class WebsocketManager {
	private final CVSS_Server server;
	private final ArrayList<WsContext> wsEventClients = new ArrayList<>();
	private final ArrayList<WsContext> wsTimeClients = new ArrayList<>();

	public WebsocketManager(CVSS_Server server) {
		this.server = server;
	}

	public void handleEventStream(WsConfig cfg) {
		cfg.onConnect(e -> {
			e.session.setIdleTimeout(Duration.ofDays(365));
			wsEventClients.add(e);
		});

		cfg.onMessage(h -> {
			log.info("[WS Event] -> {}", h.message());
		});
	}

	public void broadcastEvent(Event e) {
		log.info("Broadcasting event {}",e.name());
		wsEventClients.stream().filter(x -> x.session.isOpen()).forEach(x -> {
			x.send(e.name());
		});
	}

	public void handleTimeStream(WsConfig cfg) {
		cfg.onConnect(e -> {
			e.session.setIdleTimeout(Duration.ofDays(365));
			wsTimeClients.add(e);
		});

		cfg.onMessage(h -> {
			log.info("[WS Time] -> {}", h.message());
		});
	}

	public void broadcastClockStart() {
		wsTimeClients.stream().filter(x -> x.session.isOpen()).forEach(x -> {
			x.send(-1);
		});
	}
	public void broadcastClockStop() {
		wsTimeClients.stream().filter(x -> x.session.isOpen()).forEach(x -> {
			x.send(-2);
		});
	}

	public void broadcastRemainingTime(int remainingSeconds) {
		wsTimeClients.stream().filter(x -> x.session.isOpen()).forEach(x -> {
			x.send(remainingSeconds);
		});
	}
}
