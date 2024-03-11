package com.nowcoder.community;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.mapper.elasticsearch.DiscussPostRepository;
import com.nowcoder.community.service.DiscussPostService;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

@SpringBootTest
public class ElasticSearchTest {
    @Autowired
    private DiscussPostRepository discussPostRepository;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Test
    public void testUpdate() {
        DiscussPost discussPost = discussPostService.selectDiscussPostById(231);
        discussPost.setContent("dffddsdssdfds");
        discussPostRepository.save(discussPost);
    }
    @Test
    public void testDelete() {
        discussPostRepository.delete(discussPostService.selectDiscussPostById(231));
    }
    @Test
    public void testSearch() {
        NativeSearchQuery build = new NativeSearchQueryBuilder()
                //设置搜索的关键字以及搜索的字段
                .withQuery(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content"))
                //搜搜结果先对type进行倒序排序
                .withSorts(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                //type相同的按照score进行倒序排序
                .withSorts(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                //前两者都相同的按照创建时间排序
                .withSorts(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                //设置分页属性，第一页，显示十个数据
                .withPageable(PageRequest.of(0, 10))
                //设置高亮属性，查询到的结果前后加一个em标签！可以自定义样式进行高亮显示！
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build();

        SearchHits<DiscussPost>search = elasticsearchRestTemplate.search(build, DiscussPost.class);
        System.out.println(search.getTotalHits());
        List<DiscussPost> discussPosts = new ArrayList<>();
        for (SearchHit<DiscussPost> hit : search) {

            DiscussPost content = hit.getContent();
            //设置高亮的两种方法！
//            List<String> title = hit.getHighlightFields().get("title");
//            if (!title.isEmpty()) {
//                content.setTitle(title.get(0));
//            }
            Map<String, List<String>> highlightFields = hit.getHighlightFields();
            if (highlightFields.containsKey("title")) {
                content.setTitle(highlightFields.get("title").get(0));
            }
            if (highlightFields.containsKey("content")) {
                content.setContent(highlightFields.get("content").get(0));
            }
            discussPosts.add(content);
        }
        for (DiscussPost discussPost : discussPosts) {
            System.out.println(discussPost);
        }
        PriorityQueue<Object> objects = new PriorityQueue<>();
        
    }
}
