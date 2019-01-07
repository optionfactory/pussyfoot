package net.optionfactory.pussyfoot.extjs;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Function;
import net.optionfactory.pussyfoot.FilterRequest;

/**
 * Helpers to deserialize the 'value' portions of {@link FilterRequest} issued
 * by common ExtJs gridfilters plugin, some of which are defined within this
 * project's pussyfoot-extjs-overrides.js.
 *
 * <p>
 * e.g. Given a stringified {@link FilterRequest#value}'s like :
 * </p>
 * <ul>
 * <li> "{\"value\":42,\"operator\":\"gte\"}", you can use {@link #comparator}
 * to deserialize it</li>
 * <li> "{\"value\":\"42\",\"operator\":\"gte\"}", you can use
 * {@link #comparator} to deserialize it</li>
 * <li> "{\"value\":[],\"operator\":\"gte\"}", you can use {@link #comparator}
 * to deserialize it</li>
 * <li> "{\"value\":1541462400000,\"operator\":\"gte\"}", you can use
 * {@link #utcDate} to deserialize it</li>
 * <li> "{\"value\":1541462400000,\"timeZone\":\"Europe/Rome\",
 * \"operator\":\"gte\"}", you can use {@link #utcDateWithTimeZone} to
 * deserialize it</li>
 * <li> "[\"mario\", \"luigi\"]", you can use {@link #valuesList} or
 * {@link #enumSetList} to deserialize it</li>
 * </ul>
 */
public class ExtJs {

    /**
     * To be used in conjunction with a column with a "filter: 'number'" or
     * "filter: 'date'" definition
     *
     * @param <T> The type of the value to filter against
     * @param clazz The class of the value to filter against
     * @param mapper jacksons' {@link ObjectMapper} instance to be used for
     * deserialization
     * @return a {@link Comparison} instance
     */
    public static <T extends Comparable<? super T>> Function<String, Comparison<T>> comparator(Class<T> clazz, ObjectMapper mapper) {
        return (String v) -> {
            try {
                return (Comparison<T>) mapper.readValue(v, mapper.getTypeFactory().constructParametricType(Comparison.class, clazz));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        };
    }

    /**
     * To be used in conjunction with a column with a "filter: 'utcdate'"
     * definition
     *
     * @param mapper jacksons' {@link ObjectMapper} instance to be used for
     * deserialization
     * @return a {@link Comparison} instance
     */
    public static Function<String, Comparison<ZonedDateTime>> utcDate(ObjectMapper mapper) {
        return (String v) -> {
            try {
                final UTCDate utcDateFilter = (UTCDate) mapper.readValue(v, new TypeReference<UTCDate>() {
                });
                return Comparison.<ZonedDateTime>of(utcDateFilter.operator, utcDateFilter.value.atZone(ZoneId.of("UTC")));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        };
    }

    /**
     * To be used in conjunction with a column with a "filter:
     * 'utcDateWithTimeZone'" definition
     *
     * @param mapper jacksons' {@link ObjectMapper} instance to be used for
     * deserialization
     * @return a {@link Comparison} instance
     */
    public static Function<String, Comparison<ZonedDateTime>> utcDateWithTimeZone(ObjectMapper mapper) {
        return (String v) -> {
            try {
                final UTCDateWithTimeZone filter = (UTCDateWithTimeZone) mapper.readValue(v, new TypeReference<UTCDateWithTimeZone>() {
                });
                return Comparison.<ZonedDateTime>of(filter.operator, filter.value.atZone(filter.timeZone));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        };
    }

    /**
     * To be used in conjunction with a column with a "filter: 'list'"
     * definition
     *
     * @param <T> The type of the value to filter against
     * @param clazz The class of the value to filter against
     * @param mapper jacksons' {@link ObjectMapper} instance to be used for
     * deserialization
     * @return a {@link Comparison} instance
     */
    public static <T extends Enum<T>> Function<String, EnumSet<T>> enumSetList(Class<T> clazz, ObjectMapper mapper) {
        return (String v) -> {
            try {
                return (EnumSet<T>) mapper.readValue(v, mapper.getTypeFactory().constructCollectionLikeType(EnumSet.class, clazz));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        };
    }

    /**
     * To be used in conjunction with a column with a "filter: 'list'"
     * definition
     *
     * @param <T> The type of the value to filter against
     * @param clazz The class of the value to filter against
     * @param mapper jacksons' {@link ObjectMapper} instance to be used for
     * deserialization
     * @return a {@link Comparison} instance
     */
    public static <T> Function<String, List<T>> valuesList(Class<T> clazz, ObjectMapper mapper) {
        return (String v) -> {
            try {
                return (List<T>) mapper.readValue(v, mapper.getTypeFactory().constructCollectionLikeType(List.class, clazz));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        };
    }

}
