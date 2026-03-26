package org.minikube.master;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.minikube.model.Vote.VoteRequest;
import org.minikube.model.Vote.VoteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class RaftManager {

    private static final Logger log = LoggerFactory.getLogger(RaftManager.class);

    private final String nodeUrl;
    private final List<String> peerUrls;

    // Raft State
    private RaftState currentState = RaftState.FOLLOWER;
    private int currentTerm = 0;
    private String votedFor = null;
    private final AtomicLong lastHeartbeatTime = new AtomicLong(System.currentTimeMillis());

    // Tools
    private final Random random = new Random();
    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(500))
            .build();

    public RaftManager(String nodeUrl, List<String> peerUrls, ClusterState state) {
        this.nodeUrl = nodeUrl;
        this.peerUrls = peerUrls;
    }

    public void start() {
        startElectionTimer();
    }

    // ====================================================================
    // INTERNAL RAFT LOOPS
    // ====================================================================
    private void startElectionTimer() {
        Thread timerThread = new Thread(() -> {
            while (true) {
                try {
                    int electionTimeout = 150 + random.nextInt(150);
                    Thread.sleep(electionTimeout);

                    long timeSinceLastHeartbeat = System.currentTimeMillis() - lastHeartbeatTime.get();

                    if (currentState != RaftState.LEADER && timeSinceLastHeartbeat >= electionTimeout) {
                        log.warn("Election timeout reached! No heartbeat from Leader.");
                        startElection();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        timerThread.setDaemon(true);
        timerThread.start();
    }

    private void startElection() {
        currentState = RaftState.CANDIDATE;
        currentTerm++;
        votedFor = nodeUrl;

        //voting is considered as a heartbeat
        lastHeartbeatTime.set(System.currentTimeMillis());
        log.info("Transitioned to CANDIDATE. Starting election for Term {}", currentTerm);
        askConsensus();
    }

    private void askConsensus() {
        AtomicInteger votesReceived = new AtomicInteger(1);
        int totalNodes = peerUrls.size() + 1;
        int votesNeeded = (totalNodes / 2) + 1;

        try {
            String voteRequest = jsonMapper.writeValueAsString(new VoteRequest(nodeUrl, currentTerm));
            BodyPublisher body = BodyPublishers.ofString(voteRequest);

            for (String peerUrl : peerUrls) {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(peerUrl + "/request-vote"))
                        .header("Content-Type", "application/json")
                        .POST(body)
                        .build();

                httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        try {
                            VoteResponse voteResponse = jsonMapper.readValue(response.body(), VoteResponse.class);
                            if (voteResponse.voteGranted()) {
                                int currentVotes = votesReceived.incrementAndGet();
                                log.info("Received vote! (Total: {}/{})", currentVotes, votesNeeded);
                                
                                if (currentVotes >= votesNeeded && currentState == RaftState.CANDIDATE) {
                                    becomeLeader();
                                }
                            }
                        } catch (Exception e) {
                            log.debug("Failed to parse vote response from {}", peerUrl);
                        }
                    })
                    .exceptionally(ex -> {
                        log.debug("Peer {} is offline.", peerUrl);
                        return null;
                    });
            }
        } catch (Exception e) {
            log.error("Failed to construct vote request", e);
        }
    }

    private void becomeLeader() {
        currentState = RaftState.LEADER;
        log.info("Promoted to leader for term {}", currentTerm);
    }

    // ====================================================================
    // INCOMING REQUESTS (Called by the ApiServer)
    // ====================================================================
    public synchronized VoteResponse handleVoteRequest(VoteRequest req) {
        if (req.term() > currentTerm) {
            currentTerm = req.term();
            currentState = RaftState.FOLLOWER;
            votedFor = null;
        }

        boolean voteGranted = false;

        if (req.term() == currentTerm && (votedFor == null || votedFor.equals(req.candidateId()))) {
            voteGranted = true;
            votedFor = req.candidateId();
            lastHeartbeatTime.set(System.currentTimeMillis());
            log.info("Voted for {} in Term {}", req.candidateId(), currentTerm);
        } else {
            log.info("Denied vote to {} in Term {}", req.candidateId(), currentTerm);
        }

        return new VoteResponse(currentTerm, voteGranted);
    }
}