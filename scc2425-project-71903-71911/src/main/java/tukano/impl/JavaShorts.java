package main.java.tukano.impl;

import static java.lang.String.format;
import static main.java.tukano.api.Result.ErrorCode.*;
import static main.java.tukano.api.Result.error;
import static main.java.tukano.api.Result.errorOrResult;
import static main.java.tukano.api.Result.errorOrValue;
import static main.java.tukano.api.Result.ok;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import com.mongodb.client.model.Filters;
import main.java.tukano.api.Blobs;
import main.java.tukano.api.Result;
import main.java.tukano.api.Short;
import main.java.tukano.api.Shorts;
import main.java.tukano.api.User;
import main.java.tukano.impl.data.Following;
import main.java.tukano.impl.data.Likes;
import main.java.tukano.impl.data.ShortIdResult;
import main.java.tukano.impl.rest.TukanoRestServer;
import main.java.utils.RedisCache;
import main.java.utils.db.CosmosDB;
import main.java.utils.db.MongoDB;
import org.bson.conversions.Bson;

public class JavaShorts implements Shorts {

	private static Logger Log = Logger.getLogger(JavaShorts.class.getName());

	//private final CosmosDB db = CosmosDB.getInstance("shorts");
	private final MongoDB db = new MongoDB("shorts");

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

		Result<Short> cachedShort = cache.get(key, Short.class);

		if (cachedShort.value() != null) {
			return cachedShort;
		}

		//var query = format("SELECT * FROM Likes l WHERE l.shortId = '%s'", shortId);
		Bson query = Filters.eq("shortId", shortId);

		var likes = db.sql(query, Likes.class);

		Result<Short> shrt = errorOrValue( db.get(shortId, Short.class), shr ->
				shr == null ? null : shr.copyWithLikes_And_Token( likes.value().size()));
		cache.insert(key, shrt.value());

