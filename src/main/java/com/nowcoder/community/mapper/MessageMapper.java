package com.nowcoder.community.mapper;

import com.nowcoder.community.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MessageMapper {
    /**
     * 根据当前用户的id查询当前用户的会话列表，进行分页显示，每个回话显示的是最后一条消息
     * @param userId 用户id
     * @param offset 当前页起始行
     * @param limit 当前页显示数量
     * @return 当前页的回话消息集合
     */
    List<Message> selectConversationList(@Param("userId") int userId,@Param("offset") int offset,@Param("limit") int limit);

    /**
     * 根据用户的id查询当前用户涉及的会话的数量
     * @param userId
     * @return
     */
    int selectConversionCount(int userId);

    /**
     * 此方法根据会话id取查询当前会话里有多少条消息并分页显示
     * @param conversationId 会话id
     * @param offset
     * @param limit
     * @return 当前页的会话中的消息的列表
     */
    List<Message> selectMessageList(@Param("conversationId") String conversationId,@Param("offset") int offset,@Param("limit") int limit);

    int selectMessageCount(String conversationId);

    /**
     * 此方法可以实现两个业务，如果会话id为空，那么查询的就是用户所有会话的未读消息的数量。如果会话id不为空，那么查询的就是当前会话中用户未读消息的数量
     * @param userId
     * @param conversationId
     * @return
     */
    int selectUnReadMessageCount(@Param("userId") int userId, @Param("conversationId") String conversationId);

    int insertMessage(Message message);

//    int updateReadMessage(Message message);
    int updateReadMessage(@Param("ids") List<Integer> ids);

    /**
     * 该方法是根据系统通知的类型来获取该类型的最新的系统通知，以显示在通知页面上
     * @param toId 通知的用户的id
     * @param conversationId 通知的类型
     * @return
     */
    Message selectLatestNotice(@Param("toId") int toId,@Param("conversationId") String conversationId);

    /**
     * 方法是根据系统通知的类别获取该类型的通知有多少条
     * @param toId 通知的用户的id
     * @param conversationId 通知的类型
     * @return
     */
    int selectTotalNoticeCount(@Param("toId") int toId,@Param("conversationId") String conversationId);

    /**
     * 方法是根据系统通知的类别获取该类型的未读通知有多少条
     * @param toId 通知的用户的id
     * @param conversationId 通知的类型 如果传入的类型为空，就查询所有类型的未读的系统通知的数量！
     * @return
     */
    int selectUnreadNoticeCount(@Param("toId") int toId,@Param("conversationId") String conversationId);

    /**
     * 该方法是根据用户的id以及系统通知的类型去分页查询出系统通知列表
     * @param toId 系统通知的用户id
     * @param conversationId 系统通知的类别
     * @param offset
     * @param limit
     * @return
     */
    List<Message> selectNoticeList(@Param("toId") int toId, @Param("conversationId") String conversationId,
                                   @Param("offset") int offset, @Param("limit") int limit);
}
