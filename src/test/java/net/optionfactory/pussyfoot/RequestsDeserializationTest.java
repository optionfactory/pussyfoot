package net.optionfactory.pussyfoot;

import org.junit.Assert;
import org.junit.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * FilterRequest and SortRequest must be deserializable by an unconfigured
 * mapper: they carry their own @JsonCreator, so callers are not required to
 * register anything.
 */
public class RequestsDeserializationTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    public void filterRequestsAreDeserializedByAnUnconfiguredMapper() {
        final FilterRequest[] got = mapper.readValue("[{\"name\":\"id\",\"value\":42}]", FilterRequest[].class);
        Assert.assertEquals(1, got.length);
        Assert.assertEquals("id", got[0].name);
        Assert.assertEquals(42, got[0].value);
    }

    @Test
    public void sortRequestsAreDeserializedByAnUnconfiguredMapper() {
        final SortRequest[] got = mapper.readValue("[{\"name\":\"id\",\"direction\":\"DESC\"}]", SortRequest[].class);
        Assert.assertEquals(1, got.length);
        Assert.assertEquals("id", got[0].name);
        Assert.assertEquals(SortRequest.Direction.DESC, got[0].direction);
    }
}
