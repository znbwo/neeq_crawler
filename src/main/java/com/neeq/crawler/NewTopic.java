package com.neeq.crawler;

/**
 * Created by bj on 16/7/25.
 */
public interface NewTopic {
    //公司信息 1,表示挂牌中。2.表示退市
    String COMPANY_INFO = "company_info";
    //正在审核的新三板公司
    String APPLY_COMPANY_INFO = "apply_company_info";
    //公告
    String POST_INFO = "post_info";
    //券商信息
    String BROKERAGE_INFO = "brokerage_info";
    //新闻
    String NEWS_INFO = "news_info";
    //股票增发信息
    String RAISE_EQUITY_INFO = "raise_equity_info";
    //投资机构信息
    String INVESTMENT_INSTITUTION_INFO = "investment_institution_info";
    //投资数据
    String INVESTMENT_DATA = "investment_data";
    //并购数据
    String MERGE_DATA = "merge_data";
    //募资数据
    String FUNDRAISING_DATA = "fundraising_data";
    //私募基金管理人
    String PRIVATELY_OFFERED_FUND_MANAGER = "privately_offered_fund_manager";
    //研报
    String RESEARCH_REPORT = "research_report";

}
