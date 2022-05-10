package com.lingfeng.rpc.trans;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @Author: wz
 * @Date: 2022/5/10 14:56
 * @Description:
 */
@Setter
@Getter
@ToString
@Builder
public class BizFrame {
    private byte type;

    private int length;

    private String content;

    public static BizFrame build(String msg) {
        return BizFrame.builder().type((byte) 1)
                .length(msg.length())
                .content(msg)
                .build();
    }
}
