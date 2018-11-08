package net.optionfactory.pussyfoot.extjs;

import java.time.Instant;
import java.time.ZoneId;

/**
 * Specialization of a {@link Comparator}, with a type T fixed to
 * {@link Instant}, plus an additional field to specify the reference
 * {@link ZoneId}
 */
public class UTCDateWithTimeZone extends Comparator<Instant> {

    /**
     * Reference time zone
     */
    public ZoneId timeZone;
}
