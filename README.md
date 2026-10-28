# Ateliê Jô Monteiro — site e sistema de gestão

Site do ateliê de papelaria criativa e presentes personalizados (cadernetas, canecas, garrafas, Bíblias, kits e cestas)
e sistema de gestão, num único aplicativo. Feito a partir da base do sistema do Lenne Carvalho Ateliê.
Desenvolvido por Joel Júnior.

- **Loja** (`/` e `/catalogo`): layout de loja com menu de categorias, busca, banner, vitrines (pronta-entrega, destaques, mais pedidos, para presentear), inspiração, como funciona, depoimentos e a **sacola de orçamento** ao lado.
  A cliente junta as peças na sacola e manda tudo num **pedido de orçamento**, com personalização e até 3 fotos de referência.
- **Meu pedido** (`/pedido/<código>`): a cliente acompanha as etapas da encomenda, a data combinada e o saldo (com a chave Pix), e aprova a arte ou pede ajuste, sem senha. Os links antigos `/arte/<código>` continuam abrindo.
- **Sistema** (`/admin`): painel, orçamentos, encomendas (lista ou quadro de produção, com artes e ficha para imprimir), produtos (fichas técnicas e catálogo), vendas, agenda, clientes, financeiro, relatórios, histórico, materiais, portfólio e configurações.

**Tecnologias:** Spring Boot 3.4 (Java 21), MySQL 8 com Flyway, React 18 + Vite, login com JWT.

## Como o fluxo funciona

1. **Orçamento pelo site.** A cliente escolhe o tipo de peça (ou uma peça do catálogo), a quantidade, a data,
   o que vai personalizado (nome, frase, tema), as cores e pode mandar **até 3 fotos de referência**.
   - As fotos são reduzidas no próprio celular antes do envio (ficam com ~300 KB) e vão para a pasta **privada**.
   - O pedido entra como "Novo" em Orçamentos, e a cliente continua a conversa no WhatsApp com o resumo já escrito.
   - No catálogo, o botão "Pedir orçamento desta peça" abre o formulário com a peça escolhida.
2. **Aprovar e criar encomenda.** Cadastra a cliente (ou reaproveita pelo WhatsApp) e cria a encomenda com valor total e sinal.
   A peça, a quantidade, a personalização e as fotos de referência passam para a encomenda, e a arte fica "a criar".
3. **Registrar pagamento.** O pagamento entra no financeiro. Quando o sinal é pago, a encomenda passa sozinha para "Em produção".
4. **Aprovação da arte** (veja abaixo). Enquanto a arte não é aprovada, a encomenda mostra o aviso "espere a aprovação antes de produzir".
5. **Encomenda até a entrega.** Os status são: Em produção → Pronta → Entregue. O saldo em aberto aparece em "A receber".
6. **Materiais.** Em "Comprei", o estoque soma e a compra pode ir para as despesas. Em "Usei", o estoque desconta. Abaixo do mínimo, aparece um aviso no painel.

### Loja e sacola de orçamento

- **Categorias** (Cadernetas, Canecas, Garrafas, Bíblias, Kits e cestas, Lembrancinhas, Pronta-entrega): as peças entram na categoria
  pelo nome (ex.: "Caneca…", "Kit…") e pela frente; "Pronta-entrega" mostra as peças com estoque. As palavras ficam em `src/site/Catalogo.jsx`.
- **Mais pedidos:** as peças do catálogo com mais unidades em encomendas e vendas nos últimos 6 meses (o site recebe só a ordem, sem números).
- **Sacola:** fica no navegador da cliente (não precisa de cadastro). No computador aparece sempre à direita; no tablet e no celular,
  abre como gaveta pelo botão "Sacola". A estimativa usa o preço do catálogo; o valor final vem no orçamento.
- **Pedido da sacola:** com uma peça só, o orçamento fica ligado a ela; com várias, o orçamento lista as peças e quantidades
  (com o preço de referência) e, ao aprovar, essa lista vai para as observações da encomenda.
- Sem fotos cadastradas, as peças mostram a Jô no círculo no lugar da foto. Sem fotos no portfólio, a "Inspiração" mostra ideias
  de kits que abrem o pedido de orçamento já com o tipo escolhido.

### Aprovação de arte

