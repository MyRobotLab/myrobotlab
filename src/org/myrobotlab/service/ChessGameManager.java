package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
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

	public ChessGameManager(String n) {
		super(n);
	}

	@Override
	public String getDescription() {
		return "used as a general template";
	}

	public void test() {

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
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {

			ChessGameManager template = (ChessGameManager) Runtime.start("chessgame", "ChessGameManager");
			template.test();

			Runtime.start("gui", "GUIService");

		} catch (Exception e) {
			Logging.logException(e);
		}
	}

}
