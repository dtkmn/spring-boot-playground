package playground.tech.springbootplayground.streams;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class MyTopology {

    private static final List<String> SUPPORTED_CURRENCIES = Arrays.asList("dogecoin", "bitcoin", "ethereum");

    @Autowired
    public void buildTopology(StreamsBuilder builder) {
        // Read the topic as stream
        KStream<byte[], String> tweetStream = builder.stream("tweets", Consumed.with(Serdes.ByteArray(), Serdes.String()));
        tweetStream.print(Printed.<byte[], String>toSysOut().withLabel("tweets"));

        // read the crypto-symbols topic as a table
        KTable<String, String> symbolsTable =
                builder.table("crypto-symbols", Consumed.with(Serdes.String(), Serdes.String()));
        symbolsTable.toStream().print(Printed.<String, String>toSysOut().withLabel("crypto-symbols"));

        // Change the key format of tweet stream from byte[]/numeric to String before doing any join
        KStream<String, String> tweetsRekeyed = tweetStream.selectKey(this::getCurrency);
        tweetsRekeyed.print(Printed.<String, String>toSysOut().withLabel("tweets-rekeyed"));

//        // join
        KStream<String, String> joined = tweetsRekeyed.join(symbolsTable,
                (tweet, symbol) -> String.format("%s - (%s)", tweet, symbol),
                Joined.with(Serdes.String(), Serdes.String(), Serdes.String()));
        joined.print(Printed.<String, String>toSysOut().withLabel("joined"));

        // count
        KTable<String, Long> counts = tweetsRekeyed
                .groupByKey(Grouped.with(Serdes.String(), Serdes.String()))
                .count();
        counts.toStream().to("counts-tweets", Produced.with(Serdes.String(), Serdes.Long()));


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

        // Tumbling Windows
        TimeWindows window = TimeWindows.of(Duration.ofMinutes(5));
        // Example of Hopping Windows:
//        TimeWindows window = TimeWindows.of(Duration.ofMinutes(5)).advanceBy(Duration.ofMinutes(4));
        // Example of Session Windows:
//        SessionWindows window = SessionWindows.with(Duration.ofMinutes(5));
        // Example of Sliding Aggregation Windows:
//        SlidingWindows window =
//                SlidingWindows.withTimeDifferenceAndGrace(
//                        Duration.ofMinutes(5), // max time diff between 2 records
//                        Duration.ofMinutes(5)); // grace period
        KStream<String, String> redditPosts = builder.stream("reddit-posts", Consumed.with(Serdes.String(), Serdes.String()));
        KTable<Windowed<String>, Long> count = redditPosts.groupByKey().windowedBy(window).count();

        // write the alerts to a topic
        count
                .filter((key, value) -> value == 3)
                .toStream()
                .map((windowKey, value) -> createAlert(windowKey, value))
                .to("alerts", Produced.with(Serdes.String(), Serdes.String()));

    }

    private static KeyValue<String, String> createAlert(Windowed<String> windowKey, Long value) {
        String userId = windowKey.key();
        String alert = String.format("%s has exceeded the 5 minute post limit", userId);
        return KeyValue.pair(userId, alert);
    }

    public List<String> getCurrencies(String tweetText) {
        List<String> words =
                Arrays.asList(tweetText.replaceAll("[^a-zA-Z ]", "").toLowerCase().trim().split(" "));
        return words.stream().distinct().filter(SUPPORTED_CURRENCIES::contains).collect(Collectors.toList());
    }

    /**
     * Extracts the first supported currency found in the tweet text.
     * The order of currencies in SUPPORTED_CURRENCIES does not determine priority here;
     * instead, the first currency mentioned in the tweet text that is also in SUPPORTED_CURRENCIES is returned.
     * If multiple supported currencies are present, the one appearing earliest in the tweet (after splitting and filtering) is chosen.
     * @param key the key of the Kafka message
     * @param tweetText the text of the tweet
     * @return the first supported currency found, or an empty string if no supported currency is found.
     */
    public String getCurrency(byte[] key, String tweetText) {
        List<String> currencies = getCurrencies(tweetText);
        if (!currencies.isEmpty()) {
            // "First currency wins" based on the order of appearance in the tweet text
            return currencies.get(0);
        }
        return "";
    }

    // TODO: Implement actual enrichment logic for Ethereum-related tweets
    private String enrichEthTweet(String tweet) {
        return tweet;
    }

}
