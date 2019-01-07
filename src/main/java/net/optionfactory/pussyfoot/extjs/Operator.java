package net.optionfactory.pussyfoot.extjs;

/**
 * The operator that defines a comparison type. One of:
 * <ul>
 * <li>{@link #lte}: a &le; b</li>
 * <li>{@link #lt} : a &lt; b</li>
 * <li>{@link #eq} : a = b</li>
 * <li>{@link #gt} : a &gt; b</li>
 * <li>{@link #gte}: a &ge; b</li>
 * </ul>
 */
public enum Operator {

    /**
     * Strictly Less than (e.g.: a &lt; b)
     */
    lt,
    /**
     * Strictly Greater than (e.g.: a &gt; b)
     */
    gt,
    /**
     * Equality (e.g.: a = b)
     */
    eq,
    /**
     * Greater or equal than (e.g.: a &ge; b)
     */
    gte,
    /**
     * Less or equal than (e.g.: a &le; b)
     */
    lte

}
