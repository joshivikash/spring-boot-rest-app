package hello;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserRegistrationController {
	private Set<Long> tokens = new HashSet<Long>();

	@RequestMapping(name = "/registerUser", method = RequestMethod.POST)
	public User registerUser(@RequestParam(value = "name", defaultValue = "Vikash", required = true) String name,
			@RequestParam(value = "email", defaultValue = "vikasjos@cisco.com", required = true) String email,
			@RequestParam(value = "pincode", defaultValue = "560061", required = true) String pincode) {
		User user = new User();
		user.setUserId(String.valueOf((int) (Math.random() * 1000000)));
		user.setEmail(email);
		user.setName(name);
		user.setPincode(pincode);
		try {
			Path path = Paths.get("UserDetails.txt");
			if (!path.toFile().exists()) {
				Files.createFile(path);
			}
			List<String> lines = new ArrayList<String>();
			lines.add(user.getUserId() + "," + user.getName() + "," + user.getEmail() + "," + user.getPincode());
			Files.write(path, lines, StandardOpenOption.APPEND);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return user;
	}

	@RequestMapping("/getLoginLink")
	public synchronized void getLoginLink(@RequestParam(value = "userId", required = true) String userId) {
		// Sending the email
		long time = new Date().getTime();
		while (!tokens.add(time)) {
			try {
				Thread.sleep(1l);
				time = new Date().getTime();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		User user = null;
		try {
			user = getUser(userId);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		if(user != null) {
			Properties mailProperties = new Properties();
			mailProperties.put("mail.smtp.auth", "true");
			mailProperties.put("mail.smtp.starttls.enable", "true");
			mailProperties.put("mail.smtp.host", "smtp.gmail.com");
			mailProperties.put("mail.smtp.port", "587");

			Session session = Session.getInstance(mailProperties, new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication("", "");
				}
			});
			try {
				Message message = new MimeMessage(session);
				message.setFrom(new InternetAddress(""));
				message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(user.getEmail()));
				message.setSubject("User Login Link");
				message.setText("http://localhost:8080/getUserDetails?userId=" + userId + "&token=" + time);
				Transport.send(message);
				tokens.add(time);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@RequestMapping("/getUserDetails")
	public User getUserDetails(@RequestParam(value = "userId", required = true) String userId,
			@RequestParam(value = "token", required = true) String token) {
		User user = new User();
		try {
			if (tokens.isEmpty()) {
				return null;
			}
			boolean anyMatch = tokens.stream().anyMatch((t) -> tokens.contains(Long.parseLong(token)));
			if (!anyMatch) {
				return null;
			}
			Long gotToken = tokens.stream().filter((t) -> tokens.contains(Long.parseLong(token))).findFirst().get();
			if (new Date().getTime() - gotToken <= 60000) {
				user = getUser(userId);
			} else {
				tokens.remove(gotToken);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return user;
	}
	
	private User getUser(String userId) throws IOException {
		User user = new User();
		List<String> readAllLines = Files.readAllLines(Paths.get("UserDetails.txt"));
		readAllLines.forEach((line) -> {
			String[] userDetails = line.split(",");
			if (userId.equals(userDetails[0])) {
				user.setUserId(userDetails[0]);
				user.setName(userDetails[1]);
				user.setEmail(userDetails[2]);
				user.setPincode(userDetails[3]);
			}
		});
		return user;
	}
}
