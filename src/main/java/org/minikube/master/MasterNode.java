package org.minikube.master;

import org.minikube.scheduler.BinPack;
import java.util.List;

public class MasterNode {

    private final ApiServer server;
    private final RaftManager raft;

    public MasterNode(int port, String nodeUrl, List<String> peerUrls) {

        // ClusteState is the Memory -> it contains the scheduler, the tasks and the active nodes
        ClusterState state = new ClusterState(new BinPack());

        // RaftManger is the Brain of the system, using the Raft consensus algorithm, makes the masters nodes talk to each other
        this.raft = new RaftManager(nodeUrl, peerUrls, state);
        
        this.server = new ApiServer(port, state, raft);
    }

    public void start() {
        server.start();
        raft.start();
    }

    public static void main(String[] args) {
        
        List<String> peers1 = List.of("http://localhost:7071", "http://localhost:7072");
        MasterNode master1 = new MasterNode(7070, "http://localhost:7070", peers1);
        master1.start();

        List<String> peers2 = List.of("http://localhost:7070", "http://localhost:7072");
        MasterNode master2 = new MasterNode(7071, "http://localhost:7071", peers2);
        master2.start();

        List<String> peers3 = List.of("http://localhost:7070", "http://localhost:7071");
        MasterNode master3 = new MasterNode(7072, "http://localhost:7072", peers3);
        master3.start();
    }
}