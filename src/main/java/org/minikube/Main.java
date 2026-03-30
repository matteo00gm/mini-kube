package org.minikube;

import org.minikube.master.MasterNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException {
    
        if (args.length == 0) {
            int number = 0;
            try (Scanner scanner = new Scanner(System.in)) {
                log.info("Enter the number of Masters to boot: ");
                String input = scanner.nextLine();
                number = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                log.error("Error: Invalid integer.");
                return;
            }

            List<String> allPeers = new ArrayList<>();
            for (int i = 0; i < number; i++) {
                allPeers.add("http://localhost:" + (7070 + i));
            }

            // Get the current classpath so the new windows know where to find the compiled code
            String classpath = System.getProperty("java.class.path");

            for (int i = 0; i < number; i++) {
                int port = 7070 + i;
                String endpoint = "http://localhost:" + port;
                
                String peers = String.join(",", allPeers.stream().filter(s -> !s.equals(endpoint)).toList());

                String[] command = {
                    "cmd.exe", 
                    "/c", 
                    "start", 
                    "Master " + port, // window title
                    "java", 
                    "-cp", classpath,          // classpath
                    "org.minikube.Main",       // main class
                    String.valueOf(port),      // args(0) -> port
                    peers                 // args(1) -> peers
                };

                // Pass the array to the ProcessBuilder
                ProcessBuilder pb = new ProcessBuilder(command);
                
                pb.start();
            }
            
            return;
        }

        // code running inside the windows
        int port = Integer.parseInt(args[0]);
        String endpoint = "http://localhost:" + port;
        List<String> peers = Arrays.asList(args[1].split(","));

        log.info("Booting Master Node on Port: {}", port);
        log.info("Peers: {}", peers);

        MasterNode master = new MasterNode(port, endpoint, peers);
        master.start();
    }
}