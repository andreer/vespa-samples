package com.kkraune;

public class Capacity {
    public int cpu;
    public int disksize;
    public int memory;

    Capacity() {
        this.cpu      = 0;
        this.disksize = 0;
        this.memory   = 0;
    }

    Capacity(int cpu, int memory, int disksize) {
        this.cpu      = cpu;
        this.memory   = memory;
        this.disksize = disksize;
    }

    void setCapacityFromString(String flavor) {
        switch (flavor){
            case "C-2B/24/500":      cpu = 24; memory = 24;  disksize = 500;  break;
            case "C-77C/256/800-10": cpu = 40; memory = 256; disksize = 1760; break;
            case "C-77C/256/960-10": cpu = 40; memory = 256; disksize = 1920; break;
            case "C-2E/64/480":      cpu = 48; memory = 64;  disksize = 480;  break;
            case "C-2E/64/960":      cpu = 48; memory = 64;  disksize = 960;  break;
            case "C-77E/128/960":    cpu = 48; memory = 128; disksize = 1920; break;
            case "C-77E/256/960":    cpu = 48; memory = 256; disksize = 1920; break;
            case "C-2I/64/1200":     cpu = 64; memory = 64;  disksize = 1200; break;
            case "C-78I/64/1920":    cpu = 64; memory = 64;  disksize = 7680; break;
            case "C-77I/256/1920":   cpu = 64; memory = 256; disksize = 3840; break;

            //
            default:
                throw new RuntimeException("Flavor " + flavor + " not found");
        }
    }

    boolean canFit(Capacity capacity) {
        if (this.cpu < capacity.cpu || this.memory < capacity.memory || this.disksize < capacity.disksize) {
            return false;
        }
        return true;
    }

    boolean isUsable() {
        return (cpu > 0 && memory > 0 && disksize > 0);
    }

    Capacity(String flavor) {
        // C-2E/64/480, d-12-16-100
        if ("d".equals(flavor.substring(0, 1))){
            String [] dimensions = flavor.split("-");
            cpu      = Integer.parseInt(dimensions[1]);
            memory   = Integer.parseInt(dimensions[2]);
            disksize = Integer.parseInt(dimensions[3]);
        }
        else {
            setCapacityFromString(flavor);
        }
    }

    Capacity normalized() {
        return new Capacity(1, memory/cpu, disksize/cpu);
    }

    String toFlavor(){
        return "d-" + cpu + "-" + memory + "-" + disksize;
    }

    void add(Capacity capacity) {
        this.cpu      += capacity.cpu;
        this.memory   += capacity.memory;
        this.disksize += capacity.disksize;
    }
}
