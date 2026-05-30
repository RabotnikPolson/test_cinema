import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { getProducts, createProduct, createOrder, getMyOrders } from "../api/shopApi";

export const useProducts = () =>
  useQuery({
    queryKey: ["products"],
    queryFn: getProducts,
    staleTime: 1000 * 60 * 2,
  });

export const useCreateProduct = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: createProduct,
    onSuccess: () => qc.invalidateQueries({ queryKey: ["products"] }),
  });
};

export const useCreateOrder = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: createOrder,
    onSuccess: () => qc.invalidateQueries({ queryKey: ["products"] }),
  });
};

export const useMyOrders = () =>
  useQuery({
    queryKey: ["my-orders"],
    queryFn: getMyOrders,
    staleTime: 1000 * 60,
  });
