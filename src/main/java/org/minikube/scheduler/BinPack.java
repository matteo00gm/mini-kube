package src.main.java.org.minikube.scheduler;

import java.util.Comparator;
import java.util.TreeSet;

import src.main.java.org.minikube.model.DesiredTask;
import src.main.java.org.minikube.model.Node;

public class BinPack implements SchedulerStrategy {

    private final TreeSet<Node> bst = new TreeSet<>(
        Comparator.comparingInt(Node::getAvailableMemoryMB).reversed()
            .thenComparing(Node::getTotalMemoryMB)
            .thenComparing(Node::getName)
    );

    @Override
    public void addNode(Node node) {
        bst.add(node);
    }

    @Override
    public Node schedule(DesiredTask task) {
        Node key = new Node("dummyNode", task.requiredMemoryMB());

        Node binPacked = bst.ceiling(key);

        if (binPacked != null) {
            bst.remove(binPacked);
            binPacked.allocateMemory(task.requiredMemoryMB());
            bst.add(binPacked);
            return binPacked;
        }

        return null;
    }
    
}
