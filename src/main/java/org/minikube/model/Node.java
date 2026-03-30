package org.minikube.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Node {

    private final String name;
    private final int totalMemoryMB;
    private int availableMemoryMB;

    @JsonCreator
    public Node(@JsonProperty("name") String name, 
                @JsonProperty("totalMemoryMB") int totalMemoryMB) {
        this.name = name;
        this.totalMemoryMB = totalMemoryMB;
        this.availableMemoryMB = totalMemoryMB; 
    }

    public boolean canHost(int requiredRam) {
        return availableMemoryMB >= requiredRam;
    }

    public void allocateMemory(int ram) {
        if (!canHost(ram)) throw new IllegalStateException("Insufficient memory on Node " + name);
        availableMemoryMB -= ram;
    }

    public void freeMemory(int ram) {
        availableMemoryMB += ram;

        //safe check
        if (availableMemoryMB > totalMemoryMB) {
            availableMemoryMB = totalMemoryMB;
        }
    }

    public String getName() { return name; }
    public int getTotalMemoryMB() {  return totalMemoryMB; }
    public int getAvailableMemoryMB() { return availableMemoryMB; }
}