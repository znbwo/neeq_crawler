package com.neeq.crawler.consumer;

import com.alibaba.fastjson.JSONObject;
import com.neeq.crawler.dependence.Config;
import com.neeq.crawler.pool.ThreadPool;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

/**
 * Created by kidbei on 16/7/1.
 */
public abstract class DefaultKafkaConsumer implements Consumer {

    private static final Logger log = LoggerFactory.getLogger(DefaultKafkaConsumer.class);

    private final long pollTimeout = 1000 * 60;

    private KafkaConsumer<String, byte[]> consumer;


    private String topic;

    public DefaultKafkaConsumer(String topic) {
        this.topic = topic;
        init();
    }


    private void init() {
        Properties props = new Properties();
        props.put("bootstrap.servers", Config.get("consumer.bootstrap.servers", "127.0.0.1:9092"));
        props.put("offsets.storage", "kafka");
        props.put("group.id", Config.get("consumer.groupId", "crawler1"));
        props.put("enable.auto.commit", "true");
        props.put("auto.commit.interval.ms", "1000");
        props.put("session.timeout.ms", "30000");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        consumer = new KafkaConsumer<>(props);

    }


    private class ConsumerRunner implements Runnable {

        @Override
        public void run() {
            consumer.subscribe(Arrays.asList(topic));

            try {
                while (true) {
                    ConsumerRecords<String, byte[]> records = consumer.poll(pollTimeout);
                    for (ConsumerRecord<String, byte[]> record : records) {
                        if (log.isDebugEnabled()) {
                            log.debug("offset = {}, key = {}, value = {}, partition = {}, topic = {} \n",
                                    record.offset(), record.key(), record.value(), record.partition(), record.topic());
                        }
                        byte[] body = record.value();
                        consume(topic, body);
                    }
                }
            } catch (Exception e) {
                log.error("poll messages error on topic {}", topic, e);
            }
        }
    }


    public void start() {
        ThreadPool.getPool().execute(new ConsumerRunner());
    }

    public static void main(String[] args) {
        DefaultKafkaConsumer consumer = new DefaultKafkaConsumer("neeq_rules") {
            @Override
            public void consume(String topic, byte[] data) {
                JSONObject json = JSONObject.parseObject(new String(data));
                System.out.println(json.toJSONString());
            }
        };
        consumer.start();
        CountDownLatch latch = new CountDownLatch(1);
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
