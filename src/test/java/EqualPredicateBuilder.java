
import net.optionfactory.pussyfoot.hibernate.SinglePathPredicateBuilder;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;

public class EqualPredicateBuilder<TFilterValue extends TCol, TCol> implements SinglePathPredicateBuilder<TFilterValue, TCol> {
    
    @Override
    public Predicate predicateFor(CriteriaBuilder criteriaBuilder, TFilterValue resolvedFilterValue, Expression<TCol> resolvedColValue) {
        return criteriaBuilder.equal(resolvedColValue, resolvedFilterValue);
    }
    
}
