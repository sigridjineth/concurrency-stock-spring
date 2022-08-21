# 간단한 재고 시스템으로 학습하는 동시성 이슈

본 GitHub 저장소는 [다음 인프런 강의](https://www.inflearn.com/course/%EB%8F%99%EC%8B%9C%EC%84%B1%EC%9D%B4%EC%8A%88-%EC%9E%AC%EA%B3%A0%EC%8B%9C%EC%8A%A4%ED%85%9C#reviews)를 학습한 정리 노트이다.

## 학습 목표

* Quantity 변수의 기존 값은 1000이었다. 이를 1000개의 쓰레드가 각각 개별로 1씩 감소하여 0으로 만드는 테스트를 작성하고, 이를 통과시키는 비즈니스 로직을 작성하여라.

## 테스트 환경

* Apple Macbook Pro M1
* Docker Compose-based MySQL
* Docker Compose-based Redis

## 테스트 로직

### 1. StockNonSynchronizedService: 동시성을 고려하지 않은 기본적인 로직

* Method Level에 `@Transactional` 어노테이션 적용

### 2. StockService: Synchronized 키워드를 적용한 동기화 로직

* Method Level에 `Synchronized` 를 사용하였으나, 해당 키워드는 같은 프로세스 단위에서만 동시성을 보장한다.
* 따라서 서버가 1대일 때는 동시성 이슈가 해결되는 듯 하나, 여러 대의 서버를 활용하면 여러 개의 인스턴스가 존재하는 것과 동일하기 때문에 동시성을 보장하지 못한다.

### 3. Pessimistic Lock의 사용

* 실제로 데이터에 Lock을 걸어서 정합성을 맞추는 방법이다. Exclusive Lock을 적용하게 되면 다른 트랜잭션에서는 Lock이 해제되기 전까지 데이터를 가져갈 수 없으므로 데이터 정합성을 보장한다.
* Method Level에 `@Transactional`  및 DB 조회 시에 `@Lock(LockModeType.PESSIMISTIC_WRITE)` 을 사용하여 트랜잭션이 시작할 때 Shared/Exclusive Lock을 적용하게 된다.
* Pessimistic Lock은 동시성 충돌이 잦을 것으로 예상되어 동시성을 **강력하게** 지켜야 할 때 사용하여야 한다.
* 충돌이 빈번하게 일어난다면 Optimistic Lock보다 성능 좋고, 데이터 정합성이 안정적이다.
* 하지만 별도의 Lock을 잡기 때문에 속도가 느리고, 경우에 따라 Dead Lock의 위험성이 있다.

### 4. Optimistic Lock의 사용

![img](https://cdn.discordapp.com/attachments/927446963614531624/1010730849752334476/unknown.png)

* 실제로 Lock을 사용하지 않고 Version을 이용함으로써 데이터 정합성을 준수하는 방법이다.
* 먼저 데이터를 읽은 후에 update를 실행하고, 이 때 현재 내가 읽은 버전이 맞는 지 확인하는 Query를 조회한다.
* 내가 읽은 Version에서 수정사항이 생겨서 Version의 값이 증가하였다면, 새롭게 Application에서 데이터를 다시 읽은 후에 작업을 수행해야 한다.
* Method Level에 `@Transactional` 및 DB 조회시 `@Lock(LockModeType.OPTIMISTIC)` 사용.
* 하지만 추가 기능을 별도로 구현해야 하는 번거러움이 존재한다.
  * Version 관리를 위하여 테이블을 마이그레이션 하여야 한다.
  * Version 충돌 시 재시도 로직을 구현해야 한다.
  * DB 트랜잭션을 활용하지 않기 때문에 롤백을 직접 구현해야 한다.
* 실제로 이번 테스트 케이스에서는 Version 충돌이 많기에 Optimistic Lock의 성능이 가장 좋지 않다.

### 5. Named Lock의 사용

![img](https://cdn.discordapp.com/attachments/927446963614531624/1010778948403212318/unknown.png)

* MySQL의 Native Named Lock을 사용한다.
* MySQL에서 `GET_LOCK`과 `RELEASE_LOCK` 으로 분산 락(distributed lock)을 구현할 수 있다. [참고](https://kwonnam.pe.kr/wiki/database/mysql/user_lock)
* Named Lock을 활용할 때 데이터소스를 분리하지않고 하나로 사용하게되면 커넥션풀이 부족해지는 현상을 겪을 수 있어서 락을 사용하지 않는 다른 서비스까지 영향을 끼칠 수 있다.
  * Named Lock을 활용하면 분산 락을 구현할 수 있고 Pessmistic Lock은 타임아웃을 구현하기 쉽지만 Named Lock은 타임아웃을 구현하기 쉽다. 그리고 데이터 정합성을 받춰야 하는 경우에도 Named Lock이 좋다.
  * 하지만 트랜잭션 종료 시에 Lock 해제와 세션 관리 (데이터 소스 분리 시) 관리가 수동으로 진행되어야 하고 일일이 수동으로 해야 한다는 불편한 점이 있어 실무 구현에서는 좀 빡세다. 
* Pessmistic Lock은 column/row 단계에서 Lock을 걸지만, Named Lock은 metadata 단위에 lock을 건다.
* Named Lock에서는 Thread가 아니라 Session이라고 부른다.

### 6. Distributed Lock - Lettuce의 사용

![img](https://cdn.discordapp.com/attachments/927446963614531624/1010789773864087552/unknown.png)

* Lettuce 방식은 setnx (set when not exists) 명령어를 사용하여 분산락을 구현하는 방식이다.
* spin lock 방식을 구현하여야 하는데, 이는 lock을 해제할 수 있는지 일정 주기에 따라 확인하는 방법이다.
* Named Lock과 달리 Redis를 사용하면 트랜잭션에 따라 대응되는 현재 트랜잭션 풀 세션 관리를 하지 않아도 되므로 구현이 편리하다.
* Spin Lock 방식이므로 Sleep Time이 적을 수록 Redis에 부하를 줄 수 있어서 thread busy waiting의 요청 간의 시간을 적절히 주어야 한다.
* Lettuce 는 Spring Data Redis에서 기존 인터페이스를 제공하기 때문에 러닝 커브가 빠르다.
* 반드시 수동으로 Lock을 unlock 해주어야 한다.

### 7. Distributed Lock - Redisson의 사용

```markdown
// https://devroach.tistory.com/83 
1. 스핀락은 계속해서 Lock 을 획득하기 위해 순회하기 때문에  만약 Lock 을 획득한 스레드나 프로세스가 Lock 을 정상적으로 해제해주지 못한다면 현재 스레드는 계속해서 락을 획득하려 시도하느라 어플리케이션이 중지될 것입니다.
2. 대표적으로 순회 횟수를 5회로 제한한다거나, 아니면 시간으로 제한한다거나를 택할 수 있을 겁니다.
3. setnx 메소드는 만약 키가 존재하지 않는다면 설정하게 되는 것이므로 Redis 에 계속해서 LockKeyName 이 존재하는지 확인해야만 합니다. 따라서 순회하는 동안 계속해서 Redis 에 요청을 보내게 되는 것이므로 스레드 혹은 프로세스가 많다면 Redis 에 부하가 가게 될 것입니다.
4. Lettuce 에서는 Lock 에 대한 기능을 별도로 제공하지 않고, 기존 key-value 를 Setting 하는 방법과 동일하게 사용합니다. 하지만 Redisson 에서는 RLock 이라는 클래스를 따로 제공합니다.
```

![img](https://cdn.discordapp.com/attachments/927446963614531624/1010790056031686706/unknown.png)

* Pub-sub 기반으로 Lock을 구현. 채널을 하나 만들고 락을 점유하고 있는 쓰레드가 락을 받으려는 쓰레드에게 점유 해제를 공지한다.
* 별도의 retry 로직이 필요없다.
* pub-sub 방식을 통하여 분산 락을 획득한다.

``` 
(Session 1) $ docker exec -it 6c7c0a47dd34 redis-cli
(Session 2) $ docker exec -it 6c7c0a47dd34 redis-cli

(Session 1) $ subscribe ch1
// Reading messages... (press Ctrl-C to quit)
// 1) "subscribe"
// 2) "ch1"
// 3) (integer) 1

(Session 2) $ publish ch1 hello
// (integer) 1

(Session 1) $
// 1) "message"
// 2) "ch1"
// 3) "hello"
```

* Redisson은 Lettuce와 달리 별도의 인터페이스이기 때문에 Gradle 의존 패키지 설치 및 별도 Facade 작성이 필요하다.
* leaseTime을 잘못 잡으면 작업 도중 Lock이 해제될 수도 있으니 주의하도록 한다. 이를 `IllegalMonitorStateException` 이라고 부른다.

```markdown
// https://devroach.tistory.com/83
Lock 을 해제하는 과정 중 정상적으로 Lock 이 해제가 되지 않는다면 문제가 발생할 수 있는데요. 그래서 Redisson 에서는 LockExpire 를 설정할 수 있도록 해줍니다. 그래서 Redison 의 tryLock Method 에서는 leaseTime 을 설정할 수 있습니다.

Lock 경과시간 만료후 Lock 에 접근하게 될 수도 있습니다.
만약 A 프로세스가 Lock 을 취득한 후 leaseTime 을 1초로 설정했다고 해봅시다.
근데 A 프로세스의 작업이 2초가 걸리는 작업이였다면 이미 Lock 은 leaseTime 이 경과하여 도중에 해제가 되었을 테고, A 프로세스는 Lock 에 대해서 Monitor 상태가 아닌데 Lock 을 해제하려고 할 것 입니다.
따라서 IllegalMonitorStateException 이 발생하게 됩니다.
```

* Lock 획득이 실패하고 재시도가 반드시 필요하지 않은 경우에는 Lettuce를 사용하고, 재시도가 반드시 필요한 경우에는 Redisson을 활용하도록 하자.

## 참고

### Pessimistic Lock vs Optimistic Lock

* 충돌이 적은 경우 optimistic lock 이 빠르지만, 충돌이 많다면 pessimistic lock 이 더 빠르므로, 경우에 따라 다르다.

### Facade? Helper?

* Facade는 내부 로직을 캡슐화하는 디자인 패턴. 사실 우리 구현사항에서 Facade에는 락을 얻는 행위만 있으므로 다른 패턴이 더 적합할 수 있지만, 구현이 매우 쉬워서 실무에서 자주 쓰는 편이다.

### MySQL? Redis?

* 이미 MySQL 을 사용하고 있다면 별도의 비용 없이 사용가능하다. 어느 정도의 트래픽까지는 문제 없이 활용이 가능하다. ㅏ하지만 Redis 보다는 성능이 좋지 않다.
* 만약 현재 활용중인 Redis 가 없다면 별도의 구축비용과 인프라 관리비용이 발생한다. 하지만, MySQL 보다 성능이 좋다.

### Version 주입할 시의 어노테이션

```java
import javax.persistence.Version;
```

### 더 살펴보기

* [What is the purpose of await() in CountDownLatch?](https://stackoverflow.com/questions/41866691/what-is-the-purpose-of-await-in-countdownlatch)
* [MySQL에서 사용하는 Lock 이해](http://web.bluecomtech.com/saltfactory/blog.saltfactory.net/database/introduce-mysql-lock.html)

## 참고 문헌

* https://devroach.tistory.com/83
* https://github.com/Hyune-c/manage-stock-concurrency
