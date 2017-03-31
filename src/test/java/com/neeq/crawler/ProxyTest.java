package com.neeq.crawler;

import com.alibaba.fastjson.JSONObject;
import org.junit.Test;

/**
 * Created by bj on 16/7/1.
 */
public class ProxyTest {
    @Test
    public void Test(){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(null, null);
        System.out.println(jsonObject.toJSONString());
    }
}
