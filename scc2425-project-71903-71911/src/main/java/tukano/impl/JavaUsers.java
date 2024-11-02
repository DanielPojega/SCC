package tukano.impl;

import static java.lang.String.format;
import static tukano.api.Result.ErrorCode.BAD_REQUEST;
import static tukano.api.Result.ErrorCode.FORBIDDEN;
import static tukano.api.Result.error;
import static tukano.api.Result.errorOrResult;
import static tukano.api.Result.errorOrValue;
import static tukano.api.Result.ok;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import tukano.api.Result;
import tukano.api.User;
import tukano.api.Users;
import utils.RedisCache;
import utils.db.CosmosDB;
import utils.JSON;

public class JavaUsers implements Users {

	private static Logger Log = Logger.getLogger(JavaUsers.class.getName());

	private final CosmosDB db = CosmosDB.getInstance("users");

	private final RedisCache cache = new RedisCache();

	private static Users instance;

	synchronized public static Users getInstance() {
		if( instance == null )
			instance = new JavaUsers();
		return instance;
	}

	private JavaUsers() {}

	@Override
	public Result<String> createUser(User user) {
		Log.info(() -> format("createUser : %s\n", user));

		if( badUserInfo( user ) )
			return error(BAD_REQUEST);

		return errorOrValue( db.insert(user), user.getUserId() );
	}

	@Override
	public Result<User> getUser(String userId, String pwd) {
		Log.info( () -> format("getUser : userId = %s, pwd = %s\n", userId, pwd));


		if (userId == null) return error(BAD_REQUEST);

		String key = User.class.getName() + "" + userId;

		Result<User> cachedUser = cache.get(key, User.class);
		if (cachedUser.value() != null) {
			return validatedUserOrError(cachedUser, pwd);
		}

		Result<User> user = db.get(userId, User.class);
		cache.insert(key, user.value());

		return validatedUserOrError(user, pwd);
	}

	@Override
	public Result<User> updateUser(String userId, String pwd, User other) {
		Log.info(() -> format("updateUser : userId = %s, pwd = %s, user: %s\n", userId, pwd, other));

		if (badUpdateUserInfo(userId, pwd, other))
			return error(BAD_REQUEST);

		return errorOrResult(
				validatedUserOrError(db.get( userId, User.class), pwd),
				user -> {
					User updatedUser = user.updateFrom(other);
					Result<User> dbUpdateResult = db.update(updatedUser);
					if (dbUpdateResult.isOK()) {
						String key = User.class.getName() + "" + userId;
						cache.insert(key, updatedUser);
					}
					return dbUpdateResult;
				}
		);
	}

	@Override
	public Result<User> deleteUser(String userId, String pwd) {
		Log.info(() -> format("deleteUser : userId = %s, pwd = %s\n", userId, pwd));

		if (userId == null || pwd == null )
			return error(BAD_REQUEST);

		return errorOrResult( validatedUserOrError(db.get( userId, User.class), pwd), user -> {

			// Delete user shorts and related info asynchronously in a separate thread
			Executors.defaultThreadFactory().newThread( () -> {
				JavaShorts.getInstance().deleteAllShorts(userId, pwd, Token.get(userId));
				JavaBlobs.getInstance().deleteAllBlobs(userId, Token.get(userId));
			}).start();
			Result<User> deleteResult = (Result<User>) db.delete(user);
			if (deleteResult.isOK()) {
				String key = User.class.getName() + "" + userId;
				cache.delete(key);
			}

			return deleteResult;
		});
	}

	@Override
	public Result<List<User>> searchUsers(String pattern) {
		Log.info( () -> format("searchUsers : patterns = %s\n", pattern));

		Result<List<String>> cachedList = cache.getList(pattern);
		if (cachedList.value() != null) {
			return ok(cachedList.value().stream().map(userString ->
					JSON.decode(userString, User.class)
			).toList());
		}

		var query = format("SELECT * FROM User u WHERE UPPER(u.userId) LIKE '%%%s%%'", pattern.toUpperCase());
		var hits = db.sql(query, User.class)
				.value()
				.stream()
				.map(User::copyWithoutPassword)
				.toList();

		cache.insertList(pattern, hits);
		cache.expire(pattern, 200);

		return ok(hits);
	}


	private Result<User> validatedUserOrError( Result<User> res, String pwd ) {
		if( res.isOK())
			return res.value().getPwd().equals( pwd ) ? res : error(FORBIDDEN);
		else
			return res;
	}

	private boolean badUserInfo( User user) {
		return (user.userId() == null || user.pwd() == null || user.displayName() == null || user.email() == null);
	}

	private boolean badUpdateUserInfo( String userId, String pwd, User info) {
		return (userId == null || pwd == null || info.getUserId() != null && ! userId.equals( info.getUserId()));
	}
}
