package me.mrkirby153.uhc.bot.network.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class Utility {

    private static final Gson gson = new GsonBuilder().create();
    public static String serialize(Object object){
        return gson.toJson(object);
    }

    public static <T> T deserialize(String json, Class<T> type){
        return gson.fromJson(json, type);
    }

    public static JedisPool createPool(RedisConnection connection) {
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(Utility.class.getClassLoader());
        JedisPoolConfig cfg = new JedisPoolConfig();
        cfg.setMaxWaitMillis(1000);
        cfg.setMinIdle(5);
        cfg.setTestOnBorrow(true);
        cfg.setMaxTotal(20);
        cfg.setBlockWhenExhausted(true);
        JedisPool pool = new JedisPool(cfg, connection.getHost(), connection.getPort(), 1000, connection.getPassword());
        Thread.currentThread().setContextClassLoader(previous);
        return pool;
    }

    public static long currentTime(JedisPool pool) {

       return System.currentTimeMillis();
    }
}
