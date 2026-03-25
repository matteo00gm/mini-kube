package org.minikube.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.minikube.engine.Kubelet;
import org.minikube.model.DesiredTask;
import org.minikube.model.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class WorkerNode {

    private static final Logger log = LoggerFactory.getLogger(WorkerNode.class);
    private static final String WORKER_NAME = "worker-" + ProcessHandle.current().pid();
    private final String MASTER_URL = System.getenv().getOrDefault("MASTER_URL", "http://localhost:7070");

    private final Kubelet engine = new Kubelet();
    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(
                    Long.parseLong(System.getenv().getOrDefault("CONNECT_TIMEOUT_SECONDS", "5"))
            ))
            .build();

    private final int totalRamMB;

    public WorkerNode(int totalRamMB) {
        this.totalRamMB = totalRamMB;
    }

    public void start() throws Exception {
        log.info("Starting worker node {}", WORKER_NAME);

        Node workerNode = new Node(WORKER_NAME, totalRamMB);

        registerWithMaster(workerNode);

        log.info("Start polling");

        while (true) {
            pollForTasks(workerNode);
            Thread.sleep(2000);
        }
    }

    private void registerWithMaster(Node newNode) throws Exception {
        String jsonPayload = jsonMapper.writeValueAsString(newNode);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(MASTER_URL + "/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            log.info("Worker node successfully registered. Response: {}", response.body());
        } else {
            log.error("Failed to register worker node: {}", response.body());
            throw new RuntimeException("Worker Node registration failed.");
        }
    }

    private void pollForTasks(Node assignedNode) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(MASTER_URL + "/poll-tasks/" + WORKER_NAME))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 && response.body() != null && !response.body().isEmpty()) {
                
                DesiredTask task = jsonMapper.readValue(response.body(), DesiredTask.class);
                log.info("Received new task from Master: {}", task.id());

                engine.startPod(task, assignedNode);
                
            } else if (response.statusCode() == 204) {
                log.debug("No tasks in queue.");
            }
        } catch (Exception e) {
            log.warn("Error while polling: {}", e);
        }
    }

    public static void main(String[] args) throws Exception {
        WorkerNode worker = new WorkerNode(2048);
        worker.start();
    }
}