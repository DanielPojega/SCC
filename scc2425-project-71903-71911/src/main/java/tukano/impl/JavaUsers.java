package main.java.tukano.impl;

import static java.lang.String.format;
import static main.java.tukano.api.Result.ErrorCode.BAD_REQUEST;
import static main.java.tukano.api.Result.ErrorCode.FORBIDDEN;
import static main.java.tukano.api.Result.error;
import static main.java.tukano.api.Result.errorOrResult;
import static main.java.tukano.api.Result.errorOrValue;
import static main.java.tukano.api.Result.ok;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import com.mongodb.client.model.Filters;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import main.java.tukano.api.Result;
import main.java.tukano.api.User;
import main.java.tukano.api.Users;
import main.java.utils.JSON;
import main.java.utils.RedisCache;
import main.java.utils.db.CosmosDB;
import main.java.utils.db.MongoDB;
import org.bson.conversions.Bson;

public class JavaUsers implements Users {
	
	private static Logger Log = Logger.getLogger(JavaUsers.class.getName());

	//private final CosmosDB db = CosmosDB.getInstance("users");
	private final MongoDB db = new MongoDB("users");

	int MAX_COOKIE_AGE = 3600;
	static final String COOKIE_KEY = "scc:session";


	private final RedisCache cache = new RedisCache();

	private static Users instance;
	
	synchronized public static Users getInstance() {
		if( instance == null )
			instance = new JavaUsers();
		return instance;
	}
	
	private JavaUsers() {}
	
	@Override
	public Result<Response> createUser(User user) {
		Log.info(() -> format("createUser : %s\n", user));

		if( badUserInfo( user ) )
				return error(BAD_REQUEST);

		var dbResult = db.insert(user);
		if (!dbResult.isOK()) {
			return Result.error(dbResult.error());
		}

		var cookie = new NewCookie.Builder(COOKIE_KEY)
				.value(user.getId()).path("/")
				.comment("sessionid")
				.maxAge(MAX_COOKIE_AGE)
				.secure(false) //ideally it should be true to only work for https requests
				.httpOnly(true)
				.build();

		cache.insert( user.getId(), user );

		return ok(Response.ok("User created successfully")
				.cookie(cookie)
				.build());
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
					Result<User> dbUpdateResult = db.update(updatedUser.getId(), updatedUser);
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

		System.out.println("HERE");
		System.out.println(db.get( userId, User.class).value());

		return errorOrResult( validatedUserOrError(db.get( userId, User.class), pwd), user -> {

			// Delete user shorts and related info asynchronously in a separate thread
			Executors.defaultThreadFactory().newThread( () -> {
				JavaShorts.getInstance().deleteAllShorts(userId, pwd);
				JavaBlobs.getInstance().deleteAllBlobs(userId, Token.get(userId));
			}).start();
			Result<User> deleteResult = (Result<User>) db.delete(user.getId());
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
		cache.expire(pattern, 60);

		if (!cachedList.value().isEmpty()) {
			return ok(cachedList.value().stream().map(userString ->
					JSON.decode(userString, User.class)
			).toList());
		}

		//var query = format("SELECT * FROM User u WHERE UPPER(u.id) LIKE '%%%s%%'", pattern.toUpperCase());
		Bson query = Filters.regex("id", ".*" + pattern.toUpperCase() + ".*", "i");
		var hits = db.sql(query, User.class)
				.value()
				.stream()
				.map(User::copyWithoutPassword)
				.toList();
		System.out.println(hits);

		cache.insertList(pattern, hits);

		return ok(hits);
	}

	
	private Result<User> validatedUserOrError( Result<User> res, String pwd ) {
		if( res.isOK() && res.value() != null )
			return res.value().getPwd().equals( pwd ) ? res : error(FORBIDDEN);
		else
			return res;
	}
	
	private boolean badUserInfo( User user) {
		return (user.userId() == null || user.pwd() == null || user.displayName() == null || user.email() == null);
	}
	
	private boolean badUpdateUserInfo( String userId, String pwd, User info) {
		return (userId == null || pwd == null || info.getId() != null && ! userId.equals( info.getId()));
	}
}
