package org.myrobotlab.chess;

//
//  Board.java
//  ChessApp
//
//  Created by Peter Hunter on Sun Dec 30 2001.
//  Java version copyright (c) 2001 Peter Hunter. All rights reserved.
//  This code is heavily based on Tom Kerrigan's tscp, for which he
//  owns the copyright, and is used with his permission. All rights are
//  reserved by the owners of the respective copyrights.

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

final public class Board implements Constants {
  final static int DOUBLED_PAWN_PENALTY = 10;

  final static int ISOLATED_PAWN_PENALTY = 20;

  final static int BACKWARDS_PAWN_PENALTY = 8;
  final static int PASSED_PAWN_BONUS = 20;
  final static int ROOK_SEMI_OPEN_FILE_BONUS = 10;
  final static int ROOK_OPEN_FILE_BONUS = 15;
  final static int ROOK_ON_SEVENTH_BONUS = 20;

  final static int HIST_STACK = 400;

  public int side = LIGHT;
  public int xside = DARK;
  private int castle = 15;
  private int ep = -1;
  public int fifty = 0;
  private int hply = 0;
  int history[][] = new int[64][64];
  private HistoryData histDat[] = new HistoryData[HIST_STACK];
  private int pawnRank[][] = new int[2][10];
  private int pieceMat[] = { 3100, 3100 };
  private int pawnMat[] = new int[2];

