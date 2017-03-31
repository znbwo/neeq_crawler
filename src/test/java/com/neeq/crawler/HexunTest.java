package com.neeq.crawler;

import com.neeq.crawler.tool.HttpManager;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jsoup.nodes.Document;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by kidbei on 16/7/1.
 */
public class HexunTest {
    static final String loginUrl = "https://reg.hexun.com/rest/ajaxlogin.aspx?callback=jQuery18207462505238941424_1467340674858&username=qqvtqen8pd&password=hexun123&gourl=http%253A%2F%2Fyanbao.stock.hexun.com%2Flistnews1_1.shtml&fromhost=yanbao.stock.hexun.com&hiddenReferrer=http%253A%2F%2Fyanbao.stock.hexun.com%2Flistnews1_1.shtml&loginstate=1&act=login&_=1467340685425";

    public static void main(String[] args) throws IOException {
        CloseableHttpClient client = HttpManager.getClient();

        HttpGet get = new HttpGet(loginUrl);
        HttpManager.config(get);

        CloseableHttpResponse response = client.execute(get);
        String html = IOUtils.toString(response.getEntity().getContent());

        response.close();


        String itemUrl = "http://yanbao.stock.hexun.com/dzgg714929.shtml";
        HttpGet itemGet = new HttpGet(itemUrl);
        HttpManager.config(itemGet);

        response = client.execute(itemGet);
        html = IOUtils.toString(response.getEntity().getContent());
        response.close();

        Document doc = org.jsoup.Jsoup.parse(html);
        String fileUrl = doc.getElementsByClass("check-pdf").first().attr("href");
//        System.out.println(doc.getElementsByClass("check-pdf").first());


        String cookie = "LoginStateCookie=0; SnapCookie=0; com.vfsso.cas.token=; com.hexun.staus.ext.login=; hxck_sq_common=SnapCookie=LpWZVrF73H%2bB648Hyu9V0hOSU8dHffNSW8mAS88qGCNRbWP7hKO6I6m4JaLo7x5uQ3yWWAhGAGOg3m3Uy%2fCh3R68XanUJRRCLGN%2fbUF8I99R3jKymrrLPw%3d%3d&LoginStateCookie=LpWZVrF73H8nrgGfuSue3A%3d%3d; HexunTrack=SID=20160701103253013ad4da636a3164e45af5df8bf7a7a705a&CITY=11&TOWN=0; userToken=28400628%7c0000%7c0%2cNwzGlJ6pxHQhfbFvgafv3MgkiqOomr4mU4LaUBYujiMBmEgiGGELZpc4DGKFgb8RXn%2beVIjJ8o1Q9aaENspe39LUpI0E6m1lwDZKRvYDhhar6%2fPoET%2byY2uV0MmnKM7O3c9BhmCi05r03nKlLEIW1Gdy7PxwbiPIAu8hdOrXhSu4GKLeN%2bwvogqjT%2fv4OmoXr4gWApu3y6Fjj44hlqAD0w%3d%3d; hxck_fsd_lcksso=765785F554A89C318B38E74C55DF6760E0F4A72B2FC564A09561571B12D880561E4C9D8CC43016748FE41C1EA74F2DA3871AB6014DA4067A33301384EA3E7C7324EB9E948170C6C89156E3AD4F6DD279901B7D5595D0B022891EFB5E558963D84CC3A78C47EDB48E";
        HttpGet dGet = new HttpGet(fileUrl);
//        dGet.setHeader("Cookie",cookie);
//        dGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36");
        dGet.setHeader("Referer","http://yanbao.stock.hexun.com/dzgg714929.shtml");
//        dGet.setHeader("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
//        dGet.setHeader("Host","yanbao.stock.hexun.com");
        response = client.execute(dGet);

        System.out.println(response.getStatusLine());

//        System.out.println(IOUtils.toString(response.getEntity().getContent()));

        IOUtils.write(IOUtils.toByteArray(response.getEntity().getContent()),new FileOutputStream("/Users/kidbei/Downloads/test.pdf"));
    }


}
