package main.java.tukano.api;

import java.util.List;

public interface FollowingI {

    /**
     * Causes a user to follow the shorts of another user.
     *
     * @param userId1     the user that will follow or cease to follow the
     *                    followed user
     * @param userId2     the followed user
     * @param isFollowing flag that indicates the desired end status of the
     *                    operation
     * @param password 	  the password of the follower
     * @return (OK,),
     * 	NOT_FOUND if any of the users does not exist
     *  FORBIDDEN if the password is incorrect
     */
    Result<Void> follow(String userId1, String userId2, boolean isFollowing, String password);

    /**
     * Retrieves the list of users following a given user
     * @param userId - the followed user
     * @param password - the password of the followed user
     * @return (OK, List<String>|empty list) the list of users that follow another user, or empty if the user has no followers
     * NOT_FOUND if the user does not exists
     * FORBIDDEN if the password is incorrect
     */
    Result<List<String>> followers(String userId, String password);
}