package server;

import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class SendMailSSL
{
    private String fromMail = "comp4017gaystuent@gmail.com"; // Spell wrong when create account
    private String fromPassword = "gaytestfail";

    // public static void main(String[] args)
    public void SendMail(String toMail, String otp)
    {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class",
                "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465");

        Session session = Session.getDefaultInstance(props,
            new javax.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(fromMail, fromPassword);  // Login gmail
                }
            });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("17200113@life.hkbu.edu.hk"));  // From
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(toMail));    // To
            message.setSubject("Java-chat One Time Password");  // Topic
            message.setText("Your OTP is " + otp);  // Content includes OTP

            Transport.send(message);
            System.out.println("Mail sent");

        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }
}