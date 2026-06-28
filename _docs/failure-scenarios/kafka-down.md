- Order Service, Payment Service and Inventory Service utilize outbox pattern for sending Kafka messages back to the orchestrator
- Kafka Producer Client configured with a timeout in the place (max block, request and delivery timeout)
- Each outbox entry is configured with max 3 attempts to be delivered back to the orchestrator
- Scheduler triggered every 3 seconds to check for pending events in the outbox table
- In case Kafka is not available for all 3 attempts for a single outbox entity, entity is moved in the persistent database DLQ table for the manual inspection
- Make sure no events are lost during the process

Problems:
- The DLQ Definition Rule: A Database DLQ is for Poison Pills (corrupted data, parsing errors, invalid schemas). It must never be used for infrastructure outages (Kafka down).
- The Outbox as a Persistent Buffer: When Kafka is down, the local database Outbox table becomes your temporary message broker. Let it hold the data indefinitely; that is exactly what it is designed to do.
- The Ordering Trap: Moving a single stalled outbox item to a DLQ while letting newer rows pass by breaks message sequencing. This causes down-stream state machines to process steps out of order.
- Log Pollution Control: Schedulers ticking every 3 seconds during a 2-hour outage will generate 2,400 identical error logs per microservice. Exponential backoff keeps logs clean so engineers can see the real root cause.
- At-Least-Once Delivery Guarantee: You can only guarantee zero data loss if the Outbox row deletion/status update is triggered after receiving a successful RecordMetadata acknowledgment callback from the Kafka producer API.