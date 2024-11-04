package main.java.tukano.api;

import java.util.List;

public interface LikesI {
    /**
     * Adds or removes a like to a short
     *
     * @param shortId  - the identifier of the post
     * @param userId  - the identifier of the user
     * @param isLiked - a flag with true to add a like, false to remove the like
     * @return (OK,void) if the like was added/removed;
     * 	NOT_FOUND if either the short or the like being removed does not exist,
     *  CONFLICT if the like already exists.
     *  FORBIDDEN if the password of the user is incorrect
     *  BAD_REQUEST, otherwise
     */
    Result<Void> like(String shortId, String userId, boolean isLiked, String password);


    /**
     * Returns all the likes of a given short
     *
     * @param shortId the identifier of the short
     * @param password the password of the owner of the short
     * @return (OK,Boolean),
     * NOT_FOUND if there is no Short with the given shortId
     * FORBIDDEN if the password is incorrect
     */
    Result<List<String>> likes(String shortId, String password);

}
