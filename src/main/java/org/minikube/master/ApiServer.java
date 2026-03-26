package org.minikube.master;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import org.minikube.model.DesiredTask;
import org.minikube.model.Node;
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
            Node workerNode = jsonMapper.readValue(ctx.body(), Node.class);
            if (state.isActive(workerNode.getName())) {
                ctx.status(204).result("Node already exists.");
                return;
            }
            state.registerNode(workerNode);
            ctx.status(200).result("Node registered.");
        });

        app.post("/submit-task", ctx -> {
            DesiredTask task = jsonMapper.readValue(ctx.body(), DesiredTask.class);
            Node chosenNode = state.scheduleTask(task);

            if (chosenNode != null) {
                ctx.status(202).result("Task Scheduled on " + chosenNode.getName());
            } else {
                ctx.status(503).result("Error: Insufficient Cluster Memory.");
            }
        });

        app.get("/poll-tasks/{workerName}", ctx -> {
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
    }

    public void stop() {
        if (app != null) app.stop();
    }
}