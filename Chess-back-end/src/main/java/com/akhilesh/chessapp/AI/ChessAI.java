package com.akhilesh.chessapp.AI;

import com.akhilesh.chessapp.model.Board;
import com.akhilesh.chessapp.service.Gameplay;
import com.akhilesh.chessapp.model.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Service
public class ChessAI {
    private Evaluation eval;
    private Gameplay gameplay;
    private boolean aiIsWhite;
    private final int MAX_DEPTH = 6; // Depth 4 is safer for Java speed

    @Autowired
    public ChessAI(Gameplay gameplay, @Value("${chess.ai.is-white:false}") boolean aiIsWhite) {
        this.eval = new Evaluation();
        this.gameplay = gameplay;
        this.aiIsWhite = aiIsWhite;
    }

    public AiMove findBestMOve(Board board) {
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;

        // White wants Max value, Black wants Min value
        int bestValue = aiIsWhite ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        AiMove bestMove = null;

        HashMap<Pair, HashSet<Pair>> rawMoves = board.getAllLegalMoves(board.getBoard());
        List<Pair[]> legalMoves = new ArrayList<>();

        // Flatten the moves map into a list
        for (Map.Entry<Pair, HashSet<Pair>> entry : rawMoves.entrySet()) {
            Pair from = entry.getKey();
            for (Pair to : entry.getValue()) {
                legalMoves.add(new Pair[]{from, to});
            }
        }

        if (legalMoves.isEmpty()) return null;

        for (Pair[] move : legalMoves) {
            Pair from = move[0];
            Pair to = move[1];

            // Generate the string for this root move
            String moveString = getMoveNotation(from, to);

            Board newBoard = new Board(board);
            newBoard.move(from, to);
            newBoard.switchTurn();

            // Pass the move string to minimax to start the path
            int boardValue = minimax(newBoard, MAX_DEPTH - 1, alpha, beta, moveString);

            if (aiIsWhite) {
                // AI is White: Look for Max value
                if (boardValue > bestValue) {
                    bestValue = boardValue;
                    bestMove = new AiMove(from, to, board.getBoard()[from.getX()][from.getY()]);
                }
                alpha = Math.max(alpha, bestValue);
            } else {
                // AI is Black: Look for Min value
                if (boardValue < bestValue) {
                    bestValue = boardValue;
                    bestMove = new AiMove(from, to, board.getBoard()[from.getX()][from.getY()]);
                }
                beta = Math.min(beta, bestValue);
            }
        }
        return bestMove;
    }

    private int minimax(Board board, int depth, int alpha, int beta, String path) {
        // --- PRINT STATEMENT ---
        // Prints the depth and the full sequence of moves being considered
        System.out.println("Depth: " + depth + " | Path: " + path);
        // -----------------------

        if (depth == 0 || gameOver(board)) {
            // Returns absolute score (Positive for White, Negative for Black)
            return eval.eval(board, true);
        }

        HashMap<Pair, HashSet<Pair>> rawMoves = board.getAllLegalMoves(board.getBoard());
        List<Pair[]> legalMoves = new ArrayList<>();
        for (Map.Entry<Pair, HashSet<Pair>> entry : rawMoves.entrySet()) {
            Pair from = entry.getKey();
            for (Pair to : entry.getValue()) {
                legalMoves.add(new Pair[]{from, to});
            }
        }

        boolean isWhiteTurn = board.getTurn().equals("white");

        if (isWhiteTurn) {
            // White Turn: MAXIMIZE score
            int maxEval = Integer.MIN_VALUE;
            for (Pair[] move : legalMoves) {
                Board newBoard = new Board(board);
                newBoard.move(move[0], move[1]);
                newBoard.switchTurn();

                String nextMove = getMoveNotation(move[0], move[1]);

                // Append nextMove to the path
                int currentEval = minimax(newBoard, depth - 1, alpha, beta, path + " " + nextMove);

                maxEval = Math.max(maxEval, currentEval);
                alpha = Math.max(alpha, currentEval);

                if (beta <= alpha) break;
            }
            return maxEval;
        } else {
            // Black Turn: MINIMIZE score
            int minEval = Integer.MAX_VALUE;
            for (Pair[] move : legalMoves) {
                Board newBoard = new Board(board);
                newBoard.move(move[0], move[1]);
                newBoard.switchTurn();

                String nextMove = getMoveNotation(move[0], move[1]);

                // Append nextMove to the path
                int currentEval = minimax(newBoard, depth - 1, alpha, beta, path + " " + nextMove);

                minEval = Math.min(minEval, currentEval);
                beta = Math.min(beta, currentEval);

                if (beta <= alpha) break;
            }
            return minEval;
        }
    }

    private String getMoveNotation(Pair from, Pair to) {
        String start = "" + (char)('a' + from.getY()) + (from.getX() + 1);
        String end = "" + (char)('a' + to.getY()) + (to.getX() + 1);
        return start + end;
    }


    public boolean gameOver(Board board) {
        HashMap<Pair, HashSet<Pair>> moves = board.getAllLegalMoves(board.getBoard());
        return moves.isEmpty();
    }
}