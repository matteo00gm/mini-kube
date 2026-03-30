package org.minikube.model;

public class Heartbeat {
    public record AppendEntriesRequest(int term, String leaderUrl) {}
    public record AppendEntriesResponse(int term, boolean success) {}
}
