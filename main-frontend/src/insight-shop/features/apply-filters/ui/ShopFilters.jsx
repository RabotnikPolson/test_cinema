import React from 'react';
import { useProductStore } from '../../../entities/product/model/productStore';
import './ShopFilters.css';

export const ShopFilters = () => {
  const filters = useProductStore(state => state.filters);
  const setFilter = useProductStore(state => state.setFilter);
  const resetFilters = useProductStore(state => state.resetFilters);

  const categories = ['all', 'Одежда', 'Аксессуары', 'Книги', 'Канцелярия', 'Подарки'];

  const handleCategoryChange = (cat) => {
    setFilter('category', cat);
  };

  return (
    <div className="shop-filters-sidebar">
      <div className="filter-group">
        <h3 className="filter-title">Категории</h3>
        <ul className="filter-category-list">
          {categories.map(cat => (
            <li 
              key={cat} 
              className={`filter-category-item ${filters.category === cat ? 'active' : ''}`}
              onClick={() => handleCategoryChange(cat)}
            >
              {cat === 'all' ? 'Все товары' : cat}
            </li>
          ))}
        </ul>
      </div>

      <div className="filter-group">
        <h3 className="filter-title">Цена</h3>
        <input 
          type="range" 
          className="price-range"
          min="0" 
          max="50000" 
          step="1000"
          value={filters.priceRange[1]}
          onChange={(e) => setFilter('priceRange', [0, parseInt(e.target.value)])}
        />
        <div className="price-labels">
          <span>0 ₸</span>
          <span>до {filters.priceRange[1].toLocaleString()} ₸</span>
        </div>
      </div>

      <div className="filter-group checkbox-group">
        <label className="checkbox-label">
          <input 
            type="checkbox" 
            checked={filters.inStockOnly}
            onChange={(e) => setFilter('inStockOnly', e.target.checked)}
          />
          <span className="checkbox-custom"></span>
          В наличии
        </label>
      </div>
      
      <button className="reset-filters-btn" onClick={resetFilters}>Сбросить фильтры</button>
    </div>
  );
};
