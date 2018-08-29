package com.kkraune;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

public class NodeRepo {
    private List<Node> virtualNodes;
    private List<Node> baseHosts;

    private NodeRepo(String filename) {
        virtualNodes = new ArrayList<Node>();
        baseHosts    = new ArrayList<Node>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            ObjectMapper mapper = new ObjectMapper();
            JsonNode nodeRepo = mapper.readTree(reader);
            Iterator<JsonNode> jsonNodes = nodeRepo.path("nodes").iterator();
            while (jsonNodes.hasNext()) {
                Node node = new Node(jsonNodes.next());
                if (!node.isActive()) {
                    continue;
                }
                if (node.isBaseHost()){
                    baseHosts.add(node);
                }
                else if (node.isVirtualNode()) {
                    virtualNodes.add(node);
                }
            }
            System.out.println("Base hosts: " + baseHosts.size() + ", Virtual nodes: " + virtualNodes.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void assignVirtualNodeToBaseHost(Node virtualNode, ToIntFunction<Node> optFunc) throws OutOfCapacityException {
        Optional<Node> bestBaseHost = baseHosts
                .stream()
                .filter(b -> { return   b.getFreeCapacity().canFit(virtualNode.getMaxCapacity()); })
                .filter(b -> { return ! b.hasAppInstance(virtualNode.appInstance); })
                .min(Comparator.comparingInt(optFunc));
        if (bestBaseHost.isPresent()) {
            addVirtualNodeToBaseHost(bestBaseHost.get().hostname, virtualNode);
        } else {
            throw new OutOfCapacityException("No space for " + virtualNode.hostname + " " + virtualNode.getMaxCapacity().toFlavor());
        }
    }

    public void addVirtualNodeToBaseHost(String baseHostName, Node virtualNode) {
        for (Node baseHost : baseHosts) {
            if (baseHost.hostname.equals(baseHostName)) {
                baseHost.addVirtualNode(virtualNode);
                break;
            }
        }
    }

    private List<Node> freeBaseHosts() {
        return baseHosts
                .stream()
                .filter(node -> ! node.hasChildren())
                .collect(Collectors.toList());
    }

    private Capacity getTotalUsedCapacity() {
        return baseHosts
                .stream()
                .map(Node::getUsedCapacity)
                .reduce(new Capacity(), (c1, c2) -> {
                    c1.add(c2);
                    return c1;
                });
    }

    private Capacity getTotalFreeCapacity() {
        return baseHosts
                .stream()
                .map(Node::getFreeCapacity)
                .reduce(new Capacity(), (c1, c2) -> {
                    c1.add(c2);
                    return c1;
                });
    }

    private Capacity getTotalFreeUsableCapacity() {
        return baseHosts
                .stream()
                .map(Node::getFreeCapacity)
                .filter(Capacity::isUsable)
                .reduce(new Capacity(), (c1, c2) -> {
                    c1.add(c2);
                    return c1;
                });
    }

    private String toJson() {
        ObjectMapper mapper = new ObjectMapper();
        Map<String,Object> nodes = new HashMap<>();
        ArrayList<Map> jsonBaseHosts = new ArrayList<>();
        for (Node basehost : baseHosts){
            jsonBaseHosts.add(basehost.toMap());
        }
        nodes.put("nodes", jsonBaseHosts);
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(nodes);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private static void distributeVirtualNodes(String repoFile, ToIntFunction<Node> optFunc) throws OutOfCapacityException {
        NodeRepo repo = new NodeRepo(repoFile);
        for (Node node: repo.virtualNodes) {
            repo.assignVirtualNodeToBaseHost(node, optFunc);
        }
        System.out.println("Free base hosts: " + repo.freeBaseHosts().size());
        System.out.println("Total used capacity: " + repo.getTotalUsedCapacity().toFlavor()
                + ",\t\tnormalised: " + repo.getTotalUsedCapacity().normalized().toFlavor());
        System.out.println("Total free capacity: " + repo.getTotalFreeCapacity().toFlavor()
                + ",\t\tnormalised: " + repo.getTotalFreeCapacity().normalized().toFlavor());
        System.out.println("Total free usable capacity: " + repo.getTotalFreeUsableCapacity().toFlavor()
                + ",\tnormalised: " + repo.getTotalFreeUsableCapacity().normalized().toFlavor());
        System.out.println("Average flavor: " + repo.getTotalUsedCapacity().dividedBy(repo.virtualNodes.size()).toFlavor());
        //System.out.println(repo.toJson());
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Minimum one repo file, please ...");
            System.exit(1);
        }

        List<ToIntFunction<Node>> optFunctions = Arrays.asList(
                Node::getFreeCPU,    // 1
                Node::getFreeMemory  // 2
        );

        for (String repofile : args) {
            System.out.println("\n\nEvaluating " + repofile);
            int iteration = 0;
            for (ToIntFunction<Node> fn : optFunctions) {
                System.out.println("\nIteration: " + iteration++);
                try {
                    distributeVirtualNodes(repofile, fn);
                }
                catch (OutOfCapacityException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }
}
