package com.nowcoder.community.mapper;

import com.nowcoder.community.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CommentMapper {

    /**
     * 该方法是根据评论的对象的类型以及对象的id，从而分页获取当前评论的集合
     * @param entityType 评论的对象的类型
     * @param entityId 评论的对象的id
     * @param offset 当前页的起始行
     * @param limit 当前页的评论数
     * @return 当前页的评论的集合
     */
    List<Comment> selectCommentByEntity(@Param("entityType") int entityType,
                                        @Param("entityId") int entityId,
                                        @Param("offset") int offset,
                                        @Param("limit") int limit);

    /**
     * 该方法是根据回复的对象的类型以及对象的id获取该对象共有多少条回复
     * @param entityType 对象的类型
     * @param entityId 对象的id
     * @return 对象的评论的个数
     */
    int selectCommentCountByEntity(@Param("entityType") int entityType,
                                   @Param("entityId") int entityId);

    /**
     * 插入一个评论记录
     * @param comment
     * @return
     */
    int insertComment(Comment comment);
}
