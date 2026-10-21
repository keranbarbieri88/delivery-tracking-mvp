# 📋 Documento de Análise e Especificação de Requisitos — DeliveryTracker

Este documento descreve os requisitos funcionais, não-funcionais, regras de negócio e a máquina de estados implementada no **DeliveryTracker MVP**.

---

## 1. Requisitos Funcionais (RF)

| ID        | Nome                           | Descrição                                                                                                                                           | Perfis Autorizados |
| :-------- | :----------------------------- | :-------------------------------------------------------------------------------------------------------------------------------------------------- | :----------------- |
| **RF-01** | Autenticação e Registro        | Usuários podem se registrar e autenticar via e-mail e senha, recebendo um JWT com seus respectivos perfis.                                          | Público / Todos    |
| **RF-02** | Emissão de Pedido              | Clientes autenticados podem criar pedidos com múltiplos itens, endereço de entrega e cálculo server-side.                                           | `CLIENTE`          |
| **RF-03** | Visualização Isolada (Cliente) | O cliente visualiza exclusivamente o histórico dos seus próprios pedidos.                                                                           | `CLIENTE`          |
| **RF-04** | Visão Global de Pedidos        | Lojistas e administradores visualizam todos os pedidos da plataforma ordenados cronologicamente.                                                    | `LOJISTA`, `ADMIN` |
| **RF-05** | Máquina de Estados Operacional | O lojista pode avançar o ciclo de vida do pedido: `RECEBIDO` $\rightarrow$ `EM_PREPARO` $\rightarrow$ `SAIU_PARA_ENTREGA` $\rightarrow$ `ENTREGUE`. | `LOJISTA`, `ADMIN` |
| **RF-06** | Cancelamento Defensivo         | O cliente pode cancelar o pedido se, e somente se, o status estiver em `RECEBIDO`.                                                                  | `CLIENTE`          |

---

## 2. Requisitos Não-Funcionais (RNF)

- **RNF-01 (Segurança / RBAC):** Endpoints protegidos com Spring Security e validação de claims JWT em nível de método (`@PreAuthorize`).
- **RNF-02 (Arquitetura Backend):** Organização em _Vertical Slice Architecture_ (fatias verticais autônomas por domínio `auth` e `order`).
- **RNF-03 (Performance e Portabilidade):** Persistência híbrida em PostgreSQL 16 e perfil in-memory H2 autônomo para testes instantâneos.
- **RNF-04 (Confiabilidade e Testes):** Cobertura com suíte unitária (padrão AAA) e testes de integração E2E (`MockMvc`) cobrindo 100% dos fluxos críticos.
- **RNF-05 (Compatibilidade Retroativa):** Sanitização transparente de enums de status legados via JPA `AttributeConverter`.

---

## 3. Matriz da Máquina de Estados (State Machine)

| Status Atual        | Próximos Estados Permitidos  | Autorizado Por                                 | Código HTTP em Caso de Violação        |
| :------------------ | :--------------------------- | :--------------------------------------------- | :------------------------------------- |
| `RECEBIDO`          | `EM_PREPARO`, `CANCELADO`    | `LOJISTA` (preparo) / `CLIENTE` (cancelamento) | `403 Forbidden` ou `422 Unprocessable` |
| `EM_PREPARO`        | `SAIU_PARA_ENTREGA`          | `LOJISTA` / `ADMIN`                            | `422 Unprocessable Entity`             |
| `SAIU_PARA_ENTREGA` | `ENTREGUE`                   | `LOJISTA` / `ADMIN`                            | `422 Unprocessable Entity`             |
| `ENTREGUE`          | _(Nenhum - Estado Terminal)_ | —                                              | `422 Unprocessable Entity`             |
| `CANCELADO`         | _(Nenhum - Estado Terminal)_ | —                                              | `422 Unprocessable Entity`             |
