package main.java.tukano.impl.storage;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import main.java.tukano.api.Result;
import main.java.utils.Hash;

import java.util.function.Consumer;

import static main.java.tukano.api.Result.ErrorCode.CONFLICT;
import static main.java.tukano.api.Result.ErrorCode.NOT_FOUND;
import static main.java.tukano.api.Result.error;
import static main.java.tukano.api.Result.ok;

public class AzureBlobStorage implements BlobStorage {

    private static final String BLOBS_CONTAINER_NAME = "images";
    private static final String storageConnectionString = "";
    BlobContainerClient containerClient = new BlobContainerClientBuilder()
            .connectionString(storageConnectionString)
            .containerName(BLOBS_CONTAINER_NAME)
            .buildClient();

    @Override
    public Result<Void> write(byte[] bytes) {
        String key = Hash.of(bytes);

        BinaryData data = BinaryData.fromBytes(bytes);
        BlobClient blob = containerClient.getBlobClient(key);

        if (blob.exists()) {
            System.out.println("Blob with the same name already exists.");
            return error(CONFLICT);
        }

        blob.upload(data);
        return ok();
    }

    @Override
    public Result<Void> delete(String path) {
        return null;
    }

    @Override
    public Result<byte[]> read(String path) {
        BlobClient blobClient = containerClient.getBlobClient(path);

        if (!blobClient.exists()) {
            System.out.println("Blob does not exist.");
            return error(NOT_FOUND);
        }

        BinaryData data = blobClient.downloadContent();
        byte[] content = data.toBytes();

        return ok(content);
    }

    @Override
    public Result<Void> read(String path, Consumer<byte[]> sink) {
        return null;
    }
}