package org.babyfish.jimmer.model;

public class ManualComment implements Comment {

    private String text;

    private String userId;

    ManualComment() {}

    public ManualComment(String text, String userId) {
        this.text = text;
        this.userId = userId;
    }

    @Override
    public String getText() {
        return null;
    }

    public String getUserId() {
        return userId;
    }
}