		return shrt;
	}

	
	@Override
	public Result<Void> deleteShort(String shortId, String password) {
		Log.info(() -> format("deleteShort : shortId = %s, pwd = %s\n", shortId, password));
		
		return errorOrResult( getShort(shortId), shrt ->
			errorOrResult( okUser( shrt.getOwnerId(), password), user -> {

				Result<Short> deleteShortResult = (Result<Short>) db.delete(shrt.getId());

				if (!deleteShortResult.isOK()) return Result.error(deleteShortResult.error());

				cache.delete(Short.class.getName() + "" + shortId);

				//String deleteLikesQuery = format("SELECT * FROM Likes l WHERE l.shortId = '%s'", shortId);
				Bson deleteLikesQuery = Filters.eq("shortId", shortId);

				List<Likes> likesToDelete = db.sql(deleteLikesQuery, Likes.class).value();

				for (Likes like : likesToDelete) {
					db.delete(like.getId());
				}

				cache.delete("likes:" + shortId);

				return JavaBlobs.getInstance().delete(shrt.getId(), Token.get(shrt.getBlobUrl()) );
			}
		));
	}

	@Override
	public Result<List<String>> getShorts(String userId) {
		Log.info(() -> format("getShorts : userId = %s\n", userId));

		Result<List<String>> cachedList = cache.getList("shorts:" + userId);
		cache.expire("shorts:" + userId, 60);

		if (!cachedList.value().isEmpty()) {
			return ok(cachedList.value());
		}

		//var query = format("SELECT s.id FROM Short s WHERE s.ownerId = '%s'", userId);
		Bson query = Filters.eq("ownerId", userId);

		Result<List<ShortIdResult>> results = db.sql(query, ShortIdResult.class);
		if (results.error() != OK) {
			return error(results.error());
		}
		List<String> shrtList = results.value().stream().map(ShortIdResult::getId).toList();

		cache.insertList("shorts:" + userId, shrtList);

		return ok(shrtList);
	}

	@Override
	public Result<List<String>> getFeed(String userId, String password) {
		Log.info(() -> format("getFeed : userId = %s, pwd = %s\n", userId, password));
		CosmosDB followingDb = CosmosDB.getInstance("following");
		List<String> shortsIds = new ArrayList<>();

		Result<List<String>> cachedList = cache.getList("feed:" + userId);
		cache.expire("feed:" + userId, 20);
		if (!cachedList.value().isEmpty()) {
			return ok(cachedList.value());
		}

		//String queryShorts = String.format("SELECT s.id, s.timestamp FROM Short s WHERE s.ownerId = '%s'", userId);
		Bson queryShorts = Filters.eq("ownerId", userId);

		Result<List<Short>> ownerShorts = db.sql(queryShorts, Short.class);

		String queryFollowing = String.format("SELECT f.followee FROM Following f WHERE f.follower = '%s'", userId);
		Result<List<Following>> results = followingDb.sql(queryFollowing, Following.class);

		List<String> followees = results.value().stream().map(Following::getFollowee).toList();

		if (!followees.isEmpty()) {
			String followeeIds = String.join(",", followees);
			/*String queryFolloweeShorts = String.format(
					"SELECT s.id, s.timestamp FROM Short s WHERE s.ownerId IN ('%s')", followeeIds
			);*/
			Bson queryFolloweeShorts = Filters.in("ownerId", followeeIds);

			Result<List<Short>> followeeShorts = db.sql(queryFolloweeShorts, Short.class);

			if (ownerShorts.isOK() && followeeShorts.isOK()) {
				shortsIds.addAll(ownerShorts.value().stream()
						.map(Short::getId)
						.toList());

				shortsIds.addAll(followeeShorts.value().stream()
						.map(Short::getId)
						.toList());
			}
		}

		Result<List<String>> feed = errorOrValue( okUser(userId, password), shortsIds);

		cache.insertList("feed:" + userId, feed.value());
		return feed;
	}
		
	protected Result<User> okUser( String userId, String pwd) {
		return JavaUsers.getInstance().getUser(userId, pwd);
	}
	
	@Override
	public Result<Void> deleteAllShorts(String userId, String password) {
		Log.info(() -> format("deleteAllShorts : userId = %s, password = %s\n", userId, password));
						
		try {
			// Delete shorts owned by the user
			//String deleteShortsQuery = format("SELECT * FROM Short s WHERE s.ownerId = '%s'", userId);
			Bson deleteShortsQuery = Filters.eq("ownerId", userId);

			List<Short> shortsToDelete = db.sql(deleteShortsQuery, Short.class).value();
			for (Short shortItem : shortsToDelete) {
				db.delete(shortItem.getId());
				cache.delete(Short.class.getName() + "" + shortItem.getId());
			}
			cache.delete("shorts:" + userId);

			// Delete follows where user is either follower or followee
			//String deleteFollowsQuery = format("SELECT * FROM Following f WHERE f.follower = '%s' OR f.followee = '%s'", userId, userId);
			Bson deleteFollowsQuery = Filters.or(Filters.eq("follower", userId), Filters.eq("followee", userId));

			List<Following> followsToDelete = db.sql(deleteFollowsQuery, Following.class).value();
			for (Following follow : followsToDelete) {
				db.delete(follow.getId());
			}
			cache.delete("followers:" + userId);

			// Delete likes where user is either owner or user
			//String deleteLikesQuery = format("SELECT * FROM Likes l WHERE l.ownerId = '%s' OR l.userId = '%s'", userId, userId);
			Bson deleteLikesQuery = Filters.or(Filters.eq("ownerId", userId), Filters.eq("userId", userId));

			List<Likes> likesToDelete = db.sql(deleteLikesQuery, Likes.class).value();
			for (Likes like : likesToDelete) {
				db.delete(like.getId());
				cache.delete("likes:" + like.getShortId());
			}

			return Result.ok();
		} catch (Exception e) {
			return error(Result.ErrorCode.INTERNAL_ERROR);
		}
	}
	
}