package com.neeq.crawler.dependence;

/**
 * Created by bj on 16/7/19.
 */
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//


import redis.clients.jedis.*;

import java.util.*;

public class CoopRedis {
    private JedisPool pool;

    public CoopRedis(String host, int port) {
        this(host, port, (String) null);
    }

    public CoopRedis(String host, int port, String password) {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxIdle(10);
        config.setMaxTotal(1000);
        config.setMaxWaitMillis(20000L);
        config.setTestOnBorrow(true);
        if (password != null) {
            this.pool = new JedisPool(config, host, port, '썐', password);
        } else {
            this.pool = new JedisPool(config, host, port);
        }

    }

    public Jedis getJedis() {
        return this.pool.getResource();
    }

    public long delKeysLike(final String likeKey) {
        return ((Long) (new CoopRedis.Executor(this.pool) {
            Long execute() {
                long count = 0L;
                Set keys = this.jedis.keys(likeKey + "*");
                count += this.jedis.del((String[]) keys.toArray(new String[keys.size()])).longValue();
                return Long.valueOf(count);
            }
        }).getResult()).longValue();
    }

    public Long delKey(final String key) {
        return (Long) (new CoopRedis.Executor(this.pool) {
            Long execute() {
                return this.jedis.del(key);
            }
        }).getResult();
    }

    public Long delKeys(final String[] keys) {
        return (Long) (new CoopRedis.Executor(this.pool) {
            Long execute() {
                return this.jedis.del(keys);
            }
        }).getResult();
    }

    public Long expire(final String key, final int expire) {
        return (Long) (new CoopRedis.Executor(this.pool) {
            Long execute() {
                return this.jedis.expire(key, expire);
            }
        }).getResult();
    }

    public long makeId(final String key) {
        return ((Long) (new CoopRedis.Executor(this.pool) {
            Long execute() {
                long id = this.jedis.incr(key).longValue();
                if (id + 75807L >= 9223372036854775807L) {
                    this.jedis.getSet(key, "0");
                }

                return Long.valueOf(id);
            }
        }).getResult()).longValue();
    }

    public Long insertToSet(final String key, final String value) {
        return (Long) (new CoopRedis.Executor(this.pool) {
            Long execute() {
                return this.jedis.sadd(key, new String[]{value});
            }
        }).getResult();
    }

    public Set<String> getSetMembers(final String key) {
        return (Set) (new CoopRedis.Executor(this.pool) {
            Set<String> execute() {
                return this.jedis.smembers(key);
            }
        }).getResult();
    }

    public String setString(final String key, final String value) {
        return (String) (new CoopRedis.Executor(this.pool) {
            String execute() {
                return this.jedis.set(key, value);
            }
        }).getResult();
    }

    public String setString(final byte[] key, final byte[] value) {
        return (String) (new CoopRedis.Executor(this.pool) {
            String execute() {
                return this.jedis.set(key, value);
            }
        }).getResult();
    }

    public String setString(final byte[] key, final byte[] value, final int expire) {
        return (String) (new CoopRedis.Executor(this.pool) {
            String execute() {
                return this.jedis.setex(key, expire, value);
            }
        }).getResult();
    }

    public String setString(final String key, final String value, final int expire) {
        return (String) (new CoopRedis.Executor(this.pool) {
            String execute() {
                return this.jedis.setex(key, expire, value);
            }
        }).getResult();
    }

    public Long setStringIfNotExists(final String key, final String value) {
        return (Long) (new CoopRedis.Executor(this.pool) {
            Long execute() {
                return this.jedis.setnx(key, value);
            }
        }).getResult();
    }

    public String getString(final String key) {
        return (String) (new CoopRedis.Executor(this.pool) {
            String execute() {
                return this.jedis.get(key);
            }
        }).getResult();
    }

    public byte[] getString(final byte[] key) {
        return (byte[]) (new CoopRedis.Executor(this.pool) {
            byte[] execute() {
                return this.jedis.get(key);
            }
        }).getResult();
    }

