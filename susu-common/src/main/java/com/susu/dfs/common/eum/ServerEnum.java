package com.susu.dfs.common.eum;

import lombok.Getter;

@Getter
public enum ServerEnum {

    TRACKER(0),
    STORAGE(1),
    CLIENT(2);

    private final Integer code;

    ServerEnum(Integer code) {
        this.code = code;
    }
}
