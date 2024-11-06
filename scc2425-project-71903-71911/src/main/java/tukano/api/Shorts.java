package main.java.tukano.api;

import java.util.List;

/**
 * 
 * Interface for the Shorts service.
 * 
 * This service allows existing users to create or delete short videos. 
 * Users can follow other users to gain access to their short videos.
 * User can add or remove likes to short videos.
 * 
 * @author smd
 *
 */
public interface Shorts {
	
	String NAME = "shorts";
	
	/**
	 * Creates a new short, generating its unique identifier. 
	 * The result short will include the blob storage location where the media should be uploaded.
	 * 
	 * @param userId - the owner of the new short
	 * @param password - the password of owner of the new short
	 * @return (OK, Short) if the short was created;
	 * NOT FOUND, if the owner of the short does not exist;
	 * FORBIDDEN, if the password is not correct;
	 * BAD_REQUEST, otherwise.
	 */
	Result<Short> createShort(String userId, String password);

	/**
	 * Deletes a given Short.
	 * 
	 * @param shortId the unique identifier of the short to be deleted
	 * @return (OK,void), 
	 * 	NOT_FOUND if shortId does not match an existing short
	 * 	FORBIDDEN, if the password is not correct;
	 */
	Result<Void> deleteShort(String shortId, String password);
	
	
	/**
	 * Retrieves a given Short.
	 * 
	 * @param shortId the unique identifier of the short to be deleted
	 * @return (OK,Short), 
	 * 	NOT_FOUND if shortId does not match an existing short
	 */
	Result<Short> getShort(String shortId);
	
	
	/**
	 * Retrieves the list of identifiers of the shorts created by the given user, with its total likes count updated.
	 * 
	 * @param userId the user that owns the requested shorts
	 * @return (OK, List<String>|empty list) or NOT_FOUND if the user does not exist
	 */
	Result<List<String>> getShorts( String userId );

	/**
	 * Returns the feed of the user, sorted by age. The feed is the list of shorts made by
	 * the users followed by the user.
	 * 
	 * @param userId user of the requested feed
	 * @param password the password of the user
	 * @return (OK,List<PostId>|empty list)
	 * 	NOT_FOUND if the user does not exists
	 *  FORBIDDEN if the password is incorrect
	 */
	Result<List<String>> getFeed(String userId, String password);

	Result<Void> deleteAllShorts(String userId, String password);
}
