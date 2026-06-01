import React, { useState } from 'react';
import { useCheckout } from '../../../features/checkout/model/useCheckout';
import { Link } from 'react-router-dom';
import { Check } from 'lucide-react';

export const CheckoutStepper = () => {
  const { step, nextStep, prevStep, handleCheckout, isProcessing } = useCheckout();
  
  const [formData, setFormData] = useState({
    name: '', email: '', city: '', address: '', card: ''
  });
  const [errors, setErrors] = useState({});

  const validateStep = () => {
    let newErrors = {};
    if (step === 1) {
      if (!formData.name) newErrors.name = 'Обязательное поле';
      if (!formData.email || !formData.email.includes('@')) newErrors.email = 'Некорректный email';
    }
    if (step === 2) {
      if (!formData.city) newErrors.city = 'Обязательное поле';
      if (!formData.address) newErrors.address = 'Обязательное поле';
    }
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleNext = () => {
    if (validateStep()) {
      nextStep();
    }
  };

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
    setErrors({ ...errors, [e.target.name]: '' });
  };

  if (step === 4) {
    return (
      <div className="checkout-stepper-container success-state">
        <div className="success-icon">
          <Check size={48} />
        </div>
        <h2 style={{ color: '#4caf50', fontSize: '2.5rem', marginBottom: '16px' }}>Успешно!</h2>
        <p style={{ color: '#a0a0b0', marginBottom: '40px', fontSize: '1.2rem' }}>
          Номер вашего заказа: <strong style={{ color: '#fff' }}>#ORD-{Math.floor(Math.random() * 10000)}</strong>
        </p>
        <Link to="/shop" className="checkout-btn" style={{ textDecoration: 'none' }}>
          Продолжить покупки
        </Link>
      </div>
    );
  }

  const steps = ['Контакты', 'Доставка', 'Оплата'];

  return (
    <div className="checkout-stepper-container">
      <div className="stepper-header">
        {steps.map((s, i) => (
          <div key={s} className={`stepper-step ${step > i + 1 ? 'completed' : step === i + 1 ? 'active' : ''}`}>
            <div className="step-indicator">
              {step > i + 1 ? <Check size={20} /> : i + 1}
            </div>
            <div className="step-label">{s}</div>
          </div>
        ))}
      </div>

      <div style={{ minHeight: '300px' }}>
        {step === 1 && (
          <div>
            <h3 style={{ margin: '0 0 24px', fontSize: '1.5rem' }}>Контактные данные</h3>
            <div className="checkout-form-group">
              <input 
                name="name" type="text" placeholder="Имя и Фамилия" 
                className={`checkout-input ${errors.name ? 'error' : ''}`} 
                value={formData.name} onChange={handleChange} 
              />
              {errors.name && <span className="error-msg">{errors.name}</span>}
            </div>
            <div className="checkout-form-group">
              <input 
                name="email" type="email" placeholder="Email адрес" 
                className={`checkout-input ${errors.email ? 'error' : ''}`} 
                value={formData.email} onChange={handleChange} 
              />
              {errors.email && <span className="error-msg">{errors.email}</span>}
            </div>
          </div>
        )}
        {step === 2 && (
          <div>
            <h3 style={{ margin: '0 0 24px', fontSize: '1.5rem' }}>Адрес доставки</h3>
            <div className="checkout-form-group">
              <input 
                name="city" type="text" placeholder="Город" 
                className={`checkout-input ${errors.city ? 'error' : ''}`} 
                value={formData.city} onChange={handleChange} 
              />
              {errors.city && <span className="error-msg">{errors.city}</span>}
            </div>
            <div className="checkout-form-group">
              <input 
                name="address" type="text" placeholder="Улица, дом, квартира" 
                className={`checkout-input ${errors.address ? 'error' : ''}`} 
                value={formData.address} onChange={handleChange} 
              />
              {errors.address && <span className="error-msg">{errors.address}</span>}
            </div>
          </div>
        )}
        {step === 3 && (
          <div>
            <h3 style={{ margin: '0 0 16px', fontSize: '1.5rem' }}>Оплата</h3>
            <p style={{ color: '#a0a0b0', marginBottom: '24px' }}>
              В тестовом режиме реальная оплата не требуется. Нажмите "Оплатить" для завершения.
            </p>
            <div className="checkout-form-group">
              <input 
                name="card" type="text" placeholder="Номер карты (необязательно)" 
                className="checkout-input" 
                value={formData.card} onChange={handleChange} 
              />
            </div>
          </div>
        )}
      </div>

      <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '48px', paddingTop: '24px', borderTop: '1px solid rgba(255,255,255,0.05)' }}>
        {step > 1 ? (
          <button onClick={prevStep} disabled={isProcessing} className="checkout-btn-outline">Назад</button>
        ) : <div />}
        
        {step < 3 ? (
          <button onClick={handleNext} className="checkout-btn">Далее</button>
        ) : (
          <button onClick={handleCheckout} disabled={isProcessing} className="checkout-btn">
            {isProcessing ? 'Обработка...' : 'Оплатить'}
          </button>
        )}
      </div>
    </div>
  );
};
