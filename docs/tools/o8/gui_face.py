# -*- coding: utf-8 -*-
"""行为面对账 · GUI 面（O8 第四面）

契约不是「有哪些屏类」，而是 **「哪个屏，在什么权限 / 状态 / 条件下出现」**。
故本脚本产出四项，每项都按**成因**取判据，不按类名或方法名：

  1. 菜单注册 id      —— 服务端与客户端共同认得的那个键（改类名不改它，改它就是协议变更）
  2. 屏绑定完整性    —— 每个 MenuType 是否都有 MenuScreens.register；有没有绑了却没注册的
  3. 打开点与守卫    —— 谁调用 openMenu / setScreen，其**所在方法体**里有哪些条件类别
  4. 纯客户端屏      —— 不经菜单的屏（setScreen 直开），同样取守卫

⚠ 判据设计上的三个坑（均为本仓库实证过的形态）：
  - 不按屏类名对差：类名在宿主重构里改过，改名不是行为差异；菜单 id 才是契约。
  - 不按方法名找守卫：守卫可能落在任意私有 helper 或 lambda 里，故取**整个方法体**
    （大括号配对），并按「成因词类」归类而不是按具体写法。
  - 活性数必须与结论正交：扫了几个文件 / 认出几个 id / 抓到几个打开点，
    与「差异 0」是两个独立的量；只报后者时，解析器坏掉与真的没差异长得一模一样。
"""
import os as _os
import re
import subprocess
from collections import defaultdict

# 仓库根 = 本文件所在目录向上三级（docs/tools/o8/x.py → 仓库根）
REPO = _os.path.dirname(_os.path.dirname(_os.path.dirname(
    _os.path.dirname(_os.path.abspath(__file__)))))
REFS = {"HEAD": "本树", "port/1.21.11-fabric": "基准", "origin/26.1": "宿主"}

RE_MENU_REG = re.compile(r'register\s*\(\s*"([^"]+)"\s*,\s*(\w+)\.')
RE_SCREEN_REG = re.compile(r'MenuScreens\.register\s*\(\s*[\w.]*?(\w+)\s*,\s*(\w+)::')
RE_OPEN_MENU = re.compile(r'\bopenMenu\s*\(')
# ⚠ 2026-08-18 修：原判据是 `setScreen(new XGui(...))`，把**经 helper 间接打开**的屏
#   全判成「本树没有打开点」（实证：ChairModelGui 在本树与基准都走
#   CacheIconManager.openChairModelGui，两树一致，却被报成缺口）。
#   改按成因：一个屏要能出现，**必须在它自己文件之外被构造过**。构造点不问写法。
RE_SCREEN_DECL = re.compile(r'\bclass\s+(\w+)\s+extends\s+\w*(?:Screen|Gui)\b')

# 守卫「成因」词类——问的是「这条路径上验了什么」，不是「写成什么样」
GUARD_KINDS = {
    "归属": r"isOwnedBy|getOwnerUUID|isOwner\b|ownerUUID|sameOwner|getOwner\s*\(",
    "权限": r"hasPermissions|hasPermission|canEditSite|permissionLevel|isOp\b|isCreative|GameModeUtil",
    "距离/可达": r"closerThan|distanceTo|distanceToSqr|canInteractWith|isAlive\b",
    "潜行/手": r"isDiscrete|isShiftKeyDown|isSecondaryUseActive|InteractionHand",
    "模组存在": r"isModLoaded",
    "配置开关": r"ServerRuleConfig\.get|Config\.[A-Z_]+\.get\s*\(",
    "端判定": r"isClientSide|instanceof\s+ServerPlayer|level\(\)\.isClientSide",
    "状态": r"hasBackpack|isMaidInSittingPose|isSleeping|getTask\s*\(|isPassenger",
}
RE_GUARDS = {k: re.compile(v) for k, v in GUARD_KINDS.items()}


def git(*a):
    r = subprocess.run(["git", "-C", REPO] + list(a), capture_output=True,
                       text=True, encoding="utf-8", errors="replace")
    r.check_returncode()
    return r.stdout


def strip_comments(s):
    s = re.sub(r"/\*.*?\*/", " ", s, flags=re.S)
    return re.sub(r"//[^\n]*", " ", s)


