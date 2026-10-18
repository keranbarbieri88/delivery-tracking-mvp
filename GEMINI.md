#  Diretrizes de Desenvolvimento e Contexto do Projeto (GEMINI.md)

Este documento define os padrões técnicos, restrições arquiteturais e convenções obrigatórias para qualquer agente ou desenvolvedor que atue neste repositório.

---

##  1. Stack Tecnológica

### Back-end
- **Linguagem:** Java 21 (LTS)
- **Framework:** Spring Boot 3.x
- **Build Tool:** Maven (utilizar estritamente o wrapper `./mvnw`)
- **Segurança:** Spring Security 6 com autenticação Stateless via JWT (`io.jsonwebtoken:jjwt:0.12.x`) e hashing BCrypt
- **Banco de Dados:** PostgreSQL 16 (executado via `docker compose up -d`)
- **Persistência:** Spring Data JPA / Hibernate
- **Documentação:** SpringDoc OpenAPI 3 / Swagger UI (`/swagger-ui.html`, Diagramas C4)
- **Testes:** JUnit 5, Mockito, AssertJ

### Front-end
- **Linguagem:** TypeScript
- **Framework:** React com Vite (ou Next.js com App Router)
- **Estilização:** Tailwind CSS (Mobile-first, layout responsivo a partir de 360px)
- **Cliente HTTP:** Axios com interceptors para injeção do cabeçalho `Authorization: Bearer <token>`
- **Ícones:** Lucide React

---

##  2. Padrões de Arquitetura

### 2.1 Back-end: Vertical Slice Architecture
- O código NÃO deve ser estruturado em camadas horizontais puras (`controllers/`, `services/`, `repositories/` na raiz do pacote).
- Organize por funcionalidades/fatias verticais sob `com.delivery.api`:
  - `auth/`: Controller, Service, UserEntity, UserRepository, DTOs e regras de credenciais.
  - `order/`: Controller, Service, OrderEntity, OrderItemEntity, OrderStatus enum, OrderRepository, DTOs.
  - `shared/`: Configurações transversais (SecurityConfig, JwtFilter, GlobalExceptionHandler, DTOs base de erro).

### 2.2 Front-end: Feature-Driven Structure
- Organize os componentes por domínio dentro de `src/features/`:
  - `features/auth/`: formulários de login/registro, hooks e serviços de autenticação.
  - `features/orders/`: listagem de pedidos, cards responsivos, badges de status, modal de criação e serviços de pedidos.
  - `shared/`: componentes genéricos (botões, inputs, modal, navbar, axios client).

---

##  3. Regras de Negócio e Restrições de Domínio

1. **Status de Pedido:** Estritamente limitado aos 5 estados do enum `OrderStatus`:
   - `RECEBIDO`
   - `EM_PREPARO`
   - `SAIU_PARA_ENTREGA`
   - `ENTREGUE`
   - `CANCELADO`
2. **Cálculo de Totais:** O back-end deve calcular o valor subtotal de cada item (`quantidade * precoUnitario`) e o valor total do pedido. Nunca confie em somatórios calculados exclusivamente pelo front-end.
3. **Senhas:** Nenhuma senha deve ser persistida em texto plano. Usar `BCryptPasswordEncoder`.
4. **Tratamento de Erros:** Erros de validação (`@Valid`) e exceções de negócio devem ser interceptados por um `@RestControllerAdvice` e convertidos em um payload estruturado `ErrorResponse` com timestamp, status HTTP e lista de campos inválidos.

---

##  4. Padrões de Testes

- Todo teste unitário no back-end deve seguir explicitamente o padrão **AAA (Arrange, Act, Assert)**.
- Isolar dependências utilizando `@Mock` e `@InjectMocks` do Mockito.

---

##  5. Controle de Versão e Git

- Commits semânticos no padrão **Conventional Commits**:
  - `feat(<escopo>): nova funcionalidade`
  - `fix(<escopo>): correção de bug`
  - `test(<escopo>): inclusão ou ajuste de testes`
  - `docs(<escopo>): alterações em documentação/README`
  - `chore(<escopo>): alterações em build, dependências ou configs`

##  6. Regras de Negócio e Contratos da API

1. **Autenticação e Autorização (RF01, RF02, RF03):**
   - `POST /api/v1/auth/register` → Cria usuário com e-mail único e senha BCrypt (`201 Created` ou `409 Conflict` se e-mail já existir).
   - `POST /api/v1/auth/login` → Autentica credenciais e retorna JWT com expiração definida (`200 OK` ou `401 Unauthorized`).
   - Apenas `/api/v1/auth/**` e rotas do Swagger/OpenAPI são públicas; `/api/v1/pedidos/**` exige JWT válido (`401 Unauthorized` caso ausente/inválido).
