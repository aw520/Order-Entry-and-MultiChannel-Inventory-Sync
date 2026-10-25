import { FormEvent, useEffect, useState } from "react";

type InventoryItem = {
  sku: string;
  facility: string;
  onHandQty: number;
  reservedQty: number;
  amazonHoldQty: number;
  walmartHoldQty: number;
  csrAvailableQty: number;
};

type InventoryResponse = {
  items: InventoryItem[];
};

type OrderResponse = {
  id: number;
  sku: string;
  facility: string;
  quantity: number;
  status: string;
};

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`/api${path}`, options);
  const text = await response.text();
  let body: { message?: string } = {};

  try {
    body = text ? JSON.parse(text) : {};
  } catch {
    // A stopped backend can make the development proxy return a non-JSON response.
  }

  if (!response.ok) {
    throw new Error(body.message ?? `Request failed (${response.status}).`);
  }

  return body as T;
}

function message(error: unknown): string {
  return error instanceof Error ? error.message : "Request failed.";
}

export default function App() {
  const [items, setItems] = useState<InventoryItem[]>([]);
  const [sku, setSku] = useState("");
  const [facility, setFacility] = useState("");
  const [quantity, setQuantity] = useState("1");
  const [submitting, setSubmitting] = useState(false);
  const [feedback, setFeedback] = useState("");
  const [inventoryError, setInventoryError] = useState("");

  const skuOptions = [...new Set(items.map((item) => item.sku))];
  const facilityOptions = [
    ...new Set(items.filter((item) => item.sku === sku).map((item) => item.facility))
  ];

  useEffect(() => {
    setSku((current) => (
      items.some((item) => item.sku === current) ? current : (items[0]?.sku ?? "")
    ));
  }, [items]);

  useEffect(() => {
    setFacility((current) => (
      facilityOptions.includes(current) ? current : (facilityOptions[0] ?? "")
    ));
  }, [items, sku]);

  async function loadInventory() {
    const inventory = await request<InventoryResponse>("/inventory");
    setItems(inventory.items);
    setInventoryError("");
  }

  useEffect(() => {
    let stopped = false;
    let timer: number;

    async function poll() {
      try {
        const inventory = await request<InventoryResponse>("/inventory");
        if (!stopped) {
          setItems(inventory.items);
          setInventoryError("");
        }
      } catch (error) {
        if (!stopped) setInventoryError(message(error));
      } finally {
        if (!stopped) timer = window.setTimeout(poll, 1500);
      }
    }

    void poll();
    return () => {
      stopped = true;
      window.clearTimeout(timer);
    };
  }, []);

  async function submitOrder(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setFeedback("");

    try {
      const order = await request<OrderResponse>("/orders", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ sku, facility, quantity: Number(quantity) })
      });
      setFeedback(`Order #${order.id}: ${order.quantity} ${order.sku} allocated at ${order.facility}.`);
      await loadInventory();
    } catch (error) {
      setFeedback(message(error));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main>
      <h1>Order Entry & Inventory</h1>

      <section>
        <h2>Create CSR Order</h2>
        <form onSubmit={submitOrder}>
          <label>
            SKU
            <select
              value={sku}
              disabled={skuOptions.length === 0}
              onChange={(event) => {
                const selectedSku = event.target.value;
                setSku(selectedSku);
                setFacility(items.find((item) => item.sku === selectedSku)?.facility ?? "");
              }}
            >
              {skuOptions.length === 0 && <option value="">No inventory</option>}
              {skuOptions.map((option) => (
                <option key={option} value={option}>{option}</option>
              ))}
            </select>
          </label>

          <label>
            Facility
            <select
              value={facility}
              disabled={facilityOptions.length === 0}
              onChange={(event) => setFacility(event.target.value)}
            >
              {facilityOptions.length === 0 && <option value="">No facility</option>}
              {facilityOptions.map((option) => (
                <option key={option} value={option}>{option}</option>
              ))}
            </select>
          </label>

          <label>
            Quantity
            <input
              type="number"
              min="1"
              max="100000"
              required
              value={quantity}
              onChange={(event) => setQuantity(event.target.value)}
            />
          </label>

          <button disabled={submitting || !sku || !facility}>
            {submitting ? "Submitting..." : "Create Order"}
          </button>
        </form>
        {feedback && <p className="feedback">{feedback}</p>}
      </section>

      <section>
        <h2>Inventory</h2>
        {inventoryError && <p className="error">Cannot refresh inventory: {inventoryError}</p>}
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>SKU</th>
                <th>Facility</th>
                <th>On Hand</th>
                <th>Reserved</th>
                <th>Amazon Hold</th>
                <th>Walmart Hold</th>
                <th>CSR Available</th>
              </tr>
            </thead>
            <tbody>
              {items.map((item) => (
                <tr key={`${item.sku}-${item.facility}`}>
                  <td>{item.sku}</td>
                  <td>{item.facility}</td>
                  <td>{item.onHandQty}</td>
                  <td>{item.reservedQty}</td>
                  <td>{item.amazonHoldQty}</td>
                  <td>{item.walmartHoldQty}</td>
                  <td>{item.csrAvailableQty}</td>
                </tr>
              ))}
              {items.length === 0 && (
                <tr>
                  <td colSpan={7}>Loading inventory...</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </section>
    </main>
  );
}
