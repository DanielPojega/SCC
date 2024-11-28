package main.java.tukano.impl;

import static java.lang.String.format;
import static main.java.tukano.api.Result.error;
import static main.java.tukano.api.Result.ErrorCode.FORBIDDEN;

import java.util.function.Consumer;
import java.util.logging.Logger;

import main.java.tukano.api.Blobs;
import main.java.tukano.api.Result;
import main.java.tukano.impl.rest.TukanoRestServer;
import main.java.tukano.impl.storage.AzureBlobStorage;
import main.java.tukano.impl.storage.BlobStorage;
import main.java.utils.Hash;
import main.java.utils.Hex;
import main.java.utils.auth.SessionValidation;

public class JavaBlobs implements Blobs {
	
	private static Blobs instance;
	private static Logger Log = Logger.getLogger(JavaBlobs.class.getName());

	public String baseURI;
	private BlobStorage storage;

	private static final String ADMIN = "admin";
	
	synchronized public static Blobs getInstance() {
		if( instance == null )
			instance = new JavaBlobs();
		return instance;
	}
	
	private JavaBlobs() {
		storage = new AzureBlobStorage();
		baseURI = String.format("%s/%s/", TukanoRestServer.serverURI, Blobs.NAME);
	}
	
	@Override
	public Result<Void> upload(String blobId, byte[] bytes, String token, String userId) {
		Log.info(() -> format("upload : blobId = %s, sha256 = %s, token = %s\n", blobId, Hex.of(Hash.sha256(bytes)), token));

		SessionValidation.validateSession(userId);

		if (!validBlobId(blobId, token))
			return error(FORBIDDEN);

		return storage.write(bytes);
	}

	@Override
	public Result<byte[]> download(String blobId, String token, String userId) {
		Log.info(() -> format("download : blobId = %s, token=%s\n", blobId, token));

		SessionValidation.validateSession(userId);

		if( ! validBlobId( blobId, token ) )
			return error(FORBIDDEN);

		return storage.read( toPath( blobId ) );
	}

	@Override
	public Result<Void> downloadToSink(String blobId, Consumer<byte[]> sink, String token, String userId) {
		Log.info(() -> format("downloadToSink : blobId = %s, token = %s\n", blobId, token));

		SessionValidation.validateSession(userId);

		if( ! validBlobId( blobId, token ) )
			return error(FORBIDDEN);

		return storage.read( toPath(blobId), sink);
	}

	@Override
	public Result<Void> delete(String blobId, String token) {
		Log.info(() -> format("delete : blobId = %s, token=%s\n", blobId, token));

		SessionValidation.validateSession(ADMIN);
	
		if( ! validBlobId( blobId, token ) )
			return error(FORBIDDEN);

		return storage.delete( toPath(blobId));
	}
	
	@Override
	public Result<Void> deleteAllBlobs(String userId, String token) {
		Log.info(() -> format("deleteAllBlobs : userId = %s, token=%s\n", userId, token));

		SessionValidation.validateSession(ADMIN);

		if( ! main.java.tukano.impl.Token.isValid( token, userId ) )
			return error(FORBIDDEN);
		
		return storage.delete( toPath(userId));
	}
	
	private boolean validBlobId(String blobId, String token) {
		return main.java.tukano.impl.Token.isValid(token, toURL(blobId));
	}

	private String toPath(String blobId) {
		return blobId.replace("+", "/");
	}
	
	private String toURL( String blobId ) {
		return baseURI + blobId ;
	}
}
