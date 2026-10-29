#!/usr/bin/env python3
"""
Hidden API linkage checker.

Finds every framework method/field that an APK references and reports the ones
that do not exist on a given Android build. A missing reference is a
NoSuchMethodError / NoSuchFieldError waiting to happen (unless the call site is
guarded by a version check or a try/catch, which you have to confirm by hand).

Usage:
  # 1. Dump the framework of the target device (once per Android build)
  adb pull /system/framework/framework.jar
  python3 check.py dump-framework framework.jar framework-37.sigs

  # 2. Check an APK against it (.sigs or .sigs.gz)
  python3 check.py check app-debug.apk framework-37.sigs \
      [--only android/,com/android/] [--allow allowlist.txt]

  --only   only check classes under these package prefixes
  --allow  file with reviewed references ("Lpkg/Cls;.name(desc)" per line,
           '#' comments). They are still printed but do not fail the check.

Needs `dexdump` from Android SDK build-tools (set DEXDUMP or put it on PATH).
"""
import gzip
import os
import re
import shutil
import subprocess
import sys
import tempfile
import zipfile

CLASS_RE = re.compile(r"\s+Class descriptor\s+:\s+'(.*)'")
SUPER_RE = re.compile(r"\s+Superclass\s+:\s+'(.*)'")
IFACE_RE = re.compile(r"\s+#\d+\s+:\s+'(L.*;)'")
NAME_RE = re.compile(r"\s+name\s+:\s+'(.*)'")
TYPE_RE = re.compile(r"\s+type\s+:\s+'(.*)'")
SECTION_RE = re.compile(r"\s+(Interfaces|Static fields|Instance fields|Direct methods|Virtual methods)\s+-")
REF_RE = re.compile(r"(invoke-[a-z/-]+|[is]get[a-z-]*|[is]put[a-z-]*) \{?[^}]*\}?,? ?(?:v\d+, )*(L[^;]+;)\.([^:]+):(\S+)")


def dexdump():
    exe = os.environ.get("DEXDUMP") or shutil.which("dexdump")
    if not exe:
        sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
        if sdk and os.path.isdir(os.path.join(sdk, "build-tools")):
            for v in sorted(os.listdir(os.path.join(sdk, "build-tools")), reverse=True):
                cand = os.path.join(sdk, "build-tools", v, "dexdump")
                if os.path.exists(cand):
                    exe = cand
                    break
    if not exe:
        sys.exit("dexdump not found: set DEXDUMP or ANDROID_HOME")
    return exe


def extract_dex(archive, outdir):
    paths = []
    with zipfile.ZipFile(archive) as z:
        for n in z.namelist():
            if re.fullmatch(r"classes\d*\.dex", n):
                z.extract(n, outdir)
                paths.append(os.path.join(outdir, n))
    return sorted(paths)


def run_dexdump(args):
    return subprocess.run([dexdump()] + args, check=True, capture_output=True, text=True,
                          errors="replace").stdout


def dump_framework(jars, out):
    """Writes: C <class> <super> <iface,iface>; M <class> <name><desc>; F <class> <name>:<type>"""
    with open(out, "w") as o, tempfile.TemporaryDirectory() as tmp:
        for jar in jars:
            for dex in extract_dex(jar, os.path.join(tmp, os.path.basename(jar))):
                cur = sup = None
                ifaces = []
                section = None
                name = None
                for line in run_dexdump(["-l", "plain", dex]).splitlines():
                    m = CLASS_RE.match(line)
                    if m:
                        if cur:
                            o.write(f"C {cur} {sup or '-'} {','.join(ifaces) or '-'}\n")
                        cur, sup, ifaces, section, name = m.group(1), None, [], None, None
                        continue
                    m = SUPER_RE.match(line)
                    if m:
                        sup = m.group(1)
                        continue
                    m = SECTION_RE.match(line)
                    if m:
                        section = m.group(1)
                        continue
                    if section == "Interfaces":
                        m = IFACE_RE.match(line)
                        if m:
                            ifaces.append(m.group(1))
                            continue
                    m = NAME_RE.match(line)
                    if m:
                        name = m.group(1)
                        continue
                    m = TYPE_RE.match(line)
                    if m and name is not None and cur:
                        t = m.group(1)
                        if section in ("Direct methods", "Virtual methods"):
                            o.write(f"M {cur} {name}{t}\n")
                        elif section in ("Static fields", "Instance fields"):
                            o.write(f"F {cur} {name}:{t}\n")
                        name = None
                if cur:
                    o.write(f"C {cur} {sup or '-'} {','.join(ifaces) or '-'}\n")
    print(f"wrote {out}")


