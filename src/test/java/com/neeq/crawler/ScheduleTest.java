package com.neeq.crawler;

import org.junit.After;
import org.junit.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by kidbei on 16/5/23.
 */
public class ScheduleTest {

    @Test
    public void test() {

        Executors.newScheduledThreadPool(1).scheduleWithFixedDelay(new Runnable() {

            int a = 0;
            @Override
            public void run() {
                a ++;
                if (a == 5) {
                    System.out.println("*******");
                    throw new RuntimeException();
                }
                System.out.println(a);
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
    }


    @After
    public void after() {
        try {
            Thread.sleep(1000 * 10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
