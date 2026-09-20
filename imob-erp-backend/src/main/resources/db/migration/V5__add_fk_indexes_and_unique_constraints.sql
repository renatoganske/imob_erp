-- IMOB-52: o PostgreSQL nao indexa chaves estrangeiras automaticamente; alem disso faltavam unicidades do dominio.

-- Unicidades: se ja houver duplicatas, a migration falha ANTES de alterar qualquer coisa, com mensagem acionavel
-- (nao ha como decidir sozinho qual usuario/comissao duplicada descartar).
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM users GROUP BY tenant_id, lower(email) HAVING count(*) > 1) THEN
        RAISE EXCEPTION 'IMOB-52: ha usuarios com o mesmo e-mail no mesmo tenant. Consulte: SELECT tenant_id, lower(email), count(*) FROM users GROUP BY 1, 2 HAVING count(*) > 1; resolva as duplicatas e rode a migration de novo';
    END IF;
    IF EXISTS (SELECT 1 FROM commissions GROUP BY contract_id, agent_id HAVING count(*) > 1) THEN
        RAISE EXCEPTION 'IMOB-52: ha mais de uma comissao para o mesmo contrato e corretor. Consulte: SELECT contract_id, agent_id, count(*) FROM commissions GROUP BY 1, 2 HAVING count(*) > 1; resolva as duplicatas e rode a migration de novo';
    END IF;
END $$;

CREATE UNIQUE INDEX uq_users_tenant_email ON users (tenant_id, lower(email));
CREATE UNIQUE INDEX uq_commissions_contract_agent ON commissions (contract_id, agent_id);

-- Indices em chaves estrangeiras usadas em joins/listagens.
CREATE INDEX idx_financial_entries_contract_id ON financial_entries (contract_id);
CREATE INDEX idx_contracts_property_id ON contracts (property_id);
CREATE INDEX idx_contracts_agent_id ON contracts (agent_id);
CREATE INDEX idx_contracts_lead_id ON contracts (lead_id);
CREATE INDEX idx_visits_lead_id ON visits (lead_id);
CREATE INDEX idx_visits_property_id ON visits (property_id);
CREATE INDEX idx_visits_agent_id ON visits (agent_id);
CREATE INDEX idx_lead_property_property_id ON lead_property (property_id);
CREATE INDEX idx_email_outbox_tenant_id ON email_outbox (tenant_id);

-- Job de atrasados (IMOB-27): WHERE tenant_id = ? AND status = 'PENDENTE' AND due_date < ?
CREATE INDEX idx_financial_entries_tenant_status_due ON financial_entries (tenant_id, status, due_date);
