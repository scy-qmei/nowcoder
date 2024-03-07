package com.nowcoder.community.service;

import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

/**
 * 点赞业务
 */
@Service
public class LikeService {
    @Autowired
    private RedisTemplate redisTemplate;

//    /**
//     * 该方法的用处就是进行点赞功能
//     * @param userId 点赞的用户
//     * @param entityType 点赞的目标类型
//     * @param entityId 点赞的目标的id
//     */
//    public void like(int userId, int entityType, int entityId) {
//        //根据点赞的类型及其id生成key
//        String key = RedisKeyUtil.generateKey(entityType, entityId);
//        //查询当前用户是否已经点过赞
//        //这里用set存储key对应的用户id是为了方便以后查询当前帖子是谁点赞了
//        Boolean member = redisTemplate.opsForSet().isMember(key, userId);
//        //已经点过赞的话，再次点赞就是取消点赞,没有点过赞的话就进行点赞
//        if (member) {
//            redisTemplate.opsForSet().remove(key, userId);
//        } else {
//            redisTemplate.opsForSet().add(key, userId);
//        }
//    }

    /**
     *该方法是上面的点赞方法的重构版本，添加了更新用户受到的点赞数量的逻辑
     * 注意该方法里有多个redis操作语句，所以要使用事务
     * @param userId 点赞用户id
     * @param entityType 点赞的实体类型
     * @param entityId 点赞的实体的id
     * @param entityUserId 实体的发布者的id (这里选择传入实体发布者id而不是通过实体进行数据库查询得到，是因为既然使用了redis，就要避免再使用mysql降低性能)
     */
    public void like(int userId, int entityType, int entityId, int entityUserId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                //生成实体id和用户id，用来存储给实体点赞的用户id和用户受到的赞的数量
                String userLikeKey = RedisKeyUtil.generateUserLikeKey(entityUserId);
                String entityLikeKey = RedisKeyUtil.generateKey(entityType, entityId);
                //查看当前用户是否对该实体点赞,注意这是一个查询语句，所以不能再事务里执行，否则不会进行查询！
                Boolean member = redisTemplate.opsForSet().isMember(entityLikeKey, userId);
                //下面的语句涉及多次的redis操作，所以需要开启事务
                operations.multi();
                if (member) {
                    //如果已经点过赞了，那么这次操作就是取消赞，同时还要对该实体用户受到赞的个数进行－1
                    redisTemplate.opsForSet().remove(entityLikeKey, userId);
                    //存储用户受到的赞的数量，用字符串存储即可，这里取消赞，所以进行decr操作进行减一
                    redisTemplate.opsForValue().decrement(userLikeKey);
                } else {
                    //如果还没有点赞，那么就对该实体点赞，同时对该实体的发布者的点赞数量进行加一
                    redisTemplate.opsForSet().add(entityLikeKey, userId);
                    redisTemplate.opsForValue().increment(userLikeKey);
                }
                //提交事务
                return operations.exec();
            }
        });


    }

    /**
     * 该方法是获取当前实体的点赞数量
     * @param entityType 实体类型
     * @param entityId 实体的id
     * @return
     */
    public Long getLikeCount(int entityType, int entityId) {
        String key = RedisKeyUtil.generateKey(entityType, entityId);
        return redisTemplate.opsForSet().size(key);
    }

    /**
     * 该方法是获取当前用户对当前实体的点赞状态，是或者否
     * @param userId
     * @param entityType
     * @param entityId
     * @return 这里返回的int方便之后的功能扩展，是/否/踩
     */
    public int getLikeStatus(int userId, int entityType, int entityId) {
        String key = RedisKeyUtil.generateKey(entityType, entityId);
        return redisTemplate.opsForSet().isMember(key, userId) ? 1 : 0;
    }

    /**
     * 获取用户受到的赞的数量
     * @param userId 用户的id
     * @return
     */
    public int getUserLikeCount(int userId) {
        String userLikeKey = RedisKeyUtil.generateUserLikeKey(userId);
        //注意这里不能用下面的方法获取点赞数量，因为可能该key不存在，那么返回的就是null，此时发生类型转换会报错
//        int i = Integer.parseInt(redisTemplate.opsForValue().get(userLikeKey).toString());
        //因此应该用包装类来接受返回结果
        Integer count = (Integer) redisTemplate.opsForValue().get(userLikeKey);
        //由于返回的结果可能为null，所以返回值要做特殊的处理
        return count == null ? 0 : count;
    }

}
