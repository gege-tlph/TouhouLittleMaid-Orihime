"""打印当前仓库事实。

设计意图：**机器能算的事实，文档里一律不要手写**。
本项目反复出现「文档声明的数字早已过时」（排除 78 条实际 2 条、HANDOFF 声称四个未提交文件实际为零、
RELEASE_WORKFLOW §12 指着已被取代的发布 SHA）。这类声明之所以能活很久，是因为**没有任何东西会执行文档**。

本脚本不生成文件——生成的文件同样会过时。它只在被调用时输出当下的真相，
所以文档应当写「运行 `python docs/tools/facts.py`」，而不是把数字抄进正文。

用法：python docs/tools/facts.py            完整快照
      python docs/tools/facts.py --brief    单屏摘要（接手时先看这个）
"""
import json
import pathlib
import re
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]

#: 常驻入口固定这 5 份；docs/ 下其余 *.md 视为在建设计文档，至多 1 份，实施完成即进 archive。
#: 1000 行预算**只约束常驻部分**——在建文档注定要走，把它算进预算会逼着去压不该压的常驻内容。
#:
#: 2026-07-28 曾短暂提到 1200，随即改回：**恢复余量靠归档，不靠调数**。当天 YSM 盘点进 archive
#: 加上账本按「开放项在前、已关闭压成一行」重写，合计从 1278 直接回到 846——
#: 上一次涨到 848 行的成因是只增不删（同一件事被写成设计提案 / 实施记录 / 验收清单三份），
#: 不是内容真的变多了。**写新段落时先问「这段取代了谁」，把被取代的删掉。**
PERMANENT_DOCS = {"README.md", "CURRENT_STATUS.md", "HANDOFF.md", "RELEASE_WORKFLOW.md", "COMPAT.md"}
DOC_BUDGET = 1000


def split_docs():
    docs = sorted((ROOT / "docs").glob("*.md"))
    return ([d for d in docs if d.name in PERMANENT_DOCS],
            [d for d in docs if d.name not in PERMANENT_DOCS])


def doc_line_count(doc):
    return len(doc.read_text(encoding="utf-8", errors="replace").splitlines())


def git(*args, default=""):
    try:
        out = subprocess.run(["git", "-C", str(ROOT), *args],
                             capture_output=True, check=False, timeout=60)
        if out.returncode != 0:
            return default
        return out.stdout.decode("utf-8", errors="replace").strip()
    except Exception:
        return default


def section(title):
    print()
    print(title)
    print("-" * len(title))


def count_tests():
    """(junit 总数, junit 失败, gametest 总数, gametest 失败)；未跑过的位置为 None。"""
    junit_total = junit_failed = None
    junit_dir = ROOT / "build/test-results/test"
    if junit_dir.is_dir():
        junit_total = junit_failed = 0
        for report in junit_dir.glob("*.xml"):
            head_text = report.read_text(encoding="utf-8", errors="replace")[:400]
            m = re.search(r'tests="(\d+)".*?failures="(\d+)".*?errors="(\d+)"', head_text)
            if m:
                junit_total += int(m.group(1))
                junit_failed += int(m.group(2)) + int(m.group(3))

    game_total = game_failed = None
    gametest_report = ROOT / "build/gametest/report.xml"
    if gametest_report.is_file():
        import xml.etree.ElementTree as ET
        root = ET.parse(gametest_report).getroot()
        cases = list(root.iter("testcase"))
        game_total = len(cases)
        game_failed = sum(1 for c in cases if any(x.tag in ("failure", "error") for x in c))
    return junit_total, junit_failed, game_total, game_failed


def brief():
    """单屏摘要。只回答「我接手时该知道的最少事实」，不替代完整快照。"""
    branch = git("rev-parse", "--abbrev-ref", "HEAD")
    head = git("rev-parse", "--short", "HEAD")
    subject = git("log", "-1", "--format=%s")
    dirty = git("status", "--porcelain").splitlines()
    tree = "干净" if not dirty else f"{len(dirty)} 个改动"
    print(f"{branch} @ {head}  [{tree}]  {subject}")

    junit_total, junit_failed, game_total, game_failed = count_tests()
    parts = []
    for label, total, failed in (("JUnit", junit_total, junit_failed),
                                 ("GameTest", game_total, game_failed)):
        if total is None:
            parts.append(f"{label} 未跑")
        else:
            parts.append(f"{label} {total} 项/失败 {failed}" + (" ★" if failed else ""))
    print("  " + " · ".join(parts) + "   （上次构建的结果，不代表当前工作树）")

    build_gradle = (ROOT / "build.gradle").read_text(encoding="utf-8", errors="replace")
    active = len(re.findall(r"^\s*sourceSets\.main\.java\.exclude", build_gradle, re.M))
    permanent, drafts = split_docs()
    perm_lines = sum(doc_line_count(d) for d in permanent)
    draft_note = f" · 在建设计 {len(drafts)} 份/{sum(doc_line_count(d) for d in drafts)} 行" if drafts else ""
    print(f"  活跃排除 {active} 条 · 常驻文档 {len(permanent)} 份/{perm_lines} 行（预算 {DOC_BUDGET}）{draft_note}"
          + ("  ★ 常驻应为 5 份" if len(permanent) != 5 else "")
          + ("  ★ 在建设计最多 1 份" if len(drafts) > 1 else "")
          + ("  ★ 常驻超预算" if perm_lines > DOC_BUDGET else ""))
    print("  完整快照：python docs/tools/facts.py")
    return 0


