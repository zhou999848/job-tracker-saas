# 就活進捗管理システム (Job Tracker 2.0 Pro)

## 📖 プロジェクト概要
本プロジェクトは、就職活動における応募管理・進捗管理を効率化するための **SaaS 型 Web アプリケーション** です。  
ユーザーは職務応募・面接・結果を一元的に管理でき、メモや添付ファイルも紐付け可能です。  
また、認証機能を実装し、ユーザーごとにデータを隔離して安全に利用できるようにしています。

---

## 🛠 技術スタック
- 言語: Java 21
- フレームワーク: Spring Boot 3, Spring Security + JWT
- データベース: PostgreSQL
- ORM: JPA / Hibernate
- フロントエンド: Thymeleaf (HTML, CSS, JavaScript)
- ツール: Docker, IntelliJ IDEA, Postman, GitHub Actions

---
# 功能总结表（Feature List）

# 機能一覧 (Feature List)




## ✅ 実装済み機能
- **Week 1**: プロジェクト環境構築、DB 接続設定、CI/CD 設定
- **Week 2**: Job モジュール（応募 CRUD、検索、ソート、ファイルアップロード/ダウンロード）
- **Week 3**: Note モジュール（応募 ↔ ノート関連付け、ノート CRUD、複数ファイル添付）
- **Week 4**: ユーザー登録・ログイン、JWT 認証、データの所有者チェック
- **Week 5**: Thymeleaf ページ連携、CRUD 操作を画面から実行可能に
- **Week 6**: 認証強化（CSRF 対応、エラーハンドリング、" i18n "、監査ログ）

---

## 📈 今後の予定
- **Week 7**: マルチテナント対応（企業/チームごとにデータ分離）
- **Week 8 以降**: 通知機能、統計レポート、外部 API 連携

---

## 🚀 実行方法
```bash
# クローン
git clone https://github.com/yourname/job-tracker.git
cd job-tracker

# ビルド & 実行
./mvnw spring-boot:run


## 📚 開発レポート
- [Week 1~6](docs/weekly/Week1~6.md)
- [Week 1](docs/weekly/Week1.md)
- [Week 2](docs/weekly/Week2.md)
- [Week 3](docs/weekly/Week3.md)

