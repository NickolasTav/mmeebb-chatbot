CREATE TABLE IF NOT EXISTS system_configurations (
    config_key VARCHAR(100) PRIMARY KEY,
    config_value TEXT NOT NULL,
    description VARCHAR(255),
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

INSERT INTO system_configurations (config_key, config_value, description)
VALUES
    ('uazapi.base-url', 'https://free.uazapi.com', 'URL base do gateway Uazapi'),
    ('uazapi.instance', '', 'Nome da instância ativa na Uazapi'),
    ('uazapi.api-key', '', 'Chave de autenticação da instância'),
    ('ngrok.custom-domain', '', 'Domínio estático opcional do Ngrok (ex: meu-bot.ngrok-free.app)'),
    ('webhook.last-synced-url', '', 'Última URL de webhook configurada na Uazapi')
ON CONFLICT (config_key) DO NOTHING;
