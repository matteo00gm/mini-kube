package src.test.java.org.minikube.scheduler;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import src.main.java.org.minikube.model.DesiredTask;
import src.main.java.org.minikube.model.Node;
import src.main.java.org.minikube.scheduler.BinPack;

public class BinPackTest {

    @Test
    void testBinPack() {

        BinPack scheduler = new BinPack();

        // Add Nodes to the Scheduler
        scheduler.addNode(new Node("worker-small", 100));
        scheduler.addNode(new Node("worker-medium", 300));
        scheduler.addNode(new Node("worker-large", 1000));

        // Create a task that requires 256MB of RAM
        DesiredTask task = new DesiredTask("task-web-1", null, 1, 256);

        Node chosenNode = scheduler.schedule(task);

        assertNotNull(chosenNode, "The chosen node must not be null."); 
        
        // Should choose 'worker-medium' because it offers the minimum memory waste between the existing nodes
        assertEquals("worker-medium", chosenNode.getName(), "It should choose the node with the minimum leftover space.");
        
        // Verify that the memory was actually allocated (300-256=44).
        assertEquals(44, chosenNode.getAvailableMemoryMB(), "The available RAM on the medium node should drop to 44 MB.");
    }

    @Test
    void testClusterFullReturnsNull() {

        BinPack scheduler = new BinPack();

        // Add a small Node to the Scheduler
        scheduler.addNode(new Node("worker-small", 100)); 

        // Create a task that requires 2000MB of RAM
        DesiredTask giantTask = new DesiredTask("task-db-1", null, 1, 2000);
        
        Node chosenNode = scheduler.schedule(giantTask);

        //Should not find any available node which can hosts the task
        assertNull(chosenNode, "The scheduler must return null if there is no space in the cluster.");
    }
}