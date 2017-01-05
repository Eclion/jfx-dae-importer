package com.javafx.experiments.importers.dae.structures;

/**
 * @author Eclion
 */
public final class Param {
    public final String name;
    public final String type;

    public Param(String name, String type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String toString() {
        return "Input{" +
                "name=" + name +
                ", type='" + type + '\'' +
                '}';
    }
}
