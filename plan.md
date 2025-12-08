# Plan de Modernisation Ranked4 - Java 21 & Angular 19

## Vue d'ensemble

Modernisation complète du stack Ranked4 avec focus sur **qualité du code et maintenabilité**:
- **Backend**: 6 microservices Java 21 + Spring Boot 3.5.7
- **Frontend**: Application Angular 19 SPA

**Priorité**: Améliorer la qualité du code, réduire le boilerplate, améliorer la type-safety et la maintenabilité.

---

## Stratégie d'implémentation

### Approche parallèle Backend + Frontend

Les changements backend et frontend sont largement indépendants. Nous allons procéder en **vagues** où chaque vague contient des tâches backend ET frontend de même niveau de risque.

**Vague 1** (Risque LOW): Quick wins et fondations
**Vague 2** (Risque LOW-MEDIUM): Configuration et validation
**Vague 3** (Risque MEDIUM): Services et événements
**Vague 4** (Risque MEDIUM-HIGH): État complexe et réactivité
**Vague 5** (Risque LOW): Tests et optimisations finales

---

## VAGUE 1: Fondations et Quick Wins (Semaine 1-2)

### Backend: DTOs → Records

**Objectif**: Éliminer le boilerplate des DTOs (getters/setters) en utilisant les Records Java 21.

**Services à mettre à jour** (dans cet ordre):

#### 1. auth-service (Jour 1-2)
Convertir en records:
- `LoginRequest.java` (3 champs)
- `RegisterRequest.java` (3 champs)
- `RefreshTokenRequest.java` (1 champ)
- `AuthResponse.java` (2 champs)
- `RegisterResponse.java` (1 champ)
- `LogoutResponse.java` (1 champ)

**Exemple de transformation**:
```java
// Avant: 15 lignes de boilerplate
public class LoginRequest {
    private String username;
    private String password;
    // getters, setters, equals, hashCode, toString
}

// Après: 1 ligne
public record LoginRequest(String username, String password) {}
```

**Fichier**: `D:\DEV\ranked4\auth-service\src\main\java\com\ranked4\auth\auth_service\auth\dto\*.java`

#### 2. game-service (Jour 3-4)
Convertir en records:
- `PlayerMoveDTO.java`
- `PlayerJoinDTO.java`
- `ErrorDTO.java`
- `GifDTO.java`
- `PlayerInfoDTO.java`

**Garder comme classes** (ont des factory methods):
- `GameHistoryDTO.java`
- `UserProfileDataDTO.java`

**Fichiers**: `D:\DEV\ranked4\game-service\src\main\java\com\ranked4\game\game_service\dto\*.java`

#### 3. userprofile-service, shop-service, matchmaking-service (Jour 5-7)
Convertir tous les DTOs simples en records.

**Impact attendu**:
- ~70% de réduction du code des DTOs
- Immutabilité par défaut
- `equals()`, `hashCode()`, `toString()` automatiques

---

### Frontend: Metadata Updates

**Objectif**: Mettre à jour les métadonnées des composants avec la syntaxe moderne Angular 19.

**13 composants à mettre à jour**:

1. `admin-lootboxes.component.ts`
2. `admin-skins.component.ts`
3. `admin-users.component.ts`
4. `admin.component.ts`
5. `app.component.ts`
6. `game.component.ts`
7. `matchmaking.component.ts`
8. `private-game.component.ts`
9. `game-history.component.ts`
10. `home.component.ts`
11. `leaderboard.component.ts`
12. `login.component.ts`
13. `register.component.ts`

**Changement**:
```typescript
// Avant
@Component({
  styleUrls: ['./component.scss']
})

// Après
@Component({
  styleUrl: './component.scss'
})
```

**Répertoire**: `D:\DEV\ranked4\ranked4-frontend\src\app\**\*.component.ts`

**Impact**: Code plus moderne, aligné avec Angular 19.

---

## VAGUE 2: Configuration Type-Safe et Validation (Semaine 3-4)

### Backend: @ConfigurationProperties

**Objectif**: Remplacer `@Value` par des classes `@ConfigurationProperties` type-safe.

#### 1. Créer ranked4-common (nouveau module partagé)

