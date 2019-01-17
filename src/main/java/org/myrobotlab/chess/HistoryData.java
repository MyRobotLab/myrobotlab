package org.myrobotlab.chess;

//
//  HistoryData.java
//  ChessApp
//
//  Created by Peter Hunter on Mon Dec 31 2001.
//  Copyright (c) 2001 Peter Hunter. All rights reserved.
//

final class HistoryData {
  HMove m;
  int capture;
  int castle;
  int ep;
  int fifty;
  long[] pawnBits;
  long[] pieceBits;
}
