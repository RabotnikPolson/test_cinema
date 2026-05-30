import { useState } from "react";
import { useProducts, useCreateProduct } from "@/features/shop";
import "@/pages/shop/ui/Shop.css";

const EMPTY_FORM = { name: "", description: "", price: "", imageUrl: "", stock: "" };

export default function AdminShopManager() {
  const { data: products = [], isLoading } = useProducts();
  const createMut = useCreateProduct();
  const [form, setForm] = useState(EMPTY_FORM);
  const [msg, setMsg] = useState("");

  const set = (field) => (e) => setForm((prev) => ({ ...prev, [field]: e.target.value }));

  const onSubmit = (e) => {
    e.preventDefault();
    if (!form.name || !form.price) {
      setMsg("Укажите название и цену");
      return;
    }
    createMut.mutate(
      {
        name: form.name,
        description: form.description,
        price: parseFloat(form.price),
        imageUrl: form.imageUrl,
        stock: parseInt(form.stock, 10) || 0,
      },
      {
        onSuccess: () => {
          setForm(EMPTY_FORM);
          setMsg("Товар добавлен!");
          setTimeout(() => setMsg(""), 3000);
        },
        onError: () => {
          setMsg("Ошибка при добавлении товара");
          setTimeout(() => setMsg(""), 3000);
        },
      },
    );
  };

  return (
    <div className="container shop-page">
      <h1>Управление магазином</h1>

      {msg && (
        <div className="shop-toast">{msg}</div>
      )}

      <form className="admin-shop-form" onSubmit={onSubmit}>
        <label>
          Название
          <input value={form.name} onChange={set("name")} placeholder="Футболка INSIGHT" />
        </label>
        <label>
          Цена (₸)
          <input type="number" step="0.01" value={form.price} onChange={set("price")} placeholder="5990" />
        </label>
        <label>
          URL картинки
          <input value={form.imageUrl} onChange={set("imageUrl")} placeholder="https://..." />
        </label>
        <label>
          Количество
          <input type="number" value={form.stock} onChange={set("stock")} placeholder="100" />
        </label>
        <label className="full-width">
          Описание
          <textarea value={form.description} onChange={set("description")} placeholder="Описание товара..." />
        </label>
        <div className="full-width">
          <button className="shop-buy-btn" type="submit" disabled={createMut.isPending}>
            {createMut.isPending ? "Добавляем..." : "Добавить товар"}
          </button>
        </div>
      </form>

      <h2 style={{ color: "var(--ice)", fontFamily: "var(--font-heading)", marginBottom: 16 }}>
        Товары ({products.length})
      </h2>

      {isLoading ? (
        <p>Загрузка...</p>
      ) : products.length === 0 ? (
        <p style={{ color: "var(--text-dim)" }}>Товаров пока нет.</p>
      ) : (
        <div className="shop-grid">
          {products.map((product) => (
            <div key={product.id} className="shop-card">
              {product.imageUrl && (
                <img src={product.imageUrl} alt={product.name} className="shop-card-img" />
              )}
              <div className="shop-card-body">
                <h3 className="shop-card-name">{product.name}</h3>
                <p className="shop-card-desc">{product.description}</p>
                <div className="shop-card-footer">
                  <div className="shop-card-price">{product.price} ₸</div>
                  <div className="shop-card-stock">Остаток: {product.stock}</div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