    public List<Object> batchSetString(final List<CoopRedis.Pair<String, String>> pairs) {
        return (List) (new CoopRedis.Executor(this.pool) {
            List<Object> execute() {
                Pipeline pipeline = this.jedis.pipelined();
                Iterator var2 = pairs.iterator();

                while (var2.hasNext()) {
                    CoopRedis.Pair pair = (CoopRedis.Pair) var2.next();
                    pipeline.set((String) pair.getKey(), (String) pair.getValue());
                }

                return pipeline.syncAndReturnAll();
            }
        }).getResult();
    }

    public List<String> batchGetString(final String[] keys) {
        return (List) (new CoopRedis.Executor(this.pool) {
            List<String> execute() {
                Pipeline pipeline = this.jedis.pipelined();
                ArrayList result = new ArrayList(keys.length);
                ArrayList responses = new ArrayList(keys.length);
                String[] var4 = keys;
                int resp = var4.length;

                for (int var6 = 0; var6 < resp; ++var6) {
                    String key = var4[var6];
                    responses.add(pipeline.get(key));
                }

                pipeline.sync();
                Iterator var8 = responses.iterator();

                while (var8.hasNext()) {
                    Response var9 = (Response) var8.next();
                    result.add(var9.get());
                }

                return result;
            }
        }).getResult();
    }

    public Long hashSet(final String key, final String field, final String value) {
        return (Long) (new CoopRedis.Executor(this.pool) {
            Long execute() {
                return this.jedis.hset(key, field, value);
            }
        }).getResult();
    }

    public Long hashDel(final String key, final String field) {
        return (Long) (new CoopRedis.Executor(this.pool) {
            Long execute() {
                return this.jedis.hdel(key, new String[]{field});
            }
        }).getResult();
    }

    public Long hashINCR(final String key, final String field) {
        return (Long) (new CoopRedis.Executor(this.pool) {
            Long execute() {
                return this.jedis.hincrBy(key, field, 1L);
            }
        }).getResult();
    }

    public Long hashSet(final String key, final String field, final String value, final int expire) {
        return (Long) (new CoopRedis.Executor(this.pool) {
            Long execute() {
                Pipeline pipeline = this.jedis.pipelined();
                Response result = pipeline.hset(key, field, value);
                pipeline.expire(key, expire);
                pipeline.sync();
                return (Long) result.get();
            }
        }).getResult();
    }

    public String hashGet(final String key, final String field) {
        return (String) (new CoopRedis.Executor(this.pool) {
            String execute() {
                return this.jedis.hget(key, field);
            }
        }).getResult();
    }

    public String hashGet(final String key, final String field, final int expire) {
        return (String) (new CoopRedis.Executor(this.pool) {
            String execute() {
                Pipeline pipeline = this.jedis.pipelined();
                Response result = pipeline.hget(key, field);
                pipeline.expire(key, expire);
                pipeline.sync();
                return (String) result.get();
            }
        }).getResult();
    }

    public String hashMultipleSet(final String key, final Map<String, String> hash) {
        return (String) (new CoopRedis.Executor(this.pool) {
            String execute() {
                return this.jedis.hmset(key, hash);
            }
        }).getResult();
    }

    public String hashMultipleSet(final String key, final Map<String, String> hash, final int expire) {
        return (String) (new CoopRedis.Executor(this.pool) {
            String execute() {
                Pipeline pipeline = this.jedis.pipelined();
                Response result = pipeline.hmset(key, hash);
                pipeline.expire(key, expire);
                pipeline.sync();
                return (String) result.get();
            }
        }).getResult();
    }

    public List<String> hashMultipleGet(final String key, final String... fields) {
        return (List) (new CoopRedis.Executor(this.pool) {
            List<String> execute() {
                return this.jedis.hmget(key, fields);
            }
        }).getResult();
    }

    public List<String> hashMultipleGet(final String key, final int expire, final String... fields) {
        return (List) (new CoopRedis.Executor(this.pool) {
            List<String> execute() {
                Pipeline pipeline = this.jedis.pipelined();
                Response result = pipeline.hmget(key, fields);
                pipeline.expire(key, expire);
                pipeline.sync();
                return (List) result.get();
            }
        }).getResult();
    }

    public List<String> hashSetGetAll(final String key, final int count) {
        return (List) (new CoopRedis.Executor(this.pool) {
            List<String> execute() {
                return this.jedis.srandmember(key, count);
            }
        }).getResult();
    }

