# json-feed
Java utility to create a Vespa-feed in Json

mvn install && java -jar target/dotpd-1.0-SNAPSHOT-jar-with-dependencies.jar 1 > f.json

http://docs.vespa.ai/documentation/vespa-quick-start.html

docker run --detach --name vespa --hostname vespa-container --privileged \
  --volume /Users/myuser/github/kkraune/vespa-samples:/vespa-samples --publish 8080:8080 vespaengine/vespa

docker exec -it vespa bash

vespa-deploy prepare /vespa-samples/dotp/src/main/application && vespa-deploy activate

java -jar ../vespa-http-client-jar-with-dependencies.jar --host localhost --port 8080 < f.json

java -jar target/dotp-1.0-SNAPSHOT-jar-with-dependencies.jar 1 | \
  java -jar ../vespa-http-client-jar-with-dependencies.jar --host localhost --port 8080

curl -X DELETE http://localhost:8080/document/v1/iddoc/iddoc/docid/0

http://localhost:8080/search/?yql=select%20*%20from%20sources%20*%20where%20sddocname%20contains%20%22iddoc%22;

http://localhost:8080/search/?yql=select%20*%20from%20sources%20*%20where%20intArray=1;

http://localhost:8080/search/?yql=select%20*%20from%20sources%20*%20where%20intArray=1;&ranking=myrank&ranking.properties.dotProduct.queryvector={0:2.1,1:5}
