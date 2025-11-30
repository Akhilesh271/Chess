package com.akhilesh.chessapp.model;

public class ChessConstants {
    public static final int PAWN = 0, KNIGHT = 1, BISHOP = 2, ROOK = 3, QUEEN = 4, KING = 5;
    public static final int WHITE = 0, BLACK = 1;

    // Board representation (A1 is 0, H8 is 63)
    public static final long FILE_A = 0x0101010101010101L;
    public static final long FILE_H = 0x8080808080808080L;
    public static final long RANK_2 = 0x000000000000FF00L;
    public static final long RANK_7 = 0x00FF000000000000L;

    public static final int CASTLE_WK = 1; // White King-side
    public static final int CASTLE_WQ = 2; // White Queen-side
    public static final int CASTLE_BK = 4; // Black King-side
    public static final int CASTLE_BQ = 8; // Black Queen-side

    // Helpful bitmasks for Castling paths (Squares that must be empty)
    public static final long W_K_CASTLE_EMPTY = 0x0000000000000060L; // F1, G1
    public static final long W_Q_CASTLE_EMPTY = 0x000000000000000EL; // B1, C1, D1
    public static final long B_K_CASTLE_EMPTY = 0x6000000000000000L; // F8, G8
    public static final long B_Q_CASTLE_EMPTY = 0x0E00000000000000L; // B8, C8, D8
}