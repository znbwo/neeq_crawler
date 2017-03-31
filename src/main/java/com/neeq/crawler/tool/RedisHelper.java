package com.neeq.crawler.tool;

import com.neeq.crawler.dependence.Config;
import com.neeq.crawler.dependence.CoopRedis;
import redis.clients.jedis.Jedis;

/**
 * Created by bj on 16/6/28.
 */
public class RedisHelper {
    public static CoopRedis redis;
    static {
        String host = Config.get("redis.host", "127.0.0.1");
        int port = Config.getInt("redis.port", 6379);
        String redisPasswd = Config.get("redis.password");
        if (redisPasswd == null) {
            redis = new CoopRedis(host, port);
        } else {
            redis = new CoopRedis(host, port, redisPasswd);
        }
    }
    public static boolean existInSet(CoopRedis redis, String key, String vaule) {
        Jedis jedis = null;
        try {
            jedis = redis.getJedis();
            return jedis.sismember(key, vaule);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }
}
