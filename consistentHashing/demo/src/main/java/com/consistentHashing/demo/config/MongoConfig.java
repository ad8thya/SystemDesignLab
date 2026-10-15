package com.consistentHashing.demo.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class MongoConfig {

    @Value("${mongodb.nodes.mongo-node-1.uri}")
    private String node1Uri;

    @Value("${mongodb.nodes.mongo-node-2.uri}")
    private String node2Uri;

    @Value("${mongodb.nodes.mongo-node-3.uri}")
    private String node3Uri;

    @Bean
    public MongoClient mongoClientNode1() {
        return MongoClients.create(node1Uri);
    }

    @Bean
    public MongoClient mongoClientNode2() {
        return MongoClients.create(node2Uri);
    }

    @Bean
    public MongoClient mongoClientNode3() {
        return MongoClients.create(node3Uri);
    }

    @Bean
    @Primary
    public MongoTemplate mongoTemplateNode1() {
        return new MongoTemplate(mongoClientNode1(), "college");
    }

    @Bean
    public MongoTemplate mongoTemplateNode2() {
        return new MongoTemplate(mongoClientNode2(), "college");
    }

    @Bean
    public MongoTemplate mongoTemplateNode3() {
        return new MongoTemplate(mongoClientNode3(), "college");
    }

    /**
     * Map holding all the templates indexed by the physical node name.
     * This will be used by our routing service to direct queries.
     */
    @Bean
    public Map<String, MongoTemplate> mongoTemplates() {
        Map<String, MongoTemplate> templates = new HashMap<>();
        templates.put("mongo-node-1", mongoTemplateNode1());
        templates.put("mongo-node-2", mongoTemplateNode2());
        templates.put("mongo-node-3", mongoTemplateNode3());
        return templates;
    }
}
