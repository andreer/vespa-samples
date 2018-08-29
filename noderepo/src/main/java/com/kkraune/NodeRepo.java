package com.kkraune;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.function.IntSupplier;
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
            JsonNode noderepo = mapper.readTree(reader);
            Iterator<JsonNode> jsonNodes = noderepo.path("nodes").iterator();
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

    private void AssignNodesToBaseHosts(ToIntFunction<Node> optFunc) throws OutOfCapacityException {
        for (Node node: virtualNodes) {
            AssignVirtualNodeToBaseHost(node, optFunc);
        }
    }

    private void AssignVirtualNodeToBaseHost(Node virtualNode, ToIntFunction<Node> optFunc) throws OutOfCapacityException {
        List<Node> candidateBaseHosts = new ArrayList<Node>();
        for (Node baseHost : baseHosts){
            if (!baseHost.getFreeCapacity().canFit(virtualNode.getMaxCapacity())){
                continue;
            }
            if (baseHost.hasAppInstance(virtualNode.appInstance)) {
                continue;
            }
            candidateBaseHosts.add(baseHost);
        }
        //System.out.println(candidateBaseHosts.size() + " candidates for " + node.hostname.substring(0, node.hostname.indexOf(".")));
        if (candidateBaseHosts.size() == 0) {
            throw new OutOfCapacityException("No space for " + virtualNode.hostname + " " + virtualNode.getMaxCapacity().toFlavor());
        }

        Optional<Node> bestBaseHost = candidateBaseHosts
                .stream()
                .min(Comparator.comparingInt(optFunc));
        AddVirtualNodeToBaseHost(bestBaseHost.orElse(new Node()).hostname, virtualNode);
    }

    public void AddVirtualNodeToBaseHost(String baseHostName, Node virtualNode) {
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
                .filter(Node::hasChildren)
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
        repo.AssignNodesToBaseHosts(optFunc);
        System.out.println("Free base hosts: " + repo.freeBaseHosts().size());
        System.out.println("Total used capacity: " + repo.getTotalUsedCapacity().toFlavor()
                + ", normalised: " + repo.getTotalUsedCapacity().normalized().toFlavor());
        System.out.println("Total free capacity: " + repo.getTotalFreeCapacity().toFlavor()
                + ", normalised: " + repo.getTotalFreeCapacity().normalized().toFlavor());
        System.out.println("Total free usable capacity: " + repo.getTotalFreeUsableCapacity().toFlavor()
                + ", normalised: " + repo.getTotalFreeUsableCapacity().normalized().toFlavor());
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
