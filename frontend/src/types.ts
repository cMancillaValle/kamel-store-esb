export type Product={id:number;name:string;description:string;price:number;stock:number;image:string};
export type Order={id:string;productId:number;quantity:number;status:string;productName:string|null;total:number|null;invoiceId:string|null;message:string;createdAt:string};
