package main.java.utils.db;

import main.java.tukano.api.Result;
import main.java.tukano.api.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class CosmosDBPostgreSQL {

    private static final String CONNECTION_URL = "";

    private Connection connection;
    private static CosmosDBPostgreSQL instance;

    public static synchronized CosmosDBPostgreSQL getInstance() {
        if (instance != null) return instance;

        try {
            Connection connection = DriverManager.getConnection(CONNECTION_URL);
            instance = new CosmosDBPostgreSQL(connection);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to establish database connection");
        }
        return instance;
    }

    private CosmosDBPostgreSQL(Connection connection) {
        this.connection = connection;
    }

    public <T> Result<T> get(String id, Class<T> clazz) {
        return tryCatch(() -> {
            String query = "SELECT * FROM " + clazz.getSimpleName() + " WHERE id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, id);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return clazz.getConstructor(ResultSet.class).newInstance(rs);
                }
                return null;
            } catch (SQLException | ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public <T> Result<List<T>> sql(String query, Class<T> clazz) {
        return tryCatch(() -> {
            List<T> resultList = new ArrayList<>();
            try (PreparedStatement stmt = connection.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {
                 while (rs.next()) {
                    resultList.add(clazz.getConstructor(ResultSet.class).newInstance(rs));
                 }
            } catch (SQLException | ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
            return resultList;
        });
    }

    public <T> Result<T> insert(T obj) {
        return tryCatch(() -> {
            String query = "INSERT INTO " + obj.getClass().getSimpleName() + " VALUES " + obj;
            try (PreparedStatement stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                stmt.executeUpdate();
                return obj;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public Result<User> update(User obj, String id) {
        return tryCatch(() -> {
            String query = "UPDATE " + obj.getClass().getSimpleName() + " SET email=" + obj.getEmail() +
                    ", displayName=" + obj.getDisplayName() + ", pwd=" + obj.getPwd() + " WHERE id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, id);
                stmt.executeUpdate();
                return obj;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public <T> Result<?> delete(String id, Class<T> clazz) {
        return tryCatch(() -> {
            String query = "DELETE FROM " + clazz.getSimpleName() + " WHERE id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, id);
                stmt.executeUpdate();
                return null;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private <T> Result<T> tryCatch(Supplier<T> supplierFunc) {
        try {
            return Result.ok(supplierFunc.get());
        } catch (Exception x) {
            x.printStackTrace();
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }
}