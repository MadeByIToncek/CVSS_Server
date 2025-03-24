package space.itoncek.cvss.server.managers;

import io.javalin.http.Context;
import org.jetbrains.annotations.NotNull;
import space.itoncek.cvss.server.CVSS_Server;

public class TeamManager {
	private final CVSS_Server server;

	public TeamManager(CVSS_Server server) {
		this.server = server;
	}

	public void listTeams(@NotNull Context ctx) {

	}

	public void listMatches(@NotNull Context ctx) {

	}

	public void updateTeam(@NotNull Context ctx) {

	}

	public void updateMatch(@NotNull Context ctx) {

	}
}
