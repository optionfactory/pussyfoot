package net.optionfactory.pussyfoot.hibernate.executors;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;
import net.optionfactory.pussyfoot.Pair;

public class DateInExecutor<TCol extends Comparable<? super TCol>> extends DualThresholdComparatorExecutor<TCol, ZonedDateTime> {

    public DateInExecutor(Function<ZonedDateTime, TCol> filterValueAdapter) {
        super((ZonedDateTime zdt) -> {
            final ZonedDateTime beginningOfDay = zdt.truncatedTo(ChronoUnit.DAYS);
            final ZonedDateTime beginningOfNextDay = beginningOfDay.plus(1, ChronoUnit.DAYS);
            return Pair.of(filterValueAdapter.apply(beginningOfDay), filterValueAdapter.apply(beginningOfNextDay));
        });
    }

}
