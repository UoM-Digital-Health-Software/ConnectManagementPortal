package org.radarbase.management.domain.support;

import java.time.Instant;

public interface ConntetDataLogView {
    String getUserId();
    String getDataGroupingType();
    Instant getTime();
}
