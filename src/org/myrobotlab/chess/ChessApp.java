package org.myrobotlab.chess;

//
//  ChessApp.java
//  A simple Java SwingGui applet to play chess.
//  Copyright (c) 2001 Peter Hunter. SwingGui was originally based on code
//  by David Dagon (c) 1997 David Dagon and is used with his permission.
//  The search code is heavily based on Tom Kerrigan's tscp, for which he
//  owns the copyright, and is used with his permission. All rights are
//  reserved by the owners of the respective copyrights.

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.myrobotlab.logging.LoggerFactory;
import org.op.chess.ChessBoard;
import org.slf4j.Logger;

public final class ChessApp extends JApplet implements Constants, VetoableChangeListener, PropertyChangeListener {
  final class Thinker extends Thread {
    @Override
    public void run() {
      think();
      if (searcher.isStopped())
        return;
      final HMove best = searcher.getBest();
      if (best == null) {
        System.out.println("(no legal moves)");
        computerSide = EMPTY;
        return;
      }
      board.makeMove(best);
      searcher.board.makeMove(best);
      // showStatus("Computer move: " + best.toString());
      makeMove(best);
      chessView.switchMoveMarkers(board.side == LIGHT);
      isResult();
      chessView.setMoving(false);
      guessedMove = searcher.getBestNext();
      if (guessedMove != null) {
        searcher.board.makeMove(guessedMove);
        searcher.setStopTime(Long.MAX_VALUE);
        searcher.shiftPV();
        thinkThread = new Thinker();
        thinkThread.start();
      }
    }
  }

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(ChessApp.class);
  private Board board = new Board();
  private Search searcher = new Search();
  private ChessBoard chessView;
  private int computerSide = DARK;
  private final int[] playTime = { 3000, 5000, 10000, 20000, 30000, 60000 };
  private JLabel principalVariation;
  // private int moves = 0;
  private int maxTime = 10000;
  private Thread thinkThread = null;

  private HMove guessedMove = null;

  private void computerMove() {
    searcher.stopThinking();
    try {
      if (thinkThread != null)
        thinkThread.join();
    } catch (InterruptedException ignore) {
    }
    searcher.restartThinking();
    searcher.clearPV();
    thinkThread = new Thinker();
    thinkThread.start();
    searcher.setStopTime(System.currentTimeMillis() + maxTime);
    showStatus("Thinking...");
  }

