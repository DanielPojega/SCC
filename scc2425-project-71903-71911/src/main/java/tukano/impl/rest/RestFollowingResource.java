package tukano.impl.rest;

import jakarta.inject.Singleton;
import tukano.api.FollowingI;
import tukano.api.rest.RestFollowing;
import tukano.impl.JavaFollowing;

import java.util.List;

@Singleton
public class RestFollowingResource extends RestResource implements RestFollowing {

    static final FollowingI impl = JavaFollowing.getInstance();

    @Override
    public void follow(String userId1, String userId2, boolean isFollowing, String password) {
        super.resultOrThrow( impl.follow(userId1, userId2, isFollowing, password));
    }

    @Override
    public List<String> followers(String userId, String password) {
        return super.resultOrThrow( impl.followers(userId, password));
    }

}
