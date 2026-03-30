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
    
}