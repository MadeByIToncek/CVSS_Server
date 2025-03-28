package space.itoncek.cvss.server;

import io.javalin.Javalin;
import static io.javalin.apibuilder.ApiBuilder.*;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.service.spi.Stoppable;
import org.hibernate.tool.schema.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.itoncek.cvss.server.db.*;
import space.itoncek.cvss.server.managers.Roothandler;
import space.itoncek.cvss.server.managers.ScoreManager;
import space.itoncek.cvss.server.managers.TeamManager;
import space.itoncek.cvss.server.managers.WebsocketManager;

public class CVSS_Server implements Stoppable {
	private static final Logger log = LoggerFactory.getLogger(CVSS_Server.class);
	public final EntityManagerFactory f;
	public final Javalin server;
	private final TeamManager teammgr;
	private final ScoreManager scoremgr;
	public boolean dev = true;
	public final WebsocketManager wsh;

	public CVSS_Server() {
		HibernatePersistenceConfiguration builder = new HibernatePersistenceConfiguration("CVSS")
				.managedClass(Team.class)
				.managedClass(Match.class)
				.managedClass(UserAccount.class)
				.managedClass(ScoringEvent.class)
				.managedClass(ScoreLogEntry.class)
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
		scoremgr = new ScoreManager(this);

		server = Javalin.create(cfg -> {
			cfg.router.apiBuilder(() -> {
				get("/", Roothandler::root);
				get("/time", Roothandler::time);
				path("teams", ()-> {
					get("teams", teammgr::listTeams);
					get("matches", teammgr::listMatches);
					patch("team", teammgr::updateTeam);
					patch("match", teammgr::updateMatch);
					post("team", teammgr::createTeam);
					post("match", teammgr::createMatch);
					delete("team", teammgr::deleteTeam);
					delete("match", teammgr::deleteMatch);
				});
				path("score", ()->{
					get("events", scoremgr::getScoringEvents);
					post("event", scoremgr::createNewScoringEvent);
					patch("event", scoremgr::updateScoringEvent);
					get("matchScoreLog", scoremgr::getMatchScoreLog);
					get("matchScore", scoremgr::getMatchScore);
					post("score", scoremgr::insertNewScoringEvent);
				});
				path("ws", ()-> {
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
		f.close();
	}
}