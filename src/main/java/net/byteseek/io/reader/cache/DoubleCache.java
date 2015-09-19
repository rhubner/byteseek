/*
 * Copyright Matt Palmer 2011-2012, All rights reserved.
 *
 * This code is licensed under a standard 3-clause BSD license:
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution.
 * 
 *  * The names of its contributors may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.byteseek.io.reader.cache;

import net.byteseek.io.reader.Window;

/**
 * A specialised cache which stores data in two caches.  The memory cache is checked
 * first.  If the data is not in the memory cache, the persistent cache is
 * checked.  If the data is in the persistent cache, it is re-added to the
 * memory cache.
 * <p>
 * The use case for this cache is to allow holding Windows in a fast
 * in-memory primary soft cache, which can evict data under low memory conditions.
 * The secondary cache should be an on-disk cache which can always retrieve the
 * data.  If data has been evicted from the primary cache, it can be safely
 * retrieved from the slower permanent cache.  This window is then re-added
 * to the fast primary cache.
 * <p>
 * For example, when using an InputStreamReader where we want to be able to
 * always retrieve old data, but also want to support faster access to multiple
 * Windows (memory permitting), this cache allows both requirements to be satisfied.
 * </p>
 *
 * @author Matt Palmer
 */
public final class DoubleCache extends AbstractNoFreeNotificationCache {

    private final WindowCache memoryCache;
    private final WindowCache persistentCache;

    public DoubleCache(final WindowCache memoryCache, final WindowCache secondaryCache) {
        this.memoryCache     = memoryCache;
        this.persistentCache = secondaryCache;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Window getWindow(final long position) {
        Window window = memoryCache.getWindow(position);
        if (window == null) {
            window = persistentCache.getWindow(position);
            if (window != null) {
                memoryCache.addWindow(window);
            }
        }
        return window;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addWindow(final Window window) {
        memoryCache.addWindow(window);
        persistentCache.addWindow(window);
    }

    /**
     * Clears both the memory and persistent caches, using whatever
     * mechanisms they use to clear themselves.
     */
    @Override
    public void clear() {
        memoryCache.clear();
        persistentCache.clear();
    }

    /**
     * Returns the memory cache used by this DoubleCache.
     *
     * @return WindowCache The memory cache used by this DoubleCache.
     */
    public WindowCache getMemoryCache() {
        return memoryCache;
    }


    /**
     * Returns the persistent cache used by this DoubleCache.
     *
     * @return WindowCache the persistent cache used by this DoubleCache.
     */
    public WindowCache getPersistentCache() {
        return persistentCache;
    }


    @Override
    public String toString() {
        return getClass().getSimpleName() + "[memory cache: " + memoryCache +
                " persistent cache: " + persistentCache + ']';
    }

}
