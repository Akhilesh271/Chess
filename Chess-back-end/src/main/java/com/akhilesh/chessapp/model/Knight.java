package com.akhilesh.chessapp.model;

import java.util.HashMap;
import java.util.HashSet;

public class Knight extends Pieces {
    public Knight(String color, int row, int col) {
        super(color, row, col);
        this.type = "night";
    }

    public HashSet<Pair> getMoves(Pieces[][] board) {
        HashMap<String, Object> allMoves = getPins(board);
        Pair kingPos = (Pair) allMoves.get("king");
        HashSet<Pair> pins = (HashSet<Pair>) allMoves.get("pins");
        HashSet<Pair> moves = new HashSet<>();

        if(pins.contains(new Pair(this.row, this.col))) {
            return moves;
        }

        int[][] directions = {{1, 2}, {1, -2}, {-1, 2}, {-1, -2}, {2, 1}, {2, -1}, {-2, 1}, {-2, -1}};

        for(int[] d : directions) {
            int newRow = row + d[0];
            int newCol = col + d[1];

            if(newRow < 8 && newCol < 8 && newRow >= 0 && newCol >= 0 &&
                    (board[newRow][newCol] == null || !board[newRow][newCol].color.equals(this.color))) {
                moves.add(new Pair(newRow, newCol));
            }
        }
        return moves;
    }

    // ...
    @Override
    public Pieces copy() {
        return new Knight(this.color, this.row, this.col);
    }
// ...
}