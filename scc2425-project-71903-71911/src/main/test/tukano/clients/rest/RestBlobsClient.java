package main.test.tukano.clients.rest;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import main.java.tukano.api.Blobs;
import main.java.tukano.api.Result;
import main.java.tukano.api.rest.RestBlobs;

public class RestBlobsClient extends RestClient implements Blobs {

	public RestBlobsClient(String serverURI) {
		super(serverURI, RestBlobs.PATH);
	}

	private Result<Void> _upload(String blobURL, byte[] bytes, String token, String userId) {
		return super.toJavaResult(
				client.target( blobURL )
				.queryParam(RestBlobs.TOKEN, token)
						.queryParam(RestBlobs.USER_ID, userId)
				.request()
				.post( Entity.entity(bytes, MediaType.APPLICATION_OCTET_STREAM_TYPE)));
	}

	private Result<byte[]> _download(String blobURL, String token, String userId) {
		return super.toJavaResult(
				client.target( blobURL )
				.queryParam(RestBlobs.TOKEN, token)
						.queryParam(RestBlobs.USER_ID, userId)
				.request()
				.accept(MediaType.APPLICATION_OCTET_STREAM_TYPE)
				.get(), byte[].class);
	}

	private Result<Void> _delete(String blobURL, String token) {
		return super.toJavaResult(
				client.target( blobURL )
				.queryParam( RestBlobs.TOKEN, token )
				.request()
				.delete());
	}
	
	private Result<Void> _deleteAllBlobs(String userId, String token) {
		return super.toJavaResult(
				target.path(userId)
				.path(RestBlobs.BLOBS)
				.queryParam( RestBlobs.TOKEN, token )
				.request()
				.delete());
	}
	
	@Override
	public Result<Void> upload(String blobId, byte[] bytes, String token, String userId) {
		return super.reTry( () -> _upload(blobId, bytes, token, userId));
	}

	@Override
	public Result<byte[]> download(String blobId, String token, String userId) {
		return super.reTry( () -> _download(blobId, token, userId));
	}

	@Override
	public Result<Void> delete(String blobId, String token) {
		return super.reTry( () -> _delete(blobId, token));
	}
	
	@Override
	public Result<Void> deleteAllBlobs(String userId, String password) {
		return super.reTry( () -> _deleteAllBlobs(userId, password));
	}
}
