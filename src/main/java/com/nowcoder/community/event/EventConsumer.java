package com.nowcoder.community.event;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.ElasticSearchService;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.util.CommunityConstants;
import com.nowcoder.community.util.CommunityUtil;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;

/**
 * 这就是事件的消费者
 */

@Slf4j
@Component
public class EventConsumer implements CommunityConstants {
    @Value("${wk.image.command}")
    private String wkImageCommand;
    @Value("${wk.image.storage}")
    private String wkImageStorage;
    @Value("${qiniu.key.access}")
    private String accessKey;
    @Value("${qiniu.key.secret}")
    private String secretKey;
    @Value("${qiniu.bucket.share.name}")
    private String shareBucketName;
    @Autowired
    private ThreadPoolTaskScheduler threadPoolTaskScheduler;
    @Autowired
    private MessageService messageService;
    @Autowired
    private ElasticSearchService elasticSearchService;
    @Autowired
    private DiscussPostService discussPostService;
    //因为这里点赞/评论/关注触发的事件包含的信息差不多，所以这里用一个方法来消费三个事件！
    @KafkaListener(topics = {TOPIC_COMMENT,TOPIC_FOLLOW,TOPIC_LIKE})
    public void handleEvents(ConsumerRecord record) {
        //处理业务严谨一些，使用数据先判空
        if (record == null || record.value() == null) {
            log.error("消息为空");
            return;
        }
        //注意学习解析json串到对象的方法！
        Event event = (Event) JSONObject.parseObject(record.value().toString(), Event.class);
        //判断解析到的对象是否为空,如果为空，代表消费到的消息不是一个json串，解析错误
        if (event == null) {
            log.error("消息的格式不正确");
            return;
        }
        //对象不为空，继续进行
        //这里因为事件触发业务是发送一个系统通知，所以将event对象转换为message对象存入数据库！
        Message message = new Message();
        message.setCreateTime(new Date());
        message.setFromId(SYSTEM_USER_ID);
        //这里注意消息的toId就是实体的拥有者的id，即EntityUserId
        message.setToId(event.getEntityUserId());
        message.setConversationId(event.getTopic());
        //对于event中多余的数据，统一存储到message的content中！
        //这里因为要根据系统通知的内容进行响应的展示，即xxx评论了你的xxx，所以需要存储实体类型以及实体id
        Map<String,Object> map = new HashMap<>();
        map.put("entityType", event.getEntityType());
        map.put("entityId", event.getEntityId());
        map.put("userId", event.getUserId());

        //如果event里的map存储的数据不为空，将这些数据也存储进来
        Map<String, Object> data = event.getData();
        if (data != null) {
            for (Map.Entry<String,Object> entry : data.entrySet()) {
                map.put(entry.getKey(), entry.getValue());
            }
            //如果事件对象中有帖子拥有者的id，表明该事件是回复某用户的回复触发的，那么除了给回复的用户一个通知外，还要给帖子的拥有者一个通知
            Integer discussPostOwnerId = (Integer) data.get("discussPostOwner");
            if (discussPostOwnerId != null) {
                Message message1 = new Message();
                message1.setFromId(SYSTEM_USER_ID);
                message1.setToId(discussPostOwnerId);
                message1.setCreateTime(new Date());
                message1.setConversationId(event.getTopic());
                Map<String, Object> postMap = new HashMap<>();
                postMap.put("userId", event.getUserId());
                postMap.put("entityType", COMMENT_TYPE_POST);
                postMap.put("entitiyId",data.get("postId"));
                for(Map.Entry<String,Object> entry : data.entrySet()) {
                    postMap.put(entry.getKey(), entry.getValue());
                }
                message1.setContent(JSONObject.toJSONString(postMap));
                messageService.insertMessage(message1);
            }
        }
        message.setContent(JSONObject.toJSONString(map));
        messageService.insertMessage(message);
    }

