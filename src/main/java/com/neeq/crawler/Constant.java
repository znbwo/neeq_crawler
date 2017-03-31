package com.neeq.crawler;

/**
 * Created by kidbei on 16/5/19.
 */
public class Constant {


    public interface Topic {
        String INDUSTRY_TOPIC = "dic_industry"; //行业分类 **  弃用
        String PLACE_TOPIC = "dic_place";       //地区    **弃用
        String STOCK_DIS_TOPIC = "stock_dis";   //新三板交易数据统计  **弃用
        String INVEST_DATA_TOPIC = "invest_data";   //投资数据  **弃用
        String INVEST_ORG_TOPIC = "invest_org"; //投资机构   **弃用
        String INVEST_PERSON_TOPIC = "invest_person";   //投资人   **弃用
        String AUDITING_COMPANY_TOPIC = "auditing_company"; //正在审核的新三板公司  **弃用
        String ZBQS_TOPIC = "dic_zbqs";         //主办券商  **弃用


        String NEEQ_COMPANY_TOPIC = "neeq_company";         //挂牌公司
        String INVEST_ORG_TOUZIJIE_TOPIC = "invest_org_touzijie";//投资界的投资机构数据
        String APPLY_COMPANY_INFO_TOPIC = "apply_company_info";//正在审核的新三板公司
        String DELETE_COMPANY_TOPIC = "neeq_delete_company";    //已退市公司
        String NOTICE_TOPIC = "neeq_notice";    //公告
        String NEWS_TOPIC = "company_news";     //新闻
        String ZBQS_INFO_TOPIC = "zbqs_info";   //主办券商信息
        String ZENGFA_TOPIC = "zengfa";          //增发数据
        String APPLY_COMPANY_NEWS_TOPIC = "apply_company_news_list";//审查公开信息
        String INVEST_DATA_TOUZIJIE_TOPIC = "invest_data_touzijie";   //投资界的投资数据
        String MERGER_DATA_TOUZIJIE_TOPIC = "merger_data_touzijie";   //投资界的并购数据
        String FUNDRAISING_DATA_TOUZIJIE_TOPIC = "fundraising_data_touzijie";   //投资界的募资数据
        String SIMUJIJINGUANLIREN__TOPIC = "simujijinguanliren";//私募基金管理人综合查询
        String COMPANY_REPORT_TOPIC = "company_report_topic";//中证协公司年报
        String COMPANY_INFO_ZZX_TOPIC = "company_info_zzx_topic";//中证协证券公司信息
        String COMPANY_REPORT_HEXUN_TOPIC = "company_report_hexun_topic";//和讯公司研报
        String INDUSTRY_REPORT_HEXUN_TOPIC = "industry_report_hexun_topic";//和讯行业研报
        String MACRO_REPORT_HEXUN_TOPIC = "macro_report_hexun_topic";//和讯宏观研报
        String NEW_STOCK_REPORT_HEXUN_TOPIC = "new_stock_report_hexun_topic";//和讯新股研报
        String TIANYANCHA_SITE_TOPIC = "tianyancha_site_topic";//天眼查网站数据

        String CNINFO_NOTICE_TOPIC = "cninfo_notice"; //巨潮公告


        String NEEQ_RULES = "neeq_rules"; //股转系统法规及规章
        String NEEQ_ZHISHU_ZIXUN = "neeq_zhishu_zixun"; //股转系统指数资讯
    }


    public interface Redis {
        String PROXY_HTTP_CHECK_QUEUE = "proxy_http_check_queue";
        String PROXY_HTTPS_CHECK_QUEUE = "proxy_https_check_queue";
        String PROXY_HTTP_OK_QUEUE = "proxy_http_ok_queue";
        String PROXY_HTTPS_OK_QUEUE = "proxy_https_ok_queue";
        String NEWS_CHECK_REPEAT_QUEUE = "news_check_repeat";
        String INVEST_REPEAT_QUEUE = "invest_check_repeat";
        String INVEST_ORG_REPEAT_QUEUE = "invest_org_check_repeat";//投资机构
        String INVEST_PERSON_REPEAT_QUEUE = "invest_person_check_repeat";//投资人
        String INVEST_TOUZIJIE_REPEAT_QUEUE = "invest_touzijie_check_repeat";//投资界的投资数据
        String COMPANY_REPORT_CHECK_QUEUE = "company_report_check_queue";//公司年报
        String COMPANY_INFO_ZZX_QUEUE = "company_info_zzx_queue";//中证协证券公司信息
        String RESEARCH_REPORT_HEXUN_QUEUE = "research_report_hexun_queue";//和讯研报
        String TIANYANCHA_SITE_QUEUE = "tianyancha_site_queue";//天眼查网站数据

        String NEEQ_RULES = "neeq_rules"; //股转系统法规及规章
        String NEEQ_ZHISHU_ZIXUN = "neeq_zhishu_zixun"; //股转系统指数资讯



    }
}
