package net.optionfactory.pussyfoot.hibernate.typed;

import java.util.List;
import java.util.Optional;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Selection;

public class SorterContext {

    public SorterContext() {
        additionalSelection = Optional.empty();
        groupers = Optional.empty();
    }
    public Optional<Selection<?>> additionalSelection;
    public Optional<List<Expression<?>>> groupers;
    public Expression<?> sortExpression;

}
