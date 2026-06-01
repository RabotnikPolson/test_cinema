const MOCK_PLANS = [
  {
    id: 'basic',
    name: 'Базовый',
    price: 0,
    period: 'мес',
    features: [
      { id: 'f1', text: 'Доступ к каталогу', included: true },
      { id: 'f2', text: 'Стандартное качество', included: true },
      { id: 'f3', text: 'Без рекламы', included: false },
      { id: 'f4', text: 'Оффлайн просмотр', included: false },
    ],
    isPremium: false,
  },
  {
    id: 'insight_plus',
    name: 'Insight+',
    price: 1990,
    period: 'мес',
    features: [
      { id: 'f1', text: 'Полный каталог', included: true },
      { id: 'f2', text: '4K HDR качество', included: true },
      { id: 'f3', text: 'Без рекламы', included: true },
      { id: 'f4', text: 'Оффлайн просмотр', included: true },
    ],
    isPremium: true,
  }
];

const delay = (ms) => new Promise(res => setTimeout(res, ms));

// Mock user subscription state in memory for demo purposes
let currentUserSubscription = null;

export const subscriptionApi = {
  getSubscriptionPlans: async () => {
    await delay(500);
    return { data: MOCK_PLANS };
  },
  
  getMySubscription: async () => {
    await delay(400);
    return { data: currentUserSubscription };
  },

  createSubscription: async (planId) => {
    await delay(800);
    const plan = MOCK_PLANS.find(p => p.id === planId);
    if (!plan) throw new Error("Plan not found");
    
    const endDate = new Date();
    endDate.setMonth(endDate.getMonth() + 1);

    currentUserSubscription = {
      id: `sub_${Math.random().toString(36).substring(7)}`,
      planId: plan.id,
      planName: plan.name,
      status: 'ACTIVE',
      endDate: endDate.toISOString(),
      price: plan.price
    };
    return { data: currentUserSubscription };
  },

  cancelSubscription: async () => {
    await delay(600);
    if (currentUserSubscription) {
      currentUserSubscription.status = 'CANCELLED';
    }
    return { data: currentUserSubscription };
  }
};
