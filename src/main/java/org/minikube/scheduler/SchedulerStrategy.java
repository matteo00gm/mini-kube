package org.minikube.scheduler;

import org.minikube.model.DesiredTask;
import org.minikube.model.Node;

public interface SchedulerStrategy {

    void addNode(Node node); 
    Node schedule(DesiredTask task); 
}
