from google.cloud import pubsub_v1
import json

project_id = "testde-foomlg"
subscription_id = "tutorial.inventory.customers"

# Data oplog yang telah diambil sebelumnya
oplog_data = {
    1717650545: 1717650545000,  # Contoh timestamp dan ts_ms
    1717650565: 1717650565000,
    # Tambahkan data oplog lainnya sesuai kebutuhan
}

def callback(message):
    data = json.loads(message.data.decode("utf-8"))
    if data["op"] == "r" and data["source"]["ts_ms"] == 0:
        oplog_ts = data["source"]["ts"]  # Mengambil timestamp dari source
        if oplog_ts in oplog_data:
            data["source"]["ts_ms"] = oplog_data[oplog_ts]
            print(f"Updated ts_ms: {data['source']['ts_ms']} for oplog_ts: {oplog_ts}")
    # Process modified message
    print(data)
    message.ack()

subscriber = pubsub_v1.SubscriberClient()
subscription_path = subscriber.subscription_path(project_id, subscription_id)
streaming_pull_future = subscriber.subscribe(subscription_path, callback=callback)
print(f"Listening for messages on {subscription_path}..\n")

with subscriber:
    try:
        streaming_pull_future.result()
    except KeyboardInterrupt:
        streaming_pull_future.cancel()
        streaming_pull_future.result()
