# Order Entry and Multi-Channel Inventory Sync

This project is a simplified order management system for CSR order entry and multi-channel inventory visibility. A CSR can create an order for a SKU and facility, receive clear success or error feedback, and view inventory that refreshes automatically. The backend also simulates publishing allocated quantities to Amazon and Walmart.

## Architecture and Design

The application uses a React and TypeScript frontend, a Spring Boot backend, and MySQL. Backend responsibilities are separated into controllers, services, JDBI SQL Object repositories, and a marketplace publisher. Flyway manages the schema and starting data.

The data model contains four application tables:

- `sku` and `facility` define the available products and locations.
- `inventory` stores physical on-hand quantity, reservations, and marketplace holds for each SKU and facility.
- `customer_order` stores the historical orders supplied by the demo and new CSR orders.

The inventory numbers in the exercise are treated as physical on-hand quantities. For this demo, Amazon and Walmart receive fixed, non-overlapping holds, while CSR orders may use only the remaining quantity:

```text
CSR available = on hand - reserved - Amazon hold - Walmart hold
```

CSR allocation is performed with a guarded database update that checks availability and increments the reserved quantity in one statement. InnoDB serializes concurrent updates to the same inventory row. The reservation and order insert run in one Spring transaction, so they either both commit or both roll back. This approach keeps correctness in the database and is sufficient because the allocation rule is local to one inventory row.

The complete inventory relationship is `on hand = reserved + Amazon hold + Walmart hold + CSR available`. The database also enforces `reserved + Amazon hold + Walmart hold <= on hand` to prevent overselling. After a reservation succeeds, the CSR order is created directly in the `ALLOCATED` state.

The demo covers three main scenarios:

- A valid CSR order reserves inventory and creates an `ALLOCATED` order.
- An order above the CSR-available quantity is rejected without changing inventory.
- Concurrent requests cannot reserve more than the available quantity; an order insert failure rolls back its reservation.

The 10 white-shirt and 3 black-shirt historical orders are sample records created from the exercise's starting state. Each historical record represents one unit. The design also assumes each marketplace honors its assigned inventory hold; this application does not control marketplace checkout.

## Run Locally

Requirements: JDK 17, MySQL 8+, and Node.js 20+.

Set the database credentials before starting the backend:

```bash
export DB_USER=root
export DB_PASSWORD=your_password
```

`DB_HOST`, `DB_PORT`, and `DB_NAME` are optional. The default database name is `oms_takehome`. Flyway creates and seeds the application tables on startup.

Start the backend:

```bash
cd backend
./mvnw spring-boot:run
```

Start the frontend in another terminal:

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`. The backend runs on `http://localhost:8080`.

## API

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/api/inventory` | View inventory by SKU and facility |
| `POST` | `/api/orders` | Create and allocate a CSR order |
| `GET` | `/api/orders/{id}` | Get an order by ID |
| `GET` | `/api/orders?limit=20` | List recent orders |
| `POST` | `/api/marketplaces/publish` | Simulate publishing marketplace quantities |

Example:

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"sku":"TShirt-Black","facility":"NJ-DC","quantity":1}'
```

## Tests

The backend test suite uses the separate `oms_takehome_test` database. It covers validation, insufficient inventory, transaction rollback, and concurrent allocation.

```bash
cd backend
./mvnw test
```

The frontend production build can be checked with:

```bash
cd frontend
npm run build
```

## Limitations and Future Improvements

- Products, facilities, and starting inventory are managed through Flyway data. There is no admin API for adding or updating products, replenishing inventory, or changing channel holds.
- Amazon and Walmart holds are fixed for this demo. Inventory is not dynamically rebalanced between channels.
- Marketplace publishing is log-only; there is no external delivery, retry, acknowledgement, or reconciliation process.
- CSR allocation is transactional, so partial failures roll back. However, there is no automatic retry, request idempotency, or background reconciliation process; retrying after a lost response could create a duplicate order.
- The order lifecycle stops at `ALLOCATED`. Cancellation, fulfillment, and authentication are outside the current scope.
