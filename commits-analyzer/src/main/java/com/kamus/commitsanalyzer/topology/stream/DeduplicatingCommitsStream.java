package com.kamus.commitsanalyzer.topology.stream;

import com.kamus.commitsanalyzer.config.KafkaConfig;
import com.kamus.commitsanalyzer.topology.transformer.DeduplicationTransformer;
import com.kamus.dataloader.grpcjava.RepositoryCommitMessage;
import io.confluent.kafka.streams.serdes.protobuf.KafkaProtobufSerde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.apache.kafka.streams.state.WindowStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Configuration
public class DeduplicatingCommitsStream {

    @Value("${kafka.commits.dedup.window.min:60m}")
    @DurationUnit(ChronoUnit.MINUTES)
    private Duration dedupWindow;

    @Value("${kafka.commits.dedup.retention.min:60m}")
    @DurationUnit(ChronoUnit.MINUTES)
    private Duration dedupRetention;

    @Value("${kafka.store.commits.dedup:dedupCommits-store")
    private String dedupCommitsStoreName;

    @Bean
    public KStream<String, RepositoryCommitMessage> kStream(StreamsBuilder streamsBuilder,
                                                            KafkaProtobufSerde<RepositoryCommitMessage> commitSerde) {
        streamsBuilder.addStateStore(storeBuilder(commitSerde));

        KStream<String, RepositoryCommitMessage> deduplicatedCommitsStream =
                streamsBuilder
                        .stream(KafkaConfig.COMMITS_TOPIC, Consumed.with(Serdes.String(), commitSerde))
                        .transformValues(() -> new DeduplicationTransformer<>(
                                dedupWindow.toMillis(),
                                (key, value) -> value.getCommit().getSha(),
                                dedupCommitsStoreName))
                        .filter((k, v) -> Objects.nonNull(v));

        deduplicatedCommitsStream
                .to(KafkaConfig.DEDUPLICATED_COMMITS_TOPIC, Produced.with(Serdes.String(), commitSerde));

        return deduplicatedCommitsStream;
    }

    private StoreBuilder<WindowStore<String, RepositoryCommitMessage>> storeBuilder(KafkaProtobufSerde<RepositoryCommitMessage> commitSerde) {
        return Stores.windowStoreBuilder(Stores.persistentWindowStore(
                dedupCommitsStoreName,
                dedupWindow,
                dedupRetention,
                false
        ), Serdes.String(), commitSerde);
    }

}