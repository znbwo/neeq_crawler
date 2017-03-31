package com.neeq.crawler.push;

/**
 * Created by kidbei on 16/5/19.
 */
public interface PushQueue {

    void sendToQueue(String topic,byte[] bytes);

}