    public List<Object> batchHashMultipleSet(final List<CoopRedis.Pair<String, Map<String, String>>> pairs) {
        return (List) (new CoopRedis.Executor(this.pool) {
            List<Object> execute() {
                Pipeline pipeline = this.jedis.pipelined();
                Iterator var2 = pairs.iterator();

                while (var2.hasNext()) {
                    CoopRedis.Pair pair = (CoopRedis.Pair) var2.next();
                    pipeline.hmset((String) pair.getKey(), (Map) pair.getValue());
                }

                return pipeline.syncAndReturnAll();
            }
        }).getResult();
    }

    public List<Object> batchHashMultipleSet(final Map<String, Map<String, String>> data) {
        return (List) (new CoopRedis.Executor(this.pool) {
            List<Object> execute() {
                Pipeline pipeline = this.jedis.pipelined();
                Iterator var2 = data.entrySet().iterator();

                while (var2.hasNext()) {
                    Map.Entry iter = (Map.Entry) var2.next();
                    pipeline.hmset((String) iter.getKey(), (Map) iter.getValue());
                }

                return pipeline.syncAndReturnAll();
            }
        }).getResult();
    }

    public List<List<String>> batchHashMultipleGet(final List<CoopRedis.Pair<String, String[]>> pairs) {
        return (List) (new CoopRedis.Executor(this.pool) {
            List<List<String>> execute() {
                Pipeline pipeline = this.jedis.pipelined();
                ArrayList result = new ArrayList(pairs.size());
                ArrayList responses = new ArrayList(pairs.size());
                Iterator var4 = pairs.iterator();

                while (var4.hasNext()) {
                    CoopRedis.Pair resp = (CoopRedis.Pair) var4.next();
                    responses.add(pipeline.hmget((String) resp.getKey(), (String[]) resp.getValue()));
                }

                pipeline.sync();
                var4 = responses.iterator();

                while (var4.hasNext()) {
                    Response resp1 = (Response) var4.next();
                    result.add(resp1.get());
                }

                return result;
            }
        }).getResult();
    }

    public Map<String, String> hashGetAll(final String key) {
        return (Map) (new CoopRedis.Executor(this.pool) {
            Map<String, String> execute() {
                return this.jedis.hgetAll(key);
            }
        }).getResult();
    }

    public Map<String, String> hashGetAll(final String key, final int expire) {
        return (Map) (new CoopRedis.Executor(this.pool) {
            Map<String, String> execute() {
                Pipeline pipeline = this.jedis.pipelined();
                Response result = pipeline.hgetAll(key);
                pipeline.expire(key, expire);
                pipeline.sync();
                return (Map) result.get();
            }
        }).getResult();
    }

    public List<Map<String, String>> batchHashGetAll(final String... keys) {
        return (List) (new CoopRedis.Executor(this.pool) {
            List<Map<String, String>> execute() {
                Pipeline pipeline = this.jedis.pipelined();
                ArrayList result = new ArrayList(keys.length);
                ArrayList responses = new ArrayList(keys.length);
                String[] var4 = keys;
                int resp = var4.length;

                for (int var6 = 0; var6 < resp; ++var6) {
                    String key = var4[var6];
                    responses.add(pipeline.hgetAll(key));
                }

                pipeline.sync();
                Iterator var8 = responses.iterator();

                while (var8.hasNext()) {
                    Response var9 = (Response) var8.next();
                    result.add(var9.get());
                }

                return result;
            }
        }).getResult();
    }

    public Map<String, Map<String, String>> batchHashGetAllForMap(final String... keys) {
        return (Map) (new CoopRedis.Executor(this.pool) {
            Map<String, Map<String, String>> execute() {
                Pipeline pipeline = this.jedis.pipelined();

                int capacity;
                for (capacity = 1; (int) ((double) capacity * 0.75D) <= keys.length; capacity <<= 1) {
                    ;
                }

                HashMap result = new HashMap(capacity);
                ArrayList responses = new ArrayList(keys.length);
                String[] i = keys;
                int var6 = i.length;

                for (int var7 = 0; var7 < var6; ++var7) {
                    String key = i[var7];
                    responses.add(pipeline.hgetAll(key));
                }

                pipeline.sync();

                for (int var9 = 0; var9 < keys.length; ++var9) {
                    result.put(keys[var9], ((Response) responses.get(var9)).get());
                }

                return result;
            }
        }).getResult();
    }

