package space.itoncek.cvss.server;

import io.javalin.Javalin;
import static io.javalin.apibuilder.ApiBuilder.*;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.service.spi.Stoppable;
import org.hibernate.tool.schema.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.itoncek.cvss.server.db.Match;
import space.itoncek.cvss.server.db.Team;
import space.itoncek.cvss.server.db.UserAccount;
import space.itoncek.cvss.server.managers.LoginManager;
import space.itoncek.cvss.server.managers.Roothandler;
import space.itoncek.cvss.server.managers.TeamManager;

public class CVSS_Server implements Stoppable {
	private static final Logger log = LoggerFactory.getLogger(CVSS_Server.class);
	public final EntityManagerFactory f;
	public final Javalin server;
	private final LoginManager login;
	private final TeamManager teammgr;
	public boolean dev = true;
	//public final WebsocketHandler wsh;

	public CVSS_Server() {
		HibernatePersistenceConfiguration builder = new HibernatePersistenceConfiguration("CVSS")
				.managedClass(Team.class)
				.managedClass(Match.class)
				.managedClass(UserAccount.class)
				// PostgreSQL
				.jdbcUrl(System.getenv("PG_URL") == null ? "jdbc:postgresql://postgres:5432/cvss" : System.getenv("PG_URL"))
				// Credentials
				.jdbcUsername(System.getenv("PG_USR") == null ? "cvss" : System.getenv("PG_USR"))
				.jdbcPassword(System.getenv("PG_PWD") == null ? "cvss" : System.getenv("PG_PWD"))
				// Automatic schema export
				.schemaToolingAction(Action.UPDATE)
				// SQL statement logging
				.showSql(true, true, true);

		f = builder.createEntityManagerFactory();
		login = new LoginManager(this);
		teammgr = new TeamManager(this);

		server = Javalin.create(cfg -> {
			cfg.router.apiBuilder(() -> {
				before(login::checkTokenValidity);
				get("/", Roothandler::root);
				path("uac", () -> {
					post("login", login::login);
				});
				path("config", ()-> {
					get("teams", teammgr::listTeams);
					get("matches", teammgr::listMatches);
					post("updateTeam", teammgr::updateTeam);
					post("updateMatch", teammgr::updateMatch);
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