package org.minikube.model;

import java.io.Serializable;

public class RunningPod implements Serializable {
    
    public enum PodStatus {
        STARTING, RUNNING, DEAD
    }

    private final String podId;
    private final String taskId;
    private final Process process;
    private final Node assignedNode;
    private final int memoryConsumedMB;

    private PodStatus status;

    public RunningPod(String podId, String taskId, Process process, Node assignedNode, int memoryConsumedMB) {
        this.podId = podId;
        this.taskId = taskId;
        this.process = process;
        this.assignedNode = assignedNode;
        this.memoryConsumedMB = memoryConsumedMB;
        
        this.status = PodStatus.STARTING; 
    }

    public void markAsDead(){
        this.status = PodStatus.DEAD;
        this.assignedNode.freeMemory(memoryConsumedMB);

    }
    
    public String getPodId() { return podId; }
    public String getTaskId() { return taskId; }
    public Process getProcess() { return process; }
    public Node getAssignedNode() { return assignedNode; }
    public int getMemoryConsumedMB() { return memoryConsumedMB; }
    public PodStatus getStatus() { return status; }

    public void setStatus(PodStatus status) {
        this.status = status;
    }
}
