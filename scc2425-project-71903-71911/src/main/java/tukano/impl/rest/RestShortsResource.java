package main.java.tukano.impl.rest;

import java.util.List;

import jakarta.inject.Singleton;
import main.java.tukano.api.Short;
import main.java.tukano.api.Shorts;
import main.java.tukano.api.rest.RestShorts;
import main.java.tukano.impl.JavaShorts;

@Singleton
public class RestShortsResource extends RestResource implements RestShorts {

	static final Shorts impl = JavaShorts.getInstance();
		
	@Override
	public Short createShort(String userId, String password) {
		return super.resultOrThrow( impl.createShort(userId, password));
	}

	@Override
	public void deleteShort(String shortId, String password) {
		super.resultOrThrow( impl.deleteShort(shortId, password));
	}

	@Override
	public Short getShort(String shortId) {
		return super.resultOrThrow( impl.getShort(shortId));
	}
	@Override
	public List<String> getShorts(String userId) {
		return super.resultOrThrow( impl.getShorts(userId));
	}

	@Override
	public List<String> getFeed(String userId, String password) {
		return super.resultOrThrow( impl.getFeed(userId, password));
	}

	@Override
	public void deleteAllShorts(String userId, String password) {
		super.resultOrThrow( impl.deleteAllShorts(userId, password));
	}	
}
