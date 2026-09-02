# 서버 구축 순서

AWS Lightsail / Ubuntu 24.04 / 2GB 플랜 기준.
Spring Boot JAR 과 MySQL 이 한 서버에 같이 뜨고, 앞에 Nginx 를 둔다.

```
브라우저 ──443──> Nginx ──8080──> Spring Boot (systemd: idis)
                                      └──> MySQL (localhost:3306)
```

시작하기 전에:

- Lightsail 방화벽에서 **80, 443** 을 연다. **3306 은 절대 열지 않는다** (localhost 로만 붙는다).
- 도메인 A 레코드를 서버 고정 IP로 걸어 둔다. certbot 이 도메인으로 인증서를 받는다.
- 아래에서 `example.com` 은 실제 도메인으로 바꾼다.

---

## 1. 기본 준비

```bash
sudo apt update && sudo apt upgrade -y
sudo timedatectl set-timezone Asia/Seoul
```

2GB 램에 MySQL 과 JVM 이 같이 뜨므로 스왑을 잡아 둔다. 없으면 빌드나 백업 중에 죽는다.

```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
free -h
```

## 2. Java 21

```bash
sudo apt install -y openjdk-21-jre-headless
java -version      # openjdk version "21..." 확인
```

> 서버에서 빌드하지 않는다. JAR 은 로컬에서 만들어 올린다(6단계).
> 2GB 에서 Gradle 빌드를 돌리면 메모리가 모자란다.

## 3. MySQL

```bash
sudo apt install -y mysql-server
sudo mysql_secure_installation      # root 비밀번호 설정, 나머지는 기본값(Y)
```

DB 와 전용 계정을 만든다. `<DB비밀번호>` 는 직접 정한다.

