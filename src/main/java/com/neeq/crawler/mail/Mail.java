package com.neeq.crawler.mail;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;


/**
 * Created by bj on 16/6/26.
 */
public abstract class Mail {
    String from;
    String[] tos;
    int serverPort;
    String serverUrl;
    String passWord;
    Properties properties = new Properties();

    public Mail(String serverUrl, int serverPort) {
        this.serverUrl = serverUrl;
        this.serverPort = serverPort;
    }

    void initProperties() {
        initFrom();
        initTo();
        properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.port", serverPort);
        properties.setProperty("mail.smtp.host", serverUrl);
    }

    public void sendMail(String content) throws MessagingException {
        initProperties();
        Session session = Session.getDefaultInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, passWord);
            }
        });
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        for (String to : tos) {
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
        }
        message.setText(content);
        Transport.send(message);
        System.out.println("Sent message  successfully...");
    }

    abstract void initFrom();

    abstract void initTo();

}
