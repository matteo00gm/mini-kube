package src.main.java.org.minikube.model;

public class Node {

    private final String name;
    private final int totalMemoryMB;
    private int availableMemoryMB;

    public Node(String name, int totalMemoryMB) {
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