```bash
sudo mysql
```
```sql
CREATE DATABASE idis CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'idis'@'localhost' IDENTIFIED BY '<DB비밀번호>';
GRANT ALL PRIVILEGES ON idis.* TO 'idis'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

2GB 플랜이라 MySQL 이 램을 덜 쓰게 낮춰 둔다.

```bash
sudo tee /etc/mysql/mysql.conf.d/zz-idis.cnf >/dev/null <<'EOF'
[mysqld]
innodb_buffer_pool_size = 256M
max_connections = 50
performance_schema = OFF
EOF
sudo systemctl restart mysql
```

## 4. 계정과 디렉터리

```bash
sudo useradd -r -m -d /home/idis -s /usr/sbin/nologin idis
sudo mkdir -p /opt/idis/uploads /opt/idis/backup /var/log/idis
sudo chown -R idis:idis /opt/idis /var/log/idis
```

## 5. 스키마와 초기 데이터

프로젝트의 `seed/` 를 서버로 올린다(로컬에서 실행).

```bash
scp -i <키.pem> -r seed deploy ubuntu@<서버IP>:/tmp/
```

서버에서 넣는다. **순서가 중요하다.**

```bash
mysql -u idis -p --default-character-set=utf8mb4 idis < /tmp/seed/schema.sql
```

`seed/prod-init.sql` 을 열어 `<...>` 로 표시된 값(부서명, 첫 관리자 사번·이름·전화번호·입사일·**PIN 해시**, 문의 담당자)을 실제 값으로 바꾼 뒤 넣는다.

관리자는 이름+전화번호만으로 들어올 수 없고 **PIN 6자리**가 더 필요하다.
`<PIN해시>` 자리에는 원문이 아니라 BCrypt 해시를 넣는다.

```bash
sudo apt install -y apache2-utils
htpasswd -bnBC 10 "" 123456 | tr -d ':
'
# → $2y$10$... 를 통째로 복사해 <PIN해시> 자리에 넣는다
```

> `123456` 자리에 실제로 쓸 6자리를 넣는다. 이 값은 **첫 로그인에서 바로 바뀐다**
> (`pin_change_required = 1`). 화면이 새 PIN 을 정하게 하고, 정하기 전에는 들어갈 수 없다.

```bash
nano /tmp/seed/prod-init.sql
mysql -u idis -p --default-character-set=utf8mb4 idis < /tmp/seed/prod-init.sql
```

> **데모 데이터는 운영에 넣지 않는다.** `prod-init.sql` 과 `schema.sql` 둘만 넣는다.
>
> `employee.sql`, `employee-demo.sql`, `sample-form.sql`, `form-admin-demo.sql`,
> `response-demo.sql`, `site-setting.sql`
>
> 특히 `employee.sql` 은 `super_admin = 1` 인 가짜 관리자(김관리, 010-9876-5432)를 만든다.
> 운영에 들어가면 아무나 그 번호로 최고 권한 계정에 로그인할 수 있다.

## 6. 애플리케이션 올리기

로컬에서 빌드해 올린다.

```bash
./gradlew clean bootJar
scp -i <키.pem> build/libs/idis.jar ubuntu@<서버IP>:/tmp/
```

서버에서 자리 잡는다.

```bash
sudo mv /tmp/idis.jar /opt/idis/idis.jar
sudo chown idis:idis /opt/idis/idis.jar
```

## 7. systemd 서비스

```bash
sudo cp /tmp/deploy/idis.service /etc/systemd/system/
sudo nano /etc/systemd/system/idis.service     # DB_PASSWORD 를 실제 값으로
sudo chmod 600 /etc/systemd/system/idis.service
sudo systemctl daemon-reload
sudo systemctl enable --now idis
sudo systemctl status idis
```

떴는지 확인한다.

```bash
curl -I http://127.0.0.1:8080/login      # 200
sudo journalctl -u idis -n 50 --no-pager
tail -f /var/log/idis/idis.log
```

> 여기서 `Schema-validation` 오류가 나면 5단계의 `schema.sql` 이 안 들어간 것이다.
> 운영은 `ddl-auto=validate` 라 애플리케이션이 테이블을 만들지 않는다.

## 8. Nginx

```bash
sudo apt install -y nginx
sudo cp /tmp/deploy/nginx.conf /etc/nginx/sites-available/idis
sudo nano /etc/nginx/sites-available/idis      # example.com 을 실제 도메인으로
```

인증서가 아직 없으므로 **443 블록을 잠시 주석 처리**하고 80 블록만 살린 뒤 올린다.

```bash
sudo ln -s /etc/nginx/sites-available/idis /etc/nginx/sites-enabled/
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t && sudo systemctl reload nginx
```

## 9. HTTPS (Let's Encrypt)

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d example.com
```

발급이 끝나면 443 블록의 주석을 풀고 certbot 이 넣은 중복 설정을 정리한 뒤 다시 올린다.

```bash
sudo nano /etc/nginx/sites-available/idis
sudo nginx -t && sudo systemctl reload nginx
```

자동 갱신이 걸려 있는지 확인한다.

```bash
sudo systemctl status certbot.timer
sudo certbot renew --dry-run
```

## 10. 백업

```bash
sudo cp /tmp/deploy/backup.sh /opt/idis/backup.sh
sudo chown idis:idis /opt/idis/backup.sh
sudo chmod 700 /opt/idis/backup.sh
```

비밀번호가 `ps` 에 보이지 않게 설정 파일에 둔다.

```bash
sudo -u idis tee /home/idis/.my.cnf >/dev/null <<'EOF'
[client]
user=idis
password=<DB비밀번호>
EOF
sudo chmod 600 /home/idis/.my.cnf
sudo chown idis:idis /home/idis/.my.cnf
```

한 번 돌려보고 크론에 건다.

```bash
sudo -u idis /opt/idis/backup.sh
ls -lh /opt/idis/backup/

sudo -u idis crontab -e
```
```cron
# 매일 새벽 3시 30분 DB·업로드 백업 (7일 보관)
30 3 * * * /opt/idis/backup.sh >> /var/log/idis/backup.log 2>&1
```

