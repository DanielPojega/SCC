package main.java.tukano.api.rest;

import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import main.java.tukano.api.Short;

@Path(RestShorts.PATH)
public interface RestShorts {
	String PATH = "/shorts";
	
	String USER_ID = "userId";
	String SHORT_ID = "shortId";
	
	String PWD = "pwd";
	String FEED = "/feed";
	String TOKEN = "token";
	String SHORTS = "/shorts";
	
	@POST
	@Path("/{" + USER_ID + "}")
	@Produces(MediaType.APPLICATION_JSON)
	Short createShort(@PathParam(USER_ID) String userId, @QueryParam(PWD) String password);

	@DELETE
	@Path("/{" + SHORT_ID + "}")
	void deleteShort(@PathParam(SHORT_ID) String shortId, @QueryParam(PWD) String password);

	@GET
	@Path("/{" + SHORT_ID + "}" )
	@Produces(MediaType.APPLICATION_JSON)
	Short getShort(@PathParam(SHORT_ID) String shortId);

	@GET
	@Path("/{" + USER_ID + "}" + SHORTS )
	@Produces(MediaType.APPLICATION_JSON)
	List<String> getShorts(@PathParam(USER_ID) String userId);

	@GET
	@Path("/{" + USER_ID + "}" + FEED )
	@Produces(MediaType.APPLICATION_JSON)
	List<String> getFeed( @PathParam(USER_ID) String userId, @QueryParam(PWD) String password);
	
	@DELETE
	@Path("/{" + USER_ID + "}" + SHORTS)
	void deleteAllShorts(@PathParam(USER_ID) String userId, @QueryParam(PWD) String password);

}