**Structure**:
```
ranked4-common/
├── pom.xml
└── src/main/java/com/ranked4/common/
    ├── jwt/
    │   ├── JwtService.java (extrait de auth-service)
    │   └── JwtProperties.java (configuration type-safe)
    ├── events/ (sealed interfaces - voir Vague 3)
    ├── dto/
    │   └── ErrorResponse.java (record)
    └── validation/
        └── Password.java (annotation custom)
```

**Dépendances à ajouter**:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-configuration-processor</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.13.0</version>
</dependency>
```

#### 2. Configuration Properties par service

**auth-service**: `JwtConfigProperties.java`
```java
@ConfigurationProperties(prefix = "jwt")
public record JwtConfigProperties(
    @NotBlank String secret,
    @Positive long accessTokenExpiration,
    @Positive long refreshTokenExpiration
) {}
```

**matchmaking-service**: `MatchmakingConfigProperties.java`
```java
@ConfigurationProperties(prefix = "ranked4.matchmaking")
public record MatchmakingConfigProperties(
    String queueKey,
    int eloRange,
    long staleEntryTimeoutMinutes,
    ServiceUrls serviceUrls
) {
    public record ServiceUrls(String userProfile) {}
}
```

**Fichiers à modifier**:
- `D:\DEV\ranked4\auth-service\src\main\java\com\ranked4\auth\auth_service\auth\security\JwtService.java`
- `D:\DEV\ranked4\matchmaking-service\src\main\java\com\ranked4\matchmaking\matchmaking_service\service\MatchmakingService.java`
- Similaire pour shop-service, game-service

**Impact**:
- Configuration type-safe
- Validation au démarrage
- Autocomplete dans IDE
- Documentation centralisée

---

### Backend: Bean Validation

**Objectif**: Ajouter validation déclarative aux DTOs avec annotations.

**Ajouter à tous les POMs**:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

**Exemple - RegisterRequest**:
```java
public record RegisterRequest(
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50)
    String username,

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100)
    String password
) {}
```

**Controllers**: Ajouter `@Valid`
```java
@PostMapping("/register")
public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
    // Supprimer la validation manuelle de AuthService
}
```

**Fichiers à modifier**:
- Tous les DTOs dans `*-service\src\main\java\**\dto\*.java`
- Tous les controllers dans `*-service\src\main\java\**\controller\*.java`

**Impact**:
- Validation centralisée
- Messages d'erreur standardisés
- Moins de code de validation manuelle

---

### Frontend: OnPush Change Detection (composants statiques)

**Objectif**: Améliorer les performances avec OnPush sur les composants à état simple.

**Composants à mettre à jour** (ordre de priorité):

#### Phase 2A: Composants purement présentationnels

1. **LeaderboardComponent**
   - Fichier: `D:\DEV\ranked4\ranked4-frontend\src\app\leaderboard\leaderboard.component.ts`
   - État: `leaderboard$` Observable avec async pipe
   - Changement: Ajouter `changeDetection: ChangeDetectionStrategy.OnPush`

2. **AdminUsersComponent**
   - Fichier: `D:\DEV\ranked4\ranked4-frontend\src\app\admin\admin-users\admin-users.component.ts`
   - État: `users$` Observable avec async pipe
   - Changement: Ajouter OnPush

3. **LoginComponent**
   - Fichier: `D:\DEV\ranked4\ranked4-frontend\src\app\security\login\login.component.ts`
   - État: Formulaire réactif uniquement
   - Changement: Ajouter OnPush

**Code**:
```typescript
import { ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-leaderboard',
  standalone: true,
  imports: [CommonModule, AsyncPipe],
  templateUrl: './leaderboard.component.html',
  styleUrl: './leaderboard.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LeaderboardComponent { }
```

**Impact**: 50-70% de réduction des cycles de change detection.

---

### Frontend: Lazy Loading

**Objectif**: Réduire la taille du bundle initial avec le chargement lazy des routes.

**Fichier**: `D:\DEV\ranked4\ranked4-frontend\src\app\app.routes.ts`

**Changements**:
```typescript
// Avant: imports en haut du fichier
import { LoginComponent } from './security/login/login.component';
import { AdminComponent } from './admin/admin.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'admin', component: AdminComponent }
];

