package com.neeq.crawler.mail;

import javax.mail.MessagingException;
import java.util.Date;

/**
 * Created by bj on 16/6/26.
 */
public class SendByQQMail extends Mail {

    private SendByQQMail() {
        super("smtp.qq.com", 587);
    }

    @Override
    void initFrom() {
        from = "@qq.com";
        passWord = "";
    }

    @Override
    void initTo() {
        String[] tos = {"kaifengspider@163.com", "znbwo@qq.com"};
        super.tos = tos;
    }

    private static class SingletonHolder {
        public final static SendByQQMail instance = new SendByQQMail();

    }

    public static SendByQQMail getInstance() {
        return SingletonHolder.instance;
    }


    public static void main(String[] args) {
        try {
            SendByQQMail.getInstance().sendMail(new Date().toString());
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }


}
