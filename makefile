pwd := $(shell pwd)
ipaddr := $(shell hostname -I | cut -d ' ' -f 1)
gcp_project := du-hast-mich
region := us-central1
workerType := e2-standard-2
workerZone := b
gcs_bucket = bindiego
job := bindiego-pubsub
pubsub_topic := dingo-topic
pubsub_subscription := dingo-topic-sub
worker_utilization_hint := 0.9

lo:
	@mvn compile exec:java -Dexec.mainClass=bindiego.BindiegoPubsub

df:
	@mvn -Pdataflow-runner compile exec:java \
		-Dexec.mainClass=bindiego.BindiegoPubsub \
		-Dexec.cleanupDaemonThreads=false \
		-Dexec.args="--project=$(gcp_project) \
--streaming=true \
--enableStreamingEngine \
--autoscalingAlgorithm=THROUGHPUT_BASED \
--region=$(region) \
--maxNumWorkers=20 \
--workerMachineType=$(workerType) \
--diskSizeGb=64 \
--numWorkers=3 \
--tempLocation=gs://$(gcs_bucket)/tmp/ \
--gcpTempLocation=gs://$(gcs_bucket)/tmp/gcp/ \
--stagingLocation=gs://$(gcs_bucket)/staging/ \
--runner=DataflowRunner \
--experiments=use_runner_v2 \
--defaultWorkerLogLevel=DEBUG \
--jobName=$(job) \
--dataflowServiceOptions=$(worker_utilization_hint) \
--pubsubTopic=projects/$(gcp_project)/topics/$(pubsub_topic) \
--subscription=projects/$(gcp_project)/subscriptions/$(pubsub_subscription)"

up:
	@mvn -Pdataflow-runner compile exec:java \
		-Dexec.mainClass=bindiego.BindiegoPubsub \
		-Dexec.cleanupDaemonThreads=false \
		-Dexec.args="--project=$(gcp_project) \
--streaming=true \
--enableStreamingEngine \
--autoscalingAlgorithm=THROUGHPUT_BASED \
--region=$(region) \
--maxNumWorkers=20 \
--workerMachineType=$(workerType) \
--diskSizeGb=64 \
--numWorkers=3 \
--tempLocation=gs://$(gcs_bucket)/tmp/ \
--gcpTempLocation=gs://$(gcs_bucket)/tmp/gcp/ \
--stagingLocation=gs://$(gcs_bucket)/staging/ \
--runner=DataflowRunner \
--experiments=use_runner_v2 \
--defaultWorkerLogLevel=DEBUG \
--jobName=$(job) \
--dataflowServiceOptions=$(worker_utilization_hint) \
--update \
--pubsubTopic=projects/$(gcp_project)/topics/$(pubsub_topic) \
--subscription=projects/$(gcp_project)/subscriptions/$(pubsub_subscription)"
