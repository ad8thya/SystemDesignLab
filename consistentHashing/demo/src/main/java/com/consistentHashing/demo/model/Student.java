package com.consistentHashing.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "students")
public class Student {
    @Id
    private String id;
    private String rollNo; // Shard key
    private String name;
    private String department;
    private String email;
    private Integer year;
    
    // Metadata field to help visualize which shard/node holds this document
    private String storedInNode;
}
