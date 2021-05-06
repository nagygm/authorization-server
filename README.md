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