# common-library
---

## 팀원 사용 가이드

### 1. GitHub Personal Access Token 발급

GitHub Packages에서 패키지를 내려받으려면 **인증 토큰**이 필요합니다.

1. GitHub → **Settings** → **Developer settings** → **Personal access tokens** → **Tokens (classic)**
2. **Generate new token (classic)** 클릭
3. 권한에서 **`read:packages`** 체크 (다운로드만 할 경우)
4. 토큰 생성 후 복사 (`ghp_xxxx...`)

---

### 2. Gradle 인증 설정

발급받은 토큰을 **로컬 Gradle 설정 파일**에 저장합니다.
> ⚠️ 프로젝트 내부 파일에 절대 넣지 마세요. 깃에 올라갑니다.

```
~/.gradle/gradle.properties
```

파일을 열어 아래 내용을 추가합니다:

```properties
gpr.user=본인_깃헙_아이디
gpr.key=ghp_xxxxxxxxxxxxxxxxxxxx
```

---

### 3. 프로젝트 build.gradle 설정

#### Gradle Groovy DSL

```groovy
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/nine-oogle/common-library")
        credentials {
            username = findProperty("gpr.user")
            password = findProperty("gpr.key")
        }
    }
}

dependencies {
    implementation "com.nine-oogle:common-library:1.0.0"
}
```

---

## 버전 히스토리

| 버전 | 변경 내용 |
|------|-----------|
| 1.0.0 | 최초 릴리즈 |