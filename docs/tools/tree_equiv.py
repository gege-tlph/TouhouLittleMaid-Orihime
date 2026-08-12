"""忽略注释的树等价审计：内部开发分支 vs 公开清洁分支。

工程红线是「功能改动不得只存在于公开分支」，但靠纪律守不住——2026-07-26 就查出三处构建修复
（ModMenu 依赖源、Patchouli transitive、Terraformers 仓库）只存在于公开分支。规则必须机械化。

注意：**按空白分词会产生假阳性**。注释可能紧贴代码，例如
`durability(128)/*.setNoRepair()*/)` 剥注释后变成两个空白分隔的 token。
因此这里剥完注释**去掉全部空白比较字符流**。

用法：python docs/tools/tree_equiv.py [内部ref] [公开ref]
"""
import re
import subprocess
import sys
import pathlib

ROOT = pathlib.Path(__file__).resolve().parents[2]

BLOCK = re.compile(r"/\*.*?\*/", re.S)
LINE = re.compile(r"//[^\n]*")
STRING = re.compile(r'"(?:\\.|[^"\\])*"', re.S)
HASH = re.compile(r"#[^\n]*")

#: 这些路径**允许**两侧不同：内部专用文档/配置，或按设计只在一侧维护的 CI。
ALLOWED_DIVERGENCE = (
    "docs/", ".codex", "AGENTS.md", "CLAUDE.md", ".mcp.json",
    ".ignore", ".github/", "README.md",
)


def git(*args):
    """git 失败必须炸，不能静默返回空。

    2026-07-29 实测：本函数原来是 check=False 且只取 stdout，于是 `git diff` 因为拿到一个
    不存在的 ref 而失败时返回空字符串，被上游读成「零差异」，`-Release` 那道闸因此打出绿灯。
    **空输出与命令失败必须可区分**——这正是本仓库反复栽的那类：判据分不清「没有」与「没测到」。
    """
    out = subprocess.run(["git", "-C", str(ROOT), *args], capture_output=True, check=False)
    if out.returncode != 0:
        raise SystemExit(f"git {' '.join(args)} 失败（退出码 {out.returncode}）："
                         f"{out.stderr.decode('utf-8', errors='replace').strip()}")
    return out.stdout


def show(rev, path):
    out = subprocess.run(["git", "-C", str(ROOT), "show", f"{rev}:{path}"],
                         capture_output=True, check=False)
    return None if out.returncode != 0 else out.stdout.decode("utf-8", errors="replace")


def strip_comments(text, java_like):
    """单遍扫描去注释；字符串字面量原样保留。

    **不要退回正则版。** 原来的写法是「先用正则把双引号串挖出来占位，再删注释」，
    它漏了 Groovy 的**单引号串**：`build.gradle` 里 `exclude 'META-INF/maven/**/*'` 的 `/*`
    于是被当成块注释开头，`DOTALL` 一路吃到下一个 `*/`——1035 行的内部 build.gradle
    被吃到只剩 50 行非空（2026-07-29 实证）。而 build.gradle 正是「构建修复不得只活在
    公开分支」这条规则最依赖的文件，剥错了就等于不比。

    也不要「顺手」把单引号加进那个正则：注释里的撇号（英文 it's）会反过来伪造出一个
    跨行字符串，把真注释保住。字符串与注释必须在同一遍里互斥地识别，这就是本函数。
    """
    if not java_like:
        return HASH.sub(" ", text)

    out = []
    i, n = 0, len(text)
    while i < n:
        c = text[i]
        if c in "\"'":
            quote = c
            j = i + 1
            while j < n:
                if text[j] == "\\":
                    j += 2
                    continue
                if text[j] == quote:
                    j += 1
                    break
                j += 1
            out.append(text[i:j])
            i = j
        elif c == "/" and i + 1 < n and text[i + 1] == "/":
            j = text.find("\n", i)
            i = n if j < 0 else j
            out.append(" ")
        elif c == "/" and i + 1 < n and text[i + 1] == "*":
            j = text.find("*/", i + 2)
            i = n if j < 0 else j + 2
            out.append(" ")
        else:
            out.append(c)
            i += 1
    return "".join(out)


def normalise(path, text):
    if text is None:
        return None
    java_like = path.endswith((".java", ".gradle"))
    hash_like = path.endswith((".yml", ".yaml", ".properties"))
    if java_like or hash_like:
        text = strip_comments(text, java_like)
    return re.sub(r"\s+", "", text)


