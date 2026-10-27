@echo off
rem Copies the game into the app so it runs fully offline inside the APK. Run from the android-app folder.
set SRC=%1
if "%SRC%"=="" set SRC=..
set DST=app\src\main\assets\game
if exist %DST% rmdir /s /q %DST%
mkdir %DST%
for %%f in (index.html NIGHTMARE.html gameover.html admin.html GameEditor.html style.css nightmare.css hdb.css hdb.js settings.js features.js game.js nightmare.js tutorial.js nightmare-tutorial.js manifest.webmanifest sw.js) do if exist "%SRC%\%%f" copy "%SRC%\%%f" %DST% >nul
xcopy "%SRC%\Assets" %DST%\Assets /E /I /Q >nul
if exist "%SRC%\icons" xcopy "%SRC%\icons" %DST%\icons /E /I /Q >nul
echo Game copied to %DST%
