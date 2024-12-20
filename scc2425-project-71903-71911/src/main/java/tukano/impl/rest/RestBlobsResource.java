package main.java.tukano.impl.rest;

import jakarta.inject.Singleton;
import main.java.tukano.api.Blobs;
import main.java.tukano.api.rest.RestBlobs;
import main.java.tukano.impl.JavaBlobs;

@Singleton
public class RestBlobsResource extends main.java.tukano.impl.rest.RestResource implements RestBlobs {

	final Blobs impl;
	
	public RestBlobsResource() {
		this.impl = JavaBlobs.getInstance();
	}
	
	@Override
	public void upload(String blobId, byte[] bytes, String token, String userId) {
		super.resultOrThrow( impl.upload(blobId, bytes, token, userId));
	}

	@Override
	public byte[] download(String blobId, String token, String userId) {
		return super.resultOrThrow( impl.download( blobId, token, userId ));
	}

	@Override
	public void delete(String blobId, String token) {
		super.resultOrThrow( impl.delete( blobId, token ));
	}
	
	@Override
	public void deleteAllBlobs(String userId, String password) {
		super.resultOrThrow( impl.deleteAllBlobs( userId, password ));
	}
}
