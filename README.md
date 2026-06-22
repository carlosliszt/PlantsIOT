# 🌱 PlantsIOT

Sistema de monitoramento inteligente de plantas utilizando Internet das Coisas (IoT), com integração entre hardware e aplicativo mobile.

---

## 📌 Sobre o Projeto

O **PlantsIOT** é uma solução tecnológica desenvolvida com o objetivo de **monitorar a saúde de plantas em tempo real**, utilizando sensores e comunicação entre dispositivos.

O projeto surge como resposta ao problema da **cegueira botânica**, que representa a dificuldade das pessoas em reconhecer a importância das plantas no cotidiano e no equilíbrio ambiental.

Além disso, considerando a importância da **fitopatologia** no agronegócio, o sistema busca facilitar o acompanhamento de fatores que influenciam diretamente o desenvolvimento vegetal.

---

## 🎯 Objetivo

Desenvolver uma solução acessível que permita:

* Monitorar condições ambientais das plantas 🌡️
* Auxiliar na prevenção de problemas no cultivo 🌱
* Democratizar o acesso à tecnologia no cuidado vegetal
* Integrar hardware e aplicativo mobile em tempo real

---

## 🚀 Funcionalidades

* 📊 Monitoramento em tempo real de:

  * Temperatura
  * Umidade do ar
  * Umidade do solo
  * Luminosidade
  * pH do solo

* 📱 Aplicativo mobile com:

  * Dashboard de visualização de dados
  * Histórico de medições
  * Cadastro de plantas
  * Definição de parâmetros ideais

* 🔗 Comunicação em tempo real via protocolo MQTT

* ☁️ Armazenamento em nuvem

---

## 🛠️ Tecnologias Utilizadas

### 📱 Mobile

* Kotlin
* Android Studio
* XML (interface gráfica)

### ☁️ Backend / Dados

* Firebase Realtime Database

### 🌐 Comunicação

* Protocolo MQTT
* HiveMQ (broker)

### 🔌 Hardware

* ESP32-CAM
* Sensores:

  * Umidade do solo
  * Temperatura
  * Luminosidade
  * pH do solo

---

## ⚙️ Como Funciona

1. Os sensores coletam dados ambientais da planta
2. O ESP32 envia os dados via MQTT
3. O broker (HiveMQ) intermedia a comunicação
4. O aplicativo recebe e exibe os dados em tempo real
5. Os dados são armazenados no Firebase para histórico

---

## ▶️ Como Executar

### 🔧 Pré-requisitos

* Android Studio instalado
* Dispositivo Android ou emulador
* ESP32 configurado
* Conexão com internet

---

### 📥 Clonar o repositório

```bash
git clone https://github.com/carlosliszt/PlantsIOT.git
cd PlantsIOT
```

---

### 📱 Executar o App

1. Abra o projeto no Android Studio
2. Aguarde o download das dependências
3. Execute em um emulador ou dispositivo físico

---

### 🔌 Configurar o Hardware

* Conectar sensores ao ESP32
* Configurar Wi-Fi
* Definir broker MQTT (HiveMQ)
* Iniciar envio de dados

---

## 📊 Diferencial do Projeto

Diferente de aplicativos como:

* Plantix
* Plantum
* Agrio

O **PlantsIOT** busca ser:

* 💸 Gratuito
* 📚 Educacional
* 🌱 Acessível para estudantes e pequenos produtores

---

## 🌍 Impacto

O projeto contribui para:

* Aumento da consciência ambiental
* Apoio ao cultivo doméstico e agrícola
* Integração entre tecnologia e natureza

---

## 👨‍💻 Autores

* Carlos Miguel
* Cauã Macedo
* Mario Rodrigues

---

## 📄 Referências

* AMORIM et al. Manual de Fitopatologia (2016)
* GASPAROTTO et al. Glossário de Fitopatologia (2023)
* Google Firebase Documentation (2025)
* RESENDE, Kotlin com Android (2024)
* SILVA, Desenvolvimento Android (2023)

---

## 📌 Status do Projeto

🚧 Em desenvolvimento

---

## 📜 Licença

Este projeto pode ser utilizado para fins educacionais.
