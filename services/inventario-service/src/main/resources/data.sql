INSERT INTO products (id,name,description,price,stock,image)
SELECT 1,'Bolso Ruta','Bolso ligero para acompañar cada recorrido.',89000,12,'bolso' WHERE NOT EXISTS (SELECT 1 FROM products WHERE id=1);
INSERT INTO products (id,name,description,price,stock,image)
SELECT 2,'Taza Oasis','Cerámica cálida para una pausa en el camino.',38000,18,'taza' WHERE NOT EXISTS (SELECT 1 FROM products WHERE id=2);
INSERT INTO products (id,name,description,price,stock,image)
SELECT 3,'Cuaderno Duna','Páginas para ideas y nuevos destinos.',29000,24,'cuaderno' WHERE NOT EXISTS (SELECT 1 FROM products WHERE id=3);
