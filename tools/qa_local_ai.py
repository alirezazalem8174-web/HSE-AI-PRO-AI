from pathlib import Path
import re, sys
ROOT=Path(__file__).resolve().parents[1]
errors=[]
def ok(n): print('PASS',n)
def fail(n,m): errors.append(f'{n}: {m}'); print('FAIL',n,m)

bg=(ROOT/'app/build.gradle').read_text()
man=(ROOT/'app/src/main/AndroidManifest.xml').read_text()
ai=(ROOT/'app/src/main/java/com/ekbatan/hse/HseVisionAi.java').read_text()
main=(ROOT/'app/src/main/java/com/ekbatan/hse/MainActivity.java').read_text()
aiui=(ROOT/'app/src/main/java/com/ekbatan/hse/VisionAiFeature.java').read_text()

for needle in ["com.google.ai.edge.litertlm:litertlm-android:0.19.1"]:
    ok('AI dependency '+needle) if needle in bg else fail('AI dependency', 'missing pinned LiteRT-LM 0.19.0')
for needle in ['android.permission.INTERNET','libOpenCL.so','libvndksupport.so']:
    ok('AI manifest '+needle) if needle in man else fail('AI manifest '+needle,'missing')
for needle in [
    'gemma-4-E2B-it.litertlm',
    '2588147712L',
    '181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c',
    '6e5c4f1e395deb959c494953478fa5cec4b8008f',
    'Content.ImageFile',
    'Backend.GPU()',
    'Backend.CPU()',
    'conversation.sendMessage(contents)',
    'engine.createConversation()',
    'HSE',
    'MODEL_OK',
]:
    ok('AI source '+needle) if needle in ai else fail('AI source '+needle,'missing')
if 'API key' in ai or 'Authorization' in ai or 'OpenAI' in ai or 'Gemini API' in ai:
    fail('AI no-paid-api-contract','cloud API credential/provider logic detected')
else: ok('AI no-paid-api-contract')
for needle,source in [('PICK_AI_IMAGE',main),('prepareAiImage',main),('HseAppServices',main),('VisionAiFeature',aiui)]:
    ok('AI UI '+needle) if needle in source else fail('AI UI '+needle,'missing')
if 'حدود ۲.۶' in aiui and 'بدون API پولی' in aiui: ok('AI UI offline/free disclosure')
else: fail('AI UI offline/free disclosure','missing user-facing disclosure')
# Image size bound and no multi-turn reuse of image conversation.
if '20L*1024L*1024L' in main: ok('AI image input size bound')
else: fail('AI image input size bound','missing 20MB cap')
if ai.count('createConversation()') == 1: ok('AI fresh conversation per image')
else: fail('AI fresh conversation per image','unexpected conversation reuse')
# Model is downloaded outside APK rather than bundled as a giant binary.
if 'getExternalFilesDir("ai_models")' in ai and 'MODEL_URL' in ai: ok('AI model on-demand provisioning')
else: fail('AI model on-demand provisioning','missing')
# Integrity verification and resumable download.
for needle in ['Range','sha256(part)','writeVerifiedMarker','isVerifiedMarkerValid']:
    ok('AI integrity/download '+needle) if needle in ai else fail('AI integrity/download '+needle,'missing')
ok('AI rejects main-thread inference') if 'Looper.getMainLooper()' in ai and 'تحلیل هوش مصنوعی باید خارج از Thread اصلی' in ai else fail('AI rejects main-thread inference','missing')
ok('AI checks RAM before model download') if 'if (!deviceSuitable(context))' in ai and 'ensureModel(progress)' in ai else fail('AI checks RAM before model download','missing')
ok('AI image dimension bound') if '40_000_000L' in ai and 'BitmapFactory.Options' in ai else fail('AI image dimension bound','missing')
print('\nTOTAL', 'PASS' if not errors else 'FAIL', 'errors=',len(errors))
if errors:
    print('\n'.join(errors)); sys.exit(1)
