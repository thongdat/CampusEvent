# Huong Dan Deploy CampusEvent Len Render

File nay ghi cu the cac buoc deploy du an CampusEvent len Render, dung Neon PostgreSQL lam database va Brevo de gui OTP/email.

## 1. Kiem Tra Cac File Deploy Trong Du An

Du an da co san cac file deploy quan trong:

```text
Dockerfile
render.yaml
src/main/resources/application-render.properties
src/main/resources/schema-postgresql.sql
```

Y nghia tung file:

- `Dockerfile`: Render dung file nay de build ung dung Spring Boot bang Maven va chay file `.jar`.
- `render.yaml`: cau hinh Blueprint cho Render, tao web service `campus-event`.
- `application-render.properties`: cau hinh rieng khi chay tren Render.
- `schema-postgresql.sql`: file tao schema PostgreSQL cho Neon.

Trong `render.yaml`, service dang duoc cau hinh:

```yaml
runtime: docker
region: singapore
plan: free
healthCheckPath: /api/auth/test
SPRING_PROFILES_ACTIVE=render
```

Nghia la khi deploy, Render se chay ung dung voi profile `render`.

## 2. Day Source Code Len GitHub

Truoc khi deploy, can day code len GitHub.

Lenh co ban:

```powershell
git status
git add .
git commit -m "prepare render deployment"
git push origin main
```

Luu y:

- Khong commit mat khau database, Gmail App Password, API key Brevo, Google Client Secret.
- Cac thong tin nhay cam phai dat trong Render Environment Variables.

## 3. Tao Database PostgreSQL Tren Neon

1. Vao trang:

```text
https://neon.tech
```

2. Dang nhap va tao project moi.
3. Chon database PostgreSQL mac dinh, vi du `neondb`.
4. Lay thong tin connection:

```text
Host
Database
User
Password
```

5. Tao chuoi JDBC cho Spring Boot:

```text
jdbc:postgresql://<NEON_HOST>/<DATABASE>?sslmode=require
```

Vi du mau:

```text
DB_URL=jdbc:postgresql://ep-xxx.ap-southeast-1.aws.neon.tech/neondb?sslmode=require
DB_USERNAME=neondb_owner
DB_PASSWORD=<password-cua-neon>
```

Khong dua cac gia tri that vao GitHub.

## 4. Tao Web Service Tren Render Bang Blueprint

1. Vao:

```text
https://render.com
```

2. Dang nhap bang GitHub.
3. Chon **New**.
4. Chon **Blueprint**.
5. Chon repository `CampusEvent`.
6. Chon branch `main`.
7. Render se tu doc file `render.yaml`.
8. Nhap cac bien moi truong ma Render yeu cau.

## 5. Cau Hinh Environment Variables Tren Render

Vao Render service -> **Environment** -> them cac bien sau.

### 5.1. Bien bat buoc cho database

```text
DB_URL=jdbc:postgresql://<NEON_HOST>/<DATABASE>?sslmode=require
DB_USERNAME=<NEON_USER>
DB_PASSWORD=<NEON_PASSWORD>
```

### 5.2. Bien bat buoc cho profile Render

Neu dung `render.yaml`, bien nay da co san:

```text
SPRING_PROFILES_ACTIVE=render
```

### 5.3. Bien seed du lieu demo

Trong lan deploy dau tien, nen bat seed:

```text
APP_SEED_ENABLED=true
```

Dat mat khau demo:

```text
DEMO_ADMIN_PASSWORD=<mat-khau-admin>
DEMO_DEPARTMENT_PASSWORD=<mat-khau-khoa>
DEMO_COMMITTEE_PASSWORD=<mat-khau-ban-duyet>
DEMO_STUDENT_PASSWORD=<mat-khau-sinh-vien>
```

Mat khau nen dai it nhat 8 ky tu.

Tai khoan demo sau khi seed:

```text
Admin:     aems.admin01@uni.edu.vn
Manager:   dept01@uni.edu.vn
Committee: committee01@uni.edu.vn
Student:   student001@uni.edu.vn
```

Mat khau la gia tri da dat trong cac bien `DEMO_*_PASSWORD`.

## 6. Cau Hinh Gui OTP Bang Brevo

Render free chan SMTP outbound port `25`, `465`, `587`, nen Gmail SMTP co the bi timeout. Du an da ho tro gui mail bang Brevo HTTP API qua cong `443`.

### 6.1. Tao Brevo

1. Vao:

```text
https://www.brevo.com
```

2. Tao tai khoan mien phi.
3. Vao **Senders, Domains & Dedicated IPs**.
4. Chon **Senders**.
5. Add sender:

```text
Name: Campus Events
Email: <email-gui-otp>
```

Vi du:

```text
Email: hovanthongdat90@gmail.com
```

6. Mo Gmail va bam link verify cua Brevo.

### 6.2. Lay API Key Brevo

1. Vao **SMTP & API**.
2. Chon tab **API keys & MCP**.
3. Bam **Generate API key**.
4. Copy key bat dau bang:

```text
xkeysib-...
```

### 6.3. Them bien Brevo vao Render

```text
BREVO_API_KEY=<xkeysib-...>
BREVO_SENDER_EMAIL=<email-da-verify-tren-brevo>
```

Vi du:

```text
BREVO_SENDER_EMAIL=hovanthongdat90@gmail.com
```

Sau khi them bien, bam **Save** va redeploy service.

## 7. Cau Hinh URL Public

Sau khi Render deploy lan dau, lay URL public, vi du:

```text
https://campusevent-ffdu.onrender.com
```

Them vao Render Environment:

```text
APP_PUBLIC_BASE_URL=https://campusevent-ffdu.onrender.com
```

Neu co dung Google Login, them:

```text
GOOGLE_CLIENT_ID=<google-client-id>
GOOGLE_CLIENT_SECRET=<google-client-secret>
GOOGLE_REDIRECT_URI=https://campusevent-ffdu.onrender.com/api/login/oauth2/code/google
GOOGLE_OAUTH_SCOPES=openid,profile,email,https://www.googleapis.com/auth/forms.body,https://www.googleapis.com/auth/forms.responses.readonly,https://www.googleapis.com/auth/drive.file
```

Dong thoi vao Google Cloud Console va them URI nay vao **Authorized redirect URIs**.

Neu khong dung Google Login thi co the bo qua `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_REDIRECT_URI`.

## 8. Deploy Tren Render

Sau khi cau hinh xong:

1. Vao service tren Render.
2. Chon **Manual Deploy**.
3. Chon **Deploy latest commit**.
4. Doi build hoan tat.
5. Trang thai thanh **Live** la deploy thanh cong.

Render se build theo `Dockerfile`:

```text
mvn clean package -DskipTests
java -jar /app/app.jar
```

Ung dung chay tren Java 17.

## 9. Kiem Tra Sau Khi Deploy

Mo cac URL sau:

```text
Health check:
https://campusevent-ffdu.onrender.com/api/auth/test

Landing:
https://campusevent-ffdu.onrender.com/api/landing.html

Login:
https://campusevent-ffdu.onrender.com/api/login.html

Landing API:
https://campusevent-ffdu.onrender.com/api/public/landing
```

Neu `Landing API` tra JSON la backend va database dang ket noi duoc.

## 10. Giu Web Chay Muot Bang Cron-job

Render free va Neon free co the ngu khi khong co truy cap. De web muot hon khi demo hoac cho nguoi khac dung, tao cron-job ping moi 5 phut.

### 10.1. Tao cron-job

1. Vao:

```text
https://console.cron-job.org
```

2. Tao job moi.
3. URL:

```text
https://campusevent-ffdu.onrender.com/api/public/landing
```

4. Schedule:

```text
Every 5 minutes
```

5. Bat **Enable job**.
6. Save.

### 10.2. Khi nao can tat cron-job?

Neu muon web luon san sang cho nguoi khac dung thi khong can tat.

Chi nen tat khi:

- Khong con demo.
- Khong con ai dung web.
- Muon tiet kiem Neon free compute quota.

## 11. Xu Ly Cac Loi Thuong Gap

### 11.1. Web mo lan dau rat cham

Nguyen nhan:

- Render free bi sleep.
- Neon free bi autosuspend.

Cach xu ly:

- Tao cron-job ping moi 5 phut.
- Mo web truoc khi demo 10-15 phut.
- Neu can nhanh on dinh hon, nang Render len goi tra phi.

### 11.2. OTP khong ve email

Nguyen nhan thuong gap:

- Chua set `BREVO_API_KEY`.
- `BREVO_SENDER_EMAIL` chua verify tren Brevo.
- Sender email sai.
- Mail vao Spam.

Kiem tra Render Logs. Neu thay loi `smtp.gmail.com:587 timeout` thi do Render chan SMTP, phai dung Brevo.

### 11.3. Database khong ket noi

Kiem tra:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
sslmode=require
```

Kiem tra Neon project co dang active khong.

### 11.4. Render build loi

Kiem tra:

- `Dockerfile` co ton tai o root khong.
- `pom.xml` build duoc local khong.
- Java version co dung 17 khong.
- Branch deploy co phai `main` khong.

### 11.5. Admin dashboard bi skeleton lau

Nguyen nhan:

- Service dang cold start.
- Database Neon dang warm up.
- Browser cache chua load file JS moi.

Cach xu ly:

- Doi deploy xong hoan toan.
- Bam `Ctrl + F5`.
- Mo `https://campusevent-ffdu.onrender.com/api/public/landing` de warm up.
- Dam bao cron-job dang chay.

## 12. Checklist Deploy Hoan Chinh

Truoc khi demo, kiem tra:

- [ ] Code da push len branch `main`.
- [ ] Render deploy commit moi nhat.
- [ ] Render service dang **Live**.
- [ ] Neon database ket noi duoc.
- [ ] `APP_SEED_ENABLED=true`.
- [ ] Da set du 4 bien `DEMO_*_PASSWORD`.
- [ ] Da set `BREVO_API_KEY`.
- [ ] Da set `BREVO_SENDER_EMAIL`.
- [ ] Sender email da verify tren Brevo.
- [ ] Landing API tra JSON.
- [ ] Dang nhap admin thanh cong.
- [ ] Dang nhap student thanh cong.
- [ ] OTP gui duoc.
- [ ] Cron-job ping moi 5 phut da bat.

## 13. Tom Tat Quy Trinh

Quy trinh deploy gom:

1. Push code len GitHub.
2. Tao database tren Neon.
3. Tao Blueprint tren Render.
4. Nhap bien moi truong.
5. Cau hinh Brevo de gui OTP.
6. Deploy service.
7. Kiem tra health check, landing, login.
8. Tao cron-job de giu web khong ngu.

Sau khi hoan thanh, du an CampusEvent se chay online tren Render, dung Neon PostgreSQL lam database va Brevo de gui OTP/email.