- **Encomenda › Arte:** a Jô envia o mockup (JPG, PNG ou WEBP) com um recado opcional. O sistema cria o link da encomenda
  (`/arte/<código>`) e a arte fica "com a cliente".
  - "Mandar para <cliente>" abre o WhatsApp com a mensagem e o link; "Copiar" copia só o link.
  - Os **lembretes** do dia também trazem a mensagem com o link e, se a cliente não responder em 2 dias, um lembrete para cobrar com carinho.
- **Página da cliente:** mostra a arte grande, o que foi combinado e uma lista para conferir (nomes e datas, cores, posição das fotos).
  - "Aprovar a arte" pede uma confirmação ("Posso produzir com esta arte?") e aceita um recado.
  - "Pedir ajuste" exige escrever o que mudar. A resposta aparece no sistema na hora.
  - O link mostra o total, o que já foi pago e o saldo, mas nunca o telefone da cliente nem os custos. Ele para de abrir se a encomenda for cancelada.
- **Ajuste pedido:** a Jô envia a nova versão; a versão anterior e o comentário ficam no histórico (para a Jô e para a cliente).
  Só a última versão pode ser respondida.
- **Resposta pelo WhatsApp:** os botões "Aprovou" e "Pediu ajuste" registram o que a cliente disse fora do link.
- **Sem arte:** peças de pronta-entrega ou lisas podem desmarcar "Peça personalizada". Também dá para marcar "Já foi aprovada fora do sistema".
- A versão mais recente pode ser excluída enquanto a cliente não respondeu (para corrigir um envio errado).
- O **painel** mostra artes a criar, ajustes pedidos e artes com a cliente; o menu Encomendas mostra quantas artes faltam fazer,
  e a lista de encomendas tem o filtro pela situação da arte.

### Meu pedido, quadro de produção e histórico

- **Link do pedido:** toda encomenda nasce com o seu link (`/pedido/<código>`), mesmo antes da arte.
  - Na encomenda, em "Acompanhamento da cliente": copiar, ver como a cliente e mandar pelo WhatsApp com a mensagem pronta.
  - A cliente vê as etapas (pedido confirmado → arte aprovada → em produção → pronta → entregue), a data combinada e quanto falta pagar, com a chave Pix e o botão de copiar. Quando tudo está pago, a chave some.
  - Para escolas e empresas, a saudação usa o nome completo.
  - O sistema guarda a última vez que a cliente abriu o link. Quem abre logada no sistema não conta.
- **Arte vista:** cada versão registra quando a cliente a viu pela primeira vez, a última vez e quantas visitas fez (aberturas com menos de 30 minutos entre elas contam como uma).
  - Na encomenda: "ainda não abriu a arte" (com o botão Mandar, que vira Reenviar depois de um dia) ou "já viu a arte" (com Lembrar de responder).
  - Na lista de encomendas, a coluna Arte diz se a arte com a cliente já foi vista.
- **Quadro de produção** (Encomendas › Quadro): colunas Aguardando sinal, Arte em andamento, Produzir, Prontas e Entregues (7 dias).
  - Urgentes primeiro, depois a entrega mais próxima. Selos de prazo: atrasada, hoje, amanhã, em N dias.
  - Botões no cartão: Pronta, Entregue e o raio de urgente (só a administradora marca urgente). No celular, as colunas deslizam para o lado.
- **Ficha de produção** (botão na encomenda): página para imprimir ou salvar em PDF numa folha A4, sem valores.
  - Traz a data em destaque, urgente, quantidade, personalização em letra grande, a arte aprovada (ou o aviso de que ainda não foi aprovada), as fotos de referência, os materiais da ficha × quantidade, uma lista para conferir e espaço para anotações.
- **Histórico** (menu Histórico, só a administradora): quem fez o quê e quando, por período, área e pessoa.
  - Inclui "Clientes (pelo link)". Cada encomenda também mostra a sua linha do tempo.
  - Registra: criação e edição da encomenda (só o que mudou, com antes → depois), status, urgente, pagamentos, artes enviadas, marcadas ou excluídas, respostas e visualizações da cliente, fotos, materiais, vendas, lançamentos do financeiro, compras, uso e entrada de material e produção de pronta-entrega.
  - As linhas que falam de dinheiro não aparecem para a ajudante.
  - Excluir uma encomenda deixa o registro no histórico geral.

### Custos, preço e pronta-entrega

1. **Configurações › Precificação.** Ela informa quanto quer ganhar por mês, as horas de produção, os custos fixos e a margem padrão.
   - O sistema calcula o valor da hora e o custo fixo por hora.
   - Esses dados não aparecem no site.
