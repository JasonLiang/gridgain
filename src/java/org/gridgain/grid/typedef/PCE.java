// Copyright (C) GridGain Systems Licensed under GPLv3, http://www.gnu.org/licenses/gpl.html

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.typedef;

import org.gridgain.grid.cache.*;
import org.gridgain.grid.lang.*;

/**
 * Defines {@code alias} for <tt>GridPredicate&lt;GridCacheEntry&lt;K, V&gt;&gt;</tt> by extending
 * {@link GridPredicate}. Since Java doesn't provide type aliases (like Scala, for example) we resort
 * to these types of measures. This is intended to provide for more concise code without sacrificing
 * readability. For more information see {@link GridPredicate} and {@link GridCacheEntry}.
 *
 * @author 2012 Copyright (C) GridGain Systems
 * @version 4.0.0c.22032012
 * @see GridPredicate
 * @see GridFunc
 * @see GridCacheEntry
 */
public abstract class PCE<K, V> extends GridPredicate<GridCacheEntry<K, V>> { /* No-op. */ }
