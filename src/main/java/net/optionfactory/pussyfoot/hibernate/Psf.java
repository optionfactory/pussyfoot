package net.optionfactory.pussyfoot.hibernate;

import net.optionfactory.pussyfoot.PageRequest;
import net.optionfactory.pussyfoot.PageResponse;

public interface Psf<T> {

    PageResponse<T> queryForPage(PageRequest request);
}
