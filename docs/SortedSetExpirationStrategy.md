# Sorted set expiration strategy

Copy paste the code below in the following website: https://www.websequencediagrams.com/

```sh
title Sorted set expiration strategy

Event->JavaClient: Create session
JavaClient->sessionId: Create session
activate sessionId

note right of JavaClient: 
  maxInactiveInterval = 1800s
  maxInactiveIntervalDelay =1800s + 5 min = 2100s 
end note

JavaClient->sessionId: EXPIRE sessionId 2100

JavaClient->all-sessions-set: ZADD all-sessions-set sessionId:node now()+maxInactiveInterval
activate all-sessions-set



Event->JavaClient: Access session

JavaClient->all-sessions-set: ZADD all-sessions-set sessionId:node now()+maxInactiveInterval
JavaClient->sessionId: EXPIRE sessionId 2100



alt every 60 s

	Event->JavaClient: run expire task
	JavaClient->all-sessions-set: ZRANGEBYSCORE all-sessions-set now()-5min now()
	note over JavaClient: If node == this node
	JavaClient->all-sessions-set: ZREM all-sessions-set sessionId

	note over JavaClient: If ZREM was successful
	JavaClient->JavaClient: delete sessionId
	note over JavaClient: Retrieve old sessions
	JavaClient->all-sessions-set: ZRANGEBYSCORE all-sessions-set 0 now()-5min
	JavaClient->all-sessions-set: ZREM all-sessions-set sessionId

	note over JavaClient: If ZREM was successful
	JavaClient->JavaClient: delete sessionId

end

note over JavaClient: delete sessionId process

JavaClient-> sessionId: DEL sessionId
destroy sessionId

JavaClient->expire: ZREM all-sessions-set sessionId
```
