# Job Tracker SaaS（マルチテナント型 就職支援・就活進捗管理システム）

## プロジェクト概要（Project Overview）

- 本プロジェクトは、学校などの教育機関をテナントとし、
  学生の就職活動状況を教員が把握・支援することを想定した
  マルチテナント型 Job Tracking SaaS のプロトタイプです。

- 学校間でのデータ分離、ロールベースの権限制御（RBAC）、
  認証・認可を含むバックエンド設計およびセキュリティ境界の検証を目的としています。

- 本プロジェクトはプロダクト化を目的とせず、
  Java / Spring Boot によるバックエンド工程設計力を検証するための
  エンジニアリング検証用プロジェクトです。

---

## コア能力（Core Capabilities）

- マルチテナントアーキテクチャと Tenant レベルのデータ分離
- RBAC 権限モデルによる役割別機能分離（SYSTEM_ADMIN / TENANT_ADMIN / USER）
    - **学生（USER）**：自身の就職活動記録を登録・更新し、教員からの指導コメントを閲覧
    - **教員（TENANT_ADMIN）**：学生の就活進捗を確認し、学生ごとに指導コメント（指導履歴）を登録
    - **システム管理者（SYSTEM_ADMIN）**：学校（テナント）の管理のみを担当し、個別ユーザーデータには直接関与しない
- 安全な認証・認可と統一されたエラーハンドリング（情報漏洩防止）
- Job / Note 管理（ページング・検索・添付ファイル対応）
- 日本語 / 中国語 / 英語の多言語 Web UI（Thymeleaf）
- Docker によるローカル実行可能な構成（将来拡張を考慮）

---

## 設計方針（Design Highlights）

- バックエンド主導のマルチテナント識別・分離設計
- Tenant を最上位の境界とし、User / Job / Note を tenant_id に紐付け
- 認証後はバックエンド側でテナントを自動判定（フロントで tenantId は扱わない）
- UI 層 + Service 層の二重権限チェックによる厳格なアクセス制御

---

## 技術スタック（Tech Stack）

- Java 21
- Spring Boot 3.x
- Spring Security + JWT
- PostgreSQL
- Thymeleaf
- Docker / Docker Compose

---

## ローカル実行（Local Run）

前提：
- Docker がインストール済みであること
- PostgreSQL / MinIO を Docker で起動する

依存サービス起動：
docker compose up -d db minio

アプリ起動：
- IntelliJ IDEA から Spring Boot アプリを起動（dev プロファイル）

アクセス：
http://localhost:8080/login

停止：
docker compose down

---

## デモ用アカウント

- TENANT_ADMIN：学校A / 教員A / 11111Aaa
- USER：        学校A / 学生A / 11111Aaa

（形式：tenantName / username / password）

※ SYSTEM_ADMIN はデモ環境ではログイン不可、
　または初期データとしてのみ存在します。

---

## テストと品質方針（Testing Strategy）

- JUnit5 / Mockito による Service 層中心のテスト
- マルチテナントおよび権限ロジックを重点検証
- カバレッジ数値よりも重要ロジックの安定性を優先

---

## プロジェクト位置づけ（面接用）

- マルチテナント設計・権限モデル・設計判断を重視
- 求職活動向けの成果物として位置づけ、継続的な進化を想定



