package com.nowcoder.leetcode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Test {
    public static void main(String[] args) {
        System.out.println(Solution.function(new int[]{1,3,4},new int[] {15,20,30},4));
    }
}
class Solution {
    List<List<String>> res = new ArrayList<>();
    List<String> tmp = new ArrayList<>();
    public List<List<String>> solveNQueens(int n) {
        int[][] chessboard = new int[n][n];
        recur(n, 0, chessboard);
        return res;
    }
    public void recur(int n, int row, int[][] checkboard) {

        if (tmp.size() == n) {
            res.add(new ArrayList(tmp));
            return;
        }
        if (row == n) return;
        StringBuilder sb = new StringBuilder("");
        for(int i = 0; i < n - 1; i++) sb.append(".");
        for (int i = 0; i < n; i++) {
            if (i == 0 && row != 0) {
                if (checkboard[row - 1][i] == 1) continue;
            } else if (i != 0 && row != 0) {
                if (checkboard[row - 1][i - 1] == 1 || checkboard[row - 1][i] == 1) continue;
            }

            sb.insert(i, "Q");
            checkboard[row][i] = 1;
            tmp.add(sb.toString());
            recur(n, row + 1, checkboard);
            tmp.remove(tmp.size() - 1);
            checkboard[row][i] = 0;
        }
    }
    public static int function(int[] weights, int[] values, int bagWeight) {
        int[][] dp = new int[weights.length][bagWeight + 1];
        for(int j = weights[0]; j <= bagWeight; j++) {
            dp[0][j] = values[0];
        }
        for (int i = 1; i < weights.length; i++) {
            for (int j = 1; j <= bagWeight; j++) {
                if (j < weights[i]) dp[i][j] = dp[i - 1][j];
                else dp[i][j] = Math.max(dp[i - 1][j], dp[i - 1][j - weights[i]] + values[j]);
            }
        }
        return dp[weights.length -1][bagWeight];
    }
}