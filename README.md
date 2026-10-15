# Notification API — processamento confiável

[![CI](https://github.com/FernandoTorresD97/notification-api/actions/workflows/ci.yml/badge.svg)](https://github.com/FernandoTorresD97/notification-api/actions/workflows/ci.yml)

Projeto de portfólio de Fernando Torres, com Java 21, Spring Boot, PostgreSQL, RabbitMQ e SMTP de teste.

## Fluxo implementado

`API → PostgreSQL → scheduler → RabbitMQ → consumer → SMTP / Mailpit`

- Notificações imediatas e agendadas usam o mesmo fluxo.
- O envio SMTP produz um histórico persistente, consultável por notificação.
- Falhas temporárias geram retentativas com intervalo exponencial; o padrão é de três tentativas, com intervalos de 5 e 10 segundos.
- Ao esgotar as tentativas, a notificação fica FAILED e seu ID é publicado na fila `notifications.email.dead`.
- As filas são duráveis, as mensagens são persistentes e a publicação exige confirmação e roteamento pelo broker.
- Uma falha do RabbitMQ não remove a notificação do banco. O scheduler volta a publicá-la depois.
- Um lock de linha serializa entrega, cancelamento e exclusão de cada notificação.
- Uma mensagem repetida após o sucesso não gera outro envio. `Idempotency-Key` também permite evitar duplicação na criação.
- Flyway cria o esquema e migra uma base da versão anterior preservando as notificações; essa migração é testada.

Somente EMAIL tem envio implementado. SMS e PUSH são rejeitados e não são apresentados como entregues.

## Laboratório completo

Requer Docker Engine com Compose:

```bash
docker compose up --build
```

| Serviço | Endereço |
|---|---|
| Painel | http://localhost:3000 |
| Swagger | http://localhost:8080/swagger-ui.html |
| Caixa Mailpit | http://localhost:8025 |
| RabbitMQ Management | http://localhost:15672 |

O Mailpit captura as mensagens localmente, sem entregá-las à internet. Use endereços fictícios como `customer@example.invalid`. O RabbitMQ local usa `notification` / `local-notification-broker`; os valores podem ser alterados em `.env` a partir de `.env.example`. Os serviços publicados ficam restritos ao loopback. PostgreSQL e RabbitMQ usam volumes persistentes.

## Uso da API

### Autenticação

Swagger e consultas de canais são públicos. Criar, ler, cancelar e excluir notificações exige HTTP Basic sobre HTTPS (HTTP apenas no laboratório local). O painel mantém as credenciais somente na memória da aba, sem salvá-las em localStorage.

- `NOTIFICATION_ADMIN_USERNAME` e `NOTIFICATION_ADMIN_PASSWORD`: administração de notificações e canais.
- `NOTIFICATION_INTEGRATION_USERNAME` e `NOTIFICATION_INTEGRATION_PASSWORD`: conta separada da Art Pizza, autorizada somente a criar notificações e consultar canais. Não pode listar destinatários, cancelar, excluir ou administrar canais.
- Cada senha precisa ter pelo menos 16 caracteres. Use nomes distintos. Não há senha padrão: variáveis vazias deixam a conta inexistente e os endpoints protegidos continuam bloqueados.

Antes do Compose, copie `.env.example` para `.env` e preencha as senhas locais. No Render, configure as quatro variáveis em Environment antes de usar as funções protegidas; não coloque senhas no frontend ou no Git. No Swagger, use **Authorize**. A autenticação é stateless e não cria cookies de sessão.

Consulte `GET /api/v1/channels` para descobrir o ID de EMAIL. Depois crie uma notificação:

```json
{
  "title": "Pedido confirmado",
  "message": "Mensagem de teste",
  "recipient": "customer@example.invalid",
  "channelId": 1
}
```

Envie esse JSON por `POST /api/v1/notifications`. Opcionalmente inclua `Idempotency-Key: order-demo-123-confirmed`. Repetir a mesma chave e corpo retorna a notificação existente; outra combinação de dados com a mesma chave é rejeitada. Em uma corrida entre criações, a restrição única impede duplicação; repita um 409 com a mesma chave.

- `GET /api/v1/notifications/{id}`: estado, contagem e próxima tentativa.
- `GET /api/v1/notifications/{id}/attempts`: número, horário, resultado e código de erro das tentativas.
- `PATCH /api/v1/notifications/{id}/cancel`: impede tentativas futuras de uma notificação ainda não enviada.
- `GET /api/v1/notifications?status=RETRYING`: lista falhas temporárias.

`scheduledAt` é uma data/hora sem offset, na zona do processo. O Compose define UTC. SENT significa aceito pelo servidor SMTP, sem garantir leitura ou entrega final ao destinatário.

## Demonstrar falhas e recuperação

1. Execute `docker compose stop mailpit`.
2. Crie uma notificação e acompanhe RETRYING e `/attempts`.
3. Execute `docker compose start mailpit` antes de esgotar as tentativas.
4. Confira uma tentativa SMTP_ACCEPTED e a mensagem na caixa de teste.
5. Mantendo o SMTP indisponível, após três tentativas o estado será FAILED e o ID ficará na DLQ.

Os IDs da DLQ são inspecionáveis pelo RabbitMQ Management. Não há reenvio manual automático implementado. Ao interromper o RabbitMQ, notificações novas continuam no banco e voltam ao processamento após a recuperação do broker.

## Garantias e limites

O processamento é pelo menos uma vez. Uma queda depois da aceitação SMTP, mas antes do commit no banco, pode resultar em e-mail duplicado. A DLQ também pode receber um ID repetido após uma falha entre confirmação e persistência. Não há promessa de exatamente uma entrega ao provedor.

A transação mantém o lock por notificação durante a chamada SMTP; os timeouts de conexão, leitura e escrita são de cinco segundos. Essa escolha simplifica o laboratório e limita a concorrência; uma implantação de maior escala deve avaliar outro mecanismo de reserva de trabalho.

O histórico contém códigos genéricos de erro. Destinatário e conteúdo não são registrados nos logs de entrega. A API separa administração e integração por permissões. Uma API pública de envio real também exige limites de uso, política de retenção e gestão de segredos.

## Configuração e testes

```bash
mvn verify
mvn spring-boot:run
```

O processamento fica desligado por padrão (`NOTIFICATION_DELIVERY_ENABLED=false`). Habilite-o com PostgreSQL, RabbitMQ e SMTP de teste configurados. O Compose faz essa configuração para o laboratório.

Variáveis: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USER`, `RABBITMQ_PASSWORD`, `SMTP_HOST`, `SMTP_PORT`, `SMTP_FROM`, `NOTIFICATION_DELIVERY_ENABLED`.

As propriedades `notification.delivery.max-attempts`, `initial-delay-seconds` e `max-delay-seconds` controlam as retentativas. Flyway usa baseline 1 para a base anterior e aplica V2; faça backup de dados importantes antes de uma migração real.

Os testes usam PostgreSQL 16 temporário e SMTP GreenMail, sem precisar instalar PostgreSQL. Sem Docker, os testes de transporte RabbitMQ são explicitamente ignorados; o CI exige Docker e verifica que eles rodaram. O CI não provisiona hospedagem nem serviços pagos. O Compose completo precisa ser validado em um ambiente com Docker.

A Art Pizza possui uma outbox transacional e cliente HTTP autenticado para esta API. O laboratório integrado e sua verificação ponta a ponta estão no repositório Art Pizza, em `compose.integration.yaml` e `scripts/verify-integration.py`. A integração fica desligada por padrão fora do laboratório. A confirmação HTTP registra a notificação; o resultado SMTP é assíncrono e consultável no histórico.

## Migração do banco

O [plano de migração](docs/MIGRATION.md) prepara a troca do PostgreSQL gratuito do Render por outro PostgreSQL, como o plano gratuito do Neon. O script `scripts/migrate-postgres.py` faz backup, restaura em um destino vazio e compara contagens. Ele não provisiona contas nem muda o Render automaticamente. O CI testa a cópia entre duas bases descartáveis e a recusa de destino preenchido.
