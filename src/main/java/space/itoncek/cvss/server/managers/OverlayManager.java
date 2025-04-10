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
import space.itoncek.cvss.server.types.Event;

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
		server.f.runInTransaction(em-> {
			GraphicsInstance instance = em.find(GraphicsInstance.class, ctx.body());
			if(instance == null) {
				GraphicsInstance i = GraphicsInstance.generate(ctx.body(), GraphicsInstance.GraphicsMode.NONE, true);
				em.persist(i);
				ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_PLAIN).result("ok");
			} else {
				ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_PLAIN).result("ok");
			}
		});
	}

	public void reportGraphicsReady(@NotNull Context ctx) {
		server.f.runInTransaction(em-> {
			GraphicsInstance instance = em.find(GraphicsInstance.class, ctx.body());
			if(instance == null) {
				ctx.status(HttpStatus.BAD_REQUEST).contentType(ContentType.TEXT_PLAIN).result("Unable to find this overlay!");
			} else {
				instance.setUpdating(false);
				ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_PLAIN).result("ok");
			}
		});
	}

	public void listGraphicsInstances(@NotNull Context ctx) {
		server.f.runInTransaction(em-> {
			CriteriaBuilder builder = em.getCriteriaBuilder();
			CriteriaQuery<GraphicsInstance> criteria = builder.createQuery(GraphicsInstance.class);
			criteria.from(GraphicsInstance.class);
			List<GraphicsInstance> data = em.createQuery(criteria).getResultList();

			JSONArray arr = new JSONArray();

			for (GraphicsInstance i : data) {
				arr.put(new JSONObject()
						.put("ident", i.getIdent())
						.put("nickname", i.getNickname())
						.put("mode", i.getMode())
						.put("updating",i.isUpdating()));
			}
			ctx.status(HttpStatus.OK).contentType(ContentType.APPLICATION_JSON).result(arr.toString(4));
		});
	}

	public void getGraphicsInstance(@NotNull Context ctx) {
		server.f.runInTransaction(em-> {
			GraphicsInstance i = em.find(GraphicsInstance.class, ctx.body());
			if(i == null) {
				ctx.status(HttpStatus.BAD_REQUEST).contentType(ContentType.TEXT_PLAIN).result("Unable to find this graphics instance!");
			} else {
				ctx.status(HttpStatus.OK).contentType(ContentType.APPLICATION_JSON).result(new JSONObject()
						.put("ident", i.getIdent())
						.put("nickname", i.getNickname())
						.put("mode", i.getMode())
						.put("updating",i.isUpdating()).toString());
			}
		});
	}

	public void updateGraphicsInstance(@NotNull Context ctx) {
		JSONObject body = new JSONObject(ctx.body());
		server.f.runInTransaction(em-> {
			GraphicsInstance gi = em.find(GraphicsInstance.class, body.getString("ident"));
			gi.setNickname(body.getString("nickname"));
			gi.setMode(body.getEnum(GraphicsInstance.GraphicsMode.class,"mode"));
			gi.setUpdating(body.getBoolean("updating"));
			ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_PLAIN).result("ok");
		});
		server.wsMgr.broadcastEvent(Event.GRAPHICS_UPDATE_EVENT);
	}

	public void getProbe(@NotNull Context ctx) {
		server.f.runInTransaction(em -> {
			Keystore probe = em.find(Keystore.class, "probe");
			if(probe == null) {
				em.persist(Keystore.generateKeystore("probe","true"));
				ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_PLAIN).result("true");
			} else {
				ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_PLAIN).result("%b".formatted(Boolean.parseBoolean(probe.value)));
			}
		});
	}
	public void setProbe(@NotNull Context ctx) {
		server.f.runInTransaction(em -> {
			Keystore probe = em.find(Keystore.class, "probe");
			probe.setValue(ctx.body());
			ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_PLAIN).result("ok");
		});
		server.wsMgr.broadcastEvent(Event.GRAPHICS_UPDATE_EVENT);
	}

	public void broadcastGraphicsCommand(GraphicsCommand c) {
		log.info("{} {}", List.of(GraphicsCommand.SHOW_TIME, GraphicsCommand.SHOW_LEFT, GraphicsCommand.SHOW_RIGHT).contains(c)?"Showing":"Hiding",
				List.of(GraphicsCommand.HIDE_LEFT, GraphicsCommand.SHOW_LEFT).contains(c)?"left third":List.of(GraphicsCommand.HIDE_RIGHT, GraphicsCommand.SHOW_RIGHT).contains(c)?"right third":"timer");
		wsClients.stream().filter(x -> x.session.isOpen()).forEach(x -> x.send(c.name()));
	}

	public enum GraphicsCommand {
		SHOW_RIGHT,
		HIDE_RIGHT,
		SHOW_LEFT,
		HIDE_LEFT,
		SHOW_TIME,
		HIDE_TIME
	}
}
