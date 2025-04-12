package space.itoncek.cvss.server.managers;

import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import jakarta.persistence.criteria.CriteriaQuery;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import space.itoncek.cvss.server.CVSS_Server;
import space.itoncek.cvss.server.db.Keystore;
import static space.itoncek.cvss.server.db.Keystore.KeystoreKeys.CURRENT_MATCH;
import space.itoncek.cvss.server.db.Match;
import space.itoncek.cvss.server.db.ScoreLogEntry;
import space.itoncek.cvss.server.db.ScoringEvent;

import java.util.List;

public class ScoreManager {
	private final CVSS_Server server;

	public ScoreManager(CVSS_Server server) {
		this.server = server;
	}

	public void getScoringEvents(@NotNull Context ctx) {
		server.f.runInTransaction(em -> {
			CriteriaQuery<ScoringEvent> criteria = em.getCriteriaBuilder().createQuery(ScoringEvent.class);
			criteria.from(ScoringEvent.class);
			List<ScoringEvent> data = em.createQuery(criteria).getResultList();

			JSONArray arr = new JSONArray();

			for (ScoringEvent t : data) {
				arr.put(t.serialize());
			}

			ctx.status(HttpStatus.OK).contentType(ContentType.APPLICATION_JSON).result(arr.toString(4));
		});
	}

	public void updateScoringEvent(@NotNull Context ctx) {
		JSONObject body = new JSONObject(ctx.body());
		server.f.runInTransaction(em -> {
			ScoringEvent e = em.find(ScoringEvent.class, body.getInt("id"));

			e.update(body);

			ctx.status(HttpStatus.OK).contentType(ContentType.APPLICATION_JSON).result("ok");
		});
	}

	public void createNewScoringEvent(@NotNull Context ctx) {
		JSONObject body = new JSONObject(ctx.body());
		server.f.runInTransaction(em -> {
			em.persist(ScoringEvent.createNewScoringEvent(body.getString("name"), body.getInt("points")));
			ctx.status(HttpStatus.OK).contentType(ContentType.APPLICATION_JSON).result("ok");
		});
	}

	public void getMatchScoreLog(@NotNull Context ctx) {
		JSONObject body = new JSONObject(ctx.body());
		int target = body.getInt("id");
		server.f.runInTransaction(em -> {
			Match match = em.find(Match.class, target);
			JSONArray arr = new JSONArray();
			for (ScoreLogEntry sle : match.getScoreLog()) {
				arr.put(sle.serialize());
			}

			ctx.status(HttpStatus.OK).contentType(ContentType.APPLICATION_JSON).result(arr.toString(4));
		});
	}

	public void insertNewScoringEvent(@NotNull Context ctx) {
		JSONObject body = new JSONObject(ctx.body());
		int target = body.getInt("id");
		int scoringId = body.getInt("scoring_id");
		boolean side = body.getBoolean("side");
		server.f.runInTransaction(em -> {
			Match match = em.find(Match.class, target);
			ScoringEvent event = em.find(ScoringEvent.class, scoringId);
			ScoreLogEntry sle = ScoreLogEntry.createNewScoreLogEntry(match, side, event);
			match.getScoreLog().add(sle);

			ctx.status(HttpStatus.OK).contentType(ContentType.APPLICATION_JSON).result("ok");
		});
	}

	public void getMatchScore(@NotNull Context ctx) {
		server.f.runInTransaction(em -> {
			Keystore currentMatch = em.find(Keystore.class, CURRENT_MATCH.name());
			if (currentMatch == null) {
				ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).result("-1");
			} else {
				Match m = em.find(Match.class, Integer.parseInt(currentMatch.value));

				int left = 0;
				int right = 0;

				for (ScoreLogEntry sle : m.getScoreLog()) {
					if (sle.isLeftSide()) {
						left += sle.getEvent().getPointAmount();
					} else {
						right += sle.getEvent().getPointAmount();
					}
				}

				ctx.status(HttpStatus.OK).contentType(ContentType.APPLICATION_JSON).result(new JSONObject()
						.put("left", left)
						.put("right", right)
						.toString(4));
			}
		});
	}
}
