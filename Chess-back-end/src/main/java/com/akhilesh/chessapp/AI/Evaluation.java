package com.akhilesh.chessapp.AI;

import com.akhilesh.chessapp.model.Board;
import com.akhilesh.chessapp.model.King;
import com.akhilesh.chessapp.model.Pair;
import com.akhilesh.chessapp.model.Pieces;

public class Evaluation {
    public enum PiecePoints {
        PAWN(100),
        KNIGHT(300),
        BISHOP(300),
        ROOK(500),
        QUEEN(900),
        KING(10000);

        private final int points;

        private PiecePoints(int points) {
            this.points = points;
        }

        public int getPoints() {
            return points;
        }
    }

    public int getPiecePoints(Pieces piece) {
        if (piece == null) return 0;
        switch (piece.getType()) {
            case "pawn":   return PiecePoints.PAWN.getPoints();
            case "night":  return PiecePoints.KNIGHT.getPoints();
            case "bishop": return PiecePoints.BISHOP.getPoints();
            case "rook":   return PiecePoints.ROOK.getPoints();
            case "queen":  return PiecePoints.QUEEN.getPoints();
            case "king":   return PiecePoints.KING.getPoints();
        };
        return 0;
    }

    public int eval(Board board, boolean isWhite) {
        return evaluateMaterial(board) + evaluatePosition(board);
    }

    public int evaluateMaterial(Board board) {
        int evaluation = 0;
        int whiteBishops = 0;
        int blackBishops = 0;

        for (Pieces[] row : board.getBoard()) {
            for (Pieces piece : row) {
                if (piece == null) continue;

                int points = getPiecePoints(piece);
                boolean isWhite = piece.getColor().equals("white");

                evaluation += isWhite ? points : -points;

                if (piece.getType().equals("bishop")) {
                    if (isWhite) whiteBishops++;
                    else blackBishops++;
                }
            }
        }

        if (whiteBishops >= 2) evaluation += 30;
        if (blackBishops >= 2) evaluation -= 30;

        return evaluation;
    }

