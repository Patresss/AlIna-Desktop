package com.patres.alina.server.integration.mail.send;

import com.patres.alina.server.integration.alinaintegration.AlinaIntegrationExecutor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;


public class SendMailExecutor extends AlinaIntegrationExecutor<SendMailIntegrationSettings> {

    private final JavaMailSender mailSender;

    public SendMailExecutor(final SendMailIntegrationSettings settings) {
        super(settings);
        this.mailSender = getJavaMailSender();
    }

    public Object sendEmail(SendMailFunctionRequest sendMailFunctionRequest) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(settings.getSenderEmailAddress());
        message.setTo(settings.getReceiverEmailAddress());
        message.setSubject(sendMailFunctionRequest.subject());
        message.setText(sendMailFunctionRequest.content());
        mailSender.send(message);
        return sendMailFunctionRequest;
    }

    public JavaMailSender getJavaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(settings.getHost());
        mailSender.setPort(settings.getPort());
        mailSender.setUsername(settings.getSenderEmailAddress());
        mailSender.setPassword(settings.getPassword());

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "true");

        return mailSender;
    }


}
