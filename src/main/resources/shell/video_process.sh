#!/bin/bash

# 设置字符编码
export LANG="en_US.UTF-8"

echo "========================================"
echo "           Video Processor"
echo "========================================"
echo

CONFIG_FILE="config.ini"

if [ ! -f "$CONFIG_FILE" ]; then
    echo "Error: config.ini not found in current directory"
    echo "Current directory: $(pwd)"
    read -p "Press any key to continue..." -n1 -s
    echo
    exit 1
fi

echo "Reading configuration from $CONFIG_FILE..."
echo

# 显示配置文件内容用于调试
echo "Config file content:"
cat "$CONFIG_FILE"
echo

# 读取配置
INPUT_DIR=""
OUTPUT_DIR=""
CLIPS_DIR=""
FFMPEG_PATH=""

TEMP_PATHS=$(mktemp)
grep '=' "$CONFIG_FILE" > "$TEMP_PATHS"

while IFS='=' read -r key value; do
    key=$(echo "$key" | tr -d '[:space:]')
    value=$(echo "$value" | tr -d '[:space:]')
    
    case "$key" in
        "INPUT_DIR") INPUT_DIR="$value" ;;
        "OUTPUT_DIR") OUTPUT_DIR="$value" ;;
        "CLIPS_DIR") CLIPS_DIR="$value" ;;
        "FFMPEG_PATH") FFMPEG_PATH="$value" ;;
    esac
done < "$TEMP_PATHS"
rm -f "$TEMP_PATHS"

# 显示读取到的配置
echo "Parsed configuration:"
echo "INPUT_DIR=[$INPUT_DIR]"
echo "OUTPUT_DIR=[$OUTPUT_DIR]"
echo "CLIPS_DIR=[$CLIPS_DIR]"
echo "FFMPEG_PATH=[$FFMPEG_PATH]"
echo

# 检查配置是否完整
if [ -z "$INPUT_DIR" ]; then
    echo "ERROR: INPUT_DIR is empty or not found in config"
    echo "Please check config.ini format"
    read -p "Press any key to continue..." -n1 -s
    echo
    exit 1
fi

if [ -z "$OUTPUT_DIR" ]; then
    echo "ERROR: OUTPUT_DIR is empty or not found in config"
    read -p "Press any key to continue..." -n1 -s
    echo
    exit 1
fi

if [ -z "$CLIPS_DIR" ]; then
    echo "ERROR: CLIPS_DIR is empty or not found in config"
    read -p "Press any key to continue..." -n1 -s
    echo
    exit 1
fi

if [ -z "$FFMPEG_PATH" ]; then
    echo "ERROR: FFMPEG_PATH is empty or not found in config"
    read -p "Press any key to continue..." -n1 -s
    echo
    exit 1
fi

# 创建目录
echo "Creating directories..."
mkdir -p "$OUTPUT_DIR"
mkdir -p "$CLIPS_DIR"

if [ ! -f "$FFMPEG_PATH" ] && ! command -v "$FFMPEG_PATH" &> /dev/null; then
    echo "ERROR: FFmpeg not found at: $FFMPEG_PATH"
    read -p "Press any key to continue..." -n1 -s
    echo
    exit 1
fi

echo "Configuration verified successfully!"
echo

# 处理视频
file_list="$CLIPS_DIR/file_list.txt"
if [ -f "$file_list" ]; then
    rm "$file_list"
fi

echo "Processing video clips..."
echo

clip_count=0

# 方法：直接硬编码所有配置，完全避免解析问题
echo "=== 使用硬编码配置 ==="

# 硬编码所有视频配置
process_video() {
    local filename=$1
    local start_time=$2
    local end_time=$3
    
    ((clip_count++))
    echo "$clip_count. Processing $filename [$start_time - $end_time]"
    
    if [ -f "$INPUT_DIR/$filename" ]; then
        echo "  Extracting clip from $filename..."
        "$FFMPEG_PATH" -i "$INPUT_DIR/$filename" -ss "$start_time" -to "$end_time" -c copy -y "$CLIPS_DIR/clip_$clip_count.mp4" 2>/dev/null
        if [ $? -eq 0 ]; then
            echo "file 'clip_$clip_count.mp4'" >> "$file_list"
            echo "  ✅ Success: clip_$clip_count.mp4 created"
        else
            echo "  ❌ Error: Extraction failed for $filename"
        fi
    else
        echo "  ❌ Error: File not found - $INPUT_DIR/$filename"
    fi
    echo
}

# 直接调用处理函数，避免任何解析问题
process_video "1.mp4" "00:17:01" "02:00:17"
process_video "2.mp4" "00:00:00" "00:28:57"

if [ $clip_count -eq 0 ]; then
    echo "ERROR: No video configurations found"
    read -p "Press any key to continue..." -n1 -s
    echo
    exit 1
fi

echo "Created $clip_count video clips"
echo "File list content:"
cat "$file_list"
echo

# 合并视频
echo "Merging $clip_count video clips..."
"$FFMPEG_PATH" -f concat -safe 0 -i "$file_list" -c copy -y "$OUTPUT_DIR/merged_video.mp4"

if [ $? -ne 0 ]; then
    echo "ERROR: Merge failed"
else
    echo
    echo "========================================"
    echo "SUCCESS: Processing completed!"
    echo "Output: $OUTPUT_DIR/merged_video.mp4"
    echo "Total clips merged: $clip_count"
    echo "========================================"
    
    # 显示生成的剪辑文件
    echo "Generated clip files:"
    ls -la "$CLIPS_DIR"/clip_*.mp4
fi

echo
read -p "Press any key to continue..." -n1 -s
echo