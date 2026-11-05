# Option B: Pub/Sub push instead of RabbitMQ (ai-service can scale to zero)

1. Topic + dead letter:
   gcloud pubsub topics create activity-events
   gcloud pubsub topics create activity-events-dead
2. Private ai-service + push subscription (Pub/Sub calls it with an OIDC token):
   gcloud iam service-accounts create pubsub-invoker
   gcloud run services add-iam-policy-binding ai-service --region REGION \
     --member serviceAccount:pubsub-invoker@PROJECT.iam.gserviceaccount.com --role roles/run.invoker
   gcloud pubsub subscriptions create activity-push --topic activity-events \
     --push-endpoint "$AI_URL/api/v1/tasks/process" \
     --push-auth-service-account pubsub-invoker@PROJECT.iam.gserviceaccount.com \
     --ack-deadline 120 --dead-letter-topic activity-events-dead --max-delivery-attempts 5
3. Copy TaskController.java into ai-service, ActivityPublisher.java into activity-service,
   add dependency com.google.cloud:google-cloud-pubsub, remove the @RabbitListener and spring-amqp usage,
   and set $USE_RABBIT = $false in deploy.ps1. activity-service's service account needs roles/pubsub.publisher.