def load_sigs(path):
    classes, methods, fields = {}, set(), set()
    opener = gzip.open if path.endswith(".gz") else open
    with opener(path, "rt") as f:
        for line in f:
            if line.startswith("#") or len(line) < 3:
                continue
            kind, rest = line[0], line[2:].rstrip("\n")
            if kind == "C":
                c, s, i = rest.split(" ")
                classes[c] = (None if s == "-" else s, [] if i == "-" else i.split(","))
            elif kind == "M":
                methods.add(rest)
            elif kind == "F":
                fields.add(rest)
    return classes, methods, fields


def resolve(cls, member, table, classes):
    """Walk superclasses and interfaces like the ART linker does.

    Returns True (found), False (missing) or None (an ancestor such as
    java.lang.Object is not in the dump, so the reference cannot be verified)."""
    seen = set()
    todo = [cls]
    unknown_ancestor = False
    while todo:
        c = todo.pop(0)
        if c in seen or c is None:
            continue
        seen.add(c)
        if f"{c} {member}" in table:
            return True
        if c in classes:
            s, ifaces = classes[c]
            todo.append(s)
            todo.extend(ifaces)
        elif c == "Ljava/lang/Object;":
            if member in OBJECT_MEMBERS:
                return True
        else:
            unknown_ancestor = True
    return None if unknown_ancestor else False


OBJECT_MEMBERS = {
    "hashCode()I", "equals(Ljava/lang/Object;)Z", "toString()Ljava/lang/String;",
    "getClass()Ljava/lang/Class;", "notify()V", "notifyAll()V", "wait()V", "wait(J)V",
    "wait(JI)V", "clone()Ljava/lang/Object;", "finalize()V", "<init>()V",
}


def load_allowlist(path):
    """Returns ({reference: required_alternative_or_None}, {class descriptor}).

    "<ref> requires <alt>" means <ref> is only acceptable as a fallback when the
    APK also references <alt> (the new signature), which must exist.
    "class <descriptor>" allows a referenced class that is not in the dump
    (it lives in another boot jar, or only exists on other Android versions)."""
    allowed = {}
    classes = set()
    if not path:
        return allowed, classes
    with open(path) as f:
        for line in f:
            line = line.split("#", 1)[0].strip()
            if not line:
                continue
            if line.startswith("class "):
                classes.add(line[6:].strip())
            elif " requires " in line:
                ref, alt = (x.strip() for x in line.split(" requires ", 1))
                allowed[ref] = alt
            else:
                allowed[line] = None
    return allowed, classes


