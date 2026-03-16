# Documentação Arquitetural – Scale-to-Insight

## 1. Contexto e Problema

Uma startup de e-commerce em crescimento acelerado necessita migrar seu monolito para uma arquitetura moderna que suporte:

- **Picos de tráfego** sem degradação de performance
- **Inteligência de negócio em tempo real** para o setor financeiro
- **Observabilidade** e **rastreabilidade** de toda a cadeia de dados

---

## 2. Diagrama de Atores e Componentes

```
┌──────────────────────────────────────────────────────────────────────────┐
│                              ATORES EXTERNOS                             │
│                                                                          │
│  ┌──────────┐   ┌──────────┐   ┌──────────────┐   ┌──────────────────┐  │
│  │  Cliente │   │  Mobile  │   │  Marketplace │   │  Analista /BI    │  │
│  │  Web     │   │  App     │   │  Partner     │   │  Dashboard       │  │
│  └────┬─────┘   └────┬─────┘   └──────┬───────┘   └────────┬─────────┘  │
└───────┼──────────────┼────────────────┼────────────────────┼────────────┘
        │              │                │                     │
        └──────────────┴────────────────┘                     │
                       │ HTTP :80                             │ HTTP :80
        ┌──────────────▼──────────────────────────────────────▼────────────┐
        │                      NGINX (Reverse Proxy)                        │
        │          Roteamento por prefixo de URL + Load Balancing           │
        └──────┬──────────────────────┬──────────────────────┬─────────────┘
               │ /api/orders          │ /api/products        │ /api/analytics
               │                      │                      │
   ┌───────────▼──────┐  ┌────────────▼──────┐  ┌───────────▼────────────┐
   │  order-service   │  │  product-service  │  │   analytics-service    │
   │  Java 21 / SB3   │  │  Java 21 / SB3   │  │   Java 21 / SB3        │
   │                  │  │                   │  │                         │
   │  ┌────────────┐  │  │  ┌─────────────┐ │  │  ┌───────────────────┐  │
   │  │ Controller │  │  │  │ Controller  │ │  │  │   Controller      │  │
   │  │ Service    │  │  │  │ Service     │ │  │  │   AnalyticsService│  │
   │  │ Repository │  │  │  │ Repository  │ │  │  │   SalesEtlService │  │
   │  └────────────┘  │  │  └─────────────┘ │  │  │   DataLakeService │  │
   └──────┬───────────┘  └───────┬───────────┘  │  └───────────────────┘  │
          │                      │               └───────────┬─────────────┘
          │ Kafka Produce         │ Kafka Produce             │ Kafka Consume
          │ order.created         │ product.created           │
          │                      │                            │
          └──────────────────────▼────────────────────────────┘
                                 │
                    ┌────────────▼────────────┐
                    │      Apache Kafka        │
                    │   Topics:                │
                    │   • order.created        │
                    │   • order.updated        │
                    │   • product.created      │
                    │   • product.updated      │
                    └────────────┬────────────┘
                                 │
               ┌─────────────────┼──────────────────┐
               │                 │                   │
   ┌───────────▼────────┐        │        ┌──────────▼──────────────┐
   │  PostgreSQL          │        │        │  LocalStack (S3)         │
   │                     │        │        │  Bucket: sti-data-lake  │
   │  ┌─────────────┐    │        │        │                         │
   │  │  orders_db  │    │        │        │  Zonas:                 │
   │  │  (OLTP)     │    │        │        │  raw/access-logs/       │
   │  └─────────────┘    │        │        │  raw/sales-events/      │
   │  ┌─────────────┐    │        │        │  raw/product-events/    │
   │  │ products_db │    │        │        │  processed/             │
   │  │  (OLTP)     │    │        │        │  curated/               │
   │  └─────────────┘    │        │        └─────────────────────────┘
   │  ┌─────────────┐    │        │
   │  │  dwh_db     │◄───┘        │ (analytics-service escreve no DWH
   │  │  (DWH/OLAP) │             │  e no Data Lake simultaneamente)
   │  └─────────────┘    │
   └─────────────────────┘
```

---

## 3. Decisões Arquiteturais

### 3.1 Migração do Monolito para Microsserviços

**Problema:** O monolito apresentava acoplamento forte, dificultando escalabilidade independente dos módulos de pedidos, produtos e analytics.

**Solução:** Decomposição por domínio de negócio (Domain-Driven Design):
- `order-service`: agregado de **Pedidos**
- `product-service`: agregado de **Produtos/Catálogo**
- `analytics-service`: agregado de **Dados Analíticos**

**Trade-offs:**
- ✅ Escalabilidade independente
- ✅ Deploy independente
- ⚠️ Complexidade operacional aumentada (mitigada com Docker Compose)

