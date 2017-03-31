package com.neeq.crawler.mail;

import javax.mail.MessagingException;
import java.util.Date;

/**
 * Created by bj on 16/6/26.
 */
public class SendBy163Mail extends Mail {

    private SendBy163Mail() {
        super("smtp.163.com", 25);
    }

    @Override
    void initFrom() {
        from = "kaifengspider@163.com";
        passWord = "spider2016";
    }

    @Override
    void initTo() {
        String[] tos = {"kaifengspider@163.com", "znbwo@qq.com"};
        super.tos = tos;
    }

    private static class SingletonHolder {
        public final static SendBy163Mail instance = new SendBy163Mail();

    }

    public static SendBy163Mail getInstance() {
        return SingletonHolder.instance;
    }


    public static void main(String[] args) {
        try {
            SendBy163Mail.getInstance().sendMail(new Date().toString());
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }


}
