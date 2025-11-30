package com.akhilesh.chessapp.model;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class BitBoard {

    @Autowired
    private PrecomputedMoveData precomputedMoveData;

    // --- STATE VARIABLES ---
    public long[] bitboards = new long[12];
    public long whitePieces, blackPieces, allPieces;
    public int turn = ChessConstants.WHITE;
    public int castlingRights = 15; // 1111 (WK, WQ, BK, BQ)
    public int enPassantSquare = -1;

    public BitBoard() {
        // Relies on resetBoard()
    }

    // Copy Constructor for AI / Validation
    public BitBoard(BitBoard other) {
        System.arraycopy(other.bitboards, 0, this.bitboards, 0, 12);
        this.whitePieces = other.whitePieces;
        this.blackPieces = other.blackPieces;
        this.allPieces = other.allPieces;
        this.turn = other.turn;
        this.castlingRights = other.castlingRights;
        this.enPassantSquare = other.enPassantSquare;
        this.precomputedMoveData = other.precomputedMoveData;
    }

    public BitBoard copy() {
        return new BitBoard(this);
    }

    public void resetBoard() {
        for(int i=0; i<12; i++) bitboards[i] = 0L;

        // White Pieces
        bitboards[index(ChessConstants.WHITE, ChessConstants.PAWN)]   = 0x000000000000FF00L;
        bitboards[index(ChessConstants.WHITE, ChessConstants.ROOK)]   = 0x0000000000000081L;
        bitboards[index(ChessConstants.WHITE, ChessConstants.KNIGHT)] = 0x0000000000000042L;
        bitboards[index(ChessConstants.WHITE, ChessConstants.BISHOP)] = 0x0000000000000024L;
        bitboards[index(ChessConstants.WHITE, ChessConstants.QUEEN)]  = 0x0000000000000008L;
        bitboards[index(ChessConstants.WHITE, ChessConstants.KING)]   = 0x0000000000000010L;

        // Black Pieces
        bitboards[index(ChessConstants.BLACK, ChessConstants.PAWN)]   = 0x00FF000000000000L;
        bitboards[index(ChessConstants.BLACK, ChessConstants.ROOK)]   = 0x8100000000000000L;
        bitboards[index(ChessConstants.BLACK, ChessConstants.KNIGHT)] = 0x4200000000000000L;
        bitboards[index(ChessConstants.BLACK, ChessConstants.BISHOP)] = 0x2400000000000000L;
        bitboards[index(ChessConstants.BLACK, ChessConstants.QUEEN)]  = 0x0800000000000000L;
        bitboards[index(ChessConstants.BLACK, ChessConstants.KING)]   = 0x1000000000000000L;

        turn = ChessConstants.WHITE;
        castlingRights = 15;
        enPassantSquare = -1;
        updateOccupancies();
    }

    // --- MAIN MOVE GENERATION (MAKE-VERIFY LOOP) ---

    public List<Integer> generateLegalMoves() {
        // 1. Generate all physically possible moves (Pseudo-Legal)
        List<Integer> pseudoMoves = generatePseudoLegalMoves();
        List<Integer> legalMoves = new ArrayList<>();

        // 2. Filter: Try every move, check if King dies
        for (int move : pseudoMoves) {
            BitBoard tempBoard = this.copy();

            int from = (move >> 6) & 0x3F;
            int to = move & 0x3F;

            // Assume Queen for checking (promotion type doesn't affect check status)
            tempBoard.performBitwiseMove(from, to, ChessConstants.QUEEN);

            // After the move, 'tempBoard.turn' has flipped to the OPPONENT.
            // We need to check if the person who *just moved* left their King in check.
            int ourColor = (tempBoard.turn == ChessConstants.WHITE) ? ChessConstants.BLACK : ChessConstants.WHITE;

            // Find our King
            long kingBitboard = tempBoard.bitboards[index(ourColor, ChessConstants.KING)];
            if (kingBitboard == 0) continue; // Should never happen

            int kingSq = Long.numberOfTrailingZeros(kingBitboard);

            // Is our King attacked by the opponent (tempBoard.turn)?
            if (!tempBoard.isSquareAttacked(kingSq, tempBoard.turn)) {
                legalMoves.add(move);
            }
        }
        return legalMoves;
    }

    // --- PSEUDO-LEGAL GENERATOR (Pure Geometry) ---

    private List<Integer> generatePseudoLegalMoves() {
        updateOccupancies();
        List<Integer> moves = new ArrayList<>();

        long myPieces = (turn == ChessConstants.WHITE) ? whitePieces : blackPieces;
        long enemyPieces = (turn == ChessConstants.WHITE) ? blackPieces : whitePieces;
        long allOccupancy = allPieces;

        // 1. Sliding Pieces
        generateSlidingMoves(moves, bitboards[index(turn, ChessConstants.ROOK)], 0, 4, allOccupancy, myPieces);
        generateSlidingMoves(moves, bitboards[index(turn, ChessConstants.BISHOP)], 4, 8, allOccupancy, myPieces);
        generateSlidingMoves(moves, bitboards[index(turn, ChessConstants.QUEEN)], 0, 8, allOccupancy, myPieces);

        // 2. Knights
        long knights = bitboards[index(turn, ChessConstants.KNIGHT)];
        while(knights != 0) {
            int start = Long.numberOfTrailingZeros(knights);
            long possible = PrecomputedMoveData.KNIGHT_MOVES[start] & ~myPieces;
            extractMoves(moves, start, possible);
            knights &= knights - 1;
        }

        // 3. King (Normal + Castling)
        long king = bitboards[index(turn, ChessConstants.KING)];
        if (king != 0) {
            int kingSq = Long.numberOfTrailingZeros(king);
            long possible = PrecomputedMoveData.KING_MOVES[kingSq] & ~myPieces;
            extractMoves(moves, kingSq, possible);
            generateCastlingMoves(moves, kingSq, allOccupancy);
        }

        // 4. Pawns
        generatePawnMoves(moves, enemyPieces, allOccupancy);

        return moves;
    }

    // --- SAFETY CHECK (CRITICAL FOR BUG FIXING) ---

    public boolean isSquareAttacked(int square, int attackerColor) {
        // 1. Pawn Attacks
        long pawns = bitboards[index(attackerColor, ChessConstants.PAWN)];
        if (attackerColor == ChessConstants.WHITE) {
            // Check if White pawns are at [sq-7] or [sq-9]
            if (((1L << square) >>> 7 & ~ChessConstants.FILE_A & pawns) != 0) return true;
            if (((1L << square) >>> 9 & ~ChessConstants.FILE_H & pawns) != 0) return true;
        } else {
            // Check if Black pawns are at [sq+7] or [sq+9]
            if (((1L << square) << 7 & ~ChessConstants.FILE_H & pawns) != 0) return true;
            if (((1L << square) << 9 & ~ChessConstants.FILE_A & pawns) != 0) return true;
        }

        // 2. Knight Attacks
        if ((PrecomputedMoveData.KNIGHT_MOVES[square] & bitboards[index(attackerColor, ChessConstants.KNIGHT)]) != 0) return true;

        // 3. King Attacks
        if ((PrecomputedMoveData.KING_MOVES[square] & bitboards[index(attackerColor, ChessConstants.KING)]) != 0) return true;

        // 4. Sliding Attacks (Rook/Queen)
        long orthogonal = bitboards[index(attackerColor, ChessConstants.ROOK)] | bitboards[index(attackerColor, ChessConstants.QUEEN)];
        if ((getSlidingAttacks(square, allPieces, 0, 4) & orthogonal) != 0) return true;

        // 5. Diagonal Attacks (Bishop/Queen)
        long diagonal = bitboards[index(attackerColor, ChessConstants.BISHOP)] | bitboards[index(attackerColor, ChessConstants.QUEEN)];
        if ((getSlidingAttacks(square, allPieces, 4, 8) & diagonal) != 0) return true;

        return false;
    }

    // --- HELPERS ---

    private long getSlidingAttacks(int square, long occupancy, int startDir, int endDir) {
        long attacks = 0L;
        for (int i = startDir; i < endDir; i++) {
            int dist = PrecomputedMoveData.NUM_SQUARES_TO_EDGE[square][i];
            int offset = PrecomputedMoveData.DIRECTION_OFFSETS[i];
            for (int n = 1; n <= dist; n++) {
                int targetSq = square + (offset * n);
                long targetMask = 1L << targetSq;
                attacks |= targetMask;
                if ((targetMask & occupancy) != 0) break; // Blocked
            }
        }
        return attacks;
    }

    private void generatePawnMoves(List<Integer> moves, long enemyPieces, long allOccupancy) {
        long pawns = bitboards[index(turn, ChessConstants.PAWN)];
        long empty = ~allOccupancy;

        if (turn == ChessConstants.WHITE) {
            long singlePush = (pawns << 8) & empty;
            long doublePush = ((singlePush & 0x0000000000FF0000L) << 8) & empty;
            extractPawnMoves(moves, singlePush, -8);
            extractPawnMoves(moves, doublePush, -16);
            long leftCap = (pawns & ~ChessConstants.FILE_A) << 7 & enemyPieces;
            long rightCap = (pawns & ~ChessConstants.FILE_H) << 9 & enemyPieces;
            extractPawnMoves(moves, leftCap, -7);
            extractPawnMoves(moves, rightCap, -9);

            if (enPassantSquare != -1) {
                long epTarget = 1L << enPassantSquare;
                long realLeftCap = (pawns & ~ChessConstants.FILE_A) << 7 & epTarget;
                long realRightCap = (pawns & ~ChessConstants.FILE_H) << 9 & epTarget;
                if (realLeftCap != 0) moves.add(encodeMove(enPassantSquare - 7, enPassantSquare));
                if (realRightCap != 0) moves.add(encodeMove(enPassantSquare - 9, enPassantSquare));
            }

        } else { // BLACK
            long singlePush = (pawns >>> 8) & empty;
            long doublePush = ((singlePush & 0x0000FF0000000000L) >>> 8) & empty;
            extractPawnMoves(moves, singlePush, 8);
            extractPawnMoves(moves, doublePush, 16);

            // Corrected Black Capture Logic
            long captureSE = (pawns & ~ChessConstants.FILE_H) >>> 7 & enemyPieces;
            long captureSW = (pawns & ~ChessConstants.FILE_A) >>> 9 & enemyPieces;
            extractPawnMoves(moves, captureSE, 7);
            extractPawnMoves(moves, captureSW, 9);

            if (enPassantSquare != -1) {
                long epTarget = 1L << enPassantSquare;
                long epSE = (pawns & ~ChessConstants.FILE_H) >>> 7 & epTarget;
                long epSW = (pawns & ~ChessConstants.FILE_A) >>> 9 & epTarget;
                if (epSE != 0) moves.add(encodeMove(enPassantSquare + 7, enPassantSquare));
                if (epSW != 0) moves.add(encodeMove(enPassantSquare + 9, enPassantSquare));
            }
        }
    }

    private void generateCastlingMoves(List<Integer> moves, int kingSq, long allOccupancy) {
        // Cannot castle if King is in check
        int enemyColor = (turn == ChessConstants.WHITE) ? ChessConstants.BLACK : ChessConstants.WHITE;
        if (isSquareAttacked(kingSq, enemyColor)) return;

        if (turn == ChessConstants.WHITE) {
            if ((castlingRights & ChessConstants.CASTLE_WK) != 0 && (allOccupancy & ChessConstants.W_K_CASTLE_EMPTY) == 0) {
                if (!isSquareAttacked(5, ChessConstants.BLACK) && !isSquareAttacked(6, ChessConstants.BLACK))
                    moves.add(encodeMove(4, 6));
            }
            if ((castlingRights & ChessConstants.CASTLE_WQ) != 0 && (allOccupancy & ChessConstants.W_Q_CASTLE_EMPTY) == 0) {
                if (!isSquareAttacked(3, ChessConstants.BLACK) && !isSquareAttacked(2, ChessConstants.BLACK))
                    moves.add(encodeMove(4, 2));
            }
        } else {
            if ((castlingRights & ChessConstants.CASTLE_BK) != 0 && (allOccupancy & ChessConstants.B_K_CASTLE_EMPTY) == 0) {
                if (!isSquareAttacked(61, ChessConstants.WHITE) && !isSquareAttacked(62, ChessConstants.WHITE))
                    moves.add(encodeMove(60, 62));
            }
            if ((castlingRights & ChessConstants.CASTLE_BQ) != 0 && (allOccupancy & ChessConstants.B_Q_CASTLE_EMPTY) == 0) {
                if (!isSquareAttacked(59, ChessConstants.WHITE) && !isSquareAttacked(58, ChessConstants.WHITE))
                    moves.add(encodeMove(60, 58));
            }
        }
    }

    private void generateSlidingMoves(List<Integer> moves, long pieces, int startDir, int endDir, long allOccupancy, long myPieces) {
        while (pieces != 0) {
            int startSq = Long.numberOfTrailingZeros(pieces);
            for (int dir = startDir; dir < endDir; dir++) {
                int dist = PrecomputedMoveData.NUM_SQUARES_TO_EDGE[startSq][dir];
                int offset = PrecomputedMoveData.DIRECTION_OFFSETS[dir];
                for (int n = 1; n <= dist; n++) {
                    int targetSq = startSq + (offset * n);
                    long targetMask = 1L << targetSq;
                    if ((targetMask & myPieces) != 0) break;
                    moves.add(encodeMove(startSq, targetSq));
                    if ((targetMask & allOccupancy) != 0) break;
                }
            }
            pieces &= pieces - 1;
        }
    }

    // --- EXECUTION ---

    public void performBitwiseMove(int from, int to, int promotionType) {
        long fromMask = 1L << from;
        long toMask = 1L << to;
        long moveMask = fromMask | toMask;

        int movingPiece = -1;
        int colorBase = index(turn, 0);

        // 1. Move Piece
        for (int i = 0; i < 6; i++) {
            if ((bitboards[colorBase + i] & fromMask) != 0) {
                bitboards[colorBase + i] ^= moveMask;
                movingPiece = i;
                break;
            }
        }

        // 2. Capture
        int enemyColor = (turn == ChessConstants.WHITE) ? ChessConstants.BLACK : ChessConstants.WHITE;
        int enemyBase = index(enemyColor, 0);
        for (int i = 0; i < 6; i++) {
            if ((bitboards[enemyBase + i] & toMask) != 0) {
                bitboards[enemyBase + i] ^= toMask;
                break;
            }
        }

        // 3. En Passant Capture
        if (movingPiece == ChessConstants.PAWN && to == enPassantSquare) {
            int captureSq = (turn == ChessConstants.WHITE) ? to - 8 : to + 8;
            bitboards[index(enemyColor, ChessConstants.PAWN)] &= ~(1L << captureSq);
        }

        // 4. Castling (Rook Move)
        if (movingPiece == ChessConstants.KING && Math.abs(from - to) == 2) {
            if (to == 6)  performRookCastle(7, 5);
            if (to == 2)  performRookCastle(0, 3);
            if (to == 62) performRookCastle(63, 61);
            if (to == 58) performRookCastle(56, 59);
        }

        // 5. Rights & EP
        if (movingPiece == ChessConstants.KING) {
            if (turn == ChessConstants.WHITE) castlingRights &= ~(ChessConstants.CASTLE_WK | ChessConstants.CASTLE_WQ);
            else castlingRights &= ~(ChessConstants.CASTLE_BK | ChessConstants.CASTLE_BQ);
        }
        removeRightIfInvolved(from, to);

        enPassantSquare = -1;
        if (movingPiece == ChessConstants.PAWN && Math.abs(from - to) == 16) {
            enPassantSquare = (from + to) / 2;
        }

        // 6. Promotion
        if (movingPiece == ChessConstants.PAWN) {
            boolean whitePromoting = (turn == ChessConstants.WHITE) && (to >= 56);
            boolean blackPromoting = (turn == ChessConstants.BLACK) && (to <= 7);
            if (whitePromoting || blackPromoting) {
                bitboards[index(turn, ChessConstants.PAWN)] &= ~toMask;
                bitboards[index(turn, promotionType)] |= toMask;
            }
        }

        turn = enemyColor;
        updateOccupancies();
    }

    private void performRookCastle(int from, int to) {
        long moveMask = (1L << from) | (1L << to);
        bitboards[index(turn, ChessConstants.ROOK)] ^= moveMask;
    }

    private void removeRightIfInvolved(int from, int to) {
        if (from == 0 || to == 0) castlingRights &= ~ChessConstants.CASTLE_WQ;
        if (from == 7 || to == 7) castlingRights &= ~ChessConstants.CASTLE_WK;
        if (from == 56 || to == 56) castlingRights &= ~ChessConstants.CASTLE_BQ;
        if (from == 63 || to == 63) castlingRights &= ~ChessConstants.CASTLE_BK;
    }

    // --- UTILS ---
    private void updateOccupancies() {
        whitePieces = 0L; blackPieces = 0L;
        for (int i = 0; i < 6; i++) whitePieces |= bitboards[i];
        for (int i = 6; i < 12; i++) blackPieces |= bitboards[i];
        allPieces = whitePieces | blackPieces;
    }

    private void extractMoves(List<Integer> moves, int start, long mask) {
        while (mask != 0) {
            int target = Long.numberOfTrailingZeros(mask);
            moves.add(encodeMove(start, target));
            mask &= mask - 1;
        }
    }

    private void extractPawnMoves(List<Integer> moves, long mask, int offset) {
        while (mask != 0) {
            int target = Long.numberOfTrailingZeros(mask);
            moves.add(encodeMove(target + offset, target));
            mask &= mask - 1;
        }
    }

    private int index(int color, int type) { return color * 6 + type; }
    private int encodeMove(int from, int to) { return (from << 6) | to; }

    // Add toFEN() and toPieceMap() methods here (from previous steps)
// ---------------------------------------------------------
    //  VISUALIZATION HELPERS (Replace the placeholders with these)
    // ---------------------------------------------------------

    public java.util.Map<String, String> toPieceMap() {
        java.util.Map<String, String> map = new java.util.HashMap<>();
        for (int i = 0; i < 64; i++) {
            char c = getPieceAtSquare(i);
            if (c != '-') {
                int col = i % 8;
                int row = i / 8;
                String square = "" + (char)('a' + col) + (row + 1);
                String colorPrefix = Character.isUpperCase(c) ? "w" : "b";
                String pieceCode = colorPrefix + Character.toUpperCase(c);
                map.put(square, pieceCode);
            }
        }
        return map;
    }

    public String toFEN() {
        StringBuilder fen = new StringBuilder();
        for (int rank = 7; rank >= 0; rank--) {
            int emptyCount = 0;
            for (int file = 0; file < 8; file++) {
                int square = rank * 8 + file;
                char pieceChar = getPieceAtSquare(square);
                if (pieceChar == '-') {
                    emptyCount++;
                } else {
                    if (emptyCount > 0) {
                        fen.append(emptyCount);
                        emptyCount = 0;
                    }
                    fen.append(pieceChar);
                }
            }
            if (emptyCount > 0) fen.append(emptyCount);
            if (rank > 0) fen.append('/');
        }
        fen.append(" ").append(turn == ChessConstants.WHITE ? "w" : "b");
        fen.append(" KQkq - 0 1"); // Simplification for FEN requirements
        return fen.toString();
    }

    private char getPieceAtSquare(int square) {
        long mask = 1L << square;
        for (int i = 0; i < 6; i++) {
            if ((bitboards[index(ChessConstants.WHITE, i)] & mask) != 0) return getPieceChar(i, true);
            if ((bitboards[index(ChessConstants.BLACK, i)] & mask) != 0) return getPieceChar(i, false);
        }
        return '-';
    }

    private char getPieceChar(int type, boolean white) {
        char c = switch (type) {
            case ChessConstants.PAWN -> 'p';
            case ChessConstants.KNIGHT -> 'n';
            case ChessConstants.BISHOP -> 'b';
            case ChessConstants.ROOK -> 'r';
            case ChessConstants.QUEEN -> 'q';
            case ChessConstants.KING -> 'k';
            default -> '?';
        };
        return white ? Character.toUpperCase(c) : c;
    }
    
}