    public Long listPushTail(final String key, final String... values) {
        return (Long) (new CoopRedis.Executor(this.pool) {
            Long execute() {
                return this.jedis.rpush(key, values);
            }
        }).getResult();
    }

    public Long listPushHead(final String key, final String value) {
        return (Long) (new CoopRedis.Executor(this.pool) {
            Long execute() {
                return this.jedis.lpush(key, new String[]{value});
            }
        }).getResult();
    }

    public Long listPushHead(final byte[] key, final byte[]... values) {
        return (Long) (new CoopRedis.Executor(this.pool) {
            Long execute() {
                return this.jedis.lpush(key, values);
            }
        }).getResult();
    }

    public byte[] listPopHead(final byte[] key) {
        return (byte[]) (new CoopRedis.Executor(this.pool) {
            byte[] execute() {
                return this.jedis.lpop(key);
            }
        }).getResult();
    }

    public String listPopHead(final String key) {
        return (String) (new CoopRedis.Executor(this.pool) {
            String execute() {
                return this.jedis.lpop(key);
            }
        }).getResult();
    }

    public Long countList(final String key) {
        return (Long) (new CoopRedis.Executor(this.pool) {
            Long execute() {
                return this.jedis.llen(key);
            }
        }).getResult();
    }

    public Long countList(final byte[] key) {
        return (Long) (new CoopRedis.Executor(this.pool) {
            Long execute() {
                return this.jedis.llen(key);
            }
        }).getResult();
    }

    public Long listPushHeadAndTrim(final String key, final String value, final long size) {
        return (Long) (new CoopRedis.Executor(this.pool) {
            Long execute() {
                Pipeline pipeline = this.jedis.pipelined();
                Response result = pipeline.lpush(key, new String[]{value});
                pipeline.ltrim(key, 0L, size - 1L);
                pipeline.sync();
                return (Long) result.get();
            }
        }).getResult();
    }

    public Object updateListInTransaction(final String key, final List<String> values) {
        return (new CoopRedis.Executor(this.pool) {
            Object execute() {
                Transaction transaction = this.jedis.multi();
                transaction.del(key);
                Iterator var2 = values.iterator();

                while (var2.hasNext()) {
                    String value = (String) var2.next();
                    transaction.rpush(key, new String[]{value});
                }

                transaction.exec();
                return null;
            }
        }).getResult();
    }

    public Long insertListIfNotExists(final String key, final String[] values) {
        return (Long) (new CoopRedis.Executor(this.pool) {
            Long execute() {
                CoopRedisLock lock = new CoopRedisLock(key, CoopRedis.this.pool);
                lock.lock();

                try {
                    if (!this.jedis.exists(key).booleanValue()) {
                        Long var2 = this.jedis.rpush(key, values);
                        return var2;
                    }
                } finally {
                    lock.unlock();
                }

                return Long.valueOf(0L);
            }
        }).getResult();
    }

    public List<String> listGetAll(final String key) {
        return (List) (new CoopRedis.Executor(this.pool) {
            List<String> execute() {
                return this.jedis.lrange(key, 0L, -1L);
            }
        }).getResult();
    }

    public String delListRange(final String key, final long start, final long stop) {
        return (String) (new CoopRedis.Executor(this.pool) {
            String execute() {
                return this.jedis.ltrim(key, start, stop);
            }
        }).getResult();
    }

    public String delListRange(final byte[] key, final long start, final long stop) {
        return (String) (new CoopRedis.Executor(this.pool) {
            String execute() {
                return this.jedis.ltrim(key, start, stop);
            }
        }).getResult();
    }

    public List<byte[]> listGetAll(final byte[] key) {
        return (List) (new CoopRedis.Executor(this.pool) {
            List<byte[]> execute() {
                return this.jedis.lrange(key, 0L, -1L);
            }
        }).getResult();
    }

    public List<String> listRange(final String key, final long beginIndex, final long endIndex) {
        return (List) (new CoopRedis.Executor(this.pool) {
            List<String> execute() {
                return this.jedis.lrange(key, beginIndex, endIndex - 1L);
            }
        }).getResult();
    }

