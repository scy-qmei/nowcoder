package com.nowcoder.community.service;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.mapper.elasticsearch.DiscussPostRepository;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ElasticSearchService {

    @Autowired
    private DiscussPostRepository discussPostRepository;
    @Autowired
    private ElasticsearchRestTemplate restTemplate;

    /**
     * 该方法将向es中插入数据和更新数据合二为一，因为更新数据的本质就是对原始数据的覆盖！
     * @param discussPost
     */
    public void insertDiscussPost(DiscussPost discussPost) {
        discussPostRepository.save(discussPost);
    }

    /**
     * 该方法实现的是根据帖子的id从es中删除帖子
     * @param discussPostId
     */
    public void deleteDiscussPost(int discussPostId) {
        discussPostRepository.deleteById(discussPostId);
    }

    /**
     * 该方法是根据关键字从es中搜索含有关键字的帖子并分页显示，这里注意es搜索的结果是匹配关键词的一段话，而不是整个帖子！
     * @param keyword 关键词
     * @param current 当前的页数，es中第一页是0
     * @param limit 一页显示的帖子的数量
     * @return 返回封装数据的map，两个key一个key是分页的帖子数据集合，一个key是符合条件的所有帖子数量
     */
    public Map<String,Object> searchByKeyword(String keyword, int current, int limit) {
        NativeSearchQuery build = new NativeSearchQueryBuilder()
                //设置查询的关键字，以及需要搜索的实体的值对应的属性
                .withQuery(QueryBuilders.multiMatchQuery(keyword, "content", "title"))
                //首先对帖子的类型进行降序排序，即是否置顶
                .withSorts(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                //如果类型一样，根据是否加精获取帖子的分数进行降序排序
                .withSorts(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                //如果前两者都一样，根据帖子的创建时间进行降序排序
                .withSorts(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                //设置分页属性，注意es的分页索引是从0开始的
                .withPageable(PageRequest.of(current, limit))
                //设置需要显示高亮的属性，并且给搜索到关键词的地方添加前后缀，以便前段进行样式的显示
                .withHighlightFields(
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>")
                ).build();
        //根据创建出的查询以及es中索引对应的实体的class类，即可获得搜索结果
        SearchHits<DiscussPost> search = restTemplate.search(build, DiscussPost.class);
        List<DiscussPost> discussPosts = new ArrayList<>();
        //对搜索结果进行遍历，一个searchhit就是一个命中对象！！！包含了搜索得到的贴子及其字段的值等详细信息
        for (SearchHit<DiscussPost> searchHit : search) {
            DiscussPost content = searchHit.getContent();
            //注意es的返回结果是没有对搜索到的关键次进行高亮显示的，需要我们自己设置高亮
            //查询高亮属性的title的结果，因为可能一个标题有很多匹配到的结果，如果不为空这里我们选取第一个进行赋值即可！
            List<String> title = searchHit.getHighlightFields().get("title");
            //这里如果没有匹配到的结果，那么title集合就为空牢记！！！
            if (title != null) {
                content.setTitle(title.get(0));
            }
            List<String> content1 = searchHit.getHighlightFields().get("content");
            if (content1 != null) {
                content.setContent(content1.get(0));
            }
            //至此高亮设置完毕，添加进结果集
            discussPosts.add(content);
        }
        Map<String, Object> map = new HashMap<>();
        map.put("discussPosts", discussPosts);
        map.put("count", search.getTotalHits());
        return map;
    }
}
