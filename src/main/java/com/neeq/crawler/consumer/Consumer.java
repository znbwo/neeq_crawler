package com.neeq.crawler.consumer;

/**
 * Created by kidbei on 16/7/1.
 */
public interface Consumer {


    void consume(String topic ,byte[] data);

}
