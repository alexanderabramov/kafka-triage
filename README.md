# kafka-triage
Triage records in Kafka dead letter queue (DLQ) topics

## Database setup on GCP Kubernetes

### Create a GCP Cloud SQL instance

https://cloud.google.com/sql/docs/postgres/

```
gcloud sql instances create dbk-prod-kafka-triage --database-version=POSTGRES_9_6 --region europe-west4  \
    --storage-size=10GB --tier db-f1-micro --backup
gcloud sql users set-password postgres no-host --instance kafka-triage --prompt-for-password
gcloud sql databases create kafka-triage --instance=kafka-triage
```

### Connect from GCP Kubernetes

https://cloud.google.com/sql/docs/postgres/connect-kubernetes-engine
