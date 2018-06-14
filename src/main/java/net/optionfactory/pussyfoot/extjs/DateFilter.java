package net.optionfactory.pussyfoot.extjs;

public class DateFilter {

    public long timestamp;
    public Operator operator;

    public static enum Operator {
        lt, gt, eq, gte, lte
    }

}
