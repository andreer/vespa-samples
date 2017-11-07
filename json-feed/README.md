# json-feed
Java utility to create a Vespa-feed in Json

mvn install && java -jar target/vespafeed-1.0-SNAPSHOT-jar-with-dependencies.jar 1

java -jar vespa-http-client-jar-with-dependencies.jar --host localhost < f.json

java -jar target/vespafeed-1.0-SNAPSHOT-jar-with-dependencies.jar 1 | java -jar vespa-http-client-jar-with-dependencies.jar --host localhost

deploy prepare src/main/application && deploy activate


vespa-remove-index -force

curl -X DELETE http://localhost:8080/document/v1/iddoc/iddoc/docid/0
