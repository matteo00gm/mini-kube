package org.minikube.master;

import org.minikube.model.DesiredTask;
import org.minikube.model.Node;
import org.minikube.scheduler.SchedulerStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

// This is a service class that keeps track of the active Nodes and assigns the Tasks
public class ClusterState {

    private static final Logger log = LoggerFactory.getLogger(ClusterState.class);

    private final SchedulerStrategy scheduler;
    private final ConcurrentHashMap<String, Node> activeNodes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Queue<DesiredTask>> assignedTasks = new ConcurrentHashMap<>();

    public ClusterState(SchedulerStrategy scheduler) {
        this.scheduler = scheduler;
    }

    public boolean isActive(String workerName) {
        return activeNodes.containsKey(workerName);
    }

    public void registerNode(Node workerNode) {
        String workerName = workerNode.getName();
        activeNodes.put(workerName, workerNode);
        scheduler.addNode(workerNode);
        assignedTasks.putIfAbsent(workerName, new ConcurrentLinkedQueue<>());
        log.info("Node Joined: {} ({}MB RAM)", workerName, workerNode.getAvailableMemoryMB());
    }

    public Node scheduleTask(DesiredTask task) {
        Node chosenNode = scheduler.schedule(task);
        if (chosenNode != null) {
            assignedTasks.get(chosenNode.getName()).offer(task);
            log.info("Scheduled task '{}' onto -> {}", task.id(), chosenNode.getName());
        } else {
            log.warn("Failed to schedule task '{}': Insufficient Cluster Memory.", task.id());
        }
        return chosenNode;
    }

    public DesiredTask pollTask(String workerName) {
        Queue<DesiredTask> tasks = assignedTasks.get(workerName);
        if (tasks != null && !tasks.isEmpty()) {
            return tasks.poll();
        }
        return null;
    }
}