package org.babyfish.jimmer.model;

public class RobotComment implements Comment {

    private String text;

    private int level;

    RobotComment() {}

    public RobotComment(String text, int level) {
        this.text = text;
        this.level = level;
    }

    @Override
    public String getText() {
        return text;
    }

    public int getLevel() {
        return level;
    }
}
