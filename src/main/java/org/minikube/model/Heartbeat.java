package org.minikube.model;

public class Heartbeat {
    public record AppendEntriesRequest(int term, String leaderId) {}
    public record AppendEntriesResponse(int term, boolean success) {}
}