2. **Produtos (fichas técnicas).** Cada peça tem materiais por unidade, horas de trabalho e embalagem. O preço é montado assim:
   ```
   custo = materiais + horas × valor da hora + horas × custo fixo por hora + embalagem
   preço sugerido = custo × (1 + margem), arredondado para cima no real
   ```
   A ficha mostra o lucro por peça e quanto rende a hora com o preço que ela cobra.
3. **Encomenda com ficha.** Os materiais da ficha × quantidade saem do estoque sozinhos quando a encomenda entra em produção, uma única vez.
   - Material extra pode ser registrado na encomenda.
   - Um material pode ser devolvido ao estoque.
   - A encomenda mostra os custos reais, o ganho, o valor por hora frente à meta e o lucro.
4. **Comprei material com valor.** O custo por unidade passa a ser o da última compra, e as fichas se atualizam.
5. **Pronta-entrega.**
   - "Produzi" soma peças ao estoque e baixa os materiais.
   - Em **Vendas**, o caixa rápido (feito para o celular, na feira) tira as peças do estoque e lança a entrada no financeiro.
   - Cancelar a venda devolve as peças e remove a entrada.

O estoque de material pode ficar negativo; nesse caso aparece como "registre a compra". Assim a produção nunca fica travada por um cadastro desatualizado.

### Relatórios, documentos, agenda, lembretes e fotos

- **Relatórios:** período livre ou atalhos (este mês, mês passado, 3 meses, ano).
  - Mostra entradas, saídas e resultado, comparados com o mesmo trecho do período anterior.
  - Traz um gráfico de 12 meses, as entradas por frente, quanto rendeu cada peça entregue (ganho e valor por hora frente à meta), a pronta-entrega, os mais vendidos e os materiais mais usados.
  - Exporta em **Excel** (uma aba por assunto, mais todos os lançamentos) e em **PDF**.
- **Proposta em PDF:** gerada no orçamento, com a marca do ateliê, valor, sinal, condições, validade e chave Pix.
- **Contrato em PDF:** gerado na encomenda, com as partes, objeto, valor e pagamentos, prazo, regras de cancelamento e assinaturas.
  - Os textos são editáveis em Configurações › Proposta e contrato.
  - O CPF/CNPJ e o endereço da cliente ficam no cadastro dela. Se faltarem, o contrato sai com espaço para preencher à mão.
- **Agenda:** calendário do mês com entregas, compromissos (entregas e retiradas, reuniões, dias de produção, compras), vencimentos e aniversários de clientes. No celular, vira pontinhos coloridos e a lista do dia.
- **Lembretes de WhatsApp:** o sistema escreve a mensagem e ela só confere e envia (link wa.me), depois marca "Já enviei". Os tipos:
  - arte pronta para aprovar (com o link) e arte sem resposta depois de 2 dias;
  - peça pronta;
  - entrega amanhã;
  - saldo em aberto (volta depois de 7 dias);
  - aniversário.
- **Fotos da encomenda:** referência da cliente e peça pronta.
  - Ficam numa pasta **privada**, fora do site, e só abrem com login.
  - A foto da peça pronta vai para o portfólio com um toque: o sistema faz uma cópia pública. A referência nunca vai, porque costuma ser imagem de outra pessoa.

### Compras, contas fixas, equipe e depoimentos

- **Materiais › Compras e fornecedores:** cadastro de fornecedores e compra com vários materiais de uma vez.
  - Cada compra soma no estoque, atualiza o custo de cada material (as fichas acompanham) e vira despesa no financeiro.
  - O frete é dividido entre os itens pelo valor de cada um, para o custo por unidade ser o real.
  - Na hora de lançar, o sistema mostra se o material saiu mais caro ou mais barato que o custo atual.
  - O botão de histórico em cada material mostra todos os preços pagos e o último preço em cada fornecedor, com o mais barato destacado.
  - Cancelar uma compra tira os materiais do estoque, apaga a despesa e volta o custo ao da compra anterior.
- **Financeiro › Contas fixas:** aluguel, energia, internet, MEI…
  - Cada conta tem valor previsto e dia de vencimento (dia 31 vence no último dia dos meses curtos).
  - "Paguei" lança a despesa em "Contas fixas", ligada ao mês; o mesmo mês não é pago duas vezes.
  - O painel avisa as contas vencidas e as que vencem em até 5 dias, e a agenda mostra os vencimentos.
  - Em Configurações › Precificação, "Usar as contas fixas" faz o custo fixo por hora acompanhar sozinho a soma das contas marcadas.