// Après: lazy loading
export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./security/login/login.component')
      .then(m => m.LoginComponent)
  },
  {
    path: 'admin',
    loadComponent: () => import('./admin/admin.component')
      .then(m => m.AdminComponent),
    canActivate: [adminGuard]
  }
  // ... toutes les routes
];
```

**Routes à lazy load** (9 routes):
- login, register
- home
- matchmaking, game/:id, private-game
- admin, admin-users, admin-lootboxes, admin-skins

**Impact**:
- Bundle initial: ~500kB → ~300kB
- Temps de chargement initial amélioré
- Chunks séparés par route

---

## VAGUE 3: Événements et Services (Semaine 5-6)

### Backend: Sealed Classes pour événements Kafka

**Objectif**: Créer une hiérarchie d'événements type-safe avec sealed interfaces.

**Dans ranked4-common**, créer:

```java
public sealed interface GameEvent permits MatchFoundEvent, GameFinishedEvent, PlayerDisconnectEvent {
    UUID eventId();
    Instant timestamp();
}

public record MatchFoundEvent(
    UUID eventId,
    Instant timestamp,
    UUID matchId,
    UUID playerOneId,
    UUID playerTwoId,
    boolean ranked,
    String origin
) implements GameEvent {}

public record GameFinishedEvent(
    UUID eventId,
    Instant timestamp,
    UUID gameId,
    UUID playerOneId,
    UUID playerTwoId,
    Disc winner,
    boolean ranked
) implements GameEvent {}

public record PlayerDisconnectEvent(
    UUID eventId,
    Instant timestamp,
    UUID gameId,
    UUID playerId
) implements GameEvent {}
```

**Avantages**:
- Exhaustivité garantie par le compilateur
- Pattern matching fonctionnel
- Code partagé entre services

**Migration**:
1. Créer les events dans ranked4-common
2. Ajouter dépendance ranked4-common à tous les services
3. Remplacer les classes event dupliquées
4. Mettre à jour les KafkaListeners

**Fichiers à supprimer** (événements dupliqués):
- `D:\DEV\ranked4\auth-service\src\main\java\**\UserRegisteredEvent.java`
- `D:\DEV\ranked4\game-service\src\main\java\**\GameFinishedEvent.java`
- `D:\DEV\ranked4\matchmaking-service\src\main\java\**\MatchFoundEvent.java`
- etc.

---

### Backend: Pattern Matching

**Objectif**: Utiliser le pattern matching Java 21 pour améliorer la lisibilité.

#### 1. Exception Handling avec pattern matching

**GlobalExceptionHandler**:
```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleException(Exception ex, WebRequest request) {
    return switch (ex) {
        case IllegalArgumentException iae ->
            ResponseEntity.badRequest()
                .body(ErrorResponse.of(BAD_REQUEST, iae.getMessage(), request.getPath()));

        case AccessDeniedException ade ->
            ResponseEntity.status(FORBIDDEN)
                .body(ErrorResponse.of(FORBIDDEN, ade.getMessage(), request.getPath()));

        case NoSuchElementException nsee ->
            ResponseEntity.status(NOT_FOUND)
                .body(ErrorResponse.of(NOT_FOUND, nsee.getMessage(), request.getPath()));

        default ->
            ResponseEntity.status(INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(INTERNAL_SERVER_ERROR, "Unexpected error", request.getPath()));
    };
}
```

**Fichiers**:
- `D:\DEV\ranked4\userprofile-service\src\main\java\**\GlobalExceptionHandler.java`
- `D:\DEV\ranked4\shop-service\src\main\java\**\GlobalExceptionHandler.java`
- etc.

#### 2. Kafka Event Processing

```java
@KafkaListener(topics = "game-events")
public void handleGameEvent(GameEvent event) {
    switch (event) {
        case MatchFoundEvent mfe -> handleMatchFound(mfe);
        case GameFinishedEvent gfe -> handleGameFinished(gfe);
        case PlayerDisconnectEvent pde -> handleDisconnect(pde);
    }
}
```

**Impact**: Code plus expressif, exhaustivité vérifiée par le compilateur.

---

### Backend: Kafka Acknowledgments

**Objectif**: Améliorer la fiabilité avec acknowledgments et retry.

**Configuration** (application.properties de tous les services):
```properties
spring.kafka.producer.retries=3
spring.kafka.producer.acks=all
spring.kafka.producer.enable.idempotence=true
```

**Code - auth-service**:
```java
// Avant (fire-and-forget)
kafkaTemplate.send(TOPIC_USER_REGISTERED, event);

