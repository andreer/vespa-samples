# visit-dump
Java utility to dump documents using visit


http://docs.vespa.ai/documentation/vespa-quick-start.html

docker run --detach --name vespa --hostname vespa-container --privileged \
  --volume /Users/myuser/github/kkraune/vespa-samples:/vespa-samples --publish 8080:8080 vespaengine/vespa

docker exec -it vespa bash

vespa-deploy prepare /vespa-samples/json-feed/src/main/application && vespa-deploy activate

java -jar vespa-http-client-jar-with-dependencies.jar --host localhost --port 8080 < f.json

java -jar target/json-feed-1.0-SNAPSHOT-jar-with-dependencies.jar 1 | \
  java -jar vespa-http-client-jar-with-dependencies.jar --host localhost --port 8080

curl -X DELETE http://localhost:8080/document/v1/iddoc/iddoc/docid/0
