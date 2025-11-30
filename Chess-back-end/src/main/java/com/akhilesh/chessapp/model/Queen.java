package com.akhilesh.chessapp.model;

import java.util.HashMap;
import java.util.HashSet;

public class Queen extends Pieces {
    public Queen(String color, int row, int col) {
        super(color, row, col);
        this.type = "queen";
    }

    public HashSet<Pair> getMoves(Pieces[][] board) {
        HashMap<String, Object> allMoves = getPins(board);
        Pair kingPos = (Pair) allMoves.get("king");
        HashSet<Pair> pins = (HashSet<Pair>) allMoves.get("pins");
        HashSet<Pair> moves = new HashSet<>();

        int[][] directions = null;
        if(pins.contains(new Pair(this.row, this.col))) {
            if(kingPos.getX() == this.row) {
                directions = new int[][]{{0, 1}, {0, -1}};
            }
            else if(kingPos.getY() == this.col) {
                directions = new int[][]{{1, 0}, {-1, 0}};
            }
            else if(kingPos.getX() > this.row && kingPos.getY() > this.col ||
                    kingPos.getX() < this.row && kingPos.getY() < this.col) {
                directions = new int[][]{{1, 1}, {-1, -1}};
            }
            else {
                directions = new int[][]{{1, -1}, {-1, 1}};
            }
        }
        else {
            directions = new int[][]{{0, 1}, {0, -1}, {1, 0}, {-1, 0}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        }

        for(int[] d : directions) {
            int newRow = row + d[0];
            int newCol = col + d[1];

            while(newRow >= 0 && newRow < 8 && newCol >= 0 && newCol < 8) {
                if(board[newRow][newCol] == null) {
                    moves.add(new Pair(newRow, newCol));
                }
                else{
                    if(!board[newRow][newCol].color.equals(this.color)) {
                        moves.add(new Pair(newRow, newCol));
                    }
                    break;
                }
                newRow = newRow + d[0];
                newCol = newCol + d[1];
            }
        }
        return moves;
    }

    // ...
    @Override
    public Pieces copy() {
        return new Queen(this.color, this.row, this.col);
    }
// ...
}
