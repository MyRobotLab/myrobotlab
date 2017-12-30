package org.myrobotlab.chess;

//
//  Search.java
//  ChessApp
//
//  This search code is heavily based on Tom Kerrigan's tscp for which he
//  owns the copyright - (c) 1997 Tom Kerrigan -  and is used with his permission.
//  All rights are reserved by the owners of the respective copyrights.
//  Java version created by Peter Hunter on Sat Jan 05 2002.
//  Copyright (c) 2002 Peter Hunter. All rights reserved.
//
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

final public class Search {
  final static int MAX_PLY = 32;

  public Board board = new Board();

  private HMove pv[][] = new HMove[MAX_PLY][MAX_PLY];

  private int pvLength[] = new int[MAX_PLY];

  private boolean followPV;

  private int ply = 0;

  private int nodes = 0;

  private long stopTime = Long.MAX_VALUE;

  private boolean stop = false;

  void checkup() throws StopSearchingException {
    /*
     * is the engine's time up? if so, longjmp back to the beginning of think()
     */
    if (System.currentTimeMillis() >= stopTime || stop) {
      throw new StopSearchingException();
    }
  }

  public void clearPV() {
    for (int i = 0; i < MAX_PLY; i++)
      for (int j = 0; j < MAX_PLY; j++)
        pv[i][j] = null;
  }

  /*
   * quiesce() is a recursive minimax search function with alpha-beta cutoffs.
   * In other words, negamax. It basically only searches capture sequences and
   * allows the evaluation function to cut the search off (and set alpha). The
   * idea is to find a position where there isn't a lot going on so the static
   * evaluation function will work.
   */

  public HMove getBest() {
    return pv[0][0];
  }

  /*
   * sortPV() is called when the search function is following the PV (Principal
   * Variation). It looks through the current ply's move list to see if the PV
   * move is there. If so, it adds 10,000,000 to the move's score so it's played
   * first by the search function. If not, followPV remains FALSE and search()
   * stops calling sortPV().
   */

  public HMove getBestNext() {
    return pv[0][1];
  }

  /* checkup() is called once in a while during the search. */

  public boolean isStopped() {
    return stop;
  }

  int quiesce(int alpha, int beta) throws StopSearchingException {
    pvLength[ply] = ply;

    /* are we too deep? */
    if (ply >= MAX_PLY - 1)
      return board.eval();
    /*
     * if (hply >= HIST_STACK - 1) return board.eval(); FIXME!! see above
     */
    /* check with the evaluation function */
    int x = board.eval();
    if (x >= beta)
      return beta;
    if (x > alpha)
      alpha = x;

    List<HMove> validCaptures = board.genCaps();
    if (followPV) /* are we following the PV? */
      sortPV(validCaptures);
    Collections.sort(validCaptures);

    /* loop through the moves */
    Iterator<HMove> i = validCaptures.iterator();
    while (i.hasNext()) {
      HMove m = (HMove) i.next();
      if (!board.makeMove(m))
        continue;
      ++ply;
      ++nodes;

      /* do some housekeeping every 1024 nodes */
      if ((nodes & 1023) == 0)
        checkup();

      x = -quiesce(-beta, -alpha);
      board.takeBack();
      --ply;

      if (x > alpha) {
        if (x >= beta)
          return beta;
        alpha = x;

        /* update the PV */
        pv[ply][ply] = m;
        for (int j = ply + 1; j < pvLength[ply + 1]; ++j)
          pv[ply][j] = pv[ply + 1][j];
        pvLength[ply] = pvLength[ply + 1];
      }
    }
    return alpha;
  }

  public void restartThinking() {
    stop = false;
  }

  /** search() does just that, in negascout fashion */

