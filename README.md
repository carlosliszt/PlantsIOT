# PlantsIOT — monitoramento pelo HiveMQ

Aplicativo Android em Kotlin para acompanhar uma planta em tempo real. Esta versão foi preparada para receber dados do HiveMQ Cloud por MQTT com TLS na porta 8883, exibi-los no dashboard e manter um histórico local no aparelho.

## Dados exibidos

- temperatura do ar;
- umidade do ar;
- umidade do solo;
- luminosidade;
- pH;
- cor RGB (R, G e B), incluindo uma amostra visual da cor;
- situação de saúde e pontuação de 0 a 100;
- altura, observações, tópico de origem e payload bruto;
- estado da conexão e horário da última atualização.

O app aceita tanto um JSON completo quanto valores enviados em tópicos separados. O filtro MQTT padrão é `#`, portanto todas as mensagens permitidas para o usuário do HiveMQ são recebidas. O filtro pode ser alterado em **Configurações**.

## Formato 1 — JSON completo

Pode ser publicado, por exemplo, em `planta/esp32cam/analysis` ou `estufa/dados`:

```json
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
```

Também são aceitos nomes equivalentes em inglês, como `temperature`, `airHumidity`, `soilMoisture`, `luminosity`, `healthStatus`, `red`, `green` e `blue`.

## Formato 2 — tópicos separados

| Tópico sugerido | Exemplo de payload |
| --- | --- |
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

O reconhecedor usa o nome do tópico, portanto também entende raízes como `planta/...` e `esp32/...`, além de termos em inglês.

Se a saúde e a pontuação não forem enviadas, o app gera uma indicação automática baseada na predominância RGB. Essa indicação é apenas orientativa e não substitui uma análise agronômica.

## Como executar

1. Abra a pasta `PlantsIOT-dev` no Android Studio.
2. Aguarde a sincronização do Gradle.
3. Use um aparelho ou emulador Android com internet.
4. Execute o módulo `app`.
5. Na tela inicial, abra **Dashboard**.
6. Confirme a mensagem verde `Conectado • tópico #`.
7. Publique as leituras no HiveMQ.

O broker, usuário e senha fornecidos para esta versão já estão configurados em `app/build.gradle.kts`.

## Diagnóstico rápido

- **Usuário ou senha inválidos:** revise as credenciais do HiveMQ Cloud.
- **Sem permissão para #:** libere a assinatura desse filtro no HiveMQ ou troque em Configurações por um filtro autorizado, por exemplo `estufa/#`.
- **Conectado, mas sem valores:** confirme que o ESP32 publica nesse mesmo cluster e que o payload corresponde a um dos formatos acima.
- **Valor chega, mas não é identificado:** o tópico e o payload ainda aparecem no cartão “Detalhes da leitura”, facilitando a correção.

## Observação de segurança

As credenciais estão incorporadas ao aplicativo para facilitar o teste educacional solicitado. Antes de distribuir publicamente o APK ou publicar o código, crie um usuário MQTT exclusivo com permissões mínimas e troque a senha.

## Tecnologias

- Kotlin e Android XML;
- Eclipse Paho MQTT 3.1.1;
- HiveMQ Cloud com TLS;
- MPAndroidChart;
- `SharedPreferences` para histórico local de até 200 leituras.
