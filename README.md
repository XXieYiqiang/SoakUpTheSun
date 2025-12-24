# ☀️ SoakUpTheSun - Cloud-Based Visual Assistance Platform (WIP)

<div align="center">

[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-Alibaba-blue?style=for-the-badge&logo=spring)](https://spring.io/)
[![Go](https://img.shields.io/badge/Go-1.18+-00ADD8?style=for-the-badge&logo=go)](https://go.dev/)
[![Vue.js](https://img.shields.io/badge/Vue.js-2.x-4FC08D?style=for-the-badge&logo=vue.js)](https://vuejs.org/)
[![Python](https://img.shields.io/badge/Python-AI-yellow?style=for-the-badge&logo=python)](https://www.python.org/)

[![Redis](https://img.shields.io/badge/Redis-Redisson-red?logo=redis)](https://redis.io/)
[![RocketMQ](https://img.shields.io/badge/RocketMQ-Messaging-orange?logo=apache)](https://rocketmq.apache.org/)
[![Elasticsearch](https://img.shields.io/badge/Elasticsearch-Search-green?logo=elasticsearch)](https://www.elastic.co/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)

</div>

<div align="center">
  <strong>
    <a href="README.md">🇺🇸 English</a> | 
    <a href="README_CN.md">🇨🇳 中文说明</a>
  </strong>
</div>

---

> **"Be their eyes, Online."**

**SoakUpTheSun** is a high-concurrency heterogeneous microservices platform based on **Java + Go + Python**. We have abandoned the traditional offline rescue model and built a purely online assistance system based on **Go SFU Real-time Streaming** + **Redis Hot Pool Matching**.

By integrating **RocketMQ Asynchronous Decoupling**, **Lua Atomic Locks**, and **AI Visual Analysis** technologies, we allow visually impaired users to connect with global volunteers in milliseconds with just a tap on the screen, transcending geographical limitations.

**Currently, this is a learning-oriented project. You can learn solutions for common scenarios such as flash sales (seckill), mass distribution, cache breakdown, cache penetration, instant messaging (IM), matching algorithms, and self-deployed AI model usage.**

**Due to space limitations, detailed implementation diagrams and functional charts for each feature cannot be fully displayed here. Please contact the email at the end of this document, and I will send you the complete architecture diagrams.**

## 🏗 System Topology

The system adopts a heterogeneous microservices architecture with a fully asynchronous core link design.

![System Architecture](readmeImg/img.png)

> **Diagram Explanation**: The system is divided into the Java Business Middle Platform, the Go Streaming Layer, and the Python AI Computing Layer. Core interactions are scheduled via the Redis Hot Pool, utilizing RocketMQ for traffic peak shaving.

## 🚀 Key Features

- **⚡️ Millisecond-Level Hot Pool Matching**
  Utilizes `Redis Set` to maintain a real-time pool of online volunteers. Unlike traditional database queries, we only match users who actively `activate` their status. Combined with an Elasticsearch fallback strategy, this ensures a connection success rate of > 99%.

- **🎥 Self-Developed Go SFU Streaming Service**
  A deeply customized SFU server based on the Pion framework, supporting WebRTC signaling interaction and RTP packet forwarding. Optimized for weak network environments, keeping end-to-end latency within 200ms.

- **🛡️ High-Concurrency Short Link Defense System**
  Integrates **Bloom Filter** and **Redis Token Bucket**. Performs O(1) extreme-speed deduplication before generating 6-digit short codes, effectively preventing ID collisions and malicious traversal attacks.

- **🔄 Asynchronous Slicing Import for Massive Data**
  Adopts a **TaskQueue + Async Thread Pool** solution to handle Excel imports of 100,000+ rows. Uses Mybatis-Plus for batch insertion and dual-writing to ES, avoiding long transactions that could lock the database.

- **💰 Redis Lua Atomic Inventory Flash Sale**
  In public welfare prize redemption scenarios, "inventory query" and "inventory deduction" are encapsulated into a single Lua script, executed atomically in the Redis single-threaded model to completely eliminate overselling.

## 🛠 Tech Stack

| Domain | Technology Selection | Core Function |
| :--- | :--- | :--- |
| **Service Governance** | Spring Cloud Alibaba | Nacos (Registration/Config), OpenFeign (RPC) |
| **Audio/Video** | **Go (Golang) + Pion** | WebRTC Signaling Exchange, RTP Media Forwarding |
| **AI Computing** | Python + OpenCV | Computer Vision Analysis (OCR/Object Detection) |
| **Persistence** | MySQL 8.0 | Business Data Storage |
| **Cache/Lock** | Redis (Redisson) | Distributed Lock, **Hot Pool (Set)**, **Bloom Filter**, Lua Script |
| **Search** | Elasticsearch 7.x | Multidimensional Volunteer Matching (Painless Script), Inverted Index |
| **Message Queue** | RocketMQ | Image Analysis Peak Shaving, Async Decoupling, Excel Task Distribution |
| **Task Scheduling** | XXL-Job | Distributed Scheduled Tasks (Point Settlement) |
| **Object Storage** | Tencent COS | Massive Storage for Images/Video Files |

## 🧩 Core Design Challenges & Solutions

### 🔴 Challenge 1: "Overselling" & "Collision" Under High Concurrency
**Scenario**: In emergency call mode, multiple visually impaired users might match with the same online volunteer simultaneously; or during a prize flash sale, inventory might drop below zero.
**Solution**:
* **Atomic Guarantee**: Abandoning Java-level locks, we encapsulate "query state" and "lock state" into **Redis Lua scripts**, leveraging Redis's single-threaded nature for atomic execution.
* **Distributed Lock Fallback**: After successful Lua execution, a **Redisson Watchdog** mechanism is introduced to prevent lock anomalies caused by business logic timeouts, ensuring strong data consistency.

### 🟡 Challenge 2: Balancing Security & Performance in Short Link Systems
**Scenario**: Video room links are only 6 characters long, making them vulnerable to brute-force traversal or ID collisions.
**Solution**:
* **Extreme Speed Deduplication**: Introduce **Bloom Filter** to perform O(1) memory-level deduplication before generating short codes, blocking 99% of invalid database queries.
* **Traffic Cleaning**: Combined with the **Redis Token Bucket Algorithm** to limit request rates per IP, preventing malicious scanning.

### 🔵 Challenge 3: OOM in Full Settlement of Massive Point Data
**Scenario**: The platform generates millions of review logs daily. Loading all of them into memory for point calculation would cause frequent Full GC or even OOM in the JVM.
**Solution**:
* **Streaming Processing**: Abandon `LIMIT offset` and design a **Cursor Pagination** mechanism based on primary key IDs, ensuring query performance does not degrade as data volume increases.
* **Memory Aggregation**: Use Java 8 Stream API to perform small-batch aggregation calculations at the application layer, merging 2000 database updates into a single batch submission, significantly reducing I/O overhead.

## 📂 Detailed Directory Structure

```bash

SoakUpTheSun/
├── 📂 clientService/               # [Frontend] Vue.js Client
│   ├── public/                     # Static Resources (Markdown CSS, Fonts)
│   ├── src/
│   │   ├── api/                    # Axios Interface Encapsulation (user.js)
│   │   ├── assets/                 # Assets (styles, images)
│   │   ├── libs/                   # Global Tools (Map, WebSocket Plugins)
│   │   ├── store/                  # Vuex State Management (User, MessageDispatcher)
│   │   ├── utils/                  # Core Utilities (auth.js, request.js)
│   │   └── views/                  # Page Views (ChatRoom, JoinRoom, UserHome)
│   └── vue.config.js               # Frontend Proxy & Build Config
│
├── 📂 sfu/                         # [Streaming] Self-developed Go SFU Server (WebRTC)
│   ├── internal/
│   │   ├── api/room/               # Room Management API (Join, Close, Save)
│   │   ├── cache/                  # Redis Cache Operation Wrapper
│   │   ├── config/                 # System Config (JWT, MySQL, Log)
│   │   ├── ws/                     # WebSocket Signaling (Core Handshake Logic)
│   │   └── app/                    # Application Startup Lifecycle Management
│   ├── utils/                      # Toolkit (Base58, JWT, Encrypt)
│   ├── config.yaml                 # Media Ports & ICE Configuration
│   └── main.go                     # Go Program Entry
│
├── 📂 volunteer/                   # [Backend] Volunteer Core Business Module (The Core)
│   ├── src/main/java/org/hgc/suts/volunteer/
│   │   ├── common/
│   │   │   ├── manager/            # Redis Manager
│   │   │   └── scheduledTask/      # XXL-Job Scheduled Task (Point Settlement)
│   │   ├── controller/             # Web Interface Layer (Facade Pattern)
│   │   ├── facade/                 # Facade Pattern Encapsulating Complex Business
│   │   ├── mq/                     # RocketMQ Message Driver
│   │   │   ├── consumer/           # Consumers (EasyExcel Listener, Task Execution)
│   │   │   │   └── easyExcel/      # ⚡️ Async Slicing Import (ReadExcelDistributionListener)
│   │   │   ├── event/              # Domain Event Objects
│   │   │   └── producer/           # Message Producers
│   │   └── service/
│   │       ├── impl/               # Business Logic Implementation
│   │       │   ├── VolunteerMatchServiceImpl.java  # 🚀 Hybrid Dual-Track Matching Strategy
│   │       │   └── VolunteerPrizesServiceImpl.java # Prize Redemption Logic
│   └── src/main/resources/
│       ├── lua/                    # ⚡️ Redis Atomic Script Library
│       │   ├── redeem_volunteer_prizes_stock_synchronize.lua # Inventory Deduction
│       │   └── volunteer_active_match.lua                    # Hot Pool Matching
│       └── scripts/
│           └── volunteer_match.painless  # 🔍 ES Dynamic Weight Scoring Script
│
├── 📂 picture/                     # [Backend] Image Processing & AI Integration Service
│   ├── src/main/java/org/hgc/suts/picture/
│   │   ├── common/tensentCos/      # Tencent Cloud COS Object Storage Wrapper
│   │   ├── mq/consumer/            # Listeners for Image Upload/Video Frame Analysis
│   │   └── ws/                     # AI Real-time Analysis WebSocket Channel
│
├── 📂 user/                        # [Backend] User Authentication & Authorization Center
│   ├── src/main/java/org/hgc/suts/user/
│   │   ├── common/biz/user/        # UserContext Context & Interceptors
│   │   └── remote/                 # OpenFeign Remote Call Clients
│
├── 📂 shortlink/                   # [Backend] Short Link Generation Service
│   └── src/main/java/org/hgc/suts/shortlink/
│       ├── config/                 # 🛡️ Bloom Filter Configuration (RBloomFilter)
│       └── service/impl/           # Short Code Generation & 302 Redirect Logic
│
├── 📂 docs/                        # Project Documentation Resources
└── pom.xml                         # Maven Parent Project Dependency Management
```

## 🚀 Quick Start

### Requirements
* JDK 17+ / Go 1.25+
* MySQL 8.0 / Redis 5.0+ / RocketMQ 5+
* Nacos 2.0+

### Deployment Steps

**1. Infrastructure Startup**
```bash

docker-compose up -d mysql redis rocketmq nacos elasticsearch
cd clientService && npm install && npm run serve
---
```
## 🤝 Contribution & Contact

**SoakUpTheSun** is a technology-driven public welfare project. If you are interested in **Accessibility Design** or **High Availability Architecture**, feel free to submit a PR.

* **Author:** XXieYiqiang
* **Email:** a1103938364@gmail.com

---
*If you like this project, please give it a star! ⭐️*