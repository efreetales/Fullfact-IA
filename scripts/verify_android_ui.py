"""Verify native navigation and capture the actual APK, without camera permissions."""
import pathlib
import re
import subprocess
import time
import xml.etree.ElementTree as ET

PACKAGE = 'br.com.debateemcontexto'
OUT = pathlib.Path('ui-screenshots')
OUT.mkdir(exist_ok=True)

def adb(*args):
    return subprocess.check_output(['adb', *args], text=True).strip()

def tree():
    for attempt in range(4):
        adb('shell', 'rm', '-f', '/sdcard/window.xml')
        adb('shell', 'uiautomator', 'dump', '--compressed', '/sdcard/window.xml')
        try:
            return ET.fromstring(adb('shell', 'cat', '/sdcard/window.xml'))
        except (ET.ParseError, subprocess.CalledProcessError):
            time.sleep(1)
    raise AssertionError('Não foi possível obter a árvore de acessibilidade atual.')

def find(value):
    for attempt in range(3):
        try:
            current = tree()
            for node in current.iter('node'):
                if node.attrib.get('text') == value:
                    return node
            (OUT / 'last-screen.xml').write_text(ET.tostring(current, encoding='unicode'))
        except AssertionError:
            pass
        time.sleep(1)
    screenshot('failure.png')
    (OUT / 'logcat.txt').write_text(adb('logcat', '-d', '-t', '350'))
    raise AssertionError('Elemento não encontrado: ' + value)

def tap(value):
    node = find(value)
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', node.attrib['bounds']))
    adb('shell', 'input', 'tap', str((x1+x2)//2), str((y1+y2)//2))
    time.sleep(1)

def screenshot(name):
    with (OUT / name).open('wb') as file:
        subprocess.run(['adb', 'exec-out', 'screencap', '-p'], stdout=file, check=True)
    assert adb('shell', 'pidof', PACKAGE), 'Aplicativo encerrou inesperadamente'

adb('install', '-r', 'app/build/outputs/apk/debug/app-debug.apk')
adb('shell', 'settings', 'put', 'system', 'font_scale', '1.0')
adb('shell', 'am', 'start', '-W', '-n', PACKAGE + '/.MainActivity')
time.sleep(2)
find('Conexão com a análise')
screenshot('01-preparar.png')
tap('02  Acompanhar')
find('Trecho a analisar')
find('Começar em · minutos:segundos')
screenshot('02-acompanhar.png')
tap('03  Checagens')
find('Nenhuma checagem disponível')
screenshot('03-checagens.png')
tap('01  Preparar')
adb('shell', 'am', 'force-stop', PACKAGE)
adb('shell', 'settings', 'put', 'system', 'font_scale', '1.3')
adb('shell', 'am', 'start', '-W', '-n', PACKAGE + '/.MainActivity')
time.sleep(2)
find('Conexão com a análise')
find('03  Checagens')
screenshot('04-texto-ampliado.png')
print('Interface Android: navegação entre três telas e texto ampliado conferidos.')

