package main.java.tukano.impl;

import com.mongodb.client.model.Filters;
import main.java.tukano.api.LikesI;
import main.java.tukano.api.Result;
import main.java.tukano.api.User;
import main.java.tukano.impl.data.Likes;
import main.java.utils.RedisCache;
import main.java.utils.db.CosmosDB;
import main.java.utils.db.MongoDB;
import org.bson.conversions.Bson;

import java.util.List;
import java.util.logging.Logger;

import static java.lang.String.format;
import static main.java.tukano.api.Result.*;
import static main.java.tukano.api.Result.ErrorCode.OK;

public class JavaLikes implements LikesI {

    private static Logger Log = Logger.getLogger(JavaLikes.class.getName());

   // private final CosmosDB db = CosmosDB.getInstance("likes");
   private final MongoDB db = new MongoDB("likes");

    private final RedisCache cache = new RedisCache();

    private static LikesI instance;

    synchronized public static LikesI getInstance() {
        if (instance == null)
            instance = new JavaLikes();
        return instance;
    }

    private JavaLikes() {
    }

    @Override
    public Result<Void> like(String shortId, String userId, boolean isLiked, String password) {
        Log.info(() -> format("like : id = %s, userId = %s, isLiked = %s, pwd = %s\n", shortId, userId, isLiked, password));

        return errorOrResult( JavaShorts.getInstance().getShort(shortId), shrt -> {
            var l = new Likes(userId, shortId, shrt.getOwnerId());

            Result<Void> res = errorOrVoid( okUser(userId, password), isLiked ? db.insert( l ) : db.delete( l.getId() ));
            if (res.isOK()) {
                cache.delete("likes:" + shortId);
            }
            return res;
        });
    }

    @Override
    public Result<List<String>> likes(String shortId, String password) {
        Log.info(() -> format("likes : id = %s, pwd = %s\n", shortId, password));

        Result<List<String>> cachedList = cache.getList("likes:" + shortId);
        if (!cachedList.value().isEmpty()) {
            return ok(cachedList.value());
        }

        return errorOrResult( JavaShorts.getInstance().getShort(shortId), shrt -> {

            //var query = format("SELECT l.userId FROM Likes l WHERE l.shortId = '%s'", shortId);
            Bson query = Filters.eq("shortId", shortId);

            Result<List<Likes>> results = errorOrValue( okUser( shrt.getOwnerId(), password ), db.sql(query, Likes.class));

            if (results.error() != OK) {
                return error(results.error());
            }

            List<String> likes = results.value().stream().map(Likes::getUserId).toList();

            cache.insertList("likes:" + shortId, likes);
            return ok(likes);
        });
    }

    protected Result<User> okUser(String userId, String pwd) {
        return JavaUsers.getInstance().getUser(userId, pwd);
    }
}