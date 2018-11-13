
import net.optionfactory.pussyfoot.hibernate.SinglePathPredicateBuilder;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import net.optionfactory.pussyfoot.extjs.Comparator;

public class ComparatorPredicateBuilder<TCol extends Comparable<TCol>> implements SinglePathPredicateBuilder<Comparator<TCol>, TCol> {
    
    @Override
    public Predicate predicateFor(CriteriaBuilder criteriaBuilder, Comparator<TCol> resolvedFilterValue, Expression<TCol> resolvedColValue) {
        switch (resolvedFilterValue.operator) {
            case lt:
                return criteriaBuilder.lessThan(resolvedColValue, resolvedFilterValue.value);
            case gt:
                break;
            case eq:
                break;
            case gte:
                break;
            case lte:
                break;
            default:
                throw new AssertionError(resolvedFilterValue.operator.name());
        }
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
