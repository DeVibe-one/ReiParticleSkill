// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.utils.storage;

import java.util.function.Supplier;

/**
 * Lazy-initialized memoization container.
 * The value is computed on first access via the supplier and cached.
 * Can be manually overridden or reset.
 *
 * @param <T> the cached value type
 */
public final class Memo<T> {
    private final Supplier<T> supplier;
    private T memo;
    private boolean initialized;

    public Memo(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public Supplier<T> getSupplier() {
        return supplier;
    }

    public T get() {
        if (!initialized) {
            memo = supplier.get();
            initialized = true;
        }
        return memo;
    }

    public Memo<T> setMemoValue(T memo) {
        this.memo = memo;
        return this;
    }

    public Memo<T> resetMemo() {
        initialized = false;
        memo = null;
        return this;
    }
}
