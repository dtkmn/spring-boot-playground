echo "Waiting for Kafka to come online..."

cub kafka-ready -b localhost:9092 1 20

echo "Creating Kafka topics"
# create the tweets topic
kafka-topics \
  --bootstrap-server localhost:9092 \
  --topic tweets \
  --replication-factor 1 \
  --partitions 4 \
  --create

# create output topic
kafka-topics \
  --bootstrap-server localhost:9092 \
  --topic formatted-tweets \
  --replication-factor 1 \
  --partitions 4 \
  --create

# create the users topic
kafka-topics \
  --bootstrap-server localhost:9092 \
  --topic users \
  --replication-factor 1 \
  --partitions 4 \
  --create

# create the crypto-symbols topic
kafka-topics \
  --bootstrap-server kafka:9092 \
  --topic crypto-symbols \
  --replication-factor 1 \
  --partitions 4 \
  --create

echo "Pre-populating Kafka topics"
# pre-populate the tweets topic
kafka-console-producer \
  --bootstrap-server localhost:9092 \
  --topic tweets \
  --property 'parse.key=true' \
  --property 'key.separator=|' < /data/inputs.txt

# pre-populate the users topic
kafka-console-producer \
  --bootstrap-server localhost:9092 \
  --topic users \
  --property 'parse.key=true' \
  --property 'key.separator=|' < /data/users.txt


kafka-console-producer \
  --bootstrap-server localhost:9092 \
  --topic crypto-symbols \
  --property 'parse.key=true' \
  --property 'key.separator=|' < /data/crypto-symbols.txt

sleep infinity
