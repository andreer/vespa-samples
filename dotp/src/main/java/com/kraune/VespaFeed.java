package com.kraune;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class VespaFeed {

    public static void main(String[] args) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayList<Map> feed = new ArrayList<>();

        Integer numDocs = 1;
        for (String arg : args) {
            numDocs =  Integer.parseInt(arg);
        }

        for(int i=0; i<numDocs; i++) {
            String id = "id:iddoc:iddoc::" + i;
            feed.add(buildDocument(id));
        }


        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(System.out, feed);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
        http://wiki.fasterxml.com/JacksonInFiveMinutes
     */
    static Map<String, Object> buildDocument(String id) {
        Map<String, Object> document = new HashMap<>();
        Map<String,Object> fields = new HashMap<>();

        /*
        fields.put("string", "myName");                           // Regular fields
        fields.put("integer", 42);
        fields.put("raw", "VW5rbm93biBhcnRpc3QgZnJvbSB0aGUgbW9vbg==");
        */

        ArrayList<Integer> intArr = new ArrayList<>();          // Array of integers
        intArr.add(1);
        intArr.add(101);
        fields.put("intArray", intArr);

        ArrayList<Float> floatArr = new ArrayList<>();
        floatArr.add(1.1f);
        floatArr.add(1.2f);
        fields.put("floatArray", floatArr);

        /*
        Map<String, String> nameStruct = new HashMap<>();        // Struct and Map
        nameStruct.put("first", "Joe");
        nameStruct.put("last", "Doe");
        fields.put("nameStruct", nameStruct);

        Map<String, Integer> weightedSet = new HashMap<>();      // Weighted set
        weightedSet.put("item 1", 1);
        weightedSet.put("item 2", 3);
        fields.put("weightedSet", weightedSet);

        ArrayList<Map> tensor = new ArrayList<>();            // Tensor
        Map<String, Object> cell = new HashMap<>();
        Map<String, String> address = new HashMap<>();
        address.put("x","0");
        address.put("y","1");
        address.put("z","2");
        cell.put("address", address);
        cell.put("value", 1.1);
        tensor.add(cell);
        fields.put("tensor", tensor);
        */

        fields.put("id", id);
        document.put("put", id);
        document.put("fields", fields);
        return document;
    }
}
