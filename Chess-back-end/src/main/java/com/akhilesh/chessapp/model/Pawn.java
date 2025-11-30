package com.akhilesh.chessapp.model;

import java.util.HashMap;
import java.util.HashSet;

public class Pawn extends Pieces {
    private boolean lEnPassant = true;
    private boolean rEnPassant = true;

    public Pawn(String color, int row, int col) {
        super(color, row, col);
        this.type = "pawn";
    }

    public HashSet<Pair> getMoves(Pieces[][] board) {
//        HashMap<String, Object> allMoves = getPins(board);
//        Pair kingPos = (Pair) allMoves.get("king");
//        HashSet<Pair> pins = (HashSet<Pair>) allMoves.get("pins");

//
//        if(pins.contains(new Pair(this.row, this.col))) {
//            return moves;
//        }

        HashSet<Pair> moves = new HashSet<>();
        int directions;

        if (color.equals("white")) {
            directions = 1;
        } else {
            directions =-1;
        }

        if(row < 7 && color.equals("white") || row > 0 && color.equals("black")) {
            if(board[row+directions][col] == null) {
                moves.add(new Pair(row+directions, col));
                if(row == 1 && color.equals("white") || row == 6 && color.equals("black")){
                    if(board[row+directions*2][col] == null) moves.add(new Pair(row+directions*2, col));
                }
            }

            if(col < 7 && board[row+directions][col+1] != null &&
               !board[row+directions][col+1].color.equals(this.color)) {
                moves.add(new Pair(row+directions, col+1));
            }
            if(col > 0 && board[row+directions][col-1] != null &&
               !board[row+directions][col-1].color.equals(this.color)) {
                moves.add(new Pair(row+directions, col-1));
            }

            if(color.equals("white") && col > 0 && col < 7 && (lEnPassant || rEnPassant)) {
                if(board[5][col-1] != null && !board[5][col-1].color.equals(this.color) &&
                   board[5][col-1].type.equals(this.type)) {
                    lEnPassant = false;
                }
                if(board[5][col+1] != null && !board[5][col+1].color.equals(this.color) &&
                        board[5][col+1].type.equals(this.type)) {
                    rEnPassant = false;
                }
                if(lEnPassant) {
                    if(board[4][col-1] != null && !board[4][col-1].color.equals(this.color) &&
                       board[4][col-1].type.equals(this.type) && board[4][col] != null &&
                       board[4][col].color.equals(this.color) && board[4][col].type.equals(this.type)) {
                        moves.add(new Pair(row+directions, col-1));
                        lEnPassant = false;
                    }
                    else if(board[4][col-1] != null && !board[4][col-1].color.equals(this.color) &&
                            board[4][col-1].type.equals(this.type)) {
                        lEnPassant = false;
                    }
                }
                if(rEnPassant) {
                    if(board[4][col+1] != null && !board[4][col+1].color.equals(this.color) &&
                            board[4][col+1].type.equals(this.type) && board[4][col] != null &&
                            board[4][col].color.equals(this.color) && board[4][col].type.equals(this.type)) {
                        moves.add(new Pair(row+directions, col+1));
                        rEnPassant = false;
                    }
                    else if(board[4][col+1] != null && !board[4][col+1].color.equals(this.color) &&
                            board[4][col+1].type.equals(this.type)) {
                        rEnPassant = false;
                    }
                }
            }
            if(color.equals("black") && col > 0 && col < 7 && (lEnPassant || rEnPassant)) {
                if(board[2][col-1] != null && !board[2][col-1].color.equals(this.color) &&
                        board[2][col-1].type.equals(this.type)) {
                    rEnPassant = false;
                }
                if(board[2][col+1] != null && !board[2][col+1].color.equals(this.color) &&
                        board[2][col+1].type.equals(this.type)) {
                    lEnPassant = false;
                }
                if(rEnPassant) {
                    if(board[3][col-1] != null && !board[3][col-1].color.equals(this.color) &&
                            board[3][col-1].type.equals(this.type) && board[3][col] != null &&
                            board[3][col].color.equals(this.color) && board[3][col].type.equals(this.type)) {
                        moves.add(new Pair(row+directions, col-1));
                        rEnPassant = false;
                    }
                    else if(board[3][col-1] != null && !board[3][col-1].color.equals(this.color) &&
                            board[3][col-1].type.equals(this.type)) {
                        rEnPassant = false;
                    }
                }
                if(lEnPassant) {
                    if(board[3][col+1] != null && !board[3][col+1].color.equals(this.color) &&
                            board[3][col+1].type.equals(this.type) && board[3][col] != null &&
                            board[3][col].color.equals(this.color) && board[3][col].type.equals(this.type)) {
                        moves.add(new Pair(row+directions, col+1));
                        lEnPassant = false;
                    }
                    else if(board[3][col+1] != null && !board[3][col+1].color.equals(this.color) &&
                            board[3][col+1].type.equals(this.type)) {
                        lEnPassant = false;
                    }
                }
            }
        }
        return moves;
    }

    public boolean canPromote() {
        return (color.equals("white") && row == 7 || color.equals("black") && row == 0);
    }

    // ...
    @Override
    public Pieces copy() {
        Pawn newPawn = new Pawn(this.color, this.row, this.col);
        newPawn.lEnPassant = this.lEnPassant;
        newPawn.rEnPassant = this.rEnPassant;
        return newPawn;
    }
// ...
}