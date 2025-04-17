package space.itoncek.cvss.server.managers;

import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import space.itoncek.cvss.server.CVSS_Server;
import space.itoncek.cvss.server.db.Keystore;
import static space.itoncek.cvss.server.db.Keystore.KeystoreKeys.CURRENT_MATCH;
import static space.itoncek.cvss.server.db.Keystore.KeystoreKeys.MATCH_STATE;
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

			Keystore currentmatch = em.find(Keystore.class, CURRENT_MATCH.name());
			if (currentmatch != null) {
				em.remove(currentmatch);
			}
			em.persist(Keystore.generateKeystore(CURRENT_MATCH, m.getId() + ""));

			Keystore matchstate = em.find(Keystore.class, MATCH_STATE.name());
			if (matchstate != null) {
				em.remove(matchstate);
			}
			em.persist(Keystore.generateKeystore(MATCH_STATE, "arm"));

			server.wsMgr.broadcastEvent(Event.MATCH_ARM);

			ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_PLAIN).result("ok");
		});
	}

	public void start(@NotNull Context ctx) {
		server.f.runInTransaction(em -> {
			Keystore matchstate = em.find(Keystore.class, MATCH_STATE.name());
			if (matchstate == null) {
				ctx.status(HttpStatus.BAD_REQUEST).contentType(ContentType.TEXT_PLAIN).result("No match armed!");
				return;
			}
			matchstate.value = "start";
			server.wsMgr.broadcastEvent(Event.MATCH_START);
			server.timingMgr.start();

			ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_PLAIN).result("ok");
		});
	}

	public void recycle(@NotNull Context ctx) {
		server.wsMgr.broadcastEvent(Event.MATCH_RECYCLE);
		server.timingMgr.stopClock();
		server.f.runInTransaction(em -> {
			Keystore matchstate = em.find(Keystore.class, MATCH_STATE.name());
			if (matchstate != null) {
				em.remove(matchstate);
			}
			em.persist(Keystore.generateKeystore(MATCH_STATE, "arm"));

			Keystore currentMatch = em.find(Keystore.class, CURRENT_MATCH.name());
			Match match = em.find(Match.class, Integer.parseInt(currentMatch.value));
			match.setMatchState(Match.MatchState.UPCOMING);
			ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_PLAIN).result("ok");
		});
	}

	public void endMatch() {
		if (server.timingMgr.remainingTime.get() > 0) {
			server.wsMgr.broadcastEvent(Event.MATCH_END);
			server.wsMgr.broadcastClockStop();
		}
		server.f.runInTransaction(em -> {
			em.remove(em.find(Keystore.class, CURRENT_MATCH.name()));
			em.remove(em.find(Keystore.class, MATCH_STATE.name()));
		});
	}

	public void isMatchInProgress(@NotNull Context ctx) {
		server.f.runInTransaction(em -> {
			Keystore matchstate = em.find(Keystore.class, MATCH_STATE.name());
			if (matchstate == null) {
				ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_PLAIN).result(Boolean.toString(false));
				return;
			}
			ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_PLAIN).result(Boolean.toString(matchstate.value.equals("start")));
		});
	}

	public void isMatchArmed(@NotNull Context ctx) {
		server.f.runInTransaction(em -> {
			Keystore matchstate = em.find(Keystore.class, MATCH_STATE.name());
			if (matchstate == null) {
				ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_PLAIN).result(Boolean.toString(false));
				return;
			}
			ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_PLAIN).result(Boolean.toString(matchstate.value.equals("arm")));
		});
	}

	public void getLeftTeamId(@NotNull Context ctx) {
		server.f.runInTransaction(em -> {
			Keystore currentMatch = em.find(Keystore.class, CURRENT_MATCH.name());
			if (currentMatch == null || currentMatch.value == null) {
				ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_PLAIN).result(Integer.toString(-1));
				return;
			}
			try {
				Match match = em.find(Match.class, Integer.parseInt(currentMatch.value));
				ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_PLAIN).result(match.getLeft().getId().toString());
			} catch (NumberFormatException e) {
				ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_PLAIN).result(Integer.toString(-1));
			}
		});
	}

	public void getRightTeamId(@NotNull Context ctx) {
		server.f.runInTransaction(em -> {
			Keystore currentMatch = em.find(Keystore.class, CURRENT_MATCH.name());
			if (currentMatch == null || currentMatch.value == null) {
				ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_PLAIN).result(Integer.toString(-1));
				return;
			}
			try {
				Match match = em.find(Match.class, Integer.parseInt(currentMatch.value));
				ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_PLAIN).result(match.getRight().getId().toString());
			} catch (NumberFormatException e) {
				ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_PLAIN).result(Integer.toString(-1));
			}
		});
	}

	public void forceEnd(@NotNull Context ctx) {
		server.timingMgr.stopClock();
		endMatch();
		ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_PLAIN).result("ok");
	}

	public void reset(@NotNull Context ctx) {
		server.wsMgr.broadcastEvent(Event.MATCH_RESET);
		server.f.runInTransaction(em -> {
			Keystore currentMatch = em.find(Keystore.class, CURRENT_MATCH.name());
			Match match = em.find(Match.class, Integer.parseInt(currentMatch.value));
			match.setMatchState(Match.MatchState.UPCOMING);
			em.remove(currentMatch);
			em.remove(em.find(Keystore.class, MATCH_STATE.name()));
			ctx.result("ok");
		});
	}
}
