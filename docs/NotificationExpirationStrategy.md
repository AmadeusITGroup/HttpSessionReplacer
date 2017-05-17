# Notification expiration strategy

Copy paste the code below in the following website: https://www.websequencediagrams.com/

```sh
title Notification expiration strategy

Event->JavaClient: Create session
JavaClient->sessionId: Create session
activate sessionId

note right of JavaClient: 
  maxInactiveInterval = 1800s     
  maxInactiveIntervalDelay =1800s + 5 min = 2100 
end note

JavaClient->sessionId: EXPIRE sessionId 2100

JavaClient->expireSessionId: SETEX expire:SessionId 1800 ""
activate expireSessionId

note right of JavaClient: 
  exp=unixTime+maxInactiveInterval=1439245070 
  expDelay = exp + 5min = 1439245370  
end note

JavaClient->expirations1439245070: SADD expirations:1439245070 sessionId
activate expirations1439245070
JavaClient->expirations1439245070: EXPIREAT expirations:1439245070 1439245370



Event->JavaClient: Access session
JavaClient->expirations1439245070:SREM expirations:1439245070 sessionId
destroy expirations1439245070

JavaClient->expirations1439245080:SADD expirations:1439245080 sessionId
activate expirations1439245080

JavaClient->expirations1439245080: EXPIREAT expirations:1439245080 143924538

JavaClient->sessionId: EXPIRE sessionId 2100
JavaClient->expireSessionId: SETEX expire:SessionId 1800 



alt 1439245080 time has been reached
	note over Event: 
		Redis has not detected the expiration
		We notify him manually
	end note

	Event->JavaClient: 1439245080 time reached
	JavaClient->expirations1439245080: SPOP expirations:1439245080
	expirations1439245080-->JavaClient: sessionId

	JavaClient->sessionId: EXISTS sessionId
	note over sessionId: Redis detects that the session has expired
	sessionId->JavaClient: Session expired notification

else Redis detects the expiration
	expireSessionId->JavaClient: Session expired notification
end

note over JavaClient: Cleaning process

JavaClient-> sessionId: DEL sessionId
destroy sessionId

JavaClient->expireSessionId: DEL expire:SessionId
destroy expireSessionId
```
