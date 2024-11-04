package main.java.tukano.impl.rest;

import jakarta.inject.Singleton;
import main.java.tukano.api.LikesI;
import main.java.tukano.api.rest.RestLikes;
import main.java.tukano.impl.JavaLikes;

import java.util.List;

@Singleton
public class RestLikesResource extends RestResource implements RestLikes {

    static final LikesI impl = JavaLikes.getInstance();

    @Override
    public void like(String shortId, String userId, boolean isLiked, String password) {
        super.resultOrThrow( impl.like(shortId, userId, isLiked, password));
    }

    @Override
    public List<String> likes(String shortId, String password) {
        return super.resultOrThrow( impl.likes(shortId, password));
    }
}
