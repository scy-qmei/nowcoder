package com.nowcoder.community.mapper.elasticsearch;

import com.nowcoder.community.entity.DiscussPost;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.PriorityQueue;

/***
 * 这里自定义我们需要的es仓库，实现es接口，指明与索引映射的实体类以及这个实体的主键的类型
 */
@Repository
public interface DiscussPostRepository extends ElasticsearchRepository<DiscussPost, Integer> {

}
