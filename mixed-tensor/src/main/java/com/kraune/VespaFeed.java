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

    static Map<String, Object> buildDocument(String id) {
        Map<String, Object> document = new HashMap<>();
        Map<String,Object> fields = new HashMap<>();
        Map<String, Object> tensor_attribute = new HashMap<>();
        ArrayList<Object> cells = new ArrayList<>();
        for(String yAdr : new String[]{"a", "b"}) {
            for (Integer xIndex = 0; xIndex < 2; xIndex++) {
                Map<String, Object> cell = new HashMap<>();
                Map<String, Object> address = new HashMap<>();
                address.put("x", xIndex.toString());
                //if ( (yAdr.codePointAt(0) % 2) == 0 ) {
                    address.put("y", yAdr);
                //}
                cell.put("address", address);
                cell.put("value", (xIndex+0.1));
                cells.add(cell);
            }
        }
        tensor_attribute.put("cells",cells);
        fields.put("tensor_attribute", tensor_attribute);

        fields.put("id", id);
        document.put("put", id);
        document.put("fields", fields);
        return document;
    }
}
