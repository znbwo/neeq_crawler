package com.neeq.crawler;

import com.alibaba.fastjson.JSONObject;

import java.lang.reflect.Field;

/**
 * Created by bj on 16/7/26.
 */
public class TestJson {
    public static void main(String[] args) {
//        JSONObject object = new JSONObject();
//        object.put("name", "jack");
//        object.put("books", new JSONArray());
//        JSONArray books = object.getJSONArray("books");
//        books.add("math");
//        books.add("art");
//        ListedCompanyCrawlerResult result = new ListedCompanyCrawlerResult();
//        getObjByJson(result, object);
//        System.out.println(result);
    }

    /**
     * 通过json对象 要被转换后的对象对象
     *
     * @param m
     * @param obj
     * @return
     */
    public static Object getObjByJson(Object m, JSONObject obj) {
        try {
            Class<?> mClass = m.getClass();
            Field[] fields = mClass.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                Field f = fields[i];
                f.setAccessible(true);
                String name = f.getName();
                if (f.getType().equals(String.class)) {
                    String value = obj.getString(name);
                    f.set(m, value);
                } else if ("int".equals(f.getType().toString())) {
                    int value = obj.getIntValue(name);
                    f.set(m, value);
                } else if ("boolean".equals(f.getType().toString())) {
                    boolean value = obj.getBooleanValue(name);
                    f.set(m, value);
                } else if ("double".equals(f.getType().toString())) {
                    double value = obj.getDoubleValue(name);
                    f.set(m, value);
                } else if ("long".equals(f.getType().toString())) {
                    Long value = obj.getLongValue(name);
                    f.set(m, value);
                } else if ("float".equals(f.getType().toString())) {
                    float value = obj.getFloatValue(name);
                    f.set(m, value);
                } else if ("short".equals(f.getType().toString())) {
                    short value = obj.getShortValue(name);
                    f.set(m, value);
                }
//                } else if (f.getType().equals(ArrayList.class)
//                        || f.getType().equals(List.class)) {
//                    JSONArray array = obj.getJSONArray(name);
//                    ArrayList<Object> list = new ArrayList<>();
//                    Collections.addAll(list, array.toArray());
//                    f.set(m, list);
//                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return m;
    }
}
