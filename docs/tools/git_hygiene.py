"""本地 git 卫生检查。

规则只靠记忆维持就会破——2026-07-26 一次审计查出：备份 ref 一半建成分支一半建成标签、
备份名里的版本号写错、两个被 build 编译的测试文件从未纳入版本控制、
142 MiB 中断残留躺了六天。这些都不是「谁不守规矩」，而是**没有任何检查会发现**。

退出码非零即失败，直接进发布门禁。
用法：python docs/tools/git_hygiene.py
"""
import pathlib
import re
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]

#: 26.1.2 的备份必须带版本段，例如 backup/26.1.2/release/20260822/dev。
TARGET_BACKUP_NAME = re.compile(r"^backup/26\.1\.2/[a-z0-9-]+/\d{8}/(dev|public)$")
# 共享 git 仓库里还保留 1.21.11 的旧命名；它们属于另一条工作树，不能被本分支误判。
LEGACY_BACKUP_NAME = re.compile(r"^backup/[a-z0-9-]+/\d{8}/(dev|public)$")

#: 中断残留超过这个体积就该清理。
GARBAGE_LIMIT_MIB = 50

#: 本地清洁分支，以及它对应的公开远端分支。
CLEAN_BRANCH = "release/26.1.2-clean"
PUBLIC_BRANCH = "fork/port/26.1.2-fabric"

problems: list[str] = []
notes: list[str] = []


def git(*args):
    """预期成功的 git 调用；失败必须炸，不能静默返回空字符串。

    空输出在本文件里几乎处处被读成「没有问题」（没有未跟踪源文件、没有备份标签……），
    所以一次静默失败就是一次假绿。同款缺陷 2026-07-29 在 tree_equiv.py 上实证过：
    `-Release` 那道闸因此从上线起就没真正跑过。**预期会失败的调用请用 git_optional()。**
    """
    out = subprocess.run(["git", "-C", str(ROOT), *args], capture_output=True, check=False)
    if out.returncode != 0:
        raise SystemExit(f"git {' '.join(args)} 失败（退出码 {out.returncode}）："
                         f"{out.stderr.decode('utf-8', errors='replace').strip()}")
    return out.stdout.decode("utf-8", errors="replace").strip()


def git_optional(*args):
    """允许失败的 git 调用（如 rev-parse --verify --quiet 查一个可能不存在的 ref）。"""
    out = subprocess.run(["git", "-C", str(ROOT), *args], capture_output=True, check=False)
    return out.stdout.decode("utf-8", errors="replace").strip()


def git_ok(*args):
    return subprocess.run(["git", "-C", str(ROOT), *args],
                          capture_output=True, check=False).returncode == 0


def check_no_untracked_sources():
    """构建不允许依赖未跟踪文件：一个 git clean 就能让它们无声消失。"""
    untracked = git("ls-files", "--others", "--exclude-standard", "src/").splitlines()
    offenders = [p for p in untracked if p.endswith((".java", ".json", ".mcmeta"))]
    for path in offenders:
        problems.append(f"src/ 下存在未跟踪源文件：{path}"
                        f"（它会被编译/打包，但不在版本控制里；新克隆与 CI 拿不到它）")


def check_backup_refs():
    """备份必须是标签且命名统一，否则出事时找不全。"""
    branches = [b.strip().lstrip("* +").strip()
                for b in git("branch", "--list", "backup/*").splitlines() if b.strip()]
    for branch in branches:
        problems.append(f"备份 {branch} 建成了分支：备份一律用标签"
                        f"（不可被误 checkout、不污染 git branch、语义就是钉住不动）")

    legacy = []
    for tag in git("tag", "--list", "backup/*").splitlines():
        if TARGET_BACKUP_NAME.match(tag):
            continue
        if LEGACY_BACKUP_NAME.match(tag):
            legacy.append(tag)
            continue
        problems.append(f"备份标签命名不合规：{tag}"
                        f"（26.1.2 应为 backup/26.1.2/<原因>/<YYYYMMDD>/<dev|public>）")
    if legacy:
        notes.append(f"共享仓库保留 {len(legacy)} 个旧版备份标签；本分支只使用带 26.1.2 段的新命名")


