# Examples

## The in-process demo (no cloud, no network)

```bash
mvn install
mvn -q -pl ascent-cloud exec:java
```

Watch one worker learn a request's real cost, a burst scale the pool up, and the pool drain back down.

## The HTTP gateway

```bash
mvn -q -pl ascent-cloud exec:java -Dexec.mainClass=dev.sobatista.ascent.cloud.Gateway -Dexec.args="8000"
```

Then, in another shell:

```bash
curl 'http://localhost:8000/solve?w=256&h=256&s=ASTAR'
# {"worker":"worker-1","weight":...,"nodesExpanded":...,"basicBlocks":...,"loads":...,"stores":...}

curl 'http://localhost:8000/solve?w=512&h=512&s=BFS&x0=0&y0=0&x1=255&y1=255'
curl 'http://localhost:8000/cluster'
# {"workers":[{"id":"worker-1","load":...,"served":...}, ...]}
```

Fire several big requests in quick succession and watch `/cluster` grow new workers, then settle back down once they
finish.
