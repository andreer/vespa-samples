# NodeRepo
Analyze the Node Repository

    java -jar target/noderepo-1.0-SNAPSHOT-jar-with-dependencies.jar -v nodes.json

    cat out.json | jq -r '.nodes[] | [.hostname, .relativeUsedCapacity] | @csv' | sed 's/d-//' | tr - , | tr -d '"' > ~/tmp/o.csv

    java -jar target/noderepo-1.0-SNAPSHOT-jar-with-dependencies.jar -jsonOnly us-central-1.json \
    | jq -r '.nodes[] | [.hostname, .relativeUsedCapacity] | @csv' | sed 's/d-//' | tr - , | tr -d '"'
