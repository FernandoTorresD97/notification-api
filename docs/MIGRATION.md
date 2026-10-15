# Migração preparada: Render PostgreSQL → outro PostgreSQL

O banco atual vence em **14/11/2026**. O destino sugerido é um projeto no plano gratuito do Neon, com PostgreSQL 16 ou mais recente. Isso não garante recursos ilimitados: confira os limites e o plano no momento da criação. Nenhuma conta, contratação ou migração real foi feita por este roteiro.

Fontes oficiais: [migração Neon](https://neon.com/migration), [pg_dump e pg_restore com conexão direta](https://neon.com/docs/manage/backup-pg-dump).

## Preparação

1. Crie ou selecione um projeto gratuito e uma base vazia. Não conecte a API ainda; o Flyway criaria tabelas e o script recusa destinos preenchidos.
2. Copie a conexão **direta**, sem `-pooler`, do novo PostgreSQL. Use TLS (`sslmode=require` ou verificação de certificado mais forte).
3. Instale as ferramentas oficiais PostgreSQL 16+ (`psql`, `pg_dump`, `pg_restore`). A versão do cliente deve ser compatível com a origem e o destino. O CI usa cliente PostgreSQL 16 para bases PostgreSQL 16.
4. Pause as escritas: suspenda temporariamente a API e qualquer worker que grave na origem. Um dump é consistente, mas novas escritas depois dele exigiriam outra cópia.
5. Configure `SOURCE_DATABASE_URL` com a conexão externa do Render e `TARGET_DATABASE_URL` com a conexão direta do destino, somente no ambiente do terminal. Nunca salve essas URLs no Git, na conversa ou em prints.

## Cópia e validação

Defina `MIGRATION_WRITES_PAUSED=true` após pausar as escritas e execute:

```text
python scripts/migrate-postgres.py
```

O script confere os esquemas, recusa um destino com tabelas, salva backup em `backups/`, restaura numa transação e compara contagens de todas as tabelas públicas. Preserva sequências, índices, restrições e histórico Flyway por meio do arquivo pg_dump. O diretório de backup é ignorado pelo Git; ele contém dados privados e deve ser protegido no computador. Ele não apaga tabelas nem altera a origem. Se a cópia falhar, mantenha o Render na configuração original e examine o problema antes de retomar.

Contagens são uma verificação de integridade operacional, não uma comparação criptográfica de cada linha. Depois da cópia, consulte registros e teste criação, leitura e cancelamento no destino, usando dados fictícios.

## Troca do Render

Com a cópia verificada, configure a API no Render com `SPRING_DATASOURCE_URL=jdbc:postgresql://<host-direto>/<banco>?sslmode=require`, `DB_USER` e `DB_PASSWORD` do destino. A variável padrão Spring substitui a URL montada a partir de DB_HOST. Nunca inclua senha na URL JDBC. O pool padrão foi reduzido para cinco conexões, sem mínimo ocioso.

Faça o deploy e verifique: status Live, `/api/v1/channels`, Swagger, 401 na listagem anônima e operações autenticadas. O Flyway deve encontrar o histórico migrado sem recriar o esquema. Só retome as escritas após validar o destino.

Mantenha o banco antigo e o backup até concluir a verificação. Para reverter **antes de novas escritas no destino**, restaure as variáveis do Render e publique novamente. Depois de novas escritas, não basta apontar para a origem antiga: seria preciso reconciliar os dados. Nenhuma exclusão da origem está incluída neste procedimento.
