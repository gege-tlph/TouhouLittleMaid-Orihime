"""文档 lint —— 让文档也有「编译器」。

代码有编译器，文档什么都没有：死链、不存在的路径、改名后残留的旧仓库名、
指向已被取代的提交，都能在文档里活很久，因为没有任何东西会执行文档。

本脚本只抓**语法性**错误（指向的东西不存在），抓不住语义过期
（例如某个 SHA 真实存在、只是已被取代）——那类要靠 facts.py 与人工判断。

退出码非零即失败，可直接进发布门禁。
用法：python docs/tools/doc_lint.py
"""
import pathlib
import re
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
DOCS = ROOT / "docs"

#: 常驻入口 5 份，加上当前唯一在建设计文档；其它一律进 archive。
#: 另允许**至多一份在建设计文档**：它随实施完成移入 archive，不常驻。
#: 加进来的那一份必须同时登记到 docs/README.md 的表里，否则读者找不到它。
EXPECTED_DOCS = {
    "README.md",
    "CURRENT_STATUS.md",
    "HANDOFF.md",
    "RELEASE_WORKFLOW.md",
    "COMPAT.md",
    # 当前唯一在建设计文档；完成后移入 docs/archive/，同时删除此行。
    "PORT_26X_AUDIT.md",
}

#: 命中即失败。仓库改名与术语统一后的残留最容易漏。
FORBIDDEN = [
    # 禁的是**我们自己**的旧名。行为基准仓库 `Sh1roCu/TouhouLittleMaid-Orihime` 是另一个项目，
    # 文档里正当地要提它——原式无差别匹配，逼得作者只能绕着写「行为基准所在仓库」。
    (re.compile(r"(?<!Sh1roCu/)TouhouLittleMaid-Orihime"), "本仓库已改名为 TouhouLittleMaid-Tsumugi"),
    (re.compile(r"女佣|侍女|女僕"), "术语统一为「女仆」"),
    (re.compile(r"TLM_TRANSLATION"), "内部翻译标记不应出现在文档"),
    (re.compile(r"\bP[0-9]{2,}_[A-Z]"), "内部 Pxx 锚点不应出现在活跃文档"),
]

LINK = re.compile(r"\[[^\]]*\]\(([^)]+)\)")
#: 文档里提到的保护标签必须真实存在。重命名 backup ref 后最容易留下这类死引用。
BACKUP_REF = re.compile(r"`(backup/[A-Za-z0-9_./{}*,-]+)`")
BACKTICK_PATH = re.compile(r"`((?:docs|src|gradle)/[A-Za-z0-9_./{}*,-]+)`")
SHORT_SHA = re.compile(r"(?<![0-9a-zA-Z`])`?([0-9a-f]{7,12})`?(?![0-9a-zA-Z])")

problems: list[str] = []


def fail(doc, line_no, message):
    problems.append(f"{doc}:{line_no}  {message}")


def git_object_exists(sha):
    out = subprocess.run(["git", "-C", str(ROOT), "cat-file", "-e", sha + "^{commit}"],
                         capture_output=True, check=False)
    return out.returncode == 0


def main():
    actual = {p.name for p in DOCS.glob("*.md")}
    if actual != EXPECTED_DOCS:
        extra = sorted(actual - EXPECTED_DOCS)
        missing = sorted(EXPECTED_DOCS - actual)
        if extra:
            problems.append(f"docs/ 多出非维护文档：{extra}（应移入 docs/archive/）")
        if missing:
            problems.append(f"docs/ 缺少维护文档：{missing}")

    for doc in sorted(DOCS.glob("*.md")):
        text = doc.read_text(encoding="utf-8", errors="replace")
        in_fence = False
        for line_no, line in enumerate(text.splitlines(), start=1):
            if line.lstrip().startswith("```"):
                in_fence = not in_fence
                continue

            # 代码块里出现禁用词通常正是「检查禁用词的命令」本身，不算违规。
            if not in_fence:
                for pattern, reason in FORBIDDEN:
                    match = pattern.search(line)
                    # 「已改名/301 跳转/当时名」等历史说明里允许出现旧名。
                    if match and not re.search(r"改名|301|当时名|历史|旧址", line):
                        fail(doc.name, line_no, f"禁用词 {match.group(0)!r}：{reason}")

            for target in LINK.findall(line):
                if target.startswith(("http://", "https://", "#")):
                    continue
                resolved = (doc.parent / target.split("#", 1)[0]).resolve()
                if not resolved.exists():
                    fail(doc.name, line_no, f"死链 {target}")

            for path in BACKTICK_PATH.findall(line):
                # 通配/占位路径不校验。
                if any(ch in path for ch in "*{},") or path.endswith("/"):
                    continue
                if not (ROOT / path).exists():
                    fail(doc.name, line_no, f"路径不存在 {path}")

            # 截断的长摘要由下面那条**逐 token** 的省略号守卫处理，不要在这里整行跳过：
            # 一行里往往既有摘要又有真实提交（§12 的历史发布条目就是），
            # 整行跳过会让那些真 ref 静默逃过校验——2026-07-31 实证：同一个不存在的外部 SHA
            # 写在 HANDOFF 被抓到，写在 CURRENT_STATUS 因同行有「SHA-256」而放行。
            for ref in BACKUP_REF.findall(line):
                # 含占位符的是命名规范示例，不是具体 ref。
                if any(ch in ref for ch in "<>{}*,"):
                    continue
                exists = subprocess.run(
                    ["git", "-C", str(ROOT), "rev-parse", "--verify", "--quiet", ref],
                    capture_output=True, check=False).returncode == 0
                if not exists:
                    fail(doc.name, line_no, f"引用了不存在的保护 ref {ref}")

            for sha in SHORT_SHA.findall(line):
                # 纯数字或全字母不可能是我们引用的提交，避免版本号误报。
                if sha.isdigit() or not re.search(r"[0-9]", sha) or not re.search(r"[a-f]", sha):
                    continue
                # 省略号紧随其后 = 截断的长摘要，不是提交。
                if re.search(re.escape(sha) + r"`?\s*(…|\.\.\.)", line):
                    continue
                if not git_object_exists(sha):
                    fail(doc.name, line_no, f"引用了不存在的提交 {sha}")

    if problems:
        print(f"文档 lint 失败，共 {len(problems)} 处：")
        for problem in problems:
            print(f"  - {problem}")
        return 1

    print(f"文档 lint 通过：{len(EXPECTED_DOCS)} 份维护文档，链接、路径、提交引用与禁用词均无问题。")
    return 0


if __name__ == "__main__":
    sys.exit(main())
