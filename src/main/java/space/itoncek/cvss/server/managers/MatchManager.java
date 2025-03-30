package space.itoncek.cvss.server.managers;

import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import space.itoncek.cvss.server.CVSS_Server;
import space.itoncek.cvss.server.db.Keystore;
import space.itoncek.cvss.server.db.Match;
import space.itoncek.cvss.server.types.Event;

public class MatchManager {
	private final CVSS_Server server;

	public MatchManager(CVSS_Server server) {
		this.server = server;
	}

	public void arm(@NotNull Context ctx) {
		JSONObject body = new JSONObject(ctx.body());
		int target = body.getInt("id");
		server.f.runInTransaction(em -> {
			Match m = em.find(Match.class, target);
			m.setMatchState(Match.MatchState.PLAYING);

			Keystore currentmatch = em.find(Keystore.class, "current_match");
			if (currentmatch != null) {
				em.remove(currentmatch);
			}
			em.persist(Keystore.generateKeystore("current_match", m.getId() + ""));

			Keystore matchstate = em.find(Keystore.class, "match_state");
			if (matchstate != null) {
				em.remove(matchstate);
			}
			em.persist(Keystore.generateKeystore("match_state", "arm"));

			server.wsh.broadcastEvent(Event.MATCH_ARM);

			ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_PLAIN).result("ok");
		});
	}

	public void start(@NotNull Context ctx) {
		server.f.runInTransaction(em -> {
			Keystore matchstate = em.find(Keystore.class, "match_state");
			if (matchstate == null) {
				ctx.status(HttpStatus.BAD_REQUEST).contentType(ContentType.TEXT_PLAIN).result("No match armed!");
				return;
			}
			matchstate.value = "start";
			server.wsh.broadcastEvent(Event.MATCH_START);
			server.timingManager.start();

			ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_PLAIN).result("ok");
		});
	}

	public void endMatch() {
		server.wsh.broadcastEvent(Event.MATCH_END);
		server.wsh.broadcastClockStop();
		server.f.runInTransaction(em -> {
			em.remove(em.find(Keystore.class, "current_match"));
			em.remove(em.find(Keystore.class, "match_state"));
		});
	}

	public void isMatchInProgress(@NotNull Context ctx) {
		server.f.runInTransaction(em -> {
			Keystore matchstate = em.find(Keystore.class, "match_state");
			if (matchstate == null) {
				ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_PLAIN).result(Boolean.toString(false));
				return;
			}
			ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_PLAIN).result(Boolean.toString(matchstate.value.equals("start")));
		});
	}

	public void getLeftTeamId(@NotNull Context ctx) {

	}

	public void getRightTeamId(@NotNull Context ctx) {

	}

	public void forceEnd(@NotNull Context ctx) {
		server.timingManager.stopClock();
		endMatch();
	}
}
