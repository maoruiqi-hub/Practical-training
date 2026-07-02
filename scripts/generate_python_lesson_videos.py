#!/usr/bin/env python3
from __future__ import annotations

import os
import shutil
import subprocess
import textwrap
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
OUT_DIR = ROOT / "resource" / "LessonResource" / "python"
TMP_DIR = ROOT / ".tmp_lesson_video_frames"
BUILDER = ROOT / "scripts" / "VideoBuilder"
SWIFT_SOURCE = ROOT / "scripts" / "VideoBuilder.swift"

WIDTH = 1280
HEIGHT = 720
FPS = 1
SECONDS_PER_SLIDE = 15

FONT_REGULAR = "/System/Library/Fonts/Hiragino Sans GB.ttc"
FONT_BOLD = "/System/Library/Fonts/STHeiti Medium.ttc"

LESSONS = [
    {
        "no": 1,
        "slug": "python_intro",
        "title": "Python简介与环境搭建",
        "goals": ["了解Python适用场景", "完成解释器与IDE配置", "理解虚拟环境的价值"],
        "code": "python --version\npython -m venv .venv\nsource .venv/bin/activate",
        "summary": "本节目标是让项目能跑起来，并为后续依赖管理打好基础。"
    },
    {
        "no": 2,
        "slug": "basic_syntax",
        "title": "Python基础语法与数据类型",
        "goals": ["掌握变量与缩进", "区分int、float、str、bool", "完成常见类型转换"],
        "code": "name = \"Ada\"\nage = int(\"18\")\nprint(f\"{name}: {age}\")",
        "summary": "缩进、变量、类型是阅读和编写Python程序的第一层基础。"
    },
    {
        "no": 3,
        "slug": "operators",
        "title": "运算符与表达式",
        "goals": ["掌握算术与比较运算", "理解逻辑运算组合", "能写出清晰条件表达式"],
        "code": "score = 86\npassed = 60 <= score <= 100\nprint(passed and score >= 85)",
        "summary": "表达式负责把数据关系说清楚，复杂判断要优先保证可读性。"
    },
    {
        "no": 4,
        "slug": "control_flow",
        "title": "程序控制结构",
        "goals": ["使用if处理分支", "使用for和while处理重复", "理解break与continue"],
        "code": "total = 0\nfor n in range(10):\n    if n % 2 == 0:\n        total += n\nprint(total)",
        "summary": "分支解决选择，循环解决重复，控制结构决定程序的执行路径。"
    },
    {
        "no": 5,
        "slug": "list_tuple",
        "title": "列表与元组",
        "goals": ["掌握索引与切片", "使用常见列表方法", "理解可变与不可变差异"],
        "code": "scores = [92, 75, 88]\nscores.append(96)\nprint(sorted(scores, reverse=True)[:3])",
        "summary": "列表适合动态集合，元组适合稳定结构，二者都常用于数据组织。"
    },
    {
        "no": 6,
        "slug": "dict_set",
        "title": "字典与集合",
        "goals": ["使用字典表达键值关系", "使用集合完成去重", "掌握交并差操作"],
        "code": "words = [\"py\", \"ai\", \"py\"]\ncount = {w: words.count(w) for w in set(words)}\nprint(count)",
        "summary": "字典关注映射，集合关注唯一性，它们是处理业务数据的高频工具。"
    },
    {
        "no": 7,
        "slug": "string_processing",
        "title": "字符串处理",
        "goals": ["使用切片和常用方法", "掌握格式化输出", "理解文本清洗思路"],
        "code": "raw = \"  Python,Java,Go  \"\nitems = [x.strip() for x in raw.strip().split(\",\")]\nprint(items)",
        "summary": "字符串处理的关键是标准化输入，再提取结构化信息。"
    },
    {
        "no": 8,
        "slug": "functions",
        "title": "函数定义与调用",
        "goals": ["定义清晰的函数边界", "理解参数和返回值", "减少重复代码"],
        "code": "def normalize_score(value):\n    score = int(value)\n    return max(0, min(100, score))\nprint(normalize_score(\"108\"))",
        "summary": "函数让代码从步骤堆叠变成可复用的能力单元。"
    },
    {
        "no": 9,
        "slug": "modules",
        "title": "模块与包",
        "goals": ["理解import机制", "组织自定义模块", "使用pip管理依赖"],
        "code": "from pathlib import Path\nproject_root = Path.cwd()\nprint(project_root.name)",
        "summary": "模块化的核心是把相关代码放在一起，把公共能力稳定暴露出来。"
    },
    {
        "no": 10,
        "slug": "file_io",
        "title": "文件操作",
        "goals": ["使用with安全读写文件", "处理CSV与JSON", "理解编码问题"],
        "code": "from pathlib import Path\npath = Path(\"note.txt\")\npath.write_text(\"hello\", encoding=\"utf-8\")\nprint(path.read_text(encoding=\"utf-8\"))",
        "summary": "文件操作连接程序和外部数据，with语句能减少资源泄漏风险。"
    },
    {
        "no": 11,
        "slug": "exceptions",
        "title": "异常处理",
        "goals": ["识别常见异常", "使用try-except保护关键逻辑", "理解finally适用场景"],
        "code": "try:\n    value = int(\"abc\")\nexcept ValueError:\n    value = 0\nprint(value)",
        "summary": "异常处理不是掩盖错误，而是让程序在可预期风险下仍可控。"
    },
    {
        "no": 12,
        "slug": "oop_basic",
        "title": "面向对象编程基础",
        "goals": ["定义类和对象", "理解属性与方法", "用封装表达业务实体"],
        "code": "class Student:\n    def __init__(self, name):\n        self.name = name\nprint(Student(\"Ada\").name)",
        "summary": "面向对象适合表达有状态、有行为的业务对象。"
    },
    {
        "no": 13,
        "slug": "oop_advanced",
        "title": "面向对象高级特性",
        "goals": ["使用类方法和静态方法", "理解属性方法", "认识抽象类与Mixin"],
        "code": "class Score:\n    def __init__(self, value):\n        self._value = value\n    @property\n    def passed(self):\n        return self._value >= 60",
        "summary": "高级特性要服务于清晰建模，而不是为了炫技增加复杂度。"
    },
    {
        "no": 14,
        "slug": "data_analysis",
        "title": "数据分析入门",
        "goals": ["理解数组和表格数据", "认识Pandas DataFrame", "完成基础统计思路"],
        "code": "scores = [88, 92, 75, 96]\navg = sum(scores) / len(scores)\nprint(round(avg, 1))",
        "summary": "数据分析的第一步不是画图，而是把数据结构和指标定义清楚。"
    },
    {
        "no": 15,
        "slug": "web_intro",
        "title": "Web开发入门",
        "goals": ["理解请求和响应", "认识路由概念", "了解REST接口风格"],
        "code": "def get_course(course_id):\n    return {\"id\": course_id, \"name\": \"Python\"}\nprint(get_course(1))",
        "summary": "Web开发本质是围绕资源设计接口，并让前后端稳定协作。"
    },
    {
        "no": 16,
        "slug": "crawler",
        "title": "爬虫实战",
        "goals": ["理解HTTP请求", "解析页面结构", "遵守 robots 和版权边界"],
        "code": "from urllib.parse import urlparse\nurl = \"https://example.com/page\"\nprint(urlparse(url).netloc)",
        "summary": "爬虫要先确认授权边界，再做请求、解析和存储。"
    },
    {
        "no": 17,
        "slug": "project_practice",
        "title": "Python项目实战综合案例",
        "goals": ["拆分需求和模块", "组织数据流和错误处理", "完成可运行项目闭环"],
        "code": "def main():\n    data = [1, 2, 3]\n    print(sum(data))\n\nif __name__ == \"__main__\":\n    main()",
        "summary": "综合项目考察的是把语法、数据结构、函数和工程组织串起来。"
    },
]


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(FONT_BOLD if bold else FONT_REGULAR, size)