### 3.2 Comunicação Assíncrona via Kafka

**Problema:** Integração síncrona entre serviços criaria dependência direta e ponto único de falha.

**Solução:** Kafka como barramento de eventos. O `order-service` publica eventos `order.created`; o `analytics-service` os consome de forma assíncrona para alimentar o DWH.

**Trade-offs:**
- ✅ Desacoplamento temporal
- ✅ Resiliência a falhas downstream
- ✅ Replay de eventos para reprocessamento
- ⚠️ Eventual consistency (mitigável com compensações)

### 3.3 Data Lake em Camadas (Medallion Architecture)

| Camada | Zona S3 | Descrição |
|---|---|---|
| **Bronze (Raw)** | `raw/` | Dados brutos, imutáveis, conforme chegam |
| **Silver (Processed)** | `processed/` | Dados limpos, sem duplicatas |
| **Gold (Curated)** | `curated/` | Dados agregados, prontos para consumo |

### 3.4 Data Warehouse – Schema Estrela

Escolhemos o **modelo estrela** (Star Schema) pela:
- Simplicidade de queries analíticas
- Melhor performance com índices nas FKs
- Compatibilidade com ferramentas de BI

**Tabelas de Dimensão:**
- `dim_date` – Dimensão temporal (SCD Tipo 1)
- `dim_product` – Catálogo de produtos (SCD Tipo 2 – suporta histórico)
- `dim_customer` – Base de clientes (SCD Tipo 2)
- `dim_channel` – Canal de venda (estático)

**Tabelas Fato:**
- `fact_sales` – Granularidade de item de pedido
- `fact_financial_health` – Granularidade diária por canal

### 3.5 Nginx como Reverse Proxy

Centraliza:
- Roteamento por prefixo de URL
- Terminação TLS (a ser configurado em produção)
- Logs de acesso para envio ao Data Lake

### 3.6 LocalStack para Emulação de AWS

Permite desenvolvimento e testes locais sem custo, com API S3 compatível com AWS SDK v2. Em produção, basta trocar o endpoint para o S3 real.

---

## 4. Fluxo de Dados Detalhado

```
1. [Cliente] ──HTTP──► [Nginx] ──proxy──► [order-service]
2. [order-service] ──saves──► [orders_db (PostgreSQL)]
3. [order-service] ──publishes──► [Kafka: order.created]
4. [analytics-service] ──consumes──► [Kafka: order.created]
5. [analytics-service] ──ETL──► [fact_sales (dwh_db)]
6. [analytics-service] ──archives──► [S3: raw/sales-events/]
7. [Cliente BI] ──HTTP──► [analytics-service] ──queries──► [fact_sales]
```

---

## 5. Pipeline CI/CD

```
┌─────────────────────────────────────────────────────────┐
│                   GitHub Actions                         │
│                                                         │
│  on: push (main/develop) | pull_request (main)          │
│                                                         │
│  ┌─────────────────────────────────────────────────┐    │
│  │ Jobs Paralelos (Build & Test)                   │    │
│  │  • build-order-service   (mvn test + package)   │    │
│  │  • build-product-service (mvn test + package)   │    │
│  │  • build-analytics-service (mvn test + package) │    │
│  └─────────────────────┬───────────────────────────┘    │
│                        │                                │
│           ┌────────────┴────────────┐                   │
│           │                         │                   │
│  ┌────────▼────────┐   ┌────────────▼────────┐         │
│  │ docker-publish  │   │ integration-test     │         │
│  │ (push to GHCR)  │   │ (PR smoke tests)     │         │
│  │ [push events]   │   │ [pull_request]        │         │
│  └─────────────────┘   └──────────────────────┘         │
└─────────────────────────────────────────────────────────┘
```

---

## 6. Requisitos Não-Funcionais

| Requisito | Abordagem |
|---|---|
| **Disponibilidade** | `restart: unless-stopped` em todos os containers |
| **Observabilidade** | Spring Boot Actuator + health checks Docker |
| **Rastreabilidade** | Logs estruturados com SLF4J + Kafka event log |
| **Segurança** | Credenciais via variáveis de ambiente (não hardcoded) |
| **Idempotência** | `ON CONFLICT DO NOTHING` nas seeds do DWH |

---

## 7. Próximos Passos (Evolução)

- [ ] Adicionar **Prometheus + Grafana** para métricas
- [ ] Implementar **distributed tracing** com OpenTelemetry
- [ ] Adicionar **autenticação JWT** via API Gateway
- [ ] Enriquecer ETL com dimensões de produto e cliente reais
- [ ] Implementar **Schema Registry** para controle de contratos Kafka
- [ ] Adicionar **dbt** para transformações Gold layer do Data Lake
