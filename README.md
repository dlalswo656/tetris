# Tetris Game Portfolio 20260505 완성

Java 기반 테트리스 게임 - Mobile(Android) / PC(Swing) 버전

## 프로젝트 구조

```
tetris/
├── mobile/   # Android 버전 (터치 조작)
└── pc/       # PC 버전 (키보드 조작)
```

---

## Mobile 버전 (Android)

### 조작 방법
| 입력 | 동작 |
|------|------|
| 탭 | 블록 회전 |
| 좌우 스와이프 | 블록 좌우 이동 |
| 아래 스와이프 | 하드 드롭 (즉시 낙하) |
| 위 스와이프 | 회전 |

### 실행 방법
Android Studio에서 `mobile/` 폴더 열기 → Run

---

## PC 버전 (Java Swing)

### 조작 방법
| 키 | 동작 |
|----|------|
| ← → | 블록 좌우 이동 |
| ↑ | 블록 회전 |
| ↓ | 소프트 드롭 |
| SPACE | 하드 드롭 (즉시 낙하) |
| P | 일시정지 / 재개 |
| R | 다시시작 |

### 빌드 및 실행

```bash
cd pc
mvn package
java -jar target/tetris-pc.jar
```

또는 `run.bat` 더블클릭 (Windows)

---

## 공통 게임 로직

- 7가지 테트로미노 블록
- 고스트 블록 (낙하 위치 미리보기)
- 줄 삭제 시 점수 획득
- 레벨업 (10줄마다) → 속도 증가
- 하드드롭, 소프트드롭

## 기술 스택
- **Mobile**: Java, Android SDK, SurfaceView
- **PC**: Java 17, Swing
