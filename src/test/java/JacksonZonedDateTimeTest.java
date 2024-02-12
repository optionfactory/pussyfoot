
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.time.ZonedDateTime;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

public class JacksonZonedDateTimeTest {

    @Test
    @Ignore
    public void check() throws JsonProcessingException, IOException {
        final ObjectMapper mapper = new Jackson2ObjectMapperBuilder()
                .timeZone("Europe/Rome")
                .json()
                .failOnEmptyBeans(false)
                .modules(
                        new JavaTimeModule()
                )
                .autoDetectFields(true)
                .build();
    //    mapper.getDeserializationContext().getTimeZone();
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false);
        final ZonedDateTime zdt = ZonedDateTime.now();

        String serialized = mapper.writeValueAsString(zdt);
        ZonedDateTime unmarshalled = mapper.readValue(serialized, ZonedDateTime.class);
        Assert.assertEquals(zdt.toInstant(), unmarshalled.toInstant());

    }

}
