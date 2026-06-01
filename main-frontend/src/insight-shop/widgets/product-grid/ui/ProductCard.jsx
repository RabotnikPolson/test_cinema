import React from 'react';
import { Link } from 'react-router-dom';
import { ProductPrice } from '../../../entities/product/ui/ProductPrice';
import { StockBadge } from '../../../entities/product/ui/StockBadge';
import { AddToCartButton } from '../../../features/add-to-cart/ui/AddToCartButton';
import { ShoppingCart } from 'lucide-react';

export const ProductCard = ({ product }) => {
  return (
    <div className="shop-card">
      <Link to={`/shop/${product.slug}`} style={{ textDecoration: 'none', color: 'inherit' }}>
        <div className="shop-card-img-wrapper" style={{ background: product.imageColor || 'var(--space)' }}>
          <div className="shop-card-badges">
            {product.isNew && <span className="badge new">Новинка</span>}
            {product.isHit && <span className="badge hit">Хит</span>}
          </div>
          {product.image ? (
            <img src={product.image} alt={product.name} className="shop-card-img" loading="lazy" />
          ) : (
            <div className="shop-card-img" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', color: product.accent }}>
              <span style={{ fontSize: '48px', opacity: 0.8 }}>📦</span>
            </div>
          )}
        </div>
      </Link>
      
      <div className="shop-card-body">
        <Link to={`/shop/${product.slug}`} style={{ textDecoration: 'none', color: 'inherit' }}>
          <div className="shop-card-category">{product.category || 'Мерч'}</div>
          <h3 className="shop-card-name" style={{ minHeight: '48px' }}>{product.name}</h3>
        </Link>
        <p className="shop-card-desc">
          {product.description}
        </p>
        <div className="shop-card-footer">
          <div>
            <ProductPrice price={product.price} className="shop-card-price" />
            <StockBadge stock={product.stock} />
          </div>
          <AddToCartButton product={product} />
        </div>
      </div>
    </div>
  );
};
