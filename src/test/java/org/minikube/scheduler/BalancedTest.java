package src.test.java.org.minikube.scheduler;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import src.main.java.org.minikube.model.DesiredTask;
import src.main.java.org.minikube.model.Node;
import src.main.java.org.minikube.scheduler.Balanced;

public class BalancedTest {

    @Test
    void testBalanced() {

        Balanced scheduler = new Balanced();

        // Add Nodes to the Scheduler
        scheduler.addNode(new Node("worker-small", 100));
        scheduler.addNode(new Node("worker-medium", 300));
        scheduler.addNode(new Node("worker-large", 1000));

        // Create a task that requires 256MB of RAM
        DesiredTask task = new DesiredTask("task-web-1", null, 1, 256);

        Node chosenNode = scheduler.schedule(task);

        assertNotNull(chosenNode, "The chosen node must not be null."); 
        
        // Should choose 'worker-large' because it has the maximum available memory between the existing nodes
        assertEquals("worker-large", chosenNode.getName(), "It should choose the node with the maximum leftover space.");
        
        // Verify that the memory was actually allocated (1000-256=744).
        assertEquals(744, chosenNode.getAvailableMemoryMB(), "The available RAM on the medium node should drop to 744 MB.");
    }

    @Test
    void testClusterFullReturnsNull() {

        Balanced scheduler = new Balanced();

        // Add a small Node to the Scheduler
        scheduler.addNode(new Node("worker-small", 100)); 

        // Create a task that requires 2000MB of RAM
        DesiredTask giantTask = new DesiredTask("task-db-1", null, 1, 2000);
        
        Node chosenNode = scheduler.schedule(giantTask);

        //Should not find any available node which can hosts the task
        assertNull(chosenNode, "The scheduler must return null if there is no space in the cluster.");
    }
}