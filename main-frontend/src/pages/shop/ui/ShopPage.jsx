import React, { useState } from "react";
import { Gift, Shirt, BookOpen, Coffee, HelpCircle, Package, Film } from "lucide-react";
import "@/pages/shop/ui/Shop.css";

const MOCK_PRODUCTS = [
  {
    id: 1,
    name: "Оверсайз Худи INSIGHT",
    description: "Премиальное худи черного цвета с вышитым логотипом. Идеально для долгих киномарафонов.",
    price: 18500,
    stock: 42,
    icon: <Shirt size={48} strokeWidth={1.5} />,
    color: "linear-gradient(135deg, #1f1f33, #111111)",
    accent: "rgba(201, 168, 76, 0.6)"
  },
  {
    id: 2,
    name: "Артбук: Создание INSIGHT",
    description: "Концепт-арты, интерфейсы и скрытые пасхалки: 200 страниц о том, как создавался проект.",
    price: 12000,
    stock: 15,
    icon: <BookOpen size={48} strokeWidth={1.5} />,
    color: "linear-gradient(135deg, #2a1f2b, #150d18)",
    accent: "rgba(219, 112, 147, 0.6)"
  },
  {
    id: 3,
    name: "Лимитированный Постер",
    description: "Голографический постер А2 с главными героями вселенной. Пронумерован.",
    price: 6500,
    stock: 8,
    icon: <Film size={48} strokeWidth={1.5} />,
    color: "linear-gradient(135deg, #1b2633, #0a111a)",
    accent: "rgba(85, 172, 238, 0.6)"
  },
  {
    id: 4,
    name: "Термокружка Киномана",
    description: "Держит кофе горячим весь режиссерский кат «Властелина Колец». Объем 500мл.",
    price: 8900,
    stock: 100,
    icon: <Coffee size={48} strokeWidth={1.5} />,
    color: "linear-gradient(135deg, #33261b, #1a120a)",
    accent: "rgba(224, 150, 75, 0.6)"
  },
  {
    id: 5,
    name: "Загадочный Бокс",
    description: "Что внутри? Мы сами не знаем. Возможно билеты на премьеру, а может просто стикеры.",
    price: 5000,
    stock: 3,
    icon: <Package size={48} strokeWidth={1.5} />,
    color: "linear-gradient(135deg, #1a2a22, #0d1813)",
    accent: "rgba(80, 200, 120, 0.6)"
  }
];

export default function ShopPage() {
  const [selected, setSelected] = useState(null);
  const [toast, setToast] = useState("");

  const handleOrder = () => {
    setToast("Ой! Это демо-витрина. Но мы оценили ваш вкус!");
    setSelected(null);
    setTimeout(() => setToast(""), 3500);
  };

  return (
    <div className="container shop-page">
      <div className="shop-header-banner">
        <h1>Мерч INSIGHT</h1>
        <div className="shop-demo-badge">
          <HelpCircle size={16} />
          <span>Демо-раздел: Скоро здесь появятся настоящие товары</span>
        </div>
      </div>

      {toast && <div className="shop-toast">{toast}</div>}

      <div className="shop-grid">
        {MOCK_PRODUCTS.map((product) => (
          <div key={product.id} className="shop-card">
            <div 
              className="shop-card-img placeholder-img" 
              style={{ 
                background: product.color,
                color: product.accent
              }}
            >
              {product.icon}
            </div>
            
            <div className="shop-card-body">
              <h3 className="shop-card-name">{product.name}</h3>
              <p className="shop-card-desc">{product.description}</p>
              <div className="shop-card-footer">
                <div>
                  <div className="shop-card-price">{product.price.toLocaleString("ru-RU")} ₸</div>
                  <div className="shop-card-stock">В наличии: {product.stock}</div>
                </div>
                <button
                  className="shop-buy-btn"
                  onClick={() => setSelected(product)}
                >
                  Купить
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Order confirmation modal */}
      {selected && (
        <div className="shop-modal-overlay" onClick={() => setSelected(null)}>
          <div className="shop-modal" onClick={(e) => e.stopPropagation()}>
            <div className="shop-modal-header" style={{ display: 'flex', gap: '16px', alignItems: 'center', marginBottom: '16px' }}>
              <div style={{ width: '48px', height: '48px', borderRadius: '12px', background: selected.color, display: 'flex', alignItems: 'center', justifyContent: 'center', color: selected.accent, flexShrink: 0 }}>
                {React.cloneElement(selected.icon, { size: 24 })}
              </div>
              <div>
                <h2>{selected.name}</h2>
                <div className="shop-modal-price" style={{ fontSize: '1.2rem', marginTop: '4px' }}>{selected.price.toLocaleString("ru-RU")} ₸</div>
              </div>
            </div>
            
            <div className="shop-modal-detail">
              <p>{selected.description}</p>
              <br/>
              <p style={{ color: 'var(--cv-gold)', fontSize: '0.85rem', opacity: 0.8 }}>
                *Внимание: это тестовый товар. Списание средств не произойдет.
              </p>
            </div>
            
            <div className="shop-modal-actions">
              <button className="shop-modal-cancel" onClick={() => setSelected(null)}>
                Закрыть
              </button>
              <button
                className="shop-buy-btn"
                onClick={handleOrder}
              >
                Оформить демо-заказ
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

