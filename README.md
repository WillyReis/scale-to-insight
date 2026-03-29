# Scale-to-Insight

Projeto final implementado em Java 21, com execucao local via Docker, para atendimento dos requisitos do enunciado.

## Entregaveis

- Repositorio com documentacao da arquitetura.
- Arquivo docker-compose.yml para subir o ambiente localmente.
- Relatorio tecnico com diagrama de atores e componentes.

## Como Executar

Pre-requisitos:

- Docker
- Docker Compose

Passos:

1. Subir ambiente:

   docker compose up -d --build

2. Validar gateway e servicos:

   curl http://localhost:8080/
   curl http://localhost:8080/orders/health
   curl http://localhost:8080/finance/health

3. Gerar evento de venda e consultar data mart:

   curl -X POST http://localhost:8080/orders/orders \
     -H "Content-Type: application/json" \
     -d '{"amount": 150.50, "payment_method": "pix", "status": "approved"}'

   curl http://localhost:8080/finance/kpis

4. Encerrar ambiente:

   docker compose down -v

## Atendimento ao Enunciado

1. Camada de servicos:
- Dois servicos conteinerizados.
- Proxy reverso Nginx como porta de entrada.
- Simulacao cloud com produtos Azure (Azurite).

2. Camada de dados:
- Fluxo Origem (App) -> Processamento -> Destino.
- Data Lake Raw para logs de acesso e vendas.
- Data Warehouse com Data Mart de Performance de Vendas.

3. Automacao:
- Pipeline CI/CD com GitHub Actions para build e deploy simulado.

## Arquivos-Chave

- docker-compose.yml
- .github/workflows/ci-cd.yml
- nginx/nginx.conf
- RELATORIO_TECNICO.md
- GUIA_APRENDIZADO_SISTEMA.md
