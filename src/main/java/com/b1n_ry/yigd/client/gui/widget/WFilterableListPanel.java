package com.b1n_ry.yigd.client.gui.widget;

import io.github.cottonmc.cotton.gui.widget.WListPanel;
import io.github.cottonmc.cotton.gui.widget.WWidget;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class WFilterableListPanel<D, W extends WWidget> extends WListPanel<D, W> {
    private final List<D> unfilteredData;
    private final Map<String, Predicate<D>> filters;

    /**
     * Constructs a list panel which has its contents filterable
     *
     * @param data         the list data
     * @param supplier     the widget supplier that creates unconfigured widgets
     * @param configurator the widget configurator that configures widgets to display the passed data
     */
    public WFilterableListPanel(List<D> data, Supplier<W> supplier, BiConsumer<D, W> configurator) {
        super(data, supplier, configurator);

        this.filters = new HashMap<>();
        this.unfilteredData = new ArrayList<>(data);  // Make sure this.data is not the same list
        this.reload();
    }

    public void reload() {
        this.data.clear();
        for (D data : this.unfilteredData) {
            for (Predicate<D> filter : this.filters.values()) {
                if (filter.test(data)) {
                    this.data.add(data);
                }
            }
        }

        this.layout();
    }

    public void setFilter(String key, Predicate<D> filter) {
        this.filters.put(key, filter);
        this.reload();
    }

    public Collection<W> getWidgets() {
        return this.configured.values();
    }
}
