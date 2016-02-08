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
	
	/**
	 * Sends an email to the user, depending on the given parameters
	 * 
	 * @param i_From
	 * @param i_Recipient
	 * @param i_Subject
	 * @param i_ResourceName
	 * @param i_HostName
	 * @throws MessagingException if something in the process is not verified, or goes wrong
	 */
	public static void SendEmail(String i_From, String i_Recipient, String i_Subject, String i_ResourceName, String i_HostName) 
			throws MessagingException {

		final String username = "lab02crawler";
		final String password = "CreepyCrawlers";

		String to = i_Recipient;
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
		message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
		message.setSubject("Crawler result for " + i_HostName);
		BodyPart messageBodyPart = new MimeBodyPart();
		messageBodyPart.setText("Attached crawler run results");
		Multipart multipart = new MimeMultipart();
		multipart.addBodyPart(messageBodyPart);
		messageBodyPart = new MimeBodyPart();
		DataSource source = new FileDataSource(i_ResourceName);
		messageBodyPart.setDataHandler(new DataHandler(source));
		messageBodyPart.setFileName(i_HostName + ".html");
		multipart.addBodyPart(messageBodyPart);
		message.setContent(multipart);
		Transport.send(message);
	}
}
