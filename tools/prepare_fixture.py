#!/usr/bin/env python3
"""Prepare the integration-test fixture outside any cloud-specific workspace.

Requires Java 25, Python 3 and curl. No Minecraft account or secret is needed.
Usage: python3 tools/prepare_fixture.py [build/paper-fixture]
This downloads official artifacts, verifies hashes and resolves BetterModel's
loader libraries through strict-checksum Maven using the inherited proxy.
"""
from pathlib import Path
import hashlib
import os
import shutil
import subprocess
import sys
import tarfile
from urllib.parse import urlsplit
import xml.etree.ElementTree as ET

ROOT = Path(sys.argv[1] if len(sys.argv) > 1 else 'build/paper-fixture').resolve()


def artifact(url, path, expected, algorithm='sha256'):
    path.parent.mkdir(parents=True, exist_ok=True)
    if not path.exists():
        partial = path.with_name(path.name + '.partial')
        subprocess.run(['curl', '-fLsS', '--retry', '2', '--max-time', '240', '-o', str(partial), url], check=True)
        partial.replace(path)
    with path.open('rb') as stream:
        actual = hashlib.file_digest(stream, algorithm).hexdigest()
    if actual != expected:
        raise RuntimeError('Checksum mismatch: ' + str(path))
    print('Verified:', path.name, flush=True)


def main():
    ROOT.mkdir(parents=True, exist_ok=True)
    (ROOT / 'plugins/BetterModel').mkdir(parents=True, exist_ok=True)
    sha = 'f9df8e19f4bdee97dbded48095391ea69c64342a4e19624db4f5e0d0136dfe0b'
    artifact(f'https://fill-data.papermc.io/v1/objects/{sha}/paper-26.3-253.jar', ROOT / 'paper-26.3-253.jar', sha)
    artifact('https://github.com/toxicity188/BetterModel/releases/download/3.6.0/bettermodel-3.6.0-paper.jar',
             ROOT / 'plugins/bettermodel-3.6.0-paper.jar', '66b2acc679ffcb81d9d7de686092d3e1daa38dc154da4cdaa9aaabc048a06aba')
    tools = ROOT / 'fixture-tools'
    archive = tools / 'apache-maven-3.9.11-bin.tar.gz'
    artifact('https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.tar.gz',
             archive, 'bcfe4fe305c962ace56ac7b5fc7a08b87d5abd8b7e89027ab251069faebee516b0ded8961445d6d91ec1985dfe30f8153268843c89aa392733d1a3ec956c9978', 'sha512')
    with tarfile.open(archive) as source:
        source.extractall(tools, filter='data')
    settings = ET.Element('settings', xmlns='http://maven.apache.org/SETTINGS/1.2.0')
    proxy_url = os.environ.get('HTTPS_PROXY') or os.environ.get('https_proxy')
    if proxy_url:
        parsed = urlsplit(proxy_url)
        if parsed.username or parsed.password:
            raise RuntimeError('Authenticated proxy: configure Maven settings securely instead of writing credentials here')
        proxy = ET.SubElement(ET.SubElement(settings, 'proxies'), 'proxy')
        for key, value in {'id': 'egress', 'active': 'true', 'protocol': 'http', 'host': parsed.hostname,
                           'port': str(parsed.port or 80), 'nonProxyHosts': 'localhost|127.*'}.items():
            ET.SubElement(proxy, key).text = value
    config = tools / 'settings.xml'
    ET.ElementTree(settings).write(config, encoding='utf-8', xml_declaration=True)
    config.chmod(0o600)
    project = ET.Element('project', xmlns='http://maven.apache.org/POM/4.0.0')
    for key, value in {'modelVersion': '4.0.0', 'groupId': 'local.motionrp', 'artifactId': 'fixture', 'version': '1'}.items():
        ET.SubElement(project, key).text = value
    dependencies = ET.SubElement(project, 'dependencies')
    for coordinate in ['org.semver4j:semver4j:6.0.0', 'io.github.toxicity188:dynamicuv:1.4.1',
                       'io.github.toxicity188:java-mesh:0.0.1', 'com.github.ben-manes.caffeine:caffeine:3.3.1']:
        dependency = ET.SubElement(dependencies, 'dependency')
        for key, value in zip(['groupId', 'artifactId', 'version'], coordinate.split(':')):
            ET.SubElement(dependency, key).text = value
    pom = tools / 'pom.xml'
    ET.ElementTree(project).write(pom, encoding='utf-8', xml_declaration=True)
    cache = tools / 'maven-cache'
    runtime_env = os.environ.copy()
    system_ca = Path('/etc/ssl/certs/java/cacerts')
    if system_ca.is_file() and 'javax.net.ssl.trustStore=' not in runtime_env.get('MAVEN_OPTS', ''):
        runtime_env['MAVEN_OPTS'] = runtime_env.get('MAVEN_OPTS', '') + ' -Djavax.net.ssl.trustStore=' + str(system_ca)
    subprocess.run([str(tools / 'apache-maven-3.9.11/bin/mvn'), '-B', '-C', '-ntp', '-s', str(config),
                    '-Dmaven.repo.local=' + str(cache), '-f', str(pom),
                    'org.apache.maven.plugins:maven-dependency-plugin:3.8.1:resolve'], check=True, env=runtime_env)
    for file in cache.rglob('*'):
        if file.suffix not in ('.jar', '.pom'):
            continue
        for algorithm in ('sha512', 'sha256', 'sha1'):
            checksum = file.with_name(file.name + '.' + algorithm)
            if checksum.exists():
                with file.open('rb') as stream:
                    actual = hashlib.file_digest(stream, algorithm).hexdigest()
                if actual != checksum.read_text().split()[0].lower():
                    raise RuntimeError('Invalid cached artifact: ' + str(file))
                break
        else:
            raise RuntimeError('Missing repository checksum: ' + str(file))
        target = ROOT / 'libraries' / file.relative_to(cache)
        target.parent.mkdir(parents=True, exist_ok=True)
        if target.exists() and target.read_bytes() != file.read_bytes():
            raise RuntimeError('Different existing artifact: ' + str(target))
        if not target.exists():
            shutil.copy2(file, target)
    print('Fixture prepared:', ROOT)
    print('Run integration tests with MOTIONRP_PAPER_FIXTURE=' + str(ROOT))


if __name__ == '__main__':
    main()
