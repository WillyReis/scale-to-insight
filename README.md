# Scale-to-Insight 🚀

> **Projeto Final – Arquitetura de Serviços e Dados**
> Ecossistema de dados para uma startup de e-commerce que migra de arquitetura monolítica para microsserviços com inteligência de negócios em tempo real.

---

## 📐 Visão Geral da Arquitetura

```
                          ┌─────────────────────────────────────────────┐
                          │              CLIENTE (HTTP)                  │
                          └────────────────────┬────────────────────────┘
                                               │ :80
                          ┌────────────────────▼────────────────────────┐
                          │           NGINX (Reverse Proxy)             │
                          │    /api/orders  /api/products  /api/analytics│
                          └──────┬───────────────┬─────────────┬────────┘
                                 │               │             │
                    ┌────────────▼──┐  ┌─────────▼──┐  ┌──────▼──────────┐
                    │ order-service │  │product-svc  │  │analytics-service│
                    │  :8081 (Java) │  │ :8082 (Java)│  │  :8083 (Java)   │
                    └──────┬────────┘  └──────┬──────┘  └──────┬──────────┘
                           │                  │                 │
                    ┌──────▼──────────────────▼─────┐          │
                    │         Apache Kafka           │──────────┘
                    │  (order.created, product.*)    │ (consume events)
                    └───────────────────────────────┘
                           │                  │
              ┌────────────▼──┐    ┌──────────▼──────────┐
              │  PostgreSQL    │    │    LocalStack (S3)   │
              │  orders_db     │    │    sti-data-lake     │
              │  products_db   │    │  raw/ processed/     │
              │  dwh_db (DWH)  │    │     curated/         │
              └───────────────┘    └─────────────────────┘
```

---

## 🏗️ Componentes

| Componente | Tecnologia | Porta | Responsabilidade |
|---|---|---|---|
| **nginx** | Nginx 1.25 | 80 | Reverse proxy, roteamento |
| **order-service** | Java 21 / Spring Boot 3 | 8081 | CRUD de pedidos, publicação de eventos |
| **product-service** | Java 21 / Spring Boot 3 | 8082 | Catálogo de produtos, controle de estoque |
| **analytics-service** | Java 21 / Spring Boot 3 | 8083 | ETL, Data Mart, API analítica |
| **postgres** | PostgreSQL 16 | 5432 | Banco relacional (3 databases) |
| **kafka** | Confluent Kafka 7.6 | 9092 | Mensageria / streaming de eventos |
| **zookeeper** | Confluent ZooKeeper 7.6 | 2181 | Coordenação do Kafka |
| **localstack** | LocalStack 3.4 | 4566 | Emulação de AWS S3 (Data Lake) |

---

## 🗂️ Estrutura do Projeto

```
scale-to-insight/
├── .github/
│   └── workflows/
│       └── ci-cd.yml              # Pipeline CI/CD (GitHub Actions)
├── nginx/
│   └── nginx.conf                 # Configuração do Reverse Proxy
├── localstack/
│   └── init-aws.sh                # Script de criação dos buckets S3
├── data-warehouse/
│   └── scripts/
│       ├── init-multiple-dbs.sh   # Cria múltiplas bases no PostgreSQL
│       └── init-dwh.sql           # Schema estrela do Data Warehouse
├── services/
│   ├── order-service/             # Microsserviço de pedidos
│   │   ├── Dockerfile
│   │   ├── pom.xml
│   │   └── src/
│   ├── product-service/           # Microsserviço de produtos
│   │   ├── Dockerfile
│   │   ├── pom.xml
│   │   └── src/
│   └── analytics-service/         # Microsserviço de analytics / DWH
│       ├── Dockerfile
│       ├── pom.xml
│       └── src/
├── docs/
│   └── architecture.md            # Documentação arquitetural detalhada
└── docker-compose.yml             # Orquestração local completa
```

---

## 🛢️ Camada de Dados

### Data Lake (S3 via LocalStack)

| Zona | Prefixo S3 | Conteúdo |
|---|---|---|
| **Raw / Landing** | `raw/access-logs/` | Logs de acesso do Nginx |
| **Raw / Landing** | `raw/sales-events/` | Eventos `order.created` em JSON |
| **Raw / Landing** | `raw/product-events/` | Eventos `product.created` em JSON |
| **Processed** | `processed/` | Dados limpos e padronizados |
| **Curated** | `curated/` | Dados prontos para consumo analítico |

