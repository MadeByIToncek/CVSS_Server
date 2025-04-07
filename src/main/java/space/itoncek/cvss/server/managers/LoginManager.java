package space.itoncek.cvss.server.managers;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.*;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.http.Header;
import io.javalin.http.HttpStatus;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import javalinjwt.JWTGenerator;
import javalinjwt.JWTProvider;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.itoncek.cvss.server.CVSS_Server;
import space.itoncek.cvss.server.db.UserAccount;
import static space.itoncek.cvss.server.utils.Randoms.generateRandomString;

public class LoginManager {
	private static final Logger log = LoggerFactory.getLogger(LoginManager.class);
	private final long validityTimeout = 1800000; // 30 minutes
	private final CVSS_Server server;
	public final Algorithm algorithm;
	public final JWTGenerator<UserAccount> generator;
	public final JWTVerifier verifier;
	public final JWTProvider<UserAccount> provider;

	public LoginManager(CVSS_Server server) {
		this.server = server;
		algorithm = Algorithm.HMAC512(generateRandomString(4096, true, true));

		generator = (userAccount, alg) -> {
			JWTCreator.Builder token = JWT.create()
					.withClaim("id", userAccount.getId())
					.withClaim("validUntil", System.currentTimeMillis() + validityTimeout);
			return token.sign(alg);
		};
		verifier = JWT.require(algorithm).build();
		provider = new JWTProvider<>(algorithm, generator, verifier);
	}

	public void login(Context ctx) {
		try {
			JSONObject body = new JSONObject(ctx.body());

			server.f.runInTransaction(em -> {
				CriteriaBuilder cb = em.getCriteriaBuilder();
				CriteriaQuery<UserAccount> cq = cb.createQuery(UserAccount.class);
				Root<UserAccount> root = cq.from(UserAccount.class);

				Predicate[] predicates = new Predicate[2];
				predicates[0] = cb.equal(root.get("id"), body.getInt("id"));
				predicates[1] = cb.equal(root.get("accessHash"), body.getString("accesshash"));

				TypedQuery<UserAccount> allQuery = em.createQuery(cq.select(root).where(predicates));
				UserAccount userAccount = allQuery.getSingleResult();

				if (userAccount == null) {
					ctx.status(401).result(new JSONObject().put("error", "Unable to authorize this account, this incident has been reported!").toString(4));
					return;
				}

				JSONObject ident = new JSONObject().put("token", generator.generate(userAccount, algorithm));
				ctx.res().setContentType("application/json");
				ctx.result(ident.toString());
			});

		} catch (JSONException e) {
			log.info("JSON Exception", e);
			ctx.status(401).result(new JSONObject().put("error", "Unable to authorize this account, this incident has been reported!").toString(4));
		}
	}

	public void checkTokenValidity(Context h) {
		if (h.path().startsWith("/uac/login")) return;
		else if (h.path().equals("/")) return;
		else if (h.method().equals(HandlerType.GET) && server.dev) return;

		try {
			String header = h.header(Header.AUTHORIZATION);
			if (header == null) {
				throw new MissingClaimException(h.ip());
			}
			DecodedJWT verify = verifier.verify(header.substring(7));

			if (verify.getClaim("validuntil").asLong() < System.currentTimeMillis()) {
				h.status(HttpStatus.IM_A_TEAPOT).result("expired");
			}
		} catch (JSONException | AlgorithmMismatchException | SignatureVerificationException |
				 TokenExpiredException | MissingClaimException | IncorrectClaimException e) {
			log.warn("Invalid token ", e);
			h.status(HttpStatus.UNAUTHORIZED).result("Neplatný token!");
		}
	}
}