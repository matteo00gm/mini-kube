package org.minikube.model;

public class Vote {
    public record VoteRequest(String candidateId, int term) {}
    public record VoteResponse(int term, boolean voteGranted) {}
}