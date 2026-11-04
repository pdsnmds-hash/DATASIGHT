"""Local CI helper checks. Generates only disposable fixture keys, never an upload key."""
import base64
import importlib.util
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import zipfile
import yaml

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
JAVA = Path(os.environ['JAVA_HOME']) / 'bin'
RESULT = {}

def run(args, env=None, ok=True, cwd=None, input=None):
    p = subprocess.run([str(a) for a in args], env=env, cwd=cwd, input=input, capture_output=True, text=True)
    if ok and p.returncode != 0:
        raise RuntimeError('Validation subprocess failed: ' + str(args[0]) + '\n' + p.stderr[-1500:])
    if not ok and p.returncode == 0:
        raise AssertionError('Expected failure: ' + str(args[0]))
    return p

spec = importlib.util.spec_from_file_location('bridge', HERE / 'prepare_signing.py')
bridge = importlib.util.module_from_spec(spec); spec.loader.exec_module(bridge)
for file in [ROOT / 'codemagic.yaml', HERE / 'generate-upload-key.yaml', HERE / 'codemagic.yaml']:
    doc = yaml.safe_load(file.read_text())
    for workflow in doc['workflows'].values():
        for step in workflow['scripts']:
            run(['bash', '-n'], input=step['script'])
assert (ROOT / 'codemagic.yaml').read_bytes() == (HERE / 'generate-upload-key.yaml').read_bytes()
RESULT['yaml_and_bash_syntax'] = 'PASS'

