import json
import time
import paho.mqtt.client as mqtt #paho-mqtt

BROKER = "3324ab5a5cd44751b6c3aacc57b60320.s1.eu.hivemq.cloud"
PORTA = 8883
USUARIO = "alice"
SENHA = "123456789"
TOPICO = "planta/esp32cam/analysis"

client = mqtt.Client()
client.username_pw_set(USUARIO, SENHA)
client.tls_set()  # TLS/SSL

payload = {
    "data": {
        "temperatureC": 26.4,
        "airHumidity": 61.0,
        "soilMoisture": 48.0,
        "luminosity": 820,
        "ph": 6.7,
        "rgb": {
            "r": 72,
            "g": 146,
            "b": 54
        },
        "healthStatus": "Saudável",
        "healthScore": 91,
        "heightCm": 18.5,
        "notes": "Folhas com boa predominância de verde"
    }
}

try:
    client.connect(BROKER, PORTA, 60)
    print("Conectado ao broker MQTT.")
    for i in range(3):
        msg = json.dumps(payload)
        client.publish(TOPICO, msg)
        print(f"Mensagem {i + 1} enviada para {TOPICO}:")
        print(msg)
        time.sleep(2)
    client.disconnect()
    print("Desconectado.")
except Exception as e:
    print("Erro:", e)