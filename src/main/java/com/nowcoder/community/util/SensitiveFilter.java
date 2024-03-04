package com.nowcoder.community.util;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.CharUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * 该工具是用来对内容进行敏感词过滤的，对敏感词文件中记录的敏感词都替换为***！
 * 加入IOC容器是为了方便管理和使用
 */
@Component
@Slf4j
public class SensitiveFilter {

    //内部类，即树结构，用来初始化前缀树
    private class TreeNode {
        //该标志为true，代表当前结点是叶子结点，即敏感词结点，否则就不是敏感词结点
        private boolean isSensitiveEnd = false;
        //这个前缀树并不是二叉树，所以用map来存储当前节点的子节点
        private Map<Character, TreeNode> subNodes = new HashMap<>();

        private void addSubNode(Character c, TreeNode node) {
            subNodes.put(c, node);
        }
        private TreeNode getSubNode(Character c) {
            return subNodes.get(c);
        }
    }
    //设置一个根结点属性，方便后续的使用
    private TreeNode root;
    //用来替换敏感词的字符串默认为***
    private static final String REPLACEMENT = "***";

    /**
     * 这里的init方法就是为了在该工具类实例化完毕之后（postconstruct注解的效果），就执行初始化方法，完成敏感词前缀树的构建
     */
    @PostConstruct
    public void init() {
        root = new TreeNode();
        try (
                InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("./sensitive-words.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream));
                ){
            String word = null;
            while((word = reader.readLine()) != null) {
                this.initTree(word);
            }
        } catch (Exception e) {
            log.error("读取敏感词文件出错了，错误信息：" + e.getMessage());
        }
    }

    /**
     * 该方法就是根据传入的敏感词完成前缀树的构建
     * @param word 敏感词
     */
    private void initTree(String word) {
        char c = ' ';
        TreeNode tmpNode = root;
        for (int i = 0; i < word.length(); i++) {
            c = word.charAt(i);
            TreeNode subNode = tmpNode.getSubNode(c);
            if (subNode == null) {
                subNode = new TreeNode();
                tmpNode.addSubNode(c, subNode);
            }
            tmpNode = subNode;
            if (i == word.length() - 1) {
                subNode.isSensitiveEnd = true;
            }
        }
    }

    /**
     * 该方法就是真正的完成敏感词的过滤的
     * @param text 要被过滤的原始文本
     * @return 过滤后的文本
     */
    public String sensitiveReplace(String text) {
        //指针1 用来遍历前缀树的结点
        TreeNode tmpNode = root;
        //指针2 用来记录字符串中敏感词的开头
        int begin = 0;
        //指针3 用来记录字符串中敏感词的结尾
        int end = 0;
        //StirngBuilder来构建字符串效率更高
        StringBuilder stringBuilder = new StringBuilder();
        while (end < text.length()) {
            char c = text.charAt(end);
            //如果当前字符是符号
            if (isSymbol(c)) {
                //判断指针1是否在开头，如果是在开头，表明是穿插的符号的敏感词的开始，此时不用跳过符号，将符号记录，然后end指针++
                if (tmpNode == root) {
                    stringBuilder.append(c);
                    begin++;
                }
                //如果指针1指向的不是根节点，表明该符号是穿插在敏感词之间的符号，那么这个符号就要跳过，因此不记录该符号，直接end++
                end++;
                //注意这里如果为符号后，指针更新完就要跳出循环，进行下一轮的循环
                continue;
            }
            TreeNode subNode = tmpNode.getSubNode(c);
            //如果当前字符对应的结点刚好是叶子节点，说明找到了敏感词，直接加入替换词，并将end++跳过敏感词，赋值给begin,且前缀树指针复位
            if (subNode != null && subNode.isSensitiveEnd) {
                tmpNode = root;
                stringBuilder.append(REPLACEMENT);
                begin = ++end;
                //如果当前字符对应的节点不是叶子结点，但在前缀树中，那么就end++,前缀树指针更新并继续向后判断
            } else if (subNode != null) {
                end++;
                tmpNode = subNode;
            }
            else {
                //如果当前字符在前缀树中没有对应的节点，就将begin至end范围内的字符加入字符串，并end后移赋值给begin
                stringBuilder.append(text.substring(begin, end + 1));
                begin = ++end;
                //这里注意更新完指针后不要忘记更新树的指针
                tmpNode = root;
            }

        }
        stringBuilder.append(text.substring(begin));
        return stringBuilder.toString();
    }

    /**
     * 该方法用来判断一个字符是否是正常字符（数字和字母）
     * @param c
     * @return
     */
    private boolean isSymbol(char c) {
        //这里字符的范围是东亚字符，所以将其排除在外
        return !CharUtils.isAsciiAlphanumeric(c) && (c < 0x2E80 || c > 0x9FFF);
    }
}
