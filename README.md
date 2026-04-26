# Escrow Event-Driven Microservices Platform

## Overview

This project is an escrow-based platform designed to protect buyers and sellers during online transactions. It ensures that funds are securely held until goods are delivered and verified, reducing fraud and increasing trust.

The system is built using an event-driven microservices architecture with Java Spring Boot and Spring Cloud.


## How It Works

1. **User Registration**
   Buyers and sellers sign up and create accounts.

2. **Order Creation**
   Either party creates an escrow order with item details.

3. **Order Approval**
   The other party reviews and approves the order to confirm agreement.

4. **Payment (Escrow Funding)**
   The buyer deposits funds (item cost + delivery fee) into their in-app wallet.
   Funds are locked until the transaction is completed.

5. **Driver Assignment**
   The system finds the nearest available driver and notifies them.

6. **Delivery Process**

   * Driver accepts the request
   * Buyer and seller receive driver details
   * Item is picked up and delivered

7. **Post-Delivery Actions**

   * Buyer has 1 hour to:

     * Raise a complaint, or
     * Release payment
   * If no action is taken, funds are automatically released to the seller

8. **AI Support**
   Users can interact with an AI agent to:

   * Track orders
   * Ask questions
   * Raise issues


## Microservices

* **Authentication Service** – Handles user authentication and JWT issuance
* **AI Service** – AI agent for support and interaction
* **Customer Service** – Manages customer profiles
* **Driver Service** – Manages driver profiles and activities
* **Email Service** – Sends transactional emails
* **Eureka Server** – Service discovery
* **Gateway Server** – API entry point
* **Notification Service** – Push notifications and alerts
* **Order Service** – Order creation and lifecycle management
* **Payment Service** – Handles payments and transactions
* **Wallet Service** – Manages user wallets and escrow funds


## Tech Stack

* **Backend:** Java, Spring Boot, Spring Cloud
* **Communication:** Kafka, gRPC
* **Database:** PostgreSQL
* **Caching & Realtime:** Redis, WebSockets
* **Authentication:** JWT (RSA keys), OAuth2 Resource Server
* **Service Discovery:** Eureka
* **API Gateway:** Spring Cloud Gateway
* **Resilience:** Resilience4j (Circuit Breakers, Fallbacks)


## Key Features

* Event-driven architecture using Kafka
* Escrow payment system with fund locking
* Real-time driver tracking
* AI-powered customer support
* Idempotency handling to prevent race conditions
* Outbox pattern for reliable event publishing
* Fault tolerance and service resilience


## External Integrations

* Payment processing (Paystack)
* Maps and geolocation (Google Maps API)
* Push notifications (Firebase Cloud Messaging)
* File storage (Azure Blob Storage)
* Email delivery (Java Mail Sender)

## Observability & Monitoring

* Spring Actuator for health checks and metrics

## Goal

To provide a secure, scalable, and reliable platform that eliminates fraud in online transactions by combining escrow payments, verified delivery, and intelligent automation.
