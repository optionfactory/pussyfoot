package net.optionfactory.pussyfoot.extjs;

/**
 * @deprecated replaced by {@link UTCDate}
 */
@Deprecated
public class DateFilter {

    public long timestamp;
    public Operator operator;

    public static enum Operator {
        lt, gt, eq, gte, lte
    }

}
