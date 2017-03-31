package com.neeq.crawler.consumer.companyInfo;

/**
 * Created by bj on 16/7/26.
 */
public interface CompanyInfoMapper {
    public int getId();

    public int selectIndustryCode(String industry);

    public int selectBrokerCode(String broker);

}