with tempfile.TemporaryDirectory(prefix='datasight-ci-fixtures-') as directory:
    temp = Path(directory)
    env = {k:v for k,v in os.environ.items() if not k.startswith('CM_')}
    source = temp / 'fixture-only.jks'; source.write_bytes(b'fixture, not a key')
    checkout = temp / 'checkout'; checkout.mkdir()
    special = ' leading space : = # ! \\ \t\n é Ж 🔑'
    signing = {**env, 'CM_BUILD_DIR':str(checkout), 'CM_KEYSTORE_PATH':str(source),
               'CM_KEYSTORE_PASSWORD':special, 'CM_KEY_ALIAS':'upload', 'CM_KEY_PASSWORD':special}
    run([sys.executable, HERE / 'prepare_signing.py'], signing)
    key = checkout / 'release/signing/upload-keystore.jks'; props = checkout / 'release/upload-key.properties'
    assert key.read_bytes() == source.read_bytes()
    assert key.stat().st_mode & 0o777 == 0o600 and props.stat().st_mode & 0o777 == 0o600
    before = props.read_bytes()
    run([sys.executable, HERE / 'prepare_signing.py'], signing, ok=False)
    assert before == props.read_bytes()
    for variable in ['CM_BUILD_DIR','CM_KEYSTORE_PATH','CM_KEYSTORE_PASSWORD','CM_KEY_ALIAS','CM_KEY_PASSWORD']:
        incomplete = signing.copy(); incomplete.pop(variable)
        run([sys.executable, HERE / 'prepare_signing.py'], incomplete, ok=False)
    RESULT['signing_bridge_paths_permissions_no_overwrite_required_variables'] = 'PASS'
    java = temp / 'ReadProperties.java'
    java.write_text('''import java.util.*; import java.io.*; import java.nio.charset.StandardCharsets;
class ReadProperties { public static void main(String[] args) throws Exception {
 Properties p=new Properties();try(InputStream in=new FileInputStream(args[0])){p.load(in);}
 for(String k:new String[]{"storeFile","storePassword","keyAlias","keyPassword"})
 System.out.println(Base64.getEncoder().encodeToString(p.getProperty(k).getBytes(StandardCharsets.UTF_8)));
}}''')
    run([JAVA/'javac',java])
    lines=run([JAVA/'java','-cp',temp,'ReadProperties',props]).stdout.splitlines()
    assert [base64.b64decode(s).decode() for s in lines] == ['release/signing/upload-keystore.jks',special,'upload',special]
    RESULT['java_properties_unicode_and_special_character_roundtrip'] = 'PASS'

    sdk=temp/'mock-sdk'; sdk.mkdir(); calls=temp/'sdk-calls.json'
    for name,rev in [('latest','10.0'),('latest-2','19.0')]:
        folder=sdk/'cmdline-tools'/name; (folder/'bin').mkdir(parents=True)
        (folder/'source.properties').write_text('Pkg.Revision='+rev+'\n')
        tool=folder/'bin/sdkmanager'
        tool.write_text('#!/usr/bin/env python3\nimport os,json,sys\nopen(os.environ["MOCK_CALLS"],"w").write(json.dumps(sys.argv))\nsys.exit(int(os.environ.get("MOCK_FAIL","0")))\n');tool.chmod(0o755)
    mock={**env,'ANDROID_SDK_ROOT':str(sdk),'CM_BUILD_DIR':str(checkout),'MOCK_CALLS':str(calls)}
    run([sys.executable,HERE/'configure_sdk.py'],mock)
    args=json.loads(calls.read_text())
    assert 'latest-2' in args[0] and args[1:]==[f'--sdk_root={sdk}','platforms;android-36','build-tools;36.0.0']
    run([sys.executable,HERE/'configure_sdk.py'],{**mock,'MOCK_FAIL':'2'},ok=False)
    shutil.rmtree(sdk/'cmdline-tools')
    run([sys.executable,HERE/'configure_sdk.py'],mock,ok=False)
    RESULT['sdkmanager_newest_revision_exact_packages_missing_tool_and_failure'] = 'PASS'

    # Disposable test key; no production upload key is generated here.
    fixture=temp/'test-only.jks'
    testenv={**env,'FIXTURE_STORE_PASSWORD':'temporary-fixture-password','FIXTURE_KEY_PASSWORD':'temporary-fixture-password'}
    run([JAVA/'keytool','-genkeypair','-noprompt','-keystore',fixture,'-storetype','JKS','-alias','fixture','-keyalg','RSA','-keysize','2048','-validity','1','-dname','CN=Disposable CI validation fixture','-storepass:env','FIXTURE_STORE_PASSWORD','-keypass:env','FIXTURE_KEY_PASSWORD'],testenv)
    verifier={**env,'CM_KEYSTORE_PATH':str(fixture),'CM_KEYSTORE_PASSWORD':testenv['FIXTURE_STORE_PASSWORD'],'CM_KEY_ALIAS':'fixture'}
    def archive(name, abis, signed, leak=False):
        file=temp/name
        with zipfile.ZipFile(file,'w') as z:
            z.writestr('base/manifest/AndroidManifest.xml','fixture only')
            for abi in abis:z.writestr(f'base/lib/{abi}/libfixture.so',b'fixture')
            if leak:z.writestr('base/assets/upload-key.properties','fixture')
        if signed:run([JAVA/'jarsigner','-keystore',fixture,'-storepass:env','FIXTURE_STORE_PASSWORD','-keypass:env','FIXTURE_KEY_PASSWORD',file,'fixture'],testenv)
        return file
    abis=['armeabi-v7a','arm64-v8a','x86','x86_64']
    run([sys.executable,HERE/'verify_aab.py',archive('signed.aab',abis,True)],verifier)
    run([sys.executable,HERE/'verify_aab.py',archive('unsigned.aab',abis,False)],verifier,ok=False)
    run([sys.executable,HERE/'verify_aab.py',archive('missing-abi.aab',abis[:-1],True)],verifier,ok=False)
    run([sys.executable,HERE/'verify_aab.py',archive('leaking.aab',abis,True,True)],verifier,ok=False)
    RESULT['verifier_signed_fixture_unsigned_missing_abi_and_private_file_rejection']='PASS'
    if '--real-bundle' in sys.argv:
        project=temp/'android-project';project.mkdir()
        for name in ['gradle','app/src']:
            shutil.copytree(ROOT/name,project/name)
        for name in ['gradlew','gradlew.bat','build.gradle.kts','settings.gradle.kts','gradle.properties','app/build.gradle.kts','local.properties']:
            target=project/name;target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(ROOT/name,target)
        projectenv={**testenv,'CM_BUILD_DIR':str(project),'CM_KEYSTORE_PATH':str(fixture),'CM_KEYSTORE_PASSWORD':testenv['FIXTURE_STORE_PASSWORD'],'CM_KEY_ALIAS':'fixture','CM_KEY_PASSWORD':testenv['FIXTURE_KEY_PASSWORD']}
        run([sys.executable,HERE/'prepare_signing.py'],projectenv)
        result=run(['bash',project/'gradlew','--no-daemon',':app:bundleRelease'],projectenv,cwd=project)
        (HERE/'local-release-build.txt').write_text(result.stdout+result.stderr)
        signed=project/'app/build/outputs/bundle/release/app-release.aab'
        verified=run([sys.executable,HERE/'verify_aab.py',signed],projectenv)
        (HERE/'local-release-verification.txt').write_text(verified.stdout)
        RESULT['real_release_bundle_temporary_key_isolated_checkout']='PASS; test AAB and key deleted, not deliverable'
RESULT['temporary_fixture_keys']='deleted with temporary directory'
RESULT['production_key_generation']='NOT RUN; Codemagic only'
RESULT['cloud_build']='NOT RUN'
(HERE/'helper-validation.json').write_text(json.dumps(RESULT,indent=2)+'\n')
print(json.dumps(RESULT,indent=2))
