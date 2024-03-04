package com.nowcoder.community.mapper;

import com.nowcoder.community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DiscussPostMapper {
    List<DiscussPost> selectDiscussPosts(@Param("userId") int userId, @Param("offset") int offset,@Param("limit") int limit);

    int getDiscussPostRows(@Param("userId") int userId);

    int addDiscussPort(DiscussPost discussPost);

    DiscussPost selectDiscussPostById(int id);

    /**
     * 更新帖子的评论数量
     * @param id 帖子id
     * @param commentCount 最新的帖子评论数量
     * @return
     */
    int updateDiscussPostComment(@Param("id") int id, @Param("commentCount") int commentCount);
}
