# tensor-feed
Java utility to create a tensor Vespa-feed in Json

    mvn install && java -jar target/dotp-tensor-1.0-SNAPSHOT-jar-with-dependencies.jar 1 > f.json

    docker run --detach --name vespa --hostname vespa-container --privileged \
    --volume /Users/myuser/github/kkraune/vespa-samples:/vespa-samples --publish 8080:8080 vespaengine/vespa

    docker exec -it vespa bash

    vespa-deploy prepare /vespa-samples/dotp-tensor/src/main/application && vespa-deploy activate

    curl -s --head http://localhost:8080/ApplicationStatus

    java -jar ../vespa-http-client-jar-with-dependencies.jar --host localhost --port 8080 < f.json

    java -jar target/dotp-tensor-1.0-SNAPSHOT-jar-with-dependencies.jar 1 | \
    java -jar ../vespa-http-client-jar-with-dependencies.jar --host localhost --port 8080

http://localhost:8080/search/?yql=select%20*%20from%20sources%20*%20where%20sddocname%20contains%20%22iddoc%22;&ranking=tensorrank

http://localhost:8080/search/?yql=select%20*%20from%20sources%20*%20where%20sddocname%20contains%20%22iddoc%22;&ranking=tensorrank&ranking.features.query(query_tensor)={{x:0,y:0}:1.0,{x:0,y:1}:2.0,{x:1,y:0}:3.0,{x:1,y:1}:4.0}

    curl -X DELETE http://localhost:8080/document/v1/iddoc/iddoc/docid/0
