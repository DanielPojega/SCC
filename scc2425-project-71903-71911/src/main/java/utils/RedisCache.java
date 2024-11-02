package utils;


import redis.clients.jedis.JedisPoolConfig;
import tukano.api.Result;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.List;

public class RedisCache {
    private static final String RedisHostname = "";
    private static final String RedisKey = "";
    private static final int REDIS_PORT = 6380;
    private static final int REDIS_TIMEOUT = 1000;
    private static final boolean Redis_USE_TLS = true;

    private static JedisPool instance;

    public synchronized static JedisPool getCachePool() {
        if( instance != null)
            return instance;

        var poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128);
        poolConfig.setMaxIdle(128);
        poolConfig.setMinIdle(16);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setNumTestsPerEvictionRun(3);
        poolConfig.setBlockWhenExhausted(true);
        instance = new JedisPool(poolConfig, RedisHostname, REDIS_PORT, REDIS_TIMEOUT, RedisKey, Redis_USE_TLS);
        return instance;
    }

    public <T> Result<?> insert(String key, T obj){
        String value = JSON.encode(obj);
        try (Jedis jedis = getCachePool().getResource()) {
            jedis.set(key, value);
            return Result.ok(obj);
        } catch (Exception e) {
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    public <T> Result<T> get(String key, Class<T> clazz){
        try (Jedis jedis = getCachePool().getResource()) {
            String value = jedis.get(key);
            return Result.ok(JSON.decode(value, clazz));
        } catch (Exception e) {
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    public Result<?> delete(String key){
        try (Jedis jedis = getCachePool().getResource()) {
            jedis.del(key);
            return Result.ok("Deleted successfully");
        } catch (Exception e) {
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    public <T> void insertList(String key, List<T> objs) {
        try (Jedis jedis = getCachePool().getResource()) {
            for (T obj : objs) {
                String value = JSON.encode(obj);
                jedis.lpush(key, value);
            }
        }
    }

    public Result<List<String>> getList(String key) {
        try (Jedis jedis = getCachePool().getResource()) {
            List<String> values = jedis.lrange(key, 0, -1);
            return Result.ok(values);
        } catch (Exception e) {
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    public void expire(String key, int seconds) {
        try (Jedis jedis = getCachePool().getResource()) {
            jedis.expire(key, seconds);
        }
    }
}
