@echo off

REM Identical to xccl.bat except without compiling.

if [%1]==[] goto usage

SET root=%~dp0\..\..
java -cp %root%\cls com.ccl.desktop.Runtime %root% %1
goto :eof

:usage
@echo Usage: %0 ^<path_to_module^>
exit /B 1
