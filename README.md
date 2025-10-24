# Jira LLM Data Scraper

A fault-tolerant, scalable data scraping and transformation pipeline that extracts public issue data from Apache's Jira instance and converts it into a format suitable for training Large Language Models (LLMs).

## Table of Contents

- [Quick Start](#quick-start)
- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Requirements](#requirements)
- [Installation](#installation)
- [Usage](#usage)
  - [Basic Usage](#basic-usage)
  - [Command Line Options](#command-line-options)
  - [Common Scenarios](#common-scenarios)
- [File Chunking Feature](#file-chunking-feature)
- [Configuration](#configuration)
- [Output Format](#output-format)
- [Checkpoint System](#checkpoint-system)
- [Edge Cases Handled](#edge-cases-handled)
- [Optimization Strategies](#optimization-strategies)
- [Troubleshooting](#troubleshooting)
- [Future Improvements](#future-improvements)

---

## Quick Start

### Prerequisites
Ensure you have Java 17+ installed:
```bash
java -version
```


### Using JAR (Production Deployment)
```bash
# Navigate to project directory
cd /path/to/jira-llm-scraper

# Build executable JAR
./gradlew jar

# Show help
java -jar build/libs/jira-llm-scraper-1.0.0.jar

# Run with specific project (REQUIRED)
java -jar build/libs/jira-llm-scraper-1.0.0.jar -p KAFKA

# Scrape multiple projects
java -jar build/libs/jira-llm-scraper-1.0.0.jar -p KAFKA,SPARK,HADOOP
```

### What Happens When You Run?
1. **Without arguments**: Shows help message and exits
2. **With project specified**:
   - Creates directories: `output/`, `checkpoints/`, `logs/`
   - Starts scraping issues from Apache Jira
   - Saves progress checkpoints every 10 issues
   - Generates JSONL files in `output/` directory
   - Logs activity to `logs/jira-scraper.log`

---

## Overview

This project implements a robust scraping system that:
- Extracts issue data from Apache's public Jira projects
- Handles network failures and rate limits gracefully
- Transforms raw data into structured JSONL format for LLM training
- Supports resumable scraping with checkpoint/recovery mechanism
- Automatically chunks large output files into manageable sizes

**Note**: You must specify which project(s) to scrape using the `-p` or `--projects` flag. The scraper does not run with default projects.

## Features

### Data Scraping
- **Multi-project support**: Scrape from multiple Apache Jira projects
- **Date filtering**: Filter issues by creation date with start/end date options (YYYY-MM-DD format)
- **Pagination handling**: Efficiently processes large datasets in batches
- **Automatic file chunking**: Splits large outputs into manageable chunks (default: 50 MB)
- **Rate limiting**: Respects API limits with configurable request throttling
- **Retry mechanism**: Exponential backoff for transient failures
- **Resumable scraping**: Checkpoint system allows recovery from interruptions

### Data Transformation
- **Clean text extraction**: Removes HTML/markup from descriptions and comments
- **Structured metadata**: Captures status, priority, labels, timestamps, etc.
- **Derived tasks**: Generates summarization, classification, and Q&A pairs
- **JSONL output**: One JSON object per line for easy LLM ingestion

### Reliability
- **Fault-tolerant**: Continues processing despite individual issue failures
- **Checkpoint persistence**: Saves progress periodically
- **Comprehensive error handling**: HTTP 429, 5xx, timeouts, malformed data
- **Logging**: Detailed logging for monitoring and debugging

---



## Architecture

### Component Overview

```
┌─────────────────────────────────────────────────────────────┐
│                  JiraScraperApplication                     │
│                    (Main Entry Point)                       │
└────────────────────────┬────────────────────────────────────┘
                         │
         ┌───────────────┴───────────────┐
         │                               │
┌────────▼──────────┐           ┌────────▼─────────┐
│ JiraScraperService│           │ CheckpointService│
│  - Orchestration  │◄─────────►│  - State mgmt    │
│  - Pagination     │           │  - Persistence   │
│  - File chunking  │           └──────────────────┘
└────────┬──────────┘
         │
    ┌────┴────┐
    │         │
┌───▼───┐  ┌──▼────────────────┐
│JiraAPI│  │DataTransformation │
│Client │  │     Service       │
│- HTTP │  │- HTML cleaning    │
│- Retry│  │- Task generation  │
└───────┘  └───────────────────┘
```

### Core Components

1. **JiraApiClient** (`client/JiraApiClient.java`)
   - HTTP client with rate limiting and retry logic
   - OkHttp-based implementation with interceptors
   - Exponential backoff for transient failures

2. **JiraScraperService** (`service/JiraScraperService.java`)
   - Main orchestration service
   - Manages pagination across issues
   - Coordinates checkpoint saving
   - Handles automatic file chunking
   - Monitors file sizes and rotates to new chunks

3. **DataTransformationService** (`service/DataTransformationService.java`)
   - Transforms raw Jira data to LLM training format
   - Cleans HTML from descriptions/comments
   - Generates derived tasks (summarization, Q&A, classification)

4. **CheckpointService** (`service/CheckpointService.java`)
   - Manages scraping state for fault tolerance
   - Persists progress to disk
   - Tracks current chunk number and base timestamp
   - Enables resumption after interruption

5. **Configuration** (`config/ScraperConfig.java`, `config/CliConfig.java`)
   - Centralized settings with CLI override support
   - Configurable rate limits, timeouts, and file sizes
   - Retry and backoff parameters

---

## Requirements

- **Java**: JDK 17 or higher
- **Gradle**: Not required (Gradle wrapper included)
- **Network**: Internet connection to access Apache Jira
- **Disk Space**: Sufficient space for output files (varies by project size)

---

## Installation

### 1. Navigate to Project Directory
```bash
cd /path/to/jira-llm-scraper
```

### 2. Build the Project
```bash
./gradlew build
```

### 3. Run Tests (Optional)
```bash
./gradlew test
```

### 4. Create Directories (Optional)
The application creates these automatically:
```bash
mkdir -p output checkpoints logs
```

---

## Usage

### Basic Usage

#### Using Gradle (Development)

```bash
# Show help (run without arguments)
./gradlew run

# Scrape specific project (REQUIRED - must specify at least one project)
./gradlew run --args="-p KAFKA"

# Scrape multiple projects
./gradlew run --args="--projects KAFKA,SPARK"
./gradlew run --args="-p KAFKA,SPARK,HADOOP"

# Show help explicitly
./gradlew run --args="--help"
```

#### Building and Using the JAR (Production)

```bash
# Build the executable JAR
./gradlew jar

# The JAR is created at: build/libs/jira-llm-scraper-1.0.0.jar

# Show help
java -jar build/libs/jira-llm-scraper-1.0.0.jar

# Run with specific project
java -jar build/libs/jira-llm-scraper-1.0.0.jar -p KAFKA

# Run with multiple projects
java -jar build/libs/jira-llm-scraper-1.0.0.jar --projects KAFKA,SPARK,HADOOP

# Run with custom configuration
java -jar build/libs/jira-llm-scraper-1.0.0.jar \
  -p KAFKA \
  --page-size 100 \
  --rate-limit 3 \
  --max-file-size 50 \
  --output-dir ./data

# Alternative: Build distribution with scripts
./gradlew installDist

# Run from distribution
./build/install/jira-llm-scraper/bin/jira-llm-scraper -p KAFKA
```

### Command Line Options

The scraper supports extensive command line configuration:

```bash
# Customize page size and rate limit
./gradlew run --args="--page-size 100 --rate-limit 3"
./gradlew run --args="-s 100 -r 3"

# Change output and checkpoint directories
./gradlew run --args="--output-dir ./data --checkpoint-dir ./state"
./gradlew run --args="-o ./data -c ./state"

# Configure file chunking
./gradlew run --args="--max-file-size 100"  # 100 MB chunks

# Adjust checkpoint frequency
./gradlew run --args="--checkpoint-interval 20"
./gradlew run --args="-i 20"

# Configure retry behavior
./gradlew run --args="--max-retries 5"

# Network timeouts
./gradlew run --args="--connect-timeout 60 --read-timeout 120"

# Combine multiple options
./gradlew run --args="-p KAFKA -s 100 -r 3 -o ./my-data -i 20 --max-file-size 50"
```

### Available Options Reference

| Option | Short | Type | Default | Description |
|--------|-------|------|---------|-------------|
| `--projects` | `-p` | String[] | **REQUIRED** | Comma-separated project keys (e.g., KAFKA,SPARK) |
| `--page-size` | `-s` | Integer | 50 | Issues per API request |
| `--rate-limit` | `-r` | Integer | 5 | Max requests per second |
| `--output-dir` | `-o` | String | output | Output directory path |
| `--checkpoint-dir` | `-c` | String | checkpoints | Checkpoint directory path |
| `--checkpoint-interval` | `-i` | Integer | 10 | Save checkpoint every N issues |
| `--max-retries` | `-m` | Integer | 3 | Maximum retry attempts |
| `--max-file-size` | - | Long | 50 | Max output file size in MB before chunking |
| `--jira-url` | - | String | https://issues.apache.org/jira | Jira base URL |
| `--connect-timeout` | - | Integer | 30 | HTTP connect timeout (seconds) |
| `--read-timeout` | - | Integer | 60 | HTTP read timeout (seconds) |
| `--start-date` | `-sd` | String | - | Start date for filtering (YYYY-MM-DD format) |
| `--end-date` | `-ed` | String | - | End date for filtering (YYYY-MM-DD format) |
| `--help` | `-h` | Flag | - | Show help message |
| `--version` | `-V` | Flag | - | Show version information |

### Common Scenarios

#### Testing with Small Dataset
```bash
# Scrape single project with small page size
./gradlew run --args="-p KAFKA -s 10 -r 2"

# Test file chunking with small chunks
./gradlew run --args="-p KAFKA --max-file-size 1"
```

#### Date Filtering (New Feature)
```bash
# Scrape issues created from a specific start date
./gradlew run --args="-p KAFKA --start-date 2024-01-01"
./gradlew run --args="-p KAFKA -sd 2024-01-01"

# Scrape issues created up to a specific end date
./gradlew run --args="-p KAFKA --end-date 2024-12-31"
./gradlew run --args="-p KAFKA -ed 2024-12-31"

# Scrape issues within a specific date range
./gradlew run --args="-p KAFKA --start-date 2024-01-01 --end-date 2024-12-31"
./gradlew run --args="-p KAFKA -sd 2024-01-01 -ed 2024-12-31"

# Combine with other options
./gradlew run --args="-p KAFKA,SPARK -sd 2024-06-01 -s 100 -r 3"
```

**Date Filter Notes:**
- Dates must be in YYYY-MM-DD format (e.g., 2024-01-01)
- Filters are applied to the issue's creation date (`created` field)
- Start date must be before or equal to end date
- Date filters work with checkpoints: if you resume a scrape with date filters, it will continue from the checkpoint (whichever is more recent)
- Without date filters, all issues in the project are scraped

#### Maximum Throughput
```bash
# Using Gradle
./gradlew run --args="-p KAFKA -s 100 -r 10"

# Using JAR
java -jar build/libs/jira-llm-scraper-1.0.0.jar -p KAFKA -s 100 -r 10
```

#### Conservative Settings (Avoid Rate Limiting)
```bash
# Using Gradle
./gradlew run --args="-p KAFKA --page-size 25 --rate-limit 2"

# Using JAR
java -jar build/libs/jira-llm-scraper-1.0.0.jar -p KAFKA --page-size 25 --rate-limit 2
```

#### Network Issues
```bash
# Using Gradle
./gradlew run --args="-p KAFKA --connect-timeout 60 --read-timeout 120 --max-retries 5"

# Using JAR
java -jar build/libs/jira-llm-scraper-1.0.0.jar -p KAFKA --connect-timeout 60 --read-timeout 120 --max-retries 5
```

#### Custom Jira Instance
```bash
# Using Gradle
./gradlew run --args="--jira-url https://my-company.atlassian.net --projects PROJ1,PROJ2"

# Using JAR
java -jar build/libs/jira-llm-scraper-1.0.0.jar --jira-url https://my-company.atlassian.net -p PROJ1,PROJ2
```

### Resume from Checkpoint

If scraping is interrupted (Ctrl+C, network issue, etc.):

1. Simply re-run the same command
2. The scraper automatically detects checkpoints
3. Resumes from the last saved position
4. Appends to existing output file (or continues with current chunk)

---

## File Chunking Feature

### Overview

The scraper automatically splits large output files into manageable chunks to prevent:
- Memory issues when processing large files
- Slow file I/O operations
- Difficulties with parallel processing in ML pipelines

### How It Works

**Automatic Rotation**: The scraper monitors the current output file size before writing each issue. When the file size exceeds the configured limit (default: 50 MB):

1. Closes the current writer
2. Increments the chunk number
3. Creates a new output file with chunk suffix
4. Continues writing seamlessly to the new file
5. Updates the checkpoint with the current chunk number

### File Naming Convention

```
First chunk:      {PROJECT}_{TIMESTAMP}.jsonl
Subsequent:       {PROJECT}_{TIMESTAMP}_chunk_{NUMBER}.jsonl
```

**Examples**:
```
output/
├── KAFKA_20241023_143022.jsonl              (chunk 1, no suffix)
├── KAFKA_20241023_143022_chunk_002.jsonl    (chunk 2)
├── KAFKA_20241023_143022_chunk_003.jsonl    (chunk 3)
└── SPARK_20241023_150000.jsonl              (single chunk)
```

**Note**: First chunk has no suffix for backward compatibility.

### Configuration

```bash
# Use default 50 MB chunks
./gradlew run --args="-p KAFKA"

# Custom 10 MB chunks (for testing)
./gradlew run --args="-p KAFKA --max-file-size 10"

# Large 100 MB chunks (for big datasets)
./gradlew run --args="-p SPARK --max-file-size 100"

# Effectively disable chunking (very large size)
./gradlew run --args="-p HADOOP --max-file-size 10000"
```

### Checkpoint Integration

Checkpoints track chunk information for seamless resumability:

```json
{
  "project_key": "KAFKA",
  "last_issue_key": "KAFKA-15234",
  "start_at": 3100,
  "total_processed": 3100,
  "timestamp": "2025-10-23T22:39:54.269",
  "completed": false,
  "current_chunk": 2,
  "base_timestamp": "20251023_223910"
}
```

**New Fields**:
- `current_chunk`: Which chunk file the scraper is currently writing to
- `base_timestamp`: The timestamp used for all chunks in this scraping session

### Loading Chunked Files (Python Example)

```python
import glob
import json

def load_project_data(project, timestamp):
    """Load all chunks for a project scraping session."""
    pattern = f"output/{project}_{timestamp}*.jsonl"
    files = sorted(glob.glob(pattern))

    for file_path in files:
        print(f"Loading {file_path}...")
        with open(file_path, 'r') as f:
            for line in f:
                yield json.loads(line)

# Usage
for issue in load_project_data("KAFKA", "20241023_143022"):
    print(issue['issue_key'])
```

### Parallel Processing Example

```python
from multiprocessing import Pool
import glob
import json

def process_chunk(file_path):
    """Process a single chunk file."""
    issues = []
    with open(file_path, 'r') as f:
        for line in f:
            issues.append(json.loads(line))
    return issues

# Load all chunk files
chunk_files = sorted(glob.glob("output/KAFKA_20241023_143022*.jsonl"))

# Process in parallel
with Pool(processes=4) as pool:
    results = pool.map(process_chunk, chunk_files)

# Flatten results
all_issues = [issue for chunk in results for issue in chunk]
```

---

## Configuration

### Configuration Priority

Settings are applied in the following order (highest to lowest priority):

1. **Command Line Arguments** - Runtime overrides
2. **Properties File** (`application.properties`) - Default configuration
3. **Hardcoded Defaults** - Built-in fallback values

This allows you to maintain standard settings in your properties file while easily overriding specific values for individual runs.

### Default Settings

| Parameter | Default | Description |
|-----------|---------|-------------|
| Projects | **REQUIRED** | Apache Jira projects to scrape (must specify via `-p`) |
| Rate Limit | 5 req/sec | Maximum requests per second |
| Max Retries | 3 | Number of retry attempts |
| Initial Backoff | 1000ms | Starting backoff delay |
| Max Backoff | 30000ms | Maximum backoff delay |
| Page Size | 50 | Issues per API request |
| Checkpoint Interval | 10 | Save checkpoint every N issues |
| Max File Size | 50 MB | Maximum output file size before chunking |

### Customization Methods

**Method 1: Command Line (Recommended for Runtime Changes)**

```bash
# Override specific settings without modifying files
./gradlew run --args="--projects KAFKA --page-size 100 --rate-limit 3"
```

**Method 2: Properties File (Recommended for Permanent Changes)**

Edit `src/main/resources/application.properties`:

```properties
jira.projects=KAFKA,SPARK,HADOOP
scraper.rate.limit.requests.per.second=5
scraper.pagination.page.size=50
```

**Method 3: Code Defaults (Advanced Users)**

Edit `src/main/java/com/jirascraper/config/ScraperConfig.java`:

```java
private static final int DEFAULT_MAX_REQUESTS_PER_SECOND = 5;
private static final int DEFAULT_MAX_RETRIES = 3;
private static final int DEFAULT_PAGE_SIZE = 50;
private static final long DEFAULT_MAX_OUTPUT_FILE_SIZE_MB = 50;
```

---

## Output Format

### File Locations

- **JSONL files**: `output/` directory
  - Single file: `{PROJECT}_{TIMESTAMP}.jsonl`
  - Chunked: `{PROJECT}_{TIMESTAMP}_chunk_{NUMBER}.jsonl`
  - Files automatically split when exceeding size limit

- **Checkpoints**: `checkpoints/` directory
  - Format: `{PROJECT}_checkpoint.json`
  - Tracks current chunk number for resumability
  - Automatically deleted when scraping completes

- **Logs**: `logs/` directory
  - File: `jira-scraper.log`
  - Console output also shows progress

### JSONL Structure

Each line in the JSONL output contains a complete JSON object:

```json
{
  "issue_key": "KAFKA-12345",
  "project": "KAFKA",
  "issue_type": "Bug",
  "title": "Consumer fails to handle large messages",
  "status": "Resolved",
  "priority": "Major",
  "reporter": "John Doe",
  "assignee": "Jane Smith",
  "created": "2023-01-15T10:30:00.000+0000",
  "updated": "2023-02-20T14:45:00.000+0000",
  "resolution_date": "2023-02-20T14:45:00.000+0000",
  "resolution": "Fixed",
  "labels": ["consumer", "performance"],
  "description": "When processing messages larger than 1MB...",
  "comments": [
    {
      "author": "John Doe",
      "body": "I can reproduce this consistently...",
      "created": "2023-01-16T09:00:00.000+0000"
    }
  ],
  "tasks": {
    "summarization": "Issue: Consumer fails to handle large messages\nStatus: Resolved\n...",
    "classification": {
      "issue_type": "Bug",
      "priority": "Major",
      "status": "Resolved"
    },
    "qna": [
      {
        "question": "What is the status of issue KAFKA-12345?",
        "answer": "The status is: Resolved"
      }
    ]
  }
}
```

---

## Checkpoint System

### Purpose

Checkpoints enable fault-tolerant, resumable scraping by saving progress periodically.

### Location and Format

- **Location**: `checkpoints/` directory
- **Naming**: `{PROJECT}_checkpoint.json`
- **Format**: JSON with scraping state

### Checkpoint Structure

```json
{
  "project_key": "KAFKA",
  "last_issue_key": "KAFKA-12345",
  "start_at": 150,
  "total_processed": 150,
  "timestamp": "2024-10-23T20:15:32",
  "completed": false,
  "current_chunk": 2,
  "base_timestamp": "20241023_201530"
}
```

### Operations

- **Save**: Automatically after each page and every N issues (default: 10)
- **Load**: Automatically on startup if checkpoint exists
- **Delete**: Automatically when scraping completes successfully
- **Manual Delete**: `rm checkpoints/{PROJECT}_checkpoint.json` to restart from beginning

### Resume Workflow

1. Load checkpoint (if exists)
2. Resume from `start_at` position
3. Continue writing to correct chunk file
4. Append to existing output (not overwrite)
5. Update checkpoint as scraping progresses

---

## Edge Cases Handled

### Network Errors

1. **HTTP 429 (Rate Limit)**
   - Exponential backoff with configurable max delay
   - Automatic retry after backoff period
   - Checkpoint saved to resume later

2. **5xx Server Errors**
   - Retry with exponential backoff
   - Up to 3 attempts by default
   - Graceful degradation if persistent

3. **Timeouts**
   - Configurable connect/read/write timeouts
   - Automatic retry on timeout
   - Checkpoint preservation

4. **Network Interruption**
   - Checkpoint system preserves state
   - Resume from last successful position
   - No duplicate data processing

### Data Quality

1. **Malformed JSON**
   - Try-catch around JSON parsing
   - Logs error with partial content
   - Continues with next issue

2. **Missing Fields**
   - Null-safe field access
   - Default values for missing data
   - `@JsonIgnoreProperties(ignoreUnknown = true)`

3. **Empty Responses**
   - Validation before processing
   - Logs warning and skips
   - Doesn't break scraping flow

4. **HTML in Text Fields**
   - Jsoup for HTML cleaning
   - Extracts plain text
   - Preserves readability

### Operational

1. **Interrupted Scraping**
   - Checkpoint saved periodically
   - Resume from exact position
   - Append mode for output files

2. **Duplicate Prevention**
   - Checkpoint tracks last processed issue
   - Sequential processing with pagination
   - No re-processing of completed batches

3. **Large Datasets**
   - Streaming write to JSONL
   - Pagination prevents memory overflow
   - Automatic file chunking for manageable file sizes
   - Configurable batch sizes

---

## Optimization Strategies

### Performance

1. **Rate Limiting**: Prevents API throttling while maximizing throughput
2. **Batch Processing**: Fetches multiple issues per request (pagination)
3. **Efficient JSON**: Jackson with streaming support
4. **Connection Pooling**: OkHttp connection reuse
5. **File Chunking**: Optimizes I/O for large datasets

### Reliability

1. **Exponential Backoff**: Smart retry timing for transient failures
2. **Checkpoint System**: Minimal work loss on interruption
3. **Error Isolation**: Individual issue failures don't stop batch
4. **Comprehensive Logging**: Debug and trace for troubleshooting

### Scalability

1. **Pagination**: Handles projects with 100K+ issues
2. **Streaming I/O**: Low memory footprint for large datasets
3. **Configurable Limits**: Adjust based on system resources
4. **Modular Design**: Easy to extend or modify components

---

## Troubleshooting

### Common Issues

#### Build Failure

```bash
# Clean and rebuild
./gradlew clean build
```

#### Permission Denied (gradlew)

```bash
# Make gradlew executable
chmod +x gradlew
```

#### Out of Memory

Edit `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=512m
```

Or reduce page size:
```bash
./gradlew run --args="--page-size 25"
```

#### Rate Limit Errors

Reduce request rate:
```bash
./gradlew run --args="--rate-limit 2"
```

Or via code in `ScraperConfig.java`:
```java
private static final int DEFAULT_MAX_REQUESTS_PER_SECOND = 3;
```

#### Corrupted Checkpoint

```bash
# Delete checkpoint to restart
rm checkpoints/{PROJECT}_checkpoint.json
```

#### Files Not Chunking

**Issue**: All data written to single file despite small `--max-file-size`

**Solution**:
```bash
# Verify configuration is applied
./gradlew run --args="--help" | grep max-file-size

# Check logs for chunk rotation messages
tail -f logs/jira-scraper.log | grep "rotating to chunk"
```

#### Resume Not Working with Chunks

**Issue**: After resume, scraper creates new chunk series instead of continuing

**Solution**:
```bash
# Check checkpoint has base_timestamp field
cat checkpoints/{PROJECT}_checkpoint.json

# If missing or corrupted, delete and restart
rm checkpoints/{PROJECT}_checkpoint.json
```

### Logging Levels

Adjust in `src/main/resources/logback.xml`:

```xml
<!-- Set to DEBUG for detailed output -->
<logger name="com.jirascraper" level="DEBUG" />

<!-- Set to TRACE for maximum verbosity -->
<logger name="com.jirascraper" level="TRACE" />
```

### Monitoring Progress

```bash
# View console output (real-time)
# (displayed automatically)

# View logs
tail -f logs/jira-scraper.log

# Check output files
ls -lh output/

# View checkpoint status
cat checkpoints/KAFKA_checkpoint.json
```

### Support

For issues or questions:
1. Check the logs in `logs/jira-scraper.log`
2. Review checkpoint files in `checkpoints/`
3. Verify network connectivity: `curl -I https://issues.apache.org/jira`

---

## Future Improvements

### Short-term

1. **Parallel Processing**: Scrape multiple projects concurrently
2. **Progress Bar**: Visual feedback during long-running scrapes
3. **Statistics**: Summary report of issues processed, errors, etc.
4. **Field Filtering**: Option to select specific Jira fields

### Medium-term

1. **Database Support**: Store raw data in DB for analysis
2. **Incremental Updates**: Only fetch new/updated issues
3. **Custom JQL**: Allow user-defined Jira queries
4. **Multi-threading**: Parallel issue processing within project
5. **Chunk Compression**: Gzip completed chunks automatically

### Long-term

1. **Distributed Scraping**: Scale across multiple machines
2. **Real-time Streaming**: WebSocket-based live updates
3. **ML-based Deduplication**: Detect and merge duplicate issues
4. **Advanced NLP Tasks**: More sophisticated text transformations
5. **Cloud Storage Integration**: Stream chunks to S3/cloud storage

---

## Project Structure

```
webscrap/
├── build.gradle                    # Gradle build configuration
├── settings.gradle                 # Gradle settings
├── gradlew                         # Gradle wrapper (Unix)
├── gradlew.bat                     # Gradle wrapper (Windows)
├── README.md                       # This file
│
├── src/main/java/com/jirascraper/
│   ├── JiraScraperApplication.java     # Main entry point
│   │
│   ├── client/
│   │   └── JiraApiClient.java          # HTTP client with retry/rate limiting
│   │
│   ├── config/
│   │   ├── ScraperConfig.java          # Configuration
│   │   └── CliConfig.java              # CLI argument parsing
│   │
│   ├── model/
│   │   ├── JiraIssue.java              # Jira API response model
│   │   ├── JiraSearchResponse.java     # Search API response model
│   │   ├── LLMTrainingData.java        # Output data model
│   │   └── Checkpoint.java             # Checkpoint model
│   │
│   └── service/
│       ├── JiraScraperService.java         # Main orchestration service
│       ├── CheckpointService.java          # Checkpoint persistence
│       └── DataTransformationService.java  # Data transformation
│
├── src/main/resources/
│   ├── application.properties      # Application settings
│   └── logback.xml                 # Logging configuration
│
├── output/                         # JSONL output files (created at runtime)
├── checkpoints/                    # Checkpoint files (created at runtime)
└── logs/                           # Log files (created at runtime)
```

---

## Dependencies

### Core Libraries
- **OkHttp 4.12.0**: HTTP client with connection pooling
- **Jackson 2.16.1**: JSON processing and serialization
- **SLF4J/Logback**: Logging framework
- **Jsoup 1.17.2**: HTML parsing and text extraction
- **Picocli 4.7.5**: CLI argument parsing

### Build Tools
- **Gradle 8.5**: Build automation
- **Java 17**: Runtime environment

---

## License

This project is created as an assignment demonstration. Refer to Apache Jira's terms of service for data usage restrictions.

## Acknowledgments

- Apache Software Foundation for public Jira instance
- OkHttp library for robust HTTP client
- Jackson for JSON processing
- Jsoup for HTML parsing
- Picocli for CLI framework
