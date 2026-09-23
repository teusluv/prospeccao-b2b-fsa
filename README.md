# ProspectRadar — Prospecção B2B FSA

Sistema de prospecção B2B: empresas (leads), contatos, interações com follow-up, funil de vendas, painel e
**busca automática de empresas sem site no Google Maps**.

- `backend/` — API em **Java 21 + Spring Boot 3.5** (PostgreSQL/Supabase)
- `frontend/` — site em **React + TypeScript (Vite)**

## Rodando o site (frontend)

Pré-requisito: Node.js 20+ e o backend rodando em `localhost:8080`.

```bash
cd frontend
npm install
npm run dev
```

Abra http://localhost:5173 e entre com o usuário do backend (inicial: `admin@fsa.com.br` / `admin123`).
Em desenvolvimento o Vite repassa `/api` para `localhost:8080`, então não há nada para configurar.

### Publicando na Vercel

1. No projeto da Vercel: **Settings → Git** → conecte este repositório.
2. **Settings → Build & Deployment → Root Directory**: `frontend` (framework: Vite).
3. **Settings → Environment Variables**: `VITE_API_URL` = URL pública do backend
   (ex.: `https://prospectradar-api.onrender.com`). O backend precisa estar publicado na internet —
   `localhost` só funciona no seu computador.
4. No backend, inclua o domínio do site em `CORS_ORIGENS` (o `https://prospectradar-b2b.vercel.app` já vem liberado).

## Abrir no VS Code

```bash
git clone https://github.com/teusluv/prospeccao-b2b-fsa.git
cd prospeccao-b2b-fsa
git checkout claude/brave-pascal-nj3677
code .
```

Aceite as extensões recomendadas (Java Extension Pack, Spring Boot Tools, REST Client).
Para rodar: aba **Run and Debug** → **API (H2 em memória)** → ▶️.
O arquivo `backend/requisicoes.http` tem chamadas prontas para testar a API (botão "Send Request").

## Rodando pelo terminal

Pré-requisito: JDK 21. O Maven é baixado automaticamente pelo wrapper.

```bash
cd backend
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

- API: http://localhost:8080
- Swagger (documentação interativa): http://localhost:8080/swagger-ui.html
- Console H2: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:prospeccao`, usuário `sa`)
- Login inicial: `admin@fsa.com.br` / `admin123` (troque via `ADMIN_EMAIL` / `ADMIN_SENHA`)

Testes: `./mvnw test`

### Com PostgreSQL (Docker)

```bash
cd backend
docker compose up --build
```

### Com banco no Supabase

O Supabase hospeda o **banco de dados** (PostgreSQL). O backend Java continua rodando no seu computador ou em
um serviço de hospedagem (Render, Railway, Fly.io...) e se conecta ao banco do Supabase.

1. Crie um projeto em https://supabase.com e guarde a senha do banco.
2. No painel do projeto, clique em **Connect** e copie os dados da conexão **Session pooler**
   (porta 5432). Não use a "Direct connection" (só funciona em IPv6) nem o "Transaction pooler" (porta 6543).
3. Rode o backend apontando para o Supabase:

```bash
cd backend
export SPRING_PROFILES_ACTIVE=prod
export DB_URL="jdbc:postgresql://aws-0-SUA-REGIAO.pooler.supabase.com:5432/postgres?sslmode=require"
export DB_USUARIO="postgres.SEU_PROJECT_REF"
export DB_SENHA="sua-senha-do-banco"
./mvnw spring-boot:run
```

As tabelas são criadas automaticamente (Flyway) no schema **`prospeccao`**, visível em **Table Editor** →
seletor de schema. Elas ficam fora do schema `public` de propósito: o Supabase expõe o `public` publicamente
pela API REST automática dele, e aí ficariam, por exemplo, os hashes de senha da tabela `usuarios`.
**Não** adicione o schema `prospeccao` em *Settings → API → Exposed schemas*.

## Empresas sem site (OpenStreetMap ou Google Maps)

`POST /api/prospeccao/buscar` procura empresas de uma cidade e cadastra como lead **apenas as que não têm site**.
Quem só tem Instagram/Facebook/WhatsApp também conta como "sem site" (desligue com
`"redeSocialContaComoSemSite": false`). Empresas fechadas são ignoradas e nada é cadastrado duas vezes.
`"simular": true` só mostra o que seria importado.

