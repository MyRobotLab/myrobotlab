package org.myrobotlab.service;

import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.SpeechSynthesis;
import org.slf4j.Logger;

import chess.ComputerPlayer;
import chess.Game;
import chess.HumanPlayer;
import chess.Piece;
import chess.Position;

public class ChessGameManager extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(ChessGameManager.class);

	transient WebGui webgui;
	transient Serial serial;
	transient SpeechSynthesis speech;

	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);

		// put peer definitions in
		peers.put("webgui", "WebGui", "webgui");
		peers.put("serial", "Serial", "serial");
		peers.put("speech", "AcapelaSpeech", "speech");
		return peers;
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {

			ChessGameManager template = (ChessGameManager) Runtime.start("chessgame", "ChessGameManager");

			Runtime.start("gui", "GUIService");

		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public ChessGameManager(String n) {
		super(n);
	}

	@Override
	public String[] getCategories() {
		return new String[] { "game" };
	}

	@Override
	public String getDescription() {
		return "used as a general template";
	}

}
