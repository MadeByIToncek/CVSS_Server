package space.itoncek.cvss.server.managers;

import io.javalin.http.Context;
import io.javalin.websocket.WsConfig;
import io.javalin.websocket.WsContext;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import space.itoncek.cvss.server.CVSS_Server;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class OverlayManager {
	private final CVSS_Server server;
	private final List<WsContext> wsClients = new ArrayList<>();

	public OverlayManager(CVSS_Server server) {
		this.server = server;
	}

	public void showLeftOverlay(@NotNull Context ctx) {
		broadcastOverlayCommand(OverlayCommand.SHOW_LEFT);
		log.info("hit!");
		ctx.result("ok");
	}

	public void hideLeftOverlay(@NotNull Context ctx) {
		broadcastOverlayCommand(OverlayCommand.HIDE_LEFT);
		ctx.result("ok");
	}

	public void showRightOverlay(@NotNull Context ctx) {
		broadcastOverlayCommand(OverlayCommand.SHOW_RIGHT);
		ctx.result("ok");
	}

	public void hideRightOverlay(@NotNull Context ctx) {
		broadcastOverlayCommand(OverlayCommand.HIDE_RIGHT);
		ctx.result("ok");
	}

	public void showTimeOverlay(@NotNull Context ctx) {
		broadcastOverlayCommand(OverlayCommand.SHOW_TIME);
		ctx.result("ok");
	}

	public void hideTimeOverlay(@NotNull Context ctx) {
		broadcastOverlayCommand(OverlayCommand.HIDE_TIME);
		ctx.result("ok");
	}

	public void handleOverlayStream(WsConfig cfg) {
		cfg.onConnect(e -> {
			log.info("Overlay stream has been connected!");
			e.session.setIdleTimeout(Duration.ofDays(365));
			wsClients.add(e);
		});

		cfg.onMessage(h -> {
			log.info("[WS Overlay] -> {}", h.message());
		});
	}

	public void broadcastOverlayCommand(OverlayCommand c) {
		wsClients.stream().filter(x -> x.session.isOpen()).forEach(x -> {
			x.send(c.name());
		});
	}

	public enum OverlayCommand {
		SHOW_RIGHT,
		HIDE_RIGHT,
		SHOW_LEFT,
		HIDE_LEFT,
		SHOW_TIME,
		HIDE_TIME
	}
}
