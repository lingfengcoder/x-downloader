package com.lingfeng.rpc.client;

import com.lingfeng.rpc.util.GsonTool;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * 通过缓冲队列 给channel写数据
 */
@Slf4j
@Setter
@Accessors(chain = true)

public class SendBuffer<T> {
    private int cap = 10;
    private AtomicInteger size = new AtomicInteger(0);
    private LinkedList<T> queue;


    public SendBuffer(int cap) {
        this.cap = cap;
        queue = new LinkedList<T>();
    }


    private boolean add(T t) {
        if (size.get() < cap) {
            queue.offer(t);
            return true;
        }
        return false;
    }

    public void write(T msg, MyClientHandler handler) {
        while (handler.getChannel() == null) {
            // log.info("");
        }
        int state = State.RUNNING.getCode();
        if (handler.getClient().state() != state) {
            throw new RuntimeException("[netty client id: " + handler.getClientId() + "] client state error, state=" + state);
        }
        ByteBuf byteBuf = buildMsg(msg);
        handler.getChannel().writeAndFlush(byteBuf);
    }

    private ByteBuf buildMsg(T msg) {
        String data = GsonTool.toJson(msg);
        return Unpooled.copiedBuffer(data, CharsetUtil.UTF_8);
    }

    private T parseObj(ByteBuf byteBuf, Class<T> clazz) {
        String data = parseStr(byteBuf);
        return GsonTool.fromJson(data, clazz);
    }

    private String parseStr(ByteBuf byteBuf) {
        return byteBuf.toString(CharsetUtil.UTF_8);
    }
}
