// Copyright (C) GridGain Systems Licensed under GPLv3, http://www.gnu.org/licenses/gpl.html

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.events;

import java.util.concurrent.atomic.*;

/**
 * Generator for local atomically incremented IDs for grid events.
 *
 * @author 2012 Copyright (C) GridGain Systems
 * @version 4.0.0c.22032012
 */
final class GridEventLocalOrder {
    /** Generator implementation. */
    private static final AtomicLong gen = new AtomicLong(0);

    /**
     * No-arg constructor enforces the singleton.
     */
    private GridEventLocalOrder() {
        // No-op.
    }

    /**
     * Gets next atomically incremented local event order.
     *
     * @return Next atomically incremented local event order.
     */
    public static long nextOrder() {
        return gen.getAndIncrement();
    }
}
