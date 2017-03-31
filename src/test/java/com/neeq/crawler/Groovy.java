package com.neeq.crawler;

import org.junit.Test;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

/**
 * Created by kidbei on 16/5/22.
 */
public class Groovy {

    @Test
    public void test() {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("groovy");
        try{

            engine.put("name","hello");
            engine.eval("println \"${name}\"+\"  你好\";name=name+'！'");

        } catch(Exception e) {
            e.printStackTrace();
        }


    }

}
