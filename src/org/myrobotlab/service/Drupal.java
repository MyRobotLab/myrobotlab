package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.simpleframework.xml.Element;
import org.slf4j.Logger;

public class Drupal extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Drupal.class.getCanonicalName());

	@Element
	String botName = "Cleverbot";
	@Element
	boolean doneChatting = false;
	@Element
	public String host;
	@Element
	public String username;
	@Element
	public String password;
	public String chatResponseSearchString;
	final public String cleverbotServiceName;
	final public CleverBot cleverbot;

	HTTPClient client = new HTTPClient("client");

	HashMap<String, String> usedContexts = new HashMap<String, String>();
	boolean useGreeting = true;

	String usernameTagBegin = "class=\\\"shoutbox-user-name\\\"\\x3e";
	String usernameTagEnd = "\\x3c";
	String shoutTagBegin = "class=\\\"shoutbox-shout\\\"\\x3e\\x3cp\\x3e";
	String shoutTagEnd = "\\x3c";

	int timeoutMinutes = 4;
	boolean inTimeout = false;
	HashSet<String> timeoutWords = new HashSet<String>();

	// letsmakerobots.com
	/*
	 * String usernameTagBegin = "click to view profile\">"; String
	 * usernameTagEnd = "</a></b>"; String shoutTagBegin = ": "; String
	 * shoutTagEnd = "</span>";
	 */

	public Drupal(String n) {
		super(n);
		cleverbotServiceName = String.format("%s_cleverbot", getName());
		cleverbot = new CleverBot(cleverbotServiceName);
		timeoutWords.add("timeout");
	}

	@Override
	public String getDescription() {
		return "used as a general template";
	}

	// TODO post forum topic blog etc...
	// TODO seperate token from rest...
	// TODO - schedule
	// check shout box - look for "new" comment' - if so -> take and send to
	// chatterbox
	// TODO remove static methods...

	public void shout(String text) {
		shout(host, username, password, text);
	}

	public void shout(String host, String login, String password, String text) {
		// authenticate
		HashMap<String, String> fields = new HashMap<String, String>();

		fields.put("openid_identifier", "");
		fields.put("name", login);
		fields.put("pass", password);
		fields.put("op", "Log+in");
		fields.put("form_id", "user_login_block");
		fields.put("openid.return_to", "http%3A%2F%2F" + host + "%2Fopenid%2Fauthenticate%3Fdestination%3Dfrontpage%252Fpanel");

		client.startService();
		client.post("http://" + host + "/node?destination=frontpage%2Fpanel", fields);
		// HTTPData data = HTTPClient.post("http://" + host +
		// "/node?destination=frontpage%2Fpanel", fields);

		// go to node page to get token
		String data = new String(client.get("http://" + host + "/node"));
		// HTTPClient.get("http://" + host + "/node", data);

		// get shoutbox token
		String form_token = null;

		form_token = HTTPClient.parse(data, "<input type=\"hidden\" name=\"form_token\" id=\"edit-shoutbox-add-form-form-token\" value=\"", "\"  />");

		// post comment
		fields.clear();
		fields.put("nick", login);
		fields.put("message", text);
		fields.put("ajax", "0");
		fields.put("nextcolor", "0");
		fields.put("op", "Shout");
		fields.put("form_token", form_token);
		fields.put("form_id", "shoutbox_add_form");

		// HTTPClient.post("http://" + host + "/node", fields, data);
		client.post("http://" + host + "/node", fields);

	}

	public String getShoutBox(String host) {

		String shoutbox = new String(client.get(String.format("http://%s/shoutbox/js/view?%d", host, System.currentTimeMillis())));
		log.info(shoutbox);
		return shoutbox;

	}

	public String readLastShout(String host) {
		return null;
	}

	public static class UserShout {
		public String userName;
		public String shout;
	}

	public ArrayList<UserShout> parseShoutBox(String s) {
		ArrayList<UserShout> ret = new ArrayList<UserShout>();
		if (s == null) {
			return ret;
		}

		int pos0, pos1;
		pos0 = s.indexOf(usernameTagBegin);
		while (pos0 != -1) {
			UserShout shout = null;
			pos1 = s.indexOf(usernameTagEnd, pos0);
			if (pos1 != -1) {
				pos0 = pos0 + usernameTagBegin.length();
				shout = new UserShout();
				shout.userName = s.substring(pos0, pos1);

				pos0 = s.indexOf(shoutTagBegin, pos1);
				if (pos0 != -1) {
					pos0 = pos0 + shoutTagBegin.length();
					pos1 = s.indexOf(shoutTagEnd, pos0);
					shout.shout = s.substring(pos0, pos1);

					log.info("{}-{}", shout.userName, shout.shout);
					ret.add(shout);
				}
			}

			pos0 = s.indexOf(usernameTagBegin, pos1);

		}

		return ret;
	}

	// FIXME - NON context - when a name of someone online is addressed directly
	// - its rude to respond
	// although sometimes - random response to this would be ok

	public void startChatterBot() {
		boolean foundContext = false;

		if (!cleverbot.isRunning()) {
			cleverbot.startService();
		}

		// FIXME - way to come up with contextual based questions - e.g.
		// robotics & electronic questions
		// FIXME - silence mode - say good bye - he will say I'll be back in 20
		// min..
		// FIXME - if it "finds" context - it must not be it's own
		// FIXME - replace username with Cleverbot --- outbound --- >
		// FIXME - replace Cleverbot with username <----- inbound ----
		// FIXME - random greeting ||
		// FIXME - when someone address another with @ - then high frequency to
		// disregard - its rude answering others
		// FIXME - get everyone's username - determine direction of reuqests and
		// responses
		// FIXME - (easy) when responding (mostly) respond with @-responding to
		// ... unless its top
		while (!doneChatting) {
			// wait a while
			usedContexts.put("@ mr.turing where are you from ?", null);
			Service.sleep(3000);

			foundContext = false;
			String s = getShoutBox(host);
			ArrayList<UserShout> shouts = parseShoutBox(s);
			log.info("found {} shouts - looking for context", shouts.size());

			for (int i = 0; i < shouts.size(); ++i) {
				UserShout shoutboxEntry = shouts.get(i);
				// if I have been mentioned, but not by me and this shout is not
				// one I have responded to
				if ((shoutboxEntry.shout.indexOf(chatResponseSearchString) != -1 && !shoutboxEntry.userName.equals(username)) && !usedContexts.containsKey(shoutboxEntry.shout)) {
					if (timeoutWords.contains(shoutboxEntry.shout)) {
						shout(String.format("I am going to be quiet now for %d minutes", timeoutMinutes));
						Timer timer = new Timer("chatbot timeout timer");
						inTimeout = true;
						timer.schedule(new TimerTask() {
							@Override
							public void run() {
								inTimeout = false;
							}
						}, timeoutMinutes * 60 * 1000);

					}
					usedContexts.put(shoutboxEntry.shout, shoutboxEntry.shout);
					foundContext = true;
					String shout = shoutboxEntry.shout.toLowerCase().replace(username.toLowerCase(), botName);
					log.info("found context sending [{}] to cleverbot", shout);
					String response = cleverbot.chat(shout);
					if (response != null) {
						response = response.replace(botName, username);
						log.info("shouting [{}]", response);
						shout(response);
					} else {
						log.error("response from backend chatbot is null");
					}

				}
			}

			// no context found - response to the last shout
			if (!foundContext && shouts.size() > 1) {
				String lastShout = null;
				if (useGreeting) {
					lastShout = "Hello Cleverbot";
					useGreeting = false;
				} else {
					UserShout shoutboxEntry = shouts.get(0);
					if (shoutboxEntry.userName.equals(username)) {
						log.info("i'm the last to respond - i wont respond to myself");
						continue;
					}
					lastShout = shouts.get(0).shout;
				}
				// lastShout = shouts.get(0).shout;
				// lastShout = "good night";
				lastShout = lastShout.toLowerCase().replace(username.toLowerCase(), botName);
				log.info("could not find context - sending last shout  - [{}]", lastShout);
				String response = cleverbot.chat(lastShout);
				if (response != null) {
					log.info("shouting - [{}]", response);
					response = response.replace(botName, username);
					shout(response);
				} else {
					log.error("response from backend chatbot is null");
				}
			}

			// scan through chats - find any addressed to us - from someone else
			// - that has not be responded to
			// if found ->
			// if not grab the most recent which is not us ->

			// send target to chatterbox
			// get response
			// post response

		}

	}

	public void startService() {
		super.startService();
		client.startService();
	}

	public String getCleverBotResponse(String inMsg) {
		return null;
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		// "Hello there.");
		// String s = Drupal.getShoutBox("myrobotlab.org");

		Drupal drupal = new Drupal("myrobotlab.org");
		drupal.host = "myrobotlab.org";
		// drupal.host = "letsmakerobots.com";
		drupal.username = "mr.turing";
		drupal.password = "gooby1";
		drupal.chatResponseSearchString = "turing ";
		drupal.startChatterBot();

	}

}
