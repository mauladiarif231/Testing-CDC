FROM quay.io/debezium/server:2.3

# Salin file JAR kustom ke dalam direktori plugins Kafka Connect
COPY custom-transform/target/InsertTimestamp-1.0-SNAPSHOT.jar /debezium/lib/
COPY custom-transform/target/InsertTimestamp-1.0-SNAPSHOT.jar /debezium/connect/libs/

# (Opsional) Jika Anda memiliki library tambahan, Anda bisa menambahkan di sini
# COPY lib /kafka/connect/libs/



