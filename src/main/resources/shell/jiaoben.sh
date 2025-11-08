#!/bin/bash
# file: jiaoben.sh
# macOS/Linux 视频自动分段切割脚本

# 定义 FFmpeg 和 FFprobe 命令，假设它们在 PATH 中
# 如果不在 PATH 中，请替换为完整路径，例如:
# FFPROBE_CMD="/usr/local/bin/ffprobe"
# FFMPEG_CMD="/usr/local/bin/ffmpeg"
FFPROBE_CMD="ffprobe"
FFMPEG_CMD="ffmpeg"

set -e # 遇到错误立即退出

echo "正在获取视频信息..."
INPUT_FILE="part1.mp4"

# 使用 ffprobe 直接获取以秒为单位的浮点数时长
video_duration=$("$FFPROBE_CMD" -v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 "$INPUT_FILE" 2>&1)

if [ $? -ne 0 ] || [ -z "$video_duration" ]; then
    echo "错误：无法获取视频时长，请检查文件路径 ($INPUT_FILE) 和 $FFPROBE_CMD 安装"
    read -p "Press Enter to exit..."
    exit 1
fi

echo "视频原始时长: $video_duration 秒"

# 将浮点数时长转换为整数秒 (向下取整)
total_seconds=$(printf "%.0f\n" "$video_duration" | cut -d'.' -f1)

segment_duration=133 # 133秒 (2分13秒)
# 计算分段数：使用 Shell 的整数运算 (Bash v4+ 或 zsh)
segment_count=$(((total_seconds + segment_duration - 1) / segment_duration))

echo ""
echo "视频总时长: $total_seconds 秒"
echo "每段时长: ${segment_duration}秒 (2分13秒)"
echo "自动计算分段数: $segment_count"
echo ""

OUTPUT_DIR="output"
if [ ! -d "$OUTPUT_DIR" ]; then
    mkdir "$OUTPUT_DIR"
fi

# 循环切割每一段
# 注意：Shell 的 for /l 循环从 0 开始，到 segment_count-1 结束
for ((i = 0; i < segment_count; i++)); do
    
    current_start=$((i * segment_duration))
    
    # 检查是否超过视频总时长 (Shell 循环可以自然结束，但保留检查逻辑)
    if [ "$current_start" -ge "$total_seconds" ]; then
        echo "已达到视频末尾，停止切割"
        break
    fi
    
    # === 时间格式化 (HH:MM:SS) ===
    # macOS/Linux Shell 可以使用更简洁的 printf 或直接计算
    # total_seconds 转换为 HH:MM:SS 格式
    start_time=$(printf "%02d:%02d:%02d" $((current_start / 3600)) $(((current_start % 3600) / 60)) $((current_start % 60)))
    
    # === 文件编号格式化 (001, 002, ...) ===
    file_num=$((i + 1))
    # 使用 printf 格式化为三位数 (例如 001)
    num=$(printf "%03d" "$file_num")
    
    echo "[$file_num/$segment_count] 时间: $start_time → part_$num.mp4"
    
    # 使用 -t 限制时长，使用 -c copy 进行快速切割
    # 备注：FFmpeg 切割命令中的 -t 00:02:13 (2分13秒) 保持不变
    "$FFMPEG_CMD" -ss "$start_time" -i "$INPUT_FILE" -t 00:02:13 -c copy -y "$OUTPUT_DIR/part_$num.mp4"
    
    if [ $? -eq 0 ]; then
        echo "✓ 成功创建"
    else
        echo "✗ 创建失败"
    fi
    echo ""
done

echo ""
echo "视频切割完成！"
# read -p "Press Enter to finish..."