package com.neeq.crawler.push.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.neeq.crawler.dependence.Config;
import com.neeq.crawler.push.PushQueue;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by kidbei on 16/5/19.
 */
public class KafkaPushQueue implements PushQueue {


    private final Logger log = LoggerFactory.getLogger(KafkaPushQueue.class);

    private KafkaProducer<String,byte[]> producer;



    public KafkaPushQueue() {
        log.info("start to init kafka queue producer");

        Properties props = new Properties();
        props.put("bootstrap.servers", Config.get("producer.bootstrap.servers","127.0.0.1:9092"));
        props.put("acks", "all");
        props.put("retries", Config.getInt("producer.retries",0));
        props.put("batch.size", Config.getInt("producer.batch.size",16384));
        props.put("linger.ms", Config.getInt("producer.linger.ms",1));
        props.put("buffer.memory", Config.getInt("producer.buffer.memory",33554432));
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");

        producer = new KafkaProducer<>(props);

        log.info("kafka producer is init whith properties:\n{}",props.toString());
    }



    @Override
    public void sendToQueue(String topic, byte[] bytes) {
        ProducerRecord<String,byte[]> record = new ProducerRecord<>(topic,bytes);
        Future<RecordMetadata> future = producer.send(record);
        if (log.isDebugEnabled()) {
            try {
                log.debug("send kafka record success : offset-->{},topic-->{},partition-->{}",
                        future.get().offset(),future.get().topic(),future.get().partition());
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        try {

            JSONObject object = JSONObject.parseObject(new String(bytes));
            String json = object.toJSONString();
            System.out.println(json);
        } catch (Exception e) {

            JSONArray object = JSONObject.parseArray(new String(bytes));
            String json = object.toJSONString();
            System.out.println(json);
        }
    }


}
