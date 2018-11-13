package net.optionfactory.pussyfoot.hibernate.predicates;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;
import net.emaze.dysfunctional.tuples.Pair;

public class DateIn<TCol extends Comparable<? super TCol>> extends DualThresholdComparator<TCol, ZonedDateTime> {

    public DateIn(Function<ZonedDateTime, TCol> filterValueAdapter) {
        super((ZonedDateTime zdt) -> {
            final ZonedDateTime beginningOfDay = zdt.truncatedTo(ChronoUnit.DAYS);
            final ZonedDateTime beginningOfNextDay = beginningOfDay.plus(1, ChronoUnit.DAYS);
            return Pair.of(filterValueAdapter.apply(beginningOfDay), filterValueAdapter.apply(beginningOfNextDay));
        });
    }

}
