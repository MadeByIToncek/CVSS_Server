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
	public final TeamManager teammgr;
	public final MatchManager matchMgr;
	public final ScoreManager scoreMgr;
	public boolean dev = true;
	public final WebsocketManager wsh;
	public TimingManager timingManager;

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
		teammgr = new TeamManager(this);
		wsh = new WebsocketManager(this);
		matchMgr = new MatchManager(this);
		scoreMgr = new ScoreManager(this);
		timingManager = new TimingManager(this);

		server = Javalin.create(cfg -> {
			cfg.router.apiBuilder(() -> {
				get("/", Roothandler::root);
				get("/time", Roothandler::time);
				path("teams", ()-> {
					get("teams", teammgr::listTeams);
					put("team", teammgr::getTeam);
					get("matches", teammgr::listMatches);
					put("match", teammgr::getMatch);
					patch("team", teammgr::updateTeam);
					patch("match", teammgr::updateMatch);
					post("team", teammgr::createTeam);
					post("match", teammgr::createMatch);
					delete("team", teammgr::deleteTeam);
					delete("match", teammgr::deleteMatch);
				});
				path("match", ()-> {
					post("arm", matchMgr::arm);
					post("start", matchMgr::start);
					post("force-end", matchMgr::forceEnd);
					get("leftTeamId", matchMgr::getLeftTeamId);
					get("rightTeamId", matchMgr::getRightTeamId);
					get("matchInProgress", matchMgr::isMatchInProgress);
				});
				path("score", ()->{
					get("events", scoreMgr::getScoringEvents);
					post("event", scoreMgr::createNewScoringEvent);
					patch("event", scoreMgr::updateScoringEvent);
					get("matchScoreLog", scoreMgr::getMatchScoreLog);
					get("matchScore", scoreMgr::getMatchScore);
					post("score", scoreMgr::insertNewScoringEvent);
				});
				path("stream", ()-> {
					ws("event", wsh::handleEventStream);
					ws("time", wsh::handleTimeStream);
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
		timingManager.stop();
		f.close();
	}
}