def wrap_text(text: str, width: int) -> list[str]:
    lines: list[str] = []
    for paragraph in text.split("\n"):
        if not paragraph:
            lines.append("")
            continue
        line = ""
        for char in paragraph:
            test = line + char
            if len(test.encode("utf-8")) > width:
                lines.append(line)
                line = char
            else:
                line = test
        if line:
            lines.append(line)
    return lines


def draw_wrapped(draw: ImageDraw.ImageDraw, xy: tuple[int, int], text: str, ft: ImageFont.FreeTypeFont,
                 fill: str, width: int, line_gap: int = 10) -> int:
    x, y = xy
    for line in wrap_text(text, width):
        draw.text((x, y), line, font=ft, fill=fill)
        y += ft.size + line_gap
    return y


def draw_code(draw: ImageDraw.ImageDraw, xy: tuple[int, int], code: str, max_width: int = 72) -> int:
    x, y = xy
    box_w, box_h = 780, 250
    draw.rounded_rectangle((x, y, x + box_w, y + box_h), radius=18, fill="#0f172a", outline="#334155", width=2)
    code_font = font(30)
    cy = y + 28
    for line in code.split("\n"):
        for wrapped in textwrap.wrap(line, width=max_width, replace_whitespace=False) or [""]:
            draw.text((x + 28, cy), wrapped, font=code_font, fill="#d1fae5")
            cy += 42
    return y + box_h


