package main.java.tukano.impl;

import main.java.tukano.api.FollowingI;
import main.java.tukano.api.Result;
import main.java.tukano.api.User;
import main.java.tukano.impl.data.Following;
import main.java.utils.RedisCache;
import main.java.utils.db.CosmosDB;

import java.util.List;
import java.util.logging.Logger;

import static java.lang.String.format;
import static main.java.tukano.api.Result.*;
import static main.java.tukano.api.Result.ErrorCode.FORBIDDEN;
import static main.java.tukano.api.Result.ErrorCode.OK;
import static main.java.tukano.api.Result.ok;

public class JavaFollowing implements FollowingI {

    private static Logger Log = Logger.getLogger(JavaFollowing.class.getName());

    private final CosmosDB db = CosmosDB.getInstance("following");

    private final RedisCache cache = new RedisCache();

    private static FollowingI instance;

    synchronized public static FollowingI getInstance() {
        if( instance == null )
            instance = new JavaFollowing();
        return instance;
    }

    private JavaFollowing() {}

    @Override
    public Result<Void> follow(String userId1, String userId2, boolean isFollowing, String password) {
        Log.info(() -> format("follow : userId1 = %s, userId2 = %s, isFollowing = %s, pwd = %s\n", userId1, userId2, isFollowing, password));

        return errorOrResult( okUser(userId1, password), user -> {
            var f = new Following(userId1, userId2);
            Result<Void> res = errorOrVoid( okUser( userId2), isFollowing ? db.insert( f ) : db.delete( f ));
            if (res.isOK()) {
                if (isFollowing) {
                    cache.delete("followers:" + userId2);
                }
            }
            return res;
        });
    }

    @Override
    public Result<List<String>> followers(String userId, String password) {
        Log.info(() -> format("followers : userId = %s, pwd = %s\n", userId, password));

        Result<List<String>> cachedList = cache.getList("followers:" + userId);
        cache.expire("followers:" + userId, 20);

        if (!cachedList.value().isEmpty()) {
            return ok(cachedList.value());
        }

        var query = format("SELECT f.follower FROM Following f WHERE f.followee = '%s'", userId);
        Result<List<Following>> results =
                errorOrValue( okUser(userId, password), db.sql(query, Following.class));

        if (results.error() != OK) {
            return error(results.error());
        }

        List<String> followers = results.value().stream().map(Following::getFollower).toList();

        cache.insertList("followers:" + userId, followers);
        return ok(followers);
    }

    protected Result<User> okUser(String userId, String pwd) {
        return JavaUsers.getInstance().getUser(userId, pwd);
    }

    private Result<Void> okUser( String userId ) {
        var res = okUser(userId, "");
        if( res.error() == FORBIDDEN )
            return ok();
        else
            return error( res.error() );
    }
}