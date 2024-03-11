package com.nowcoder.community.entity;


import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;

//这里建立实体对应的索引的关系！
@Document(indexName = "discusspost")
@Data
public class DiscussPost {
    @Id
    private Integer id;
    @Field(type = FieldType.Integer)
    private Integer userId;
    //这里type表明我们的标题字段是文本类型的，也就是搜索引擎需要检索的
    //这里设置存储分析器为最大ik，即将标题拆分为可以组成的最多单词的个数，然后存储进es中
    //这里设置搜索分析器为聪明分析器，即可以根据我们输入的关键词拆分出智能的词组！
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String content;
    @Field(type = FieldType.Integer)
    private Integer type;
    @Field(type = FieldType.Integer)
    private Integer status;
    @Field(type = FieldType.Date)
    private Date createTime;
    @Field(type = FieldType.Integer)
    private Integer commentCount;
    @Field(type = FieldType.Double)
    private double score;
}
