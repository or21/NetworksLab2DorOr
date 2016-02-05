import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class EmailService {
	public static void SendEmail(String i_From, String i_Recipient, String i_Subject, String i_ResourceName) throws MessagingException {
		String to = i_Recipient;

		final String username = "lab02crawler";
		final String password = "CreepyCrawlers";

		String host = "smtp.gmail.com";

		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", host);
		props.put("mail.smtp.port", "587");

		Session session = Session.getInstance(props, 
				new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, password);
			}
		});

		Message message = new MimeMessage(session);

		message.setFrom(new InternetAddress(i_From));
		message.setRecipients(Message.RecipientType.TO,
				InternetAddress.parse(to));
		message.setSubject("Crawler result for " + HtmlRepository.GetInstance().Host);
		BodyPart messageBodyPart = new MimeBodyPart();
		messageBodyPart.setText("Attached crawler run results");
		Multipart multipart = new MimeMultipart();
		multipart.addBodyPart(messageBodyPart);
		messageBodyPart = new MimeBodyPart();
		DataSource source = new FileDataSource(i_ResourceName);
		messageBodyPart.setDataHandler(new DataHandler(source));
		messageBodyPart.setFileName(HtmlRepository.GetInstance().Host);
		multipart.addBodyPart(messageBodyPart);
		message.setContent(multipart);
		Transport.send(message);
	}
}
