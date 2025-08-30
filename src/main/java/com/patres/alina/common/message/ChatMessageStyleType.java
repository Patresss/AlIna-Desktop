package com.patres.alina.common.message;

public enum ChatMessageStyleType {

    ACCENT("accent"),
    WARNING("warning"),
    SUCCESS("success"),
    DANGER("danger"),
    NONE("");

    private final String styleType;

    ChatMessageStyleType(String styleType) {
        this.styleType = styleType;
    }

    public String getStyleType() {
        return styleType;
    }

}
