
# 🟡 Ranked4 — Puissance 4 en Microservices

**Ranked4** est un projet de portfolio visant à créer un **vrai jeu complet multijoueur en temps réel** de **Puissance 4 (Connect Four)**, avec un **système de classement ELO** et un **mode Joueur vs IA** dont l’intelligence artificielle sera développée spécifiquement pour le projet.  
Il sert également de démonstration d’une **architecture microservices complète**, **asynchrone** et **résiliente**.  
Chaque service est indépendant (authentification, profil, logique de jeu, matchmaking, IA) et communique via **API REST** et **Kafka**.  
Le tout est conteneurisé avec **Docker** pour un déploiement unifié et facile.

## 🧰 Technologies Utilisées

| Catégorie | Technologies |
|------------|---------------|
| **Backend** | Java 21, Spring Boot 3.5.7, Microservices |
| **Base de données** | PostgreSQL |
| **Messagerie** | Apache Kafka |
| **Cache (prévu)** | Redis |
| **Authentification** | JWT : Access + Refresh Tokens |
| **Conteneurisation** | Docker|
| **Frontend (prévu)** | Angular |

## ⚙️ Fonctionnalités Actuelles

### 🧩 Authentification (`auth-service`)
- Inscription et connexion sécurisées (JWT : Access + Refresh Tokens).  
- Endpoints sécurisés : `/login`, `/register`, `/logout`, `/refresh`.  

### 👤 Gestion de Profil (`userprofile-service`)
- Stockage des informations utilisateur (ELO, statistiques, nom d’affichage).  
- Endpoint sécurisé : `/api/profiles/me`.  

### 🔐 Sécurité & Routage (`gateway-service`)
- **API Gateway** unique pour toutes les requêtes.  
- Validation centralisée des tokens JWT.  
- Ajout du header `X-User-Id` vers les services internes après validation.

### 🤝 Matchmaking (`matchmaking-service`)
- Endpoints sécurisés : `/api/matchmaking/join` et `/leave`.
- Logique de File d'attente : Utilise **Redis** (Sorted Set) pour stocker les joueurs en attente, triés par ELO.
- Communication Inter-Service

### 🕹️ Logique de jeu (`game-service`)
- Logique de Jeu : Gestion complète de l'état du plateau, validation des coups, détection de victoire et de match nul.
- Serveur WebSocket : Gère la partie en temps réel sur /ws.

## 🎯 Fonctionnalités Futures
- 💻 **Frontend Angular** : interface web pour jouer et suivre les stats.  
- 🧠 **IA Service** : mode "Joueur vs IA".  
- 🤝 **Partie entre amis** : mode "Joueur vs Joueur" non classé.  

## 🧑‍💻 Mise en Place en Local

### 1. Prérequis
- Docker & Docker Compose  
- Git  

### 2. Installation

```bash
git clone https://github.com/dupuishugo80/ranked4.git
cd Ranked4
```

### 3. Configuration

Définissez une clé JWT commune dans :

-   `auth-service/src/main/resources/application.properties`
-   `gateway-service/src/main/resources/application.properties`
    

`jwt.secret=votre_super_cle_secrete_de_plus_de_256_bits_ici` 

Assurez-vous que les ports suivants sont libres :  
`8080, 8081, 8082, 8083, 8084, 5432, 9092, 6379`

### 4. Lancement

`docker-compose up --build` 

Services accessibles :

-   🌐 **Gateway** → [http://localhost:8080](http://localhost:8080)
    
-   🗄️ **PostgreSQL** → `localhost:5432`
    
-   🔄 **Kafka** → `localhost:9092`