# ETHOS Backend - Spring Boot

## System Context

ETHOS is a multi-repository system, where each component is independently deployable but designed to work together as a single ethical decision platform. This repository contains the backend orchestration service.

## Related Repositories

- **ETHOS Frontend** – User interface for package submission, monitoring, and analytics  
  🔗 https://github.com/gowtham-m-2005/ethos-frontend

- **ETHOS Rule Engine** – External ethical evaluation service implemented using rule-based logic  
  🔗 https://github.com/gowtham-m-2005/ethos-python

## Role of This Repository in the System

This backend acts as the central coordinator between the user-facing frontend and the ethical rule engine.

### Responsibilities include:

- Receiving requests from the frontend
- Delegating ethical evaluation to the rule engine
- Persisting evaluated results
- Maintaining dynamic priority rankings
- Exposing distribution and analytics APIs

The backend does not embed ethical rules directly, ensuring:

- Loose coupling between services
- Independent evolution of the rule engine
- Clear separation between decision logic and orchestration

## System Interaction Flow

1. Frontend submits package data to the backend
2. Backend forwards relevant details to the rule engine
3. Rule engine returns an ethical evaluation
4. Backend persists results to database and recalculates priorities each time a package gets submitted
5. Frontend retrieves ranked packages and analytics via REST APIs

## Deployment Note

Each component can be deployed independently and communicates over HTTP, enabling flexible scaling and future replacement of individual services without impacting the overall system architecture.