    /**
     * 该方法是消费因添加帖子/添加评论导致的需要向es中更新帖子数据而产生的事件
     * @param record
     */
    @KafkaListener(topics = ES_DISCUSSPOST_UPDATE)
    public void handlePostEvents(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            log.error("消息为空");
            return;
        }
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            log.error("消息的格式不正确");
            return;
        }
        //如果对象正确的转换，就获取事件中存储的帖子id属性，根据此id查找最新的帖子数据进行更新
        //因为在封装该事件对象时，实体就是帖子，当然实体id就是对应的帖子了！
        Integer postId = event.getEntityId();
        DiscussPost discussPost = discussPostService.selectDiscussPostById(postId);
        elasticSearchService.insertDiscussPost(discussPost);
    }

    /**
     * 该方法是消费由于删除帖子触发的事件，同步到es
     * @param record
     */
    @KafkaListener(topics = ES_DISCUSSPOST_DELETE)
    public void handlerDetelePost(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            log.error("消息不能为空");
        }
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            log.error("消息的格式不正确");
        }
        elasticSearchService.deleteDiscussPost(event.getEntityId());
    }

    /**
     * 消费分享图片触发的事件
     * @param record
     */
    @KafkaListener(topics = TOPIC_SHARE)
    public void handleShareMessages(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            throw new IllegalArgumentException("消息为空");
        }
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            throw new IllegalArgumentException("消息的格式不正确");
        }
        String htmlUrl = (String)event.getData().get("htmlUrl");
        String fileName = (String)event.getData().get("fileName");
        String suffix = (String)event.getData().get("suffix");

        String cmd = wkImageCommand + " --quality 75 " + htmlUrl + " "
                 + wkImageStorage + "/" + fileName + suffix;
        try {
            Runtime.getRuntime().exec(cmd);
            log.info("html成功转换为img，执行命令为：" + cmd);
        } catch (IOException e) {
            log.error("html转换为img失败,错误原因为:" + e.getMessage());
        }
        //对方法进行重构，将本地的图片上传到云服务器，就可以删除本地的图片了！
        //这里注意exec是把任务交给操作系统执行，然后自己往下执行是一个异步的操作，因此try代码块的执行可能早于图片的生成
        //因此这里我们不能直接编写代码逻辑，而是要等待图片生成成功在进行上传
        //等待exec完成，又不能直接线程睡眠阻塞，这样是影响系统性能，这时就可以考虑使用定时任务，单开一个线程用来尝试上传图片
        //如果图片还没有生成成功，就上传失败，从而隔一段时间后重试！
        //而这里由于消息队列同一消费者组的消费者只能有一个获取消息，因此可以使用线程池避免分布式问题！
        UploadTaks uploadTaks = new UploadTaks(fileName, suffix);
        //延迟500ms执行任务 这里返回的future对象封装的是线程的执行结果，并且可以终止线程！
        Future future = threadPoolTaskScheduler.scheduleAtFixedRate(uploadTaks, 500);
        uploadTaks.setFuture(future);
    }
    //这里因为上传任务的逻辑是比较复杂的，所以我们专门编写一个线程体而不是采用匿名创建线程体的方式！
    class UploadTaks implements Runnable{
        //上传文件需要文件名以及文件的后缀名
        private String fileName;
        private String suffix;
        //需要记录任务开始的时间，到30s没有成功就放弃任务
        private long startTime;
        //需要记录任务上传图片的次数，超过三次失败也取消
        private int uploadTimes;
        //用来终止线程执行任务
        private Future future;
        //利用构造函数完成属性的初始化
        public UploadTaks(String fileName, String suffix) {
            this.fileName = fileName;
            this.suffix = suffix;
            this.startTime = System.currentTimeMillis();
        }

        public void setFuture(Future future) {
            this.future = future;
        }

        @Override
        public void run() {
            //首先判断线程执行任务是否冲过30s了，若是就直接终止任务，可能的原因有图片生成失败/网络原因/服务器原因上传失败
            if ((System.currentTimeMillis() - startTime) > 30000) {
                log.error("执行时间过长，任务终止，上传的文件名为:" + fileName);
                //终止线程执行任务
                future.cancel(true);
                return;
            }
            //如果图片生成成功，但是上传一直失败，达到3次也终止
            if (uploadTimes == 3) {
                log.error("上传次数过多，终止任务，上传的文件名为:" + fileName);
                future.cancel(true);
                return;
            }
            //如果不满足终止条件，就继续尝试上传图片到云服务器
            //判断图片是否生成成功
            String path = wkImageStorage + "/" + fileName + suffix;
            File file = new File(path);
            if (file.exists()) {
                //如果图片生成成功了，进行上传，这里的上传逻辑和客户端上传是类似的
                //设置响应信息
                StringMap policy = new StringMap();
                policy.put("returnBody", CommunityUtil.getJsonString(0));
                //生成凭证
                Auth auth = Auth.create(accessKey, secretKey);
                String uploadToken = auth.uploadToken(shareBucketName, fileName, 3600, policy);
                //指定上传机房
                UploadManager manager = new UploadManager(new Configuration(Region.region2()));
                try {
                    //进行上传，获取响应结果
                    Response response = manager.put(
                            path,fileName,uploadToken,null,"image/"+suffix,false
                    );
                    //响应结果转换为json
                    JSONObject json = JSONObject.parseObject(response.bodyString());
                    if (json == null || json.get("code") == null || !json.get("code").toString().equals("0")) {
                        //如果失败，打印错误日志
                        log.info(String.format("第%d次上传失败，上传的文件名为：%s", ++uploadTimes, fileName));
                    } else {
                        //如果成功，打印日志，停止任务的执行
                        log.info(String.format("第%d次上传成功，上传的文件名为：%s", ++uploadTimes, fileName));
                        future.cancel(true);
                    }
                } catch (QiniuException e) {
                    log.info(String.format("第%d次上传失败，上传的文件名为：%s", ++uploadTimes, fileName));
                }
            } else {
                log.info("等待图片生成,生成的文件名为:" + fileName);
            }
        }
    }

}
