package io.github.runelynx.runicparadise;

import com.sendgrid.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class EmailTest implements CommandExecutor {
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Email from = new Email("rp@rp.com");
		String subject = "Title";
		Email to = new Email("rp@gmail.com");
		Content content = new Content("text/plain", Arrays.stream(args).collect(Collectors.joining(" ")));
		Mail mail = new Mail(from, subject, to, content);

		SendGrid sg = new SendGrid("sendgrid_token");
		Request request = new Request();
		try {
			request.setMethod(Method.POST);
			request.setEndpoint("mail/send");
			request.setBody(mail.build());
			Response response = sg.api(request);
			int statusCode = response.getStatusCode();
			String body = response.getBody();
			Map<String, String> headers = response.getHeaders();
			sender.sendMessage(String.format("%d\n%s\n%s", statusCode, body, headers.toString()));
		} catch (IOException e) {
			sender.sendMessage(e.toString());
		}

		return true;
	}
}
