@echo off
echo ----------------------------------------
echo BUILDING CORE...
echo ----------------------------------------
cd core
cmd /c "gradlew.bat" clean publishtoMavenLocal

echo ----------------------------------------
echo BUILDING PLUGIN...
echo ----------------------------------------
cd ..\gradle-plugin
cmd /c "gradlew.bat" clean publishtoMavenLocal

cd ..