    public List<byte[]> listRange(final byte[] key, final long start, final long end) {
        return (List) (new CoopRedis.Executor(this.pool) {
            List<byte[]> execute() {
                return this.jedis.lrange(key, start, end - 1L);
            }
        }).getResult();
    }

    public Map<String, List<String>> batchGetAllList(final List<String> keys) {
        return (Map) (new CoopRedis.Executor(this.pool) {
            Map<String, List<String>> execute() {
                Pipeline pipeline = this.jedis.pipelined();
                HashMap result = new HashMap();
                ArrayList responses = new ArrayList(keys.size());
                Iterator i = keys.iterator();

                while (i.hasNext()) {
                    String key = (String) i.next();
                    responses.add(pipeline.lrange(key, 0L, -1L));
                }

                pipeline.sync();

                for (int var6 = 0; var6 < keys.size(); ++var6) {
                    result.put(keys.get(var6), ((Response) responses.get(var6)).get());
                }

                return result;
            }
        }).getResult();
    }

    public Long publish(final String channel, final String message) {
        return (Long) (new CoopRedis.Executor(this.pool) {
            Long execute() {
                return this.jedis.publish(channel, message);
            }
        }).getResult();
    }

    public void subscribe(final JedisPubSub jedisPubSub, final String channel) {
        (new CoopRedis.Executor(this.pool) {
            Object execute() {
                this.jedis.subscribe(jedisPubSub, new String[]{channel});
                return null;
            }
        }).getResult();
    }

    public void unSubscribe(JedisPubSub jedisPubSub) {
        jedisPubSub.unsubscribe();
    }

    public Long addWithSortedSet(final String key, final double score, final String member) {
        return (Long) (new CoopRedis.Executor(this.pool) {
            Long execute() {
                return this.jedis.zadd(key, score, member);
            }
        }).getResult();
    }

    public Long addWithSortedSet(final String key, final Map<String, Double> scoreMembers) {
        return (Long) (new CoopRedis.Executor(this.pool) {
            Long execute() {
                return this.jedis.zadd(key, scoreMembers);
            }
        }).getResult();
    }

    public Set<String> revrangeByScoreWithSortedSet(final String key, final double max, final double min) {
        return (Set) (new CoopRedis.Executor(this.pool) {
            Set<String> execute() {
                return this.jedis.zrevrangeByScore(key, max, min);
            }
        }).getResult();
    }

    public long incrBy(final String key, final long v) {
        return ((Long) (new CoopRedis.Executor(this.pool) {
            Long execute() {
                return this.jedis.incrBy(key, v);
            }
        }).getResult()).longValue();
    }

    public <K, V> CoopRedis.Pair<K, V> makePair(K key, V value) {
        return new CoopRedis.Pair(key, value);
    }

    public static void main(String[] args) {
        CoopRedis redis = new CoopRedis("127.0.0.1", 6379);
        redis.listPushHead("list", "1");
        redis.listPushHead("list", "2");
        redis.listPushHead("list", "3");
        redis.listPushHead("list", "4");
        redis.delListRange("list", 0L, -2L);
        List rs = redis.listRange("list", 0L, 4L);
        Iterator var3 = rs.iterator();

        while (var3.hasNext()) {
            String r = (String) var3.next();
            System.out.println(r);
        }

        redis.delKey("list");
    }

    public class Pair<K, V> {
        private K key;
        private V value;

        public Pair(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public K getKey() {
            return this.key;
        }

        public void setKey(K key) {
            this.key = key;
        }

        public V getValue() {
            return this.value;
        }

        public void setValue(V value) {
            this.value = value;
        }
    }

    abstract class Executor<T> {
        Jedis jedis;
        JedisPool jedisPool;

        public Executor(JedisPool jedisPool) {
            this.jedisPool = jedisPool;
            this.jedis = jedisPool.getResource();
        }

        abstract T execute();

        public T getResult() {
            Object result = null;

            try {
                result = this.execute();
            } catch (Throwable var6) {
                throw new RuntimeException("Redis execute exception", var6);
            } finally {
                if (this.jedis != null) {
                    this.jedis.close();
                }

            }

            return (T) result;
        }
    }
}

