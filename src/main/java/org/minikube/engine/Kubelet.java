package src.main.java.org.minikube.engine;

import java.io.IOException;
import java.util.UUID;

import src.main.java.org.minikube.model.DesiredTask;
import src.main.java.org.minikube.model.Node;
import src.main.java.org.minikube.model.RunningPod;
import src.main.java.org.minikube.model.RunningPod.PodStatus;

public class Kubelet {
    public RunningPod startPod(DesiredTask task, Node assignedNode) {

        try {
            ProcessBuilder pb = new ProcessBuilder(task.command());
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);

            Process process = pb.start();

            String uniquePodId = task.id() + "-" + UUID.randomUUID().toString();

            RunningPod pod = new RunningPod(uniquePodId, task.id(), process, assignedNode, task.requiredMemoryMB());
            pod.setStatus(PodStatus.RUNNING);

            return pod;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
