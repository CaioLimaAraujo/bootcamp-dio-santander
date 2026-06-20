# Desafio Final — API de Orçamento Inteligente com Spring Boot + Spring AI

Projeto final do *DIO/Santander Java Backend & AI Bootcamp*, evoluído a partir do projeto base
construído nas aulas do módulo `05-spring-ai` ([repositório original do expert Poiani](https://github.com/digitalinnovationone/dio-spring-boot-learning-track/tree/main/05-spring-ai)).

## O que o projeto faz

A aplicação é uma API de orçamento financeiro capaz de interpretar **comandos de voz** e transformá-los
em ações reais sobre transações financeiras, usando IA Generativa integrada via Spring AI.

Fluxo principal:

1. O usuário envia um arquivo de áudio (ex: *"gastei 50 reais no mercado"*);
2. O áudio é transcrito para texto (Whisper, via OpenAI);
3. O texto é interpretado por um modelo de linguagem (`gpt-4o-mini`), que decide qual ação executar
   usando **Tool Calling** — criar uma transação ou consultar transações por categoria;
4. A ação é executada de fato na aplicação (persistência em banco de dados);
5. O modelo gera uma resposta em texto, que é convertida em áudio (text-to-speech) e devolvida ao usuário.

Além do fluxo por voz, a API também expõe endpoints REST tradicionais para criar e consultar transações
diretamente, sem passar pela camada de IA.

## Qual melhoria eu implementei

Escolhi **adicionar validações de domínio antes de salvar uma transação**, evitando que dados inconsistentes
entrem no sistema — seja por uma requisição REST manual, seja por uma transação criada indiretamente pelo
assistente de IA via Tool Calling.

Regras adicionadas na entidade `Transaction`:

- A descrição não pode ser nula ou vazia;
- O valor (`amount`) precisa ser maior que zero;
- A categoria é obrigatória.

Qualquer violação lança uma `InvalidTransactionException` (exceção de domínio, sem dependência de
frameworks). Um `@RestControllerAdvice` (`GlobalExceptionHandler`) captura essa exceção e converte em uma
resposta HTTP **400 Bad Request** com uma mensagem clara, em vez de um genérico **500 Internal Server Error**:

```json
{
  "timestamp": "2026-06-20T14:32:10.123Z",
  "status": 400,
  "message": "O valor da transação deve ser maior que zero."
}
```

Como a validação vive na própria classe `Transaction` (camada de domínio), ela é aplicada **sempre**,
independentemente de qual camada está criando a transação — REST ou IA.

## Tecnologias usadas

- Java 25
- Spring Boot 4.0.5
- Spring AI 2.0.0-M4 (`ChatClient`, Tool Calling, Transcription, Text-to-Speech)
- OpenAI (`gpt-4o-mini`, `whisper-1`, `gpt-4o-mini-tts`)
- Spring Data JPA + MySQL 9.6
- Spring Boot Docker Compose Support
- Gradle 9.4.1 (via Gradle Wrapper)
- JUnit 5, Mockito e AssertJ

## Como executar a aplicação

### Pré-requisitos

- JDK instalado (o Gradle Wrapper provisiona o Java 25 automaticamente, se necessário)
- Docker Desktop instalado e em execução (sobe o MySQL automaticamente)
- Uma chave de API da OpenAI

### Passos

```bash
# defina sua chave da OpenAI
export OPENAI_API_KEY="sua_chave_aqui"        # Linux/Mac
$env:OPENAI_API_KEY="sua_chave_aqui"          # Windows PowerShell

# rode a aplicação
./gradlew bootRun        # Linux/Mac
.\gradlew.bat bootRun    # Windows
```

O Spring Boot detecta o `compose.yml` na raiz do projeto e sobe o container MySQL automaticamente —
não é necessário rodar `docker compose up` manualmente.

A aplicação fica disponível em `http://localhost:8080`.

## Como testar o fluxo principal

### 1. Rodar a suíte de testes automatizados

```bash
./gradlew clean test        # Linux/Mac
.\gradlew.bat clean test    # Windows
```

Isso executa, entre outros:
- `TransactionTest` — testes unitários puros das validações de domínio;
- `TransactionControllerTest` — testa que o endpoint REST retorna `400` ao receber uma transação inválida;
- `BudgetingApplicationTests` (`contextLoads`) — sobe o contexto Spring completo, incluindo o banco via Docker Compose.

### 2. Testar manualmente os endpoints REST

Criar uma transação válida:

```bash
curl -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{"description": "Mercado", "category": "GROCERIES", "amount": 5000}'
```

Tentar criar uma transação inválida (deve retornar `400`):

```bash
curl -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{"description": "", "category": "GROCERIES", "amount": -10}'
```

Consultar transações por categoria:

```bash
curl http://localhost:8080/transactions/GROCERIES
```

Categorias disponíveis: `GROCERIES`, `PHARMA`, `AUTO`.

### 3. Testar o fluxo de voz (via IA)

```bash
curl -X POST http://localhost:8080/transactions/ai \
  -F "file=@audio.mp3" \
  --output resposta.mp3
```

Envie um áudio dizendo algo como *"gastei 50 reais na farmácia"*. A resposta é um arquivo de áudio
(`resposta.mp3`) confirmando a ação executada.

## O que eu aprendi durante o desafio

- Como o **Spring AI** conecta transcrição de áudio, `ChatClient` e Tool Calling para transformar
  linguagem natural em ações reais de negócio, mantendo a lógica de domínio isolada de frameworks de IA;
- A importância de validar invariantes **na camada de domínio**, e não só na borda da aplicação (REST),
  garantindo que regras de negócio valham para qualquer forma de entrada — incluindo chamadas feitas
  pelo próprio modelo de IA via Tool Calling;
- Que no Spring Boot 4.0 a anotação `@MockBean` foi removida em favor de `@MockitoBean`
  (`org.springframework.test.context.bean.override.mockito.MockitoBean`);
- Que a configuração `developmentOnly` do Gradle **não** disponibiliza dependências no classpath de testes
  por padrão — foi necessário trocar para `testAndDevelopmentOnly` e desativar
  `spring.docker.compose.skip.in-tests` para que o suporte a Docker Compose funcionasse também durante
  `./gradlew test`, e não só em `bootRun`;
- Configuração de ambiente de desenvolvimento Java no Windows: o Gradle Wrapper elimina a necessidade de
  instalar o Gradle manualmente (baixa a versão correta automaticamente, assim como o `mvnw` faz no Maven),
  e os *toolchains* do Gradle resolvem a versão do JDK necessária sozinhos;
- Troubleshooting de WSL2 (erros `Catastrophic failure` e `0x8000ffff` na instalação de distribuições,
  necessidade de `wsl --update` e conversão de versão com `wsl --set-version`) como pré-requisito para o
  Docker Desktop rodar corretamente no Windows.

---

Projeto desenvolvido como parte do **DIO/Santander Java Backend & AI Bootcamp**.
