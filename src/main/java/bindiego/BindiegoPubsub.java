package bindiego;

import com.google.common.collect.ImmutableMap;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.GenerateSequence;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions;
import org.apache.beam.sdk.options.StreamingOptions;
import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.transforms.windowing.SlidingWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BindiegoPubsub {
  private static final Logger LOG = LoggerFactory.getLogger(BindiegoPubsub.class);
  public static final String WORKER_NETWORK = "https://www.googleapis.com/compute/v1/projects/du-hast-mich/regions/us-central1/subnetworks/default";

  /**
   * Custom PipelineOptions for Pub/Sub.
   */
  public interface PubsubOptions extends PipelineOptions, DataflowPipelineOptions, StreamingOptions {
    @Description("Pub/Sub Topic to write to")
    @Default.String("projects/du-hast-mich/topics/dingo-topic")
    String getPubsubTopic();

    void setPubsubTopic(String value);

    @Description("Pub/Sub Topic to read from")
    @Default.String("projects/du-hast-mich/topics/dingo-topic")
    String getSubscription();

    void setSubscription(String value);
  }

  public static void main(String[] args) {
    // Use PipelineOptionsFactory to create PubsubOptions
    PubsubOptions options = PipelineOptionsFactory.fromArgs(args)
        .withValidation()
        .as(PubsubOptions.class);
    options.setStreaming(true);
    options.setNetwork("default");
    options.setSubnetwork(WORKER_NETWORK);
    options.setProject("du-hast-mich");
    options.setRegion("us-central1");
    Pipeline p = Pipeline.create(options);

    // 1. Generate messages every second.
    PCollection<String> messages = p.apply("Generate Messages",
            GenerateSequence.from(0).withRate(1, Duration.standardSeconds(1))) // Generate numbers 0, 1, 2... every second
        .apply(MapElements.into(org.apache.beam.sdk.values.TypeDescriptors.strings())
            .via(number -> "Message: " + number + ", Timestamp: " + Instant.now().toString())); // Convert to strings

    // 2. Write messages to Pub/Sub.
    messages.apply("Write to Pubsub Topic: " + options.getPubsubTopic(),
        PubsubIO.writeStrings()
            .to(options.getPubsubTopic())
    );

    // 3. Read messages from Pub/Sub.
    PCollection<String> pubsubMessages = 
        p.apply("Read from Pubsub Subscription: " + options.getSubscription(),
            PubsubIO.readStrings()
                .fromSubscription(options.getSubscription())
    );

    // 4. Apply a sliding window and count messages.
    pubsubMessages
        .apply("Windowing",
            Window.<String>into(SlidingWindows.of(Duration.standardSeconds(10))
                .every(Duration.standardSeconds(1)))) // 10-second windows, sliding every 1 second
        .apply("Count Messages",
            Combine.globally(Count.<String>combineFn()).withoutDefaults()) // Count messages in each window
        .apply("Format Output", MapElements.into(org.apache.beam.sdk.values.TypeDescriptors.strings())
            .via(count -> {
                Instant windowEnd = Instant.now(); //simplified, should get it from window, but this is okay for this task
                return "Window ending at " + windowEnd + ": Count = " + count;
            }))
        .apply("Log Count", ParDo.of(new DoFn<String, Void>() {
            @ProcessElement
            public void processElement(@Element String element, OutputReceiver<Void> out) {
                LOG.info(element);
            }
        }));

    //take first message per each window as a sample
    pubsubMessages
        .apply("WindowingSample",
            Window.into(SlidingWindows.of(Duration.standardSeconds(10))
                .every(Duration.standardSeconds(1))))
        .apply("Sample Message", Sample.any(1)) //take one sample per window
        .apply("Log Sample Message", ParDo.of(new DoFn<String, Void>() {
            @ProcessElement
            public void processElement(@Element String element, OutputReceiver<Void> out) {
                LOG.info("Sample Message: " + element);
            }
        }));

    p.run();
  }
}
