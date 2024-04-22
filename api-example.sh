curl -X POST http://localhost:8888/api/service \
     -d '{"name":"i01.chatBot","method":"getResponse","data":["grog", "hi there!"]}'

curl -X POST http://localhost:8888/api/service/i01.chatBot/getResponse \
     -d '["grog", "hi there!"]'

curl -X POST http://localhost:8888/api/service/i01.chatBot/setCurrentBotName \
     -d '["ru-RU"]'



