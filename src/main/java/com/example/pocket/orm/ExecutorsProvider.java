package com.example.pocket.orm;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Provides shared executor services used across the ORM for async operations.
 */
public class ExecutorsProvider {
    public static final ExecutorService CACHED_THREAD_POOL = Executors.newCachedThreadPool();
}