    public int evaluatePosition(Board board) {
        int evaluation = 0;
        Pieces[][] grid = board.getBoard();

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Pieces piece = grid[row][col];
                if (piece == null) continue;

                String type = piece.getType();
                boolean isWhite = piece.getColor().equals("white");

                switch (type) {
                    case "rook":   evaluation += rookActivity(board, row, col, null, isWhite); break;
                    case "bishop": evaluation += bishopActivity(board, row, col, null, isWhite); break;
                    case "queen":  evaluation += queenActivity(board, row, col, isWhite); break;
                    case "king":   evaluation += kingSafety(board, row, col, isWhite); break;
                    case "night":  evaluation += knightActivity(board, row, col, isWhite); break;
                }
            }
        }

        return evaluation;
    }


    public int kingSafety(Board board, int row, int col, boolean isWhite) {
        int eval = 0;
        King king = (King) board.getBoard()[row][col];

        if(king.getCastled()) eval += 20;
        else eval -= 30;

        if(isWhite) {
            if(row == 0 &&
                    nullCheck(board, row+1, col) &&
                    nullCheck(board, row+1, col+1) &&
                    nullCheck(board, row+1, col-1)) {
                eval += 20;
            }
        }
        else {
            if(row == 7 &&
                    nullCheck(board, row-1, col) &&
                    nullCheck(board, row-1, col+1) &&
                    nullCheck(board, row-1, col-1)) {
                eval -= 20;
            }
        }
        return eval;
    }

    public boolean nullCheck(Board board, int row, int col) {
        if (row < 0 || row >= 8 || col < 0 || col >= 8) {
            return false;
        }
        return board.getBoard()[row][col] == null;
    }

    public int rookActivity(Board board, int row, int col, Pair kingPosition, boolean isWhite) {
        int mobility = 0;
        int[][] directions = {{1, 0}, {0, 1}, {0, -1}, {-1, 0}};
        int eval = 0;
        boolean connected = connectedRooks(board, row, col, isWhite);

        if(isWhite && row == 6) eval+=15;
        else if(!isWhite && row == 1) eval-=15;

        if(isWhite && connected) eval+=15;
        else if(!isWhite && connected) eval-=15;

        for(int[] x : directions) {
            int r = row + x[0];
            int c = col + x[1];
            while (r >= 0 && r < 8 && c >= 0 && c < 8) {
                if(isWhite) {
                    if(x[0] == 1 && x[1] == 0 && isPasser(board, r, col, isWhite)) eval += 30;
                    if(hasOpenFile(board, col)) eval += 15;
                }
                else{
                    if(x[0] == -1 && x[1] == 0 && (isPasser(board, r, col, isWhite))) eval -= 30;
                    if(hasOpenFile(board, col)) eval -= 15;
                }

                if (board.getBoard()[r][c] == null) {
                    mobility++;
                } else {
                    break;
                }
                r += x[0];
                c += x[1];
            }
        }
        eval += mobility*5;
        return eval;
    }

    public boolean hasOpenFile(Board board, int col) {
        for (int row = 0; row < 8; row++) {
            Pieces p = board.getBoard()[row][col];
            if (p != null && p.getType().equals("pawn")) {
                return false;
            }
        }
        return true;
    }

    public boolean connectedRooks(Board board, int row, int col, boolean isWhite) {
        int[][] directions = {{1, 0}, {0, 1}, {0, -1}, {-1, 0}};
        Pieces[][] grid = board.getBoard();

        for(int[] x : directions) {
            int r = row + x[0];
            int c = col + x[1];
            while (r >= 0 && r < 8 && c >= 0 && c < 8) {
                if(grid[r][c] == null) {
                    r += x[0];
                    c += x[1];
                }
                else if(grid[r][c].getType().equals("rook")
                        && grid[r][c].getColor().equals(grid[row][col].getColor())) {
                    return true;
                }
                else {
                    break;
                }
            }
        }
        return false;
    }

    public boolean isPasser(Board board, int row, int col, boolean isWhite) {
        Pieces[][] grid = board.getBoard();
        if (grid[row][col] == null || !"pawn".equals(grid[row][col].getType())) return false;
        String enemyColor;
        int direction;

        if (isWhite) {
            direction = 1;
            enemyColor = "black";
        } else {
            direction = -1;
            enemyColor = "white";
        }

        for (int r = row + direction; r >= 0 && r < 8; r += direction) {
            for (int c = col - 1; c <= col + 1; c++) {
                if (c < 0 || c >= 8) continue;
                Pieces p = grid[r][c];
                if (p != null && "pawn".equals(p.getType()) && enemyColor.equals(p.getColor())) {
                    return false;
                }
            }
        }
        return true;
    }

    public int bishopActivity(Board board, int row, int col, Pair kingPosition, boolean isWhite) {
        int[][] directions = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
        int mobility = 0;
        Pieces[][] grid = board.getBoard();

        for (int[] dir : directions) {
            int r = row + dir[0];
            int c = col + dir[1];

            while (r >= 0 && r < 8 && c >= 0 && c < 8) {
                if (grid[r][c] == null) {
                    mobility++;
                } else {
                    break;
                }
                r += dir[0];
                c += dir[1];
            }
        }

        int eval = mobility * 5;
        if(isWhite) {
            return eval;
        }
        return eval*-1;
    }

    public int queenActivity(Board board, int row, int col, boolean isWhite) {
        int[][] directions = {
                {-1, -1}, {-1, 1}, {1, -1}, {1, 1},
                {-1, 0}, {1, 0}, {0, -1}, {0, 1}
        };

        int mobility = 0;
        Pieces[][] grid = board.getBoard();

        for (int[] dir : directions) {
            int r = row + dir[0];
            int c = col + dir[1];

            while (r >= 0 && r < 8 && c >= 0 && c < 8) {
                if (grid[r][c] == null) {
                    mobility++;
                } else {
                    break;
                }
                r += dir[0];
                c += dir[1];
            }
        }

        int eval = mobility * 2;
        if(isWhite) {
            return eval;
        }
        return eval*-1;
    }

    public int knightActivity(Board board, int row, int col, boolean isWhite) {
        int[][] offsets = {
                {-2, -1}, {-2, 1}, {-1, -2}, {-1, 2},
                {1, -2}, {1, 2}, {2, -1}, {2, 1}
        };

        int mobility = 0;
        Pieces[][] grid = board.getBoard();

        for (int[] offset : offsets) {
            int r = row + offset[0];
            int c = col + offset[1];
            if (r >= 0 && r < 8 && c >= 0 && c < 8 && grid[r][c] == null) {
                mobility++;
            }
        }

        int eval = mobility * 10;
        if(isWhite) {
            return eval;
        }

        return eval*-1;
    }

    public int pawnStructure(Board board, int row, int col, boolean isWhite) {
        return 0;
    }
}