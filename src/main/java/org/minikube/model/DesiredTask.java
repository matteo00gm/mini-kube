package org.minikube.model;

import java.util.List;

public record DesiredTask (String id, List<String> command, int replicas, int requiredMemoryMB) {}