// Après (avec acknowledgment)
CompletableFuture<SendResult<String, UserRegisteredEvent>> future =
    kafkaTemplate.send(TOPIC_USER_REGISTERED, event.userId().toString(), event);

future.whenComplete((result, ex) -> {
    if (ex != null) {
        log.error("Failed to send UserRegisteredEvent for userId={}", event.userId(), ex);
        // Optionnel: marquer l'utilisateur pour sync manuelle
    } else {
        log.info("UserRegisteredEvent sent successfully: offset={}",
            result.getRecordMetadata().offset());
    }
});
```

**Fichiers**:
- `D:\DEV\ranked4\auth-service\src\main\java\**\AuthService.java` (ligne 99)
- `D:\DEV\ranked4\game-service\src\main\java\**\GameService.java` (lignes 119, 168)
- `D:\DEV\ranked4\matchmaking-service\src\main\java\**\MatchmakingService.java` (ligne 122)

---

### Frontend: Caching dans les Services

**Objectif**: Réduire les appels HTTP dupliqués avec `shareReplay(1)`.

#### ProfileService
**Fichier**: `D:\DEV\ranked4\ranked4-frontend\src\app\profile\profile.service.ts`

```typescript
export class ProfileService {
  private profileCache$ = new Map<string, Observable<UserProfile>>();

  getProfileById(userId: string): Observable<UserProfile> {
    if (!this.profileCache$.has(userId)) {
      this.profileCache$.set(userId,
        this.http.get<UserProfile>(`${this.API_URL}/${userId}`)
          .pipe(shareReplay(1))
      );
    }
    return this.profileCache$.get(userId)!;
  }

  // Méthode pour invalider le cache
  clearCache(userId?: string): void {
    if (userId) {
      this.profileCache$.delete(userId);
    } else {
      this.profileCache$.clear();
    }
  }
}
```

#### LeaderboardService
**Fichier**: `D:\DEV\ranked4\ranked4-frontend\src\app\leaderboard\leaderboard.service.ts`

```typescript
export class LeaderboardService {
  private leaderboard$?: Observable<UserProfile[]>;

  getLeaderboard(forceRefresh = false): Observable<UserProfile[]> {
    if (!this.leaderboard$ || forceRefresh) {
      this.leaderboard$ = this.http.get<UserProfile[]>(`${this.API_URL}/leaderboard`)
        .pipe(shareReplay(1));
    }
    return this.leaderboard$;
  }
}
```

**Pattern à suivre**: `D:\DEV\ranked4\ranked4-frontend\src\app\game\gif\gif.service.ts` (déjà implémenté correctement)

**Impact**: Réduction des appels HTTP, meilleure performance.

---

## VAGUE 4: État Complexe et Réactivité (Semaine 7-8)

### Backend: Fix WebClient.block() dans shop-service

**Objectif**: Éliminer les appels bloquants dans un contexte réactif.

**Fichier**: `D:\DEV\ranked4\shop-service\src\main\java\com\ranked4\shop\shop_service\service\ShopService.java`

**Problème actuel** (ligne 50):
```java
@Transactional
public Purchase buyProduct(UUID userId, Long productId) {
    // ...
    userProfileClient.post()
        .retrieve()
        .toBodilessEntity()
        .block(); // BLOQUANT dans @Transactional!
}
```

**Solution - Migration réactive**:
```java
public Mono<Purchase> buyProduct(UUID userId, Long productId) {
    return Mono.fromCallable(() ->
            productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"))
        )
        .flatMap(product ->
            debitGold(userId, product.getPrice())
                .then(Mono.fromCallable(() -> createPurchase(userId, product)))
        );
}