> 백업이 서버 안에만 있으면 서버가 죽을 때 같이 없어진다.
> Lightsail 자동 스냅숏을 켜 두거나, 주기적으로 `/opt/idis/backup` 을 밖으로 내려받는다.

## 11. 마무리 확인

- `https://example.com/login` 이 열리고 자물쇠가 보인다
- `http://example.com` 이 https 로 넘어간다
- 첫 관리자 계정(이름 + 전화번호)으로 로그인하면 **PIN 입력 화면**이 나온다
- 시드에 넣은 PIN 을 넣으면 **PIN 변경 화면**으로 넘어가고, 새 PIN 을 정하면 관리자 화면에 들어간다
- 관리자 > 설정에서 문구가 보인다
- 관리자 > 직원 관리에서 엑셀 업로드로 직원을 넣는다
- 폼을 하나 만들고 이미지 선택지를 올려 본다 (`/opt/idis/uploads` 에 파일이 생기는지)

---

## 다시 배포할 때

### 스크립트로 (권장)

`deploy/deploy.ps1` 이 아래 수동 절차를 그대로 한다.
처음 한 번 스크립트 상단의 `$ServerHost` / `$KeyPath` 를 채워 두면 이후로는 실행만 하면 된다.

```powershell
.\deploy\deploy.ps1              # 확인 프롬프트 → 빌드 → 업로드 → 교체 → 기동 확인
.\deploy\deploy.ps1 -Yes         # 프롬프트 없이
.\deploy\deploy.ps1 -SkipBuild   # 이미 만든 jar 를 그대로
```

기동 확인(`/login` 200)까지 하고, 실패하면 `systemctl status` 와 `journalctl` 을 띄운 뒤
되돌리는 명령을 알려준다. 바꾸기 전 jar 는 `idis.jar.bak` 으로 남는다.

> **스키마가 바뀐 배포에는 쓰지 않는다.** 스크립트는 DB 를 건드리지 않는다.
> 아래 ALTER 를 먼저 넣고 실행한다.

### 손으로 할 때

```bash
# 로컬
./gradlew clean bootJar
scp -i <키.pem> build/libs/idis.jar ubuntu@<서버IP>:/tmp/

# 서버
sudo systemctl stop idis
sudo cp /opt/idis/idis.jar /opt/idis/idis.jar.bak      # 되돌릴 대비
sudo mv /tmp/idis.jar /opt/idis/idis.jar
sudo chown idis:idis /opt/idis/idis.jar
sudo systemctl start idis
sudo journalctl -u idis -n 30 --no-pager
```

엔티티를 고쳐 스키마가 바뀐 배포라면, JAR 을 올리기 **전에** 바뀐 부분을 DB 에 반영한다.
운영은 `validate` 라 스키마가 안 맞으면 애플리케이션이 뜨지 않는다.

특히 **자바 enum 은 MySQL 의 ENUM 컬럼**으로 만들어져 있다.
질문 타입 같은 값을 하나 추가하면 컬럼도 같이 늘려야 한다.

```sql
-- 예: 질문 타입을 하나 추가한 배포 (현재 8종 + 새 타입을 모두 적는다)
ALTER TABLE question MODIFY type
  enum('ADDRESS','DATE','IMAGE_CHOICE','LONG_TEXT','MULTI_CHOICE',
       'PHONE','SHORT_TEXT','SINGLE_CHOICE') NOT NULL;
```

> 질문 타입은 **자바 enum · 편집 화면의 `<select>` · 이 ENUM 컬럼** 셋이 항상 같아야 한다.
> 하나만 늘리면 편집 화면이 그 질문을 그리지 못하고, 저장할 때 전송되지 않아 DB 에서 사라진다.

컬럼이 새로 생긴 배포도 마찬가지다.