### Data Warehouse – Schema Estrela (PostgreSQL `dwh_db`)

```
              ┌──────────────┐
              │  dim_date    │
              └──────┬───────┘
                     │
┌──────────────┐     │     ┌────────────────┐
│  dim_product │─────┼─────│  fact_sales    │───── fact_financial_health
└──────────────┘     │     └────────────────┘
                     │
┌──────────────┐     │     ┌──────────────┐
│ dim_customer │─────┘     │ dim_channel  │
└──────────────┘           └──────────────┘
```

**Data Mart: Performance de Vendas** (`fact_sales`)
- `gross_revenue`: quantidade × preço unitário
- `net_revenue`: receita bruta − desconto
- `gross_profit`: receita líquida − custo dos produtos

**Data Mart: Saúde Financeira** (`fact_financial_health`)
- Agregação diária por canal de venda
- `profit_margin`: margem de lucro calculada

---

## 🔄 Fluxo de Dados

```
[order-service] ──► Kafka: order.created ──► [analytics-service ETL]
                                                     │
                                          ┌──────────▼───────────┐
                                          │  fact_sales (DWH)    │
                                          └──────────────────────┘
                                                     │
                                          ┌──────────▼───────────┐
                                          │  S3 Data Lake (raw/) │
                                          └──────────────────────┘
```

---

## 🚀 Como Executar Localmente

### Pré-requisitos

- Docker >= 24.x
- Docker Compose >= 2.x
- Java 21 (para build local)
- Maven 3.9+

### 1. Build dos serviços Java

```bash
# Build de todos os serviços
for svc in order-service product-service analytics-service; do
  cd services/$svc && mvn -B package -DskipTests && cd ../..
done
```

### 2. Subir o ambiente completo

```bash
docker compose up --build -d
```

### 3. Verificar status

```bash
docker compose ps
```

### 4. Testar os endpoints

```bash
# Health do proxy
curl http://localhost/health

# Criar um produto
curl -X POST http://localhost/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Notebook Pro","category":"Electronics","unitPrice":4999.99,"stockQuantity":50}'

# Criar um pedido
curl -X POST http://localhost/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-001",
    "channel": "WEB",
    "items": [{"productId":"PROD-001","productName":"Notebook Pro","quantity":1,"unitPrice":4999.99}]
  }'

# Consultar resumo de vendas
curl http://localhost/api/analytics/sales/summary
```

### 5. Parar o ambiente

```bash
docker compose down -v
```

---

## ⚙️ CI/CD Pipeline (GitHub Actions)

O arquivo `.github/workflows/ci-cd.yml` define o seguinte fluxo:

```
Push / PR
    │
    ├── build-order-service    (mvn test + package)
    ├── build-product-service  (mvn test + package)
    └── build-analytics-service (mvn test + package)
          │
          ├── [push to main/develop] ──► docker-publish (GHCR)
          └── [pull request]         ──► integration-test (smoke test)
```

As imagens Docker são publicadas no **GitHub Container Registry (ghcr.io)** com tags por branch e SHA do commit.

---

## 📋 Decisões Arquiteturais

| Decisão | Escolha | Justificativa |
|---|---|---|
| Runtime | Java 21 + Spring Boot 3.2 | LTS, Virtual Threads, ecossistema maduro |
| Mensageria | Apache Kafka | Alta throughput, replay de eventos, desacoplamento |
| Banco transacional | PostgreSQL 16 | ACID, JSON nativo, suporte a geração de colunas |
| Migração de schema | Flyway | Versionamento declarativo do schema |
| Data Lake | S3 (LocalStack) | Padrão de mercado, custo-efetivo, escalável |
| Proxy | Nginx | Performance, configuração simples, amplamente usado |
| CI/CD | GitHub Actions | Integrado ao repositório, sem custo adicional |

---

## 👥 Grupo

> Este projeto foi desenvolvido como trabalho final do curso de **Arquitetura de Serviços e Dados**.
> Mínimo de 3 integrantes exigido.

**Data de Entrega:** 01 de Abril de 2026

---

## 📄 Licença

MIT