private Mono<Void> debitGold(UUID userId, int price) {
    return userProfileClient.post()
        .uri(uriBuilder -> uriBuilder.path("/api/profiles/debit-gold")
            .queryParam("amount", price).build())
        .header("X-User-Id", userId.toString())
        .retrieve()
        .bodyToMono(Void.class)
        .onErrorMap(WebClientResponseException.class, e ->
            switch (e.getStatusCode()) {
                case PAYMENT_REQUIRED -> new IllegalStateException("Insufficient funds");
                case NOT_FOUND -> new IllegalStateException("User not found");
                default -> new RuntimeException("Communication error", e);
            }
        );
}
```

**Controller update**:
```java
@PostMapping("/buy/{productId}")
public Mono<ResponseEntity<Purchase>> buyProduct(
    @RequestHeader("X-User-Id") UUID userId,
    @PathVariable Long productId) {
    return shopService.buyProduct(userId, productId)
        .map(ResponseEntity::ok);
}
```

**Impact**: Architecture réactive cohérente, meilleure scalabilité.

---

### Frontend: Subscription Management avec takeUntilDestroyed

**Objectif**: Éliminer la gestion manuelle des subscriptions.

#### GameComponent
**Fichier**: `D:\DEV\ranked4\ranked4-frontend\src\app\game\game\game.component.ts`

**Avant** (lignes 52-74):
```typescript
export class GameComponent implements OnInit, OnDestroy {
  private subs: Subscription[] = [];

  ngOnInit(): void {
    const gifsSub = this.gifService.getGifs().subscribe(gifs => {
      this.gifs = gifs;
    });
    this.subs.push(gifsSub);
  }

  ngOnDestroy(): void {
    this.subs.forEach(s => s.unsubscribe());
  }
}
```

**Après**:
```typescript
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

export class GameComponent implements OnInit {
  private destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.gifService.getGifs()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(gifs => {
        this.gifs = gifs;
      });
  }

  // Plus besoin de ngOnDestroy!
}
```

**Composants à mettre à jour**:
- `game.component.ts` (lignes 52-74)
- `matchmaking.component.ts` (lignes 17-18)
- `home.component.ts` (lignes 21, 26)

---

### Frontend: Fix Nested Subscriptions (anti-pattern)

**Fichier**: `D:\DEV\ranked4\ranked4-frontend\src\app\game\game\game.component.ts` (lignes 66-73)

**Avant** (ANTI-PATTERN):
```typescript
const gifReactSub = this.gameService.gifReactions$
  .subscribe(event => {
    this.lastReactionsByPlayer[event.playerId] = event;

    const clearSub = timer(3000).subscribe(() => {  // NESTED!
      // cleanup
    });
    this.subs.push(clearSub);
  });
```

**Après** (RxJS correct):
```typescript
this.gameService.gifReactions$
  .pipe(
    filter((event): event is GifReactionEvent => !!event),
    switchMap(event => {
      this.lastReactionsByPlayer[event.playerId] = event;
      return timer(3000).pipe(map(() => event));
    }),
    takeUntilDestroyed(this.destroyRef)
  )
  .subscribe(event => {
    const current = this.lastReactionsByPlayer[event.playerId];
    if (current && current.timestamp === event.timestamp) {
      this.lastReactionsByPlayer[event.playerId] = null;
    }
  });
```

**Impact**: Meilleure lisibilité, pas de fuites mémoire.

---

### Frontend: Migration Signals (services simples)

**Objectif**: Convertir BehaviorSubjects en Signals pour un état plus clair.

#### HomeComponent
**Fichier**: `D:\DEV\ranked4\ranked4-frontend\src\app\home\home.component.ts`

```typescript
// Avant
export class HomeComponent {
  public userProfile$ = new BehaviorSubject<UserProfile | null>(null);

  fetchData(): void {
    this.profileService.getProfile().subscribe(profile => {
      this.userProfile$.next(profile);
    });
  }
}

// Après
export class HomeComponent {
  public userProfile = signal<UserProfile | null>(null);

  fetchData(): void {
    this.profileService.getProfile()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(profile => {
        this.userProfile.set(profile);
      });
  }
}
```

**Template update**:
```html
<!-- Avant -->
<app-leaderboard [currentUserId]="(userProfile$ | async)?.userId">

