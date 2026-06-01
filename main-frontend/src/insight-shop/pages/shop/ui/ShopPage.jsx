import React, { useEffect, useMemo } from 'react';
import { useProductStore } from '../../../entities/product/model/productStore';
import { ProductGrid } from '../../../widgets/product-grid/ui/ProductGrid';
import { ShopFilters } from '../../../features/apply-filters/ui/ShopFilters';
import { Star } from 'lucide-react';
import '@/insight-shop/pages/shop/ui/Shop.css';

export default function ShopPage() {
  const fetchProducts = useProductStore(state => state.fetchProducts);
  const productsMap = useProductStore(state => state.products);
  const isLoading = useProductStore(state => state.isLoading);
  const filters = useProductStore(state => state.filters);
  
  useEffect(() => {
    fetchProducts();
  }, [fetchProducts]);

  const filteredProducts = useMemo(() => {
    let result = Object.values(productsMap);

    if (filters.category !== 'all') {
      result = result.filter(p => p.category === filters.category);
    }
    
    result = result.filter(p => p.price <= filters.priceRange[1] && p.price >= filters.priceRange[0]);
    
    if (filters.inStockOnly) {
      result = result.filter(p => p.stock > 0);
    }

    if (filters.sortBy === 'priceAsc') {
      result.sort((a, b) => a.price - b.price);
    } else if (filters.sortBy === 'priceDesc') {
      result.sort((a, b) => b.price - a.price);
    } else if (filters.sortBy === 'new') {
      result.sort((a, b) => (a.isNew === b.isNew ? 0 : a.isNew ? -1 : 1));
    }
    // 'popular' keeps default order for now

    return result;
  }, [productsMap, filters]);

  return (
    <div className="shop-page">
      <div className="shop-header-simple">
        <h1>Insight Store</h1>
        <p>Premium Merch & Accessories</p>
      </div>

      <div className="shop-container">
        <aside>
          <ShopFilters />
        </aside>
        
        <main>
          {filteredProducts.length === 0 && !isLoading ? (
            <div style={{ textAlign: 'center', padding: '80px 0', color: '#a0a0b0' }}>
              <h2>Ничего не найдено</h2>
              <p>Попробуйте изменить параметры фильтрации</p>
            </div>
          ) : (
            <ProductGrid products={filteredProducts} isLoading={isLoading} />
          )}
        </main>
      </div>
    </div>
  );
}