  int search(int alpha, int beta, int depth) throws StopSearchingException {
    /*
     * we're as deep as we want to be; call quiesce() to get a reasonable score
     * and return it.
     */
    if (depth == 0)
      return quiesce(alpha, beta);

    if (beta - alpha != 1) {
      pvLength[ply] = ply;
    }

    /*
     * if this isn't the root of the search tree (where we have to pick a move
     * and can't simply return 0) then check to see if the position is a repeat.
     * if so, we can assume that this line is a draw and return 0.
     */
    if ((ply > 0) && (board.reps() > 0))
      return 0;

    /* are we too deep? */
    if (ply >= MAX_PLY - 1)
      return board.eval();
    /*
     * if (hply >= HIST_STACK - 1) return board.eval(); FIXME!!! We could in
     * principle overflow the move history stack.
     */
    /* are we in check? if so, we want to search deeper */
    boolean check = board.inCheck(board.side);
    if (check)
      ++depth;
    List<HMove> validMoves = board.gen();
    if (followPV) /* are we following the PV? */
      sortPV(validMoves);
    Collections.sort(validMoves);

    /* loop through the moves */
    boolean foundMove = false;
    Iterator<HMove> i = validMoves.iterator();
    int a = alpha;
    int b = beta;
    boolean first = true;
    while (i.hasNext()) {
      HMove m = (HMove) i.next();
      if (!board.makeMove(m))
        continue;
      ++ply;
      ++nodes;
      /* do some housekeeping every 1024 nodes */
      if ((nodes & 1023) == 0)
        checkup();

      foundMove = true;

      int x = -search(-b, -a, depth - 1);
      boolean betterMove = false;
      if ((x > a) && (x < beta) && (!first)) {
        a = -search(-beta, -x, depth - 1);
        if (a >= x)
          betterMove = true;
      }
      board.takeBack();
      --ply;

      if (x > a) {
        a = x;
        betterMove = true;
      }
      if (betterMove) {
        /*
         * this move caused a cutoff, so increase the history value so it gets
         * ordered high next time we can search it
         */
        board.history[m.getFrom()][m.getTo()] += depth;
        if (x >= beta)
          return beta;

        /* update the PV */
        pv[ply][ply] = m;
        for (int j = ply + 1; j < pvLength[ply + 1]; ++j)
          pv[ply][j] = pv[ply + 1][j];
        pvLength[ply] = pvLength[ply + 1];
      }
      b = a + 1;
      first = false;
    }

    /* no legal moves? then we're in checkmate or stalemate */
    if (!foundMove) {
      if (check)
        return -10000 + ply;
      else
        return 0;
    }

    /* fifty move draw rule */
    if (board.fifty >= 100)
      return 0;
    return a;
  }

  public void setStopTime(long stop) {
    stopTime = stop;
  }

  public void shiftPV() {
    pvLength[0] -= 2;
    for (int i = 0; i < pvLength[0]; i++)
      pv[0][i] = pv[0][i + 2];
  }

  void sortPV(Collection<HMove> moves) {
    followPV = false;
    if (pv[0][ply] == null)
      return;
    Iterator<HMove> i = moves.iterator();
    while (i.hasNext()) {
      HMove m = (HMove) i.next();
      if (m.equals(pv[0][ply])) {
        followPV = true;
        m.score += 10000000;
        return;
      }
    }
  }

  public void stopThinking() {
    stop = true;
  }

  public void think() {
    think(null);
  }

  void think(ChessApp app) {
    stop = false;
    try {
      ply = 0;
      nodes = 0;
      for (int i = 0; i < 64; i++)
        for (int j = 0; j < 64; j++)
          board.history[i][j] = 0;
      for (int i = 3; i <= MAX_PLY; ++i) {
        followPV = true;
        int x = search(-10000, 10000, i);
        System.out.println(i + " " + nodes + " " + x);
        StringBuffer sb = new StringBuffer("[");
        sb.append(x);
        sb.append("]");
        for (int j = 0; j < pvLength[0]; ++j) {
          sb.append(" ");
          sb.append(pv[0][j].toString());
        }
        // app.setPrincipalVariation(sb.toString());
        // log.info(sb.toString());
        if (x > 9000 || x < -9000)
          break;
      }
    } catch (StopSearchingException e) {
      /* make sure to take back the line we were searching */
      while (ply != 0) {
        board.takeBack();
        --ply;
      }
    }
    System.out.println("Nodes searched: " + nodes);
    return;
  }
}
