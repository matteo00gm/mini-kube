package org.minikube.engine;

import org.junit.jupiter.api.Test;

import org.minikube.model.DesiredTask;
import org.minikube.model.Node;
import org.minikube.model.RunningPod;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

public class KubeletTest {

    @Test
    void testSuccessfulPodStartup() throws InterruptedException {
        Kubelet kubelet = new Kubelet();
        Node workerNode = new Node("worker-1", 2048);
        
        DesiredTask task = new DesiredTask(
            "task-java-check", 
            List.of("java", "-version"), 
            1,
            256
        );

        RunningPod pod = kubelet.startPod(task, workerNode);

        assertNotNull(pod, "The Kubelet should have returned a valid RunningPod");
        assertEquals(RunningPod.PodStatus.RUNNING, pod.getStatus(), "Pod should be RUNNING");
        assertNotNull(pod.getProcess(), "The underlying OS Process must not be null");
        assertEquals("worker-1", pod.getAssignedNode().getName(), "Pod should be assigned to worker-1");

        // Cleanup: Wait for the "java -version" command to finish, then clean up resources
        pod.getProcess().waitFor();
        pod.markAsDead();
    }

    @Test
    void testHandlingOfInvalidCommand() {
        Kubelet kubelet = new Kubelet();
        Node workerNode = new Node("worker-1", 2048);
        
        DesiredTask badTask = new DesiredTask(
            "task-crash", 
            List.of("this-command-does-not-exist"), 
            1, 
            256
        );

        RunningPod pod = kubelet.startPod(badTask, workerNode);

        assertNull(pod, "The Kubelet should return null if the OS throws an IOException (command not found)");
    }
}