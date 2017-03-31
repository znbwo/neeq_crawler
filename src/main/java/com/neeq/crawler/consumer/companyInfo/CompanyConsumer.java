package com.neeq.crawler.consumer.companyInfo;

import com.neeq.crawler.NewTopic;
import com.neeq.crawler.consumer.DefaultKafkaConsumer;
import com.neeq.crawler.consumer.companyInfo.bean.CompanyInfo;
import com.neeq.crawler.consumer.db.SessionFactory;
import net.sf.json.JSONObject;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 获取消息队列里面的公司数据
 * Created by kidbei on 16/6/29.
 */
public class CompanyConsumer extends DefaultKafkaConsumer {
    private final static Logger log = LoggerFactory.getLogger(CompanyConsumer.class);


    public CompanyConsumer() {
        this(NewTopic.COMPANY_INFO);
    }

    public CompanyConsumer(String topic) {
        super(topic);
    }

    @Override
    public void consume(String topic, byte[] data) {
        String json = new String(data);
        CompanyInfo bean = (CompanyInfo) JSONObject.toBean(JSONObject.fromObject(json), CompanyInfo.class, CompanyInfo.getClassMap());
        //mybatis
        SqlSession session = SessionFactory.getSession().openSession();
        String industry = session.selectOne("InfoUtil.selectIndustryCode", bean.getBaseinfo().getIndustry());
        String broker = session.selectOne("InfoUtil.selectBrokerCode", bean.getBaseinfo().getBroker());
        session.selectOne("CompanyInfo", bean);
        System.out.println();
//        CompanyInfo mapper = session.getMapper(CompanyInfo.class);
//        mapper.se();

    }
}