def enclosing_body(src, idx):
    """从 idx 往回找到它所在方法体的 '{'，再往前配对截出整个方法体。
    截不到就返回 None——**必须显式区分「没有守卫」与「截取失败」**。"""
    depth = 0
    start = None
    for i in range(idx, -1, -1):
        c = src[i]
        if c == '}':
            depth += 1
        elif c == '{':
            if depth == 0:
                start = i
                break
            depth -= 1
    if start is None:
        return None
    depth = 0
    for j in range(start, len(src)):
        if src[j] == '{':
            depth += 1
        elif src[j] == '}':
            depth -= 1
            if depth == 0:
                return src[start:j + 1]
    return None


def guards_of(body):
    if body is None:
        return None
    return sorted(k for k, rx in RE_GUARDS.items() if rx.search(body))


def scan(ref):
    files = [p for p in git("ls-tree", "-r", "--name-only", ref, "--",
                            "src/main/java").splitlines() if p.endswith(".java")]
    menus = {}          # 菜单注册 id -> 容器类
    screens = {}        # MenuType 常量名 -> 屏类
    menu_consts = {}    # MenuType 常量名 -> 菜单注册 id
    opens = defaultdict(set)   # 打开点所在文件 -> 守卫类别
    open_sites = 0
    open_unparsed = 0
    screen_decls = {}          # 屏类 -> 声明它的文件
    ctor_sites = defaultdict(set)   # 屏类 -> 构造它的文件（不含自身）
    srcs = {}

    for p in files:
        raw = git("show", "%s:%s" % (ref, p))
        src = strip_comments(raw)
        srcs[p] = src

        # 1. 菜单注册：只认「注册进 BuiltInRegistries.MENU」的那个 helper 所在文件
        if "BuiltInRegistries.MENU" in src:
            for m in RE_MENU_REG.finditer(src):
                menus[m.group(1)] = m.group(2)
            # 常量名 -> id（同一行的 `MenuType<X> NAME = register("id", ...)`）
            for m in re.finditer(r'MenuType<[^>]*>\s+(\w+)\s*=\s*register\s*\(\s*"([^"]+)"', src):
                menu_consts[m.group(1)] = m.group(2)

        # 2. 屏绑定
        for m in RE_SCREEN_REG.finditer(src):
            screens[m.group(1)] = m.group(2)

        # 3. openMenu 打开点
        for m in RE_OPEN_MENU.finditer(src):
            open_sites += 1
            g = guards_of(enclosing_body(src, m.start()))
            if g is None:
                open_unparsed += 1
                continue
            opens[p.rsplit("/", 1)[-1][:-5]] |= set(g)

        # 4. 屏类声明（用于「有没有任何构造点」这条成因判据）
        for m in RE_SCREEN_DECL.finditer(src):
            screen_decls[m.group(1)] = p

    # 4b. 逐个屏类数它在**自身文件之外**的构造点——不问是 setScreen 直开还是经 helper
    for cls, decl_path in screen_decls.items():
        # `new X(` 与 `X::new` 都是构造点——只认前者会把所有经 MenuScreens.register(TYPE, X::new)
        # 绑定的屏全判成「零构造点」（实证：两树各 16 个，全是菜单屏，纯属判据盲区）
        rx = re.compile(r"\bnew\s+%s\s*\(|\b%s\s*::\s*new\b" % (re.escape(cls), re.escape(cls)))
        for p, src in srcs.items():
            if p == decl_path:
                continue
            if rx.search(src):
                ctor_sites[cls].add(p)

    orphan = sorted(c for c in screen_decls if not ctor_sites.get(c))
    live = dict(files=len(files), menus=len(menus), screens=len(screens),
                open_sites=open_sites, open_unparsed=open_unparsed,
                open_files=len(opens), screen_decls=len(screen_decls),
                orphan=len(orphan))
    return dict(menus=menus, screens=screens, menu_consts=menu_consts,
                opens=dict(opens), screen_decls=screen_decls,
                ctors={k: sorted(v) for k, v in ctor_sites.items()},
                orphan=orphan), live


