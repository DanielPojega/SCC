package main.java.utils.db;

import com.google.gson.Gson;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import main.java.tukano.api.Result;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MongoDB {

    Gson gson = new Gson();
    private static final String CONNECTION_URL = "mongodb://mongodb:27017";
    private static final String DB_NAME = "scc2425";
    private final MongoCollection<Document> collection;

    private final MongoDatabase db;


    public MongoDB(String collectionName) {
        MongoClient client = MongoClients.create(CONNECTION_URL);
        db = client.getDatabase(DB_NAME);
        this.collection = db.getCollection(collectionName);
    }

    public <T> Result<T> get(String id, Class<T> clazz) {
        return tryCatch(() -> {
            Document document = collection.find(new Document("id", id)).first();
            if (document == null) {
                return null;
            }
            return gson.fromJson(document.toJson(), clazz);
        });
    }

    public <T> Result<List<T>> sql(/*String*/ Bson query, Class<T> clazz) {
        return tryCatch(() -> {
            List<Document> documents = collection.find(query).into(new ArrayList<>());
            return documents.stream()
                    .map(doc -> gson.fromJson(doc.toJson(), clazz))
                    .collect(Collectors.toList());
        });
    }

    public <T> Result<T> insert(T obj) {
        return tryCatch(() -> {
            Document document = gson.fromJson(gson.toJson(obj), Document.class);
            collection.insertOne(document);
            return obj;
        });
    }

    public <T> Result<T> update(String id, T obj) {
        return tryCatch(() -> {
            Document document = gson.fromJson(gson.toJson(obj), Document.class);
            collection.replaceOne(new Document("id", id), document);
            return obj;
        });
    }

    public <T> Result<?> delete(String id) {
        return tryCatch(() -> {
            collection.deleteOne(new Document("id", id));
            return null;
        });
    }

    <T> Result<T> tryCatch(Supplier<T> supplierFunc) {
        try {
            return Result.ok(supplierFunc.get());
        }
        catch (Exception e) {
            e.printStackTrace();
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }
}
