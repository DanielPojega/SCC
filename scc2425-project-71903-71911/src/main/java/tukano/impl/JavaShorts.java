package tukano.impl;

import static java.lang.String.format;
import static tukano.api.Result.error;
import static tukano.api.Result.errorOrResult;
import static tukano.api.Result.errorOrValue;
import static tukano.api.Result.errorOrVoid;
import static tukano.api.Result.ok;
import static tukano.api.Result.ErrorCode.BAD_REQUEST;
import static tukano.api.Result.ErrorCode.FORBIDDEN;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import tukano.api.Blobs;
import tukano.api.Result;
import tukano.api.Short;
import tukano.api.Shorts;
import tukano.api.User;
import tukano.impl.data.Following;
import tukano.impl.data.Likes;
import tukano.impl.rest.TukanoRestServer;
import utils.RedisCache;
import utils.db.CosmosDB;

public class JavaShorts implements Shorts {

	private static Logger Log = Logger.getLogger(JavaShorts.class.getName());

	private final CosmosDB db = CosmosDB.getInstance("shorts");

	private final RedisCache cache = new RedisCache();

	private static Shorts instance;

	synchronized public static Shorts getInstance() {
		if( instance == null )
			instance = new JavaShorts();
		return instance;
	}

	private JavaShorts() {}


	@Override
	public Result<Short> createShort(String userId, String password) {
		Log.info(() -> format("createShort : userId = %s, pwd = %s\n", userId, password));

		return errorOrResult( okUser(userId, password), user -> {

			var shortId = format("%s+%s", userId, UUID.randomUUID());
			var blobUrl = format("%s/%s/%s", TukanoRestServer.serverURI, Blobs.NAME, shortId);
			var shrt = new Short(shortId, userId, blobUrl);

			Result<Short> res = errorOrValue(db.insert(shrt), s -> s.copyWithLikes_And_Token(0));
			if (res.isOK()) {
				cache.delete("shorts:" + userId);
			}

			return res;
		});
	}

	@Override
	public Result<Short> getShort(String shortId) {
		Log.info(() -> format("getShort : shortId = %s\n", shortId));

		if( shortId == null ) return error(BAD_REQUEST);

		String key = User.class.getName() + "" + shortId;

		Result<Short> cachedShort = db.get(key, Short.class);
		if (cachedShort.value() != null) {
			return cachedShort;
		}

		var query = format("SELECT count(*) FROM Likes l WHERE l.shortId = '%s'", shortId);
		var likes = db.sql(query, Long.class);
		Result<Short> shrt = errorOrValue( db.get(shortId, Short.class), shr -> shr.copyWithLikes_And_Token( likes.value().get(0)));
		cache.insert(key, shrt.value());
		return shrt;
	}


	@Override
	public Result<Void> deleteShort(String shortId, String password) {
		Log.info(() -> format("deleteShort : shortId = %s, pwd = %s\n", shortId, password));

		return errorOrResult( getShort(shortId), shrt ->
				errorOrResult( okUser( shrt.getOwnerId(), password), user -> {
							Result<Short> deleteShortResult = (Result<Short>) db.delete(shrt);
							if (deleteShortResult.error() != null) return Result.error(deleteShortResult.error());
							cache.delete(Short.class.getName() + "" + shortId);

							String deleteLikesQuery = format("SELECT * FROM Likes l WHERE l.shortId = '%s'", shortId);
							List<Likes> likesToDelete = db.sql(deleteLikesQuery, Likes.class).value();
							for (Likes like : likesToDelete) {
								db.delete(like);
							}
							cache.delete("likes:" + shortId);

							return JavaBlobs.getInstance().delete(shrt.getBlobUrl(), Token.get() );
						}
				));
	}

	@Override
	public Result<List<String>> getShorts(String userId) {
		Log.info(() -> format("getShorts : userId = %s\n", userId));

		Result<List<String>> cachedList = cache.getList("shorts:" + userId);
		if (cachedList.value() != null) {
			return ok(cachedList.value());
		}

		var query = format("SELECT s.shortId FROM Short s WHERE s.ownerId = '%s'", userId);
		Result<List<String>> shrtList = errorOrValue( okUser(userId), db.sql( query, String.class));
		cache.insertList("shorts:" + userId, shrtList.value());
		return shrtList;
	}

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
		if (cachedList.value() != null) {
			return ok(cachedList.value());
		}

