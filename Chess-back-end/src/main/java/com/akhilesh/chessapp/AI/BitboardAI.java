package com.akhilesh.chessapp.AI;

import com.akhilesh.chessapp.model.BitBoard;
import com.akhilesh.chessapp.model.ChessConstants;
import com.akhilesh.chessapp.model.Pair;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BitboardAI {

    private static final int MAX_DEPTH = 4; // Start small. 4 is usually a good balance for Java.
    private static final int CHECKMATE_SCORE = 100000;

    // Piece Values (P, N, B, R, Q, K)
    private static final int[] PIECE_VALUES = {100, 320, 330, 500, 900, 20000};

    // Simplified Piece-Square Tables (Middle game)
    // Positive = Good square. Tables are for White. Flip for Black.
    private static final int[] PAWN_TABLE = {
            0,  0,  0,  0,  0,  0,  0,  0,
            50, 50, 50, 50, 50, 50, 50, 50,
            10, 10, 20, 30, 30, 20, 10, 10,
            5,  5, 10, 25, 25, 10,  5,  5,
            0,  0,  0, 20, 20,  0,  0,  0,
            5, -5,-10,  0,  0,-10, -5,  5,
            5, 10, 10,-20,-20, 10, 10,  5,
            0,  0,  0,  0,  0,  0,  0,  0
    };

    private static final int[] KNIGHT_TABLE = {
            -50,-40,-30,-30,-30,-30,-40,-50,
            -40,-20,  0,  0,  0,  0,-20,-40,
            -30,  0, 10, 15, 15, 10,  0,-30,
            -30,  5, 15, 20, 20, 15,  5,-30,
            -30,  0, 15, 20, 20, 15,  0,-30,
            -30,  5, 10, 15, 15, 10,  5,-30,
            -40,-20,  0,  5,  5,  0,-20,-40,
            -50,-40,-30,-30,-30,-30,-40,-50
    };

    // (You can add tables for other pieces, but Pawns/Knights are most critical for structure)

    public AiMove findBestMove(BitBoard board) {
        long startTime = System.currentTimeMillis();
        int bestMove = -1;
        int bestScore = -CHECKMATE_SCORE;

        // 1. Generate all legal moves for the current board state
        // This fixes the "undeclared 'moves'" error
        List<Integer> moves = board.generateLegalMoves();

        // 2. Iterate through each move to find the best one
        for (int move : moves) {

            // 3. Create a copy of the board to simulate the move
            // This fixes the "undeclared 'futureBoard'" error
            BitBoard futureBoard = board.copy();

            // 4. Decode the integer move into From/To squares
            int from = (move >> 6) & 0x3F;
            int to = move & 0x3F;

            // 5. Perform the move on the imaginary board
            // We default to Queen promotion for calculation purposes
            futureBoard.performBitwiseMove(from, to, ChessConstants.QUEEN);

            // 6. Call Minimax recursively
            // We negate the result because it's the opponent's turn in the next step
            int score = -minimax(futureBoard, MAX_DEPTH - 1, -CHECKMATE_SCORE, CHECKMATE_SCORE);

            // 7. Track the best score found so far
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }

        System.out.println("AI calculated " + moves.size() + " positions in " + (System.currentTimeMillis() - startTime) + "ms. Best Score: " + bestScore);

        if (bestMove != -1) {
            return convertToAiMove(bestMove, bestScore);
        }

        return null; // Checkmate or Stalemate
    }

    private int minimax(BitBoard board, int depth, int alpha, int beta) {
        if (depth == 0) {
            return evaluate(board);
        }

        List<Integer> moves = board.generateLegalMoves();
        if (moves.isEmpty()) {
            // Checkmate logic: If King is attacked, it's terrible (-Score). If not, it's Draw (0).
            // For simplicity, we return a generic low score, but real engine needs isSquareAttacked check here
            return -CHECKMATE_SCORE + depth; // Prefer delaying checkmate
        }

        int maxScore = -CHECKMATE_SCORE;

        for (int move : moves) {
            BitBoard nextBoard = board.copy();
            nextBoard.performBitwiseMove((move >> 6) & 0x3F, move & 0x3F, ChessConstants.QUEEN);

            int score = -minimax(nextBoard, depth - 1, -beta, -alpha);

            if (score > maxScore) {
                maxScore = score;
            }
            alpha = Math.max(alpha, score);
            if (alpha >= beta) {
                break; // Pruning
            }
        }
        return maxScore;
    }

    private int evaluate(BitBoard board) {
        int whiteScore = 0;
        int blackScore = 0;

        // Calculate Material & Positional Score
        for (int i = 0; i < 64; i++) {
            long mask = 1L << i;
            // A simplified evaluation loop
            for (int type = 0; type < 6; type++) {
                int value = PIECE_VALUES[type];

                // White
                if ((board.bitboards[type] & mask) != 0) {
                    whiteScore += value;
                    if (type == ChessConstants.PAWN) whiteScore += PAWN_TABLE[flip(i)]; // White uses table as-is (flipped for array index logic)
                    if (type == ChessConstants.KNIGHT) whiteScore += KNIGHT_TABLE[flip(i)];
                }

                // Black
                if ((board.bitboards[type + 6] & mask) != 0) {
                    blackScore += value;
                    if (type == ChessConstants.PAWN) blackScore += PAWN_TABLE[i]; // Black mirrors the table
                    if (type == ChessConstants.KNIGHT) blackScore += KNIGHT_TABLE[i];
                }
            }
        }

        int evaluation = whiteScore - blackScore;
        return (board.turn == ChessConstants.WHITE) ? evaluation : -evaluation;
    }

    // Helper to flip square index for table lookups (Rank 0 becomes Rank 7)
    private int flip(int i) {
        return (7 - (i / 8)) * 8 + (i % 8);
    }

    // Update the method signature to accept 'score'
    private AiMove convertToAiMove(int moveInt, int score) {
        int from = (moveInt >> 6) & 0x3F;
        int to = moveInt & 0x3F;

        // Pass the score as the 3rd parameter
        // (Assuming AiMove(Pair from, Pair to, int score))
        return new AiMove(indexToPair(from), indexToPair(to), score);
    }

    private Pair indexToPair(int index) {
        return new Pair(index / 8, index % 8);
    }
}