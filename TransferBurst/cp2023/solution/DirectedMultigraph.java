package cp2023.solution;

import cp2023.base.ComponentId;
import cp2023.base.ComponentTransfer;
import cp2023.base.DeviceId;

import java.util.*;

class DirectedMultigraph {

    // edges of each vertex
    private Map<DeviceId, ArrayList<Edge>> adjacencyList = new HashMap<>();
    public class Edge {
        DeviceId source;
        DeviceId target;
        ComponentId label;
        ComponentTransfer transfer;

        public Edge(DeviceId source, DeviceId target, ComponentId label, ComponentTransfer transfer) {
            this.source = source;
            this.target = target;
            this.label = label;
            this.transfer = transfer;
        }
    }
    public void addVertex(DeviceId label) {
        adjacencyList.putIfAbsent(label, new ArrayList<>());
    }
    public void addEdge(DeviceId source, DeviceId target, ComponentId label, ComponentTransfer transfer) {
        adjacencyList.computeIfAbsent(source, k -> new ArrayList<>()).add(new Edge(source, target, label, transfer));
    }
    public void removeEdge(DeviceId source, DeviceId target, ComponentId label, ComponentTransfer transfer) {
        ArrayList<Edge> edges = adjacencyList.get(source);
        if (edges != null) {
            Iterator<Edge> iterator = edges.iterator();
            while (iterator.hasNext()) {
                Edge edge = iterator.next();
                if (edge.target.equals(target) && edge.label.equals(label) && edge.transfer == transfer) {
                    iterator.remove();
                    break;
                }
            }
            adjacencyList.put(source, new ArrayList<>(edges));
        }
    }
    public void removeEdge(Edge e) {
        List<Edge> edges = adjacencyList.get(e.source);
        if (edges != null) {
            Iterator<Edge> iterator = edges.iterator();
            while (iterator.hasNext()) {
                Edge edge = iterator.next();
                if (edge.target.equals(e.target) && edge.label.equals(e.label) && edge.transfer == e.transfer) {
                    iterator.remove();
                    break;
                }
            }
            adjacencyList.put(e.source, new ArrayList<>(edges));
        }
    }
    public List<Edge> findCycle(DeviceId startVertex){
        ArrayList<Edge> edges = new ArrayList<>();
        ArrayList<Edge> cycle = new ArrayList<>();
        HashSet<DeviceId> visited = new HashSet<>();
        boolean[] found = new boolean[1];
        found[0] = false;
        FindCycleUtil(startVertex, cycle, visited, startVertex, edges, found);
        return cycle;
    }
    private void FindCycleUtil(DeviceId currVertex, ArrayList<Edge> cycleEdges,
                                HashSet<DeviceId> visited, DeviceId target, ArrayList<Edge> recursiveList, boolean[] found){
        if(found[0]){
            return;
        }
        if(visited.contains(currVertex) && currVertex != target){
            return;
        }
        if(visited.contains(currVertex) && currVertex == target){
             found[0] = true;
             cycleEdges.addAll(recursiveList);
             return;
        }
        if(!visited.contains(currVertex)){
            visited.add(currVertex);
            List<Edge> list = adjacencyList.get(currVertex);
            for(Edge e : list){
                recursiveList.add(e);
                if(e.target.equals(target)){
                    found[0] = true;
                    cycleEdges.addAll(recursiveList);
                    return;
                }
                FindCycleUtil(e.target, cycleEdges, visited, target, recursiveList, found);
                recursiveList.remove(e);
            }
        }
    }
}