<!-- Après -->
<app-leaderboard [currentUserId]="userProfile()?.userId">
```

**Services à mettre à jour**:
- HomeComponent (simple)
- LoginService (token refresh - ATTENTION!)

---

## VAGUE 5: GameService Signals et Tests (Semaine 9-10)

### Frontend: GameService Migration Signals

**⚠️ PHASE À HAUT RISQUE - WebSocket et gameplay temps réel**

**Fichier**: `D:\DEV\ranked4\ranked4-frontend\src\app\game\game\game.service.ts`

**État actuel** (lignes 39-47): 7 BehaviorSubjects/Subjects

**Stratégie de migration**:

1. **Convertir les BehaviorSubjects en Signals**:
```typescript
// Avant
public gameState$ = new BehaviorSubject<GameUpdate | null>(null);
public gameStatus$ = new BehaviorSubject<GameStatus>('IDLE');
public queueTime$ = new BehaviorSubject<number>(0);

// Après
public gameState = signal<GameUpdate | null>(null);
public gameStatus = signal<GameStatus>('IDLE');
public queueTime = signal<number>(0);

// Computed signals pour état dérivé
public isInGame = computed(() => this.gameStatus() === 'IN_GAME');
public isQueuing = computed(() => this.gameStatus() === 'QUEUEING');
```

2. **Garder Subjects pour les event streams**:
```typescript
// Garder comme Subjects (événements one-shot)
private gameErrorSubject = new Subject<string>();
public gameError$ = this.gameErrorSubject.asObservable();

private gifReactionsSubject = new Subject<GifReactionEvent>();
public gifReactions$ = this.gifReactionsSubject.asObservable();
```

3. **Remplacer setTimeout/setInterval par RxJS**:
```typescript
// Avant (lignes 267-269)
this.queueTimerInterval = setInterval(() => {
  this.queueTime$.next(this.queueTime$.value + 1);
}, 1000);

// Après
this.queueTimerSub = interval(1000)
  .pipe(
    startWith(0),
    map(tick => tick + 1)
  )
  .subscribe(time => {
    this.queueTime.set(time);
  });
```

4. **Option de compatibilité** (moins invasif):
```typescript
// Exposer les signals comme Observables pour compatibilité
import { toObservable } from '@angular/core/rxjs-interop';

public gameState$ = toObservable(this.gameState);
public gameStatus$ = toObservable(this.gameStatus);
```

**Fichiers critiques**:
- `game.service.ts` (état du jeu)
- `websocket.service.ts` (connexion WebSocket)
- `game.component.ts` (consommateur principal)

**Tests obligatoires**:
- Connexion WebSocket
- Matchmaking (timer, cancel, timeout)
- Gameplay temps réel (moves, winner)
- GIF reactions
- Lobbies privés

---

### Backend: Infrastructure de Tests

**Objectif**: Créer les bases pour tests unitaires et d'intégration.

#### Dépendances (tous les services):
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>kafka</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
```

#### Tests prioritaires:

**auth-service**:
- `JwtServiceTest.java` - Génération/validation tokens
- `AuthServiceTest.java` - Registration, login, refresh
- `UserRegisteredEventTest.java` - Kafka event publishing

**game-service**:
- `GameBoardTest.java` - Logique de victoire
- `GameServiceTest.java` - Validation moves
- `KafkaConsumerTest.java` - Consommation match.found

**shop-service**:
- `ShopServiceTest.java` - Flow d'achat (avec mocks WebClient)

**userprofile-service**:
- `EloCalculServiceTest.java` - Calcul ELO

**Objectif de couverture**: 60% pour la logique métier critique.

---

### Backend: Virtual Threads

**Objectif**: Améliorer le throughput avec les virtual threads Java 21.

**Configuration** (application.properties de tous les services):
```properties
spring.threads.virtual.enabled=true
```

**Configuration Kafka**:
```java
@Bean
public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
        ConsumerFactory<String, Object> consumerFactory) {
    ConcurrentKafkaListenerContainerFactory<String, Object> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory);
    factory.getContainerProperties().setListenerTaskExecutor(
        Executors.newVirtualThreadPerTaskExecutor()
    );
    return factory;
}
```

**Impact**: Meilleure scalabilité pour les opérations I/O.

---

## Résumé des Fichiers Critiques

### Backend (par priorité)

**Vague 1 - DTOs**:
- `D:\DEV\ranked4\auth-service\src\main\java\com\ranked4\auth\auth_service\auth\dto\*.java`
- `D:\DEV\ranked4\game-service\src\main\java\com\ranked4\game\game_service\dto\*.java`

