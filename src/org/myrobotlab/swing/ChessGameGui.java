/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.swing;

import java.awt.BorderLayout;
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

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.myrobotlab.chess.Board;
import org.myrobotlab.chess.Constants;
import org.myrobotlab.chess.HMove;
import org.myrobotlab.chess.Search;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.service.SwingGui;
import org.op.chess.ChessBoard;

public class ChessGameGui extends ServiceGui implements Constants, VetoableChangeListener, PropertyChangeListener {

  final class Thinker extends Thread {

    private ChessGameGui gui;

    public Thinker(ChessGameGui gui) {
      this.gui = gui;
    }

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
      gui.myService.send(boundServiceName, "computerMoved", best.toString());

      makeMove(best, true);
      chessView.switchMoveMarkers(board.side == LIGHT);
      isResult();
      chessView.setMoving(false);
      guessedMove = searcher.getBestNext();
      if (guessedMove != null) {
        searcher.board.makeMove(guessedMove);
        searcher.setStopTime(Long.MAX_VALUE);
        searcher.shiftPV();
        thinkThread = new Thinker(gui);
        thinkThread.start();
      }
    }
  }

  static final long serialVersionUID = 1L;

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

  public static String cleanMove(String t) {
    log.info("cleanMove " + t);

    // remove piece descriptor
    if (t.length() > 5) {
      char check = t.charAt(t.length() - 1);
      if (Character.isDigit(check)) {
        t = t.substring(1);
      } else {
        t = t.substring(0, t.length() - 1);
      }
    }

    // remove -
    if (t.contains("-")) {
      t = (t.substring(0, 2) + t.substring(3));
    }

    t = t.toLowerCase();

    log.info("cleanedMove " + t);
    return t;
  }

  public ChessGameGui(final String boundServiceName, final SwingGui myService) {
    super(boundServiceName, myService);


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

    // Container cp = getContentPane();
    display.setLayout(new BorderLayout());
    display.add(p1, "North");
    display.add(p2, "Center");
    display.add(p3, "South");

    display.setSize(800, 600);

  
  }

  @Override
  public void subscribeGui() {
    subscribe("inputMove", "inputMove"); // FIXME - out of spec - should be onMove
    subscribe("inputHMove", "inputHMove");
  }

  private void computerMove() {
    searcher.stopThinking();
    try {
      if (thinkThread != null)
        thinkThread.join();
    } catch (InterruptedException ignore) {
    }
    searcher.restartThinking();
    searcher.clearPV();
    thinkThread = new Thinker(this);
    thinkThread.start();
    searcher.setStopTime(System.currentTimeMillis() + maxTime);
    showStatus("Thinking...");
  }

  @Override
  public void unsubscribeGui() {
    unsubscribe("inputMove", "inputMove");
    unsubscribe("inputHMove", "inputHMove");
  }

  private int getPos(String s) {
    log.info("getPos " + s);
    String temp1 = "";
    temp1 += s.charAt(1);
    int num = Integer.parseInt(temp1);
    int pos = s.charAt(0) - 97 + ((8 - num) * 8);
    return pos;
  }

  public HMove inputHMove(HMove m2) {
    try {
      youGotToMoveItMoveIt(null, m2, false);
    } catch (PropertyVetoException e) {
      // TODO Auto-generated catch block
      Logging.logError(e);
    }

    return m2;
  }

  public String inputMove(String m) {
    log.info(m);
    String s = cleanMove(m);
    log.info(s);

    log.info(m + " pfrom " + getPos(s) + " pto " + getPos(s.substring(2)));

    org.op.chess.Move m2 = new org.op.chess.Move(getPos(s), getPos(s.substring(2)));
    try {
      youGotToMoveItMoveIt(null, m2, true); // last param is to publish or
      // not
    } catch (PropertyVetoException e) {
      // TODO Auto-generated catch block
      Logging.logError(e);
    }

    return s;
  }

  private boolean isResult() {
    Collection<?> validMoves = board.gen();

    Iterator<?> i = validMoves.iterator();
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
      // int choice = JOptionPane.showConfirmDialog(this, message +
      // "\nPlay again?", "Play Again?", JOptionPane.YES_NO_OPTION);

      // int choice = JOptionPane.showInternalConfirmDialog(this, message
      // + "\nPlay again?", "Play Again?", JOptionPane.YES_NO_OPTION);
      int choice = 1;
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

  private void makeMove(HMove m, boolean publishEvent) {
    int from = m.getFrom();
    int to = m.getTo();
    String s = cleanMove(m.toString());
    log.info(m + "  from " + from + "  to " + to);
    log.info(m + " pfrom " + getPos(s) + " pto " + getPos(s.substring(2)));

    // log.info((int)s.charAt(0));
    // log.info(testFrom);
    // log.info(m + " from " + testFrom + " to " + to);
    if (publishEvent) {
      myService.send(boundServiceName, "makeMove", m, "n");
      myService.send(boundServiceName, "makeHMove", m);
    }

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

  public void showStatus(String msg) {
    log.info(msg);
  }

  public void start() {
    playNewGame();
  }

  public void stop() {
    searcher.stopThinking();
  }

  private void think() {
    // searcher.think(this);
    searcher.think();
  }

  @Override
  public void vetoableChange(PropertyChangeEvent pce) throws PropertyVetoException {
    youGotToMoveItMoveIt(pce, null, true);
  }

  private void youGotToMoveItMoveIt(PropertyChangeEvent pce, org.op.chess.Move m2, boolean publishEvent) throws PropertyVetoException {
    org.op.chess.Move move = null;
    if (pce != null) {
      move = (org.op.chess.Move) pce.getNewValue();
    } else {
      move = m2;
    }

    if (move == null)
      return;

    // HMove h1 = new HMove(5,3,2,4,5);
    /*
     * Move m1 = new Move(5,8); log.info(m1); log.info("user move from " +
     * move.getFrom() + " " + move.getToRow() + "," + move.getToCol());
     * log.info("user move to " + move.getTo() + " " + move.getToRow() + "," +
     * move.getToCol());
     */

    int promote = 0;
    int to = move.getTo();
    int from = move.getFrom();
    if ((((to < 8) && (board.side == LIGHT)) || ((to > 55) && (board.side == DARK))) && (board.getPiece(from) == PAWN)) {
      promote = chessView.promotionDialog(board.side == LIGHT);
    }
    boolean found = false;
    Collection<?> validMoves = board.gen();
    Iterator<?> i = validMoves.iterator();
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

      HMove illegal = new HMove(move.getFrom(), move.getTo(), 0, 0, 'p');
      myService.send(boundServiceName, "makeMove", illegal, "i");
      myService.send(boundServiceName, "makeHMove", illegal);

      if (pce != null) {
        throw new PropertyVetoException("Illegal move", pce);
      } else {
        log.error("ILLEGAL MOVE");
      }
    } else {
      makeMove(m, publishEvent);
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
            thinkThread = new Thinker(this);
            thinkThread.start();
          }
        } else {
          searcher.clearPV();
          searcher.board.makeMove(m);
          thinkThread = new Thinker(this);
          thinkThread.start();
        }
        searcher.setStopTime(System.currentTimeMillis() + maxTime);
        showStatus("Thinking...");
      }
    }

  }

}
