## Diagrams

```mermaid
sequenceDiagram

    participant ABC
    link ABC: Dashboard @ https://www.google.com


    box Process Id p1
    participant WebsocketClient    
    end

    box Process Id p2
    participant WebGui
    participant Runtime
    end

    WebsocketClient->>WebGui: ws://localhost:8888/api/messages?user=root&pwd=pwd&session_id=2309adf3dlkdk&id=p1

    opt Server Add Listener, Describe and Reserve
    WebGui->>WebsocketClient: {"topicMethod"<br/>:"describe","callbackName":"runtime@caring-hector","callbackMethod":"onDescribe","class":"org.myrobotlab.framework.MRLListener"} 
    WebGui->>WebsocketClient: message name:runtime method:addListener
    Note left of WebGui: runtime<br/>addListener<br/>describe  [Duck Duck Go](https://duckduckgo.com)
    end

    WebGui->> Runtime: runtime<br/>addListener<br/>describe

    Alice->>John: Hello John, how are you?
    loop Healthcheck
        John->>John: Fight against hypochondria
    end
    links Alice: {"Dashboard": "https://dashboard.contoso.com/alice", "Wiki": "https://wiki.contoso.com/alice"}
    Note right of John: Rational thoughts <br/>prevail!
    John-->>Alice: Great!
    John->>Bob: How about you?
    Bob-->>John: Jolly good!
```
```json
{
  "msgId": 1690145377501,
  "name": "runtime",
  "sender": "webgui@caring-hector",
  "sendingMethod": "",
  "historyList": [],
  "properties": null,
  "status": null,
  "encoding": "json",
  "method": "addListener",
  "data": [
    "{\"topicMethod\":\"describe\",\"callbackName\":\"runtime@caring-hector\",\"callbackMethod\":\"onDescribe\",\"class\":\"org.myrobotlab.framework.MRLListener\"}"
  ],
  "class": "org.myrobotlab.framework.Message"
}

```


```mermaid
graph TD;
    A-->B;
    A-->C;
    B-->D;
    C-->D;
```