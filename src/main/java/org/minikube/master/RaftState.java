package org.minikube.master;

public enum RaftState {
    FOLLOWER,
    CANDIDATE,
    LEADER
}