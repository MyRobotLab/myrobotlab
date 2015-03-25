package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

/*
 * TODO :
 *     AuthenticationProvider interface ????
 *     WebServiceHandler interface ????
 */
public class SoccerGame extends Service {

	public class Player {
		int number;
		int fouls;
		String name;
		String team;
		String status;
		Arduino arduino = null;
	}

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(SoccerGame.class.getCanonicalName());

	public HashMap<String, Object> session = new HashMap<String, Object>();
	int maxPlayers = 6;
	Date gameEndTime = null;
	Date gameStartTime = null;
	// clock thread
	String team0 = "team0";

	String team1 = "team1";

	ArrayList<Player> players = new ArrayList<Player>();

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		try {
			SoccerGame template = new SoccerGame("soccergame");
			template.startService();

			GUIService gui = new GUIService("gui");
			gui.startService();

		} catch (Exception e) {
			Logging.logError(e);
		}

	}

	public SoccerGame(String n) {
		super(n);
		for (int i = 0; i < maxPlayers; ++i) {
			Player p = new Player();
			p.arduino = new Arduino("p" + i);
			p.number = i;
			p.name = "p" + i;
			p.team = (i < 3) ? team0 : team1;
			p.status = "available";
			players.add(p);
		}
	}

	@Override
	public String[] getCategories() {
		return new String[] { "game" };
	}

	@Override
	public String getDescription() {
		return "used as a general template";
	}

	// TODO - public exec (Message ? ) handler
	public void logon(String name, String password) {
		log.info("logon " + name + " password " + password);
	}

}
