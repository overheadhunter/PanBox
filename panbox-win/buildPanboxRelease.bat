@echo off
echo Building new Panbox release...

cd ..\panbox-core
call ant
if %errorlevel% neq 0 (
   exit /b %errorlevel%
)

cd ..\panbox-common
call ant
if %errorlevel% neq 0 (
   exit /b %errorlevel%
)

cd ..\panbox-win 
call ant
if %errorlevel% neq 0 (
   exit /b %errorlevel%
)

echo Copying all dependencies for Panbox build
xcopy /y /i JDokan.dll dist\
xcopy /y /i jniopencv_core.dll dist\
xcopy /y /i jniopencv_highgui.dll dist\
xcopy /y /i jniopencv_imgproc.dll dist\
xcopy /y /i jniopencv_ml.dll dist\
xcopy /y /i opencv_core249.dll dist\
xcopy /y /i opencv_highgui249.dll dist\
xcopy /y /i opencv_imgproc249.dll dist\
xcopy /y /i opencv_ml249.dll dist\
xcopy /y /i jnivideoInputLib.dll dist\
xcopy /y /i msvcp100.dll dist\
xcopy /y /i msvcr100.dll dist\
xcopy /y /i build-dependencies\Panbox.ico dist\
xcopy /y /i build-dependencies\panbox_splashscreen.png dist\
xcopy /y /i build-dependencies\commons-daemon-1.0.15-bin-windows\prunmgr.exe dist\
xcopy /y /i build-dependencies\commons-daemon-1.0.15-bin-windows\prunsrv.exe dist\