```sql
-- 예: 슈퍼 관리자 컬럼을 추가한 배포
ALTER TABLE employee ADD COLUMN super_admin bit(1) NOT NULL DEFAULT 0;
-- 그리고 슈퍼 관리자로 쓸 계정 하나를 켠다
UPDATE employee SET super_admin = 1 WHERE emp_no = '<사번>';
```

바뀐 뒤의 정확한 정의는 로컬에서 확인한다.

```bash
mysql -u root -p -e "SELECT column_type FROM information_schema.columns   WHERE table_schema='idis' AND table_name='question' AND column_name='type'"
```

되돌리기:

```bash
sudo systemctl stop idis
sudo mv /opt/idis/idis.jar.bak /opt/idis/idis.jar
sudo systemctl start idis
```

## 관리자 PIN 복구

슈퍼 관리자가 PIN 을 잊으면 화면에서 풀 방법이 없다. DB 에서 직접 넣는다.
(일반 관리자는 슈퍼 관리자가 직원 관리 화면에서 초기화해 주면 된다.)

```bash
# 1) 새 PIN 의 해시를 만든다
htpasswd -bnBC 10 "" 654321 | tr -d ':
'

# 2) 그 값을 넣고 잠금·실패 횟수를 함께 푼다
mysql -u idis -p idis
```
```sql
UPDATE employee
   SET pin_hash            = '<위에서 만든 $2y$... 해시>',
       pin_change_required = 1,   -- 본인이 바로 바꾸게 한다
       pin_fail_count      = 0,
       pin_locked_until    = NULL
 WHERE emp_no = '<사번>';

-- 확인
SELECT emp_no, name, role, super_admin,
       pin_hash IS NOT NULL AS has_pin, pin_change_required, pin_locked_until
  FROM employee WHERE emp_no = '<사번>';
```

> 잠금만 풀면 될 때는 `pin_fail_count = 0, pin_locked_until = NULL` 만 바꾼다.
> `pin_hash` 에는 **원문 PIN 을 절대 넣지 않는다.** `$2` 로 시작하지 않으면 로그인할 수 없다.

## 502 가 뜰 때

nginx 는 살아 있는데 앱이 죽은 상태다. 먼저 서비스를 본다.

```bash
systemctl is-active idis          # inactive 면 앱이 꺼진 것
sudo systemctl start idis
sudo journalctl -u idis -n 50 --no-pager
```

### 자동 보안 업데이트가 앱을 멈추는 문제 (2026-09-02 발생)

`apt-daily-upgrade` 가 `mysql-server` 를 올리면서 MySQL 을 재시작했는데,
유닛에 `Requires=mysql.service` 가 있어 idis 도 함께 멈추고 **다시 올라오지 않았다.**
`Requires=` 는 정지는 전파하지만 재시작은 보장하지 않는다.

```
06:38:48  mysql 정지 → idis 정지
06:38:57  mysql 시작 → idis 는 그대로 멈춤   ← 4시간 반 502
```

지금은 아래로 고쳐 두었다. 유닛을 다시 만들 일이 있으면 이 형태를 유지한다.

```ini
After=network-online.target mysql.service
Wants=network-online.target mysql.service   # Requires= 를 쓰지 않는다
Restart=always                              # on-failure 로 두지 않는다
```

확인 방법 — MySQL 을 재시작해도 앱이 살아 있어야 한다.

```bash
sudo systemctl restart mysql && sleep 3 && systemctl is-active idis   # active
```

## 자주 보는 곳

| 무엇 | 어디 |
|---|---|
| 애플리케이션 로그 | `/var/log/idis/idis.log` (30일 보관) |
| 기동 실패 원인 | `sudo journalctl -u idis -n 100 --no-pager` |
| Nginx 로그 | `/var/log/nginx/idis.{access,error}.log` |
| 업로드된 이미지 | `/opt/idis/uploads/forms/` |
| 백업 | `/opt/idis/backup/` |
| 서비스 설정(환경변수) | `/etc/systemd/system/idis.service` |
