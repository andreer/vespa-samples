package com.kkraune;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
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

    private void printFlavorDistribution() {
        ConcurrentMap<String, AtomicInteger> flavorCounts = new ConcurrentHashMap<>();
        for (Node node : virtualNodes) {
            flavorCounts.putIfAbsent(node.flavor, new AtomicInteger(0));
            flavorCounts.get(node.flavor).incrementAndGet();
        }
        Set<String> flavors = flavorCounts.keySet();
        for (String key : flavors) {
            System.out.println(key + ":\t" + flavorCounts.get(key).get());
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
            throw new OutOfCapacityException("No space for " + virtualNode.hostname + " " + virtualNode.getMaxCapacity().toFlavorString());
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

    private Capacity getRepoCapacity() {
        return baseHosts
                .stream()
                .map(Node::getMaxCapacity)
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
        //repo.printFlavorDistribution();
        for (Iterator<Node> iterator = repo.virtualNodes.iterator(); iterator.hasNext();) { // assign the "too-large" flavors first
            Node node = iterator.next();
            if ("d-32-24-2400".equals(node.flavor) || "d-64-64-7680".equals(node.flavor)) {
                repo.assignVirtualNodeToBaseHost(node, optFunc);
                iterator.remove();
            }
        }
        for (Node node : repo.virtualNodes) {
            repo.assignVirtualNodeToBaseHost(node, optFunc);
        }
        System.out.println("Free base hosts: " + repo.freeBaseHosts().size());
        System.out.println("Repo capacity       : "
                + String.format("%20s", repo.getRepoCapacity().toFlavorString())
                + ", normalised: " + String.format("%10s", repo.getRepoCapacity().normalized().toFlavorString()));
        System.out.println("Used capacity       : "
                + String.format("%20s", repo.getTotalUsedCapacity().toFlavorString())
                + ", normalised: " + String.format("%10s", repo.getTotalUsedCapacity().normalized().toFlavorString())
                + ", of total: "   + repo.getTotalUsedCapacity().fractionOf(repo.getRepoCapacity()).toFlavorString());
        System.out.println("Free capacity       : "
                + String.format("%20s", repo.getTotalFreeCapacity().toFlavorString())
                + ", normalised: " + String.format("%10s", repo.getTotalFreeCapacity().normalized().toFlavorString())
                + ", of total: "   + repo.getTotalFreeCapacity().fractionOf(repo.getRepoCapacity()).toFlavorString());
        System.out.println("Free usable capacity: "
                + String.format("%20s", repo.getTotalFreeUsableCapacity().toFlavorString())
                + ", normalised: " + String.format("%10s", repo.getTotalFreeUsableCapacity().normalized().toFlavorString())
                + ", of total: "   + repo.getTotalFreeUsableCapacity().fractionOf(repo.getRepoCapacity()).toFlavorString());
        System.out.println("Average flavor: " + repo.getTotalUsedCapacity().dividedBy(repo.virtualNodes.size()).toFlavorString());
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
