# DocPortal

DocPortal est une plateforme web sécurisée destinée à la gestion et à la supervision d’un serveur d’archivage documentaire. Elle centralise les fichiers de l’organisation, structure leur classement et assure le suivi des opérations réalisées sur chaque document.

## Objectif

Le projet répond aux besoins suivants :

- centraliser les documents numériques dans un espace unique ;
- sécuriser l’accès selon le rôle de l’utilisateur ;
- faciliter la recherche et le classement des archives ;
- conserver les versions successives d’un document ;
- garantir la traçabilité des opérations ;
- administrer l’archivage, la restauration et la suppression des fichiers.

## Fonctionnalités principales

- authentification sécurisée par jeton JWT ;
- gestion des rôles `ADMIN`, `USER` et `VIEWER` ;
- dépôt et téléchargement de documents ;
- modification des métadonnées ;
- classement par catégories ;
- recherche, filtrage, tri et pagination ;
- visibilité publique ou privée ;
- archivage et restauration des documents ;
- gestion des versions précédentes ;
- historique des actions ;
- corbeille avec restauration et suppression définitive ;
- tableau de bord de suivi.

## Profils utilisateurs

| Profil | Responsabilités principales |
|---|---|
| Administrateur | Administration globale, catégories, versions, corbeille et supervision des documents |
| Utilisateur | Gestion de ses documents et consultation des documents publics |
| Lecteur | Consultation et téléchargement des documents accessibles |

## Architecture

DocPortal repose sur une architecture client-serveur organisée en couches :

```text
Interface Angular
       |
       | HTTP / JSON
       v
API REST Spring Boot
       |
       +-- Sécurité Spring Security et JWT
       +-- Services métier
       +-- Spring Data JPA et Hibernate
       |
       +-- PostgreSQL : métadonnées et historique
       +-- Stockage local : fichiers documentaires
```

## Technologies

### Frontend

- Angular 20 ;
- TypeScript ;
- Angular Material ;
- Tailwind CSS ;
- RxJS ;
- HTML et CSS.

### Backend

- Java 17 ;
- Spring Boot ;
- Spring Web MVC ;
- Spring Security ;
- Spring Data JPA ;
- Hibernate ;
- Maven ;
- Lombok.

### Données et sécurité

- PostgreSQL ;
- JSON Web Token ;
- BCrypt ;
- contrôle d’accès basé sur les rôles.

## Structure du projet

```text
DocPortal/
|-- frontend/                 Application Angular
|-- src/main/java/            Backend Spring Boot
|   |-- config/               Configuration et sécurité
|   |-- controllers/          API REST
|   |-- dto/                  Objets de transfert
|   |-- entities/             Entités de la base de données
|   |-- repositories/         Accès aux données
|   |-- security/             Gestion des jetons JWT
|   `-- services/             Logique métier
|-- src/main/resources/       Configuration de l’application
|-- uploads/                  Stockage local des documents
`-- pom.xml                   Configuration Maven
```

## Prérequis

- Java Development Kit 17 ;
- Maven ou le wrapper Maven fourni ;
- Node.js et npm ;
- PostgreSQL ;
- Angular CLI, facultatif si les commandes npm sont utilisées.

## Installation

### 1. Base de données

Créer une base PostgreSQL nommée `DocPortal`, puis adapter les paramètres de connexion dans :

```text
src/main/resources/application.properties
```

### 2. Backend

Sous Windows :

```powershell
.\mvnw.cmd spring-boot:run
```

Le serveur démarre par défaut sur :

```text
http://localhost:8080
```

### 3. Frontend

```powershell
cd frontend
npm install
npm start
```

L’interface est ensuite accessible sur :

```text
http://localhost:4200
```

## Formats acceptés

La plateforme accepte les formats `PDF`, `DOCX`, `XLSX`, `PNG`, `JPG`, `JPEG` et `TXT`. La taille maximale d’un fichier est fixée à 20 Mo.

## Vérification du projet

Tests et compilation du backend :

```powershell
.\mvnw.cmd test
```

Compilation du frontend en mode développement :

```powershell
cd frontend
npm run build -- --configuration development
```

## Sécurité avant déploiement

Avant une mise en production, il est nécessaire de :

- remplacer les identifiants de démonstration ;
- externaliser le mot de passe PostgreSQL et la clé JWT ;
- définir précisément les origines CORS autorisées ;
- utiliser HTTPS ;
- configurer un stockage persistant et une stratégie de sauvegarde ;
- limiter l’accès aux documents selon les règles internes de l’entreprise.

## Statut

La version `v2` présente une base fonctionnelle de gestion documentaire pouvant servir à une démonstration et à une évaluation en entreprise.
