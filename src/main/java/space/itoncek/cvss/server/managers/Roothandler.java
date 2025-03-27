package space.itoncek.cvss.server.managers;

import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.jetbrains.annotations.NotNull;

public class Roothandler {
	public static void root(@NotNull Context ctx) {
		ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_PLAIN).result(getVersion());
	}

	private static String getVersion() {
		return Roothandler.class.getPackage().getImplementationVersion() == null ? "vDEVELOPMENT" : Roothandler.class.getPackage().getImplementationVersion();
	}
	public static void time(@NotNull Context ctx) {
		ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_PLAIN).result(System.currentTimeMillis() + "");
	}
}
