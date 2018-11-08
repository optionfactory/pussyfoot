package net.optionfactory.pussyfoot.hibernate;

import java.util.List;
import java.util.Optional;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Selection;

/**
 * Defines all that is necessary to apply a sorter function
 */
public class SorterContext {

    /**
     * Default constructor
     */
    public SorterContext() {
        additionalSelection = Optional.empty();
        groupers = Optional.empty();
    }

    /**
     * Additional selections to be added to the query
     */
    public Optional<Selection<?>> additionalSelection;
    
    /**
     * Grouping criteria for the query - use at own risk
     */
    public Optional<List<Expression<?>>> groupers;

    /**
     * The actual sort expression
     */
    public Expression<?> sortExpression;

}
