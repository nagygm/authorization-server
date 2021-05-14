# Introduction
Authentication and authorization server with spring reactive stack
The server uses kotlin coroutines to process web requests.

# Starting in docker
cd server
docker-compose -p authserver up -d

# Server
Running unit tests
```gradle clean test```
                    
Runnint integrationTests
```gradle clean integrationTest```

//TODO token endpoint auth code
// check consent, check auth code
// create access token with claims (scopes issuer etc)
// add provider properties for token creation

//TODO add introspect endpoint
// create intropsect handler
// validate access token if it was revoked or not and if it is valid

//todo add user registration, userdetails service
//todo add revoke token endpoint