```json
{ "fonte": "OPENSTREETMAP", "termo": "RESTAURANTES", "cidade": "Feira de Santana", "uf": "BA", "simular": true }
```

| Fonte | Custo | Observações |
|---|---|---|
| `OPENSTREETMAP` (padrão) | Gratuito, sem chave | Usa a [Overpass API](https://wiki.openstreetmap.org/wiki/Overpass_API). `termo` é um ramo (lista em `GET /api/prospeccao/fontes`) ou parte do nome. A cidade deve estar escrita como no mapa. Tem menos empresas cadastradas que o Google. Servidor configurável em `OSM_OVERPASS_URL`. |
| `GOOGLE_MAPS` | Cota gratuita mensal; exige cartão no Google Cloud | Requer `GOOGLE_PLACES_API_KEY` ([Places API (New)](https://developers.google.com/maps/documentation/places/web-service/get-api-key)). Máximo de 60 resultados por busca. Configure um limite diário de cota para nunca ser cobrado. |

Para listar os leads sem site já cadastrados: `GET /api/empresas?semSite=true`.

## Principais endpoints

Todos (exceto login) exigem o header `Authorization: Bearer <token>`.

| Método | Rota | Descrição |
|---|---|---|
| POST | `/api/auth/login` | Login, devolve o token JWT |
| GET | `/api/auth/me` | Usuário logado |
| POST | `/api/auth/trocar-senha` | Troca a própria senha |
| GET/POST/PUT | `/api/usuarios` | Gestão de usuários (só ADMIN) |
| GET | `/api/empresas` | Lista com filtros: `busca`, `etapa`, `segmento`, `cidade`, `uf`, `responsavelId`, `semSite`, paginação `page`/`size`/`sort` |
| POST/PUT/DELETE | `/api/empresas/{id}` | CRUD de empresas (excluir: só ADMIN) |
| PATCH | `/api/empresas/{id}/etapa` | Move no funil (`motivoPerda` obrigatório para PERDIDO) |
| POST | `/api/empresas/importar` | Importa CSV (campo `arquivo`; coluna `razaoSocial` obrigatória) |
| GET | `/api/prospeccao/fontes` | Fontes disponíveis e ramos do OpenStreetMap |
| POST | `/api/prospeccao/buscar` | Busca empresas sem site (OpenStreetMap ou Google Maps) |
| GET/POST | `/api/empresas/{id}/contatos` | Contatos da empresa |
| PUT/DELETE | `/api/contatos/{id}` | Edita/remove contato |
| GET/POST | `/api/empresas/{id}/interacoes` | Histórico de ligações, e-mails, reuniões... |
| GET | `/api/follow-ups?meus=true` | Follow-ups vencidos |
| PATCH | `/api/interacoes/{id}/concluir-follow-up` | Marca follow-up como feito |
| GET | `/api/dashboard` | Funil, valores, conversão, follow-ups |

Etapas do funil: `NOVO → CONTATADO → QUALIFICADO → PROPOSTA → NEGOCIACAO → GANHO / PERDIDO`.
Registrar a primeira interação move automaticamente um lead de `NOVO` para `CONTATADO`.

Erros seguem o padrão RFC 7807 (`application/problem+json`); erros de validação trazem o mapa `campos`.

## Variáveis de ambiente

| Variável | Padrão | Uso |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | `dev` = H2 em memória, `prod` = PostgreSQL |
| `DB_URL`, `DB_USUARIO`, `DB_SENHA` | localhost/prospeccao | Conexão PostgreSQL (perfil `prod`) |
| `DB_SCHEMA` | `prospeccao` | Schema onde as tabelas são criadas (perfil `prod`) |
| `DB_POOL` | `5` | Máximo de conexões abertas com o banco |
| `JWT_SECRET` | valor de exemplo | **Troque em produção** (mín. 32 caracteres) |
| `JWT_EXPIRACAO_MINUTOS` | `480` | Validade do token |
| `CORS_ORIGENS` | `http://localhost:3000,http://localhost:5173` | Origens do frontend |
| `ADMIN_NOME`, `ADMIN_EMAIL`, `ADMIN_SENHA` | admin@fsa.com.br / admin123 | Admin criado no primeiro start |
| `GOOGLE_PLACES_API_KEY` | vazio | Habilita a busca no Google Maps (opcional) |
| `OSM_OVERPASS_URL` | `https://overpass-api.de/api/interpreter` | Servidor Overpass do OpenStreetMap |
