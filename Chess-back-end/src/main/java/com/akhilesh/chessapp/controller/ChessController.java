package com.akhilesh.chessapp.controller;

import com.akhilesh.chessapp.AI.AiMove;
import com.akhilesh.chessapp.AI.BitboardAI;
import com.akhilesh.chessapp.AI.ChessAI;
import com.akhilesh.chessapp.model.BitBoard;
import com.akhilesh.chessapp.model.Board;
import com.akhilesh.chessapp.model.Pair;
import com.akhilesh.chessapp.request.MoveRequest;
import com.akhilesh.chessapp.service.Gameplay;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/chess")
@CrossOrigin(origins = "*") // Allow frontend to talk to backend
public class ChessController {

    @Autowired
    private Gameplay gameplay;

    @Autowired
    private ChessAI legacyAI;

    @Autowired
    private BitboardAI bitboardAI;

    // Inject the raw BitBoard model for the new AI
    @Autowired
    private BitBoard bitBoardModel;

    // --- 1. GAME MANAGEMENT ---

    @PostMapping("/start")
    public String startGame() {
        gameplay.startNewGame();
        return "Game started";
    }

    @GetMapping("/board")
    public ResponseEntity<?> getBoardState() {
        // Safe for both engines
        return ResponseEntity.ok(gameplay.getEngine().getPieceMap());
    }

    @GetMapping("/fen")
    public String getFEN() {
        return gameplay.getEngine().getFen();
    }

    // --- 2. MOVE EXECUTION ---

    @PostMapping("/move")
    public ResponseEntity<?> makeMove(@RequestBody MoveRequest moveRequest,
                                      @RequestParam(required = false) String promotion) {
        Pair from = moveRequest.getFrom();
        Pair to = moveRequest.getTo();

        // Optional: Set promotion on old board if it exists (for safety)
        if (gameplay.getBoard() != null && promotion != null) {
            gameplay.getBoard().setPendingPromotion(promotion);
        }

        boolean moved = gameplay.makeMove(from, to, promotion);

        if (moved) {
            // FIX: Always ask the Engine for the map, never the Board
            return ResponseEntity.ok(gameplay.getEngine().getPieceMap());
        } else {
            return ResponseEntity.badRequest().body("Invalid move");
        }
    }

    // Fallback GET endpoint for simple testing
    @GetMapping("/move")
    public ResponseEntity<?> makeMove(@RequestParam int fromX,
                                      @RequestParam int fromY,
                                      @RequestParam int toX,
                                      @RequestParam int toY,
                                      @RequestParam(required = false) String promotion) {
        Pair from = new Pair(fromX, fromY);
        Pair to = new Pair(toX, toY);

        if (gameplay.getBoard() != null && promotion != null) {
            gameplay.getBoard().setPendingPromotion(promotion);
        }

        boolean moved = gameplay.makeMove(from, to, promotion);

        if (moved) {
            return ResponseEntity.ok(gameplay.getEngine().getPieceMap());
        } else {
            return ResponseEntity.badRequest().body("Invalid move");
        }
    }

    // --- 3. AI LOGIC ---

    @GetMapping("/aiMove")
    public ResponseEntity<?> makeAiMove() {
        // Ensure game state is current
        gameplay.game();

        AiMove aiMove = null;
        Board legacyBoard = gameplay.getBoard(); // Will be null if using BitboardEngine

        if (legacyBoard == null) {
            // --- CASE A: BITBOARD ENGINE ---
            System.out.println("AI: Using High-Performance Bitboard AI");
            aiMove = bitboardAI.findBestMove(bitBoardModel);
        } else {
            // --- CASE B: OOP ENGINE ---
            System.out.println("AI: Using Legacy OOP AI");
            aiMove = legacyAI.findBestMOve(legacyBoard);
        }

        // Check if AI found a move (or if it's checkmate/stalemate)
        if (aiMove == null) {
            String status = gameplay.getStatus().toString();
            System.out.println("AI Move: Game Over - " + status);
            return ResponseEntity.ok().body("Game over. Status: " + status);
        }

        System.out.println("AI Selected: " + aiMove);

        // Execute the move found by AI
        // Note: AI usually auto-queens, so we pass null or "q" as default
        boolean moved = gameplay.makeMove(aiMove.getFrom(), aiMove.getTo(), "q");

        if (moved) {
            return ResponseEntity.ok(gameplay.getEngine().getPieceMap());
        } else {
            return ResponseEntity.internalServerError().body("AI calculated an invalid move.");
        }
    }

    // --- 4. LEGAL MOVES ---

    @GetMapping("/legalMoves")
    public Map<Pair, HashSet<Pair>> getLegalMoves() {
        gameplay.game();
        // FIX: Ask Engine directly
        return gameplay.getEngine().getLegalMoves();
    }

    @GetMapping("/legalMoves/{row}/{col}")
    public ResponseEntity<Set<Pair>> getLegalMovesForPiece(@PathVariable int row, @PathVariable int col) {
        gameplay.game();
        // FIX: Ask Engine directly
        Set<Pair> legalMoves = gameplay.getEngine().getLegalMovesForSquare(new Pair(row, col));

        if (legalMoves == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(legalMoves);
    }
}