def base_slide(lesson: dict, eyebrow: str, accent: str) -> tuple[Image.Image, ImageDraw.ImageDraw]:
    image = Image.new("RGB", (WIDTH, HEIGHT), "#f8fafc")
    draw = ImageDraw.Draw(image)
    draw.rectangle((0, 0, WIDTH, 92), fill="#111827")
    draw.rectangle((0, 92, WIDTH, 104), fill=accent)
    draw.text((62, 28), f"Python 微课 · 第{lesson['no']:02d}讲", font=font(28, True), fill="#ffffff")
    draw.text((970, 30), eyebrow, font=font(24), fill="#cbd5e1")
    return image, draw


def make_slides(lesson: dict) -> list[tuple[Image.Image, str]]:
    accent_colors = ["#2563eb", "#0891b2", "#7c3aed", "#dc2626", "#ca8a04", "#059669"]
    accent = accent_colors[(lesson["no"] - 1) % len(accent_colors)]
    slides: list[tuple[Image.Image, str]] = []

    image, draw = base_slide(lesson, "课程导入", accent)
    draw.text((64, 190), lesson["title"], font=font(58, True), fill="#0f172a")
    draw_wrapped(draw, (68, 300), lesson["summary"], font(32), "#475569", 58)
    draw.text((68, 600), "目标：用两分钟建立本课时的核心印象", font=font(28), fill=accent)
    slides.append((image, f"第{lesson['no']}讲，{lesson['title']}。{lesson['summary']}"))

    image, draw = base_slide(lesson, "学习目标", accent)
    draw.text((64, 150), "本节你需要抓住三件事", font=font(46, True), fill="#0f172a")
    y = 250
    for i, goal in enumerate(lesson["goals"], 1):
        draw.ellipse((76, y + 8, 112, y + 44), fill=accent)
        draw.text((87, y + 7), str(i), font=font(24, True), fill="#ffffff")
        draw_wrapped(draw, (136, y), goal, font(34), "#1e293b", 62)
        y += 94
    slides.append((image, "本节学习目标包括：" + "，".join(lesson["goals"]) + "。"))

    image, draw = base_slide(lesson, "核心概念", accent)
    draw.text((64, 148), "概念框架", font=font(46, True), fill="#0f172a")
    concept_text = "先确认问题边界，再选择合适语法结构，最后用清晰代码表达结果。"
    draw_wrapped(draw, (70, 230), concept_text, font(38), "#334155", 54)
    draw.rounded_rectangle((70, 440, 1170, 565), radius=22, fill="#ffffff", outline="#cbd5e1", width=2)
    draw.text((106, 480), "观察输入 → 设计处理 → 输出结果 → 检查异常", font=font(36, True), fill=accent)
    slides.append((image, f"{lesson['title']}的学习重点，是把语法知识放进输入、处理和输出的完整链路里。"))

    image, draw = base_slide(lesson, "代码演示", accent)
    draw.text((64, 140), "最小可运行示例", font=font(46, True), fill="#0f172a")
    draw_code(draw, (72, 230), lesson["code"])
    draw_wrapped(draw, (72, 535), "建议先手打一遍，再尝试替换输入数据观察结果。", font(30), "#475569", 60)
    slides.append((image, f"看一个最小代码例子。{lesson['title']}需要通过运行结果来确认理解是否正确。"))

    image, draw = base_slide(lesson, "课堂检查", accent)
    draw.text((64, 145), "一分钟自测", font=font(46, True), fill="#0f172a")
    checks = [
        "能否说清本节概念适合解决什么问题？",
        "能否独立写出一个最小示例？",
        "能否解释代码运行结果？",
    ]
    y = 242
    for check in checks:
        draw.rounded_rectangle((72, y, 1120, y + 70), radius=18, fill="#ffffff", outline="#dbe3ee", width=2)
        draw.text((104, y + 18), check, font=font(30), fill="#1e293b")
        y += 92
    slides.append((image, "课堂检查：说清使用场景，写出最小示例，并解释运行结果。"))

    image, draw = base_slide(lesson, "小结", accent)
    draw.text((64, 170), "本节小结", font=font(52, True), fill="#0f172a")
    draw_wrapped(draw, (72, 270), lesson["summary"], font(38), "#334155", 48)
    draw.rounded_rectangle((72, 520, 1130, 600), radius=18, fill=accent)
    draw.text((110, 540), "下一步：完成对应课时练习，并查看知识点掌握变化", font=font(30, True), fill="#ffffff")
    slides.append((image, f"本节小结：{lesson['summary']}完成后可以进入对应练习检查掌握情况。"))

    return slides


