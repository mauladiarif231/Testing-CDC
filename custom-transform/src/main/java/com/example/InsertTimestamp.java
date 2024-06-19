package com.example;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.transforms.Transformation;
import org.bson.BsonTimestamp;
import org.bson.Document;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCursor;
import org.bson.types.ObjectId;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public class InsertTimestamp implements Transformation<SourceRecord> {

    private MongoClient mongoClient;
    private MongoCollection<Document> oplogCollection;
    private ObjectMapper objectMapper;

    @Override
    public void configure(Map<String, ?> configs) {
        String mongoUri = (String) configs.get("mongo.uri");
        System.out.println("Configuring InsertTimestamp with mongo.uri: " + mongoUri); // log buat mongori
        if (mongoUri == null) {
            throw new IllegalArgumentException("Property mongo.uri must be provided");
        }
        mongoClient = MongoClients.create(mongoUri);
        MongoDatabase database = mongoClient.getDatabase("local");
        oplogCollection = database.getCollection("oplog.rs");
        objectMapper = new ObjectMapper();
    }


    // @Override
    // public SourceRecord apply(SourceRecord record) {
    //     Object value = record.value();

    //     if (value instanceof Struct) {
    //         Struct structValue = (Struct) value;

    //         try {
    //             // Memeriksa field snapshot dari source
    //             Struct sourceStruct = structValue.getStruct("source");
    //             if (sourceStruct != null) {
    //                 Object snapshotObj = sourceStruct.get("snapshot");
    //                 boolean isSnapshot = snapshotObj != null && Boolean.parseBoolean(snapshotObj.toString());

    //                 if (isSnapshot) {
    //                     // Memeriksa field after dan memastikan itu adalah string JSON
    //                     Object afterObj = structValue.get("after");
    //                     if (afterObj instanceof String) {
    //                         String afterJson = (String) afterObj;

    //                         // Mengurai string JSON menjadi JsonNode
    //                         JsonNode afterNode = objectMapper.readTree(afterJson);
    //                         System.out.println("After JsonNode: " + afterNode);

    //                         // Mendapatkan _id dari after
    //                         JsonNode idNode = afterNode.get("_id").get("$oid");
    //                         if (idNode != null) {
    //                             String objectIdStr = idNode.asText();
    //                             ObjectId objectId = new ObjectId(objectIdStr);
                                
    //                             // Membuat query untuk oplog berdasarkan namespace dan _id
    //                             String namespace = sourceStruct.getString("db") + "." + sourceStruct.getString("collection");
    //                             Document query = new Document("op", "i")
    //                                     .append("ns", namespace)
    //                                     .append("o._id", objectId);

    //                             MongoCursor<Document> cursor = oplogCollection.find(query)
    //                                     .sort(new Document("$natural", -1))
    //                                     .limit(1)
    //                                     .iterator();

    //                             if (cursor.hasNext()) {
    //                                 Document oplogEntry = cursor.next();
    //                                 BsonTimestamp oplogTimestamp = oplogEntry.get("ts", BsonTimestamp.class);
    //                                 if (oplogTimestamp != null) {
    //                                     long timestamp = oplogTimestamp.getTime();
    //                                     structValue.put("source.ts_ms", timestamp);
    //                                 } else {
    //                                     System.err.println("No oplog timestamp found for this read operation.");
    //                                 }
    //                             } else {
    //                                 System.err.println("No matching oplog entry found.");
    //                             }
    //                         } else {
    //                             System.err.println("No _id field found in after struct.");
    //                         }
    //                     } else {
    //                         System.err.println("After field is not a String or is missing.");
    //                     }
    //                 }
    //             } else {
    //                 System.err.println("Source struct is missing or null.");
    //             }
    //         } catch (Exception e) {
    //             System.err.println("Error processing document: " + e.getMessage());
    //         }
    //     } else {
    //         System.err.println("Expected record value to be of type STRUCT but found: " + (value != null ? value.getClass().getName() : "null"));
    //     }
    //     return record;
    // }
    @Override
    public SourceRecord apply(SourceRecord record) {
        Object value = record.value();

        if (value instanceof Struct) {
            Struct structValue = (Struct) value;
            try {
                // Memeriksa field source dari structValue
                Struct sourceStruct = structValue.getStruct("source");
                if (sourceStruct != null) {
                    System.out.println("Source struct: " + sourceStruct.schema().fields()); // Test Debug
                    Object snapshotObj = sourceStruct.get("snapshot");
                    boolean isSnapshot = snapshotObj != null;

                    if (isSnapshot) {
                        Object afterObj = structValue.get("after");
                        if (afterObj instanceof String) {
                            String afterJson = (String) afterObj;
                            JsonNode afterNode = objectMapper.readTree(afterJson);

                            JsonNode idNode = afterNode.get("_id").get("$oid");
                            if (idNode != null) {
                                String objectIdStr = idNode.asText();
                                ObjectId objectId = new ObjectId(objectIdStr);
                                String namespace = sourceStruct.getString("db") + "." + sourceStruct.getString("collection");
                                Document query = new Document("op", "i")
                                        .append("ns", namespace)
                                        .append("o._id", objectId);

                                MongoCursor<Document> cursor = oplogCollection.find(query)
                                        .sort(new Document("$natural", -1))
                                        .limit(1)
                                        .iterator();

                                if (cursor.hasNext()) {
                                    Document oplogEntry = cursor.next();
                                    BsonTimestamp oplogTimestamp = oplogEntry.get("ts", BsonTimestamp.class);
                                    if (oplogTimestamp != null) {
                                        long timestamp = oplogTimestamp.getTime();
                                        sourceStruct.put("ts_ms", timestamp); // Menyimpan ke source.ts_ms
                                        System.out.println("Added timestamp to source.ts_ms: " + timestamp); // tes debug
                                    } else {
                                        System.err.println("No oplog timestamp found for this read operation.");
                                    }
                                } else {
                                    System.err.println("No matching oplog entry found.");
                                }
                            } else {
                                System.err.println("No _id field found in after struct.");
                            }
                        } else {
                            System.err.println("After field is not a String or is missing.");
                        }
                    } else {
                        System.err.println("Snapshot is false, skipping timestamp update.");
                    }
                } else {
                    System.err.println("Source struct is missing or null.");
                }
            } catch (Exception e) {
                System.err.println("Error processing document: " + e.getMessage());
                e.printStackTrace(); // Print stack trace for more detailed debugging
            }
        } else {
            System.err.println("Expected record value to be of type STRUCT but found: " + (value != null ? value.getClass().getName() : "null"));
        }
        return record;
    }

    @Override
    public ConfigDef config() {
        return new ConfigDef()
            .define("mongo.uri", ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, "MongoDB URI");
    }

    @Override
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}
