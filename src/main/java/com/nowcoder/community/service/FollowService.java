package com.nowcoder.community.service;

import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityConstants;
import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 关注业务
 */
@Service
public class FollowService implements CommunityConstants {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private UserService userService;

    /**
     * 关注用户的业务，关注成功，既要更新关注列表，又要更新被关注用户的粉丝列表
     * @param userId
     * @param entityType
     * @param entityId
     */
    public void follow(int userId, int entityType, int entityId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String followeekey = RedisKeyUtil.generateFolloweeKey(userId, entityType);
                String followerKey = RedisKeyUtil.generateFollowerKey(entityType, entityId);

                operations.multi();
                //如果关注了，既要更新用户关注的id列表，又要更新被关注的用户的粉丝列表，在一个redis业务涉及了多个操作，所以就需要开启事务！
                //因为这里使用了有序集合，要给值一个分数，以便后序按顺序查看关注列表，所以这里就将关注的时间转化为毫秒数作为分数，方便排序！
                redisTemplate.opsForZSet().add(followeekey, entityId, System.currentTimeMillis());
                redisTemplate.opsForZSet().add(followerKey,userId,System.currentTimeMillis());
                return operations.exec();
            }
        });
    }

    /**
     * 取消关注的功能，和关注一样，有两部操作
     * @param userId
     * @param entityType
     * @param entityId
     */
    public void unfollow(int userId, int entityType, int entityId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String followeekey = RedisKeyUtil.generateFolloweeKey(userId, entityType);
                String followerKey = RedisKeyUtil.generateFollowerKey(entityType, entityId);

                operations.multi();
                //如果取消关注了，既要更新用户关注的id列表，又要更新被关注的用户的粉丝列表，在一个redis业务涉及了多个操作，所以就需要开启事务！
                //删除有序集合的元素，指明key即可，不用指明分数
                redisTemplate.opsForZSet().remove(followeekey, entityId);
                redisTemplate.opsForZSet().remove(followerKey,userId);
                return operations.exec();
            }
        });
    }

    /**
     * 该业务方法是查看当前用户对指定实体的关注状态
     * @param userId 当前用户id
     * @param entityType 实体类型
     * @param entityId 实体ID
     * @return 是否关注
     */
    public boolean getFollowStatus(int userId, int entityType, int entityId) {
        String s = RedisKeyUtil.generateFolloweeKey(userId, entityType);
        //这里根据集合中查找实体id对应的分数，如果实体id不存在，那么返回的分数就是null，如果实体id存在，返回的就不是null！
        //注意有序集合查询分数的用法，集合的key，元素值，返回的是元素值的分数
        return redisTemplate.opsForZSet().score(s,entityId) != null;
    }

    /**
     * 获取当前用户关注的指定实体的数量
     * @param userId 当前用户id
     * @param entityType 关注的实体的类型
     * @return 注意有序集合和集合的统计大小的返回值都是long
     */
    public long getFolloweeCount(int userId, int entityType) {
        String s = RedisKeyUtil.generateFolloweeKey(userId, entityType);
        return redisTemplate.opsForZSet().zCard(s);
    }

    /**
     * 根据当前实体类型以及实体id获取关注该实体的粉丝数量
     * @param entityType 实体的类型
     * @param entityId 实体的id
     * @return 实体的粉丝数量
     */
    public long getFollowerCount(int entityType, int entityId) {
        String followerKey = RedisKeyUtil.generateFollowerKey(entityType, entityId);
        return redisTemplate.opsForZSet().zCard(followerKey);
    }


    /**
     * 查询用户的关注的用户的列表，注意这里不是实体，因为不同的实体，获取列表后的转换方式是不同的，所以这里分开写！
     * @param userId 用户的id
     * @param offset 分页属性
     * @param limit 分页属性
     * @return 封装的集合，元素是map，map存储的就是用户以及关注的时间等数据，以供前端进行显示！
     */
    public List<Map<String,Object>> getFolloweeList(int userId, int offset, int limit) {
        String followeeKey = RedisKeyUtil.generateFolloweeKey(userId, ENTITY_TYPE_USER);
        //redis的分页查询的实现，牢记！
        //redis的分页查询其实就range方法，传入一个查询范围，得到的是范围内的排好序的元素set集合
        //offset是起始行，limit是每页的数量，由于range是左闭右闭的，所以要减一！
        Set<Integer> ids = redisTemplate.opsForZSet().reverseRange(followeeKey, offset, offset + limit - 1);
        //使用数据前一定要对数据做好非法判断!
        if (ids == null) {
            return null;
        }
        //得到了用户的关注的id列表，即可通过该列表转为关注者列表！
        List<Map<String,Object>> list = new ArrayList<>();
        for (int id : ids) {
            Map<String,Object> map = new HashMap<>();
            //根据id得到关注者
            User followee = userService.getUserById(id);
            map.put("followee",followee);
            //还需要得到关注时间，其实就是有序集合的score
            Double score = redisTemplate.opsForZSet().score(followeeKey, id);
            //将得到的毫秒数转为Date
            Date date = new Date(score.longValue());
            map.put("followTime", date);

            list.add(map);
        }
        return list;
    }

    /**
     * 该业务是分页查询当前用户的粉丝列表，总体逻辑与关注者列表一样
     * @param userId
     * @param offset
     * @param limit
     * @return
     */
    public List<Map<String,Object>> getFollowerList(int userId, int offset, int limit) {
        String followerKey = RedisKeyUtil.generateFollowerKey(ENTITY_TYPE_USER, userId);
        //redis的分页查询的实现，牢记！
        //redis的分页查询其实就range方法，传入一个查询范围，得到的是范围内的排好序的元素set集合
        //offset是起始行，limit是每页的数量，由于range是左闭右闭的，所以要减一！
        Set<Integer> ids = redisTemplate.opsForZSet().reverseRange(followerKey, offset, offset + limit - 1);
        //使用数据前一定要对数据做好非法判断!
        if (ids == null) {
            return null;
        }
        //得到了用户的关注的id列表，即可通过该列表转为关注者列表！
        List<Map<String,Object>> list = new ArrayList<>();
        for (int id : ids) {
            Map<String,Object> map = new HashMap<>();
            //根据id得到关注者
            User follower = userService.getUserById(id);
            map.put("follower",follower);
            //还需要得到关注时间，其实就是有序集合的score
            Double score = redisTemplate.opsForZSet().score(followerKey, id);
            //将得到的毫秒数转为Date
            Date date = new Date(score.longValue());
            map.put("followTime", date);

            list.add(map);
        }
        return list;
    }
}
