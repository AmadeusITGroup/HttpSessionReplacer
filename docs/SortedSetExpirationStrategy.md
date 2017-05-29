# Sorted set expiration strategy

![Sorted set expiration strategy sequence diagram](https://www.websequencediagrams.com/cgi-bin/cdraw?lz=dGl0bGUgU29ydGVkIHNldCBleHBpcmF0aW9uIHN0cmF0ZWd5CgpFdmVudC0-SmF2YUNsaWVudDogQ3JlYXRlIHNlc3Npb24KABEKLT4ADQdJZAAVEWFjdGl2ACwLSWQKCm5vdGUgcmlnaHQgb2YgAFcMCiAgbWF4SW4AMQVlSW50ZXJ2YWwgPSAxODAwcwAIFkRlbGF5ID0AHQUgKyA1IG1pbiA9IDIxMDBzIAplbmQgbm90ZQoAgRgYRVhQSVJFAIEVCgA0BQAgDmFsbC0AgW8Hcy1zZXQ6IFpBREQgAAcQAIFZCjpub2RlIG5vdygpKwCBPRMAggsKAEoQCgoAgmUVQWNjZXNzAIJxCQA_XgCBei4KCmFsdCBldmVyeSA2MCBzCgoJAIQoE3J1bgCEYQZlIHRhc2sKCQCCQB9SQU5HRUJZU0NPUkUAglYSAIJUBS01bWluAIJeBgoJAIRNBW92ZXIAhEENSWYgAIMEBT09IHRoaXMACAUAWSJFTQCDPxsKAFEbADYFd2FzIHN1AIMrBWZ1bACBWw4AhkEMZGVsZQCGCQ0AgS0XUmV0cmlldmUgb2xkAIZzCHMAggE_MACCNwsAd4EZCmVuAIgjCACDSBEAgi0QIHByb2Nlc3MAh1cOAIcFCyBERUwAiHcLZGVzdHJveQCJCAwAiUIMAIUkBgCDayE&s=modern-blue)

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