  @Override
  public void init() {
    super.init();

    // showStatus ("Please Wait; Program Loading");
    chessView = new ChessBoard();
    chessView.addPropertyChangeListener(this);
    chessView.addVetoableChangeListener(this);

    JPanel p1 = new JPanel();
    p1.setLayout(new FlowLayout(FlowLayout.LEFT));

    // add (flipBox = new Checkbox("Flip Board"));
    JButton resetButton = new JButton("New Game");
    resetButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        playNewGame();
      }
    });
    p1.add(resetButton);

    JButton switchSidesButton = new JButton("Switch Sides");
    switchSidesButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        computerSide = board.side;
        guessedMove = null;
        computerMove();
      }
    });
    p1.add(switchSidesButton);

    String[] timeStrings = { "3 seconds", "5 seconds", "10 seconds", "20 seconds", "30 seconds", "1 minute" };
    JComboBox<String> timeBox = new JComboBox<String>(timeStrings);
    timeBox.setSelectedIndex(2);
    timeBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        JComboBox<Object> cb = (JComboBox<Object>) e.getSource();
        int selection = cb.getSelectedIndex();
        setMaxTime(playTime[selection]);
      }
    });
    p1.add(timeBox);
    p1.add(new JLabel("per computer move"));
    JCheckBox showPV = new JCheckBox("Show thinking");
    showPV.setSelected(true);
    showPV.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        JCheckBox cb = (JCheckBox) e.getSource();
        if (cb.isSelected())
          principalVariation.setVisible(true);
        else
          principalVariation.setVisible(false);
      }
    });
    p1.add(showPV);

    JPanel p2 = new JPanel();
    p2.add(chessView);

    principalVariation = new JLabel();
    JPanel p3 = new JPanel();
    p3.setLayout(new GridLayout(2, 1));
    p3.add(principalVariation);
    p3.add(new JLabel("Copyright 2002 Peter Hunter. All rights reserved."));

    Container cp = getContentPane();
    cp.setLayout(new BorderLayout());
    cp.add(p1, "North");
    cp.add(p2, "Center");
    cp.add(p3, "South");
  }

  private boolean isResult() {
    Collection<HMove> validMoves = board.gen();

    Iterator<HMove> i = validMoves.iterator();
    boolean found = false;
    while (i.hasNext()) {
      if (board.makeMove((HMove) i.next())) {
        board.takeBack();
        found = true;
        break;
      }
    }
    String message = null;
    if (!found) {
      if (board.inCheck(board.side)) {
        if (board.side == LIGHT)
          message = "0 - 1 Black mates";
        else
          message = "1 - 0 White mates";
      } else
        message = "0 - 0 Stalemate";
    } else if (board.reps() == 3)
      message = "1/2 - 1/2 Draw by repetition";
    else if (board.fifty >= 100)
      message = "1/2 - 1/2 Draw by fifty move rule";
    if (message != null) {
      int choice = JOptionPane.showConfirmDialog(this, message + "\nPlay again?", "Play Again?", JOptionPane.YES_NO_OPTION);
      if (choice == JOptionPane.YES_OPTION) {
        searcher.stopThinking();
        playNewGame();
      }
      return true;
    }
    if (board.inCheck(board.side))
      showStatus("Check!");
    return false;
  }

  private void makeMove(HMove m) {
    int from = m.getFrom();
    int to = m.getTo();

    log.info("computer " + from + " " + to);
    if (m.promote != 0) {
      chessView.makeMoveWithPromote(m, m.promote, board.side != LIGHT);
    } else {
      if ((m.bits & 2) != 0) {
        if (from == E1 && to == G1)
          chessView.makeMove(new org.op.chess.Move(H1, F1));
        else if (from == E1 && to == C1)
          chessView.makeMove(new org.op.chess.Move(A1, D1));
        else if (from == E8 && to == G8)
          chessView.makeMove(new org.op.chess.Move(H8, F8));
        else if (from == E8 && to == C8)
          chessView.makeMove(new org.op.chess.Move(A8, D8));
      } else if ((m.bits & 4) != 0) {
        if (board.xside == LIGHT)
          chessView.clear(m.getToRow() + 1, m.getToCol());
        else
          chessView.clear(m.getToRow() - 1, m.getToCol());
      }

      chessView.makeMove(m);
    }
  }

  public void playNewGame() {
    computerSide = DARK;
    guessedMove = null;
    searcher = new Search();
    board = new Board();
    chessView.setupBoard();
  }

  @Override
  public void propertyChange(PropertyChangeEvent pce) {
  }

  public void setMaxTime(int millis) {
    maxTime = millis;
  }

  public void setPrincipalVariation(String s) {
    principalVariation.setText("Thinking: " + s);
  }

  @Override
  public void showStatus(String msg) {
    log.info(msg);
  }

  @Override
  public void start() {
    playNewGame();
  }

  @Override
  public void stop() {
    searcher.stopThinking();
  }

  private void think() {
    searcher.think(this);
  }

  @Override
  public void vetoableChange(PropertyChangeEvent pce) throws PropertyVetoException {
    org.op.chess.Move move = (org.op.chess.Move) pce.getNewValue();
    if (move == null)
      return;

    log.info("user move from " + move.getFrom() + " " + move.getToRow() + "," + move.getToCol());
    log.info("user move to " + move.getTo() + " " + move.getToRow() + "," + move.getToCol());
    int promote = 0;
    int to = move.getTo();
    int from = move.getFrom();
    if ((((to < 8) && (board.side == LIGHT)) || ((to > 55) && (board.side == DARK))) && (board.getPiece(from) == PAWN)) {
      promote = chessView.promotionDialog(board.side == LIGHT);
    }
    boolean found = false;
    Collection<HMove> validMoves = board.gen();
    Iterator<HMove> i = validMoves.iterator();
    HMove m = null;
    while (i.hasNext()) {
      m = (HMove) i.next();
      if (m.getFrom() == from && m.getTo() == to && m.promote == promote) {
        found = true;
        break;
      }
    }
    if (!found || !board.makeMove(m)) {
      showStatus("Illegal move");
      throw new PropertyVetoException("Illegal move", pce);
    } else {
      makeMove(m);
      chessView.switchMoveMarkers(board.side == LIGHT);

      if (isResult())
        return;

      if (board.side == computerSide) {
        if (guessedMove != null) {
          if (!m.equals(guessedMove)) {
            searcher.stopThinking();
            try {
              thinkThread.join();
            } catch (InterruptedException ignore) {
            }
            searcher.restartThinking();
            searcher.board.takeBack();
            searcher.board.makeMove(m);
            searcher.clearPV();
            thinkThread = new Thinker();
            thinkThread.start();
          }
        } else {
          searcher.clearPV();
          searcher.board.makeMove(m);
          thinkThread = new Thinker();
          thinkThread.start();
        }
        searcher.setStopTime(System.currentTimeMillis() + maxTime);
        showStatus("Thinking...");
      }
    }
  }
}