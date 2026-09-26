import json
import time

import paho.mqtt.client as mqtt

BROKER = "3324ab5a5cd44751b6c3aacc57b60320.s1.eu.hivemq.cloud"
PORT = 8883
USERNAME = "alice"
PASSWORD = "123456789"
TOPIC = "planta/esp32cam/analysis"
INTERVAL_SECONDS = 1

readings = [
    {
        "temperatureC": 26.4,
        "airHumidity": 61.0,
        "soilMoisture": 48.0,
        "luminosity": 820.0,
        "ph": 6.7,
        "red": 72,
        "green": 146,
        "blue": 54,
        "heightCm": 18.5,
        "healthScore": 91,
        "healthStatus": "Saudável",
        "greenIndex": 0.78,
        "yellowIndex": 0.22,
        "notes": "Folhas verdes e boa umidade do solo",
    },
    {
        "temperatureC": 29.8,
        "airHumidity": 48.0,
        "soilMoisture": 28.0,
        "luminosity": 970.0,
        "ph": 6.3,
        "red": 130,
        "green": 145,
        "blue": 45,
        "heightCm": 19.1,
        "healthScore": 65,
        "healthStatus": "Atenção",
        "greenIndex": 0.45,
        "yellowIndex": 0.55,
        "notes": "Umidade do solo abaixo do ideal",
    },
    {
        "temperatureC": 34.2,
        "airHumidity": 31.0,
        "soilMoisture": 12.0,
        "luminosity": 1120.0,
        "ph": 5.8,
        "red": 180,
        "green": 75,
        "blue": 35,
        "heightCm": 19.0,
        "healthScore": 30,
        "healthStatus": "Crítico",
        "greenIndex": 0.20,
        "yellowIndex": 0.80,
        "notes": "Folhas amareladas e possível falta de água",
    },
    {
        "temperatureC": 27.5,
        "airHumidity": 56.0,
        "soilMoisture": 42.0,
        "luminosity": 760.0,
        "ph": 6.5,
        "red": 90,
        "green": 125,
        "blue": 48,
        "heightCm": 19.3,
        "healthScore": 65,
        "healthStatus": "Recuperando",
        "greenIndex": 0.60,
        "yellowIndex": 0.40,
        "notes": "Melhora após irrigação",
    },
    {
        "temperatureC": 25.2,
        "airHumidity": 68.0,
        "soilMoisture": 62.0,
        "luminosity": 700.0,
        "ph": 6.8,
        "red": 40,
        "green": 180,
        "blue": 45,
        "heightCm": 20.4,
        "healthScore": 100,
        "healthStatus": "Excelente",
        "greenIndex": 0.90,
        "yellowIndex": 0.10,
        "notes": "Crescimento ideal e folhas muito saudáveis",
    },
]


def build_message(reading):
    timestamp = int(time.time() * 1000)
    data = {
        **reading,
        "timestamp": timestamp,
        "sourceTopic": TOPIC,
        "rawPayload": json.dumps(reading, ensure_ascii=False),
    }
    return json.dumps({"data": data}, ensure_ascii=False)


client = mqtt.Client()
client.username_pw_set(USERNAME, PASSWORD)
client.tls_set()

try:
    client.connect(BROKER, PORT, 60)
    client.loop_start()
    print(f"Conectado ao broker MQTT. Publicando em: {TOPIC}")

    for index, reading in enumerate(readings, start=1):
        message = build_message(reading)
        result = client.publish(TOPIC, message, qos=1)
        result.wait_for_publish(timeout=2)

        if result.rc == mqtt.MQTT_ERR_SUCCESS:
            print(
                f"[{index}/{len(readings)}] "
                f"{reading['healthStatus']} enviado com sucesso"
            )
            print(message)
        else:
            print(f"[{index}/{len(readings)}] Falha ao publicar: código {result.rc}")

        if index < len(readings):
            time.sleep(INTERVAL_SECONDS)

except KeyboardInterrupt:
    print("\nSimulação interrompida pelo usuário.")
except Exception as error:
    print(f"Erro: {error}")
finally:
    client.loop_stop()
    client.disconnect()
    print("Desconectado.")
