Downstream Service is down for a longer time:
- Sage orchestrator has a scheduler which will be triggered every 6 minutes
- Every order not in final state will be set in TIMED_OUT state after 5 minutes
- Compensation steps will be applied (e.g., PAYMENT REFUND, INVENTORY RELEASE...)

Logs:
![img.png](timeout1.png)
![img_1.png](timeout3.png)
![img_2.png](timeout2.png)

Metrics:
![img.png](timeout-metrics.png)