---
name: pdf-converter
description: "PDF converter powered by MinerU — convert PDF to Word, Markdown, HTML, LaTeX, or plain text. Also handles image-to-text OCR, scanned document recognition, and Office formats (DOCX, PPTX, Excel). Supports 80+ languages. Use this skill when the user wants to convert, extract, read, parse, or summarize any PDF or document. Also applies when the user shares a PDF file or link and asks about its content, needs tables or formulas extracted, wants PDF OCR, or says things like 'turn this into a doc' or 'what does this paper say'."
---

# Document to Markdown

Convert PDF, images, Office docs, and more to clean Markdown using the MinerU Open API CLI. No API key needed for basic use.

## Language Rule

Reply to the user in the SAME language they use. This is non-negotiable.

## Core Workflow

Extraction is often just the first step. The typical flow is:

1. **Extract** — Use `mineru-open-api` to convert the document to Markdown
2. **Read & Process** — Help the user with what they actually need

MinerU outputs raw Markdown — it doesn't interpret or restructure the content. If the user asks to "extract the tables", "summarize the paper", or "find the key findings", you need to read the output and do that work yourself. MinerU handles the OCR and layout; you handle the understanding.

Use `-o` to save to a file when the user wants persistent output (conversion, batch processing). Skip `-o` and read stdout directly when the content is consumed immediately (summarization, Q&A).

For example:
- "帮我把这个PDF转成markdown" → use `-o` to save to file, done
- "提取这篇论文里的表格" → use `-o` to save, then read the file and pull out the tables
- "这篇论文讲了什么" → stdout is fine, read the output directly and summarize
- "把PDF里的参考文献整理出来" → stdout or `-o`, then parse the references section

### Page Range Extraction Rule

When `--pages` is used with `-o` pointing to a **directory**, the CLI derives the output filename solely from the input file name. This means multiple page-range extracts of the same file will overwrite each other.

**CRITICAL**: You MUST avoid this by converting the output path to an **explicit file path** that includes the page range.

```bash
# ❌ WRONG — same file overwrites itself
mineru-open-api flash-extract report.pdf --pages 1-20  -o ./out/
mineru-open-api flash-extract report.pdf --pages 21-40 -o ./out/

# ✅ CORRECT — unique filenames per chunk
mineru-open-api flash-extract report.pdf --pages 1-20  -o ./out/report_p1-20.md
mineru-open-api flash-extract report.pdf --pages 21-40 -o ./out/report_p21-40.md
```

Whenever the user asks to split a document by page ranges (e.g., "extract pages 1-20", "split into chunks"), always generate `-o` as an exact file path with the `_p{range}` suffix.

| User says | You generate |
|---|---|
| "把 report.pdf 每20页拆分成多个文件" | `-o ./out/report_p1-20.md`, `-o ./out/report_p21-40.md`... |
| "extract pages 1-10 and 11-20" | `-o ./out/report_p1-10.md`, `-o ./out/report_p11-20.md` |

## Two Extraction Modes

### flash-extract — Fast, no auth

Best for quick reads. No API key, no setup.

```bash
mineru-open-api flash-extract report.pdf                               # to stdout (for immediate consumption)
mineru-open-api flash-extract report.pdf -o ./output/                  # save to file
mineru-open-api flash-extract report.pdf -o ./output/report_p1-10.md   # page range (explicit file path)
mineru-open-api flash-extract report.pdf -o ./output/ --language en    # language hint
mineru-open-api flash-extract https://example.com/paper.pdf            # URL input
```

**Supports:** PDF, images (PNG, JPG, WebP...), DOCX, PPTX, Excel (XLS, XLSX)
**Limits:** 10 MB / 20 pages per document
**Output:** Markdown only — images, tables, and formulas may become placeholders

Use flash-extract as the default unless the user needs more.

### extract — Precision, auth required

Use when the user needs full-fidelity output: preserved images, accurate tables, LaTeX formulas, or non-Markdown formats. Requires a token via `mineru-open-api auth`.

```bash
mineru-open-api extract report.pdf                              # to stdout
mineru-open-api extract report.pdf -o ./out/                    # save with all assets
mineru-open-api extract report.pdf -o ./out/ -f md,docx         # multiple output formats
mineru-open-api extract report.pdf -o ./out/report_p1-20.md --pages 1-20  # page range (explicit file path)
mineru-open-api extract report.pdf -o ./out/ --ocr          # force OCR for scanned docs
mineru-open-api extract *.pdf -o ./results/                 # batch processing
mineru-open-api extract --list files.txt -o ./results/      # batch from file list
```

**Supports:** PDF, images, DOC, DOCX, PPT, PPTX, HTML
**Limits:** 200 MB / 600 pages per document
**Output formats:** `md`, `json`, `html`, `latex`, `docx` (comma-separated with `-f`)
**Features:** formula recognition (on by default), table recognition (on by default), OCR toggle, batch mode, model selection (`vlm`, `pipeline`, `html`)

If the user hasn't authenticated yet, guide them to run `mineru-open-api auth` first.

## When to Use Which

| Situation | Mode |
|---|---|
| "What does this PDF say?" | flash-extract |
| Quick summary or content scan | flash-extract |
| Need images/tables/formulas preserved | extract |
| Document > 10 MB or > 20 pages | extract |
| Batch converting multiple files | extract |
| Need DOCX/LaTeX/HTML output | extract |
| Scanned document needs OCR | extract with `--ocr` |

## Language Support

Default is `ch` (Chinese + English). Use `--language` to specify others. Common codes:

| Language | Code | Language | Code |
|---|---|---|---|
| Chinese + English | `ch` | Japanese | `japan` |
| English | `en` | Korean | `korean` |
| French | `fr` | Chinese Traditional | `chinese_cht` |
| German | `de` | Spanish | `es` |
| Russian | `ru` | Arabic | `ar` |
| Portuguese | `pt` | Hindi | `hi` |
| Italian | `it` | Vietnamese | `vi` |
| Thai | `th` | Turkish | `tr` |

80+ languages supported in total — use the PaddleOCR language code for any language not listed above.

## Data Flow

Both commands send the document to MinerU's API (mineru.net) for processing. This is a stateless API call with no persistent storage. MinerU is open-source by OpenDataLab (Shanghai AI Lab): https://github.com/opendatalab/MinerU

## Troubleshooting

- **Debug API requests:** Add `-v` flag to see HTTP request/response details (e.g., `mineru-open-api flash-extract report.pdf -v`)
- **CLI not found:** Install via one of:
    - `npm i -g mineru-open-api` (Node.js)
    - `uv tool install mineru-open-api` (Python/uv)
    - macOS/Linux: `curl -fsSL https://cdn-mineru.openxlab.org.cn/open-api-cli/install.sh | sh`
    - Windows: `irm https://cdn-mineru.openxlab.org.cn/open-api-cli/install.ps1 | iex`
- **Auth error on extract:** Run `mineru-open-api auth` to set up your token
- **Timeout on large files:** Increase with `--timeout 600` (seconds)
- **Wrong language output:** Set `--language` explicitly (e.g., `--language en` for English docs)