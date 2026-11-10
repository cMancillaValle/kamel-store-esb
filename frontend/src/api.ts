import type {Product,Order} from './types';
const base=import.meta.env.VITE_API_URL || '';
async function request<T>(path:string,options?:RequestInit):Promise<T>{const response=await fetch(`${base}/api${path}`,options);const data=await response.json();if(!response.ok)throw new Error(data.message || 'No se pudo conectar con la tienda');return data as T;}
export const api={products:()=>request<Product[]>('/products'),orders:()=>request<Order[]>('/orders'),create:(productId:number,quantity:number,orderId:string)=>request<Order>('/orders',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({productId,quantity,orderId})}),retry:(id:string)=>request<Order>(`/orders/${id}/retry`,{method:'POST'})};
