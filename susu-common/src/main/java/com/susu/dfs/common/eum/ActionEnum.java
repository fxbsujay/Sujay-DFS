package com.susu.dfs.common.eum;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ActionEnum {

    ADD(1),
    UPDATE(2),
    DELETE(3);

    private final int value;

    public static ActionEnum getEnum(int value) {
        for (ActionEnum action : values()) {
            if (action.value == value) {
                return action;
            }
        }
        return ADD;
    }

}
