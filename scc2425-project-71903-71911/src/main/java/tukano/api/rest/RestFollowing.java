package tukano.api.rest;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path(RestFollowing.PATH)
public interface RestFollowing {
    String PATH = "/following";
    String FOLLOWERS = "/followers";
    String USER_ID1 = "userId1";
    String USER_ID2 = "userId2";
    String USER_ID = "userId";
    String PWD = "pwd";

    @POST
    @Path("/{" + USER_ID1 + "}/{" + USER_ID2 + "}" + FOLLOWERS )
    @Consumes(MediaType.APPLICATION_JSON)
    void follow(@PathParam(USER_ID1) String userId1, @PathParam(USER_ID2) String userId2, boolean isFollowing, @QueryParam(PWD) String password);

    @GET
    @Path("/{" + USER_ID + "}" + FOLLOWERS )
    @Produces(MediaType.APPLICATION_JSON)
    List<String> followers(@PathParam(USER_ID) String userId, @QueryParam(PWD) String password);
}