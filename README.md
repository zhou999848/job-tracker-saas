# Job Tracker SaaS（マルチテナント就活進捗管理システム）

## プロジェクト概要（Project Overview）

本プロジェクトは、複数の企業・チーム間で  
就職活動の進捗や関連情報を**安全に管理するための  
マルチテナント型 Job Tracking SaaS のプロトタイプ**です。

SaaS アーキテクチャとバックエンド設計を主軸に、  
シンプルでありながら拡張可能な業務ドメインを題材として、

- マルチテナントデータ分離
- ロールベースアクセス制御（RBAC）
- 認証・セキュリティ設計

を中心に、  
**Java / Spring Boot 技術スタックで  
ゼロから実行可能なバックエンドシステムを構築する能力**を  
示すことを目的としています。

---

## コア能力（Core Capabilities）

- マルチテナントアーキテクチャとデータ分離（Tenant レベル）
- RBAC 権限モデル（SYSTEM_ADMIN / TENANT_ADMIN / USER）
- 安全な認証と統一されたエラーハンドリング（情報漏洩防止）
- Job / Note 管理（ページング・検索・添付ファイル対応）
- 中国語 / 日本語 / 英語の多言語 Web UI（Thymeleaf）
- Docker によるローカル実行可能な構成（将来拡張を考慮）

本プロジェクトは、  
**単なる CRUD サンプルではなく、  
エンジニアリング指向で設計されたシステム**です。

---

## 設計方針（Design Highlights）

- バックエンド主導のマルチテナント識別・分離設計
- Tenant エンティティを導入し、User / Job / Note を tenant_id に紐付け
- ログイン後はバックエンド側でテナントを自動判定（フロントで tenantId は扱わない）
- リクエストスコープの TenantContext によるテナント管理
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

以下のコマンドで起動が可能です。
docker compose up --build

起動後、ブラウザで以下の URL にアクセスしてください。
http://localhost:8080

---

## デモ用アカウント

- TENANT_ADMIN：demo-company / admin1 / 11111Aaa
- USER：        demo-company / user1  / 11111Aaa

（形式：tenantName / username / password）

※ SYSTEM_ADMIN はデモ環境ではログイン不可、
　または初期データとしてのみ存在します。

---

## デモフロー（3–5 分）

- TENANT_ADMIN：テナントメンバー管理
- USER：Job / Note の作成・参照（本人データのみ）
- 権限検証：USER が管理者画面にアクセス → 拒否される

---

## テストと品質方針（Testing Strategy）

- JUnit5 / Mockito / Jacoco を導入
- コア Service、マルチテナントおよび権限ロジックを重点的にテスト
- 現段階ではカバレッジ数値よりも、重要ロジックの安定性を優先

---

## 提供形態（Delivery Notes）

- ローカルで実行可能なマルチテナント SaaS プロトタイプ
- 現時点ではパブリック環境へのデプロイは未実施
- 将来的な展開を前提としたアーキテクチャ設計

---

## プロジェクト位置づけ（面接用）

- CRUD 練習ではない
- マルチテナント設計・権限モデル・設計判断を重視
- 求職活動向けの成果物として位置づけ、継続的な進化を想定



