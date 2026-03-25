package org.minikube.master;

import io.javalin.Javalin;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.minikube.model.DesiredTask;
import org.minikube.model.Node;
import org.minikube.scheduler.SchedulerStrategy;
import org.minikube.scheduler.BinPack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MasterNode {

    private static final Logger log = LoggerFactory.getLogger(MasterNode.class);

    private final SchedulerStrategy scheduler;
    private final ConcurrentHashMap<String, Node> activeNodes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Queue<DesiredTask>> assignedTasks = new ConcurrentHashMap<>();
    private final ObjectMapper jsonMapper = new ObjectMapper();
    private Javalin app;

    public MasterNode(SchedulerStrategy scheduler) {
        this.scheduler = scheduler;
    }

    public void start(int port) {

        app = Javalin.create().start(port);
        log.info("Server listening on port {}", port);

        app.post("/register", ctx -> {
            Node workerNode = jsonMapper.readValue(ctx.body(), Node.class);
            String workerName = workerNode.getName();

            if (activeNodes.containsKey(workerName)) {
                log.info("Node {} already exists.", workerName);
                ctx.status(204).result("Node already exists.");
                return;
            }

            activeNodes.put(workerName, workerNode);
            scheduler.addNode(workerNode);
            assignedTasks.put(workerName, new ConcurrentLinkedQueue<>());

            log.info("Node Joined: {} ({}MB RAM)", workerName, workerNode.getAvailableMemoryMB());
            ctx.status(200).result("Node registered.");
        });

        app.post("/submit-task", ctx -> {
            DesiredTask task = jsonMapper.readValue(ctx.body(), DesiredTask.class);
            log.info("Received new task request: {}", task.id());

            Node chosenNode = scheduler.schedule(task);

            if (chosenNode != null) {
                assignedTasks.get(chosenNode.getName()).offer(task);
                log.info("Scheduled task '{}' onto -> {}", task.id(), chosenNode.getName());
                ctx.status(202).result("Task Scheduled on " + chosenNode.getName());
            } else {
                log.warn("Failed to schedule task '{}': Insufficient Cluster Memory.", task.id());
                ctx.status(503).result("Error: Insufficient Cluster Memory.");
            }
        });

        app.get("/poll-tasks/{workerName}", ctx -> {
            String workerName = ctx.pathParam("workerName");
            Queue<DesiredTask> tasks = assignedTasks.get(workerName);

            if (tasks != null && !tasks.isEmpty()) {
                DesiredTask taskToRun = tasks.poll();
                ctx.status(200).json(taskToRun);
            } else {
                ctx.status(204);
            }
        });
    }

    public static void main(String[] args) {
        SchedulerStrategy strategy = new BinPack();
        MasterNode master = new MasterNode(strategy);
        master.start(7070);
    }
}