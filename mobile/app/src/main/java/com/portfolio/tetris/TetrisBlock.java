package com.portfolio.tetris;

import android.graphics.Color;

public class TetrisBlock {

    // 블록 타입
    public static final int I = 0;
    public static final int O = 1;
    public static final int T = 2;
    public static final int S = 3;
    public static final int Z = 4;
    public static final int J = 5;
    public static final int L = 6;

    // 블록 색상 (이미지 참고)
    public static final int[] COLORS = {
        Color.parseColor("#9B6BD4"), // I - 보라
        Color.parseColor("#E86BB5"), // O - 핑크
        Color.parseColor("#7DD87A"), // T - 초록
        Color.parseColor("#E8857A"), // S - 연어
        Color.parseColor("#C8D44A"), // Z - 연두
        Color.parseColor("#4DD0C4"), // J - 청록
        Color.parseColor("#F5C542"), // L - 노랑
    };

    // 각 블록 모양 (4가지 회전 상태)
    public static final int[][][][] SHAPES = {
        // I
        {
            {{0,0,0,0},{1,1,1,1},{0,0,0,0},{0,0,0,0}},
            {{0,0,1,0},{0,0,1,0},{0,0,1,0},{0,0,1,0}},
            {{0,0,0,0},{0,0,0,0},{1,1,1,1},{0,0,0,0}},
            {{0,1,0,0},{0,1,0,0},{0,1,0,0},{0,1,0,0}}
        },
        // O
        {
            {{0,1,1,0},{0,1,1,0},{0,0,0,0},{0,0,0,0}},
            {{0,1,1,0},{0,1,1,0},{0,0,0,0},{0,0,0,0}},
            {{0,1,1,0},{0,1,1,0},{0,0,0,0},{0,0,0,0}},
            {{0,1,1,0},{0,1,1,0},{0,0,0,0},{0,0,0,0}}
        },
        // T
        {
            {{0,1,0,0},{1,1,1,0},{0,0,0,0},{0,0,0,0}},
            {{0,1,0,0},{0,1,1,0},{0,1,0,0},{0,0,0,0}},
            {{0,0,0,0},{1,1,1,0},{0,1,0,0},{0,0,0,0}},
            {{0,1,0,0},{1,1,0,0},{0,1,0,0},{0,0,0,0}}
        },
        // S
        {
            {{0,1,1,0},{1,1,0,0},{0,0,0,0},{0,0,0,0}},
            {{0,1,0,0},{0,1,1,0},{0,0,1,0},{0,0,0,0}},
            {{0,0,0,0},{0,1,1,0},{1,1,0,0},{0,0,0,0}},
            {{1,0,0,0},{1,1,0,0},{0,1,0,0},{0,0,0,0}}
        },
        // Z
        {
            {{1,1,0,0},{0,1,1,0},{0,0,0,0},{0,0,0,0}},
            {{0,0,1,0},{0,1,1,0},{0,1,0,0},{0,0,0,0}},
            {{0,0,0,0},{1,1,0,0},{0,1,1,0},{0,0,0,0}},
            {{0,1,0,0},{1,1,0,0},{1,0,0,0},{0,0,0,0}}
        },
        // J
        {
            {{1,0,0,0},{1,1,1,0},{0,0,0,0},{0,0,0,0}},
            {{0,1,1,0},{0,1,0,0},{0,1,0,0},{0,0,0,0}},
            {{0,0,0,0},{1,1,1,0},{0,0,1,0},{0,0,0,0}},
            {{0,1,0,0},{0,1,0,0},{1,1,0,0},{0,0,0,0}}
        },
        // L
        {
            {{0,0,1,0},{1,1,1,0},{0,0,0,0},{0,0,0,0}},
            {{0,1,0,0},{0,1,0,0},{0,1,1,0},{0,0,0,0}},
            {{0,0,0,0},{1,1,1,0},{1,0,0,0},{0,0,0,0}},
            {{1,1,0,0},{0,1,0,0},{0,1,0,0},{0,0,0,0}}
        }
    };

    public int type;
    public int rotation;
    public int x, y;

    public TetrisBlock(int type) {
        this.type = type;
        this.rotation = 0;
        this.x = 3;
        this.y = 0;
    }

    public int[][] getShape() {
        return SHAPES[type][rotation];
    }

    public int getColor() {
        return COLORS[type];
    }

    public void rotate() {
        rotation = (rotation + 1) % 4;
    }

    public void rotateBack() {
        rotation = (rotation + 3) % 4;
    }

    public static int randomType() {
        return (int)(Math.random() * 7);
    }
}
