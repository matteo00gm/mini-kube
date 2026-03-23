package src.main.java.org.minikube.scheduler;

import src.main.java.org.minikube.model.DesiredTask;
import src.main.java.org.minikube.model.Node;

public interface SchedulerStrategy {

    void addNode(Node node); 
    Node schedule(DesiredTask task); 
}
