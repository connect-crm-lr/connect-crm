-- V2: carga minima para o sistema funcionar (usuario inicial + tags de exemplo).
-- Dados de teste (clientes, tecidos, pedidos) ficam fora desta migration.

-- Senha inicial "Trocar@123", gravada so como hash BCrypt (custo 10).
-- PENDENTE: definir a senha real deste usuario antes de o sistema entrar em uso.
INSERT INTO usuario (nome, email, senha_hash) VALUES
    ('Revendedor LR', 'revendedor@lrrepresentacoes.com.br', '$2a$10$l5u6xmA6w/vcNgrtB5GYdOlXBRyNH6PLvZoyuevKisPa.N6bMASKy');

INSERT INTO tag (nome) VALUES
    ('Moda Festa'),
    ('Linho'),
    ('Estofaria'),
    ('Malha'),
    ('Denim');
