## Inventory Service Metrics
- Monitor stock reservations, commits, and release operations.
- Identify stockout situations and catalog discrepancies.

### Reservations
- `inventory.reservations.total`: Total inventory reservation requests
- `inventory.reservations.success.total`: Successfully reserved inventory
- `inventory.reservations.failed.total`: Failed inventory reservations

### Exceptions and Failures
- `inventory.outofstock.total`: Inventory reservation failures caused by insufficient stock
- `inventory.itemnotfound.total`: Inventory reservation failures caused by missing products

### Commits and Releases
- `inventory.commit.success.total`: Successfully committed inventory
- `inventory.commit.failed.total`: Failed inventory commit operations
- `inventory.release.total`: Successfully released inventory