  private int color[] = { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

  private int piece[] = { 3, 1, 2, 4, 5, 2, 1, 3, 0, 0, 0, 0, 0, 0, 0, 0, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 0, 0, 0,
      0, 0, 0, 0, 0, 3, 1, 2, 4, 5, 2, 1, 3 };

  long pawnBits[] = { 0x00ff000000000000L, 0xff00L };
  long pieceBits[] = { 0xffff000000000000L, 0xffffL };
  long oldPawnBits = 0;
  long oldPieceBits = 0;
  int kingSquare[] = { 60, 4 };

  final private static char pieceChar[] = { 'P', 'N', 'B', 'R', 'Q', 'K' };

  final private static boolean slide[] = { false, false, true, true, true, false };

  final private static int offsets[] = { 0, 8, 4, 4, 8, 8 };

  final private static int offset[][] = { { 0, 0, 0, 0, 0, 0, 0, 0 }, { -21, -19, -12, -8, 8, 12, 19, 21 }, { -11, -9, 9, 11, 0, 0, 0, 0 }, { -10, -1, 1, 10, 0, 0, 0, 0 },
      { -11, -10, -9, -1, 1, 9, 10, 11 }, { -11, -10, -9, -1, 1, 9, 10, 11 } };

  final private static int mailbox[] = { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, -1, -1, 8, 9, 10, 11, 12, 13,
      14, 15, -1, -1, 16, 17, 18, 19, 20, 21, 22, 23, -1, -1, 24, 25, 26, 27, 28, 29, 30, 31, -1, -1, 32, 33, 34, 35, 36, 37, 38, 39, -1, -1, 40, 41, 42, 43, 44, 45, 46, 47, -1,
      -1, 48, 49, 50, 51, 52, 53, 54, 55, -1, -1, 56, 57, 58, 59, 60, 61, 62, 63, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 };

  private final static int mailbox64[] = { 21, 22, 23, 24, 25, 26, 27, 28, 31, 32, 33, 34, 35, 36, 37, 38, 41, 42, 43, 44, 45, 46, 47, 48, 51, 52, 53, 54, 55, 56, 57, 58, 61, 62,
      63, 64, 65, 66, 67, 68, 71, 72, 73, 74, 75, 76, 77, 78, 81, 82, 83, 84, 85, 86, 87, 88, 91, 92, 93, 94, 95, 96, 97, 98 };

  private final static int castleMask[] = { 7, 15, 15, 15, 3, 15, 15, 11, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15,
      15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 13, 15, 15, 15, 12, 15, 15, 14 };

  /* the values of the pieces */
  private final static int pieceValue[] = { 100, 300, 300, 500, 900, 0 };

  /*
   * The "pcsq" arrays are piece/square tables. They're values added to the
   * material value of the piece based on the location of the piece.
   */

  private final static int pawnPcsq[] = { 0, 0, 0, 0, 0, 0, 0, 0, 5, 10, 15, 20, 20, 15, 10, 5, 4, 8, 12, 16, 16, 12, 8, 4, 3, 6, 9, 12, 12, 9, 6, 3, 2, 4, 6, 8, 8, 6, 4, 2, 1, 2,
      3, -10, -10, 3, 2, 1, 0, 0, 0, -40, -40, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

  private final static int knightPcsq[] = { -10, -10, -10, -10, -10, -10, -10, -10, -10, 0, 0, 0, 0, 0, 0, -10, -10, 0, 5, 5, 5, 5, 0, -10, -10, 0, 5, 10, 10, 5, 0, -10, -10, 0, 5,
      10, 10, 5, 0, -10, -10, 0, 5, 5, 5, 5, 0, -10, -10, 0, 0, 0, 0, 0, 0, -10, -10, -30, -10, -10, -10, -10, -30, -10 };

  private final static int bishopPcsq[] = { -10, -10, -10, -10, -10, -10, -10, -10, -10, 0, 0, 0, 0, 0, 0, -10, -10, 0, 5, 5, 5, 5, 0, -10, -10, 0, 5, 10, 10, 5, 0, -10, -10, 0, 5,
      10, 10, 5, 0, -10, -10, 0, 5, 5, 5, 5, 0, -10, -10, 0, 0, 0, 0, 0, 0, -10, -10, -10, -20, -10, -10, -20, -10, -10 };

  private final static int kingPcsq[] = { -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40,
      -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -20, -20, -20, -20, -20, -20, -20, -20, 0, 20, 40, -20, 0, -20, 40,
      20 };

  private final static int kingEndgamePcsq[] = { 0, 10, 20, 30, 30, 20, 10, 0, 10, 20, 30, 40, 40, 30, 20, 10, 20, 30, 40, 50, 50, 40, 30, 20, 30, 40, 50, 60, 60, 50, 40, 30, 30,
      40, 50, 60, 60, 50, 40, 30, 20, 30, 40, 50, 50, 40, 30, 20, 10, 20, 30, 40, 40, 30, 20, 10, 0, 10, 20, 30, 30, 20, 10, 0 };

  /*
   * The flip array is used to calculate the piece/square values for DARK
   * pieces. The piece/square value of a LIGHT pawn is pawnPcsq[sq] and the
   * value of a DARK pawn is pawnPcsq[flip[sq]]
   */
  private final static int flip[] = { 56, 57, 58, 59, 60, 61, 62, 63, 48, 49, 50, 51, 52, 53, 54, 55, 40, 41, 42, 43, 44, 45, 46, 47, 32, 33, 34, 35, 36, 37, 38, 39, 24, 25, 26,
      27, 28, 29, 30, 31, 16, 17, 18, 19, 20, 21, 22, 23, 8, 9, 10, 11, 12, 13, 14, 15, 0, 1, 2, 3, 4, 5, 6, 7 };

  private static final int m1 = 0x55555555;

  private static final int m2 = 0x33333333;

  static int COL(int x) {
    return (x & 7);
  }

  static int ROW(int x) {
    return (x >> 3);
  }

  public Board() {
  }

  boolean attack(int sq, int s) {
    long attackSq = (1L << sq);
    if (s == LIGHT) {
      long moves = ((pawnBits[LIGHT] & 0x00fefefefefefefeL) >> 9) & attackSq;
      if (moves != 0)
        return true;
      moves = ((pawnBits[LIGHT] & 0x007f7f7f7f7f7f7fL) >> 7) & attackSq;
      if (moves != 0)
        return true;
    } else {
      long moves = ((pawnBits[DARK] & 0x00fefefefefefefeL) << 7) & attackSq;
      if (moves != 0)
        return true;
      moves = ((pawnBits[DARK] & 0x007f7f7f7f7f7f7fL) << 9) & attackSq;
      if (moves != 0)
        return true;
    }
    long pieces = pieceBits[s] ^ pawnBits[s];
    while (pieces != 0) {
      int i = getLBit(pieces);
      int p = piece[i];
      for (int j = 0; j < offsets[p]; ++j)
        for (int n = i;;) {
          n = mailbox[mailbox64[n] + offset[p][j]];
          if (n == -1)
            break;
          if (n == sq)
            return true;
          if (color[n] != EMPTY)
            break;
          if (!slide[p])
            break;
        }
      pieces &= (pieces - 1);
    }
    return false;
  }

  /*
   * inCheck() returns true if side s is in check and false otherwise. It just
   * scans the board to find side s's king and calls attack() to see if it's
   * being attacked.
   */

  int eval() {
    int score[] = new int[2]; /* each side's score */

    /* this is the first pass: set up pawnRank, and pawnMat. */
    if (oldPawnBits != (pawnBits[LIGHT] | pawnBits[DARK])) {
      for (int i = 0; i < 10; ++i) {
        pawnRank[LIGHT][i] = 0;
        pawnRank[DARK][i] = 7;
      }
      pawnMat[LIGHT] = 0;
      pawnMat[DARK] = 0;
      long pieces = pawnBits[LIGHT];
      while (pieces != 0) {
        int i = getLBit(pieces);
        pawnMat[LIGHT] += pieceValue[PAWN];
        int f = COL(i) + 1; /*
                             * add 1 because of the extra file in the array
                             */
        if (pawnRank[LIGHT][f] < ROW(i))
          pawnRank[LIGHT][f] = ROW(i);
        pieces &= (pieces - 1);
      }
      pieces = pawnBits[DARK];
      while (pieces != 0) {
        int i = getLBit(pieces);
        pawnMat[DARK] += pieceValue[PAWN];
        int f = COL(i) + 1; /*
                             * add 1 because of the extra file in the array
                             */
        if (pawnRank[DARK][f] > ROW(i))
          pawnRank[DARK][f] = ROW(i);
        pieces &= (pieces - 1);
      }
      oldPawnBits = pawnBits[LIGHT] | pawnBits[DARK];
    }
    /* this is the second pass: evaluate each piece */
    score[LIGHT] = pieceMat[LIGHT] + pawnMat[LIGHT];
    score[DARK] = pieceMat[DARK] + pawnMat[DARK];
    for (int i = 0; i < 64; ++i) {
      if (color[i] == EMPTY)
        continue;
      if (color[i] == LIGHT) {
        switch (piece[i]) {
          case PAWN:
            score[LIGHT] += evalLightPawn(i);
            break;
          case KNIGHT:
            score[LIGHT] += knightPcsq[i];
            break;
          case BISHOP:
            score[LIGHT] += bishopPcsq[i];
            break;
          case ROOK:
            if (pawnRank[LIGHT][COL(i) + 1] == 0) {
              if (pawnRank[DARK][COL(i) + 1] == 7)
                score[LIGHT] += ROOK_OPEN_FILE_BONUS;
              else
                score[LIGHT] += ROOK_SEMI_OPEN_FILE_BONUS;
            }
            if (ROW(i) == 1)
              score[LIGHT] += ROOK_ON_SEVENTH_BONUS;
            break;
          case KING:
            if (pieceMat[DARK] <= 1200)
              score[LIGHT] += kingEndgamePcsq[i];
            else
              score[LIGHT] += evalLightKing(i);
            break;
        }
      } else {
        switch (piece[i]) {
          case PAWN:
            score[DARK] += evalDarkPawn(i);
            break;
          case KNIGHT:
            score[DARK] += knightPcsq[flip[i]];
            break;
          case BISHOP:
            score[DARK] += bishopPcsq[flip[i]];
            break;
          case ROOK:
            if (pawnRank[DARK][COL(i) + 1] == 7) {
              if (pawnRank[LIGHT][COL(i) + 1] == 0)
                score[DARK] += ROOK_OPEN_FILE_BONUS;
              else
                score[DARK] += ROOK_SEMI_OPEN_FILE_BONUS;
            }
            if (ROW(i) == 6)
              score[DARK] += ROOK_ON_SEVENTH_BONUS;
            break;
          case KING:
            if (pieceMat[LIGHT] <= 1200)
              score[DARK] += kingEndgamePcsq[flip[i]];
            else
              score[DARK] += evalDarkKing(i);
            break;
        }
      }
    }

    /*
     * the score[] array is set, now return the score relative to the side to
     * move
     */
    if (side == LIGHT)
      return score[LIGHT] - score[DARK];
    return score[DARK] - score[LIGHT];
  }

  /*
   * attack() returns true if square sq is being attacked by side s and false
   * otherwise.
   */

  int evalDarkKing(int sq) {
    int r;
    int i;

    r = kingPcsq[flip[sq]];
    if (COL(sq) < 3) {
      r += evalDkp(1);
      r += evalDkp(2);
      r += evalDkp(3) / 2;
    } else if (COL(sq) > 4) {
      r += evalDkp(8);
      r += evalDkp(7);
      r += evalDkp(6) / 2;
    } else {
      for (i = COL(sq); i <= COL(sq) + 2; ++i)
        if ((pawnRank[LIGHT][i] == 0) && (pawnRank[DARK][i] == 7))
          r -= 10;
    }
    r *= pieceMat[LIGHT];
    r /= 3100;
    return r;
  }

  /*
   * gen() generates pseudo-legal moves for the current position. It scans the
   * board to find friendly pieces and then determines what squares they attack.
   * When it finds a piece/square combination, it calls genPush to put the move
   * on the "move stack."
   */

  int evalDarkPawn(int sq) {
    int r = 0; /* the value to return */
    int f = COL(sq) + 1; /* the pawn's file */

    r += pawnPcsq[flip[sq]];

    /* if there's a pawn behind this one, it's doubled */
    if (pawnRank[DARK][f] < ROW(sq))
      r -= DOUBLED_PAWN_PENALTY;

    /*
     * if there aren't any friendly pawns on either side of this one, it's
     * isolated
     */
    if ((pawnRank[DARK][f - 1] == 7) && (pawnRank[DARK][f + 1] == 7))
      r -= ISOLATED_PAWN_PENALTY;

    /* if it's not isolated, it might be backwards */
    else if ((pawnRank[DARK][f - 1] > ROW(sq)) && (pawnRank[DARK][f + 1] > ROW(sq)))
      r -= BACKWARDS_PAWN_PENALTY;

    /* add a bonus if the pawn is passed */
    if ((pawnRank[LIGHT][f - 1] <= ROW(sq)) && (pawnRank[LIGHT][f] <= ROW(sq)) && (pawnRank[LIGHT][f + 1] <= ROW(sq)))
      r += ROW(sq) * PASSED_PAWN_BONUS;

    return r;
  }

  /*
   * genCaps() is basically a copy of gen() that's modified to only generate
   * capture and promote moves. It's used by the quiescence search.
   */

  int evalDkp(int f) {
    int r = 0;

    if (pawnRank[DARK][f] == 1)
      ;
    else if (pawnRank[DARK][f] == 2)
      r -= 10;
    else if (pawnRank[DARK][f] != 7)
      r -= 20;
    else
      r -= 25;

    if (pawnRank[LIGHT][f] == 0)
      r -= 15;
    else if (pawnRank[LIGHT][f] == 2)
      r -= 10;
    else if (pawnRank[LIGHT][f] == 3)
      r -= 5;

    return r;
  }

  /*
   * genPush() puts a move on the move stack, unless it's a pawn promotion that
   * needs to be handled by genPromote(). It also assigns a score to the move
   * for alpha-beta move ordering. If the move is a capture, it uses MVV/LVA
   * (Most Valuable Victim/Least Valuable Attacker). Otherwise, it uses the
   * move's history heuristic value. Note that 1,000,000 is added to a capture
   * move's score, so it always gets ordered above a "normal" move.
   */

  int evalLightKing(int sq) {
    int r = kingPcsq[sq]; /* return value */

    /*
     * if the king is castled, use a special function to evaluate the pawns on
     * the appropriate side
     */
    if (COL(sq) < 3) {
      r += evalLkp(1);
      r += evalLkp(2);
      r += evalLkp(3)
          / 2; /*
                * problems with pawns on the c &amp; f files are not as severe
                */
    } else if (COL(sq) > 4) {
      r += evalLkp(8);
      r += evalLkp(7);
      r += evalLkp(6) / 2;
    }

    /*
     * otherwise, just assess a penalty if there are open files near the king
     */
    else {
      for (int i = COL(sq); i <= COL(sq) + 2; ++i)
        if ((pawnRank[LIGHT][i] == 0) && (pawnRank[DARK][i] == 7))
          r -= 10;
    }

    /*
     * scale the king safety value according to the opponent's material; the
     * premise is that your king safety can only be bad if the opponent has
     * enough pieces to attack you
     */
    r *= pieceMat[DARK];
    r /= 3100;

    return r;
  }

  /*
   * genPromote() is just like genPush(), only it puts 4 moves on the move
   * stack, one for each possible promotion piece
   */

  int evalLightPawn(int sq) {
    int r = 0; /* return value */
    int f = COL(sq) + 1; /* pawn's file */

    r += pawnPcsq[sq];

    /* if there's a pawn behind this one, it's doubled */
    if (pawnRank[LIGHT][f] > ROW(sq))
      r -= DOUBLED_PAWN_PENALTY;

    /*
     * if there aren't any friendly pawns on either side of this one, it's
     * isolated
     */
    if ((pawnRank[LIGHT][f - 1] == 0) && (pawnRank[LIGHT][f + 1] == 0))
      r -= ISOLATED_PAWN_PENALTY;

    /* if it's not isolated, it might be backwards */
    else if ((pawnRank[LIGHT][f - 1] < ROW(sq)) && (pawnRank[LIGHT][f + 1] < ROW(sq)))
      r -= BACKWARDS_PAWN_PENALTY;

    /* add a bonus if the pawn is passed */
    if ((pawnRank[DARK][f - 1] >= ROW(sq)) && (pawnRank[DARK][f] >= ROW(sq)) && (pawnRank[DARK][f + 1] >= ROW(sq)))
      r += (7 - ROW(sq)) * PASSED_PAWN_BONUS;

    return r;
  }

  /*
   * makemove() makes a move. If the move is illegal, it undoes whatever it did
   * and returns false. Otherwise, it returns true.
   */

  int evalLkp(int f) {
    int r = 0;

    if (pawnRank[LIGHT][f] == 6)
      ; /* pawn hasn't moved */
    else if (pawnRank[LIGHT][f] == 5)
      r -= 10; /* pawn moved one square */
    else if (pawnRank[LIGHT][f] != 0)
      r -= 20; /* pawn moved more than one square */
    else
      r -= 25; /* no pawn on this file */

    if (pawnRank[DARK][f] == 7)
      r -= 15; /* no enemy pawn */
    else if (pawnRank[DARK][f] == 5)
      r -= 10; /* enemy pawn on the 3rd rank */
    else if (pawnRank[DARK][f] == 4)
      r -= 5; /* enemy pawn on the 4th rank */

    return r;
  }

  /* takeBack() is very similar to makeMove(), only backwards :) */

  public List<HMove> gen() {
    List<HMove> ret = new ArrayList<HMove>();

    long emptySlots = ~(pieceBits[LIGHT] | pieceBits[DARK]);
    if (side == LIGHT) {
      long moves = (pawnBits[LIGHT] >> 8) & emptySlots;
      long keep = moves;
      while (moves != 0) {
        int theMove = getLBit(moves);
        genPush(ret, theMove + 8, theMove, 16);
        moves &= (moves - 1);
      }
      moves = ((keep & 0x0000ff0000000000L) >> 8) & emptySlots;
      while (moves != 0) {
        int theMove = getLBit(moves);
        genPush(ret, theMove + 16, theMove, 24);
        moves &= (moves - 1);
      }
      moves = ((pawnBits[LIGHT] & 0x00fefefefefefefeL) >> 9) & pieceBits[DARK];
      while (moves != 0) {
        int theMove = getLBit(moves);
        genPush(ret, theMove + 9, theMove, 17);
        moves &= (moves - 1);
      }
      moves = ((pawnBits[LIGHT] & 0x007f7f7f7f7f7f7fL) >> 7) & pieceBits[DARK];
      while (moves != 0) {
        int theMove = getLBit(moves);
        genPush(ret, theMove + 7, theMove, 17);
        moves &= (moves - 1);
      }
    } else {
      long moves = (pawnBits[DARK] << 8) & emptySlots;
      long keep = moves;
      while (moves != 0) {
        int theMove = getLBit(moves);
        genPush(ret, theMove - 8, theMove, 16);
        moves &= (moves - 1);
      }
      moves = ((keep & 0xff0000L) << 8) & emptySlots;
      while (moves != 0) {
        int theMove = getLBit(moves);
        genPush(ret, theMove - 16, theMove, 24);
        moves &= (moves - 1);
      }
      moves = ((pawnBits[DARK] & 0x00fefefefefefefeL) << 7) & pieceBits[LIGHT];
      while (moves != 0) {
        int theMove = getLBit(moves);
        genPush(ret, theMove - 7, theMove, 17);
        moves &= (moves - 1);
      }
      moves = ((pawnBits[DARK] & 0x007f7f7f7f7f7f7fL) << 9) & pieceBits[LIGHT];
      while (moves != 0) {
        int theMove = getLBit(moves);
        genPush(ret, theMove - 9, theMove, 17);
        moves &= (moves - 1);
      }
    }
    long pieces = pieceBits[side] ^ pawnBits[side];
    while (pieces != 0) {
      int i = getLBit(pieces);
      int p = piece[i];
      for (int j = 0; j < offsets[p]; ++j)
        for (int n = i;;) {
          n = mailbox[mailbox64[n] + offset[p][j]];
          if (n == -1)
            break;
          if (color[n] != EMPTY) {
            if (color[n] == xside)
              genPush(ret, i, n, 1);
            break;
          }
          genPush(ret, i, n, 0);
          if (!slide[p])
            break;
        }
      pieces &= (pieces - 1);
    }

    /* generate castle moves */
    if (side == LIGHT) {
      if (((castle & 1) != 0) && (piece[F1] == EMPTY) && (piece[G1] == EMPTY))
        genPush(ret, E1, G1, 2);
      if (((castle & 2) != 0) && (piece[D1] == EMPTY) && (piece[C1] == EMPTY) && (piece[B1] == EMPTY))
        genPush(ret, E1, C1, 2);
    } else {
      if (((castle & 4) != 0) && (piece[F8] == EMPTY) && (piece[G8] == EMPTY))
        genPush(ret, E8, G8, 2);
      if (((castle & 8) != 0) && (piece[D8] == EMPTY) && (piece[C8] == EMPTY) && (piece[B8] == EMPTY))
        genPush(ret, E8, C8, 2);
    }

    /* generate en passant moves */
    if (ep != -1) {
      if (side == LIGHT) {
        if (COL(ep) != 0 && color[ep + 7] == LIGHT && piece[ep + 7] == PAWN)
          genPush(ret, ep + 7, ep, 21);
        if (COL(ep) != 7 && color[ep + 9] == LIGHT && piece[ep + 9] == PAWN)
          genPush(ret, ep + 9, ep, 21);
      } else {
        if (COL(ep) != 0 && color[ep - 9] == DARK && piece[ep - 9] == PAWN)
          genPush(ret, ep - 9, ep, 21);
        if (COL(ep) != 7 && color[ep - 7] == DARK && piece[ep - 7] == PAWN)
          genPush(ret, ep - 7, ep, 21);
      }
    }
    return ret;
  }

  List<HMove> genCaps() {
    List<HMove> ret = new ArrayList<HMove>();

    if (side == LIGHT) {
      long moves = ((pawnBits[LIGHT] & 0x00fefefefefefefeL) >> 9) & pieceBits[DARK];
      while (moves != 0) {
        int theMove = getLBit(moves);
        genPush(ret, theMove + 9, theMove, 17);
        moves &= (moves - 1);
      }
      moves = ((pawnBits[LIGHT] & 0x007f7f7f7f7f7f7fL) >> 7) & pieceBits[DARK];
      while (moves != 0) {
        int theMove = getLBit(moves);
        genPush(ret, theMove + 7, theMove, 17);
        moves &= (moves - 1);
      }
    } else {
      long moves = ((pawnBits[DARK] & 0x00fefefefefefefeL) << 7) & pieceBits[LIGHT];
      while (moves != 0) {
        int theMove = getLBit(moves);
        genPush(ret, theMove - 7, theMove, 17);
        moves &= (moves - 1);
      }
      moves = ((pawnBits[DARK] & 0x007f7f7f7f7f7f7fL) << 9) & pieceBits[LIGHT];
      while (moves != 0) {
        int theMove = getLBit(moves);
        genPush(ret, theMove - 9, theMove, 17);
        moves &= (moves - 1);
      }
    }
    long pieces = pieceBits[side] ^ pawnBits[side];
    while (pieces != 0) {
      int p = getLBit(pieces);
      for (int j = 0; j < offsets[piece[p]]; ++j)
        for (int n = p;;) {
          n = mailbox[mailbox64[n] + offset[piece[p]][j]];
          if (n == -1)
            break;
          if (color[n] != EMPTY) {
            if (color[n] == xside)
              genPush(ret, p, n, 1);
            break;
          }
          if (!slide[piece[p]])
            break;
        }
      pieces &= (pieces - 1);
    }
    if (ep != -1) {
      if (side == LIGHT) {
        if (COL(ep) != 0 && color[ep + 7] == LIGHT && piece[ep + 7] == PAWN)
          genPush(ret, ep + 7, ep, 21);
        if (COL(ep) != 7 && color[ep + 9] == LIGHT && piece[ep + 9] == PAWN)
          genPush(ret, ep + 9, ep, 21);
      } else {
        if (COL(ep) != 0 && color[ep - 9] == DARK && piece[ep - 9] == PAWN)
          genPush(ret, ep - 9, ep, 21);
        if (COL(ep) != 7 && color[ep - 7] == DARK && piece[ep - 7] == PAWN)
          genPush(ret, ep - 7, ep, 21);
      }
    }
    return ret;
  }

  /*
   * reps() returns the number of times that the current position has been
   * repeated. Thanks to John Stanback for this clever algorithm.
   */

  void genPromote(Collection<HMove> ret, int from, int to, int bits) {
    for (char i = KNIGHT; i <= QUEEN; ++i) {
      HMove g = new HMove(from, to, i, (bits | 32), 'P');
      g.setScore(1000000 + (i * 10));
      ret.add(g);
    }
  }

  void genPush(Collection<HMove> ret, int from, int to, int bits) {
    if ((bits & 16) != 0) {
      if (side == LIGHT) {
        if (to <= H8) {
          genPromote(ret, from, to, bits);
          return;
        }
      } else {
        if (to >= A1) {
          genPromote(ret, from, to, bits);
          return;
        }
      }
    }

    HMove g = new HMove(from, to, 0, bits, pieceChar[piece[from]]);

    if (color[to] != EMPTY)
      g.setScore(1000000 + (piece[to] * 10) - piece[from]);
    else
      g.setScore(history[from][to]);
    ret.add(g);
  }

  public int getColor(int i) {
    return color[i];
  }

  public int getColor(int i, int j) {
    return color[(i << 3) + j];
  }

  private int getLBit(long y) {
    int x, shift;
    if ((y & 0xffffffffL) == 0) {
      x = (int) (y >> 32);
      shift = 32;
    } else {
      x = (int) y;
      shift = 0;
    }
    x = ~(x | -x);
    int a = x - ((x >> 1) & m1);
    int c = (a & m2) + ((a >> 2) & m2);
    c = (c & 0x0f0f0f0f) + ((c >> 4) & 0x0f0f0f0f);
    c = (c & 0xffff) + (c >> 16);
    c = (c & 0xff) + (c >> 8);
    return c + shift;
  }

  /* evalLkp(f) evaluates the Light King Pawn on file f */

  public int getPiece(int i) {
    return piece[i];
  }

  public int getPiece(int i, int j) {
    return piece[(i << 3) + j];
  }

  public boolean inCheck(int s) {
    return attack(kingSquare[s], s ^ 1);
  }

  public boolean isWhiteToMove() {
    return (side == LIGHT);
  }

  public boolean makeMove(HMove m) {
    long oldBits[] = { pieceBits[LIGHT], pieceBits[DARK] };

    int from, to;
    /*
     * test to see if a castle move is legal and move the rook (the king is
     * moved with the usual move code later)
     */
    if ((m.bits & 2) != 0) {

      if (inCheck(side))
        return false;
      switch (m.getTo()) {
        case 62:
          if (color[F1] != EMPTY || color[G1] != EMPTY || attack(F1, xside) || attack(G1, xside))
            return false;
          from = H1;
          to = F1;
          break;
        case 58:
          if (color[B1] != EMPTY || color[C1] != EMPTY || color[D1] != EMPTY || attack(C1, xside) || attack(D1, xside))
            return false;
          from = A1;
          to = D1;
          break;
        case 6:
          if (color[F8] != EMPTY || color[G8] != EMPTY || attack(F8, xside) || attack(G8, xside))
            return false;
          from = H8;
          to = F8;
          break;
        case 2:
          if (color[B8] != EMPTY || color[C8] != EMPTY || color[D8] != EMPTY || attack(C8, xside) || attack(D8, xside))
            return false;
          from = A8;
          to = D8;
          break;
        default: /* shouldn't get here */
          from = -1;
          to = -1;
          break;
      }
      color[to] = color[from];
      piece[to] = piece[from];
      color[from] = EMPTY;
      piece[from] = EMPTY;
      pieceBits[side] ^= (1L << from) | (1L << to);
    }

    /* back up information so we can take the move back later. */
    HistoryData h = new HistoryData();
    h.m = m;
    to = m.getTo();
    from = m.getFrom();
    h.capture = piece[to];
    h.castle = castle;
    h.ep = ep;
    h.fifty = fifty;
    h.pawnBits = new long[] { pawnBits[LIGHT], pawnBits[DARK] };
    h.pieceBits = oldBits;
    histDat[hply++] = h;

    /*
     * update the castle, en passant, and fifty-move-draw variables
     */
    castle &= castleMask[from] & castleMask[to];
    if ((m.bits & 8) != 0) {
      if (side == LIGHT)
        ep = to + 8;
      else
        ep = to - 8;
    } else
      ep = -1;
    if ((m.bits & 17) != 0)
      fifty = 0;
    else
      ++fifty;

    /* move the piece */
    int thePiece = piece[from];
    if (thePiece == KING)
      kingSquare[side] = to;
    color[to] = side;
    if ((m.bits & 32) != 0) {
      piece[to] = m.promote;
      pieceMat[side] += pieceValue[m.promote];
    } else
      piece[to] = thePiece;
    color[from] = EMPTY;
    piece[from] = EMPTY;
    long fromBits = 1L << from;
    long toBits = 1L << to;
    pieceBits[side] ^= fromBits | toBits;
    if ((m.bits & 16) != 0) {
      pawnBits[side] ^= fromBits;
      if ((m.bits & 32) == 0)
        pawnBits[side] |= toBits;
    }
    int capture = h.capture;
    if (capture != EMPTY) {
      pieceBits[xside] ^= toBits;
      if (capture == PAWN)
        pawnBits[xside] ^= toBits;
      else
        pieceMat[xside] -= pieceValue[capture];
    }

    /* erase the pawn if this is an en passant move */
    if ((m.bits & 4) != 0) {
      if (side == LIGHT) {
        color[to + 8] = EMPTY;
        piece[to + 8] = EMPTY;
        pieceBits[DARK] ^= (1L << (to + 8));
        pawnBits[DARK] ^= (1L << (to + 8));
      } else {
        color[to - 8] = EMPTY;
        piece[to - 8] = EMPTY;
        pieceBits[LIGHT] ^= (1L << (to - 8));
        pawnBits[LIGHT] ^= (1L << (to - 8));
      }
    }

    /*
     * switch sides and test for legality (if we can capture the other guy's
     * king, it's an illegal position and we need to take the move back)
     */
    side ^= 1;
    xside ^= 1;
    if (inCheck(xside)) {
      takeBack();
      return false;
    }
    return true;
  }

  public int reps() {
    int b[] = new int[64];
    int c = 0; /*
                * count of squares that are different from the current position
                */
    int r = 0; /* number of repetitions */

    /* is a repetition impossible? */
    if (fifty <= 3)
      return 0;

    /* loop through the reversible moves */
    for (int i = hply - 1; i >= hply - fifty - 1; --i) {
      if (++b[histDat[i].m.getFrom()] == 0)
        --c;
      else
        ++c;
      if (--b[histDat[i].m.getTo()] == 0)
        --c;
      else
        ++c;
      if (c == 0)
        ++r;
    }

    return r;
  }

  public void takeBack() {
    side ^= 1;
    xside ^= 1;
    HistoryData h = histDat[--hply];
    pawnBits = h.pawnBits;
    pieceBits = h.pieceBits;
    HMove m = h.m;
    castle = h.castle;
    ep = h.ep;
    fifty = h.fifty;
    int from = m.getFrom();
    int to = m.getTo();
    color[from] = side;
    if ((m.bits & 32) != 0) {
      piece[from] = PAWN;
      pieceMat[side] -= pieceValue[h.m.promote];
    } else {
      int thePiece = piece[to];
      if (thePiece == KING)
        kingSquare[side] = from;
      piece[from] = thePiece;
    }
    if (h.capture == EMPTY) {
      color[to] = EMPTY;
      piece[to] = EMPTY;
    } else {
      color[to] = xside;
      piece[to] = h.capture;
      if (h.capture != PAWN)
        pieceMat[xside] += pieceValue[h.capture];
    }
    if ((m.bits & 2) != 0) {
      int cfrom, cto;

      switch (to) {
        case 62:
          cfrom = F1;
          cto = H1;
          break;
        case 58:
          cfrom = D1;
          cto = A1;
          break;
        case 6:
          cfrom = F8;
          cto = H8;
          break;
        case 2:
          cfrom = D8;
          cto = A8;
          break;
        default: /* shouldn't get here */
          cfrom = -1;
          cto = -1;
          break;
      }
      color[cto] = side;
      piece[cto] = ROOK;
      color[cfrom] = EMPTY;
      piece[cfrom] = EMPTY;
    }
    if ((m.bits & 4) != 0) {
      if (side == LIGHT) {
        color[to + 8] = xside;
        piece[to + 8] = PAWN;
      } else {
        color[to - 8] = xside;
        piece[to - 8] = PAWN;
      }
    }
  }

  @Override
  public String toString() {
    int i;

    StringBuffer sb = new StringBuffer("\n8 ");
    for (i = 0; i < 64; ++i) {
      switch (color[i]) {
        case EMPTY:
          sb.append(" .");
          break;
        case LIGHT:
          sb.append(" ");
          sb.append(pieceChar[piece[i]]);
          break;
        case DARK:
          sb.append(" ");
          sb.append((char) (pieceChar[piece[i]] + ('a' - 'A')));
          break;
        default:
          throw new IllegalStateException("Square not EMPTY, LIGHT or DARK: " + i);
      }
      if ((i + 1) % 8 == 0 && i != 63) {
        sb.append("\n");
        sb.append(Integer.toString(7 - ROW(i)));
        sb.append(" ");
      }
    }
    sb.append("\n\n   a b c d e f g h\n\n");
    return sb.toString();
  }
}