- **Equipe (Configurações › Equipe):** acesso de ajudante para quem ajuda na produção.
  - A ajudante vê encomendas (sem valores), envia e acompanha as artes, registra material usado, marca como pronta ou entregue, envia fotos, vê a agenda, lança "Produzi" e vende no caixa.
  - Ela não vê valor cobrado, custos, saldos, financeiro, relatórios, compras, contas, orçamentos, clientes nem configurações. O servidor bloqueia essas rotas e tira os valores das respostas, não só da tela.
  - Desativar o acesso bloqueia na hora, mesmo com o login aberto. Sempre fica pelo menos uma administradora ativa.
- **Portfólio › Depoimentos:** texto, quem é, estrelas, foto opcional, ordem e publicar ou esconder.
  - Os publicados aparecem no site depois do portfólio (no celular, em carrossel).
  - Alguns dias depois de uma entrega quitada, os lembretes sugerem pedir um depoimento à cliente.
  - Os depoimentos "Exemplo — …" do banco de testes são só para ver o visual: apague antes de publicar o site.

### Catálogo

- **Catálogo (Produtos › botão Catálogo):** a peça da ficha técnica aparece no site em `/catalogo` e numa seção da página inicial.
  - Tem texto para a cliente, até 8 fotos (a primeira é a capa), prazo de produção, ordem, e preço ou "sob consulta".
  - O preço é o que ela cobra: peça de pronta-entrega mostra o valor e as unidades disponíveis; peça sob encomenda mostra "a partir de".
  - Peça sob encomenda: "Pedir orçamento desta peça" abre o formulário do site com ela escolhida. Pronta-entrega: "Quero comprar" abre o WhatsApp. Custos e materiais nunca vão para o site.

### Identidade visual

- **Logos:** extraídas do PDF do manual (2024), com fundo transparente, em `frontend/public/marca/` (WebP):
  horizontal (mascote de busto), empilhada (mascote de corpo inteiro), circular (selo "Ateliê Jô Monteiro / Papelaria criativa"),
  cada uma também na versão **clara** (texto branco) para fundo laranja ou preto; mais o símbolo (a Jô no círculo laranja),
  a mascote sem texto e o monograma JM com a raposinha, usado como estampa de fundo bem suave.
- **Cores:** laranja `#F2591A`, preto `#231F20` e branco. Nos botões com texto branco usamos um laranja mais escuro (`#C9440F`)
  para dar contraste. Cores de apoio vindas da mascote: roxo da blusa `#6A47A8` e amarelo da caderneta `#E3A21B`.
- **Fontes (provisórias, parecidas com as do manual):** o manual usa Mavera Brush (nome), Patrick Oklahoma (slogan cursivo) e
  Montserrat (texto). Até chegarem os arquivos das duas primeiras, o site usa **Big Shoulders Display** nos títulos,
  **Sacramento** na cursiva e **Special Elite** no detalhe "papelaria criativa" (máquina de escrever). Montserrat é a mesma do manual.
  Todas ficam no próprio site (`frontend/public/fontes/`, licenças OFL/Apache), sem depender do Google Fonts.
  Para trocar pelas fontes oficiais: coloque os `.woff2` em `public/fontes/` e ajuste os `@font-face` e as variáveis
  `--titulo` e `--cursiva` no topo de `src/estilo.css`.
- **PDFs:** proposta, contrato e relatório usam a logo (PNG em `backend/src/main/resources/marca/`) e as cores da marca.
- **WhatsApp:** enquanto o número do ateliê não for cadastrado em Configurações, os botões do site usam o link de orçamento
  que a Jô já usa no Instagram (abre.ai/jomonteirodesigner).

## Rodar no computador (Windows + NetBeans)

**Pré-requisitos:**
- JDK 21
- MySQL local (usuário `root`, senha `root` por padrão)
- Node.js 20 ou mais novo

**Passos:**

1. Monte o site uma vez. Isso gera o React dentro de `backend/src/main/resources/static`:
   ```
   cd frontend
   npm install
   npm run build
   ```
