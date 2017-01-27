package net.optionfactory.pussyfoot;

public class SortRequest {

    public final String name;
    public final Direction direction;

    public SortRequest(String name, Direction direction) {
        this.name = name;
        this.direction = direction;
    }

    public enum Direction {

        ASC, DESC;
    }

    @Override
    public String toString() {
        return "SortRequest{" + "name=" + name + ", direction=" + direction + '}';
    }

}
