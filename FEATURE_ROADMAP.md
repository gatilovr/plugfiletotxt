# Code Exporter for AI — Feature Roadmap

## 🎯 Vision
Transform from **simple exporter** → **intelligent code context builder for AI workflows**

---

## 📊 Phase 1: Core Enhancements (2-3 weeks)

### 1.1 **Git Integration — Smart Diff Export**
**Why**: Developers want to export only what matters (staged changes, PR diffs, commits)

**Features**:
- Export only staged changes (pre-commit review)
- Export unstaged changes (work-in-progress)
- Export changes since last commit
- Export files changed in current branch vs main
- Include git metadata (author, last change date)

**Impact**: 40% faster AI code review workflows

---

### 1.2 **AI-Friendly Output Formats**
**Why**: Different AI models work better with structured data vs raw text

**Formats**:
- **JSON with metadata** — Functions, classes, dependencies, metrics
- **Markdown with sections** — Files grouped by type/purpose with headers
- **Plain text** (current) — Default option
- **CSV** — For data analysis

**Example JSON output**:
```json
{
  "project": "my-app",
  "exportDate": "2026-04-20",
  "files": [{
    "path": "src/main/Main.java",
    "type": "java",
    "size": 2048,
    "lines": 45,
    "tokens": 234,
    "functions": ["main", "process"],
    "classes": ["Main"],
    "imports": ["java.util.*"]
  }]
}
```

**Impact**: AI models get better context, 30% more accurate responses

---

### 1.3 **Export Presets System**
**Why**: Users repeat same filters (e.g. "export Java+XML, skip tests, 25 files")

**Features**:
- Save current filter settings as named preset
- Quick-load presets (1-click export)
- Built-in presets:
  - **"Code Review"** — Source files + tests, no build artifacts
  - **"Documentation"** — README, docs, config files
  - **"Architecture"** — Main classes + diagrams + README
  - **"Bug Analysis"** — Modified files + test files
  - **"Full Project"** — Everything except vendor/node_modules

**Impact**: 5x faster workflow for frequent users

---

### 1.4 **Advanced Token Counting**
**Why**: Users need accurate estimates for different AI models (Claude, GPT-4, Gemini)

**Features**:
- Model-specific token counters (Claude, GPT-4, GPT-3.5, Gemini)
- Real-time token estimate updates
- Show tokens per file
- Warn when exceeding model limits
- Suggest auto-compression strategies

**Impact**: No "token limit exceeded" surprises

---

## 📊 Phase 2: Code Intelligence (3-4 weeks)

### 2.1 **Code Metadata Extraction**
**Why**: AI needs structured info about code (functions, classes, complexity)

**Features**:
- Parse and extract:
  - Class/function definitions
  - Method signatures
  - Dependencies/imports
  - Code complexity metrics
  - Test coverage indicators
- Export as searchable index

**Example**:
```markdown
## src/main/Main.java
- Class: `Main` (extends None)
- Methods: `main(String[])`, `process(File)`
- Dependencies: java.util.*, org.example.service.*
- LOC: 45 | Complexity: 3 | Tests: 2
```

**Impact**: AI can understand code structure without full reading

---

### 2.2 **Template-Based Exports**
**Why**: Different export purposes need different formats

**Templates**:
- **Code Review** — Diff view + metrics + before/after
- **Features** — Comments + function headers + tests
- **Architecture Docs** — Package structure + key classes + relationships
- **Debugging** — Error logs + related code + stack traces
- **API Export** — Public methods + docs + examples

**Impact**: Export perfectly tailored to use case

---

## 📊 Phase 3: Automation & Integration (2-3 weeks)

### 3.1 **Multi-Format Batch Export**
**Features**:
- Export same selection in multiple formats simultaneously
- Generate report comparing formats
- Automated diff detection

---

### 3.2 **Direct AI Integration**
**Features**:
- "Send to Claude" button (copy-paste ready)
- "Send to ChatGPT" (opens web app with context)
- "Copy as 1-liner" for prompts

---

### 3.3 **Export History & Comparison**
**Features**:
- Track past exports
- Compare exports: "What was different in export from 2 days ago?"
- Revert to previous preset

---

## 🔄 Phase 4: Advanced Features (Ongoing)

### 4.1 **Project Intelligence**
- Language/framework auto-detection
- Recommended filters based on project type
- Dependency tree visualization

### 4.2 **Performance**: 
- Index caching for faster rescans
- Incremental exports (only changed files)
- Parallel processing for large projects

### 4.3 **Collaboration**:
- Share presets with team
- Cloud sync of settings
- Export templates marketplace

---

## 💾 Implementation Strategy

### File Structure Changes
```
src/main/java/com/example/plugfiletotxt/
├── service/
│   ├── ProjectExportService.java (EXISTING)
│   ├── GitService.java (NEW)
│   ├── CodeAnalysisService.java (NEW)
│   └── PresetService.java (NEW)
├── export/
│   ├── ExportFormat.java (ENUM)
│   ├── TextExporter.java (EXISTING)
│   ├── JsonExporter.java (NEW)
│   └── MarkdownExporter.java (NEW)
├── model/
│   ├── CodeMetadata.java (NEW)
│   ├── ExportPreset.java (NEW)
│   └── DiffInfo.java (NEW)
└── window/
    └── MyToolWindowFactory.java (EXTEND)
```

### Testing Strategy
- Add integration tests for each new service
- Mock git operations for testing
- Validate output format compliance
- Performance benchmarks for large projects

---

## 📈 Expected Impact

| Feature | Adoption | Time Saved | Quality Improvement |
|---------|----------|-----------|-------------------|
| Diff Export | 25% daily | 5 min/export | Better context |
| Presets | 40% daily | 2 min/export | Consistency |
| Formats | 30% weekly | 10 min | AI accuracy +30% |
| Token Counter | 50% daily | 3 min | Zero overages |
| Metadata | 15% weekly | 15 min | AI understanding |

---

## 🎯 Priority Order for MVP
1. Git Integration (Diff Export) — **Strategic**
2. Presets System — **Fast win**
3. JSON/Markdown formats — **High value**
4. Token counting for Claude/GPT — **Practical**
5. Code metadata — **Advanced**

**Estimated Timeline**: 6-8 weeks for full Phase 1 + 2

---

## 📝 Success Metrics
- GitHub stars: 20 → 100+
- Download rate: 50 → 500+/week  
- Community requests: 10+ issues/month
- User satisfaction: 4.5/5 stars
