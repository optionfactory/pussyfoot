import java.time.ZonedDateTime;
import java.util.TimeZone;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

public class JacksonZonedDateTimeTest {

    @Test
    @Ignore
    public void check() {
        final JsonMapper mapper = JsonMapper.builder()
                .defaultTimeZone(TimeZone.getTimeZone("Europe/Rome"))
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DateTimeFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                .build();
        final ZonedDateTime zdt = ZonedDateTime.now();

        String serialized = mapper.writeValueAsString(zdt);
        ZonedDateTime unmarshalled = mapper.readValue(serialized, ZonedDateTime.class);
        Assert.assertEquals(zdt.toInstant(), unmarshalled.toInstant());

    }

}
