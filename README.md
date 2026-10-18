#  PlantsIOT — Monitoramento Inteligente de Plantas

Aplicativo Android desenvolvido em **Kotlin** para monitorar informações de plantas por meio da Internet das Coisas (IoT). O projeto utiliza comunicação MQTT com o HiveMQ Cloud para receber dados de sensores, exibir as informações em um dashboard e armazenar um histórico local das leituras.

##  Índice

- [Sobre o projeto](#-sobre-o-projeto)
- [Funcionalidades](#-funcionalidades)
- [Tecnologias utilizadas](#-tecnologias-utilizadas)
- [Arquitetura do sistema](#-arquitetura-do-sistema)
- [Formato das mensagens MQTT](#-formato-das-mensagens-mqtt)
- [Como executar](#-como-executar)
- [Solução de problemas](#-solução-de-problemas)
- [Segurança](#-segurança)
- [Objetivo do projeto](#-objetivo-do-projeto)

##  Sobre o projeto

O **PlantsIOT** foi desenvolvido para facilitar o acompanhamento das condições de uma planta por meio de dados enviados por dispositivos IoT, como o ESP32.

A aplicação recebe informações através do protocolo MQTT, utilizando o HiveMQ Cloud como broker, e apresenta as leituras em uma interface organizada e intuitiva.

O sistema aceita tanto mensagens em formato JSON completo quanto valores publicados em tópicos MQTT separados, oferecendo flexibilidade na integração com diferentes dispositivos.

##  Funcionalidades

-  **Temperatura do ar:** acompanhamento da temperatura ambiente.
-  **Umidade do ar:** visualização da umidade relativa do ambiente.
-  **Umidade do solo:** acompanhamento das condições de umidade do substrato.
-  **Luminosidade:** monitoramento dos níveis de luz.
-  **pH:** exibição do valor de pH informado pelo dispositivo.
-  **Identificação de cores RGB:** apresentação dos valores vermelho, verde e azul, incluindo uma amostra visual da cor.
-  **Indicador de saúde:** exibição da situação da planta e de uma pontuação de 0 a 100, quando disponível.
-  **Informações adicionais:** visualização da altura, observações, tópico de origem e conteúdo da mensagem recebida.
-  **Comunicação MQTT:** conexão com o HiveMQ Cloud utilizando TLS.
-  **Dashboard:** centralização das informações recebidas.
-  **Histórico local:** armazenamento de até 200 leituras no dispositivo.
-  **Configurações:** personalização do filtro MQTT e das configurações da planta e da conexão.
-  **Gerenciamento de conta:** telas para edição de informações da conta e recuperação de senha por e-mail.

> **Observação:** quando os dados de saúde não são enviados, o aplicativo pode gerar uma indicação baseada na predominância RGB. Essa indicação é apenas orientativa e não substitui uma avaliação agronômica.

##  Tecnologias utilizadas

| Tecnologia | Aplicação no projeto |
|---|---|
| Kotlin | Desenvolvimento da aplicação Android |
| Android XML | Construção das interfaces |
| MQTT | Comunicação entre o aplicativo e os dispositivos IoT |
| HiveMQ Cloud | Broker responsável pela comunicação MQTT |
| TLS | Proteção da conexão com o broker |
| Eclipse Paho MQTT 3.1.1 | Cliente MQTT utilizado na comunicação |
| MPAndroidChart | Visualização de gráficos |
| SharedPreferences | Armazenamento local do histórico |
| Gradle | Configuração e compilação do projeto |

##  Arquitetura do sistema

O funcionamento do PlantsIOT pode ser dividido em quatro etapas principais:

1. **Coleta de dados:** sensores realizam medições das condições ambientais.
2. **Transmissão:** um dispositivo IoT publica os valores em tópicos MQTT.
3. **Comunicação:** o HiveMQ Cloud recebe as mensagens e as disponibiliza aos clientes autorizados.
4. **Visualização:** o aplicativo interpreta os dados, atualiza o dashboard e armazena as leituras localmente.

### Fluxo de comunicação

\`\`\`text
Sensores
   ↓
Dispositivo IoT (ex.: ESP32)
   ↓ MQTT + TLS
HiveMQ Cloud
   ↓ MQTT
Aplicativo Android (Kotlin)
   ↓
Dashboard e histórico local
\`\`\`

##  Formato das mensagens MQTT

O aplicativo permite receber informações de duas maneiras: por meio de um JSON completo ou por valores enviados em tópicos separados.

### 1. Mensagem JSON completa

Exemplo de mensagem que pode ser publicada no tópico `planta/esp32cam/analysis` ou `estufa/dados`:

\`\`\`json
{
  "temperatura": 26.4,
  "umidadeAr": 61.0,
  "umidadeSolo": 48.0,
  "luminosidade": 820,
  "ph": 6.7,
  "rgb": {
    "r": 72,
    "g": 146,
    "b": 54
  },
  "saude": "Saudável",
  "healthScore": 91,
  "heightCm": 18.5,
  "notes": "Folhas com boa predominância de verde"
}
\`\`\`

Os valores apresentados são fictícios e servem apenas como exemplo.

O aplicativo também aceita nomes equivalentes em inglês, como `temperature`, `airHumidity`, `soilMoisture`, `luminosity`, `healthStatus`, `red`, `green` e `blue`.

### 2. Mensagens em tópicos separados

Também é possível enviar cada informação individualmente.

| Tópico sugerido | Exemplo de payload |
|---|---|
| `estufa/temperatura` | `26.4` |
| `estufa/umidade/ar` | `61` |
| `estufa/umidade/solo` | `48` |
| `estufa/luminosidade` | `820` |
| `estufa/ph` | `6.7` |
| `estufa/cor/r` | `72` |
| `estufa/cor/g` | `146` |
| `estufa/cor/b` | `54` |
| `estufa/saude` | `Saudável` |
| `estufa/healthScore` | `91` |

Os tópicos são exemplos e podem ser adaptados conforme a configuração do dispositivo. O reconhecedor também aceita raízes como `planta/...` e `esp32/...`, além de termos em inglês.

O filtro MQTT padrão é `#`, que permite receber todas as mensagens autorizadas pelas credenciais utilizadas. Esse filtro pode ser alterado nas configurações do aplicativo.

##  Como executar

### Pré-requisitos

- Android Studio;
- JDK compatível com a configuração Gradle do projeto;
- dispositivo Android ou emulador com acesso à internet;
- broker configurado no HiveMQ Cloud;
- credenciais MQTT válidas.

### Instalação

**1. Clone o repositório:**

\`\`\`bash
git clone https://github.com/carlosliszt/PlantsIOT.git
\`\`\`

**2. Abra o projeto no Android Studio.**

Abra a pasta `PlantsIOT` e aguarde a sincronização do Gradle.

**3. Configure as propriedades locais.**

No arquivo `local.properties`, localizado na raiz do projeto, configure os dados necessários para a conexão:

\`\`\`properties
MQTT_BROKER=ssl://seu-broker:8883
MQTT_USER=seu-usuario
MQTT_PASS=sua-senha
TREFLE_API_TOKEN=seu-token
\`\`\`

Substitua os valores de exemplo pelas suas próprias credenciais. Os nomes das propriedades devem corresponder aos utilizados na configuração Gradle do projeto.

**4. Execute o aplicativo.**

Selecione o módulo `app` e execute-o em um dispositivo ou emulador Android com acesso à internet.

**5. Verifique a conexão.**

Abra o Dashboard e confirme o estado da conexão MQTT. Em seguida, publique mensagens nos tópicos configurados para visualizar as leituras.

O Gradle utiliza essas propriedades durante a compilação local para configurar o `BuildConfig`.

##  Solução de problemas

| Problema | Possível solução |
|---|---|
| Falha na conexão MQTT | Verifique o endereço do broker, a porta 8883 e as credenciais. |
| Usuário ou senha inválidos | Confira as credenciais configuradas no HiveMQ Cloud. |
| Falta de permissão para o tópico `#` | Utilize um filtro autorizado, como `estufa/#`, de acordo com as permissões do broker. |
| Dashboard sem leituras | Confirme se o dispositivo publica no mesmo cluster e se os tópicos correspondem ao filtro configurado. |
| Mensagem não reconhecida | Verifique o formato do JSON e o nome do tópico. Consulte o cartão de detalhes da leitura para visualizar o tópico de origem e o payload recebido. |

##  Segurança

Para manter o projeto mais seguro:

- Não publique credenciais MQTT ou tokens de API no repositório.
- Mantenha os dados sensíveis em `local.properties` e confirme que esse arquivo não está sendo versionado.
- Utilize um usuário MQTT exclusivo, com permissões mínimas e acesso apenas aos tópicos necessários.
- Evite compartilhar credenciais pessoais em ambientes públicos.
- Antes de distribuir o APK, considere que valores incorporados durante a compilação podem ser extraídos do aplicativo. Para ambientes de produção, avalie utilizar um serviço intermediário para gerenciar o acesso aos dados.

##  Objetivo do projeto

O PlantsIOT tem como objetivo demonstrar a integração entre **desenvolvimento Android, Internet das Coisas (IoT), comunicação MQTT e visualização de dados**.

A proposta é facilitar o acompanhamento de informações relacionadas às plantas por meio de uma aplicação móvel conectada a dispositivos sensores.

---

Desenvolvido com foco em tecnologia, conectividade e monitoramento inteligente de plantas.
