package com.nowcoder.leetcode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Test {
    public static void main(String[] args) {
       new Solution().rob(new int[]{2,7,9,3,1});
    }
}
class Solution {
    public int rob(int[] nums) {
        if (nums.length == 1) return nums[0];
        int[] dp = new int[nums.length + 1];
        dp[1] = nums[0];
        dp[2] = nums[1];
        for (int i = 3; i < dp.length; i++) {
            for (int j = i - 2; j >= 0; j--) {
                dp[i] = Math.max(dp[j] + nums[i - 1], dp[i - 1]);
            }

        }
        return dp[dp.length - 1];
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