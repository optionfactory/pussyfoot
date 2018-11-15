package net.optionfactory.pussyfoot.hibernate;

import java.util.Optional;
import net.optionfactory.pussyfoot.SortRequest;
import net.optionfactory.pussyfoot.hibernate.HibernatePsf.PagingDirection;
import org.junit.Assert;
import org.junit.Test;

public class HibernatePsfTest {

    @Test
    public void ascAndNoTokenLeadsToAsc() {
        final SortRequest.Direction got = HibernatePsf.determineEffectiveDirection(SortRequest.Direction.ASC, Optional.empty());
        Assert.assertEquals(SortRequest.Direction.ASC, got);
    }

    @Test
    public void descAndNoTokenLeadsToDesc() {
        final SortRequest.Direction got = HibernatePsf.determineEffectiveDirection(SortRequest.Direction.DESC, Optional.empty());
        Assert.assertEquals(SortRequest.Direction.DESC, got);
    }

    @Test
    public void ascAndNextPageTokenLeadsToAsc() {
        final SortRequest.Direction got = HibernatePsf.determineEffectiveDirection(SortRequest.Direction.ASC, Optional.of(PagingDirection.NextPage));
        Assert.assertEquals(SortRequest.Direction.ASC, got);
    }

    @Test
    public void descAndNextPageTokenLeadsToDesc() {
        final SortRequest.Direction got = HibernatePsf.determineEffectiveDirection(SortRequest.Direction.DESC, Optional.of(PagingDirection.NextPage));
        Assert.assertEquals(SortRequest.Direction.DESC, got);
    }

    @Test
    public void ascAndPrevPageTokenLeadsToDesc() {
        final SortRequest.Direction got = HibernatePsf.determineEffectiveDirection(SortRequest.Direction.ASC, Optional.of(PagingDirection.PreviousPage));
        Assert.assertEquals(SortRequest.Direction.DESC, got);
    }

    @Test
    public void descAndPrevPageTokenLeadsToAsc() {
        final SortRequest.Direction got = HibernatePsf.determineEffectiveDirection(SortRequest.Direction.DESC, Optional.of(PagingDirection.PreviousPage));
        Assert.assertEquals(SortRequest.Direction.ASC, got);
    }

}
