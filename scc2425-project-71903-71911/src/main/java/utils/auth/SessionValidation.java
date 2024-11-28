package main.java.utils.auth;

import jakarta.ws.rs.NotAuthorizedException;
import main.java.tukano.api.User;
import main.java.utils.RedisCache;

public class SessionValidation {
    static final String COOKIE_KEY = "scc:session";

    private static final RedisCache cache = new RedisCache();

    private SessionValidation() {
    }

    public static User validateSession(String userId) throws NotAuthorizedException {
        var cookie = RequestCookies.get().get(COOKIE_KEY);

        if (cookie == null )
            throw new NotAuthorizedException("No session initialized");

        var session = cache.get(cookie.getValue(), User.class);

        if (session.isOK()) {

            if (session.value().getDisplayName() == null || session.value().getDisplayName().length() == 0)
                throw new NotAuthorizedException("No valid session initialized");

            if (!session.value().getId().equals(userId))
                throw new NotAuthorizedException("Session not valid for user");

            return session.value();
        }
        else {
            throw new NotAuthorizedException("No valid session initialized");
        }
    }
}
