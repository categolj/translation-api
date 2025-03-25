# 大規模マークダウン翻訳の改善提案

> [!NOTE] このドキュメントはAIによって生成されました

## 1. 現状の課題

Translation APIでは、ブログ記事のマークダウンを翻訳する際にOpenAI APIを利用していますが、以下の問題が発生しています：

- **入力サイズ制限**: OpenAI APIには1リクエストあたりの入力トークン数に制限があります（モデルによって異なる）
- **大規模マークダウン**: 長文の技術ブログ記事では、コードブロックや画像リンク等を含め、制限を超えるサイズになることがある
- **コンテキスト分断**: 単純に記事を分割すると、文脈が失われ、翻訳品質が低下する可能性がある

現在のフローでは、翻訳処理が以下のように実行されています：

```mermaid
sequenceDiagram
    participant TranslationService
    participant SpringAI
    participant OpenAI
    
    TranslationService->>SpringAI: 翻訳リクエスト（全文）
    SpringAI->>OpenAI: OpenAI API呼び出し
    Note over OpenAI: 入力サイズが制限を超えると<br>エラーが発生
    alt サイズ制限エラー
        OpenAI-->>SpringAI: エラー応答
        SpringAI-->>TranslationService: エラー処理
    else 正常処理
        OpenAI-->>SpringAI: 翻訳結果
        SpringAI-->>TranslationService: 翻訳結果返却
    end
```

## 2. 改善案

### 2.1 チャンク分割と再結合アプローチ

大規模マークダウンを適切に分割し、翻訳後に再結合する方式を提案します。

```mermaid
flowchart TD
    A[ブログ記事] --> B[マークダウン解析]
    B --> C[論理的なチャンクに分割]
    C --> D[チャンク単位の翻訳]
    D --> E[翻訳結果を再結合]
    E --> F[メタデータ更新]
    F --> G[最終翻訳記事]
    
    subgraph 分割処理
    C
    end
    
    subgraph 翻訳処理
    D
    end
    
    subgraph 再結合処理
    E
    F
    end
    
    classDef process fill:#f9f,stroke:#333,stroke-width:1px;
    class B,C,D,E,F process;
```

### 2.2 具体的な実装方法

#### 2.2.1 マークダウン解析と分割

マークダウンを構造的に解析し、論理的なチャンクへの分割を行います：

```mermaid
classDiagram
    class MarkdownSegmenter {
        +segment(String markdown) List~MarkdownSegment~
    }
    
    class MarkdownSegment {
        +String content
        +SegmentType type
        +int order
    }
    
    class SegmentType {
        <<enumeration>>
        HEADING
        PARAGRAPH
        CODE_BLOCK
        LIST
        TABLE
        FRONTMATTER
    }
    
    MarkdownSegmenter --> MarkdownSegment : creates
    MarkdownSegment --> SegmentType : has
```

分割アルゴリズムの具体的なステップ：

