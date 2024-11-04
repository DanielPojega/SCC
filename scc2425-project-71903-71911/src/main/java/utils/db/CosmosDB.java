package utils.db;

import com.azure.cosmos.*;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import tukano.api.Result;
import java.util.List;
import java.util.function.Supplier;

public class CosmosDB {

    private static final String CONNECTION_URL = "";
    private static final String DB_KEY = "";
    private static final String DB_NAME = "scc2425";

    private CosmosClient client;

    private CosmosDatabase db;

    private CosmosContainer container;

    public static synchronized CosmosDB getInstance(String containerName) {
        CosmosClient client = new CosmosClientBuilder()
                .endpoint(CONNECTION_URL)
                .key(DB_KEY)
                .directMode()
                .consistencyLevel(ConsistencyLevel.SESSION)
                .connectionSharingAcrossClientsEnabled(true)
                .contentResponseOnWriteEnabled(true)
                .buildClient();
        return new CosmosDB(client, containerName);
    }

    public CosmosDB(CosmosClient client, String containerName) {
        this.client = client;
        init(containerName);
    }

    private synchronized void init(String containerName) {
        if( db != null)
            return;
        db = client.getDatabase(DB_NAME);
        container = db.getContainer(containerName);
    }

    public <T> Result<T> get(String id, Class<T> clazz) {
        return tryCatch( () -> container.readItem(id, new PartitionKey(id), clazz).getItem());
    }

    public <T> Result<List<T>> sql(String query, Class<T> clazz) {
        return tryCatch(() -> {
            var res = container.queryItems(query, new CosmosQueryRequestOptions(), clazz);
            return res.stream().toList();
        });
    }

    public <T> Result<T> insert(T obj){
        return tryCatch( () -> container.createItem(obj).getItem());
    }

    public <T> Result<T> update(T obj){
        return tryCatch( () -> container.upsertItem(obj).getItem());
    }

    public <T> Result<?> delete(T obj){
        return tryCatch( () -> container.deleteItem(obj, new CosmosItemRequestOptions()).getItem());
    }

    static Result.ErrorCode errorCodeFromStatus(int status ) {
        return switch( status ) {
            case 200 -> Result.ErrorCode.OK;
            case 404 -> Result.ErrorCode.NOT_FOUND;
            case 409 -> Result.ErrorCode.CONFLICT;
            default -> Result.ErrorCode.INTERNAL_ERROR;
        };
    }

    <T> Result<T> tryCatch(Supplier<T> supplierFunc) {
        try {
            return Result.ok(supplierFunc.get());
        } catch (CosmosException ce) {
            System.err.println("CosmosException: Status code = " + ce.getStatusCode() + ", Message = " + ce.getMessage());
            return Result.error(errorCodeFromStatus(ce.getStatusCode()));
        } catch (Exception x) {
            x.printStackTrace();
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }
}