2. **Gestão de Pedidos (RF04, RF05, RF06, RF07):**
   - `POST /api/v1/pedidos` → Registra pedido vinculado ao usuário/cliente, endereço de entrega textual e lista não vazia de itens (`nome`, `quantidade`, `precoUnitario`), calculando automaticamente subtotais e total.
   - `GET /api/v1/pedidos` → Lista todos os pedidos ordenados pelos mais recentes (`createdAt DESC`).
   - `GET /api/v1/pedidos/{id}` → Retorna detalhes completos do pedido ou `404 Not Found`.
   - `PATCH /api/v1/pedidos/{id}/status` → Atualiza o status estritamente dentro do enum `OrderStatus`: `RECEBIDO`, `EM_PREPARO`, `SAIU_PARA_ENTREGA`, `ENTREGUE`, `CANCELADO`.
3. **Padronização de Erros (Issue 2.4):**
   - `@RestControllerAdvice` retornando `ErrorResponse` (`timestamp`, `status`, `message`, `errors`).


## 7. Diretrizes de Geração de Código e Manutenção

1. **Eficiência de Tokens e Concisão:**
   - Evitar reescrever arquivos completos ao alterar apenas trechos específicos; aplicar edições cirúrgicas.
   - Não gerar comentários redundantes ou código morto.
2. **Padrões Back-end (Spring Boot):**
   - Utilizar `record` para DTOs com Bean Validation (`@NotBlank`, `@Email`, `@NotEmpty`, `@Valid`, `@Positive`).
2. **Padrões Back-end (Spring Boot, Java 21 & Lombok `1.18.36`):**
   - **Dependência Lombok:** Declarar explicitamente na versão `1.18.36` com `<scope>provided</scope>` (compatibilidade total com Java 21 e *Flexible Constructor Bodies* no Language Server).
   - **DTOs de Entrada e Saída (Requests e Responses):** Utilizar preferencialmente **Java Records nativos** (imutabilidade, construtor canônico e acessores limpos, sem `@Getter`, `@AllArgsConstructor` ou `@Data`), aplicando Bean Validation (`@NotBlank`, `@Email`, `@NotEmpty`, `@Valid`, `@Positive`, `@Size`) diretamente nos componentes do `record`.
   - **Controllers e Services:** Utilizar `@RequiredArgsConstructor` para assegurar injeção de dependências por construtor sobre campos `private final`.
   - **Entidades JPA/Hibernate:** **Proibido o uso de `@Data`**. Manter uso granular e defensivo de `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor` e `@Builder`, evitando loops de recursão em `toString()` ou quebras de `equals`/`hashCode` em coleções de entidades persistidas.
   - Manter regras de negócio e cálculos (totais de pedidos, transições de status) encapsulados nas classes `*Service` e entidades de domínio.
3. **Padrões Front-end (React + Redux Toolkit):**
   - Utilizar `createSlice` com reducers diretos para estado local/sessão e `createAsyncThunk` apenas quando estritamente necessário.
   - Preferir `createApi` (RTK Query) com tags de invalidação (`Orders`) ou interceptor centralizado para evitar boilerplate manual de loading/error.
   - Centralizar `configureStore` exportando `RootState`, `AppDispatch`, `useAppDispatch` e `useAppSelector`.
4. **Fluxo de Trabalho por Issue:**
   - Executar uma Issue por vez seguindo a ordem das Milestones (`M1` → `M2` → `M3` → `M4`).
   - Validar compilação, execução do fluxo e testes unitários essenciais (padrão AAA) antes de avançar para a próxima Issue.

---

## 8. Roadmap de Milestones e Issues

- **M1 — Fundação do Projeto e Autenticação (Core & Security)**
  - `Issue 1.1`: `chore(setup): inicialização do repositório e configurações base`
  - `Issue 1.2`: `feat(auth): implementação de cadastro de usuários e persistência de credenciais`
  - `Issue 1.3`: `feat(auth): autenticação com JWT e proteção de rotas`
- **M2 — Módulo de Pedidos no Back-end (Spring Boot REST API)**
  - `Issue 2.1`: `feat(order): modelagem de entidades e ciclo de vida de status`
  - `Issue 2.2`: `feat(order): endpoints para criação, listagem e busca por id`
  - `Issue 2.3`: `feat(order): endpoint de atualização de status do pedido`
  - `Issue 2.4`: `feat(common): tratamento global de erros da API`
- **M3 — Interface Responsiva de Pedidos (React SPA)**
  - `Issue 3.1`: `feat(front-auth): setup do cliente React e telas de login/cadastro`
  - `Issue 3.2`: `feat(front-orders): tela responsiva de listagem de pedidos`
  - `Issue 3.3`: `feat(front-orders): formulário de criação de novo pedido`
- **M4 — Polimento, Qualidade e Documentação (Final Delivery)**
  - `Issue 4.1`: `test(backend): testes unitários dos fluxos críticos`
  - `Issue 4.2`: `docs: documentação da API e guia de execução no README`