2. No NetBeans, abra a pasta `backend` (projeto Maven) e rode `AtelieApplication`.
   - O banco `jo_atelie` é criado sozinho, e o Flyway cria as tabelas.
   - Se sua senha do MySQL não for `root`, defina a variável `DB_PASSWORD`.
   - **Configuração só do seu computador** (senha do MySQL, porta…): não altere o `application.yml` do projeto. Crie `backend/config/application.yml` só com o que muda, por exemplo:
     ```yaml
     spring:
       datasource:
         password: minha-senha
     ```
     O Spring lê esse arquivo por cima do padrão, e o Git ignora a pasta. Assim o `git pull` nunca entra em conflito.
3. Acesse:
   - Site: http://localhost:8080
   - Sistema: http://localhost:8080/admin
   - Login inicial: `jo@atelie.local` / `trocar123`. Troque a senha em Configurações e cadastre o WhatsApp do ateliê.

**Para mexer no visual com recarga automática:** deixe o backend rodando e, em `frontend`, rode `npm run dev`. Depois abra http://localhost:5173.

## Publicar na VPS (Docker)

1. Copie a pasta para a VPS, por exemplo `/opt/jo-monteiro-atelie`.
2. Crie o `.env` a partir do `.env.exemplo` e preencha senhas fortes, `JWT_SECRET`, o e-mail da Jô e `URL_PUBLICA` (endereço do site, usado nos links de aprovação de arte).
3. Suba os serviços:
   ```
   docker compose up -d --build
   ```
   O sistema fica em `127.0.0.1:8086` (ou na `APP_PORTA` definida).
4. No Nginx, aponte o domínio para essa porta e ative HTTPS com o Certbot:
   ```nginx
   server {
       server_name jomonteiroatelie.com.br www.jomonteiroatelie.com.br;
       client_max_body_size 20M;
       location / {
           proxy_pass http://127.0.0.1:8086;
           proxy_set_header Host $host;
           proxy_set_header X-Forwarded-For $remote_addr;
           proxy_set_header X-Forwarded-Proto $scheme;
       }
   }
   ```
   Depois rode: `certbot --nginx -d jomonteiroatelie.com.br -d www.jomonteiroatelie.com.br`

**Servidor de 1 GB (demonstração).** Em máquinas pequenas como a Google Cloud e2-micro, crie 3 GB de swap e suba com os ajustes de memória (MySQL enxuto e Java limitado a 256 MB; o sistema usa cerca de 350 MB):
```
docker compose -f docker-compose.yml -f docker-compose.pequena.yml up -d --build
```

**Backups.** O banco fica no volume `banco`, as fotos do site no volume `uploads` e as fotos privadas das encomendas no volume `privado`. Para um backup do banco:
```
docker compose exec banco sh -c 'mysqldump -u root -p"$MYSQL_ROOT_PASSWORD" jo_atelie' > backup.sql
```

## Testes

```
cd backend
mvn test
```

Os testes cobrem:
- login
- orçamento → encomenda → pagamento
- materiais
- envio de fotos
- cálculo da ficha técnica e do preço sugerido
- baixa automática de materiais e lucro da encomenda
- venda de pronta-entrega e cancelamento
- relatório com números conferidos à mão, Excel e PDF
- proposta e contrato em PDF
- agenda e lembretes
- fotos privadas e publicação no portfólio
- compra com frete rateado, histórico de preços e cancelamento
- contas fixas, pagamento no financeiro e precificação automática
- acesso de ajudante sem valores e com rotas bloqueadas
- depoimentos com foto no site e lembrete de pedir depoimento
- orçamento do site com peça do catálogo, personalização e fotos (recusa arquivo falso e mais de 3 fotos)
- sacola com várias peças (soma repetidas, ignora peça fora do catálogo), uma peça só e "mais pedidos" do site
- aprovação do orçamento levando peça, personalização e fotos para a encomenda
- aprovação de arte pelo link: ajuste, nova versão, aprovação, histórico, lembretes e link que some ao cancelar
- resposta da arte registrada pelo sistema e exclusão da última versão

## Atualizar a VPS

```
git pull
docker compose up -d --build
```

O Flyway aplica sozinho as mudanças no banco, sem apagar dados.

## Segurança

- Senhas guardadas com BCrypt.
- Login com limite de tentativas.
- Formulários públicos com limite por IP e campo anti-robô.
- Fotos validadas pelo conteúdo do arquivo (JPG, PNG ou WEBP, até 8 MB). Fotos de orçamento, encomendas e artes ficam fora da pasta pública.
- Link de aprovação de arte com código aleatório, limite de acessos por IP e sem valores nem dados da cliente.
- Rotas `/api/**` exigem login, exceto `/api/publico/**`.
