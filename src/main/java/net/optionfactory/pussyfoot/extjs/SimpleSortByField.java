package net.optionfactory.pussyfoot.extjs;

import java.util.function.Function;
import net.optionfactory.pussyfoot.SortRequest;
import org.hibernate.criterion.Order;

public class SimpleSortByField implements Function<SortRequest.Direction, Order> {

    private final String fieldName;

    public SimpleSortByField(String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public Order apply(SortRequest.Direction t) {
        return t == SortRequest.Direction.ASC ? Order.asc(fieldName) : Order.desc(fieldName);
    }

}