def main():
    data = {}
    for ref, label in REFS.items():
        d, live = scan(ref)
        data[ref] = d
        print("[活性] %-4s java 文件 %d · 菜单 id %d · 屏绑定 %d · openMenu 点 %d(截取失败 %d，落在 %d 个类) "
              "· 屏类 %d(其中零构造点 %d)"
              % (label, live["files"], live["menus"], live["screens"],
                 live["open_sites"], live["open_unparsed"], live["open_files"],
                 live["screen_decls"], live["orphan"]))
    print()

    h, b, o = data["HEAD"], data["port/1.21.11-fabric"], data["origin/26.1"]

    # ① 菜单 id 存在性
    print("=== ① 菜单注册 id 对差 ===")
    only_b = sorted(set(b["menus"]) - set(h["menus"]))
    only_h = sorted(set(h["menus"]) - set(b["menus"]))
    print("  ⚠ 基准有、本树无（%d）：" % len(only_b))
    for i in only_b:
        print("      %-42s 基准容器=%-34s 宿主有？%s"
              % (i, b["menus"][i], "是" if i in o["menus"] else "否"))
    print("  本树有、基准无（%d）：" % len(only_h))
    for i in only_h:
        print("      %-42s 本树容器=%-34s 宿主有？%s"
              % (i, h["menus"][i], "是" if i in o["menus"] else "否"))

    # ② 屏绑定完整性：每个菜单 id 是否都有屏
    print("\n=== ② 屏绑定完整性（每个 MenuType 常量是否都被 MenuScreens.register）===")
    for ref, label in (("HEAD", "本树"), ("port/1.21.11-fabric", "基准")):
        d = data[ref]
        unbound = sorted(c for c in d["menu_consts"] if c not in d["screens"])
        bound_unknown = sorted(c for c in d["screens"] if c not in d["menu_consts"])
        print("  [%s] 菜单常量 %d · 已绑定 %d · **未绑定屏 %d** · 绑了但不是本仓库菜单常量 %d"
              % (label, len(d["menu_consts"]), len(d["screens"]),
                 len(unbound), len(bound_unknown)))
        for c in unbound:
            print("      ⚠ 未绑定：%-38s (id=%s)" % (c, d["menu_consts"][c]))
        for c in bound_unknown:
            print("      · 外部/兼容绑定：%s -> %s" % (c, d["screens"][c]))

    # ③ 打开点守卫对差
    print("\n=== ③ openMenu 打开点的守卫类别对差（按类）===")
    common = sorted(set(h["opens"]) & set(b["opens"]))
    diff = 0
    for c in common:
        miss = set(b["opens"][c]) - set(h["opens"][c])
        if miss:
            diff += 1
            print("      ⚠ %-34s 基准有本树无：%-28s 本树=%s"
                  % (c, ",".join(sorted(miss)), ",".join(h["opens"][c]) or "无"))
    print("      共有的打开点类 %d · 守卫变弱 %d" % (len(common), diff))
    ob = sorted(set(b["opens"]) - set(h["opens"]))
    oh = sorted(set(h["opens"]) - set(b["opens"]))
    print("      仅基准有打开点的类（%d）：%s" % (len(ob), ", ".join(ob) or "无"))
    print("      仅本树有打开点的类（%d）：%s" % (len(oh), ", ".join(oh) or "无"))

    # ④ 屏的可达性：基准有这个屏且它会被构造，本树要么没这个屏、要么它零构造点
    print("\n=== ④ 屏可达性对差（判据＝该屏在自身文件之外有没有构造点）===")
    unreachable = []
    for s in sorted(b["screen_decls"]):
        if not b["ctors"].get(s):
            continue                      # 基准自己就零构造点，不作基准
        if s not in h["screen_decls"]:
            unreachable.append((s, "本树无此屏类"))
        elif not h["ctors"].get(s):
            unreachable.append((s, "本树有此屏类但**零构造点**"))
    print("      ⚠ 基准可达、本树不可达（%d）：" % len(unreachable))
    for s, why in unreachable:
        print("          %-42s %s  宿主有此屏？%s"
              % (s, why, "是" if s in o["screen_decls"] else "否"))
    print("      本树零构造点的屏（%d）：%s"
          % (len(h["orphan"]), ", ".join(h["orphan"]) or "无"))
    print("      基准零构造点的屏（%d）：%s"
          % (len(b["orphan"]), ", ".join(b["orphan"]) or "无"))


if __name__ == "__main__":
    main()
