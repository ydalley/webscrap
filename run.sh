#!/bin/bash

# Jira LLM Scraper - Run Script
#
# Usage Examples:
#   ./run.sh                                    # Run with defaults
#   ./run.sh --help                             # Show help
#   ./run.sh --projects KAFKA,SPARK             # Specific projects
#   ./run.sh -p HADOOP -s 100 -r 3              # Custom settings
#   ./run.sh --page-size 100 --rate-limit 3     # Long form options

set -e

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}=====================================${NC}"
echo -e "${GREEN}Jira LLM Data Scraper${NC}"
echo -e "${GREEN}=====================================${NC}"
echo ""

# Check Java version
echo -e "${YELLOW}Checking Java version...${NC}"
if ! command -v java &> /dev/null; then
    echo -e "${RED}Error: Java is not installed${NC}"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo -e "${RED}Error: Java 17 or higher is required (found: $JAVA_VERSION)${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Java version OK${NC}"
echo ""

# Use Gradle wrapper
echo -e "${YELLOW}Checking build tool...${NC}"
if [ -f "./gradlew" ]; then
    BUILD_CMD="./gradlew"
    echo -e "${GREEN}✓ Using Gradle wrapper${NC}"
else
    echo -e "${RED}Error: Gradle wrapper not found${NC}"
    echo "gradlew file is missing from the project"
    exit 1
fi
echo ""

# Create required directories
echo -e "${YELLOW}Creating required directories...${NC}"
mkdir -p output checkpoints logs
echo -e "${GREEN}✓ Directories created${NC}"
echo ""

# Build the project
echo -e "${YELLOW}Building project...${NC}"
$BUILD_CMD build --quiet
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Build successful${NC}"
else
    echo -e "${RED}✗ Build failed${NC}"
    exit 1
fi
echo ""

# Run the scraper
echo -e "${GREEN}=====================================${NC}"
echo -e "${GREEN}Starting Jira Scraper${NC}"
echo -e "${GREEN}=====================================${NC}"
echo ""

# Check if arguments are provided
if [ $# -eq 0 ]; then
    echo -e "${YELLOW}Scraping all default projects (KAFKA, SPARK, HADOOP)${NC}"
    echo -e "${YELLOW}Tip: Use ./run.sh --help to see all available options${NC}"
    $BUILD_CMD run --quiet --console=plain
else
    # Pass all arguments to the scraper
    echo -e "${YELLOW}Running with arguments: $@${NC}"
    $BUILD_CMD run --quiet --console=plain --args="$*"
fi

echo ""
echo -e "${GREEN}=====================================${NC}"
echo -e "${GREEN}Scraping Complete!${NC}"
echo -e "${GREEN}=====================================${NC}"
echo ""
echo -e "Output files: ${YELLOW}output/*.jsonl${NC}"
echo -e "Logs: ${YELLOW}logs/jira-scraper.log${NC}"
echo ""
