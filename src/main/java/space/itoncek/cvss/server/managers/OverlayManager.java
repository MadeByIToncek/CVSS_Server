package space.itoncek.cvss.server.managers;

import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.websocket.WsConfig;
import io.javalin.websocket.WsContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import space.itoncek.cvss.server.CVSS_Server;
import space.itoncek.cvss.server.db.GraphicsInstance;
import space.itoncek.cvss.server.db.Keystore;
import static space.itoncek.cvss.server.db.Keystore.KeystoreKeys.PROBE;
import space.itoncek.cvss.server.types.Event;
import space.itoncek.cvss.server.types.GraphicsCommand;

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
		broadcastGraphicsCommand(GraphicsCommand.SHOW_LEFT);
		ctx.result("ok");
	}

	public void hideLeftOverlay(@NotNull Context ctx) {
		broadcastGraphicsCommand(GraphicsCommand.HIDE_LEFT);
		ctx.result("ok");
	}

	public void showRightOverlay(@NotNull Context ctx) {
		broadcastGraphicsCommand(GraphicsCommand.SHOW_RIGHT);
		ctx.result("ok");
	}

	public void hideRightOverlay(@NotNull Context ctx) {
		broadcastGraphicsCommand(GraphicsCommand.HIDE_RIGHT);
		ctx.result("ok");
	}

	public void showTimeOverlay(@NotNull Context ctx) {
		broadcastGraphicsCommand(GraphicsCommand.SHOW_TIME);
		ctx.result("ok");
	}

	public void hideTimeOverlay(@NotNull Context ctx) {
		broadcastGraphicsCommand(GraphicsCommand.HIDE_TIME);
		ctx.result("ok");
	}

	public void handleOverlayStream(WsConfig cfg) {
		cfg.onConnect(e -> {
			e.session.setIdleTimeout(Duration.ofDays(365));
			wsClients.add(e);
		});

		cfg.onMessage(h -> log.info("[WS Overlay] -> {}", h.message()));
	}

	public void registerGraphicsClient(@NotNull Context ctx) {
		server.f.runInTransaction(em -> {
			GraphicsInstance instance = em.find(GraphicsInstance.class, ctx.body());
			if (instance == null) {
				GraphicsInstance i = GraphicsInstance.generate(ctx.body(), GraphicsInstance.GraphicsMode.NONE);
				em.persist(i);
				ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_PLAIN).result("ok");
			} else {
				ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_PLAIN).result("ok");
			}
		});
	}

	public void listGraphicsInstances(@NotNull Context ctx) {
		server.f.runInTransaction(em -> {
			CriteriaBuilder builder = em.getCriteriaBuilder();
			CriteriaQuery<GraphicsInstance> criteria = builder.createQuery(GraphicsInstance.class);
			criteria.from(GraphicsInstance.class);
			List<GraphicsInstance> data = em.createQuery(criteria).getResultList();

			JSONArray arr = new JSONArray();

			for (GraphicsInstance i : data) {
				arr.put(i.serialize());
			}
			ctx.status(HttpStatus.OK).contentType(ContentType.APPLICATION_JSON).result(arr.toString(4));
		});
	}

	public void getGraphicsInstance(@NotNull Context ctx) {
		server.f.runInTransaction(em -> {
			GraphicsInstance i = em.find(GraphicsInstance.class, ctx.body());
			if (i == null) {
				ctx.status(HttpStatus.BAD_REQUEST).contentType(ContentType.TEXT_PLAIN).result("Unable to find this graphics instance!");
			} else {
				ctx.status(HttpStatus.OK).contentType(ContentType.APPLICATION_JSON).result(i.serialize().toString(4));
			}
		});
	}

	public void updateGraphicsInstance(@NotNull Context ctx) {
		JSONObject body = new JSONObject(ctx.body());
		server.f.runInTransaction(em -> {
			GraphicsInstance gi = em.find(GraphicsInstance.class, body.getString("ident"));
			gi.update(body);
			ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_PLAIN).result("ok");
		});
		server.wsMgr.broadcastEvent(Event.GRAPHICS_UPDATE_EVENT);
	}

	public void getProbe(@NotNull Context ctx) {
		server.f.runInTransaction(em -> {
			Keystore probe = em.find(Keystore.class, PROBE.name());
			if (probe == null) {
				ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_PLAIN).result("true");
			} else {
				ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_PLAIN).result("%b".formatted(Boolean.parseBoolean(probe.value)));
			}
		});
	}

	public void setProbe(@NotNull Context ctx) {
		server.f.runInTransaction(em -> {
			Keystore probe = em.find(Keystore.class, PROBE.name());
			if(probe==null) {
				probe = Keystore.generateKeystore(PROBE,Boolean.toString(Boolean.parseBoolean(ctx.body())));
				em.persist(probe);
			} else probe.setValue(Boolean.toString(Boolean.parseBoolean(ctx.body())));
			ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_PLAIN).result("ok");
		});
		server.wsMgr.broadcastEvent(Event.GRAPHICS_UPDATE_EVENT);
	}

	public void broadcastGraphicsCommand(GraphicsCommand c) {
		log.info("{} {}", List.of(GraphicsCommand.SHOW_TIME, GraphicsCommand.SHOW_LEFT, GraphicsCommand.SHOW_RIGHT).contains(c) ? "Showing" : "Hiding",
				List.of(GraphicsCommand.HIDE_LEFT, GraphicsCommand.SHOW_LEFT).contains(c) ? "left third" : List.of(GraphicsCommand.HIDE_RIGHT, GraphicsCommand.SHOW_RIGHT).contains(c) ? "right third" : "timer");
		wsClients.stream().filter(x -> x.session.isOpen()).forEach(x -> x.send(c.name()));
	}

}
