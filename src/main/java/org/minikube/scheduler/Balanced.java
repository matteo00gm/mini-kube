package src.main.java.org.minikube.scheduler;

import java.util.Comparator;
import java.util.PriorityQueue;

import src.main.java.org.minikube.model.DesiredTask;
import src.main.java.org.minikube.model.Node;

public class Balanced implements SchedulerStrategy {

    private final PriorityQueue<Node> maxHeap = new PriorityQueue<>(
        Comparator.comparingInt(Node::getAvailableMemoryMB).reversed()
            .thenComparing(Node::getTotalMemoryMB)
            .thenComparing(Node::getName)
    );

    @Override
    public void addNode(Node node) {
        maxHeap.offer(node);
    }

    @Override
    public Node schedule(DesiredTask task) {
        Node topNode = maxHeap.peek();
        
        if (topNode != null && topNode.canHost(task.requiredMemoryMB())) {
            maxHeap.poll();
            topNode.allocateMemory(task.requiredMemoryMB()); 
            maxHeap.offer(topNode); 
            return topNode;
        }

        // since it's a maxHeap, no other Node can host this task
        return null;
    }
}