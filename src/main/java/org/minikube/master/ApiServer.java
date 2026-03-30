package org.minikube.master;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;

import org.minikube.model.DesiredTask;
import org.minikube.model.Node;
import org.minikube.model.Heartbeat.AppendEntriesRequest;
import org.minikube.model.Heartbeat.AppendEntriesResponse;
import org.minikube.model.Vote.VoteRequest;
import org.minikube.model.Vote.VoteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// This class takes HTTP requests and hands them to either the ClusterState or the RaftManager
public class ApiServer {

    private static final Logger log = LoggerFactory.getLogger(ApiServer.class);

    private final int port;
    private final ClusterState state;
    private final RaftManager raft;
    private final ObjectMapper jsonMapper = new ObjectMapper();
    private Javalin app;
    private final HttpClient proxyClient = HttpClient.newHttpClient();

    public ApiServer(int port, ClusterState state, RaftManager raft) {
        this.port = port;
        this.state = state;
        this.raft = raft;
    }

    public void start() {
        app = Javalin.create().start(port);
        log.info("API Server listening on port {}", port);

        // =======================
        // WORKER & USER ENDPOINTS
        // =======================
        app.post("/register", ctx -> {
            if (forwardToMaster(ctx)) return;
            
            Node workerNode = jsonMapper.readValue(ctx.body(), Node.class);
            if (state.isActive(workerNode.getName())) {
                ctx.status(204).result("Node already exists.");
                return;
            }
            state.registerNode(workerNode);
            ctx.status(200).result("Node registered.");
        });

        app.post("/submit-task", ctx -> {
            if (forwardToMaster(ctx)) return;

            DesiredTask task = jsonMapper.readValue(ctx.body(), DesiredTask.class);
            Node chosenNode = state.scheduleTask(task);

            if (chosenNode != null) {
                ctx.status(202).result("Task Scheduled on " + chosenNode.getName());
            } else {
                ctx.status(503).result("Error: Insufficient Cluster Memory.");
            }
        });

        app.get("/poll-tasks/{workerName}", ctx -> {
            if (forwardToMaster(ctx)) return;

            DesiredTask task = state.pollTask(ctx.pathParam("workerName"));
            if (task != null) {
                ctx.status(200).json(task);
            } else {
                ctx.status(204);
            }
        });

        // =======================
        // RAFT ENDPOINTS
        // =======================
        app.post("/request-vote", ctx -> {
            VoteRequest req = jsonMapper.readValue(ctx.body(), VoteRequest.class);
            VoteResponse res = raft.handleVoteRequest(req);
            ctx.json(res);
        });

        app.post("/append-entries", ctx -> {
            AppendEntriesRequest req = jsonMapper.readValue(ctx.body(), AppendEntriesRequest.class);
            AppendEntriesResponse res = raft.handleAppendEntries(req);
            ctx.json(res);
        });
    }

    public void stop() {
        if (app != null) app.stop();
    }

    private boolean forwardToMaster(Context ctx) {
        if (raft.isLeader()) {
            return false; 
        }

        String leaderUrl = raft.getLeaderUrl();
        
        if (leaderUrl == null) {
            ctx.status(503).result("Cluster is currently electing a new leader. Try again in a few milliseconds.");
            return true; 
        }

        log.info("Proxying {} {} to Leader at {}", ctx.method(), ctx.path(), leaderUrl);

        try {
            BodyPublisher body = ctx.body().isEmpty() 
                ? BodyPublishers.noBody() 
                : BodyPublishers.ofString(ctx.body());

            HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(leaderUrl + ctx.path()))
                    .method(ctx.method().toString(), body)
                    .header("Content-Type", "application/json")
                    .build();

            HttpResponse<String> response = proxyClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            ctx.status(response.statusCode()).result(response.body());
        } catch (Exception e) {
            log.error("Proxy failed", e);
            ctx.status(500).result("Failed to forward request to Leader.");
        }
        
        return true;
    }
}