package space.itoncek.cvss.server.managers;

import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import space.itoncek.cvss.server.CVSS_Server;
import space.itoncek.cvss.server.db.Match;
import space.itoncek.cvss.server.db.Team;
import space.itoncek.cvss.server.types.Event;

import java.util.List;

@Slf4j
public class TeamManager {
	private final CVSS_Server server;

	public TeamManager(CVSS_Server server) {
		this.server = server;
	}

	public void listTeams(@NotNull Context ctx) {
		server.f.runInTransaction(em -> {
			CriteriaBuilder builder = em.getCriteriaBuilder();
			CriteriaQuery<Team> criteria = builder.createQuery(Team.class);
			criteria.from(Team.class);
			List<Team> data = em.createQuery(criteria).getResultList();

			JSONArray arr = new JSONArray();

			for (Team t : data) {
				arr.put(t.serialize());
			}

			ctx.status(HttpStatus.OK).contentType(ContentType.APPLICATION_JSON).result(arr.toString(4));
		});
	}

	public void listMatches(@NotNull Context ctx) {
		server.f.runInTransaction(em -> {
			CriteriaBuilder builder = em.getCriteriaBuilder();
			CriteriaQuery<Match> criteria = builder.createQuery(Match.class);
			criteria.from(Match.class);
			List<Match> data = em.createQuery(criteria).getResultList();

			JSONArray arr = new JSONArray();

			for (Match m : data) {
				arr.put(m.serialize());
			}

			ctx.status(HttpStatus.OK).contentType(ContentType.APPLICATION_JSON).result(arr.toString(4));
		});
	}

	public void updateTeam(@NotNull Context ctx) {
		JSONObject body = new JSONObject(ctx.body());
		int target = body.getInt("id");
		server.f.runInTransaction(em -> {
			Team team = em.find(Team.class, target);
			team.update(body);
			server.wsMgr.broadcastEvent(Event.TEAM_UPDATE_EVENT);
			ctx.status(HttpStatus.OK).contentType(ContentType.APPLICATION_JSON).result("ok");
		});
	}

	public void updateTeamMembers(@NotNull Context ctx) {
		JSONObject body = new JSONObject(ctx.body());
		int target = body.getInt("id");
		server.f.runInTransaction(em -> {
			Team team = em.find(Team.class, target);
			JSONArray bodyMembers = body.getJSONArray("members");
			team.updateMembers(bodyMembers);
			server.wsMgr.broadcastEvent(Event.TEAM_UPDATE_EVENT);
			ctx.status(HttpStatus.OK).contentType(ContentType.APPLICATION_JSON).result("ok");
		});
	}

	public void updateMatch(@NotNull Context ctx) {
		JSONObject body = new JSONObject(ctx.body());
		int target = body.getInt("id");
		server.f.runInTransaction(em -> {
			Match match = em.find(Match.class, target);
			Team left = em.find(Team.class, body.getInt("leftTeamId"));
			Team right = em.find(Team.class, body.getInt("rightTeamId"));

			match.update(body, left, right);

			server.wsMgr.broadcastEvent(Event.MATCH_UPDATE_EVENT);
			ctx.status(HttpStatus.OK).contentType(ContentType.APPLICATION_JSON).result("ok");
		});
	}

	public void createMatch(@NotNull Context ctx) {
		JSONObject body = new JSONObject(ctx.body());

		server.f.runInTransaction(em -> {
			Team left = em.find(Team.class, body.getInt("leftTeamId"));
			Team right = em.find(Team.class, body.getInt("rightTeamId"));
			em.persist(Match.newMatch(left, right));
			server.wsMgr.broadcastEvent(Event.MATCH_UPDATE_EVENT);
			ctx.status(HttpStatus.OK).contentType(ContentType.APPLICATION_JSON).result("ok");
		});
	}

	public void createTeam(@NotNull Context ctx) {
		JSONObject body = new JSONObject(ctx.body());

		server.f.runInTransaction(em -> {
			em.persist(Team.newTeam(body.getString("name"), body.getString("colorBright"), body.getString("colorDark")));
			server.wsMgr.broadcastEvent(Event.TEAM_UPDATE_EVENT);
			ctx.status(HttpStatus.OK).contentType(ContentType.APPLICATION_JSON).result("ok");
		});
	}

	public void deleteTeam(@NotNull Context ctx) {
		JSONObject body = new JSONObject(ctx.body());

		server.f.runInTransaction(em -> {
			em.remove(em.find(Team.class, body.getInt("id")));
			server.wsMgr.broadcastEvent(Event.TEAM_UPDATE_EVENT);
			ctx.status(HttpStatus.OK).contentType(ContentType.APPLICATION_JSON).result("ok");
		});
	}

	public void deleteMatch(@NotNull Context ctx) {
		JSONObject body = new JSONObject(ctx.body());

		server.f.runInTransaction(em -> {
			em.remove(em.find(Match.class, body.getInt("id")));
			server.wsMgr.broadcastEvent(Event.MATCH_UPDATE_EVENT);
			ctx.status(HttpStatus.OK).contentType(ContentType.APPLICATION_JSON).result("ok");
		});
	}

	public void getTeam(@NotNull Context ctx) {
		JSONObject body = new JSONObject(ctx.body());

		server.f.runInTransaction(em -> {
			Team t = em.find(Team.class, body.getInt("id"));
			ctx.contentType(ContentType.APPLICATION_JSON).status(HttpStatus.OK).result(t.serialize().toString(4));
		});
	}

	public void getMatch(@NotNull Context ctx) {
		JSONObject body = new JSONObject(ctx.body());

		server.f.runInTransaction(em -> {
			Match m = em.find(Match.class, body.getInt("id"));
			ctx.contentType(ContentType.APPLICATION_JSON).status(HttpStatus.OK).result(m.serialize().toString(4));
		});
	}
}