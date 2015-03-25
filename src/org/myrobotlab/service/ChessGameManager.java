package org.myrobotlab.service;

import org.myrobotlab.framework.Peers;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import chess.ComputerPlayer;
import chess.Game;
import chess.HumanPlayer;
import chess.Piece;
import chess.Position;

public class ChessGameManager extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(ChessGameManager.class);

	transient WebGUI webgui;
	transient Serial serial;
	transient Speech speech;

	public static Peers getPeers(String name) {
		Peers peers = new Peers(name);

		// put peer definitions in
		peers.put("webgui", "WebGUI", "webgui");
		peers.put("serial", "Serial", "serial");
		peers.put("speech", "Speech", "speech");
		return peers;
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {

			ChessGameManager template = (ChessGameManager) Runtime.start("chessgame", "ChessGameManager");
			template.test();

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

	@Override
	public Status test() {
		Status status = Status.info("starting %s %s test", getName(), getType());

		ComputerPlayer white = new ComputerPlayer();
		Game game2 = new Game(white, new ComputerPlayer());

		Game game = new Game(new HumanPlayer(), new HumanPlayer());
		game.haveDrawOffer();
		game.getGameState();
		if (Piece.BPAWN == game.pos.getPiece(Position.getSquare(4, 4))) {

		}

		boolean res = game.processString("e4");
		res = game.processString("draw offer e5");
		res = game.processString("Nc6");
		res = game.processString("draw offer Bb5");
		res = game.processString("draw accept");
		res = game.processString("undo");
		res = game.processString("redo");
		res = game.processString("new");

		return status;
	}

}
