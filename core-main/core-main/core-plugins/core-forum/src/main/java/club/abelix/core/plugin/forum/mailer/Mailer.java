package club.abelix.core.plugin.forum.mailer;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Mailer {

    final ExecutorService mailerExecutor = Executors.newSingleThreadExecutor();

    Session session;
    String senderMail, password, username, host, from;

    public void setSettings(
            final @NonNull String senderMail,
            final @NonNull String username,
            final @NonNull String password,
            final @NonNull String host,
            final @NonNull String from
    ) {
        this.senderMail = senderMail;
        this.password = password;
        this.username = username;
        this.host = host;
        this.from = from;
        this.session = createSession(true);
    }

    /**
     * Отправить сообщение на почту
     *
     * @param subject - тема сообщения
     * @param content - содержимое сообщения
     * @param to      - email адрес получателя
     */
    public void sendMessage(
            final String subject,
            final @NonNull String content,
            final @NonNull String to,
            final Consumer<MessagingException> exceptionConsumer
    ) {
        Runnable messageSender = () -> {
            try {
                val message = new MimeMessage(session);

                message.setFrom(new InternetAddress(senderMail, from));
                message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
                message.setSubject(subject, StandardCharsets.UTF_8.name());
                message.setContent(content, "text/html; charset=UTF-8");

                Transport.send(message);
            } catch (final @NonNull MessagingException ex) {
                if (exceptionConsumer != null) {
                    exceptionConsumer.accept(ex);
                }

                ex.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        };

        mailerExecutor.execute(messageSender);
    }

    public void sendMessage(
            final String subject,
            final @NonNull String content,
            final @NonNull String to
    ) {
        sendMessage(subject, content, to, null);
    }

    private Session createSession(final boolean use465) {
        val properties = new Properties();

        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.connectiontimeout", "5000");
        properties.put("mail.smtp.timeout", "5000");
        properties.put("mail.smtp.ssl.trust", host);

        properties.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");

        if (use465) {
            properties.put("mail.smtp.port", "465");
            properties.put("mail.smtp.ssl.enable", "true");
        } else {
            properties.put("mail.smtp.port", "587");
            properties.put("mail.smtp.starttls.enable", "true");
            properties.put("mail.smtp.starttls.required", "true");
        }

        return Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }

}
