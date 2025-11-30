package com.akhilesh.chessapp.model;

import java.util.HashMap;
import java.util.HashSet;

public abstract class Pieces {
    protected String color;
    protected int row;
    protected int col;
    protected String type;
    public abstract HashSet<Pair> getMoves(Pieces[][] board);

    public Pieces(String color, int row, int col) {
        this.color = color;
        this.row = row;
        this.col = col;
    }

    public String getColor() {
        return color;
    }

    public String getType() {
        return type;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public void setCol(int col) {
        this.col = col;
    }

    public abstract Pieces copy();

    public void move(int newRow, int newCol, Pieces[][] board) {
        board[newRow][newCol] = board[row][col];
        board[row][col] = null;

        this.row = newRow;
        this.col = newCol;
    }


    public Pair getKingPosition(Pieces[][] board) {
        for(int i = 0; i < 8; i++) {
            for(int j = 0; j < 8; j++) {
                if(board[i][j] != null && board[i][j].type.equals("king") &&
                        board[i][j].color.equals(this.color)) {
                    return new Pair(i, j);
                }
            }
        }
        return null;
    }

    public HashMap<String, Object> getPins(Pieces[][] board) {
        HashMap<String, Object> coords = new HashMap<String, Object>();
        HashSet<Pair> pins = new HashSet<>();
        int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {1, -1}, {1, 1}, {-1, 0}, {-1, -1}, {-1, 1}};
        Pair kingPos = getKingPosition(board);

        if (kingPos == null) {
            coords.put("king", null);
            coords.put("pins", pins);
            return coords;
        }

        coords.put("king", kingPos);

        for(int[] d : directions) {
            int newRow = kingPos.getX() + d[0];
            int newCol = kingPos.getY() + d[1];
            int counter = 0;
            Pair ally = null;

            while(newRow >= 0 && newRow < 8 && newCol >= 0 && newCol < 8) {
                Pieces piece = board[newRow][newCol];
                if(piece != null) {
                    if(piece.color.equals(this.color)){
                        ally = new Pair(newRow, newCol);
                        counter++;
                        if(counter == 2) {
                            break;
                        }
                    }
                    else {
                        if(piece.type.equals("queen") ||
                                (piece.type.equals("rook") && (d[0] == 0 || d[1] == 0)) ||
                                (piece.type.equals("bishop") && (d[0] != 0 && d[1] != 0))) {
                            if(counter == 1) {
                                pins.add(ally);
                            }
                            break;
                        }
                    }
                }
                newRow = newRow + d[0];
                newCol = newCol + d[1];
            }
        }
        coords.put("pins", pins);
        return coords;
    }

}