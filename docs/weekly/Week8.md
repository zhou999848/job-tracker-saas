# 📊 開発進捗報告（Week 8）
累計開発時間：約 ○○ 時間

---

## 🎥 デモ動画
- ファイルストレージ & マルチテナント検証デモ（準備中）

---
1. 完成した機能（要約版）
   ✅ 第8週：テスト体系強化 & ファイルストレージ機能

Day 1：JUnit5・Mockito・Jacoco を導入し、Service / Controller の基本テスト基盤と CI 自動テスト環境を構築した。

Day 2：Testcontainers（PostgreSQL）を用いた統合テストを実装し、テナント分離・ページング・ソートの正確性を検証した。

Day 3：MinIO（S3 互換 API）によるファイルストレージ機能を実装し、Presigned URL を用いた安全なダウンロードを実現した。

Day 4：MinIO を含む E2E テストを構築し、テナント間アクセス制御（同一可・他テナント不可）を検証した。

Day 5：異常系・境界ケースのテストを追加し、テスト構造を整理して全体カバレッジを改善した。
## 1. 完成した機能

### ✅ 第8週：テスト体系強化 & ファイルストレージ機能

- **テスト基盤構築**
    - JUnit 5 + Mockito によるテスト基盤の初期構築
    - Jacoco によるコードカバレッジ計測を導入
    - Service 層の単体テスト（正常系 / 異常系）を実装
    - MockMvc を用いた Controller 最小統合テストを追加
    - GitHub Actions CI を設定し、push / PR 時に自動テストを実行

- **実データベース統合テスト**
    - Testcontainers を導入し、PostgreSQL 実コンテナ環境でのテストを実現
    - Repository 層の統合テストを実装
    - tenant_id によるデータ分離（多租户隔離）を検証
    - ページング・ソート処理の動作確認
    - SpringBootTest + MockMvc による Controller 統合テストを追加
    - テスト専用 `application-test.yml` を整備

- **ファイルストレージ機能（MinIO / S3 API）**
    - MinIO（S3 互換 API）を導入し、ローカル保存からオブジェクトストレージへ移行
    - `FileStorageService` を新規実装
        - ファイルアップロード（Bucket 保存）
        - ファイルメタデータ（FileObject）を DB に保存
    - Presigned URL を用いた安全なファイルダウンロード機能を実装
    - ファイル Key に `tenant/{tenantId}/...` プレフィックスを付与
    - 物理レベルでのテナント分離とアクセス制御を実現

- **多租户権限 & E2E 検証**
    - Testcontainers による MinIO 統合テスト環境を構築
    - テスト用 `X-Debug-Tenant` フィルタを導入し、テナント切り替えを再現
    - エンドツーエンド（E2E）テストを実装
        - 同一テナント：アップロード → ダウンロード成功
        - 別テナント：アクセス時に 403 Forbidden
    - ファイル一覧 / 詳細取得 API を追加
    - テナント単位でのページング取得を実装
    - 権限チェックを Service / Controller 両層で実施
    - CI 上でも PostgreSQL + MinIO テストが自動実行されることを確認

- **テスト仕上げ & カバレッジ改善**
    - Jacoco レポートを元に未カバー領域を洗い出し
    - 異常系（404 / 403 / 400）および境界ケースのテストを追加
    - Unit / Integration / E2E のテスト構造と命名規則を整理
    - config / dto / exception パッケージをカバレッジ対象外に調整
    - 全体テストカバレッジを 70％以上に向上

---

## 2. 発生した問題と解決策

### 🧪 テストは成功するがカバレッジが低い
- **問題**  
  テストは通るが、Jacoco の全体カバレッジが 10％未満だった
- **原因**
    - `@WebMvcTest` のみ使用し、Service / Repository がロードされていなかった
    - Repository や外部依存を過度に Mock 化していた
- **解決策**
    - `@SpringBootTest` + Testcontainers を併用
    - 実際の処理フローを通す統合テストを追加

---

### 🐳 Windows + Docker 環境で Maven が実行できない
- **問題**  
  ローカルに Maven がなく `mvn: command not found` が発生
- **解決策**
    - Docker Maven イメージを使用し `docker run maven:3.9-temurin-21 mvn test` で実行
    - CI とローカルで同一の実行環境を確保

---

## 3. 次週（Week 9）の目標

- **UI / フロントエンド改善**
    - Thymeleaf + Bootstrap による画面整備
    - 一覧ページの視認性向上
    - AJAX による削除・更新処理の導入

- **デプロイ準備**
    - Docker Compose（App + PostgreSQL + MinIO）
    - README にローカル起動手順・デモ手順を整理

- **求職向け仕上げ**
    - デモ動画作成
    - 機能説明・設計意図の整理（面接用）

---

## 📌 まとめ
Week 8 では **テスト体系の本格導入** と **ファイルストレージ機能の実装** を行い、  
単体テスト・統合テスト・E2E テストを通じてシステム品質を大きく向上させた。  
また、S3 互換ストレージを用いたファイル管理により、多租户 SaaS としての安全性・実用性を強化できた。  
次週は UI とデプロイを中心に、実際に操作可能な成果物として完成度を高めていく。  
累計開発時間は約 **○○ 時間** に到達。
