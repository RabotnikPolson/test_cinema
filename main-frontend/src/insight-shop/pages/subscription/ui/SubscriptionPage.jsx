import React, { useEffect, useState } from 'react';
import { PlanComparisonTable } from '../../../widgets/plan-comparison/ui/PlanComparisonTable';
import '@/insight-shop/pages/subscription/ui/Subscription.css';
import { useSubscriptionStore } from '../../../entities/subscription/model/subscriptionStore';
import { SubscriptionStatus } from '../../../entities/subscription/ui/SubscriptionStatus';
import { useCancelSubscription } from '../../../features/cancel-subscription/model/useCancelSubscription';
import { CancelSubscriptionModal } from '../../../features/cancel-subscription/ui/CancelSubscriptionModal';
import { Plus } from 'lucide-react';

const FAQItem = ({ question, answer }) => {
  const [isOpen, setIsOpen] = useState(false);
  return (
    <div className={`faq-item ${isOpen ? 'open' : ''}`}>
      <div className="faq-question" onClick={() => setIsOpen(!isOpen)}>
        {question}
        <Plus size={20} className="faq-icon" style={{ transition: 'transform 0.3s' }} />
      </div>
      <div className="faq-answer">
        {answer}
      </div>
    </div>
  );
};

export default function SubscriptionPage() {
  const mySubscription = useSubscriptionStore(state => state.mySubscription);
  const fetchMySubscription = useSubscriptionStore(state => state.fetchMySubscription);
  const { isModalOpen, openModal, closeModal, handleCancel, isLoading, error } = useCancelSubscription();
  
  const [isYearly, setIsYearly] = useState(false);

  useEffect(() => {
    fetchMySubscription();
  }, [fetchMySubscription]);

  return (
    <div className="sub-page">
      <header className="sub-header">
        <h1 className="sub-title">Insight Premium</h1>
        <p className="sub-desc">
          Получите безграничный доступ к шедеврам кинематографа и эксклюзивному мерчу со скидками. Ваш пропуск в мир высокого искусства.
        </p>
      </header>

      {mySubscription && mySubscription.status === 'ACTIVE' ? (
        <div style={{ marginBottom: '60px', maxWidth: '800px', margin: '0 auto 60px' }}>
          <SubscriptionStatus 
            subscription={mySubscription} 
            onCancel={openModal} 
            onChangePayment={() => alert('Демо: Смена способа оплаты')} 
          />
        </div>
      ) : null}

      <div className="billing-toggle" data-yearly={isYearly}>
        <span 
          className={`toggle-label ${!isYearly ? 'active' : ''}`}
          onClick={() => setIsYearly(false)}
        >
          Ежемесячно
        </span>
        <div className="toggle-switch" onClick={() => setIsYearly(!isYearly)}>
          <div className="toggle-thumb" />
        </div>
        <span 
          className={`toggle-label ${isYearly ? 'active' : ''}`}
          onClick={() => setIsYearly(true)}
        >
          Ежегодно <span className="discount-badge">-20%</span>
        </span>
      </div>

      <PlanComparisonTable isYearly={isYearly} />

      <section className="faq-section">
        <h2 className="faq-title">Частые вопросы</h2>
        <div className="faq-list">
          <FAQItem 
            question="Как работает скидка на мерч?" 
            answer="Сразу после оформления подписки Premium, цены в официальном магазине изменятся автоматически. Скидка 10% применяется ко всем товарам без исключения." 
          />
          <FAQItem 
            question="Можно ли отменить подписку в любой момент?" 
            answer="Да, вы можете отменить автопродление в любой момент в настройках профиля. Доступ к функциям сохранится до конца оплаченного периода." 
          />
          <FAQItem 
            question="Поддерживается ли 4K HDR?" 
            answer="Да, подписчики Premium получают доступ к максимальному качеству 4K HDR для всех поддерживаемых фильмов, без дополнительной платы." 
          />
        </div>
      </section>

      {isModalOpen && (
        <CancelSubscriptionModal 
          onClose={closeModal} 
          onConfirm={handleCancel} 
          isLoading={isLoading} 
          error={error} 
        />
      )}
    </div>
  );
}