def vtt_timestamp(seconds: int) -> str:
    return f"00:{seconds // 60:02d}:{seconds % 60:02d}.000"


def write_vtt(path: Path, captions: list[str]) -> None:
    lines = ["WEBVTT", ""]
    for i, caption in enumerate(captions):
        start = i * SECONDS_PER_SLIDE
        end = (i + 1) * SECONDS_PER_SLIDE
        lines.append(f"{vtt_timestamp(start)} --> {vtt_timestamp(end)}")
        lines.append(caption)
        lines.append("")
    path.write_text("\n".join(lines), encoding="utf-8")


def ensure_builder() -> None:
    if BUILDER.exists():
        return
    cache_dir = ROOT / ".swift-module-cache"
    cache_dir.mkdir(parents=True, exist_ok=True)
    subprocess.run(
        ["swiftc", "-module-cache-path", str(cache_dir), str(SWIFT_SOURCE), "-o", str(BUILDER)],
        check=True,
    )


def build_video(frames_dir: Path, output: Path) -> None:
    subprocess.run([str(BUILDER), str(frames_dir), str(output), str(FPS)], check=True)


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    TMP_DIR.mkdir(parents=True, exist_ok=True)
    ensure_builder()

    for lesson in LESSONS:
        stem = f"lesson_{lesson['no']:02d}_{lesson['slug']}"
        frames_dir = TMP_DIR / stem
        if frames_dir.exists():
            shutil.rmtree(frames_dir)
        frames_dir.mkdir(parents=True)

        slides = make_slides(lesson)
        cover_path = OUT_DIR / f"{stem}_cover.png"
        slides[0][0].save(cover_path)

        frame_index = 0
        for slide, _caption in slides:
            for _ in range(SECONDS_PER_SLIDE):
                slide.save(frames_dir / f"frame_{frame_index:04d}.png")
                frame_index += 1

        write_vtt(OUT_DIR / f"{stem}.vtt", [caption for _slide, caption in slides])
        build_video(frames_dir, OUT_DIR / f"{stem}.mp4")
        shutil.rmtree(frames_dir)
        print(f"generated {stem}.mp4")

    shutil.rmtree(TMP_DIR, ignore_errors=True)
    print(f"done: {OUT_DIR}")


if __name__ == "__main__":
    main()