1. **ヘッダーレベルによる分割**: `# h1`, `## h2` などのヘッダーで基本チャンクを定義
2. **コードブロックの保護**: コードブロック (```` ```code```` など) を独立したセグメントとして抽出
3. **論理的なグループ化**: 関連するパラグラフや箇条書きをヘッダーに基づいてグループ化
4. **サイズ管理**: 各チャンクがモデルの最大許容トークン数を超えないよう調整
5. **コンテキスト保持**: チャンク間の文脈を維持するために必要な情報を付加

#### 2.2.2 翻訳処理の拡張

翻訳処理はチャンク単位で実行し、コンテキストを維持します：

```mermaid
sequenceDiagram
    participant Service as TranslationService
    participant Segmenter as MarkdownSegmenter
    participant Translator as ChunkTranslator
    participant AI as SpringAI
    
    Service->>Segmenter: markdown解析を依頼
    Segmenter->>Service: チャンクリスト
    
    loop 各チャンク
        Service->>Translator: チャンク翻訳を依頼
        Translator->>AI: コンテキスト付きプロンプト送信
        AI->>Translator: 翻訳結果
        Translator->>Service: 翻訳済みチャンク
    end
    
    Service->>Service: 翻訳結果を再結合
```

#### 2.2.3 コンテキスト管理クラス

翻訳の一貫性を保つためのコンテキスト管理クラスを新設します：

```mermaid
classDiagram
    class TranslationContext {
        +Map~String, String~ terminology
        +Map~String, List~String~~ previousTranslations
        +addTerminology(String source, String target)
        +updateContext(MarkdownSegment original, MarkdownSegment translated)
        +getContextPrompt() String
    }
    
    class ChunkTranslator {
        -TranslationContext context
        +translate(MarkdownSegment segment) MarkdownSegment
        -buildPrompt(MarkdownSegment segment) String
    }
    
    ChunkTranslator --> TranslationContext : uses
```

### 2.3 プロンプトエンジニアリング

チャンク単位の翻訳を効果的に行うための特殊なプロンプト構造：

```
# 翻訳コンテキスト
これまでに翻訳された主な用語:
${terminology}

直前のセクションの内容:
${previousContext}

# 翻訳指示
以下のマークダウン形式の日本語テキストを英語に翻訳してください。
コードブロック内のコードやHTML要素は翻訳せず、そのまま保持してください。
${chunkType}セクションの翻訳を行ってください。

# 入力テキスト
${inputChunk}

# 出力形式
マークダウン形式で翻訳結果のみを出力してください。
```

## 3. 実装計画

### 3.1 新規クラス構成

```mermaid
classDiagram
    class TranslationService {
        +translateAndSendPullRequest(Long entryId, int issueNumber)
        +translate(Long entryId) Entry
        -translateLargeMarkdown(String markdown) String
    }
    
    class MarkdownSegmenter {
        +segment(String markdown) List~MarkdownSegment~
        -detectSegmentType(String line) SegmentType
        -calculateTokens(String text) int
    }
    
    class ChunkTranslator {
        -ChatClient chatClient
        -TranslationContext context
        +translate(MarkdownSegment segment) MarkdownSegment
        +translateBatch(List~MarkdownSegment~ segments) List~MarkdownSegment~
    }
    
    class TranslationContext {
        +Map~String, String~ terminology
        +List~TranslationHistory~ history
        +addTerminology(String source, String target)
        +updateContext(MarkdownSegment original, MarkdownSegment translated)
    }
    
    class MarkdownSegment {
        +String content
        +SegmentType type
        +int order
        +Map~String, String~ metadata
    }
    
    class TranslationHistory {
        +MarkdownSegment original
        +MarkdownSegment translated
        +LocalDateTime timestamp
    }
    
    TranslationService --> MarkdownSegmenter : uses
    TranslationService --> ChunkTranslator : uses
    ChunkTranslator --> TranslationContext : uses
    TranslationContext --> TranslationHistory : contains
    MarkdownSegmenter --> MarkdownSegment : creates
    TranslationHistory --> MarkdownSegment : references
```

### 3.2 実装ステップ

1. **マークダウンセグメンターの実装**：マークダウンを解析し、適切なチャンクに分割するクラスを作成
2. **翻訳コンテキスト管理の実装**：チャンク間での用語一貫性を保持する仕組みを構築
3. **チャンク翻訳クラスの実装**：コンテキスト付きの翻訳処理を行うクラスを作成
4. **再結合ロジックの実装**：翻訳したチャンクを元の構造に合わせて再結合する処理
5. **エラーハンドリングの強化**：部分的な翻訳失敗時にもリカバリー可能な仕組み
6. **TranslationServiceの拡張**：大規模マークダウン処理に対応するよう既存サービスを拡張

## 4. パフォーマンス考慮事項

```mermaid
graph TD
    A[翻訳リクエスト] --> B{サイズ判定}
    B -->|小さい| C[通常の翻訳処理]
    B -->|大きい| D[チャンク分割処理]
    
    D --> E[並列翻訳]
    E --> F[再結合]
    
    C --> G[翻訳完了]
    F --> G
    
    subgraph 並列処理
    E
    end
    
    classDef process fill:#d4f1c5,stroke:#333,stroke-width:1px;
    classDef decision fill:#f9e79f,stroke:#333,stroke-width:1px;
    class B decision;
    class C,D,E,F process;
```

### 4.1 並列処理

- **独立したチャンクは並列翻訳**：相互依存のないチャンクは並行して処理
- **コンテキスト依存チャンクは逐次処理**：前のセクションの翻訳を参照必要なものは順次処理

### 4.2 キャッシング

- **翻訳結果のキャッシング**：同じパターンの翻訳要求に対する結果をキャッシュ
- **用語集の構築**：プロジェクト固有の用語対応表を蓄積・活用

## 5. テスト戦略

1. **様々なサイズのマークダウンでの検証**：小・中・大サイズの記事でテスト
2. **特殊要素を含むケース**：コードブロック、表、リスト等の特殊マークダウン要素を含む記事の処理確認
3. **エラー条件のテスト**：API制限エラー、部分的な翻訳失敗などの状況のハンドリングを検証

## 6. まとめ

大規模マークダウンの翻訳には、構造的な分割と文脈を維持した再結合が鍵となります。提案した改善策では：

1. **構造的な分割**：マークダウンの論理構造に基づくインテリジェントな分割
2. **コンテキスト維持**：チャンク間で文脈情報を伝播させる仕組み
3. **適応的処理**：文書サイズに応じて最適な処理方法を選択
4. **耐障害性**：部分的な翻訳失敗からのリカバリー機能

これにより、任意の大きさのマークダウン文書でも効率的かつ高品質な翻訳が可能になります。
