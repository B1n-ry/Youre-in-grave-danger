package com.b1n_ry.yigd.config;

import java.util.HashMap;
import java.util.Map;

public abstract class MapEntryConfig<T> {
    public String key;
    public T value;

    @SuppressWarnings("unused")
    public MapEntryConfig() {  // Required for cloth config GUI to work
        this.key = "";
    }

    public MapEntryConfig(String key) {
        this.key = key;
    }

    public static class StringType extends MapEntryConfig<String> {
        @SuppressWarnings("unused")
        public StringType() {  // Required for cloth config GUI to work
            super();
            this.value = "";
        }

        public StringType(String key, String value) {
            super(key);
            this.value = value;
        }
    }

    public static class IntType extends MapEntryConfig<Integer> {
        @SuppressWarnings("unused")
        public IntType() {  // Required for cloth config GUI to work
            super();
            this.value = 0;
        }

        public IntType(String key, int value) {
            super(key);
            this.value = value;
        }
    }
}
