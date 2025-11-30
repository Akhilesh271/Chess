package com.akhilesh.chessapp.model;

import org.springframework.stereotype.Component;

@Component
public class PrecomputedMoveData {

    // Lookups for Leapers (King/Knight) - exact moves for every square
    public static final long[] KNIGHT_MOVES = new long[64];
    public static final long[] KING_MOVES = new long[64];

    // Lookups for Sliders - distance to edge for every square in 8 directions
    // [SquareIndex][DirectionIndex]
    public static final int[][] NUM_SQUARES_TO_EDGE = new int[64][8];

    // Direction Offsets (North, South, West, East, NW, SE, NE, SW)
    // Note: In bitboards, +8 is North, -1 is West, etc.
    public static final int[] DIRECTION_OFFSETS = {8, -8, -1, 1, 7, -7, 9, -9};

    public PrecomputedMoveData() {
        initLeaperMoves();
        initSliderData();
    }

    private void initLeaperMoves() {
        for (int square = 0; square < 64; square++) {
            // Calculate Knight Moves
            long knight = 0L;
            int[] kRows = {1, 1, -1, -1, 2, 2, -2, -2};
            int[] kCols = {2, -2, 2, -2, 1, -1, 1, -1};

            int row = square / 8;
            int col = square % 8;

            for (int k = 0; k < 8; k++) {
                int nRow = row + kRows[k];
                int nCol = col + kCols[k];
                if (isValid(nRow, nCol)) {
                    knight |= (1L << (nRow * 8 + nCol));
                }
            }
            KNIGHT_MOVES[square] = knight;

            // Calculate King Moves
            long king = 0L;
            int[] dirs = {8, -8, -1, 1, 7, -7, 9, -9}; // N, S, W, E, NW, SE, NE, SW
            for (int d : dirs) {
                // We use our helper check to ensure no wrapping around the board
                int targetIndex = square + d;
                int tRow = targetIndex / 8;
                int tCol = targetIndex % 8;
                // Basic bounds check + ensuring we didn't warp from col H to A
                if (isValid(tRow, tCol) && Math.abs(tCol - col) <= 1 && Math.abs(tRow - row) <= 1) {
                    king |= (1L << targetIndex);
                }
            }
            KING_MOVES[square] = king;
        }
    }

    private void initSliderData() {
        for (int col = 0; col < 8; col++) {
            for (int row = 0; row < 8; row++) {
                int squareIndex = row * 8 + col;

                int north = 7 - row;
                int south = row;
                int west = col;
                int east = 7 - col;

                NUM_SQUARES_TO_EDGE[squareIndex][0] = north;
                NUM_SQUARES_TO_EDGE[squareIndex][1] = south;
                NUM_SQUARES_TO_EDGE[squareIndex][2] = west;
                NUM_SQUARES_TO_EDGE[squareIndex][3] = east;

                NUM_SQUARES_TO_EDGE[squareIndex][4] = Math.min(north, west); // NW
                NUM_SQUARES_TO_EDGE[squareIndex][5] = Math.min(south, east); // SE
                NUM_SQUARES_TO_EDGE[squareIndex][6] = Math.min(north, east); // NE
                NUM_SQUARES_TO_EDGE[squareIndex][7] = Math.min(south, west); // SW
            }
        }
    }

    private boolean isValid(int r, int c) {
        return r >= 0 && r < 8 && c >= 0 && c < 8;
    }
}