def resolve(ref):
    """把 ref 解析成一个 git 对象，解析不了就停——**绝不让一个坏 ref 表现成「零差异」**。

    不限定成 commit：发布流程里要拿「暂存区写出的 tree」与内部分支比对（同步做完、
    发布提交还没建的那一刻），而 tree 不是 commit。坏参数照样挡得住——`--release`
    这类根本不是对象名。
    """
    out = subprocess.run(["git", "-C", str(ROOT), "rev-parse", "--verify", "--quiet", ref],
                         capture_output=True, check=False)
    if out.returncode != 0:
        raise SystemExit(f"★ ref 无法解析：{ref}\n"
                         f"  （若你是想传开关，注意开关必须以 -- 开头且不占位置参数）")
    return out.stdout.decode().strip()


def main():
    # 位置参数与开关分开取。原来直接用 sys.argv[1] 当内部 ref，
    # 于是 release-gate.ps1 传来的 `--release` 被当成了 ref——那次假绿的直接成因。
    flags = [a for a in sys.argv[1:] if a.startswith("-")]
    positional = [a for a in sys.argv[1:] if not a.startswith("-")]
    unknown = [f for f in flags if f != "--release"]
    if unknown:
        raise SystemExit(f"★ 不认识的开关：{' '.join(unknown)}")
    if len(positional) > 2:
        raise SystemExit(f"★ 位置参数最多两个（内部 ref、公开 ref），收到：{positional}")

    internal = positional[0] if positional else "HEAD"
    public = positional[1] if len(positional) > 1 else "release/26.1.2-clean"
    resolve(internal)
    resolve(public)

    changed = [p for p in git("diff", "--name-only", internal, public)
               .decode("utf-8").splitlines() if p.strip()]

    comment_only, real, one_sided = [], [], []
    for path in changed:
        if path.startswith(ALLOWED_DIVERGENCE):
            continue
        left, right = show(internal, path), show(public, path)
        if left is None or right is None:
            one_sided.append((path, "仅公开有" if left is None else "仅内部有"))
            continue
        if normalise(path, left) == normalise(path, right):
            comment_only.append(path)
        else:
            real.append(path)

    print(f"比较 {internal} vs {public}")
    print(f"  差异文件 {len(changed)}（已排除设计内分叉：{', '.join(ALLOWED_DIVERGENCE)}）")
    print(f"  仅注释差异 {len(comment_only)}")
    print(f"  ★ 真实差异 {len(real)}")
    for path in real:
        print(f"      {path}")
    print(f"  ★ 单侧存在 {len(one_sided)}")
    for path, side in one_sided:
        print(f"      {path}  ({side})")

    public_only = [p for p, side in one_sided if side == "仅公开有"]
    release_mode = "--release" in flags

    # 「只在公开树里有」有两种成因，处置完全相反，必须分开：
    #   1. 内部从来没有过它 —— 这正是本规则要抓的：功能/依赖/构建修复只活在公开树。
    #   2. 内部曾经有、已被**故意删除** —— 公开树只是还没收到这次删除，下次发布同步即消失。
    # 分辨证据是内部分支的历史：删过就查得到那次提交，从未存在则查不到任何提交。
    # 不分开的后果是：每删掉一个功能，门禁就长期变红，而长期红的门禁等于没有门禁
    # —— v0.8.4 正是在 required 用例红着的状态下发的。
    never_internal, deleted_internal = [], []
    for path in public_only:
        if git("rev-list", "-n1", internal, "--", path).strip():
            deleted_internal.append(path)
        else:
            never_internal.append(path)

    if deleted_internal:
        print(f"\n  内部已删除、公开树待同步 {len(deleted_internal)}（不判失败）")
        for path in deleted_internal:
            print(f"      {path}")

    if never_internal:
        print("\n★ 失败：以下文件只存在于公开分支，且内部分支历史中从未出现过。"
              "功能、依赖或构建修复不得只活在公开树，必须回写内部开发分支并重跑门禁。")
        for path in never_internal:
            print(f"      {path}")
        return 1

    if release_mode and (real or one_sided):
        print("\n★ 发布模式失败：同步后两树的非注释内容应当等价，上述差异需逐条定案。")
        return 1

    if real or one_sided:
        print("\n（审计模式）上述差异属于「内部领先公开树」，两次发布之间正常。"
              "发布同步后请用 --release 复跑，届时要求完全等价。")
        return 0

    print("\n两树非注释内容等价。")
    return 0


if __name__ == "__main__":
    sys.exit(main())
