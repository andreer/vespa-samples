package com.kkraune;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;

public class Node {
    String flavor;
    Capacity capacity = null;
    Capacity usedCapacity = null;
    Capacity freeCapacity = null;
    String parentHostname;
    String environment;
    String hostname;
    String type;
    String appInstance;
    String state;
    List<Node> virtualNodes;
    HashSet<String> appInstances = new HashSet<>();

    Node() {}

    Node(JsonNode node) {
        virtualNodes = new ArrayList<>();
        this.parentHostname = "";
        if (!node.path("parentHostname").isMissingNode()){
            this.parentHostname = node.get("parentHostname").asText();
        }
        this.flavor = node.get("flavor").asText();
        this.hostname = node.get("hostname").asText();
        this.environment = node.get("environment").asText();
        this.type = node.get("type").asText();
        this.state = node.get("state").asText();

        if (!node.path("owner").isMissingNode()) {
            appInstance = node.get("owner").get("tenant").asText() + "/"
                        + node.get("owner").get("application").asText();
        }
    }

    public Map<String, Object> toMap() {
        CapacityFloat cap = new CapacityFloat(getUsedCapacity().fractionOf(getMaxCapacity()));
        Map<String,Object> fields = new HashMap<>();
        fields.put("hostname", hostname);
        fields.put("freeCapacity", getFreeCapacity().toFlavorString());
        fields.put("usedCapacity", getUsedCapacity().toFlavorString());
        fields.put("maxCapacity", getMaxCapacity().toFlavorString());
        fields.put("relativeUsedCapacity", cap.toFlavorString());
        fields.put("utilization", cap.cpu/3 + cap.memory/3 + cap.disksize/3);
        return fields;
    }

    boolean isBaseHost() {
        return "host".equals(type);
    }

    boolean isVirtualNode() {
        return "DOCKER_CONTAINER".equals(environment);
    }

    boolean isActive() {
        return "active".equals(state);
    }

    boolean hasAppInstance(String appInstance) {
        return appInstances.contains(appInstance);
    }

    Capacity getUsedCapacity() {
        if (this.usedCapacity == null) {
            Capacity usedCapacity = new Capacity();
            for (Node virtualnode : virtualNodes) {
                usedCapacity.add(virtualnode.getMaxCapacity());
            }
            this.usedCapacity = usedCapacity;
        }
        return usedCapacity;
    }

    Capacity getMaxCapacity() {
        if(capacity == null)
            capacity = new Capacity(this.flavor);
        return capacity;
    }

    Capacity getFreeCapacity() {
        if (freeCapacity == null)
            freeCapacity = new Capacity(
                    getMaxCapacity().cpu - getUsedCapacity().cpu,
                    getMaxCapacity().memory - getUsedCapacity().memory,
                    getMaxCapacity().disksize - getUsedCapacity().disksize);
        return freeCapacity;
    }

    public boolean hasChildren() {
        return ! virtualNodes.isEmpty();
    }

    int getFreeCPU() {
        return getFreeCapacity().cpu;
    }

    int getFreeMemory() {
        return getFreeCapacity().memory;
    }

    int getFreeDisksize() {
        return getFreeCapacity().disksize;
    }

    int wastedCPU(Node node) {
        // by allocating this virtual node, if exhausting memory or disk,
        // no more nodes can be allocated to this base host, rest-CPU is wasted
        if (    (this.getFreeMemory()   - node.getMaxCapacity().memory) == 0 ||
                (this.getFreeDisksize() - node.getMaxCapacity().disksize) == 0) {
            return this.getFreeCPU() - node.getMaxCapacity().cpu;
        }
        return 0;
    }

    int wastedMemory(Node node) {
        // by allocating this virtual node, if exhausting cpu or disk,
        // no more nodes can be allocated to this base host, rest-memory is wasted
        if (    (this.getFreeCPU()   - node.getMaxCapacity().cpu) == 0 ||
                (this.getFreeDisksize() - node.getMaxCapacity().disksize) == 0) {
            return this.getFreeMemory() - node.getMaxCapacity().memory;
        }
        return 0;
    }

    int wastedDisksize(Node node) {
        // by allocating this virtual node, if exhausting cpu or memory,
        // no more nodes can be allocated to this base host, rest-disk is wasted
        if (    (this.getFreeCPU()   - node.getMaxCapacity().cpu) == 0 ||
                (this.getFreeMemory() - node.getMaxCapacity().memory) == 0) {
            return this.getFreeDisksize() - node.getMaxCapacity().disksize;
        }
        return 0;
    }

    void addVirtualNode(Node node) {
        virtualNodes.add(node);
        usedCapacity = null;
        freeCapacity = null;
        appInstances.add(node.appInstance);
    }

    public Node(Node node) { // copy constructor
        this.flavor = node.flavor;
        this.capacity = null;
        this.usedCapacity = null;
        this.parentHostname = node.parentHostname;
        this.environment = node.environment;
        this.hostname = node.hostname;
        this.type = node.type;
        this.appInstance = node.appInstance;
        this.state = node.state;
        this.virtualNodes = new ArrayList<>();
    }
}
