package com.nowcoder.community.mapper;

import com.nowcoder.community.entity.LoginTicket;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 登录凭证的数据访问层
 */
@Mapper
@Deprecated
public interface LoginTicketMapper {
    /**
     * 向数据库中插入一条新的登录凭证
     * @param loginTicket
     * @return
     */
    int insertTicket(LoginTicket loginTicket);

    /**
     * 这里是根据ticket的内容来查询登录凭证，从而识别客户端的id
     * @param ticket
     * @return
     */
    LoginTicket selectTicketByTicket(String ticket);

    /**
     * 更新登录凭证的状态，0是有效，1是无效
     * @param ticket
     * @param status
     * @return
     */
    int updateTicketByStatus(@Param("ticket") String ticket, @Param("status") int status);
}
