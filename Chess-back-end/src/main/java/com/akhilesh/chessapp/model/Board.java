package com.akhilesh.chessapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class Board {
    private Pieces[][] board;
    private String turn = "white";
    private HashMap<Pair, HashSet<Pair>> legalMoves = new HashMap<>();
    private King whiteKing;
    private King blackKing;
    private String pendingPromotion = null;

    public Board() {
        board = new Pieces[8][8];
        this.whiteKing = new King("white",0, 4);
        this.blackKing = new King("black",7, 4);

        board[0][4] = whiteKing;
        board[7][4] = blackKing;

        board[0][3] = new Queen("white", 0, 3);
        board[7][3] = new Queen("black", 7, 3);

        for(int i = 0; i < 8; i++) {
            board[1][i] = new Pawn("white", 1, i);
            board[6][i] = new Pawn("black", 6, i);
        }
        board[0][0] = new Rook("white", 0, 0);
        board[0][7] = new Rook("white", 0, 7);
        board[7][0] = new Rook("black", 7, 0);
        board[7][7] = new Rook("black", 7, 7);

        board[0][2] = new Bishop("white", 0, 2);
        board[0][5] = new Bishop("white", 0, 5);
        board[7][2] = new Bishop("black", 7, 2);
        board[7][5] = new Bishop("black", 7, 5);

        board[0][1] = new Knight("white", 0, 1);
        board[0][6] = new Knight("white", 0, 6);
        board[7][1] = new Knight("black", 7, 1);
        board[7][6] = new Knight("black", 7, 6);
    }

    // Copy constructor
    public Board(Board other) {
        this.board = new Pieces[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (other.board[i][j] != null) {
                    this.board[i][j] = other.board[i][j].copy();
                }
            }
        }
        this.turn = other.turn;
        this.legalMoves = new HashMap<>(other.legalMoves);
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (this.board[i][j] instanceof King) {
                    if (this.board[i][j].getColor().equals("white")) {
                        this.whiteKing = (King) this.board[i][j];
                    } else {
                        this.blackKing = (King) this.board[i][j];
                    }
                }
            }
        }
        this.pendingPromotion = other.pendingPromotion;
    }


    public void setPendingPromotion(String promotion) {
        this.pendingPromotion = promotion;
    }

    public String getPendingPromotion() {
        return this.pendingPromotion;
    }

    public Pieces[][] getBoard() {
        return board;
    }

    public void switchTurn() {
        if(turn.equals("white")) {
            this.turn = "black";
        }
        else {
            this.turn = "white";
        }
    }

    public String getTurn() {
        return turn;
    }

    public HashMap<Pair, HashSet<Pair>> getLegalMoves() {
        return legalMoves;
    }

    public void setLegalMoves(HashMap<Pair, HashSet<Pair>> legalMoves) {
        this.legalMoves = legalMoves;
    }

    public King getCurrentKing() {
        if(turn.equals("white")) {
            return whiteKing;
        }
        else {
            return blackKing;
        }
    }

    public String toFEN() {
        StringBuilder fen = new StringBuilder();

        for (int row = 7; row >=0 ; row--) {
            int emptyCount = 0;
            for (int col = 0; col < 8; col++) {
                Pieces piece = board[row][col];
                if (piece == null) {
                    emptyCount++;
                } else {
                    if (emptyCount > 0) {
                        fen.append(emptyCount);
                        emptyCount = 0;
                    }
                    fen.append(getFENChar(piece));
                }
            }
            if (emptyCount > 0) {
                fen.append(emptyCount);
            }
            if (row > 0) {
                fen.append('/');
            }
        }

        fen.append(" ").append(turn.startsWith("w") ? "w" : "b");
        fen.append(" KQkq - 0 1");

        return fen.toString();
    }

    private char getFENChar(Pieces piece) {
        char c;
        switch (piece.getType()) {
            case "pawn":   c = 'p'; break;
            case "rook":   c = 'r'; break;
            case "night":  c = 'n'; break; // KEEPING "night" as per your request
            case "bishop": c = 'b'; break;
            case "queen":  c = 'q'; break;
            case "king":   c = 'k'; break;
            default:     c = '?';
        }
        return piece.getColor().equals("white") ? Character.toUpperCase(c) : c;
    }

    public Map<String, String> toPieceMap() {
        Map<String, String> map = new HashMap<>();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Pieces p = board[row][col];
                if (p != null) {
                    String square = "" + (char) ('a' + col) + (row + 1);
                    String code = (p.getColor().charAt(0)) + p.getType().substring(0, 1).toUpperCase();
                    map.put(square, code);
                }
            }
        }
        return map;
    }

    // This method is critical for King Safety
    public HashMap<Pair, HashSet<Pair>> getAllLegalMoves(Pieces[][] board) {
        HashMap<Pair, HashSet<Pair>> pseudoLegalMoves = new HashMap<>();
        HashMap<Pair, HashSet<Pair>> trueLegalMoves = new HashMap<>();

        // 1. Generate all moves the pieces *can* make (ignoring Check)
        for(int i = 0; i < 8; i++) {
            for(int j = 0; j < 8; j++) {
                Pieces piece = board[i][j];
                if (piece != null && piece.getColor().equals(getTurn())) {
                    HashSet<Pair> movesForPiece = piece.getMoves(board);
                    pseudoLegalMoves.put(new Pair(i, j), movesForPiece);
                }
            }
        }

        // 2. Filter out moves that leave the King in Check
        for (Map.Entry<Pair, HashSet<Pair>> entry : pseudoLegalMoves.entrySet()) {
            Pair from = entry.getKey();
            HashSet<Pair> moves = entry.getValue();
            HashSet<Pair> legalMovesAfterCheck = new HashSet<>();

            for (Pair to : moves) {
                // Simulate the move on a copy
                Board tempBoard = new Board(this);
                // Note: tempBoard.move does not switch turn, so we can still check *our* king
                tempBoard.move(from, to);

                // After moving, is my king attacked?
                if (!tempBoard.getCurrentKing().isCheck(tempBoard.getCurrentKing().getRow(),
                        tempBoard.getCurrentKing().getCol(),
                        tempBoard.board)) {
                    legalMovesAfterCheck.add(to);
                }
            }

            if (!legalMovesAfterCheck.isEmpty()) {
                trueLegalMoves.put(from, legalMovesAfterCheck);
            }
        }

        setLegalMoves(trueLegalMoves);
        return trueLegalMoves;
    }

    // ... (toString method)

    public boolean move(Pair from, Pair to) {
        Pieces piece = board[from.getX()][from.getY()];
        Pieces checkEmpty = board[to.getX()][to.getY()];

        // Simplified move logic, relying on validation happening before this method is called
        if (piece != null) {

            board[to.getX()][to.getY()] = piece;
            board[from.getX()][from.getY()].setRow(to.getX());
            board[from.getX()][from.getY()].setCol(to.getY());
            board[from.getX()][from.getY()] = null;

            if (piece.type.equals("king")) {
                ((King) piece).setHasMoved(true);

                // Queenside Castling
                if ((from.getY() - to.getY()) == 2) {
                    Pieces rook = board[from.getX()][0];
                    if (rook != null) {
                        board[from.getX()][3] = rook;
                        board[from.getX()][0] = null;
                        rook.setCol(3);
                    }
                    ((King) piece).setCastled(true);
                }
                // Kingside Castling
                else if ((to.getY() - from.getY()) == 2) {
                    Pieces rook = board[from.getX()][7];
                    if (rook != null) {
                        board[from.getX()][5] = rook;
                        board[from.getX()][7] = null;
                        rook.setCol(5);
                    }
                    ((King) piece).setCastled(true);
                }
            }
            else if (piece.type.equals("pawn") && ((Pawn) piece).canPromote()) {
                String promotion = this.getPendingPromotion();
                // Default to Queen if no promotion specified
                if (promotion == null) promotion = "q";

                if (promotion != null) {
                    Pieces promoted = switch (promotion) {
                        case "q" -> new Queen(piece.color, to.getX(), to.getY());
                        case "r" -> new Rook(piece.color, to.getX(), to.getY());
                        case "n" -> new Knight(piece.color, to.getX(), to.getY());
                        case "b" -> new Bishop(piece.color, to.getX(), to.getY());
                        default -> new Queen(piece.color, to.getX(), to.getY());
                    };
                    promotePawn((Pawn) piece, promoted);
                }
                this.setPendingPromotion(null);
            }
            // En Passant capture (basic check)
            else if (piece.type.equals("pawn") && to.getY() != from.getY() && checkEmpty == null) {
                if (piece.color.equals("white")) {
                    board[to.getX() - 1][to.getY()] = null;
                } else {
                    board[to.getX() + 1][to.getY()] = null;
                }
            }
            return true;
        }
        return false;
    }

    public void promotePawn(Pawn pawn, Pieces promotedPiece) {
        board[pawn.getRow()][pawn.getCol()] = promotedPiece;
    }

    public HashSet<Pair> highlightMove(int row, int col) {
        return getLegalMoves().get(new Pair(row, col));
    }
}