package com.neeq.crawler.consumer.test;

import com.neeq.crawler.consumer.companyInfo.CompanyInfoMapper;
import com.neeq.crawler.consumer.companyInfo.bean.CompanyInfo;
import com.neeq.crawler.consumer.db.SessionFactory;
import net.sf.json.JSONObject;
import org.apache.ibatis.session.SqlSession;

/**
 * Created by bj on 16/7/26.
 */
public class TestJsonLib {
    public static void main(String[] args) {
        String json = "{\"baseinfo\":{\"area\":\"北京市\",\"englishName\":\"Sinosoft Co., Ltd.\",\"listingDate\":\"20060123\",\"website\":\"http://www.sinosoft.com.cn/\",\"code\":\"430002\",\"address\":\"北京市海淀区中关村新科祥园甲六号楼\",\"postcode\":\"null\",\"industry\":\"软件和信息技术服务业\",\"broker\":\"申万宏源证券有限公司\",\"shortname\":\"中科软\",\"secretaries\":\"张玮\",\"legalRepresentative\":\"左春\",\"phone\":\"010-62570007\",\"transferMode\":\"协议\",\"name\":\"中科软科技股份有限公司\",\"fax\":\"010-82523227\",\"email\":\"zhangwei@sinosoft.com.cn\",\"totalStockEquity\":\"381600000\",\"desc\":\"公司是一家集行业解决方案设计、自主软件产品研发、大型行业应用软件开发、系统集成与服务、技术支持和培训于一体的公司,是中国软件产业最大规模前100家企业之一.公司拥有多项自主研发的核心产品,其中\\\"保险核心业务处理系统\\\"一直在国内保险行业的信息建设中处于领先地位,被评为保险行业IT应用解决方案国内市场排名第一,公司同时还承担了国家\\\"十五\\\"科技攻关项目、科技部863项目等多项国家级科技攻关项目,行业应用解决方案得到国家应用软件产品质量监督检验中心的权威评测与认证,目前已成为Oracle、IBM,、HP,、Microsoft、BEA、Borland、BakBone、BMC及NEC等众多IT厂商的合作伙伴。\"},\"executives\":[{\"education\":\"硕士\",\"gender\":\"男\",\"name\":\"左春\",\"term\":\"2015.4-2016.4\",\"job\":\"董事长总经理\",\"salary\":\"是\",\"age\":\"57\"},{\"education\":\"硕士\",\"gender\":\"男\",\"name\":\"张玮\",\"term\":\"2013.5-2016.4\",\"job\":\"董事副总经理\",\"salary\":\"是\",\"age\":\"62\"},{\"education\":\"博士\",\"gender\":\"男\",\"name\":\"钟华\",\"term\":\"2014.7-2016.4\",\"job\":\"董事\",\"salary\":\"否\",\"age\":\"45\"},{\"education\":\"硕士\",\"gender\":\"男\",\"name\":\"刘志勇\",\"term\":\"2015.4-2016.4\",\"job\":\"董事\",\"salary\":\"否\",\"age\":\"39\"},{\"education\":\"本科\",\"gender\":\"男\",\"name\":\"林屹\",\"term\":\"2013.5-2016.4\",\"job\":\"董事\",\"salary\":\"否\",\"age\":\"46\"},{\"education\":\"硕士\",\"gender\":\"男\",\"name\":\"陈建军\",\"term\":\"2013.5-2016.4\",\"job\":\"董事\",\"salary\":\"是\",\"age\":\"52\"},{\"education\":\"本科\",\"gender\":\"男\",\"name\":\"冯卓志\",\"term\":\"2014.7-2016.4\",\"job\":\"独立董事\",\"salary\":\"是\",\"age\":\"60\"},{\"education\":\"博士\",\"gender\":\"女\",\"name\":\"顾奋玲\",\"term\":\"2013.5-2016.4\",\"job\":\"独立董事\",\"salary\":\"是\",\"age\":\"52\"},{\"education\":\"博士\",\"gender\":\"女\",\"name\":\"赵玉焕\",\"term\":\"2013.5-2016.4\",\"job\":\"独立董事\",\"salary\":\"是\",\"age\":\"42\"},{\"education\":\"硕士\",\"gender\":\"女\",\"name\":\"张天伴\",\"term\":\"2013.5-2016.4\",\"job\":\"监事长助理总经理核心技术人员\",\"salary\":\"是\",\"age\":\"48\"},{\"education\":\"硕士\",\"gender\":\"女\",\"name\":\"蔡庆安\",\"term\":\"2013.5-2016.4\",\"job\":\"监事\",\"salary\":\"否\",\"age\":\"45\"},{\"education\":\"硕士\",\"gender\":\"女\",\"name\":\"蒲洁宁\",\"term\":\"2014.7-2016.4\",\"job\":\"监事\",\"salary\":\"否\",\"age\":\"29\"},{\"education\":\"硕士\",\"gender\":\"男\",\"name\":\"邢立\",\"term\":\"2013.5-2016.4\",\"job\":\"副总经理核心技术人员\",\"salary\":\"是\",\"age\":\"47\"},{\"education\":\"硕士\",\"gender\":\"女\",\"name\":\"孙静\",\"term\":\"2013.5-2016.4\",\"job\":\"副总经理核心技术人员\",\"salary\":\"是\",\"age\":\"53\"},{\"education\":\"本科\",\"gender\":\"男\",\"name\":\"张志华\",\"term\":\"2013.5-2016.4\",\"job\":\"副总经理财务总监\",\"salary\":\"是\",\"age\":\"51\"},{\"education\":\"硕士\",\"gender\":\"男\",\"name\":\"谢中阳\",\"term\":\"2013.5-2016.4\",\"job\":\"副总经理核心技术人员\",\"salary\":\"是\",\"age\":\"47\"},{\"education\":\"硕士\",\"gender\":\"男\",\"name\":\"孙熙杰\",\"term\":\"2013.5-2016.4\",\"job\":\"副总经理核心技术人员\",\"salary\":\"是\",\"age\":\"42\"},{\"education\":\"硕士\",\"gender\":\"女\",\"name\":\"王欣\",\"term\":\"2014.4-2016.4\",\"job\":\"副总经理核心技术人员\",\"salary\":\"是\",\"age\":\"43\"}]}";
        CompanyInfo bean = (CompanyInfo) JSONObject.toBean(JSONObject.fromObject(json), CompanyInfo.class, CompanyInfo.getClassMap());
        SqlSession session = SessionFactory.getSession().openSession();
        CompanyInfoMapper mapper = session.getMapper(CompanyInfoMapper.class);
        int industryCode = mapper.selectIndustryCode(bean.getBaseinfo().getIndustry());
        int brokerCode = mapper.selectBrokerCode(bean.getBaseinfo().getBroker());
//        Object one = session.selectOne("InfoUtil.selectIndustryCode", bean.getBaseinfo().getIndustry());
//        String broker = session.selectOne("InfoUtil.selectBrokerCode", bean.getBaseinfo().getBroker());
        System.out.println();


    }
}
