## Google Cloud Pubsub & Dataflow

### TL;DR

Pipelines
- Pubsub data producer, 1 message per second
- Pubsub data consumer, 10s sliding window, for every 1s log a sample message per window + count number of messages per window

For load tests, use this [data producer](https://github.com/cloudymoma/pubsub-producer) to dump messages to Pubsub

### Quickstart

Simply change parameters defined in this [`makefile`](https://github.com/cloudymoma/pubsub-dataflow/blob/main/makefile#L3-L10), for example, `project id`, `pubsub topic name`, `pubsub subscription name` etc.

then `make df` will launch the dataflow job, or `make up` will update the current running job

### DAG

![](https://raw.githubusercontent.com/cloudymoma/pubsub-dataflow/main/miscs/dag.png)
