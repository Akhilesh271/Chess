package com.akhilesh.chessapp.model;

import java.sql.SQLOutput;
import java.util.HashSet;

public class King extends Pieces {
    private boolean moved = false;
    private final HashSet<Pair> attackingPieces = new HashSet<>();
    private boolean castled = false;

    public King(String color, int row, int col) {
        super(color, row, col);
        this.type = "king";
    }

    public boolean getCastled() {
        return castled;
    }

    public void setCastled(boolean castled) {
        this.castled = castled;
    }

    public HashSet<Pair> getAttackingPieces() {
        return attackingPieces;
    }

    public boolean getHasMoved() {
        return moved;
    }

    public void setHasMoved(boolean moved) {
        this.moved = moved;
    }

    public boolean canKCastle(Pieces[][] board) {
        Rook rook = (Rook) board[row][7];
        if(rook != null && rook.color.equals(this.color) &&
                board[row][7].type.equals("rook") && !rook.getHasMoved()) {
            return true;
        }
        return false;
    }

    public boolean canQCastle(Pieces[][] board) {
        Rook rook = (Rook) board[row][0];
        if(rook != null && rook.color.equals(this.color) &&
                board[row][0].type.equals("rook") && !rook.getHasMoved()) {
            return true;
        }
        return false;
    }

    public HashSet<Pair> getMoves(Pieces[][] board) {
        int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {1, -1}, {1, 1}, {-1, 0}, {-1, -1}, {-1, 1}};
        HashSet<Pair> moves = new HashSet<>();

        // Standard king moves
        for (int[] d : directions) {
            int newRow = row + d[0];
            int newCol = col + d[1];
            if (newRow >= 0 && newRow < 8 && newCol >= 0 && newCol < 8) {
                Pair position = new Pair(newRow, newCol);
                if ((board[newRow][newCol] == null || !board[newRow][newCol].color.equals(this.color)) && !isCheck(newRow, newCol, board)) {
                    moves.add(position);
                }
            }
        }

        // Castling logic
        boolean kCastle = false;
        boolean qCastle = false;

        // First, check if the king is on its home square.
        boolean onHomeSquare = (this.color.equals("white") && row == 0 && col == 4) || (this.color.equals("black") && row == 7 && col == 4);

        if (!getHasMoved() && onHomeSquare && !isCheck(row, col, board)) {
            // Kingside castling with boundary checks
            if (col + 2 < 8 && board[row][col + 1] == null && board[row][col + 2] == null) {
                if (board[row][7] != null && !isCheck(row, col + 1, board) && !isCheck(row, col + 2, board)) {
                    kCastle = canKCastle(board);
                }
            }
            // Queenside castling with boundary checks
            if (col - 3 >= 0 && board[row][col - 1] == null && board[row][col - 2] == null && board[row][col - 3] == null) {
                if (board[row][0] != null && !isCheck(row, col - 1, board) && !isCheck(row, col - 2, board)) {
                    qCastle = canQCastle(board);
                }
            }
        }

        if (kCastle) {
            moves.add(new Pair(row, col + 2));
        }
        if (qCastle) {
            moves.add(new Pair(row, col - 2));
        }
        return moves;
    }

    public boolean isCheck(int row, int col, Pieces[][] board) {
        attackingPieces.clear();
        int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {1, -1}, {1, 1}, {-1, 0}, {-1, -1}, {-1, 1}};
        boolean check = false;

        for(int[] d : directions) {
            int newRow = row + d[0];
            int newCol = col + d[1];

            if((newRow >= 0 && newRow < 8 && newCol >= 0 && newCol < 8) &&
                    board[newRow][newCol] != null &&
                    !board[newRow][newCol].color.equals(this.color) &&
                    board[newRow][newCol].type.equals("king")) {
                check = true;
                attackingPieces.add(new Pair(newRow, newCol));
            }

            while(newRow >= 0 && newRow < 8 && newCol >= 0 && newCol < 8) {
                Pieces piece = board[newRow][newCol];

                if(piece != null) {
                    if(piece.color.equals(this.color)) {
                        break;
                    }
                    if(((piece.type.equals("rook") || piece.type.equals("queen")) &&
                            (d[0] == 0 || d[1] == 0)) ||
                            ((piece.type.equals("bishop") || piece.type.equals("queen")) &&
                                    (d[0] != 0 && d[1] != 0))) {
                        check = true;
                        attackingPieces.add(new Pair(newRow, newCol));
                        break;
                    }
                    else {
                        break;
                    }
                }
                newRow = newRow + d[0];
                newCol = newCol + d[1];
            }
        }

        int[][] knightDirections = {{1, 2}, {1, -2}, {-1, 2}, {-1, -2}, {2, 1}, {2, -1}, {-2, 1}, {-2, -1}};

        for(int[] d : knightDirections) {
            int newRow = row + d[0];
            int newCol = col + d[1];

            if(newRow >= 0 && newRow < 8 && newCol >= 0 && newCol < 8) {
                Pieces piece = board[newRow][newCol];

                // REVERTED FIX: Check for the type "night" as intended by the user
                if(piece != null && !piece.color.equals(this.color) && piece.type.equals("night")) {
                    check = true;
                    attackingPieces.add(new Pair(newRow, newCol));
                }
            }
        }

        if(color.equals("white")) {
            // Check for attacks from black pawns
            if (row + 1 < 8) {
                if (col + 1 < 8) {
                    Pieces p = board[row + 1][col + 1];
                    if (p != null && p.color.equals("black") && p.type.equals("pawn")) {
                        attackingPieces.add(new Pair(row + 1, col + 1));
                        check = true;
                    }
                }
                if (col - 1 >= 0) {
                    Pieces p = board[row + 1][col - 1];
                    if (p != null && p.color.equals("black") && p.type.equals("pawn")) {
                        attackingPieces.add(new Pair(row + 1, col - 1));
                        check = true;
                    }
                }
            }
        } else { // color is "black"
            // Check for attacks from white pawns
            if (row - 1 >= 0) {
                if (col + 1 < 8) {
                    Pieces p = board[row - 1][col + 1];
                    if (p != null && p.color.equals("white") && p.type.equals("pawn")) {
                        attackingPieces.add(new Pair(row - 1, col + 1));
                        check = true;
                    }
                }
                if (col - 1 >= 0) {
                    Pieces p = board[row - 1][col - 1];
                    if (p != null && p.color.equals("white") && p.type.equals("pawn")) {
                        attackingPieces.add(new Pair(row - 1, col - 1));
                        check = true;
                    }
                }
            }
        }

        return check;
    }

    @Override
    public Pieces copy() {
        King newKing = new King(this.color, this.row, this.col);
        newKing.setHasMoved(this.moved);
        newKing.setCastled(this.castled);
        return newKing;
    }

}