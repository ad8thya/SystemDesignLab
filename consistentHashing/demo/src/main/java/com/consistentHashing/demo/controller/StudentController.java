package com.consistentHashing.demo.controller;

import com.consistentHashing.demo.model.Student;
import com.consistentHashing.demo.service.MongoRoutingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class StudentController {

    private final MongoRoutingService routingService;

    @Autowired
    public StudentController(MongoRoutingService routingService) {
        this.routingService = routingService;
    }

    /**
     * Add a new student. The record will be routed to the appropriate MongoDB shard node.
     */
    @PostMapping("/students")
    public ResponseEntity<?> createStudent(@RequestBody Student student) {
        try {
            Student saved = routingService.saveStudent(student);
            return new ResponseEntity<>(saved, HttpStatus.CREATED);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Search for a student by roll number. The query will be routed directly to the target shard.
     */
    @GetMapping("/students/{rollNo}")
    public ResponseEntity<Student> getStudentByRollNo(@PathVariable String rollNo) {
        Student student = routingService.getStudentByRollNo(rollNo);
        if (student == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(student, HttpStatus.OK);
    }

    /**
     * Retrieve all students from all active MongoDB shards (Scatter-Gather).
     */
    @GetMapping("/students")
    public ResponseEntity<List<Student>> getAllStudents() {
        return new ResponseEntity<>(routingService.getAllStudents(), HttpStatus.OK);
    }

    /**
     * Get the status and virtual node distribution of the hash ring.
     */
    @GetMapping("/ring/status")
    public ResponseEntity<Map<String, Object>> getRingStatus() {
        return new ResponseEntity<>(routingService.getRingStatus(), HttpStatus.OK);
    }

    /**
     * See the current distribution of students across all nodes
     */
    @GetMapping("/storage-nodes/distribution")
    public ResponseEntity<Map<String, Long>> getDistribution() {
        return new ResponseEntity<>(routingService.getDistribution(), HttpStatus.OK);
    }

    /**
     * Add a new MongoDB storage node and migrate affected records
     */
    @PostMapping("/storage-nodes/add")
    public ResponseEntity<String> addStorageNode() {
        try {
            routingService.autoProvisionNode();
            return new ResponseEntity<>("New MongoDB storage node dynamically provisioned and added successfully", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Remove a MongoDB storage node and rescue its records
     */
    @DeleteMapping("/storage-nodes/{nodeName}")
    public ResponseEntity<String> removeStorageNode(@PathVariable String nodeName) {
        try {
            routingService.removeAndDestroyNode(nodeName);
            return new ResponseEntity<>("Node " + nodeName + " successfully drained and destroyed.", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
