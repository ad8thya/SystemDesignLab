package com.consistentHashing.demo.sharding;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

public class ConsistentHashRing {

    private final int numberOfReplicas; // Number of virtual nodes per physical node
    private final TreeMap<Long, String> circle = new TreeMap<>();

    public ConsistentHashRing(int numberOfReplicas, Collection<String> nodes) {
        this.numberOfReplicas = numberOfReplicas;
        for (String node : nodes) {
            addNode(node);
        }
    }

    /**
     * Adds a physical node to the ring by generating its virtual nodes.
     */
    public synchronized void addNode(String node) {
        for (int i = 0; i < numberOfReplicas; i++) {
            String vNodeLabel = node + "#" + i;
            circle.put(hash(vNodeLabel), node);
        }
    }

    /**
     * Removes a physical node (and all its virtual nodes) from the ring.
     */
    public synchronized void removeNode(String node) {
        for (int i = 0; i < numberOfReplicas; i++) {
            String vNodeLabel = node + "#" + i;
            circle.remove(hash(vNodeLabel));
        }
    }

    /**
     * Finds the node responsible for a given key on the ring.
     */
    public synchronized String getNode(String key) {
        if (circle.isEmpty()) {
            return null;
        }
        long hash = hash(key);
        if (!circle.containsKey(hash)) {
            // Find the portion of the ring containing values greater than or equal to key hash
            SortedMap<Long, String> tailMap = circle.tailMap(hash);
            // If empty, wrap around to the beginning of the circle
            hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
        }
        return circle.get(hash);
    }

    /**
     * A helper method to hash string keys using MD5 and convert to 64-bit Long.
     */
    private long hash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(key.getBytes(StandardCharsets.UTF_8));
            
            // Map the first 8 bytes of MD5 hash to a 64-bit Long
            long hashValue = 0;
            for (int i = 0; i < 8; i++) {
                hashValue = (hashValue << 8) | (digest[i] & 0xFF);
            }
            return hashValue;
        } catch (NoSuchAlgorithmException e) {
            // Fallback to Java standard string hashCode if MD5 fails (should never happen)
            return key.hashCode();
        }
    }

    /**
     * Returns a copy of the ring configuration for client/visualization use.
     */
    public synchronized TreeMap<Long, String> getCircleCopy() {
        return new TreeMap<>(circle);
    }
}
