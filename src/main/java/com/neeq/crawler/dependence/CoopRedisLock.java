package com.neeq.crawler.dependence;

/**
 * Created by bj on 16/7/19.
 */
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Random;

public class CoopRedisLock {
    public static final String LOCKED = "TRUE";
    public static final long MILLI_NANO_CONVERSION = 1000000L;
    public static final long DEFAULT_TIME_OUT = 1000L;
    public static final Random RANDOM = new Random();
    public static final int EXPIRE = 180;
    private JedisPool jedisPool;
    private Jedis jedis;
    private String key;
    private boolean locked = false;

    public CoopRedisLock(String key, JedisPool jedisPool) {
        this.key = key + "_lock";
        this.jedisPool = jedisPool;
        this.jedis = this.jedisPool.getResource();
    }

    public boolean lock(long timeout) {
        long nano = System.nanoTime();
        timeout *= 1000000L;

        try {
            while(System.nanoTime() - nano < timeout) {
                if(this.jedis.setnx(this.key, "TRUE").longValue() == 1L) {
                    this.jedis.expire(this.key, 180);
                    this.locked = true;
                    return this.locked;
                }

                Thread.sleep(3L, RANDOM.nextInt(500));
            }

            return false;
        } catch (Exception var6) {
            throw new RuntimeException("Locking error", var6);
        }
    }

    public boolean lock(long timeout, int expire) {
        long nano = System.nanoTime();
        timeout *= 1000000L;

        try {
            while(System.nanoTime() - nano < timeout) {
                if(this.jedis.setnx(this.key, "TRUE").longValue() == 1L) {
                    this.jedis.expire(this.key, expire);
                    this.locked = true;
                    return this.locked;
                }

                Thread.sleep(3L, RANDOM.nextInt(500));
            }

            return false;
        } catch (Exception var7) {
            throw new RuntimeException("Locking error", var7);
        }
    }

    public boolean lock() {
        return this.lock(1000L);
    }

    public void unlock() {
        try {
            if(this.locked) {
                this.jedis.del(this.key);
            }
        } finally {
            this.jedisPool.returnResource(this.jedis);
        }

    }
}
