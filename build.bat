@echo off
set OLD_CD=%cd%
cd "%~dp0"
if exist out rmdir /S /Q out
mkdir out
javac -cp ./lib/morkai-californium-0.8.4-SNAPSHOT.jar;./lib/naga-no-em-3_0.jar -d ./out ./src/*.java
cd out
jar cvfm ../run/cf-proxy.jar ../MANIFEST.MF *.class
cd ..
rmdir /S /Q out
cd %OLD_CD%