def check(apk, sigs, prefixes, allowlist=None):
    allowed, allowed_classes = load_allowlist(allowlist)
    classes, methods, fields = load_sigs(sigs)
    own = set()
    refs = {}
    with tempfile.TemporaryDirectory() as tmp:
        dexes = extract_dex(apk, tmp)
        for dex in dexes:
            for line in run_dexdump(["-l", "plain", dex]).splitlines():
                m = CLASS_RE.match(line)
                if m:
                    own.add(m.group(1))
        for dex in dexes:
            caller = None
            for line in run_dexdump(["-d", dex]).splitlines():
                if "|[" in line and "] " in line and ":(" in line:
                    caller = line.split("] ", 1)[1].strip()
                    continue
                m = REF_RE.search(line)
                if not m:
                    continue
                op, cls, name, desc = m.groups()
                if cls in own:
                    continue
                if prefixes and not any(cls[1:].startswith(p) for p in prefixes):
                    continue
                kind = "M" if op.startswith("invoke") else "F"
                key = (kind, cls, f"{name}{desc}" if kind == "M" else f"{name}:{desc}")
                refs.setdefault(key, set()).add(caller)
    missing_class, missing = {}, []
    for (kind, cls, member), callers in sorted(refs.items()):
        if cls not in classes:
            missing_class.setdefault(cls, set()).update(callers)
            continue
        table = methods if kind == "M" else fields
        if resolve(cls, member, table, classes) is False:
            missing.append((kind, cls, member, callers))
    print(f"{os.path.basename(sigs)}: checked {len(refs)} framework references")
    referenced = {f"{cls}.{member}" for (_, cls, member) in refs}
    unexpected, reviewed, missing_alt = [], [], []
    for m in missing:
        key = f"{m[1]}.{m[2]}"
        if key not in allowed:
            unexpected.append(m)
        elif allowed[key] is not None and allowed[key] not in referenced:
            missing_alt.append((m, allowed[key]))
        else:
            reviewed.append(m)
    if missing_alt:
        print(f"\n{len(missing_alt)} fallback(s) without the new signature (would fail on this Android):")
        for (kind, cls, member, callers), alt in missing_alt:
            print(f"  [{kind}] {cls}.{member}\n        needs {alt}")
            for c in sorted(callers)[:5]:
                print(f"        from {c}")
        unexpected.extend(m for m, _ in missing_alt)
    if reviewed:
        print(f"\n{len(reviewed)} missing reference(s) are in the allow-list (guarded fallbacks):")
        for kind, cls, member, callers in reviewed:
            print(f"  [{kind}] {cls}.{member}")
    if unexpected:
        print(f"\n{len(unexpected)} reference(s) NOT FOUND in {sigs}:")
        for kind, cls, member, callers in unexpected:
            print(f"  [{kind}] {cls}.{member}")
            for c in sorted(callers)[:5]:
                print(f"        from {c}")
    unknown_classes = {c: callers for c, callers in missing_class.items() if c not in allowed_classes}
    if unknown_classes:
        print(f"\n{len(unknown_classes)} referenced class(es) NOT FOUND in {sigs} and not allow-listed:")
        for c, callers in sorted(unknown_classes.items()):
            print(f"  {c}")
            for caller in sorted(callers)[:3]:
                print(f"        from {caller}")
        unexpected.extend(("C", c, "", callers) for c, callers in unknown_classes.items())
    if missing_class and os.environ.get("VERBOSE"):
        print(f"\n{len(missing_class)} referenced class(es) not in the dump (often fine: java.*, "
              f"androidx, or classes only present on other Android versions):")
        for c, callers in sorted(missing_class.items()):
            print(f"  {c}  ({len(callers)} caller(s), e.g. {sorted(callers)[0]})")
    return 1 if unexpected else 0


def main():
    if len(sys.argv) >= 4 and sys.argv[1] == "dump-framework":
        dump_framework(sys.argv[2:-1], sys.argv[-1])
        return 0
    if len(sys.argv) >= 4 and sys.argv[1] == "check":
        prefixes = []
        if "--only" in sys.argv:
            prefixes = sys.argv[sys.argv.index("--only") + 1].split(",")
        allow = None
        if "--allow" in sys.argv:
            allow = sys.argv[sys.argv.index("--allow") + 1]
        return check(sys.argv[2], sys.argv[3], prefixes, allow)
    print(__doc__)
    return 2


if __name__ == "__main__":
    sys.exit(main())
