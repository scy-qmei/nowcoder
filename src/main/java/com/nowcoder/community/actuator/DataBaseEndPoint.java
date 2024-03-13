package com.nowcoder.community.actuator;

import com.nowcoder.community.util.CommunityUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 对数据库连接情况的监控端点！
 */
@Component
@Endpoint(id = "database")
@Slf4j
public class DataBaseEndPoint {
    @Autowired
    private DataSource dataSource;

    @ReadOperation
    public String checkConnection() {
        try (
                Connection connection = dataSource.getConnection();
                ){
            return CommunityUtil.getJsonString(0,"获取连接成功");
        } catch (SQLException e) {
             e.printStackTrace();
             log.error("获取连接失败:" + e.getMessage());
             return CommunityUtil.getJsonString(1, "获取连接失败");
        }
    }
}
