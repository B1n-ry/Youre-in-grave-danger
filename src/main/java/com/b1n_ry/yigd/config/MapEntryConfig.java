package com.b1n_ry.yigd.config;

public class MapEntryConfig {
    public String key;
    public String value;

    @SuppressWarnings("unused")
    public MapEntryConfig() {  // Required for cloth config GUI to work
        this.key = "";
        this.value = "";
    }
    public MapEntryConfig(String key, String value) {
        this.key = key;
        this.value = value;
    }
}