		var query = format("SELECT f.follower FROM Following f WHERE f.followee = '%s'", userId);
		Result<List<String>> followers = errorOrValue( okUser(userId, password), db.sql(query, String.class));
		cache.insertList("followers:" + userId, followers.value());
		return followers;
	}

	@Override
	public Result<Void> like(String shortId, String userId, boolean isLiked, String password) {
		Log.info(() -> format("like : shortId = %s, userId = %s, isLiked = %s, pwd = %s\n", shortId, userId, isLiked, password));


		return errorOrResult( getShort(shortId), shrt -> {
			var l = new Likes(userId, shortId, shrt.getOwnerId());
			Result<Void> res = errorOrVoid( okUser(userId, password), isLiked ? db.insert( l ) : db.delete( l ));
			if (res.isOK()) {
				cache.delete("likes:" + shortId);
			}
			return res;
		});
	}

	@Override
	public Result<List<String>> likes(String shortId, String password) {
		Log.info(() -> format("likes : shortId = %s, pwd = %s\n", shortId, password));

		Result<List<String>> cachedList = cache.getList("likes:" + shortId);
		if (cachedList.value() != null) {
			return ok(cachedList.value());
		}

		return errorOrResult( getShort(shortId), shrt -> {

			var query = format("SELECT l.userId FROM Likes l WHERE l.shortId = '%s'", shortId);
			Result<List<String>> likes = errorOrValue( okUser( shrt.getOwnerId(), password ), db.sql(query, String.class));
			cache.insertList("likes:" + shortId, likes.value());
			return likes;
		});
	}

	@Override
	public Result<List<String>> getFeed(String userId, String password) {
		Log.info(() -> format("getFeed : userId = %s, pwd = %s\n", userId, password));

		Result<List<String>> cachedList = cache.getList("feed:" + userId);
		if (cachedList.value() != null) {
			return ok(cachedList.value());
		}

		final var QUERY_FMT = """
				SELECT s.shortId, s.timestamp FROM Short s WHERE	s.ownerId = '%s'				
				UNION			
				SELECT s.shortId, s.timestamp FROM Short s, Following f 
					WHERE 
						f.followee = s.ownerId AND f.follower = '%s' 
				ORDER BY s.timestamp DESC""";

		Result<List<String>> feed = errorOrValue( okUser(userId, password), db.sql( format(QUERY_FMT, userId, userId), String.class));
		cache.insertList("feed:" + userId, feed.value());
		return feed;
	}

	protected Result<User> okUser( String userId, String pwd) {
		return JavaUsers.getInstance().getUser(userId, pwd);
	}

	private Result<Void> okUser( String userId ) {
		var res = okUser( userId, "");
		if( res.error() == FORBIDDEN )
			return error( res.error() );
		else
			return ok();
	}

	@Override
	public Result<Void> deleteAllShorts(String userId, String password, String token) {
		Log.info(() -> format("deleteAllShorts : userId = %s, password = %s, token = %s\n", userId, password, token));

		if( ! Token.isValid( token, userId ) )
			return error(FORBIDDEN);


		try {
			// Delete shorts owned by the user
			String deleteShortsQuery = format("SELECT * FROM Short s WHERE s.ownerId = '%s'", userId);
			List<Short> shortsToDelete = db.sql(deleteShortsQuery, Short.class).value();
			for (Short shortItem : shortsToDelete) {
				db.delete(shortItem);
				cache.delete(Short.class.getName() + "" + shortItem.getShortId());
			}
			cache.delete("shorts:" + userId);

			// Delete follows where user is either follower or followee
			String deleteFollowsQuery = format("SELECT * FROM Following f WHERE f.follower = '%s' OR f.followee = '%s'", userId, userId);
			List<Following> followsToDelete = db.sql(deleteFollowsQuery, Following.class).value();
			for (Following follow : followsToDelete) {
				db.delete(follow);
			}
			cache.delete("followers:" + userId);

			// Delete likes where user is either owner or user
			String deleteLikesQuery = format("SELECT * FROM Likes l WHERE l.ownerId = '%s' OR l.userId = '%s'", userId, userId);
			List<Likes> likesToDelete = db.sql(deleteLikesQuery, Likes.class).value();
			for (Likes like : likesToDelete) {
				db.delete(like);
				cache.delete("likes:" + like.getShortId());
			}

			return Result.ok();
		} catch (Exception e) {
			Log.severe(() -> format("Error deleting all shorts, follows, and likes for user %s: %s", userId, e.getMessage()));
			return error(Result.ErrorCode.INTERNAL_ERROR);
		}
	}

}