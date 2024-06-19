package io.debezium.server.pubsub;

import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.transforms.Transformation;
import org.apache.kafka.connect.transforms.util.Requirements;

import java.util.Map;

public class PubSub implements Transformation<SourceRecord> {

    @Override
    public SourceRecord apply(SourceRecord record) {
        Struct value = (Struct) record.value();
        if (value == null) {
            return record;
        }

        String op = value.getString("op");

        if ("r".equals(op)) {
            Struct source = value.getStruct("source");
            long currentTimestamp = System.currentTimeMillis();
            source.put("ts_ms", currentTimestamp);
        }

        return record;
    }

    @Override
    public ConfigDef config() {
        return new ConfigDef();
    }

    @Override
    public void configure(Map<String, ?> configs) {
    }

    @Override
    public void close() {
    }

    @Override
    public SchemaAndValue apply(SchemaAndValue schemaAndValue) {
        return null;
    }
}