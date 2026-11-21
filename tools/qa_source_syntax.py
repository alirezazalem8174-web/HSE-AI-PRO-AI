from pathlib import Path
import re, sys
ROOT=Path(__file__).resolve().parents[1]
errors=[]

def fail(name,msg):
    errors.append(f"{name}: {msg}")
    print("FAIL",name,msg)

def ok(name): print("PASS",name)

# This project intentionally uses ordinary Java string literals (not Java text blocks).
# Scan source character-by-character so escaped \n inside a literal is allowed, while a real
# physical newline inside a quoted literal is rejected.
def has_raw_newline_in_string(src):
    in_string=False
    escaped=False
    in_char=False
    line=1
    for ch in src:
        if ch == "\n":
            if in_string or in_char:
                return line
            line += 1
            continue
        if in_string:
            if escaped:
                escaped=False
            elif ch == "\\":
                escaped=True
            elif ch == '"':
                in_string=False
        elif in_char:
            if escaped:
                escaped=False
            elif ch == "\\":
                escaped=True
            elif ch == "'":
                in_char=False
        else:
            if ch == '"':
                in_string=True
            elif ch == "'":
                in_char=True
    return None

for p in sorted((ROOT/'app/src/main/java').rglob('*.java')):
    s=p.read_text(encoding='utf-8')
    line=has_raw_newline_in_string(s)
    if line:
        fail(f"Java string syntax {p.name}", f"a quoted literal crosses physical line {line}")
    else:
        ok(f"Java string syntax {p.name}")

# Guard the AI dependency against accidental downgrade to a release without the intended
# Gemma 4 multimodal support used by this project.
bg=(ROOT/'app/build.gradle').read_text(encoding='utf-8')
if 'com.google.ai.edge.litertlm:litertlm-android:0.19.1' in bg:
    ok('LiteRT-LM version pinned 0.19.1')
else:
    fail('LiteRT-LM version', 'expected pinned 0.19.1')

print('\nSOURCE SYNTAX QA:', 'PASS' if not errors else f'FAIL errors={len(errors)}')
if errors:
    print('\n'.join(errors)); sys.exit(1)
