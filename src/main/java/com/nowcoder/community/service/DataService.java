package com.nowcoder.community.service;

import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 统计网站UV和DAU数据的业务类
 */
@Service
public class DataService {
    @Autowired
    private RedisTemplate redisTemplate;
    //设置日期格式化器，方便之后业务的复用
    private SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");

    /**
     * 存储当天UV数据，因为要使用用户的ip进行统计，这里传入的就是ip，利用hyperloglog进行统计
     * @param ip 当前访问网站的用户的ip
     */
    public void setUV(String ip) {
        //生成单日UVkey
        String uv = RedisKeyUtil.getUV(df.format(new Date()));
        redisTemplate.opsForHyperLogLog().add(uv, ip);
    }

    /**
     * 获取一段时间内的UV总量,如果是同一天，那么start=end
     * @param start 起始日期
     * @param end 结束日期
     * @return 该段时间内的UV访问量
     */
    public Long getUV(Date start, Date end) {
        //对传入的数据进行合法判断
        if (start == null || end == null) {
            throw new IllegalArgumentException("参数不能为空！");
        }
        //初始化集合，存放这段时间内的所有日期的单日UVkey
        List<String> keyList = new ArrayList<>();
        //通过calendar类来进行日期之间的计算
        Calendar calendar = Calendar.getInstance();
        //设置当前日期时间为起始时间
        calendar.setTime(start);
        //如果当前日期在end日期之前
        while (!calendar.getTime().after(end)) {
            //生成单日UVkey
            String uv = RedisKeyUtil.getUV(df.format(calendar.getTime()));
            //往后加一天 DATE指的就是天，1就是加一天！
            calendar.add(Calendar.DATE, 1);
            keyList.add(uv);
        }

        //生成多日UVkey
        String uv = RedisKeyUtil.getUV(df.format(start), df.format(end));
        //查询多日UV数据，存入UVkey
        //注意union方法的可变参数就是Object可变参数，等同于Object数组，而list.toArray如果不传入任何参数，返回的值就是Object数组，所以这里不会报错！
        redisTemplate.opsForHyperLogLog().union(uv, keyList.toArray());
        //查询最终结果的数量,用long防止数据量太大
        Long size = redisTemplate.opsForHyperLogLog().size(uv);
        return size;
    }

    /**
     * 该方法利用bitmap来存储网站的当日DAU，利用用户的id作为map的索引,注意这里的活跃用户判定为访问一次网站就算！
     * @param userId 用户的id
     */
    public void setDAU(int userId) {
        //获取单日daukey
        String dau = RedisKeyUtil.getDAU(df.format(new Date()));
        //记录用户活跃情况
        redisTemplate.opsForValue().setBit(dau, userId, true);
    }

    /**
     * 获取一段时间内的DAU数据，注意利用的bitmap存储的DAU，所以统计多日的DAU数据，使用的是每日的DAU数据做位或运算即可！！
     * @param start
     * @param end
     * @return
     */
    public Long getDAU(Date start, Date end) {
        //对传入的数据进行合法判断
        if (start == null || end == null) {
            throw new IllegalArgumentException("参数不能为空！");
        }
        //因为要利用位或运算统计多日的DAU，而位运算的方法，传入的key必须是byte数组，因此这里初始化byte数组集合
        List<byte[]> keyList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        //统计时间范围内的单日DAUkey加入集合
        while(!calendar.getTime().after(end)) {
            String dau = RedisKeyUtil.getDAU(df.format(calendar.getTime()));
            keyList.add(dau.getBytes());
            calendar.add(Calendar.DATE, 1);
        }



        //查询最终结果的数量,用long防止数据量太大
        Long execute = (Long) redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                String dau = RedisKeyUtil.getDAU(df.format(start), df.format(end));
                //多日的DAU数据进行或运算并将结果存储到多日DAUkey中
                //注意这里bitOp的方法需要的可变参数是byte数组，即等同于一个二维的数组，每一维都是一个单日DAUkey的byte数组
                //而list.toArray方法默认是返回Object数组的，所以想要指定返回的数组的类型，就在toArray（传入一个指定类型的空数组）
                connection.bitOp(RedisStringCommands.BitOperation.OR, dau.getBytes(), keyList.toArray(new byte[0][0]));
                //根据多日DAUkey统计查询出多日DAU数据！
                return connection.bitCount(dau.getBytes());
            }
        });
        return execute;
    }
}
