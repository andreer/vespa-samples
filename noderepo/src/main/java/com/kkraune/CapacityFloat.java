package com.kkraune;

public class CapacityFloat {
    public float cpu;
    public float memory;
    public float disksize;

    CapacityFloat() {
        cpu      = 0;
        memory   = 0;
        disksize = 0;
    }

    CapacityFloat(float cpu, float memory, float disksize) {
        this.cpu      = cpu;
        this.memory   = memory;
        this.disksize = disksize;
    }

    public String toFlavorString() {
        return "d-"
                + String.format("%.2f", cpu)     + "-"
                + String.format("%.2f", memory ) + "-"
                + String.format("%.2f", disksize);
    }
}
