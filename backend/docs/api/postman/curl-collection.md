# Guia de Requisições e Testes — DeliveryTracker API

Este diretório contém a coleção oficial de testes manuais da API (`http://localhost:8080/api/v1`).

---

## 1. Importação da Coleção no Postman / Insomnia

A coleção completa já exportada encontra-se no ficheiro:
`Delivery Tacking API.postman_collection.json`

### Passos para Importar:

1. Abra o **Postman** ou **Insomnia**.
2. Clique no botão **Import** (canto superior esquerdo).
3. Selecione o ficheiro `Delivery Tacking API.postman_collection.json` presente nesta pasta.
4. Todos os endpoints, cabeçalhos e payloads serão carregados automaticamente (com preenchimento automático de `{{jwt_token}}` no Login e `{{pedido_id}}` na criação do pedido).

---