def main():
    if "--brief" in sys.argv[1:]:
        return brief()
    print("=" * 66)
    print("仓库事实快照 —— 由 docs/tools/facts.py 生成，不要把这些数字抄进文档")
    print("=" * 66)

    section("Git")
    branch = git("rev-parse", "--abbrev-ref", "HEAD")
    head = git("rev-parse", "--short", "HEAD")
    subject = git("log", "-1", "--format=%s")
    dirty = git("status", "--porcelain")
    print(f"  分支      {branch}")
    print(f"  HEAD      {head}  {subject}")
    print(f"  工作树    {'干净' if not dirty else str(len(dirty.splitlines())) + ' 个改动'}")
    if dirty:
        for line in dirty.splitlines()[:10]:
            print(f"            {line}")

    section("发布")
    tags = git("tag", "--list", "v*").splitlines()
    current = [t for t in tags if "mc26.1.2" in t]
    for tag in current:
        target = git("rev-list", "-n", "1", tag)
        print(f"  {tag:<28} -> {target[:8]}")
    print(f"  （另有 {len(tags) - len(current)} 个更早版本的历史标签，未列出）")
    public = git("rev-parse", "--short", "fork/port/26.1.2-fabric", default="(未 fetch)")
    clean = git("rev-parse", "--short", "release/26.1.2-clean", default="(不存在)")
    print(f"  公开远端分支 fork/port/26.1.2-fabric -> {public}")
    print(f"  本地清洁分支 release/26.1.2-clean     -> {clean}")

    section("构建配置")
    build_gradle = (ROOT / "build.gradle").read_text(encoding="utf-8", errors="replace")
    active = len(re.findall(r"^\s*sourceSets\.main\.java\.exclude", build_gradle, re.M))
    commented = len(re.findall(r"^\s*//.*sourceSets\.main\.java\.exclude", build_gradle, re.M))
    print(f"  活跃 source-set 排除   {active} 条")
    print(f"  已注释历史锚点         {commented} 条")

    section("注册完整性")
    resources = ROOT / "src/main/resources"
    sources = ROOT / "src/main/java"
    mod = json.loads((resources / "fabric.mod.json").read_text(encoding="utf-8"))
    gametests = mod.get("entrypoints", {}).get("fabric-gametest", [])
    on_disk = sum(1 for _ in sources.rglob("*GameTest.java"))
    print(f"  GameTest 类  磁盘 {on_disk} / entrypoint 登记 {len(gametests)}"
          f"  {'一致' if on_disk == len(gametests) else '★ 不一致'}")
    for name in ("touhou_little_maid.mixins.json", "touhou_little_maid_fabric.mixins.json"):
        cfg = json.loads((resources / name).read_text(encoding="utf-8"))
        listed = sum(len(cfg.get(k) or []) for k in ("mixins", "client", "server"))
        pkg_dir = sources / cfg["package"].replace(".", "/")
        files = sum(1 for _ in pkg_dir.rglob("*.java")) if pkg_dir.is_dir() else 0
        print(f"  {name:<38} 磁盘 {files} / 登记 {listed}")
    print("  （登记数可大于文件数：嵌套 mixin Outer$Inner 与外层类同处一个源文件。"
          "严格核对由 RegistrationInvariantTest 负责）")

    section("测试")
    junit_dir = ROOT / "build/test-results/test"
    if junit_dir.is_dir():
        total = failed = 0
        for report in junit_dir.glob("*.xml"):
            head_text = report.read_text(encoding="utf-8", errors="replace")[:400]
            m = re.search(r'tests="(\d+)".*?failures="(\d+)".*?errors="(\d+)"', head_text)
            if m:
                total += int(m.group(1))
                failed += int(m.group(2)) + int(m.group(3))
        print(f"  JUnit  {total} 项，失败/错误 {failed}   （上次 build 的结果）")
    else:
        print("  JUnit  尚未运行")

    gametest_report = ROOT / "build/gametest/report.xml"
    if gametest_report.is_file():
        import xml.etree.ElementTree as ET
        root = ET.parse(gametest_report).getroot()
        cases = list(root.iter("testcase"))
        bad = [c for c in cases if any(x.tag in ("failure", "error") for x in c)]
        print(f"  GameTest  {len(cases)} 项，失败 {len(bad)}   （上次 runGametest 的结果）")
        for case in bad:
            print(f"            ★ {case.get('name')}")
    else:
        print("  GameTest  尚未运行")

    section("文档")
    permanent, drafts = split_docs()
    perm_lines = 0
    for doc in permanent:
        lines = doc_line_count(doc)
        perm_lines += lines
        print(f"  {doc.name:<26} {lines:>5} 行")
    print(f"  {'常驻合计':<24} {perm_lines:>5} 行 / 预算 {DOC_BUDGET}"
          f"  {'超出 ★' if perm_lines > DOC_BUDGET else 'OK'}")
    if len(permanent) != 5:
        print(f"  ★ 常驻文档应为 5 份，实际 {len(permanent)} 份")
    for doc in drafts:
        print(f"  {doc.name:<26} {doc_line_count(doc):>5} 行  在建设计，不计入预算，实施完成即进 archive")
    if len(drafts) > 1:
        print(f"  ★ 在建设计文档至多 1 份，实际 {len(drafts)} 份")

    print()
    return 0


if __name__ == "__main__":
    sys.exit(main())