**Vague 2 - Configuration**:
- Nouveau: `D:\DEV\ranked4\ranked4-common\` (module entier)
- `D:\DEV\ranked4\auth-service\src\main\resources\application.properties`
- `D:\DEV\ranked4\auth-service\src\main\java\**\JwtService.java`
- `D:\DEV\ranked4\gateway\src\main\java\**\JwtService.java`

**Vague 3 - Événements**:
- `D:\DEV\ranked4\game-service\src\main\java\**\GameFinishedEvent.java`
- `D:\DEV\ranked4\auth-service\src\main\java\**\AuthService.java` (ligne 99)

**Vague 4 - Réactivité**:
- `D:\DEV\ranked4\shop-service\src\main\java\**\ShopService.java` (ligne 50)

### Frontend (par priorité)

**Vague 1 - Metadata**:
- Tous les fichiers `D:\DEV\ranked4\ranked4-frontend\src\app\**\*.component.ts`

**Vague 2 - OnPush et Routing**:
- `D:\DEV\ranked4\ranked4-frontend\src\app\leaderboard\leaderboard.component.ts`
- `D:\DEV\ranked4\ranked4-frontend\src\app\app.routes.ts`

**Vague 3 - Services**:
- `D:\DEV\ranked4\ranked4-frontend\src\app\profile\profile.service.ts`
- `D:\DEV\ranked4\ranked4-frontend\src\app\leaderboard\leaderboard.service.ts`

**Vague 4 - Subscriptions**:
- `D:\DEV\ranked4\ranked4-frontend\src\app\game\game\game.component.ts` (lignes 52-74)
- `D:\DEV\ranked4\ranked4-frontend\src\app\home\home.component.ts`

**Vague 5 - État complexe**:
- `D:\DEV\ranked4\ranked4-frontend\src\app\game\game\game.service.ts` (lignes 39-47, 267-277)
- `D:\DEV\ranked4\ranked4-frontend\src\app\game\websocket\websocket.service.ts`

---

## Bénéfices Attendus

### Qualité du Code
- ✅ **70% moins de boilerplate** (Records)
- ✅ **Type-safety complète** (ConfigurationProperties, Sealed Classes, Signals)
- ✅ **Code plus expressif** (Pattern Matching, Computed Signals)
- ✅ **Moins d'erreurs runtime** (validation au compile-time)

### Maintenabilité
- ✅ **Configuration centralisée** (ranked4-common)
- ✅ **Pas de duplication JWT** (library partagée)
- ✅ **Subscriptions propres** (takeUntilDestroyed)
- ✅ **Gestion d'état claire** (Signals vs Observables)

### Performance
- ✅ **OnPush**: 50-70% moins de change detection
- ✅ **Lazy loading**: 40% bundle initial réduit
- ✅ **Caching**: Moins d'appels HTTP
- ✅ **Virtual Threads**: Meilleur throughput I/O

### Fiabilité
- ✅ **Kafka acknowledgments**: Messages garantis
- ✅ **Bean Validation**: Validation déclarative
- ✅ **Tests**: 60% couverture logique métier
- ✅ **Architecture réactive cohérente**: Pas de .block()

---

## Stratégie de Déploiement

### Par Vague
1. Créer une branche Git par vague
2. Commits granulaires (1 service/composant à la fois)
3. Review de code entre chaque vague
4. Tests end-to-end après chaque vague

### Validation Continuous
- `docker-compose up --build` après chaque changement backend
- `ng build` après chaque changement frontend
- Tests manuels: register → matchmaking → game → ELO check

### Rollback
- Branches Git séparées permettent rollback facile
- Pas de breaking changes dans les APIs
- ranked4-common rétrocompatible pendant transition

---

## Prochaines Étapes

1. **Commencer par Vague 1** (risque LOW, impact HIGH)
2. **Valider avec tests** après chaque service/composant
3. **Review de code** avant de passer à la vague suivante
4. **Documenter** les patterns adoptés dans CLAUDE.md

**Temps estimé total**: 9-10 semaines (avec tests complets)

**Priorité absolue**: Maintenir la qualité et la maintenabilité à chaque étape. Pas de rush, chaque changement doit être testé et validé.
