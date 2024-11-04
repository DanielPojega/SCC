package tukano.api.rest;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;


@Path(RestLikes.PATH)
public interface RestLikes {
    String PATH = "/likes";

    String USER_ID = "userId";
    String SHORT_ID = "shortId";
    String LIKES = "/likes";
    String PWD = "pwd";

    @POST
    @Path("/{" + SHORT_ID + "}/{" + USER_ID + "}" + LIKES )
    @Consumes(MediaType.APPLICATION_JSON)
    void like(@PathParam(SHORT_ID) String shortId, @PathParam(USER_ID) String userId, boolean isLiked, @QueryParam(PWD) String password);

    @GET
    @Path("/{" + SHORT_ID + "}" + LIKES )
    @Produces(MediaType.APPLICATION_JSON)
    List<String> likes(@PathParam(SHORT_ID) String shortId, @QueryParam(PWD) String password);
}