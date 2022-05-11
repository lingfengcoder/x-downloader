package com.lingfeng.rpc.constant;

public enum State {
    CLOSED(0), RUNNING(1), IDLE(2);
    private int code;

    State(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static State trans(int code) {
        for (State state : State.values()) {
            if (state.getCode() == code) {
                return state;
            }
        }
        return null;
    }
}
