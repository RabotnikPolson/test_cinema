import { useState } from "react";
import { useProducts, useCreateOrder } from "@/features/shop";
import "@/shared/styles/pages/Shop.css";

export default function ShopPage() {
  const { data: products = [], isLoading, isError } = useProducts();
  const orderMut = useCreateOrder();
  const [selected, setSelected] = useState(null);
  const [toast, setToast] = useState("");

  const handleOrder = () => {
    if (!selected) return;
    orderMut.mutate(selected.id, {
      onSuccess: () => {
        setSelected(null);
        setToast("Заказ успешно оформлен!");
        setTimeout(() => setToast(""), 3000);
      },
      onError: (err) => {
        const msg = err.response?.data?.message || "Ошибка оформления заказа";
        setToast(msg);
        setTimeout(() => setToast(""), 3000);
      },
    });
  };

  if (isLoading) {
    return <div className="container shop-page"><p>Загрузка товаров...</p></div>;
  }

  if (isError) {
    return <div className="container shop-page"><p>Ошибка загрузки товаров</p></div>;
  }

  return (
    <div className="container shop-page">
      <h1>Мерч CineVerse</h1>

      {toast && <div className="shop-toast">{toast}</div>}

      {products.length === 0 ? (
        <p style={{ color: "var(--text-dim)" }}>Пока нет товаров в магазине.</p>
      ) : (
        <div className="shop-grid">
          {products.map((product) => (
            <div key={product.id} className="shop-card">
              {product.imageUrl && (
                <img src={product.imageUrl} alt={product.name} className="shop-card-img" />
              )}
              {!product.imageUrl && (
                <div className="shop-card-img" style={{ background: "var(--space)" }} />
              )}
              <div className="shop-card-body">
                <h3 className="shop-card-name">{product.name}</h3>
                <p className="shop-card-desc">{product.description}</p>
                <div className="shop-card-footer">
                  <div>
                    <div className="shop-card-price">{product.price} ₸</div>
                    <div className="shop-card-stock">
                      {product.stock > 0 ? `В наличии: ${product.stock}` : "Нет в наличии"}
                    </div>
                  </div>
                  <button
                    className="shop-buy-btn"
                    disabled={product.stock <= 0}
                    onClick={() => setSelected(product)}
                  >
                    Купить
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Order confirmation modal */}
      {selected && (
        <div className="shop-modal-overlay" onClick={() => setSelected(null)}>
          <div className="shop-modal" onClick={(e) => e.stopPropagation()}>
            <h2>Оформление заказа</h2>
            <div className="shop-modal-detail">
              <strong>{selected.name}</strong>
              <br />
              {selected.description}
            </div>
            <div className="shop-modal-price">{selected.price} ₸</div>
            <div className="shop-modal-actions">
              <button className="shop-modal-cancel" onClick={() => setSelected(null)}>
                Отмена
              </button>
              <button
                className="shop-buy-btn"
                onClick={handleOrder}
                disabled={orderMut.isPending}
              >
                {orderMut.isPending ? "Оформляем..." : "Подтвердить заказ"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
