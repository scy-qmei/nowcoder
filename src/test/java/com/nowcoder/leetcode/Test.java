package com.nowcoder.leetcode;

import java.util.ArrayList;
import java.util.List;

public class Test {
    public static void main(String[] args) {
        new Solution().restoreIpAddresses("25525511135");
    }
}
class Solution {
    List<String> res = new ArrayList<>();
    StringBuilder sb = new StringBuilder();
    public List<String> restoreIpAddresses(String s) {
        if (s.length() < 4) return res;
        recur(s, 0, 4);
        return res;
        
    }
    public void recur(String s, int index, int count) {
        if (count == 1 && Integer.parseInt(s.substring(index, s.length())) > 255) return;
        if (index == s.length()) {
            res.add(new String(sb));
            res.remove(res.size() - 1);
            return;
        }
        for (int i = index; i < s.length(); i++) {
            if (Integer.parseInt(s.substring(index, i + 1)) >= 0
                    && Integer.parseInt(s.substring(index, i + 1)) <= 255) {
                sb.append(s.substring(index, i + 1) + ".");
            }
            recur(s, i + 1, count - 1);
            sb.deleteCharAt(sb.length() - 1);
            sb.deleteCharAt(sb.length() - 1);
        }
    }
}