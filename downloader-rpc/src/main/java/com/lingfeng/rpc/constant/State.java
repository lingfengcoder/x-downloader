package com.lingfeng.rpc.constant;

public enum State {
    //关闭并且不重启
    CLOSED_NO_RETRY(-1),
    //关闭态可以重启
    CLOSED(0),
    //运行态
    RUNNING(1),
    //正在启动态
    STARTING(2);
    private final int code;

    State(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static State trans(int code) {
        for (State state : State.values()) {
            if (state.code() == code) {
                return state;
            }
        }
        return null;
    }
}
