# Blossom ERP - PORTUGUÊS

#### Sistema completo de gestão de pedidos, estoque e clientes desenvolvido com Kotlin e JavaFX.

O Blossom ERP é um sistema abrangente para gerenciamento de pedidos, controle de estoque e cadastro de clientes, oferecendo uma interface moderna e intuitiva para operações comerciais.


## Resumo do Projeto

O sistema permite a criação, edição e exclusão de usuários, perfis, módulos, transações e funções, além de associar perfis a usuários para definir permissões de acesso.

## Funcionalidades Implementadas

#### Gestão de Pedidos
- Criação de pedidos através de interface interativa;
- Visualização e edição de pedidos existentes;
- Cancelamento de pedidos.
  
#### Gestão de Clientes 
- Cadastro completo de clientes;
- Edição de informações de clientes;
- Histórico de pedidos por cliente.

#### Controle de Estoque
- Cadastro de produtos;
- Controle de entradas e saídas;
- Alertas de estoque baixo.

#### Processamento de Pagamentos
- Suporte a múltiplas formas de pagamento;
- Emissão de comprovantes de pagamento;
- Controle de recebimentos.

#### Gestão de Entregas
- Registro de informações de entrega;
- Acompanhamento de status de entregas;
- Histórico de entregas por cliente.

#### Relatórios
- Geração de relatórios em PDF usando iText7;
- Relatórios de vendas, estoque e clientes;
- Impressão de comprovantes e recibos.

#### Interface do Usuário
- Interface moderna com Material Design;
- Navegação intuitiva por abas e wizards;
- Suporte a impressoras térmicas (ESC/POS).

#### Dashboards
- Dashboards administrativos para visualização de atividades e gestão.

## Tecnologias Utilizadas
- Kotlin 1.8.0
- JavaFX 17.0.2
- SQLite 3.45.1
- JFoenix 9.0.10 (componentes Material Design)
- iText7 7.2.3 (geração de PDF)
- ESC/POS Coffee 4.1.0 (suporte a impressoras térmicas)
- Gradle (sistema de build)

## Requisitos de Sistema
- Java 17 ou superior;
- Sistema Operacional Windows (para o instalador);
- 300MB de espaço em disco.

## Instalação e Configuração
- Baixe o instalador (.exe) da página de releases;
  ```
  https://github.com/gripka/sistema_pedidos/releases
  ```
- Execute o instalador e siga as instruções na tela;
- Inicie o aplicativo através do atalho criado no menu Iniciar.

## Compilação do Código-Fonte

1. Clone o repositório:
  ```
  git clone https://github.com/gripka/sistema_pedidos.git
  ```

2. Navegue até o diretório do projeto:
  ```
  cd "nome-do-repositorio"
  ```
3. Compile o projeto:
  ```
  ./gradlew build
  ```
##### Para a criação do instalador:
  ```
  ./gradlew jpackage
  ```
OBS: O instalador será criado no diretório build/jpackage.

## Compilação do Código-Fonte
O projeto utiliza o padrão MVC (Model-View-Controller) e é estruturado com os seguintes pacotes principais:

- com.sistema_pedidos.model - Classes de domínio e acesso a dados;
- com.sistema_pedidos.view - Componentes de interface do usuário;
- com.sistema_pedidos.controller - Lógica de negócios e controle.

A classe de entrada principal é ```com.sistema_pedidos.MainKt```.

## Licença
Copyright © 2025. Todos os direitos reservados.

Este software é de código aberto e fornecido "como está", sem garantias expressas ou implícitas.

## Contribuição
- [Gripka](https://github.com/gripka)
- [Luiz Felipe](https://github.com/luizmachado432)
