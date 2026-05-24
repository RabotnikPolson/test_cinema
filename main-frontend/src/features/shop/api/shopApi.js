import http from "@/shared/api/http-client";

export const getProducts = () => http.get("/shop/products").then((r) => r.data);

export const createProduct = (data) => http.post("/shop/products", data).then((r) => r.data);

export const createOrder = (productId) => http.post("/shop/orders", { productId }).then((r) => r.data);

export const getMyOrders = () => http.get("/shop/orders/my").then((r) => r.data);
