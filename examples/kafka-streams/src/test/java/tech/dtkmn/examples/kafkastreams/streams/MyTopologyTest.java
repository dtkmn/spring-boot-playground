package tech.dtkmn.examples.kafkastreams.streams;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MyTopologyTest {

    @Test
    void shouldExtractKnownCurrenciesFromTweet() {
        MyTopology topology = new MyTopology();

        List<String> currencies = topology.getCurrencies("Bitcoin and Ethereum are moving together");

        assertThat(currencies).containsExactlyInAnyOrder("bitcoin", "ethereum");
    }
}
