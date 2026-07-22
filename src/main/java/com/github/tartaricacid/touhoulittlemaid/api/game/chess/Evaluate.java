/*
Evaluate.java - Source Code for Mobile Chess, Part VII

Mobile Chess - a Chess Program for Java ME
Designed by Morning Yellow, Version: 1.01, Last Modified: Feb. 2008
Copyright (C) 2008 mobilechess.sourceforge.net

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License along
with this program; if not, write to the Free Software Foundation, Inc.,
51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package com.github.tartaricacid.touhoulittlemaid.api.game.chess;

// 这部分移植自 Borland C++ 5.0 中的 Owl Chess Sample
@SuppressWarnings("all")
public class Evaluate {
    public static final int PIECE_KING = Position.PIECE_KING;
    public static final int PIECE_QUEEN = Position.PIECE_QUEEN;
    public static final int PIECE_ROOK = Position.PIECE_ROOK;
    public static final int PIECE_BISHOP = Position.PIECE_BISHOP;
    public static final int PIECE_KNIGHT = Position.PIECE_KNIGHT;
    public static final int PIECE_PAWN = Position.PIECE_PAWN;

    public static final int FULL_BIT_RANK = 0x0ff0;
    public static final int LAZY_MARGIN = 100;
    public static final int ISOLATED_PENALTY = 10;
    public static final int DOUBLE_PENALTY = 4;

    public static final int[] PIECE_VALUE = {0, 9, 5, 3, 3, 1};

    public static final int[] PASS_PAWN = {0, 35, 30, 20, 10, 5, 0, 0};

    public static final byte[] DISTANCE = {
            0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 7, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 7, 6, 7, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 7, 6, 5, 6, 7, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 7, 6, 5, 4, 5, 6, 7, 0, 0, 0, 0, 0,
            0, 0, 0, 7, 6, 5, 4, 3, 4, 5, 6, 7, 0, 0, 0, 0,
            0, 0, 7, 6, 5, 4, 3, 2, 3, 4, 5, 6, 7, 0, 0, 0,
            0, 7, 6, 5, 4, 3, 2, 1, 2, 3, 4, 5, 6, 7, 0, 0,
            7, 6, 5, 4, 3, 2, 1, 0, 1, 2, 3, 4, 5, 6, 7, 0,
            0, 7, 6, 5, 4, 3, 2, 1, 2, 3, 4, 5, 6, 7, 0, 0,
            0, 0, 7, 6, 5, 4, 3, 2, 3, 4, 5, 6, 7, 0, 0, 0,
            0, 0, 0, 7, 6, 5, 4, 3, 4, 5, 6, 7, 0, 0, 0, 0,
            0, 0, 0, 0, 7, 6, 5, 4, 5, 6, 7, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 7, 6, 5, 6, 7, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 7, 6, 7, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 7, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0,
    };

    public static final byte[] ENDGAME_EDGE = {
            0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0,
            0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0,
            0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0,
            0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0,
            0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0,
            0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0,
            0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0,
            0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0,
    };

    public static final byte[] ENDGAME_BOTTOM = {
            0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0,
    };

    public static final byte[] ENDGAME_KING_PENALTY = {
            0, 0, 0, 0, 25, 22, 19, 16, 16, 19, 22, 25, 0, 0, 0, 0,
            0, 0, 0, 0, 17, 14, 11, 8, 8, 11, 14, 17, 0, 0, 0, 0,
            0, 0, 0, 0, 13, 10, 7, 4, 4, 7, 10, 13, 0, 0, 0, 0,
            0, 0, 0, 0, 9, 6, 3, 0, 0, 3, 6, 9, 0, 0, 0, 0,
            0, 0, 0, 0, 9, 6, 3, 0, 0, 3, 6, 9, 0, 0, 0, 0,
            0, 0, 0, 0, 13, 10, 7, 4, 4, 7, 10, 13, 0, 0, 0, 0,
            0, 0, 0, 0, 17, 14, 11, 8, 8, 11, 14, 17, 0, 0, 0, 0,
            0, 0, 0, 0, 25, 22, 19, 16, 16, 19, 22, 25, 0, 0, 0, 0,
    };

    public static final byte[] EDGE_PENALTY = {
            0, 0, 0, 0, 6, 5, 4, 3, 3, 4, 5, 6, 0, 0, 0, 0,
            0, 0, 0, 0, 5, 4, 3, 2, 2, 3, 4, 5, 0, 0, 0, 0,
            0, 0, 0, 0, 4, 3, 2, 1, 1, 2, 3, 4, 0, 0, 0, 0,
            0, 0, 0, 0, 3, 2, 1, 0, 0, 1, 2, 3, 0, 0, 0, 0,
            0, 0, 0, 0, 3, 2, 1, 0, 0, 1, 2, 3, 0, 0, 0, 0,
            0, 0, 0, 0, 4, 3, 2, 1, 1, 2, 3, 4, 0, 0, 0, 0,
            0, 0, 0, 0, 5, 4, 3, 2, 2, 3, 4, 5, 0, 0, 0, 0,
            0, 0, 0, 0, 6, 5, 4, 3, 3, 4, 5, 6, 0, 0, 0, 0,
    };

    public static final byte[] PAWN_VALUE = {
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 30, 30, 46, 70, 78, 46, 30, 30, 0, 0, 0, 0,
            0, 0, 0, 0, 8, 8, 22, 43, 50, 22, 8, 8, 0, 0, 0, 0,
            0, 0, 0, 0, 4, 4, 16, 34, 40, 16, 4, 4, 0, 0, 0, 0,
            0, 0, 0, 0, 2, 2, 12, 27, 32, 12, 2, 2, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 8, 20, 24, 8, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 6, 15, 18, 6, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    };

    public static final byte[] CENTER_IMPORTANCE = {
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 2, 2, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 2, 5, 5, 2, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 2, 5, 8, 8, 5, 2, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 2, 5, 8, 8, 5, 2, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 2, 5, 5, 2, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 2, 2, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    };

    public static final byte[] RANK_IMPORTANCE = {
            0, 0, 0, 0, 12, 12, 12, 12, 12, 12, 12, 12, 0, 0, 0, 0,
            0, 0, 0, 0, 12, 12, 12, 12, 12, 12, 12, 12, 0, 0, 0, 0,
            0, 0, 0, 0, 6, 6, 6, 6, 6, 6, 6, 6, 0, 0, 0, 0,
            0, 0, 0, 0, 3, 3, 3, 3, 3, 3, 3, 3, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    };

    public static boolean IN_BOARD(int sq) {
        return Position.IN_BOARD(sq);
    }

    public static int SQUARE_FLIP(int sq) {
        return Position.SQUARE_FLIP(sq);
    }

    public static int losingKingValue(int sq) {
        return -ENDGAME_KING_PENALTY[sq] * 2 - ENDGAME_BOTTOM[sq] * 8;
    }

    public static int winningKingValue(int sq, int sqOppKing) {
        return -DISTANCE[sqOppKing - sq + 128] * 2 - ENDGAME_EDGE[sq] * 8;
    }

    public static int calcRookControl(Position pos, int sqSrc, short[] attack) {
        return calcSlideControl(pos, sqSrc, attack, true);
    }

    public static int calcBishopControl(Position pos, int sqSrc, short[] attack) {
        return calcSlideControl(pos, sqSrc, attack, false);
    }

    public static int calcSlideControl(Position pos, int sqSrc, short[] attack, boolean isRook) {
        int control = 0;
        for (int i = 0; i < 4; i++) {
            int delta = (isRook ? Position.ROOK_DELTA[i] : Position.BISHOP_DELTA[i]);
            int sqDst = sqSrc + delta;
            boolean direct = true;
            while (IN_BOARD(sqDst)) {
                control += (direct ? attack[sqDst] : attack[sqDst] / 2);
                if (pos.squares[sqDst] > 0) {
                    int pieceType = Position.PIECE_TYPE(pos.squares[sqDst]);
                    if (pieceType == PIECE_PAWN) {
                        break;
                    } else if (pieceType == PIECE_KING || pieceType == PIECE_KNIGHT ||
                            pieceType == (isRook ? PIECE_BISHOP : PIECE_ROOK)) {
                        direct = false;
                    }
                }
                sqDst += delta;
            }
        }
        return control;
    }

    public static void preEval(Position pos) {
        // 1. 计算双方的简单材料
        int vlWhite = 0, vlBlack = 0, sqWhiteKing = 0, sqBlackKing = 0;
        for (int sq = 0; sq < 128; sq++) {
            int pc = pos.squares[sq];
            if (pc == 0) {
                continue;
            }
            if (pc < 16) {
                vlWhite += PIECE_VALUE[pc - 8];
                if (pc == 8) {
                    sqWhiteKing = sq;
                }
            } else {
                vlBlack += PIECE_VALUE[pc - 16];
                if (pc == 16) {
                    sqBlackKing = sq;
                }
            }
        }
        boolean inEndgame = Math.min(vlWhite, vlBlack) <= 6 && Math.abs(vlWhite - vlBlack) >= 2;

        short[] whiteAttack = new short[128], blackAttack = new short[128];
        for (int sq = 0; sq < 128; sq++) {
            whiteAttack[sq] = blackAttack[SQUARE_FLIP(sq)] = (short)
                    (RANK_IMPORTANCE[sq] * Math.max(vlWhite + vlBlack - 24, 8) / 32 + CENTER_IMPORTANCE[sq]);
        }
        for (int i = 0; i < 8; i++) {
            int importance = 12 * Math.max(vlWhite + vlBlack - 24, 8) / 32;
            int sq = sqWhiteKing + Position.KING_DELTA[i];
            if (IN_BOARD(sq)) {
                blackAttack[sq] += importance;
            }
            sq = sqBlackKing + Position.KING_DELTA[i];
            if (IN_BOARD(sq)) {
                whiteAttack[sq] += importance;
            }
        }
        // 3.计算每个方格的控制表
        short[] whiteRookControl = new short[128], whiteBishopControl = new short[128];
        short[] blackRookControl = new short[128], blackBishopControl = new short[128];
        for (int sq = 0; sq < 128; sq++) {
            if (IN_BOARD(sq)) {
                whiteRookControl[sq] = (short) calcRookControl(pos, sq, whiteAttack);
                blackRookControl[sq] = (short) calcRookControl(pos, sq, blackAttack);
                whiteBishopControl[sq] = (short) calcBishopControl(pos, sq, whiteAttack);
                blackBishopControl[sq] = (short) calcBishopControl(pos, sq, blackAttack);
            }
        }
        // 4. 计算每个方格和每个棋子类型的棋子值表
        for (int sq = 0; sq < 128; sq++) {
            if (!IN_BOARD(sq)) {
                continue;
            }
            int edgePenalty = EDGE_PENALTY[sq];
            if (inEndgame) {
                if (vlWhite < vlBlack) {
                    // 4.1.在残局中，失败的国王应该靠近中心并远离底部
                    pos.vlWhitePiecePos[PIECE_KING][sq] = (short) losingKingValue(sq);
                    // 4.2.在残局中，获胜的国王应该靠近失败的国王，远离边界
                    pos.vlBlackPiecePos[PIECE_KING][sq] = (short) winningKingValue(sq, sqWhiteKing);
                } else {
                    // 4.1. ...
                    pos.vlBlackPiecePos[PIECE_KING][sq] = (short) losingKingValue(sq);
                    // 4.2. ...
                    pos.vlWhitePiecePos[PIECE_KING][sq] = (short) winningKingValue(sq, sqBlackKing);
                }
                // 4.3.在残局中，其他棋子与其位置无关
                for (int i = PIECE_QUEEN; i <= PIECE_KNIGHT; i++) {
                    pos.vlWhitePiecePos[i][sq] = pos.vlBlackPiecePos[i][sq] =
                            (short) (PIECE_VALUE[i] * 100);
                }
            } else {
                // 4.4.在中局或残局中国王应该靠近中心
                if (vlWhite + vlBlack <= 32) {
                    pos.vlWhitePiecePos[PIECE_KING][sq] =
                            pos.vlBlackPiecePos[PIECE_KING][sq] = (short) -edgePenalty;
                } else {
                    pos.vlWhitePiecePos[PIECE_KING][sq] =
                            pos.vlBlackPiecePos[PIECE_KING][sq] = 0;
                }
                // 4.5.后、车、主教应该偏向于他们的控制值
                pos.vlWhitePiecePos[PIECE_QUEEN][sq] = (short)
                        (PIECE_VALUE[PIECE_QUEEN] * 100 + (whiteRookControl[sq] + whiteBishopControl[sq]) / 8);
                pos.vlBlackPiecePos[PIECE_QUEEN][sq] = (short)
                        (PIECE_VALUE[PIECE_QUEEN] * 100 + (blackRookControl[sq] + blackBishopControl[sq]) / 8);
                pos.vlWhitePiecePos[PIECE_ROOK][sq] = (short)
                        (PIECE_VALUE[PIECE_ROOK] * 100 + whiteRookControl[sq] / 2);
                pos.vlBlackPiecePos[PIECE_ROOK][sq] = (short)
                        (PIECE_VALUE[PIECE_ROOK] * 100 + blackRookControl[sq] / 2);
                pos.vlWhitePiecePos[PIECE_BISHOP][sq] = (short)
                        (PIECE_VALUE[PIECE_BISHOP] * 100 + whiteBishopControl[sq] / 2);
                pos.vlBlackPiecePos[PIECE_BISHOP][sq] = (short)
                        (PIECE_VALUE[PIECE_BISHOP] * 100 + blackBishopControl[sq] / 2);
                // 4.6.骑士应该偏爱其攻击值
                int whiteKnightAttack = 0, blackKnightAttack = 0;
                for (int i = 0; i < 8; i++) {
                    int sqDst = sq + Position.KNIGHT_DELTA[i];
                    if (IN_BOARD(sqDst)) {
                        whiteKnightAttack += whiteAttack[sqDst];
                        blackKnightAttack += blackAttack[sqDst];
                    }
                }
                pos.vlWhitePiecePos[PIECE_KNIGHT][sq] = (short)
                        (PIECE_VALUE[PIECE_KNIGHT] * 100 + whiteKnightAttack / 4 - edgePenalty * 3 / 2);
                pos.vlBlackPiecePos[PIECE_KNIGHT][sq] = (short)
                        (PIECE_VALUE[PIECE_KNIGHT] * 100 + blackKnightAttack / 4 - edgePenalty * 3 / 2);
            }
            // 4.7.典当应优先考虑其头寸价值
            pos.vlWhitePiecePos[PIECE_PAWN][sq] = pos.vlBlackPiecePos[PIECE_PAWN][SQUARE_FLIP(sq)] =
                    (short) (PIECE_VALUE[PIECE_PAWN] * 100 + PAWN_VALUE[sq] / 2 - 6);
        }
        /*
         * 5. 计算典当结构的件值表
         *
         * 自：x P x - P = brForward，x = brLeftCover/brRightCover ^ x P x - P = brSelf，x = brSide | x P x - P = brSelf(最后), x = brChain
         *
         * 对手：
         *   . . .
         * ^. x 。 - x = brOppPass，或 BehindOppPass 如果前面有一个 Pawn | o o o - o = ~brSelf/brSide（最后）
         */
        for (int sd = 0; sd < 2; sd++) {
            int brSelf = 0, brSide = 0, brBehindOppPass = 0;
            int brOppPass = FULL_BIT_RANK;
            for (int i = 1; i <= 6; i++) {
                int y = (sd == 0 ? 7 - i : i);
                int brOpp = (sd == 0 ? pos.brBlackPawn[y] : pos.brWhitePawn[y]);
                brOppPass &= ~(brSelf | brSide);
                brBehindOppPass |= (brOppPass & brOpp);
                int brChain = brSide;
                brSelf = (sd == 0 ? pos.brWhitePawn[y] : pos.brBlackPawn[y]);
                brSide = ((brSelf >> 1) | (brSelf << 1)) & FULL_BIT_RANK;
                int brForward = (sd == 0 ? pos.brWhitePawn[y + 1] : pos.brBlackPawn[y + 1]);
                int brLeftCover = (brForward >> 1) & FULL_BIT_RANK;
                int brRightCover = (brForward << 1) & FULL_BIT_RANK;
                for (int x = Position.FILE_LEFT; x <= Position.FILE_RIGHT; x++) {
                    int sq = Position.COORD_XY(x, y);
                    int brSquare = 1 << x;
                    // 5.1.平行和受保护棋子的奖励
                    int value = ((brSide & brSquare) != 0 ? 3 : 0) + ((brChain & brSquare) != 0 ? 2 : 0);
                    // 5.2.可以保护其他棋子的棋子的奖励
                    value += ((brLeftCover & brSquare) != 0 ? 2 : 0) + ((brRightCover & brSquare) != 0 ? 2 : 0);

                    value += ((brSelf & brSquare) != 0 ? 1 : 0);
                    if (sd == 0) {
                        pos.vlWhitePiecePos[PIECE_PAWN][sq] += value;
                    } else {
                        pos.vlBlackPiecePos[PIECE_PAWN][sq] += value;
                    }
                    if (vlWhite + vlBlack <= 32) {
                        // 5.4.传递典当的奖励
                        if ((brOppPass & brSquare) != 0) {
                            if (sd == 0) {
                                pos.vlBlackPiecePos[PIECE_PAWN][sq] += PASS_PAWN[i];
                            } else {
                                pos.vlWhitePiecePos[PIECE_PAWN][sq] += PASS_PAWN[i];
                            }
                        }

                        if ((brBehindOppPass & brSquare) != 0) {
                            pos.vlWhitePiecePos[PIECE_ROOK][sq] += 8;
                            pos.vlBlackPiecePos[PIECE_ROOK][sq] += 8;
                            if (i == 6) {
                                int sqBottom = sq + Position.FORWARD_DELTA(sd);
                                pos.vlWhitePiecePos[PIECE_ROOK][sqBottom] += 8;
                                pos.vlBlackPiecePos[PIECE_ROOK][sqBottom] += 8;
                            }
                        }
                    }
                }
            }
        }
        // 6. 计算用主教阻挡中心棋子的惩罚
        for (int sq = 0x67; sq <= 0x68; sq++) {
            if (pos.squares[sq] == 8 + PIECE_PAWN) {
                pos.vlWhitePiecePos[PIECE_BISHOP][sq - 16] -= 10;
            }
        }
        for (int sq = 0x17; sq <= 0x18; sq++) {
            if (pos.squares[sq] == 16 + PIECE_PAWN) {
                pos.vlBlackPiecePos[PIECE_BISHOP][sq + 16] -= 10;
            }
        }
        // 7.更新“pos”中的“vlWhite”和“vlBlack”
        pos.vlWhite = pos.vlBlack = 0;
        for (int sq = 0; sq < 128; sq++) {
            int pc = pos.squares[sq];
            if (pc > 0) {
                if (pc < 16) {
                    pos.vlWhite += pos.vlWhitePiecePos[pc - 8][sq];
                } else {
                    pos.vlBlack += pos.vlBlackPiecePos[pc - 16][sq];
                }
            }
        }
    }

    public static int evaluate(Position pos, int vlAlpha, int vlBeta) {

        int vl = pos.material();
        if (vl + LAZY_MARGIN <= vlAlpha) {
            return vl + LAZY_MARGIN;
        } else if (vl - LAZY_MARGIN >= vlBeta) {
            return vl - LAZY_MARGIN;
        }
        // 2. 典当结构价值
        for (int sd = 0; sd < 2; sd++) {
            int brSingle = 0, brDouble = 0;
            int[] brs = (sd == 0 ? pos.brWhitePawn : pos.brBlackPawn);
            for (int i = 1; i <= 6; i++) {
                brDouble |= brSingle & brs[i];
                brSingle |= brs[i];
            }
            int brIsolated = brSingle & ~((brSingle << 1) | (brSingle >> 1));
            int penalty = Util.POP_COUNT_16(brDouble) * DOUBLE_PENALTY +
                    Util.POP_COUNT_16(brIsolated) * ISOLATED_PENALTY +
                    Util.POP_COUNT_16(brIsolated & brDouble) * ISOLATED_PENALTY * 2;
            vl += (pos.sdPlayer == sd ? -penalty : penalty);
        }
        return vl;
    }
}