def check_unreachable_backups():
    """指向不可达提交的备份是唯一的存档，必须显式提示不可删除。"""
    for tag in git("tag", "--list", "backup/26.1.2/*").splitlines():
        sha = git("rev-parse", f"{tag}^{{commit}}")
        if not sha:
            continue
        containing = [line.strip().lstrip("* +").strip()
                      for line in git("branch", "--contains", sha).splitlines() if line.strip()]
        if not containing:
            notes.append(f"{tag} -> {sha[:8]}  ★ 不可从任何分支到达，删除该标签即永久丢失该历史")


def check_garbage():
    output = git("count-objects", "-v")
    size_kib = 0
    for line in output.splitlines():
        if line.startswith("size-garbage:"):
            size_kib = int(line.split(":")[1].strip())
    size_mib = size_kib / 1024
    if size_mib > GARBAGE_LIMIT_MIB:
        problems.append(f"中断残留 {size_mib:.1f} MiB 超过 {GARBAGE_LIMIT_MIB} MiB："
                        f"先确认无 git 进程在跑，再 `git gc` 并删除 .git/objects/**/tmp_obj_*")


def check_clean_branch_contains_public_tip():
    """清洁分支必须包含公开远端 tip，否则下次发布会把远端上的提交顶掉。

    公开仓库上会出现不是我们推的提交（v0.8.4 就快进叠在用户自己的 README 提交之上）。
    只要本地清洁分支没有把远端 tip 含进来，下一次发布要么被拒、要么只能 --force 覆盖，
    而 --force 会静默丢掉那些提交。这条检查让「该先 fetch 并把远端并进来」在发布前就暴露。
    """
    clean = git_optional("rev-parse", "--verify", "--quiet", f"{CLEAN_BRANCH}^{{commit}}")
    public = git_optional("rev-parse", "--verify", "--quiet", f"{PUBLIC_BRANCH}^{{commit}}")
    if not clean:
        notes.append(f"本地清洁分支 {CLEAN_BRANCH} 不存在，跳过「含远端 tip」检查")
        return
    if not public:
        notes.append(f"远端分支 {PUBLIC_BRANCH} 本地无记录（先 git fetch），跳过「含远端 tip」检查")
        return
    if not git_ok("merge-base", "--is-ancestor", public, clean):
        problems.append(
            f"{CLEAN_BRANCH}（{clean[:8]}）未包含 {PUBLIC_BRANCH}（{public[:8]}）作为祖先："
            f"远端有本地没有的提交，此时发布会被拒或需要 --force 覆盖掉它们。"
            f"先 `git fetch` 再把远端 tip 并入清洁分支")


def check_worktrees_clean():
    for line in git("worktree", "list", "--porcelain").splitlines():
        if line.startswith("worktree "):
            path = line[len("worktree "):]
            dirty = subprocess.run(["git", "-C", path, "status", "--porcelain"],
                                   capture_output=True, check=False)
            count = len([x for x in dirty.stdout.decode("utf-8", "replace").splitlines() if x.strip()])
            if count:
                notes.append(f"worktree {path} 有 {count} 处未提交改动")


def main():
    check_no_untracked_sources()
    check_backup_refs()
    check_unreachable_backups()
    check_garbage()
    check_clean_branch_contains_public_tip()
    check_worktrees_clean()

    if notes:
        print("提示（不判失败）：")
        for note in notes:
            print(f"  · {note}")
        print()

    if problems:
        print(f"git 卫生检查失败，共 {len(problems)} 处：")
        for problem in problems:
            print(f"  - {problem}")
        return 1

    print("git 卫生检查通过：无未跟踪源文件，备份 ref 命名与类型统一，"
          "清洁分支含公开远端 tip，无超量中断残留。")
    return 0


if __name__ == "__main__":
    sys.exit(main())
