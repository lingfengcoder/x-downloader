package com.lingfeng.rpc.trans;

import com.lingfeng.rpc.model.Message;
import com.lingfeng.rpc.model.MessageType;
import com.lingfeng.rpc.store.MessageStore;
import com.lingfeng.rpc.util.GsonTool;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: wz
 * @Date: 2022/5/9 20:17
 * @Description:
 */
@Slf4j
public class MessageTrans {


    public static Message<Object> trans(String msg) {
        return GsonTool.fromJson(msg, Message.class);
    }

    public static BizFrame buildMsg(String data, int clientId) {
        //note 增加消息编号
        Long seq = MessageStore.getClientMsgSeq(clientId);
        Message<Object> msg = Message.builder()
                .time(System.currentTimeMillis())
                .type(MessageType.MSG.getCode())
                .clientId(clientId)
                .seq(seq)
                .data(data)
                .build();
        String s = GsonTool.toJson(msg);
//        return Unpooled.copiedBuffer(s, CharsetUtil.UTF_8);
        return BizFrame.build(s);
    }

    public static Message<Object> parseStr(BizFrame frame) {
        String msg = frame.getContent();
        if (!StringUtil.isNullOrEmpty(msg)) {
            try {
                return trans(msg);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        return null;
    }

    public static Message<Object> parseStr(ByteBuf byteBuf) {
        String msg = byteBuf.toString(CharsetUtil.UTF_8);
        if (!StringUtil.isNullOrEmpty(msg)) {
            return trans(msg);
        }
        return null;
    }
}
