WORKFLOW
1) create a WorkerNode (which has its own kubelet)
2) start the worker node -> it registerWithMaster and start loop pollForTasks
3) postman POST /submit-task -> apiServer -> ClusterState -> Scheduler (allocateMemory) -> ClusterState (saves the task in the queue of the assigned worker)
4) the worker node polls the new task -> asks its own kubelet to startPod

scheduler -> decides the strategy used to pick a pod for a task
