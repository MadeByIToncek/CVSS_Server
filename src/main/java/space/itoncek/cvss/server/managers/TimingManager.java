package space.itoncek.cvss.server.managers;

import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.service.spi.Stoppable;
import org.jetbrains.annotations.NotNull;
import space.itoncek.cvss.server.CVSS_Server;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class TimingManager implements Stoppable {
	private final CVSS_Server server;
	private static final int time = 10;
	private final ScheduledExecutorService ses = Executors.newScheduledThreadPool(4);
	AtomicInteger remainingTime = new AtomicInteger(-1);

	public TimingManager(CVSS_Server server) {
		this.server = server;
		ses.scheduleAtFixedRate(() -> {
			if (remainingTime.get() > 0) {
				int time = remainingTime.decrementAndGet();
				if (time < 0) {
					server.matchMgr.endMatch();
				} else {
					server.wsMgr.broadcastRemainingTime(time);
				}
			}
			//log.info("Tick! {}",remainingTime.get());
		}, 1, 1, TimeUnit.SECONDS);
	}

	public void getTime(@NotNull Context ctx) {
		ctx.status(HttpStatus.OK).contentType(ContentType.TEXT_PLAIN).result("%d".formatted(time));
	}

	public void start() {
		server.wsMgr.broadcastClockStart();
		remainingTime.set(time);
	}

	@Override
	public void stop() {
		try {
			if (ses.awaitTermination(5, TimeUnit.SECONDS)) {
				log.info("Clean shutdown :)");
			} else {
				log.info("Abrupt shutdown!");
			}
		} catch (InterruptedException e) {
			log.info("Interrupted shutdown!");
		}
	}

	public void stopClock() {
		remainingTime.set(-1);
	}
}
