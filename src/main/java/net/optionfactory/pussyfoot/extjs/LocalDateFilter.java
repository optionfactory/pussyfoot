package net.optionfactory.pussyfoot.extjs;

import java.time.LocalDate;

public class LocalDateFilter {

    public LocalDate date;
    public Operator operator;

    public static enum Operator {
        lt, gt, eq
    }

}
