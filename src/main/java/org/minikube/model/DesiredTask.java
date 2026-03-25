package org.minikube.model;

import java.io.Serializable;
import java.util.List;

public record DesiredTask (String id, List<String> command, int targetReplicas, int requiredMemoryMB) implements Serializable {}