package com.neeq.crawler.consumer.companyInfo.bean;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by bj on 16/7/26.
 */
public class CompanyInfo {
    Company_baseinfo baseinfo;
    List executives;
    List topTenHolders;
    Finance finance;

    public static Map getClassMap() {
        Map<Object, Object> map = new HashMap<>();
        map.put("baseinfo", Company_baseinfo.class);
        map.put("executives", Executives.class);
        map.put("topTenHolders", TopTenHolders.class);
        map.put("finance", Finance.class);
        return map;
    }

    public Company_baseinfo getBaseinfo() {
        return baseinfo;
    }

    public void setBaseinfo(Company_baseinfo baseinfo) {
        this.baseinfo = baseinfo;
    }

    public List getExecutives() {
        return executives;
    }

    public void setExecutives(List executives) {
        this.executives = executives;
    }

    public List getTopTenHolders() {
        return topTenHolders;
    }

    public void setTopTenHolders(List topTenHolders) {
        this.topTenHolders = topTenHolders;
    }

    public Finance getFinance() {
        return finance;
    }

    public void setFinance(Finance finance) {
        this.finance = finance;
    }
}
