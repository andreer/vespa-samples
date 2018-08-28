package com.kkraune;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class NodeRepo {
    private List<Node> virtualNodes;
    private List<Node> baseHosts;

    private NodeRepo(String filename){
        virtualNodes = new ArrayList<Node>();
        baseHosts    = new ArrayList<Node>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            ObjectMapper mapper = new ObjectMapper();
            JsonNode noderepo = mapper.readTree(reader);
            Iterator<JsonNode> jsonNodes = noderepo.path("nodes").iterator();
            while (jsonNodes.hasNext()) {
                Node node = new Node(jsonNodes.next());
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

    private void AssignVirtualNodeToBaseHost(Node virtualNode) throws OutOfCapacityException {
        List<Node> candidateBaseHosts = new ArrayList<Node>();
        for (Node baseHost : baseHosts){
            if (baseHost.getFreeCapacity().canFit(virtualNode.getMaxCapacity())){
                candidateBaseHosts.add(baseHost);
            }
        }
        //System.out.println(candidateBaseHosts.size() + " candidates for " + node.hostname.substring(0, node.hostname.indexOf(".")));
        if (candidateBaseHosts.size() == 0) {
            throw new OutOfCapacityException("No space for " + virtualNode.hostname + " " + virtualNode.getMaxCapacity().toFlavor());
        }

        Optional<Node> bestBaseHost = candidateBaseHosts
                .stream()
                .min(Comparator.comparingInt(Node::getFreeCPU));
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

    private void AssignNodesToBaseHosts() throws OutOfCapacityException {
        for (Node node: virtualNodes) {
            AssignVirtualNodeToBaseHost(node);
        }
    }

    private List<Node> freeBaseHosts() {
        return baseHosts
                .stream()
                .filter(Node::hasChildren)
                .collect(Collectors.toList());
    }

    private Capacity getTotalFreeCapacity() {
        return baseHosts
                .stream()
                .map(n -> n.getFreeCapacity())
                .reduce(new Capacity(), (c1, c2) -> {
                    c1.add(c2);
                    return c1;
                });
    }

    private Capacity getTotalFreeUsableCapacity() {
        return baseHosts
                .stream()
                .map(node -> node.getFreeCapacity())
                .filter(capacity -> { return capacity.isUsable(); })
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

    public static void main(String[] args) {
        String inputRepo = "nodes.json";
        for (String arg : args) {
            inputRepo =  arg;
        }

        NodeRepo repo = new NodeRepo(inputRepo);
        try {
            repo.AssignNodesToBaseHosts();
            System.out.println("Free base hosts: " + repo.freeBaseHosts().size());
            System.out.println("Total free capacity: " + repo.getTotalFreeCapacity().toFlavor());
            System.out.println("Total free usable capacity: " + repo.getTotalFreeUsableCapacity().toFlavor());
            //System.out.println(repo.toJson());
        } catch (OutOfCapacityException e) {
            System.out.println(repo.toJson());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
