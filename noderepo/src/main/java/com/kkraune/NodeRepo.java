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
import java.util.stream.Collectors;

public class NodeRepo {
    private List<Node> virtualNodes;
    private List<Node> baseHosts;

    private NodeRepo(String filename) {
        virtualNodes = new ArrayList<Node>();
        baseHosts    = new ArrayList<Node>();
        try {
            BufferedReader reader        = new BufferedReader(new FileReader(filename));
            ObjectMapper mapper          = new ObjectMapper();
            JsonNode nodeRepo            = mapper.readTree(reader);
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
            //System.out.println("Base hosts: " + baseHosts.size() + ", Virtual nodes: " + virtualNodes.size());
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

    private void assignVirtualNodeToBaseHost(Node virtualNode) throws OutOfCapacityException {
        Random rnd = new Random();
        Optional<Node> bestBaseHost = baseHosts
                .stream()
                .filter(b -> { return   b.getFreeCapacity().canFit(virtualNode.getMaxCapacity()); })
                .filter(b -> { return ! b.hasAppInstance(virtualNode.appInstance); })
                //.reduce((n1, n2) -> n1.getFreeCPU() < n2.getFreeCPU() ? n1 : n2);
                //.reduce((n1, n2) -> n1.getFreeMemory() < n2.getFreeMemory() ? n1 : n2);
                //.reduce((n1, n2) -> n1.getFreeDisksize() < n2.getFreeDisksize() ? n1 : n2);
                //.reduce((n1, n2) -> n1.wastedCPU(virtualNode) < n2.wastedCPU(virtualNode) ? n1 : n2);
                //.reduce((n1, n2) -> n1.wastedMemory(virtualNode) < n2.wastedMemory(virtualNode) ? n1 : n2);
                //.reduce((n1, n2) -> n1.wastedDisksize(virtualNode) < n2.wastedCPU(virtualNode) ? n1 : n2);
                .reduce((n1, n2) -> rnd.nextDouble() > 0.5 ? n1 : n2);
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

    private static int distributeVirtualNodes(NodeRepo repo, boolean verbose) throws OutOfCapacityException {
        for (Iterator<Node> iterator = repo.virtualNodes.iterator(); iterator.hasNext();) { // assign the "too-large" flavors first
            Node node = iterator.next();
            if ("d-32-24-2400".equals(node.flavor) || "d-64-64-7680".equals(node.flavor)) {
                repo.assignVirtualNodeToBaseHost(node);
                iterator.remove();
            }
        }
        for (Node node : repo.virtualNodes) {
            repo.assignVirtualNodeToBaseHost(node);
        }
        if (verbose) {
            System.out.println(String.format("Repo capacity       : %20s, normalised: %10s",
                    repo.getRepoCapacity().toFlavorString(),
                    repo.getRepoCapacity().normalized().toFlavorString()));
            System.out.println(String.format("Used capacity       : %20s, normalised: %10s, of total: %s",
                    repo.getTotalUsedCapacity().toFlavorString(),
                    repo.getTotalUsedCapacity().normalized().toFlavorString(),
                    repo.getTotalUsedCapacity().fractionOf(repo.getRepoCapacity()).toFlavorString()));
            System.out.println(String.format("Free capacity       : %20s, normalised: %10s, of total: %s",
                    repo.getTotalFreeCapacity().toFlavorString(),
                    repo.getTotalFreeCapacity().normalized().toFlavorString(),
                    repo.getTotalFreeCapacity().fractionOf(repo.getRepoCapacity()).toFlavorString()));
            System.out.println(String.format("Free usable capacity: %20s, normalised: %10s, of total: %s",
                    repo.getTotalFreeUsableCapacity().toFlavorString(),
                    repo.getTotalFreeUsableCapacity().normalized().toFlavorString(),
                    repo.getTotalFreeUsableCapacity().fractionOf(repo.getRepoCapacity()).toFlavorString()));
            System.out.println("Average flavor: " + repo.getTotalUsedCapacity().dividedBy(repo.virtualNodes.size()).toFlavorString());
        }
        return repo.freeBaseHosts().size();
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Minimum one repo file, please ...");
            System.exit(1);
        }
        boolean verbose = false;
        int iterations = 1;
        for (String repofile : args) {
            if ("-v".equals(repofile)) {
                verbose = true;
                continue;
            }
            int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE, sum = 0, freeHosts = 0;
            System.out.println("\n\n" + repofile + ": ");
            for (int i=0; i<iterations; i++) {
                NodeRepo repo = new NodeRepo(repofile);
                Collections.shuffle(repo.virtualNodes);
                //repo.printFlavorDistribution();
                try {
                    freeHosts = distributeVirtualNodes(repo, verbose);
                }
                catch (OutOfCapacityException e) {
                    System.out.println(e.getMessage());
                    freeHosts = 0;
                }
                if (verbose) {
                    System.out.println("Iteration: " + i + ": Free base hosts: " + freeHosts + "\n");
                    System.out.println(repo.toJson());
                }
                min =  freeHosts < min ? freeHosts : min;
                max =  freeHosts > max ? freeHosts : max;
                sum += freeHosts;
            }
            System.out.println(String.format("Min: %d, max: %d, avg: %.2f", min, max, (float) sum / iterations));
        }
    }

    private String toJson() {
        ObjectMapper mapper      = new ObjectMapper();
        Map<String,Object> nodes = new HashMap<>();
        ArrayList<Map> sorted = baseHosts
                .stream()
                .map(Node::toMap)
                .sorted((m1, m2) -> {
                    return (int)(((float)m2.get("utilization") - (float)m1.get("utilization")) * 1000);
                })
                .collect(Collectors.toCollection(ArrayList::new));
        nodes.put("nodes", sorted);
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(nodes);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
