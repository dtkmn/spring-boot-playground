package playground.tech.springbootplayground.streams;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

@Configuration
public class MyTopology {

    @Bean
    public Topology buildTopology() {
        StreamsBuilder builder = new StreamsBuilder();
        // Read the topic as stream
        KStream<byte[], String> tweetStream = builder.stream("tweets", Consumed.with(Serdes.ByteArray(), Serdes.String()));
        // 1:N transform
        KStream<byte[], String> sentences = tweetStream.flatMapValues((k, v) -> Arrays.asList(v.split("\\.")));
        // 1:1 transform
        KStream<byte[], String> lowercaseTweets = sentences.mapValues((k, v) -> v.toLowerCase().trim());
        // filter
        KStream<byte[], String> filteredTweets = lowercaseTweets.filter((k, v) -> v.contains("bitcoin") || v.contains("ethereum"));
        // branch
        Map<String, KStream<byte[], String>> branches = filteredTweets
                .split(Named.as("branch-"))
                .branch(
                    (k, v) -> v.contains("ethereum"),
                    Branched.as("ethereum"))
                .defaultBranch(Branched.as("default"));
        // Get the branches
        KStream<byte[], String> ethBranch = branches.get("branch-ethereum");
        KStream<byte[], String> defaultBranch = branches.get("branch-default");
        // process on single branch
        KStream<byte[], String> processedEthTweets = ethBranch.mapValues((k, v) -> enrichEthTweet(v));
        // merge the streams
        KStream<byte[], String> merged = defaultBranch.merge(processedEthTweets);
        // output topic
        merged.to("formatted-tweets", Produced.with(Serdes.ByteArray(), Serdes.String()));

        return builder.build();
    }

    private String enrichEthTweet(String tweet) {
        return tweet;
    }

    @Bean
    public KafkaStreams kafkaStreams(Topology topology) {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "tweet-streams-app");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

        KafkaStreams streams = new KafkaStreams(topology, props);
        streams.start();
        return streams;
    }

}
