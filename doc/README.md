# Network Distributed Architecture

## Websockets - Default Response for New Connection

```mermaid
sequenceDiagram
    autonumber
    box Process Id p1
    participant runtime@p1   
    end

    box Process Id p2
    participant webgui@p2
    participant runtime@p2
    end

    Note right of runtime@p1: Client runtime@p1 opens a <br/>websocket to remote webgui

    runtime@p1->>webgui@p2: Connect<br/>ws://localhost:8888/api/messages?user=root&pwd=pwd&session_id=2309adf3dlkdk&id=p1

    Note left of webgui@p2: Remote webgui@p2 and runtime@p2 attempt to subscribe <br/> to the describe method of the runtime@p1
    webgui@p2->>runtime@p1: addListener describe
    Note left of webgui@p2: runtime@p2 sends a describe request to runtime@p1
    webgui@p2->>runtime@p1: describe 
    Note right of runtime@p1: runtime@p1 responds with a describe response
    runtime@p1->>webgui@p2: onDescribe 
    Note left of webgui@p2: Based on the results of the describe,<br/> more querying and subscriptions can be processed


    %% opt Server Add Listener, Describe and Reserve
    %% end


```
### 1 Connection
A connection with a websocket starts with an HTTP GET 
```
ws://localhost:8888/api/messages?user=root&pwd=pwd&session_id=2309adf3dlkdk&id=p1
```
### 2 Subscribe to Runtime Describe
When a connection is established between two different myrobotlab instances p1 and p2,
the following messages are sent.  The first message is adding a subscription to the runtime of the
other process.  The subscription is for the function "describe".  The second message will be a describe request.
```json
{
  "msgId": 1690145377501,
  "name": "runtime",
  "sender": "webgui@p2",
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
### 3 Send Describe
```json
{
  "msgId": 1690145377501,
  "name": "runtime",
  "sender": "webgui@p2",
  "sendingMethod": "",
  "historyList": [],
  "properties": null,
  "status": null,
  "encoding": "json",
  "method": "describe",
  "data": [
    "\"fill-uuid\"",
    "{\"id\":\"caring-hector\",\"uuid\":\"383b4070-2848-4c3d-85f4-e7f6e081d18e\",\"platform\":{\"os\":\"linux\",\"arch\":\"x86\",\"osBitness\":64,\"jvmBitness\":64,\"lang\":\"java\",\"vmName\":\"OpenJDK 64-Bit Server VM\",\"vmVersion\":\"11\",\"mrlVersion\":\"unknownVersion\",\"isVirtual\":false,\"id\":\"caring-hector\",\"branch\":\"develop\",\"pid\":\"1500044\",\"hostname\":\"t14-gperry\",\"commit\":\"55d0163663825dd0aaa10568bc01e035c7f21532\",\"build\":null,\"motd\":\"resistance is futile, we have cookies and robots ...\",\"startTime\":1690135873670,\"manifest\":{\"git.branch\":\"develop\",\"git.build.host\":\"t14-gperry\",\"git.build.time\":\"2023-07-23T10:38:46-0700\",\"git.build.user.email\":\"grog@myrobotlab.org\",\"git.build.user.name\":\"grog\",\"git.build.version\":\"0.0.1-SNAPSHOT\",\"git.closest.tag.commit.count\":\"13447\",\"git.closest.tag.name\":\"1.0.119\",\"git.commit.author.time\":\"2023-07-22T20:15:51-0700\",\"git.commit.committer.time\":\"2023-07-22T20:15:51-0700\",\"git.commit.id\":\"55d0163663825dd0aaa10568bc01e035c7f21532\",\"git.commit.id.abbrev\":\"55d0163\",\"git.commit.id.describe\":\"1.0.119-13447-g55d0163\",\"git.commit.id.describe-short\":\"1.0.119-13447\",\"git.commit.message.full\":\"Cron enhanced 2 (#1318)\\n\\n* Improved Cron and Cron history\\r\\n\\r\\n* forgot one\\r\\n\\r\\n* Teamwork fix of Hd44780\\r\\n\\r\\n* updated from review\",\"git.commit.message.short\":\"Cron enhanced 2 (#1318)\",\"git.commit.time\":\"2023-07-22T20:15:51-0700\",\"git.commit.user.email\":\"grog@myrobotlab.org\",\"git.commit.user.name\":\"GroG\",\"git.dirty\":\"false\",\"git.local.branch.ahead\":\"0\",\"git.local.branch.behind\":\"0\",\"git.remote.origin.url\":\"git@github.com-myrobotlab:MyRobotLab/myrobotlab.git\",\"git.tags\":\"1.1.1194\",\"git.total.commit.count\":\"14104\"},\"shortCommit\":\"55d0163\",\"class\":\"org.myrobotlab.framework.Platform\"},\"class\":\"org.myrobotlab.framework.DescribeQuery\"}"
  ],
  "class": "org.myrobotlab.framework.Message"
}

```

### 4 Process onDescribe Response
The response back will include serialized services which should be refactor to be more minimal, and more describe parameters which can return interfaces or methods
