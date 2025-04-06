package space.itoncek.cvss.server;

import io.javalin.Javalin;
import static io.javalin.apibuilder.ApiBuilder.*;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.service.spi.Stoppable;
import org.hibernate.tool.schema.Action;
import space.itoncek.cvss.server.db.*;
import space.itoncek.cvss.server.managers.*;

public class CVSS_Server implements Stoppable {
	public final EntityManagerFactory f;
	public final Javalin server;
	public final TeamManager teamMgr;
	public final MatchManager matchMgr;
	public final ScoreManager scoreMgr;
	private final OverlayManager overlayMgr;
	public boolean dev = true;
	public final WebsocketManager wsMgr;
	public TimingManager timingMgr;

	public CVSS_Server() {
		HibernatePersistenceConfiguration builder = new HibernatePersistenceConfiguration("CVSS")
				.managedClass(Team.class)
				.managedClass(Match.class)
				.managedClass(UserAccount.class)
				.managedClass(ScoringEvent.class)
				.managedClass(ScoreLogEntry.class)
				.managedClass(Keystore.class)
				// PostgreSQL
				.jdbcUrl(System.getenv("PG_URL") == null ? "jdbc:postgresql://postgres:5432/cvss" : System.getenv("PG_URL"))
				// Credentials
				.jdbcUsername(System.getenv("PG_USR") == null ? "cvss" : System.getenv("PG_USR"))
				.jdbcPassword(System.getenv("PG_PWD") == null ? "cvss" : System.getenv("PG_PWD"))
				// Automatic schema export
				.schemaToolingAction(Action.UPDATE)
				// SQL statement logging
				.showSql(true, false, true);

		f = builder.createEntityManagerFactory();
		teamMgr = new TeamManager(this);
		wsMgr = new WebsocketManager(this);
		matchMgr = new MatchManager(this);
		scoreMgr = new ScoreManager(this);
		timingMgr = new TimingManager(this);
		overlayMgr = new OverlayManager(this);

		server = Javalin.create(cfg -> {
			cfg.router.apiBuilder(() -> {
				get("/", Roothandler::root);
				get("/time", Roothandler::time);
				get("/defaultMatchLength", timingMgr::getTime);
				path("teams", ()-> {
					get("teams", teamMgr::listTeams);
					put("team", teamMgr::getTeam);
					get("matches", teamMgr::listMatches);
					put("match", teamMgr::getMatch);
					patch("team", teamMgr::updateTeam);
					patch("teamMembers", teamMgr::updateTeamMembers);
					patch("match", teamMgr::updateMatch);
					post("team", teamMgr::createTeam);
					post("match", teamMgr::createMatch);
					delete("team", teamMgr::deleteTeam);
					delete("match", teamMgr::deleteMatch);
				});
				path("match", ()-> {
					post("arm", matchMgr::arm);
					post("start", matchMgr::start);
					post("recycle", matchMgr::recycle);
					post("reset", matchMgr::reset);
					get("leftTeamId", matchMgr::getLeftTeamId);
					get("rightTeamId", matchMgr::getRightTeamId);
					get("matchInProgress", matchMgr::isMatchInProgress);
					get("matchArmed", matchMgr::isMatchArmed);
				});
				path("score", ()->{
					get("events", scoreMgr::getScoringEvents);
					post("event", scoreMgr::createNewScoringEvent);
					patch("event", scoreMgr::updateScoringEvent);
					get("matchScoreLog", scoreMgr::getMatchScoreLog);
					get("matchScore", scoreMgr::getMatchScore);
					post("score", scoreMgr::insertNewScoringEvent);
				});
				path("overlay", ()-> {
					path("left", ()-> {
						put("show", overlayMgr::showLeftOverlay);
						put("hide", overlayMgr::hideLeftOverlay);
					});
					path("right", ()-> {
						put("show", overlayMgr::showRightOverlay);
						put("hide", overlayMgr::hideRightOverlay);
					});
					path("timer", ()-> {
						put("show", overlayMgr::showTimeOverlay);
						put("hide", overlayMgr::hideTimeOverlay);
					});
					ws("stream", overlayMgr::handleOverlayStream);
				});
				path("stream", ()-> {
					ws("event", wsMgr::handleEventStream);
					ws("time", wsMgr::handleTimeStream);
				});
			});
		}).start(4444);
	}

	public static void main(String[] args) {
		CVSS_Server server = new CVSS_Server();

		Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
	}

	@Override
	public void stop() {
		server.stop();
		timingMgr.stop();
		f.close();
	}
}