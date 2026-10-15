package com.consistentHashing.demo.service;

import com.consistentHashing.demo.model.Student;
import com.consistentHashing.demo.sharding.ConsistentHashRing;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class MongoRoutingService {

    private final Map<String, MongoTemplate> mongoTemplates;
    private ConsistentHashRing ring;
    private static final int VIRTUAL_NODE_COUNT = 100;
    private int nextNodeId = 4;
    private int nextPort = 27020;

    @Autowired
    public MongoRoutingService(Map<String, MongoTemplate> mongoTemplates) {
        // Use a thread-safe map so we can support dynamic additions/removals
        this.mongoTemplates = new ConcurrentHashMap<>(mongoTemplates);
    }

    @PostConstruct
    public void init() {
        // Register the configured physical nodes on the ring
        this.ring = new ConsistentHashRing(VIRTUAL_NODE_COUNT, mongoTemplates.keySet());
    }

    /**
     * Saves a student record to the correct database instance using Consistent Hashing.
     */
    public Student saveStudent(Student student) {
        if (student.getRollNo() == null || student.getRollNo().isBlank()) {
            throw new IllegalArgumentException("Roll number is required as the shard key.");
        }

        // 1. Identify which shard (node) this roll number hashes to
        String targetNode = ring.getNode(student.getRollNo());
        if (targetNode == null) {
            throw new IllegalStateException("No shard available on the hash ring.");
        }

        // 2. Add metadata indicating the chosen node
        student.setStoredInNode(targetNode);

        // 3. Save the student to the corresponding MongoTemplate
        MongoTemplate template = mongoTemplates.get(targetNode);
        return template.save(student);
    }

    /**
     * Finds a student by roll number by routing the query to the correct database instance.
     */
    public Student getStudentByRollNo(String rollNo) {
        if (rollNo == null || rollNo.isBlank()) {
            throw new IllegalArgumentException("Roll number is required.");
        }

        // 1. Query the ring to see which node has this student
        String targetNode = ring.getNode(rollNo);
        if (targetNode == null) {
            return null;
        }

        // 2. Fetch directly from that node
        MongoTemplate template = mongoTemplates.get(targetNode);
        Query query = new Query(Criteria.where("rollNo").is(rollNo));
        return template.findOne(query, Student.class);
    }

    /**
     * Queries all active nodes to retrieve all student records (Scatter-Gather).
     */
    public List<Student> getAllStudents() {
        List<Student> allStudents = new ArrayList<>();
        for (Map.Entry<String, MongoTemplate> entry : mongoTemplates.entrySet()) {
            try {
                List<Student> studentsFromNode = entry.getValue().findAll(Student.class);
                // Ensure the metadata reflects where they were loaded from
                studentsFromNode.forEach(s -> s.setStoredInNode(entry.getKey()));
                allStudents.addAll(studentsFromNode);
            } catch (Exception e) {
                // If a node is down, log the error and proceed with remaining nodes (fault tolerance)
                System.err.printf("Error querying node %s: %s%n", entry.getKey(), e.getMessage());
            }
        }
        return allStudents;
    }

    /**
     * Returns the list of all currently active physical nodes on the ring.
     */
    public Set<String> getActiveNodes() {
        return new HashSet<>(ring.getCircleCopy().values());
    }

    /**
     * Provides metadata about the hash ring and virtual node distribution.
     */
    public Map<String, Object> getRingStatus() {
        TreeMap<Long, String> circle = ring.getCircleCopy();
        
        Map<String, Long> nodesDistribution = circle.values().stream()
                .collect(Collectors.groupingBy(node -> node, Collectors.counting()));

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("activeNodes", getActiveNodes());
        status.put("totalVirtualNodes", circle.size());
        status.put("virtualNodesPerPhysicalNode", nodesDistribution);
        
        // Sample some ring hashes for visualization (limit to first 10 for readability)
        List<Map<String, Object>> ringSample = circle.entrySet().stream()
                .limit(10)
                .map(e -> {
                    Map<String, Object> point = new HashMap<>();
                    point.put("hash", e.getKey());
                    point.put("node", e.getValue());
                    return point;
                })
                .collect(Collectors.toList());
        status.put("ringSample", ringSample);
        
        return status;
    }

    /**
     * Get data distribution before or after migration
     */
    public Map<String, Long> getDistribution() {
        Map<String, Long> distribution = new HashMap<>();
        for (String node : getActiveNodes()) {
            distribution.put(node, mongoTemplates.get(node).count(new Query(), Student.class));
        }
        return distribution;
    }

    /**
     * Automatically provisions a new MongoDB container using Docker, waits for it to be ready,
     * adds it to the hash ring, and migrates affected records.
     */
    public void autoProvisionNode() throws Exception {
        String newNodeName = "mongo-node-" + nextNodeId;
        int port = nextPort;
        
        // Advance counters for next time
        nextNodeId++;
        nextPort++;

        // 1. Run Docker command
        System.out.println("Starting Docker container: " + newNodeName + " on port " + port);
        ProcessBuilder pb = new ProcessBuilder(
            "docker", "run", "-d", "--name", newNodeName, "-p", port + ":27017", "mongo:7.0"
        );
        pb.inheritIO();
        Process p = pb.start();
        p.waitFor();

        // 2. Retry connection mechanism
        String uri = "mongodb://localhost:" + port + "/college";
        MongoClient client = MongoClients.create(uri);
        MongoTemplate newTemplate = new MongoTemplate(client, "college");

        boolean isReady = false;
        int retries = 0;
        while (!isReady && retries < 15) {
            try {
                System.out.println("Checking if " + newNodeName + " is ready (Attempt " + (retries + 1) + ")...");
                newTemplate.executeCommand("{ ping: 1 }");
                isReady = true;
                System.out.println(newNodeName + " is ready!");
            } catch (Exception e) {
                retries++;
                Thread.sleep(2000); // Wait 2s before retry
            }
        }

        if (!isReady) {
            throw new RuntimeException("Failed to connect to newly provisioned node: " + newNodeName);
        }

        // 3. Add to our local map and ring
        mongoTemplates.put(newNodeName, newTemplate);
        ring.addNode(newNodeName);
        
        Set<String> currentNodes = getActiveNodes();

        // 4. Migrate data
        System.out.println("Migrating data to " + newNodeName + "...");
        int migrated = 0;
        for (String oldNode : currentNodes) {
            if (oldNode.equals(newNodeName)) continue; // Don't migrate from the new node

            MongoTemplate oldTemplate = mongoTemplates.get(oldNode);
            List<Student> students = oldTemplate.findAll(Student.class);
            for (Student s : students) {
                String targetNode = ring.getNode(s.getRollNo());
                if (targetNode.equals(newNodeName)) {
                    // Move it
                    s.setStoredInNode(newNodeName);
                    newTemplate.save(s);
                    oldTemplate.remove(s);
                    migrated++;
                }
            }
        }
        System.out.println("Migration complete. Moved " + migrated + " records to " + newNodeName);
    }

    /**
     * Safely drains data from a node, removes it from the ring, and destroys the container.
     */
    public void removeAndDestroyNode(String nodeName) throws Exception {
        Set<String> currentNodes = getActiveNodes();
        if (!currentNodes.contains(nodeName)) {
            throw new IllegalArgumentException("Node not found in ring: " + nodeName);
        }
        if (currentNodes.size() <= 1) {
            throw new IllegalStateException("Cannot remove the last node in the cluster!");
        }
        MongoTemplate dyingTemplate = mongoTemplates.get(nodeName);
        
        // 1. Remove from the hash ring first so new routing ignores it
        ring.removeNode(nodeName);
        // 2. Migrate (Drain) all data out of the dying node
        System.out.println("Draining data from " + nodeName + "...");
        List<Student> strandedStudents = dyingTemplate.findAll(Student.class);
        int migrated = 0;
        for (Student s : strandedStudents) {
            // Find its new home on the updated ring
            String newHomeNode = ring.getNode(s.getRollNo());
            
            // Move it to the new home
            s.setStoredInNode(newHomeNode);
            mongoTemplates.get(newHomeNode).save(s);
            migrated++;
        }
        
        System.out.println("Successfully rescued and moved " + migrated + " records from " + nodeName);
        // 3. Remove connection from our application
        mongoTemplates.remove(nodeName);
        // 4. Send command to Docker to physically destroy the container
        System.out.println("Destroying Docker container: " + nodeName);
        ProcessBuilder pb = new ProcessBuilder("docker", "rm", "-f", nodeName);
        pb.inheritIO();
        Process p = pb.start();
        p.waitFor();
        
        System.out.println(nodeName + " has been successfully decommissioned!");
    }
}
