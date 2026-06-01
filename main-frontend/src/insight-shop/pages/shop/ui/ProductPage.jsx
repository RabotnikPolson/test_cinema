import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useProductStore } from '../../../entities/product/model/productStore';
import { ProductPrice } from '../../../entities/product/ui/ProductPrice';
import { StockBadge } from '../../../entities/product/ui/StockBadge';
import { AddToCartButton } from '../../../features/add-to-cart/ui/AddToCartButton';
import { ArrowLeft } from 'lucide-react';
import './ProductPage.css';

export default function ProductPage() {
  const { slug } = useParams();
  const getProductBySlug = useProductStore(state => state.getProductBySlug);
  const [product, setProduct] = useState(null);
  const [loading, setLoading] = useState(true);

  const [selectedSize, setSelectedSize] = useState(null);
  const [selectedColor, setSelectedColor] = useState(null);
  const [activeTab, setActiveTab] = useState('description');

  useEffect(() => {
    getProductBySlug(slug).then(p => {
      setProduct(p);
      if (p) {
        if (p.sizes && p.sizes.length > 0) setSelectedSize(p.sizes[0]);
        if (p.colors && p.colors.length > 0) setSelectedColor(p.colors[0]);
      }
      setLoading(false);
    });
  }, [slug, getProductBySlug]);

  if (loading) return <div style={{ padding: '60px', textAlign: 'center', color: '#a0a0b0' }}>Загрузка...</div>;
  if (!product) return <div style={{ padding: '60px', textAlign: 'center', color: '#a0a0b0' }}>Товар не найден</div>;

  return (
    <div className="product-page-container">
      <Link to="/shop" className="back-link">
        <ArrowLeft size={16} /> Вернуться в каталог
      </Link>
      
      <div className="product-details-layout">
        <div className="product-gallery">
          <div className="product-main-image-wrapper" style={{ background: product.imageColor }}>
            {product.isNew && <span className="badge new" style={{ position: 'absolute', top: 16, left: 16, zIndex: 2 }}>Новинка</span>}
            {product.isHit && <span className="badge hit" style={{ position: 'absolute', top: 16, left: 16, zIndex: 2 }}>Хит</span>}
            {product.image ? (
              <img src={product.image} alt={product.name} className="product-main-image" />
            ) : (
              <span style={{ fontSize: '100px', opacity: 0.8 }}>📦</span>
            )}
          </div>
        </div>
        
        <div className="product-info-wrapper">
          <div>
            <div className="product-category-tag">{product.category || 'Коллекция'}</div>
            <h1 className="product-title">{product.name}</h1>
            <div className="product-price-large">
              <ProductPrice price={product.price} />
            </div>
            
            <p className="product-description-text">
              {product.description}
            </p>
          </div>
          
          <div className="product-variants">
            {product.colors && product.colors.length > 0 && (
              <div className="variant-group">
                <h4>Цвет</h4>
                <div className="color-selector">
                  {product.colors.map(color => (
                    <div 
                      key={color}
                      className={`color-circle ${selectedColor === color ? 'selected' : ''}`}
                      style={{ backgroundColor: color }}
                      onClick={() => setSelectedColor(color)}
                    />
                  ))}
                </div>
              </div>
            )}
            
            {product.sizes && product.sizes.length > 0 && (
              <div className="variant-group">
                <h4>Размер</h4>
                <div className="size-selector">
                  {product.sizes.map(size => (
                    <button 
                      key={size}
                      className={`size-btn ${selectedSize === size ? 'selected' : ''}`}
                      onClick={() => setSelectedSize(size)}
                    >
                      {size}
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>

          <div className="add-to-cart-container">
            <StockBadge stock={product.stock} />
            {/* Pass selected options if needed to cart button in the future */}
            <div style={{ width: '100%', maxWidth: '400px', marginTop: '16px' }}>
              <AddToCartButton product={product} />
            </div>
          </div>
          
          <div className="product-tabs">
            <div className="tabs-header">
              <button 
                className={`tab-btn ${activeTab === 'description' ? 'active' : ''}`}
                onClick={() => setActiveTab('description')}
              >
                Описание
              </button>
              <button 
                className={`tab-btn ${activeTab === 'features' ? 'active' : ''}`}
                onClick={() => setActiveTab('features')}
              >
                Характеристики
              </button>
            </div>
            <div className="tab-content">
              {activeTab === 'description' ? (
                <p>{product.description} Эта эксклюзивная вещь создана специально для настоящих ценителей кинематографа. Каждая деталь продумана так, чтобы подчеркнуть ваш стиль и любовь к искусству. Ограниченный тираж.</p>
              ) : (
                <ul>
                  <li>Материал: Премиальное качество</li>
                  <li>Доставка: По всему Казахстану</li>
                  <li>Гарантия: 30 дней на возврат</li>
                  <li>Эксклюзивность: Лимитированная серия</li>
                </ul>
              )}
            </div>
          </div>
          
        </div>
      </div>
    </div>
  );
}
