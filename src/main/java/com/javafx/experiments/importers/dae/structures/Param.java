package com.javafx.experiments.importers.dae.structures;

/**
 * @author Eclion
 */
public final class Param {
    public final String name;
    public final String type;

    public Param(final String name, final String type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String toString() {
        return "Input{"
                + "name=" + this.name
                + ", type='" + this.type + '\''
                + '}';
    }
}
