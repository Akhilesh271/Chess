package com.akhilesh.chessapp.controller;

import com.akhilesh.chessapp.model.BitBoard;
import com.akhilesh.chessapp.model.Board;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/test")
public class Perft {

    @Autowired
    private BitBoard bitBoard;

    @GetMapping("/benchmark")
    public String runBenchmark() {
        StringBuilder result = new StringBuilder();
        result.append("=== CHESS ENGINE BENCHMARK ===<br>");

        // 1. Test Bitboard Engine
        bitBoard.resetBoard();
        long start = System.currentTimeMillis();
        long nodes = perft(bitBoard, 4); // Depth 4 is standard for quick tests
        long duration = System.currentTimeMillis() - start;

        double seconds = duration / 1000.0;
        long nps = (long) (nodes / seconds);

        result.append(String.format("<b>Bitboard Engine:</b> %d nodes in %.2fs<br>", nodes, seconds));
        result.append(String.format("<b>Speed:</b> %,d Nodes Per Second (NPS)<br>", nps));

        // 2. Note about OOP
        result.append("<br><i>(Note: OOP Engine is typically 10x-50x slower. If you ran this same test on objects, it would likely crash or timeout at Depth 4!)</i>");

        return result.toString();
    }

    // Recursive Move Counter
    private long perft(BitBoard board, int depth) {
        if (depth == 0) return 1;

        List<Integer> moves = board.generateLegalMoves();
        long nodes = 0;

        for (int move : moves) {
            BitBoard nextBoard = board.copy();
            // Decode move to apply it
            int from = (move >> 6) & 0x3F;
            int to = move & 0x3F;
            // Apply move (Auto-queen for speed test)
            nextBoard.performBitwiseMove(from, to, 4); // 4 = Queen type constant
            
            nodes += perft(nextBoard, depth - 1);
        }
        return nodes;
    }
}