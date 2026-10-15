# Implantação da Notification API

## Ambiente público atual

- API: https://notification-api-il76.onrender.com
- Swagger: https://notification-api-il76.onrender.com/swagger-ui.html
- Canais: https://notification-api-il76.onrender.com/api/v1/channels

O ambiente público oferece cadastro, consulta, agendamento, cancelamento e histórico mediante autenticação administrativa. Sem credenciais configuradas no Render, essas funções ficam bloqueadas. O processamento de entrega está desligado por padrão (NOTIFICATION_DELIVERY_ENABLED=false): notificações podem permanecer PENDING ou SCHEDULED sem tentativas. Não é um serviço público de envio de e-mails.

O laboratório completo com RabbitMQ e SMTP de teste está documentado no [README](README.md). O repositório Art Pizza contém o laboratório integrado e um simulador local de pagamentos. Nenhum provedor externo de pagamentos está conectado.

## API e PostgreSQL no Render

1. Use um PostgreSQL ativo no mesmo workspace e região da API para acessar o hostname interno.
2. No serviço web Docker, configure DB_HOST (somente o hostname), DB_PORT (5432), DB_NAME, DB_USER e DB_PASSWORD com os valores atuais informados pelo banco. Mantenha NOTIFICATION_DELIVERY_ENABLED=false na demonstração pública.
3. A aplicação monta a URL JDBC a partir desses cinco campos. Uma DATABASE_URL isolada não substitui essa configuração. Não coloque a URL completa em DB_HOST e não publique credenciais no repositório.
4. Faça o deploy. O Flyway aplica as migrações; o Hibernate valida o esquema. O DataLoader cadastra os canais iniciais se a tabela estiver vazia.
5. Confirme o status Live, HTTP 200 em /api/v1/channels e /v3/api-docs e carregamento do Swagger. O serviço escuta na porta 8080. A instância gratuita pode levar alguns minutos para iniciar.

## Diagnóstico

- UnknownHostException: confira o hostname, a existência do banco e a região/workspace. Esse erro ocorre antes da autenticação no banco.
- Falha de autenticação: confira usuário, senha e nome do banco.
- Falha de migração: examine a causa SQL e preserve os dados. Não desative Flyway nem apague tabelas para contornar o erro.
- ClassNotFoundException/NoClassDefFoundError: confira as dependências do JAR publicado. O CI inicia o JAR com PostgreSQL temporário e consulta Swagger e canais para detectar falhas ausentes do classpath dos testes.

## Limites do ambiente gratuito

O banco criado em 15/10/2026 tem expiração informada pelo Render em **14/11/2026**. Ele começou vazio; os registros do banco antigo não foram recuperados. Planeje uma migração ou upgrade antes do vencimento, com orçamento aprovado.

Serviços web gratuitos podem suspender por inatividade. O Render também bloqueia saída SMTP nas portas 25, 465 e 587 desses serviços; habilitar apenas a variável de entrega não provisiona RabbitMQ nem um servidor SMTP. Consulte os [limites oficiais](https://render.com/docs/free) e as [regras de conexão PostgreSQL](https://render.com/docs/postgresql-creating-connecting).

## Painel e acesso

O painel em frontend/ é HTML/CSS/JS e pode ser servido localmente ou em hospedagem estática. Sua URL da API é configurável na interface; o padrão público aponta para o Render. Ele não é publicado automaticamente pelo workflow atual, e o link Swagger é a demonstração pública disponível.

A API exige HTTP Basic e distingue ADMIN de INTEGRATION. Configure NOTIFICATION_ADMIN_USERNAME/PASSWORD e NOTIFICATION_INTEGRATION_USERNAME/PASSWORD em Environment; senhas precisam de pelo menos 16 caracteres. Sem configuração, não há conta padrão. O Swagger e GET de canais permanecem públicos. O envio real continua desligado; antes de habilitá-lo, acrescente limites de uso e restrinja as origens permitidas. Não use dados pessoais na demonstração.

## Validação automática

O GitHub Actions executa os testes funcionais e de permissões, exige Docker para RabbitMQ, verifica a sintaxe do painel e inicia o JAR compilado com PostgreSQL descartável. Também testa a migração para outra base vazia. Essa validação não substitui conferir os segredos e a disponibilidade dos serviços no Render.
